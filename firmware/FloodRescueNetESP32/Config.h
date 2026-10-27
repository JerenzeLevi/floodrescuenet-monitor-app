#pragma once
// Classic dual-core ESP32 Dev Module; the original prototype pin map is retained.
constexpr int WATER_PIN=34, BUZZER_PIN=18, IN1_PIN=26, IN2_PIN=27, ENA_PIN=25;
constexpr int ARM_PIN=23, ESTOP_PIN=19;
// Optional peripherals. Set each enabled flag only after wiring and checking it.
constexpr bool USE_LIMITS=false, USE_BATTERY=false, USE_RAIN=false, USE_ALIGNMENT=false;
constexpr int HOME_LIMIT_PIN=32, END_LIMIT_PIN=33, BATTERY_PIN=35, RAIN_PIN=36, ALIGNMENT_PIN=39;
constexpr bool WATER_CALIBRATED=false; // Set true only after measuring the values below.
constexpr int DRY_ADC=0, FULL_ADC=4095;
constexpr float FULL_DEPTH_CM=20.0f;
constexpr float BATTERY_DIVIDER=3.0f, BATTERY_EMPTY_V=6.0f, BATTERY_FULL_V=8.4f;
constexpr int RAIN_DRY_ADC=4095, RAIN_WET_ADC=0;
constexpr char UNIT_ID[]="ESP32-01";
constexpr char DEFAULT_LOCATION[]="Prototype test rig";
// With limit switches, a missing endpoint is a fault. Without them, state is a timed estimate.
// GPIO19: normally-closed shutdown circuit to GND; open circuit stops movement.
// GPIO23: momentary arming button to GND, hold for 1 second after manually repacking.

static_assert(FULL_ADC>2000 && DRY_ADC<880 && FULL_ADC>DRY_ADC && FULL_DEPTH_CM>0 && FULL_DEPTH_CM<=20,"Invalid water calibration range");
static_assert(!USE_BATTERY || (BATTERY_DIVIDER>0 && BATTERY_FULL_V>BATTERY_EMPTY_V),"Invalid battery calibration");
static_assert(!USE_RAIN || RAIN_WET_ADC!=RAIN_DRY_ADC,"Invalid rain calibration");
