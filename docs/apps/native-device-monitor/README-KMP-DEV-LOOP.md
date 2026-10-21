[🏠 README](index.md) · Kiến trúc: [App chạy thế nào](README-KMP-HOW-IT-WORKS.md) · Plan: [P0–P16](plan/README-KMP-MIGRATION.md)

# Sửa code và xem kết quả nhanh

> Cập nhật 2026-10-19. **Cách đang dùng:** sửa Kotlin trong Android Studio, sửa Swift và chạy iOS trong **project Xcode riêng**
> (`iosApp/iosApp.xcodeproj`). Các cách hot reload bên dưới chỉ ghi lại để **biết là có**, chưa cài vào dự án.

## Cách đang dùng

| Sửa gì | Mở bằng | Chạy / xem |
|---|---|---|
| Kotlin: `shared/` (logic, ViewModel), `androidApp/` | Android Studio | Android: run config **androidApp** → ▶ |
| Swift: `iosApp/` (vỏ app + UI SwiftUI 5 tab) | Xcode, mở `iosApp/iosApp.xcodeproj` | iOS: chọn iPhone Simulator → ▶ (Cmd+R) |

Hai IDE cùng đọc **một bộ file trên đĩa**: sửa ở đâu thì lần build sau bên kia cũng thấy.
Xcode có build phase `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, nên mỗi lần ▶ nó tự build lại framework
`Shared` từ Kotlin.

## Mỗi lần sửa có phải build lại không

| Sửa | Bấm ▶ thì | Thời gian |
|---|---|---|
| Chỉ file Swift | Xcode biên dịch lại file Swift đó; bước Gradle thấy Kotlin không đổi nên bỏ qua | vài giây |
| File Kotlin trong `shared/` | Kotlin/Native biên dịch + link lại framework `Shared`, rồi mới build app | vài chục giây → vài phút |
| Đổi thư viện / Gradle, lỗi lạ | Mới cần **Clean Build Folder** (Cmd+Shift+K) | lâu nhất |

Sửa logic trong `commonMain` → kiểm tra trên Android hoặc `./gradlew :shared:iosSimulatorArm64Test` trước, vì nhanh hơn
build app iOS.

## Xem UI mà không chạy lại cả app (biết để dùng khi cần)

| Cách | Dùng cho | Ghi chú |
|---|---|---|
| **Xcode Previews** (Canvas, Cmd+Option+Enter) | SwiftUI | Cần `Shared` build ít nhất 1 lần. Dùng được cho cả 5 tab từ P10; P0–P7 thì không (UI iOS lúc đó là Compose trong `MainViewController`) |
| **Compose Preview** (`@Preview`) | Compose trên Android | Xem trong khung Design / Split, không cần emulator. UI Android nằm trong `androidApp/` (Jetpack Compose) từ P11 |
| **Live Edit** | Compose trên Android, app đang chạy (API 30+) | Chỉ sửa UI composable; đổi logic / ViewModel / class mới vẫn phải Run lại |
| **Apply Changes** (⚡ cạnh nút Run) | Android | Đẩy code đã sửa vào app đang chạy, không cài lại |
| Compose Hot Reload (JetBrains) | — | Chỉ cho target Desktop; dự án không có Desktop → không dùng được |

### Hot reload Swift khi sửa ngoài Xcode: InjectionIII (không dùng, chỉ ghi lại)

Android Studio không vẽ được SwiftUI Preview. Nếu sau này muốn sửa Swift bằng editor khác (Android Studio, VS Code…) mà
app trên Simulator tự cập nhật, có [InjectionIII](https://github.com/johnno1962/InjectionIII) kèm package `Inject`:

- Nó theo dõi file `.swift` trên đĩa, nên **sửa bằng editor nào cũng được**. App đang chạy trên Simulator cập nhật mà
  không cần Run lại.
- Phải cài đặt 1 lần:
    - thêm cờ linker `-Xlinker -interposable` (chỉ cho Debug);
    - thêm `@ObserveInjection` + `.enableInjection()` vào các View.
- Chỉ chạy trên **Simulator**, không chạy trên iPhone thật.
- Chỉ áp dụng cho Swift. Sửa Kotlin trong `shared/` vẫn phải build lại framework.

Cách nhẹ hơn, không cần cài gì: để Xcode mở bên cạnh với Canvas của đúng file đó. Lưu file trong editor khác thì Xcode
đọc lại file từ đĩa và vẽ lại Preview (đôi khi phải bấm *Resume*).
