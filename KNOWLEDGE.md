> Historical/reference document. For the current 14 October 2026 source update, use `README_FULL_UPDATE.md`, `FEATURE_COVERAGE.md`, `TEST_REPORT_FULL.md` and `firmware/README.md`. Earlier percentage claims and “no firmware” statements below describe earlier versions.

> Update — 13 October 2026: See `README_80_UPDATE.md`, `MILESTONE_80.md`, `DEVICE_CONTRACT.md` and `TEST_REPORT_80.md` for the current app changes and validation. The older status statements below are historical. The implementation is Java/XML with Firebase, separate demo/device sessions, scoped in-app admin approval and full paginated history. Hardware completion is not claimed.

# KNOWLEDGE.md

Thesis/hardware context for this Android app. Mirrored (trimmed) from `C:\D\Thesis-records\KNOWLEDGE.md` — that file is the full source of truth (research gaps, literature, day-by-day logbook milestones); this copy keeps only what's relevant to building the mobile companion app. If the two diverge, defer to the Thesis-records copy for research/hardware facts.

---

## Thesis Identity

**Full Title:** Development of an Automated Flood Rescue Net Deployment System with Tiered Warning Using ESP32 and Water Level Sensors
**Researcher / Group:** Jerenze Levi T. Omandam — group name **"Soul Grievers"**
**Institution:** Saint Columban College, College of Computing Studies, Pagadian City

## Team

| Member | Role |
|---|---|
| Jerenze Levi T. Omandam | Project Manager, hardware/firmware lead, research lead |
| Aenon John S. Abay | System Programmer, LED/buzzer integration, mobile app UI |
| Sarah G. Giamat | Quality Assurance, initial mobile app wireframes |

## The Core Idea

Existing flood detection systems (ESP32 + sensors + alerts) are purely passive — detect and alert, no physical action. This system adds an active rescue mechanism: a motorized net that deploys across a river channel when dangerous water levels are detected, so people swept by floodwater can cling to it. The mobile app exists to close the "no real-time visibility into the physical mechanism's state" gap identified in prior literature — i.e., the app's job is to show what the hardware is doing, not just raw sensor numbers.

## Hardware System (what the app represents/monitors)

```
Water Level Sensor (analog, GPIO34)
    ↓
ESP32 Microcontroller
    ├── I2C LCD Display (0x27, 16x2) — shows Level + Status
    ├── L298N Motor Driver (IN1=GPIO26, IN2=GPIO27, ENA=GPIO25)
    │       └── DC Motor (winch/spool that deploys the net)
    ├── Active Buzzer (GPIO18)
    └── Mobile App (companion — this project)
```

**Water Level Thresholds (tiered states the app should reflect):**
- `< 880` raw → **NORMAL** → motor stopped, buzzer off
- `880–2000` raw → **WARNING** → buzzer beeps intermittently
- `≥ 2000` raw → **DANGER** → motor runs (net deploys), buzzer continuous

**Auto-retract:** once level drops back below WARNING, system waits 5s then reverses motor to retract net.
**Motor timing:** 3s forward to deploy, 3s reverse to retract (tunable to spool size).

These three states (NORMAL / WARNING / DANGER) are the state machine the Dashboard, Live Monitoring, and Alerts & History screens should key off of — status badges/colors, alert log entries, and manual override controls all map to this.

## Connectivity Model

- ESP32 is intended to run as its own WiFi access point/hotspot; the phone app connects directly to it (no external cloud/internet backend planned for the prototype).
- There is currently no real backend — `data/SampleData.kt` in the app provides static mock data standing in for what will eventually come from the ESP32 over WiFi.
- Auth model: per-barangay data isolation (each barangay's officials see only their own deployment/alert data).

## Mobile App Scope

- **Target users:** barangay officials / disaster response teams — NOT the general public.
- **Screens (per thesis + Visily wireframes):** Splash, Login, Sign Up, Dashboard, Live Monitoring, Alerts & History, Recent Deployments, Manual Deploy, Emergency Contacts.
- **Explicitly out of scope:** AI/flood prediction (excluded as too time-intensive for the thesis timeline).
- **UI design source:** `../Thesis-records/visily-multiscreens.pdf` — Visily-generated multi-screen mockups are the visual spec these screens were built from.

## Key Technical Lessons (hardware side, for context when wiring up real data later)

1. L298N drops ~2V — a 3V motor needs 5V+ input; two 3.7V 18650 in series (~7.4V) works, one alone doesn't.
2. ESP32 PWM uses `ledcAttach()` (Core 3.x), not legacy `ledcSetup()`.
3. ENA jumper cap must be removed on L298N before PWM speed control works.

## Related Files

- Full research context, literature review, and day-by-day logbook: `C:\D\Thesis-records\KNOWLEDGE.md`
- UI design spec: `C:\D\Thesis-records\visily-multiscreens.pdf`
- Plain-language build guide: `C:\D\Thesis-records\BUILD_COMPONENTS_GUIDE.md`
