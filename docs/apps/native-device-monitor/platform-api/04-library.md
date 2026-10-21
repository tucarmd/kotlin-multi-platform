[← 03 Notification](03-notification.md) · [↑ Platform API](index.md) · [05 Picker →](05-picker.md)

# 04 · Library Permission (thư viện ảnh / media)

!!! info "App này cố ý KHÔNG xin quyền thư viện"
    Tab Media chỉ cần **chọn 1 ảnh** do user chỉ định → dùng **Picker** (không cần quyền, xem [05 Picker](05-picker.md)). Trang này dành cho lúc app cần **đọc cả thư viện** (gallery tự vẽ, backup ảnh, quét ảnh) hoặc **lưu ảnh vào thư viện**.

    Quy tắc chọn: **chọn vài ảnh → Picker. Duyệt / quét cả thư viện → quyền Library.** Google Play và App Review đều hỏi vì sao bạn cần quyền thư viện đầy đủ.

## 1. API

=== "Android"

    Quyền đổi **3 lần** theo version:

    | Android | Đọc ảnh | Đọc video | Ghi ảnh của app |
    |---|---|---|---|
    | ≤ 9 (API ≤ 28) | `READ_EXTERNAL_STORAGE` | như trái | `WRITE_EXTERNAL_STORAGE` |
    | 10–12 (API 29–32) | `READ_EXTERNAL_STORAGE` | như trái | **không cần**, `MediaStore.insert` |
    | 13 (API 33) | `READ_MEDIA_IMAGES` | `READ_MEDIA_VIDEO` | không cần |
    | 14+ (API 34+) | `READ_MEDIA_IMAGES` **hoặc** chỉ `READ_MEDIA_VISUAL_USER_SELECTED` (user chọn "Select photos") | như trái | không cần |

    ```xml
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />  <!-- API 34: hỗ trợ "truy cập một phần" -->
    ```

    ```kotlin
    // Android 14+: xin cả hai, user chọn "Allow all" hoặc "Select photos"
    launcher.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED))

    val full = isGranted(READ_MEDIA_IMAGES)
    val partial = !full && isGranted(READ_MEDIA_VISUAL_USER_SELECTED)   // chỉ thấy ảnh user đã chọn
    // Đọc: ContentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, …)
    ```

=== "iOS"

    | Key `Info.plist` | Khi nào |
    |---|---|
    | `NSPhotoLibraryUsageDescription` | Đọc (và ghi) thư viện |
    | `NSPhotoLibraryAddUsageDescription` | **Chỉ thêm** ảnh vào thư viện (`UIImageWriteToSavedPhotosAlbum`, `PHAssetCreationRequest`) |
    | `PHPhotoLibraryPreventAutomaticLimitedAccessAlert` = `YES` | Tắt alert "Select more photos" iOS tự hiện mỗi lần mở app khi đang Limited |

    ```swift
    let status = PHPhotoLibrary.authorizationStatus(for: .readWrite)   // hoặc .addOnly
    let new = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
    switch new {
    case .notDetermined: break
    case .authorized:    break                 // toàn bộ thư viện
    case .limited:       break                 // iOS 14+: chỉ các ảnh user đã chọn
    case .denied, .restricted: break
    @unknown default:    break
    }
    // Limited: cho user chọn thêm ảnh
    PHPhotoLibrary.shared().presentLimitedLibraryPicker(from: viewController)
    // Lắng nghe thay đổi (user chọn thêm / bớt): PHPhotoLibraryChangeObserver
    ```

## 2. Trạng thái: Android ↔ iOS

| Ý nghĩa | Android 14+ | iOS |
|---|---|---|
| Chưa hỏi | DENIED cả hai + chưa lưu cờ | `.notDetermined` |
| Toàn bộ | `READ_MEDIA_IMAGES` GRANTED | `.authorized` |
| **Một phần** | chỉ `READ_MEDIA_VISUAL_USER_SELECTED` GRANTED | `.limited` |
| Chỉ ghi | (không cần quyền) | `.addOnly` → `.authorized` |
| Từ chối | như quyền khác | `.denied` / `.restricted` |

## 3. Vòng đời: khởi động lại, kill, crash

| Chuyện xảy ra | Android | iOS |
|---|---|---|
| Khởi động lại app | Full giữ nguyên. **Partial (API 34)**: quyền giữ, nhưng hệ thống có thể hỏi lại để chọn ảnh khác; ảnh đã chọn vẫn đọc được | Full / Limited giữ nguyên. Limited: iOS tự hiện alert chọn thêm ảnh (trừ khi tắt bằng key ở trên) |
| Hệ thống kill / crash | Không ảnh hưởng quyền. Cursor / stream đang mở bị mất | Không ảnh hưởng quyền |
| Đổi quyền trong Settings | Thu hồi → process bị kill | App bị kill |
| User xoá ảnh bên ngoài app | `Uri` cũ → `FileNotFoundException` | `PHAsset` cũ → fetch trả rỗng; observer báo thay đổi |
| Xoá app cài lại | Mất quyền | Mất quyền |

## 4. Use case & error case

| Mã | Tình huống | Triệu chứng | Xử lý | Cách giả lập |
|---|---|---|---|---|
| `M01` | Xin `READ_EXTERNAL_STORAGE` trên Android 13+ | Không dialog, luôn denied | Xin `READ_MEDIA_*` theo version (bảng trên) | Chạy code cũ trên API 33 |
| `M02` | Android 14 user chọn "Select photos" | App coi là bị từ chối / gallery trống | Xử lý **3 trạng thái** (full / partial / none), có nút "Chọn thêm ảnh" (gọi `launch` lại) | Trong dialog chọn "Select photos" |
| `M03` | iOS `.limited` | Gallery chỉ có vài ảnh, user tưởng app lỗi | Hiện dòng "Đang xem ảnh bạn đã chọn · Chọn thêm" | Dialog chọn "Limit Access…"; Simulator: `xcrun simctl privacy booted grant photos com.learn.ndmkmp` cho full |
| `M04` | Thiếu `NSPhotoLibraryAddUsageDescription` khi lưu ảnh (iOS) | **Crash** lúc gọi `UIImageWriteToSavedPhotosAlbum` | Thêm key | Xoá key, bấm "Lưu ảnh" |
| `M05` | Google Play từ chối bản build | "Photo and video permissions policy" | Không phải gallery/editor → bỏ `READ_MEDIA_*`, dùng Photo Picker | — |
| `M06` | Đọc ảnh trên main thread | Giật / ANR khi thư viện lớn | Query + decode trên `Dispatchers.IO` / background queue | Thư viện 2.000 ảnh, query trong `onCreate` |
| `M07` | Ảnh iCloud chưa tải về máy (iOS) | `requestImage` trả ảnh mờ / `nil` | `PHImageRequestOptions.isNetworkAccessAllowed = true` + hiện tiến trình | Bật iCloud Photos "Optimize Storage" trên máy thật |
| `M08` | Ảnh bị xoay sai | Ảnh dọc hiển thị nằm ngang | Đọc EXIF orientation (app này: `rotationOf()` trong [`AndroidMediaPicker.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/shared/src/androidMain/kotlin/com/learn/ndmkmp/core/media/AndroidMediaPicker.kt)) | Ảnh chụp dọc trên máy Samsung |
| `M09` | Thư viện trống trên máy ảo | Không có gì để test | Đẩy ảnh vào | Android: `adb push a.jpg /sdcard/Pictures/` rồi mở app Files/Photos để media scanner quét. iOS: `xcrun simctl addmedia booted a.jpg` |

## 5. Khác biệt cần nhớ

- Cả hai đều có **truy cập một phần** (Android 14 / iOS 14), phải xử lý như một trạng thái riêng, không phải "từ chối".
- iOS tách **đọc** và **chỉ ghi**; Android từ 10 trở đi **ghi ảnh của mình không cần quyền**.
- Thiếu key trên iOS là **crash**; thiếu quyền trên Android là **denied im lặng**.
- 90% app chỉ cần [Picker](05-picker.md). Xin quyền Library khi không cần = thêm 1 dialog đáng sợ + rủi ro bị store từ chối.
