import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("fixture", Path(__file__).parents[1] / "run_receiver_fixture.py")
fixture = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fixture)


class ReceiverFixtureTest(unittest.TestCase):
    def report(self):
        return dict(status="passed", input_frames=300, decoded_output_frames=300, output_eos=True,
                    is_carwith_session=False, is_snapdragon_625_validation=False)

    def test_complete_local_fixture(self):
        self.assertEqual(fixture.validate_report(self.report())["decoded_output_frames"], 300)

    def test_missing_frame_or_eos_cannot_pass(self):
        for key, value in [("input_frames", 299), ("decoded_output_frames", 299), ("output_eos", False)]:
            report = self.report()
            report[key] = value
            with self.subTest(key=key), self.assertRaises(ValueError):
                fixture.validate_report(report)

    def test_cancelled_and_failed_are_not_success(self):
        for status in ["cancelled", "failed", None]:
            report = self.report()
            report["status"] = status
            with self.subTest(status=status), self.assertRaises(ValueError):
                fixture.validate_report(report)

    def test_projection_claim_is_rejected(self):
        report = self.report()
        report["is_carwith_session"] = True
        with self.assertRaises(ValueError):
            fixture.validate_report(report)

    def test_physical_phone_is_rejected_before_adb(self):
        with patch.object(fixture.subprocess, "run") as adb, self.assertRaises(ValueError):
            fixture.collect("adb", "4d8453fe")
        adb.assert_not_called()

    def test_missing_report_has_bounded_timeout(self):
        with patch.object(fixture.subprocess, "run", return_value=type("Result", (), {"returncode": 0, "stdout": "1"})()), patch.object(fixture.time, "monotonic", side_effect=[1, 3]):
            with self.assertRaises(TimeoutError):
                fixture.collect("adb", "emulator-5580", timeout=1)

    def test_emulator_named_device_requires_qemu_before_mutation(self):
        result = type("Result", (), {"returncode": 0, "stdout": "0"})()
        with patch.object(fixture.subprocess, "run", return_value=result) as adb:
            with self.assertRaises(ValueError):
                fixture.collect("adb", "emulator-5580")
        self.assertEqual(adb.call_count, 1)
        self.assertIn("getprop", adb.call_args.args[0])
