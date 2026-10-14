# Build và phát hành

Mỗi nền tảng vẫn phát hành theo cách riêng của nó: AAB lên Google Play, archive Xcode lên App Store, gói cài đặt cho Desktop.
KMP không thay đổi phần này — nó chỉ làm cho code bên trong là chung.

## Mục tiêu

- Build được bản phát hành cho cả 3 nền tảng.
- CI GitHub Actions chạy test mỗi lần push.

## Ba nền tảng, ba đường

| Nền tảng | Build | Đi đâu |
|---|---|---|
| Android | `./gradlew :composeApp:bundleRelease` → file `.aab` (cần ký bằng upload key) | Google Play Console |
| iOS | Xcode → *Product → Archive* | App Store Connect / TestFlight |
| Desktop | `./gradlew :composeApp:packageDmg` (macOS), `packageMsi` (Windows), `packageDeb` (Linux) | tải trực tiếp hoặc store |

Gói Desktop cấu hình trong `compose.desktop { application { nativeDistributions { … } } }`: tên gói, phiên bản, icon.
Mỗi hệ điều hành chỉ build được gói của chính nó → dùng CI nhiều runner nếu cần cả ba.

## App Store privacy manifest

Apple yêu cầu file `PrivacyInfo.xcprivacy` khai báo lý do dùng một số API nhạy cảm (gọi là *required reason APIs*).
Thư viện Kotlin bạn dùng có thể chạm tới các API đó → kiểm theo hướng dẫn docs trước khi nộp.

## CI với GitHub Actions

```yaml
name: test
on: [push, pull_request]
jobs:
  test:
    runs-on: macos-latest        # cần macOS để chạy test iOS
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew :composeApp:allTests
```

## Làm trên app MVP

1. Đặt `versionName`/`versionCode` Android và version Xcode cùng là `1.0.0`.
2. Build cả 3 bản, cài thử bản Desktop từ file `.dmg`.
3. Thêm workflow CI ở trên, push, thấy dấu ✓ xanh trên GitHub.

## Kiểm tra

- [ ] Có file `.aab`, một archive trong Xcode Organizer, và file `.dmg`.
- [ ] CI xanh.

## Tiếp theo?

- [Bài 14: đưa app Android có sẵn sang KMP](14-migrate-android.md) — nếu bạn có app cũ.
- Hoặc quay lại [checklist MVP](../mvp/checklist.md) và đánh dấu nốt.

## Docs gốc

[Publish your application](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-apps.html) · [Native distributions](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html) · [Privacy manifest](https://kotlinlang.org/docs/multiplatform/multiplatform-privacy-manifest.html) · [GitHub Actions for KMP](https://kotlinlang.org/docs/multiplatform/github-actions-for-kmp.html) · [Build final native binaries](https://kotlinlang.org/docs/multiplatform/multiplatform-build-native-binaries.html)
