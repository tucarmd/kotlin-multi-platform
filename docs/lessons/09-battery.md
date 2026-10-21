# Đọc pin thật trên Android và iOS

Tới đây thẻ Pin vẫn chạy bằng `FakeBatterySensor`. Bài này viết code nền tảng đầu tiên: `AndroidBatterySensor` trong
`androidMain` và `IosBatterySensor` trong `iosMain`, cùng cài interface `BatterySensor` ở `commonMain`, rồi cắm vào Koin.
`PulseMonitor` và cả hai UI không sửa một dòng.

## Mục tiêu

- Biến API dạng callback của từng nền tảng thành `Flow` bằng `callbackFlow` + `awaitClose`.
- Đổi dữ liệu thô (Intent extra, `UIDevice`) về `BatteryInfo` / `ChargingState` chung.
- Không để exception nào vượt qua ranh giới sensor: mọi lỗi thành `Reading.Error` / `Reading.Unsupported`.
- Ghi log bằng Kermit.

## Interface thay vì expect/actual

`BatterySensor` là interface thường; mỗi nền tảng có một class cài nó, Koin chọn class nào. So với `expect class`:
dễ thay bằng bản giả khi test (bài 13), constructor mỗi nền tảng được khác nhau (Android cần `Context`, iOS không).
`expect/actual` chỉ dùng cho `platformModule` — chỗ duy nhất phải biết nền tảng.

## callbackFlow

```kotlin
callbackFlow {
    val listener = /* đăng ký với hệ điều hành, mỗi lần có dữ liệu thì trySend(x) */
    awaitClose { /* huỷ đăng ký — chạy khi người thu dừng (WhileSubscribed hết 5 giây, màn đóng) */ }
}
```

Quên `awaitClose` → flow kết thúc ngay và báo lỗi; quên huỷ đăng ký trong đó → rò bộ nhớ, tốn pin.

## Android: sticky broadcast

Hệ thống phát `ACTION_BATTERY_CHANGED` mỗi khi pin đổi, và giữ bản cuối (*sticky*): `registerReceiver` trả ngay
`Intent` gần nhất nên không phải chờ lần đổi sau.

```kotlin
// androidMain/kotlin/dev/devicepulse/sensor/AndroidBatterySensor.kt
class AndroidBatterySensor(context: Context) : BatterySensor {
    private val appContext = context.applicationContext
    private val log = Logger.withTag("Battery")

    override fun observe(): Flow<Reading<BatteryInfo>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(intent.toReading())
            }
        }
        val sticky = ContextCompat.registerReceiver(
            appContext, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { trySend(it.toReading()) }
        awaitClose { log.d { "stop" }; appContext.unregisterReceiver(receiver) }
    }
        .distinctUntilChanged()
        .onEach { log.d { "reading=$it" } }
        .catch { e ->
            log.e(e) { "battery failed" }
            emit(Reading.Error(e.message ?: "battery error"))
        }

    private fun Intent.toReading(): Reading<BatteryInfo> {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return Reading.Unsupported
        val charging = when (getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> ChargingState.Charging
            BatteryManager.BATTERY_STATUS_FULL -> ChargingState.Full
            BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargingState.Discharging
            else -> ChargingState.Unknown
        }
        return Reading.Available(BatteryInfo(levelPercent = level * 100 / scale, charging = charging))
    }
}
```

`distinctUntilChanged` bỏ bản trùng (receiver cũng nhận lại chính Intent sticky ngay khi đăng ký). Không cần quyền nào trong Manifest.

## iOS: UIDevice + NSNotificationCenter

iOS chỉ cập nhật pin khi bật `batteryMonitoringEnabled`. `batteryLevel` là `Float` 0..1, hoặc `-1` khi không đọc được —
**luôn luôn như vậy trên iOS Simulator** (Simulator không có pin), nên ta trả `Reading.Unsupported` thay vì báo lỗi.

```kotlin
// iosMain/kotlin/dev/devicepulse/sensor/IosBatterySensor.kt
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.*

class IosBatterySensor : BatterySensor {
    private val log = Logger.withTag("Battery")

    override fun observe(): Flow<Reading<BatteryInfo>> = callbackFlow {
        val device = UIDevice.currentDevice
        device.batteryMonitoringEnabled = true
        val center = NSNotificationCenter.defaultCenter
        val observers = listOf(
            UIDeviceBatteryLevelDidChangeNotification,
            UIDeviceBatteryStateDidChangeNotification,
        ).map { name ->
            center.addObserverForName(name, `object` = null, queue = NSOperationQueue.mainQueue) { _ ->
                trySend(device.toReading())
            }
        }
        trySend(device.toReading())
        awaitClose {
            log.d { "stop" }
            observers.forEach { center.removeObserver(it) }
            device.batteryMonitoringEnabled = false
        }
    }
        .distinctUntilChanged()
        .onEach { log.d { "reading=$it" } }
        .catch { e -> log.e(e) { "battery failed" }; emit(Reading.Error(e.message ?: "battery error")) }

    private fun UIDevice.toReading(): Reading<BatteryInfo> {
        if (batteryLevel < 0f) return Reading.Unsupported
        val charging = when (batteryState) {
            UIDeviceBatteryState.UIDeviceBatteryStateCharging -> ChargingState.Charging
            UIDeviceBatteryState.UIDeviceBatteryStateFull -> ChargingState.Full
            UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> ChargingState.Discharging
            else -> ChargingState.Unknown
        }
        return Reading.Available(BatteryInfo((batteryLevel * 100).roundToInt(), charging))
    }
}
```

`batteryLevel` trên iPhone thật làm tròn theo bước 5% và thông báo đổi mức không đến ngay từng phần trăm — đó là hành vi của iOS, không phải lỗi.

## Làm trên Device Pulse

1. Tạo hai file ở trên (Kermit đã có từ bài 5); xoá dòng `single<BatterySensor> { FakeBatterySensor() }` khỏi `commonModule`.
2. Đăng ký trong `platformModule`:

=== "androidMain"

    ```kotlin
    actual val platformModule = module {
        single<BatterySensor> { AndroidBatterySensor(androidContext()) }
    }
    ```

    `androidContext()` có được nhờ `initKoin { androidContext(this@DevicePulseApp) }` ở bài 5.

=== "iosMain"

    ```kotlin
    actual val platformModule = module {
        single<BatterySensor> { IosBatterySensor() }
    }
    ```

Chạy app, mở Logcat (lọc tag `Battery`) hoặc console Xcode để thấy log Kermit.

## Kiểm tra

- [ ] Android emulator: **⋯ → Extended Controls → Battery**, kéo *Charge level* xuống 15% → thẻ Pin đổi theo trong vài giây.
- [ ] Android: đổi *Charger connection* sang *None* → `Đang dùng pin`; *AC charger* → `Đang sạc`. (Máy thật: `adb shell dumpsys battery set level 15`, xong thì `adb shell dumpsys battery reset`.)
- [ ] Android: Logcat có dòng `Battery` với từng reading, không trùng lặp.
- [ ] iOS Simulator: thẻ Pin hiện "Máy này không hỗ trợ" (`Unsupported`), app không crash.
- [ ] iPhone thật: hiện đúng % pin; cắm / rút sạc → trạng thái đổi.
- [ ] Rời app 10 giây rồi quay lại: log có dòng `stop` rồi reading mới (nhờ `WhileSubscribed`).

Chạy lên iPhone thật cần ký app — xem [xử lý sự cố](../reference/troubleshooting.md).

## Tiếp theo?

- [Bài 10: vị trí GPS và xin quyền](10-location.md).
- Đọc thêm: [Kermit](https://kermit.touchlab.co/) · [`callbackFlow`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html) · [Monitor battery level (Android)](https://developer.android.com/training/monitoring-device-state/battery-monitoring).

## Docs gốc

[Platform-specific APIs](https://kotlinlang.org/docs/multiplatform/multiplatform-connect-to-apis.html) · [iOS interop với Kotlin/Native](https://kotlinlang.org/docs/native-objc-interop.html) · [BatteryManager](https://developer.android.com/reference/android/os/BatteryManager) · [UIDevice](https://developer.apple.com/documentation/uikit/uidevice) · [batteryLevel](https://developer.apple.com/documentation/uikit/uidevice/batterylevel)
