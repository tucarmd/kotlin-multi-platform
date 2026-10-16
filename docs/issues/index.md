# 900 lỗi Kotlin hay gặp

"900 bài hát thiếu nhi" của người viết Kotlin: **900 lỗi, bẫy và câu hỏi** mà người mới (và cả người lâu năm) gặp đi gặp lại,
mỗi cái kèm **vì sao** và **cách sửa**. Chia 18 nhóm, mỗi nhóm 50 mục, đánh mã từ **K001** tới **K900**.

!!! tip "Dùng thế nào"
    - **Đang kẹt một lỗi:** copy câu báo lỗi (ví dụ `Unresolved reference`, `Smart cast to 'String' is impossible`) vào ô
      **tìm kiếm** của site. Mục nào có đúng câu đó sẽ hiện lên.
    - **Học dần:** mỗi ngày đọc 10 mục của một nhóm, bấm **▶ Chạy thử** ở mục có link để thấy lỗi xảy ra thật, rồi sửa code trong
      Playground cho hết lỗi.
    - Mục nào đã gặp thật trong 5 app luyện tập thì có dòng **Gặp ở:** trỏ tới file trong repo.

## 18 nhóm

| # | Nhóm | Mã | Ví dụ |
|:-:|---|---|---|
| 1 | [Cú pháp & kiểu cơ bản](01-basics.md) | K001–K050 | Chia số nguyên, tràn `Int`, so sánh `Double`, `when` thiếu nhánh |
| 2 | [Null safety](02-null-safety.md) | K051–K100 | `!!`, smart cast không được, platform type từ Java, `lateinit` |
| 3 | [Collections & Sequence](03-collections.md) | K101–K150 | Sửa list khi đang duyệt, `List` chỉ đọc vẫn đổi, `first()` trên list rỗng |
| 4 | [Class, object, sealed, enum](04-classes.md) | K151–K200 | `data class` so sánh sai, `copy` nông, `sealed` không phủ hết nhánh |
| 5 | [Hàm, lambda, generics](05-functions.md) | K201–K250 | `return` trong lambda, 5 scope function, `reified`, variance `in`/`out` |
| 6 | [Coroutines](06-coroutines.md) | K251–K300 | `GlobalScope`, nuốt `CancellationException`, `runBlocking` trên main |
| 7 | [Flow, StateFlow, SharedFlow](07-flow.md) | K301–K350 | `StateFlow` không phát giá trị trùng, cold vs hot, `collect` chặn mãi |
| 8 | [Gradle & build](08-gradle.md) | K351–K400 | Sync lỗi vì JDK, version catalog, xung đột thư viện, cache hỏng |
| 9 | [KMP: source set, expect/actual](09-kmp.md) | K401–K450 | Code Android lọt vào `commonMain`, `actual` thiếu, target không build |
| 10 | [Swift ↔ Kotlin, iOS, Xcode](10-ios-interop.md) | K451–K500 | `Int` thành `Int32`, tên enum đổi, `reified` không gọi được, framework không thấy |
| 11 | [Android: lifecycle, quyền, Manifest](11-android.md) | K501–K550 | Leak `Context`, mất state khi xoay, quyền bị từ chối vĩnh viễn |
| 12 | [Compose: state & recomposition](12-compose-state.md) | K551–K600 | `remember` mất khi xoay, recompose vô hạn, state saveable sai chỗ |
| 13 | [Compose: layout, navigation, side effect](13-compose-ui.md) | K601–K650 | `LaunchedEffect` key sai, `LazyColumn` thiếu `key`, bàn phím che UI |
| 14 | [Lưu dữ liệu: SQLDelight, Room, DataStore](14-persistence.md) | K651–K700 | Quên migration, `-lsqlite3`, query trên main thread |
| 15 | [Mạng & JSON: Ktor, serialization](15-networking.md) | K701–K750 | Field lạ làm crash, lỗi trả HTTP 200, timeout, `CancellationException` |
| 16 | [Test](16-testing.md) | K751–K800 | Tên test có dấu phẩy trên iOS, `runTest` không chạy job nền, fake vs mock |
| 17 | [Hiệu năng & chạy ngầm](17-performance-background.md) | K801–K850 | Việc nặng trên main thread, Foreground Service crash, GPS hao pin |
| 18 | [Style, Java interop, tooling, phát hành](18-style-release.md) | K851–K900 | `@JvmStatic`, R8 xoá class, ký app, cảnh báo bị lờ đi |

## Một mục trông thế nào

Mọi mục viết theo cùng một khuôn, để lướt nhanh:

````markdown
### K052 · `!!` làm app crash với NullPointerException

`🟢 mới học` · `Kotlin`

**Gặp:** `java.lang.NullPointerException` ở dòng có `!!`, thường sau khi dữ liệu từ server hoặc Intent thiếu field.

**Vì sao:** `!!` nghĩa là "tôi chắc chắn không null". Sai một lần là crash, và compiler không còn bảo vệ bạn nữa.

**Sửa:** dùng `?.` + `?:` để có giá trị thay thế, hoặc `requireNotNull(x) { "lý do" }` để crash có thông báo rõ.

```kotlin
val name: String? = intent.getStringExtra("name")
val len = name!!.length          // ❌ crash nếu không có extra
val len = name?.length ?: 0      // ✅
```

[▶ Chạy thử](../playground/index.html#code=…) · Xem thêm: K061
````

- **Mức:** `🟢 mới học` · `🟡 đã làm app` · `🔴 khó, hiếm nhưng đau`.
- **Phạm vi:** `Kotlin` · `Android` · `iOS` · `KMP` · `Gradle` · `Compose`.
- **▶ Chạy thử** chỉ có ở mục chạy được bằng Kotlin thuần trên JVM. Code trong link đã được chạy thật trên compiler của JetBrains.
  Mục về Gradle, Xcode, Android, Compose không chạy được trên Playground nên chỉ có code và lệnh sửa.

## Trang liên quan

- [Phụ lục: Kotlin khó gì, bẫy gì](../reference/kotlin-pitfalls.md): 13 bẫy giải thích kỹ, đọc trước khi lướt 900 mục.
- [Lỗi hay gặp](../reference/troubleshooting.md): lỗi Gradle / Xcode / runtime khi chạy Device Pulse.
- [Quy ước code Kotlin](../reference/kotlin-conventions.md): viết đúng ngay từ đầu để khỏi gặp phần lớn các mục ở đây.
- Comment `TRAP:` trong 5 app luyện tập: `grep -rn "TRAP:" TipSplit PocketNotes RateWatch TripLog ObdLink`.
