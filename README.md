# Tasbih

Personal Android tasbih counter (sideloaded APK, not on Play Store). Counts with screen taps, volume buttons and the power button, with pocket/lock protection, statistics, daily goal, reminder, home screen widget and Quick Settings tile.

Full plan and design decisions: [PLAN.md](PLAN.md). HTML mockup: [mockup/index.html](mockup/index.html).

## Stack
Kotlin 2.1, Jetpack Compose (Material 3), Room, DataStore, AccessibilityService. minSdk 31, targetSdk 35. Languages: Uzbek (default), English.

## Build
Requires JDK 17 and the Android SDK (`local.properties` with `sdk.dir`).

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

After install, enable the accessibility service (Settings → Accessibility → Tasbih) to count with the device buttons.
