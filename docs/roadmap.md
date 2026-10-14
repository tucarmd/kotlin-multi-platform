# Lộ trình: học Kotlin Multiplatform qua app Device Pulse

Lộ trình này đi theo mục lục [docs Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform/get-started.html)
(Get started → Create KMP apps → Compose Multiplatform → Project structure → Share code → iOS integration → Test → Publish),
nhưng sắp lại thành 14 bài **làm tay**. Mỗi bài thêm một mảnh vào cùng một app: [Device Pulse](project/index.md), app đo pin, GPS và mạng.

## Bạn sẽ có gì ở cuối

- Một app chạy thật trên **Android và iOS** từ một codebase Kotlin, UI viết chung bằng Compose Multiplatform.
- Hiểu vì sao code nằm ở `commonMain`, `androidMain` hay `iosMain`, và cách gọi API riêng của từng máy (pin, GPS, mạng).
- Biết xin quyền, gọi mạng, lưu dữ liệu offline, viết test và build bản phát hành.

## Bốn chặng

| Chặng | Bài | Kết quả trên app |
|---|---|---|
| **A. Nền** | [1](lessons/01-what-is-kmp.md) · [2](lessons/02-setup.md) · [3](lessons/03-project-structure.md) · [4](lessons/04-share-code.md) · [5](lessons/05-dependencies.md) | Project chạy 2 nền tảng; có model, luật đánh giá, Koin, log |
| **B. Giao diện** | [6](lessons/06-compose-basics.md) · [7](lessons/07-resources.md) · [8](lessons/08-viewmodel-navigation.md) | Dashboard 3 thẻ + màn chi tiết, dữ liệu giả, tiếng Việt / Anh |
| **C. Cảm biến thật** | [9](lessons/09-battery.md) · [10](lessons/10-location.md) · [11](lessons/11-network.md) · [12](lessons/12-history-sqldelight.md) | Pin, GPS, mạng + độ trễ đọc thật; lưu lịch sử offline |
| **D. Ra mắt** | [13](lessons/13-testing.md) · [14](lessons/14-build-publish.md) | Có test, build AAB / iOS, crash report, CI GitHub Actions |

## Nhịp học gợi ý

- **1 bài / 1–2 buổi tối.** Đọc phần khái niệm 15 phút, còn lại để code.
- Chặng C nên thử trên **máy thật**: pin trên iOS Simulator không đo được.
- Xong chặng nào, đánh dấu ở [checklist](project/checklist.md).
- Kẹt quá 30 phút: xem [lỗi hay gặp](reference/troubleshooting.md), rồi mới đọc docs gốc.

## Cần gì trước

- Máy **Mac** (bắt buộc để build iOS). Không có Mac vẫn học được phần Android.
- Biết Kotlin cơ bản: `val/var`, data class, lambda, coroutine ở mức `suspend` + `launch`. Chưa biết thì đi
  [Kotlin tour](https://kotlinlang.org/docs/kotlin-tour-welcome.html) trước (xem thêm [tài nguyên học](reference/learning-resources.md)).
- Không cần biết Swift: phần iOS gọi API của Apple ngay từ Kotlin.

## Tiếp theo?

- Đọc qua [dự án Device Pulse](project/index.md) để biết đích đến.
- Bắt đầu [bài 1: KMP là gì, khi nào dùng](lessons/01-what-is-kmp.md).
- Muốn biết KMP được và mất gì trước khi học: [Đánh giá & kế hoạch](plan/kmp-review.md).
