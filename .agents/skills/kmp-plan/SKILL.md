---
name: kmp-plan
description: >-
  Lên plan cho một app KMP trong repo kotlin-multi-platform TRƯỚC khi code: chốt hợp đồng State / Event, xác định
  "mảnh mới duy nhất" của app, liệt kê luật có tiền/thời gian/làm tròn, rồi viết trang docs/apps/0N-<app>.md đúng 9
  mục như 01-tip-split.md (tóm tắt · màn hình · phạm vi · kiến trúc · 7 bước · bản đồ function dự kiến · test ·
  xong khi · tham khảo). Dùng khi nói "plan app X", "lên kế hoạch app mới", "state có gì event nào", "mảnh mới của
  app là gì", "viết trang plan", "chốt hợp đồng", "B1 bắt đầu từ đâu".
---

# kmp-plan — chốt hợp đồng trước khi code

Đây là **khoảnh khắc bung sức số 1** trong `docs/big-picture.md`: plan sai thì mọi bước sau sai theo.

## 1. Hỏi đủ 5 câu (hỏi người dùng nếu chưa rõ, đừng đoán)

1. App làm gì, nói trong **một câu**?
2. Có những màn nào, mỗi màn có những action nào?
3. **Mảnh mới duy nhất** so với các app trước là gì? Đối chiếu bảng mục 2 của `big-picture.md`. Nếu ra hai mảnh
   mới thì cắt bớt phạm vi, dồn mảnh kia sang cột "Làm sau".
4. Có tiền, thời gian, đơn vị đo hay làm tròn không? Nếu có thì đó là `rules/` và phải có ví dụ vào → ra.
5. Lỗi nào người dùng sẽ thấy? Mỗi lỗi thành một nhánh của sealed class.

## 2. Viết hợp đồng dưới dạng code Kotlin (chưa viết thân hàm)

```kotlin
data class <X>Input(...)                       // những gì người dùng nhập hoặc chọn
sealed class <X>Result { Invalid(..) / Ready(..) / Loading / Error(..) }
data class <X>State(val input: <X>Input, val result: <X>Result)
// Event = các hàm public của <X>Core: addPerson(), setBill(text: String)…
```

Giữ đúng luật: State là dữ liệu thuần, **không có chuỗi hiển thị**, tiền là `Long`, thời gian là tham số truyền vào.

## 3. Viết trang plan

Đặt ở `docs/apps/0N-<app>.md`, **copy khung của `docs/apps/01-tip-split.md`** và giữ đúng 9 mục:

| Mục | Ghi gì |
|---|---|
| 1. Tóm tắt | Bảng: cấp độ · mới học · cần trước · thư mục code · thời gian |
| 2. Màn hình | Màn · hiện gì · action |
| 3. Phạm vi | Có / Làm sau |
| 4. Kiến trúc | Mermaid `UI → Core → rules/data/platform` với tên class thật |
| 5. 7 bước làm | B1–B7, mỗi bước ở app này làm gì |
| 6. Bản đồ function | Hàm · File · Làm gì · Vì sao (cột File để **dự kiến**, khi code xong sửa thành link) |
| 7. Test | Test nào giữ luật nào |
| 8. Xong khi | Checklist `- [ ]`, có ít nhất một mục chạy máy thật |
| 9. Tham khảo | Link docs, concepts |

Rồi:

- thêm một dòng vào bảng mục 2 của `docs/big-picture.md`;
- thêm trang vào nav `mkdocs.yml` ở nhóm **Thực hành** (đổi nav thì làm theo `kmp-docs`);
- chạy `mkdocs build --strict`.

Commit: `docs(<scope>): plan page, State/Event contract`.

Tiếp theo: `kmp-implement` B1.
