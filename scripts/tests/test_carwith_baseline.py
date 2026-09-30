import importlib.util
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    'baseline', Path(__file__).resolve().parents[1] / 'collect_carwith_baseline.py')
baseline = importlib.util.module_from_spec(spec)
spec.loader.exec_module(baseline)


class BaselineTests(unittest.TestCase):
    def test_selects_one_device_and_discards_private_dump_fields(self):
        calls = []
        def reader(adb, args):
            calls.append(args)
            if args == ['devices']:
                return 'List of devices attached\nprivate-selector\tdevice\nother\tdevice\n'
            self.assertEqual(args[:3], ['-s', 'private-selector', 'shell'])
            if args[3] == 'getprop':
                self.assertIn(args[4], baseline.PROPERTIES.values())
                return 'synthetic\n'
            self.assertEqual(args[3:], ['dumpsys', 'package', 'org.example.carwith'])
            return '  versionName=1.2.3\n  versionCode=12 minSdk=28\n  serial=secret\n  token=secret\n'
        result = baseline.collect('adb', 'private-selector', 'phone', ['org.example.carwith'], reader)
        self.assertEqual(result['packages']['org.example.carwith']['version_name'], '1.2.3')
        self.assertNotIn('private-selector', str(result))
        self.assertNotIn('secret', str(result))
        self.assertEqual(len(calls), 8)
        self.assertEqual(result['verification'], 'metadata_only_G0_not_passed')

    def test_absent_offline_unauthorized_never_reads_shell(self):
        for status in ('offline', 'unauthorized', ''):
            calls = []
            def reader(adb, args):
                calls.append(args)
                return f'List of devices attached\nselected\t{status}\n'
            with self.assertRaises(baseline.CollectionError):
                baseline.collect('adb', 'selected', 'phone', [], reader)
            self.assertEqual(calls, [['devices']])

    def test_rejects_shell_package_and_selector_options_before_adb(self):
        def forbidden(*args):
            self.fail('must reject before ADB')
        with self.assertRaises(baseline.CollectionError):
            baseline.collect('adb', 'selected', 'phone', ['org.example;getprop'], forbidden)
        with self.assertRaises(baseline.CollectionError):
            baseline.collect('adb', '-d', 'phone', [], forbidden)

    def test_missing_app_does_not_fabricate_version(self):
        def reader(adb, args):
            if args == ['devices']:
                return 'selected\tdevice\n'
            return ''
        with self.assertRaises(baseline.CollectionError):
            baseline.collect('adb', 'selected', 'phone', ['org.example.missing'], reader)

    def test_adb_error_and_timeout_hide_raw_identifiers(self):
        with patch.object(subprocess, 'run', return_value=subprocess.CompletedProcess(
                ['adb'], 1, '', 'private-selector token=secret')):
            with self.assertRaises(baseline.CollectionError) as error:
                baseline.run_adb('adb', ['devices'])
            self.assertNotIn('secret', str(error.exception))
        with patch.object(subprocess, 'run', side_effect=subprocess.TimeoutExpired('adb', 15)):
            with self.assertRaises(baseline.CollectionError):
                baseline.run_adb('adb', ['devices'])


if __name__ == '__main__':
    unittest.main()
