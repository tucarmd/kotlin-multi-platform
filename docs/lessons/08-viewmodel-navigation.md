# ViewModel và điều hướng

Đưa state ra khỏi màn hình vào **ViewModel** (dùng chung Android và iOS), lấy dữ liệu từ các *sensor giả* qua Koin,
rồi thêm màn Chi tiết và màn Lịch sử với điều hướng type-safe.

## Mục tiêu

- `DashboardViewModel` gộp 3 luồng sensor thành một `StateFlow<DashboardState>`; màn hình chỉ vẽ.
- Sensor giả (`FakeBatterySensor`…) cắm vào qua Koin — bài 9–11 thay bằng bản thật mà không sửa ViewModel.
- Bấm thẻ mở `DetailRoute`, có nút mở `HistoryRoute`, back quay lại trên cả hai nền tảng.

## Thư viện

Thêm vào `commonMain` (qua version catalog như [bài 5](05-dependencies.md)):

| Thư viện | Dùng cho |
|---|---|
| `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` | `ViewModel`, `viewModelScope` trong `commonMain` |
| `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose` | `collectAsStateWithLifecycle` |
| `org.jetbrains.androidx.navigation:navigation-compose` | `NavHost`, route type-safe |
| `io.insert-koin:koin-compose-viewmodel` | `koinViewModel()`, `viewModelOf` |

Và plugin `org.jetbrains.kotlin.plugin.serialization` trong `composeApp/build.gradle.kts` — route `@Serializable` cần nó.

## ViewModel multiplatform

```kotlin
// commonMain/kotlin/dev/devicepulse/ui/DashboardViewModel.kt
class DashboardViewModel(
    battery: BatterySensor,
    location: LocationSensor,
    network: NetworkSensor,
    // probe: LatencyProbe (bài 11), history: HistoryRepository (bài 12)
) : ViewModel() {
    val state: StateFlow<DashboardState> =
        combine(battery.observe(), location.observe(), network.observe()) { b, l, n ->
            DashboardState(battery = b, location = l, network = n)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())
}
```

- `combine` phát lại mỗi khi **một trong ba** luồng có giá trị mới (sau khi cả ba đã phát ít nhất một lần).
- `stateIn(... WhileSubscribed(5_000))`: chỉ nghe sensor khi có màn đang thu; xoay màn hình (dưới 5 giây) không phải đăng ký lại.
- Giá trị đầu `DashboardState()` có mọi thẻ ở `Reading.Loading`.

## Sensor giả và Koin

```kotlin
// commonMain/kotlin/dev/devicepulse/sensor/FakeSensors.kt
class FakeBatterySensor(
    initial: Reading<BatteryInfo> = Reading.Available(BatteryInfo(76, ChargingState.Charging)),
) : BatterySensor {
    val readings = MutableStateFlow(initial)
    override fun observe(): Flow<Reading<BatteryInfo>> = readings
}

class FakeLocationSensor(initial: Reading<LocationFix> = Reading.PermissionDenied) : LocationSensor {
    val readings = MutableStateFlow(initial)
    override fun observe(): Flow<Reading<LocationFix>> = readings
}

class FakeNetworkSensor(
    initial: Reading<NetworkStatus> = Reading.Available(NetworkStatus(ConnectionType.Wifi, isOnline = true)),
) : NetworkSensor {
    val readings = MutableStateFlow(initial)
    override fun observe(): Flow<Reading<NetworkStatus>> = readings
}
```

`readings` để public để test (bài 13) đẩy giá trị mới vào.

```kotlin
// commonMain/kotlin/dev/devicepulse/di/Koin.kt
val commonModule = module {
    single<BatterySensor> { FakeBatterySensor() }
    single<LocationSensor> { FakeLocationSensor() }
    single<NetworkSensor> { FakeNetworkSensor() }
    viewModelOf(::DashboardViewModel)
}

// expect val platformModule + initKoin giữ nguyên như bài 5
```

`initKoin`, `platformModule` (đang trống) và chỗ gọi ở `DevicePulseApp` / `iOSApp.swift` đã có từ [bài 5](05-dependencies.md) —
bài này chỉ điền `commonModule`. Bài 9 bắt đầu thêm sensor thật vào `platformModule`.

## Điều hướng type-safe

Route là các kiểu `@Serializable` theo spec; `SensorKind` là enum nên truyền thẳng làm tham số được:

```kotlin
@Serializable object DashboardRoute
@Serializable data class DetailRoute(val sensor: SensorKind)
@Serializable object HistoryRoute

@Composable
fun App() {
    MaterialTheme {
        val nav = rememberNavController()
        NavHost(nav, startDestination = DashboardRoute) {
            composable<DashboardRoute> {
                val vm = koinViewModel<DashboardViewModel>()
                val state by vm.state.collectAsStateWithLifecycle()
                DashboardScreen(state, onOpen = { nav.navigate(DetailRoute(it)) }, onHistory = { nav.navigate(HistoryRoute) })
            }
            composable<DetailRoute> { entry ->
                val sensor = entry.toRoute<DetailRoute>().sensor
                DetailScreen(sensor, onBack = { nav.popBackStack() })
            }
            composable<HistoryRoute> {
                PlaceholderScreen("Lịch sử — có ở bài 12", onBack = { nav.popBackStack() })
            }
        }
    }
}
```

`DashboardScreen` thêm tham số `onHistory` và một nút ở cuối cột. `DetailScreen` tự lấy `koinViewModel<DashboardViewModel>()` rồi hiện
`Reading` của đúng `sensor` cỡ lớn. iOS không có nút Back hệ thống → mỗi màn con cần `TopAppBar` có `navigationIcon` gọi `onBack`
(`TopAppBar` cần `@OptIn(ExperimentalMaterial3Api::class)`).

## Lifecycle

Compose Multiplatform ánh xạ vòng đời từng nền tảng (Activity Android, `UIViewController` iOS) về chung một mô hình `Lifecycle`.
`collectAsStateWithLifecycle` dựa vào đó để **ngừng thu** khi app xuống nền — kết hợp `WhileSubscribed` thì sensor thật (bài 9+)
cũng được huỷ đăng ký, đỡ tốn pin.

## Làm trên Device Pulse

1. Thêm 4 thư viện + plugin serialization, sync Gradle.
2. Tạo `FakeSensors.kt`, `DashboardViewModel.kt`; điền `commonModule` trong `di/Koin.kt`.
3. Thay `App()` của bài 6 bằng `NavHost` ở trên; xoá `fakeState`.
4. Viết `DetailScreen` và `PlaceholderScreen` (một `Scaffold` + `TopAppBar` + nội dung).

## Kiểm tra

- [ ] Android: Dashboard hiện 3 thẻ từ sensor giả (Vị trí hiện "Chưa được cấp quyền").
- [ ] Android: xoay màn hình → state giữ nguyên, các thẻ không quay lại trạng thái Loading.
- [ ] Bấm thẻ Pin → màn Chi tiết; nút back trên thanh trên và nút Back hệ thống Android đều quay lại.
- [ ] iOS: mở Chi tiết và Lịch sử, quay lại bằng nút trên `TopAppBar`.
- [ ] Đổi giá trị mặc định của `FakeBatterySensor` → cả hai nền tảng đổi theo, không sửa UI.

Lỗi `Serializer for class 'DetailRoute' is not found` → thiếu plugin serialization. Xem thêm [xử lý sự cố](../reference/troubleshooting.md).

## Tiếp theo?

- [Bài 9: đọc pin thật trên Android và iOS](09-battery.md).
- Đọc thêm: [Navigation 3](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html) · [Deep links](https://kotlinlang.org/docs/multiplatform/compose-navigation-deep-links.html) · [Koin cho Compose Multiplatform](https://insert-koin.io/docs/reference/koin-compose/compose/).

## Docs gốc

[Multiplatform ViewModel](https://kotlinlang.org/docs/multiplatform/compose-viewmodel.html) · [Lifecycle](https://kotlinlang.org/docs/multiplatform/compose-lifecycle.html) · [Navigation in Compose](https://kotlinlang.org/docs/multiplatform/compose-navigation.html) · [Navigation and routing](https://kotlinlang.org/docs/multiplatform/compose-navigation-routing.html) · [StateFlow và stateIn](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
