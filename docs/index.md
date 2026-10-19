# Native Device Monitor KMP

App `native-device-monitor` cũ (repo private [tucarmd/native-mobile-learning](https://github.com/tucarmd/native-mobile-learning/tree/main/native-device-monitor)) viết **hai lần toàn bộ** (Kotlin + Swift). Bản này làm lại theo hướng **KMP core + UI native**:

1. **UI vẫn viết riêng từng nền tảng:** Android là Kotlin + Jetpack Compose (`androidApp/`), iOS là Swift + SwiftUI (`iosApp/`).
2. **Chỉ phần dùng chung mới viết bằng KMP** (`shared/`): model, luật, ViewModel, interface, HTTP (Ktor), logger, vòng lặp monitor.

!!! info "Đã chuyển hướng (2026-10-19, bước R1–R4)"
    P0–P7 từng dùng **Compose Multiplatform** (một UI Kotlin vẽ cho cả 2 bên). Từ R4, `shared/` không còn Compose:
    Android vẽ bằng Jetpack Compose, iOS vẽ bằng SwiftUI, cả hai đọc **cùng một ViewModel** trong `shared/`.
    Xem [Spec](plan/README-KMP-NATIVE-UI-SPEC.md) · [Plan R1–R6](plan/README-KMP-NATIVE-UI-PLAN.md).
    Còn lại: **R5**, thay các service fake trên iOS (permission, BLE, camera, monitor) bằng code Swift gọi SDK Apple thật.

## Đọc theo thứ tự

1. [Kế hoạch + tiến độ](plan/README-KMP-MIGRATION.md): mục tiêu, kiến trúc, ai viết phần nào (KMP hay native), phase P0–P7.
2. [UI native + KMP core](plan/README-KMP-NATIVE-UI-PLAN.md): bước R1–R6 đổi sang UI native, mỗi bước 1 commit.
3. [Từng phase: dev đã làm gì](README-KMP-PHASES.md): P0 → P7 rồi R1 → R4, mỗi bước gắn nhãn 🎨 UI · 🧩 KMP chung · 🤖 Android · 🍎 iOS. **Đọc trang này khi trình bày / review.**
4. [App chạy thế nào](README-KMP-HOW-IT-WORKS.md): 6 tầng, app khởi động ai gọi ai, bấm "Scan" hàm nào chạy, KMP gọi SDK nào.
5. [Kế hoạch site này](plan/README-KMP-DOCS-SITE-PLAN.md): site tài liệu được dựng ra sao (S1–S5).

## Module

| Thư mục | Ngôn ngữ | Chứa gì |
|---|---|---|
| `shared/commonMain` | Kotlin (KMP) | `core/`: model, luật, interface, Ktor, FileLogger, vòng monitor · `presentation/`: 5 ViewModel + `AppViewModel` + chuỗi hiển thị |
| `shared/androidMain` | Kotlin · Android SDK | xin quyền, BLE (BluetoothGatt), camera / chọn ảnh, FGS + WorkManager |
| `shared/iosMain` | Kotlin/Native | `iosServices` (đang fake, chờ R5) + hàm nhỏ hỗ trợ Swift |
| `androidApp/` | Kotlin · **Jetpack Compose** | `MainActivity` → `App(AppViewModel)` → 5 màn hình |
| `iosApp/` | **Swift · SwiftUI** | `AppModel` giữ `AppViewModel` → `RootView` (TabView) → 5 View · `Observed<T>` đổi `StateFlow` Kotlin thành `@Published` |

## Chạy

```bash
# Android (JDK 17+), từ thư mục gốc repo
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n com.learn.ndmkmp/.MainActivity --es tab scanner   # --es tab: mở thẳng 1 tab
```

```bash
# iOS Simulator (hoặc mở iosApp/iosApp.xcodeproj trong Xcode rồi bấm Run)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath iosApp/build/dd build
xcrun simctl install "iPhone 17" "iosApp/build/dd/Build/Products/Debug-iphonesimulator/Device Monitor KMP.app"
xcrun simctl launch "iPhone 17" com.learn.ndmkmp -tab scanner
```

```bash
./gradlew :shared:allTests   # 78 test (luật + ViewModel) trên JVM của Android host + iOS simulator
```

## Ảnh chạy thật

Chọn tab bên dưới để xem từng màn, Android (Jetpack Compose) bên trái · iOS (SwiftUI) bên phải. Hai UI viết riêng nhưng đọc cùng một ViewModel, nên chữ và trạng thái giống nhau. iOS đang chạy service fake (chờ R5). Bấm vào ảnh để mở ảnh gốc.

=== "Permissions"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![Permissions Android](assets/kmp_tab_android_1_permissions.png)](assets/kmp_tab_android_1_permissions.png)
    <figcaption>Android · Jetpack Compose · Samsung A32</figcaption>
    </figure>
    <figure markdown>
    [![Permissions iOS](assets/kmp_tab_ios_1_permissions.png)](assets/kmp_tab_ios_1_permissions.png)
    <figcaption>iOS · SwiftUI · iPhone 17 Simulator</figcaption>
    </figure>
    </div>

=== "Scanner"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![Scanner Android](assets/kmp_tab_android_2_scanner.png)](assets/kmp_tab_android_2_scanner.png)
    <figcaption>Android · Jetpack Compose · Samsung A32</figcaption>
    </figure>
    <figure markdown>
    [![Scanner iOS](assets/kmp_tab_ios_2_scanner.png)](assets/kmp_tab_ios_2_scanner.png)
    <figcaption>iOS · SwiftUI · iPhone 17 Simulator</figcaption>
    </figure>
    </div>

=== "Media"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![Media Android](assets/kmp_tab_android_3_media.png)](assets/kmp_tab_android_3_media.png)
    <figcaption>Android · Jetpack Compose · Samsung A32</figcaption>
    </figure>
    <figure markdown>
    [![Media iOS](assets/kmp_tab_ios_3_media.png)](assets/kmp_tab_ios_3_media.png)
    <figcaption>iOS · SwiftUI · iPhone 17 Simulator</figcaption>
    </figure>
    </div>

=== "API"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![API Android](assets/kmp_tab_android_4_api.png)](assets/kmp_tab_android_4_api.png)
    <figcaption>Android · Jetpack Compose · Samsung A32</figcaption>
    </figure>
    <figure markdown>
    [![API iOS](assets/kmp_tab_ios_4_api.png)](assets/kmp_tab_ios_4_api.png)
    <figcaption>iOS · SwiftUI · iPhone 17 Simulator</figcaption>
    </figure>
    </div>

=== "Monitor"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![Monitor Android](assets/kmp_tab_android_5_monitor.png)](assets/kmp_tab_android_5_monitor.png)
    <figcaption>Android · Jetpack Compose · Samsung A32</figcaption>
    </figure>
    <figure markdown>
    [![Monitor iOS](assets/kmp_tab_ios_5_monitor.png)](assets/kmp_tab_ios_5_monitor.png)
    <figcaption>iOS · SwiftUI · iPhone 17 Simulator</figcaption>
    </figure>
    </div>
