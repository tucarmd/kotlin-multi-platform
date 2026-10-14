# Device Pulse — app mẫu Kotlin Multiplatform

App đo nhanh "sức khỏe" điện thoại: **pin, vị trí GPS, mạng + độ trễ HTTP**, chạy **Android + iOS** với **một UI chung** viết bằng
Compose Multiplatform. Đây là bản code hoàn chỉnh của app mà 14 bài trên site [Học Kotlin Multiplatform](../README.md) xây dần.

| | Android | iOS |
|---|---|---|
| Dashboard | Pin thật, GPS, Wi-Fi/di động, đo độ trễ | Giống hệt (cùng code Compose) |
| Pin | `ACTION_BATTERY_CHANGED` | `UIDevice` (Simulator → "Không hỗ trợ") |
| Vị trí | `LocationManager` (không cần Play Services) | `CLLocationManager` + delegate Kotlin |
| Mạng | `ConnectivityManager` | `nw_path_monitor` |
| Lịch sử | SQLDelight (`AndroidSqliteDriver`) | SQLDelight (`NativeSqliteDriver`) |

## Chạy

Cần: JDK 17+ (đã thử JDK 21), Android SDK 36, Xcode 16+ (đã thử 26.5).

```bash
# Android: build + cài lên emulator/máy đang cắm
./gradlew :composeApp:installDebug

# Test chung (rules, ViewModel, LatencyProbe) trên JVM và iOS Simulator
./gradlew :composeApp:testDebugUnitTest :composeApp:iosSimulatorArm64Test

# iOS: mở project rồi bấm Run (bước build tự gọi Gradle để dựng ComposeApp.framework)
open iosApp/iosApp.xcodeproj
```

Chạy iOS trên máy thật: chọn Team ở tab *Signing & Capabilities* của target `iosApp`.
Project Xcode sinh từ `iosApp/project.yml` bằng [XcodeGen](https://github.com/yonaskolb/XcodeGen) — sửa file yml rồi chạy
`xcodegen generate` trong `iosApp/` thay vì sửa `.pbxproj` bằng tay.

Thử cảm biến trên emulator Android: `adb emu geo fix 106.7009 10.7769` (vị trí), `adb emu power capacity 15` (pin).
iOS Simulator: `xcrun simctl location booted set 10.7769,106.7009`.

## Cấu trúc

```text
composeApp/src/
├── commonMain/kotlin/dev/devicepulse/
│   ├── model/    BatteryInfo, LocationFix, NetworkStatus, Reading (5 trạng thái)
│   ├── rules/    batteryLevel · fixQuality · latencyGrade (hàm thuần, có test)
│   ├── sensor/   interface 3 cảm biến, LatencyProbe (Ktor), expect rememberLocationPermission
│   ├── data/     HistoryRepository (SQLDelight)
│   ├── di/       Koin: commonModule + expect platformModule
│   └── ui/       App (NavHost), Dashboard, Detail, History, DashboardViewModel
├── commonMain/sqldelight/      Snapshot.sq
├── commonMain/composeResources/ strings en + vi
├── androidMain/  MainActivity, 3 cảm biến Android, permission, platformModule
├── iosMain/      MainViewController, 3 cảm biến iOS, permission, platformModule
└── commonTest/   RulesTest, LatencyProbeTest, DashboardViewModelTest (Turbine)
iosApp/           SwiftUI vỏ bọc + project.yml (XcodeGen)
```

## Version và vì sao

Kotlin 2.4.20 · Compose Multiplatform 1.11.1 · AGP 8.13.2 · Ktor 3.5.1 · Koin 4.2.2 · SQLDelight 2.4.1 · Navigation 2.9.2.

CMP 1.12 / Lifecycle 2.11 / Ktor 3.6 kéo theo thư viện Jetpack đòi **AGP 9.1 + compileSdk 37**, mà AGP 9 bắt tách module
thành `:shared` + `:androidApp` (không còn `androidTarget()` + `com.android.application` chung một module). App giữ một module
`composeApp` cho dễ học, nên đứng lùi một bậc version. Ghi chú ngay trong `gradle/libs.versions.toml`.

## Mấy cái bẫy đã gặp khi làm

- **Route điều hướng có tham số enum làm crash iOS** (`could not find any NavType for argument sensor`): iOS không có
  reflection để tự suy `NavType` cho enum. `DetailRoute` vì vậy mang `sensorName: String`.
- **Link lỗi `_sqlite3_*` trên iOS**: framework là static, nên `-lsqlite3` phải thêm vào `OTHER_LDFLAGS` của app, không chỉ ở
  `linkerOpts` của Gradle.
- **iOS chỉ hỏi quyền vị trí một lần**: sau "Don't Allow" chỉ đổi được trong Settings — `PermissionRequester.canPrompt` cho UI
  biết khi nào nút "Cấp quyền" nên mở Settings thay vì hiện hộp thoại.
