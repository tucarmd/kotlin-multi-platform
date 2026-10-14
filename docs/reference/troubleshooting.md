# Lỗi hay gặp

## Gradle

| Triệu chứng | Sửa |
|---|---|
| Lần đầu sync rất lâu | Bình thường (tải Kotlin/Native, thư viện). Đừng huỷ |
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

Còn kẹt: tìm trong [docs chính chủ](https://kotlinlang.org/docs/multiplatform/get-started.html) và kênh `#multiplatform` trên Kotlin Slack.
