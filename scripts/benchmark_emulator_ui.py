#!/usr/bin/env python3
"""Measure DiPlay UI on an explicitly selected emulator; never projection FPS."""
import argparse
from datetime import datetime, timezone
import json
import math
from pathlib import Path
import re
import statistics
import subprocess
import time

PACKAGE = 'com.shihab.diplay.hudtest'
COMPONENT = PACKAGE + '/com.shilapi.xcertplay.DiPlayActivity'


def adb(serial, *args):
    if not re.fullmatch(r'emulator-[0-9]+', serial):
        raise ValueError('UI benchmark only permits emulator selectors, not phones')
    result = subprocess.run(['adb', '-s', serial, *args], capture_output=True,
                            text=True, encoding='utf-8', errors='replace', timeout=30)
    if result.returncode or 'Error:' in result.stdout or 'Error type' in result.stdout:
        raise RuntimeError('ADB benchmark command failed; inspect the selected emulator locally')
    return result.stdout


def launch_ms(output):
    found = re.search(r'^TotalTime:\s*(\d+)\s*$', output, re.MULTILINE)
    if not found or not re.search(r'^Status:\s*ok\s*$', output, re.MULTILINE):
        raise ValueError('No measured successful launch time')
    return int(found.group(1))


def gfx_summary(output):
    result = {}
    for label, pattern in {
        'rendered_ui_frames': r'Total frames rendered:\s*(\d+)',
        'janky_ui_frames': r'Janky frames:\s*(\d+)',
        'ui_frame_p50_ms': r'50th percentile:\s*(\d+)ms',
        'ui_frame_p90_ms': r'90th percentile:\s*(\d+)ms',
        'ui_frame_p95_ms': r'95th percentile:\s*(\d+)ms',
        'ui_frame_p99_ms': r'99th percentile:\s*(\d+)ms',
    }.items():
        match = re.search(pattern, output)
        result[label] = int(match.group(1)) if match else None
    frames, jank = result['rendered_ui_frames'], result['janky_ui_frames']
    result['janky_ui_percent'] = round(jank * 100 / frames, 2) if frames and jank is not None else None
    # UI render timing histograms must not be mislabeled video FPS or video drops.
    result['projection_fps'] = None
    result['projection_latency_ms'] = None
    return result


def process_ticks(output):
    # comm may contain spaces/parentheses; fields after its last ')' start at state.
    fields = output.rsplit(')', 1)[-1].split()
    if len(fields) < 13:
        raise ValueError('Incomplete process CPU counters')
    return int(fields[11]) + int(fields[12])


def resource_sample(serial, previous, ticks_per_second):
    memory = adb(serial, 'shell', 'dumpsys', 'meminfo', PACKAGE)
    pss = re.search(r'TOTAL PSS:\s*(\d+)', memory)
    rss = re.search(r'TOTAL RSS:\s*(\d+)', memory)
    cpu = adb(serial, 'shell', 'dumpsys', 'cpuinfo')
    line = next((line.strip() for line in cpu.splitlines() if '/' + PACKAGE + ':' in line), None)
    value = re.match(r'([0-9.]+)%', line or '')
    window = next((line.strip() for line in cpu.splitlines() if 'CPU usage from' in line), None)
    pid = adb(serial, 'shell', 'pidof', PACKAGE).strip()
    ticks = process_ticks(adb(serial, 'shell', 'cat', '/proc/' + pid + '/stat'))
    measured_at = time.monotonic()
    percent = None
    if previous and previous[0] == pid and ticks >= previous[1]:
        percent = round((ticks - previous[1]) / ticks_per_second /
                        (measured_at - previous[2]) * 100, 2)
    previous[:] = [pid, ticks, measured_at]
    return {'cpu_percent_single_core_interval': percent,
            'pss_kib': int(pss.group(1)) if pss else None,
            'rss_kib': int(rss.group(1)) if rss else None,
            'cpu_percent_reported': float(value.group(1)) if value else None,
            'cpu_window': window}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--profile', required=True, choices=('baseline', 'low_resource'))
    parser.add_argument('--host-affinity', choices=('not_set', '0x3'), default='not_set')
    parser.add_argument('--duration', type=int, default=60)
    parser.add_argument('--launches', type=int, default=5)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if not 30 <= args.duration <= 600 or not 3 <= args.launches <= 20:
        parser.error('duration must be 30..600 seconds and launches 3..20')
    if args.output.exists():
        parser.error('output already exists; choose a new local file')
    if adb(args.serial, 'shell', 'getprop', 'sys.boot_completed').strip() != '1':
        parser.error('selected emulator has not completed boot')
    times = []
    for _ in range(args.launches):
        adb(args.serial, 'shell', 'am', 'force-stop', PACKAGE)
        times.append(launch_ms(adb(args.serial, 'shell', 'am', 'start', '-W', '-n', COMPONENT, '--es', 'page', 'home')))
        time.sleep(1)
    adb(args.serial, 'shell', 'dumpsys', 'gfxinfo', PACKAGE, 'reset')
    resources, pid_changes, transitions = [], 0, 0
    previous_cpu = []
    ticks_per_second = int(adb(args.serial, 'shell', 'getconf', 'CLK_TCK').strip())
    first_pid = adb(args.serial, 'shell', 'pidof', PACKAGE).strip()
    started = time.monotonic()
    next_sample = 0
    while time.monotonic() - started < args.duration:
        elapsed = time.monotonic() - started
        page = 'settings' if transitions % 2 == 0 else 'home'
        adb(args.serial, 'shell', 'am', 'start', '-n', COMPONENT, '--es', 'page', page)
        # Scroll project UI only; do not press connect or grant permissions.
        adb(args.serial, 'shell', 'input', 'swipe', '1000', '600', '1000', '180', '300')
        transitions += 1
        if elapsed >= next_sample:
            sample = resource_sample(args.serial, previous_cpu, ticks_per_second)
            sample['elapsed_seconds'] = round(elapsed, 2)
            resources.append(sample)
            if adb(args.serial, 'shell', 'pidof', PACKAGE).strip() != first_pid:
                pid_changes += 1
            next_sample += 10
            print(f'{args.profile}: measured {elapsed:.0f}s, {transitions} UI transitions', flush=True)
        time.sleep(0.15)
    measured_duration = time.monotonic() - started
    summary = gfx_summary(adb(args.serial, 'shell', 'dumpsys', 'gfxinfo', PACKAGE, 'framestats'))
    cpuinfo = adb(args.serial, 'shell', 'cat', '/proc/cpuinfo')
    meminfo = adb(args.serial, 'shell', 'cat', '/proc/meminfo')
    total = re.search(r'^MemTotal:\s*(\d+)', meminfo, re.MULTILINE)
    report = {
        'schema_version': 1, 'profile': args.profile, 'host_affinity_mask': args.host_affinity,
        'captured_at': datetime.now(timezone.utc).isoformat(),
        'scope': 'debug UI only; no CarWith session or video fixture',
        'package': PACKAGE, 'android': adb(args.serial, 'shell', 'getprop', 'ro.build.version.release').strip(),
        'abi': adb(args.serial, 'shell', 'getprop', 'ro.product.cpu.abi').strip(),
        'guest_cpu_count': len(re.findall(r'^processor\s*:', cpuinfo, re.MULTILINE)),
        'guest_mem_total_kib': int(total.group(1)) if total else None,
        'display': adb(args.serial, 'shell', 'wm', 'size').strip(),
        'density': adb(args.serial, 'shell', 'wm', 'density').strip(),
        'cold_process_launch_ms': times, 'launch_median_ms': statistics.median(times),
        'launch_p95_ms': sorted(times)[math.ceil(len(times)*0.95)-1],
        'cold_launch_caveat': 'process force-stopped; OS/storage caches not cleared',
        'duration_seconds': round(measured_duration, 2), 'ui_transitions': transitions,
        'observed_pid_changes': pid_changes, 'gfx': summary, 'resources': resources,
        'cpu_caveat': 'proc stat utime+stime delta / CLK_TCK / elapsed seconds; 100% means one guest CPU; first interval null. dumpsys is an independent historical window.',
        'snapdragon_625_equivalent': False,
    }
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(report, stream, indent=2, ensure_ascii=False)
        stream.write('\n')
    print(json.dumps({'launch_median_ms': report['launch_median_ms'], 'gfx': summary}), flush=True)


if __name__ == '__main__':
    main()
