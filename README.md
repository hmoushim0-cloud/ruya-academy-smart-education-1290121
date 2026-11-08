# أكاديمية رؤية للتعليم الذكي والتدريب اللغوي (`ruya-academy-smart-education-1290121`)
> منظومة smart_education المتكاملة مع ربط Smart Stylus Digitizer 4096-Level + BLE Beacon

- **Project ID**: `#1290121`
- **Commercial Archetype**: `smart_education`
- **Bound Hardware Peripheral**: `Smart Stylus Digitizer 4096-Level + BLE Beacon`
- **Package Name**: `sa.factory.ruyaacademysmarted`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

---

## [Sidekick Autonomous Update]

### 1. Executive Architecture Overhaul
- **SmartStylusDigitizerProtocolEngine:**
  - High-precision HID digitizer protocol engine supporting 4096 discrete pressure levels and dual tilt angles (-60 to +60 degrees).
  - Palm rejection filter rejecting touch contacts exceeding 12.0 mm2 contact area or missing active pen proximity.
  - Quadratic Bezier midpoint smoothing and nib angle modulation for authentic Arabic calligraphy chisel strokes.
  - Arabic grammatical diacritic stroke classifier detecting Fathah, Kasrah, Dammah, and Sukun marks.
  - 5 operational stylus modes: PEN_FINE, CALLIGRAPHY_CHISEL, HIGHLIGHTER, ERASER, and LASER_POINTER.

- **BleClassroomAttendanceEngine:**
  - Micro-fencing classroom attendance engine based on Bluetooth Low Energy iBeacon / Eddystone signal analysis.
  - Log-distance path loss distance estimation using -59 dBm reference power at 1 meter.
  - 4 proximity zones: IMMEDIATE (&lt; 0.6m), NEAR (&lt; 2.8m), FAR (&lt; 6.0m), and OUT_OF_RANGE.
  - Continuous dwell-time verification requiring 60+ seconds presence inside classroom before confirming attendance.
  - SHA-256 rolling cryptographic signature verification preventing beacon cloning and replay attacks.

- **Hardware Driver & UI Modernization:**
  - Bound dual protocol engines into `HardwareSensorDriver.kt` emitting live telemetry frames reporting active stroke counts, pressure level resolution, and classroom roll-call metrics into Jetpack Compose `StateFlow`.
  - Modernized UI dividers to Material3 `HorizontalDivider` and resolved string template syntax bugs.
  - Cleaned Arabic typography, application manifests, and verified 100% Zero-Emoji invariant.
  - Established unit test suite under `app/src/test/java` with 10 comprehensive regression tests (100% assertions green).

---

### 2. Build, Run, and Test Instructions

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Run cloud test harness verification
python3 /root/capture_harness_screens.py --repo-dir "/root/sidekick-workspace/ruya-academy-smart-education-1290121" --output /tmp/sidekick_shot.png
```
