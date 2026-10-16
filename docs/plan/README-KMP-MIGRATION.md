[🏠 README](../../README.md) · Old app: [tucarmd/native-mobile-learning](https://github.com/tucarmd/native-mobile-learning) (`native-device-monitor/`)

# Plan — Native Device Monitor → one Kotlin app (KMP + Compose Multiplatform)

> Created: 2026-10-16 · Status: **Approved 2026-10-16 (D1–D5 as recommended) · P0 ✅ done 2026-10-16**
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
| P2 | UI shell | 6–8 | Theme tokens + AppCard/AppPrimaryButton/AppHintText, 5-tab bottom bar, strings → resources, every screen on **fake** data | Both platforms show all 5 tabs, screenshots |
| P3 | Permissions | 9 | `PermissionService` interface; 🤖 ActivityResult (API ≤30 vs 31+ branch kept) · 🍎 CBManager + AVCaptureDevice authorization; open Settings | Deny → retry → Settings path works on both (real devices) |
| P4 | BLE | — (app #5 "OBD Link") | `BleClient` interface: scan → connect → discover → read → notify → write → MTU → disconnect; platform rules from the README table (Android GATT queue, iOS 15 s connect timeout, radio-off cleanup) | Full Scanner E2E against nRF Connect advertiser on **Samsung A32 + real iPhone** |
| P5 | Media | — | 🤖 TakePicture + PickVisualMedia · 🍎 UIImagePickerController / PHPicker presented from Kotlin; image → `ImageBitmap` + MediaInfo | Capture + pick + cancel on real devices (simulator has no camera) |
| P6 | API | 11 | Ktor client: GET/POST/500/timeout; MockEngine tests | Tab works both platforms, tests green |
| P7 | Monitor + background | 9, 12 | Providers (battery/network/BT) · FileLogger JSONL · 🤖 FGS + WorkManager in androidMain · 🍎 BGTaskScheduler registered at launch (one call from `iOSApp.swift`) | Logs written while in background on both; screenshot of Monitor logs |
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

## 7. Next step

P2 — UI shell on fake data (5 tabs, theme, resources); stop after P2 for a screenshot review before touching BLE.
