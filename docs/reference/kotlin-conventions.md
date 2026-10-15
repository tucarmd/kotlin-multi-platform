# Quy ước code Kotlin

Gom các luật viết Kotlin từ 3 nguồn chính chủ. Trang này nói **cách viết từng dòng code**; còn cây thư mục, 7 bước và cách comment chung cho các app luyện tập thì xem [Khung chung & quy ước comment](../apps/conventions.md).

| Nguồn | Dùng cho |
|---|---|
| [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) (JetBrains) | Luật gốc, áp dụng cho mọi code Kotlin: `shared/`, server, script |
| [Android Kotlin style guide](https://developer.android.com/kotlin/style-guide) (Google) | Code Android; chặt hơn bản gốc ở vài chỗ (bảng bên dưới) |
| [Compose API guidelines](https://developer.android.com/develop/ui/compose/api-guidelines) | Cách đặt tên và tham số cho hàm `@Composable` |

Repo này theo **luật gốc của Kotlin**. Code trong `androidApp/` theo thêm các luật chặt hơn của Android nếu không mâu thuẫn.

## 1. Bật style chính chủ (làm một lần)

- `gradle.properties` có dòng `kotlin.code.style=official`. DevicePulse đã có sẵn dòng này.
- Android Studio: **Settings → Editor → Code Style → Kotlin → Set from… → Kotlin style guide**.
- Bật kiểm tra format: **Settings → Editor → Inspections → General → Incorrect formatting**.
- Format file đang mở: `⌥⌘L`. Dọn import thừa: `⌃⌥O`. Có thể bật **Settings → Tools → Actions on Save → Reformat code + Optimize imports** để IDE tự làm mỗi lần lưu.

## 2. Đặt tên

| Thứ | Quy ước | Ví dụ |
|---|---|---|
| Package | chữ thường, không gạch dưới | `dev.devicepulse.sensor` |
| Class, interface, object | `UpperCamelCase` | `PulseMonitor` |
| Hàm, biến, thuộc tính | `lowerCamelCase` | `latencyGrade()`, `levelPercent` |
| Hằng (`const val`, `val` bất biến hẳn ở top-level hoặc trong `object`) | `SCREAMING_SNAKE_CASE` | `const val MAX_SAMPLES = 50` |
| Backing property | `_` + tên của property public | `private val _state` / `val state` |
| Enum | `UpperCamelCase` (repo này) hoặc `SCREAMING_SNAKE_CASE` | `BatteryLevel.Critical` |
| `@Composable` trả `Unit` | `UpperCamelCase`, là danh từ | `BatteryCard()` |
| `@Composable` trả giá trị | `lowerCamelCase` | `rememberPulseState()` |
| Viết tắt | 2 chữ viết hoa hết; từ 3 chữ trở lên chỉ hoa chữ đầu | `IOStream`, `HttpClient`, `GpsFix` |
| Tên test | luật gốc cho phép backtick có dấu cách; Android bắt dùng `_` | `` `low battery is critical`() `` (shared) · `batteryLevel_at10_isCritical` (androidApp) |

**Động từ cho biết hàm có sửa dữ liệu gốc hay không:** `sort()` sửa ngay trên list đó, còn `sorted()` trả về một bản copy đã sắp xếp. Tên hàm của mình cũng nên theo kiểu này.

## 3. File và thư mục

- Thư mục khớp với package nhưng bỏ phần gốc chung: package `dev.devicepulse.sensor` nằm ở `kotlin/dev/devicepulse/sensor/`.
- File chỉ có một class thì đặt tên theo class đó (`PulseMonitor.kt`). File có nhiều khai báo thì đặt tên theo nội dung (`Rules.kt`), tránh `Utils.kt`.
- **KMP:** file theo nền tảng thêm hậu tố của source set: `Platform.kt` (common), `Platform.android.kt`, `Platform.ios.kt`. Nếu không có hậu tố, build có thể báo lỗi *Duplicate JVM classes*.
- Thứ tự trong class: thuộc tính và `init` → constructor phụ → hàm → `companion object`. Đừng sắp hàm theo bảng chữ cái.
- Mỗi file chỉ nên dài vài trăm dòng.

## 4. Format

- Thụt lề 4 dấu cách, không dùng tab. Không dùng dấu `;`.
- Dấu `{` nằm cuối dòng; `else`, `catch` và `finally` nằm cùng dòng với `}` phía trước.
- Độ dài dòng: Android giới hạn **100 ký tự**. Luật gốc không ghi con số; IDE mặc định là 120.
- Nên có **trailing comma** (dấu phẩy sau phần tử cuối) khi khai báo tham số, enum hoặc nhánh `when`, để diff gọn hơn:

    ```kotlin
    data class BatteryInfo(
        val levelPercent: Int,
        val isCharging: Boolean,
    )
    ```

- Gọi hàm theo chuỗi: xuống dòng **trước** dấu `.` hoặc `?.`, thụt vào 4 dấu cách.
- Hàm chỉ có một biểu thức thì viết dạng `=`: `fun isLow() = level <= 20`.
- Annotation đặt trên một dòng riêng, phía trên khai báo.
- Thứ tự modifier: `private` → `expect/actual` → `final/open/abstract/sealed/const` → `override lateinit suspend …` → `companion inline value data`.

## 5. Viết cho "chất Kotlin" (idiom)

| Nên | Tránh | Vì |
|---|---|---|
| `val` | `var` khi không gán lại | Ít trạng thái thay đổi thì ít bug |
| `List`, `Map` (chỉ đọc) và `listOf()` | `ArrayList` và `arrayListOf()` khi không cần sửa list | Kiểu dữ liệu nói rõ ý định |
| Tham số mặc định `fun f(a: Int = 0)` | Viết nhiều overload | Ít code hơn |
| `if` cho 2 nhánh, `when` cho 3 nhánh trở lên | Chuỗi `else if` dài | Dễ đọc như một bảng luật |
| `return when (x) { … }` | Gán biến rồi mới `return` | Dạng biểu thức gọn hơn |
| `map`, `filter`, `first { }` | Vòng `for` tự gom kết quả | Ngắn gọn, khó sai |
| `for (x in list)` | `list.forEach { }` khi chỉ để lặp | Luật gốc ưu tiên `for` |
| `"$name có ${items.size}"` | Nối chuỗi bằng `+` | String template |
| Gọi tên đối số khi truyền `Boolean` hoặc nhiều số cùng kiểu: `show(animate = true)` | `show(true)` | Người đọc biết `true` là gì |
| `value == true` với `Boolean?` | `value!!` | Tránh crash |
| `it` trong lambda ngắn, không lồng nhau | `it` trong lambda lồng nhau | Lồng nhau thì phải đặt tên tham số |
| Property khi tính nhanh, không ném lỗi và trả kết quả ổn định | Hàm `getX()` | Đúng ý nghĩa của property |
| Extension function có `private` hoặc `internal` | Extension public tràn lan | Không làm rối API |

Hạn chế `!!`. Thay bằng `?.`, `?:`, `requireNotNull(x) { "lý do" }` hoặc `checkNotNull`.

## 6. Comment và KDoc

- KDoc dùng `/** … */` và mô tả tham số ngay trong câu văn bằng cách link `[tên]`; tránh liệt kê `@param` và `@return` khi không cần.
- Android yêu cầu KDoc cho mọi type public và mọi member public/protected; override thì được miễn.
- Comment trong thân hàm theo 3 nhãn cố định của repo: xem [Quy ước comment](../apps/conventions.md#4-quy-uoc-comment).

## 7. Compose

```kotlin
@Composable
fun BatteryCard(
    info: BatteryInfo,                  // 1. tham số bắt buộc (state đã hoist lên trên)
    onRefresh: () -> Unit,              //    sự kiện đặt tên onXxx
    modifier: Modifier = Modifier,      // 2. modifier là tham số tùy chọn ĐẦU TIÊN
    showDetails: Boolean = false,       // 3. các tham số tùy chọn khác
    content: @Composable () -> Unit = {}, // 4. lambda nội dung đặt cuối
) { /* … */ }
```

- **State hoisting:** composable không tự giữ state mà nhận state qua tham số, rồi báo thay đổi lên trên bằng callback. Ô nhập liệu thì dùng cặp `value` / `onValueChange`.
- Hàm tạo và nhớ đối tượng trong composition có tiền tố `remember…`.
- Giá trị mặc định của tham số phải rẻ và không có side effect, vì có thể bị tính lại khi recompose.
- Nên truyền tham số bất biến (`val`, `data class`, `List`) để Compose bỏ qua được những lần recompose không cần thiết.

## 8. KMP

- `commonMain` không được import `android.*` hay `platform.*`. Chỗ nào cần API nền tảng thì dùng `expect`/`actual` hoặc dùng interface rồi inject implementation.
- Hậu tố file theo source set (mục 3). `actual` phải cùng package và cùng tên với `expect`.
- Module chia sẻ cho nhiều team nên bật `kotlin { explicitApi() }`. Khi đó compiler bắt mọi khai báo public phải ghi rõ visibility và kiểu trả về.

## 9. Máy kiểm tra hộ

| Công cụ | Bắt lỗi gì | Ghi chú |
|---|---|---|
| IDE (Incorrect formatting) | Format | Bật như ở mục 1 |
| **ktlint** | Format và style theo luật gốc | Tự sửa được: `ktlint -F` |
| **detekt** | Code smell (hàm quá dài, quá nhiều nhánh, số "magic") | Bản 2.0 hỗ trợ Kotlin 2.4 nhưng tới 06/2026 vẫn là alpha ([changelog](https://detekt.dev/changelog-2.0.0/)) |
| **Android Lint** | Lỗi riêng của Android: quyền, resource, API level | `./gradlew :androidApp:lint` |

Người mới chỉ cần bật format của IDE cùng Android Lint là đủ. Thêm ktlint và detekt khi project có CI ([bài 13](../lessons/13-testing.md)).

## Nguồn

- [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin style guide](https://developer.android.com/kotlin/style-guide)
- [Compose API guidelines](https://developer.android.com/develop/ui/compose/api-guidelines)
- [detekt 2.0 changelog](https://detekt.dev/changelog-2.0.0/)
