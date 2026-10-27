> Historical/reference document. For the current 14 October 2026 source update, use `README_FULL_UPDATE.md`, `FEATURE_COVERAGE.md`, `TEST_REPORT_FULL.md` and `firmware/README.md`. Earlier percentage claims and “no firmware” statements below describe earlier versions.

> Update — 13 October 2026: See `README_80_UPDATE.md`, `MILESTONE_80.md`, `DEVICE_CONTRACT.md` and `TEST_REPORT_80.md` for the current app changes and validation. The older status statements below are historical. The implementation is Java/XML with Firebase, separate demo/device sessions, scoped in-app admin approval and full paginated history. Hardware completion is not claimed.

# FloodRescueNet Monitor — Firebase demo milestone

This is the updated **native Android app**, using your existing Java/XML screens. Monitoring and history now come from Firebase. Commands go to a private simulated session and wait for an acknowledgement. The included laptop data feeder lets you demonstrate the app before connecting the 3D website.

Start with the Android app and this feeder. The website has not been modified or connected by this update. No ESP32 is needed for these tests.

## 1. Open the Android project

1. Extract the ZIP into a new folder, keeping your original project as a backup.
2. In Android Studio, choose **Open** and select the folder containing `settings.gradle.kts`.
3. Use **JDK 17** for Gradle. Allow Studio to install **Android SDK 36** and **Build Tools 35.0.0** if prompted.
4. Let Gradle sync. The project uses AGP 8.10.1 and Gradle 8.11.1; the wrapper is included.
5. Select an Android emulator or phone (Android 7.1/API 25 or later) and run `app`.

The supplied `app/google-services.json` is retained from your uploaded project. It must refer to the Firebase project you intend to test. For a separate development project, replace it with that project's Android configuration for `com.example.floodrescuenetmonitor`.

## 2. Configure Firebase once

Use a development Firebase project where you can manage Authentication and Realtime Database.

1. Enable **Authentication → Sign-in method → Email/Password**.
2. Open/create **Realtime Database**. Ensure its URL is present as `project_info.firebase_url` in `app/google-services.json`.
3. Review `firebase/database.rules.json`. It defines `users/{uid}` and isolated `demoSessions/{uid}`. For a new demo database, copy its complete contents into **Realtime Database → Rules** and publish. If this project already serves the team's site or other devices, have the project owner merge these branches with its existing rules; do not replace unrelated permissions or data.
4. Run the Android app and sign up. A new profile has `verified: false`, so it cannot access monitoring yet.
5. As the Firebase project administrator, set **`users/<new user's UID>/verified` to boolean `true`** in the console. This is account approval, separate from email verification. The app deliberately cannot approve itself.
6. Log in. A connected screen initially says it is waiting for a data source. Empty history and “Not reported” health tiles are expected.

An existing approved account can be used instead. No passwords or service-account credentials need to be placed in the source code or shared in chat.

## 3. Start the laptop demo feeder

Use Node.js 22 LTS. The normal feeder uses built-in Node modules, so **`npm install` is not required to run it**.

Open PowerShell in the extracted project folder:

```powershell
.\tools\start-demo.ps1
```

Enter the **same approved account** you used in the Android app. Keep the terminal open. The script clears its credential environment variables when it exits.

If your computer's policy blocks PowerShell scripts, use these commands directly in PowerShell:

```powershell
$demoCredential = Get-Credential
try {
    $env:FLOOD_EMAIL = $demoCredential.UserName
    $env:FLOOD_PASSWORD = $demoCredential.GetNetworkCredential().Password
    node .\tools\demo-feed.mjs
} finally {
    Remove-Item Env:FLOOD_EMAIL, Env:FLOOD_PASSWORD -ErrorAction SilentlyContinue
    $demoCredential = $null
}
```

The terminal accepts:

| Command | Result |
|---|---|
| `water 4` | Normal simulated reading |
| `water 11` | Warning reading and an event |
| `water 17` | Danger reading; automatic mode starts the five-second demo |
| `status` | Prints demo water level, mode and net state |
| `reset` | Represents manual repacking: net ready, water 4 cm |
| `quit` | Stops updates; the app marks data stale after 15 seconds |

Use one feeder per account. A different account has a different session. Both the laptop and Android device need internet access; they do not need to be on the same Wi-Fi network. Keep the laptop clock set automatically because request expiry uses timestamps.

If a prior run stopped while deploying, the feeder requires an explicit repack:

```powershell
.\tools\start-demo.ps1 -Repack
```

## 4. Demonstrate the working app features

Run these checks before claiming the app milestone in a presentation:

1. **Authentication:** wrong password fails; a new account remains blocked until approved; an approved account opens Dashboard; Sign out returns to Login.
2. **Dashboard and Live:** enter `water 4`, then `water 11`. Both screens update, the live gauge changes, readings accumulate, and history shows the warning event.
3. **Automatic demo:** ensure Auto mode is on, then enter `water 17`. Net state changes from Ready to Deploying to Deployed. History and deployment counters update from the recorded result.
4. **Manual repack:** enter `water 4`. The net stays Deployed. Enter `reset` to make it Ready. This matches the accepted manual-repacking demonstration, not the older automatic-retraction proposal.
5. **Manual control:** in Deploy, turn Auto mode off and wait for confirmation. Complete the four demo checklist items, enter a location and send one deployment request. Check pending → accepted → completed. A second request is unavailable while one is active.
6. **History:** open Alerts, change filters, tap an event or Details, then view all deployment records. Counts reflect the loaded records; no invented “today” totals or 24-hour archive are claimed.
7. **Diagnostic:** use Live → System Diagnostic. It checks the simulated data source and produces a result event. It does not claim to test a physical sensor or motor.
8. **Stale/offline:** stop the feeder. After 15 seconds, the app marks the reading stale and disables commands. Disable the phone's network to check the offline state. Last readings remain visible with their timestamp.
9. **Contacts:** add a verified test contact at `demoSessions/<uid>/contacts/<id>` with `name`, `subtitle`, and `number` strings. Tapping opens the dialer for review; it does not place a call automatically. Do not call emergency services for a demo.

A command expires after about 30 seconds. A timeout means **result not confirmed**, not success. Start a fresh request after the feeder is connected again; expired requests are not replayed.

## 5. Troubleshooting

| What you see | What to check |
|---|---|
| Account pending verification | Administrator must approve `users/<uid>/verified` as boolean `true` |
| Permission denied | Rules are installed in the same database as `google-services.json`; account is approved; timestamps are current |
| Waiting for a data source | Start the feeder with the same account as the phone |
| Stale data | Keep the feeder running and restore internet access |
| Request timed out | Feeder was unavailable or did not confirm before expiry; no success is assumed |
| Net not ready | Use `reset` in the feeder to represent manual repacking |
| Missing health metrics | Battery/rain/alignment values were not reported; this is expected for this demo |
| App cannot install over an older debug build | Debug signing keys may differ; build with your own Studio setup, or uninstall the older demo app first |

## Later: connect the existing 3D website

`DATA_CONTRACT.md` defines the same Firebase paths, timestamps and command lifecycle for the future website adapter. The website will replace the laptop feeder as the session's single data producer. It will publish its water/net state, acknowledge mobile commands, and show the result through its existing animation.

This update does **not** deploy rules to your cloud project, sign into your account, publish the website, or claim a completed phone-to-website test. See `MILESTONE_60.md` and `TEST_REPORT.md` for the exact coverage and validation status.
