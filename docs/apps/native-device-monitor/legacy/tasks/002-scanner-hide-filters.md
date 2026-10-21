# TASK — Scanner: Hide unknown devices + Hide audio devices switches

> Platforms: Android (Kotlin/Compose) + iOS (Swift/SwiftUI) · Created: 2026-09-22 · Code folders: `native-device-monitor/android/…/ui/scanner`, `native-device-monitor/android/…/nativecore/bluetooth`, `native-device-monitor/ios/NativeDeviceMonitor/UI/Scanner`, `native-device-monitor/ios/NativeDeviceMonitor/NativeCore/Bluetooth`

## 1. Requirement (verbatim, translated)

> Add an Unknown Device filter to cut down the list · if possible also add an Audio Device filter with a toggle to turn it on/off
> i.e. hide these (because they're not BLE)

Design: [README-HOW-TO-IMPLEMENT.md §8.3](../README-HOW-TO-IMPLEMENT.md#83-filter-unknown-device-audio-device) · how headphones are recognised: [bluetooth-audio-devices.md](../bluetooth-audio-devices.md).

## 2. Acceptance criteria

- AC1 — Turn on *Hide unknown devices* → no more `Unknown device` rows; off → they come back immediately, no rescan.
- AC2 — Turn on *Hide audio devices* with AirPods / BLE headphones nearby → the headphones disappear; `CMD-Test` (nRF) and the `CMD-…` dongle **stay**.
- AC3 — The 2 switches + the Device Name box **combine** (AND); while filtering show `X of Y`; when everything is hidden show `All N devices are hidden — turn a filter off`.
- AC4 — The **selected** or **connected** device is never hidden by a switch (the Disconnect button never disappears).
- AC5 — Unit tests with the same cases 🤖🍎 for `isUnknownDevice` / `isAudioDevice` / `isDeviceShown` (🍎 skips the Appearance / device class cases), passing.

AC1, AC2 need a real device (the Simulator has no Bluetooth); AC3, AC4 are covered by unit tests.

**Out of scope:** guessing by name (`Buds`, `JBL`…) · filtering during the scan (`ScanFilter`) · remembering the switch state after closing the app.

## 3. Design

```
 devices (from BluetoothClientService)
   └─ filter { isDeviceShown(device, query, hideUnknown, hideAudio, isPinned = selected || connected) }
         ├─ matchesName(name, query)          Device Name box — applies to every device
         ├─ isPinned → always shown            switches never hide the selected / connected device
         ├─ hideUnknown && isUnknownDevice(name)
         └─ hideAudio   && isAudioDevice(advertisement)
                              ├─ 🤖 isAudioClass · appearance (0x084x/0x088x/0x094x)
                              ├─ serviceUuids ∩ {184E, 1850, 1853, 184F}   (LE Audio)
                              ├─ serviceDataUuids ∋ FE2C                     (Fast Pair)
                              └─ appleType == 0x07                           (AirPods/Beats)
```

| Layer | 🤖 File | 🍎 File | Added |
|---|---|---|---|
| Screen rules | `ui/scanner/ScannerActions.kt` | `UI/Scanner/ScannerActions.swift` | `isUnknownDevice` · `isAudioDevice` · `isDeviceShown` |
| Lookups | `nativecore/bluetooth/BluetoothLookup.kt` | `NativeCore/Bluetooth/BluetoothLookup.swift` | LE Audio UUID table, `FE2C`, Apple `0x07`; 🤖 `isAudioAppearance` |
| Decoding | `nativecore/bluetooth/BluetoothDecoder.kt` | — | `appearanceOf(bytes)` reads AD type `0x19` (🍎 CoreBluetooth doesn't expose it) |
| Model | `BluetoothDeviceInfo.kt` | `BluetoothDeviceInfo.swift` | `serviceDataUuids`, `appleType`; 🤖 also `appearance`, `isAudioClass` |
| Service | `BluetoothClientService.kt` | `BluetoothClientService.swift` | fills the fields above from the advertising packet |
| UI | `ScannerScreen.kt` · `ScannerStrings.kt` | `ScannerView.swift` · `ScannerStrings.swift` | 2 switches under *CarMD tools only*, `X of Y` counter, all-hidden message |

## 4. Plan

1. Logic + model + service on both sides
2. Unit tests on both sides — passing
3. UI with 2 switches (default OFF, not disabled while scanning since they only filter the list)
4. Build both sides + 🍎 Simulator screenshot
5. Run for real on the A32 next to real headphones → AC1, AC2 + fill in the survey table in [bluetooth-audio-devices.md §4](../bluetooth-audio-devices.md)

## 5. State — log

| When | Done | Result / error | Next step |
|---|---|---|---|
| 2026-09-22 | code on both sides + tests + docs | 🤖 27 tests pass, assembleDebug OK · 🍎 24 tests pass, Simulator build OK, 2 switches visible. Error hit: the Swift tests lacked `import Foundation` (`UUID`) → import added | step 5: run on the A32 |

## 6. Verification results

| Command | Result |
|---|---|
| `cd native-device-monitor/android && ./gradlew :app:testDebugUnitTest` | ✅ 27 tests (ScannerActionsTest 20 · BluetoothLookupTest 7), 0 failures |
| `cd native-device-monitor/android && ./gradlew :app:assembleDebug` | ✅ BUILD SUCCESSFUL |
| `xcodebuild test -project native-device-monitor/ios/NativeDeviceMonitor.xcodeproj -scheme NativeDeviceMonitor -destination 'platform=iOS Simulator,name=iPhone 17' CODE_SIGNING_ALLOWED=NO` | ✅ 24 tests in 2 suites passed · TEST SUCCEEDED |
| iPhone 17 Simulator — Scanner tab | ✅ 3 switches: CarMD tools only · Hide unknown devices · Hide audio devices (headphones, speakers) — `docs/assets/scanner_ios.png` |
