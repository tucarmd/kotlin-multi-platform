---
name: kmp-auto-test
description: >-
  Viết và chạy UI TEST TỰ ĐỘNG cho app KMP trong repo kotlin-multi-platform trên CẢ HAI nền tảng, dùng chung bảng
  id UiIds ở commonMain: Compose UI test (androidTest), XCUITest (iosAppUITests), và Maestro flow (.maestro/*.yaml)
  chạy y nguyên trên Android lẫn iOS, kèm chụp màn hình. Dùng khi nói "UI test", "auto test", "automation test",
  "maestro", "xcuitest", "compose ui test", "test bấm nút", "test end-to-end", "test cả hai nền tảng", "testTag",
  "accessibilityIdentifier", "flow maestro fail lần đầu".
---

# kmp-auto-test — một bảng id, ba loại UI test

Mẫu đầy đủ ở TipSplit:

| Loại | File | Chạy |
|---|---|---|
| Bảng id chung | `shared/src/commonMain/kotlin/dev/<app>/ui/UiIds.kt` | — |
| Compose UI test | `androidApp/src/androidTest/kotlin/dev/<app>/android/<X>ScreenUiTest.kt` | `./gradlew :androidApp:connectedDebugAndroidTest` |
| XCUITest | `iosApp/iosAppUITests/<X>UITests.swift` (target khai báo trong `project.yml`) | `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17'` |
| Maestro | `.maestro/*.yaml` | `maestro test -e APP_ID=dev.<app> .maestro/` · iOS: `-e APP_ID=dev.<app>.ios` |

## Cách làm

1. **Id trước, test sau.** Thêm hằng vào `UiIds` (dạng `<màn>_<vật>`, vd `split_bill_input`). Đổi tên id thì test của
   cả hai app gãy ngay lúc compile, không gãy âm thầm ở một bên.
2. Gắn id vào UI:
   - Compose: `Modifier.testTag(UiIds.X)`. Màn gốc bật `testTagsAsResourceId = true` để Maestro thấy được.
   - SwiftUI: `.accessibilityIdentifier(UiIds.shared.X)`.
3. Viết **một** flow Maestro cho mỗi kịch bản chính trong mục "Xong khi" của trang plan. Flow đó phải chạy được trên
   cả hai nền tảng mà không sửa gì. Đặt `tags: [smoke]` cho flow ngắn nhất.
4. Compose test và XCUITest chỉ dùng cho trạng thái khó dựng bằng Maestro (lỗi nhập, nhánh `Error`).
5. Cài app lên máy bằng `kmp-run` rồi mới chạy Maestro. Chụp màn hình bằng `- takeScreenshot: <tên>` trong flow.

## Bẫy đã gặp

- Lần chạy đầu sau khi cài (cold start + Koin) chậm hơn thời gian chờ mặc định của `tapOn`. Đầu flow luôn có
  `extendedWaitUntil: { visible: { id: … }, timeout: 15000 }`.
- Segmented `Picker` của SwiftUI không có id cho từng segment, nên trên iOS phải tìm nút theo chữ (`"10%"`).
- `launchApp: clearState: true` để mỗi flow bắt đầu sạch, nhất là app có database.
- Bàn phím che nút: thêm `- hideKeyboard` sau `inputText`.

Commit: `test(<scope>): UI tests on both platforms, shared ids`.
