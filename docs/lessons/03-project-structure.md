# Cấu trúc project

Trước khi viết tính năng, đọc hiểu project wizard vừa sinh. KMP chỉ có hai khái niệm cần nắm chắc: **target** và **source set**.

## Mục tiêu

- Phân biệt target và source set.
- Đọc được `build.gradle.kts` và `gradle/libs.versions.toml`.
- Biết đặt từng file của Device Pulse vào thư mục nào.

## Cây thư mục

```text
apps/DevicePulse/
├── shared/                      # 🧩 Kotlin Multiplatform — KHÔNG có UI
│   ├── build.gradle.kts        # target + thư viện của module, framework "Shared" cho iOS
│   └── src/
│       ├── commonMain/          # code chung: model, rules, interface cảm biến, PulseMonitor (state)
│       ├── androidMain/         # cài đặt cảm biến bằng Android SDK
│       ├── iosMain/             # cài đặt cảm biến bằng API Apple (Kotlin/Native)
│       └── commonTest/          # test chạy trên mọi target
├── androidApp/                  # 🤖 Kotlin + Jetpack Compose: MainActivity, màn hình, strings.xml
├── iosApp/                      # 🍎 Swift + SwiftUI: project Xcode, View, Localizable.strings — import Shared
├── gradle/libs.versions.toml    # version catalog: phiên bản thư viện, plugin
├── build.gradle.kts             # cấu hình gốc (khai báo plugin)
└── settings.gradle.kts          # danh sách module: include(":shared", ":androidApp")
```

Quy tắc đặt file: **không gọi SDK và không vẽ** → `shared/commonMain`. **Gọi SDK** → `shared/androidMain` / `iosMain`.
**Vẽ màn hình, xin quyền, chuỗi hiển thị** → `androidApp` / `iosApp`.

!!! note "Wizard cũ đặt tên khác"
    Wizard bản cũ đặt app Android là `composeApp/`. Đổi tên thành `androidApp/` cho giống code mẫu, hoặc giữ nguyên rồi
    đọc `composeApp` thay cho `androidApp` trong các bài. Giải thích các thư mục `.gradle`, `.idea`, `.kotlin`, `build`,
    `local.properties`: xem [Cây thư mục của code mẫu](../project/source-code.md#cay-thu-muc).

## Target — "build ra cho máy nào"

Target là một nền tảng đích. Device Pulse có ba:

- `androidTarget()` — Android.
- `iosArm64()` — iPhone thật.
- `iosSimulatorArm64()` — simulator trên Mac chip Apple.

Khai báo trong khối `kotlin { }` của `shared/build.gradle.kts`. `androidApp` là app Android thường (plugin `com.android.application`), không có target KMP.

## Source set — "code này dùng cho máy nào"

| Source set | Biên dịch cho | Được dùng gì |
|---|---|---|
| `commonMain` | mọi target | chỉ Kotlin chung + thư viện multiplatform |
| `androidMain` | Android | thêm Android SDK (`Context`, `BatteryManager`, `LocationManager`…) |
| `iosMain` | mọi target iOS | thêm API Apple (`platform.UIKit`, `platform.CoreLocation`, `platform.Network`) |
| `commonTest` | test chạy trên mọi target | `kotlin.test` |

## Hierarchy — source set lồng nhau

KMP tự dựng cây source set theo **default hierarchy template**: `iosArm64Main` và `iosSimulatorArm64Main` cùng nằm dưới `iosMain`,
`iosMain` nằm dưới `appleMain` → `nativeMain` → `commonMain`. Nhờ vậy bạn viết code iOS **một lần** trong `iosMain`,
không phải chép cho từng kiến trúc chip.

```mermaid
flowchart BT
  iosArm64Main --> iosMain
  iosSimulatorArm64Main --> iosMain
  iosMain --> appleMain --> nativeMain --> commonMain
  androidMain --> commonMain
```

## Đọc `build.gradle.kts`

Khung thường thấy (rút gọn — file thật của bạn có thể khác tên alias):

```kotlin
kotlin {
    androidTarget()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"       // Swift: import Shared
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies { /* thư viện dùng chung */ }
        androidMain.dependencies { /* chỉ Android */ }
        iosMain.dependencies { /* chỉ iOS */ }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
```

`gradle/libs.versions.toml` giữ phiên bản; Gradle sinh ra accessor `libs.…` để dùng ở trên. Bài 5 thêm thư viện qua file này.

## Làm trên Device Pulse

Tạo sẵn các package trong `shared/src/commonMain/kotlin/dev/devicepulse/`:

| Package | Chứa gì | Bài |
|---|---|---|
| `model/` | `BatteryInfo`, `LocationFix`, `NetworkStatus`, `Reading`, `SensorKind` | 4 |
| `rules/` | hàm thuần `batteryLevel`, `fixQuality`, `latencyGrade` | 4 |
| `sensor/` | interface `BatterySensor`, `LocationSensor`, `NetworkSensor` | 4 |
| `core/` | `PulseMonitor` (state chung cho 2 UI), `PulseCore` | 8 |
| `data/` | `LatencyProbe`, `HistoryRepository` | 11–12 |

Màn hình **không** nằm trong `shared/`: Compose ở `androidApp/src/main/kotlin/dev/devicepulse/android/ui/`, SwiftUI ở `iosApp/iosApp/Views/` (bài 6).

Code cài đặt cảm biến thật sẽ nằm trong `androidMain/kotlin/dev/devicepulse/sensor/` và `iosMain/kotlin/dev/devicepulse/sensor/`.

Thử nhanh: tạo file tạm trong `commonMain/.../model/`, gõ `import android.content.Context` — IDE báo đỏ.
Đó là `commonMain` đang bảo vệ bạn. Xoá file tạm đi.

## Kiểm tra

- [ ] Chỉ ra được dòng khai báo `androidTarget()` và hai target iOS trong Gradle.
- [ ] Giải thích được vì sao `LocationFix` nằm ở `commonMain`, còn code gọi `CLLocationManager` nằm ở `iosMain`.
- [ ] Sau khi tạo package, app vẫn build và chạy trên **Android** và **iOS**.

## Tiếp theo?

- [Bài 4: chia sẻ code giữa các nền tảng](04-share-code.md).
- Đọc thêm: [Module structure recommendations](https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html) · [Project organization options](https://kotlinlang.org/docs/multiplatform/multiplatform-project-configuration.html).

## Docs gốc

[Targets and source sets](https://kotlinlang.org/docs/multiplatform/multiplatform-discover-project.html) · [Source set hierarchy](https://kotlinlang.org/docs/multiplatform/multiplatform-hierarchy.html) · [Dependencies between source sets](https://kotlinlang.org/docs/multiplatform/multiplatform-advanced-project-structure.html)
