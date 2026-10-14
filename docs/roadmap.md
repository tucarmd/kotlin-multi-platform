# Lộ trình: học Kotlin Multiplatform bằng một app MVP

Lộ trình này đi theo đúng mục lục [docs Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform/get-started.html)
(Get started → Create KMP apps → Compose Multiplatform → Project structure → Share code → iOS integration → Test → Publish),
nhưng sắp lại thành 14 bài **làm tay**. Mỗi bài thêm một mảnh vào cùng một app: [Habit Tick](mvp/index.md).

## Bạn sẽ có gì ở cuối

- Một app chạy thật trên **Android, iOS và Desktop** từ một codebase Kotlin.
- Hiểu vì sao code nằm ở `commonMain`, `androidMain` hay `iosMain`.
- Biết tự thêm thư viện, lưu dữ liệu offline, gọi API, viết test và build bản phát hành.

## Bốn chặng

| Chặng | Bài | Kết quả trên app |
|---|---|---|
| **A. Nền** | [1](lessons/01-what-is-kmp.md) · [2](lessons/02-setup.md) · [3](lessons/03-project-structure.md) · [4](lessons/04-share-code.md) · [5](lessons/05-dependencies.md) | Project chạy 3 nền tảng, hiện tên máy qua expect/actual |
| **B. Giao diện** | [6](lessons/06-compose-basics.md) · [7](lessons/07-resources.md) · [8](lessons/08-viewmodel-navigation.md) | Màn danh sách + chi tiết thói quen, có tiếng Việt / Anh |
| **C. Dữ liệu** | [9](lessons/09-sqldelight.md) · [10](lessons/10-ktor.md) | Lưu offline bằng SQLDelight, lấy ngày lễ qua API bằng Ktor |
| **D. Ra mắt** | [11](lessons/11-ios-integration.md) · [12](lessons/12-testing.md) · [13](lessons/13-build-publish.md) · [14](lessons/14-migrate-android.md) | Nối SwiftUI, có test, build được AAB / iOS / DMG, CI GitHub Actions |

## Nhịp học gợi ý

- **1 bài / 1–2 buổi tối.** Đọc phần khái niệm 15 phút, còn lại để code.
- Xong chặng nào, đánh dấu ở [checklist](mvp/checklist.md).
- Kẹt quá 30 phút: xem [lỗi hay gặp](reference/troubleshooting.md), rồi mới đọc docs gốc.

## Cần gì trước

- Máy **Mac** (bắt buộc để build iOS). Không có Mac vẫn học được Android + Desktop.
- Biết Kotlin cơ bản: `val/var`, data class, lambda, coroutine ở mức `suspend` + `launch`.
- Không cần biết Swift — bài 11 chỉ dùng vài dòng SwiftUI.

## Tiếp theo?

- Đọc qua [app MVP sẽ làm](mvp/index.md) để biết đích đến.
- Bắt đầu [bài 1: KMP là gì, khi nào dùng](lessons/01-what-is-kmp.md).
