# Kotlin Variants (Claude): plugin Android Studio

Bấm icon **3V** cạnh một `fun` Kotlin thì Claude Code viết 3 cách cài đặt khác cho function đó, rồi mở kết quả trong split bên phải, nằm cạnh code gốc. Plugin này dùng để học, không dùng để sửa code project.

```
 code gốc (Rules.kt)                 │ learn-variants/Rules.latencyGrade.variants.kt
 ─────────────────────────────────── │ ─────────────────────────────────────────────
 [3V] fun latencyGrade(millis: Long) │ // ===== Variant 1: Cơ bản =====
        = when { ... }               │ // Ý tưởng / Học được / Ưu / Nhược / Dùng khi
                                     │ fun latencyGradeV1(...)
                                     │ // ===== Variant 2: Idiomatic Kotlin =====
                                     │ // ===== Variant 3: Nâng cao =====
                                     │ fun main() { ... }   // chạy thử cả 3
                                     │ // TÔI CHỌN:
                                     │ // VÌ:
```

## Cài

1. Build: `./gradlew buildPlugin`. Kết quả nằm ở `build/distributions/studio-variants-plugin-0.1.0.zip`.
2. Trong Android Studio: **Settings → Plugins → ⚙ → Install Plugin from Disk…**, chọn file zip ở bước 1 rồi restart.
3. Máy phải có `claude` (Claude Code CLI) và đã đăng nhập. Plugin tự tìm `claude` trong PATH của login shell. Nếu không tìm thấy, plugin sẽ hỏi đường dẫn (lấy bằng lệnh `which claude`) và nhớ cho các lần sau.

## Dùng

| Cách gọi | |
|---|---|
| Bấm icon **3V** ở gutter, cạnh tên function | |
| `Ctrl + ⌥ + ⇧ + V` khi con trỏ nằm trong function | Đổi phím ở Settings → Keymap → "Show 3 Variants" |
| Chuột phải → **Show 3 Variants (Claude)** | |

- Claude chạy ngầm khoảng 1 phút; thanh tiến trình nằm ở góc dưới và bấm hủy được.
- Kết quả lưu ở `<project>/learn-variants/<File>.<fun>.variants.kt`. Thư mục này nằm ngoài source set nên không ảnh hưởng tới build.
- Bấm lại trên function đã có variant thì chọn **Open** để mở bản cũ, hoặc **Ask Again** để Claude viết lại.
- Thấy icon rối mắt thì tắt ở Settings → Editor → General → Gutter Icons → "Kotlin variants (Claude)".

## Cấu trúc

| File | Việc |
|---|---|
| `VariantsLineMarkerProvider.kt` | Gắn icon 3V lên tên mỗi `fun` |
| `ShowVariantsAction.kt` | Phím tắt và menu chuột phải |
| `VariantsPrompt.kt` | Prompt gửi cho Claude (format 3 variant, phần giải thích tiếng Việt) |
| `ClaudeCli.kt` | Gọi `claude -p`, truyền prompt qua stdin, hết giờ sau 5 phút |
| `VariantsGenerator.kt` | Chạy nền, ghi file, mở split |

Plugin build dựa trên Android Studio đã cài trong máy (`studioPath` trong `gradle.properties`), nên không phải tải IDE về. Cần Gradle 9+ và JDK 21; nếu máy chưa có JDK 21 thì foojay tự tải.

Chạy thử trong một Android Studio sandbox: `./gradlew runIde`. Kiểm tra tương thích: `./gradlew verifyPlugin`.
