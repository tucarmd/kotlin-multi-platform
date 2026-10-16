---
name: kmp-run
description: >-
  Build, cài, chạy và nhìn tận mắt app KMP trong repo kotlin-multi-platform trên Android (emulator/máy thật) và iOS
  (Simulator/iPhone), chụp màn hình hai bên để so. Chỉ biết đường dẫn và lệnh riêng của khung apps/<App>; phần
  đọc log, crash, logcat, ký app thì chuyển sang kotlin-android-build-run và swift-ios-build-run. Dùng khi nói
  "chạy app X", "build cả hai", "run android ios", "chụp màn hình hai bên", "xem UI giống nhau chưa", "chạy B5/B6".
---

# kmp-run — chạy hai nền tảng

Lệnh đúng của từng app nằm ở mục **Chạy** trong `apps/<App>/README.md`. Nếu README khác với các lệnh dưới đây thì
làm theo README.

## Android

```bash
cd apps/<App>
./gradlew :androidApp:installDebug
adb shell am start -n dev.<app>/dev.<app>.android.MainActivity
adb exec-out screencap -p > /tmp/<app>-android.png
```

## iOS

```bash
cd apps/<App>/iosApp
xcodegen                                  # chỉ cần khi đã sửa project.yml
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  -derivedDataPath build/dd CODE_SIGNING_ALLOWED=NO build
xcrun simctl install booted build/dd/Build/Products/Debug-iphonesimulator/<App>.app
xcrun simctl launch booted dev.<app>.ios
xcrun simctl io booted screenshot /tmp/<app>-ios.png
```

Build iOS đầu tiên sẽ lâu vì phải link framework `Shared`. Chậm bất thường thì dùng `kotlin-tooling-native-build-performance`.

## Kiểm B5 / B6

- Đọc hai ảnh chụp và so với `design/mockups/` (nếu có): cùng nội dung, cùng thứ tự, cùng các trạng thái.
- Đi qua **mọi nhánh** của sealed state, không chỉ màn `Ready`: nhập sai, rỗng, đang tải, lỗi mạng…
- Muốn gõ thử không cần chạm tay: `adb shell input text 100000`. Để lặp lại được thì viết thành flow Maestro (`kmp-auto-test`).

## Chuyển skill khi

| Gặp | Skill |
|---|---|
| Gradle lỗi, crash, ANR, logcat, cài lên máy thật Android | `kotlin-android-build-run` |
| `xcodebuild` lỗi, crash iOS, chạy lên iPhone thật, xem network | `swift-ios-build-run` |
| Cần iPhone thật mà chỉ có profile CarMD | `ios-device-carmd-identity` |
