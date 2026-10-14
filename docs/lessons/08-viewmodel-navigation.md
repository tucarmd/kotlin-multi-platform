# ViewModel và điều hướng

Đưa state ra khỏi màn hình vào **ViewModel** (dùng chung mọi nền tảng), rồi thêm màn thứ hai và điều hướng giữa hai màn.

## Mục tiêu

- `HabitListViewModel` giữ state bằng `StateFlow`; màn hình chỉ vẽ.
- Bấm một thói quen mở màn chi tiết, back quay lại.

## ViewModel multiplatform

Thư viện `lifecycle-viewmodel` của AndroidX có bản multiplatform (do JetBrains phát hành cho Compose Multiplatform),
nên `ViewModel` + `viewModelScope` dùng được ngay trong `commonMain`.

```kotlin
data class HabitListState(
    val today: String = "",
    val habits: List<Habit> = emptyList(),
)

class HabitListViewModel(private val repo: HabitRepository) : ViewModel() {
    val state: StateFlow<HabitListState> = repo.observeHabits()
        .map { HabitListState(today = todayIso(), habits = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitListState())

    fun toggle(habit: Habit) = viewModelScope.launch { repo.toggleToday(habit.id) }
}
```

Trong màn hình:

```kotlin
@Composable
fun HabitListRoute(vm: HabitListViewModel = viewModel { HabitListViewModel(repo) }, onOpen: (Long) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    HabitListScreen(state.habits, state.today, onToggle = vm::toggle)
}
```

Tới bài 9 mới có `HabitRepository` thật — tạm thời viết một bản `InMemoryHabitRepository` giữ list trong `MutableStateFlow`.

## Điều hướng type-safe

Navigation Compose cũng có bản multiplatform. Mỗi màn là một route `@Serializable`:

```kotlin
@Serializable object HabitList
@Serializable data class HabitDetail(val id: Long)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = HabitList) {
        composable<HabitList> { HabitListRoute(onOpen = { nav.navigate(HabitDetail(it)) }) }
        composable<HabitDetail> { entry ->
            HabitDetailRoute(id = entry.toRoute<HabitDetail>().id, onBack = { nav.popBackStack() })
        }
    }
}
```

Cần plugin `kotlinx-serialization` cho các route `@Serializable`.

## Lifecycle

Compose Multiplatform ánh xạ vòng đời từng nền tảng (Activity Android, UIViewController iOS, cửa sổ Desktop) về chung một mô hình
`Lifecycle`. `collectAsStateWithLifecycle` dựa vào đó để ngừng thu dữ liệu khi app chạy nền.

## Kiểm tra

- [ ] Xoay màn hình Android → danh sách và trạng thái tick không mất.
- [ ] Mở chi tiết rồi back trên cả 3 nền tảng (iOS: vuốt cạnh trái; Desktop: nút back trong app).

## Tiếp theo?

- [Bài 9: lưu dữ liệu với SQLDelight](09-sqldelight.md).
- Đọc thêm: [Navigation 3](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html) · [Deep links](https://kotlinlang.org/docs/multiplatform/compose-navigation-deep-links.html).

## Docs gốc

[Multiplatform ViewModel](https://kotlinlang.org/docs/multiplatform/compose-viewmodel.html) · [Lifecycle](https://kotlinlang.org/docs/multiplatform/compose-lifecycle.html) · [Navigation in Compose](https://kotlinlang.org/docs/multiplatform/compose-navigation.html) · [Navigation and routing](https://kotlinlang.org/docs/multiplatform/compose-navigation-routing.html)
