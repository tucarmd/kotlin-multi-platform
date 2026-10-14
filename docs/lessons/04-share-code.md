# Chia sẻ code giữa các nền tảng

Bài lõi của KMP: đặt tối đa code vào `commonMain`, và khi buộc phải gọi API riêng của máy thì nối bằng interface hoặc `expect`/`actual`.
Ở bài này Device Pulse có bộ "xương": model, các hàm chấm điểm và interface cảm biến.

## Mục tiêu

- Biết ba mức chia sẻ: mọi nền tảng, một nhóm nền tảng, một nền tảng.
- Viết được một `expect`/`actual`.
- Hiểu "cảm biến" trong app là gì, và vì sao viết nó bằng **interface** chứ không bằng `expect class`.
- Có đủ model, `Reading` và rules của Device Pulse trong `commonMain`.

## Ba mức chia sẻ

1. **Mọi nền tảng** → `commonMain`. Logic thuần Kotlin: model, chấm điểm pin/GPS/độ trễ.
2. **Một nhóm nền tảng giống nhau** → source set trung gian (`iosMain`). Code dùng API Apple, viết một lần cho iPhone thật lẫn simulator.
3. **Một nền tảng** → `androidMain`.

Trong `iosMain` bạn gọi thẳng API Apple qua **platform libraries** đã kèm sẵn: `platform.Foundation`, `platform.UIKit`, `platform.CoreLocation`…

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

Thiếu một `actual` là build báo lỗi ngay — không có chuyện quên một nền tảng. Hai bên phải **cùng package, cùng tên**.

## "Cảm biến" trong bài này là gì?

Trong Device Pulse, **cảm biến** là phần code **hỏi điện thoại một con số thật**:

| Cảm biến | Hỏi điện thoại cái gì | Android hỏi bằng | iOS hỏi bằng |
|---|---|---|---|
| `BatterySensor` | Pin còn bao nhiêu %, có đang sạc không | `BatteryManager` | `UIDevice` |
| `LocationSensor` | Đang đứng ở toạ độ nào | `LocationManager` | `CLLocationManager` |
| `NetworkSensor` | Đang dùng Wi-Fi hay 4G, có mạng không | `ConnectivityManager` | `nw_path_monitor` |

Câu hỏi thì giống nhau trên hai máy, nhưng **cách hỏi khác hẳn nhau**: mỗi hệ điều hành có API riêng. Đây chính là chỗ
code không thể viết chung được, nên phải tách ra.

## Cách 2: interface (dùng cho cảm biến)

Hãy hình dung `interface` như một **tờ yêu cầu**: "ai làm cảm biến pin thì phải có hàm `observe()` trả về thông tin pin".
Tờ yêu cầu viết một lần trong `commonMain`. Android và iOS mỗi bên tự làm một bản đáp ứng tờ yêu cầu đó.

```kotlin
// commonMain — tờ yêu cầu, chưa có code thật
interface BatterySensor { fun observe(): Flow<Reading<BatteryInfo>> }

// androidMain — bản cho Android (bài 9)
class AndroidBatterySensor(context: Context) : BatterySensor { /* hỏi BatteryManager */ }

// iosMain — bản cho iOS (bài 9)
class IosBatterySensor : BatterySensor { /* hỏi UIDevice */ }
```

Phần code chung (`PulseMonitor`, màn Dashboard) chỉ biết tới `BatterySensor`. Nó **không biết và không cần biết**
đang chạy trên máy nào. Lúc app khởi động, Koin đưa đúng bản Android hoặc bản iOS vào (bài 5).

### Sao không dùng `expect class` cho gọn?

`expect class BatterySensor` cũng tách được Android/iOS. Nhưng có 3 chuyện nó làm không tốt:

1. **Không làm được bản giả để test.** Muốn test "pin 15% thì hiện cảnh báo" mà không cần điện thoại, bạn cần một
   cảm biến *giả* luôn trả về 15%. Với interface, viết bản giả rất dễ:

    ```kotlin
    class FakeBattery : BatterySensor {
        override fun observe() = flowOf(Reading.Available(BatteryInfo(15, ChargingState.Discharging)))
    }
    ```

    Với `expect class` thì mỗi nền tảng chỉ có **đúng một** bản thật, không chen bản giả vào được. Bài 13 dùng toàn bản giả.

2. **Android cần thêm đồ, iOS thì không.** Cảm biến Android phải được đưa `Context` (cái "chìa khoá" để gọi dịch vụ hệ thống),
   còn iOS không cần. Với interface, mỗi bản tự có hàm khởi tạo riêng. Với `expect class`, hai bên phải khớp nhau từng chữ.

3. **`expect class` vẫn đang Beta.** Kotlin hiện cảnh báo khi dùng, còn interface là Kotlin bình thường.

### Vậy khi nào dùng cái nào?

| Dùng | Khi | Ví dụ |
|---|---|---|
| `expect fun` | Hàm nhỏ, hỏi một lần là xong, không cần bản giả khi test | `platformName()` |
| `interface` | Thứ chạy lâu (bật lên, theo dõi, tắt đi), hoặc cần bản giả khi test | 3 cảm biến, `HistoryRepository` |

!!! warning "Không exception nào được vượt qua ranh giới cảm biến"
    Mọi lỗi (thiếu quyền, máy không hỗ trợ, API ném lỗi) phải được bắt **bên trong** bản cài đặt và đổi thành
    `Reading.PermissionDenied`, `Reading.Unsupported` hoặc `Reading.Error`. UI chỉ việc vẽ theo trạng thái, không bao giờ crash.

## Làm trên Device Pulse

**1. Model** — `commonMain/kotlin/dev/devicepulse/model/`:

```kotlin
import kotlin.time.Instant

data class BatteryInfo(val levelPercent: Int, val charging: ChargingState)
enum class ChargingState { Charging, Discharging, Full, Unknown }

data class LocationFix(
    val latitude: Double, val longitude: Double,
    val accuracyMeters: Double, val speedMps: Double?, val timestamp: Instant,
)

enum class ConnectionType { Wifi, Cellular, Ethernet, None, Unknown }
data class NetworkStatus(val type: ConnectionType, val isOnline: Boolean)

sealed interface Reading<out T> {
    data object Loading : Reading<Nothing>
    data class Available<T>(val value: T) : Reading<T>
    data object PermissionDenied : Reading<Nothing>
    data object Unsupported : Reading<Nothing>      // vd pin trên iOS simulator
    data class Error(val message: String) : Reading<Nothing>
}

enum class SensorKind { Battery, Location, Network }
```

`Instant` nằm trong thư viện chuẩn (`kotlin.time`); nếu IDE đòi opt-in thì thêm `@OptIn(ExperimentalTime::class)` theo gợi ý. Bài 5 thêm `kotlinx-datetime` để hiển thị giờ theo múi giờ máy.

**2. Rules** — hàm thuần, `commonMain/kotlin/dev/devicepulse/rules/`:

```kotlin
enum class BatteryLevel { Critical, Low, Ok }
fun batteryLevel(info: BatteryInfo): BatteryLevel = when {
    info.levelPercent <= 10 -> BatteryLevel.Critical
    info.levelPercent <= 20 -> BatteryLevel.Low
    else -> BatteryLevel.Ok
}

enum class FixQuality { Good, Fair, Poor }
fun fixQuality(fix: LocationFix): FixQuality = when {
    fix.accuracyMeters <= 20.0 -> FixQuality.Good
    fix.accuracyMeters <= 100.0 -> FixQuality.Fair
    else -> FixQuality.Poor
}

enum class LatencyGrade { Fast, Ok, Slow }
fun latencyGrade(millis: Long): LatencyGrade = when {
    millis < 100 -> LatencyGrade.Fast
    millis < 300 -> LatencyGrade.Ok
    else -> LatencyGrade.Slow
}
```

Không phụ thuộc nền tảng nào → test một lần ở `commonTest` (bài 13).

**3. Interface cảm biến** — `commonMain/kotlin/dev/devicepulse/sensor/`:

```kotlin
import kotlinx.coroutines.flow.Flow

interface BatterySensor  { fun observe(): Flow<Reading<BatteryInfo>> }
interface LocationSensor { fun observe(): Flow<Reading<LocationFix>> }
interface NetworkSensor  { fun observe(): Flow<Reading<NetworkStatus>> }
```

`Flow` đến từ kotlinx-coroutines (Compose đã kéo theo; bài 5 khai báo rõ ràng).

**4.** Thêm `platformName()` như trên và hiện dòng "Đang chạy trên …" ở màn mẫu.

## Kiểm tra

- [ ] **Android** và **iOS** hiện hai dòng "Đang chạy trên …" khác nhau.
- [ ] Xoá `actual` của `iosMain` → build đỏ; trả lại → xanh.
- [ ] `batteryLevel(BatteryInfo(15, ChargingState.Discharging))` trả `Low`; `latencyGrade(300)` trả `Slow`.
- [ ] Nói được bằng lời của mình: vì sao `BatterySensor` là interface (gợi ý: bản giả khi test, `Context` của Android).

## Lỗi hay gặp

- Đặt `expect`/`actual` khác package → không khớp.
- Lạm dụng `expect class` cho mọi thứ → khó test. Dùng interface.
- Để exception từ API nền tảng lọt ra `Flow` → UI crash. Bắt và đổi thành `Reading.Error`.

## Tiếp theo?

- [Bài 5: thêm thư viện](05-dependencies.md).
- Đọc thêm: [Use platform-specific APIs](https://kotlinlang.org/docs/multiplatform/multiplatform-connect-to-apis.html) · [Hierarchical project structure](https://kotlinlang.org/docs/multiplatform/multiplatform-hierarchy.html).

## Docs gốc

[Share code between platforms](https://kotlinlang.org/docs/multiplatform/multiplatform-share-on-platforms.html) · [Expected and actual declarations](https://kotlinlang.org/docs/multiplatform/multiplatform-expect-actual.html)
