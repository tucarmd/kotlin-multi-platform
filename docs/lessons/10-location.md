# Vị trí GPS và quyền truy cập

Pin ở [bài 9](09-battery.md) đọc được ngay, còn vị trí thì phải **xin quyền** trước. Bài này viết luồng xin quyền
dùng chung cho cả hai nền tảng, rồi đọc GPS thật bằng API gốc của Android và iOS — không cần Google Play Services.

## Mục tiêu

- Một hàm `rememberLocationPermission()` dùng trong Compose chung, mỗi nền tảng tự cài đặt.
- `AndroidLocationSensor` và `IosLocationSensor` phát `Reading<LocationFix>`; bị từ chối quyền thì ra `Reading.PermissionDenied`.
- Màn Detail hiện độ chính xác và `fixQuality`.

## Quyền: `expect` composable

Xin quyền là việc của UI, nên khai báo một composable `expect` trong `commonMain`:

```kotlin
// commonMain
interface PermissionRequester { val granted: Boolean; fun request() }

@Composable
expect fun rememberLocationPermission(): PermissionRequester
```

**Android** dùng Activity Result API:

```kotlin
// androidMain
@Composable
actual fun rememberLocationPermission(): PermissionRequester {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val current = granted
    return remember(current) {
        object : PermissionRequester {
            override val granted = current
            override fun request() = launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
```

Khai báo trong `androidMain/AndroidManifest.xml`: `<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />`
và `ACCESS_COARSE_LOCATION` (Android 12+ cho người dùng chọn vị trí gần đúng).

**iOS** hỏi qua `CLLocationManager`. Kết quả về qua delegate, nên giữ trạng thái trong một `mutableStateOf`:

```kotlin
// iosMain
@Composable
actual fun rememberLocationPermission(): PermissionRequester = remember { IosPermissionHolder() }

private class IosPermissionHolder : PermissionRequester {
    private val manager = CLLocationManager()
    private var status by mutableStateOf(manager.authorizationStatus)
    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            status = manager.authorizationStatus
        }
    }
    init { manager.delegate = delegate }

    override val granted: Boolean
        get() = status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways
    override fun request() = manager.requestWhenInUseAuthorization()
}
```

Thiếu khoá `NSLocationWhenInUseUsageDescription` trong `iosApp/iosApp/Info.plist` thì iOS **không hiện hộp thoại**:

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>Device Pulse hiện vị trí và độ chính xác GPS của máy.</string>
```

!!! warning "Giữ tham chiếu tới delegate"
    `CLLocationManager.delegate` là tham chiếu *weak*: delegate không được lưu vào biến/field sẽ bị GC dọn và callback không bao giờ tới.

## Android: `LocationManager` trong `callbackFlow`

```kotlin
// androidMain
class AndroidLocationSensor(private val context: Context) : LocationSensor {
    override fun observe(): Flow<Reading<LocationFix>> = callbackFlow {
        val manager = context.getSystemService(LocationManager::class.java)
        val listener = LocationListener { loc -> trySend(Reading.Available(loc.toFix())) }
        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2_000L, 0f, listener, Looper.getMainLooper())
        } catch (e: SecurityException) {
            trySend(Reading.PermissionDenied)
        }
        awaitClose { manager.removeUpdates(listener) }
    }
}

private fun Location.toFix() = LocationFix(
    latitude = latitude, longitude = longitude,
    accuracyMeters = accuracy.toDouble(),
    speedMps = if (hasSpeed()) speed.toDouble() else null,
    timestamp = Instant.fromEpochMilliseconds(time),
)
```

`awaitClose` chạy khi không còn ai collect — đúng chỗ để `removeUpdates`, tránh hao pin.

## iOS: `CLLocationManager` + delegate Kotlin

```kotlin
// iosMain
@OptIn(ExperimentalForeignApi::class)
class IosLocationSensor : LocationSensor {
    override fun observe(): Flow<Reading<LocationFix>> = callbackFlow {
        val manager = CLLocationManager()
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val loc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                val (lat, lon) = loc.coordinate.useContents { latitude to longitude }
                trySend(Reading.Available(LocationFix(
                    latitude = lat, longitude = lon,
                    accuracyMeters = loc.horizontalAccuracy,
                    speedMps = loc.speed.takeIf { it >= 0 },
                    timestamp = Instant.fromEpochMilliseconds((loc.timestamp.timeIntervalSince1970 * 1000).toLong()),
                )))
            }
            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                trySend(Reading.Error(didFailWithError.localizedDescription))
            }
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                when (manager.authorizationStatus) {
                    kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> trySend(Reading.PermissionDenied)
                    else -> manager.startUpdatingLocation()
                }
            }
        }
        manager.delegate = delegate            // biến `delegate` sống tới awaitClose
        manager.desiredAccuracy = kCLLocationAccuracyBest
        awaitClose { manager.stopUpdatingLocation(); manager.delegate = null }
    }
}
```

`coordinate` là C struct (`CValue<CLLocationCoordinate2D>`) → đọc bằng `useContents`; `speed < 0` nghĩa là không rõ tốc độ → `null`.
Gán delegate xong, iOS gọi `locationManagerDidChangeAuthorization` ngay một lần, nên `startUpdatingLocation` nằm trong đó.

## Làm trên Device Pulse

1. Thêm `AndroidLocationSensor` vào `platformModule` Android, `IosLocationSensor` vào `platformModule` iOS (Koin, [bài 5](05-dependencies.md)).
2. Trên card Vị trí: nếu `!permission.granted` thì hiện nút "Cho phép vị trí" gọi `permission.request()`.
   iOS tự bắt đầu nhận vị trí nhờ `locationManagerDidChangeAuthorization`; Android thì flow đã phát `PermissionDenied`
   sẽ dừng, nên khi `granted` đổi sang `true` cần collect lại `observe()`.
3. Màn Detail của `SensorKind.Location`: vĩ độ, kinh độ, `accuracyMeters`, tốc độ (nếu có) và nhãn `fixQuality(fix)` — Good / Fair / Poor.

Giả lập vị trí khi test: Android emulator → nút `⋯` → *Extended Controls* → *Location* (chọn điểm rồi *Set location*, hoặc phát route GPX);
iOS Simulator → menu *Features* → *Location* → *Apple* / *City Run* / *Custom Location…*.

## Kiểm tra

- [ ] Android: lần đầu mở app hiện hộp thoại quyền; cho phép → card hiện toạ độ đặt trong Extended Controls.
- [ ] Android: từ chối quyền → card hiện "Chưa có quyền", app không crash.
- [ ] iOS Simulator: hộp thoại hiện đúng chuỗi trong Info.plist; chọn *City Run* → toạ độ và tốc độ đổi liên tục.
- [ ] iOS: *Settings → Privacy → Location* tắt quyền app → card chuyển sang `PermissionDenied`.
- [ ] Màn Detail hiện `fixQuality` khớp với độ chính xác (≤ 20 m Good, ≤ 100 m Fair).

## Tiếp theo?

[Bài 11: mạng và độ trễ HTTP](11-network.md). Kẹt ở bài này? Xem [Lỗi hay gặp](../reference/troubleshooting.md).

## Docs gốc

[Request location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions) · [LocationManager](https://developer.android.com/reference/android/location/LocationManager) · [Request runtime permissions](https://developer.android.com/training/permissions/requesting) · [CLLocationManager](https://developer.apple.com/documentation/corelocation/cllocationmanager) · [Requesting authorization for location services](https://developer.apple.com/documentation/corelocation/requesting-authorization-to-use-location-services) · [Interoperability with Swift/Objective-C](https://kotlinlang.org/docs/native-objc-interop.html) · [Expected and actual declarations](https://kotlinlang.org/docs/multiplatform/multiplatform-expect-actual.html)
