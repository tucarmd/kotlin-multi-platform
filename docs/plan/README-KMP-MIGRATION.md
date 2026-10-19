[🏠 README](../../README.md) · Old app: [tucarmd/native-mobile-learning](https://github.com/tucarmd/native-mobile-learning) (`native-device-monitor/`)

# Plan — Native Device Monitor → KMP core + native UI (Jetpack Compose · SwiftUI)

> Created: 2026-10-16 · Status: **Approved 2026-10-16 · P0–P7 ✅ (P3 P4 P5 P7 Android only) · 2026-10-19 D1 changed → native UI, P8–P12 ✅ (P12 checked on the simulator, real iPhone pending)**
> Native-UI switch: [spec](README-KMP-NATIVE-UI-SPEC.md) · [plan P8–P12](README-KMP-MIGRATION.md)
> Step-by-step per phase (what the dev did, which layer): [Từng phase: dev đã làm gì](../README-KMP-PHASES.md)
> Learning path: [Học KMP qua Device Pulse](https://tucarmd.github.io/kotlin-multi-platform/lessons/02-setup/) (14 lessons) +
> official docs [kotlinlang.org/docs/multiplatform](https://kotlinlang.org/docs/multiplatform/get-started.html).
> Done rule (same as the curriculum): **build OK + run for real + screenshot read**. Code written ≠ done.

## 1. Goal · scope · out of scope

**Goal:** rebuild Native Device Monitor so that **everything that can be shared is written once in Kotlin (KMP)**, while
each platform keeps its **own native UI**. The old app is written twice end to end (Kotlin/Compose ~6.5k lines +
Swift/SwiftUI ~5.6k lines). Now: models, rules, ViewModels, HTTP, logger, monitor loop once in `shared/`; UI twice
(Jetpack Compose + SwiftUI), both reading the same ViewModels.

> Until 2026-10-19 (P0–P7) the goal was "only Kotlin, Compose Multiplatform UI, Swift = 20-line shell". Changed in
> P8–P11 — see [spec](README-KMP-NATIVE-UI-SPEC.md).

**In scope** — feature parity with tasks 001–006, same 5 tabs:

| Tab | Features to keep |
|---|---|
| Permissions | BLE + Camera check / request / denied / permanently denied → Settings, live BT radio, *Ready to scan* |
| Scanner | scan 10 s de-dup, name filter, hide unknown / audio, select row + Connect/Disconnect, GATT discover, read + decode, notify (CCCD), Received Data (50), write Text/Hex + ELM327 presets, MTU, unexpected-disconnect cleanup |
| Media | camera capture + photo picker, image + info, cancel |
| API | GET / POST / HTTP 500 / timeout, buttons locked in flight |
| Monitor | battery / network / BT providers, JSONL FileLogger, BackgroundMonitor (🤖 FGS + WorkManager · 🍎 BGAppRefresh) |

**Out of scope (→ backlog):** Pomodoro (task 007, iOS-only, not committed yet — port later as an MVVM exercise),
Phase A–D items of `docs/plan/README-NEXT-PHASES.md` in the old repo, desktop/web targets, store release, new features.
The old `native-device-monitor/` (repo native-mobile-learning) stays untouched as the reference ("before" version).

### Who writes what: shared KMP vs native

**Short answer:** the **UI is native on each platform**: Android draws with **Kotlin + Jetpack Compose** (`androidApp/`),
iOS with **Swift + SwiftUI** (`iosApp/`). **Only the shared part is KMP** (`shared/`): models, rules, the 5 ViewModels,
interfaces, Ktor, FileLogger, monitor loop. Both UIs read the **same ViewModel state**, so texts, enabled buttons and
filters are identical. Platform SDK code: Android in Kotlin (`shared/androidMain`), iOS in **Swift** (`iosApp/iosApp/Platform/`, P12) behind small Kotlin adapters in `shared/iosMain`.

| Layer | Folder | Language | What lives there | Runs on |
|---|---|---|---|---|
| **UI Android** | `androidApp/…/ui/`, `App.kt` | Kotlin · Jetpack Compose | `PermissionScreen`, `ScannerScreen` + `GattSection`, `MediaScreen`, `ApiScreen`, `MonitorScreen`, theme | 🤖 only |
| **UI iOS** | `iosApp/iosApp/UI/` | Swift · SwiftUI | `PermissionView`, `ScannerView` + `GattSection`, `MediaView`, `ApiView`, `MonitorView`, theme, `RootView` (TabView) | 🍎 only |
| **ViewModels** | `shared/commonMain/…/presentation/` | Kotlin (KMP) | `AppViewModel` + `Permission/Scanner/Media/Api/MonitorViewModel`: `state: StateFlow<XxxUiState>` + actions · `*Strings` | 🤖 + 🍎 (shared) |
| **Logic + interfaces** | `shared/commonMain/…/core/` | Kotlin (KMP) | models + rules (`GattModel`, `BluetoothDecoder`, `DeviceStatus`…), interfaces (`PermissionController`, `BleClient`, `MediaPicker`, `ApiService`, `DeviceMonitor`), `KtorApiService`, `LoopDeviceMonitor`, `FileLogger` | 🤖 + 🍎 (shared) |
| **Android SDK** | `shared/androidMain/` | Kotlin · Android SDK | `AndroidPermissionController`, `AndroidBleClient`, `AndroidMediaPicker`, `AndroidDeviceStatusSource`, `MonitorService` + `MonitorWorker` | 🤖 only |
| **iOS SDK** | `iosApp/iosApp/Platform/` | Swift · Apple frameworks | `PermissionNative` (CBManager / AVCaptureDevice), `BleNative` (CoreBluetooth), `MediaNative` (UIImagePickerController / PHPicker), `DeviceNative` (UIDevice, NWPathMonitor, BGTaskScheduler) | 🍎 only |
| **iOS adapters** | `shared/iosMain/…/platform/` | Kotlin/Native | `IosPermissionController`, `IosBleClient`, `IosMediaPicker`, `iosDeviceMonitor`: hold `StateFlow` + rules, implement the `core/` interfaces, call the Swift objects | 🍎 only |
| Bridges | `iosApp/iosApp/App/` | Swift | `AppModel` (owns `AppViewModel`, `clear()` on deinit) · `Observed<T>` (Kotlin `StateFlow` → `@Published`, via SKIE) | 🍎 |

Per tab:

| Tab | 🤖 UI (Compose) | 🍎 UI (SwiftUI) | Shared ViewModel + logic | 🤖 SDK | 🍎 SDK |
|---|---|---|---|---|---|
| Permissions | `PermissionScreen` | `PermissionView` | `PermissionViewModel` · `PermissionController` | `AndroidPermissionController` ✅ | `PermissionNative` + `IosPermissionController` ✅ |
| Scanner | `ScannerScreen`, `GattSection` | `ScannerView`, `GattSection` | `ScannerViewModel` · `BleClient`, `GattModel`, `ScannerActions` | `AndroidBleClient` ✅ | `BleNative` + `IosBleClient` ✅ (needs real iPhone) |
| Media | `MediaScreen` | `MediaView` | `MediaViewModel` · `MediaPicker` | `AndroidMediaPicker` ✅ | `MediaNative` + `IosMediaPicker` ✅ |
| API | `ApiScreen` | `ApiView` | `ApiViewModel` · **`KtorApiService` (100% shared)** | OkHttp engine ✅ | Darwin engine ✅ |
| Monitor | `MonitorScreen` | `MonitorView` | `MonitorViewModel` · `DeviceMonitor`, `LoopDeviceMonitor`, `FileLogger` | `MonitorService` + `MonitorWorker` ✅ | `DeviceNative` + shared `LoopDeviceMonitor` ✅ |

Rule of thumb: no platform SDK and no drawing → `shared/commonMain`. Drawing → the platform's UI module. Platform SDK →
an implementation of a `core/` interface, picked in one file per platform (`AndroidServices.kt` / `IosServices.kt`).

## 2. Architecture (since P11: "Share logic, native UI")

```
native-device-monitor-kmp/            ← own private repo tucarmd/native-device-monitor-kmp, KMP-App-Template layout (AGP 9)
├── androidApp/       Kotlin + Jetpack Compose: MainActivity → App(AppViewModel) → ui/ (5 screens, theme)
├── shared/src/       KMP library, no Compose
│   ├── commonMain/   core/ (model · rules · interfaces · Ktor · FileLogger · loop) · presentation/ (ViewModels · strings)
│   ├── androidMain/  Android implementations: permissions, BLE, camera/picker, battery/network, FGS + WorkManager
│   ├── iosMain/      Kotlin adapters (IosPermissionController, IosBleClient, IosMediaPicker, iosDeviceMonitor) + helpers for Swift
│   └── commonTest/   kotlin.test — rules + ViewModel tests (78)
└── iosApp/           Swift + SwiftUI: iOSApp → AppModel → RootView (TabView) → UI/ (5 views, theme) · Platform/ (Apple SDK in Swift) · Info.plist
```

Platform code is behind **interfaces in commonMain + one implementation per platform** (the skill's advice: plain interface
instead of `expect class`). Dependencies one way: `UI (Compose / SwiftUI) → ViewModel (shared) → core interfaces`.

Library choices (versions from the wizard / lesson 5, not hand-picked):

| Need | Old | New |
|---|---|---|
| UI | Compose (🤖) + SwiftUI (🍎) | ~~Compose Multiplatform~~ (P0–P7) → **Jetpack Compose (🤖) + SwiftUI (🍎)** since P11 |
| ViewModel / nav | hand-rolled | shared `SharedViewModel` (own ~10-line base, see spec §3.3) + plain tab state |
| Swift ↔ Kotlin Flow | — | **SKIE** (StateFlow → AsyncSequence) + `Observed<T>` |
| HTTP | HttpURLConnection / URLSession | **Ktor** (OkHttp / Darwin) + `ktor-client-mock` tests (lesson 11) |
| JSON | org.json / Codable | kotlinx.serialization |
| Files (logger) | File / FileManager | kotlinx-io (or okio) |
| BLE | BluetoothGatt / CoreBluetooth | **decision D2** below |
| Logs | Log / os.Logger | Kermit |

## 3. Phases (each ends with something you can run + screenshot)

| # | Phase | Lesson | Work | Done means |
|---|---|---|---|---|
| P0 ✅ | Setup | 2–3 | Wizard layout: Android + iOS, **Share UI**, package `com.learn.ndmkmp` (D4). Hello screen. | Runs on Android emulator **and** iOS simulator, 2 screenshots — [kmp_p0_android.png](../assets/kmp_p0_android.png) · [kmp_p0_ios.png](../assets/kmp_p0_ios.png) |
| P1 ✅ | Shared core | 4–5 | Move the 8 pure files to `commonMain` (GattModel, BluetoothDecoder, BluetoothLookup, BluetoothScanFilter, BluetoothDeviceInfo, DeviceStatus, MediaInfo, PermissionStatus) + ScannerActions; port their tests to `kotlin.test` | `./gradlew :shared:allTests` green on JVM/Android **and** iOS simulator — 52/52 on both |
| P2 ✅ | UI shell | 6–8 | Theme tokens + AppCard/AppPrimaryButton/AppHintText, 5-tab bottom bar, every screen on **fake** data behind 5 interfaces | Both platforms show all 5 tabs — [android](../assets/kmp_p2_android_tabs.png) · [ios](../assets/kmp_p2_ios_tabs.png) · [fake GATT](../assets/kmp_p2_android_gatt_fake.png) |
| P3 🤖✅ 🍎→P12 | Permissions | 9 | `PermissionService` interface; 🤖 ActivityResult (API ≤30 vs 31+ branch kept) · 🍎 CBManager + AVCaptureDevice authorization; open Settings | Deny → retry → Settings path works on both (real devices) |
| P4 🤖✅ 🍎→P12 | BLE | — (app #5 "OBD Link") | `BleClient` interface: scan → connect → discover → read → notify → write → MTU → disconnect; platform rules from the README table (Android GATT queue, iOS 15 s connect timeout, radio-off cleanup) | Full Scanner E2E against nRF Connect advertiser on **Samsung A32 + real iPhone** |
| P5 🤖✅ 🍎→P12 | Media | — | 🤖 TakePicture + PickVisualMedia · 🍎 UIImagePickerController / PHPicker presented from Kotlin; image → `ImageBitmap` + MediaInfo | Capture + pick + cancel on real devices (simulator has no camera) |
| P6 ✅ | API | 11 | Ktor client: GET/POST/500/timeout; MockEngine tests | Tab works both platforms, tests green |
| P7 🤖✅ 🍎→P12 | Monitor + background | 9, 12 | Providers (battery/network/BT) · FileLogger JSONL · 🤖 FGS + WorkManager in androidMain · 🍎 BGTaskScheduler registered at launch (one call from `iOSApp.swift`) | Logs written while in background on both; screenshot of Monitor logs |
| P8 ✅ | Shared ViewModels | 8 | State + actions of the 5 tabs move out of the composables into `shared/presentation/` (`AppViewModel` + 5 ViewModels, `*Strings`); `PickedImage` carries bytes | 78/78 tests JVM + iOS, both apps unchanged on screen — [android](../assets/kmp_r1_android_tabs.png) · [ios](../assets/kmp_r1_ios_tabs.png) · commits say **P8** |
| P9 ✅ | SwiftUI shell + API tab | — | **SKIE** (Flow → AsyncSequence) + `Observed<T>`, `AppModel`, `RootView`; API tab in SwiftUI; Xcode synchronized folder, iOS 17 | Maestro GET 200 + 500 on the simulator — [ảnh](../assets/kmp_r2_ios_swiftui_api.png) · commits say **P9** |
| P10 ✅ | SwiftUI all 5 tabs | — | Permissions, Scanner + GATT, Media, Monitor in SwiftUI on the shared ViewModels; `InfoRow` for Swift | Maestro through all 5 tabs — [ảnh](../assets/kmp_r3_ios_swiftui_tabs.png) · commits say **P10** |
| P11 ✅ | Compose → androidApp | — | Compose UI moves to `androidApp/` (Jetpack Compose BOM); `shared/` drops Compose Multiplatform | 78/78 tests, A32 5 tabs + Maestro — [ảnh](../assets/kmp_r4_android_compose_tabs.png) · commits say **P11** |
| P12 ✅ code · ⏳ iPhone | iOS real SDK in Swift | 9, 12 | = the 🍎 half of P3 P4 P5 P7. P12a permissions · P12b camera/PHPicker · P12c battery, network, BGAppRefresh · P12d CoreBluetooth. Swift `iosApp/Platform/*Native` + Kotlin adapters in `iosMain` | 82/82 tests (+ `IosAdaptersTest`), simulator checked; **real iPhone not yet** · commits say **P12a–P12d** |
| P13 | Final | 13–14 | Real-device run of P12 (capture, BLE GATT vs nRF Connect, BGAppRefresh), task 006 E2E checklist, lint clean, README + "old vs new" table | All rows ✅ with screenshots, README updated |

Order matters: P0–P2 can be checked on simulator/emulator only; P3–P5, P7, P12 need **real devices** (BLE, camera, background).
P8–P12 = the switch to native UI (decision D1 changed 2026-10-19, see [spec](README-KMP-NATIVE-UI-SPEC.md)); their commits were written with the temporary labels **P8–P12**, kept in git history.
Rule per phase: one commit, build OK + run for real + screenshot read, then stop for an OK.

## 4. Decisions for you (review these)

| # | Question | Recommended | Alternative |
|---|---|---|---|
| D1 | UI | ~~**Compose Multiplatform** — only Kotlin, matches lesson 2 "Share UI"~~ → **changed 2026-10-19: KMP core + native UI** (Jetpack Compose + SwiftUI) | — |
| D2 | BLE | **Write our own `BleClient`** in androidMain (reuse current Kotlin) + iosMain (CoreBluetooth in Kotlin/Native) — keeps the platform-difference learning | **Kable** (JuulLabs) — far less code, one API, used in app #5 of the lesson site; hides the GATT queue / CCCD details |
| D3 | Where | ~~New folder in native-mobile-learning~~ → **changed 2026-10-16: own private repo `tucarmd/native-device-monitor-kmp`** | — |
| D4 | IDs | `com.learn.ndmkmp` (both platforms) — can be installed next to the old app | reuse `com.learn.nativedevicemonitor` (replaces old app on device) |
| D5 | Pomodoro | backlog, after P13 | port in P2 |

## 5. Risks

- **CoreBluetooth from Kotlin/Native** (if D2 = own code): delegates must subclass `NSObject`; ObjC interop is verbose. Fallback: Kable.
- ~~**iOS look:** Compose on iOS draws Material~~ — gone since P10: iOS is SwiftUI with native controls.
- **Two UIs drift apart:** same ViewModel + strings keep texts and rules equal; only drawing can differ. Compare screenshots per tab.
- **BGTaskScheduler** must be registered before launch finishes → keep that one line in `iOSApp.swift`; Info.plist needs `BGTaskSchedulerPermittedIdentifiers` + background modes.
- **Gradle/AGP:** wizard may generate AGP 9 (`com.android.kotlin.multiplatform.library`); old app is on AGP 8.13. Follow the wizard; skill `kotlin-tooling-agp9-migration` if it breaks.
- **iOS build time** (Kotlin/Native link) — skill `kotlin-tooling-native-build-performance`.

## 6. Log

- **2026-10-16 · P0 done.** Skeleton = official KMP-App-Template layout (`shared/` + `androidApp/` + `iosApp/`, AGP 9.1,
  Kotlin 2.4.10, Compose Multiplatform 1.11.1), copied from the wizard-generated Device Pulse bootstrap, renamed to
  `com.learn.ndmkmp`, minSdk 26 (same as the old app). One `App()` in `commonMain` + `platformName()` expect/actual.
  Android: `:androidApp:assembleDebug` ✅, runs on Pixel 8 emulator (API 34). iOS: `xcodebuild` ✅, runs on iPhone 17
  Simulator (iOS 26.5).
- **2026-10-16 · moved to its own private repo** `tucarmd/native-device-monitor-kmp` (was a folder in native-mobile-learning).
- **2026-10-16 · P1 done.** 9 pure files → `shared/src/commonMain/.../core/` (+ `ui/scanner/ScannerActions.kt`).
  JVM-only bits replaced: `java.util.UUID` → `String` in `BluetoothDecoder.shortUuid`, `"%02X".format` → own `hex()`,
  `Charsets` → `decodeToString()` / `encodeToByteArray()`, `"%.1f".format(Locale.US)` → `oneDecimal()`,
  `org.json` → kotlinx.serialization 1.11.0 (same JSONL: `null` for unmeasured, `source` omitted when null).
  Tests: 50 old JUnit cases → `kotlin.test` in `commonTest` + 2 new (short line, broken line) = **52 pass on
  `testAndroidHostTest` and `iosSimulatorArm64Test`**. FileLogger file-I/O tests wait for P7, ApiClient tests for P6.

- **2026-10-16 · P2 done.** All UI → `commonMain/ui/` (theme, common components, 5 screens). Each screen talks to one
  commonMain interface: `PermissionController` · `BleClient` · `MediaPicker` · `ApiService` · `DeviceMonitor`, wired in
  one place (`AppServices` in `App.kt`); P2 uses `fake/Fake*` for all five. `App()` = Scaffold + top bar + 5-tab
  `NavigationBar` (moved out of the old `MainActivity`). JVM-only bits replaced: `SystemClock` → `monotonicMs()`,
  `SimpleDateFormat` → `clockText()` / `isoLocalTimestamp()` (expect/actual; iOS `NSDateFormatter` en_US_POSIX),
  `"%02X".format` → `toString(16)`. New dep: `material-icons-core` (tab + search icons).
  Strings stay as `*Strings.kt` objects next to each screen (old rule 5.3), not Compose resources — moving them is
  optional later. `--es tab <name>` (Android) / `-tab <name>` (iOS launch arg) opens a tab directly for screenshots.
  Checked: `:androidApp:assembleDebug` + `xcodebuild` ✅, 52/52 tests on both targets still green, Pixel emulator
  (API 34) + iPhone 17 Simulator show all 5 tabs; on Android the fake flow request → turn on radio → scan → select →
  connect → discover (Battery + FFE0 UART, MTU 247) and Pick from library work. Fix found on iOS: "Permissions" tab
  label wrapped to 2 lines → `maxLines = 1`, 11 sp.

- **2026-10-16 · P6 done (both platforms).** `KtorApiService` in commonMain (OkHttp / Darwin engines), 5-branch
  `ApiResult`, "no network" detected per platform (expect/actual). Prepared for P7: `FileLogger` (kotlinx-io) +
  `LoopDeviceMonitor`. `createAndroidServices` / `createIosServices` = the one place per platform that picks real vs
  fake. 67 tests per target green. Emulator against the real network: GET 200, POST 201, HTTP 500, timeout 8 s.
- **2026-10-16 · P3 Android done.** `AndroidPermissions` + `AndroidPermissionController` + `AndroidBluetoothRadio`,
  BLE/CAMERA matrix in the shared manifest. Pixel 8 API 34: NOT REQUESTED → DENIED → PERMANENTLY DENIED → Settings,
  radio card follows `cmd bluetooth_manager disable`.
- **2026-10-16 · P4 Android scan done.** `AndroidBleClient` (port of the 777-line `BluetoothClientService`). Emulator:
  scan auto-stops after 10 s, radio off clears the list, revoked permission → Not ready. **Not verified:** connect /
  discover / read / notify / write / MTU (needs a real phone + nRF Connect).
- **2026-10-16 · P5 Android done.** `AndroidMediaPicker` (TakePicture + PickVisualMedia, FileProvider). Emulator:
  capture, cancel, pick, deny → Settings. Known limit: photo lost if Android kills the process while Camera is open.
- **2026-10-16 · P7 Android done.** Device providers, Foreground Service + WorkManager on the common loop. Emulator:
  FGS keeps writing "fgs" records in background, WorkManager ENQUEUED + first "worker" record. Every `AppServices`
  entry on Android is now real. UI files were not touched in P3–P7.

- **2026-10-19 · D1 changed → KMP core + native UI** ([spec](README-KMP-NATIVE-UI-SPEC.md) · [plan](README-KMP-MIGRATION.md)).
  **P8** 5 shared ViewModels + `AppViewModel` (`presentation/`), Compose screens only draw · `PickedImage` carries bytes ·
  78 tests. **P9** SKIE + `Observed<T>`, SwiftUI shell + API tab, Xcode synchronized folder, iOS 17. **P10** SwiftUI
  Permissions / Scanner / Media / Monitor, Maestro through all 5 tabs on the simulator. **P11** Compose UI → `androidApp`
  (Jetpack Compose BOM 2026.06.01), `shared/` without Compose; A32 checked.

- **2026-10-19 · P12 iOS real SDK (code + simulator).** Swift in `iosApp/iosApp/Platform/` calls the Apple SDK through
  small callback interfaces (`IosPermissionNative`, `IosBleNative`, `IosMediaNative`, `IosDeviceNative`); Kotlin adapters
  in `iosMain` keep the state + rules (the old `BluetoothClientService.swift` rules now live in `IosBleClient`).
  `IosPlatform.services` builds `AppServices` once (screen + BGAppRefresh share the monitor). 82/82 tests (+ `IosAdaptersTest`).
  Simulator: camera prompt, real PHPicker, in-app loop JSONL, BGTask error 1 as expected. **Not yet on a real iPhone.**

## 7. Next step

Run P12 on a real iPhone (camera capture, BLE scan → GATT against nRF Connect, BGAppRefresh via the LLDB
`_simulateLaunchForTaskWithIdentifier` trick), then BLE GATT end-to-end on both phones, then P13.
