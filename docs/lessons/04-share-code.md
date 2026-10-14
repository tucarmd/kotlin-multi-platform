# Chia sẻ code giữa các nền tảng

Bài lõi của KMP: đặt tối đa code vào `commonMain`, và khi buộc phải gọi API riêng của máy thì nối bằng `expect`/`actual` hoặc interface.
Đây chính là nội dung trang [Share code between platforms](https://kotlinlang.org/docs/multiplatform/multiplatform-share-on-platforms.html) bạn đang đọc.

## Mục tiêu

- Biết khi nào code chung được cho **mọi** nền tảng, khi nào chỉ cho **một nhóm** (vd mọi iOS).
- Viết được một `expect`/`actual` và một interface nền tảng.

## Ba mức chia sẻ

1. **Mọi nền tảng** → `commonMain`. Logic thuần Kotlin: tính streak, định dạng dữ liệu.
2. **Một nhóm nền tảng giống nhau** → source set trung gian (`iosMain`, `appleMain`). Code dùng API Apple, viết một lần cho iPhone thật lẫn simulator.
3. **Một nền tảng** → `androidMain`, `desktopMain`…

Trong `iosMain` bạn gọi thẳng API Apple qua **platform libraries** đã kèm sẵn: `platform.Foundation`, `platform.UIKit`, POSIX.

## Cách 1: expect / actual

Khai báo "lời hứa" trong `commonMain`, mỗi nền tảng "giữ lời" bằng `actual`:

=== "commonMain"

    ```kotlin
    expect fun platformName(): String
    ```

=== "androidMain"

    ```kotlin
    actual fun platformName(): String = "Android ${android.os.Build.VERSION.SDK_INT}"
    ```

=== "iosMain"

    ```kotlin
    import platform.UIKit.UIDevice

    actual fun platformName(): String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    ```

=== "desktopMain"

    ```kotlin
    actual fun platformName(): String = "Desktop " + System.getProperty("os.name")
    ```

Thiếu một `actual` là build báo lỗi ngay — không có chuyện quên một nền tảng.

## Cách 2: interface + truyền vào (ưu tiên khi logic phức tạp)

```kotlin
// commonMain
interface Clock { fun today(): String }

class StreakCalculator(private val clock: Clock) { /* dùng clock.today() */ }
```

Mỗi nền tảng tạo bản cài đặt riêng và truyền vào lúc khởi động app. Ưu điểm: test dễ (truyền `FakeClock`), không cần `expect class`.
Quy tắc thực hành: **`expect fun` cho hàm nhỏ; interface cho thứ có trạng thái hoặc cần giả lập khi test.**

## Làm trên app MVP

1. Thêm `platformName()` như trên, hiện dòng "Đang chạy trên …" ở màn chào.
2. Trong `commonMain`, viết hàm tính streak:

```kotlin
fun streak(doneDates: Set<String>, today: String, previousDay: (String) -> String): Int {
    var day = today
    var count = 0
    while (day in doneDates) { count++; day = previousDay(day) }
    return count
}
```

(Bài 5 thay `previousDay` bằng `kotlinx-datetime`.)

## Kiểm tra

- [ ] Ba nền tảng hiện ba dòng "Đang chạy trên …" khác nhau.
- [ ] Xoá `actual` của desktop → build đỏ; trả lại → xanh.

## Lỗi hay gặp

- Đặt `expect`/`actual` khác package → không khớp. Hai bên phải **cùng package, cùng tên**.
- Lạm dụng `expect class` cho mọi thứ → khó test. Dùng interface.

## Tiếp theo?

- [Bài 5: thêm thư viện](05-dependencies.md).
- Đọc thêm: [Expected and actual declarations](https://kotlinlang.org/docs/multiplatform/multiplatform-expect-actual.html) · [Use platform-specific APIs](https://kotlinlang.org/docs/multiplatform/multiplatform-connect-to-apis.html) · [Hierarchical project structure](https://kotlinlang.org/docs/multiplatform/multiplatform-hierarchy.html).

## Docs gốc

[Share code between platforms](https://kotlinlang.org/docs/multiplatform/multiplatform-share-on-platforms.html)
