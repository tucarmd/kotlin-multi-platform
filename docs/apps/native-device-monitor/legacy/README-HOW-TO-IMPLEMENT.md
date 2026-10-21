[← README](index.md)

# How to implement — native-device-monitor (Day 1 → Day 4 + Write/MTU)

> A guide for beginners: **what the code does · why it's written that way · how to test it · what's next**.
> No long code dumps — every item points straight at file:line so you can open it and read.
> Want to see **the whole flow as diagrams** first (how far it runs, which step lives in which file): [README-BLE-FLOW.md](README-BLE-FLOW.md).
> Numbering: the BLE "Day 1 → Day 4" in this document = **Day 06 → Day 09** of the curriculum (Week 2). See the [roadmap](index.md).
> Status: 🟡 **24/09 (afternoon) — Write characteristic + MTU coded 🤖🍎 ([§8.4](#84-write-mtu-task-004), [task 004](tasks/004-write-mtu.md)), build + unit tests OK (🤖 45 · 🍎 42), NOT yet run on a real device.**
> Morning 24/09: added [§6.0 How notify works](#60-how-notify-works-read-before-61) + the Device Name box hides the keyboard by itself (case 30). **Day 3 (discover + read + decode) + Day 4 (notify stream + cleanup on disconnect) coded 🤖🍎 — [task 003](tasks/003-gatt-read-notify.md), build + unit tests OK on both sides (🤖 38 · 🍎 35), NOT yet run on a real device.**
> Before that (22/09): Scanner layout — [task 001](tasks/001-scanner-row-connect.md); the 2 *Hide unknown / Hide audio devices* switches — [task 002](tasks/002-scanner-hide-filters.md) / §8.3; 🤖 Day 1–2 ran for real on a Samsung A32, 🍎 Simulator only.

## Contents

1. [Goal](#1-goal)
2. [Overview diagram](#2-overview-diagram)
3. [Day 1 — Permissions + BLE radio](#3-day-1-permissions-ble-radio)
4. [Day 2 — Scan + connect](#4-day-2-scan-connect)
5. [Day 3 — GATT discover + read + decode](#5-day-3-gatt-discover-read-decode)
6. [Day 4 — Notify + disconnect + errors](#6-day-4-notify-disconnect-errors)
7. [How to test](#7-how-to-test)
8. [Next — implementation plan](#8-next-implementation-plan)
9. [Note: BLE object model (Day 1 exercise)](#9-note-ble-object-model-day-1-exercise)
10. [Explained for beginners](#10-explained-for-beginners)

---

## 1. Goal

A standalone native app (🤖 Kotlin/Compose + 🍎 Swift/SwiftUI) covering exactly the 4 days of the BLE schedule:

| Day | Do | Target |
|---|---|---|
| **1** (Mon 21/09) | check / request / handle the result of BLE + Camera permissions · concepts central/peripheral, service, characteristic, UUID, read/write/notify | handle every branch including **denied** · able to explain service/characteristic/notify ([§9](#9-note-ble-object-model-day-1-exercise)) |
| **2** (Tue 22/09) | startScan/stopScan · list devices · connect/disconnect · report connection state | scan → pick → connect → see the **live** state on both platforms |
| **3** (Wed 23/09) | discover services & characteristics · identify readable / notify ones · read 1 characteristic · bytes → value | find the **target** characteristic, read + decode its value ([§5](#5-day-3-gatt-discover-read-decode)) |
| **4** (Thu 24/09) | subscribe to notify · data → event → UI · manual + unexpected disconnect · clean up subscriptions | the whole scan → … → disconnect flow runs; the UI shows **every** state including errors ([§6](#6-day-4-notify-disconnect-errors)) |

The app has 2 BLE-related screens: **Permissions** and **Scanner**.

## 2. Overview diagram

```
 🤖 MainActivity / 🍎 NativeDeviceMonitorApp  (tabs)
  ├─ Permissions tab ─► PermissionScreen / PermissionView
  │                       ├─ reads  PermissionService.states   (4 states / group)
  │                       ├─ reads  BluetoothService.radio      (ON / OFF)
  │                       └─ shows  readiness = f(permissions, radio)  → "Ready to scan: YES/NO"
  │
  └─ Scanner tab ─────► ScannerScreen / ScannerView
                          ├─ readiness != READY → "Not ready" card + [Open Permissions]
                          ├─ [Scan] ─► BluetoothClientService.startScan(filter) ── 10 s ──► stopScan
                          │              └─ devices (deduped by MAC / UUID) ─► list
                          ├─ list = devices.filter { isDeviceShown(Device Name box, Hide unknown, Hide audio) }
                          ├─ tap a row = select · tap again = show that row's advertising packet
                          ├─ [Connect] [Disconnect] under the list ─► connect(selected device)   (buttons via scannerButtonsOf)
                          ├─ connection: Idle → Connecting → Connected(name) → Disconnected | Error(msg)
                          ├─ Day 3: Connected → discover → services ─► GattSection (★ target, [Read] → raw + value)
                          └─ Day 4: [Notify] → CCCD 2902 → received (≤ 50) ─► ReceivedDataSection · disconnect → clearGattState()

 ONE-WAY dependency:  ui/ ──► nativecore/      (nativecore never imports ui, holds no UI strings)
```

| Layer | 🤖 Android | 🍎 iOS | Role |
|---|---|---|---|
| Entry | [MainActivity.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/MainActivity.kt) | [NativeDeviceMonitorApp.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeDeviceMonitorApp.swift) | tabs; Android also holds the permission launchers + lifecycle |
| Permissions | [PermissionService.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/permission/PermissionService.kt) · [PermissionGroup.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/permission/PermissionGroup.kt) · [PermissionStatus.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/permission/PermissionStatus.kt) | [PermissionService.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Permission/PermissionService.swift) · [PermissionGroup.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Permission/PermissionGroup.swift) · [PermissionStatus.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Permission/PermissionStatus.swift) | read / request permissions, grouped by feature |
| BLE radio | [BluetoothService.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothService.kt) | [BluetoothService.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothService.swift) | live ON/OFF + `readiness()` |
| Lookups | [BluetoothLookup.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothLookup.kt) | [BluetoothLookup.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothLookup.swift) | vendor names, service names, signal bars, last seen |
| Scanner screen rules | [ScannerActions.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/ui/scanner/ScannerActions.kt) | [ScannerActions.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/UI/Scanner/ScannerActions.swift) | `matchesName` · `isDeviceShown` (2 switches) · `scannerButtonsOf` (Connect/Disconnect) — pure functions, unit tested |
| GATT model + pure rules | [GattModel.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/GattModel.kt) · [BluetoothDecoder.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothDecoder.kt) | [GattModel.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/GattModel.swift) · [BluetoothDecoder.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothDecoder.swift) | `GattProperties.of` · `readTargetOf` / `notifyTargetOf` · `decode` · `appendCapped` — unit tested |
| GATT block + Received Data | [GattSection.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/ui/scanner/GattSection.kt) | [GattSection.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/UI/Scanner/GattSection.swift) | takes state + lambdas, not the service |
| Scan/connect | [BluetoothClientService.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt) | [BluetoothClientService.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift) | 10 s scan, dedupe, connect, state · Day 3–4: discover, read, notify, cleanup on disconnect |
| Model | [BluetoothDeviceInfo.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothDeviceInfo.kt) · [BluetoothScanFilter.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothScanFilter.kt) | [BluetoothDeviceInfo.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothDeviceInfo.swift) · [BluetoothScanFilter.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothScanFilter.swift) | device + advertising packet snapshot, name filter |
| Screens | [PermissionScreen.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/ui/permission/PermissionScreen.kt) · [ScannerScreen.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/ui/scanner/ScannerScreen.kt) | [PermissionView.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/UI/Permission/PermissionView.swift) · [ScannerView.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/UI/Scanner/ScannerView.swift) | draw state, fire actions |
| UI strings | `ui/<feature>/<Feature>Strings.kt` | `UI/<Feature>/<Feature>Strings.swift` | every piece of UI text, in English |

## 3. Day 1 — Permissions + BLE radio

### 3.1 Permission request flow

```
 🤖 Android                                            🍎 iOS
 ───────────                                           ──────
 onResume ─► refreshPermissionStates()                 scenePhase .active ─► refreshAll()
   └─ statusOfGroup(): checkSelfPermission                └─ CBManager.authorization (static,
      + shouldShowRequestPermissionRationale                 does NOT trigger the dialog)
      + "asked" flag (SharedPreferences)                   └─ AVCaptureDevice.authorizationStatus
 [Request] ─► markAsked() ─► requestPermissions.launch  [Request] ─► request(group)
   └─ callback Map<perm,Boolean> ─► refresh again         ├─ camera: AVCaptureDevice.requestAccess
                                                           └─ bluetooth: create a temporary CBCentralManager (probe)
 4 states: NOT_REQUESTED · GRANTED ·                   4 states: notDetermined · granted ·
           DENIED · PERMANENTLY_DENIED                           denied (final) · restricted
 PERMANENTLY_DENIED ─► [Open app settings]             denied ─► [Open app settings]
```

| Code | What it does | Why |
|---|---|---|
| 🤖 [PermissionGroup.kt `permissionGroups()`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/permission/PermissionGroup.kt) | 3 groups: `ble_scan` · `ble_connect` · `camera`, the permission set changes with `SDK_INT` | API 31+ uses `BLUETOOTH_SCAN/CONNECT`, API ≤ 30 uses `ACCESS_FINE_LOCATION` — the UI follows features so it looks the same on every device |
| 🤖 [PermissionService.kt:57 `statusOf`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/permission/PermissionService.kt#L57) | combines 3 pieces of information into 4 states | Android **cannot** tell "never asked" from "denied twice" — you must record a flag yourself |
| 🤖 [PermissionService.kt:97 `markAsked`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/permission/PermissionService.kt#L97) | writes the flag **before** opening the dialog, synchronous `commit()` | the flag survives the app being killed while the dialog is showing |
| 🤖 [MainActivity.kt:89 `requestPermissions`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/MainActivity.kt#L89) | `RequestMultiplePermissions` registered in the Activity | the launcher must exist before the Activity is STARTED; BLE needs a set (scan + connect) |
| 🍎 [PermissionService.swift:147 `requestBluetooth`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Permission/PermissionService.swift#L147) | creates a temporary `CBCentralManager`, `await`s until the delegate reports a state | iOS has no `requestAccess` for Bluetooth — it asks the first time you touch CoreBluetooth |
| 🍎 `project.yml` `INFOPLIST_KEY_NSBluetoothAlwaysUsageDescription` / `NSCameraUsageDescription` | the explanation shown in the dialog | missing key = **crash** the moment you request, not a denied result |

### 3.2 Bluetooth radio + readiness

```
 readiness(radio, granted):  UNSUPPORTED ─► MISSING_PERMISSION ─► RADIO_OFF ─► READY
                             (nothing to do)  (→ Request)          (→ Turn on)   (can Scan)
 🤖 onStart: observeRadio() = BroadcastReceiver(ACTION_STATE_CHANGED)   ·  onStop: unregister
 🍎 BluetoothService.start(): CBCentralManager delegate — only created once permission is granted
```

- 🤖 [BluetoothService.kt:90 `observeRadio`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothService.kt#L90) — toggling Bluetooth in Quick Settings does **not** take the Activity through onPause/onResume → you must listen to the broadcast.
- 🤖 [MainActivity.kt:218 `turnOnBluetooth`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/MainActivity.kt#L218) — blocked until `BLUETOOTH_CONNECT` is granted (on API 31+ it crashes without it).
- 🍎 [BluetoothService.swift:78 `showSystemPowerAlert`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothService.swift#L78) — iOS doesn't let apps turn the radio on, it only shows a system alert with a Settings button.

## 4. Day 2 — Scan + connect

### 4.1 Flow

```
 [Scan] ─► startScan(filter)
             ├─ 🤖 bluetoothLeScanner.startScan(scanCallback)       (callback on a Binder thread)
             └─ 🍎 scanForPeripherals(nil, AllowDuplicates)          (delegate on the main queue)
        onScanResult / didDiscover  (once per advertising PACKET)
             ├─ filter.matches(name)?  no ─► hiddenCount++
             └─ found[id] = DeviceInfo(name, rssi, advertisement)    ← Map = dedupe
        devices ─► list (🤖 LazyColumn key=address · 🍎 LazyVStack ForEach id)
        10 s ─► stopScan()  (🤖 coroutine delay · 🍎 Task.sleep)

 tap a row (select) ─► [Connect] ─► connect()   (connected to A → A is dropped first, A's late callbacks are ignored)
             ├─ 🤖 connectGatt(ctx, autoConnect=false, cb, TRANSPORT_LE)  → onConnectionStateChange
             └─ 🍎 central.connect(peripheral) + 15 s timer               → didConnect / didFail / didDisconnect
 state:  Idle ─► Connecting ─► Connected(name) ─► [Disconnect] ─► Disconnected (grey)
                     └──────────────► signal lost / radio off ─► Error(msg + status) (red)
```

### 4.2 Things to remember in the code

| Code | What it does | What happens without it |
|---|---|---|
| 🤖 [BluetoothClientService.kt:217 `scanCallback`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L217) — **one** instance | `stopScan(callback)` compares by reference | a new one each time → stop doesn't match, the scan runs in the background forever |
| 🤖 `SCAN_TIMEOUT_MS = 10_000` ([:107](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L107)) | stops by itself | starting > 5 times/30 s → Android silently returns 0 results |
| 🤖 `found: LinkedHashMap` + assigning a new `List` | dedupe by MAC, the UI redraws | 200 duplicate rows / the list never updates |
| 🤖 [:362 `gattCallback`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L362) → always `gatt.close()` on DISCONNECTED | returns the client slot | ~30 slots run out → every later connect returns `status=133` |
| 🤖 [:482 `disconnect`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L482) sets `userRequestedDisconnect` **first** | tells manual from signal lost | misreports "Unexpected disconnect" |
| 🍎 [BluetoothClientService.swift `peripherals[id]`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L374) | keeps a strong ref to `CBPeripheral` | CoreBluetooth only holds it weakly → the connection is cancelled silently, stuck on `connecting` |
| 🍎 [:217 `connectTimeoutTask`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L217) | gives up after 15 s | iOS waits forever |
| 🍎 [:342 `handleStateChange`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L342) / 🤖 [:748 `onRadioOff`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L748) | radio off → clear the list + Error | no disconnect callback at all → the UI is stuck on `Connected` |
| Filter **off** by default ([ScannerScreen.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/ui/scanner/ScannerScreen.kt) / [ScannerView.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/UI/Scanner/ScannerView.swift)) | Day 2 tests with nRF / ESP32 | with `CarMD2 / CMD-*` on, the list is empty and scanning looks broken |

## 5. Day 3 — GATT discover + read + decode

> Schedule: **Wed 23/09 — Steps 4–6**. Do: discover services & characteristics, identify which ones are readable / notify, read 1 characteristic, convert bytes → value.
> Target: **find the target characteristic, read + decode its value.** Task: [tasks/003-gatt-read-notify.md](tasks/003-gatt-read-notify.md).

### 5.1 Flow

```
 Connected(name)
   │  🤖 scope.launch { g.discoverServices() }             (hop to main before calling)
   │  🍎 peripheral.delegate = bridge; discoverServices(nil)
   ▼
 🤖 onServicesDiscovered(status)              🍎 didDiscoverServices
   │  g.services → each service already has      │  each service: discoverCharacteristics(nil, for:)   ← 🍎 asks in 2 levels
   │  its characteristics                        ▼  didDiscoverCharacteristicsFor × N (spinner stops after all N)
   ▼
 services: [GattServiceInfo(uuid, [GattCharacteristicInfo(uuid, GattProperties.of(bitmask))])]
   ├─ readTargetOf()    2A19 → 2A29 → 2A00 → the first READ one          ★ in the UI
   └─ notifyTargetOf()  FFE1 → Nordic UART TX → 2A37 → 2A19 → the first NOTIFY/INDICATE one

 [Read] on a characteristic
   │  🤖 enqueue("Read …") { g.readCharacteristic(c) }    ← GATT queue: 1 op at a time
   │  🍎 pendingReads.insert(key); peripheral.readValue(for: ch)
   ▼
 🤖 onCharacteristicRead(value, status)       🍎 didUpdateValueFor (key is in pendingReads → READ)
   ▼
 GattValue(hex = toHex(bytes), decoded = BluetoothDecoder.decode(uuid, bytes), source = READ)
   ├─ lastRead  → "Last read: 2A19 Battery Level · raw: 5A · value: 90 %"
   ├─ received += value
   └─ Log  "READ 2A19 raw=[5A] decoded=90 %"      (🤖 Logcat tag BleClient · 🍎 Xcode console [BleClient])
```

### 5.2 Bytes → value (`BluetoothDecoder.decode`)

| UUID | Example bytes | Result | Rule |
|---|---|---|---|
| `2A19` Battery Level | `5A` | `90 %` | uint8 |
| `2A00` `2A24`–`2A29` (device name, model, serial, firmware, hardware, software, manufacturer) | `4E 6F 72 64 69 63` | `Nordic` | UTF-8, trailing `00` byte dropped |
| `2A37` Heart Rate | `00 48` · `01 2C 01` | `72 bpm` · `300 bpm` | bit 0 of the flags byte: 0 → uint8, 1 → uint16 LE |
| `2A01` Appearance | `41 09` | `0x0941` | uint16 LE |
| unknown, all printable | `41 54 0D` | `"AT\r"` | treated as ASCII text (UART dongles answer in text) |
| unknown, 1 / 2 / 4 bytes | `34 12` | `4660 (uint16 LE)` | **little-endian** number |
| unknown, anything else | `01 02 03` | `—` | hex only |

### 5.3 Things to remember in the code

| Code | What it does | What happens without it |
|---|---|---|
| 🤖 [BluetoothClientService.kt:378](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L378) `scope.launch { startDiscovery(g) }` | calls `discoverServices()` on main | some older Samsung devices ignore a call made directly from the Binder thread → stuck on *Discovering…* |
| 🤖 [:673 `drainOps`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L673) — the `gattOps` queue | runs reads / CCCD writes one by one, the next op waits for the previous callback | Android runs **only 1 GATT op at a time**: tap Read during a CCCD write → returns `false`, the op is lost, no callback |
| 🤖 [:404 / :411 `onCharacteristicRead`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L404) — override **both** versions | API 33+ passes `value` as a parameter · API 26–32 reads `c.value` then `copyOf()` | overriding only the new one → Android 12 and below never receive a value |
| 🍎 [BluetoothClientService.swift:464 `handleCharacteristicsDiscovered`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L464) | counts `pendingServiceCount`, draws each service as it arrives | stopping the spinner at the first service → the user thinks the device has only 1 service |
| 🍎 [:498 `handleValue`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L498) + `pendingReads` | read and notify arrive on **the same** `didUpdateValueFor` → remember what we just asked for | can't tell "we asked" from "the peripheral pushed" |
| [GattModel.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/GattModel.kt) / [GattModel.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/GattModel.swift) — `GattProperties.of(raw)` | one bitmask parser for both platforms (same spec bit values) | each side reads flags its own way → they drift, tests can't be shared |
| The UI only holds **snapshots** (`GattCharacteristicInfo`), the service holds the real handles (🤖 `charHandles` · 🍎 looks up `peripheral.services` again) | the UI never touches Bluetooth objects | `BluetoothGattCharacteristic` / `CBCharacteristic` die with the connection; holding them in the UI means tapping Read on a dead object |

## 6. Day 4 — Notify + disconnect + errors

> Schedule: **Thu 24/09 — Steps 7–9 + full-flow test**. Do: subscribe to 1 notify characteristic, route data → event → UI, handle manual + unexpected disconnect, clean up subscriptions.
> Target: **the whole scan → … → disconnect flow works; the UI reflects every state including errors.**

### 6.0 How notify works (read before 6.1)

**The idea in one sentence:** Read = the app **asks** and the peripheral answers once (pull). Notify = the app **subscribes once**, and from then on the peripheral **pushes by itself** whenever the value changes (push) — like subscribing to SMS alerts instead of phoning every second to ask. No "ask" packets, no polling lag, and the peripheral can sleep when there's nothing new.

**All 3 pieces are needed — miss any one and it's _silent_, no error:**

| # | Piece | Where it lives | Who handles it |
|---|---|---|---|
| 1 | The characteristic has the **NOTIFY** or **INDICATE** property | peripheral (declared in its GATT) | readable after discovery → the *Notify* button only shows when present ([GattSection](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/ui/scanner/GattSection.kt)) |
| 2 | Descriptor **CCCD `2902`** = the "switch" on the peripheral: `01 00` notify · `02 00` indicate · `00 00` off | peripheral | the app **writes** it → only then does the peripheral start pushing |
| 3 | The phone's OS agrees to **route packets** to the app | phone | 🤖 `setCharacteristicNotification(c, true)` · 🍎 CoreBluetooth does it itself |

**What goes over the air (ATT — the layer below GATT):**

```
 App (UI → Service)           OS Bluetooth stack                 Peripheral (dongle / ESP32 / nRF)
 ──────────────────           ──────────────────                 ─────────────────────────────────
 [Notify] tapped
   │ 🤖 setCharacteristicNotification(c,true) ─► (piece 3: "packets for c go to this app")
   │ 🤖 writeDescriptor(2902, 01 00)  ┐
   │ 🍎 setNotifyValue(true, ch) ─────┴──────► ATT Write Request  (CCCD handle = 01 00) ──►  CCCD := notify
   │                                         ◄── ATT Write Response (ok / error) ◄──────────┘
   │ 🤖 onDescriptorWrite(status) · 🍎 didUpdateNotificationStateFor(error)
   ▼ isNotifying = true → [Stop] button
                                                                  value changes (counter ticks, OBD answers…)
                                         ◄── ATT Handle Value Notification (handle, bytes) ── repeats forever
   🤖 onCharacteristicChanged(c, bytes) ◄──┘   (NO acknowledgement goes back)
   🍎 didUpdateValueFor(ch)            ◄──┘
   └─► GattValue(NOTIFY) ─► received (≤ 50) ─► ReceivedDataSection draws a new line

 [Stop] = exactly the same but writes CCCD 00 00 (🤖 write + setCharacteristicNotification(false) · 🍎 setNotifyValue(false))
```

**Notify vs Indicate** — the code picks automatically from the property ([BluetoothClientService.kt:585](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L585); 🍎 CoreBluetooth picks itself):

| | Notify (`01 00`) | Indicate (`02 00`) |
|---|---|---|
| Acknowledgement | none — fire and forget | every packet needs a *Confirmation* from the phone (the OS sends it, the app does nothing) |
| Speed | fast, several packets per connection interval | slow: the next packet waits for the previous confirmation |
| Packet loss | possible (weak signal, app too slow) | no loss (the link layer retransmits) |
| Typical use | sensor streams, ESP32 counters, the dongle's UART `FFE1` | important events (Service Changed `2A05`, commands that must arrive) |

**The subscription lifetime — tied to one _connection_, not to the app:**

- Disconnect (manual / signal lost / radio off) → the subscription is **gone**. An **unbonded** peripheral resets CCCD to `00 00`; the next connection must **discover again and tap Notify again** (auto-resubscribe not done yet — §8.5 item 1).
- A **bonded** peripheral remembers CCCD across connections → you should write `00 00` before disconnecting; the current version only turns it off on the OS side ([BluetoothClientService.kt:485](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L485)).
- `BluetoothGattCharacteristic` / `CBCharacteristic` handles die with the connection → that's why every disconnect path calls `clearGattState()` (§6.3).
- 🍎 The app goes to the background without `UIBackgroundModes = bluetooth-central` → iOS stops delivering packets; 🤖 keeps going until the process is killed (foreground service is Week 3).

**The path of one packet through the code (data → event → UI):**

| Step | 🤖 Android | 🍎 iOS |
|---|---|---|
| Packet arrives (BLE stack thread) | [:423 `onCharacteristicChanged(g, c, value)`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L423) (API 33+) · [:429](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L429) the old variant must `c.value.copyOf()` immediately | [:648 `didUpdateValueFor`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L648) |
| Tell read from notify | 2 different callbacks → not needed | **shared** callback with read → [:498 `handleValue`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L498): key in `pendingReads` = READ, otherwise NOTIFY |
| Back to the main thread | `scope.launch { }` (scope = Main) | `@MainActor` class |
| Bytes → value | `valueOf(c, bytes, NOTIFY)` → `BluetoothDecoder.decode` (§5.2) | same as 🤖 |
| Store (keep the latest 50 lines) | [:608 `handleNotified`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L608) → `appendCapped` → `_received: StateFlow` | [:518](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L518) `received = appendCapped(…)` (`@Observable`) |
| Draw | `collectAsState()` → [`ReceivedDataSection`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/ui/scanner/GattSection.kt#L312) | [`ReceivedDataSection`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/UI/Scanner/GattSection.swift#L262) redraws by itself |

**Limits to know:**

- **Default MTU is 23** → each notify packet carries at most **20 bytes** of data; anything longer the peripheral sends is truncated. Task 004: 🤖 `requestMtu(247)` after discovery, 🍎 iOS negotiates itself; the GATT block shows `MTU … · max … bytes per packet` ([§8.4](#84-write-mtu-task-004)).
- Throughput is capped by the *connection interval* (usually 15–50 ms) × packets per interval; if the ESP32 pushes faster than that, notify packets are **dropped** and nobody reports it.
- **CarMD dongle:** `FFE1` is a UART port — enabling notify succeeds but **no packets arrive** until the app **writes** a command (`ATZ\r`, `010C\r`…) into it; only then does the dongle's answer come back via notify. So for the dongle: *Notify = the ears, Write = the mouth* — Day 4 has the ears, task 004 adds the mouth ([§8.4](#84-write-mtu-task-004)).

**Tapped Notify but no lines appear — check in this order:**

1. Did the button turn into **Stop**? No → subscribe failed, read the red text (`has no CCCD` / `status=…`) — piece 2 is missing.
2. It's **Stop** but still empty → is the peripheral really changing the value? (The nRF server must press send; the dongle must receive a written command first.)
3. Logs: 🤖 `adb logcat -s BleClient` look for `NOTIFY` · 🍎 console `[BleClient]`. Log present but UI empty → a UI-layer bug; no log → packets never reach the app.
4. Just reconnected? → the old subscription is gone, tap Notify again.

### 6.1 Flow

```
 [Notify] on a characteristic with NOTIFY / INDICATE
   │  🤖 ① setCharacteristicNotification(c, true)        ← tells ANDROID to route packets to the app (local)
   │     ② enqueue { write CCCD 2902 = 01 00 (notify) | 02 00 (indicate) }   ← tells the PERIPHERAL to start pushing
   │  🍎 peripheral.setNotifyValue(true, for: ch)        ← iOS does both ① and ②
   ▼
 🤖 onDescriptorWrite(status)                 🍎 didUpdateNotificationStateFor(error)
   │  ok    → isNotifying = true → blue [Stop] button
   │  error → red gattError ("has no CCCD", "status=…"), the connection is kept
   ▼
 peripheral pushes ──► 🤖 onCharacteristicChanged(value)   🍎 didUpdateValueFor (not in pendingReads)
                     └─ GattValue(source = NOTIFY) ──► received = appendCapped(received, v)   (max 50)
                                                          └─ StateFlow / @Observable ──► ReceivedDataSection draws a new line
                                                             "12:03:04.123 NOTIFY FFE1  41 54 0D → "AT\r""

 [Disconnect] (manual)
   │  turn off notify on every enabled characteristic (🤖 setCharacteristicNotification(false) · 🍎 setNotifyValue(false))
   │  clear the queue · userRequestedDisconnect = true · lastEvent "Unsubscribed N · disconnecting…"
   ▼  disconnect / cancelPeripheralConnection
 DISCONNECTED ─► clearGattState() ─► services = [], spinner off, GATT errors cleared
                └─ connection = Disconnected (grey)
 Signal lost / peripheral off ─► clearGattState() ─► Error("Unexpected disconnect…") (red)
 Bluetooth off on phone A     ─► clearGattState() ─► Error("Bluetooth turned off")    (red)
 received + lastRead are KEPT after disconnect (still readable) · cleared on Connect to a new device or [Clear]
```

### 6.2 Every UI state must be visible

| State | What shows | Source |
|---|---|---|
| Not connected | GATT: *Connect a device to list…* · Received: *nothing yet…* | `connection` ≠ Connected, `services` empty |
| Connecting | Connect button spinner · orange Status `Connecting…` | `Connecting` |
| Discovering | *Discovering services…* spinner | `isDiscovering` |
| Discovered | `N services · M characteristics`, ★ target, Read / Notify buttons by property | `services` |
| Read done | *Last read* raw + value · 1 READ line in Received | `lastRead`, `received` |
| Notifying | blue **Stop** button · NOTIFY lines keep flowing, counter `N / 50` | `isNotifying`, `received` |
| Read / subscribe error | red text in the GATT block, Status stays green | `gattError` |
| Manual disconnect | grey Status `Disconnected`, GATT empty, old log kept | `Disconnected` |
| Unexpected disconnect | red Status `Unexpected disconnect…` (🤖 + `status=8/19/133`) | `Error` |
| Radio off | red Status `Bluetooth turned off`, device list empty | `Error` |

### 6.3 Things to remember in the code

| Code | What it does | What happens without it |
|---|---|---|
| 🤖 [BluetoothClientService.kt:569 `setNotify`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L569) — **2 steps** | enable locally + write CCCD `2902` | missing ① → the peripheral pushes but Android drops it · missing ② → the peripheral pushes nothing. Both are **silent** |
| 🤖 [:690 `writeDescriptor`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L690) | API 33+ `writeDescriptor(d, value)` · older: set `d.value` then write | the old API on 33+ still works but is deprecated; the new API on older devices → `NoSuchMethodError` crash |
| 🤖 [:594 `handleCccdWritten`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L594) | only flips `isNotifying` once the peripheral **confirms**; on error undoes step ① | the UI shows *Stop* while no packets ever arrive |
| 🍎 [BluetoothClientService.swift:521 `handleNotificationState`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L521) | takes the **real** `ch.isNotifying` from CoreBluetooth | guessing from what we requested → wrong when the characteristic has no CCCD |
| 🤖 [:482 `disconnect`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L482) · 🍎 [:227 `disconnect`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L227) | turns notify off before disconnecting | late packets slip into the log after Disconnect was tapped |
| 🤖 [:392 / :732 `clearGattState`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L732) · 🍎 [:330](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L330) — called on **every** disconnect path (manual, signal lost, radio off, device switch, connect fail) | clears services, the queue, pending CCCD writes | the dead device's service list stays → tapping Read gives a confusing `Not connected`; a stuck queue → the next connection's ops never run |
| `appendCapped(…, max = 50)` | keeps the latest 50 lines | an ESP32 pushing hundreds of packets/second → the list bloats, the UI slows down then freezes |

Limits (deliberate, see *Out of scope* in task 003): no **auto-reconnect + resubscribe** yet (§8.5). Write + MTU — the CarMD dongle only returns `FFE1` data after receiving an `ATZ\r` command — was added in task 004 ([§8.4](#84-write-mtu-task-004)).

## 7. How to test

Setup: 2 phones — phone A runs **Native Device Monitor**, phone B runs **nRF Connect → Advertiser** (add an advertising packet with *Complete Local Name*, turn on *Connectable*). Emulators/Simulators **have no Bluetooth** → only the permission part + the "unsupported/not ready" branch can be tested there.

### 7.1 Set up phone B as the peripheral (iPhone + nRF Connect)

> The short step-by-step version (nRF Connect Android GATT server · LightBlue · ESP32) + the Scan → Connect → Pair → Read → Notify checklist: **[peripheral-setup.md](peripheral-setup.md)**.

```
 iPhone B — nRF Connect                          Android A — Native Device Monitor
 Advertiser ON (app in foreground)               Scanner tab, filter OFF
   Local Name "CMD-Test" + UUID 180D   ──BLE──►  row "CMD-Test" (strongest RSSI)
```

1. iPhone B: turn Bluetooth on in **Settings** (the Control Center toggle may not fully turn it on).
2. Open **nRF Connect for Mobile** → **Advertiser** tab → `+`.
3. Use a **short** Local Name, e.g. `CMD-Test`. A name with the `CMD-` prefix is visible whether *CarMD tools only* is on or off, so it also works for case 12.
4. Add a **16-bit** Service UUID, e.g. `180D` (Heart Rate) or `180F` (Battery). Don't use a 128-bit UUID the first time.
5. Turn *Connectable* on (only case 14 turns it off) → Save → **turn on the toggle** of the advertiser.
6. **Keep nRF Connect in the foreground with the screen on** for the whole test.

Why it must be done exactly like this:

| Point | Reason |
|---|---|
| The app must be in the foreground | iOS **drops the Local Name** when the app goes to the background, and the Service UUID moves into an "overflow" area only other iPhones can read → Android sees nothing |
| Short name, 16-bit UUID | an advertising packet is only 31 bytes; a 128-bit UUID alone takes 18 bytes → the name gets truncated or pushed into the *scan response* |
| Don't filter by MAC | iPhones use a **random** address that rotates periodically → the MAC differs every test |
| The name may not be `CMD-Test` | 🤖 the code reads `device.name` first, then `scanRecord.deviceName` ([BluetoothClientService.kt:223](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L223)). If Android was ever paired with that iPhone it shows the system name ("…'s iPhone"); the first packet without a scan response shows `Unknown`. Look for the strongest RSSI |

Phone A (Android) needs:

- Bluetooth on. Android 12+ (API 31+): grant the **Nearby devices** permission. Android ≤ 11: also turn on **Location** (the system switch) + grant location permission, otherwise scanning returns 0 results with no error.
- Scanner tab: *CarMD tools only* **off** (unless the name has `CMD-`), the *Device Name* box **empty**.
- Keep the 2 phones close together.

Can't see it? Narrow it down in this order:

1. Install **nRF Connect on Android A** → Scanner, no filters → does it **see** iPhone B? No = the problem is on iPhone B or phone A's permissions/Location, not the app.
2. nRF on Android sees it but the app doesn't → send the `adb logcat -s BleClient` log (it has a `startScan filter=...` line) to check which filter is on.

> Phone B can also be Android: nRF Connect Android → **Advertiser** tab → create a packet with *Complete Local Name*. Android has no foreground restriction like iOS.

### 7.2 Test cases

| # | Case | Steps | Expected result | 🤖 | 🍎 |
|---|---|---|---|---|---|
| 1 | First-time permissions | Fresh install → Permissions tab | BLE Scan / BLE Connect / Camera (🍎 Bluetooth / Camera) are all `NOT REQUESTED`, with a *Request permission* button | ✅ | ⏳ |
| 2 | Granted | Request BLE → Allow | `GRANTED`, button disappears; radio card `RADIO ON` + `Ready to scan: YES` | ✅ | ⏳ |
| 3 | Denied | Request Camera → Don't allow | 🤖 `DENIED` + reason + Request button still there · 🍎 `DENIED` + *Open app settings* | ⏳ | ⏳ |
| 4 | Permanently denied | 🤖 Request Camera → deny a 2nd time | `PERMANENTLY DENIED` → *Open app settings* opens the right App Info | ⏳ | — |
| 5 | Live radio | Toggle Bluetooth off/on (Quick Settings / Control Center) | the chip changes immediately without leaving the screen; `Ready to scan` follows | ⏳ | ⏳ |
| 6 | Not ready | Revoke the BLE permission in Settings → come back → Scanner tab | *Not ready* card + *Open Permissions* switches tab; Scan button grey | ⏳ | ⏳ |
| 7 | Scan | Scanner tab → Scan | phone B's peripheral shows as **1 row** (name + MAC/UUID + RSSI changing); stops by itself after 10 s, `Scan finished: N devices` | ✅ | ⏳ |
| 8 | Connect | Tap phone B's row (select) → tap **Connect** | Connect shows a spinner → `CONNECTED` tag on the row + green dot `Connected to <name>`; nRF on phone B sees 1 client | ✅ | ⏳ |
| 9 | Disconnect | Tap **Disconnect** | **grey** `Disconnected`, Connect enabled again, Disconnect disabled | ✅ | ⏳ |
| 10 | Signal lost | While Connected → phone B turns off the Advertiser / Bluetooth | **red** text `Unexpected disconnect…` (🤖 with `status=8/19`) | ⏳ | ⏳ |
| 11 | Radio off | While scanning or Connected → turn off Bluetooth on phone A | red `Bluetooth turned off`, empty list, no stuck spinner | ⏳ | ⏳ |
| 12 | Filter | Turn on *CarMD tools only* → Scan (phone B named `CMD-TEST`) | only `CMD-TEST` left, plus a `N other devices hidden` line | ⏳ | ⏳ |
| 13 | Switch device | Connected to phone B → select device C → **Connect** | B is dropped, C `Connecting…`; status does **not** jump to `Disconnected` midway | ⏳ | ⏳ |
| 14 | Non-connectable | nRF on phone B turns off *Connectable* → Scan → select row B | **Connect** button grey, can't be tapped | ⏳ | ⏳ |
| 15 | Advertising details | Tap the selected row once more | shows Connectable · TX · Interval · Last seen · (🤖 type/bond/flags/raw) · named Services · Manufacturer with vendor name; tap again to collapse | ✅ | ⏳ |
| 17 | Hide unknown devices | Scan in a crowded place → turn on the switch | `Unknown device` rows disappear, counter `X of Y`; off → they come back immediately, no rescan | ⏳ | ⏳ |
| 18 | Hide audio devices | AirPods / BLE headphones nearby → turn on the switch | headphones disappear; `CMD-Test` / `CMD-…` dongle **stays**; the selected / connected device is never hidden | ⏳ | ⏳ |
| 30 | Hide keyboard from the Device Name box | Tap the box → type `cmd` → try in turn: the **Search/Return** key · tap empty space · drag the screen · tap **Scan** / tap a row / flip a switch | the keyboard **closes** every way; `cmd` stays, the list is still filtered; tapping the box again (including the 🍎 magnifier) = keyboard opens again | ⏳ | ⏳ |
| 16 | Faded card | While scanning → phone B turns off the Advertiser | after ~5 s row B fades; no further fading after the scan ends | ⏳ | ⏳ |
| **Day 3** | | *(peripheral with GATT — see [§7.3](#73-peripheral-with-gatt-for-day-3-day-4))* | | | |
| 19 | Discover | Connect phone B | `Discovering services…` → `N services · M characteristics`; each characteristic labelled `READ / WRITE / NOTIFY / INDICATE`; log `service 180A: 2A29[READ], …` | ⏳ | ⏳ |
| 20 | Target | Look at the GATT block | `Read target:` + `Notify target:` in the §5.1 priority order; the matching row has `★ target` | ⏳ | ⏳ |
| 21 | Read + decode | Tap **Read** on the target (e.g. `2A29` → vendor name, `2A19` → `%`) | *Last read* shows `raw: …` + `value: …`; 1 `READ` line in Received Data; Logcat / console prints `READ 2A29 raw=[…] decoded=…` | ⏳ | ⏳ |
| 22 | Read rejected | Read a characteristic that requires pairing / is not readable | **red** text `Read … failed, status=…` (🍎 `…: <reason>`); Status stays green `Connected` | ⏳ | ⏳ |
| **Day 4** | | | | | |
| 23 | Notify stream | Tap **Notify** on a NOTIFY characteristic (ESP32 counter / Heart Rate / nRF server sending values) | the button becomes a blue **Stop**; one `NOTIFY` line per packet with `HH:mm:ss.SSS` time; counter `N / 50` stops at 50 (old lines scroll away); **Clear** empties it | ⏳ | ⏳ |
| 24 | Subscribe error | Notify on a characteristic without a CCCD `2902` | red text `… has no CCCD` (🍎 `Subscribe … failed: …`); the button stays **Notify** | ⏳ | ⏳ |
| 25 | Stop | While streaming → **Stop** | `Notify OFF: …`, the button goes back to **Notify**, no new lines | ⏳ | ⏳ |
| 26 | Manual disconnect while streaming | While streaming → **Disconnect** | `Unsubscribed 1 · disconnecting…` → **grey** Status `Disconnected`; GATT block back to *Connect a device…*; old log **kept**, no new lines | ⏳ | ⏳ |
| 27 | Unexpected disconnect while streaming | While streaming → **kill the peripheral** (unplug the ESP32 / turn off Bluetooth on phone B / stop the GATT server) | **red** Status `Unexpected disconnect…` (🤖 `status=8` or `19`); GATT empty; no crash; **Connect** works again, new log | ⏳ | ⏳ |
| 28 | Radio off while streaming | Turn off Bluetooth on phone A | red `Bluetooth turned off`, list + GATT empty, no stuck spinner | ⏳ | ⏳ |
| 29 | **Full flow** | Scan → select → Connect → Discover → Read → Notify → Stop → Disconnect | each step shows exactly 1 state from the [§6.2](#62-every-ui-state-must-be-visible) table, no step gets stuck | ⏳ | ⏳ |
| **Write + MTU** | | *(task 004 — [§8.4](#84-write-mtu-task-004))* | | | |
| 31 | MTU | Connect phone B, wait for discovery to finish | 🤖 `MTU 247 · max 244…` (or a smaller number the peripheral returns; Logcat `onMtuChanged mtu=…`) · 🍎 `MTU 185…` or higher; after a disconnect the next connection shows `MTU 23` first, then changes | ⏳ | ⏳ |
| 32 | Write target + preview | Look at the *Write* block | `Write target:` in priority order (FFE1 → Nordic RX → the first one); type `ATZ\r` in Text → `→ 41 54 5A 0D (4 bytes)`; switch to Hex and type `41 5` → **red** text, Send dimmed | ⏳ | ⏳ |
| 33 | Write to nRF server | nRF *GATT server* with a WRITE characteristic → tap **Write** on that row → type Hex `01 02` → **Send** | line `WRITE <uuid> 01 02 → 513 (uint16 LE)`; nRF on phone B shows the new value | ⏳ | ⏳ |
| 34 | Write rejected / too long | Write to a characteristic the peripheral rejects · type 21+ characters with MTU 23 | red error `Write … failed, status=3` (🍎 `…: Writing is not permitted`) · too long is blocked before sending: `21 bytes > max 20 for MTU 23` | ⏳ | ⏳ |
| 35 | **CarMD dongle** | Connect `CMD-…` → **Notify** `FFE1` → preset `ATZ\r` → **Send** → then `010C\r` | `WRITE FFE1 41 54 5A 0D …` then **NOTIFY** `"ELM327 v…\r\r>"`; `010C` (engine running) → `"41 0C …"` | ⏳ | ⏳ |

✅ = run for real (🤖 Samsung A32, 22/09). Unit tests for the pure rules: see [task 001 §6](tasks/001-scanner-row-connect.md#6-verification-results).

Cases 19–29: coded 24/09, **not yet run on a real device**. Day 3–4 pure-rule unit tests: [task 003 §6](tasks/003-gatt-read-notify.md#6-verification-results).
Cases 31–35: coded 24/09 afternoon, **not yet run on a real device**. Unit tests: [task 004 §5](tasks/004-write-mtu.md#5-verification-results).

Logs to compare against: 🤖 `adb logcat -s BleClient MainActivity PermissionService BluetoothService` · 🍎 Xcode console filtered on `[BleClient]`.
Reset permissions to test from scratch: 🤖 `adb shell pm reset-permissions com.learn.nativedevicemonitor` + tap *Reset asked flags* · 🍎 delete and reinstall the app.

Acceptance screenshots go into `docs/assets/` (names as in the README Screenshots table):
🤖 open the right screen on the phone then run `bash scripts/screenshot-android.sh permissions` (or `scanner`, `connected`, …) → `docs/assets/<name>_android.png`.

### 7.3 Peripheral with GATT for Day 3 + Day 4

How to set up each kind: [peripheral-setup.md](peripheral-setup.md). The Day 2 nRF *Advertiser* only sends advertising packets; once you connect, what you see is **phone B's own GATT server**. Pick 1 of 3:

| Peripheral | What it offers for testing | Notes |
|---|---|---|
| **iPhone B + nRF Connect Advertiser** (as in §7.1) | `180A` Device Information: `2A29` = `Apple Inc.`, `2A24` = model → cases 19–21 | the iPhone may require pairing for some characteristics → case 22. Few usable NOTIFY characteristics → for case 23 use one of the options below |
| **Android B + nRF Connect → *Configure GATT server*** | add a sample service (e.g. Heart Rate / Battery), edit the value then press send notification | covers read, notify, Stop; stopping the server / turning off Bluetooth on B = case 27 |
| **ESP32** — the Arduino-ESP32 `BLE_notify` sample sketch | 1 NOTIFY characteristic pushing a 4-byte counter continuously → `N (uint32 LE)` | the densest stream → shows the 50 cap (case 23); unplugging = the cleanest case 27 |

CarMD dongle (`FFE0` / `FFE1`): `FFE1` can be subscribed but the dongle only returns data after receiving a command (e.g. `ATZ\r`) — it needs a **write**: turn on Notify `FFE1` → type `ATZ\r` in the *Write* block → **Send** (cases 31–35, [§8.4](#84-write-mtu-task-004)).

## 8. Next — implementation plan

### 8.1 + 8.2 ✅ Scanner layout + device information (done 22/09)

Details (AC, design, test commands, log): [tasks/001-scanner-row-connect.md](tasks/001-scanner-row-connect.md). Layout per the Learning Plan §4 mockup:

```
 Device Name  [🔍 cmd              ]
 CarMD tools only        [○]   Hide unknown devices [○]   Hide audio devices [○]
 [          Scan Bluetooth          ]
 Discovered Devices                               1 of 165
 ┌───────────────────────────────────────────────┐
 │ CMD-3C71BF… [CONNECTED]            ▂▄▆ -58 dBm │  ← tap = select · tap again = advertising details
 │ 3C:71:BF:12:34:56   (🍎: UUID assigned by the iPhone) │
 └───────────────────────────────────────────────┘
 [ Connect ] [ Disconnect ]                         ← scannerButtonsOf()
 ● Status: Connected to CMD-3C71BF…
 Received Data  ┌ 12:03:04.123 NOTIFY FFE1 … ┐   ← Day 4
```

| Done | 🤖 | 🍎 |
|---|---|---|
| Device Name box filters the list · select a row · shared Connect/Disconnect via `scannerButtonsOf()` | [ScannerActions.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/ui/scanner/ScannerActions.kt) · [ScannerScreen.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/ui/scanner/ScannerScreen.kt) | [ScannerActions.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/UI/Scanner/ScannerActions.swift) · [ScannerView.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/UI/Scanner/ScannerView.swift) |
| Tap the selected row again = show advertising details | `showDetails` | `showDetails` |
| Switching device while connected: drop the old one, **ignore the old device's late callbacks** | `gattCallback`: `g !== gatt` → ignore | `isCurrent(peripheral)` in `handleDisconnected/Connected/Failed` + the timer checks `connectedId` |
| 3-level signal bars + colors (≥ -60 · -60…-80 · < -80) | `BleSignal.of` | `BleSignal.of` |
| Vendor name from company id · standard service names (lookup tables, no network) | [BluetoothLookup.kt](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothLookup.kt) | [BluetoothLookup.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothLookup.swift) |
| Device type · address type (API 35+) | ✅ | ❌ iOS doesn't expose it |
| Last seen + packet count; the row fades **while scanning** after > 5 s without packets | `lastSeenMs` + a 1 s `LaunchedEffect` clock | `lastSeen` + `TimelineView(.periodic)` |
| The list keeps **first-seen order** (🍎 used to sort by RSSI → rows jumped around, wrong device picked) | already so (LinkedHashMap) | switched to `order: [UUID]` |
| Unit tests with the same cases on both platforms | `app/src/test/…` JUnit4 | `NativeDeviceMonitorTests/` Swift Testing |

Remaining (not done): a separate **Device detail** screen (the service list + Device Information `2A29`/`2A24`/`2A26` exist since Day 3, but live on the Scanner screen) (Navigation Compose / `NavigationStack`). Cancel while `Connecting…`: not yet, waits for the timeout (🤖 ~30 s · 🍎 15 s).

### 8.3 Filter Unknown device + Audio device

> 🟡 **Coded 22/09** 🤖🍎 — `isUnknownDevice` · `isAudioDevice` · `isDeviceShown` in `ScannerActions`, unit tests pass on both sides. **Not yet run on a real device** (cases 17, 18 in §7.2). Task: [tasks/002-scanner-hide-filters.md](tasks/002-scanner-hide-filters.md).

Purpose: scanning in a crowded place gives 150–170 devices (the A32 sees `165`), most of them **unnamed** or headphones/speakers/earbuds → **hide** them to find the dongle faster.
Headphones still end up in the list because they **do send BLE packets** (quick pairing, battery — e.g. AirPods), but audio goes over Bluetooth Classic / LE Audio; they're not BLE (GATT) devices this app needs to connect to.

```
 Device Name: [ cmd      ]
 Hide unknown devices   [●──]   ← hide devices WITHOUT a name
 Hide audio devices     [●──]   ← hide headphones / speakers / earbuds
 [ Scan Bluetooth ]
 Discovered Devices · 12 of 165 (153 hidden)
```

| | Hide unknown devices | Hide audio devices |
|---|---|---|
| Rule | `name` empty/null (both `device.name` and `scanRecord.deviceName` / 🍎 `peripheral.name` and `kCBAdvDataLocalName`) → hide | matches **1 of** the signs below → hide |
| 🤖 signs | — | `device.bluetoothClass.majorDeviceClass == AUDIO_VIDEO` (only for dual-mode / previously paired devices) · Appearance (AD `0x19`) in Audio Sink `0x084x`, Audio Source `0x088x`, Wearable Audio `0x094x` (earbud/headset/headphones) · LE Audio service UUIDs: `184E` ASCS, `1850` PACS, `1853` CAP, `184F` BASS · Google Fast Pair service data `FE2C` (Android earbuds; trackers use it too — hidden anyway, a tracker isn't a dongle) |
| 🍎 signs | — | iOS exposes **neither** device class nor Appearance → only the LE Audio service UUIDs above; AirPods/Beats: manufacturer `0x004C` Apple, type byte `0x07` (a heuristic, documented in a comment) · `FE2C` Fast Pair as on 🤖 |
| Pure function (unit tested on both sides) | `isUnknownDevice(device)` | `isAudioDevice(device)` |
| Where | `ScannerActions.kt` / `ScannerActions.swift` — filters the **list already seen** like `matchesName`, doesn't change how scanning works | same as left; Appearance / UUID code tables in `BluetoothLookup` |
| Default | OFF (nRF / ESP32 tests don't lose rows) | OFF |

Planned acceptance:

1. Turn on *Hide unknown devices* → no more `Unknown` rows; off → they come back immediately, no rescan.
2. Turn on *Hide audio devices* with AirPods / BLE headphones nearby → the headphones disappear; the nRF Advertiser `CMD-Test` and the `CMD-…` dongle **stay**.
3. The two toggles + the Device Name box **combine** (AND); the `X of Y (Z hidden)` counter is correct.
4. A **Connected** or selected device is never hidden (so the Disconnect button never disappears).
5. Unit tests with the same cases 🤖🍎 for `isUnknownDevice` / `isAudioDevice`.

Details on how headphones / speakers are recognised (which signs, what 🤖🍎 can read, the field survey to do): [bluetooth-audio-devices.md](bluetooth-audio-devices.md).

Limits to state in the UI/docs: headphones that advertise BLE **without an audio sign** (no Appearance, no LE Audio UUID) → *Hide audio devices* doesn't recognise them, they stay visible; 🍎 recognises fewer than 🤖 (no device class / Appearance). **Classic-only** headphones never show up in a BLE scan anyway.

### 8.4 Write + MTU (task 004)

🟡 **24/09 — coded 🤖🍎, build + unit tests OK, NOT yet run on a real device.** Details + criteria: [task 004](tasks/004-write-mtu.md).

```
 [Write] on a characteristic row ─► pick the write target (default writeTargetOf: FFE1 → Nordic RX 6E400002 → the first writable one ★)
 input box + [Text|Hex] + presets [ATZ\r] [ATI\r] [010C\r]
    │
    ▼  planWrite(input, asHex, properties, maxPayloadOf(mtu))      ← a PURE function, 7 test cases on both sides
    ├─ Invalid("21 bytes > max 20 for MTU 23")  → red text, Send dimmed
    └─ Ok(41 54 5A 0D, withResponse = has WRITE?) → preview "→ 41 54 5A 0D (4 bytes)"
    │
 [Send] ─► service.write(chr, bytes, withResponse)
    🤖 gattOps ─► writeCharacteristic(DEFAULT | NO_RESPONSE) ─► onCharacteristicWrite(status) ─┐
    🍎 writeValue(.withResponse)    ─► didWriteValueFor(error) ──────────────────────────────┤
       writeValue(.withoutResponse) ─► (iOS does NOT call back) ── log right away ───────────┤
                                                                                              ▼
                                            received += WRITE FFE1  41 54 5A 0D → "ATZ\r"
 ELM327 dongle runs the command ─► answers via notify FFE1 ─► received += NOTIFY FFE1  45 4C … → "ELM327 v1.5\r\r>"

 MTU
 🤖 discovery done ─► gattOps "Request MTU" ─► requestMtu(247) ─► onMtuChanged(mtu, status) ─► mtu
 🍎 didConnect + discovery done ─► maximumWriteValueLength(.withoutResponse) + 3 ─► mtu    (iOS negotiates itself)
 disconnect / switch device ─► clearGattState() ─► mtu = 23
```

| Task | 🤖 Android | 🍎 iOS |
|---|---|---|
| Send button rules | [`GattModel.kt:191` `planWrite`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/GattModel.kt#L191) | [`GattModel.swift:179` `planWrite`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/GattModel.swift#L179) |
| Send | [`:626` `write`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L626) — queued in `gattOps` like Read / CCCD | [`:280` `write`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L280) — CoreBluetooth queues itself |
| Write result | [`:640` `handleWritten`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L640) | [`:542` `handleWritten`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L542) (with response only) |
| MTU | [`:534` request](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L534) · [`:654` `handleMtuChanged`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/android/app/src/main/java/com/learn/nativedevicemonitor/nativecore/bluetooth/BluetoothClientService.kt#L654) | [`:310` `refreshMtu`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Bluetooth/BluetoothClientService.swift#L310) |

**Things to remember**
- **Pick the write type from the properties.** Has `WRITE` → *with response* (the peripheral ACKs, real errors come back). Only `WRITE NO RESP` → *without response*. 🍎 with the wrong type CoreBluetooth **drops the write** and only prints a warning; 🤖 usually returns an error status.
- **Without response doesn't mean the peripheral received it.** 🤖 `onCharacteristicWrite` still arrives but only means "pushed down the stack"; 🍎 there's no callback at all. To be sure → use with response, or wait for the answer via notify (dongle).
- **The write callback doesn't return the bytes.** You must keep them yourself (`pendingWrite` 🤖 / `pendingWrites[key]` 🍎) to print the `WRITE` line in *Received Data*.
- **🤖 MTU is a GATT op** → goes through `gattOps`. Calling `requestMtu` while another op is running = returns `false`, op lost. `onMtuChanged` can also arrive when the **peripheral** requests it → only `opDone()` when the in-flight op really is `Request MTU`.
- **🍎 you can't request an MTU.** iOS negotiates on connect (usually 185 or more); the app only reads `maximumWriteValueLength(for: .withoutResponse)` = MTU − 3. Don't read the `.withResponse` variant — it returns 512 because iOS splits "long writes" itself.
- **Longer than MTU − 3 is blocked**, no automatic splitting (see *Out of scope* in task 004). ELM327 commands are only a few bytes so it's enough.
- **ELM327 dongles need `\r` at the end of a command.** Without `0x0D` the dongle waits and never answers — that's why Text mode accepts the `\r` escape, and every preset includes `\r`.

### 8.5 After task 004 (not done yet)

1. **Reconnect + resubscribe** after the signal drops: remember the keys being notified, on reconnect discover then re-enable each one.
2. **Write CCCD = 0 before disconnecting** for bonded peripherals (currently only turned off on the phone side — §6.3).
3. A separate **Device detail** screen (Navigation Compose / `NavigationStack`) when the GATT block gets too long.
4. Split payloads longer than MTU − 3 · flow control for bursts of write-without-response (🍎 `canSendWriteWithoutResponse` / `peripheralIsReady(toSendWriteWithoutResponse:)`).

The full list of remaining work across all weeks: [plan/README-NEXT-PHASES.md](plan/README-NEXT-PHASES.md).

## 9. Note: BLE object model (Day 1 exercise)

```
  CENTRAL (this app: the phone)                 PERIPHERAL (dongle / ESP32 / nRF Advertiser)
  ─────────────────────────────                 ─────────────────────────────────────────────
  scan  ◄──────── advertising packet ────────── advertises: name, service UUID, manufacturer data
  connect ─────────────────────────────────────► GATT server
                                                 └─ Service  (UUID, e.g. 180F Battery / FFE0 dongle)
                                                     └─ Characteristic (UUID + properties)
                                                          ├─ value (bytes)
                                                          └─ Descriptor 2902 (CCCD: notify on/off)
  read   ──── ask once ───────────────────────►  value            (the central pulls)
  write  ──── send bytes ─────────────────────►  value            (with / without response)
  notify ◄─── peripheral pushes on change ─────  value            (CCCD must be enabled first)
```

- **Central / peripheral**: a role during the connection, not a kind of device. The central scans + connects; the peripheral advertises + waits.
- **Service**: a group of functionality (Battery, Heart Rate, the dongle's UART port). **Characteristic**: one data slot inside a service, whose *properties* say READ / WRITE / NOTIFY.
- **UUID**: the name of a service/characteristic. 16-bit (`180F`) is a SIG standard = `0000180F-0000-1000-8000-00805F9B34FB`; 128-bit is self-assigned.
- **Read** = pull once · **Write** = push down · **Notify** = the peripheral pushes up continuously, cheaper than polling.

## 10. Explained for beginners

**`PermissionStatus` with 4 states (instead of `Boolean granted`)**
- What: tells apart *never asked* / *granted* / *denied but can ask again* / *only Settings left*.
- Why: each state needs a different button. Collapse them into `false` → "Request" shows when the dialog is dead, the user taps, nothing happens, they think the app is broken.
- Ref: [Android — Request runtime permissions](https://developer.android.com/training/permissions/requesting)

**The "asked" flag in SharedPreferences (Android only)**
- What: remembers which permissions the app has ever shown a dialog for.
- Why: `checkSelfPermission` + `shouldShowRequestPermissionRationale` give **exactly the same** result for "fresh install" and "denied twice". Without the flag a fresh install already shows *Open app settings*. iOS has `.notDetermined` so it doesn't need one.
- Ref: [shouldShowRequestPermissionRationale](https://developer.android.com/reference/androidx/core/app/ActivityCompat#shouldShowRequestPermissionRationale(android.app.Activity,java.lang.String))

**`registerForActivityResult` lives in the Activity**
- What: registers the launchers for requesting permissions / enabling Bluetooth.
- Why: they must be registered before the Activity is STARTED; calling it in a Composable/on a button tap throws `IllegalStateException`.
- Ref: [Activity Result APIs](https://developer.android.com/training/basics/intents/result)

**`BluetoothService.readiness()` — a pure function combining permissions + radio**
- What: returns one single answer *can we scan yet / what's blocking*.
- Why: with permissions but the radio off, scanning returns 0 devices without an error. 2 screens share 1 function → no screen guesses differently.
- Ref: [Set up Bluetooth](https://developer.android.com/develop/connectivity/bluetooth/setup) · [CBManagerState](https://developer.apple.com/documentation/corebluetooth/cbmanagerstate)

**`BroadcastReceiver(ACTION_STATE_CHANGED)` 🤖 / delegate `centralManagerDidUpdateState` 🍎**
- What: know immediately when the user toggles Bluetooth.
- Why: pulling down Quick Settings doesn't take the Activity through onResume → without listening the screen is wrong. Register in onStart, unregister in onStop, or it leaks.
- Ref: [BluetoothAdapter.ACTION_STATE_CHANGED](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#ACTION_STATE_CHANGED)

**`StateFlow` 🤖 / `@Observable` 🍎 in `BluetoothClientService`**
- What: the service holds state (devices, isScanning, connection), the screen `collectAsState()` / reads the property and redraws by itself.
- Why: Android BLE callbacks run on a Binder thread — writing a plain variable that Compose reads is a cross-thread access. iOS uses `queue: nil` so the delegate is already on main.
- Ref: [StateFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow) · [Observation](https://developer.apple.com/documentation/observation)

**10 s scan timeout + dedupe by id**
- What: `stopScan` by itself after 10 s; a Map `id → device` overwritten on every packet.
- Why: every advertising packet = 1 callback (dozens per second). An endless scan drains the battery + Android silently blocks you when you start too often.
- Ref: [Find BLE devices](https://developer.android.com/develop/connectivity/bluetooth/ble/find-ble-devices) · [scanForPeripherals](https://developer.apple.com/documentation/corebluetooth/cbcentralmanager/scanforperipherals(withservices:options:))

**`connectGatt(..., autoConnect=false, ..., TRANSPORT_LE)` + always `close()` 🤖**
- What: connect right away over BLE; close GATT on disconnect.
- Why: without `TRANSPORT_LE` dual-mode devices may go over Classic by mistake → 133. No `close()` → leaked client slots, after ~30 times every connect returns 133.
- Ref: [Connect to a GATT server](https://developer.android.com/develop/connectivity/bluetooth/ble/connect-gatt-server)

**Keep a strong ref to `CBPeripheral` + your own 15 s timeout 🍎**
- What: store the peripheral in a dictionary before `connect`; schedule a 15 s `Task.sleep` then `cancelPeripheralConnection`.
- Why: CoreBluetooth holds it weakly → when the peripheral is released the connection is cancelled silently; iOS `connect` never times out by itself.
- Ref: [connect(_:options:)](https://developer.apple.com/documentation/corebluetooth/cbcentralmanager/connect(_:options:))

**`scannerButtonsOf()` / `isDeviceShown()` — screen rules are pure functions, not inside the Composable/View**
- What: `scannerButtonsOf` takes (selected?, connectable?, is it the connected device?, state, ready) and returns Connect enabled/disabled/spinner + Disconnect enabled/disabled; `isDeviceShown` decides which rows show based on the Device Name box + the 2 switches.
- Why: many combinations make it easy to miss a case (e.g. right after `Error` Connect must work again; the connected device must never be hidden by a switch). Inside the UI it can only be checked by hand on a device; split out, the unit tests run in 1 second, and both platforms share the same cases.
- Ref: [Android — Guide to app architecture (UI layer)](https://developer.android.com/topic/architecture/ui-layer) · [Swift Testing](https://developer.apple.com/documentation/testing)

**Ignore callbacks from the OLD connection when switching devices**
- What: connected to A, select B and tap **Connect** → A is dropped; a late "A disconnected" callback is ignored (🤖 `g !== gatt` · 🍎 compare `peripheral.identifier`).
- Why: BLE callbacks arrive **asynchronously**. Without filtering, "A disconnected" arrives while B is `Connecting…` → the card flips to `Disconnected`, row B loses its spinner even though B is still connecting.
- Ref: [BluetoothGattCallback.onConnectionStateChange](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt,%20int,%20int)) · [didDisconnectPeripheral](https://developer.apple.com/documentation/corebluetooth/cbcentralmanagerdelegate/centralmanager(_:diddisconnectperipheral:error:))

**The list keeps first-seen order, not sorted by RSSI**
- What: new rows are appended at the end; RSSI updates in place.
- Why: tapping a row = selecting the device to Connect, so rows must not move — RSSI changes several times a second, sorting by signal means your finger lands on another device.

**The screen's "clock": `LaunchedEffect` + `delay(1000)` 🤖 / `TimelineView(.periodic)` 🍎**
- What: redraw every second so "Last seen: 3s ago" counts by itself and cards fade on their own.
- Why: the UI only redraws when state changes. A silent device = no new packets = no new state → without a clock the card never fades when it should.
- Ref: [LaunchedEffect](https://developer.android.com/develop/ui/compose/side-effects#launchedeffect) · [TimelineView](https://developer.apple.com/documentation/swiftui/timelineview)

**The `gattOps` GATT queue (🤖 only) — Day 3**
- What: each read / CCCD write is one `GattOp`; the next op only runs when the previous op's callback arrives (`opDone()`).
- Why: the Android Bluetooth stack runs **one GATT op at a time**. Calling `readCharacteristic` while a descriptor write is in flight → returns `false`, the op is lost, **no callback at all** → the UI waits forever. CoreBluetooth queues by itself so 🍎 doesn't need it.
- Ref: [BluetoothGatt.readCharacteristic](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#readCharacteristic(android.bluetooth.BluetoothGattCharacteristic)) · [Punch Through — Android BLE guide (operation queue)](https://punchthrough.com/android-ble-guide/)

**Properties are a bitmask, `GattProperties.of(raw)` — Day 3**
- What: 1 byte declared by the peripheral → 5 flags READ / WRITE / WRITE NO RESP / NOTIFY / INDICATE → decide which buttons show.
- Why: Android's `PROPERTY_*` and iOS's `CBCharacteristicProperties` are **the same bit values** from the spec → 1 function, 1 test suite for both platforms. Showing a Read button for a non-READ characteristic = an error on tap.
- Ref: [BluetoothGattCharacteristic.PROPERTY_READ](https://developer.android.com/reference/android/bluetooth/BluetoothGattCharacteristic#PROPERTY_READ) · [CBCharacteristicProperties](https://developer.apple.com/documentation/corebluetooth/cbcharacteristicproperties)

**Little-endian + decode by UUID — Day 3**
- What: `decode(uuid, bytes)` — known UUID → per spec (battery `%`, UTF-8 strings, heart rate), unknown → ASCII / uint LE / hex only.
- Why: BLE sends the **low byte first**: `34 12` = `0x1234` = 4660. Reading it backwards as 13330 is the most common hand-decoding bug. Kotlin's Byte is signed → always `and 0xFF`.
- Ref: [Bluetooth SIG — Assigned Numbers / GATT Specification Supplement](https://www.bluetooth.com/specifications/assigned-numbers/)

**Read and notify arrive on the same callback (🍎) — Day 3–4**
- What: `pendingReads` remembers the key just `readValue`d; `didUpdateValueFor` with that key → READ, without → NOTIFY.
- Why: CoreBluetooth doesn't say whether a value arrived because we asked or because the peripheral pushed. Android splits it into 2 callbacks (`onCharacteristicRead` / `onCharacteristicChanged`).
- Ref: [peripheral(_:didUpdateValueFor:error:)](https://developer.apple.com/documentation/corebluetooth/cbperipheraldelegate/peripheral(_:didupdatevaluefor:error:))

**CCCD `2902` — Day 4**
- What: the "switch" descriptor on the peripheral; write `01 00` = notify on, `02 00` = indicate on, `00 00` = off.
- Why: the peripheral **pushes nothing** until someone enables the CCCD. 🤖 must do 2 steps itself (`setCharacteristicNotification` + write CCCD), missing either is silent with no error. 🍎 `setNotifyValue` does both.
- Ref: [Android — Receive GATT notifications](https://developer.android.com/develop/connectivity/bluetooth/ble/transfer-ble-data#notification) · [setNotifyValue(_:for:)](https://developer.apple.com/documentation/corebluetooth/cbperipheral/setnotifyvalue(_:for:))

**API 33 callbacks (🤖) — override both the new and the old version — Day 3–4**
- What: `onCharacteristicRead(…, value, status)` / `onCharacteristicChanged(…, value)` for API 33+; the old ones read `c.value` + `copyOf()` for API 26–32 (the old ones guard `SDK_INT >= 33 → return`).
- Why: minSdk 26. Writing only the new version → Android 12 and below never get data. The old version uses **one shared buffer** `c.value` → the next notify packet overwrites the previous one unless copied immediately.
- Ref: [BluetoothGattCallback.onCharacteristicChanged](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onCharacteristicChanged(android.bluetooth.BluetoothGatt,%20android.bluetooth.BluetoothGattCharacteristic,%20byte[]))

**`clearGattState()` on every disconnect path + keep the log — Day 4**
- What: manual disconnect, signal lost, radio off, device switch, connect fail all call one function that clears services + the queue + pending CCCD writes. `received` / `lastRead` are kept until Connect to a new device or **Clear**.
- Why: characteristic objects die with the connection — keeping them means tapping Read on a dead object. The log is kept because it's exactly what you need to read after "killing the peripheral" to know what the last packet was.

**`appendCapped(…, 50)` — Day 4**
- What: append the new line at the end, drop the oldest when over 50.
- Why: notify can be hundreds of packets/second (ESP32 `BLE_notify`); an unbounded list → memory + draw time grow forever, the screen freezes after a few minutes.

**`BluetoothConnectionState` is a `sealed interface` / an `enum` with associated values**
- What: 5 states, `Connected` carries the name, `Error` carries the reason.
- Why: exhaustive `when` / `switch` — add a state and forget to draw it in the UI → build error, not a runtime bug. `Disconnected` (we tapped, grey) is separate from `Error` (dropped, red).
- Ref: [Sealed classes](https://kotlinlang.org/docs/sealed-classes.html) · [Swift enums](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/enumerations/)

**Write with response / without response — task 004**
- What: `planWrite` picks the type from the properties: has `WRITE` → with response, only `WRITE NO RESP` → without response.
- Why: with response = the peripheral sends an ACK → you know for sure it arrived, errors (e.g. not writable) come back for real. Without response is faster but "fire and forget": 🍎 no callback, 🤖 the callback only means it was sent. 🍎 writing with the wrong type for the properties silently drops the write.
- Ref: [BluetoothGatt.writeCharacteristic](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#writeCharacteristic(android.bluetooth.BluetoothGattCharacteristic,%20byte[],%20int)) · [CBCharacteristicWriteType](https://developer.apple.com/documentation/corebluetooth/cbcharacteristicwritetype)

**`fromHex` / `fromText` + the `\r` escape — task 004**
- What: convert typed text into bytes. Hex accepts `41 54`, `4154`, `0x41,0x54`; Text turns `\r` `\n` `\t` `\\` into the real characters.
- Why: the input box can't type Enter as `0x0D`, yet ELM327 only runs a command when it sees `\r`. Many hex styles are accepted because people copy from nRF / Logcat / docs. Pure functions → the same test cases on both sides.
- Ref: [ELM327 datasheet — AT commands](https://www.elmelectronics.com/wp-content/uploads/2017/01/ELM327DS.pdf)

**MTU = the max ATT packet, data = MTU − 3 — task 004**
- What: 🤖 `requestMtu(247)` through the queue after discovery, the real value arrives in `onMtuChanged`; 🍎 reads `maximumWriteValueLength(for: .withoutResponse)`. The UI shows `MTU · max bytes` and blocks longer payloads.
- Why: the default 23 → only 20 data bytes per packet (3 bytes opcode + handle). Longer notify / write packets get truncated or rejected (`status=13` INVALID_ATTRIBUTE_LENGTH). The MTU belongs to one connection → back to 23 after a disconnect.
- Ref: [BluetoothGatt.requestMtu](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#requestMtu(int)) · [maximumWriteValueLength(for:)](https://developer.apple.com/documentation/corebluetooth/cbperipheral/maximumwritevaluelength(for:)) · [Punch Through — Maximizing BLE throughput](https://punchthrough.com/maximizing-ble-throughput-part-2-use-larger-att-mtu-2/)
