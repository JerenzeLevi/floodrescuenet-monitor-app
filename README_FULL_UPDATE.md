> Website-prototype integration update: the controlled source for this Mobile Dev demonstration is the website 3D model. Select Simulation in the app. Follow `../START_HERE.md` in the combined package. Physical ESP32 material below is optional and is not required for this demo.

# FloodRescueNet Monitor — full-system source update

14 October 2026. Start here; this guide supersedes the older 60% and 80% milestone notes.

This update adds the remaining ESP32, Android and archive implementation paths against `function1.md`. **It is not a claim that the physical system is 100% working.** Android/firmware build results and executable checks are in `TEST_REPORT_FULL.md`; unperformed phone, cloud and hardware tests are in `ACCEPTANCE_CHECKLIST.md`. `FEATURE_COVERAGE.md` maps every one of the 41 app bullets and nine hardware groups to source and dependencies.

The original inventory explicitly chose Firebase in its introduction while retaining older hotspot wording in two bullets. This project implements that Firebase-first decision: ESP32 and phone independently connect to the Internet; the phone does not automatically join an ESP32 hotspot. If direct hotspot operation is still required by your rubric, those two literal requirements remain unmet. No unqualified 100% score is assigned.

## Included changes

- Android: scoped account approval, separate position/access roles, local access-attempt audit, source selection, startup sensor-readiness feedback, device-backed vitals and diagnostics, duration/power inputs, full paginated histories, archive browsing and complete daily totals.
- ESP32: GPIO34 water sensing; 880/2000 thresholds; 16×2 LCD; tiered buzzer; 3-second automatic deployment; retraction after five continuous normal seconds; manual commands; expiring/deduplicated acknowledgements; TLS Firebase transport; optional battery/rain/alignment measurements; motor leave-and-return diagnostic with limit switches.
- Archive service: scheduled 15-minute checks move records aged at least 24 hours into a separate browsable archive. Copy/removal is transactional. Cloud deployment is still required.
- Permissions: approved viewers create requests; separately provisioned producers publish ESP32 telemetry and acknowledgements. Barangay administrators can approve only their own barangay; the initial superadmin is bootstrapped by the project owner.

The successfully built debug APK is included at `dist/FloodRescueNetMonitor-debug.apk`. It uses the uploaded Firebase Android configuration and is for development testing. Firmware is supplied as source so credentials and calibration can be set before flashing.

## Android and Firebase setup

1. Extract into a new folder and open `FloodRescueNetMonitor` in Android Studio. Use JDK 17, SDK 36, Build Tools 35.0.0 and the included Gradle 8.11.1 wrapper. Run `./gradlew assembleDebug testDebugUnitTest`.
2. Confirm `app/google-services.json` belongs to your project. Enable Firebase Email/Password Authentication and Realtime Database. This update retains the uploaded Android Firebase configuration.
3. Review `firebase/database.rules.json`, then deploy to your development project using `npx firebase deploy --only database --project YOUR_PROJECT_ID`. Merge branches first if the database hosts unrelated applications.
4. Register accounts through the app. In Console, set one existing account's `users/<uid>/verified=true` and `role=superadmin`. Use its Admin Panel to approve officials. Do not put privileged credentials on the ESP32.
5. For simulation, select Profile → Choose monitoring source → Simulation and use the existing `tools/start-demo.ps1` feeder. The simulator deliberately requires manual repacking/reset; automatic physical retraction is implemented in firmware.
6. For hardware, follow `firmware/README.md` and `DEVICE_CONTRACT.md`, then select ESP32 in Profile. Sensor calibration/readiness, valid fresh telemetry and connectivity are required before remote commands are enabled.
7. Configure actual local emergency contacts in the session's `contacts` records. The dialer uses the supplied numbers; no unverified local numbers are fabricated.

No project rules, functions, billing configuration, phone installation or hardware flashing were performed by this source update.

## Archive setup

From `functions`, run `npm ci`; return to the project root, authenticate Firebase CLI and deploy with `npx firebase deploy --only functions:archiveMonitorHistory --project YOUR_PROJECT_ID`. The scheduled Cloud Function needs a project supporting scheduled Functions and any required billing enabled by its owner. It uses Node 22 and region `asia-southeast1`.

Archive location: `<demoSessions|deviceSessions>/<uid>/archive/<readings|events|deployments>`. Open any full-history screen, then **Open cloud archive**. Nothing is deleted from the archive automatically. Records can remain in current history beyond 24 hours until the next successful scheduled run; large backlogs take multiple batches. `archiveStatus` reports the last run and moved count. The current implementation is for a small prototype: transactions read the entire session including archived data, so production-scale history requires a partitioned retention design.

## Boundaries that affect acceptance

The GPIO34 sensor's centimeters and thresholds require physical calibration; its ADC value alone cannot prove a connected/accurate sensor. The gauge range is 0–20 cm. Rain is calibrated wetness percentage, not measured mm/hour. Drive power is PWM duty, not measured mechanical thrust. Position is a timed estimate unless both endpoint switches are fitted and enabled. Battery, rain and alignment peripherals must be wired/enabled for their readouts; missing values remain unknown.

Firmware starts unarmed; the local shutdown circuit and arming button must be wired. ESP32 automatic control continues during Wi-Fi loss, but remote commands and reporting need Internet access. Offline event buffering is RAM-only and limited to 32 queued records; losses are exposed by `droppedRecords`. The device-local login audit retains 200 entries and is not a central tamper-proof audit. One producer is mapped to one viewer session; shared multi-viewer barangay devices are outside this access model.
