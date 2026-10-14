# Cài máy, tạo project đầu tiên

Bài này dựng project Device Pulse bằng wizard và chạy nó trên Android emulator lẫn iOS simulator. Chưa viết tính năng — chỉ cần thấy app chạy.

## Mục tiêu

- Máy có đủ công cụ để build Android và iOS.
- Project `DevicePulse` chạy được trên Android emulator và iOS simulator.
- Biết chỗ giả lập pin, vị trí, mạng — các bài sau sẽ dùng.

## Cài công cụ

1. **IDE:** IntelliJ IDEA hoặc Android Studio, kèm **plugin Kotlin Multiplatform**. Plugin thêm cấu hình chạy iOS ngay trong IDE và có bước kiểm môi trường.
2. **Xcode** (từ App Store) — bắt buộc để build iOS, chỉ chạy trên macOS. Mở Xcode một lần để nó cài thêm thành phần, rồi tải một iOS Simulator.
3. **JDK 17+** — IDE thường tự kèm. Android SDK + emulator cài qua IDE (*Device Manager*).

!!! tip "Kiểm môi trường trước"
    Plugin KMP có bước *preflight checks* báo thiếu gì (Xcode, JDK, Android SDK…). Sửa hết đỏ rồi mới tạo project.

!!! note "Dùng VS Code được không?"
    Được để **đọc / sửa code Kotlin**, chưa đủ cho cả dự án KMP (10/2026):

    - Cài extension **Kotlin by JetBrains** (`JetBrains.kotlin-server`, bản Alpha) — completion, báo lỗi, điều hướng, format, import Gradle.
      Docs ghi rõ: *hỗ trợ Kotlin Multiplatform đang được phát triển*.
    - Máy vẫn cần **JDK 17+**, **Android SDK**, **Xcode** — VS Code không thay được các thứ này.
    - Build/chạy bằng terminal: `./gradlew :composeApp:assembleDebug` (Android), `./gradlew :composeApp:allTests` (test); iOS mở `iosApp/` bằng Xcode.

    Khuyên dùng: tạo project + chạy app bằng IntelliJ IDEA / Android Studio, VS Code chỉ là editor phụ.

## Tạo project bằng wizard

Dùng wizard trong IDE (*New Project → Kotlin Multiplatform*) hoặc trang web wizard của JetBrains:

- **Tên:** `DevicePulse` · **package:** `dev.devicepulse`
- **Nền tảng:** Android, iOS
- **iOS UI:** chọn **Share UI** (Compose Multiplatform)
- Bỏ trống các nền tảng còn lại (Web, Server)

Wizard sinh sẵn Gradle, version catalog, project Xcode `iosApp/` và một màn mẫu. **Đừng tự gõ Gradle từ đầu** — dùng file wizard sinh rồi đọc hiểu nó (bài 3).

## Chạy trên Android và iOS

| Nền tảng | Cách chạy |
|---|---|
| Android | chọn cấu hình `composeApp` trong IDE → chọn emulator → chạy (hoặc `./gradlew :composeApp:installDebug`) |
| iOS | chọn cấu hình `iosApp` → chọn simulator → chạy (lần đầu build lâu vài phút); hoặc mở `iosApp/iosApp.xcodeproj` bằng Xcode rồi Run |

Tên task và cấu hình có thể khác chút tuỳ phiên bản wizard; xem tab Gradle trong IDE.

## Giả lập cảm biến (dùng từ bài 9)

Device Pulse đọc pin, vị trí và mạng. Trên máy ảo bạn tự "bơm" giá trị:

| | Android Emulator | iOS Simulator |
|---|---|---|
| Pin | *Extended Controls* (nút `⋯`) → **Battery**: mức %, đang sạc | không có pin — app sẽ hiện "Không hỗ trợ" |
| Vị trí | *Extended Controls* → **Location**: nhập toạ độ hoặc phát lộ trình | menu **Features → Location**: Custom Location, City Run, Freeway Drive |
| Mạng | *Extended Controls* → **Cellular**, hoặc bật chế độ máy bay | tắt Wi-Fi của Mac (simulator dùng mạng của Mac) |

Muốn đo pin thật trên iOS thì cần iPhone thật — không bắt buộc cho lộ trình.

## Làm trên Device Pulse

1. Mở `App.kt` trong `commonMain`, đổi chữ trên nút mẫu thành **"Device Pulse"**.
2. Chạy lại trên emulator và simulator. Một chỗ sửa, hai nơi đổi — đó là cảm giác của KMP.
3. Mở thử *Extended Controls* của emulator và menu *Features → Location* của simulator để biết chúng nằm đâu.

## Kiểm tra

- [ ] App chạy trên **Android** emulator, chụp được màn hình.
- [ ] App chạy trên **iOS** simulator, chụp được màn hình.
- [ ] Sửa `App.kt` một lần, cả hai nền tảng đổi theo.
- [ ] Tìm được chỗ chỉnh pin/vị trí trên emulator và chỗ chỉnh vị trí trên simulator.

## Lỗi hay gặp

- Build iOS báo không tìm thấy Xcode → chạy `sudo xcode-select -s /Applications/Xcode.app` (bạn tự gõ, cần mật khẩu máy).
- Lần đầu Gradle tải rất lâu → bình thường; đừng huỷ giữa chừng.
- Xem thêm [lỗi hay gặp](../reference/troubleshooting.md).

## Tiếp theo?

- [Bài 3: cấu trúc project](03-project-structure.md) — đọc hiểu những gì wizard vừa sinh.
- Đọc thêm: [Quickstart](https://kotlinlang.org/docs/multiplatform/quickstart.html) · [Recommended IDEs](https://kotlinlang.org/docs/multiplatform/recommended-ides.html) · [Build and run](https://kotlinlang.org/docs/multiplatform/build-and-run-kmp.html).

## Docs gốc

[Get started](https://kotlinlang.org/docs/multiplatform/get-started.html) · [Create app with shared UI](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-new-project.html)
