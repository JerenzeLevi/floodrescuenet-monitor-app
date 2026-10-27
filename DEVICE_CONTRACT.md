# Provisioned ESP32 session contract

This document describes the Android/ESP32 contract implemented by `firmware/FloodRescueNetESP32`. Firmware is included but has not been flashed or physically tested. This path is separate from the demonstration contract.

## Provisioning

1. Use an approved mobile user UID (the viewer). Create a separate Firebase Authentication account for the ESP32 producer. Do not use administrator credentials on the device.
2. As the Firebase project owner, set `deviceLinks/<viewerUid>` to the producer’s exact Auth UID in the Console. Clients cannot write deviceLinks. This prototype supports one assigned producer per viewer; shared barangay-wide devices require a future access model.
3. The producer signs in through Firebase Auth and refreshes tokens. It reads/writes `deviceSessions/<viewerUid>` using its own identity. It cannot create mobile commands.
4. The phone signs in as the viewer and selects Profile → Choose monitoring source → ESP32. The viewer can read this session and create requests; it cannot forge telemetry or acknowledge its own physical commands.
5. Synchronize device time and use server timestamps for reported records where practical. Timestamps more than five seconds in the future are rejected. Missing/stale readings disable mobile commands after 15 seconds.

## Telemetry

Write `deviceSessions/<viewerUid>/telemetry` with this shape. The numbers below are **schema examples**, not actual measurements:

```json
{
  "schemaVersion": 1,
  "source": "esp32",
  "unitId": "ESP32-01",
  "waterCm": 4.0,
  "warningCm": 10.0,
  "dangerCm": 16.0,
  "net": "ready",
  "mode": "manual",
  "updatedAt": {".sv": "timestamp"},
  "lastActivityAt": {".sv": "timestamp"},
  "sensorReady": true,
  "positionFeedback": "limit switches",
  "droppedRecords": 0
}
```

`net`: unarmed, ready, deploying, deployed, retracting, diagnosing, or fault. `mode`: automatic or manual. Calibration must convert the original ADC reading to centimeters; never send raw GPIO34 ADC counts as `waterCm`. The current classroom gauge/schema supports 0–20 cm. Thresholds must satisfy 0 < warning < danger <= 20.

Optional measured/reported fields: batteryPercent (0–100), latencyMs (0–60000), coreTempC (-50–150), signal, rain, alignment and motorPowerPercent (0–100). Omit measurements that are unavailable. Report status based on observed feedback where available, not solely on an elapsed animation timer.

## Requests and acknowledgements

Read `command`. Mobile requests include id, requestedBy, target (`esp32`), type (`DEPLOY`, `SET_MODE`, `DIAGNOSTIC`), status (`pending`), location, mode, durationMs (1000–10000), thrustPercent (10–100), createdAt and expiresAt. DIAGNOSTIC and SET_MODE also carry default options. The included DIAGNOSTIC intentionally moves the motor briefly; SET_MODE does not directly move it, but enabling automatic mode at Danger can immediately initiate deployment.

- Process only a fresh pending request addressed to this source/viewer. Persist or otherwise deduplicate the request ID across restarts; never replay an accepted actuation after restart.
- Copy the original command fields unchanged when writing `accepted`, `completed` or `failed`, adding acknowledgedAt and a truthful message (maximum 240 characters).
- Do not acknowledge expired requests or overwrite a newer ID. Use conditional writes/transactions as appropriate in the producer.
- `DEPLOY`: apply validated duration/power within the prototype’s tested mechanical limits; report actual completion or an observed fault. Stop outputs safely on failure. Network delivery alone is not evidence of net deployment.
- `SET_MODE`: update actual controller behavior and telemetry mode before claiming completion.
- `DIAGNOSTIC`: run only diagnostics the hardware actually supports. Report unknown/unavailable tests honestly. Never label a heartbeat-only response as a complete motor/sensor test.

## History records

Use stable IDs so retries do not duplicate events.

- `readings/<id>`: timestamp, waterCm, optional trend.
- `events/<id>`: timestamp, severity, title, description, category; optional type and resolution (Resolved/Passed/Pending).
- `deployments/<id>`: timestamp, location, initiator, status (Success/Failed/Pending), optional unitId, cycleMs, thrustPercent and error. Prefer command ID for manual records. Include unitId to avoid an invented unit label.
- `diagnostic`: the latest diagnostic event, including type `DIAGNOSTIC`. It persists independently of the latest-100 event window.
- `contacts/<id>`: name, number and optional subtitle. Configure verified local contacts; do not invent emergency numbers.

After a real diagnostic, publish its event and latest diagnostic record atomically when possible. Set resolution according to the actual result, not automatically to Passed.

## Tests before switching a live demonstration to ESP32

Confirm telemetry visibility and source labels, wrong-producer rejection, manual/automatic mode changes, observed deploy completion, duplicate/expired command rejection, network-loss behavior, and the real sensor/motor result. These hardware checks were not performed by this update.
