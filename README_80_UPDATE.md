> Historical/reference document. For the current 14 October 2026 source update, use `README_FULL_UPDATE.md`, `FEATURE_COVERAGE.md`, `TEST_REPORT_FULL.md` and `firmware/README.md`. Earlier percentage claims and “no firmware” statements below describe earlier versions.

# FloodRescueNet Monitor — 80% app-side upgrade

Start here. This update targets **34 of 41 mobile-app bullets (82.9%) at the app-code level**, using the supplied `function1.md` and equal weighting. This is a reviewable implementation target, **not a certified grade or 82.9% completion of the full app-plus-hardware system**. Requirements involving the ESP32 still need a controller that implements `DEVICE_CONTRACT.md` and a physical test. No ESP32 firmware is supplied in this ZIP.

Read `MILESTONE_80.md` for the complete requirement-by-requirement accounting. Seven requirements are deliberately excluded from the count; a screen alone is not evidence of a working sensor or motor. The original `function1.md` is included unchanged.

## What changed

- Fixed administrator account listing with barangay-scoped Firebase queries and matching rules. An administrator can approve a pending account with a different email; regular users cannot approve themselves or promote their own roles. Optional job position is now saved separately from access role.
- Added a device-local access log, viewable in Profile → Security Settings → Access activity. It records sign-in outcomes without passwords, tokens, or email addresses. It is limited to 200 entries and is not a central/tamper-proof security audit.
- Added Profile → Choose monitoring source. Simulation and ESP32 use separate database branches and labels. Only the provisioned device account may publish physical telemetry or acknowledge physical commands.
- Added deployment duration (1–10 seconds) and commanded drive power (10–100%). The included feeder honors duration and reports commanded power. Power percentage is a control setting, not a measured physical thrust. An older website simulator may ignore these new fields; use the included updated feeder for this milestone.
- Added source-specific pre-deployment checklists. Device checklist boxes are operator confirmations of stabilizers, calibration, communications and shutdown; they do not claim automatic measurements.
- Added full paginated reading/event/deployment history, with stable cursors when timestamps are equal. An end-of-history marker is shown only after all pages have been loaded.
- Added real Today totals in Asia/Manila, independent of the recent-100 list. Unknown/loading totals display a dash, and the query rolls over at midnight.
- Added a separately retained latest diagnostic result and source-aware net status/activity timestamps. Physical diagnostic execution still depends on controller firmware.
- Startup now waits for a real Firebase connection or a bounded timeout. It does not claim it initialized the physical sensors.
- Aligned Gradle with API 36 support and removed the uploaded computer-specific SDK path/JVM download configuration. Original Firebase Android configuration is retained.

## Run the app and demonstration

1. Extract the ZIP into a new folder. Open `FloodRescueNetMonitor` in Android Studio. Use JDK 17, Android SDK 36 and Build Tools 35.0.0. Let Gradle sync, then run `app`.
2. Keep `app/google-services.json` for your Firebase project. Enable Email/Password authentication and Realtime Database in that project if needed.
3. Review and deploy `firebase/database.rules.json` to your **development** Firebase database. If other projects share this database, merge the relevant branches instead of overwriting unrelated rules. No cloud rules were deployed by this update.
4. Create accounts in the app. In Firebase Console, bootstrap one administrator by setting its existing `users/<uid>/verified` to `true` and `role` to `superadmin`. This initial privileged action must be performed by the project owner. Never let a registration form assign an admin role.
5. Sign in as that administrator. The Admin Panel can approve pending accounts and promote verified users to barangay administrators. Local administrators see only their own barangay. Reject removes the profile, not the Firebase Authentication account; the owner must manage any later re-registration in Firebase Console.
6. Sign in on the phone with an approved user. In Profile → Choose monitoring source, choose **Simulation**.
7. Run `tools/start-demo.ps1` on the computer with the same approved account (Node.js 22). No npm install is needed for the feeder. Keep it open. Enter `water 11`, `water 17`, `reset`, or `status` as in the original `START_HERE.md`.
8. Test a manual request with 2 seconds / 40% power. Complete the checklist, confirm the request, and wait for the source acknowledgement. The feeder reports the configured power while deploying and logs the selected duration. The app does not equate a successful database write with a completed deployment.

For real device sessions, follow `DEVICE_CONTRACT.md`. Selecting ESP32 alone does not connect unmodified firmware. The app remains empty/stale and disables commands until valid source data arrives.

## Where to find the added features

| Feature | App location |
|---|---|
| Data-source selection | Profile → Choose monitoring source |
| Access-attempt log | Profile → Security Settings → Access activity |
| Full water-level history | Live Monitoring → Full logs |
| Full event history with filters | Alerts → Browse all event history |
| Full deployment history | Recent Deployments → Browse all deployment history |
| Today counters | Recent Deployments → Total / Success / Failed |
| Duration and drive-power inputs | Manual Deploy → Configuration |
| Account approval | Admin account login or Profile → Admin Panel |

## Acceptance evidence still required

Record phone screenshots and observed Firebase updates for sign-in, approval, live telemetry, filters, history paging, day boundaries, settings, acknowledgements, and stale/offline blocking. Record a separate physical test for actual ESP32 telemetry and motor motion. Do not present simulator results as physical tests.

`TEST_REPORT_80.md` lists checks actually run for this update. Older `MILESTONE_60.md`, `TEST_REPORT.md`, `START_HERE.md`, and `DATA_CONTRACT.md` are retained for history; this guide and the 80% report take precedence where they differ.
