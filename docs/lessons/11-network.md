# Mạng và độ trễ HTTP

Card thứ ba của Device Pulse cho biết máy đang dùng Wi-Fi hay di động, có ra được Internet không, và một request HTTP mất bao lâu.
Trạng thái mạng đọc bằng API gốc từng nền tảng; phần đo độ trễ viết **một lần** trong `commonMain` bằng Ktor.

## Mục tiêu

- `AndroidNetworkSensor` và `IosNetworkSensor` phát `Reading<NetworkStatus>` mỗi khi mạng đổi.
- `LatencyProbe` đo thời gian một request tới `https://www.gstatic.com/generate_204`.
- Nút "Đo độ trễ" (Compose và SwiftUI) gọi `PulseMonitor.measureLatency()`, card hiện số ms và `latencyGrade`.

## Android: `ConnectivityManager`

Thêm vào manifest: `ACCESS_NETWORK_STATE` (đọc trạng thái) và `INTERNET` (cho Ktor).

```kotlin
// androidMain
class AndroidNetworkSensor(private val context: Context) : NetworkSensor {
    override fun observe(): Flow<Reading<NetworkStatus>> = callbackFlow {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        if (cm.activeNetwork == null) trySend(Reading.Available(NetworkStatus(ConnectionType.None, isOnline = false)))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val type = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.Wifi
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.Cellular
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.Ethernet
                    else -> ConnectionType.Unknown
                }
                val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                trySend(Reading.Available(NetworkStatus(type, online)))
            }
            override fun onLost(network: Network) {
                trySend(Reading.Available(NetworkStatus(ConnectionType.None, isOnline = false)))
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }
}
```

`NET_CAPABILITY_VALIDATED` nghĩa là Android đã thử và **thật sự** ra được Internet — Wi-Fi quán cà phê chưa đăng nhập sẽ không có cờ này.

## iOS: `nw_path_monitor` của Network framework

```kotlin
// iosMain
import platform.Network.*
import platform.darwin.dispatch_get_main_queue

class IosNetworkSensor : NetworkSensor {
    override fun observe(): Flow<Reading<NetworkStatus>> = callbackFlow {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            val online = nw_path_get_status(path) == nw_path_status_satisfied
            val type = when {
                !online -> ConnectionType.None
                nw_path_uses_interface_type(path, nw_interface_type_wifi) -> ConnectionType.Wifi
                nw_path_uses_interface_type(path, nw_interface_type_cellular) -> ConnectionType.Cellular
                nw_path_uses_interface_type(path, nw_interface_type_wired) -> ConnectionType.Ethernet
                else -> ConnectionType.Unknown
            }
            trySend(Reading.Available(NetworkStatus(type, online)))
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
        awaitClose { nw_path_monitor_cancel(monitor) }
    }
}
```

Monitor gọi handler ngay khi `start` với trạng thái hiện tại, sau đó mỗi lần mạng đổi. Đã `cancel` thì không `start` lại được —
mỗi lần collect tạo monitor mới, như trên.

## Ktor: một `HttpClient`, hai engine

| Thư viện | Source set |
|---|---|
| `ktor-client-core` | `commonMain` |
| `ktor-client-okhttp` | `androidMain` |
| `ktor-client-darwin` | `iosMain` |
| `ktor-client-mock` | `commonTest` (bài 13) |

Engine khác nhau theo nền tảng, nên đưa nó vào qua Koin `platformModule`; phần cấu hình client nằm chung:

```kotlin
// commonMain
fun createHttpClient(engine: HttpClientEngine) = HttpClient(engine) {
    install(HttpTimeout) { requestTimeoutMillis = 5_000 }
}
val commonModule = module {
    single { createHttpClient(get()) }
    single { LatencyProbe(get()) }
}

// androidMain:  single<HttpClientEngine> { OkHttp.create() }   trong platformModule
// iosMain:      single<HttpClientEngine> { Darwin.create() }   trong platformModule
```

## `LatencyProbe`

```kotlin
// commonMain
class LatencyProbe(private val client: HttpClient) {
    suspend fun measure(url: String = "https://www.gstatic.com/generate_204"): Reading<Long> = try {
        val mark = TimeSource.Monotonic.markNow()
        client.head(url)
        Reading.Available(mark.elapsedNow().inWholeMilliseconds)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Reading.Error(e.message ?: "Không gọi được $url")
    }
}
```

- Dùng `TimeSource.Monotonic` (hoặc `measureTime { }`), **không** dùng giờ hệ thống: giờ hệ thống có thể bị chỉnh giữa chừng.
- `generate_204` trả về rỗng với mã 204 — request nhỏ nhất có thể, hợp để đo độ trễ.
- `CancellationException` phải ném lại, để huỷ coroutine vẫn đúng. Mọi lỗi khác (timeout, mất mạng) thành `Reading.Error`.

## Làm trên Device Pulse

Thêm `private val probe: LatencyProbe` vào constructor của `PulseMonitor` và `PulseCore` (bài 8); `single { PulseCore(get(), get(), get(), get()) }` lấy nó từ Koin.

```kotlin
// trong PulseMonitor (shared/commonMain)
private val latency = MutableStateFlow<Reading<Long>?>(null)

fun measureLatency() {
    if (latency.value == Reading.Loading) return      // đang đo thì bỏ qua lần bấm thứ hai
    latency.value = Reading.Loading
    scope.launch { latency.value = probe.measure() }  // scope = viewModelScope (Android) / MainScope (iOS)
}
```

`latency` được `combine` cùng ba sensor vào `state` (trường `latencyMs`). Card Mạng hiện loại kết nối, chấm xanh/xám theo `isOnline`,
nút "Đo độ trễ", và khi có `Reading.Available(ms)` thì hiện `"$ms ms"` kèm nhãn `latencyGrade(ms)`: dưới 100 ms Fast, dưới 300 ms Ok, còn lại Slow.

## Kiểm tra

- [ ] Android emulator: card hiện Wi-Fi hoặc Cellular; bật chế độ máy bay → `None`, không online; tắt → trở lại.
- [ ] Android: bấm "Đo độ trễ" → hiện số ms; khi đang ở chế độ máy bay → hiện lỗi, app không crash.
- [ ] iOS Simulator: card hiện loại mạng của Mac (thường Wi-Fi hoặc Ethernet); tắt Wi-Fi của Mac → `None`.
- [ ] iOS: đo độ trễ ra số ms; nhãn `latencyGrade` đổi đúng ngưỡng.
- [ ] Không còn request nào chạy sau khi rời màn hình (log Kermit không in tiếp).

Kẹt? Xem [Lỗi hay gặp](../reference/troubleshooting.md).

## Tiếp theo?

- [Bài 12: lưu lịch sử với SQLDelight](12-history-sqldelight.md).

## Docs gốc

[Ktor client: multiplatform](https://ktor.io/docs/client-create-multiplatform-application.html) · [Ktor engines](https://ktor.io/docs/client-engines.html) · [Ktor timeout](https://ktor.io/docs/client-timeout.html) · [Monitor connectivity status](https://developer.android.com/training/basics/network-ops/reading-network-state) · [NWPathMonitor](https://developer.apple.com/documentation/network/nwpathmonitor) · [Time measurement](https://kotlinlang.org/docs/time-measurement.html)
