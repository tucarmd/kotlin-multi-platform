# UI native: Jetpack Compose + SwiftUI

Device Pulse **không** dùng chung UI. Lõi Kotlin (`shared/`) giữ dữ liệu và luật, còn mỗi nền tảng tự vẽ màn hình bằng
bộ UI của chính nó: Android bằng **Kotlin + Jetpack Compose**, iOS bằng **Swift + SwiftUI**. Bài này dựng màn Dashboard
3 thẻ **hai lần**, cả hai đọc cùng một `DashboardState` giả nằm trong `shared/`.

## Mục tiêu

- Biết mỗi app khởi động thế nào và lấy code chung từ đâu (`project(":shared")` · `import Shared`).
- Viết `SensorCard` + `ReadingContent` bằng Compose và bằng SwiftUI, vẽ đủ 5 trạng thái của `Reading<T>`.
- Thấy rõ ranh giới: **đổi dữ liệu ở `shared/` → hai UI cùng đổi**; đổi giao diện thì sửa từng bên.

## Hai app, một lõi

```text
apps/DevicePulse/
├── shared/      Kotlin Multiplatform: model, luật, cảm biến, state  → Android: thư viện · iOS: Shared.framework
├── androidApp/  Kotlin + Jetpack Compose — MainActivity → App() → các màn
└── iosApp/      Swift + SwiftUI — iOSApp → RootView → các View
```

=== "Android · androidApp"

    ```kotlin
    // androidApp/build.gradle.kts
    dependencies {
        implementation(project(":shared"))          // model, Reading, DashboardState… dùng như một thư viện Kotlin
        implementation(platform(libs.compose.bom))
        implementation(libs.compose.material3)
        implementation(libs.androidx.activity.compose)
    }
    ```

    ```kotlin
    // androidApp/src/main/kotlin/dev/devicepulse/android/MainActivity.kt
    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            enableEdgeToEdge()
            super.onCreate(savedInstanceState)
            setContent { App() }
        }
    }
    ```

=== "iOS · iosApp"

    ```swift
    // iosApp/iosApp/iOSApp.swift
    import SwiftUI
    import Shared            // framework Gradle build từ shared/ (baseName = "Shared")

    @main
    struct iOSApp: App {
        var body: some Scene {
            WindowGroup { DashboardView(state: fakeDashboardState()) }
        }
    }
    ```

    Xcode có một bước *Run Script* gọi Gradle (`embedAndSignAppleFrameworkForXcode`) để build `Shared.framework`
    trước khi biên dịch Swift. Wizard tạo sẵn bước này; sửa Kotlin xong chỉ cần bấm Run lại trong Xcode.

## Dữ liệu giả đặt ở `shared/`

Cả hai UI phải vẽ **cùng một** state, nên dữ liệu giả viết một lần trong `commonMain`:

```kotlin
// shared/src/commonMain/kotlin/dev/devicepulse/core/Fakes.kt
data class DashboardState(
    val battery: Reading<BatteryInfo> = Reading.Loading,
    val location: Reading<LocationFix> = Reading.Loading,
    val network: Reading<NetworkStatus> = Reading.Loading,
)

fun fakeDashboardState() = DashboardState(
    battery = Reading.Available(BatteryInfo(levelPercent = 76, charging = ChargingState.Charging)),
    location = Reading.Loading,
    network = Reading.Available(NetworkStatus(ConnectionType.Wifi, isOnline = true)),
)
```

Bài 8 dời `DashboardState` vào `PulseMonitor.kt`, nơi sinh ra state thật.

## Bật SKIE: cho Swift đọc Kotlin dễ chịu

Không có gì thêm, Swift thấy Kotlin qua lớp Objective-C: `sealed class` thành một đống class con, `enum` không `switch`
gọn được, hàm top-level nằm trong `FakesKt`, `Flow` gần như không dùng được. **SKIE** (Touchlab) là plugin Gradle sinh
thêm lớp Swift đẹp hơn cho framework:

```kotlin
// shared/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.skie)          // libs.versions.toml: skie = "0.10.x", id "co.touchlab.skie"
}
```

| Kotlin | Swift không có SKIE | Swift có SKIE |
|---|---|---|
| `sealed class Reading` | `if reading is ReadingLoading … else if let a = reading as? ReadingAvailable` | `switch onEnum(of: reading) { case .loading: … case .available(let a): … }` — thiếu nhánh là lỗi build |
| `enum class ChargingState` | `switch` phải có `default` | `switch` như enum Swift (`.charging`, `.full`) |
| hàm top-level `fakeDashboardState()` | `FakesKt.fakeDashboardState()` | `fakeDashboardState()` |
| `StateFlow<T>` (bài 8) | phải tự viết callback | `for await value in flow` |

## Vẽ 5 trạng thái của `Reading`

Phần khung (spinner, chưa có quyền, không hỗ trợ, lỗi) giống nhau ở mọi thẻ, nên tách một component nhận `content` cho
trạng thái `Available`. Viết **hai lần**, cùng tên, cùng ý:

=== "Kotlin · Jetpack Compose"

    ```kotlin
    // androidApp/.../ui/Components.kt
    @Composable
    fun <T : Any> ReadingContent(reading: Reading<T>, content: @Composable (T) -> Unit) {
        when (reading) {
            Reading.Loading -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Reading.PermissionDenied -> Text("Chưa được cấp quyền", color = MaterialTheme.colorScheme.error)
            Reading.Unsupported -> Text("Máy này không hỗ trợ")
            is Reading.Error -> Text("Lỗi: ${reading.message}", color = MaterialTheme.colorScheme.error)
            is Reading.Available -> content(reading.value)
        }
    }

    @Composable
    fun SensorCard(emoji: String, title: String, onClick: () -> Unit, body: @Composable () -> Unit) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$emoji  $title", style = MaterialTheme.typography.titleMedium)
                body()
            }
        }
    }
    ```

=== "Swift · SwiftUI"

    ```swift
    // iosApp/iosApp/Views/Components.swift
    struct ReadingView<T: AnyObject, Content: View>: View {
        let reading: Reading<T>
        @ViewBuilder let content: (T) -> Content

        var body: some View {
            switch onEnum(of: reading) {            // SKIE: sealed class Kotlin → enum Swift
            case .loading:
                ProgressView()
            case .permissionDenied:
                Text("Chưa được cấp quyền").foregroundStyle(.red)
            case .unsupported:
                Text("Máy này không hỗ trợ").foregroundStyle(.secondary)
            case .error(let error):
                Text("Lỗi: \(error.message)").foregroundStyle(.red)
            case .available(let available):
                content(available.value)
            }
        }
    }

    struct SensorCard<Body: View>: View {
        let emoji: String
        let title: String
        let onTap: () -> Void
        @ViewBuilder let body_: () -> Body

        var body: some View {
            VStack(alignment: .leading, spacing: 10) {
                Text("\(emoji)  \(title)").font(.headline)
                body_()
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
            .onTapGesture(perform: onTap)
        }
    }
    ```

    `T: AnyObject` vì qua Objective-C mọi object Kotlin là class. Nhờ SKIE, `switch` này phủ đủ 5 nhánh — thêm trạng thái
    mới vào `Reading` mà quên vẽ thì Xcode báo lỗi, đúng như `when` bên Kotlin.

Bên Kotlin, `when` trên `sealed class` là *exhaustive*: thêm trạng thái mới vào `Reading` mà quên vẽ thì Android báo lỗi ngay.

## Màn Dashboard

Màn hình chỉ **nhận state và vẽ**, không tự lấy dữ liệu:

=== "Kotlin · Jetpack Compose"

    ```kotlin
    // androidApp/.../ui/DashboardScreen.kt
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardScreen(state: DashboardState, onOpenDetail: (SensorKind) -> Unit = {}) {
        Scaffold(topBar = { TopAppBar(title = { Text("Device Pulse") }) }) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SensorCard("🔋", "Pin", { onOpenDetail(SensorKind.Battery) }) {
                        ReadingContent(state.battery) { Text("${it.levelPercent}% · ${it.charging}") }
                    }
                }
                item {
                    SensorCard("📍", "Vị trí", { onOpenDetail(SensorKind.Location) }) {
                        ReadingContent(state.location) { Text("${it.latitude}, ${it.longitude}") }
                    }
                }
                item {
                    SensorCard("📶", "Mạng", { onOpenDetail(SensorKind.Network) }) {
                        ReadingContent(state.network) { Text(if (it.isOnline) "${it.type} · online" else "Mất mạng") }
                    }
                }
            }
        }
    }

    @Composable
    fun App() = MaterialTheme { DashboardScreen(fakeDashboardState()) }
    ```

=== "Swift · SwiftUI"

    ```swift
    // iosApp/iosApp/Views/DashboardView.swift
    struct DashboardView: View {
        let state: DashboardState

        var body: some View {
            NavigationStack {
                ScrollView {
                    VStack(spacing: 12) {
                        SensorCard(emoji: "🔋", title: "Pin", onTap: {}) {
                            ReadingView(reading: state.battery) { info in
                                Text("\(info.levelPercent)% · \(info.charging)")
                            }
                        }
                        SensorCard(emoji: "📍", title: "Vị trí", onTap: {}) {
                            ReadingView(reading: state.location) { fix in
                                Text("\(fix.latitude), \(fix.longitude)")
                            }
                        }
                        SensorCard(emoji: "📶", title: "Mạng", onTap: {}) {
                            ReadingView(reading: state.network) { net in
                                Text(net.isOnline ? "\(net.type) · online" : "Mất mạng")
                            }
                        }
                    }
                    .padding(16)
                }
                .navigationTitle("Device Pulse")
            }
        }
    }
    ```

Hai bảng tra nhanh khi chuyển qua lại:

| Ý | Jetpack Compose | SwiftUI |
|---|---|---|
| Xếp dọc / ngang / chồng | `Column` / `Row` / `Box` | `VStack` / `HStack` / `ZStack` |
| Danh sách dài | `LazyColumn` | `List` hoặc `ScrollView` + `LazyVStack` |
| Chỉnh cỡ, lề, nền | `Modifier.padding(16.dp).background(…)` | `.padding(16).background(…)` |
| State cục bộ | `var x by remember { mutableStateOf(0) }` | `@State private var x = 0` |
| Thanh tiêu đề | `Scaffold` + `TopAppBar` | `NavigationStack` + `.navigationTitle` |
| Tai thỏ / thanh trạng thái | `Scaffold` lo, hoặc `Modifier.safeDrawingPadding()` | SwiftUI tự tránh safe area |
| Xem trước trong IDE | `@Preview` (Android Studio) | `#Preview { … }` (Xcode) |

Thứ tự modifier có ý nghĩa ở **cả hai**: `padding` trước `background` khác `background` trước `padding`.

## Preview

=== "Android Studio"

    ```kotlin
    @Preview
    @Composable
    private fun ErrorCardPreview() = MaterialTheme {
        SensorCard("🔋", "Pin", {}) { ReadingContent(Reading.Error("timeout")) { } }
    }
    ```

=== "Xcode"

    ```swift
    #Preview {
        DashboardView(state: fakeDashboardState())
    }
    ```

    Preview của Xcode cần `Shared.framework` đã build một lần (Run app trước, hoặc `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`).

## Làm trên Device Pulse

1. Bật plugin SKIE trong `shared/build.gradle.kts`, thêm `Fakes.kt` vào `shared/src/commonMain/.../core/`.
2. Android: `Components.kt`, `DashboardScreen.kt`, `App()` như trên.
3. iOS: tạo nhóm `Views/` trong Xcode, thêm `Components.swift`, `DashboardView.swift`; sửa `iOSApp.swift`.
4. Chữ đang viết cứng — bài 7 chuyển ra file chuỗi của **từng nền tảng**.

## Kiểm tra

- [ ] Android: Dashboard hiện 3 thẻ — Pin `76% · Charging`, Vị trí đang quay spinner, Mạng `Wifi · online`.
- [ ] iOS (Simulator): cùng 3 thẻ, vẽ bằng SwiftUI (thanh tiêu đề lớn kiểu iOS, không phải Material).
- [ ] Sửa `fakeDashboardState()` trong `shared/`: `location = Reading.PermissionDenied` rồi `Reading.Error("x")` → **cả hai** app đổi chữ, không sửa dòng UI nào.
- [ ] Preview hiện được ở Android Studio **và** Xcode.

## Lỗi hay gặp

- Xcode báo `No such module 'Shared'` → framework chưa build; bấm Run một lần hoặc kiểm tra bước *Run Script* gọi Gradle.
- Sửa Kotlin xong iOS không đổi → Xcode chưa build lại framework; *Product → Clean Build Folder* rồi Run.
- `Cannot find 'onEnum' in scope` → chưa bật SKIE hoặc chưa build lại framework.
- Swift thấy `Int32` / `KotlinLong` thay vì `Int` → kiểu số của Kotlin qua Objective-C; ép kiểu khi hiển thị (`Int(info.levelPercent)`).
- Xem thêm [xử lý sự cố](../reference/troubleshooting.md).

## Tiếp theo?

- [Bài 7: chuỗi và đa ngôn ngữ trên từng nền tảng](07-resources.md).
- Đọc thêm: [SwiftUI tutorials](https://developer.apple.com/tutorials/swiftui) · [Compose layouts](https://developer.android.com/develop/ui/compose/layouts/basics) · [Kotlin–Swift interopedia](https://github.com/kotlin-hands-on/kotlin-swift-interopedia) · [SKIE](https://skie.touchlab.co/).

## Docs gốc

[Create an app with native UI](https://kotlinlang.org/docs/multiplatform/multiplatform-create-first-app.html) · [iOS integration methods](https://kotlinlang.org/docs/multiplatform/multiplatform-ios-integration-overview.html) · [Interop with Swift/Objective-C](https://kotlinlang.org/docs/native-objc-interop.html) · [Material 3 trong Compose](https://developer.android.com/develop/ui/compose/designsystems/material3) · [SwiftUI](https://developer.apple.com/documentation/swiftui)
