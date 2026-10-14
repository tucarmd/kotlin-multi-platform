# Compose Multiplatform cơ bản

Compose Multiplatform là Jetpack Compose của Android, được JetBrains đưa lên iOS. Biết Jetpack Compose rồi thì gần như
biết luôn — khác chủ yếu ở **điểm khởi động** của từng nền tảng. Bài này dựng màn Dashboard của Device Pulse với 3 thẻ,
chạy trên dữ liệu giả.

## Mục tiêu

- Biết Android và iOS khởi động hàm `App()` chung thế nào.
- Viết composable `SensorCard` vẽ được mọi trạng thái của `Reading<T>`.
- Dựng Dashboard 3 thẻ (Pin / Vị trí / Mạng) bằng Material 3, layout và modifier.

## Một App(), hai điểm khởi động

Toàn bộ UI nằm trong `@Composable fun App()` ở `commonMain`. Mỗi nền tảng chỉ có vài dòng gọi nó:

=== "Android · androidMain"

    ```kotlin
    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            setContent { App() }
        }
    }
    ```

=== "iOS · iosMain"

    ```kotlin
    fun MainViewController() = ComposeUIViewController { App() }
    ```

    Phía Swift (`iosApp/ContentView.swift`, wizard đã tạo sẵn) bọc `MainViewController()` vào SwiftUI qua `UIViewControllerRepresentable`.

## Layout và modifier

- `Column` / `Row` / `Box` xếp dọc / ngang / chồng; `LazyColumn` cho danh sách dài (chỉ vẽ dòng đang thấy).
- `Modifier` đổi cỡ, khoảng cách, bấm. **Thứ tự modifier có ý nghĩa**: `padding` trước `background` khác `background` trước `padding`.
- Composable nên nhận `modifier: Modifier = Modifier` để chỗ gọi tự chỉnh vị trí, cỡ.
- Tràn viền (edge-to-edge): dùng `Scaffold` hoặc `Modifier.safeDrawingPadding()` để nội dung không bị tai thỏ / thanh trạng thái che.

## Làm trên Device Pulse

`SensorCard` là composable generic: phần khung (tiêu đề, spinner, lỗi) dùng chung, phần hiện giá trị do chỗ gọi truyền vào.

```kotlin
// commonMain/kotlin/dev/devicepulse/ui/SensorCard.kt
@Composable
fun <T> SensorCard(
    title: String,
    reading: Reading<T>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable (T) -> Unit,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            when (reading) {
                Reading.Loading -> CircularProgressIndicator(Modifier.size(24.dp))
                is Reading.Available -> content(reading.value)
                Reading.PermissionDenied -> Text("Chưa được cấp quyền", color = MaterialTheme.colorScheme.error)
                Reading.Unsupported -> Text("Máy này không hỗ trợ")
                is Reading.Error -> Text("Lỗi: ${reading.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
```

`when` trên `sealed interface` là *exhaustive*: thêm trạng thái mới vào `Reading` mà quên vẽ thì compiler báo lỗi ngay.

Màn Dashboard chỉ nhận `DashboardState` và vẽ — không tự lấy dữ liệu:

```kotlin
// commonMain/kotlin/dev/devicepulse/ui/DashboardScreen.kt
@Composable
fun DashboardScreen(state: DashboardState, modifier: Modifier = Modifier, onOpen: (SensorKind) -> Unit = {}) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SensorCard("Pin", state.battery, onClick = { onOpen(SensorKind.Battery) }) { b ->
            Text("${b.levelPercent}% · ${b.charging}", style = MaterialTheme.typography.headlineMedium)
        }
        SensorCard("Vị trí", state.location, onClick = { onOpen(SensorKind.Location) }) { fix ->
            Text("${fix.latitude}, ${fix.longitude} (±${fix.accuracyMeters} m)")
        }
        SensorCard("Mạng", state.network, onClick = { onOpen(SensorKind.Network) }) { net ->
            Text(if (net.isOnline) "${net.type} · online" else "Mất mạng")
        }
    }
}

private val fakeState = DashboardState(
    battery = Reading.Available(BatteryInfo(levelPercent = 76, charging = ChargingState.Charging)),
    location = Reading.Loading,
    network = Reading.Available(NetworkStatus(ConnectionType.Wifi, isOnline = true)),
)

@Composable
fun App() {
    MaterialTheme {
        Scaffold { padding -> DashboardScreen(fakeState, Modifier.padding(padding)) }
    }
}
```

Chuỗi đang viết cứng — bài 7 chuyển ra resources. `DashboardState` là `data class` trong spec chung
(xem [trang dự án](../project/index.md)); bài 8 mới có ViewModel sinh ra nó.

### @Preview

Xem thẻ ngay trong IDE, không cần chạy máy ảo:

```kotlin
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun SensorCardErrorPreview() {
    MaterialTheme {
        SensorCard<BatteryInfo>("Pin", Reading.Error("timeout")) {}
    }
}
```

!!! tip "Preview trong commonMain"
    Bản Compose Multiplatform mới cho phép `@Preview` ngay trong `commonMain` (cần dependency `ui-tooling-preview`,
    wizard thường thêm sẵn). Không thấy khung preview → kiểm tra plugin Compose Multiplatform trong IDE, hoặc đặt preview tạm ở `androidMain`.

## Kiểm tra

- [ ] Android: Dashboard hiện 3 thẻ — Pin `76% · Charging`, Vị trí đang quay spinner, Mạng `Wifi · online`.
- [ ] iOS (Simulator): cùng 3 thẻ, không bị tai thỏ che tiêu đề đầu.
- [ ] Đổi `fakeState.location` thành `Reading.PermissionDenied` rồi `Reading.Error("x")` → thẻ Vị trí đổi chữ trên cả hai nền tảng.
- [ ] Preview `SensorCardErrorPreview` hiện trong IDE.

Gặp lỗi build iOS → xem [xử lý sự cố](../reference/troubleshooting.md).

## Tiếp theo?

- [Bài 7: resources và đa ngôn ngữ](07-resources.md).
- Đọc thêm: [Platform differences](https://kotlinlang.org/docs/multiplatform/compose-platform-specifics.html) · [Adaptive layouts](https://kotlinlang.org/docs/multiplatform/compose-adaptive-layouts.html) · [Compose Hot Reload](https://kotlinlang.org/docs/multiplatform/compose-hot-reload.html) · [Compose Multiplatform và Jetpack Compose](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-and-jetpack-compose.html).

## Docs gốc

[App entry points](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-entry-points.html) · [Layout basics](https://kotlinlang.org/docs/multiplatform/compose-layout.html) · [Modifiers](https://kotlinlang.org/docs/multiplatform/compose-layout-modifiers.html) · [UI previews](https://kotlinlang.org/docs/multiplatform/compose-previews.html) · [Material 3 trong Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
