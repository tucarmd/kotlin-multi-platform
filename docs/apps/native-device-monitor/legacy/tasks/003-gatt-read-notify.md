# TASK — Day 3 + Day 4: GATT discover · read + decode · notify stream · disconnect & errors

> Platforms: Android (Kotlin/Compose) + iOS (Swift/SwiftUI) · Created: 2026-09-24 · Code folders: `native-device-monitor/android/…/nativecore/bluetooth`, `native-device-monitor/android/…/ui/scanner`, `native-device-monitor/ios/NativeDeviceMonitor/NativeCore/Bluetooth`, `native-device-monitor/ios/NativeDeviceMonitor/UI/Scanner`

## 1. Requirement (verbatim)

> **Day 3 — Wed 23 Sep — BLE discovery + read — Steps 4–6**
> Do: discover services & characteristics, identify readable/notify ones, read a characteristic, convert bytes→value
> Exercise: after connect, list services/characteristics; read one characteristic and print raw bytes + a decoded value
> Target: can locate a target characteristic and read + decode its value
>
> **Day 4 — Thu 24 Sep — BLE notifications, disconnect & errors — Steps 7–9 + full-flow test**
> Do: subscribe to a notify characteristic, route data → event → UI, handle manual + unexpected disconnect, clean up subscriptions
> Exercise: stream notification values into the UI; kill the peripheral to force an unexpected disconnect and update state
> Target: full BLE flow works scan→…→disconnect; UI reflects every state including errors

Design + explanation: [README-HOW-TO-IMPLEMENT.md §5 Day 3 · §6 Day 4](../README-HOW-TO-IMPLEMENT.md#5-day-3-gatt-discover-read-decode).

## 2. Acceptance criteria

- AC1 — After connecting the app discovers **by itself**: shows `Discovering services…` then a *service → characteristic* list, each characteristic labelled `READ / WRITE / NOTIFY / INDICATE` from the properties bitmask.
- AC2 — The app picks the **target** itself (read: `2A19` → `2A29` → `2A00` → the first readable one · notify: `FFE1` → Nordic UART TX → `2A37` → `2A19` → the first notifiable one) and marks it ★.
- AC3 — Tap **Read** → *Last read* shows `raw: 5A` + `value: 90 %` (decoded by UUID; unknown UUID → ASCII text / uint LE number / hex only); Logcat / Xcode console prints the same raw + decoded line.
- AC4 — Tap **Notify** → CCCD `2902` is enabled, the button becomes **Stop**, each notify packet becomes 1 line in *Received Data* (time · UUID · hex · decoded), at most 50 lines kept, with **Clear**.
- AC5 — Manual **Disconnect**: cancel every subscription before disconnecting, grey status `Disconnected`, empty service list, no new notify lines.
- AC6 — Turn off the peripheral / Advertiser while streaming: red status `Unexpected disconnect…`, subscriptions + services cleaned up, no crash; read/notify errors (e.g. a characteristic without a CCCD) show in red in the GATT block.
- AC7 — Unit tests with the same cases 🤖🍎 for `GattProperties.of`, `BluetoothDecoder.decode`, `readTargetOf` / `notifyTargetOf`, `appendCapped`, passing.

AC1–AC6 need a real device + a peripheral (nRF Connect *GATT server* or an ESP32). The Simulator has no Bluetooth.

**Out of scope:** write characteristic (Write button, `fromHex`) · auto-reconnect + resubscribe · MTU request · a separate Device detail screen · bonding/pairing.

## 3. Design

```
 connect ─► Connected ─► discoverServices ─► (🍎 discoverCharacteristics per service)
                              │
                              ▼
           services: [GattServiceInfo]  ─► readTargetOf / notifyTargetOf  ─► ★ in the UI
                              │
   [Read]   ─► read(svc, chr) ─► 🤖 GATT queue (1 op at a time) ─► onCharacteristicRead
                                                        🍎 didUpdateValueFor (pendingReads)
                              │                                 │
                              ▼                                 ▼
                     lastRead: GattValue{hex, decoded}   received += GattValue(READ)
   [Notify] ─► setNotify(on) ─► write CCCD 2902 ─► onDescriptorWrite / didUpdateNotificationState
                                                 └─ isNotifying = true → Stop button
            ◄─ onCharacteristicChanged / didUpdateValueFor ─► received += GattValue(NOTIFY)  (cap 50)
   Manual disconnect ─► unsubscribe every notify ─► disconnect ─► clearGattState()
   Signal lost / radio off ──────────────────────────────► clearGattState() + Error (red)
```

| Role | 🤖 Android | 🍎 iOS |
|---|---|---|
| Model + pure functions | `nativecore/bluetooth/GattModel.kt` | `NativeCore/Bluetooth/GattModel.swift` |
| Decode bytes → value | `BluetoothDecoder.decode` | `BluetoothDecoder.decode` |
| Characteristic names | `BluetoothLookup.characteristicName` | `BluetoothLookup.characteristicName` |
| Discover / read / notify / cleanup | `BluetoothClientService.kt` (`gattCallback`, the `gattOps` queue) | `BluetoothClientService.swift` (`PeripheralDelegate`) |
| UI: GATT block + Received Data | `ui/scanner/GattSection.kt` | `UI/Scanner/GattSection.swift` |
| Strings | `ScannerStrings.kt` | `ScannerStrings.swift` |
| Tests | `test/…/GattModelTest.kt` | `NativeDeviceMonitorTests/GattModelTests.swift` |

## 4. Checklist

- Model + pure functions + tests 🤖🍎
- Service: discover + read + notify + cleanup 🤖🍎
- UI: GATT block + Received Data 🤖🍎
- Build + unit tests 🤖🍎
- Docs: README + README-HOW-TO-IMPLEMENT (§5 Day 3 · §6 Day 4 · test cases 19–29 · §7.3 peripheral with GATT · Explained for beginners)
- Run on a real device (AC1–AC6) — waiting for the user

## 5. State

| Date | Work | Next step |
|---|---|---|
| 2026-09-24 | Created the task | code the model + tests |
| 2026-09-24 | Coded 🤖🍎, build OK, unit tests pass, docs updated. `xcodegen generate` to add 3 new Swift files | run cases 19–29 on a real device (README-HOW-TO-IMPLEMENT §7.2) with an nRF GATT server or ESP32 `BLE_notify`, screenshot the GATT block + Received Data |

## 6. Verification results

| Command | Result |
|---|---|
| `cd native-device-monitor/android && ./gradlew assembleDebug` | ✅ BUILD SUCCESSFUL, no errors |
| `cd native-device-monitor/android && ./gradlew :app:testDebugUnitTest` | ✅ **38 tests, 0 failures** — `GattModelTest` 11 (new) · `ScannerActionsTest` 20 · `BluetoothLookupTest` 7 |
| `xcodebuild test -project native-device-monitor/ios/NativeDeviceMonitor.xcodeproj -scheme NativeDeviceMonitor -destination 'platform=iOS Simulator,name=iPhone 17' CODE_SIGNING_ALLOWED=NO` | ✅ **35 tests / 3 suites pass** — `GattModelTests` 11 (new, same cases as Android) · `ScannerActionsTests` 20 · `BluetoothLookupTests` 4 |
| Real device (AC1–AC6) | ⏳ not run yet |
