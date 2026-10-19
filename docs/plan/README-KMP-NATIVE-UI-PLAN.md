[🏠 README](../../README.md) · Spec: [README-KMP-NATIVE-UI-SPEC.md](README-KMP-NATIVE-UI-SPEC.md)

# Plan R1–R6: UI native riêng, KMP chỉ cho phần chung

> Tạo: 2026-10-19 · Trạng thái: **Đã duyệt 2026-10-19** (N1 SKIE · N2 Swift · N3 dời) · R1 ✅ · R2 🟡 (iOS: tab API đã là SwiftUI + SKIE, 4 tab còn lại chờ R3) · R3–R6 ⏳
> Luật: mỗi phase **1 commit** `refactor(kmp): Rn …`, xong phải **build OK, chạy thật, chụp ảnh**. Không gộp phase.

## 1. Thứ tự và lý do

Đi từ an toàn đến rủi ro: rút logic ra ViewModel trước, lúc đó UI cũ vẫn chạy. Sau đó dựng iOS SwiftUI song song.
Chỉ khi cả 2 bên đều có UI mới thì mới gỡ Compose khỏi `shared`. Như vậy không có lúc nào app "chết" giữa chừng.

## 2. Các phase

| # | Làm gì | File chính | Vì sao | Xong khi |
|---|---|---|---|---|
| **R1** ✅ | Rút state + action từ 5 composable ra **5 ViewModel** + `AppViewModel` trong `commonMain/presentation/`. Dời `*Strings` sang đó. Composable chỉ còn `vm.state.collectAsState()` + gọi hàm. Chỉnh interface cho Swift implement được (nếu N2 = Swift) | `presentation/*ViewModel.kt`, `ui/*/*Screen.kt` (gọn lại), `core/*` (chữ ký interface) | Chuẩn MVVM. Là điều kiện để SwiftUI dùng chung logic | `allTests` xanh + test ViewModel mới. Android **và** iOS (vẫn Compose) chạy đủ 5 tab như trước, có ảnh. **Kết quả:** 78/78 test trên JVM và iOS simulator · [android](../assets/kmp_r1_android_tabs.png) · [ios](../assets/kmp_r1_ios_tabs.png) |
| **R2** ✅ | Cầu nối Swift theo N1 (SKIE hoặc helper). `iOSApp.swift` tạo `AppServices` + ViewModel. Dựng **tab API bằng SwiftUI** đầu tiên (đơn giản nhất, đã thật trên iOS) | `shared/build.gradle.kts`, `iosMain/…/IosBridge.kt`, `iosApp/…/Api/ApiView.swift`, `ApiObservable.swift` | Chứng minh đường đi SwiftUI → ViewModel KMP → Ktor trên 1 tab trước khi nhân ra 5 | iPhone simulator: tab API SwiftUI gọi GET/POST/500/timeout được, có ảnh. **Kết quả:** SKIE 0.10.15 · `Observed<T>` (StateFlow → `@Published`) · Xcode dùng folder đồng bộ (thêm file Swift không cần sửa `pbxproj`) · iOS 17.0 · Maestro bấm 200 + 500 · [ảnh](../assets/kmp_r2_ios_swiftui_api.png) |
| **R3** ✅ | Port **4 tab còn lại** sang SwiftUI (Permissions, Scanner + GATT, Media, Monitor), dùng service fake cho iOS. Theme SwiftUI (`AppColor`, `AppSpacing`) + `TabView` | `iosApp/iosApp/UI/**` (lấy từ app cũ, đổi nguồn dữ liệu sang ViewModel) | UI iOS đủ 5 tab bằng Swift | Simulator: 5 tab SwiftUI, ảnh từng tab. **Kết quả:** Maestro chạy hết 5 tab (quyền → BT → scan → connect → notify → write → nhận NOTIFY · chụp ảnh fake · refresh monitor) · `InfoRow` thay `Pair` cho Swift dễ đọc · `IosBridgeKt.previewData` đổi ảnh sang `Data` · [ảnh](../assets/kmp_r3_ios_swiftui_tabs.png) |
| **R4** | Dời Compose UI sang `androidApp/` (`ui/`, `theme/`, `App()`). Gỡ plugin Compose MP + `MainViewController` khỏi `shared`. `androidApp` thêm Jetpack Compose (BOM) | `androidApp/build.gradle.kts`, `androidApp/src/main/kotlin/…/ui/**`, `shared/build.gradle.kts` | `shared` thành KMP thuần, chỉ còn phần chung | Android: 5 tab như R1, BLE/camera/monitor thật vẫn chạy trên Samsung A32. `shared` build không còn Compose |
| **R5** | 🍎 platform thật theo N2: permission, BLE, camera/picker, battery + BGTaskScheduler (= phần 🍎 của P3 P4 P5 P7) | N2 = Swift: `iosApp/iosApp/Platform/*.swift`. N2 = K/N: `shared/iosMain/core/**` | iOS hết fake | Mỗi mục test trên **iPhone thật** (skill `ios-device-carmd-identity`), có ảnh. Có thể chia R5a–R5d, mỗi cái 1 commit |
| **R6** | Cập nhật docs: trang chủ, Migration (D1 + Log), How it works (sơ đồ tầng mới), Phases (thêm R1–R5), README gốc. Deploy site | `docs/**`, `README.md` | Doc khớp code | `mkdocs build --strict` OK, site lên |

## 3. Kiểm tra mỗi phase

- `./gradlew :shared:allTests` (JVM + iOS simulator).
- Android: `./gradlew :androidApp:installDebug`, chụp ảnh (skill `kotlin-android-build-run`).
- iOS: `xcodebuild` + `simctl`, chụp ảnh (skill `swift-ios-build-run`).
- Sau mỗi phase: báo ngắn đã làm gì, tại sao, rồi **dừng chờ OK** mới qua phase sau.

## 4. Ước lượng

| Phase | Kích cỡ |
|---|---|
| R1 | lớn: Scanner nhiều state nhất (~550 dòng composable) |
| R2 | vừa |
| R3 | lớn: port ~2.4k dòng SwiftUI cũ |
| R4 | vừa: chủ yếu dời file + Gradle |
| R5 | lớn: cần máy thật |
| R6 | nhỏ |
