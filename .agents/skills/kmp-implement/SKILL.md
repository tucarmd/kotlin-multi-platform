---
name: kmp-implement
description: >-
  Code ĐÚNG MỘT bước Bx (B1–B6) của một app KMP trong repo kotlin-multi-platform theo khung chung: B1 model+rules,
  B2 Core giữ StateFlow, B3 data/platform (SQLDelight, Ktor, GPS, BLE, expect/actual), B4 Koin + cửa vào Swift
  (KoinIos.kt, doInitKoin), B5 UI Compose, B6 UI SwiftUI qua SKIE. Comment theo [App N · Bx] / TRAP: / PLATFORM: /
  TEST:, mỗi bước một commit. Dùng khi nói "làm B3 cho app X", "viết Core", "viết rules", "nối Koin", "làm UI
  Android/iOS cho app X", "implement bước tiếp", "code app X".
---

# kmp-implement — một bước, một commit

Trước khi code: đọc trang plan `docs/apps/0N-<app>.md`, mục 5 và 6. Chưa có trang plan thì dừng lại, chuyển sang `kmp-plan`.
Đọc **file cùng vai ở app trước** (TipSplit hoặc app gần nhất) và viết theo y khuôn đó.

## Mỗi bước làm gì

| Bước | File | Mẫu để xem | Bắt buộc |
|:-:|---|---|---|
| B1 | `model/Models.kt`, `rules/*.kt` + test | `apps/TipSplit/shared/.../rules/Money.kt` | Hàm thuần. KDoc có ví dụ vào → ra. Test đi kèm (`kmp-test`) |
| B2 | `core/<X>Core.kt` | `TipCore.kt` | `private val _state = MutableStateFlow(..)`, `update { }` tính lại **toàn bộ** state từ input. Repository là interface để fake được |
| B3 | `data/`, `platform/`, `androidMain`, `iosMain`, `commonMain/sqldelight` | PocketNotes · RateWatch · TripLog · ObdLink | Lỗi thành nhánh sealed, không throw lên UI. Callback → `callbackFlow` |
| B4 | `di/Koin.kt`, `di/PlatformModule.*.kt`, `iosMain/di/KoinIos.kt` | TipSplit `di/` | Android `koinViewModel()` lấy được core, Swift `doInitKoin()` lấy được core |
| B5 | `androidApp/.../ui/<X>Screen.kt`, `<X>ViewModel.kt`, `Labels.kt` | `SplitScreen.kt` | Vẽ đủ mọi nhánh của state. Chuỗi để trong `strings.xml` (vi + en). `Modifier.testTag(UiIds.…)` |
| B6 | `iosApp/iosApp/<X>Model.swift`, `Views/<X>View.swift` | `SplitModel.swift` | `@Observable`, đọc state bằng `for await` (SKIE). `.accessibilityIdentifier(UiIds.shared.…)`. Giống Android |

## Comment (luật đầy đủ ở `docs/apps/conventions.md` mục 4)

- Đầu file: KDoc, dòng đầu là `[App N · Bx] <Tên> — <làm gì>`, sau đó là *why* và *Called by / Calls*. Viết tiếng Anh.
- Mỗi hàm public có KDoc 1–3 dòng; hàm trong `rules/` thêm ví dụ vào → ra.
- Trong thân hàm chỉ dùng `// TRAP:`, `// PLATFORM:`, `// TEST:`.
- Bẫy: trong KDoc, chuỗi `/*` sẽ mở một comment lồng, file báo lỗi `Unclosed comment`. Viết `folder/ *.yaml`, có dấu cách.

## Xong bước

1. Chạy lệnh kiểm "xong" của bước đó (bảng trong `kmp-app-frame`; B5 và B6 thì qua `kmp-run`).
2. Chạy `kmp-review` cho bước này.
3. Commit `feat(<scope>): Bx <làm gì>`. Scope viết kiểu kebab giống các commit cũ (`tip-split`, `pocket-notes`). Body theo quy ước học tập: Chương / App / Áp dụng / Học được / Chạy thử.
4. Sửa cột *File* trong bản đồ function của trang plan thành link `File.kt#L42`.

Không gộp hai bước vào một commit. Đang làm mà thấy cần đổi hợp đồng State thì dừng, sửa trang plan trước.
