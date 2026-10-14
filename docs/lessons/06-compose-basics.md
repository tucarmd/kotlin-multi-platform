# Compose Multiplatform cơ bản

Compose Multiplatform là Jetpack Compose của Android, được JetBrains đưa lên iOS, Desktop và Web.
Biết Jetpack Compose rồi thì gần như biết luôn — khác chủ yếu ở **điểm khởi động** của từng nền tảng.

## Mục tiêu

- Biết mỗi nền tảng khởi động hàm `App()` chung thế nào.
- Dựng màn danh sách thói quen bằng layout + modifier.

## Một App(), ba điểm khởi động

Toàn bộ UI nằm trong `@Composable fun App()` ở `commonMain`. Mỗi nền tảng chỉ có vài dòng gọi nó:

=== "Android · androidMain"

    ```kotlin
    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent { App() }
        }
    }
    ```

=== "iOS · iosMain"

    ```kotlin
    fun MainViewController() = ComposeUIViewController { App() }
    ```

    Phía Swift gọi `MainViewController()` và bọc vào SwiftUI (bài 11).

=== "Desktop · desktopMain"

    ```kotlin
    fun main() = application {
        Window(onCloseRequest = ::exitApplication, title = "Habit Tick") { App() }
    }
    ```

## Layout và modifier — đủ dùng cho MVP

- `Column` / `Row` / `Box` xếp dọc / ngang / chồng.
- `LazyColumn` cho danh sách dài (chỉ vẽ dòng đang thấy).
- `Modifier` đổi cỡ, khoảng cách, bấm: **thứ tự modifier có ý nghĩa** — `padding` trước `background` khác `background` trước `padding`.
- Màn rộng (desktop, iPad) → xem *adaptive layouts* để đổi bố cục theo cỡ cửa sổ.

## Làm trên app MVP

```kotlin
@Composable
fun HabitListScreen(habits: List<Habit>, today: String, onToggle: (Habit) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(habits, key = { it.id }) { habit ->
            HabitRow(habit, done = today in habit.doneDates, onToggle = { onToggle(habit) })
        }
    }
}

@Composable
fun HabitRow(habit: Habit, done: Boolean, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(habit.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Checkbox(checked = done, onCheckedChange = { onToggle() })
        }
    }
}
```

Tạm dùng dữ liệu giả: `remember { mutableStateListOf(...) }` trong `App()`. Bài 8 chuyển state sang ViewModel.

!!! tip "Xem nhanh không cần chạy app"
    Thêm `@Preview` cho `HabitRow` để xem trong IDE. Với desktop, **Compose Hot Reload** cho thấy thay đổi UI ngay khi lưu file.

## Kiểm tra

- [ ] Danh sách 3 thói quen giả hiện trên cả 3 nền tảng.
- [ ] Bấm dòng → checkbox đổi.

## Tiếp theo?

- [Bài 7: resources và đa ngôn ngữ](07-resources.md).
- Đọc thêm: [Platform differences](https://kotlinlang.org/docs/multiplatform/compose-platform-specifics.html) · [Adaptive layouts](https://kotlinlang.org/docs/multiplatform/compose-adaptive-layouts.html) · [Compose Hot Reload](https://kotlinlang.org/docs/multiplatform/compose-hot-reload.html) · [Compose Multiplatform và Jetpack Compose](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-and-jetpack-compose.html).

## Docs gốc

[App entry points](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-entry-points.html) · [Layout basics](https://kotlinlang.org/docs/multiplatform/compose-layout.html) · [Modifiers](https://kotlinlang.org/docs/multiplatform/compose-layout-modifiers.html) · [UI previews](https://kotlinlang.org/docs/multiplatform/compose-previews.html)
