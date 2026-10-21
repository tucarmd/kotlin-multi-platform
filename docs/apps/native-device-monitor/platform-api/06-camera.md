[← 05 Picker](05-picker.md) · [↑ Platform API](index.md) · [07 Lifecycle →](07-lifecycle.md)

# 06 · Camera Permission

!!! info "App này dùng ở đâu"
    - Tab **Permissions**: thẻ Camera (xin quyền thử, tập các nhánh granted / denied).
    - Tab **Media**: nút "Chụp". Android mở **app Camera của hệ thống** (`TakePicture` + `FileProvider`) trong [`AndroidMediaPicker.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/shared/src/androidMain/kotlin/com/learn/ndmkmp/core/media/AndroidMediaPicker.kt); iOS mở `UIImagePickerController(.camera)` trong [`MediaNative.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/iosApp/iosApp/Platform/MediaNative.swift).
    - Quyền: [`PermissionNative.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/iosApp/iosApp/Platform/PermissionNative.swift) (`AVCaptureDevice`), [manifest](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/shared/src/androidMain/AndroidManifest.xml) (`CAMERA`, `FileProvider`).

## 1. Hai cách dùng camera

| Cách | Android | iOS | Cần quyền? |
|---|---|---|---|
| **Nhờ app camera hệ thống** chụp hộ | `ActivityResultContracts.TakePicture(uri)` | `UIImagePickerController(sourceType: .camera)` | Android: **không**, NHƯNG nếu manifest có khai `CAMERA` thì **bắt buộc** phải được cấp. iOS: **có** (prompt tự hiện) |
| **Tự vẽ preview** trong app | CameraX / Camera2 | `AVCaptureSession` | Có |

App này dùng cách 1 (đủ cho "chụp 1 ảnh"). CameraX = 3 thư viện + gắn lifecycle, quá mức cho mục tiêu.

## 2. API

=== "Android"

    ```xml
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />  <!-- máy không camera vẫn cài được -->

    <!-- TakePicture: đưa content:// cho app Camera ghi vào -->
    <provider android:name="androidx.core.content.FileProvider"
        android:authorities="${applicationId}.fileprovider"
        android:exported="false" android:grantUriPermissions="true">
        <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths" />
    </provider>
    ```

    ```kotlin
    val hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> … }
    val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved: Boolean ->
        // true = app Camera đã ghi ảnh vào uri · false = Cancel
    }

    if (!isGranted(CAMERA)) requestCamera.launch(Manifest.permission.CAMERA)
    else takePicture.launch(FileProvider.getUriForFile(context, "$packageName.fileprovider", file))
    ```

=== "iOS"

    ```xml
    <key>NSCameraUsageDescription</key>
    <string>Take a photo on the Media tab…</string>
    <!-- Quay video có tiếng: thêm NSMicrophoneUsageDescription -->
    ```

    ```swift
    guard UIImagePickerController.isSourceTypeAvailable(.camera) else { return }   // Simulator = false

    switch AVCaptureDevice.authorizationStatus(for: .video) {
    case .notDetermined:
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async { … }          // completion chạy trên queue bất kỳ
        }
    case .authorized:  presentCamera()
    case .denied, .restricted: showOpenSettings()
    @unknown default: showOpenSettings()
    }
    ```

## 3. Trạng thái: Android ↔ iOS

| Ý nghĩa | Android | iOS |
|---|---|---|
| Chưa hỏi | DENIED + chưa lưu cờ | `.notDetermined` |
| Cho | GRANTED | `.authorized` |
| Chỉ lần này | GRANTED tới khi app rời foreground một lúc (Android 11+) | (không có cho camera) |
| Từ chối, còn hỏi được | rationale `true` | không có |
| Từ chối hẳn | đã hỏi + rationale `false` | `.denied` |
| Bị chặn | Công tắc **Camera access** trong Quick Settings (Android 12+): quyền vẫn GRANTED nhưng hình **đen** | `.restricted` (Screen Time) |
| Không có camera | `FEATURE_CAMERA_ANY` = false | `isSourceTypeAvailable(.camera)` = false (**Simulator**) |

## 4. Vòng đời: khởi động lại, kill, crash

| Chuyện xảy ra | Android | iOS |
|---|---|---|
| Khởi động lại app | Quyền giữ; "Only this time" có thể đã hết | Quyền giữ |
| **Hệ thống kill app khi app Camera đang mở** (máy ít RAM, chuyện **thường gặp**) | Activity được tạo lại, `TakePicture` vẫn trả `true`, ảnh **đã ghi** vào file. Nhưng nếu `uri` chỉ nằm trong biến / coroutine thì **mất**, không biết file nào. App này: ảnh mất, user chụp lại (đã ghi trong doc) | Camera picker chạy trong process của app, app chết thì camera đóng theo |
| Crash khi đang chụp | File 0 byte còn lại trong `cacheDir` | Không để lại file |
| Đổi quyền trong Settings | Thu hồi → process bị kill | App **bị kill** |
| App khác đang dùng camera | CameraX: `ERROR_CAMERA_IN_USE`; Intent: app Camera tự xử lý | `AVCaptureSession` bị interrupt (`.videoDeviceInUseByAnotherClient`) |
| Cuộc gọi đến khi đang quay | Session bị ngắt | `AVCaptureSessionWasInterrupted` |

## 5. Use case & error case

| Mã | Tình huống | Triệu chứng | Xử lý | Cách giả lập |
|---|---|---|---|---|
| `C01` | Thiếu `NSCameraUsageDescription` (iOS) | **Crash** ngay khi gọi `requestAccess` / mở camera | Thêm key | Xoá key khỏi `Info.plist` |
| `C02` | Khai `CAMERA` trong manifest, chưa cấp, gọi `TakePicture` (Android) | **Crash** `SecurityException` | Kiểm tra quyền trước (app này: `capture()` trả `null` nếu chưa cấp) | Bỏ dòng kiểm tra, `adb shell pm revoke com.learn.ndmkmp android.permission.CAMERA`, bấm Chụp |
| `C03` | Đưa `file://` cho app Camera | **Crash** `FileUriExposedException` (API 24+) | Dùng `FileProvider` → `content://` | `Uri.fromFile(file)` thay cho `getUriForFile` |
| `C04` | `file_paths.xml` không khớp thư mục | `IllegalArgumentException: Failed to find configured root` | Thư mục trong xml phải khớp (`images/` trong `cacheDir`) | Đổi `CAPTURE_DIR` thành tên khác |
| `C05` | Kill app trong lúc app Camera mở (Android) | Chụp xong quay lại, không có ảnh | Lưu `uri` vào `SavedStateHandle` / `rememberSaveable` trước khi `launch` | Bấm Chụp → khi app Camera đang hiện chạy `adb shell am kill com.learn.ndmkmp` → chụp → OK. Hoặc "Don't keep activities" |
| `C06` | User Cancel sau khi app Camera đã tạo file | File 0 byte, decode `null` | Kiểm tra `saved == false` và kích thước file; dọn `cacheDir` | Mở camera, bấm Back |
| `C07` | Mở camera trên Simulator (iOS) | Crash `Source type 1 not available` nếu không kiểm tra | `isSourceTypeAvailable(.camera)`; app này ẩn nút | Bỏ guard, chạy Simulator |
| `C08` | Quick Settings "Camera access" tắt (Android 12+) | Quyền GRANTED nhưng preview đen / hệ thống hiện dialog "unblock camera" | Không cần code riêng với Intent; CameraX: báo lỗi rõ | Kéo Quick Settings › tắt "Camera access" |
| `C09` | Ảnh bị xoay sai / quá nặng | Ảnh dọc nằm ngang; OOM khi decode 12 MP | Đọc EXIF, decode `inSampleSize` (Android); `UIGraphicsImageRenderer` thu nhỏ (iOS). App này làm cả hai | Chụp dọc trên Samsung |
| `C10` | Emulator không có camera / camera đen | `hasCamera` false hoặc ảnh trống | AVD Manager › Advanced › Camera: `Emulated` hoặc `Webcam0` | Tạo AVD với camera `None` |
| `C11` | Xin Camera khi user chưa bấm gì | Tỉ lệ từ chối cao | Xin đúng lúc bấm "Chụp" (app này: Media tab xin khi bấm) | — |
| `C12` | Chạm camera trong extension / widget (iOS) | Không được phép | Chỉ app chính được dùng camera | — |

## 6. Khác biệt cần nhớ

- **Android có bẫy ngược**: dùng Intent camera thì *không cần* quyền, nhưng *đã khai* `CAMERA` mà không cấp thì crash. iOS luôn cần quyền + key.
- Android hay bị **kill khi app Camera đang mở** (app Camera nặng RAM). iOS gần như không.
- Simulator **không có camera**; Android emulator có camera ảo.
- Chấm xanh (iOS) / icon camera trên status bar (Android 12+) báo user app đang dùng camera, không tắt được.
