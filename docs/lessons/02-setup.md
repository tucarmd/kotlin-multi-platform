# Cài máy, tạo project đầu tiên

Bài này dựng project Habit Tick bằng wizard và chạy nó trên cả 3 nền tảng. Chưa viết tính năng — chỉ cần thấy app chạy.

## Mục tiêu

- Máy có đủ công cụ để build Android, iOS, Desktop.
- Project `habit-tick` chạy được trên Android emulator, iOS simulator và cửa sổ Desktop.

## Cài công cụ

1. **IDE:** IntelliJ IDEA hoặc Android Studio, kèm **plugin Kotlin Multiplatform**. Plugin thêm cấu hình chạy iOS ngay trong IDE và có bước kiểm môi trường.
2. **Xcode** (từ App Store) — bắt buộc để build iOS. Mở Xcode một lần để nó cài thêm thành phần, rồi chọn một iOS Simulator.
3. **JDK** — IDE thường tự kèm. Android SDK + emulator cài qua IDE.

!!! tip "Kiểm môi trường trước"
    Plugin KMP có bước *preflight checks* báo thiếu gì (Xcode, JDK, Android SDK…). Sửa hết đỏ rồi mới tạo project.

!!! note "Dùng VS Code được không?"
    Được để **đọc / sửa code Kotlin**, chưa đủ cho cả dự án KMP (10/2026):

    - Cài extension **Kotlin by JetBrains** (`JetBrains.kotlin-server`, bản Alpha) — completion, báo lỗi, điều hướng, format, import Gradle.
      Docs ghi rõ: *hỗ trợ Kotlin Multiplatform đang được phát triển*.
    - Máy vẫn cần **JDK 17+**, **Android SDK**, **Xcode** — VS Code không thay được các thứ này.
    - Build/chạy bằng terminal: `./gradlew :composeApp:assembleDebug`, `./gradlew :composeApp:run`; iOS mở `iosApp/` bằng Xcode.

    Khuyên dùng: tạo project + chạy app bằng IntelliJ IDEA / Android Studio, VS Code chỉ là editor phụ.

## Tạo project bằng wizard

Dùng wizard trong IDE (*New Project → Kotlin Multiplatform*) hoặc trang web wizard của JetBrains:

- **Tên:** `HabitTick` · **package:** `dev.habittick`
- **Nền tảng:** Android, iOS, Desktop
- **iOS UI:** chọn **Share UI** (Compose Multiplatform)
- Không chọn Server, Web ở MVP

Wizard sinh sẵn Gradle, version catalog, project Xcode `iosApp/` và một màn mẫu. **Đừng tự gõ Gradle từ đầu** — dùng file wizard sinh rồi đọc hiểu nó (bài 3).

## Chạy trên 3 nền tảng

| Nền tảng | Cách chạy |
|---|---|
| Android | chọn cấu hình `composeApp` trong IDE → chạy trên emulator |
| iOS | chọn cấu hình `iosApp` → chọn simulator → chạy (lần đầu build lâu vài phút) |
| Desktop | chạy task `run` của `composeApp` (ví dụ `./gradlew :composeApp:run`) |

Tên task và cấu hình có thể khác chút tuỳ phiên bản wizard; xem tab Gradle trong IDE.

## Làm trên app MVP

Mở `App.kt` trong `commonMain`, đổi chữ trên nút mẫu thành **"Habit Tick"**, chạy lại cả 3 nền tảng. Một chỗ sửa, ba nơi đổi —
đó là cảm giác của KMP.

## Kiểm tra

- [ ] Chụp được màn hình app trên Android, iOS, Desktop.
- [ ] Sửa `App.kt` một lần, cả 3 nền tảng đổi theo.

## Lỗi hay gặp

- Build iOS báo không tìm thấy Xcode → chạy `sudo xcode-select -s /Applications/Xcode.app` (bạn tự gõ, cần mật khẩu máy).
- Lần đầu Gradle tải rất lâu → bình thường; đừng huỷ giữa chừng.
- Xem thêm [lỗi hay gặp](../reference/troubleshooting.md).

## Tiếp theo?

- [Bài 3: cấu trúc project](03-project-structure.md) — đọc hiểu những gì wizard vừa sinh.
- Đọc thêm: [Quickstart](https://kotlinlang.org/docs/multiplatform/quickstart.html) · [Recommended IDEs](https://kotlinlang.org/docs/multiplatform/recommended-ides.html) · [Build and run](https://kotlinlang.org/docs/multiplatform/build-and-run-kmp.html).

## Docs gốc

[Get started](https://kotlinlang.org/docs/multiplatform/get-started.html) · [Create app with shared UI](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-new-project.html)
