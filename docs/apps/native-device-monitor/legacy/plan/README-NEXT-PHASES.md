[🏠 README](../index.md) · [Final — task 006](../tasks/006-final-e2e-cleanup.md)

# Plan — what is still missing vs. the curriculum + next work

> ⏸ **Re-planned 2026-10-14 — this plan ran more than 1 week late.** Phases B, C, D and all Android work are **parked** until real devices
> and a BLE peripheral are available. A1/A3/A4 are folded into the new 2-week Swift plan:
> **[Swift 2 weeks — overview](https://github.com/tucarmd/native-mobile-learning/blob/main/docs/plan/README-SWIFT-2W-00-OVERVIEW.md)** (status of every item: [§5 there](https://github.com/tucarmd/native-mobile-learning/blob/main/docs/plan/README-SWIFT-2W-00-OVERVIEW.md#5-kế-hoạch-cũ--những-gì-giữ-và-những-gì-tạm-dừng)).

> Created: 2026-10-08 · Compares the code in this repo with the learning curriculum (Setup → Week 1 → Week 2 → Week 3 → Final, Day 01–17).
> The Done rule is the same as in the curriculum: **build OK + run for real + see the result yourself (screenshot / log)**. Code written ≠ done.
> Paths below are relative to the app folder `native-device-monitor/`.

## 0. Gap summary

| Curriculum phase | In this repo | Still missing |
|---|---|---|
| Setup | ✅ Gradle + XcodeGen, CLI builds, unit tests on both platforms | — |
| Week 1 (Day 01–05) | Kotlin/Swift knowledge used in real code; 5-tab TabView, list, loading | Standalone fundamentals exercises · readable lifecycle logs · **a second screen (push navigation)** |
| Week 2 (Day 06–10) | Permissions · BLE scan/connect/GATT/notify/write/MTU · Media · API | Runs on **real devices**: deny/retry, BLE E2E, camera capture, the whole Android column |
| Week 3 (Day 11–15) | NativeCore · providers · FileLogger · BackgroundMonitor · FGS/WorkManager/BGAppRefresh · Monitor tab | Measurements on real devices: Doze, reboot, BGAppRefresh |
| Final (Day 16–17) | Refactor checklist · clean build · E2E on the Simulator | E2E on real devices · D-02 · D-03 · retro + Level |

---

## Phase A — Week 1 Fundamentals & UI

Week 1 of the curriculum was studied in a separate repo. This phase brings the minimum into **this app** so the repo covers all 4 weeks.

| # | Work | File / where | Done means |
|---|---|---|---|
| A1 | Day 01–02 fundamentals exercises: Kotlin console (null safety, data class, sealed/when, lambda, collections, coroutines) + Swift (optionals, struct vs class, protocol, enum, closure, async/await, errors) — one small function per concept, **using the app's BLE models as examples** (`BluetoothDeviceInfo`, `PermissionStatus`) | `learning/week1/kotlin/` (Gradle JVM module or `.kts`) · `learning/week1/swift/` (Swift package, `swift run`) | `./gradlew :week1:run` and `swift run` print every concept; a short README maps concept ↔ the real app file that uses it |
| A2 | Readable lifecycle logs (Day 03–04): Android logs `onCreate → onStart → onResume → onPause → onStop → onDestroy`; iOS logs `scenePhase` active / inactive / background | `MainActivity.kt` (add `onPause` + a `Lifecycle` log tag) · `NativeDeviceMonitorApp.swift` (`AppLog` in `.onChange(of: scenePhase)`) | Logcat / Xcode console show the full sequence when opening the app → Home → reopening → rotating; the order is recorded in a table in this doc |
| A3 | Second screen (Day 05): **Device detail** — tap a device in the Scanner → push a detail screen (name, id, RSSI, advertised services, manufacturer data, Connect button) → Back to the list with its state intact | 🤖 `ui/scanner/DeviceDetailScreen.kt` + Navigation Compose (`NavHost`) · 🍎 `UI/Scanner/DeviceDetailView.swift` + `NavigationLink(value:)` / `.navigationDestination` | Detail screen + Back work on the emulator and the Simulator, with screenshots; data passed by id (not a large object); a unit test for the display formatter |
| A4 | Week 1 gate from the curriculum README — answer in your own words: null safety vs optionals, data class vs struct/class, lambda vs closure | `docs/plan/week1-gate.md` | All 7 gate rows ticked in both columns |

## Phase B — Week 2 on real devices (Day 06–10)

Needed: Samsung A32 + a real iPhone + a peripheral (nRF Connect GATT server / ESP32 `BLE_notify` / a `CMD-…` dongle).

| # | Work | Done means |
|---|---|---|
| B1 | Permission **Deny → Retry → Settings** for both BLE + Camera (Android: deny twice → *permanently denied*) | The Permission table in [task 006 §2](../tasks/006-final-e2e-cleanup.md) fully ✅ in both columns, with screenshots |
| B2 | BLE E2E: Scan · Connect · Discover · Read · Notify · Write `ATZ\r` · Disconnect · Unexpected disconnect · Bluetooth off | The Bluetooth table in task 006 fully ✅; a *Received Data* screenshot with a `NOTIFY` ELM327 reply |
| B3 | Camera Capture + Cancel on real devices; the 🤖 Media + API column | The Camera/API tables in task 006 fully ✅ |
| B4 | Retake the iOS README screenshots on a real device (replacing the Simulator's `NO BLUETOOTH` shots) | `docs/assets/*_ios.png` are real-device screenshots |

## Phase C — Week 3 on real devices (Day 14)

| # | Work | Done means |
|---|---|---|
| C1 | Android Doze: `adb shell dumpsys deviceidle force-idle` while the FGS + WorkManager are running | Real log times recorded in the behaviour table of [task 005 §4](../tasks/005-week3-nativecore-background.md) |
| C2 | Android reboot: WorkManager keeps its schedule after a restart; the FGS doesn't auto-start (by design) | Task 005's table has a *Reboot* row |
| C3 | iOS BGAppRefresh on a real iPhone (`e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateLaunchForTaskWithIdentifier:…]` in the debugger) | The `FileLogger` log has a record from a background run |

## Phase D — Finishing Final (Day 16–17)

| # | Work | Done means |
|---|---|---|
| D1 | Re-check **D-02** (ANR from `radioState` on the main thread) on the A32 | Doesn't reproduce → close it; reproduces → move `radioState` off the main thread + retest |
| D2 | **D-03** split `BluetoothClientService` (~700 lines): `BleScanner` · `GattConnection` · `GattOperationQueue` — **only after B2** so a real device can retest it | Each file < ~300 lines, the existing unit tests stay green, B2 re-run ✅ |
| D3 | Refactor checklist #6 *Consistent BLE state* | ✅ after B2 |
| D4 | `retro.md` + the Level column (written by the learner — Claude doesn't fill it in) | The file exists in `docs/plan/` |

## Phase E — Beyond the curriculum (optional)

Taken from the Day 18+ backlog and [HOW-TO §8.5](../README-HOW-TO-IMPLEMENT.md#85-after-task-004-not-done-yet). Only once A–D are done.

| # | Work |
|---|---|
| E1 | Auto-reconnect + re-subscribe after an unexpected disconnect; turn the CCCD off before disconnecting (bonded devices) |
| E2 | Local notification when the Monitor detects low battery / a lost BLE connection |
| E3 | Log viewer: filter by level / time, export the JSONL file (share sheet) |
| E4 | Shared modal / side menu (ported from the Kotlin `device-monitor-manager` to Compose + SwiftUI) |
| E5 | UI tests: Compose `createComposeRule` + XCUITest for the Permissions → Scanner → Detail flow |

---

## Suggested order when running it

1. **A3** (detail screen) → **A2** (lifecycle log) → **A1** (exercises) → **A4** — doable right away on the emulator / Simulator.
2. Once real devices + a peripheral are available: **B1 → B2 → B3 → B4 → C1–C3 → D1 → D3**.
3. **D2** (split the BLE file) after B2 is green.
4. **D4** retro — written by the learner.
5. Phase E as time allows.
