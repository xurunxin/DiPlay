# Test checklist

Use the [installation guide](INSTALL.md). With the car parked, verify wired and wireless connection, picture, touch and music. Test disconnect/reconnect, then settings Apply/Cancel. Save a diagnostic report after reproducing an issue.

For channel memory, connect until authenticated CarPlay renders, disconnect and reconnect without changing the car's Wi-Fi association. Look for `remembered saved` followed by `remembered first`. Report absent events; creating a hotspot alone is insufficient.

Include head-unit model, DiLink/Android, iPhone/iOS, wired/wireless, app version and exact steps. Do not post credentials or unreviewed personal information. See [compatibility](COMPATIBILITY.md) for remaining limitations.

CarWith roadmap work starts with the [M0 access review and bench baseline](carwith/M0_REVIEW.md). Its current G0 decision is Blocked; CarWith support is not implemented or hardware-verified.

Reproducible emulator UI/CPU measurements and current CarWith, CarbitLink, and ICCOA blockers are recorded in [emulator findings](carwith/EMULATOR_FINDINGS.md). These measurements do not validate projection or Snapdragon 625 equivalence.

CarLife priority has been restored based on the new phone's official component UI; see [compatibility evidence](carwith/CARLIFE_COMPATIBILITY.md). The independent [debug receiver decoder bench](carwith/RECEIVER_BENCH.md) tests synthetic local media only and cannot unlock G0.
