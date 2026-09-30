import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location(
    'm0', Path(__file__).resolve().parents[1] / 'check_carwith_m0.py')
m0 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m0)


def complete():
    # Synthetic completeness data, never hardware evidence.
    result = {'schema_version': 1, 'decision': 'Go', 'reason': 'synthetic',
              'next_step': 'manual review', 'test_environment': 'bench'}
    for device, fields in m0.DEVICE_FIELDS.items():
        result[device] = dict.fromkeys(fields, 'synthetic')
    for field in ('carwith_entry', 'reference_receiver', 'reference_version',
                  'reference_source', 'authorization_review', 'license_review',
                  'authentication_conditions', 'independent_carlife_comparison'):
        result[field] = 'synthetic'
    result['runs'] = [dict(source='CarWith', continuous_video=True,
                          date='synthetic', steps='synthetic', evidence_reference='synthetic',
                          observed_limitations='synthetic') for _ in range(2)]
    return result


class GateCompletenessTests(unittest.TestCase):
    def test_template_does_not_unlock_gate(self):
        path = Path(__file__).resolve().parents[2] / 'docs/carwith/m0-record.json'
        import json
        record = json.loads(path.read_text(encoding='utf-8-sig'))
        self.assertEqual(record['decision'], 'Blocked')
        self.assertEqual(m0.check(record), [])
        record['decision'] = 'Go'
        self.assertTrue(m0.check(record))

    def test_complete_record_only_eligible_for_review(self):
        self.assertEqual(m0.check(complete()), [])

    def test_independent_carlife_is_not_carwith_evidence(self):
        record = complete()
        record['runs'][0]['source'] = 'CarLife App'
        self.assertTrue(m0.check(record))

    def test_one_run_is_not_repeatable(self):
        record = complete()
        record['runs'].pop()
        self.assertTrue(m0.check(record))

    def test_unverified_version_and_auth_are_rejected(self):
        record = complete()
        record['phone']['carwith_version'] = 'unknown'
        record['authorization_review'] = ''
        self.assertEqual(len(m0.check(record)), 2)

    def test_no_go_preserves_blocker_without_fake_devices(self):
        self.assertEqual(m0.check(dict(schema_version=1, decision='No-Go',
            reason='entry absent', next_step='confirm supported plugin')), [])

    def test_malformed_runs_fail_without_crashing(self):
        record = complete()
        record['runs'] = [None, []]
        self.assertEqual(len(m0.check(record)), 2)
        self.assertTrue(m0.check([]))


if __name__ == '__main__':
    unittest.main()
