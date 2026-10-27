> Historical/reference document. For the current 14 October 2026 source update, use `README_FULL_UPDATE.md`, `FEATURE_COVERAGE.md`, `TEST_REPORT_FULL.md` and `firmware/README.md`. Earlier percentage claims and “no firmware” statements below describe earlier versions.

> Update — 13 October 2026: See `README_80_UPDATE.md`, `MILESTONE_80.md`, `DEVICE_CONTRACT.md` and `TEST_REPORT_80.md` for the current app changes and validation. The older status statements below are historical. The implementation is Java/XML with Firebase, separate demo/device sessions, scoped in-app admin approval and full paginated history. Hardware completion is not claimed.

# AGENTS.md

Guidance for coding agents (Codex, etc.) working in this repository. Also see `CLAUDE.md` (Claude Code) and `KNOWLEDGE.md` (thesis/hardware context) — all three should stay in sync.

## Commands

Native Android app, Gradle (Kotlin DSL) build scripts, but app code is plain Java + XML views (not Jetpack Compose — see Architecture below). Use the wrapper, not a system-wide `gradle`:

- Build debug APK: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`)
- Full build: `./gradlew build`
- Unit tests (JVM, `app/src/test`): `./gradlew test`
- Single unit test: `./gradlew test --tests "com.example.floodrescuenetmonitor.SomeTestClass.someMethod"`
- Instrumented tests (`app/src/androidTest`, needs emulator/device): `./gradlew connectedAndroidTest`
- Lint: `./gradlew lint`
- Clean: `./gradlew clean`
- Stop a stuck/misbehaving Gradle daemon: `./gradlew --stop`

The JVM heap for the Gradle daemon is capped at 1024m in `gradle.properties` (`org.gradle.jvmargs=-Xmx1024m`) — if builds crash the daemon or fail with OOM/JVM crash logs (`hs_err_pid*.log`, `replay_pid*.log` in the repo root), raising this value is the usual fix. Configuration cache is enabled (`org.gradle.configuration-cache=true`).

## Architecture

Single Gradle module (`app`), package `com.example.floodrescuenetmonitor`, minSdk 25 / targetSdk & compileSdk 36, Java 11 target.

**Actual UI toolkit is classic Java + XML views (`Activity` + `activity_*.xml` layouts), NOT Jetpack Compose.** No `.kt` or composable files exist under `app/src/main/java` — everything is `.java`. (Corrected 2026-08-04; prior versions of this doc wrongly described a Compose architecture.)

- Each screen is an `XxxActivity.java` in the root package paired with an `activity_xxx.xml` layout under `res/layout/`. Activities start each other via `Intent`s (e.g. `SplashActivity` → `LoginActivity`/`DashboardActivity`).
- Screens: Splash, Login, SignUp, Dashboard, LiveMonitoring, AlertsHistory, RecentDeployments, ManualDeploy.
- `ui/` holds Java view helpers (`BottomNavHelper.java`, `StatusPill.java`) and RecyclerView adapters under `ui/adapter/`.
- `data/SampleData.java` provides in-memory sample/mock data for list content (contacts, history, alerts, deployment logs).
- **Auth is real (Firebase), not sample data**: `LoginActivity`/`SignUpActivity` use Firebase Auth + Realtime Database (`users/{uid}`), including a `verified` boolean gate. See Known gaps below.

Dependency versions are centralized in `gradle/libs.versions.toml` (version catalog); `app/build.gradle.kts` references them via `libs.*` aliases.

## Known gaps (as of 2026-08-04)

- **Circular water level gauge not implemented.** The mockup (`../Thesis-records/visily-multiscreens.pdf`) shows a circular progress ring for water level on Dashboard and Live Monitoring. Current code has no arc/progress view: Dashboard is plain `TextView` numbers + static legend dots; Live Monitoring's "circle" is a flat solid-color oval drawable (`drawable/bg_gauge_circle.xml`) with no fill logic. Needs a real custom View or arc-drawing (e.g. `CircularProgressIndicator`-style) that reflects the live reading against Normal/Warning/Danger thresholds.
- **No admin approval flow.** `SignUpActivity` hardcodes `verified: false` on the new user's Realtime Database profile (`users/{uid}/verified`). `LoginActivity` blocks sign-in until that field is `true`, but nothing in the app can ever set it — no admin role, no admin screen, no Cloud Function. Must be flipped manually in the Firebase console for now. This is the reason the app currently can't be logged into out of the box.

## Project context

This app is the mobile companion for an undergraduate thesis (Saint Columban College, "Soul Grievers" group): *Development of an Automated Flood Rescue Net Deployment System with Tiered Warning Using ESP32 and Water Level Sensors*. It is NOT a standalone commercial product — it's a companion app for a physical ESP32 + L298N + DC motor winch prototype that deploys a rescue net across a river channel on high water level.

- **Target users:** barangay officials / disaster response teams (not the general public), with per-barangay data isolation.
- **Connectivity intent:** the ESP32 runs as a WiFi hotspot/AP that the phone connects to directly (no external cloud backend planned).
- **Screen set mirrors the thesis's identified real-time-visibility gap** — Live Monitoring and Alerts & History exist specifically to show net deployment status, which prior literature (cited in the thesis) lacks.
- **UI source of truth:** the screens' visual design originates from `../Thesis-records/visily-multiscreens.pdf` (Visily-generated wireframes/mockups). When implementing or revising UI, treat that PDF as the design spec to match, not just `SampleData.kt`.

Full hardware/firmware/research context lives in `KNOWLEDGE.md` in this directory (mirrored from the thesis records folder) — read it before making UI/data-model decisions that need to reflect real device behavior (thresholds, pin mapping, deploy/retract timing, etc).
