# ESP32 prototype firmware

Target: classic dual-core ESP32 Dev Module, Arduino ESP32 core **3.0.7**, ArduinoJson **7.4.2**. These versions were used for compilation. The sketch uses native core libraries and the included PCF8574 LCD driver; no separate LCD library is needed.

## Wiring and configuration

| Function | ESP32 pin / connection |
|---|---|
| Analog water sensor | GPIO34, voltage limited to ESP32 input range |
| Active buzzer control | GPIO18, appropriate driver for the buzzer load |
| L298N forward/reverse | IN1 GPIO26, IN2 GPIO27 |
| L298N PWM enable | ENA GPIO25; remove ENA jumper for PWM |
| LCD | SDA21/SCL22, address 0x27, 16×2 PCF8574 backpack |
| Local arming button | GPIO23 to ground, hold one second after verifying home/repacked |
| Shutdown input | GPIO19, normally closed contact to ground; opening stops motor |
| Home/end switches (optional, required for motor diagnostic) | GPIO32/GPIO33, active-low to ground |
| Battery divider (optional) | GPIO35, calculate divider so maximum battery never exceeds ADC range |
| Rain analog module (optional) | GPIO36 |
| Alignment switch (optional) | GPIO39 with external pull-up to 3.3V; aligned contact to ground |

Use common signal ground and suitable motor power/driver wiring. ESP32 GPIOs must not receive 5V. Level-shift a 5V LCD backpack's I2C lines as needed. Shutdown should also remove motor power through a suitable physical circuit; the firmware input is a status/interlock input. Check direction with the motor mechanically unloaded before coupling it to the net.

`Config.h` defaults to `WATER_CALIBRATED=false` and optional sensors disabled. Measure dry/full ADC values and corresponding depth, then set the calibration values and flag. The ADC thresholds remain Normal <880, Warning 880–1999 and Danger ≥2000. Set full ADC >2000, dry ADC <880 and depth >0 to ≤20 cm. Calibrate battery divider/empty/full voltage and rain dry/wet values before enabling those readouts. Enable both limits and alignment for the corresponding feedback. Without limits, position is explicitly a timed estimate, and a full motor diagnostic will not pass.

## Provision and compile

1. Create a dedicated non-admin Firebase Auth producer and an approved mobile viewer. Set `deviceLinks/<viewerUid>` to the producer UID through Firebase Console; deploy the included rules.
2. Copy `FloodRescueNetESP32/secrets.example.h` to `secrets.h` locally. Fill Wi-Fi, API key, database URL (no trailing slash), producer email/password and viewer UID. Never share the populated file. An API key alone is not producer authorization.
3. Install ESP32 core 3.0.7 using the Espressif board manager URL and ArduinoJson 7.4.2. Select ESP32 Dev Module. CLI equivalent:

```sh
arduino-cli core update-index --additional-urls https://espressif.github.io/arduino-esp32/package_esp32_index.json
arduino-cli core install esp32:esp32@3.0.7 --additional-urls https://espressif.github.io/arduino-esp32/package_esp32_index.json
arduino-cli lib install ArduinoJson@7.4.2
arduino-cli compile --fqbn esp32:esp32:esp32 firmware/FloodRescueNetESP32
arduino-cli upload --fqbn esp32:esp32:esp32 --port YOUR_PORT firmware/FloodRescueNetESP32
```

4. Power up with motor unloaded. Verify shutdown state, sensor calibration and LCD, then hold the arm button. In automatic mode Danger starts deployment immediately after arming. Use manual mode for controlled commissioning.
5. Sign in as the viewer and select ESP32 in the app. Follow `ACCEPTANCE_CHECKLIST.md` before attaching a loaded mechanism.

Compilation used placeholder credentials only. No board was flashed. The included root CA bundle came from Google Trust Services; TLS certificate checks are enabled and the clock must synchronize before network requests.

## Behavior

Local control runs separately from blocking network I/O. Automatic forward motion lasts three seconds (or until end feedback), no duplicate deployment occurs while deployed, and retraction starts after five uninterrupted seconds below 880. Warning/Danger resets that delay. Retraction lasts three seconds or stops at home feedback. Manual mode disables automatic deployment/retraction; manual deployment accepts 1–10 seconds and 10–100% PWM.

A diagnostic requires Ready, calibrated sensor configuration, LCD response and both limit switches. It drives forward at 40% for 400 ms, pauses 100 ms and returns at 40% for up to 800 ms. Passing requires leaving and returning to the home switch. A timeout or open interlock faults the controller. This proves the tested feedback sequence, not loaded rescue-net performance, sensor accuracy or mechanical integrity. The app requests confirmation before this movement.

Firebase requests are valid for at most 35 seconds, checked for owner/target, remembered before enqueue and never intentionally replayed after reboot. Conditional acknowledgement writes cannot overwrite a newer request. Reporting is best effort: a power failure after actuation may leave an unconfirmed outcome, and queue loss is counted. Never infer physical completion solely from command acceptance.
