# Design: replace Habit Tick with Device Pulse

Date: 2026-10-14 · Status: approved (user: "go")

## Goal

The learning site's sample app (Habit Tick, a habit tracker) is replaced by **Device Pulse**: one app that reads
**battery, GPS location and network** (plus HTTP latency) on Android and iOS, built with Kotlin Multiplatform and a
shared Compose Multiplatform UI. The word "MVP" is removed from the site; the site is "Học Kotlin Multiplatform".

Site language stays Vietnamese. Lessons contain code snippets only; there is no companion app repository.

## Scope

- Targets: **Android + iOS** (`androidTarget`, `iosArm64`, `iosSimulatorArm64`). Desktop is dropped.
- UI: Compose Multiplatform, one UI for both platforms.
- Out of scope (listed as "làm sau"): background monitoring, notifications, charts, export, widgets, Desktop.

## Shared contract (all lessons use exactly these names)

Project: wizard project `DevicePulse`, module `composeApp`, package `dev.devicepulse`.

```kotlin
// commonMain/kotlin/dev/devicepulse/model/
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
    data object Unsupported : Reading<Nothing>      // e.g. battery on iOS simulator
    data class Error(val message: String) : Reading<Nothing>
}

enum class SensorKind { Battery, Location, Network }

// commonMain/kotlin/dev/devicepulse/rules/  (pure functions, unit-tested in lesson 13)
enum class BatteryLevel { Critical, Low, Ok }          // <=10 Critical, <=20 Low
fun batteryLevel(info: BatteryInfo): BatteryLevel
enum class FixQuality { Good, Fair, Poor }             // <=20 m Good, <=100 m Fair
fun fixQuality(fix: LocationFix): FixQuality
enum class LatencyGrade { Fast, Ok, Slow }             // <100 ms Fast, <300 ms Ok
fun latencyGrade(millis: Long): LatencyGrade

// commonMain/kotlin/dev/devicepulse/sensor/  (implemented in androidMain / iosMain)
interface BatterySensor  { fun observe(): Flow<Reading<BatteryInfo>> }
interface LocationSensor { fun observe(): Flow<Reading<LocationFix>> }
interface NetworkSensor  { fun observe(): Flow<Reading<NetworkStatus>> }

// commonMain — shared, uses Ktor
class LatencyProbe(private val client: HttpClient) {
    suspend fun measure(url: String = "https://www.gstatic.com/generate_204"): Reading<Long>
}

// commonMain — permission, expect/actual composable (lesson 10)
interface PermissionRequester { val granted: Boolean; fun request() }
@Composable expect fun rememberLocationPermission(): PermissionRequester

// commonMain/kotlin/dev/devicepulse/ui/
data class DashboardState(
    val battery: Reading<BatteryInfo> = Reading.Loading,
    val location: Reading<LocationFix> = Reading.Loading,
    val network: Reading<NetworkStatus> = Reading.Loading,
    val latencyMs: Reading<Long>? = null,
)
class DashboardViewModel(
    battery: BatterySensor, location: LocationSensor, network: NetworkSensor,
    private val probe: LatencyProbe, private val history: HistoryRepository,
) : ViewModel() {
    val state: StateFlow<DashboardState>
    fun measureLatency()
    fun saveSnapshot()
}

// Navigation (type-safe, kotlinx.serialization)
@Serializable object DashboardRoute
@Serializable data class DetailRoute(val sensor: SensorKind)
@Serializable object HistoryRoute

// History (lesson 12): SQLDelight database `PulseDb`, table `snapshot`
interface HistoryRepository {
    fun observe(): Flow<List<Snapshot>>
    suspend fun save(state: DashboardState)
}
```

Libraries: kotlinx-coroutines, kotlinx-datetime, kotlinx-serialization, Koin (DI, `commonModule` + `platformModule`),
Kermit (logging), Ktor client (OkHttp engine on Android, Darwin on iOS), SQLDelight, lifecycle-viewmodel-compose,
navigation-compose (multiplatform), Turbine (test).

Platform implementations:

| Sensor | Android (`androidMain`) | iOS (`iosMain`) |
|---|---|---|
| Battery | sticky `ACTION_BATTERY_CHANGED` + `BroadcastReceiver` in `callbackFlow` | `UIDevice.currentDevice.batteryMonitoringEnabled`, level/state notifications; level `-1` → `Unsupported` (simulator) |
| Location | `LocationManager.requestLocationUpdates` (no Play Services) in `callbackFlow` | `CLLocationManager` + Kotlin `NSObject` delegate; `NSLocationWhenInUseUsageDescription` in Info.plist |
| Network | `ConnectivityManager.registerDefaultNetworkCallback` | `nw_path_monitor_*` from `platform.Network` |
| Permission | `rememberLauncherForActivityResult(RequestPermission)` | `CLLocationManager.requestWhenInUseAuthorization` |

Rule: no exception crosses a sensor boundary; every failure becomes `Reading.Error` / `PermissionDenied` / `Unsupported`.

## Lesson plan (files under `docs/lessons/`)

| # | File | App gains |
|---|---|---|
| 1 | `01-what-is-kmp.md` | Why KMP; Device Pulse introduced |
| 2 | `02-setup.md` | Wizard project runs on Android + iOS (keeps the VS Code note) |
| 3 | `03-project-structure.md` | Understand `commonMain` / `androidMain` / `iosMain`, Gradle files |
| 4 | `04-share-code.md` | Models, `Reading`, rules; expect/actual vs interface |
| 5 | `05-dependencies.md` | Coroutines, datetime, Koin, Kermit via version catalog |
| 6 | `06-compose-basics.md` | Dashboard with 3 cards on fake data |
| 7 | `07-resources.md` | Vietnamese / English strings, icons |
| 8 | `08-viewmodel-navigation.md` | `DashboardViewModel`, fake sensors, Dashboard → Detail |
| 9 | `09-battery.md` | Real battery on both platforms |
| 10 | `10-location.md` | Real GPS + permission flow |
| 11 | `11-network.md` | Network status + Ktor latency probe |
| 12 | `12-history-sqldelight.md` | Save snapshots, History screen |
| 13 | `13-testing.md` | Rules tests, fake sensors, ViewModel test with Turbine |
| 14 | `14-build-publish.md` | Release builds, crash reporting, CI |

Lesson page template: `# Title` · intro paragraph · `## Mục tiêu` · concept sections · `## Làm trên Device Pulse` ·
`## Kiểm tra` (checklist, Android + iOS) · `## Tiếp theo?` · `## Docs gốc`.

## Other site changes

- `docs/mvp/` → `docs/project/` (`index.md`, `checklist.md`).
- Home (`overrides/home.html`), `docs/index.md`, `docs/roadmap.md`, `README.md`, `mkdocs.yml` (site name, nav), `hooks/lesson_bar.py` label.
- Fix lesson links in `docs/reference/*` and `docs/plan/kmp-review.md`.
- Verify with `mkdocs build --strict`.
