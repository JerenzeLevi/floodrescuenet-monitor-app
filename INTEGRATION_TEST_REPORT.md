# Website/mobile integration validation — 14 October 2026

## Passed

- **18 existing Firebase permission/history tests rerun against the updated rules:** all passed, including account approval, scoped admins, physical-device separation, expiry, paging, daily totals and archives.

- **7 website regression tests:** shared navigation, asset routes, same-origin scripts/Firebase connection policy, mobile menu, automatic/manual behavior, thresholds and finite net geometry.
- **8 Firebase integration tests against actual RTDB rules:** mobile deployment changes the same Simulation instance used by the 3D renderer; configured duration/power; completion/history; mode commands; successful/failed diagnostics; water/health feedback; duplicate-browser exclusion; wrong writer/user rejection; reconnect/no replay; paused-view behavior; reset failure; unapproved/expired request rejection.
- **Real Chromium/WebGL browser test with Firebase Auth + Database emulators:** signed into the website, sent four mobile-format requests from a separate authenticated SDK client, watched net animation progress, verified completion/deployment records, changed mode, ran diagnostic, reset/repacked, changed water, triggered automatic deployment and disconnected. No uncaught page errors. Desktop and 390-pixel mobile screenshots were inspected; no mobile horizontal overflow.
- **Android compatibility:** the same `demoSessions/<uid>`/`source: simulation` contract used by MonitorRepository. The included APK is the previously successfully built and signature-verified debug APK; Android source was not changed for this bridge. Database rules and documentation are updated in this package.

The browser test uses the real command shape and transaction behavior of the mobile app, but does not run the Android APK on a phone. The visible 3D mesh uses the same controller progress tested here; this is not a separate terminal feeder pretending to be the website.

## Reproduce locally

Inside `site`, run `npm ci`, `npm run build`, and `npm test`. With Node and Java installed, run these from the package root:

```sh
npx firebase-tools@14 emulators:exec --project demo-flood-web --only database "node --test site/tests/firebase-integration.test.mjs"
```

For the actual browser test, install Chromium using `npx playwright install chromium` inside `site`, then from the package root:

```sh
npx firebase-tools@14 emulators:exec --project demo-flood-web --only database,auth "node site/tests/browser-integration.mjs"
```

These tests create disposable emulator users; they do not access production accounts. The browser test substitutes emulator configuration only in its local test server. The shipped Firebase config targets the same project as the Android app and contains no test credentials.

## Acceptance still required in your deployed project

- [ ] Deploy the matching rules and updated website to your actual hosting.
- [ ] Install/open the supplied APK, sign in to both clients with the same approved account, and select Simulation in the app.
- [ ] Repeat the eight demonstration steps in START_HERE.md on the phone, including timeout/stale behavior.
- [ ] Deploy and observe the archive job if 24-hour archival is part of the assessment.
- [ ] Configure actual emergency-contact numbers and record screenshots/results for your assessor.

There is no claim of physical sensor/motor testing, direct-hotspot integration, loaded rescue-net performance or a verified 100% grading score. All health values except network transaction timing are explicitly simulated inputs for this website prototype.
