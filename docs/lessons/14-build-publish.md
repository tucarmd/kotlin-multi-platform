# Build và phát hành

Mỗi nền tảng vẫn phát hành theo cách riêng: AAB lên Google Play, archive Xcode lên App Store Connect.
KMP không đổi phần này — nó chỉ làm cho code bên trong là chung. Bài cuối đưa Device Pulse thành bản release, gắn báo crash và CI.

## Mục tiêu

- Có file `.aab` đã ký và một archive iOS trong Xcode Organizer.
- Crash trên cả hai nền tảng được báo về một dashboard, có stack trace đọc được.
- GitHub Actions chạy test mỗi lần push.

## Android: AAB đã ký

```kotlin
// androidApp/build.gradle.kts
android {
    defaultConfig { versionCode = 1; versionName = "1.0.0" }
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

Chạy `./gradlew :androidApp:bundleRelease` → `androidApp/build/outputs/bundle/release/androidApp-release.aab`.

- Mật khẩu keystore lấy từ biến môi trường, **không** commit file `.jks` hay mật khẩu.
- Bật Play App Signing: key trên máy bạn chỉ là *upload key*, mất thì xin Google đặt lại được.
- `isMinifyEnabled = true` (R8) thu nhỏ app nhưng có thể xoá nhầm lớp dùng qua reflection. Thư viện như Ktor, kotlinx.serialization
  thường tự kèm luật; nếu bản release crash mà debug thì không, kiểm `proguard-rules.pro` và file `mapping.txt`.
- Cài thử bản release trên máy thật: quyền vị trí, đo độ trễ (cần `INTERNET`) vẫn chạy.

## iOS: archive trong Xcode

1. Mở `iosApp/iosApp.xcodeproj`, target `iosApp` → *Signing & Capabilities*: chọn Team, bundle id riêng.
2. Đặt *Version* `1.0.0`, *Build* `1`.
3. Kiểm `Info.plist` có `NSLocationWhenInUseUsageDescription` (bài 10) với câu giải thích rõ ràng — App Review từ chối câu chung chung.
4. Chọn *Any iOS Device (arm64)* → *Product → Archive* → *Distribute App* → App Store Connect / TestFlight.

Build Phase của wizard tự gọi Gradle để build framework Kotlin ở cấu hình Release, không cần bước riêng.

!!! note "Privacy manifest"
    Apple yêu cầu `PrivacyInfo.xcprivacy` khai báo dữ liệu thu thập (Device Pulse đọc vị trí) và lý do dùng một số
    *required reason APIs*. Thư viện Kotlin cũng có thể chạm tới các API đó — kiểm theo hướng dẫn trong Docs gốc trước khi nộp.

## Báo crash

| Lựa chọn | Android | iOS |
|---|---|---|
| Firebase Crashlytics + **CrashKiOS** | SDK Crashlytics bình thường | CrashKiOS (Touchlab) chuyển exception Kotlin/Native thành báo cáo Crashlytics đọc được |
| **Sentry KMP** | một SDK Kotlin Multiplatform dùng từ `commonMain` | cùng SDK đó, gọi khởi tạo ở `iosMain` |

Với cả hai:

- Android: upload `mapping.txt` để giải mã stack trace sau R8 (plugin Gradle của Crashlytics/Sentry làm tự động).
- iOS: upload **dSYM** của app *và* của framework Kotlin, nếu không stack trace chỉ là địa chỉ bộ nhớ.
  Bật `Debug Information Format = DWARF with dSYM File` cho Release.
- Kermit (bài 5) có writer gửi log vào Crashlytics/Sentry làm breadcrumb, giúp biết trước crash app đang làm gì.

## CI với GitHub Actions

Test JVM/Android chạy được trên Linux (rẻ, nhanh); test iOS cần runner macOS.

```yaml
name: test
on: [push, pull_request]
jobs:
  android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew :shared:testDebugUnitTest
  ios:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew :shared:iosSimulatorArm64Test
```

Muốn một lệnh cho tất cả thì chạy `allTests` trên runner macOS — chậm và tốn phút hơn.

## Làm trên Device Pulse

1. Đặt `versionName`/`versionCode` Android và Version/Build Xcode cùng là `1.0.0` / `1`.
2. Build `.aab` đã ký và archive iOS; cài bản release lên máy thật và đi lại bảng test tay của [bài 13](13-testing.md).
3. Gắn Crashlytics + CrashKiOS hoặc Sentry; tạo một crash thử ở bản debug, thấy stack trace có tên hàm Kotlin trên dashboard.
4. Thêm workflow CI ở trên, push, thấy cả hai job xanh.

## Làm sau

Những thứ cố ý để ngoài phạm vi, hợp để luyện tiếp:

- Theo dõi nền: ghi lần đo định kỳ (WorkManager trên Android, `BGTaskScheduler` trên iOS).
- Thông báo khi pin xuống `Critical` hoặc mất mạng.
- Biểu đồ pin / độ trễ theo thời gian từ bảng `snapshot`.
- Xuất lịch sử ra CSV, widget màn hình chính.
- Thêm target Desktop / Web: `shared/` dùng lại nguyên, chỉ cần viết UI cho nền tảng mới (hoặc thử Compose Multiplatform cho riêng nền tảng đó).

## Kiểm tra

- [ ] Android: có file `.aab` đã ký; bản release cài lên máy chạy đủ 3 card và màn History.
- [ ] iOS: archive hiện trong Xcode Organizer; hộp thoại vị trí hiện đúng câu giải thích.
- [ ] Crash thử hiện trên dashboard với stack trace đọc được trên cả Android và iOS.
- [ ] CI: job ubuntu và job macOS đều xanh.
- [ ] Không có keystore, mật khẩu hay key nào trong git.

Kẹt? Xem [Lỗi hay gặp](../reference/troubleshooting.md).

## Tiếp theo?

- Quay lại [checklist dự án](../project/checklist.md) và đánh dấu nốt.
- Tự đánh giá lại toàn bộ lộ trình: [Đánh giá KMP & kế hoạch học](../plan/kmp-review.md).

## Docs gốc

[Publish your application](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-apps.html) · [Privacy manifest](https://kotlinlang.org/docs/multiplatform/multiplatform-privacy-manifest.html) · [GitHub Actions for KMP](https://kotlinlang.org/docs/multiplatform/github-actions-for-kmp.html) · [Sign your app](https://developer.android.com/studio/publish/app-signing) · [Shrink, obfuscate, and optimize](https://developer.android.com/build/shrink-code) · [Distributing your app](https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases) · [Privacy manifest files](https://developer.apple.com/documentation/bundleresources/privacy-manifest-files)
