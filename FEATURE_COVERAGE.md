> Website-prototype integration update: the controlled source for this Mobile Dev demonstration is the website 3D model. Select Simulation in the app. Follow `../START_HERE.md` in the combined package. Physical ESP32 material below is optional and is not required for this demo.

# Full-system requirement coverage

Source map against the supplied `function1.md`: 41 mobile bullets and nine hardware groups. “Implemented” describes a code path, not a passed physical acceptance test. The original document is included unchanged. No unqualified 100% completion or grading claim is made.

Two legacy hotspot requirements are adapted to the explicit Firebase-first architecture in the inventory introduction. Optional health peripherals, cloud deployment, calibration and physical testing remain prerequisites. See `TEST_REPORT_FULL.md` for measured test results and `ACCEPTANCE_CHECKLIST.md` for remaining acceptance work.

## A. Android

| # | Requirement | Status | Evidence / dependency |
|---|---|---|---|
| 1 | Splash identity | Implemented; phone acceptance pending | Existing branded splash. |
| 2 | Connect to ESP32 over Wi-Fi | Adapted | SplashActivity + ESP32 network task: automatic Firebase reconnection. Direct hotspot auto-join is not implemented; follows the inventory’s Firebase-first decision. |
| 3 | Sensor initialization feedback | Implemented; acceptance pending | SplashActivity checks authenticated source telemetry, freshness and sensorReady with bounded feedback; firmware samples GPIO34. Physical calibration and startup timing need observation. |
| 4 | Email/password input | Implemented; phone acceptance pending | Validated Firebase sign-in form. |
| 5 | Credential validation | Implemented; phone acceptance pending | Firebase Auth plus verified-account gate; phone/cloud sign-in test pending. |
| 6 | Password recovery | Implemented; phone acceptance pending | Firebase reset-email request and reset-link screen; actual email delivery pending. |
| 7 | Official notice and access logging | Implemented; phone acceptance pending | Local bounded audit; does not claim central or tamper-proof logging. |
| 8 | Login to registration navigation | Implemented; phone acceptance pending | Existing route. |
| 9 | Collect registration fields | Implemented; phone acceptance pending | Full name, barangay, separate position, email and both passwords. |
| 10 | Required-field validation | Implemented; phone acceptance pending | Existing validation; profile rules enforce stored fields. |
| 11 | Administrator verification | Implemented; phone acceptance pending | Fixed scoped directory read and approval writes; initial superadmin provisioned in Console. |
| 12 | Registration to login navigation | Implemented; phone acceptance pending | Existing route. |
| 13 | Water reading and tier | Implemented; sensor calibration pending | MonitorState validates fresh ESP32 cm/tier; waterCm converts configured ADC calibration and thresholds. Physical calibration is necessary. |
| 14 | Live net status | Implemented; hardware test pending | Firmware reports Controller state; optional limits confirm endpoints. Without switches, position is explicitly a timed estimate. |
| 15 | Mini reading and sparkline | Implemented; phone acceptance pending | Latest Firebase readings drive the chart. |
| 16 | Last three history events/status chips | Implemented; phone acceptance pending | Preserves reported resolution (Resolved/Passed/Pending); legacy data falls back to severity. |
| 17 | Latest physical self-check | Implemented; hardware test pending | Firmware diagnosticResult retains latest result; Dashboard renders it. Actual motor/sensor acceptance pending. |
| 18 | Emergency-contact quick dial | Implemented; phone acceptance pending | Configured numbers open the dialer; local numbers must be supplied by the team. |
| 19 | Bottom navigation | Implemented; phone acceptance pending | Existing routes. |
| 20 | Real-time water gauge | Implemented; phone acceptance pending | Custom gauge updates from received validated telemetry. |
| 21 | Trend and live tag | Implemented; phone acceptance pending | Freshness-aware source labels and trend calculation. |
| 22 | Physical sensor-health tiles | Implemented; peripherals required | ESP32 reports RSSI, calibrated battery percentage and calibrated rain wetness when fitted/enabled; LiveMonitoring displays them. Rain wetness is not mm/hour. |
| 23 | Deployment state/activity time | Implemented; hardware test pending | ESP32 publishes net, lastActivityAt and positionFeedback; LiveMonitoring displays them. Device write identity enforced by rules. |
| 24 | Physical diagnostic trigger | Implemented; hardware test pending | LiveMonitoring confirms motor motion and sends DIAGNOSTIC; Controller performs leave/return feedback test and reports failure when prerequisites are absent. |
| 25 | Recent activity and full reading logs | Implemented; phone acceptance pending | Recent feed plus paginated complete stored readings. |
| 26 | Threshold warning banner | Implemented; phone acceptance pending | Fresh Warning/Danger readings control banner. |
| 27 | Event category filters | Implemented; phone acceptance pending | Existing recent filters; full history also supports filters. |
| 28 | Detailed event cards | Implemented; phone acceptance pending | Actual received fields, severity and timestamps. |
| 29 | Event detail drill-down | Implemented; phone acceptance pending | Existing detail dialog. |
| 30 | Link to live monitoring | Implemented; phone acceptance pending | Existing route. |
| 31 | End-of-history marker | Implemented; phone acceptance pending | Full-history browser marks end only after exhaustion; recent feed links to it. |
| 32 | Physical system vitals | Implemented; peripherals required | Firmware supplies HTTP latency, chip temperature, optional battery and alignment; ManualDeploy renders unknown for unavailable metrics. |
| 33 | Automatic/manual toggle | Implemented; hardware test pending | SET_MODE changes Controller automatic behavior; telemetry reports the actual mode. |
| 34 | Location/duration/thrust inputs | Implemented; hardware test pending | Validated location/duration/PWM settings reach firmware. Manual forward cycle uses requested 1–10 seconds and 10–100% duty. Duty is not measured thrust. |
| 35 | Pre-deployment checklist | Implemented; phone acceptance pending | Device checklist matches original four operator confirmations; demo uses explicit simulation confirmations. |
| 36 | Manual deploy button | Implemented; hardware test pending | MonitorRepository creates source-targeted expiring requests; Controller executes only while ready with closed interlocks, and producer acknowledges with immutable command identity. |
| 37 | Deployment preview/View All | Implemented; phone acceptance pending | Recent preview, detailed screen and full paginated stored history. |
| 38 | Today deployment counters | Implemented; phone acceptance pending | Asia/Manila day query without last-100 truncation; midnight rollover. |
| 39 | Detailed deployment entries | Implemented; phone acceptance pending | Preserves actual unit ID, timestamp, location, initiator, cycle/result. |
| 40 | Failed-deployment diagnostics | Implemented; phone acceptance pending | Displays reported error instead of inventing success. |
| 41 | Refresh and 24-hour archival | Implemented; cloud deployment pending | Refresh exists. functions/index.mjs schedules transactional 24-hour archival; HistoryBrowserActivity switches between current and archive. Service not deployed. |

## B. ESP32

All source paths below are under `firmware/FloodRescueNetESP32`. Firmware compiles for ESP32 Dev Module; physical tests are pending.

| # | Requirement | Source implementation | Acceptance dependency |
|---|---|---|---|
| B1 | Water level sensing and tiers | loop/analogRead(GPIO34), waterCm, Controller::tier | Calibrate ADC/cm and verify boundaries 879/880/1999/2000. |
| B2 | Local 16×2 LCD | Lcd16x2.h and loop; ADC/cm plus tier/net | Verify actual backpack mapping/address and legibility. |
| B3 | Tiered buzzer | GPIO18 normal off, warning 400 ms pulses, danger on | Observe/hear all three states. |
| B4 | Automatic deployment | Controller ready→deploying→deployed, forward PWM three seconds | Confirm driver direction, travel and endpoints with prototype. |
| B5 | Automatic retraction | Five continuous seconds below 880, reverse three seconds | Verify transient warning resets the delay and physical retraction succeeds. |
| B6 | Prevent repeat actuation | Explicit states; local arm; persisted command ID | Verify no duplicate deployment, reset behavior and shutdown. |
| B7 | Wireless status | TLS Wi-Fi station→Firebase→Android | Adapted to Firebase-first; direct ESP32 hotspot feed is not implemented. |
| B8 | Manual override | DEPLOY request validated, acknowledged and executed | Verify chosen duration/duty below danger threshold; stale/duplicate requests stay blocked. |
| B9 | Motor/sensor diagnostics | Calibrated ADC setup, LCD probe, motor leave-and-return limit feedback | Both limit switches required; fault injection and loaded mechanical evaluation remain unperformed. |

## Interpretation

The missing source paths from the earlier 80% milestone have been added. That is different from proving the whole system works. Completion can be marked only after the project owner configures/deploys Firebase, the peripherals are wired and calibrated, and phone/prototype acceptance is recorded. If the instructor requires the original direct-hotspot wording literally, revise the architecture requirement or implement that additional transport before claiming full coverage.
