> Historical/reference document. For the current 14 October 2026 source update, use `README_FULL_UPDATE.md`, `FEATURE_COVERAGE.md`, `TEST_REPORT_FULL.md` and `firmware/README.md`. Earlier percentage claims and “no firmware” statements below describe earlier versions.

> Update — 13 October 2026: See `README_80_UPDATE.md`, `MILESTONE_80.md`, `DEVICE_CONTRACT.md` and `TEST_REPORT_80.md` for the current app changes and validation. The older status statements below are historical. The implementation is Java/XML with Firebase, separate demo/device sessions, scoped in-app admin approval and full paginated history. Hardware completion is not claimed.

# Demo session contract — version 1

Scope: classroom software simulation. The Android app and one data producer (the included feeder now, the 3D website later) use the **same Firebase Auth account**. No physical device namespace or motor endpoint is used.

## Paths and ownership

| Path | Reader / writer |
|---|---|
| `users/{uid}` | Account creates its own unapproved profile; administrator approves in Firebase Console |
| `demoSessions/{uid}/telemetry` | Producer writes; Android reads |
| `demoSessions/{uid}/command` | Android creates one pending command transactionally; producer acknowledges it |
| `demoSessions/{uid}/readings/{id}` | Producer writes samples; Android loads newest 60 |
| `demoSessions/{uid}/events/{id}` | Producer writes events; Android loads newest 100 |
| `demoSessions/{uid}/deployments/{id}` | Producer writes outcomes; Android loads newest 100 |
| `demoSessions/{uid}/contacts/{id}` | Approved demo account/administrator configures; Android loads up to 20 |

Rules enforce the authenticated UID and `users/{uid}/verified == true`. Isolation is **per account**, not per barangay or shared team. Both clients have the same demo permissions; this is not a trusted hardware attestation protocol. A future shared-team deployment needs a separate membership/role design.

## Telemetry

```json
{
  "schemaVersion": 1,
  "source": "simulation",
  "waterCm": 4,
  "warningCm": 10,
  "dangerCm": 16,
  "net": "ready",
  "mode": "automatic",
  "updatedAt": 1800000000000,
  "lastActivityAt": 1800000000000
}
```

Example timestamps above are placeholders: always send current **Unix milliseconds**. Telemetry must refresh about every two seconds even when water is unchanged. After 15 seconds without a valid update, Android disables commands. `.info/connected` alone does not mean the data producer is alive. Android adjusts its clock using `.info/serverTimeOffset`.

- `waterCm`: 0–20 cm in the tabletop demo. Normal `<10`, Warning `10..<16`, Danger `>=16`.
- `net`: `ready`, `deploying`, `deployed`; `mode`: `automatic` or `manual`.
- `warningCm` must be below `dangerCm`. Defaults in the feeder match the existing 3D prototype: 10/16 cm.
- Optional metrics: `batteryPercent`, `latencyMs`, `coreTempC`, `signal`, `rain`, `alignment`. Omit unavailable metrics; the app displays “Not reported.”
- The old raw ADC thresholds 880/2000 in the proposal are not converted to centimetres. A real sensor would need separate calibration.

## Command lifecycle

```json
{
  "id": "a-unique-uuid-for-this-request",
  "requestedBy": "the-authenticated-uid",
  "target": "simulation",
  "type": "DEPLOY",
  "status": "pending",
  "location": "Tabletop demo",
  "mode": "manual",
  "createdAt": 1800000000000,
  "expiresAt": 1800000030000
}
```

Types are `DEPLOY`, `SET_MODE`, `DIAGNOSTIC`. Android creates the command with a Firebase transaction and a server timestamp. There is one command slot: a pending/accepted command blocks a new one until completion, failure or expiry.

The producer verifies the UID, target, type and expiry. It preserves all request fields, changes `status` to `accepted`, then `completed` or `failed`, and adds `acknowledgedAt` plus `message`. Mode/diagnostic requests can go directly to completed. Completed/failed IDs cannot be reset to pending. A new request uses a new ID. Do not set success merely because Firebase saved the request.

For a deployment, publish `net: deploying`, perform the five-second demo transition, then publish `net: deployed`, a Success deployment record and the completed acknowledgement together. A request whose acknowledgement expires remains unconfirmed even if later telemetry reports a deployed state. Never replay old accepted requests after a restart. The feeder fails an unexpired leftover request at startup; a stopped in-progress demo requires explicit `--repack`.

Automatic mode starts a demo when water reaches Danger and net is Ready. Manual mode waits for a DEPLOY command. Once Deployed, lowering water does not retract the net. `reset` represents manual repacking and resets the demonstration to water 4 cm / net Ready; it is not a remote hardware command.

## Histories and contacts

```json
{
  "reading": {"timestamp": 1800000000000, "waterCm": 11, "trend": "RISING"},
  "event": {"timestamp": 1800000000000, "severity": "WARNING", "title": "Water level: WARNING", "description": "Demo water is 11 cm.", "category": "System", "type": "STATUS"},
  "deployment": {"timestamp": 1800000000000, "location": "Tabletop demo", "initiator": "Manual", "status": "Success", "cycleMs": 5000},
  "contact": {"name": "Your verified contact", "subtitle": "Demo contact", "number": "+1 202 555 0100"}
}
```

Use separate child records at the paths above, not a combined object named reading/event/deployment/contact. The sample contact number is illustrative; configure actual contacts deliberately. Deployment `status` is case-sensitive: `Success`, `Failed`, `Pending`. Optional `error` appears for failures. Event categories are `Danger`, `Net Deployments`, `System`; the All tab includes every category. Use `type: DIAGNOSTIC` for the latest diagnostic result shown on Dashboard.

IDs remain stable across network retries. Do not retry a terminal command acknowledgement against a newer command or an already completed command. Events/readings/deployments are queried with limits in the app; this milestone has no automatic database retention/deletion job. The feeder is for short demonstrations, not unattended continuous collection.

## Future website adapter

1. Add Firebase email/password sign-in for the demo operator using the same development project and account as Android.
2. Publish the existing simulator's state under the telemetry schema; keep a heartbeat while the simulator is active.
3. Observe the command slot. Validate and de-duplicate IDs before calling the simulator's existing deploy/mode actions.
4. Publish command acknowledgement and events from the actual animation state, not from button clicks alone.
5. Stop the laptop feeder before starting the website producer. Do not have two producers overwrite the same session.
6. Repeat the acceptance scenarios in `START_HERE.md` with the website open on a second device.

The current Firebase contract intentionally leaves room for that adapter; no website integration is claimed by this Android source update.

Primary references: [Firebase Android reads/writes](https://firebase.google.com/docs/database/android/read-and-write), [Security rules conditions](https://firebase.google.com/docs/database/security/rules-conditions), [Emulator connections](https://firebase.google.com/docs/emulator-suite/connect_rtdb), [Firebase password authentication](https://firebase.google.com/docs/auth/android/password-auth).
