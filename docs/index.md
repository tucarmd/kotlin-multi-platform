# Native Device Monitor KMP

App `native-device-monitor` cũ (repo private [tucarmd/native-mobile-learning](https://github.com/tucarmd/native-mobile-learning/tree/main/native-device-monitor)) viết **hai lần** (Kotlin + Swift), làm lại thành **một codebase Kotlin** bằng Kotlin Multiplatform + Compose Multiplatform cho Android và iOS. Swift chỉ còn là vỏ iOS (`iosApp/`).

## Đọc theo thứ tự

1. [Kế hoạch + tiến độ](plan/README-KMP-MIGRATION.md): mục tiêu, kiến trúc, phase P0–P7, cái gì đã chạy thật.
2. [Từng phase: dev đã làm gì](README-KMP-PHASES.md): P0 → P7, mỗi bước gắn nhãn 🎨 UI · 🧩 KMP chung · 🤖 Android · 🍎 iOS. **Đọc trang này khi trình bày / review.**
3. [App chạy thế nào](README-KMP-HOW-IT-WORKS.md): 6 tầng, app khởi động ai gọi ai, bấm "Scan" hàm nào chạy, KMP gọi SDK nào.
4. [Kế hoạch site này](plan/README-KMP-DOCS-SITE-PLAN.md): site tài liệu được dựng ra sao (S1–S5).

## Module

- `shared/`: thư viện KMP. `commonMain` (UI, model, luật), `androidMain`, `iosMain` (code riêng nền tảng).
- `androidApp/`: app Android, `MainActivity` gọi `App()`.
- `iosApp/`: project Xcode, nhúng framework `Shared`, `ContentView.swift` hiện `MainViewController()`.

## Chạy

```bash
# Android (JDK 17+), từ thư mục gốc repo
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n com.learn.ndmkmp/.MainActivity
```

```bash
# iOS Simulator (hoặc mở iosApp/iosApp.xcodeproj trong Xcode rồi bấm Run)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath iosApp/build/dd build
xcrun simctl install "iPhone 17" "iosApp/build/dd/Build/Products/Debug-iphonesimulator/Device Monitor KMP.app"
xcrun simctl launch "iPhone 17" com.learn.ndmkmp
```

```bash
./gradlew :shared:allTests   # commonTest trên JVM của Android host + iOS simulator
```

## Ảnh chạy thật

Chọn tab bên dưới để xem từng màn, Android bên trái · iOS bên phải. Bấm vào ảnh để mở ảnh gốc.

=== "Permissions"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![Permissions Android](assets/kmp_tab_android_1_permissions.png)](assets/kmp_tab_android_1_permissions.png)
    <figcaption>Android — Pixel 8, API 34</figcaption>
    </figure>
    <figure markdown>
    [![Permissions iOS](assets/kmp_tab_ios_1_permissions.png)](assets/kmp_tab_ios_1_permissions.png)
    <figcaption>iOS — iPhone 17, iOS 26.5</figcaption>
    </figure>
    </div>

=== "Scanner"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![Scanner Android](assets/kmp_tab_android_2_scanner.png)](assets/kmp_tab_android_2_scanner.png)
    <figcaption>Android — Pixel 8, API 34</figcaption>
    </figure>
    <figure markdown>
    [![Scanner iOS](assets/kmp_tab_ios_2_scanner.png)](assets/kmp_tab_ios_2_scanner.png)
    <figcaption>iOS — iPhone 17, iOS 26.5</figcaption>
    </figure>
    </div>

=== "Media"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![Media Android](assets/kmp_tab_android_3_media.png)](assets/kmp_tab_android_3_media.png)
    <figcaption>Android — Pixel 8, API 34</figcaption>
    </figure>
    <figure markdown>
    [![Media iOS](assets/kmp_tab_ios_3_media.png)](assets/kmp_tab_ios_3_media.png)
    <figcaption>iOS — iPhone 17, iOS 26.5</figcaption>
    </figure>
    </div>

=== "API"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![API Android](assets/kmp_tab_android_4_api.png)](assets/kmp_tab_android_4_api.png)
    <figcaption>Android — Pixel 8, API 34</figcaption>
    </figure>
    <figure markdown>
    [![API iOS](assets/kmp_tab_ios_4_api.png)](assets/kmp_tab_ios_4_api.png)
    <figcaption>iOS — iPhone 17, iOS 26.5</figcaption>
    </figure>
    </div>

=== "Monitor"

    <div class="shots shots--pair" markdown>
    <figure markdown>
    [![Monitor Android](assets/kmp_tab_android_5_monitor.png)](assets/kmp_tab_android_5_monitor.png)
    <figcaption>Android — Pixel 8, API 34</figcaption>
    </figure>
    <figure markdown>
    [![Monitor iOS](assets/kmp_tab_ios_5_monitor.png)](assets/kmp_tab_ios_5_monitor.png)
    <figcaption>iOS — iPhone 17, iOS 26.5</figcaption>
    </figure>
    </div>
