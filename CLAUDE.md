> Historical/reference document. For the current 14 October 2026 source update, use `README_FULL_UPDATE.md`, `FEATURE_COVERAGE.md`, `TEST_REPORT_FULL.md` and `firmware/README.md`. Earlier percentage claims and “no firmware” statements below describe earlier versions.

> Update — 13 October 2026: See `README_80_UPDATE.md`, `MILESTONE_80.md`, `DEVICE_CONTRACT.md` and `TEST_REPORT_80.md` for the current app changes and validation. The older status statements below are historical. The implementation is Java/XML with Firebase, separate demo/device sessions, scoped in-app admin approval and full paginated history. Hardware completion is not claimed.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

This is a native Android app built with Gradle (Kotlin DSL) and Jetpack Compose. Use the wrapper, not a system-wide `gradle`:

- Build debug APK: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`)
- Full build: `./gradlew build`
- Unit tests (JVM, `app/src/test`): `./gradlew test`
- Run a single unit test: `./gradlew test --tests "com.example.floodrescuenetmonitor.SomeTestClass.someMethod"`
- Instrumented tests (`app/src/androidTest`, needs emulator/device): `./gradlew connectedAndroidTest`
- Lint: `./gradlew lint`
- Clean: `./gradlew clean`
- Stop a stuck/misbehaving Gradle daemon: `./gradlew --stop`

The JVM heap for the Gradle daemon is capped at 1024m in `gradle.properties` (`org.gradle.jvmargs=-Xmx1024m`) — if builds crash the daemon or fail with OOM/JVM crash logs (`hs_err_pid*.log`, `replay_pid*.log` in the repo root), raising this value is the usual fix. Configuration cache is enabled (`org.gradle.configuration-cache=true`).

## Architecture

Single Gradle module (`app`), package `com.example.floodrescuenetmonitor`, minSdk 25 / targetSdk & compileSdk 36, Java 11 target.

**Actual UI toolkit is classic Java + XML views (`Activity` + `activity_*.xml` layouts), NOT Jetpack Compose.** No `.kt` or `XxxScreen.kt` composable files exist anywhere in `app/src/main/java` — everything is `.java`. (Earlier versions of this doc described a Compose architecture that was never built; corrected 2026-08-04.)

- Each screen is an `XxxActivity.java` in the root package paired with an `activity_xxx.xml` layout under `res/layout/`. Activities start each other via `Intent`s (e.g. `SplashActivity` → `LoginActivity`/`DashboardActivity`).
- Screens: Splash, Login, SignUp, Dashboard, LiveMonitoring, AlertsHistory, RecentDeployments, ManualDeploy.
- `ui/` holds Java view helpers: `BottomNavHelper.java`, `StatusPill.java`, and RecyclerView adapters under `ui/adapter/` (ContactAdapter, AlertAdapter, HistoryAdapter, DeployLogAdapter, DeploymentLogAdapter, ActivityLogAdapter).
- `data/SampleData.java` provides in-memory sample/mock data for list content (contacts, history, alerts, deployment logs) — dashboards/lists are still driven by static sample data, not live Firebase queries.
- **Auth is real, not sample data**: `LoginActivity`/`SignUpActivity` use Firebase Auth (`createUserWithEmailAndPassword`/`signInWithEmailAndPassword`) and write/read a profile at Realtime Database path `users/{uid}`, including a `verified` boolean gate (see Known gaps below).
- **Water level gauge:** `ui/WaterLevelGauge.java` is a custom ring view (0–20 cm arc; `setFilled(true)` adds the tinted water fill used on Live Monitoring). UI aligned to `../Thesis-records/visily-multiscreens/` (10 PNGs) on 2026-09-30; wording that would claim behaviour the app lacks (e.g. "connecting to node", "access attempts are logged") is deliberately kept honest for the Firebase demo session.
- **Known gap — no admin approval flow:** signup hardcodes `verified: false` on the new user's profile. Login blocks with "pending verification" until `verified` is `true`. There is no admin role, no admin screen, and no Cloud Function to flip this — it must currently be done by hand in the Firebase console (Realtime Database → `users/<uid>/verified` → set to `true`).

Dependency versions are centralized in `gradle/libs.versions.toml` (version catalog); `app/build.gradle.kts` references them via `libs.*` aliases.
