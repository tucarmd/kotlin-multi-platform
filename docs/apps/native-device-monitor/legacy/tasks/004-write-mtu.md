# TASK — Write characteristic + MTU

> Platforms: Android (Kotlin/Compose) + iOS (Swift/SwiftUI) · Created: 2026-09-24 · Code folders: `native-device-monitor/android/…/nativecore/bluetooth`, `native-device-monitor/android/…/ui/scanner`, `native-device-monitor/ios/NativeDeviceMonitor/NativeCore/Bluetooth`, `native-device-monitor/ios/NativeDeviceMonitor/UI/Scanner`

## 1. Requirement

The app can already **listen** to the peripheral (Read + Notify, task 003) but can't **talk** yet. This task adds 2 things:

1. **Write characteristic** — send bytes to a characteristic with the `WRITE` or `WRITE NO RESP` property.
   - The user picks the target characteristic, types the content as **text** (with the `\r` escape) or **hex**, previews the bytes, then taps **Send**.
   - The main reason: the CarMD dongle (ELM327, UART port `FFE0`/`FFE1`) only returns data via notify **after** receiving a command such as `ATZ\r`. Without Write, enabling Notify on `FFE1` shows nothing.
2. **MTU** — know and display the connection's maximum packet size, block payloads over the limit.
   - 🤖 Android requests MTU 247 itself after discovery (`requestMtu`, through the `gattOps` queue).
   - 🍎 iOS has no API to request it — only reads the value iOS negotiated itself (`maximumWriteValueLength`).
   - Why: the default MTU of 23 → each packet carries only 20 bytes of data; longer packets get truncated or rejected.

Source: [README-HOW-TO-IMPLEMENT.md §8.4](../README-HOW-TO-IMPLEMENT.md#84-write-mtu-task-004) (previously items 1 + 2 of the "After Day 4" list).

## 2. Acceptance criteria

- AC1 — A characteristic with `WRITE` / `WRITE NO RESP` has a **Write** button; tapping it selects it as the write target. By default the app picks the **write target** itself (`FFE1` → Nordic UART RX `6E400002-…` → the first writable one), marked ★.
- AC2 — The *Write* block has an input box + **Text / Hex** mode. Text: `ATZ\r` (type `\r` `\n` `\t` as escapes) → `41 54 5A 0D`. Hex: `41 54 5A 0D`, `41545A0D`, `0x41,0x54` are all accepted. A preview of the bytes to send; invalid input → a red line, *Send* disabled.
- AC3 — Tap **Send** → writes to the peripheral (with WRITE → *with response*, only NO RESP → *without response*); *Received Data* gets a line `WRITE FFE1 41 54 5A 0D → "ATZ\r"`. Write errors (e.g. status=3 WRITE_NOT_PERMITTED) show in red.
- AC4 — CarMD dongle: enable Notify `FFE1` → Send `ATZ\r` → a NOTIFY reply line (`ELM327 …`) shows in *Received Data*.
- AC5 — **MTU**: 🤖 after discovery `requestMtu(247)` through the queue; 🍎 reads `maximumWriteValueLength(.withoutResponse) + 3`. The GATT block shows `MTU 247 · max 244 bytes per packet`; payloads over the limit are blocked before sending. Disconnect → back to `23`.
- AC6 — Unit tests with the same cases 🤖🍎 for `fromHex`, `fromText`, `planWrite`, `writeTargetOf`, `maxPayloadOf`.

AC1–AC5 need a real device + a peripheral (nRF Connect *GATT server*, an ESP32 or a `CMD-…` dongle).

**Out of scope:** auto-reconnect + resubscribe · chunking payloads longer than the MTU · a flow-control queue for write-without-response (`canSendWriteWithoutResponse` / bursts of `onCharacteristicWrite`) · storing a history of sent commands · a separate Device detail screen.

## 3. Design

```
 [Write] on a characteristic row ─► pick the target (default writeTargetOf ★)
 input + Text/Hex ─► planWrite(input, asHex, properties, maxPayloadOf(mtu))    ← pure function, testable
                        ├─ Invalid("…")  → red text, Send disabled
                        └─ Ok(bytes, withResponse) → preview "41 54 5A 0D (4 bytes)"
 [Send] ─► service.write(chr, bytes, withResponse)
     🤖 gattOps.enqueue ─► writeCharacteristic ─► onCharacteristicWrite ─► received += GattValue(WRITE)
     🍎 writeValue(.withResponse)    ─► didWriteValueFor ─► received += GattValue(WRITE)
        writeValue(.withoutResponse) ─► (no callback) ─► received += GattValue(WRITE) right away
 the peripheral answers ─► notify ─► received += GattValue(NOTIFY)   (already in Day 4)

 MTU: 🤖 discovery done ─► gattOps "Request MTU 247" ─► onMtuChanged(mtu) ─► mtu
      🍎 connect / discovery done ─► maximumWriteValueLength(.withoutResponse) + 3 ─► mtu
      disconnect ─► mtu = 23
```

| Role | 🤖 Android | 🍎 iOS |
|---|---|---|
| Pure functions `fromHex` / `fromText` | `BluetoothDecoder.kt` | `BluetoothDecoder.swift` |
| `WritePlan` · `planWrite` · `writeTargetOf` · `maxPayloadOf` | `GattModel.kt` | `GattModel.swift` |
| Write + MTU | `BluetoothClientService.kt` (`write`, `onCharacteristicWrite`, `requestMtu`, `onMtuChanged`) | `BluetoothClientService.swift` (`write`, `didWriteValueFor`, `maximumWriteValueLength`) |
| UI: Write block | `ui/scanner/GattSection.kt` (`WriteBlock`) | `UI/Scanner/GattSection.swift` (`WriteBlock`) |
| Strings | `ScannerStrings.kt` | `ScannerStrings.swift` |
| Tests | `test/…/GattModelTest.kt` | `NativeDeviceMonitorTests/GattModelTests.swift` |

## 4. Checklist

- Pure functions + tests 🤖🍎
- Service: write + MTU 🤖🍎
- UI: Write block + MTU 🤖🍎
- Build + unit tests 🤖🍎
- Docs: README (status table + test steps 13–14) · README-HOW-TO-IMPLEMENT (§8.4 Write + MTU with a diagram, §8.5 remaining, test cases 31–35, 3 *Explained for beginners* entries) · README-BLE-FLOW · peripheral-setup (step 8b) · updated every `file:line` ref that shifted
- Run on a real device (AC1–AC5) — waiting for the user

## 5. Verification results

| Command | Result |
|---|---|
| `cd native-device-monitor/android && ./gradlew assembleDebug` | ✅ BUILD SUCCESSFUL |
| `cd native-device-monitor/android && ./gradlew :app:testDebugUnitTest` | ✅ **45 tests, 0 failures** — `GattModelTest` 18 (7 new) · `ScannerActionsTest` 20 · `BluetoothLookupTest` 7 |
| `xcodebuild test -project native-device-monitor/ios/NativeDeviceMonitor.xcodeproj -scheme NativeDeviceMonitor -destination 'platform=iOS Simulator,name=iPhone 17' CODE_SIGNING_ALLOWED=NO` | ✅ **42 tests / 3 suites pass** — `GattModelTests` 18 (7 new, same cases as Android) · `ScannerActionsTests` 20 · `BluetoothLookupTests` 4 |
| Real device (AC1–AC5) | ⏳ not run yet |
