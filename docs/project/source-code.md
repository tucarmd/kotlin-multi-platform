# Code mẫu Device Pulse

Bản code hoàn chỉnh của app nằm trong repo, ở thư mục
**[`DevicePulse/`](https://github.com/tucarmd/kotlin-multi-platform/tree/main/DevicePulse)**. Đây là đáp án sau bài 14:
pin, GPS, mạng, đo độ trễ và lịch sử đều chạy thật trên Android và iOS.

!!! warning "Tự gõ trước, mở đáp án sau"
    Làm bài trên project của bạn trước. Chỉ mở code mẫu khi đã kẹt hơn 20 phút, hoặc khi đã chạy được và muốn so cách viết.
    Copy nguyên file thì app vẫn chạy, nhưng bạn sẽ không học được gì.

## Chạy thử

```bash
git clone https://github.com/tucarmd/kotlin-multi-platform.git
cd kotlin-multi-platform/DevicePulse

./gradlew :androidApp:installDebug                                  # Android: cài lên emulator / máy đang cắm
./gradlew :shared:testDebugUnitTest :shared:iosSimulatorArm64Test   # test core trên JVM + iOS Simulator
open iosApp/iosApp.xcodeproj                                        # iOS: bấm Run trong Xcode
```

Cần JDK 17+, Android SDK 36, Xcode 16+. Chi tiết và lệnh giả lập cảm biến:
[`DevicePulse/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/README.md).

## Cây thư mục

Mở `DevicePulse/` trong Android Studio sẽ thấy các mục dưới đây. Mục **[máy]** do Studio/Gradle tự sinh, nằm trong
`.gitignore`, nên không có trên GitHub. Xoá đi thì lần build sau sẽ được tạo lại.

```text
DevicePulse/
├── shared/                          [git]  module KMP core: commonMain, androidMain, iosMain, commonTest
├── androidApp/                      [git]  app Android, Jetpack Compose
├── iosApp/                          [git]  app iOS, SwiftUI + project.yml (XcodeGen)
├── design/mockups/                  [git]  mockup UI 8 màn, chia android/ và ios/
├── gradle/
│   ├── libs.versions.toml           [git]  version catalog: phiên bản thư viện + plugin
│   ├── gradle-daemon-jvm.properties [git]  ghim Gradle chạy bằng JDK 21
│   └── wrapper/                     [git]  Gradle wrapper (8.14.5)
├── build.gradle.kts                 [git]  khai báo plugin cho cả project
├── settings.gradle.kts              [git]  repo Maven + danh sách module
├── gradle.properties                [git]  RAM cho daemon, bật cache, AndroidX
├── gradlew, gradlew.bat             [git]  chạy đúng phiên bản Gradle, không cần cài
├── local.properties                 [máy]  sdk.dir → Android SDK
├── .idea/                           [máy]  setting Android Studio
├── .gradle/                         [máy]  cache Gradle của project
├── .kotlin/                         [máy]  cache Kotlin compiler daemon
└── build/                           [máy]  output build
```

Cây bên trong `shared/src/`, `androidApp/src/` và `iosApp/` có ở mục *Cấu trúc* của
[`DevicePulse/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/README.md#cau-truc).

## Code mẫu khác bài học ở đâu

!!! warning "Code mẫu đã chuyển sang UI native"
    14 bài viết UI **chung** bằng Compose Multiplatform. Code mẫu hiện dùng kiến trúc **KMP core + UI native**:
    `shared/` chứa toàn bộ logic, `androidApp/` vẽ bằng Jetpack Compose, `iosApp/` vẽ bằng SwiftUI.
    Phần core và Koin (bài 4–5, 9–13) gần như giữ nguyên; phần UI (bài 6–8) khác bài học.

| Bài | File trong code mẫu (tính từ `DevicePulse/`) |
|---|---|
| [3. Cấu trúc project](../lessons/03-project-structure.md) | `settings.gradle.kts`, `shared/build.gradle.kts`, `androidApp/build.gradle.kts`, `gradle/libs.versions.toml` |
| [4. Chia sẻ code](../lessons/04-share-code.md) | `shared/.../model/Models.kt`, `shared/.../rules/Rules.kt` |
| [5. Thêm thư viện](../lessons/05-dependencies.md) | `shared/.../di/Koin.kt`, `*/di/PlatformModule.*.kt`, `iosMain/.../di/KoinIos.kt` · Android: `androidApp/.../DevicePulseApp.kt` (`appModule`) |
| [6. Compose cơ bản](../lessons/06-compose-basics.md) | Android: `androidApp/.../ui/DashboardScreen.kt`, `Components.kt` · iOS: `iosApp/iosApp/Views/DashboardView.swift`, `Components.swift` |
| [7. Resources](../lessons/07-resources.md) | `androidApp/src/main/res/values*/strings.xml` · `iosApp/iosApp/*.lproj/Localizable.strings` |
| [8. ViewModel và điều hướng](../lessons/08-viewmodel-navigation.md) | `shared/.../core/PulseMonitor.kt` · Android: `ui/PulseViewModel.kt`, `ui/App.kt` · iOS: `PulseModel.swift`, `Views/RootView.swift` |
| [9. Đọc pin](../lessons/09-battery.md) | `shared/.../sensor/Sensors.kt`, `AndroidBatterySensor.kt`, `IosBatterySensor.kt` |
| [10. GPS và xin quyền](../lessons/10-location.md) | `AndroidLocationSensor.kt`, `IosLocationSensor.kt` · quyền: `androidApp/.../ui/LocationPermission.kt`, `iosApp/iosApp/LocationPermission.swift` |
| [11. Mạng và độ trễ](../lessons/11-network.md) | `shared/.../sensor/LatencyProbe.kt`, `AndroidNetworkSensor.kt`, `IosNetworkSensor.kt` |
| [12. Lịch sử với SQLDelight](../lessons/12-history-sqldelight.md) | `shared/src/commonMain/sqldelight/.../Snapshot.sq`, `data/HistoryRepository.kt` |
| [13. Test](../lessons/13-testing.md) | `shared/src/commonTest/.../RulesTest.kt`, `LatencyProbeTest.kt`, `PulseMonitorTest.kt` |
| [14. Build và phát hành](../lessons/14-build-publish.md) | `iosApp/project.yml`, `iosApp/iosApp/Info.plist` |

Code mẫu có thể đi trước bài học một chút về phiên bản thư viện. Gặp chỗ khác nhau thì xem mục *Version và vì sao* trong
README của `DevicePulse/`.

## Mockup giao diện

8 màn (Dashboard, 3 màn chi tiết, Lịch sử, dark mode) có prompt và bảng theo dõi ở
[`DevicePulse/design/mockups/`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/design/mockups/README-MOCKUPS.md).
Dùng để đối chiếu khi tự vẽ UI ở bài 6 và bài 8.

## Muốn xem thêm app khác?

Xem [Repo mẫu tham khảo](../reference/sample-repos.md): template chính chủ, đáp án tutorial của JetBrains và các app KMP thật.

## Phụ lục: Kotlin cần nhớ khi đọc code mẫu

Phần này gom lại cú pháp hay gặp nhất, các best practice mà code mẫu đang dùng, và một số mẹo nhỏ. Ví dụ lấy từ
chính `DevicePulse/` (Kotlin 2.4, coroutines 1.11). Không cần học thuộc. Gặp chỗ lạ trong code mẫu thì quay lại đây tra.

### Cú pháp cần thiết nhất

#### Biến, kiểu và null safety

```kotlin
val name = "Pixel"            // val = không gán lại được (ưu tiên dùng)
var count = 0                 // var = gán lại được
val speed: Double? = null     // dấu ? = có thể null

speed?.let { println(it) }    // ?.  chỉ chạy khi khác null
val shown = speed ?: 0.0      // ?:  (Elvis) giá trị dự phòng khi null
val sure = speed!!            // !!  "tôi chắc chắn khác null", sai thì crash, tránh dùng
```

#### Hàm

```kotlin
fun level(percent: Int): String = if (percent < 20) "Thấp" else "Ổn"   // hàm một biểu thức

fun connect(host: String, port: Int = 443, timeoutMs: Long = 5_000) { /* ... */ }
connect("example.com", timeoutMs = 2_000)        // tham số mặc định + gọi theo tên
```

#### Class: `data class`, `enum class`, `sealed class`, `object`

```kotlin
data class BatteryInfo(val levelPercent: Int, val charging: ChargingState)   // equals/hashCode/toString/copy tự sinh
enum class ChargingState { Charging, Discharging, Full, Unknown }            // tập giá trị cố định

sealed class Reading<out T : Any> {                                          // tập trạng thái đóng, có dữ liệu riêng
    data object Loading : Reading<Nothing>()
    data class Available<out T : Any>(val value: T) : Reading<T>()
    data class Error(val message: String) : Reading<Nothing>()
}

object Clock { fun nowMs() = 0L }                                            // singleton
```

`copy()` của data class là cách sửa state mà không làm hỏng object cũ:

```kotlin
val next = state.copy(latencyMs = Reading.Loading)
```

#### `when`: switch mạnh hơn

```kotlin
val text = when (reading) {
    Reading.Loading -> "Đang đọc..."
    is Reading.Available -> "${reading.value.levelPercent}%"   // smart cast: tự hiểu reading là Available
    is Reading.Error -> reading.message
    else -> "Không hỗ trợ"
}
```

Khi `when` phủ hết các nhánh của một `sealed class` thì không cần `else`. Thêm nhánh mới vào sealed class, compiler sẽ
báo lỗi ở mọi `when` còn thiếu. Đây là lý do code mẫu dùng `Reading` thay vì trả `null` hay ném exception.

#### Lambda và collection

```kotlin
val low = snapshots.filter { it.batteryPercent < 20 }   // it = tham số duy nhất của lambda
val levels = snapshots.map { it.batteryPercent }
val avg = levels.average()
val byType = snapshots.groupBy { it.connection }
val first = snapshots.firstOrNull { it.isOnline }       // bản ...OrNull không bao giờ ném lỗi
```

Lambda là tham số cuối thì được viết ra ngoài ngoặc: `items.forEach { ... }`, `scope.launch { ... }`.

#### Extension function

Thêm hàm cho class có sẵn mà không sửa class đó. Code mẫu dùng ở `Models.kt`:

```kotlin
fun <T : Any> Reading<T>.valueOrNull(): T? = (this as? Reading.Available<T>)?.value

val battery = state.battery.valueOrNull()   // gọi như hàm của Reading
```

#### Scope function: `let`, `apply`, `also`, `run`

| Hàm | Trong block gọi object là | Trả về | Dùng khi |
|---|---|---|---|
| `let` | `it` | kết quả của block | xử lý giá trị khác null: `x?.let { ... }` |
| `apply` | `this` | chính object | cấu hình object vừa tạo |
| `also` | `it` | chính object | làm thêm việc phụ (log) giữa chuỗi |
| `run` | `this` | kết quả của block | tính toán dùng nhiều thuộc tính của object |

```kotlin
val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
val saved = repository.insert(snapshot).also { println("đã lưu $it") }
```

#### Coroutines và Flow

```kotlin
suspend fun measure(): Long { /* ... */ }        // suspend = hàm được phép "chờ" mà không chặn thread

scope.launch { latency.value = Reading.Available(measure()) }   // chạy một việc nền

fun observe(): Flow<Reading<BatteryInfo>> = flow {             // Flow = dòng giá trị theo thời gian
    while (true) { emit(readOnce()); delay(1_000) }
}
```

Mẫu quan trọng nhất của code mẫu nằm ở `PulseMonitor.kt`: gộp nhiều Flow thành một state cho UI.

```kotlin
val state: StateFlow<DashboardState> = combine(
    battery.observe(),
    locationRestarts.flatMapLatest { location.observe() },
    network.observe(),
    latency,
) { b, l, n, ms -> DashboardState(b, l, n, ms) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5_000), DashboardState())
```

#### `expect` / `actual` và interface (KMP)

Có hai cách để `commonMain` gọi code riêng từng nền tảng:

```kotlin
// Cách 1: interface trong commonMain, class thật ở androidMain/iosMain, nối bằng Koin (code mẫu dùng cách này)
interface BatterySensor { fun observe(): Flow<Reading<BatteryInfo>> }

// Cách 2: expect/actual, hợp với hàm hoặc giá trị nhỏ
expect fun platformName(): String                 // commonMain
actual fun platformName(): String = "Android"     // androidMain
```

Ưu tiên interface: dễ thay bằng bản giả khi test (xem `PulseMonitorTest.kt`).

### Best practices

1. **`val` trước, `var` khi bắt buộc.** State thay đổi thì để trong `MutableStateFlow` private, chỉ mở ra ngoài
   `StateFlow` chỉ đọc:

    ```kotlin
    private val _latency = MutableStateFlow<Reading<Long>?>(null)
    val latency: StateFlow<Reading<Long>?> = _latency
    ```

2. **Mô hình hoá trạng thái bằng `sealed class`**, không dùng cặp `isLoading` + `error` + `data` rời rạc. Một
   `Reading` chỉ có thể ở đúng một trạng thái, nên UI không bao giờ vừa "đang tải" vừa "lỗi".
3. **Không để exception vượt qua ranh giới module.** Sensor bắt lỗi và trả `Reading.Error`. Với Swift điều này càng
   quan trọng: exception Kotlin không được khai báo `@Throws` sẽ làm app iOS crash.
4. **Không dùng `!!`.** Dùng `?.`, `?:`, `requireNotNull(x) { "lý do" }` hoặc `checkNotNull`. Lỗi có thông điệp rõ
   ràng dễ sửa hơn nhiều so với `NullPointerException`.
5. **Không dùng `GlobalScope`.** Mọi coroutine chạy trong một scope có vòng đời: `viewModelScope` trên Android,
   scope do `PulseCore.startMonitor` tạo trên iOS (nhớ gọi `close()`).
6. **Logic để ở `commonMain`, chỉ phần chạm API hệ điều hành mới ở `androidMain`/`iosMain`.** Logic chung thì test
   một lần chạy cho cả hai nền tảng.
7. **Inject dependency qua constructor.** `PulseMonitor` nhận sensor, repository, scope qua constructor. Koin chỉ là
   chỗ lắp ráp, class không tự gọi `get()` bên trong.
8. **Đặt tên theo convention Kotlin:** class `PascalCase`, hàm/biến `camelCase`, hằng số `UPPER_SNAKE_CASE` trong
   `companion object` hoặc top-level `const val`. File chứa một class chính thì đặt tên trùng class.
9. **Version thư viện chỉ khai báo ở `gradle/libs.versions.toml`.** `build.gradle.kts` gọi qua `libs.xxx`, không
   ghi version cứng.
10. **Test logic bằng `kotlin.test` + `runTest`** trong `commonTest`. `runTest` bỏ qua `delay()` nên test chạy tức
    thì.

### Tips & tricks

??? tip "Số dài dễ đọc hơn với dấu gạch dưới"
    `5_000`, `1_000_000L`. Compiler bỏ qua dấu `_`, chỉ để mắt người đọc.

??? tip "Destructuring data class và Pair"
    ```kotlin
    val (level, charging) = batteryInfo
    for ((index, item) in items.withIndex()) { /* ... */ }
    val (ok, failed) = results.partition { it.isSuccess }
    ```

??? tip "`buildList`, `buildString` thay cho list/StringBuilder tạm"
    ```kotlin
    val rows = buildList {
        add("Pin: $level%")
        if (online) add("Đang online")
    }
    ```

??? tip "`require` / `check` thay cho if + throw"
    ```kotlin
    require(percent in 0..100) { "percent phải trong 0..100, nhận $percent" }   // lỗi do người gọi
    check(isStarted) { "Gọi start() trước" }                                     // lỗi do trạng thái
    ```

??? tip "`runCatching` gói lỗi thành `Result`"
    ```kotlin
    val ms = runCatching { measure() }
        .fold(onSuccess = { Reading.Available(it) }, onFailure = { Reading.Error(it.message ?: "lỗi") })
    ```
    Cẩn thận: `runCatching` cũng nuốt luôn `CancellationException`. Trong coroutine, ném lại nó nếu bắt được.

??? tip "`value class` cho đơn vị đo, không tốn object"
    ```kotlin
    @JvmInline value class Millis(val value: Long)
    fun timeout(ms: Millis) { /* ... */ }      // không thể truyền nhầm giây vào
    ```

??? tip "`lazy` cho giá trị tốn công tạo"
    ```kotlin
    private val formatter by lazy { createFormatter() }   // chỉ tạo lần đầu dùng
    ```

??? tip "`flowOn`, `distinctUntilChanged`, `debounce` cho Flow"
    ```kotlin
    sensor.observe()
        .distinctUntilChanged()           // bỏ giá trị trùng liên tiếp, UI đỡ vẽ lại
        .flowOn(Dispatchers.Default)      // phần phía trên chạy ở thread nền
    ```

??? tip "Phím tắt Android Studio đáng nhớ"
    | Việc | macOS |
    |---|---|
    | Sửa nhanh / gợi ý (import, thêm `?`, tạo hàm) | `⌥+Enter` |
    | Nhảy tới định nghĩa | `⌘+B` |
    | Tìm mọi nơi dùng | `⌥+F7` |
    | Đổi tên an toàn | `Shift+F6` |
    | Xem kiểu của biến | `⌃+Shift+P` |
    | Tìm mọi thứ | `Shift` `Shift` (bấm 2 lần) |
    | Xem Kotlin dịch ra bytecode / Java | *Tools → Kotlin → Show Kotlin Bytecode → Decompile* |

??? tip "Dán code Java vào file `.kt`"
    Android Studio hỏi có chuyển sang Kotlin không. Chọn có, rồi đọc lại kết quả: bản chuyển thường còn `!!` và
    `var` thừa, cần dọn theo mục best practice ở trên.

??? tip "Thử nhanh một đoạn Kotlin"
    Dùng [play.kotlinlang.org](https://play.kotlinlang.org) hoặc *File → New → Scratch File → Kotlin* trong Android
    Studio. Không cần tạo project.

Đọc thêm: [Kotlin idioms](https://kotlinlang.org/docs/idioms.html) và
[Coding conventions](https://kotlinlang.org/docs/coding-conventions.html) của JetBrains.
