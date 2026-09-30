#!/usr/bin/env python3
"""Run only the identity-free debug decoder fixture on an explicit emulator."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time

PACKAGE = "com.shihab.diplay.hudtest"
ACTIVITY = "com.shilapi.xcertplay.bench.ReceiverBenchActivity"


def validate_report(report):
    if report.get("status") != "passed":
        raise ValueError("Fixture did not pass: " + str(report.get("status")))
    if (report.get("input_frames") != 300 or report.get("decoded_output_frames") != 300
            or report.get("output_eos") is not True):
        raise ValueError("Incomplete fixture frames or EOS")
    if report.get("is_carwith_session") is not False:
        raise ValueError("Not a local fixture report")
    if report.get("is_snapdragon_625_validation") is not False:
        raise ValueError("Invalid performance scope")
    return report


def collect(adb, serial, timeout=45):
    if not re.fullmatch(r"emulator-\d+", serial):
        raise ValueError("Only an explicit emulator serial is allowed; no phone installation or test")

    def command(*args, required=True):
        result = subprocess.run([adb, "-s", serial, *args], capture_output=True, text=True,
                                timeout=10, encoding="utf-8", errors="replace")
        if required and result.returncode:
            raise RuntimeError("ADB command failed; raw device output is not persisted")
        return result

    if command("shell", "getprop", "ro.kernel.qemu").stdout.strip() != "1":
        raise ValueError("Selected device is not a verified emulator")
    # Restart only this debug app on the explicit emulator, never another app or a phone.
    command("shell", "am", "force-stop", PACKAGE)
    # Delete only this app's old synthetic report so stale output cannot pass a new run.
    command("shell", "run-as", PACKAGE, "rm", "-f", "files/receiver-bench.json")
    command("shell", "am", "start", "-W", "-n", PACKAGE + "/" + ACTIVITY,
            "--ez", "run_fixture", "true")
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        result = command("shell", "run-as", PACKAGE, "cat", "files/receiver-bench.json", required=False)
        if result.returncode == 0:
            return validate_report(json.loads(result.stdout))
        time.sleep(.5)
    raise TimeoutError("No new fixture report within bounded wait")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", required=True)
    parser.add_argument("--profile", choices=["baseline", "low_resource"], required=True)
    parser.add_argument("--apk", type=Path, required=True, help="Already installed debug APK; hashed, not installed")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        parser.error("Refusing to overwrite an existing measurement")
    try:
        apk_hash = hashlib.sha256(args.apk.read_bytes()).hexdigest()
        result = collect(args.adb, args.serial)
        result["requested_profile"] = args.profile
        result["host_source_apk_sha256"] = apk_hash
        with args.output.open("x", encoding="utf-8") as file:
            json.dump(result, file, ensure_ascii=False, indent=2)
            file.write("\n")
    except (ValueError, RuntimeError, TimeoutError, OSError, subprocess.TimeoutExpired) as error:
        parser.exit(1, str(error) + "\n")


if __name__ == "__main__":
    main()
