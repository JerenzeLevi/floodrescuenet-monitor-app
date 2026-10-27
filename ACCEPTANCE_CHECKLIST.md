> Website-prototype integration update: the controlled source for this Mobile Dev demonstration is the website 3D model. Select Simulation in the app. Follow `../START_HERE.md` in the combined package. Physical ESP32 material below is optional and is not required for this demo.

# Phone, cloud and prototype acceptance

All boxes start unchecked because these end-to-end tests have not been run here. Record tester, date, device, firmware/configuration, observed result and evidence for each. A code test is not a substitute for the physical observation.

## Account and phone

- [ ] Install the newly built Android app; launch/relaunch, rotate and navigate all screens without a crash.
- [ ] Register all required fields; invalid inputs fail; optional position never assigns an access role.
- [ ] Unapproved user is denied; correct barangay admin can approve; other barangay and regular users cannot.
- [ ] Verified login, password recovery email/link and official-use notice work; access activity contains no credentials.
- [ ] Splash reports disconnected, waiting, uncalibrated and ready states truthfully; it never claims a sensor is ready on Firebase connectivity alone.
- [ ] Choose Simulation and ESP32; source identity and histories remain separate; a demo cannot acknowledge physical commands.
- [ ] Water cm, tiers, gauge, trend, sparkline, last-three chips, latest diagnostic and timestamp match the database/source.
- [ ] Emergency-contact list contains verified 911/local office/team numbers; tapping opens the correct dialer number.
- [ ] Full history can page past 100 records, handles tied timestamps, filters/detail dialogs and marks end only when exhausted.
- [ ] Today counts match all deployments in the Asia/Manila day, including more than 100 and midnight rollover.
- [ ] Offline/stale/invalid/uncalibrated data disables requests; reconnect restores fresh readings. Timeout never claims success.

## Firmware and mechanism

Start with an unloaded mechanism; confirm wiring/calibration before coupling the prototype.

- [ ] Power-on and reboot keep motor stopped until local arming; open shutdown input stops it and prevents arming.
- [ ] Measured depth matches calibrated cm; ADC boundary tests produce correct tier, LCD and buzzer behavior.
- [ ] At Danger, armed automatic mode drives forward for three seconds or stops at endpoint; state prevents repeated deployment.
- [ ] Normal for five continuous seconds starts reverse three-second travel; Warning/Danger during the delay resets it.
- [ ] Manual mode does not trigger from rising water; manual override works with chosen duration/PWM below Danger.
- [ ] End/home feedback stops travel; missing or conflicting endpoints report faults. Without limits, UI explicitly says timed estimate.
- [ ] Diagnostics leave and return to home; disconnected LCD, missing switch, motor blockage and shutdown cause truthful failure.
- [ ] Battery, rain and alignment sensors are installed/enabled/calibrated and displayed; omitted peripherals remain unknown.
- [ ] Network latency and chip temperature values are plausible; phone stale state occurs after 15 seconds without fresh data.
- [ ] Wi-Fi loss does not block local motor stopping/automatic logic. Reconnect publishes telemetry and queued outcomes; dropped record count is visible in telemetry.
- [ ] Duplicate/expired commands and reboot do not replay an old motion; incomplete outcomes remain unconfirmed.
- [ ] Evaluate loaded travel, jams, mounting, motor heating, supply behavior and local shutdown with the actual prototype before operational use.

## Cloud archive

- [ ] Deploy matching rules and provision viewer/producer link; verify wrong identity is rejected.
- [ ] Deploy archiveMonitorHistory in the intended Firebase project; scheduled runs appear in cloud logs.
- [ ] Seed a record older than 24 hours in development. After a run it appears in archive and disappears from current history without losing fields.
- [ ] Latest telemetry/diagnostic/commands and current-day totals survive archival; current and archived pages are readable only as authorized.
- [ ] Verify retry/backlog behavior, archiveStatus and history volume before a longer demonstration.

## Scope sign-off

- [ ] Supervisor accepts the inventory’s Firebase-first architecture in place of legacy direct-hotspot wording.
- [ ] Supervisor accepts rain wetness percentage and PWM percentage with their stated meanings; real physical measurements are not inferred from them.
- [ ] Record final results against FEATURE_COVERAGE.md. Do not mark this system 100% verified while any required acceptance result is missing.
