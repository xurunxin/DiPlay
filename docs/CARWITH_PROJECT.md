# DiPlay · CarWith 手机投屏计划

## 当前交付状态

这是 **Project 的完整计划与初始化工具，不是已经创建成功的 Project**。创建时，当前 GitHub 连接没有 Projects 创建/编辑接口；仓库创建 Issue 的请求返回 `410 Issues has been disabled in this repository.`。因此没有虚构 Project URL、Issue 编号或已完成开发进度。

本变更仅提供计划与脚本，不修改 Android 应用代码，不改变 `main`。合并或签出本 PR 本身不会创建任何 Project，也不会开启 Issues；必须由仓库所有者显式运行初始化命令。

## 本地执行

需要 Python 3.9+ 和 GitHub CLI `gh`。在仓库根目录，使用 `xurunxin` 登录 GitHub：

```powershell
gh auth login --hostname github.com --scopes project
python scripts/setup_carwith_project.py --apply
```

已有 CLI 登录、但缺少 Projects 权限时：

```powershell
gh auth refresh --hostname github.com --scopes project
```

仅预览，不登录、不联网、不写入任何资源：

```powershell
python scripts/setup_carwith_project.py
```

脚本只调用本机 `gh`，不会要求把 Token 填入代码、发到对话或上传至仓库。`--apply` 明确授权以下变更：

1. 在 `xurunxin` 下创建或复用 **DiPlay · CarWith 手机投屏计划**（GitHub Projects v2），关联 `xurunxin/DiPlay`。新 Project 默认为私有；已有 Project 不改变可见性。
2. 如果本仓库关闭 Issues，将 `has_issues` 设为 `true`。不修改仓库可见性、分支保护或任何其他设置。**本仓库是公开仓库，因此新 Issues 也会公开；Project 私有不会使 Issues 私有。**
3. 创建 1 条总路线图、13 条任务 Issues、6 个原生 Milestones，以及 `carwith`、`carwith:roadmap`、`carwith:P0/P1/P2` 标签。
4. 导入全部 Issues，填入 `Priority`、`Stage`、`Area`、`Readiness` 与内置 `Status`；没有擅自指派个人或设置截止日期。
5. 建立原生父子 Issue、`blocked_by` 依赖与双向 Markdown 引用；创建命名表格视图和看板视图。

若某个可选的原生关系或视图 API 被权限/服务版本阻止，脚本保留已有成果与文本依赖，明确输出警告并以退出码 2 结束，不报告完整成功。核心创建失败返回退出码 1。完整初始化返回 0，并打印真实 Project 和总路线图链接。

## 计划范围

第一版目标：指定小米手机的 **现有 CarWith，通过 CarLife 兼容入口/插件**，在 DiPlay 中实现无线画面、单指反控、媒体音频和导航播报。

当前并未在目标设备完成联调，不保证任何 CarWith/插件版本均可用。历史 CarLife 源码只是研究参考；首先确认合法接入条件和真实协议。原生 ICCOA、小窗、完整手机镜像、USB 与 HUD 不能混入第一版兼容承诺。

| 阶段 | 任务 | 退出关卡 |
| --- | --- | --- |
| M0 | 设备/软件矩阵与真实入口；来源/许可/认证审查 | G0：明确 Go/No-Go，且通过结论有证据 |
| M1 | 已知 IP 最小会话与协议编解码；首帧/连续视频 | G1：自有接收实现显示真实 CarWith 连续画面 |
| M2 | ProjectionBackend 与公共媒体层；单指反控；基础音频 | G2：可交互 MVP，CarPlay 无已知新增回归 |
| M3 | 生命周期/恢复；回归/诊断/兼容矩阵/试用发布 | G3：范围明确的可重复台架验收 |
| M4 | 自动发现/重连；USB；结构化导航/HUD | 各功能独立验证，不阻塞无线 MVP |
| R1 | ICCOA SDK/授权/能力等级与接入研究 | 独立 Go/No-Go，通过后再新建实现任务 |

每个任务已定义目标、工作项、依赖、验收条件、证据要求与风险。完整数据源是 [carwith-project-plan.json](../.github/carwith-project-plan.json)。不将阶段 M0–M4 与优先级 P0–P2 混淆。

### 初始状态与维护

所有新任务的实现状态是 `Todo`；没有任务被虚标为进行中或完成。`Readiness` 区分：

- `Ready`：M0 两个研究/审查任务可启动。
- `Blocked`：主路径尚未满足前置关卡。
- `Deferred`：自动发现、USB、HUD、ICCOA 后续研究/增强。
- `Tracking`：总路线图。

`Status` 沿用 GitHub 默认 Todo / In Progress / Done，不替换已存在的状态选项。`Readiness` 由维护者根据关卡证据更新；**仅关闭前置研究 Issue（尤其 No-Go）不代表可以启动下游实现**。

脚本创建两个视图：`CarWith · 全部任务`（表格）与 `CarWith · 执行看板`（看板），展示关键字段。分组/过滤细节请在界面按需要调整：表格按 Stage 分组、Priority 排序；看板按 Status 分列，并用 `Readiness:Ready` 聚焦可启动任务。脚本不声称已配置自定义筛选、自动拉取新 Issues、持续更新 Readiness 或关闭任务自动推进等工作流。

## 重复运行与恢复

脚本用稳定的 HTML 注释标识查找属于本计划的 Project 与 Issues，先读取再创建；完整分页，不依赖搜索索引。重新运行时复用现有对象，仅补缺失字段值，不重置手工修改的进度、优先级、标题、标签、里程碑、负责人或正文任务清单。仅管理标记内的关系/索引区块会更新。

遇到同名但无标识的 Project/Issue，会停止而不是重复创建或擅自接管。如果 Project 刚创建后中断、尚未来得及写标识，可核对已打印链接后显式恢复：

```powershell
python scripts/setup_carwith_project.py --apply --project-number 真实项目编号
```

已有自定义字段的类型/选项不兼容时，脚本停止并要求先核对，不覆盖选项清空已有卡片值。不会重开已关闭项目或取消归档项目项，也不会把已有子任务从其他父 Issue 强行移走。请勿同时运行多份脚本，或运行中并发编辑其管理区块。

变更计划 JSON 不会强行覆盖已经人工维护的 Issues；新增任务可以再次运行导入，原有任务的实质内容更新应通过 GitHub 正常维护流程完成。本工具是初始化/恢复器，不是全量声明式同步系统。

## 验证情况

已完成 Python 语法检查、离线预览和 **12 个本地测试**。测试覆盖计划校验、依赖顺序、重复标识防护、管理区块保留人工文字、完整模拟初始化、第二次运行不重复创建、不重置进行中状态、账号/管理权限检查。

这些是使用内存中的 REST/GraphQL 模拟器进行的本地测试，**没有使用真实授权对 GitHub Projects 做端到端初始化**；不应将模拟测试通过描述为线上 Project 已创建。测试文件随下载包提供。

## API 参考

- GitHub Projects API：<https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-api-to-manage-projects>
- Projects GraphQL 类型、字段、视图：<https://docs.github.com/en/graphql/reference/projects>
- Issue 父子关系：<https://docs.github.com/en/rest/issues/sub-issues>
- Issue 依赖：<https://docs.github.com/en/rest/issues/issue-dependencies>
- 仓库设置：<https://docs.github.com/en/rest/repos/repos#update-a-repository>

初始化采用 GitHub REST API `2026-03-10` 与当前文档中的 GraphQL 操作。不新增后台自动化、外部服务器或凭据存储。
