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

./gradlew :composeApp:installDebug                                          # Android: cài lên emulator / máy đang cắm
./gradlew :composeApp:testDebugUnitTest :composeApp:iosSimulatorArm64Test   # test chung trên JVM + iOS Simulator
open iosApp/iosApp.xcodeproj                                                # iOS: bấm Run trong Xcode
```

Cần JDK 17+, Android SDK 36, Xcode 16+. Chi tiết và lệnh giả lập cảm biến:
[`DevicePulse/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/README.md).

## Bài nào, xem file nào

Đường dẫn tính từ `DevicePulse/composeApp/src/`.

| Bài | File trong code mẫu |
|---|---|
| [3. Cấu trúc project](../lessons/03-project-structure.md) | `DevicePulse/settings.gradle.kts`, `composeApp/build.gradle.kts`, `gradle/libs.versions.toml` |
| [4. Chia sẻ code](../lessons/04-share-code.md) | `commonMain/.../model/Models.kt`, `commonMain/.../rules/Rules.kt` |
| [5. Thêm thư viện](../lessons/05-dependencies.md) | `commonMain/.../di/Koin.kt`, `androidMain/.../DevicePulseApp.kt`, `*/di/PlatformModule.*.kt` |
| [6. Compose cơ bản](../lessons/06-compose-basics.md) | `commonMain/.../ui/DashboardScreen.kt`, `ui/Components.kt` |
| [7. Resources](../lessons/07-resources.md) | `commonMain/composeResources/values/strings.xml`, `values-vi/strings.xml` |
| [8. ViewModel và điều hướng](../lessons/08-viewmodel-navigation.md) | `ui/DashboardViewModel.kt`, `ui/App.kt`, `ui/Routes.kt` |
| [9. Đọc pin](../lessons/09-battery.md) | `commonMain/.../sensor/Sensors.kt`, `AndroidBatterySensor.kt`, `IosBatterySensor.kt` |
| [10. GPS và xin quyền](../lessons/10-location.md) | `sensor/Permission.kt` + `Permission.android.kt` / `.ios.kt`, `AndroidLocationSensor.kt`, `IosLocationSensor.kt` |
| [11. Mạng và độ trễ](../lessons/11-network.md) | `sensor/LatencyProbe.kt`, `AndroidNetworkSensor.kt`, `IosNetworkSensor.kt` |
| [12. Lịch sử với SQLDelight](../lessons/12-history-sqldelight.md) | `commonMain/sqldelight/.../Snapshot.sq`, `data/HistoryRepository.kt`, `ui/HistoryScreen.kt` |
| [13. Test](../lessons/13-testing.md) | `commonTest/.../RulesTest.kt`, `LatencyProbeTest.kt`, `DashboardViewModelTest.kt` |
| [14. Build và phát hành](../lessons/14-build-publish.md) | `DevicePulse/iosApp/project.yml`, `iosApp/iosApp/Info.plist` |

Code mẫu có thể đi trước bài học một chút về phiên bản thư viện. Gặp chỗ khác nhau thì xem mục *Version và vì sao* trong
README của `DevicePulse/`.

## Mockup giao diện

8 màn (Dashboard, 3 màn chi tiết, Lịch sử, dark mode) có prompt và bảng theo dõi ở
[`DevicePulse/design/mockups/`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/design/mockups/README-MOCKUPS.md).
Dùng để đối chiếu khi tự vẽ UI ở bài 6 và bài 8.

## Muốn xem thêm app khác?

Xem [Repo mẫu tham khảo](../reference/sample-repos.md): template chính chủ, đáp án tutorial của JetBrains và các app KMP thật.
