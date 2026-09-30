# Debug receiver media bench

This is independent test infrastructure for Project #1 / issue #11. It is not a CarLife receiver, a CarWith session, G0 evidence, CarPlay authentication, or Snapdragon 625 validation. It does not alter network settings or access a phone.

## Reproduce

Build the ordinary identity-free debug APK with `./gradlew :mobile:assembleDebug`. On an explicitly authorized emulator, install that APK and launch:

```powershell
adb -s emulator-5580 install -r mobile/build/outputs/apk/debug/mobile-debug.apk
adb -s emulator-5580 shell am force-stop com.shihab.diplay.hudtest
adb -s emulator-5580 shell am start -n com.shihab.diplay.hudtest/com.shilapi.xcertplay.bench.ReceiverBenchActivity --ez run_fixture true
# Wait for completion (normally seconds; decode loop has a 30-second bound).
adb -s emulator-5580 shell run-as com.shihab.diplay.hudtest cat files/receiver-bench.json
```

Do not silently select an attached physical phone. A completed report has `status=passed`, 300 input and output frames and `output_eos=true`. The report timestamp must belong to the current run; an old file is not evidence of a new run. Cancel or leave the activity to interrupt the worker and release extractor/codec. Retry after that worker finishes. No release activity or fixture is shipped. The exported debug entry requires the existing shell/system `android.permission.DUMP`; ordinary apps cannot invoke it without that permission. No permission grant is performed by this bench.

## Fixture provenance

The MP4 is an original synthetic `testsrc2` pattern generated with the existing host ffmpeg, not copied media. It contains no audio, personal information or protocol identity. It is licensed under GPL-3.0 for redistribution with this project. Recreate with:

```text
ffmpeg -f lavfi -i testsrc2=size=1280x720:rate=30 -t 10 -an -c:v libx264 -preset veryfast -crf 30 -pix_fmt yuv420p -profile:v baseline -g 30 -bf 0 -movflags +faststart receiver_fixture.mp4
ffprobe -v error -select_streams v:0 -count_frames -show_entries stream=codec_name,profile,width,height,r_frame_rate,nb_read_frames -show_entries format=duration,size -of json receiver_fixture.mp4
```

Encoded bytes can differ with ffmpeg/x264 versions. The committed fixture's SHA-256 and measured runs are recorded below. Resource is debug-only, 1280×720, AVC Constrained Baseline, 30 fps, 300 frames / 10 seconds, YUV420P. Decode is intentionally **unpaced**: `unpaced_output_frames_per_second` is local throughput including capability query/configure and drain, not playback FPS. The Surface preview is scaled by the activity layout, not a full-screen road display.

For bounded, stale-report-resistant collection after installing the debug APK:

```text
python scripts/run_receiver_fixture.py --serial emulator-5580 --profile baseline --apk mobile/build/outputs/apk/debug/mobile-debug.apk --output NEW-RESULT.json
```

The script refuses physical phone selectors and existing output files, restarts only this emulator debug app, removes only its old synthetic report, validates frame counts/EOS and waits at most 45 seconds plus bounded individual ADB calls. `--apk` hashes the host file, does not install it or independently prove installed bytes.

## What measurements mean

- Codec name, ABI, API, RAM and codec-declared 720p30 support are captured. Hardware/software declarations are capabilities reported by Android, not calibrated ARM/GPU measurements. Wi-Fi Direct/Bluetooth feature declarations are not discovery or connectivity tests.
- Input samples and codec output buffers are counted separately; EOS/frame mismatch or timeout fails the fixture. Surface `OnFrameRenderedListener` callbacks can be delayed/batched. Their count is not a reliable dropped-frame counter or compositor/display-present measurement.
- Queue-to-Surface p95 uses matched presentation timestamps and framework rendering timestamps. It covers only this local synthetic path, with no phone encoding, transport, input or glass-to-glass measurement. Missing timing samples are reported, not estimated.
- Startup/background load, host scheduling and x86 software codecs affect results. Existing resource profiles are approximate stress configurations, not Snapdragon 625 equivalents. The requested low-resource RAM may be clamped by the emulator and must be checked in the report. ARM hardware decoding, sustained real-stream CPU/PSS, video pacing, touch and audio require the eventual physical video path.

## Results

Measured 2026-09-30 on the existing isolated DiPlay_G0_API34 AVD, Emulator 37.1.11 / API34 Google Play x86_64 rev14 / WHPX, Intel i5-13600KF (14 cores / 20 logical CPUs), 64 GB host RAM, SwiftShader, 1280×720 override / 240 dpi, hidden window. The selected decoder was **c2.goldfish.h264.decoder**, declared hardware accelerated by the emulator; this is not evidence of ARM hardware decoding or an Adreno path. Both resource profiles ran the same already-installed source-only APK.

| Run / raw record | Requested vCPU / RAM | Elapsed seconds | Unpaced output frames/s | Input / output / callbacks |
| --- | --- | --- | --- | --- |
| [Baseline](performance/decoder-baseline.json) | 4 / 2048 MB | 3.679 | 81.54 | 300 / 300 / 300 |
| [Baseline after cancellation](performance/decoder-baseline-recovery.json) | 4 / 2048 MB | 3.518 | 85.29 | 300 / 300 / 300 |
| [Low resource](performance/decoder-low-resource.json) | 2 / 1024 MB | 6.468 | 46.38 | 300 / 300 / 300 |
| [Low resource repeat](performance/decoder-low-resource-repeat.json) | 2 / 1024 MB | 4.654 | 64.47 | 300 / 300 / 300 |

Each completed run drained EOS and passed. Actual low-resource online CPUs were `0-1`. **Both profiles were clamped to emulator RAM 2560 MB** (app reported total RAM 2,594,942,976 bytes, with guest overhead); the low profile did not provide a 1 GB memory test. There was no host affinity limit (`1048575 / 0xfffff`). Counts validate this fixture only. Throughput includes the capability query, configure and decode/drain; it does not include app launch or the subsequent 500ms callback grace period. Between-run differences can include background/boot load and scheduling; these four samples are not a calibrated chipset comparison.

All runs had **zero valid queue-to-Surface timestamp samples**, so no latency/p95 is published. The framework produced callbacks but this run did not produce usable matched monotonic timing samples; their cause has not been established. Stable playback FPS, display drops, video decode CPU/PSS under sustained load, touch, audio, and CarWith end-to-end performance were not measured. Historical UI-only CPU/memory/jank measurements remain in [EMULATOR_FINDINGS.md](EMULATOR_FINDINGS.md).

[Cancellation](performance/decoder-cancel.json) was actually tested by launching the fixture then sending HOME on this emulator: status `cancelled`, 28 queued / 24 output frames, no EOS. A subsequent full 300-frame run passed. Completed/cancelled report persistence and recovery were exercised; resource release also follows from the `finally` path, but no long-term leak study is claimed.

Fixture SHA-256: `f1f14c814fe3db62730f7ed89dc231cd6975c85e778919e964a37da5fee1c4e4` (1,682,535 bytes), generated with ffmpeg 9.0.1 / libx264. Tested debug APK SHA-256: `e3ead3797a3da66235f81db531057ca8d32a7a6a3c95ba06b1e5db62ec8c8171`. Only the test APK was installed on the emulator; no phone app or component was installed. At completion the owned emulator was stopped, with full CPU affinity verified before shutdown; no other AVD was modified.

Validation: `python -m unittest discover -s scripts/tests -v` (24 tests); `python scripts/check_public_tree.py`; `./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :mobile:lintDebug :mobile:assembleDebug --continue --stacktrace`. The first local full run hit an existing RemoteMfiAuthenticationClientTest 50ms loopback socket timeout (227 tests, one failure); the complete retry passed without changing authentication code. CI on the final commit is the independent build result. Both old/new G0 records remain valid Blocked (checker exit 2).
