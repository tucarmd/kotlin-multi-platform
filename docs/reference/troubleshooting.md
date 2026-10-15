# Lỗi hay gặp

## Gradle

| Triệu chứng | Sửa |
|---|---|
| Lần đầu sync rất lâu | Bình thường (tải Kotlin/Native, thư viện). Đừng huỷ |
| `Gradle JVM version incompatible` (… requires a Gradle JVM that supports version 17) | Gradle đang chạy bằng JDK quá cũ (11) hoặc quá mới (JBR 25 của Studio; Gradle 8.14 chỉ hỗ trợ tới Java 24). Thêm file `gradle/gradle-daemon-jvm.properties` có dòng `toolchainVersion=21` rồi Sync. Gradle sẽ tự tìm JDK 21 trên máy, không còn phụ thuộc setting Gradle JDK trong Studio |
| `Unresolved reference` cho thư viện vừa thêm | Thêm sai source set, hoặc chưa sync Gradle |
| Lớp `Res` thiếu chuỗi mới | Build lại để sinh code resource |
| Phiên bản Kotlin và Compose không hợp | Xem bảng *Compatibility and versions* của Compose Multiplatform; dùng bộ phiên bản wizard sinh |

## iOS / Xcode

| Triệu chứng | Sửa |
|---|---|
| Không tìm thấy Xcode | `sudo xcode-select -s /Applications/Xcode.app` (bạn tự chạy) |
| `No such module 'ComposeApp'` | Build từ Xcode một lần; kiểm `baseName` của framework |
| Sửa Kotlin mà iOS không đổi | Clean build folder trong Xcode |
| Build iOS chậm | Bình thường khi đổi nhiều; chỉ build target simulator khi đang phát triển |

## Runtime

| Triệu chứng | Sửa |
|---|---|
| Android không gọi được mạng | Thiếu quyền `INTERNET` |
| `NoTransformationFoundException` (Ktor) | Thiếu `ContentNegotiation` hoặc plugin serialization |
| Danh sách không cập nhật sau khi ghi DB | Flow đang quan sát sai bảng — xem cảnh báo ở bài 9 |
| Crash iOS vì gọi UI ngoài main thread | Cập nhật state UI trên `Dispatchers.Main` |

Code compile được mà chạy sai (tràn số, `!!`, coroutine không dừng…): xem [Phụ lục: Kotlin khó gì, bẫy gì](kotlin-pitfalls.md).

Còn kẹt: tìm trong [docs chính chủ](https://kotlinlang.org/docs/multiplatform/get-started.html) và kênh `#multiplatform` trên Kotlin Slack.
