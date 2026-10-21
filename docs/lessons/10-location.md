# Vị trí GPS và quyền truy cập

Pin ở [bài 9](09-battery.md) đọc được ngay, còn vị trí thì phải **xin quyền** trước. Bài này viết phần xin quyền
ở UI của từng app, rồi đọc GPS thật bằng API gốc của Android và iOS — không cần Google Play Services.

## Mục tiêu

- Xin quyền ở tầng UI của từng app: `rememberLocationPermission()` (Compose) và `LocationPermission` (Swift).
- `AndroidLocationSensor` và `IosLocationSensor` phát `Reading<LocationFix>`; bị từ chối quyền thì ra `Reading.PermissionDenied`.
- Màn Detail hiện độ chính xác và `fixQuality`.

## Quyền: việc của UI, viết ở từng app

Hộp thoại xin quyền gắn với màn hình (Activity bên Android, view đang hiện bên iOS), nên phần **hỏi quyền** nằm ở
`androidApp` / `iosApp`, viết bằng ngôn ngữ của từng bên. Phần **đọc vị trí** vẫn ở `shared/` (Kotlin, `androidMain` /
`iosMain`): cảm biến tự kiểm tra quyền và phát `Reading.PermissionDenied` khi chưa có.

=== "Android · Kotlin + Compose"

    ```kotlin
    // androidApp/.../ui/LocationPermission.kt
    private val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    class LocationPermission internal constructor(
        private val isGranted: () -> Boolean,
        private val launch: () -> Unit,
    ) {
        val granted: Boolean get() = isGranted()      // đọc trong composable → tự vẽ lại khi người dùng trả lời
        fun request() = launch()
    }

    @Composable
    fun rememberLocationPermission(): LocationPermission {
        val context = LocalContext.current
        var isGranted by remember {
            mutableStateOf(LOCATION_PERMISSIONS.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED })
        }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            isGranted = result.values.any { it }
        }
        return remember(launcher) { LocationPermission({ isGranted }, { launcher.launch(LOCATION_PERMISSIONS) }) }
    }
    ```

    Khai báo trong `androidApp/src/main/AndroidManifest.xml`: `ACCESS_FINE_LOCATION` và `ACCESS_COARSE_LOCATION`
    (Android 12+ cho người dùng chọn vị trí gần đúng).

=== "iOS · Swift"

    ```swift
    // iosApp/iosApp/LocationPermission.swift
    import CoreLocation
    import Observation
    import UIKit

    @MainActor @Observable
    final class LocationPermission: NSObject, CLLocationManagerDelegate {
        private let manager = CLLocationManager()
        private(set) var status: CLAuthorizationStatus = .notDetermined

        override init() {
            super.init()
            status = manager.authorizationStatus
            manager.delegate = self
        }

        var granted: Bool { status == .authorizedWhenInUse || status == .authorizedAlways }
        var canPrompt: Bool { status == .notDetermined }      // iOS chỉ hỏi MỘT lần

        func request() {
            if canPrompt {
                manager.requestWhenInUseAuthorization()
            } else if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url)                  // đã từ chối → chỉ đổi được trong Settings
            }
        }

        nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
            let status = manager.authorizationStatus
            Task { @MainActor in self.status = status }
        }
    }
    ```

Cầu nối với lõi: khi quyền vừa được cấp, gọi `monitor.refreshLocation()` để luồng vị trí đăng ký lại (luồng cũ đã phát
`PermissionDenied` và dừng). Thêm vào `PulseMonitor`:

```kotlin
private val locationRestarts = MutableStateFlow(0)
// trong combine: locationRestarts.flatMapLatest { location.observe() } thay cho location.observe()
fun refreshLocation() { locationRestarts.value++ }
```

=== "Android"

    ```kotlin
    val permission = rememberLocationPermission()
    LaunchedEffect(Unit) { if (!permission.granted) permission.request() }
    LaunchedEffect(permission.granted) { if (permission.granted) monitor.refreshLocation() }
    ```

=== "iOS"

    ```swift
    // PulseModel thêm: func refreshLocation() { monitor.refreshLocation() }
    NavigationStack { … }
        .task { if permission.canPrompt { permission.request() } }
        .onChange(of: permission.granted) { _, granted in
            if granted { model.refreshLocation() }
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
2. Thẻ Vị trí ở trạng thái `PermissionDenied` hiện nút "Cho phép vị trí" gọi `permission.request()` (Compose và SwiftUI).
   Quyền vừa được cấp → `monitor.refreshLocation()` như trên.
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
