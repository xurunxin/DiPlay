#!/usr/bin/env python3
"""Bootstrap the CarWith plan using the locally authenticated GitHub CLI.

Python 3.9+ and gh are required for --apply. Without --apply this is an offline
preview. No tokens are accepted, printed, or stored. Writes are sequential;
a failed write is not blindly retried. Re-run to resume after checking errors.
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path
from typing import Any

BASE = Path(__file__).resolve().parents[1]
PLAN_PATH = BASE / '.github' / 'carwith-project-plan.json'
API_VERSION = '2026-03-10'
FIELD_FRAGMENT = '''... on ProjectV2FieldCommon { id name dataType }
... on ProjectV2SingleSelectField { options { id name } }'''


class SetupError(RuntimeError):
    pass


class GitHub:
    def __init__(self) -> None:
        self.exe = shutil.which('gh')
        if not self.exe:
            raise SetupError('GitHub CLI (gh) was not found. Install it and run gh auth login --hostname github.com --scopes project.')
        self.last_write = 0.0

    def call(self, path: str, method: str = 'GET', data: Any = None,
             write: bool = False) -> Any:
        if write:
            # Avoid bursts of content creation / secondary rate limiting.
            time.sleep(max(0.0, 1.1 - (time.monotonic() - self.last_write)))
        command = [self.exe, 'api', '--hostname', 'github.com', path,
                   '--method', method, '-H', 'Accept: application/vnd.github+json',
                   '-H', 'X-GitHub-Api-Version: ' + API_VERSION]
        raw = None
        if data is not None:
            command += ['--input', '-']
            raw = json.dumps(data, ensure_ascii=False).encode('utf-8')
        try:
            result = subprocess.run(command, input=raw, capture_output=True, timeout=120)
        except subprocess.TimeoutExpired as exc:
            raise SetupError('GitHub call timed out; its write may have completed. Re-run to reconcile, do not create duplicates manually.') from exc
        finally:
            if write:
                self.last_write = time.monotonic()
        if result.returncode:
            message = result.stderr.decode('utf-8', errors='replace').strip()
            raise SetupError(f'{method} {path}: {message}\nCheck gh authentication, project scope, and repository permissions. Partial writes are retained.')
        text = result.stdout.decode('utf-8')
        obj = json.loads(text) if text.strip() else None
        if isinstance(obj, dict) and obj.get('errors'):
            raise SetupError('GraphQL: ' + json.dumps(obj['errors'], ensure_ascii=False))
        return obj

    def rest(self, path: str, method: str = 'GET', data: Any = None) -> Any:
        return self.call(path, method, data, write=(method != 'GET'))

    def pages(self, path: str) -> list[dict]:
        out = []
        for page in range(1, 10001):
            separator = '&' if '?' in path else '?'
            rows = self.rest(f'{path}{separator}per_page=100&page={page}')
            if not isinstance(rows, list):
                raise SetupError('Expected paginated list: ' + path)
            out.extend(rows)
            if len(rows) < 100:
                return out
        raise SetupError('Pagination safety limit reached; result was not treated as complete.')

    def gql(self, query: str, variables: dict | None = None) -> dict:
        obj = self.call('graphql', 'POST', {'query': query, 'variables': variables or {}},
                        write=query.lstrip().startswith('mutation'))
        if not isinstance(obj, dict) or not isinstance(obj.get('data'), dict):
            raise SetupError('GraphQL returned no usable data.')
        return obj['data']

    def mutate(self, name: str, input_type: str, values: dict, selection: str) -> dict:
        query = f'mutation($input:{input_type}!) {{ {name}(input:$input) {{ {selection} }} }}'
        return self.gql(query, {'input': values})[name]


def marker(plan: dict, key: str) -> str:
    return f'<!-- {plan["key"]}:{key} -->'


def section(body: str, name: str, value: str) -> str:
    """Replace only an explicitly managed section, retaining human edits elsewhere."""
    start, end = f'<!-- {name}:start -->', f'<!-- {name}:end -->'
    block = start + '\n' + value.rstrip() + '\n' + end
    if start not in body and end not in body:
        return body.rstrip() + '\n\n' + block + '\n'
    if body.count(start) != 1 or body.count(end) != 1 or body.index(start) > body.index(end):
        raise SetupError('Malformed managed section: ' + name)
    return body[:body.index(start)] + block + body[body.index(end) + len(end):]


def validate(plan: dict) -> None:
    if plan.get('schema_version') != 1 or plan.get('owner') != 'xurunxin' or plan.get('repository') != 'DiPlay':
        raise SetupError('This initializer is restricted to schema 1 and xurunxin/DiPlay.')
    keys = [t['key'] for t in plan['tasks']]
    if len(set(keys)) != len(keys) or 'ROADMAP' in keys:
        raise SetupError('Duplicate or reserved task keys.')
    stages = {m['key'] for m in plan['milestones']}
    if len(stages) != len(plan['milestones']):
        raise SetupError('Duplicate milestone keys.')
    seen = set()
    for task in plan['tasks']:
        if task['stage'] not in stages or task['priority'] not in ('P0', 'P1', 'P2'):
            raise SetupError('Invalid phase or priority: ' + task['key'])
        if task['readiness'] not in ('Ready', 'Blocked', 'Deferred'):
            raise SetupError('Invalid readiness: ' + task['key'])
        if any(dep not in seen for dep in task['depends_on']):
            raise SetupError('Tasks must be topologically ordered, without cycles or unknown dependencies: ' + task['key'])
        seen.add(task['key'])


def task_body(plan: dict, task: dict) -> str:
    lines = [marker(plan, task['key']), '## 目标', task['objective'],
             f'\n阶段：{task["stage"]} · 初始优先级：{task["priority"]} · 模块：{task["area"]}',
             '\n## 工作范围']
    lines += ['- [ ] ' + item for item in task['work']]
    lines += ['\n## 验收条件']
    lines += ['- [ ] ' + item for item in task['acceptance']]
    lines += ['\n## 边界与风险', task['limits'],
              '\n## 交付证据', '关联 PR/研究结论、测试步骤、设备与软件版本、脱敏证据、已知限制。',
              '\n初始状态为待办；这里的目标与验收参数不表示功能已经实现。']
    return '\n'.join(lines) + '\n'


def read_projects(gh: GitHub, owner: str) -> tuple[str, list[dict]]:
    query = '''query($owner:String!,$cursor:String) { user(login:$owner) { id
      projectsV2(first:100,after:$cursor) { nodes { id number title url readme closed viewerCanUpdate }
      pageInfo { hasNextPage endCursor } } } }'''
    cursor, out = None, []
    while True:
        user = gh.gql(query, {'owner': owner, 'cursor': cursor})['user']
        if not user:
            raise SetupError('GitHub owner not found.')
        page = user['projectsV2']
        out.extend(page['nodes'])
        if not page['pageInfo']['hasNextPage']:
            return user['id'], out
        cursor = page['pageInfo']['endCursor']


def project_connection(gh: GitHub, pid: str, name: str, selection: str) -> list[dict]:
    query = f'''query($id:ID!,$cursor:String) {{ node(id:$id) {{ ... on ProjectV2 {{
      {name}(first:100,after:$cursor) {{ nodes {{ {selection} }} pageInfo {{ hasNextPage endCursor }} }}
    }} }} }}'''
    out, cursor = [], None
    while True:
        node = gh.gql(query, {'id': pid, 'cursor': cursor})['node']
        if not node:
            raise SetupError('Project not accessible.')
        page = node[name]
        out.extend(n for n in page['nodes'] if n is not None)
        if not page['pageInfo']['hasNextPage']:
            return out
        cursor = page['pageInfo']['endCursor']


def ensure_fields(gh: GitHub, pid: str, plan: dict) -> dict:
    def read() -> dict:
        fields = project_connection(gh, pid, 'fields', FIELD_FRAGMENT)
        if len({f['name'] for f in fields}) != len(fields):
            raise SetupError('Duplicate field names; resolve in Project settings before retrying.')
        return {f['name']: f for f in fields}
    fields = read()
    specs = {
        'Priority': ['P0', 'P1', 'P2'],
        'Stage': ['Overview'] + [m['key'] for m in plan['milestones']],
        'Area': ['Planning'] + sorted({t['area'] for t in plan['tasks']}),
        'Readiness': ['Ready', 'Blocked', 'Deferred', 'Tracking'],
    }
    for name, choices in specs.items():
        if name in fields:
            field = fields[name]
            available = {o['name'] for o in field.get('options', [])}
            if field['dataType'] != 'SINGLE_SELECT' or not set(choices).issubset(available):
                raise SetupError(f'Existing {name} field differs from the plan. Its options were not replaced; reconcile them manually.')
            continue
        values = {'projectId': pid, 'name': name, 'dataType': 'SINGLE_SELECT',
                  'singleSelectOptions': [{'name': c, 'color': 'GRAY', 'description': c} for c in choices]}
        gh.mutate('createProjectV2Field', 'CreateProjectV2FieldInput', values,
                  'projectV2Field { ... on ProjectV2FieldCommon { id name } }')
    fields = read()
    if 'Status' not in fields or 'Todo' not in {o['name'] for o in fields['Status'].get('options', [])}:
        raise SetupError('The default Status field must include Todo; no existing Status options were overwritten.')
    return fields


def issue_index(plan: dict, issues: list[dict]) -> dict:
    out = {}
    pattern = re.compile(r'<!-- ' + re.escape(plan['key']) + r':([A-Z_]+) -->')
    for issue in issues:
        if 'pull_request' in issue:
            continue
        matches = pattern.findall(issue.get('body') or '')
        if len(matches) > 1:
            raise SetupError('Multiple plan identifiers in issue #' + str(issue['number']))
        if not matches:
            continue
        key = matches[0]
        if key in out:
            raise SetupError('Duplicate plan issues for ' + key + '; reconcile rather than silently adopting one.')
        out[key] = issue
    return out


def links_block(plan: dict, task: dict, issues: dict, project_url: str) -> str:
    parent = issues['ROADMAP']['html_url']
    dependencies = ', '.join(issues[d]['html_url'] for d in task['depends_on']) or '无硬前置；参见任务边界。'
    return f'## 跟踪关系\n总路线图：{parent}\n\nProject：{project_url}\n\n前置任务：{dependencies}\n\n注意：关卡必须有明确 Go/通过证据；No-Go 研究结论不能解锁下游实施。'


def root_links(plan: dict, issues: dict, project_url: str) -> str:
    lines = [f'## Project\n{project_url}', '\n## 阶段与任务']
    for milestone in plan['milestones']:
        lines += ['\n### ' + milestone['title'], milestone['description']]
        for task in plan['tasks']:
            if task['stage'] == milestone['key']:
                issue = issues[task['key']]
                done = 'x' if issue.get('state') == 'closed' else ' '
                lines.append(f'- [{done}] {issue["html_url"]} — {task["title"]}')
    lines += ['\n任务关闭只表示对应交付完成；G0/G1 等关卡仍须查验结论。',
              '\n## 建议视图\n全部任务：按 Stage 分组、Priority 排序。执行看板：按 Status 分列，Readiness=Ready 筛选可启动任务。',
              '\n未设置截止日期、负责人或持续运行的自动化。Readiness 需由维护者在关卡通过后更新。']
    return '\n'.join(lines)


def bootstrap(gh: GitHub, plan: dict, project_number: int | None = None) -> tuple[dict, dict, list[str]]:
    repo_name = plan['owner'] + '/' + plan['repository']
    base = 'repos/' + repo_name
    me = gh.rest('user')
    if me['login'].lower() != plan['owner'].lower():
        raise SetupError(f'Logged in as {me["login"]}; expected {plan["owner"]}. Use gh auth switch before --apply.')
    repo = gh.rest(base)
    if repo.get('full_name', '').lower() != repo_name.lower() or repo.get('archived'):
        raise SetupError('Repository identity mismatch or repository is archived.')
    if not repo.get('permissions', {}).get('push'):
        raise SetupError('Repository write permission is required.')
    if not repo.get('has_issues') and not repo.get('permissions', {}).get('admin'):
        raise SetupError('Enabling Issues requires repository administration permission. No writes performed.')

    owner_id, projects = read_projects(gh, plan['owner'])  # project-read preflight before writes
    project_marker = marker(plan, 'PROJECT')
    if project_number is not None:
        candidates = [p for p in projects if p['number'] == project_number]
        if len(candidates) != 1 or (project_marker not in (candidates[0].get('readme') or '') and candidates[0]['title'] != plan['project_title']):
            raise SetupError('Specified project was not found or does not match this plan.')
    else:
        candidates = [p for p in projects if project_marker in (p.get('readme') or '')]
        if not candidates and any(p['title'] == plan['project_title'] for p in projects):
            raise SetupError('An unmarked project with the same title exists. Check it and use --project-number N to resume explicitly.')
    if len(candidates) > 1:
        raise SetupError('Multiple matching projects; use --project-number N.')
    is_new = not candidates
    if is_new:
        project = gh.mutate('createProjectV2', 'CreateProjectV2Input',
                            {'ownerId': owner_id, 'repositoryId': repo['node_id'], 'title': plan['project_title']},
                            'projectV2 { id number title url readme closed viewerCanUpdate }')['projectV2']
    else:
        project = candidates[0]
        if project['closed'] or not project['viewerCanUpdate']:
            raise SetupError('Project is closed or not writable; no project settings changed.')
    pid = project['id']
    print('PROJECT: ' + project['url'], flush=True)
    print('Repository issues are ' + ('private.' if repo['private'] else 'PUBLIC.') + ' New Project visibility: private.', flush=True)
    if is_new or project_marker not in (project.get('readme') or ''):
        initial = project_marker + '\n\n' + plan['summary'] + '\n\n初始化尚未完成；成功输出前不要视为完整看板。'
        settings = {'projectId': pid, 'readme': section(project.get('readme') or '', plan['key'] + '-overview', initial)}
        if is_new:
            settings.update({'public': False, 'shortDescription': plan['summary']})
        project['readme'] = gh.mutate('updateProjectV2', 'UpdateProjectV2Input', settings,
                                      'projectV2 { readme }')['projectV2']['readme']
    linked = project_connection(gh, pid, 'repositories', 'id')
    if repo['node_id'] not in {r['id'] for r in linked}:
        gh.mutate('linkProjectV2ToRepository', 'LinkProjectV2ToRepositoryInput',
                  {'projectId': pid, 'repositoryId': repo['node_id']}, 'repository { id }')
    fields = ensure_fields(gh, pid, plan)

    if not repo['has_issues']:
        gh.rest(base, 'PATCH', {'has_issues': True})
        print('Enabled Issues for ' + repo_name, flush=True)
    existing_labels = {l['name'] for l in gh.pages(base + '/labels')}
    labels = {'carwith': 'CarWith projection plan', 'carwith:roadmap': 'Roadmap tracking',
              'carwith:P0': 'Critical path / decision gate', 'carwith:P1': 'MVP and quality',
              'carwith:P2': 'Optional enhancement / research'}
    for name, description in labels.items():
        if name not in existing_labels:
            gh.rest(base + '/labels', 'POST', {'name': name, 'color': '1D76DB', 'description': description})
    milestones = {m['title']: m for m in gh.pages(base + '/milestones?state=all')}
    by_stage = {}
    for milestone in plan['milestones']:
        title = milestone['title']
        if title not in milestones:
            milestones[title] = gh.rest(base + '/milestones', 'POST',
                                       {'title': title, 'description': milestone['description']})
        by_stage[milestone['key']] = milestones[title]['number']

    all_issues = gh.pages(base + '/issues?state=all')
    issues = issue_index(plan, all_issues)
    specs = [dict(plan['roadmap'], priority='P0', stage='Overview', area='Planning', readiness='Tracking', depends_on=[])] + plan['tasks']
    for spec in specs:
        key = spec['key']
        if key in issues:
            continue  # preserve human-edited title/body/labels/milestone/assignees/state
        if any(i['title'] == spec['title'] and 'pull_request' not in i for i in all_issues):
            raise SetupError('Unmarked issue with matching title exists: ' + spec['title'])
        values = {'title': spec['title'], 'body': marker(plan, key) + '\n\n' + spec['body'] if key == 'ROADMAP' else task_body(plan, spec),
                  'labels': ['carwith', 'carwith:' + spec['priority']]}
        if key == 'ROADMAP':
            values['labels'].append('carwith:roadmap')
        else:
            values['milestone'] = by_stage[spec['stage']]
        issues[key] = gh.rest(base + '/issues', 'POST', values)
        print('ISSUE: ' + issues[key]['html_url'], flush=True)

    for task in plan['tasks']:
        issue = issues[task['key']]
        updated = section(issue.get('body') or '', plan['key'] + '-links', links_block(plan, task, issues, project['url']))
        if updated != issue.get('body'):
            issues[task['key']] = gh.rest(base + f'/issues/{issue["number"]}', 'PATCH', {'body': updated})
    root = issues['ROADMAP']
    links = root_links(plan, issues, project['url'])
    updated = section(root.get('body') or '', plan['key'] + '-links', links)
    if updated != root.get('body'):
        issues['ROADMAP'] = gh.rest(base + f'/issues/{root["number"]}', 'PATCH', {'body': updated})

    item_fields = '''id isArchived content { ... on Issue { id } }
    fieldValues(first:100) { nodes { ... on ProjectV2ItemFieldSingleSelectValue {
      field { ... on ProjectV2FieldCommon { id } } } } }'''
    existing_items = project_connection(gh, pid, 'items', item_fields)
    by_content = {i['content']['id']: i for i in existing_items if i.get('content') and i['content'].get('id')}
    for spec in specs:
        issue = issues[spec['key']]
        item = by_content.get(issue['node_id'])
        if item is None:
            item = gh.mutate('addProjectV2ItemById', 'AddProjectV2ItemByIdInput',
                             {'projectId': pid, 'contentId': issue['node_id']}, 'item { id }')['item']
        if item.get('isArchived'):
            continue  # never unarchive somebody's work
        populated = {v['field']['id'] for v in item.get('fieldValues', {}).get('nodes', []) if v and 'field' in v}
        initial = {'Status': 'Todo', 'Priority': spec['priority'], 'Stage': spec['stage'],
                   'Area': spec['area'], 'Readiness': spec['readiness']}
        if issue.get('state') == 'closed':
            initial.pop('Readiness')  # do not infer gate success from closure
            initial['Status'] = 'Done'
        for name, value in initial.items():
            field = fields[name]
            if field['id'] in populated:
                continue  # preserve progress and any manually edited field values
            options = {o['name']: o['id'] for o in field.get('options', [])}
            if value not in options:
                raise SetupError(f'Missing option {name}={value}; existing fields were not replaced.')
            gh.mutate('updateProjectV2ItemFieldValue', 'UpdateProjectV2ItemFieldValueInput',
                      {'projectId': pid, 'itemId': item['id'], 'fieldId': field['id'],
                       'value': {'singleSelectOptionId': options[value]}}, 'projectV2Item { id }')

    warnings = []
    def optional(label: str, action: Any) -> None:
        try:
            action()
        except SetupError as exc:
            warnings.append(label + ': ' + str(exc))
            print('WARNING: ' + warnings[-1], file=sys.stderr, flush=True)

    def relationships() -> None:
        root_path = base + f'/issues/{root["number"]}/sub_issues'
        children = {i['id'] for i in gh.pages(root_path)}
        for task in plan['tasks']:
            issue = issues[task['key']]
            if issue['id'] not in children:
                gh.rest(root_path, 'POST', {'sub_issue_id': issue['id']})  # never replace another parent
            path = base + f'/issues/{issue["number"]}/dependencies/blocked_by'
            blockers = {i['id'] for i in gh.pages(path)} if task['depends_on'] else set()
            for dependency in task['depends_on']:
                blocker = issues[dependency]
                if blocker['id'] not in blockers:
                    gh.rest(path, 'POST', {'issue_id': blocker['id']})
    optional('Native sub-issues/dependencies (text links remain available)', relationships)

    def views() -> None:
        current = {v['name'] for v in project_connection(gh, pid, 'views', 'id name')}
        visible = [fields[n]['id'] for n in ('Title', 'Status', 'Priority', 'Stage', 'Area', 'Readiness', 'Milestone', 'Assignees') if n in fields]
        for name, layout in [('CarWith · 全部任务', 'TABLE_LAYOUT'), ('CarWith · 执行看板', 'BOARD_LAYOUT')]:
            if name not in current:
                gh.mutate('createProjectV2View', 'CreateProjectV2ViewInput',
                          {'projectId': pid, 'name': name, 'layout': layout,
                           'configuration': {'visibleFieldIds': visible}}, 'projectV2View { id name }')
    optional('Named table/board views', views)

    overview = project_marker + '\n\n' + plan['summary'] + '\n\n' + links
    if warnings:
        overview += '\n\n## 初始化未完成项\n部分原生关系或视图未完成；请查看本地输出。文本依赖仍在 Issues 中。修复权限后可重跑。'
    overview = section(project.get('readme') or '', plan['key'] + '-overview', overview)
    gh.mutate('updateProjectV2', 'UpdateProjectV2Input', {'projectId': pid, 'readme': overview}, 'projectV2 { id }')
    return project, issues, warnings


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true', help='Create GitHub resources; enable Issues if disabled.')
    parser.add_argument('--project-number', type=int, help='Explicitly resume an existing matching user Project.')
    args = parser.parse_args()
    plan = json.loads(PLAN_PATH.read_text(encoding='utf-8'))
    validate(plan)
    print(plan['project_title'])
    print(f'Target: {plan["owner"]}/{plan["repository"]}; 1 roadmap + {len(plan["tasks"])} tasks; {len(plan["milestones"])} milestones.')
    for task in plan['tasks']:
        print(f'  {task["key"]}: {task["priority"]} / {task["readiness"]} / {task["title"]}')
    if not args.apply:
        print('\nOFFLINE PREVIEW ONLY. No API requests or writes were made. Use --apply to initialize.')
        return 0
    project, issues, warnings = bootstrap(GitHub(), plan, args.project_number)
    print('\n' + ('PARTIAL SUCCESS (see warnings)' if warnings else 'INITIALIZATION COMPLETE'))
    print('Project: ' + project['url'])
    print('Roadmap: ' + issues['ROADMAP']['html_url'])
    print('No application code, repository visibility, personal assignees, due dates, or ongoing automations were changed.')
    return 2 if warnings else 0


if __name__ == '__main__':
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, 'reconfigure'):
            stream.reconfigure(encoding='utf-8', errors='replace')
    try:
        raise SystemExit(main())
    except (SetupError, OSError, ValueError, KeyError) as error:
        print('STOPPED: ' + str(error), file=sys.stderr)
        print('Partial resources may exist. Check the printed Project/Issue URLs, fix the cause, then re-run. Do not run concurrent instances.', file=sys.stderr)
        raise SystemExit(1)
