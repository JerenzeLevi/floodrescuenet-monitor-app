# Mobile app ↔ Firebase ↔ website 3D prototype

**The website's existing 3D net is the prototype controlled by your mobile app. No physical ESP32 is needed for this demonstration.** Its model, camera views, materials and net geometry are preserved.

The phone writes a request to Firebase. The open website receives it, drives the actual 3D animation, and sends status, results and history back to the phone. Use this package together instead of the earlier local-only website or ESP32-focused instructions.

## Included

- `site/`: complete website, built Firebase bundle and editable source. Deploy only this folder as the website root.
- `mobile/`: Android source and the compatible debug APK at `mobile/dist/FloodRescueNetMonitor-debug.apk`. The existing Simulation mode already uses this protocol; no Android binary change was needed for the website bridge.
- `firebase/database.rules.json`: matching rules with a single active browser producer per account. The same rules are in `mobile/firebase/database.rules.json`.
- `mobile/functions/`: scheduled 24-hour archive service, supplied but not deployed.
- `INTEGRATION_TEST_REPORT.md`: local test evidence and live acceptance checklist.
- `legacy/`: original integration patch and guide, retained as history. Applying that patch alone will not add the Firebase connection.

## Setup

1. Install `mobile/dist/FloodRescueNetMonitor-debug.apk` on your Android test phone, or build `mobile/` in Android Studio. It uses your uploaded Firebase Android configuration.
2. In Firebase project **floodrescue-app**, enable Email/Password Authentication and Realtime Database if needed. Register an account in the app and approve it. The project owner must bootstrap the first verified administrator in Firebase Console; see the app guide.
3. From this package root, deploy the matching database rules:

```sh
npx firebase-tools@14 deploy --only database --project floodrescue-app
```

Older rules can reject the new browser-producer record. If this database also serves unrelated applications, merge their branches before deployment.

4. Deploy `site/` to your existing hosting. For Vercel, use `site` as the root directory, or copy its contents into the website repository root. Retain `vercel.json`: its security policy permits the necessary Firebase connections. A built bundle is included; to rebuild after editing, run `npm ci` then `npm run build` inside `site/`.
5. Open the hosted `/simulator/` page over HTTPS. For local preview, run `py -m http.server 8000` inside `site/` and open `http://localhost:8000/simulator/`. Do not double-click the HTML file.
6. On the phone, select **Profile → Choose monitoring source → Simulation**. Sign in to the website with **the same approved email and password**, then click **Connect prototype**.
7. The website should say **Linked to Firebase**. This means the browser is ready to exchange data; it is not a phone-presence indicator. Keep this tab visible and the computer awake.
8. Stop the old command-line demo feeder and close any other connected prototype using that account. A second browser is blocked while the first owns the session. Allow up to 15 seconds after disconnecting/closing before reconnecting.

No real account was signed in, website published, production rules deployed, or phone installed during this update. Tests used local Firebase emulators and a real browser rendering the 3D model.

## Demonstrate the connection

1. Change to Manual mode in the phone; wait for Completed. The website selector should change.
2. In the phone, enter a location, duration of 1–10 seconds and power of 10–100%; check the simulation confirmations and request deployment.
3. Watch the website net feed and unfold. The phone should show Accepted/Deploying, followed by Completed/Deployed. The deployment log reports unit `WEB-3D-01`, your location, result and cycle time. Power is a virtual control setting, not measured physical thrust.
4. Click **Reset & repack** on the website; Ready returns in the app. Reset during movement creates a failed deployment result.
5. Raise water with the website slider; the app's reading, warning tier, gauge and history update. In Automatic mode, Danger starts deployment.
6. Request System Diagnostic in the phone. The website returns a retained simulation result and event. Set **Simulated health inputs → Alignment → Misaligned**, or battery to zero, to demonstrate a failed virtual diagnostic/deployment.
7. Change virtual battery, temperature and rain values on the website; they appear in the app's health/vitals tiles. Network latency measures a Firebase transaction; other health inputs are explicitly simulated.
8. Disconnect/close the website. After the app's 15-second freshness window, it should mark the reading stale and block new commands instead of inventing completion.

Configure verified emergency-contact numbers in the session's `contacts` records using the schema in `mobile/DEVICE_CONTRACT.md`. No local office phone numbers are invented by this package.

## Archive setup

The app's current and archived history screens use the same session UID. To enable the supplied 24-hour archive service, run `npm ci` inside `mobile/functions`, then deploy from this package root:

```sh
npx firebase-tools@14 deploy --only functions:archiveMonitorHistory --project floodrescue-app
```

Scheduled Functions need the project's appropriate service/billing setup, controlled by its owner. No billing is activated here. The app already has **Open cloud archive** in full-history screens. Records move on successful scheduled runs, so the transition need not occur at the exact 24-hour instant.

## Behavior and boundaries

- Defaults: Warning 10 cm, Danger 16 cm. Website threshold edits are published to the app.
- The supplied feed-through 3D net still uses manual reset/repacking. Falling water does not rewind the mesh. This preserves the supplied model and does not claim physical retraction.
- Refresh/reconnect never resumes an accepted command. An interrupted net is conservatively shown as deployed until an explicit repack, and old pending requests are failed on reconnect.
- Hidden or unavailable 3D views stop publishing fresh telemetry and do not consume new requests. Keep the page visible; background browser suspension is expected.
- The app label remains **DEMO/Simulation**. Do not select ESP32 for this website. The optional physical firmware in the Android source is not used in this flow.
- Auth sessions stay in browser memory; the password input is cleared after login. No credentials are included in the source. The public Firebase settings match your Android project.
- If that API key is restricted to Android only, use the same Firebase project's Web app configuration in `site/simulator/firebase-config.js` and configure the required Auth/Database API access in your project.
- Event buffering is finite and in memory; this is not a durable offline controller. `droppedRecords` reports overflow. The old feeder does not participate in the new browser lock, so do not run both.

The app-to-website code is implemented and locally tested. Live hosting and actual-phone acceptance still require your approved account. No unqualified 100% physical-system or grading claim is made.
