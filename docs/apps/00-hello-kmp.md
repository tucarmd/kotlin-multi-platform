# App 0 · Hello KMP 👋

> App nhỏ nhất có thể: một màn hình, một lời chào, một nút đếm. Mục tiêu chỉ là **thấy KMP chạy**: code Kotlin
> trong `shared/` được gọi từ UI Compose (Android) và UI SwiftUI (iOS).

Trang này chỉ có **Phase 1**. Làm xong, chạy được trên 2 máy rồi mới tính phase sau.

## 1. Tóm tắt

| | |
|---|---|
| **Cấp độ** | ⭐ (trước cả [App 1 · Tip Split](01-tip-split.md)) |
| **Học gì** | (1) UI native gọi vào shared thế nào · (2) shared được nhúng vào Android và iOS thế nào |
| **Thư mục code** | `apps/HelloKmp/` · package `dev.hellokmp` |
| **Không dùng** | Koin, SKIE, coroutines trong shared, database, mạng (để phase sau) |
| **Thời gian** | 1 buổi tối |

## 2. Màn hình

Một màn duy nhất, hai nền tảng giống nhau:

```
┌──────────────────────────────┐
│  Xin chào từ Android 36      │  ← chuỗi lấy từ shared (expect/actual)
│                              │
│            3                 │  ← số đếm, state nằm ở ViewModel
│                              │
│     [ − ]        [ + ]       │  ← bấm → gọi CounterRules trong shared
└──────────────────────────────┘
```

| Action | Kết quả |
|---|---|
| Bấm **+** | Số tăng 1 |
| Bấm **−** | Số giảm 1, **không xuống dưới 0** (luật nằm ở shared) |

## 3. Shared được nhúng thế nào

```
                 shared/  (Kotlin Multiplatform)
   commonMain:  Greeting · CounterRules · expect platformName()
   androidMain: actual platformName() = "Android 36"
   iosMain:     actual platformName() = "iOS 26.0"
        │                                   │
        │ Gradle: implementation(           │ Gradle task embedAndSignAppleFrameworkForXcode
        │   project(":shared"))             │ build ra Shared.framework, Xcode chạy task này
        ▼                                   ▼ ở bước "Compile Kotlin Framework" trước khi build Swift
   androidApp/  (Compose)              iosApp/  (SwiftUI)
   MainActivity                        iOSApp.swift
     └ HelloScreen                       └ HelloView
         └ HelloViewModel ── gọi ──►         └ HelloModel ── gọi ──►  import Shared
```

- **Android**: shared chỉ là một module Gradle bình thường. `androidApp` thêm `implementation(project(":shared"))`
  là gọi được `Greeting()` như class Kotlin cùng project.
- **iOS**: Kotlin/Native biên dịch shared thành `Shared.framework`. Xcode có một Run Script (khai báo trong
  `iosApp/project.yml`) gọi `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, rồi Swift `import Shared`.
- **MVVM**: View chỉ vẽ và gửi action; ViewModel (Android) / Model `@Observable` (iOS) giữ state và gọi shared;
  luật (`không xuống dưới 0`) nằm trong shared để hai máy chạy cùng một code.

## 4. Phase 1 — file sẽ có

Tạo khung bằng `.claude/skills/kmp-new/scripts/new-app.sh HelloKmp TipSplit`, rồi **xoá** phần Tip Split không cần
(Koin, SKIE, Kover, Turbine, UI test, Maestro, `strings.xml`). Kết quả chỉ còn:

```
apps/HelloKmp/
├── settings.gradle.kts            include(":shared", ":androidApp")
├── build.gradle.kts
├── gradle/libs.versions.toml      chỉ còn kotlin, agp, compose, activity, lifecycle
├── shared/
│   ├── build.gradle.kts           androidTarget + iosArm64 + iosSimulatorArm64, framework "Shared"
│   └── src/
│       ├── commonMain/kotlin/dev/hellokmp/
│       │   ├── Platform.kt        expect fun platformName(): String
│       │   ├── Greeting.kt        class Greeting { fun greet(): String }
│       │   └── CounterRules.kt    object CounterRules { plus(count) · minus(count) }
│       ├── androidMain/kotlin/dev/hellokmp/Platform.android.kt   actual = "Android ${Build.VERSION.SDK_INT}"
│       ├── iosMain/kotlin/dev/hellokmp/Platform.ios.kt           actual = UIDevice systemName + systemVersion
│       └── commonTest/kotlin/dev/hellokmp/CounterRulesTest.kt
├── androidApp/src/main/kotlin/dev/hellokmp/android/
│   ├── MainActivity.kt            setContent { HelloScreen() }
│   ├── HelloViewModel.kt          StateFlow<HelloUiState(greeting, count)>, onPlus() / onMinus()
│   └── HelloScreen.kt             Text + 2 Button, đọc state bằng collectAsStateWithLifecycle
└── iosApp/
    ├── project.yml                xcodegen, có Run Script build Shared.framework
    └── iosApp/
        ├── iOSApp.swift           WindowGroup { HelloView() }
        ├── HelloModel.swift       @Observable, gọi Greeting().greet() và CounterRules.shared.plus(count:)
        └── HelloView.swift        Text + 2 Button
```

Chữ trên UI (nút, tiêu đề) để thẳng trong `HelloScreen.kt` / `HelloView.swift`, không dùng `strings.xml`.

### Mấy điểm sẽ thấy khi gọi shared từ Swift

| Kotlin | Swift thấy |
|---|---|
| `class Greeting` | `Greeting()` |
| `object CounterRules` | `CounterRules.shared` |
| `fun plus(count: Int): Int` | `plus(count: Int32) -> Int32` (Kotlin `Int` = `Int32`) |
| `expect fun platformName()` ở file `Platform.kt` | `PlatformKt.platformName()` |

## 5. Các bước trong Phase 1

| Việc | Xong |
|---|:-:|
| 1.1 Tạo khung `apps/HelloKmp` từ TipSplit, xoá phần thừa, `xcodegen`, build xanh | ⬜ |
| 1.2 Shared: `Platform` (expect/actual), `Greeting`, `CounterRules` + `CounterRulesTest` | ⬜ |
| 1.3 Android: `HelloViewModel` + `HelloScreen`, chạy emulator | ⬜ |
| 1.4 iOS: `HelloModel` + `HelloView`, chạy simulator | ⬜ |
| 1.5 `apps/HelloKmp/README.md`: cách chạy 2 máy + sơ đồ mục 3 | ⬜ |

## 6. Kiểm tra

```bash
cd apps/HelloKmp
./gradlew :shared:testDebugUnitTest :shared:iosSimulatorArm64Test   # test luật chạy trên JVM và trên iOS
./gradlew :androidApp:installDebug                                  # cài lên emulator
cd iosApp && xcodegen && xcodebuild -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build        # build iOS
```

| Việc | Xong |
|---|:-:|
| Test `CounterRulesTest` xanh trên JVM và iOS | ⬜ |
| Android hiện "Xin chào từ Android …", bấm +/− chạy, − dừng ở 0 | ⬜ |
| iOS hiện "Xin chào từ iOS …", bấm +/− chạy, − dừng ở 0 | ⬜ |
| Chụp màn hình 2 máy | ⬜ |

Xong Phase 1 → 1 commit: `feat(hellokmp): P1 — shared Greeting + CounterRules, Compose and SwiftUI screens`.

## 7. Tham khảo

- [Bài 1 · KMP là gì](../lessons/01-what-is-kmp.md) — `shared` + UI native
- [App 1 · Tip Split](01-tip-split.md) — bước kế tiếp: StateFlow trong shared, Koin, SKIE
- [Kotlin docs · expect/actual](https://kotlinlang.org/docs/multiplatform/multiplatform-expect-actual.html)
- [Kotlin docs · Swift/Objective-C interop](https://kotlinlang.org/docs/native-objc-interop.html)
