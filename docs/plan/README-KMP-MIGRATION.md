[🏠 README](../../README.md) · Old app: [tucarmd/native-mobile-learning](https://github.com/tucarmd/native-mobile-learning) (`native-device-monitor/`)

# Plan — Native Device Monitor → one Kotlin app (KMP + Compose Multiplatform)

> Created: 2026-10-16 · Status: **Approved 2026-10-16 (D1–D5 as recommended) · P0 ✅ P1 ✅ P2 ✅ P6 ✅ · P3 P4 P5 P7 ✅ Android only, iOS not started**
> Step-by-step per phase (what the dev did, which layer): [Từng phase: dev đã làm gì](../README-KMP-PHASES.md)
> Learning path: [Học KMP qua Device Pulse](https://tucarmd.github.io/kotlin-multi-platform/lessons/02-setup/) (14 lessons) +
> official docs [kotlinlang.org/docs/multiplatform](https://kotlinlang.org/docs/multiplatform/get-started.html).
> Done rule (same as the curriculum): **build OK + run for real + screenshot read**. Code written ≠ done.

## 1. Goal · scope · out of scope

**Goal:** rebuild Native Device Monitor as a **new app written only in Kotlin**: one codebase → Android + iOS.
Today the same app is written twice (Kotlin/Compose ~6.5k lines + Swift/SwiftUI ~5.6k lines). After this: one Kotlin
codebase, Swift shrinks to the iOS shell (~20 lines) the KMP wizard generates.

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

**Short answer:** there is **no hand-written native UI** on either platform. All 5 tabs are Compose Multiplatform in
`shared/commonMain`, and the same Kotlin code draws the UI on Android and on iOS. "Native" here only means **platform APIs**
(permissions, BLE, camera, battery, background). Those are also written **in Kotlin**: `androidMain` calls the Android SDK,
and `iosMain` calls Apple frameworks through Kotlin/Native (`import platform.CoreBluetooth.*`). **Swift is only the iOS
shell.**

| Layer | Folder | Language | What lives there | Runs on | Lines today |
|---|---|---|---|---|---|
| **UI**: 5 tabs, theme, buttons, strings | `shared/src/commonMain/…/ui/` | Kotlin · Compose MP | `PermissionScreen`, `ScannerScreen` + `GattSection`, `MediaScreen`, `ApiScreen`, `MonitorScreen`, `AppCard`… | 🤖 + 🍎 (shared) | ~2.6k |
| **Logic + interfaces** | `shared/src/commonMain/…/core/` | Kotlin (pure) | models + rules (`GattModel`, `BluetoothDecoder`, `DeviceStatus`…), interfaces (`PermissionController`, `BleClient`, `MediaPicker`, `ApiService`, `DeviceMonitor`), fully shared code (`KtorApiService`, `LoopDeviceMonitor`, `FileLogger`) | 🤖 + 🍎 (shared) | ~2.0k |
| **Android platform** | `shared/src/androidMain/` | Kotlin · Android SDK | `AndroidPermissionController`, `AndroidBleClient` (BluetoothGatt), `AndroidMediaPicker`, `AndroidDeviceStatusSource`, `MonitorService` (FGS) + `MonitorWorker` (WorkManager) | 🤖 only | ~1.9k |
| **iOS platform** | `shared/src/iosMain/` | Kotlin/Native · Apple frameworks | `MainViewController`, `IosServices`. **To do (P3–P5, P7 🍎):** CBManager/AVCaptureDevice, CoreBluetooth, PHPicker, UIDevice, BGTaskScheduler. For now it plugs in **fakes** | 🍎 only | ~80 |
| Android entry | `androidApp/…/MainActivity.kt` | Kotlin | `setContent { App(services) }` | 🤖 | 20 |
| **iOS shell** | `iosApp/iosApp/` | **Swift** | `iOSApp.swift` + `ContentView.swift` (wraps `MainViewController` in SwiftUI) + `Info.plist`. P7 🍎 adds one BGTaskScheduler register line | 🍎 | 29 |

Per tab, which file does what:

| Tab | UI (shared) | Logic (shared) | 🤖 androidMain | 🍎 iosMain | Swift |
|---|---|---|---|---|---|
| Permissions | `PermissionScreen` | `PermissionController`, `PermissionStatus` | `AndroidPermissionController`, `AndroidPermissions`, `AndroidBluetoothRadio` ✅ | CBManager + AVCaptureDevice: *P3, not started* | — |
| Scanner | `ScannerScreen`, `GattSection` | `BleClient`, `GattModel`, `BluetoothDecoder/Lookup/ScanFilter`, `ScannerActions` | `AndroidBleClient` ✅ | CoreBluetooth: *P4, not started* | — |
| Media | `MediaScreen` | `MediaPicker`, `MediaInfo` | `AndroidMediaPicker` ✅ | UIImagePickerController / PHPicker: *P5, not started* | — |
| API | `ApiScreen` | `ApiService`, **`KtorApiService` (100% shared)** | only `NetworkErrorKind.android.kt` (OkHttp engine) | only `NetworkErrorKind.ios.kt` (Darwin engine) ✅ | — |
| Monitor | `MonitorScreen` | `DeviceMonitor`, `LoopDeviceMonitor`, `DeviceStatus`, `FileLogger` | `AndroidDeviceStatusSource`, `MonitorService`, `MonitorWorker` ✅ | UIDevice + BGTaskScheduler: *P7, not started* | 1 register line (P7) |

Rule of thumb: if it can be written without touching a platform SDK, it goes in `commonMain`. Only the part that must call
the Android SDK or an Apple framework goes in `androidMain` / `iosMain`, behind an interface from `commonMain`. Each
platform's real implementations are chosen in one file: `platform/AndroidServices.kt` / `platform/IosServices.kt`.

## 2. Architecture (follows the lesson site: lesson 2 → "Share UI")

```
native-device-monitor-kmp/            ← own private repo tucarmd/native-device-monitor-kmp, KMP-App-Template layout (AGP 9)
├── androidApp/       Android application module: MainActivity → App()
├── shared/src/
│   ├── commonMain/   model · rules · ViewModels · Compose UI (all 5 tabs) · strings (Compose resources)
│   ├── androidMain/  MainActivity + actual: permissions, BLE*, camera/picker, battery/network, FGS + WorkManager
│   ├── iosMain/      MainViewController + actual: CoreBluetooth*, AVFoundation/PHPicker, UIDevice, BGTaskScheduler
│   └── commonTest/   kotlin.test — ported JUnit / Swift Testing tests
└── iosApp/           Xcode shell: iOSApp.swift + ContentView.swift + Info.plist (usage strings, BG modes)
```

Platform code is behind **interfaces in commonMain + one implementation per platform** (the skill's advice: plain interface
instead of `expect class`). Dependencies one way: `ui → viewmodel → core interfaces`.

Library choices (versions from the wizard / lesson 5, not hand-picked):

| Need | Old | New |
|---|---|---|
| UI | Compose (🤖) + SwiftUI (🍎) | **Compose Multiplatform** + Material 3 |
| ViewModel / nav | hand-rolled | `androidx.lifecycle` ViewModel (multiplatform) + Navigation Compose (lesson 8) |
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
| P3 🤖✅ 🍎— | Permissions | 9 | `PermissionService` interface; 🤖 ActivityResult (API ≤30 vs 31+ branch kept) · 🍎 CBManager + AVCaptureDevice authorization; open Settings | Deny → retry → Settings path works on both (real devices) |
| P4 🤖✅ 🍎— | BLE | — (app #5 "OBD Link") | `BleClient` interface: scan → connect → discover → read → notify → write → MTU → disconnect; platform rules from the README table (Android GATT queue, iOS 15 s connect timeout, radio-off cleanup) | Full Scanner E2E against nRF Connect advertiser on **Samsung A32 + real iPhone** |
| P5 🤖✅ 🍎— | Media | — | 🤖 TakePicture + PickVisualMedia · 🍎 UIImagePickerController / PHPicker presented from Kotlin; image → `ImageBitmap` + MediaInfo | Capture + pick + cancel on real devices (simulator has no camera) |
| P6 ✅ | API | 11 | Ktor client: GET/POST/500/timeout; MockEngine tests | Tab works both platforms, tests green |
| P7 🤖✅ 🍎— | Monitor + background | 9, 12 | Providers (battery/network/BT) · FileLogger JSONL · 🤖 FGS + WorkManager in androidMain · 🍎 BGTaskScheduler registered at launch (one call from `iOSApp.swift`) | Logs written while in background on both; screenshot of Monitor logs |
| P8 | Final | 13–14 | Run task 006 E2E checklist on the new app, lint clean, README + "old vs new" table (lines of code, files, what still needs Swift) | All rows ✅ with screenshots, README updated |

Order matters: P0–P2 can be checked on simulator/emulator only; P3–P5, P7 need **real devices** (BLE, camera, background).

## 4. Decisions for you (review these)

| # | Question | Recommended | Alternative |
|---|---|---|---|
| D1 | UI | **Compose Multiplatform** — only Kotlin, matches lesson 2 "Share UI" and your request | KMP core + keep SwiftUI (= Device Pulse demo app) — still 2 UI languages |
| D2 | BLE | **Write our own `BleClient`** in androidMain (reuse current Kotlin) + iosMain (CoreBluetooth in Kotlin/Native) — keeps the platform-difference learning | **Kable** (JuulLabs) — far less code, one API, used in app #5 of the lesson site; hides the GATT queue / CCCD details |
| D3 | Where | ~~New folder in native-mobile-learning~~ → **changed 2026-10-16: own private repo `tucarmd/native-device-monitor-kmp`** | — |
| D4 | IDs | `com.learn.ndmkmp` (both platforms) — can be installed next to the old app | reuse `com.learn.nativedevicemonitor` (replaces old app on device) |
| D5 | Pomodoro | backlog, after P8 | port in P2 |

## 5. Risks

- **CoreBluetooth from Kotlin/Native** (if D2 = own code): delegates must subclass `NSObject`; ObjC interop is verbose. Fallback: Kable.
- **iOS look:** Compose on iOS draws Material, not native iOS controls. Fine for a learning/monitor app; tell testers.
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

## 7. Next step

iOS side of P3 / P4 / P5 / P7 in `iosMain` (CBManager + AVCaptureDevice, CoreBluetooth, PHPicker, UIDevice +
BGTaskScheduler), then BLE GATT end-to-end on real phones, then P8.
