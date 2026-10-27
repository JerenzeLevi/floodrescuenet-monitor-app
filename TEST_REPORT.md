> Historical/reference document. For the current 14 October 2026 source update, use `README_FULL_UPDATE.md`, `FEATURE_COVERAGE.md`, `TEST_REPORT_FULL.md` and `firmware/README.md`. Earlier percentage claims and “no firmware” statements below describe earlier versions.

> Update — 13 October 2026: See `README_80_UPDATE.md`, `MILESTONE_80.md`, `DEVICE_CONTRACT.md` and `TEST_REPORT_80.md` for the current app changes and validation. The older status statements below are historical. The implementation is Java/XML with Firebase, separate demo/device sessions, scoped in-app admin approval and full paginated history. Hardware completion is not claimed.

# Validation record — 24 September 2026

## Passed

| Check | Result | What it establishes |
|---|---|---|
| Android `assembleDebug` | PASS | Java compilation, resource linking, dependency metadata, dexing and debug APK packaging |
| Android `testDebugUnitTest` | 8 passed, 0 failed | Exact tier boundaries, missing/invalid values, schema/source validation, stale/future timestamps, command expiry, permission/offline guards, history ordering and trend calculation |
| `npm test` | 11 passed, 0 failed | Demo transitions, manual/automatic behavior, completed/failed acknowledgements, one-time handling, expiry, foreign-target rejection, manual repack and restart replay protection |
| Local Firebase emulator rules suite | 10 passed, 0 failed | Verified-owner access, cross-account and anonymous rejection, approval protection, validation, single pending command, immutable acknowledgement fields, expired commands and atomic history/state updates |
| XML / referenced view-ID check | PASS | Resource XML parses and all Java `R.id` references resolve to declared IDs |
| Feeder JavaScript syntax check | PASS | `demo-feed.mjs` parses successfully |

**Total automated behavior tests: 29 passed.** Tests run against local emulators or pure state logic, not your live Firebase project. Expected permission-denied messages in the rules test log are successful negative tests.

The build used JDK 17, Gradle 8.11.1, AGP 8.10.1, Android SDK 36 revision 2 and Build Tools 35.0.0. SDK/compiler tools were prepared in the build environment; those tools and caches are not part of the source archive. An existing unused TensorFlow Lite dependency was retained from your project; its native-library packaging warning did not prevent the APK build.

## Not yet verified

- Installation and visual/lifecycle behavior on your Android phone or a running Android emulator.
- Your Firebase project's deployed rules, approved test account, actual sign-in/sign-up/reset-email delivery and network behavior.
- A complete Android ↔ feeder demonstration against your development Firebase project.
- Android ↔ 3D website communication; the website adapter is still to be implemented.
- Physical ESP32 sensors/motors or any real-world rescue behavior.

Use the nine acceptance scenarios in START_HERE.md to record phone screenshots and observed results. Those checks are required before calling the milestone demonstrated; the automated count is not a grading percentage.

## Repeat the automated checks

From the project folder, with a JDK and Android SDK installed:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
node --test tools/demo-engine.test.mjs
npm ci
npm run test:rules
```

The Firebase command uses only the local `demo-flood-monitor` emulator project and does not deploy cloud rules. On a network that sets a global HTTP proxy, ensure localhost connections bypass it. The tests use `127.0.0.1:9000` and need no real Firebase login credentials.
