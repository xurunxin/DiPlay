import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location(
    'bench', Path(__file__).resolve().parents[1] / 'benchmark_emulator_ui.py')
bench = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bench)


class MeasurementTests(unittest.TestCase):
    def test_missing_measurements_are_not_zero(self):
        result = bench.gfx_summary('No process found')
        self.assertIsNone(result['rendered_ui_frames'])
        self.assertIsNone(result['janky_ui_percent'])
        self.assertIsNone(result['projection_fps'])

    def test_ui_jank_is_not_video_drops_or_fps(self):
        result = bench.gfx_summary('Total frames rendered: 100\nJanky frames: 12 (12%)\nJanky frames (legacy): 80 (80%)\n95th percentile: 35ms')
        self.assertEqual(result['janky_ui_frames'], 12)
        self.assertEqual(result['janky_ui_percent'], 12)
        self.assertEqual(result['ui_frame_p95_ms'], 35)
        self.assertIsNone(result['projection_fps'])
        self.assertIsNone(result['projection_latency_ms'])

    def test_failed_or_unmeasured_launch_is_rejected(self):
        with self.assertRaises(ValueError):
            bench.launch_ms('Error type 3\nTotalTime: 0')
        with self.assertRaises(ValueError):
            bench.launch_ms('Status: ok')
        self.assertEqual(bench.launch_ms('Status: ok\nTotalTime: 345\nWaitTime: 400'), 345)

    def test_process_cpu_stat_handles_complex_name(self):
        fields = ['S'] + ['0'] * 10 + ['123', '45']
        self.assertEqual(bench.process_ticks('123 (app (UI worker)) ' + ' '.join(fields)), 168)
        with self.assertRaises(ValueError):
            bench.process_ticks('123 (app) S')

    def test_phone_is_rejected_before_adb(self):
        with self.assertRaises(ValueError):
            bench.adb('physical-phone', 'shell', 'am', 'force-stop', bench.PACKAGE)


if __name__ == '__main__':
    unittest.main()
