# Resources và đa ngôn ngữ

Chữ, ảnh, font của app đặt **một chỗ** trong `composeResources` và dùng chung cho mọi nền tảng. Plugin sinh sẵn lớp `Res` để gọi an toàn kiểu.

## Mục tiêu

- Đưa mọi chữ trên màn ra `strings.xml`.
- App tự đổi tiếng Việt / tiếng Anh theo ngôn ngữ máy.

## Thư mục

```text
composeApp/src/commonMain/composeResources/
├── values/strings.xml        # mặc định (tiếng Anh)
├── values-vi/strings.xml     # tiếng Việt
├── drawable/ic_check.xml     # ảnh vector / png
└── font/                     # font riêng nếu có
```

Sau khi thêm file, build lại (hoặc chạy task sinh resource) để lớp `Res` được cập nhật.

## Làm trên app MVP

`values/strings.xml`:

```xml
<resources>
    <string name="app_name">Habit Tick</string>
    <string name="today_label">Today: %1$s</string>
    <string name="streak_days">%1$d-day streak</string>
    <string name="add_habit">Add habit</string>
</resources>
```

`values-vi/strings.xml`:

```xml
<resources>
    <string name="app_name">Habit Tick</string>
    <string name="today_label">Hôm nay: %1$s</string>
    <string name="streak_days">Chuỗi %1$d ngày</string>
    <string name="add_habit">Thêm thói quen</string>
</resources>
```

Dùng trong UI:

```kotlin
import habittick.composeapp.generated.resources.Res          // tên package sinh ra tuỳ project
import habittick.composeapp.generated.resources.today_label
import org.jetbrains.compose.resources.stringResource

Text(stringResource(Res.string.today_label, today))
```

## Kiểm tra

- [ ] Không còn chuỗi chữ cứng nào trong file `.kt` của màn hình.
- [ ] Đổi ngôn ngữ máy sang tiếng Việt → app hiện tiếng Việt (thử trên ít nhất 2 nền tảng).

## Lỗi hay gặp

- `Res` không có chuỗi mới → chưa build lại để sinh code.
- Import sai package `Res` → để IDE tự gợi ý import, đừng gõ tay.

## Tiếp theo?

- [Bài 8: ViewModel và điều hướng](08-viewmodel-navigation.md).
- Đọc thêm: [Regional format](https://kotlinlang.org/docs/multiplatform/compose-regional-format.html) · [RTL languages](https://kotlinlang.org/docs/multiplatform/compose-rtl.html) · [Localization tests](https://kotlinlang.org/docs/multiplatform/compose-localization-tests.html).

## Docs gốc

[Resources overview](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources.html) · [Setup](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-setup.html) · [Usage](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html) · [Localizing strings](https://kotlinlang.org/docs/multiplatform/compose-localize-strings.html)
