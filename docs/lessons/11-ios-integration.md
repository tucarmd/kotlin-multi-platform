# Nối với iOS

Code Kotlin chạy trên iOS dưới dạng một **framework** mà Xcode import. Bài này giải thích framework đó vào Xcode bằng cách nào,
và Swift gọi Kotlin ra sao.

## Mục tiêu

- Chọn được cách tích hợp iOS hợp với project.
- Bọc màn Compose trong SwiftUI, và viết một màn SwiftUI gọi code Kotlin.

## Ba cách đưa framework vào Xcode

| Cách | Khi nào dùng |
|---|---|
| **Direct integration** | Mặc định của wizard. Xcode chạy một Gradle task trong *Build Phase* để build + nhúng framework. Hợp khi project iOS chỉ phục vụ app KMP này |
| **SwiftPM** | Xuất framework thành Swift package (từ xa hoặc local), hoặc import Swift package vào Kotlin. Hướng mà hệ sinh thái Apple đang đi |
| **CocoaPods** | Project iOS đã dùng CocoaPods sẵn, hoặc cần dùng Pod trong Kotlin. Docs có hướng dẫn chuyển sang SwiftPM |

Habit Tick giữ **direct integration** do wizard sinh. Mở *Build Phases* của target `iosApp` trong Xcode để thấy dòng gọi Gradle
(dạng `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`).

## Compose trong SwiftUI

Bài 6 đã có `MainViewController()` ở `iosMain`. Phía Swift:

```swift
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea(.keyboard)
    }
}
```

Hàm Kotlin cấp cao nhất trong file `MainViewController.kt` hiện ra phía Swift qua lớp `MainViewControllerKt`.

## Swift gọi Kotlin

- Lớp, hàm Kotlin trong `commonMain` / `iosMain` dùng thẳng được từ Swift.
- Hàm `suspend` → Swift thấy dạng `async` (hoặc completion handler).
- `Flow` không tự thành `AsyncSequence` — thường dùng thư viện cộng đồng như **SKIE** hoặc **KMP-NativeCoroutines**, hoặc tự viết hàm bọc callback.

## Làm trên app MVP

Thêm tab "Giới thiệu" viết bằng **SwiftUI thuần**, hiện `Greeting().greet()` và `platformName()` lấy từ Kotlin.
Bạn vừa trải nghiệm kiểu "chỉ chia sẻ logic, UI native" của [bài 1](01-what-is-kmp.md).

## Kiểm tra

- [ ] App iOS mở được từ Xcode (không chỉ từ IntelliJ).
- [ ] Có một màn SwiftUI gọi hàm Kotlin.

## Lỗi hay gặp

- Xcode báo `No such module 'ComposeApp'` → build một lần từ Xcode để Gradle task sinh framework; kiểm `baseName` trong Gradle.
- Đổi code Kotlin mà Xcode không thấy → clean build folder trong Xcode.

## Tiếp theo?

- [Bài 12: test](12-testing.md).
- Đọc thêm: [SwiftUI integration](https://kotlinlang.org/docs/multiplatform/compose-swiftui-integration.html) · [UIKit integration](https://kotlinlang.org/docs/multiplatform/compose-uikit-integration.html) · [CocoaPods → SwiftPM](https://kotlinlang.org/docs/multiplatform/multiplatform-cocoapods-spm-migration.html).

## Docs gốc

[Choose integration method](https://kotlinlang.org/docs/multiplatform/multiplatform-ios-integration-overview.html) · [Direct integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html) · [SwiftPM export](https://kotlinlang.org/docs/multiplatform/multiplatform-spm-export.html) · [CocoaPods overview](https://kotlinlang.org/docs/multiplatform/multiplatform-cocoapods-overview.html)
