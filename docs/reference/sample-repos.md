# Repo mẫu tham khảo

Đọc code thật là cách nhanh nhất để thấy KMP được dùng thế nào ngoài bài học. Danh sách dưới đây chỉ gồm repo **còn được cập nhật**
(đã kiểm tháng 10/2026). Repo nào đã archive, ví dụ `compose-multiplatform-ios-android-template`, thì không đưa vào.

!!! tip "Đọc thế nào cho đỡ ngợp"
    Đừng đọc hết cả repo. Mở `settings.gradle.kts` + `build.gradle.kts` để xem có những module nào, rồi đi thẳng vào
    `commonMain` tìm đúng thứ bạn đang học (ViewModel, Ktor, SQLDelight…). Mỗi lần chỉ đọc một chủ đề.

## Template để tạo project

| Repo | Dùng để làm gì | Bài |
|---|---|---|
| [Kotlin/KMP-App-Template](https://github.com/Kotlin/KMP-App-Template) | Template chính chủ, **UI chung** bằng Compose Multiplatform: Koin, Ktor, Navigation, ViewModel đã nối sẵn | [2](../lessons/02-setup.md), [3](../lessons/03-project-structure.md) |
| [Kotlin/KMP-App-Template-Native](https://github.com/Kotlin/KMP-App-Template-Native) | Cùng app nhưng UI **native**: Jetpack Compose + SwiftUI, chỉ chia sẻ logic | [1](../lessons/01-what-is-kmp.md) (so sánh 2 cách) |
| [Kotlin/kmp-basic-sample](https://github.com/Kotlin/kmp-basic-sample) | Project tối giản nhất: chỉ có `expect/actual` và một hàm chung | [4](../lessons/04-share-code.md) |
| [Kotlin/multiplatform-library-template](https://github.com/Kotlin/multiplatform-library-template) | Mẫu để viết **thư viện** KMP và đăng lên Maven Central | [14](../lessons/14-build-publish.md) |

## Code nguồn của tutorial chính chủ

Mỗi repo là đáp án của một tutorial trên kotlinlang.org. Làm tutorial xong mà kẹt thì so code với repo này.

| Repo | Tutorial | Bài |
|---|---|---|
| [kotlin-hands-on/get-started-with-cm](https://github.com/kotlin-hands-on/get-started-with-cm) | Get started with Compose Multiplatform | [6](../lessons/06-compose-basics.md), [7](../lessons/07-resources.md) |
| [kotlin-hands-on/kmp-networking-and-data-storage](https://github.com/kotlin-hands-on/kmp-networking-and-data-storage) | Share data access layer: Ktor + SQLDelight + Koin | [5](../lessons/05-dependencies.md), [11](../lessons/11-network.md), [12](../lessons/12-history-sqldelight.md) |
| [Kotlin/kmp-integration-sample](https://github.com/Kotlin/kmp-integration-sample) | Make your Android app cross-platform: đưa app Android có sẵn sang KMP | [1](../lessons/01-what-is-kmp.md) |

## App thật, cỡ vừa

Đọc sau khi đã xong khoảng bài 8, lúc đã quen cấu trúc `commonMain` / `androidMain` / `iosMain`.

| Repo | Đáng xem chỗ nào |
|---|---|
| [android/kotlin-multiplatform-samples](https://github.com/android/kotlin-multiplatform-samples) | Google dùng Jetpack (Room, DataStore, ViewModel) trong KMP. Ví dụ thay thế SQLDelight bằng Room ở [bài 12](../lessons/12-history-sqldelight.md) |
| [JetBrains/compose-multiplatform](https://github.com/JetBrains/compose-multiplatform/tree/master/examples) (thư mục `examples/`) | `nav_cupcake` cho điều hướng, `imageviewer` cho app nhiều màn, `interop` cho nhúng view native vào Compose |
| [Kotlin/kmp-production-sample](https://github.com/Kotlin/kmp-production-sample) | Đọc RSS, **đã lên App Store và Google Play**. Xem cách tổ chức một app KMP thật sự phát hành |
| [touchlab/KaMPKit](https://github.com/touchlab/KaMPKit) | Bộ khởi đầu của Touchlab: Koin, Ktor, SQLDelight, Kermit, test. Kiến trúc rất gần Device Pulse |
| [joreilly/PeopleInSpace](https://github.com/joreilly/PeopleInSpace) | Một logic chung chạy trên **rất nhiều** client: SwiftUI, Compose, Wear OS, Desktop, Web |

## App lớn, đọc để học kiến trúc

| Repo | Đáng xem chỗ nào |
|---|---|
| [JetBrains/kotlinconf-app](https://github.com/JetBrains/kotlinconf-app) | App hội nghị KotlinConf chính thức: UI chung, backend Ktor, resources, đa ngôn ngữ |
| [joreilly/Confetti](https://github.com/joreilly/Confetti) | GraphQL (Apollo), Wear OS, Desktop, iOS. Xem cách tách module khi app lớn dần |

## Android thuần, đọc để so sánh kiến trúc

Không phải KMP. Đưa vào để thấy app Android "kiểu cũ" tổ chức thế nào, rồi so với cách Device Pulse làm.

| Repo | Đáng xem chỗ nào | Lưu ý |
|---|---|---|
| [janishar/android-kotlin-mvp-architecture](https://github.com/janishar/android-kotlin-mvp-architecture) | Kiến trúc **MVP**: tách View / Presenter / Interactor rõ ràng, mỗi màn một package | Ngừng cập nhật từ 01/2024. Dagger2, RxJava2, FastAndroidNetworking **không chạy trên iOS**, nên đừng chép sang KMP. Ở KMP dùng Koin, Coroutines/Flow, Ktor, và **MVVM** với `ViewModel` ([bài 8](../lessons/08-viewmodel-navigation.md)) |

## Kotlin trên web

Template Compose + Wasm, Compose HTML, Kobweb, kotlin-wrappers: xem bảng cuối trang [Kotlin làm web](../plan/kotlin-web.md#repo-va-nguon-e-oc-tiep).

## Thư viện Device Pulse đang dùng

Mở `README` và thư mục `samples/` của từng thư viện khi cần tra API.

| Thư viện | Dùng ở | Bài |
|---|---|---|
| [ktorio/ktor](https://github.com/ktorio/ktor) | Đo độ trễ HTTP (`LatencyProbe`), `MockEngine` khi test | [11](../lessons/11-network.md), [13](../lessons/13-testing.md) |
| [InsertKoinIO/koin](https://github.com/InsertKoinIO/koin) | `commonModule` + `platformModule` | [5](../lessons/05-dependencies.md) |
| [sqldelight/sqldelight](https://github.com/sqldelight/sqldelight) | Bảng `Snapshot`, `HistoryRepository` | [12](../lessons/12-history-sqldelight.md) |
| [Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | `Flow` của cảm biến, `viewModelScope` | [8](../lessons/08-viewmodel-navigation.md) |
| [cashapp/turbine](https://github.com/cashapp/turbine) | Test `Flow` của ViewModel | [13](../lessons/13-testing.md) |
| [yonaskolb/XcodeGen](https://github.com/yonaskolb/XcodeGen) | Sinh `iosApp.xcodeproj` từ `project.yml` | [14](../lessons/14-build-publish.md) |

## Thư viện làm sẵn phần cảm biến

Device Pulse tự viết phần xin quyền và đọc GPS để bạn hiểu từng bước. Làm app thật thì có thể dùng thư viện cho nhanh:

| Thư viện | Thay cho |
|---|---|
| [icerockdev/moko-permissions](https://github.com/icerockdev/moko-permissions) | `rememberLocationPermission` tự viết ở [bài 10](../lessons/10-location.md) |
| [jordond/compass](https://github.com/jordond/compass) | `AndroidLocationSensor` / `IosLocationSensor`, có thêm geocoding |
| [MohamedRejeb/Calf](https://github.com/MohamedRejeb/Calf) | Permission, file picker, WebView, UI tự đổi theo nền tảng |

## Tìm thêm

- [terrakok/kmp-awesome](https://github.com/terrakok/kmp-awesome): danh sách thư viện và công cụ KMP được cộng đồng chọn lọc. Muốn có thư viện cho X thì tìm ở đây trước.
- [KMP samples](https://kotlinlang.org/docs/multiplatform/multiplatform-samples.html): trang JetBrains liệt kê project mẫu, lọc được theo thư viện dùng.
- [klibs.io](https://klibs.io/): tìm thư viện theo nền tảng hỗ trợ (iOS, Android, JS…).
