[🏠 README](../../README.md) · Plan + bảng phase (P0–P13): [README-KMP-MIGRATION.md](README-KMP-MIGRATION.md)

# Spec: UI native riêng từng nền tảng, chỉ phần chung dùng KMP

> Tạo: 2026-10-19 · Trạng thái: **Đã duyệt 2026-10-19** · N1 = SKIE · N2 = Swift · N3 = dời Compose UI sang `androidApp/`
> Tiến độ: **P8–P11 ✅ · P12 ✅ code + simulator (⏳ iPhone thật) · P13 Final chưa**. Các bước này nằm chung một bảng phase với P0–P7: [Plan §3](README-KMP-MIGRATION.md). Commit của P8–P12 ghi nhãn tạm **R1–R5**.
> Thay quyết định **D1** của plan cũ (UI chung bằng Compose Multiplatform) bằng phương án thay thế: **KMP core + UI native**.

## 1. Mục tiêu

Thay đổi 2 điều:

1. **UI tách riêng cho từng nền tảng:** Android viết bằng **Kotlin + Jetpack Compose**, iOS viết bằng **Swift + SwiftUI**.
2. **Chỉ phần dùng chung mới viết bằng KMP:** model, luật, ViewModel, interface, HTTP, logger, vòng lặp monitor.

Mục đích là học được cả hai kiểu UI native, đồng thời thấy rõ ranh giới: cái gì share được, cái gì phải viết riêng.
Đây cũng là cách nhiều team KMP thật đang dùng (UI native, logic chung).

## 2. Hiện trạng lúc bắt đầu (2026-10-19)

> Ảnh chụp **trước P8**, giữ lại làm mốc "trước" để so. Trạng thái hôm nay xem dòng *Tiến độ* ở đầu trang.

| Phần | Đang ở đâu | Ghi chú |
|---|---|---|
| UI 5 tab | `shared/commonMain/ui/` (~2.6k dòng, Compose MP) | Vẽ cho cả 2 nền tảng. **State nằm ngay trong composable** (`remember`, `collectAsState`), chưa có ViewModel |
| Logic + interface | `shared/commonMain/core/` (~2.0k) | Đã thuần Kotlin, không dính Compose → **giữ nguyên** |
| 🤖 Platform | `shared/androidMain/` (~1.9k) | P3 P4 P5 P7 thật → **giữ nguyên** |
| 🍎 Platform | `shared/iosMain/` (~80) | Chỉ có `MainViewController` + `IosServices`, đang dùng fake (trừ API) |
| iOS shell | `iosApp/` (29 dòng Swift) | Chỉ bọc `MainViewController` của Compose |
| App cũ | repo `native-mobile-learning/native-device-monitor/ios/` | Có sẵn **SwiftUI** `UI/` (~2.4k) + Swift `NativeCore/` (BLE, Permission, Media, Background), port lại được |

## 3. Thiết kế

### 3.1 Bố cục sau khi đổi

```
native-device-monitor-kmp/
├── shared/src/                    KMP: KHÔNG còn Compose
│   ├── commonMain/
│   │   ├── core/                  model · luật · interface · Ktor · FileLogger · LoopDeviceMonitor   (giữ nguyên)
│   │   └── presentation/          🆕 5 ViewModel: state (StateFlow) + action, dùng chung 2 bên
│   ├── androidMain/               actual Android: permission, BLE, camera, FGS + WorkManager            (giữ nguyên)
│   ├── iosMain/                   cầu nối cho Swift (bọc Flow, tạo ViewModel) + actual iOS nếu viết bằng Kotlin
│   └── commonTest/                test luật + 🆕 test ViewModel
├── androidApp/                    🤖 Kotlin + Jetpack Compose: 5 màn hình, theme, tab bar   (dời từ shared/ui sang)
└── iosApp/                        🍎 Swift + SwiftUI: 5 View, theme, TabView                (port từ app cũ)
```

### 3.2 Ai làm gì

| Tầng | Android | iOS | Dùng chung? |
|---|---|---|---|
| UI (màn hình, theme, tab bar) | Kotlin · Jetpack Compose · `androidApp/` | Swift · SwiftUI · `iosApp/` | ❌ viết 2 lần |
| ViewModel (state + action) | `shared/commonMain/presentation/` | ← cùng file đó | ✅ KMP |
| Model, luật, interface | `shared/commonMain/core/` | ← cùng file đó | ✅ KMP |
| HTTP, logger, vòng monitor | `KtorApiService`, `FileLogger`, `LoopDeviceMonitor` | ← cùng file đó | ✅ KMP |
| Gọi SDK nền tảng | Kotlin · `shared/androidMain/` | **quyết định N2** bên dưới | ❌ riêng |

### 3.3 MVVM

`View (Compose / SwiftUI) → ViewModel (commonMain) → interface core (commonMain) → implementation từng nền tảng`.

- Mỗi tab có 1 ViewModel: `PermissionViewModel`, `ScannerViewModel`, `MediaViewModel`, `ApiViewModel`, `MonitorViewModel`.
  Đi kèm là một `AppViewModel` nhỏ giữ tab đang chọn và trạng thái *Ready to scan*.
- ViewModel có 2 phần:
  - `state: StateFlow<XxxUiState>`: một data class chứa mọi thứ màn hình cần vẽ.
  - Hàm action (`onScanClick()`, `send(preset)`…): View chỉ gọi hàm, **không có logic trong View**.
- Lớp nền là **`SharedViewModel` tự viết** (~10 dòng: một `CoroutineScope` trên Main + `clear()`), không dùng
  `androidx.lifecycle.ViewModel`. *Đổi lúc làm P8:* các service mà ViewModel bọc (launcher xin quyền, BLE client) được
  tạo theo Activity và chết cùng Activity. Nếu ViewModel sống lâu hơn Activity thì nó sẽ giữ service đã chết. Vì vậy
  ViewModel được tạo cạnh `AppServices` và `clear()` cùng lúc: Android ở `MainActivity.onDestroy`, iOS khi `AppModel`
  của Swift bị huỷ.
- Chuỗi hiển thị (`ApiStrings`, `ScannerStrings`…) dời từ `ui/` về `presentation/` để 2 UI dùng chung câu chữ.
  Màu, khoảng cách, font thì mỗi nền tảng tự giữ.

### 3.4 Swift đọc StateFlow thế nào

Swift không `collect` trực tiếp được Flow của Kotlin, nên cần một lớp cầu nối (**quyết định N1**). SwiftUI sẽ có một
`ObservableObject` mỏng: nó đăng ký nghe `state` của ViewModel, gán vào `@Published`, rồi gọi `viewModel.clear()` khi
View biến mất.

## 4. Quyết định cho bạn

| # | Câu hỏi | Đề xuất | Phương án khác |
|---|---|---|---|
| N1 | Swift đọc Flow bằng gì | **SKIE** (Touchlab): plugin Gradle, Flow thành `AsyncSequence`, sealed class thành `enum` Swift. Viết ít, Swift gọn | Tự viết helper `watch(callback)` trong iosMain (~40 dòng). Thấy rõ cơ chế nhưng Swift dài hơn, enum sealed khó dùng |
| N2 | Code gọi SDK Apple (permission, BLE, camera, BGTask) viết bằng gì | **Swift**, implement interface Kotlin, rồi đưa vào `AppServices` từ `iOSApp.swift`. Port lại `NativeCore/` của app cũ, đúng ý "chỉ phần chung mới KMP" | **Kotlin/Native** trong `iosMain` (`import platform.CoreBluetooth`), theo D2 cũ. Một ngôn ngữ, nhưng interop ObjC dài dòng |
| N3 | Compose UI hiện có | **Dời** sang `androidApp/`, gỡ Compose MP khỏi `shared` | Giữ song song cả 2 UI cho iOS (thêm cấu hình, dễ rối) |

Nếu chọn N2 = Swift thì interface trong core phải dễ implement từ Swift: callback hoặc hàm `suspend` thay vì bắt Swift
tự tạo `MutableStateFlow`. *Cách làm ở P12:* interface `core/` giữ nguyên. Thêm interface nhỏ kiểu callback trong `iosMain`
(`IosPermissionNative`, `IosBleNative`, `IosMediaNative`, `IosDeviceNative`) cho Swift implement, cùng adapter Kotlin
(`IosPermissionController`, `IosBleClient`, `IosMediaPicker`, `iosDeviceMonitor`) giữ `StateFlow` và luật.

## 5. Ảnh hưởng tới plan cũ

- **D1** đổi thành "KMP core + UI native". Ghi vào mục Log của [README-KMP-MIGRATION.md](README-KMP-MIGRATION.md).
- P0–P2 vẫn là lịch sử đúng (đã từng chạy Compose MP). P3–P7 phần 🤖 giữ nguyên. Phần 🍎 làm lại theo N2.
- Các trang doc phải sửa: trang chủ, Migration §1–2, How it works (sơ đồ tầng), Phases (thêm P8–P12).

## 6. Rủi ro

- **Khối lượng:** UI viết 2 lần, nên số dòng tăng khoảng 2k so với Compose MP. Đây là cái giá chấp nhận để học UI native.
- **ViewModel trên iOS:** iOS không có `ViewModelStore` tự dọn, nên phải tự gọi `clear()`. Quên gọi thì coroutine chạy mãi.
- **Đổi interface khi đã có bản Android thật:** test `commonTest` hiện có (52+) là lưới an toàn. Mỗi bước phải xanh trước khi commit.
- **SKIE và Kotlin 2.4.10:** phải kiểm tra bản SKIE hỗ trợ. Nếu chưa hỗ trợ thì lùi về helper tự viết (N1 phương án khác).

## 7. Ngoài phạm vi

Pomodoro, desktop/web, phát hành lên store, tính năng mới. Phần 🍎 thật của P3/P4/P5/P7 là P12
(xem [plan](README-KMP-MIGRATION.md) §3).
