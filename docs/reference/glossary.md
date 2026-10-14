# Từ điển

| Từ | Nghĩa |
|---|---|
| **KMP** | Kotlin Multiplatform — viết Kotlin một lần, chạy nhiều nền tảng |
| **CMP** | Compose Multiplatform — UI Compose dùng chung cho Android, iOS, Desktop, Web |
| **Target** | nền tảng đích build ra: `android`, `iosArm64`, `jvm`… |
| **Source set** | thư mục code dùng cho một nhóm target: `commonMain`, `iosMain`… |
| **commonMain** | code chung cho mọi target, chỉ được dùng Kotlin chung + thư viện multiplatform |
| **Default hierarchy template** | cây source set KMP tự dựng (`iosMain` → `appleMain` → `nativeMain` → `commonMain`) |
| **expect / actual** | khai báo ở common, mỗi nền tảng cài đặt riêng |
| **Platform libraries** | API của hệ điều hành gọi được từ Kotlin/Native, vd `platform.UIKit` |
| **Framework** | gói code Kotlin đã biên dịch cho iOS, Xcode import như thư viện Swift |
| **Kotlin/Native** | trình biên dịch Kotlin ra mã máy (iOS, macOS…) |
| **Version catalog** | file `libs.versions.toml` gom phiên bản thư viện |
| **Engine (Ktor)** | phần HTTP riêng mỗi nền tảng: OkHttp, Darwin, CIO |
| **Driver (SQLDelight)** | cách mở SQLite trên từng nền tảng |
| **StateFlow** | luồng dữ liệu luôn có giá trị hiện tại; UI quan sát để vẽ lại |
| **Composable** | hàm vẽ UI có `@Composable` |
| **Modifier** | chuỗi chỉnh cỡ, khoảng cách, nền, bấm cho một composable |
| **klibs.io** | danh mục thư viện Kotlin Multiplatform |
| **AAB** | file app Android nộp Google Play |
| **DMG** | gói cài app macOS |
