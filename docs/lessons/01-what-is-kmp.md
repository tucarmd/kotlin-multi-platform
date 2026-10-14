# KMP là gì, khi nào dùng

Kotlin Multiplatform (KMP) cho bạn viết **một lần** phần logic bằng Kotlin, rồi dùng lại trên Android, iOS, Desktop, Web và server.
Phần giao diện thì tuỳ bạn: dùng chung luôn bằng Compose Multiplatform, hoặc giữ UI native riêng từng nền tảng.

## Mục tiêu

- Hiểu KMP chia sẻ **cái gì** và **không** chia sẻ cái gì.
- Biết hai kiểu app KMP và chọn kiểu cho Habit Tick.

## KMP chia sẻ những gì

Một app thường có ba lớp:

| Lớp | Ví dụ | Chia sẻ bằng KMP? |
|---|---|---|
| Logic nghiệp vụ | tính streak, kiểm dữ liệu, gọi API, lưu DB | **Có** — đây là lý do chính để dùng KMP |
| Giao diện | màn hình, nút, điều hướng | **Tuỳ chọn** — Compose Multiplatform, hoặc SwiftUI/Jetpack Compose riêng |
| API riêng của máy | Bluetooth, camera, Keychain | **Không trực tiếp** — viết phần riêng rồi nối bằng `expect`/`actual` hoặc interface |

Code Kotlin được biên dịch ra đúng thứ mỗi nền tảng hiểu: bytecode JVM cho Android và Desktop, mã máy native (framework) cho iOS,
JavaScript/Wasm cho web. Không có máy ảo chen vào giữa trên iOS.

## Hai kiểu app

=== "Chia sẻ cả UI (Compose Multiplatform)"

    Logic **và** giao diện viết một lần. Ít code nhất, giao diện giống nhau trên mọi máy.
    Docs gốc minh hoạ bằng app chọn múi giờ. **Habit Tick chọn kiểu này.**

=== "Chỉ chia sẻ logic, UI native"

    Logic chung trong module `shared`; Android dùng Jetpack Compose, iOS dùng SwiftUI.
    Hợp khi đã có đội iOS/Android riêng, hoặc cần cảm giác native tuyệt đối. Bài 11 có thử một màn SwiftUI để bạn biết cách này.

## So nhanh với các lựa chọn khác

- **Flutter / React Native:** chia sẻ cả UI bằng ngôn ngữ khác (Dart / JS), có lớp runtime riêng.
- **KMP:** Kotlin, ra mã native; muốn chia sẻ ít (chỉ logic) hay nhiều (cả UI) đều được, và **chuyển dần** được từ app Android có sẵn.

## Kiểm tra

- [ ] Nói được 3 lớp của app và lớp nào nên chia sẻ.
- [ ] Giải thích vì sao Habit Tick chọn Compose Multiplatform.

## Tiếp theo?

- [Bài 2: cài máy, tạo project đầu tiên](02-setup.md).
- Đọc thêm: [What is Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform/kmp-overview.html) · [Supported platforms](https://kotlinlang.org/docs/multiplatform/supported-platforms.html) · [FAQ](https://kotlinlang.org/docs/multiplatform/faq.html).

## Docs gốc

[Kotlin Multiplatform overview](https://kotlinlang.org/docs/multiplatform/kmp-overview.html) · [Native and cross-platform](https://kotlinlang.org/docs/multiplatform/native-and-cross-platform.html)
