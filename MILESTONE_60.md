> Historical/reference document. For the current 14 October 2026 source update, use `README_FULL_UPDATE.md`, `FEATURE_COVERAGE.md`, `TEST_REPORT_FULL.md` and `firmware/README.md`. Earlier percentage claims and “no firmware” statements below describe earlier versions.

> Update — 13 October 2026: See `README_80_UPDATE.md`, `MILESTONE_80.md`, `DEVICE_CONTRACT.md` and `TEST_REPORT_80.md` for the current app changes and validation. The older status statements below are historical. The implementation is Java/XML with Firebase, separate demo/device sessions, scoped in-app admin approval and full paginated history. Hardware completion is not claimed.

# The 60% target: scope and acceptance record

`function.md` combines **8 app screen groups and 9 hardware groups**. It does not define weights or an official 60% grading rubric. Completing app screens alone cannot honestly be counted as completing 60% of all 17 groups. Its opening “0 of 17” statement is also older than the real authentication code and Firebase configuration found in the uploaded project.

This delivery targets the **Mobile Dev app milestone**. It implements database-backed behavior and a demo command loop before the 3D website is connected. The table maps the original 41 app bullets, one-for-one, so your group can agree on what counts. The hardware section is not counted as finished.

**Status definitions:** Implemented = software logic exists in this source; Adapted = works against the explicit demo contract with a change from the old proposal; Deferred = not implemented. These are implementation statuses, not a claim that every phone/cloud acceptance test has passed. Device acceptance remains to be recorded using START_HERE.md. See TEST_REPORT.md for the automated checks actually run.

| # | Original app requirement | Source status | What to demonstrate / limitation |
|---|---|---|---|
| 1 | Splash identity | Implemented | Opens the existing branded screen |
| 2 | Splash connects to ESP32 | Deferred | Firebase demo session replaces a physical sensor connection |
| 3 | Sensor initialization feedback | Adapted | Loading/session feedback; protected screens display actual data availability |
| 4 | Email and password input | Implemented | Validated inputs; password whitespace preserved |
| 5 | Credential validation | Implemented | Firebase Auth and administrator approval gate |
| 6 | Forgot password | Implemented | Firebase password-reset email request; delivery needs account testing |
| 7 | Official notice and unauthorized-access logging | Deferred | Unsupported logging claim removed; no audit-log feature claimed |
| 8 | Login → Sign Up route | Implemented | Open registration and return |
| 9 | Collect profile fields | Implemented | Full name, office, optional role, email, password and confirmation |
| 10 | Required-field validation | Implemented | Empty/invalid/mismatched values cannot submit |
| 11 | Submit for admin verification | Implemented | Profile saved unapproved; administrator approves in Firebase Console |
| 12 | Sign Up → Login route | Implemented | Returns after submission; no approval bypass |
| 13 | Dashboard water reading and tier | Implemented | Firebase value with matching classification |
| 14 | Dashboard net status | Adapted | Live simulated Ready/Deploying/Deployed state |
| 15 | Dashboard mini reading and trend | Implemented | Actual latest sample series; empty until samples arrive |
| 16 | Last three history events | Implemented | Real events with their reported severity; no fabricated resolution status |
| 17 | Most recent self-check | Adapted | Reports the latest acknowledged demo diagnostic; not a physical self-test |
| 18 | Contacts quick dial | Implemented | Configured numbers open a reviewable dialer; no invented local numbers |
| 19 | Bottom navigation | Implemented | Home/Live/Alerts/Deploy routes |
| 20 | Live water-level gauge | Implemented | Custom arc driven by actual received values |
| 21 | Trend and live indicator | Implemented | Calculated trend and freshness-aware DEMO LIVE label |
| 22 | Sensor health tiles | Adapted | Renders optional metrics; feeder does not invent physical measurements |
| 23 | Physical deployment status/time | Adapted | Received simulated state and activity time |
| 24 | Diagnostic trigger | Adapted | Sends a request and waits for a demo response; no hardware diagnosis |
| 25 | Recent readings/full data logs | Adapted | Latest 60 readings shown; history link opens events, not a complete export |
| 26 | Threshold warning banner | Implemented | Appears for fresh Warning/Danger readings |
| 27 | Event filters | Implemented | All/Danger/Net Deployments/System |
| 28 | Event cards | Implemented | Actual title, severity, time and description |
| 29 | Event details | Implemented | Tap Details/card to view the received event |
| 30 | Link to Live Monitoring | Implemented | Navigates to current telemetry |
| 31 | End-of-list marker | Adapted | Explicitly says up to 100 loaded records; does not claim all history |
| 32 | Deployment vitals | Adapted | Renders supplied values; unavailable physical health stays unknown |
| 33 | Automatic/manual switch | Adapted | Acknowledged mode change in the demo engine |
| 34 | Location/duration/thrust configuration | Adapted | Location editable; five-second animation and simulation target replace thrust |
| 35 | Pre-deployment checklist | Adapted | Four functioning checkboxes describe the demo, not unverified hardware checks |
| 36 | Manual deployment button | Adapted | One expiring Firebase request with acknowledged outcome |
| 37 | Deployment preview and View All | Implemented | Latest three records and full loaded list |
| 38 | Today's deployment totals | Adapted | Accurate totals for latest loaded records, clearly labeled; not a Today filter |
| 39 | Deployment record detail | Implemented | Timestamp, location, initiator, demo unit, cycle and result |
| 40 | Inline failed-result diagnostics | Implemented | Displays the actual supplied error message |
| 41 | Refresh and 24-hour cloud archival | Adapted | Refresh works; no automatic archive/retention job |

To assess a 60% **app-only** threshold using these equally weighted bullets, your instructor would need to accept at least **25 of 41** requirements as working. Do not count an adapted requirement as an exact match without agreeing on the revised demo scope. Use the acceptance tests and capture phone screenshots plus database/feeder results as evidence; a source-code checklist alone is not a passing demonstration.

Additional reliability behavior implemented: stale/invalid/offline data disables commands, expired requests show “result not confirmed,” two phones cannot replace an active request, protected screens detach listeners when stopped, and Firebase rules prevent cross-account access and self-approval.

Still separate: physical ESP32 telemetry, actual battery/rain/alignment sensors, hardware self-tests, background/push alerts, shared barangay roles, in-app admin approval and production retention. The earlier automatic-retraction proposal is intentionally replaced by manual repacking in the current demo.

**Update (website adapter):** the 3D tabletop simulator at `Website/simulator/` (the `floodrescuenet-monitor-site.vercel.app/simulator/` site) can now act as the demo session's data producer instead of the laptop feeder, per `DATA_CONTRACT.md` §"Future website adapter". It runs the same `DemoEngine` state machine as `tools/demo-feed.mjs`, so it produces identical telemetry/events/readings/deployments and answers phone DEPLOY/SET_MODE/DIAGNOSTIC commands the same way. This still does not touch hardware/ESP32 status — it replaces the laptop keyboard with the 3D model's own slider and reset button as the source of the demo water reading. See `Website/simulator/firebase-adapter.js` for the connection panel and its caveats (a Firebase Web app key may be needed if the bundled Android key is platform-restricted).
