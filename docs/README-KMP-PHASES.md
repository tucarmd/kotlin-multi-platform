[🏠 README](../README.md) · Kế hoạch: [README-KMP-MIGRATION.md](plan/README-KMP-MIGRATION.md) · Kiến trúc: [App chạy thế nào](README-KMP-HOW-IT-WORKS.md)

# Từng phase: dev đã làm gì

Trang này để **trình bày và review lại**. Mỗi phase đọc từ trên xuống: bước 1 → 2 → 3 là đúng thứ tự dev làm, mỗi bước gắn **một nhãn tầng** để biết đang đụng UI, code KMP dùng chung hay code riêng Android / iOS.

> Cập nhật 2026-10-16. **Android: P0–P7 chạy thật** (trừ BLE connect/GATT cần máy thật). **iOS: mới tới P2 + P6**: các tab còn lại trên iOS vẫn chạy bằng `Fake*`.

## Đọc nhãn tầng

| Nhãn | Tầng | Thư mục | Chạy ở đâu |
|---|---|---|---|
| 🎨 **UI** | Màn hình Compose | `shared/src/commonMain/.../ui/` | **chung** Android + iOS |
| 🧩 **KMP chung** | interface, luật, model, Ktor, logger | `shared/src/commonMain/.../core/` · `fake/` | **chung** Android + iOS |
| 🤖 **Android** | cài đặt interface bằng Android SDK | `shared/src/androidMain/` | chỉ Android |
| 🍎 **iOS** | cài đặt interface bằng Apple SDK (vẫn viết Kotlin) | `shared/src/iosMain/` | chỉ iOS |
| 📱 **Vỏ app** | `MainActivity` · `iOSApp.swift` · Manifest · Info.plist | `androidApp/` · `iosApp/` | riêng |
| ✅ **Kiểm tra** | build, test, chạy máy, chụp màn hình | — | — |

```mermaid
flowchart LR
    V["📱 Vỏ app"] --> U["🎨 UI<br/>commonMain/ui"]
    U --> K["🧩 KMP chung<br/>interface + luật"]
    A["🤖 androidMain"] -- "cài đặt" --> K
    I["🍎 iosMain"] -- "cài đặt" --> K
    A --> AS["Android SDK"]
    I --> IS["Apple SDK"]
    classDef shell fill:#e5e1d8,stroke:#1F2A44,color:#1F2A44
    classDef ui fill:#7EC8E3,stroke:#1F2A44,color:#1F2A44
    classDef kmp fill:#8FB59A,stroke:#1F2A44,color:#1F2A44
    classDef and fill:#F5C451,stroke:#1F2A44,color:#1F2A44
    classDef ios fill:#F08A5D,stroke:#1F2A44,color:#1F2A44
    classDef sdk fill:#C9C1B3,stroke:#1F2A44,color:#1F2A44,stroke-width:3px
    class V shell
    class U ui
    class K kmp
    class A and
    class I ios
    class AS,IS sdk
```

## Bảng tổng: phase nào đụng tầng nào

| Phase | 🎨 UI | 🧩 KMP chung | 🤖 Android | 🍎 iOS | 📱 Vỏ | Trạng thái |
|---|:-:|:-:|:-:|:-:|:-:|---|
| [P0 Setup](#p0-setup-khung-kmp) | ● | ● | ● | ● | ● | ✅ 2 máy |
| [P1 Core](#p1-đưa-luật-thuần-vào-commonmain) | | ● | | | | ✅ 52 test × 2 máy |
| [P2 UI shell](#p2-5-tab-ui-chạy-bằng-fake) | ● | ● | ● | ● | ● | ✅ 2 máy (data giả) |
| [P6 API](#p6-api-thật-bằng-ktor) | | ● | ● | ● | ● | ✅ 2 máy |
| [P3 Permissions](#p3-xin-quyền-thật-android) | | | ● | ○ | | 🤖 ✅ · 🍎 chưa |
| [P4 BLE](#p4-bluetooth-thật-android) | | | ● | ○ | | 🤖 scan ✅, GATT cần máy thật · 🍎 chưa |
| [P5 Media](#p5-camera--thư-viện-ảnh-android) | | | ● | ○ | | 🤖 ✅ · 🍎 chưa |
| [P7 Monitor](#p7-monitor--chạy-nền-android) | | ● | ● | ○ | | 🤖 ✅ · 🍎 chưa |
| P8 Final | | | | | | chưa |

● = phase này có sửa · ○ = phải làm nhưng chưa làm.

Điều cần thấy khi nhìn bảng: **từ P3 trở đi UI không đổi một dòng**. Màn hình đã viết xong ở P2 trên interface; mỗi phase sau chỉ thay `Fake*` bằng cài đặt thật ở 🤖 / 🍎, rồi đổi **một dòng** trong `createAndroidServices` / `createIosServices`.

## P0 · Setup khung KMP

**Mục tiêu:** một màn hình "Hello" chạy được trên cả Android và iOS từ một codebase.

1. 📱 **Vỏ app**: tạo layout chuẩn KMP-App-Template (`shared/` + `androidApp/` + `iosApp/`), package `com.learn.ndmkmp`, AGP 9.1, Kotlin 2.4.10, Compose Multiplatform 1.11.1.
2. 🎨 **UI**: một hàm `App()` trong [App.kt](../shared/src/commonMain/kotlin/com/learn/ndmkmp/App.kt).
3. 🧩 **KMP chung**: `expect fun platformName()` trong [Platform.kt](../shared/src/commonMain/kotlin/com/learn/ndmkmp/Platform.kt).
4. 🤖 / 🍎: hai `actual`: [Platform.android.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/Platform.android.kt) · [Platform.ios.kt](../shared/src/iosMain/kotlin/com/learn/ndmkmp/Platform.ios.kt).
5. 📱 **Vỏ app**: [MainActivity.kt](../androidApp/src/main/kotlin/com/learn/ndmkmp/MainActivity.kt) gọi `App()`; [ContentView.swift](../iosApp/iosApp/ContentView.swift) bọc `MainViewController()`.
6. ✅ **Kiểm tra**: Pixel 8 emulator + iPhone 17 Simulator.

<div class="shots shots--pair" markdown>

<figure markdown>
[![Android](assets/kmp_p0_android.png)](assets/kmp_p0_android.png)
<figcaption>Android</figcaption>
</figure>

<figure markdown>
[![iOS](assets/kmp_p0_ios.png)](assets/kmp_p0_ios.png)
<figcaption>iOS</figcaption>
</figure>

</div>

## P1 · Đưa luật thuần vào commonMain

**Mục tiêu:** phần không đụng phần cứng chỉ viết **một lần**, test chạy trên cả 2 máy. **Chỉ đụng 🧩.**

1. 🧩 **KMP chung**: chép 9 file thuần từ app Android cũ sang `commonMain/core/`: `GattModel`, `BluetoothDecoder`, `BluetoothLookup`, `BluetoothScanFilter`, `BluetoothDeviceInfo`, `DeviceStatus`, `MediaInfo`, `PermissionStatus`, `ScannerActions`.
2. 🧩 **KMP chung**: thay API chỉ có trên JVM: `java.util.UUID` → `String`, `"%02X".format` → `hex()`, `Charsets` → `decodeToString()`, `org.json` → kotlinx.serialization.
3. 🧩 **KMP chung**: chuyển 50 test JUnit sang `kotlin.test` trong `commonTest`, thêm 2 test.
4. ✅ **Kiểm tra**: `./gradlew :shared:allTests`: **52/52** trên Android host **và** iOS Simulator.

## P2 · 5 tab UI chạy bằng fake

**Mục tiêu:** cả 5 màn hình hiện trên 2 máy **trước khi** có code phần cứng nào. Đây là phase duy nhất viết UI.

1. 🎨 **UI**: theme (`AppColor`, `AppSpacing`, `AppTheme`) + component chung (`AppCard`, `AppPrimaryButton`, `AppHintText`) trong `ui/theme/`, `ui/common/`.
2. 🎨 **UI**: 5 màn `PermissionScreen` · `ScannerScreen` (+ `GattSection`) · `MediaScreen` · `ApiScreen` · `MonitorScreen`; `App()` = Scaffold + thanh 5 tab.
3. 🧩 **KMP chung**: 5 interface UI gọi vào: [`PermissionController`](../shared/src/commonMain/kotlin/com/learn/ndmkmp/core/permission/PermissionController.kt) · [`BleClient`](../shared/src/commonMain/kotlin/com/learn/ndmkmp/core/bluetooth/BleClient.kt) · [`MediaPicker`](../shared/src/commonMain/kotlin/com/learn/ndmkmp/core/media/MediaPicker.kt) · [`ApiService`](../shared/src/commonMain/kotlin/com/learn/ndmkmp/core/network/ApiService.kt) · [`DeviceMonitor`](../shared/src/commonMain/kotlin/com/learn/ndmkmp/core/background/DeviceMonitor.kt), gom trong `AppServices`.
4. 🧩 **KMP chung**: 5 bản giả `fake/Fake*.kt` để UI chạy được ngay.
5. 🧩 → 🤖 / 🍎: `expect` cho đồng hồ (`monotonicMs`, `clockText`, `isoLocalTimestamp`), `actual` mỗi bên.
6. ✅ **Kiểm tra**: 5 tab trên Pixel + iPhone Simulator; flow giả request → bật radio → scan → connect → discover chạy. Sửa 1 lỗi iOS: nhãn tab "Permissions" xuống 2 dòng.

Chọn tab bên dưới để xem từng màn, Android bên trái · iOS bên phải. Bấm vào ảnh để mở ảnh gốc.

=== "Permissions"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![Permissions Android](assets/kmp_tab_android_1_permissions.png)](assets/kmp_tab_android_1_permissions.png)
    <figcaption>Android — Pixel 8, API 34</figcaption>
    </figure>
    <figure markdown>
    [![Permissions iOS](assets/kmp_tab_ios_1_permissions.png)](assets/kmp_tab_ios_1_permissions.png)
    <figcaption>iOS — iPhone 17, iOS 26.5</figcaption>
    </figure>
    </div>

=== "Scanner"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![Scanner Android](assets/kmp_tab_android_2_scanner.png)](assets/kmp_tab_android_2_scanner.png)
    <figcaption>Android — Pixel 8, API 34</figcaption>
    </figure>
    <figure markdown>
    [![Scanner iOS](assets/kmp_tab_ios_2_scanner.png)](assets/kmp_tab_ios_2_scanner.png)
    <figcaption>iOS — iPhone 17, iOS 26.5</figcaption>
    </figure>
    </div>

=== "Media"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![Media Android](assets/kmp_tab_android_3_media.png)](assets/kmp_tab_android_3_media.png)
    <figcaption>Android — Pixel 8, API 34</figcaption>
    </figure>
    <figure markdown>
    [![Media iOS](assets/kmp_tab_ios_3_media.png)](assets/kmp_tab_ios_3_media.png)
    <figcaption>iOS — iPhone 17, iOS 26.5</figcaption>
    </figure>
    </div>

=== "API"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![API Android](assets/kmp_tab_android_4_api.png)](assets/kmp_tab_android_4_api.png)
    <figcaption>Android — Pixel 8, API 34</figcaption>
    </figure>
    <figure markdown>
    [![API iOS](assets/kmp_tab_ios_4_api.png)](assets/kmp_tab_ios_4_api.png)
    <figcaption>iOS — iPhone 17, iOS 26.5</figcaption>
    </figure>
    </div>

=== "Monitor"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![Monitor Android](assets/kmp_tab_android_5_monitor.png)](assets/kmp_tab_android_5_monitor.png)
    <figcaption>Android — Pixel 8, API 34</figcaption>
    </figure>
    <figure markdown>
    [![Monitor iOS](assets/kmp_tab_ios_5_monitor.png)](assets/kmp_tab_ios_5_monitor.png)
    <figcaption>iOS — iPhone 17, iOS 26.5</figcaption>
    </figure>
    </div>

<div class="shots shots--pair" markdown>

<figure markdown>
[![GATT giả (Android)](assets/kmp_p2_android_gatt_fake.png)](assets/kmp_p2_android_gatt_fake.png)
<figcaption>GATT giả (Android)</figcaption>
</figure>

</div>

## P6 · API thật bằng Ktor

**Mục tiêu:** tab API gọi mạng thật trên **cả 2 máy** bằng **một** file Kotlin. Làm trước P3 vì không cần máy thật.

1. 🧩 **KMP chung**: [KtorApiService.kt](../shared/src/commonMain/kotlin/com/learn/ndmkmp/core/network/KtorApiService.kt): GET / POST / HTTP 500 / timeout, kết quả 5 nhánh `ApiResult`.
2. 🤖 / 🍎: chỉ khác **engine** (OkHttp / Darwin) và cách nhận ra "không có mạng": [NetworkErrorKind.android.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/core/network/NetworkErrorKind.android.kt) · [NetworkErrorKind.ios.kt](../shared/src/iosMain/kotlin/com/learn/ndmkmp/core/network/NetworkErrorKind.ios.kt).
3. 🧩 **KMP chung**: làm sẵn cho P7: [FileLogger.kt](../shared/src/commonMain/kotlin/com/learn/ndmkmp/core/logging/FileLogger.kt) (JSONL, kotlinx-io) + [LoopDeviceMonitor.kt](../shared/src/commonMain/kotlin/com/learn/ndmkmp/core/background/LoopDeviceMonitor.kt).
4. 🤖 / 🍎: tạo **chỗ duy nhất chọn fake hay thật**: [AndroidServices.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/platform/AndroidServices.kt) · [IosServices.kt](../shared/src/iosMain/kotlin/com/learn/ndmkmp/platform/IosServices.kt).
5. ✅ **Kiểm tra**: test MockEngine, tổng **67 test × 2 máy**; emulator gọi mạng thật: GET 200, POST 201, HTTP 500, timeout 8 s.

<div class="shots shots--pair" markdown>

<figure markdown>
[![Android — tab API gọi mạng thật](assets/kmp_p6_android_api.png)](assets/kmp_p6_android_api.png)
<figcaption>Android — tab API gọi mạng thật</figcaption>
</figure>

</div>

## P3 · Xin quyền thật (Android)

**Mục tiêu:** tab Permissions đúng 4 trạng thái như app cũ. **UI không đổi**, chỉ thay `FakePermissionController`.

1. 🤖 **Android**: [AndroidPermissions.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/core/permission/AndroidPermissions.kt): đọc trạng thái, nhánh API ≤ 30 (`ACCESS_FINE_LOCATION`) và 31+ (`BLUETOOTH_SCAN/CONNECT`), cờ "đã hỏi" để phân biệt DENIED với PERMANENTLY DENIED.
2. 🤖 **Android**: [AndroidPermissionController.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/core/permission/AndroidPermissionController.kt): cài đặt interface `PermissionController` bằng `RequestMultiplePermissions`, mở Settings.
3. 🤖 **Android**: [AndroidBluetoothRadio.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/core/bluetooth/AndroidBluetoothRadio.kt): theo dõi bật/tắt Bluetooth, nút "Turn on Bluetooth".
4. 📱 **Khai báo**: quyền BLE + CAMERA trong [shared AndroidManifest.xml](../shared/src/androidMain/AndroidManifest.xml).
5. 🤖 **Nối dây**: đổi 1 dòng trong `createAndroidServices`: `FakePermissionController` → `AndroidPermissionController`.
6. ✅ **Kiểm tra** (Pixel 8 API 34): mới cài = NOT REQUESTED → từ chối 1 lần = DENIED → 2 lần = PERMANENTLY DENIED → "Open app settings" mở đúng trang; tắt radio thì thẻ đổi OFF ngay.

## P4 · Bluetooth thật (Android)

1. 🤖 **Android**: [AndroidBleClient.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/core/bluetooth/AndroidBleClient.kt) (~780 dòng, port từ `BluetoothClientService` cũ): scan 10 s + lọc trùng, `connectGatt`, hàng đợi thao tác GATT (1 lệnh một lúc), ghi CCCD để bật notify, `requestMtu(247)`, dọn dẹp khi tắt radio.
2. 🧩 **KMP chung** (dùng lại, không sửa): `BluetoothDecoder`, `GattModel` từ P1 giải mã byte.
3. 🤖 **Nối dây**: `createAndroidServices` đổi sang `AndroidBleClient`, `release()` khi Activity bị hủy.
4. ✅ **Kiểm tra** (emulator, không có thiết bị BLE): Scan chạy và tự dừng sau 10 s; tắt radio giữa chừng → danh sách bị xóa; thu hồi quyền → "Not ready".
5. ⏳ **Chưa kiểm tra**: connect / discover / read / notify / write / MTU: cần Samsung A32 + nRF Connect.

## P5 · Camera + thư viện ảnh (Android)

1. 🤖 **Android**: [AndroidMediaPicker.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/core/media/AndroidMediaPicker.kt): `TakePicture` + `PickVisualMedia`, xin quyền CAMERA, đọc tên / dung lượng / kích thước ảnh, xoay theo EXIF.
2. 🧩 **KMP chung** (dùng lại): `MediaInfo` từ P1.
3. 📱 **Khai báo**: `FileProvider` + [file_paths.xml](../shared/src/androidMain/res/xml/file_paths.xml) trong manifest.
4. ✅ **Kiểm tra**: chụp ảnh → hiện ảnh + info (1392 × 1856, 70 KB); hủy chụp → "Capture cancelled"; chọn ảnh từ thư viện; từ chối quyền → chữ đỏ + mở Settings.
5. ⚠️ **Giới hạn đã ghi**: Android kill app khi đang mở Camera thì mất ảnh.

## P7 · Monitor + chạy nền (Android)

1. 🧩 **KMP chung** (đã có từ P6): `LoopDeviceMonitor` + `FileLogger` = vòng đọc trạng thái và ghi JSONL, dùng chung.
2. 🤖 **Android**: [AndroidDeviceStatusSource.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/core/device/AndroidDeviceStatusSource.kt): đọc pin, mạng, Bluetooth.
3. 🤖 **Android**: [MonitorService.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/core/background/MonitorService.kt) (Foreground Service + notification Stop) và [MonitorWorker.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/core/background/MonitorWorker.kt) (WorkManager 15 phút), điều khiển qua [AndroidPlatformBackground.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/core/background/AndroidPlatformBackground.kt).
4. 🤖 **Android**: [AndroidMonitorRuntime.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/core/background/AndroidMonitorRuntime.kt): một monitor cho cả màn hình, service và worker; [NotificationGatedMonitor.kt](../shared/src/androidMain/kotlin/com/learn/ndmkmp/core/background/NotificationGatedMonitor.kt) xin quyền thông báo trước khi bật FGS.
5. 📱 **Khai báo**: quyền mạng, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`, `<service>` trong manifest.
6. ✅ **Kiểm tra**: pin đổi theo `adb emu power capacity 42`; app ở nền vẫn ghi bản ghi "fgs"; WorkManager ENQUEUED + bản ghi "worker" đầu tiên. Từ đây **cả 5 interface trên Android đều là code thật**.

## Còn lại

| Việc | Tầng | Cần gì |
|---|---|---|
| P3 / P4 / P5 / P7 bên iOS | 🍎 `iosMain` | `CBManager` + `AVCaptureDevice`, `CoreBluetooth`, `PHPicker`, `UIDevice` + `BGTaskScheduler`; iPhone thật |
| P4 GATT end-to-end | ✅ | Samsung A32 + iPhone + nRF Connect |
| P8 Final | ✅ | checklist E2E task 006, README "cũ vs mới" |
