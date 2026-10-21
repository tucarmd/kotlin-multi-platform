# TASK — Scanner: select a row + shared Connect/Disconnect + Device information

> Platforms: Android (Kotlin/Compose) + iOS (Swift/SwiftUI) · Created: 2026-09-22 · Code folders: `native-device-monitor/android/…/ui/scanner`, `native-device-monitor/android/…/nativecore/bluetooth`, `native-device-monitor/ios/NativeDeviceMonitor/UI/Scanner`, `native-device-monitor/ios/NativeDeviceMonitor/NativeCore/Bluetooth`
> ⚠️ **Design change 22/09:** dropped the per-row Connect button (nRF style) → per the Learning Plan §4 mockup: tap a row = select, one **Connect / Disconnect** pair under the list. The file name `…-row-connect` is kept so links don't break.

## 1. Requirement (verbatim, translated)

> Next — per-row Connect button (nRF Scanner style) + device information: continue with this part to build the connect feature

Design source: [README-HOW-TO-IMPLEMENT.md §8.1 + §8.2](../README-HOW-TO-IMPLEMENT.md#8-next-implementation-plan), referencing nRF Connect's Scanner tab.

## 2. Acceptance criteria

- AC1 — Tap a row = select (light-blue background, bold text) → **Connect** → spinner → `CONNECTED` tag on the row + green dot `Connected to <name>` → **Disconnect** → grey `Disconnected`.
- AC2 — The selected device advertises `isConnectable == false` → Connect grey, can't be tapped. Missing permission / radio off → Connect grey.
- AC3 — Connected to device A, select device B → **Connect** → A is dropped, B `Connecting…`; A's late disconnect callback does **not** overwrite B's state.
- AC4 — Tap the selected row again = show that row's advertising details (tap again = collapse).
- AC5 — Device information: 3-level signal bars colored by RSSI · vendor name next to the company id · standard service name next to the UUID · 🤖 device type + address type · last seen + packet count · a row with no packet for > 5 s while scanning fades out.
- AC6 — Pure rules (`matchesName`, `scannerButtonsOf`, `BleSignal.of`, `BluetoothLookup`, `isStale`) have unit tests with the same cases on both platforms, passing.

**Out of scope:**
- GATT after connecting (MTU, discover services, read Device Information `2A29/2A24/2A26`) — Day 3, done 24/09 in [task 003](003-gatt-read-notify.md).
- A separate Device detail screen / Navigation.
- Multiple simultaneous connections (Day 2 keeps only 1), filtering by RSSI / connectable, RAW/MORE buttons.
- Cancelling while `Connecting…` (waits for the timeout: Android ~30 s, iOS 15 s).

## 3. Design: UI separated from logic

```
 ScannerScreen / ScannerView  ── reads devices, connection, connectedAddress/Id ──▶ draws cards
        │  tap a row = select · tap again = details · [Connect] / [Disconnect]
        ▼
 matchesName(name, query) ──▶ list  ·  scannerButtonsOf(selected?, connectable?, connected device?, state, ready) ──▶ ScannerButtons
        │
        ▼
 BluetoothClientService.connect()/disconnect()  (same API, only added: ignore the old device's callbacks)
        │
        ▼
 BluetoothLookup (company / service name) · BleSignal (signal bars) · isStale (last seen)
```

| Layer | 🤖 File | 🍎 File | Contents |
|---|---|---|---|
| Screen rules | `ui/scanner/ScannerActions.kt` | `UI/Scanner/ScannerActions.swift` | `matchesName(name, query)` + `scannerButtonsOf(...)` → `ScannerButtons` — pure functions |
| Lookups | `nativecore/bluetooth/BluetoothLookup.kt` | `NativeCore/Bluetooth/BluetoothLookup.swift` | `companyName`, `serviceName`, `BleSignal`, `isStale` |
| Model | `BluetoothDeviceInfo.kt` | `BluetoothDeviceInfo.swift` | added `lastSeen`, `deviceType`, `addressType` (🤖) |
| Service | `BluetoothClientService.kt` | `BluetoothClientService.swift` | switch device while connected, ignore the old device's callbacks |
| UI | `ScannerScreen.kt` | `ScannerView.swift` | §4 mockup layout, no business `if/else` |
| Colors | `ui/theme/AppColor.kt` | `UI/Theme/AppColor.swift` | added `terminalBackground`, `terminalText` |

## 4. Plan

1. Logic: `ScannerActions` (replaces `RowAction`), `BluetoothLookup`, `BleSignal`, `isStale` + model + service
2. Logic tests (🤖 JUnit4 · 🍎 Swift Testing) — passing
3. UI: §4 mockup layout (Device Name box, row selection, shared Connect/Disconnect, Received Data)
4. Build both platforms
5. Run for real + screenshots — 🤖 A32 ✅ connect/disconnect a dongle; device switch (AC3) not tested yet · 🍎 Simulator ✅ UI, real iPhone 📝 planned
6. Update docs (§6 → done, test table §5, "Explained for beginners")

## 5. State — work log

| When | Done | Result / error | Next step |
|---|---|---|---|
| 2026-09-22 | created TASK.md | — | step 1 |
| 2026-09-22 | switched to the §4 mockup layout: `ScannerActions` (replaces `ScannerRowAction`), Device Name box filters by name, shared Connect/Disconnect, Received Data placeholder | 🤖 13+4 tests pass, assembleDebug OK · 🍎 17 tests pass. **Coded, NOT yet run on a real device** | step 5: run on Samsung A32 + iPhone with the nRF Advertiser |
| 2026-09-22 | 🤖 ran for real on Samsung A32 (SM-A325F) with dongle `CMD-123354324069` | typed `cmd` → `1 of 165` · select a row → Connect → `CONNECTED` + green dot → Disconnect → `Disconnected (status=0)` ✅. Fixed: `PanelButton` lost its background + border when enabled → switched to `Surface(onClick)` | 🍎 run on iPhone |
| 2026-09-22 | 🤖 logic + UI + 13 tests | tests pass, assembleDebug OK | iOS |
| 2026-09-22 | 🍎 logic + UI + test target `NativeDeviceMonitorTests` (xcodegen) | 13 tests pass | run on the A32 |
| 2026-09-22 | 🤖 installed on the A32, real scan (174 devices), tap a card to expand | UI correct: signal bars, grey N/A, `0x004C Apple` | user tests CONNECT with nRF (AC1, AC3) |

## 6. Verification results

| Command | Result |
|---|---|
| `cd native-device-monitor/android && ./gradlew :app:testDebugUnitTest` | ✅ 17 tests (ScannerActionsTest 13 · BluetoothLookupTest 4), 0 failures — before adding the filters in [task 002](002-scanner-hide-filters.md) |
| `cd native-device-monitor/android && ./gradlew :app:assembleDebug` | ✅ BUILD SUCCESSFUL |
| `xcodebuild test -project native-device-monitor/ios/NativeDeviceMonitor.xcodeproj -scheme NativeDeviceMonitor -destination 'platform=iOS Simulator,name=iPhone 17' CODE_SIGNING_ALLOWED=NO` | ✅ 17 tests in 2 suites passed · TEST SUCCEEDED |
| Samsung A32 (Android, `RF8R6037LPB`) — Scan | ✅ list of 174 devices, colored signal bars, non-connectable beacons show grey `N/A`, the `CMD-…` dongle shows `CONNECT` |
| Samsung A32 — tap a card | ✅ expands: Connectable · TX · Interval · Last seen · Device type · Address type `n/a (Android 15+)` · Bond · Flags · Manufacturer `0x004C Apple` · Raw |
| Samsung A32 — CONNECT / DISCONNECT / device switch | ⏳ waiting for a test with the nRF Advertiser (cases 8, 9, 13, 14, 16 in README-HOW-TO-IMPLEMENT §7.2) |
| Real iPhone | ⏳ not run yet |

## 7. Explained for beginners

The full version is in [README-HOW-TO-IMPLEMENT §10](../README-HOW-TO-IMPLEMENT.md#10-explained-for-beginners). Summary:

| Code | What it does | Why | Reference |
|---|---|---|---|
| `scannerButtonsOf()` | enabled/disabled/spinner for the Connect · Disconnect pair | rules split out of the UI → unit testable, both platforms share the cases | [UI layer](https://developer.android.com/topic/architecture/ui-layer) |
| 🤖 `g !== gatt` · 🍎 `isCurrent(peripheral)` | ignore the old device's callbacks when switching | BLE callbacks arrive late; without filtering they overwrite the new device's state | [onConnectionStateChange](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback) |
| 🍎 `order: [UUID]` | the list keeps first-seen order | sorting by RSSI makes rows jump → wrong button tapped | — |
| `LaunchedEffect` 1 s · `TimelineView(.periodic)` | a clock redrawing every second | a silent device = no new state = no redraw → the card never fades | [TimelineView](https://developer.apple.com/documentation/swiftui/timelineview) |
| `BluetoothLookup` | vendor names, service names, signal thresholds, 5 s stale | static lookup tables, no network; 5 s because each scan only lasts 10 s | [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/) |

## 8. Delivery

- Code: 🤖 `nativecore/bluetooth/BluetoothLookup.kt` (new) · `ui/scanner/ScannerActions.kt` (new) · `ScannerScreen.kt` · `ScannerStrings.kt` · `BluetoothClientService.kt` · `BluetoothDeviceInfo.kt` — 🍎 the files with the same names under `native-device-monitor/ios/NativeDeviceMonitor/`
- Tests: 🤖 `native-device-monitor/android/app/src/test/…/ScannerActionsTest.kt`, `BluetoothLookupTest.kt` · 🍎 `native-device-monitor/ios/NativeDeviceMonitorTests/`
- Screenshots: taken with `bash scripts/screenshot-android.sh <name>` → `docs/assets/` (waiting for the user after testing CONNECT)
