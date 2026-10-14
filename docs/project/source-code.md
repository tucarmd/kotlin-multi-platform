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
