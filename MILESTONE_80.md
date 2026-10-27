> Historical/reference document. For the current 14 October 2026 source update, use `README_FULL_UPDATE.md`, `FEATURE_COVERAGE.md`, `TEST_REPORT_FULL.md` and `firmware/README.md`. Earlier percentage claims and “no firmware” statements below describe earlier versions.

# 80% app-side implementation accounting

**34 / 41 = 82.9% app-side code coverage**, with every original Section A bullet weighted equally. This number is a proposed Mobile Dev assessment basis, not an official rubric, a phone acceptance pass, or the completion percentage of all 17 app/hardware groups. "App code" means the Android/data-contract logic is implemented; it does not mean every external dependency or physical behavior has been verified.

The original review used the upload’s older 24/15/2 checklist. This report replaces that classification after code changes. It makes the app boundary explicit: input/rendering and request/acknowledgement are assessed on the phone side, while the physical controller remains an external dependency. If the teacher requires each bullet to work end-to-end on hardware, **82.9% cannot yet be certified**. Seven exclusions below are not counted, even where partial UI/transport exists. See the tests and demonstration evidence separately.

| # | Original requirement | Classification | Evidence / remaining dependency |
|---|---|---|---|
| 1 | Splash identity | App code | Existing branded splash. |
| 2 | Connect to ESP32 over Wi-Fi | Excluded | Uses Firebase; no direct ESP32 hotspot connection or controller firmware supplied. |
| 3 | Sensor initialization feedback | Excluded | Actual Firebase connection feedback exists; physical sensor initialization is not implemented. |
| 4 | Email/password input | App code | Validated Firebase sign-in form. |
| 5 | Credential validation | App code | Firebase Auth plus verified-account gate; phone/cloud sign-in test pending. |
| 6 | Password recovery | App code | Firebase reset-email request and reset-link screen; actual email delivery pending. |
| 7 | Official notice and access logging | App code | Local bounded audit; does not claim central or tamper-proof logging. |
| 8 | Login to registration navigation | App code | Existing route. |
| 9 | Collect registration fields | App code | Full name, barangay, separate position, email and both passwords. |
| 10 | Required-field validation | App code | Existing validation; profile rules enforce stored fields. |
| 11 | Administrator verification | App code | Fixed scoped directory read and approval writes; initial superadmin provisioned in Console. |
| 12 | Registration to login navigation | App code | Existing route. |
| 13 | Water reading and tier | App code | Validates source and values, shows received cm and threshold classification; real readings need a producer. |
| 14 | Live net status | App code | Consumes simulation or provisioned ESP32 status; no inference of physical motion from a command write. |
| 15 | Mini reading and sparkline | App code | Latest Firebase readings drive the chart. |
| 16 | Last three history events/status chips | App code | Preserves reported resolution (Resolved/Passed/Pending); legacy data falls back to severity. |
| 17 | Latest physical self-check | Excluded | Latest diagnostic result is retained and displayed, but actual hardware self-check is not supplied. |
| 18 | Emergency-contact quick dial | App code | Configured numbers open the dialer; local numbers must be supplied by the team. |
| 19 | Bottom navigation | App code | Existing routes. |
| 20 | Real-time water gauge | App code | Custom gauge updates from received validated telemetry. |
| 21 | Trend and live tag | App code | Freshness-aware source labels and trend calculation. |
| 22 | Physical sensor-health tiles | Excluded | Renders optional supplied fields; no battery/rain sensors or physical measurements added. |
| 23 | Deployment state/activity time | App code | Receives ESP32 or demo state and lastActivityAt; physical provenance enforced by device-session rules. |
| 24 | Physical diagnostic trigger | Excluded | Request/ack transport works, but no physical sensor/motor diagnostic implementation is supplied. |
| 25 | Recent activity and full reading logs | App code | Recent feed plus paginated complete stored readings. |
| 26 | Threshold warning banner | App code | Fresh Warning/Danger readings control banner. |
| 27 | Event category filters | App code | Existing recent filters; full history also supports filters. |
| 28 | Detailed event cards | App code | Actual received fields, severity and timestamps. |
| 29 | Event detail drill-down | App code | Existing detail dialog. |
| 30 | Link to live monitoring | App code | Existing route. |
| 31 | End-of-history marker | App code | Full-history browser marks end only after exhaustion; recent feed links to it. |
| 32 | Physical system vitals | Excluded | Optional metrics are rendered but actual battery/latency/temperature/alignment measurements need hardware. |
| 33 | Automatic/manual toggle | App code | Source-targeted request; visible mode comes from source acknowledgement/telemetry. Physical execution pending controller. |
| 34 | Location/duration/thrust inputs | App code | Validated and transmitted options; duration honored by feeder and commanded power reported. Actual motor PWM/thrust requires controller implementation. |
| 35 | Pre-deployment checklist | App code | Device checklist matches original four operator confirmations; demo uses explicit simulation confirmations. |
| 36 | Manual deploy button | App code | Transaction-protected expiring source-targeted request; trusted device acknowledgement supported. Physical execution pending controller. |
| 37 | Deployment preview/View All | App code | Recent preview, detailed screen and full paginated stored history. |
| 38 | Today deployment counters | App code | Asia/Manila day query without last-100 truncation; midnight rollover. |
| 39 | Detailed deployment entries | App code | Preserves actual unit ID, timestamp, location, initiator, cycle/result. |
| 40 | Failed-deployment diagnostics | App code | Displays reported error instead of inventing success. |
| 41 | Refresh and 24-hour archival | Excluded | Refresh and full cloud-stored history work; no automatic archival/retention job. |

## Section B — hardware is not counted

The nine ESP32 requirements (sensing, LCD, buzzer, automatic deployment, retraction, state tracking, wireless reporting, manual override and hardware diagnosis) have **no new physical completion claim** in this delivery. The app now has a separated, permission-tested device transport contract, but the ZIP contains no newly implemented ESP32 firmware. Firmware must implement that contract and be tested on the prototype.

## What prevents a verified 80% claim today

- The team must perform phone/cloud acceptance checks using the updated app, deployed rules and approved accounts.
- Device-backed functions need provisioned producer credentials, matching firmware and observed real readings/results.
- Instructor acceptance of the app-only implementation boundary and equal weighting is required for this percentage to be used as a grade.

Do not relabel the simulation as a physical sensor or count an unrun acceptance test as passed.
