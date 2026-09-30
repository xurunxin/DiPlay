#!/usr/bin/env python3
"""Collect a local Android version baseline from one explicitly selected device."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import subprocess

PROPERTIES = {
    'manufacturer': 'ro.product.manufacturer',
    'model': 'ro.product.model',
    'android': 'ro.build.version.release',
    'sdk': 'ro.build.version.sdk',
    'build_id': 'ro.build.id',
    'security_patch': 'ro.build.version.security_patch',
}
PACKAGE = re.compile(r'[A-Za-z][A-Za-z0-9_]*(?:[.][A-Za-z][A-Za-z0-9_]*)+')


class CollectionError(Exception):
    pass


def run_adb(adb, args):
    try:
        result = subprocess.run([adb, *args], capture_output=True, text=True,
                                encoding='utf-8', errors='replace', timeout=15)
    except (OSError, subprocess.TimeoutExpired):
        raise CollectionError('ADB unavailable or timed out; no baseline produced.') from None
    if result.returncode:
        # Never expose raw ADB output, device identifiers or package dumps.
        raise CollectionError('ADB read failed; check device access locally.')
    return result.stdout


def collect(adb, serial, role, packages, reader=run_adb):
    if not serial or serial.startswith('-') or any(c.isspace() for c in serial):
        raise CollectionError('An explicit valid device selector is required.')
    if role not in ('phone', 'head_unit'):
        raise CollectionError('Unknown device role.')
    if any(not PACKAGE.fullmatch(package) for package in packages):
        raise CollectionError('Invalid package name; shell expressions are not accepted.')
    devices = {}
    for line in reader(adb, ['devices']).splitlines():
        parts = line.split()
        if len(parts) == 2 and parts[1] in ('device', 'offline', 'unauthorized'):
            devices[parts[0]] = parts[1]
    if devices.get(serial) != 'device':
        raise CollectionError('Selected device is absent, offline or unauthorized; no baseline produced.')
    prefix = ['-s', serial, 'shell']
    baseline = {'schema_version': 1, 'device_role': role,
                'captured_at': datetime.now(timezone.utc).isoformat(),
                'verification': 'metadata_only_G0_not_passed', 'build': {}, 'packages': {}}
    for field, prop in PROPERTIES.items():
        baseline['build'][field] = reader(adb, prefix + ['getprop', prop]).strip() or None
    for package in packages:
        output = reader(adb, prefix + ['dumpsys', 'package', package])
        name = re.search(r'^\s*versionName=(\S+)\s*$', output, re.MULTILINE)
        code = re.search(r'^\s*versionCode=(\d+)\b', output, re.MULTILINE)
        if not name or not code:
            raise CollectionError('Requested package version is missing; verify the package locally.')
        baseline['packages'][package] = {'version_name': name.group(1), 'version_code': code.group(1)}
    return baseline


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--serial', required=True, help='explicit authorized device selector; not saved')
    parser.add_argument('--role', required=True, choices=('phone', 'head_unit'))
    parser.add_argument('--package', action='append', default=[], help='exact app package; repeat as needed')
    parser.add_argument('--output', type=Path, required=True, help='new local JSON file; never overwritten')
    args = parser.parse_args()
    try:
        baseline = collect(args.adb, args.serial, args.role, args.package)
        with args.output.open('x', encoding='utf-8') as stream:
            json.dump(baseline, stream, ensure_ascii=False, indent=2)
            stream.write('\n')
    except (CollectionError, OSError) as error:
        # OSError file paths may include personal details, so do not print them.
        message = str(error) if isinstance(error, CollectionError) else 'Output unavailable or already exists; no file overwritten.'
        parser.exit(2, message + '\n')
    print('Local metadata baseline saved. Review before sharing; G0 remains unverified.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
