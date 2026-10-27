# Validation report — 14 October 2026

This report records checks actually run. None is a physical-prototype or deployed-cloud acceptance pass.

| Check | Result |
|---|---|
| ESP32 board compilation | PASS: Arduino ESP32 3.0.7, ESP32 Dev Module, ArduinoJson 7.4.2, placeholder credentials. Final sketch: 1,128,605 bytes flash (86%), 49,492 bytes globals (15%). |
| C++ controller behavior | PASS: 14 scenarios with g++ C++17, `-Wall -Wextra -Werror`. Covers thresholds, initial unarmed state, duplicate prevention, deploy/retract timing, interrupted normal delay, manual mode, shutdown, missing/end feedback, diagnostic success/failure/pause interlocks and millis rollover. |
| Java presentation/command logic | PASS: 28 assertions on source separation, valid/fresh/stale data, physical readiness, states, duration/power bounds, Asia/Manila dates, actual unit/error display and expiry. |
| Demo source tests | PASS: 14 Node tests including acknowledgements, timing, restart/expiry handling, duration/power and retained diagnostic. |
| Archive transformation | PASS: four Node tests covering 24-hour boundary, idempotent retries/bounded backlog, conflicting IDs and invalid timestamps. |
| Firebase Realtime Database rules emulator | PASS: 18 tests against the actual rules, including admin scope, approval, viewer/producer separation, immutable commands, tied-time pagination, >100 daily records, archive protection and device startup/diagnostic telemetry. Expected permission-denied cases passed. |
| Cloud Functions module | PASS: dependencies installed, syntax checked and scheduled export imported locally. The schedule was not deployed or invoked in a real cloud project. |
| Android XML | PASS: all 105 source XML files parse. |
| Android build and JUnit | PASS: clean `assembleDebug testDebugUnitTest`, 41 tasks executed; one JUnit case, 28 assertions, zero failures/errors. Debug APK is included in `dist/`. |

Local Node tests ran on Node 24.19.0. The deployed Functions runtime is configured for Node 22. Android uses JDK 17, SDK 36 and Build Tools 35.0.0.

## Reproduce

```sh
npm ci
npm test
npm run test:rules
g++ -std=c++17 -Wall -Wextra -Werror firmware/tests/controller_test.cpp -o controller-test
./controller-test
./gradlew assembleDebug testDebugUnitTest
```

`npm test` runs 14 demo tests and four archive tests. `npm run test:rules` requires Java and starts the Database Emulator; it does not test against or modify production. ESP32 compilation/setup commands are in `firmware/README.md`. Gradle runs the 28 Java checks through one JUnit test method; do not count that as 28 separate JUnit test cases.

## Not performed

- Android installation, instrumented UI/emulator runs, screenshots or actual phone navigation.
- Real Firebase login/reset email, provisioning, cloud rule deployment or scheduled archive execution.
- ESP32 flashing, TLS/Firebase operation on a board, sensor calibration, physical LCD/buzzer output, motor actuation, battery/rain/alignment measurements or loaded-net tests.
- Operational reliability, prolonged offline operation, security certification or grading approval.

An earlier environment attempt produced a truncated linked-resource archive. The final isolated clean build generated a valid resource archive and successfully packaged the APK. The delivered APK ZIP integrity check passed.

A successful build does not resolve the unchecked acceptance items in `ACCEPTANCE_CHECKLIST.md`. Full system completion remains unverified.

Debug APK SHA-256: `fbd4e78983b9622a69846d2ff08f53f161cdb2f9d8b5d10eeb2d0972c57f459b`.
