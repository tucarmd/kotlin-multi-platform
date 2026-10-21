[🏠 README](index.md) · Kiến trúc: [App chạy thế nào](README-KMP-HOW-IT-WORKS.md) · Từng bước: [Từng phase](README-KMP-PHASES.md)

# Bản đồ thư mục: từng file làm gì

Trang này giải thích **từng thư mục và từng file** trong repo, viết dạng cây có ghi chú. Gồm 2 bản đồ:

1. [**Bản đồ hiện tại (nhánh `main`)**](#1-ban-o-hien-tai-nhanh-main): code đang chạy, KMP core + UI native (Jetpack Compose · SwiftUI).
2. [**Bản đồ P0 (nhánh `ndm/notes/p0`)**](#2-ban-o-p0-nhanh-ndmnotesp0): khung đầu tiên, đúng cây bạn thấy khi mở worktree `ndm-phase-view` trong Android Studio. Mỗi dòng có ghi chú ➜ nói nó đã đổi thành gì trên `main`.

Ký hiệu dùng trong cây:

| Ký hiệu | Nghĩa |
|---|---|
| 🧩 | KMP dùng chung: chạy cả Android lẫn iOS |
| 🤖 | chỉ Android |
| 🍎 | chỉ iOS |
| 🎨 | UI (màn hình) |
| 🧠 | ViewModel (state + action của màn hình) |
| ⚙️ | cấu hình build, không phải code app |
| 🟠 | thư mục **sinh ra khi build** (`build/`): xoá được, không commit, Android Studio tô màu cam |
| ⭐ | file nên đọc đầu tiên |

## 3 khái niệm phải nắm trước

1. **Repo có 3 "project" lồng nhau.** Gradle quản lý 2 module Kotlin (`shared`, `androidApp`), Xcode quản lý 1 project iOS (`iosApp`). Khi bấm Run trong Xcode, Xcode tự gọi Gradle để build `Shared.framework` rồi nhúng vào app.
2. **Source set.** Module `shared` có nhiều thư mục `src/xxxMain`. Cùng một module, nhưng code trong mỗi thư mục chạy ở nơi khác nhau:

    | Source set | Chạy ở | Được import |
    |---|---|---|
    | `commonMain` | Android **và** iOS | chỉ thư viện Kotlin thuần (coroutines, Ktor, serialization…) |
    | `androidMain` | chỉ Android | thêm `android.*`, `androidx.*` |
    | `iosMain` | chỉ iOS | thêm `platform.UIKit.*`, `platform.Foundation.*`… (Kotlin/Native) |
    | `commonTest` / `iosTest` | máy build (JVM) / iOS simulator | `kotlin.test` |

3. **Hai cách nối phần chung với phần riêng**:
    - `expect` / `actual`: dùng cho hàm nhỏ. Ví dụ `commonMain/Platform.kt` khai báo `expect fun platformName()`, còn `Platform.android.kt` và `Platform.ios.kt` mỗi bên viết phần thân.
    - `interface` + class riêng mỗi nền tảng: dùng cho thứ có state. Ví dụ `BleClient` có `AndroidBleClient` (Android) và `IosBleClient` (iOS), còn `FakeBleClient` dùng cho test.

## 1. Bản đồ hiện tại (nhánh `main`)

### 1.1 Gốc repo + Gradle

```
apps/NativeDeviceMonitor/          (repo kotlin-multi-platform; trước 2026-10-21 là gốc repo native-device-monitor-kmp)
├── ⭐ README.md                   giới thiệu, cách chạy, trạng thái từng phần
├── ⚙️ settings.gradle.kts          Gradle đọc ĐẦU TIÊN: tên project, nơi tải thư viện (google(), mavenCentral()),
│                                  danh sách module: include(":androidApp", ":shared")
├── ⚙️ build.gradle.kts             build gốc: khai báo plugin dùng chung ("apply false" = khai báo, module nào cần tự bật)
├── ⚙️ gradle.properties            cấu hình Gradle: RAM (-Xmx4096M), cache, android.useAndroidX=true
├── ⚙️ local.properties             đường dẫn Android SDK trên MÁY BẠN (sdk.dir=…), riêng từng máy, không commit
├── ⚙️ gradlew · gradlew.bat         "Gradle wrapper": ./gradlew tự tải đúng Gradle 9.6.1, khỏi cài tay
├── ⚙️ .editorconfig · .gitignore    quy ước format · những thứ không đưa lên git (build/, local.properties…)
│
├── gradle/
│   ├── ⭐ libs.versions.toml       "version catalog": MỌI phiên bản thư viện/plugin ở đây
│   │                              (Kotlin 2.4.10, AGP 9.1, Compose BOM, Ktor 3.5.1, SKIE…).
│   │                              build.gradle.kts gọi qua tên: libs.ktor.client.core
│   ├── gradle-daemon-jvm.properties   Gradle tự chọn JDK để chạy (tự sinh, không sửa tay)
│   └── wrapper/                   gradle-wrapper.jar + .properties: cái ./gradlew dùng để tải Gradle
│
├── 🟠 build/                       sinh ra khi build (Gradle)
│
│   Tài liệu (trang bạn đang đọc) KHÔNG nằm ở đây mà ở docs/apps/native-device-monitor/ của repo:
│   index.md · README-KMP-*.md · plan/ (spec + plan từng giai đoạn) · assets/ (ảnh chụp). Site chung build bằng
│   mkdocs.yml ở gốc repo kotlin-multi-platform, push main → GitHub Actions đăng lên tucarmd.github.io/kotlin-multi-platform
```

### 1.2 `shared/`: module KMP (phần dùng chung + code SDK Android + adapter iOS)

Build ra 2 thứ: Android dùng như một thư viện, iOS nhận `Shared.framework` (Swift viết `import Shared`).

```
shared/
├── ⚙️ build.gradle.kts             target: androidLibrary + iosArm64 (máy thật) + iosSimulatorArm64;
│                                  framework tên "Shared"; plugin SKIE (để Swift đọc được Flow); thư viện từng source set
├── 🟠 build/                       Shared.framework nằm trong build/bin/…, kết quả test trong build/test-results/
└── src/
    ├── commonMain/kotlin/com/learn/ndmkmp/          🧩 CHẠY CẢ 2 MÁY: không import android.* hay UIKit
    │   ├── AppServices.kt          gom 5 interface (quyền, BLE, media, API, monitor) thành 1 object;
    │   │                           mỗi nền tảng tạo 1 cái thật (AndroidServices.kt / IosServices.kt)
    │   ├── Platform.kt             expect: platformName(), monotonicMs(), clockText(), isoLocalTimestamp()
    │   │
    │   ├── core/                   🧩 MODEL + LUẬT + INTERFACE (không biết gì về màn hình)
    │   │   ├── permission/
    │   │   │   ├── PermissionController.kt   interface: groups, states, radio, request(), openSettings()…
    │   │   │   └── PermissionStatus.kt       4 trạng thái: NOT_REQUESTED · GRANTED · DENIED · PERMANENTLY_DENIED
    │   │   ├── bluetooth/
    │   │   │   ├── BleClient.kt              interface BLE: scan → connect → read/notify/write → disconnect;
    │   │   │   │                             bleReadiness() = luật "quét được chưa"
    │   │   │   ├── BluetoothDeviceInfo.kt    1 thiết bị thấy khi quét + gói quảng bá + trạng thái kết nối
    │   │   │   ├── BluetoothScanFilter.kt    lọc tên lúc quét (CarMD2 · CMD-*)
    │   │   │   ├── BluetoothLookup.kt        bảng tra UUID → tên (180F = Battery…), hãng, thiết bị âm thanh
    │   │   │   ├── BluetoothDecoder.kt       byte GATT → chữ đọc được (pin %, nhịp tim, text), hex ↔ byte
    │   │   │   └── GattModel.kt              service / characteristic / giá trị, chọn target, planWrite() (luật nút Send), MTU
    │   │   ├── media/
    │   │   │   ├── MediaPicker.kt            interface chụp / chọn ảnh (suspend, null = huỷ) + PickedImage
    │   │   │   └── MediaInfo.kt              thông tin ảnh (tên, dung lượng, mime, kích thước), formatBytes()
    │   │   ├── network/
    │   │   │   ├── ApiService.kt             interface HTTP + ApiResult (5 nhánh) + ApiFormat (dựng URL, JSON đẹp)
    │   │   │   └── KtorApiService.kt         cài đặt bằng Ktor, DÙNG CHUNG 100% (engine OkHttp / Darwin chọn theo máy)
    │   │   ├── device/DeviceStatus.kt        1 bản ghi: pin, Wi-Fi, 4G, Bluetooth + đọc/ghi JSON
    │   │   ├── logging/FileLogger.kt         ghi/đọc file monitor.jsonl (kotlinx-io)
    │   │   └── background/
    │   │       ├── DeviceMonitor.kt          interface tab Monitor (collect, start/stop, logs, periodic)
    │   │       └── LoopDeviceMonitor.kt      vòng lặp đọc → ghi file, DÙNG CHUNG; chỉ cần DeviceStatusSource + PlatformBackground
    │   │
    │   ├── presentation/          🧠 VIEWMODEL DÙNG CHUNG: "màn hình hiện gì" — Compose và SwiftUI cùng đọc
    │   │   ├── SharedViewModel.kt          lớp nền ~10 dòng: scope trên Main + clear()
    │   │   ├── AppViewModel.kt             tab đang chọn + 5 ViewModel con + AppTab (danh sách tab)
    │   │   ├── AppStrings.kt · InfoRow.kt  tên app/tab · 1 dòng "nhãn: giá trị"
    │   │   ├── permission/  PermissionViewModel.kt (thẻ nào hiện nút gì) · PermissionStrings.kt
    │   │   ├── scanner/     ScannerViewModel.kt (dòng nào hiện, nút nào bật, Write…) · ScannerActions.kt (luật lọc/nút) · ScannerStrings.kt
    │   │   ├── media/       MediaViewModel.kt · MediaStrings.kt
    │   │   ├── api/         ApiViewModel.kt · ApiStrings.kt (6 nút test)
    │   │   └── monitor/     MonitorViewModel.kt · MonitorStrings.kt
    │   │
    │   └── fake/                  🧩 bản GIẢ của 5 interface: dùng cho test + chạy khi chưa có phần cứng
    │       └── FakePermissionController · FakeBleClient · FakeMediaPicker · FakeApiService · FakeDeviceMonitor
    │
    ├── androidMain/               🤖 CHỈ ANDROID: gọi Android SDK
    │   ├── AndroidManifest.xml     quyền BLE/Camera/FGS + FileProvider + MonitorService (gộp vào manifest của app)
    │   ├── res/xml/file_paths.xml  thư mục FileProvider cho ảnh chụp camera
    │   └── kotlin/com/learn/ndmkmp/
    │       ├── Platform.android.kt         actual: Build.VERSION, SystemClock, SimpleDateFormat
    │       ├── platform/AndroidServices.kt ⭐ chỗ DUY NHẤT tạo AppServices thật cho Android
    │       └── core/
    │           ├── permission/  AndroidPermissionController.kt (launcher xin quyền) · AndroidPermissions.kt (bảng quyền theo API)
    │           ├── bluetooth/   AndroidBleClient.kt (BluetoothGatt + hàng đợi) · AndroidBluetoothRadio.kt (bật/tắt radio)
    │           ├── media/       AndroidMediaPicker.kt (TakePicture + PickVisualMedia)
    │           ├── device/      AndroidDeviceStatusSource.kt (BatteryManager, ConnectivityManager)
    │           ├── network/     NetworkErrorKind.android.kt (nhận ra lỗi "không có mạng")
    │           └── background/  MonitorService.kt (Foreground Service) · MonitorWorker.kt (WorkManager 15′) ·
    │                            AndroidPlatformBackground.kt · AndroidMonitorRuntime.kt (1 monitor/process) ·
    │                            NotificationGatedMonitor.kt (xin quyền thông báo trước khi bật FGS)
    │
    ├── iosMain/kotlin/com/learn/ndmkmp/   🍎 CHỈ iOS (Kotlin/Native): adapter giữ state + luật, gọi xuống Swift
    │   ├── Platform.ios.kt                 actual: UIDevice, NSProcessInfo, NSDateFormatter
    │   ├── core/network/NetworkErrorKind.ios.kt
    │   └── platform/
    │       ├── IosServices.kt              ⭐ createIosServices(...): Swift truyền 4 object vào, nhận AppServices
    │       ├── IosPermissionController.kt  IosPermissionNative (Swift làm) → 4 trạng thái
    │       ├── IosBleClient.kt             IosBleNative + IosBleEvents (Swift làm) → luật quét/kết nối/GATT
    │       ├── IosMediaPicker.kt           IosMediaNative (Swift làm) → suspend
    │       ├── IosMonitor.kt               IosDeviceNative (Swift làm) → LoopDeviceMonitor dùng chung
    │       └── IosBridge.kt                hàm nhỏ cho Swift (ByteArray → NSData)
    │
    ├── commonTest/            ✅ test chạy cả JVM lẫn iOS simulator: core/ (luật) + presentation/ (ViewModel)
    └── iosTest/               ✅ test chỉ chạy iOS: IosAdaptersTest (adapter iOS với native giả)
```

### 1.3 `androidApp/`: app Android (Kotlin + Jetpack Compose)

```
androidApp/
├── ⚙️ build.gradle.kts         plugin com.android.application · applicationId com.learn.ndmkmp · minSdk 26 ·
│                              Jetpack Compose (BOM) · phụ thuộc projects.shared
└── src/main/
    ├── AndroidManifest.xml     "CMND" app Android: tên app ghi thẳng (android:label), icon, Activity mở đầu tiên (LAUNCHER)
    ├── res/                    CHỈ icon launcher (drawable/, mipmap-anydpi-v26/), không có strings.xml (Rule 5.3)
    └── kotlin/com/learn/ndmkmp/
        ├── ⭐ MainActivity.kt   cửa vào: createAndroidServices(this) → AppViewModel → setContent { App(vm) };
        │                        onDestroy → vm.clear()
        ├── App.kt              🎨 khung: thanh trên + nội dung + thanh 5 tab (icon Material)
        └── ui/                 🎨 màn hình Compose, chỉ VẼ state của ViewModel
            ├── permission/PermissionScreen.kt
            ├── scanner/ScannerScreen.kt · GattSection.kt
            ├── media/MediaScreen.kt
            ├── api/ApiScreen.kt
            ├── monitor/MonitorScreen.kt
            ├── common/         AppCard · AppHintText · AppPrimaryButton/AppSecondaryButton
            └── theme/          AppColor (bảng màu CarMD) · AppSpacing · AppTheme
```

### 1.4 `iosApp/`: app iOS (Swift + SwiftUI)

```
iosApp/
├── iosApp.xcodeproj/           project Xcode. project.pbxproj có bước "Compile Kotlin Framework" chạy
│                              ./gradlew :shared:embedAndSignAppleFrameworkForXcode. Thư mục iosApp/ là
│                              "synchronized folder": thả file .swift vào là được build, không sửa pbxproj
├── Configuration/Config.xcconfig   BUNDLE_ID=com.learn.ndmkmp · APP_NAME · TEAM_ID
├── 🟠 build/                    sinh ra khi build bằng xcodebuild
└── iosApp/
    ├── ⭐ iOSApp.swift          cửa vào (@main): đăng ký BGTask · @StateObject AppModel · RootView ·
    │                            app quay lại foreground → vm.onResume()
    ├── Info.plist              "CMND" app iOS: chuỗi xin quyền Bluetooth/Camera, BGTask id, chế độ nền
    ├── Assets.xcassets · Preview Content/   icon, màu · ảnh chỉ dùng cho SwiftUI Preview
    ├── App/                    cầu nối Swift ↔ Kotlin
    │   ├── AppModel.swift      giữ AppViewModel (Kotlin) cho cả app; deinit → vm.clear()
    │   └── Observed.swift      StateFlow (Kotlin) → @Published (SwiftUI): bản Swift của collectAsState()
    ├── UI/                     🎨 màn hình SwiftUI, chỉ VẼ state của ViewModel (cùng ViewModel với Android)
    │   ├── RootView.swift      TabView 5 tab (icon SF Symbols)
    │   ├── Permission/PermissionView.swift
    │   ├── Scanner/ScannerView.swift · GattSection.swift
    │   ├── Media/MediaView.swift
    │   ├── Api/ApiView.swift
    │   ├── Monitor/MonitorView.swift
    │   ├── Common/             AppCard · AppHintText · AppPrimaryButton · AppChip/InfoTable/TerminalBox
    │   └── Theme/              AppColor · AppSpacing (giống bảng màu Android)
    └── Platform/               🍎 code gọi SDK Apple bằng Swift (implement interface callback của iosMain)
        ├── ⭐ IosPlatform.swift tạo AppServices 1 lần: PermissionNative · BleNative · MediaNative · DeviceNative
        ├── PermissionNative.swift   CBManager.authorization, AVCaptureDevice, theo dõi radio, alert bật Bluetooth
        ├── BleNative.swift          CoreBluetooth mỏng: scan, connect, discover, read/notify/write
        ├── MediaNative.swift        UIImagePickerController (camera) · PHPicker (thư viện ảnh)
        └── DeviceNative.swift       pin (UIDevice), mạng (NWPathMonitor), BGAppRefresh (BGTaskScheduler)
```

### 1.5 Một lần bấm nút đi qua những file nào

Ví dụ bấm **Scan Bluetooth**:

```
🎨 ScannerScreen.kt (Android) / ScannerView.swift (iOS)        bấm nút → vm.onScanClick()
        │
🧠 presentation/scanner/ScannerViewModel.kt                     đang quét? → stopScan() : startScan(filter)
        │
🧩 core/bluetooth/BleClient.kt                                   interface: ViewModel không biết ai làm
        │
   ┌────┴──────────────────────────────┐
🤖 androidMain/AndroidBleClient.kt      🍎 iosMain/IosBleClient.kt → iosApp/Platform/BleNative.swift
   BluetoothLeScanner                      CBCentralManager.scanForPeripherals
        │                                      │
        └──── cập nhật StateFlow devices ──────┘
        │
🧠 ScannerViewModel.rebuild() → ScannerUiState (lọc, nhãn, nút)
        │
🎨 collectAsState() (Compose) / Observed (SwiftUI) → vẽ lại danh sách
```

### 1.6 Thêm code mới thì đặt ở đâu?

| Bạn muốn… | Đặt ở | Ví dụ |
|---|---|---|
| thêm luật / model thuần (không UI, không SDK) | `shared/commonMain/.../core/` + test ở `commonTest/` | luật mới cho `planWrite()` |
| đổi "màn hình hiện gì" (chữ, nút nào bật) | `shared/commonMain/.../presentation/xxx/XxxViewModel.kt` | thêm dòng mới vào `ScannerUiState` |
| đổi cách VẼ trên Android | `androidApp/.../ui/` | đổi màu, bố cục `ScannerScreen.kt` |
| đổi cách VẼ trên iOS | `iosApp/iosApp/UI/` | đổi bố cục `ScannerView.swift` |
| gọi một API Android mới | `shared/androidMain/` | thêm vào `AndroidBleClient.kt` |
| gọi một API Apple mới | Swift: `iosApp/iosApp/Platform/` + thêm hàm vào interface `IosXxxNative` trong `shared/iosMain/` | thêm vào `BleNative.swift` |
| thêm thư viện | `gradle/libs.versions.toml` rồi `build.gradle.kts` của module cần | — |
| xin thêm quyền | `shared/androidMain/AndroidManifest.xml` · `iosApp/iosApp/Info.plist` | — |
| thêm chữ hiển thị (nhãn, nút, thông báo) | `shared/commonMain/.../presentation/xxx/XxxStrings.kt`. **Không** dùng `strings.xml` (xem Rule 5.3 bên dưới) | thêm `const val` vào `ScannerStrings` |

### 1.7 Rule 5.3: chuỗi hiển thị chỉ nằm trong file `.kt` / `.swift`

Quy định mang sang từ app cũ: **mọi chữ trên màn hình nằm trong `object XxxStrings` (Kotlin)**, không dùng `strings.xml` hay `composeResources`.
Từ P8, các object này nằm trong `shared/commonMain/.../presentation/`, nên Compose và SwiftUI dùng chung **một** bộ chữ (Swift đọc `ScannerStrings.shared.SCAN_BUTTON`).
Màn hình SwiftUI không tự đặt chữ riêng, trừ ký tự trang trí như "•".

| Chỗ | Có chữ? | Vì sao được phép |
|---|---|---|
| `presentation/*Strings.kt` | ✅ toàn bộ chữ UI | đây là chỗ đúng |
| `AndroidManifest.xml` `android:label` | tên app | Android đọc nhãn launcher từ manifest, ghi thẳng chữ, không qua `@string/` |
| `iosApp/iosApp/Info.plist` (`NSBluetoothAlwaysUsageDescription`, `NSCameraUsageDescription`) | câu xin quyền | Apple **bắt buộc** đặt trong Info.plist, iOS tự hiện trong hộp thoại hệ thống |
| `iosApp/Configuration/Config.xcconfig` `APP_NAME` | tên app iOS | cấu hình build của Xcode, không phải chữ UI |
| ~~`res/values/strings.xml`~~ · ~~`composeResources/values/strings.xml`~~ | ❌ | đã xoá (P0 còn `composeResources`, xoá ở P11; `app_name` xoá 2026-10-19) |

## 2. Bản đồ P0 (nhánh `ndm/notes/p0`)

Đây là cây bạn thấy trong Android Studio khi mở worktree `ndm-phase-view` (nhánh `ndm/notes/p0`): khung KMP đầu tiên, chỉ có 1 màn "hello" vẽ bằng **Compose Multiplatform cho cả 2 máy**. Dòng ➜ cho biết thứ đó trên `main` đã thành gì.

```
NativeDeviceMonitorKMP/
├── settings.gradle.kts · build.gradle.kts · gradle.properties · local.properties · gradlew   (như mục 1.1)
├── gradle/libs.versions.toml      có Compose Multiplatform 1.11.1        ➜ main: bỏ Compose MP, thêm Jetpack Compose BOM + SKIE
├── docs/plan/README-KMP-MIGRATION.md    kế hoạch P0–P8
├── 🟠 build/
│
├── androidApp/                    🤖
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml     chỉ có quyền INTERNET, label=@string/app_name   ➜ main: quyền BLE/Camera/FGS ở shared/androidMain, label ghi thẳng
│       ├── res/                    icon + values/strings.xml (app_name)     ➜ main: đã xoá theo Rule 5.3, tên ghi thẳng trong manifest
│       └── kotlin/…/MainActivity.kt   setContent { App() }        ➜ main: tạo AppServices + AppViewModel, App(vm) bằng Jetpack Compose
│
├── shared/                        🧩
│   ├── build.gradle.kts            có plugin Compose Multiplatform       ➜ main: không còn Compose, thêm SKIE
│   ├── 🟠 build/
│   └── src/
│       ├── commonMain/
│       │   ├── composeResources/values/strings.xml
│       │   │                       chuỗi của Compose MP (app_title, p0_hint), code gọi Res.string.app_title, SAI Rule 5.3
│       │   │                       ➜ main: đã xoá; chuỗi nằm trong presentation/*Strings.kt
│       │   └── kotlin/…/
│       │       ├── App.kt          🎨 màn hello bằng Compose Multiplatform, CHUNG cho 2 máy
│       │       │                   ➜ main: dời sang androidApp/App.kt (chỉ Android); iOS có RootView.swift
│       │       └── Platform.kt     expect fun platformName()     ➜ main: thêm monotonicMs(), clockText()…
│       ├── androidMain/kotlin/…/Platform.android.kt   actual: "Android ${Build.VERSION.RELEASE}"
│       │                           ➜ main: thêm toàn bộ code SDK Android (core/…, platform/AndroidServices.kt)
│       └── iosMain/kotlin/…/
│           ├── Platform.ios.kt     actual: UIDevice (Kotlin gọi thẳng UIKit, không cần Swift)
│           └── MainViewController.kt   ComposeUIViewController { App() }: bọc UI Compose cho Swift
│                                   ➜ main: đã xoá (SwiftUI tự vẽ); thay bằng platform/Ios*.kt (adapter)
│
└── iosApp/                        🍎
    ├── iosApp.xcodeproj/           project Xcode (nhóm file kiểu cũ: thêm file phải sửa pbxproj)
    │                               ➜ main: synchronized folder, deployment target iOS 17
    ├── Configuration/Config.xcconfig   BUNDLE_ID · APP_NAME · TEAM_ID
    ├── Frameworks                  nhóm ảo của Xcode (trống: Shared.framework do Gradle nhúng lúc build)
    ├── Products                    nhóm ảo: file .app sau khi build
    └── iosApp/
        ├── iOSApp.swift            @main → WindowGroup { ContentView() }
        │                           ➜ main: AppModel + RootView + đăng ký BGTask
        ├── ContentView.swift       ComposeView: UIViewControllerRepresentable bọc MainViewController(),
        │                           Swift chỉ là cái VỎ (~20 dòng)
        │                           ➜ main: đã xoá; thay bằng App/, UI/ (5 View SwiftUI), Platform/ (SDK Apple)
        ├── Info.plist              cơ bản                       ➜ main: thêm chuỗi xin quyền + BGTask
        ├── Assets.xcassets
        └── Preview Content/
```

Luồng gọi ở P0: hai máy cùng vẽ **một** `App()`:

```
🤖 AndroidManifest → MainActivity.onCreate → setContent { App() } ──┐
                                                                   ├──► commonMain/App.kt ──► platformName()
🍎 iOSApp.swift → ContentView → MainViewController() → App() ──────┘                              │
                                                       expect  Platform.kt ◄───────────────────────┘
                                                       actual  Platform.android.kt (Build.VERSION)
                                                       actual  Platform.ios.kt     (UIDevice)
```

So với `main`: từ P8 tới P11, UI tách thành 2 (Compose trong `androidApp`, SwiftUI trong `iosApp`), còn logic "hiện gì" dời vào ViewModel dùng chung trong `shared/presentation`. Từ P12, iOS gọi SDK thật qua `iosApp/Platform` (Swift) + adapter `shared/iosMain`. Chi tiết từng bước xem [Từng phase](README-KMP-PHASES.md).
