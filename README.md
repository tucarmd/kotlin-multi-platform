# Native Device Monitor — KMP

> The same app as `native-device-monitor/` in the private repo [tucarmd/native-mobile-learning](https://github.com/tucarmd/native-mobile-learning/tree/main/native-device-monitor), rebuilt as **one Kotlin codebase**
> (Kotlin Multiplatform + Compose Multiplatform) for Android and iOS. Swift is only the iOS shell (`iosApp/`).
> Plan + progress: [docs/plan/README-KMP-MIGRATION.md](docs/plan/README-KMP-MIGRATION.md)
> How it works (diagrams, function flow, SDK map): [docs/README-KMP-HOW-IT-WORKS.md](docs/README-KMP-HOW-IT-WORKS.md)

Skeleton from the official [Kotlin/KMP-App-Template](https://github.com/Kotlin/KMP-App-Template) layout
(Kotlin 2.4.10 · Compose Multiplatform 1.11.1 · AGP 9.1 · Gradle 9.6). IDs: Android `com.learn.ndmkmp`, iOS `com.learn.ndmkmp`
— installs next to the old app.

## Modules

- `shared/` — KMP library: `commonMain` (UI, models, rules), `androidMain`, `iosMain` (platform code)
- `androidApp/` — Android application, `MainActivity` calls `App()`
- `iosApp/` — Xcode project, embeds the `Shared` framework, `ContentView.swift` shows `MainViewController()`

## Run

```bash
# Android (JDK 17+), from this folder
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n com.learn.ndmkmp/.MainActivity
```

```bash
# iOS Simulator (or open iosApp/iosApp.xcodeproj in Xcode and press Run)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath iosApp/build/dd build
xcrun simctl install "iPhone 17" "iosApp/build/dd/Build/Products/Debug-iphonesimulator/Device Monitor KMP.app"
xcrun simctl launch "iPhone 17" com.learn.ndmkmp
```

## Test

```bash
./gradlew :shared:allTests   # commonTest on the Android host JVM + iOS simulator
```

## Status

| Phase | Android | iOS |
|---|:-:|:-:|
| P0 — project + hello screen from one `App()` | ✅ | ✅ |
| P1 — shared core (BLE rules, GATT decode, DeviceStatus JSON, media, permission, scanner rules) + 52 tests | ✅ | ✅ |

| Android (Pixel 8 emulator, API 34) | iOS (iPhone 17 Simulator, iOS 26.5) |
|---|---|
| <img src="docs/assets/kmp_p0_android.png" width="200"> | <img src="docs/assets/kmp_p0_ios.png" width="200"> |
