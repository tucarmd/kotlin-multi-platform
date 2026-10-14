# Đưa app Android có sẵn sang KMP

Điểm mạnh nhất của KMP: **không phải viết lại từ đầu**. Bạn chuyển từng phần của app Android vào module chung, app vẫn chạy sau mỗi bước.

## Mục tiêu

- Biết thứ tự chuyển một app Android sang KMP.
- Nhận ra thư viện nào đang chặn việc chuyển.

## Thứ tự chuyển

1. **Thêm module `shared`** (KMP) vào project Android, app Android phụ thuộc vào nó.
2. **Chuyển model + logic thuần Kotlin** trước — thứ không import `android.*`.
3. **Thay thư viện chỉ-Android** bằng bản multiplatform:

    | Đang dùng | Thay bằng |
    |---|---|
    | Retrofit / OkHttp trực tiếp | Ktor client |
    | Room | Room bản multiplatform, hoặc SQLDelight |
    | Gson / Moshi | kotlinx.serialization |
    | `java.time` | kotlinx-datetime |
    | Hilt | Koin (hoặc truyền phụ thuộc bằng tay) |

4. **Chuyển ViewModel** — AndroidX ViewModel đã multiplatform (bài 8).
5. **Thêm target iOS**, viết UI iOS (SwiftUI) hoặc chuyển UI Compose sang Compose Multiplatform.

## Làm trên app MVP

Bài tập suy nghĩ: liệt kê thư viện trong `build.gradle` của một app Android bạn có, đánh dấu cái nào đã có bản multiplatform
(tìm trên [klibs.io](https://kotlinlang.org/docs/multiplatform/klibs-io.html)). Đó là bản đồ chuyển của bạn.

## Kiểm tra

- [ ] Có danh sách thư viện chặn + thư viện thay thế.
- [ ] Biết bước đầu tiên làm được ngay mà không phá app.

## Tiếp theo?

- Xong lộ trình! Quay lại [checklist MVP](../mvp/checklist.md).
- Muốn đào sâu: [Create a KMP library](https://kotlinlang.org/docs/multiplatform/create-kotlin-multiplatform-library.html) · [Compatibility guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html) · [Upgrading to AGP 9](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html).

## Docs gốc

[Make your app multiplatform](https://kotlinlang.org/docs/multiplatform/multiplatform-integrate-in-existing-app.html) · [Migrate advanced Android app](https://kotlinlang.org/docs/multiplatform/migrate-from-android.html)
