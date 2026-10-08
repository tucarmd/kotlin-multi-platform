# Native Device Monitor

> The learning-path app of [native-mobile-learning](../README.md) — the same app written twice,
> **Android (Kotlin + Jetpack Compose)** and **iOS (Swift + SwiftUI)**, with the same layers and the same file names on both platforms.
> Every week of the curriculum (Setup → Week 1 → Week 2 → Week 3 → Final) adds one more layer of features to this app.
>
> Learning roadmap: [../README.md#learning-roadmap--all-weeks](../README.md#learning-roadmap--all-weeks) ·
> What is still missing + next work: [../docs/plan/README-NEXT-PHASES.md](../docs/plan/README-NEXT-PHASES.md)

## Features

5 tabs, with the same file names and the same layers on both platforms.

- **Permissions** — check / request / handle the result for BLE + Camera, including the *denied* and *permanently denied* paths; live Bluetooth radio ON/OFF and a single *Ready to scan* answer.
- **Scanner** — start/stop scan (10 s timeout, de-duplicated), *Device Name* filter, *Hide unknown devices* / *Hide audio devices* toggles, tap a row to select it, shared **Connect / Disconnect**, live connection status.
  - **GATT** — discover every service → characteristic, label `READ / WRITE / NOTIFY / INDICATE`, ★ read / notify target, **Read** shows raw bytes + a decoded value (battery `%`, UTF-8, heart rate, ASCII, little-endian uint).
  - **Notify / Stop** writes the CCCD (`2902`); notifications stream into *Received Data* (last 50, **Clear**). Manual disconnect unsubscribes first; an unexpected disconnect / Bluetooth-off shows a red state and cleans up GATT state.
  - **Write** (Text / Hex + ELM327 presets) and **MTU** (`MTU 247 · max 244 bytes per packet`).
- **Media** — camera capture + photo picker, shows the image + info (source · name · size · MIME · dimensions), cancel handled.
- **API** — GET / POST / HTTP 500 / timeout against jsonplaceholder + httpbin; buttons locked while a request is in flight.
- **Monitor** — battery / network / Bluetooth providers, a JSONL `FileLogger` (append · read · clear, corrupt lines skipped) and a `BackgroundMonitor` (`start(interval)` · `stop()` · `getStatus()` · `getLogs()` · `clearLogs()`) running as an in-app loop, an Android **Foreground Service** / **WorkManager** job, or an iOS **BGAppRefresh** task.

> Code walkthrough (what the code does, why, how to test): [docs/README-HOW-TO-IMPLEMENT.md](../docs/README-HOW-TO-IMPLEMENT.md)
>
> BLE flow diagrams (how far it runs, which file:line each step goes through, 🤖 + 🍎): [docs/README-BLE-FLOW.md](../docs/README-BLE-FLOW.md)

| Status | Android | iOS |
|---|:-:|:-:|
| Permissions (BLE + Camera) + Bluetooth radio | ✅ | ✅ |
| Scan + connect/disconnect + live state | ✅ | ✅ |
| Scanner layout: name filter + select row + shared Connect / Disconnect — [task 001](../docs/tasks/001-scanner-row-connect.md) | ✅ | ✅ |
| Run + screenshots | ✅ | ✅ |
| Scanner filters: **Hide unknown devices** + **Hide audio devices** — [task 002](../docs/tasks/002-scanner-hide-filters.md) | ✅ | ✅ |
| GATT discover · target characteristic · read + decode — [task 003](../docs/tasks/003-gatt-read-notify.md) | ✅ | ✅ |
| Notify stream → UI · manual / unexpected disconnect · cleanup · error states — [task 003](../docs/tasks/003-gatt-read-notify.md) | ✅ | ✅ |
| Write characteristic (Text / Hex + ELM327 presets) · MTU — [task 004](../docs/tasks/004-write-mtu.md) | ✅ | ✅ |
| Media (capture + picker) · API client (GET / POST / error / timeout) — Day 10 | ✅ | ✅ |
| Week 3 — device providers · FileLogger JSONL · BackgroundMonitor · 🤖 FGS + WorkManager · 🍎 BGAppRefresh · tab Monitor — [task 005](../docs/tasks/005-week3-nativecore-background.md) | ✅ | ✅ |
| Final — E2E checklist + refactor checklist + clean build (Lint / Analyze) — [task 006](../docs/tasks/006-final-e2e-cleanup.md) | ✅ | ✅ |

## Requirements

| | Android | iOS |
|---|---|---|
| IDE | Android Studio (AGP 8.13.2) | Xcode 16+ (built with Xcode 26.5) |
| Language / UI | Kotlin 2.4 · Jetpack Compose (BOM 2026.06.01) · Material 3 | Swift 5 · SwiftUI · `@Observable` |
| Min OS | Android 8.0 (API 26) · target 36 | iOS 17.0 |
| JDK | 17+ (21 recommended) | — |
| Real BLE | Emulator has no Bluetooth → **real phone** for scan/connect | Simulator has no Bluetooth → **real iPhone** for scan/connect |
| Test peripheral | [nRF Connect for Mobile](https://www.nordicsemi.com/Products/Development-tools/nRF-Connect-for-mobile) → *Advertiser* on a second phone, or an ESP32 | same |

## Getting started

Commands below run from this folder (`native-device-monitor/`).

### Android

```bash
cd android
./gradlew assembleDebug
```

```bash
./gradlew installDebug && adb shell am start -n com.learn.nativedevicemonitor/.MainActivity
```

Or open `android/` in Android Studio and press ▶. `local.properties` (`sdk.dir=...`) is created by Android Studio.

Unit tests (pure rules — name filter, hide unknown / audio, Connect/Disconnect buttons, lookups, GATT properties / target / decode / log cap; JVM, no device):

```bash
./gradlew :app:testDebugUnitTest
```

### iOS

```bash
open ios/NativeDeviceMonitor.xcodeproj
```

Pick your iPhone, set your Team under *Signing & Capabilities*, press ▶. The `.xcodeproj` is generated from
[`ios/project.yml`](ios/project.yml) with [XcodeGen](https://github.com/yonaskolb/XcodeGen) — only needed if you add/rename files:

```bash
cd ios && xcodegen generate
```

Build from the command line (Simulator, no signing):

```bash
xcodebuild -project ios/NativeDeviceMonitor.xcodeproj -scheme NativeDeviceMonitor -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

Unit tests (Swift Testing, runs on a Simulator — same cases as Android):

```bash
xcodebuild test -project ios/NativeDeviceMonitor.xcodeproj -scheme NativeDeviceMonitor -destination 'platform=iOS Simulator,name=iPhone 17' CODE_SIGNING_ALLOWED=NO
```

## How to test (short)

Full scenario with expected results: [docs/README-HOW-TO-IMPLEMENT.md §7](../docs/README-HOW-TO-IMPLEMENT.md#7-how-to-test).

1. **Permissions tab** → *Request permission* on BLE → allow → `GRANTED`. Do the same on Camera but **deny** → `DENIED`
   (Android: deny twice → `PERMANENTLY DENIED` → button becomes *Open app settings*; iOS: one deny is final).
2. Toggle Bluetooth in Quick Settings / Control Center → *Bluetooth radio* card flips `RADIO ON` ↔ `RADIO OFF` without leaving the screen.
3. Second phone: nRF Connect → *Advertiser* → start advertising (connectable). Short Local Name (e.g. `CMD-Test`) + 16-bit service UUID; on iPhone keep nRF Connect **in the foreground** or Android won't see it — full setup + troubleshooting: [§7.1](../docs/README-HOW-TO-IMPLEMENT.md#71-set-up-phone-b-as-the-peripheral-iphone--nrf-connect).
4. **Scanner tab** → *Scan* → the peripheral appears once (no duplicates), RSSI updates, scan stops by itself after 10 s.
5. Type part of the name in *Device Name* (e.g. `cmd`) → list narrows. Tap the peripheral's row to select it → **Connect** → spinner → `CONNECTED` (green dot) → **Disconnect** → `Disconnected` (grey).
6. Turn on *Hide unknown devices* / *Hide audio devices* → unnamed rows / headphones disappear at once (no rescan); the selected or connected device always stays.
7. Stop advertising / walk away while connected → red `Unexpected disconnect…`. Turn Bluetooth off → red `Bluetooth turned off`, list cleared.

Step-by-step peripheral setup + Scan → Connect → Pair → Read → Notify checklist: [docs/peripheral-setup.md](../docs/peripheral-setup.md).

Day 3 + Day 4 need a peripheral with a real GATT server — nRF Connect *Configure GATT server* on an Android phone, or an ESP32 running the Arduino `BLE_notify` example ([§7.3](../docs/README-HOW-TO-IMPLEMENT.md#73-peripheral-with-gatt-for-day-3--day-4)):

8. **Connect** → `Discovering services…` → the **GATT** block lists every service and characteristic with its properties; one row is marked `★ target`.
9. **Read** a readable characteristic (e.g. `2A29` Manufacturer Name, `2A19` Battery Level) → *Last read* shows `raw: …` and `value: …`; Logcat / Xcode console prints `READ 2A19 raw=[5A] decoded=90 %`.
10. **Notify** a notify characteristic → button turns into a blue **Stop**; each notification becomes one line in *Received Data* (capped at 50, **Clear** empties it).
11. While streaming, **Disconnect** → `Unsubscribed 1 · disconnecting…` → grey `Disconnected`, GATT block empty, earlier lines kept.
12. While streaming, **kill the peripheral** (unplug the ESP32 / stop the GATT server) → red `Unexpected disconnect…`, GATT state cleaned up, **Connect** works again.
13. After discovery the GATT block shows `MTU 247 · max 244 bytes per packet` (Android asks for 247; iOS negotiates on its own, usually 185+).
14. **Write** block: pick a writable characteristic (★ `FFE1` on a CarMD dongle), type `ATZ\r` in Text mode (or `41 54 5A 0D` in Hex) → preview `→ 41 54 5A 0D (4 bytes)` → **Send** → a `WRITE` line in *Received Data*. On the dongle, turn **Notify** on `FFE1` first — the `ELM327 …` answer arrives as a `NOTIFY` line.

## Architecture

Two independent native apps (5 tabs: Permissions · Scanner · Media · API · Monitor) with the **same layers and the same file names** — only the platform API differs.
Dependencies go one way: `ui/` → `nativecore/`, never the other way.

```
            ┌──────────────── UI (Compose / SwiftUI) ────────────────┐
            │  Permissions tab                    Scanner tab        │
            │  PermissionScreen / PermissionView  ScannerScreen / ScannerView
            │  *Strings (all UI text, English)                       │
            └──────────┬───────────────────────────────┬─────────────┘
                       │ calls / reads state           │ calls / collects state
                       ▼                               ▼
            ┌────────────────────────── NATIVECORE (no UI import) ──────────────────────────┐
            │  PermissionService        BluetoothService           BluetoothClientService    │
            │  status 4 states          radio ON/OFF + readiness   scan · connect · state    │
            │                                                      GATT: discover · read ·   │
            │                                                      notify · cleanup          │
            └──────────┬────────────────────────┬──────────────────────────┬────────────────┘
                       ▼                        ▼                          ▼
              Android: ActivityResult /   BluetoothAdapter +         BluetoothLeScanner +
              checkSelfPermission         ACTION_STATE_CHANGED       BluetoothGatt (connectGatt)
              iOS: CBManager.authorization / CBCentralManager state / scanForPeripherals + connect
```

### Same role, different file

| Role | 🤖 Android (`android/app/src/main/java/com/learn/nativedevicemonitor/`) | 🍎 iOS (`ios/NativeDeviceMonitor/`) |
|---|---|---|
| Entry point + tabs | `MainActivity.kt` (holds permission launchers) | `NativeDeviceMonitorApp.swift` (`TabView`) |
| Permission state (4 states) | `nativecore/permission/PermissionService.kt` (+ SharedPreferences "asked" flag) | `NativeCore/Permission/PermissionService.swift` (no flag needed) |
| Permission groups per feature | `nativecore/permission/PermissionGroup.kt` (depends on API level) | `NativeCore/Permission/PermissionGroup.swift` |
| Bluetooth radio + readiness | `nativecore/bluetooth/BluetoothService.kt` (BroadcastReceiver) | `NativeCore/Bluetooth/BluetoothService.swift` (delegate) |
| Scan + connect + GATT (discover · read · notify · cleanup) | `nativecore/bluetooth/BluetoothClientService.kt` (`StateFlow`, GATT op queue) | `NativeCore/Bluetooth/BluetoothClientService.swift` (`@Observable`, `PeripheralDelegate`) |
| GATT model + pure rules (properties bitmask, target picker, log cap) | `nativecore/bluetooth/GattModel.kt` | `NativeCore/Bluetooth/GattModel.swift` |
| Device providers (battery · network · Bluetooth) | `nativecore/device/DeviceProviders.kt` + `DeviceStatus.kt` | `NativeCore/Device/DeviceProviders.swift` + `DeviceStatus.swift` |
| JSONL log | `nativecore/logging/FileLogger.kt` | `NativeCore/Logging/FileLogger.swift` (`actor`) |
| Background monitor | `nativecore/background/BackgroundMonitor.kt` + `MonitorService.kt` (FGS) + `MonitorWorker.kt` | `NativeCore/Background/BackgroundMonitor.swift` + `.backgroundTask` in `NativeDeviceMonitorApp.swift` |
| Monitor screen | `ui/monitor/MonitorScreen.kt` + `MonitorStrings.kt` | `UI/Monitor/MonitorView.swift` + `MonitorStrings.swift` |
| Bytes → value | `nativecore/bluetooth/BluetoothDecoder.kt` (`decode`) | `NativeCore/Bluetooth/BluetoothDecoder.swift` (`decode`) |
| Device / advertisement model | `nativecore/bluetooth/BluetoothDeviceInfo.kt` (id = MAC) | `NativeCore/Bluetooth/BluetoothDeviceInfo.swift` (id = per-phone UUID) |
| Name filter (CarMD2 / CMD-*) | `nativecore/bluetooth/BluetoothScanFilter.kt` | `NativeCore/Bluetooth/BluetoothScanFilter.swift` |
| Permissions screen | `ui/permission/PermissionScreen.kt` + `PermissionStrings.kt` | `UI/Permission/PermissionView.swift` + `PermissionStrings.swift` |
| Scanner screen | `ui/scanner/ScannerScreen.kt` + `ScannerStrings.kt` | `UI/Scanner/ScannerView.swift` + `ScannerStrings.swift` |
| GATT block + Received Data | `ui/scanner/GattSection.kt` | `UI/Scanner/GattSection.swift` |
| Design tokens + shared components | `ui/theme/` · `ui/common/` | `UI/Theme/` · `UI/Common/` |

### Platform differences that matter

| Topic | Android | iOS |
|---|---|---|
| BLE permissions | API 31+: `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` · API ≤ 30: `ACCESS_FINE_LOCATION` | one `NSBluetoothAlwaysUsageDescription` |
| Deny | ask again once → then *permanently denied* → Settings | one deny is final → Settings |
| Device id | MAC address, same on every phone | UUID assigned by *this* iPhone |
| Connect timeout | system gives up after ~30 s | waits forever → app sets 15 s |
| Callback thread | Binder thread → `StateFlow` | main queue (`queue: nil`) |
| Radio off while connected | may not call back → app cleans up | never calls `didDisconnect` → app cleans up |
| Discover | `discoverServices()` returns services **with** their characteristics | two rounds: `discoverServices` → `discoverCharacteristics(for:)` per service |
| GATT operations | **one at a time** — app keeps its own queue, otherwise calls return `false` and are lost | CoreBluetooth queues them |
| Enable notify | `setCharacteristicNotification` **+** write CCCD `2902` yourself | `setNotifyValue(true, for:)` does both |
| Read vs notify callback | separate: `onCharacteristicRead` / `onCharacteristicChanged` (API 33+ passes `value`, older reads `c.value`) | same `didUpdateValueFor` → app remembers pending reads |

## Folder layout

```
native-device-monitor/
├── README.md                ← this file
├── android/                 ← Gradle project (open this folder in Android Studio)
│   ├── app/src/main/java/com/learn/nativedevicemonitor/
│   │   ├── MainActivity.kt
│   │   ├── nativecore/      ← permission · bluetooth · media · network · device · logging · background (no UI imports)
│   │   └── ui/              ← permission · scanner · media · api · monitor · theme · common
│   └── app/src/test/…       ← JUnit4 unit tests
└── ios/                     ← Xcode project
    ├── project.yml          ← XcodeGen spec
    ├── NativeDeviceMonitor.xcodeproj
    ├── NativeDeviceMonitor/
    │   ├── NativeDeviceMonitorApp.swift
    │   ├── NativeCore/      ← Permission · Bluetooth · Media · Network · Device · Logging · Background (no UI imports)
    │   └── UI/              ← Permission · Scanner · Media · Api · Monitor · Theme · Common
    └── NativeDeviceMonitorTests/  ← Swift Testing unit tests
```

Dependencies go one way: `ui/` → `nativecore/`, never the other way.

## Screenshots

**iOS — real iPhone 14** (run with the CarMD Connect signing identity, 2026-10-08): Bluetooth + Camera granted, radio ON, real BLE scan (`21 of 202` with filters on), camera capture, photo picker, monitor loop on a real battery.

| Permissions | Scanner — real BLE scan | Media — capture | Media — library | API | Monitor | Monitor — BGAppRefresh + logs |
|---|---|---|---|---|---|---|
| <img src="../docs/assets/iphone14_permissions.png" width="140"> | <img src="../docs/assets/iphone14_scanner.png" width="140"> | <img src="../docs/assets/iphone14_media_capture.png" width="140"> | <img src="../docs/assets/iphone14_media_library.png" width="140"> | <img src="../docs/assets/iphone14_api.png" width="140"> | <img src="../docs/assets/iphone14_monitor.png" width="140"> | <img src="../docs/assets/iphone14_monitor_bgrefresh.png" width="140"> |

**Android: Samsung A32 (real device) · iOS Simulator (iPhone 17)** — the Simulator has no Bluetooth, so the radio shows `NO BLUETOOTH` and the Scanner shows *Not ready*.

| Permissions (Android) | Scanner (Android) | Permissions (iOS Simulator) | Scanner (iOS Simulator) |
|---|---|---|---|
| <img src="../docs/assets/permissions_android.png" width="200"> | <img src="../docs/assets/scanner_android.png" width="200"> | <img src="../docs/assets/permissions_ios.png" width="200"> | <img src="../docs/assets/scanner_ios.png" width="200"> |

| Monitor (Android emulator) | Monitor logs — FGS in background (Android) | Monitor (iOS Simulator) |
|---|---|---|
| <img src="../docs/assets/monitor_android.png" width="200"> | <img src="../docs/assets/monitor_logs_android.png" width="200"> | <img src="../docs/assets/monitor_ios.png" width="200"> |

Retake: Android → open the screen on the phone, `bash ../scripts/screenshot-android.sh <permissions|scanner>` (saves to `../docs/assets/`) · iOS Simulator → build + launch, then `xcrun simctl launch "iPhone 17" com.learn.NativeDeviceMonitor -openScanner` and `xcrun simctl io "iPhone 17" screenshot ../docs/assets/scanner_ios.png`.

## Docs

- Code walkthrough + how to test: [../docs/README-HOW-TO-IMPLEMENT.md](../docs/README-HOW-TO-IMPLEMENT.md)
- BLE flow diagrams (file:line for every step): [../docs/README-BLE-FLOW.md](../docs/README-BLE-FLOW.md)
- Test peripheral setup (nRF Connect / ESP32): [../docs/peripheral-setup.md](../docs/peripheral-setup.md)
- How *Hide audio devices* recognises headphones: [../docs/bluetooth-audio-devices.md](../docs/bluetooth-audio-devices.md)
- Feature tasks 001–006 (requirement, AC, test results): [../docs/tasks/](../docs/tasks/)

## References

- UI reference for the scanner list: **nRF Connect for Mobile → Scanner tab** (Nordic Semiconductor).
- Android: [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions) · [Find BLE devices](https://developer.android.com/develop/connectivity/bluetooth/ble/find-ble-devices) · [Connect to a GATT server](https://developer.android.com/develop/connectivity/bluetooth/ble/connect-gatt-server) · [Transfer BLE data](https://developer.android.com/develop/connectivity/bluetooth/ble/transfer-ble-data)
- iOS: [Core Bluetooth](https://developer.apple.com/documentation/corebluetooth) · [CBCentralManager](https://developer.apple.com/documentation/corebluetooth/cbcentralmanager) · [CBPeripheral](https://developer.apple.com/documentation/corebluetooth/cbperipheral) · [NSBluetoothAlwaysUsageDescription](https://developer.apple.com/documentation/bundleresources/information-property-list/nsbluetoothalwaysusagedescription)
