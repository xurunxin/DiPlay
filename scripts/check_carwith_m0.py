#!/usr/bin/env python3
"""Check M0 record completeness; never certify hardware or authorization."""
import argparse
import json
from pathlib import Path

DEVICE_FIELDS = {
    'phone': ('model', 'region_rom', 'hyperos', 'android', 'carwith_version',
              'plugin_version', 'installation_source'),
    'head_unit': ('model', 'firmware', 'android', 'display', 'network_role'),
}


def filled(value):
    return isinstance(value, str) and value.strip().lower() not in (
        '', 'unknown', 'unverified', 'todo', 'pending')


def check(record):
    """Return missing requirements. A complete record still needs human review."""
    errors = []
    if not isinstance(record, dict):
        return ['record must be an object']
    if type(record.get('schema_version')) is not int or record['schema_version'] != 1:
        errors.append('schema_version must be 1')
    if record.get('decision') not in ('Go', 'No-Go', 'Blocked'):
        errors.append('decision must be Go, No-Go or Blocked')
    for name in ('reason', 'next_step'):
        if not filled(record.get(name)):
            errors.append(name + ' is required')
    if record.get('decision') != 'Go':
        return errors
    for device, fields in DEVICE_FIELDS.items():
        values = record.get(device)
        if not isinstance(values, dict):
            errors.append(device + ' must be an object')
            continue
        for field in fields:
            if not filled(values.get(field)):
                errors.append(device + '.' + field + ' is required')
    for field in ('carwith_entry', 'reference_receiver', 'reference_version',
                  'reference_source', 'authorization_review', 'license_review',
                  'authentication_conditions', 'independent_carlife_comparison'):
        if not filled(record.get(field)):
            errors.append(field + ' is required')
    if record.get('test_environment') not in ('parked', 'bench'):
        errors.append('test_environment must be parked or bench')
    runs = record.get('runs')
    if not isinstance(runs, list) or len(runs) < 2:
        errors.append('at least two repeatable CarWith runs are required')
    else:
        for index, run in enumerate(runs):
            if not isinstance(run, dict):
                errors.append(f'runs[{index}] must be an object')
                continue
            if run.get('source') != 'CarWith' or run.get('continuous_video') is not True:
                errors.append(f'runs[{index}] requires actual CarWith continuous video')
            for field in ('date', 'steps', 'evidence_reference', 'observed_limitations'):
                if not filled(run.get(field)):
                    errors.append(f'runs[{index}].{field} is required')
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('record', type=Path)
    args = parser.parse_args()
    try:
        record = json.loads(args.record.read_text(encoding='utf-8-sig'))
    except (OSError, ValueError) as error:
        parser.exit(1, f'Cannot read record: {error}\n')
    errors = check(record)
    if errors:
        print('\n'.join(errors))
        return 1
    if record['decision'] != 'Go':
        print('G0 remains blocked: ' + record['reason'])
        return 2
    print('Record complete for manual G0 review. Hardware, evidence authenticity,')
    print('authorization and license approval are NOT certified by this tool.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
