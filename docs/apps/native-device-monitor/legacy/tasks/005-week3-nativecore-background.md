# TASK — Week 3: NativeCore + Device providers + FileLogger + BackgroundMonitor

> Platforms: Android (Kotlin/Compose) + iOS (Swift/SwiftUI) · Created: 2026-10-05 (catching up on Week 3, original schedule Mon 28/09 → Fri 02/10)
> Code folders: `native-device-monitor/android/…/nativecore/{device,logging,background}`, `native-device-monitor/android/…/ui/monitor` · `native-device-monitor/ios/NativeDeviceMonitor/NativeCore/{Device,Logging,Background}`, `native-device-monitor/ios/NativeDeviceMonitor/UI/Monitor`

## 1. Requirement (Week 3 · Day 11–15)

| Day | Original requirement | Done in this repo |
|---|---|---|
| 11 · NativeCore refactor | The UI only calls services, holds no device logic | Already in place since Week 2 (`ui/` → `nativecore/`). Checked with the grep in §5: `ui/` / `UI/` call no device APIs |
| 12 · Device providers + FileLogger | Battery / Network / Bluetooth providers; append 1 JSONL line | `DeviceProviders.*` · `DeviceStatus.*` · `FileLogger.append` |
| 13 · FileLogger + BackgroundMonitor v1 | read / clear / file errors; `start(interval)` `stop()` `getStatus()` `getLogs()` `clearLogs()` | `FileLogger.readLogs/clearLogs` (skips + counts broken lines) · `BackgroundMonitor` 10 s loop |
| 14 · Background behaviour | The right mechanism per platform + a 5-state behaviour table, no OS workarounds | 🤖 Foreground Service `dataSync` + WorkManager 15′ · 🍎 BGAppRefresh (`.backgroundTask`) + `UIBackgroundModes` · table §4 |
| 15 · UI integration | Every feature controllable from the UI | A **Monitor** tab in both apps |

Records follow the plan's format exactly (plus `source` to know which mechanism wrote the line):

```json
{"timestamp":"2026-10-05T11:31:16+07:00","battery":100,"wifiEnabled":true,"wifiConnected":false,"cellularAvailable":false,"bluetoothEnabled":true,"bluetoothConnected":false,"source":"fgs"}
```

**Out of scope:** `MediaManager` / `ApiClient` (Day 10 — Camera/Media + Networking, done in a separate commit afterwards, see §6) · auto-start after reboot / force-stop · any trick to keep the app alive (silent audio, fake location, revival alarms).

## 2. Acceptance criteria

- AC1 — The 3 providers return the current values; fields that can't be measured = `null` (iOS `wifiEnabled`, Simulator battery, Bluetooth without permission) — emulator + Simulator
- AC2 — FileLogger: append / read / clear, no file yet = empty, broken lines skipped + counted — 8 unit test cases × 2 platforms
- AC3 — The monitor writes 1 record every 10 s in the foreground; Stop stops immediately; Start twice doesn't double it (Start disabled while RUNNING + a `job/task` guard in the service)
- AC4 — 🤖 FGS runs in the background, `isForeground=true`, the notification has a Stop button; Stop/force-stop stop cleanly
- AC5 — 🤖 periodic WorkManager: Schedule → `ENQUEUED`, the worker writes `"worker"` lines; force-stop cancels the schedule, reopening the app makes WorkManager reschedule by itself
- AC6 — 🍎 BGAppRefresh: submit OK + `_simulateLaunchForTaskWithIdentifier` writes a `"bgRefresh"` line — **needs a real iPhone** (Simulator: `BGTaskSchedulerErrorDomain error 1`, observed)
- AC7 — The §4 behaviour table filled in from real observations (real-device cells marked ⏳)
- AC8 — The code has comments about OS limits, no workarounds

## 3. Design

```
 UI (Monitor tab)                      NATIVECORE
 ─────────────────                     ──────────────────────────────────────────────────────────
 [Refresh status] ──────────────────►  DeviceStatusCollector.collect()
                                          ├─ BatteryProvider    🤖 BatteryManager      🍎 UIDevice.batteryLevel
                                          ├─ NetworkProvider    🤖 ConnectivityManager 🍎 NWPathMonitor (singleton)
                                          └─ BluetoothProvider  🤖 adapter + getConnectedDevices(GATT)
                                                                🍎 BluetoothService.radio + BluetoothClientService.connection
 [Append to log] ───► BackgroundMonitor.collectAndSave("manual") ──► FileLogger.append ──► monitor.jsonl
 [Start in-app loop] ► BackgroundMonitor.start(10 s) ── loop: collect → record → save → delay ──┘
 🤖 [Start FGS] ─► startForegroundService ─► MonitorService.startForeground ─► BackgroundMonitor.start(FGS)
 🤖 [Schedule] ──► WorkManager periodic 15′ ─► MonitorWorker.doWork ─► collectAndSave("worker")
 🍎 scenePhase .background ─► BGTaskScheduler.submit ─► .backgroundTask(.appRefresh) ─► collectAndSave("bgRefresh")
 Logs card ◄── BackgroundMonitor.getLogs() ◄── FileLogger.readLogs()  (re-read on every tick)
```

| Role | 🤖 Android | 🍎 iOS |
|---|---|---|
| Record + JSON | `nativecore/device/DeviceStatus.kt` (`org.json`) | `NativeCore/Device/DeviceStatus.swift` (`Codable`, `null` encoded by hand) |
| 3 providers + collector | `nativecore/device/DeviceProviders.kt` | `NativeCore/Device/DeviceProviders.swift` |
| JSONL | `nativecore/logging/FileLogger.kt` (`@Synchronized`, `filesDir`) | `NativeCore/Logging/FileLogger.swift` (`actor`, Application Support) |
| Loop + the plan's API | `nativecore/background/BackgroundMonitor.kt` (`object`, coroutine, `StateFlow`) | `NativeCore/Background/BackgroundMonitor.swift` (`@Observable`, `Task`) |
| Background mechanisms | `MonitorService.kt` (FGS `dataSync`, `START_STICKY`, `onTimeout`) · `MonitorWorker.kt` (`CoroutineWorker`) | `.backgroundTask(.appRefresh)` in `NativeDeviceMonitorApp.swift` |
| Declarations | `AndroidManifest.xml`: `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `FOREGROUND_SERVICE(_DATA_SYNC)`, `POST_NOTIFICATIONS`, `<service>` | `project.yml` → `Info.plist`: `UIBackgroundModes` (fetch, bluetooth-central), `BGTaskSchedulerPermittedIdentifiers` |
| UI | `ui/monitor/MonitorScreen.kt` + `MonitorStrings.kt` · `MainActivity` (tab + `POST_NOTIFICATIONS` request) | `UI/Monitor/MonitorView.swift` + `MonitorStrings.swift` |
| Tests | `test/…/logging/FileLoggerTest.kt` | `NativeDeviceMonitorTests/FileLoggerTests.swift` |

## 4. Behaviour table (Day 14) — measured 2026-10-05

Devices: 🤖 Pixel 9 Pro emulator, Android 17 (API 37) · 🍎 iPhone 17 Pro Max Simulator, iOS 26.5. Data = lines in `monitor.jsonl` (the `source` column).

| State | 🤖 In-app loop 10 s | 🤖 FGS 10 s | 🤖 WorkManager 15′ | 🍎 In-app loop 10 s | 🍎 BGAppRefresh |
|---|---|---|---|---|---|
| Foreground | ✅ steady 10 s | ✅ steady 10 s | ✅ ENQUEUED, the first run fires right when scheduled (12:03:40) | ✅ steady 10–11 s | — (only runs in the background) |
| Background (Home) | ✅ kept writing for 70 s (11:36:41 → 11:37:49, process not frozen) — **not guaranteed**: Android may freeze/kill a cached process at any time | ✅ 7 lines / 60 s, 10 s apart (11:31:16 → 11:32:17) | ⏳ waiting for the 15′ schedule | ❌ stops immediately: last line 11:52:58, Home at 11:53:01, 0 lines in 60 s (app suspended) | ❌ Simulator: `submit` error `BGTaskSchedulerErrorDomain error 1` · ⏳ real iPhone |
| Return to foreground | the loop is still running (same process) | still running | — | ✅ resumes immediately (11:54:21), an 83 s gap in the log | — |
| OS termination (process killed) | ❌ dead for good, doesn't restart (`am kill` 11:37:59 → 0 new lines); reopen = STOPPED, 0 records | ✅ `kill -9` 11:32:32 → the system recreates the service after ~4 s (`Scheduling restart of crashed service … 3826ms`, `restarted=true`), writing again from 11:32:40. `am kill` can't kill it (an FGS process isn't cached) | ✅ schedule survives (WorkManager keeps its own DB) | ❌ `simctl terminate`: 0 lines; reopen = STOPPED | ⏳ real device (Apple: can still be woken up) |
| Force-kill | ❌ | ❌ `am force-stop` 11:33:02: service gone, no restart, 0 lines in 40 s | ❌ schedule cancelled (the job disappears); **reopening the app makes WorkManager reschedule** (`WM-ForceStopRunnable: Application was force-stopped, rescheduling`) | ❌ (= swiping it away in the App Switcher) | ❌ swiped away → doesn't run until reopened (Apple docs) · ⏳ to measure |
| Screen off / Doze 10′ | ⏳ real device | ⏳ (FGS is exempt from Doze for CPU, network may be blocked) | ⏳ (Doze defers to a maintenance window) | ⏳ | ⏳ |
| Reboot | ❌ | ❌ (doesn't auto-start — by design, no BOOT_COMPLETED added) | ⏳ (WorkManager re-registers itself after boot) | ❌ | ⏳ |

**Conclusions**
1. A short, regular interval (10 s) while the app is in the background → **only an Android Foreground Service** can do it, at the cost of a notification. iOS has **no equivalent**; a few seconds in the background and the app is suspended.
2. Long periodic work (≥ 15′), timing drift acceptable → 🤖 WorkManager (survives OS kill + reboot) · 🍎 BGAppRefresh (**iOS doesn't guarantee an interval**, it may never run).
3. Force-kill is the user's decision: both platforms stop everything, there's no legitimate way to run again until the user opens the app — this app doesn't work around it.
4. The in-app loop on Android "seems" to keep running in the background on the emulator, but nothing keeps the process alive — don't rely on it.

Emulator notes: `wifiConnected=false` because the emulator's virtual Wi-Fi lacks `NET_CAPABILITY_VALIDATED`; the virtual Bluetooth stack (API 37) hung with `HCI timeout` during testing, making System UI and the app ANR on launch (main thread waiting on the Bluetooth binder) — an emulator bug, not caused by the Week 3 code.

## 5. Checks run

```bash
# Day 11 — the UI calls no device APIs (only mentioned in comments)
grep -rnE "BluetoothGatt|BluetoothLeScanner|ScanCallback|checkSelfPermission|ConnectivityManager|BatteryManager|File\(" native-device-monitor/android/app/src/main/java/com/learn/nativedevicemonitor/ui
grep -rnE "CBCentralManager|CBPeripheral|NWPathMonitor|BGTaskScheduler|FileManager|UIDevice" native-device-monitor/ios/NativeDeviceMonitor/UI
```

| Command | Result |
|---|---|
| `./gradlew :app:assembleDebug` | ✅ |
| `./gradlew :app:testDebugUnitTest` | ✅ 53 tests (FileLoggerTest 8 new) |
| `xcodebuild … build` (Simulator) | ✅ |
| `xcodebuild test … iPhone 17 Pro Max` | ✅ 50 tests / 4 suites (FileLoggerTests 8 new) |
| Re-run 2026-10-05 13:04 (uncommitted working tree) | ✅ Android 53/53 · iOS 50/50 (Swift Testing — the XCTest log saying "Executed 0" is normal, see the real numbers with `xcresulttool get test-results summary`) |

Commands to inspect / try by hand:

```bash
PKG=com.learn.nativedevicemonitor
adb shell am start -n $PKG/.MainActivity --es tab monitor          # open the Monitor tab directly
adb shell run-as $PKG sh -c 'wc -l files/monitor.jsonl; tail -3 files/monitor.jsonl'
adb shell dumpsys activity services $PKG | grep isForeground       # FGS
adb shell dumpsys jobscheduler | grep -A3 "u0a.*$PKG"              # WorkManager
adb shell run-as $PKG sh -c 'echo "not json" >> files/monitor.jsonl'  # broken line → "1 corrupt line(s) skipped"
adb shell am kill $PKG ; adb shell am force-stop $PKG

xcrun simctl launch booted com.learn.NativeDeviceMonitor -openMonitor
cat "$(xcrun simctl get_app_container booted com.learn.NativeDeviceMonitor data)/Library/Application Support/monitor.jsonl"
```

🍎 BGAppRefresh on a real iPhone: run from Xcode → Home → Pause (⌃⌘Y) → paste the LLDB command shown in the *BGAppRefresh* card → Continue → Logs get a `[bgRefresh]` line.

## 6. Remaining

- ⏳ Real devices: the Doze / Reboot rows, the 🍎 BGAppRefresh column (AC6), FGS on Samsung (One UI tends to kill more aggressively — see dontkillmyapp.com).
- ✅ Day 10 (`MediaManager` capture / pick + `ApiClient` GET / POST) was added afterwards as the **Media** + **API** tabs — see [task 006](006-final-e2e-cleanup.md) for the E2E results.
- References: [FGS](https://developer.android.com/develop/background-work/services/fgs) · [FGS types](https://developer.android.com/develop/background-work/services/fgs/service-types) · [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent) · [BackgroundTasks](https://developer.apple.com/documentation/backgroundtasks) · [Starting and terminating tasks during development](https://developer.apple.com/documentation/backgroundtasks/starting-and-terminating-tasks-during-development)
