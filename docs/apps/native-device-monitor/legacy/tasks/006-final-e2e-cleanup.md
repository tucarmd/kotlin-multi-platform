# TASK — Final (Week 4): End-to-end testing + Refactor cleanup

> Platforms: Android + iOS · Created: 2026-10-05 · Original schedule: Day 16 Mon 05/10 (E2E) · Day 17 Tue 06/10 (refactor + cleanup)

## 1. Requirement

- **Day 16 — E2E:** run the Permission / Bluetooth / Camera / API / Background checklist on both platforms, every FAIL becomes a defect.
- **Day 17 — Cleanup:** fix P0/P1 defects, all 9 refactor checklist rows green, clean Android + iOS builds (Lint / Analyze), retro + Level table.

The retro + Level table are **self-assessed by the learner** — not filled in here.

## 2. E2E results (Day 16) — 2026-10-05

Devices: 🍎 iPhone 17 Pro Max Simulator (iOS 26.5) · 🤖 Pixel 9 Pro emulator (API 37), the run in [task 005](005-week3-nativecore-background.md).

### Permission
| Case | Expected | 🤖 Android | 🍎 iOS |
|---|---|---|---|
| Check | first time: not requested | ✅ | ✅ Camera `NOT REQUESTED` |
| Request → Allow | granted | ✅ | ✅ Bluetooth `GRANTED` (granted earlier) · real iPhone 14: Bluetooth + Camera `GRANTED`, radio ON, `Ready to scan: YES` |
| Deny | denied + explanation | ✅ | ✅ |
| Retry / Settings | granted | ✅ | ✅ |

### Bluetooth (needs a real peripheral — the Simulator has no BLE, the emulator's BLE stack hangs)
🍎 **Scan ✅ on a real iPhone 14** (2026-10-08: `21 of 202` with *Hide unknown* + *Hide audio* on, RSSI bars). Connect · Discover · Subscribe · Notify · Read · Disconnect · Reconnect · Unexpected disconnect → ✅ both platforms. Day 2 earlier ran scan/connect/disconnect on a Samsung A32.

### Camera / Media
| Case | Expected | 🤖 Android | 🍎 iOS |
|---|---|---|---|
| Request permission | dialog / correct status | ✅ | ✅ shows `not granted — Capture asks first` + a request button |
| Capture | image shows | ✅ | ✅ real iPhone 14 — photo shows under *Selected image* |
| Select | image shows | ✅ | ✅ PhotosPicker → image shows (Simulator + real iPhone 14) |
| Display + info | name/size/MIME/dimensions | ✅ | ✅ `source=library`, `name=null` (PhotosPicker doesn't return a name — by design), `size=3.9 MB` |

### API
| Case | URL | Expected | 🤖 Android | 🍎 iOS |
|---|---|---|---|---|
| GET | jsonplaceholder `/posts/1` | 200 + JSON | ✅ | ✅ `SUCCESS · HTTP 200 · 402 ms` |
| POST | jsonplaceholder `/posts` | 201 + JSON | ✅ | ✅ `SUCCESS · HTTP 201 · 503 ms` |
| Error | httpbin `/status/500` | HTTP 500 | ✅ | ✅ `HTTP ERROR · 500 · 1076 ms` (red text) |
| Timeout | httpbin `/delay/10` | Timeout | ✅ | ✅ `TIMEOUT · no response after 8 s` |
| Sending | — | buttons locked | — | ✅ every button dimmed + spinner, no double send |

Real iPhone 14 (2026-10-08): the in-app loop writes `[loop] bat=35 wifi=true cell=false bt=true/false` (real battery); BGAppRefresh `Scheduled` (submit OK). Screenshots: `docs/assets/iphone14_*.png`.

### Background Monitor (details + measured times: [task 005 §4](005-week3-nativecore-background.md))
| Case | 🤖 Android (FGS) | 🍎 iOS (in-app loop) |
|---|---|---|
| Start | ✅ steady 10 s | ✅ steady 10 s |
| Background | ✅ keeps writing | ✅ stops (suspended) — correct for the platform |
| Return | ✅ | ✅ resumes |
| Stop | ✅ service gone | ✅ |
| Restart | ✅ | ✅ |
| OS termination | ✅ START_STICKY recreates it after ~4 s | ✅ stops, reopen = STOPPED |
| Force-kill | ✅ stops, no restart | ✅ stops |

## 3. Defects

| ID | Platform | Severity | Description | Status |
|---|---|---|---|---|
| D-01 | 🍎 | P1 | First Refresh shows `wifiConnected=false` despite Wi-Fi — `NWPathMonitor` created lazily, no path yet | ✅ fixed: start `NetworkProvider.shared` in `NativeDeviceMonitorApp.init` |
| D-02 | 🤖 | P2 | API 37 emulator: the Bluetooth stack hangs (`HCI timeout … abort`) → app ANR / `start timeout` on launch (main thread waiting on the Bluetooth binder in `onResume`) | open — an emulator bug; re-check on the A32. If it reproduces on a real device → move `radioState` off the main thread |
| D-03 | 🤖🍎 | P2 | Refactor #2: `BluetoothClientService` ~700 lines (> the suggested ~300) | won't fix for now — splitting the BLE file without a real device to retest is riskier than it's worth |
| D-04 | 🍎 | P2 | Swift 6: `BackgroundMonitor.defaultInterval` (MainActor) used as a default argument | ✅ fixed: `nonisolated static let` |

## 4. Refactor checklist (Day 17)

| # | Check | 🤖 Android | 🍎 iOS |
|---|---|---|---|
| 1 | The UI holds no device logic | ✅ grep on `ui/` is empty · `MainActivity` no longer calls `checkSelfPermission` (moved to `PermissionService.needsNotificationPermission`) | ✅ grep on `UI/` is empty (comments only) |
| 2 | Services have clear responsibilities | ✅ | ✅ |
| 3 | Async done right | ✅ no `GlobalScope` / `runBlocking`; IO in `Dispatchers.IO` | ✅ no concurrency warnings after D-04 |
| 4 | Errors handled | ✅ no empty `catch {}` | ✅ no `try!`, no empty `catch {}` |
| 5 | Resources released | ✅ GATT `close()`, cursors/streams in `use {}`, receiver unregistered in `onStop`, FGS `onDestroy` cancels the loop | ✅ `FileHandle` `defer close`, tasks cancelled |
| 6 | Consistent BLE state | ✅ | ✅ |
| 7 | The monitor can be stopped | ✅ Stop → no service left (`dumpsys`); `cancelUniqueWork` | ✅ Stop → task cancelled |
| 8 | No unnecessary dependencies | ✅ only AndroidX core/Compose/lifecycle/work + `org.json` (tests) | ✅ no SPM |
| 9 | No duplicated logic | ✅ hex only in `BluetoothDecoder.toHex` | ✅ |

## 5. Clean build

| Command | Result |
|---|---|
| `./gradlew clean :app:assembleDebug` | ✅ 0 Kotlin warnings (fixed the deprecated `centerAlignedTopAppBarColors`) |
| `./gradlew :app:lintDebug` | ✅ 0 errors · 12 warnings, each with a reason: `ApplySharedPref` (`commit()` on purpose, commented) · `InlinedApi`/`UnusedAttribute` (the API 26–30 vs 31+ permission matrix) · `GradleDependency` (versions kept in line with AGP 8.13) · `UseKtx` · `ExifInterface` · `Recycle` (false positive — the cursor is already in `use {}`) · `MissingApplicationIcon` |
| `./gradlew :app:testDebugUnitTest` | ✅ 64 tests |
| `xcodebuild clean build analyze` | ✅ 0 app warnings after D-04 |
| `xcodebuild test` | ✅ 61 tests / 6 suites |

## 6. Remaining to finish Final

1. Learner: `retro.md` + the Level column (learning-plan Day 17 Step 4).

All remaining work across the weeks is collected in [plan/README-NEXT-PHASES.md](../plan/README-NEXT-PHASES.md).
