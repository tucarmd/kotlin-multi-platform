[← README](index.md) · [How to implement](README-HOW-TO-IMPLEMENT.md) · [Peripheral setup](peripheral-setup.md)

# BLE flow diagrams — how far it runs, which files it goes through

> Read this file **before opening the code**. Every diagram gives `file:line` → open the file next to it to compare.
> Covers all of **Day 1 → Day 4** and **both platforms** (🤖 Kotlin · 🍎 Swift).
> Numbering: BLE Day 1 → Day 4 here = **Day 06 → Day 09** of the curriculum (Week 2).
> Goal: once you can redraw **diagram 3** (pipeline) and **diagram 7** (state machine) with your eyes closed, then read the code in detail.

Path abbreviations used in this file:

- 🤖 `A/` = `native-device-monitor/android/app/src/main/java/com/learn/nativedevicemonitor/`
- 🍎 `I/` = `native-device-monitor/ios/NativeDeviceMonitor/`

| # | Diagram | Answers the question |
|---|---|---|
| 0 | [How far it runs](#0-how-far-it-runs) | Which steps are coded, built, run on a real device? |
| 1 | [File map](#1-file-map-who-calls-whom) | Which file calls which? |
| 2 | [4 layers](#2-four-layers-commands-go-down-data-comes-up) | How do commands go down and data come up? |
| 3 | [Pipeline](#3-pipeline-from-opening-the-app-to-disconnect) | From opening the app to disconnect, which steps, and which Day does each belong to? |
| 4 | [Day 1 — permissions](#4-day-1-requesting-the-bluetooth-permission) | What do the 2 platforms do when you tap *Request*? |
| 5 | [Day 1 — readiness](#5-day-1-radio-ready-to-scan) | When does the Scan button light up? |
| 6 | [Day 2 — scan](#6-day-2-scan-one-advertising-packet-becomes-one-list-row) | How does one advertising packet become one list row? |
| 7 | [Connection state machine](#7-state-machine-bluetoothconnectionstate) | How do `Idle / Connecting / Connected / Disconnected / Error` transition? |
| 8 | [Day 2 — connect](#8-day-2-sequence-after-tapping-connect) | What happens over time after tapping Connect? |
| 9 | [Day 3 — discover](#9-day-3-discover-services-characteristics) | Why 1 call on Android but 2 rounds on iOS? |
| 10 | [Day 3 — read + decode](#10-day-3-read-decode) | Which path does Read take, where do bytes become a value? |
| 11 | [Day 4 — notify](#11-day-4-notify-cccd-2902-received-data) | What is CCCD `2902`, how does a notify packet reach the UI? |
| 12 | [Day 4 — 3 disconnect paths](#12-day-4-3-disconnect-paths) | How do manual / signal lost / radio off differ? |
| 13 | [Timers](#13-timers-scan-10-s-connect-15-s-ios) | How are the 10 s scan and 15 s connect timers cancelled? |
| 14 | [Test → branch](#14-test-case-which-branch-it-goes-through) | Which branch of the diagrams does each test case run through? |

---

## 0. How far it runs

Updated **24/09/2026**. Per-case results: [task 003 §6](tasks/003-gatt-read-notify.md#6-verification-results).

```
 Day 1 ─────────────────── Day 2 ───────────────────────────────── Day 3 ──────────────── Day 4 ───────────────────
 Permission ─► Radio ─► Ready ─► Scan ─► Filter ─► Select ─► Connect ─► Discover ─► Read ─► Notify ─► Disconnect / error
  ✅🤖         ✅🤖     ✅🤖     ✅🤖    ✅🤖      ✅🤖      ✅🤖       🟡🤖        🟡🤖     🟡🤖      🟡🤖
  🟢🍎         🟢🍎     🟢🍎     🟡🍎    🟡🍎      🟡🍎      🟡🍎       🟡🍎        🟡🍎     🟡🍎      🟡🍎

 ✅ = runs OK on a real device (Samsung A32)   🟢 = runs OK on the Simulator (UI only, the Simulator has no Bluetooth)
 🟡 = coded · build + unit tests OK · NOT yet run on a real device             📝 = not done
```

| Day | Date | Steps | 🤖 Android | 🍎 iOS |
|---|---|---|---|---|
| 1 | Mon 21/09 | BLE + Camera permissions · radio ON/OFF · *Ready to scan* | ✅ real device | 🟢 Simulator |
| 2 | Tue 22/09 | 10 s scan · dedupe · name filter / Hide unknown / Hide audio · select a row · Connect / Disconnect | ✅ real device (connected a `CMD-…` dongle) | 🟡 build + tests · ⏳ real iPhone |
| 3 | Wed 23/09 | Discover services → characteristics · ★ target · Read → raw + decoded | 🟡 build + tests (38 cases) · ⏳ real device | 🟡 build + tests (35 cases) · ⏳ real iPhone |
| 4 | Thu 24/09 | Notify (CCCD) → *Received Data* · manual / signal lost / radio off · subscription cleanup | 🟡 build + tests · ⏳ real device | 🟡 build + tests · ⏳ real iPhone |
| 5 | Fri 25/09 | Camera / media / networking — **outside the BLE flow**, lives in the Media + API tabs ([README](index.md)) | 🟡 build + tests · ⏳ real device | ✅ Simulator (picker + API) · ⏳ camera on a real iPhone |
| — | Thu 24/09 | Write characteristic (Text / Hex, ELM327 presets) · MTU — [task 004](tasks/004-write-mtu.md) | 🟡 build + tests (45 cases) · ⏳ real device | 🟡 build + tests (42 cases) · ⏳ real iPhone |
| — | later | Auto-reconnect + re-subscribe · CCCD off before disconnect (bonded) · Device detail screen | 📝 [plan §8.5](README-HOW-TO-IMPLEMENT.md#85-after-task-004-not-done-yet) | 📝 |

**Next step to reach ✅:** set up a peripheral with GATT ([peripheral-setup.md](peripheral-setup.md) — nRF Connect *Configure GATT server* or ESP32 `BLE_notify`) → run cases 8–12 from [README › How to test](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/README.md#how-to-test-short) on the Samsung A32 + a real iPhone.

---

## 1. File map — who calls whom

```
 🤖 MainActivity.kt                                 🍎 NativeDeviceMonitorApp.swift  (@main)
    holds: permission launchers · radioState ·        TabView :26
           bleClient (lazy) :103                         │
    onStart  → observeRadio :231                         │
    onResume → refreshPermissionStates :239             │
    onDestroy → bleClient.release() :252                │
          │                                              │
          ├─ Permissions tab ─────────────┬──────────────┤
          │   ui/permission/PermissionScreen.kt           UI/Permission/PermissionView.swift
          │                                               │
          └─ Scanner tab ─────────────────┴──────────────┘
              ui/scanner/ScannerScreen.kt                 UI/Scanner/ScannerView.swift
                │ collectAsState() :115-126                 │ reads @Observable directly
                │ calls: startScan/stopScan :247            │ .task { start() } :128
                │        connect :320 · disconnect :330     │ scan :196 · connect :343 · disconnect :348
                │                                           │
                ├─► ScannerActions (PURE functions, unit tested) ◄┤  matchesName · isDeviceShown · scannerButtonsOf
                └─► GattSection + ReceivedDataSection   ◄────┘  take state + lambdas, NOT the service
                         │  onRead / onNotify / onClear
                         ▼
 ┌──────────────────────── NATIVECORE (no UI imports) ──────────────────────────────────────────┐
 │ permission/PermissionService    bluetooth/BluetoothService       bluetooth/BluetoothClientService │
 │   statusOf / request              radio ON/OFF + readiness()       scan · connect · GATT         │
 │                                                                     │                           │
 │                                   BluetoothScanFilter ◄─────────────┤ name filter during scan  │
 │                                   BluetoothDeviceInfo ◄─────────────┤ device snapshot + state  │
 │                                   GattModel ◄───────────────────────┤ properties · target · cap │
 │                                   BluetoothDecoder ◄────────────────┘ bytes → hex / value       │
 └──────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
 🤖 BluetoothAdapter · BluetoothLeScanner · BluetoothGatt        🍎 CBCentralManager · CBPeripheral
```

**The one-way rule:** `ui/` ──► `nativecore/`. Nativecore doesn't know which screen is showing and holds no UI strings.
`GattSection` only takes **snapshots** (`GattServiceInfo`, `GattCharacteristicInfo`) + lambdas — it never holds a `BluetoothGatt` / `CBCharacteristic`.

---

## 2. Four layers — commands go down, data comes up

```
 ┌──────────────────────── UI (Compose / SwiftUI) ──────────────────────────┐
 │ Scanner: [Scan] · Device Name · 2 switches · list · [Connect][Disconnect] │
 │          GATT block (★ target, Read, Notify) · Received Data              │
 └───────────────┬───────────────────────────────────────▲──────────────────┘
 COMMANDS (calls)│ startScan(filter) · stopScan()          │ DATA (state changes → redraw)
                 │ connect(device) · disconnect()          │ devices · isScanning · connection
                 │ read(chr) · setNotify(chr, on)          │ services · isDiscovering · lastRead
                 │ write(chr, bytes, withResponse)         │ mtu  (task 004)
                 │ clearReceived()                         │ received · gattError · lastEvent
                 ▼                                         │ 🤖 StateFlow ─collectAsState()
 ┌────────────── SERVICE (BluetoothClientService) ─────────┴──── 🍎 @Observable ───────┐
 │ device dedupe · holds the connection handle · GATT queue (🤖) · pendingReads (🍎)    │
 │ tells manual disconnect from signal lost · clears state on disconnect               │
 └───────────────┬───────────────────────────────────────▲──────────────────────────────┘
                 │ 🤖 startScan · connectGatt              │ 🤖 ScanCallback · BluetoothGattCallback
                 │    discoverServices · readChar          │    (Binder thread → scope.launch to main)
                 │    writeDescriptor(CCCD)                │ 🍎 CentralDelegate · PeripheralDelegate
                 │ 🍎 scanForPeripherals · connect         │    (queue: nil → main queue)
                 │    discoverServices/Characteristics     │
                 │    readValue · setNotifyValue           │
                 ▼                                         │
 ┌────────────── OS Bluetooth stack (Android / CoreBluetooth) ────────────────────────┐
 └───────────────┬───────────────────────────────────────▲─────────────────────────────┘
                 │ 2.4 GHz radio                           │ advertisement · read response · notification
                 ▼                                         │
 ┌────────────── PERIPHERAL (CMD-… dongle, nRF Connect, ESP32) ────────────────────────┐
 └─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Pipeline — from opening the app to disconnect

This is the "backbone" of all 4 days. Each box is one step, below it is the function that starts that step.

```
 ╔═ DAY 1 ═════════════════════════════════════════════════════════════════════════════╗
 ║ ① BLE permission     ② Radio                 ③ Readiness                             ║
 ║  🤖 statusOfGroup     🤖 observeRadio          readiness(radio, granted)             ║
 ║  🍎 CBManager         🍎 RadioObserver         = READY | MISSING_PERMISSION          ║
 ║     .authorization                             | RADIO_OFF | UNSUPPORTED             ║
 ╚═══════════════════════════════════════════════════╤═════════════════════════════════╝
                                                     │ READY → Scan button lights up
 ╔═ DAY 2 ═══════════════════════════════════════════▼═════════════════════════════════╗
 ║ ④ Scan 10 s ──► ⑤ Dedupe + filter ──► ⑥ Select row ──► ⑦ Connect ──► Connected(name) ║
 ║  startScan       found[id] = info      selected          connect(dev)                ║
 ║  (stops at 10 s) isDeviceShown()       scannerButtonsOf  🍎 + 15 s timeout           ║
 ╚══════════════════════════════════════════════════════════════════╤══════════════════╝
                                                                    │ automatic, no tap needed
 ╔═ DAY 3 ══════════════════════════════════════════════════════════▼══════════════════╗
 ║ ⑧ Discover ────────────► services: [GattServiceInfo] ──► ★ readTarget / notifyTarget ║
 ║  🤖 discoverServices()     GattProperties.of(bitmask)      ⑨ [Read] ─► lastRead      ║
 ║  🍎 discoverServices → discoverCharacteristics                 raw hex + decoded     ║
 ╚══════════════════════════════════════════════════════════════════╤══════════════════╝
 ╔═ DAY 4 ══════════════════════════════════════════════════════════▼══════════════════╗
 ║ ⑩ [Notify] ─► CCCD 2902 = 01 00 ─► each packet ─► received (≤ 50) ─► Received Data   ║
 ║ ⑪ Disconnect:  manual → Disconnected (grey)                                         ║
 ║                signal lost / radio off → Error (red)   all 3 paths → clearGattState() ║
 ╚═════════════════════════════════════════════════════════════════════════════════════╝
```

| Step | 🤖 Android | 🍎 iOS |
|---|---|---|
| ① Permission | `A/nativecore/permission/PermissionService.kt:57` `statusOf` | `I/NativeCore/Permission/PermissionService.swift:89` `bluetoothStatus` |
| ② Radio | `A/nativecore/bluetooth/BluetoothService.kt:90` `observeRadio` | `I/NativeCore/Bluetooth/BluetoothService.swift:54` `start` |
| ③ Readiness | `BluetoothService.kt:67` | `BluetoothService.swift:67` |
| ④ Scan | `BluetoothClientService.kt:320` `startScan` | `BluetoothClientService.swift:144` `startScan` |
| ⑤ Dedupe | `BluetoothClientService.kt:219` `onScanResult` | `BluetoothClientService.swift:364` `handleDiscovered` |
| ⑤ Display filter | `A/ui/scanner/ScannerActions.kt:70` `isDeviceShown` | `I/UI/Scanner/ScannerActions.swift:52` |
| ⑥ Buttons | `ScannerActions.kt:106` `scannerButtonsOf` | `ScannerActions.swift:85` |
| ⑦ Connect | `BluetoothClientService.kt:455` `connect` | `BluetoothClientService.swift:193` `connect` |
| ⑧ Discover | `BluetoothClientService.kt:499` `startDiscovery` | `BluetoothClientService.swift:436` `startDiscovery` |
| ⑨ Read | `BluetoothClientService.kt:538` `read` | `BluetoothClientService.swift:246` `read` |
| ⑩ Notify | `BluetoothClientService.kt:569` `setNotify` | `BluetoothClientService.swift:260` `setNotify` |
| ⑪ Disconnect | `BluetoothClientService.kt:482` `disconnect` · `:381` DISCONNECTED · `:748` `onRadioOff` | `BluetoothClientService.swift:227` `disconnect` · `:562` `handleDisconnected` · `:342` `handleStateChange` |

---

## 4. Day 1 — requesting the Bluetooth permission

```
                🤖 ANDROID                                             🍎 iOS
 [Request] on PermissionScreen                          [Request] on PermissionView
      │                                                      │
      ▼                                                      ▼
 MainActivity.requestGroup :185                        PermissionService.request :116
      │ markAsked(group)    ← SharedPreferences flag         │
      │ requestPermissions.launch(SCAN + CONNECT) :89        ▼
      │  (API ≤ 30: ACCESS_FINE_LOCATION)              requestBluetooth :147
      ▼                                                      │ authorization != .notDetermined? → return
 System dialog (one chain)                                   │ (iOS has NO requestAccess for BLE)
      │                                                      ▼
      ▼                                                BluetoothPermissionProbe :179
 callback results :91                                        │ create CBCentralManager → iOS shows the dialog
      │ a Boolean is NOT ENOUGH to tell DENIED from PERMANENT│ wait for centralManagerDidUpdateState ≠ unknown
      ▼                                                      │ resume the continuation exactly once
 refreshPermissionStates :206                                ▼
      │ statusOf :57                                   refreshAll → CBManager.authorization
      ├─ checkSelfPermission == GRANTED → GRANTED            ├─ .allowedAlways → granted
      ├─ never asked                  → NOT_REQUESTED        ├─ .notDetermined → notDetermined
      ├─ shouldShowRationale == true  → DENIED (can ask again)├─ .denied       → denied ─► Open Settings
      └─ otherwise                    → PERMANENTLY_DENIED   └─ .restricted    → restricted
                                         ─► Open app settings
```

- 🤖 needs **both** `BLE_SCAN` **and** `BLE_CONNECT` → `isBleGranted()` `A/MainActivity.kt:213`.
- 🍎 one deny is final. Back from Settings → `scenePhase == .active` → `refreshAll()` (`I/UI/Scanner/ScannerView.swift:129`). The 🤖 equivalent is in `onResume` (`MainActivity.kt:237`).
- Details + the 4-state table: [How to implement §3.1](README-HOW-TO-IMPLEMENT.md#31-permission-request-flow).

---

## 5. Day 1 — radio → *Ready to scan*

```
 readiness(radio, permissionGranted)       🤖 BluetoothService.kt:67 · 🍎 BluetoothService.swift:67
      │
      ├─ radio == UNSUPPORTED ?  ──yes──► UNSUPPORTED        (🍎 the Simulator always lands here)
      │
      ├─ missing permission ?    ──yes──► MISSING_PERMISSION  → "Not ready" card + [Open Permissions]
      │   (🍎 also radio == .unauthorized)
      │
      ├─ radio != ON ?           ──yes──► RADIO_OFF           → 🤖 [Turn on] = ACTION_REQUEST_ENABLE
      │                                                          🍎 showSystemPowerAlert (only points the way)
      └─ otherwise ────────────────────► READY  ✅            → [Scan] button lights up
```

The radio changes **live** without leaving the screen:

- 🤖 `BroadcastReceiver` `ACTION_STATE_CHANGED` registered in `onStart` (`MainActivity.kt:231`); radio OFF also calls `bleClient.onRadioOff()` (`:233`) → see [§12 path C](#12-day-4-3-disconnect-paths).
- 🍎 `RadioObserver.centralManagerDidUpdateState` (`BluetoothService.swift:104`). The manager is only created **after** permission is granted (`:55`) — creating it earlier makes iOS show the permission dialog as soon as the screen opens.

---

## 6. Day 2 — scan → one advertising packet becomes one list row

```
 [Scan]  🤖 ScannerScreen.kt:247  ·  🍎 ScannerView.swift:198
    │
    ▼
 startScan(filter)                          🤖 :320                         🍎 :144
    ├─ already scanning? → return            ✓                                ✓
    ├─ 🤖 scanner == null → "Bluetooth is off"   🍎 central.state != .poweredOn → return (trap 3)
    ├─ clear found / hidden / packetStats, devices = []
    │   🍎 keep the connected peripheral (trap 1: dropping the strong ref = connection dies silently) :159
    ├─ 🤖 scanner.startScan(scanCallback)   — ONE callback for the service's whole life :199
    │   🍎 scanForPeripherals(nil, AllowDuplicates: true) :171
    └─ schedule 10 s → stopScan()             → §13
         │
         ▼   (every advertising packet — dozens of times per second per device)
 🤖 onScanResult :201  (Binder thread)        🍎 didDiscover → handleDiscovered :309  (main)
    │
    ├─ name = device.name ?? the name in the advertising packet
    ├─ filter.matches(name)?  (BluetoothScanFilter — CarMD2 / CMD-*)
    │     └─ no → hidden.add(id) → hiddenCount   ("N other devices hidden")
    ├─ info = BluetoothDeviceInfo(id, name, rssi, advertisementOf(...))
    ├─ found[id] = info          ← overwrite = DEDUPE + RSSI always the latest
    │     🤖 id = MAC (LinkedHashMap keeps order)   🍎 id = UUID specific to this phone (+ an order array)
    └─ devices = a NEW list  (🤖 StateFlow compares with equals — mutating in place won't redraw the UI)
         │
         ▼
 UI: devices.filter { isDeviceShown(it, query, hideUnknown, hideAudio, selected, connected) }
         │   Device Name box · Hide unknown · Hide audio — filters the EXISTING list, no rescan
         │   the selected / connected device is always kept
         ▼
 ┌──────────────────────────────────────────────────────┐
 │ ((•)) CMD-Test          ▂▄▆  -52 dBm   CONNECTABLE   │  ← tap = select
 │ ((•)) Unknown device    ▂▄   -71 dBm                 │
 └──────────────────────────────────────────────────────┘
          [ Connect ]  [ Disconnect ]    ← scannerButtonsOf(hasSelection, connection, ready)
```

List order = **first-seen order**, not sorted by RSSI — sorting by signal makes rows jump around and you tap the wrong device (`BluetoothClientService.swift:376`).
Name/audio filters: [task 002](tasks/002-scanner-hide-filters.md) · [bluetooth-audio-devices.md](bluetooth-audio-devices.md).

---

## 7. State machine — `BluetoothConnectionState`

🤖 `A/nativecore/bluetooth/BluetoothDeviceInfo.kt:101` · 🍎 `I/NativeCore/Bluetooth/BluetoothDeviceInfo.swift:76`

```
                         connect(device)
     ┌──────┐  ───────────────────────────────►  ┌────────────┐
     │ Idle │                                     │ Connecting │ ── 🍎 not done in 15 s ─┐
     └──────┘                  ┌────────────────► └─────┬──────┘                         │
                               │ connect(another device)│ 🤖 STATE_CONNECTED             │
                               │ (old one cancelled first)│ 🍎 didConnect                │
                               │                        ▼                               │
     ┌──────────────┐          │               ┌─────────────────┐                      │
     │ Disconnected │ ◄────────┼────────────── │ Connected(name) │                      │
     │   (grey)     │  disconnect() manual     └────────┬────────┘                      │
     └──────┬───────┘          │                        │ signal lost · peripheral drops │
            │                  │                        │ radio off                     │
            │ connect()        │                        ▼                               │
            └──────────────────┤               ┌─────────────────┐ ◄────────────────────┘
                               └───────────────│  Error(message) │ ◄── 🍎 didFailToConnect
                                   connect()   │     (red)       │ ◄── 🤖 status=133 …
                                               └─────────────────┘
```

Telling *manual* from *signal lost* — both arrive on the same callback → the `userRequestedDisconnect` flag, set **BEFORE** calling disconnect
(🤖 `:461` · 🍎 `:228`). Set it after and an early callback reads the old flag → misreports "Unexpected".

---

## 8. Day 2 — sequence after tapping Connect

```
 User         ScannerScreen/View        BluetoothClientService              OS stack        Peripheral
  │ tap a row       │                          │                               │                 │
  │────────────────►│ selected = device        │                               │                 │
  │ [Connect]       │                          │                               │                 │
  │────────────────►│─ connect(device) ───────►│ stopScan()                    │                 │
  │                 │                          │ old device? → cancel it       │                 │
  │                 │                          │ clearGattState · received = []│                 │
  │                 │                          │ userRequestedDisconnect=false │                 │
  │                 │ ◄── Connecting (spinner) │ 🍎 connectedPeripheral = p (trap 1)             │
  │                 │                          │ 🤖 gatt = connectGatt(ctx, false, cb, LE) :474  │
  │                 │                          │ 🍎 central.connect(p) :213 + 15 s timer        │
  │                 │                          │──────────────────────────────►│── CONNECT_REQ ─►│
  │                 │                          │                               │◄──── OK ────────│
  │                 │                          │ 🤖 onConnectionStateChange :345 (Binder)         │
  │                 │                          │ 🍎 didConnect → handleConnected :366 (main)     │
  │                 │                          │   callback from the OLD device? → ignore         │
  │                 │                          │   (🤖 g !== gatt · 🍎 isCurrent)                 │
  │                 │ ◄── Connected(name) 🟢   │ 🍎 cancel the 15 s timer                         │
  │                 │                          │ ──► startDiscovery  (Day 3, §9) — automatic     │
```

- 🤖 `autoConnect = false` → connect right away, the OS gives up after ~30 s. `TRANSPORT_LE` stops dual-mode devices from picking Classic by mistake → 133.
- 🤖 DISCONNECTED **always** `gatt.close()` (`:372`) — no close leaks a slot, the next connect returns `status=133`.

---

## 9. Day 3 — discover services & characteristics

```
          🤖 ANDROID — 1 call                                  🍎 iOS — 2 rounds
 STATE_CONNECTED                                     handleConnected
   └─ scope.launch { startDiscovery(g) } :378           └─ startDiscovery :436
        (hop to main: older Samsungs ignore a call            peripheral.delegate = PeripheralDelegate  ← set FIRST
         made on the Binder thread)                           discoverServices(nil)
        │                                                     │
        ▼                                                     ▼
 g.discoverServices() :504                           didDiscoverServices → handleServicesDiscovered :446
   false → gattError "refused to start"                 pendingServiceCount = N
        │                                                for each service: discoverCharacteristics(nil, for:)
        ▼                                                     │
 onServicesDiscovered → handleServicesDiscovered :480         ▼  (arrives N times)
   status != SUCCESS → gattError                     didDiscoverCharacteristicsFor → :464
   g.services ALREADY HAVE their characteristics        pendingServiceCount -= 1
   charHandles[key] = the real handle                    services = … (drawn service by service)
        │                                                pendingServiceCount == 0 → isDiscovering = false
        └──────────────────────┬──────────────────────────────┘
                               ▼
 services: [GattServiceInfo { uuid, characteristics: [GattCharacteristicInfo] }]
   properties = GattProperties.of(bitmask)   → READ / WRITE / NOTIFY / INDICATE    (GattModel)
   readTargetOf(services)   priority 2A19 → 2A29 → 2A00 → the first READ one        ★ read target
   notifyTargetOf(services) priority FFE1 → Nordic UART TX → 2A37 → 2A19            ★ notify target
                               │
                               ▼
 GattSection (🤖 A/ui/scanner/GattSection.kt:87 · 🍎 I/UI/Scanner/GattSection.swift:29)
```

🍎 the peripheral changes its GATT table (`didModifyServices`) → discover again from scratch (`:432`).

---

## 10. Day 3 — read + decode

```
 [Read] on a characteristic  →  onRead(info)  →  service.read(info)
                │
     🤖 read :505                                      🍎 read :237
       handle = charHandles[info.key]                   ch = cbCharacteristic(info)   (looked up each time)
       enqueue("Read …") { g.readCharacteristic(c) }    pendingReads.insert(key)      ← to tell it from notify
            │                                           peripheral.readValue(for: ch)
            ▼                                                 │  (CoreBluetooth queues itself)
       gattOps (queue, 1 op at a time) :668-687               │
         op.start() == false → gattError "refused"            │
            │                                                 │
            ▼                                                 ▼
       onCharacteristicRead :386 (API 33+ has value)     didUpdateValueFor → handleValue :440
       / :411 (API < 33: copy c.value immediately)         wasRead = pendingReads.remove(key) != nil
            │                                                 │
            ▼                                                 │
       handleRead :516 → opDone() → next op                   │
            │   status != SUCCESS → gattError                 │   error → gattError
            │   (2 = not permitted · 5/15 = pairing needed)   │   ("Authentication is insufficient")
            └──────────────────────┬──────────────────────────┘
                                   ▼
 GattValue(hex = toHex(bytes), decoded = BluetoothDecoder.decode(uuid, bytes), source = READ)
   decode: 2A19 → "90 %" · 2A00/2A29… → UTF-8 · 2A37 → heart rate · otherwise → ASCII / uint LE / hex
                                   │
                                   ├─► lastRead            → "Last read: raw … · value …"
                                   ├─► received (≤ 50)     → 1 line in Received Data
                                   └─► log: READ 2A19 raw=[5A] decoded=90 %
```

---

## 11. Day 4 — notify: CCCD `2902` → *Received Data*

Notify = the peripheral **pushes** data by itself when the value changes. The switch lives on the peripheral: descriptor **CCCD `2902`**.
Longer explanation: [How to implement §6.0](README-HOW-TO-IMPLEMENT.md#60-how-notify-works-read-before-61).

```
 [Notify] → onNotify(info, true)
      │
      │ 🤖 setNotify :536 — 2 STEPS, miss either and it's silent    🍎 setNotify :251 — 1 CALL
      │   c.getDescriptor(2902) == null → gattError "no CCCD"       peripheral.setNotifyValue(true, for: ch)
      │   ① setCharacteristicNotification(c, true)  ← local          (iOS writes the CCCD itself)
      │   ② enqueue writeDescriptor(2902 = 01 00 | 02 00)                 │
      │        (notify)   (indicate)                                      │
      │        │                                                          ▼
      │        ▼                                                   didUpdateNotificationStateFor
      │   onDescriptorWrite → handleCccdWritten :561                 → handleNotificationState :463
      │     error → undo step ① + gattError                           error → gattError
      │     OK    → markNotifying(key, true)                           takes the REAL ch.isNotifying
      └──────────────────────────────┬──────────────────────────────────┘
                                     ▼
                  [Notify] button → [Stop] (blue) · lastEvent "Notify ON: FFE1"
                                     │
     ─ ─ ─ ─ ─ ─ ─ ─ ─ the peripheral pushes packets (every time the value changes) ─ ─ ─ ─ ─
                                     │
 🤖 onCharacteristicChanged :423 / :429           🍎 didUpdateValueFor → handleValue :498
    → handleNotified :575                              key NOT in pendingReads → NOTIFY
                                     │
                                     ▼
 GattValue(source = NOTIFY) → received = appendCapped(received, value)   (max 50, oldest pushed out)
                                     │    data → event → UI
                                     ▼
 ReceivedDataSection (🤖 GattSection.kt:312 · 🍎 GattSection.swift:262)
 ┌──────────────────────────────────────────────────────────┐
 │ 10:42:03  NOTIFY FFE1   01 2C       → 300                 │
 │ 10:42:04  NOTIFY FFE1   01 2D       → 301                 │   [Clear] → clearReceived()
 └──────────────────────────────────────────────────────────┘
```

🍎 read and notify arrive on **the same** `didUpdateValueFor` → `pendingReads` must be tracked by hand. 🤖 has 2 separate callbacks but must queue GATT ops itself.

---

## 12. Day 4 — 3 disconnect paths

```
 ── A. Tap [Disconnect] ─────────────────────────────────────────────────────────────────
 disconnect()                                   🤖 :482                      🍎 :227
   ├─ turn off notify on everything enabled     setCharacteristicNotification  setNotifyValue(false)
   │                                            (false) — local                (iOS writes CCCD = 0)
   ├─ 🤖 gattOps.clear() · 🍎 pendingReads.removeAll()
   ├─ userRequestedDisconnect = true   ← BEFORE the call
   ├─ lastEvent "Unsubscribed N · disconnecting…"
   └─ 🤖 gatt.disconnect()  (NO close here)       🍎 cancelPeripheralConnection
         ▼
   DISCONNECTED callback  → flag = true → Disconnected (grey) → clearGattState()
                            🤖 closeGatt() :390

 ── B. Signal lost / peripheral off / out of range ──────────────────────────────────────
   🤖 onConnectionStateChange(STATE_DISCONNECTED) :381   🍎 didDisconnectPeripheral → :562
   → flag = false → Error("Unexpected disconnect …") (red)
     🤖 status 8 = signal lost · 19 = peripheral disconnected · 133 = generic error
     🍎 CBError 6 = unexpected timeout · 7 = peripheral disconnected
   → clearGattState()  ·  the [Connect] button works again right away

 ── C. Bluetooth turned off on the phone ────────────────────────────────────────────────
   🤖 broadcast OFF → MainActivity :233 → onRadioOff() :748
   🍎 centralManagerDidUpdateState(≠ poweredOn) → handleStateChange :287
   ⚠ the OS may NOT fire a disconnect callback → the app cleans up ITSELF:
   → cancel timers · isScanning = false · clear the list · close GATT / drop the peripheral · clearGattState()
   → Error("Bluetooth turned off") (red), empty list
```

`clearGattState()` (🤖 `:638` · 🍎 `:277`) clears: `services`, `isDiscovering`, the queue / `pendingReads`, `gattError`.
It **keeps** `lastRead` + `received` so they can be read after disconnecting — only cleared on `connect` to a new device or tapping **Clear**.

---

## 13. Timers — scan 10 s · connect 15 s (iOS)

```
 SCAN (both)   🤖 scanTimeoutJob :321  ·  🍎 scanTimeoutTask :165

 A. leave it   t=0 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ t=10  stopScan() "Scan finished: N devices"
 B. manual Stop t=0 ━━━━━━━━━━━━ t=4 stopScan() → cancel
                                     ╳╳╳╳╳╳╳╳╳╳╳╳╳╳╳╳╳╳╳╳ t=10  doesn't run
 C. Connect    t=0 ━━━━━━━━━ t=3 connect() → stopScan() → cancel     (scanning in parallel slows connecting)
 D. radio off  t=0 ━━━━━━━━━━━━━━━ t=6 onRadioOff / handleStateChange → cancel

 Not cancelling in B → Scan again at t=5 and the old timer (t=10) cuts the new scan short after 5 s.
 🤖 another reason: starting a scan > 5 times / 30 s → Android silently returns 0 results (SCANNING_TOO_FREQUENTLY).

 CONNECT (🍎 only)   connectTimeoutTask :207 — iOS waits forever, Android gives up after ~30 s
   t=0 connect ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ t=15 still .connecting + same id?
                                                           → cancelPeripheralConnection → Error("timed out")
   didConnect / didFail / didDisconnect / device switch → cancel
```

---

## 14. Test case → which branch it goes through

Compare with [README › How to test](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/README.md#how-to-test-short) and [How to implement §7.2](README-HOW-TO-IMPLEMENT.md#72-test-cases).

| # | Test case | Path through the diagrams | 🤖 | 🍎 |
|---|---|---|:-:|:-:|
| 1 | Request BLE → Allow | §4 → GRANTED → §5 READY | ✅ | 🟢 |
| 1b | Camera → deny (🤖 deny twice) | §4 → DENIED → PERMANENTLY_DENIED → Open settings | ✅ | 🟢 |
| 2 | Toggle Bluetooth in Quick Settings / Control Center | §5 radio changes live · READY ↔ RADIO_OFF | ✅ | ⏳ |
| 4 | Scan | §6 → dedupe → §13-A stops by itself at 10 s | ✅ | ⏳ |
| 5 | Type `cmd` · select a row · Connect → Disconnect | §6 isDeviceShown → §8 → §7 Connected → §12-A Disconnected | ✅ | ⏳ |
| 6 | Hide unknown / Hide audio | §6 filters the existing list, the selected device is kept | ✅ | ⏳ |
| 7 | Walk away while connected · turn off Bluetooth | §12-B red Error · §12-C empty list | ✅ | ⏳ |
| 8 | Connect → GATT block shows | §8 → §9 → ★ target | ⏳ | ⏳ |
| 9 | Read `2A19` / `2A29` | §10 → lastRead + received | ⏳ | ⏳ |
| 10 | Notify → Stop | §11 CCCD → received ≤ 50 | ⏳ | ⏳ |
| 11 | Notifying → Disconnect | §12-A "Unsubscribed 1 · disconnecting…" | ⏳ | ⏳ |
| 12 | Notifying → turn off the peripheral | §12-B Error · clearGattState · Connect works again | ⏳ | ⏳ |

---

## Suggested code reading order (20 minutes)

Read **in execution order**, not file order. Do 🤖 first, then open the 🍎 file with the same name to compare.

1. `BluetoothDeviceInfo` — end of file: `BluetoothConnectionState` (diagram 7).
2. `BluetoothService.readiness` — 5 lines, diagram 5.
3. `BluetoothClientService` — read the doc at the top of the file (a short ASCII diagram), then:
   `startScan` → `onScanResult` / `handleDiscovered` → `stopScan` → `connect` → connected callback →
   `startDiscovery` → `handleServicesDiscovered` → `read` → `handleRead` / `handleValue` →
   `setNotify` → `handleCccdWritten` / `handleNotificationState` → `handleNotified` →
   `disconnect` → disconnected callback → `clearGattState` → `onRadioOff` / `handleStateChange`.
4. `GattModel` + `BluetoothDecoder` — pure functions, read alongside the unit tests (`GattModelTest` / `GattModelTests`).
5. `ScannerActions` → `ScannerScreen` / `ScannerView` — find where state is **read** and where service functions are **called**.

**Self-check:** cover the code, redraw diagrams 3 and 7 on paper. If you can → open [README-HOW-TO-IMPLEMENT.md](README-HOW-TO-IMPLEMENT.md) and read "why it's written that way".
