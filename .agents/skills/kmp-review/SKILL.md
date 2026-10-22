---
name: kmp-review
description: >-
  Review một app KMP trong repo kotlin-multi-platform xem có đúng KHUNG CHUNG không, và kiểm GATE cuối mỗi bước
  B1–B7: shared/ không có chuỗi UI và không import UI, không exception đi qua ranh giới core → UI, tiền là Long,
  rules không tự đọc đồng hồ, comment [App N · Bx] / TRAP: / PLATFORM: / TEST:, tên test hợp lệ trên
  Kotlin/Native, mỗi bước một commit. Có script audit tĩnh. Dùng khi nói "review app X", "check khung", "qua B3
  được chưa", "gate", "code này đúng khung chưa", "audit kmp".
---

# kmp-review — gate từng bước

## 1. Chạy audit tĩnh

```bash
.Codex/skills/kmp-review/scripts/audit.sh <App>
```

Script tìm những lỗi máy bắt được: `shared/` import `androidx`/`android.`/`UIKit`, có `Double` dính tới tiền,
có `Clock.System` hay `System.currentTimeMillis` trong `rules/`, file `.kt` thiếu nhãn `[App`, tên test trong
backtick chứa `,.:`, `throw` trong `core/` (đã bỏ qua chỗ trả lại `CancellationException`). Kết quả chỉ là gợi ý,
**đọc lại từng dòng rồi mới kết luận**. Những chỗ đã biết là không phải lỗi:

- `DevicePulse` có trước quy ước nhãn `[App N · Bx]`, nên thiếu nhãn ở đó là bình thường.
- Tỷ giá (`RateWatch`) là `Double` thì hợp lý; chỉ **số tiền người dùng nhập** mới bắt buộc là `Long`.
- `throw` trong `core/` mà có `catch` bao ngoài cũng trong `core/` (vd `CommandQueue` → `ObdCore`) thì không lọt ra UI.

## 2. Đọc tay những gì script không bắt được

| Luật | Cách kiểm |
|---|---|
| UI không tính toán gì | Trong `<X>Screen.kt` / `<X>View.swift` không có phép tính, không lọc, không định dạng tiền ngoài `Labels` |
| Core tính lại toàn bộ state từ input | Không có chỗ nào sửa một trường của `result` riêng lẻ |
| Lỗi là nhánh sealed | Mọi `try/catch` ở `data/` đều trả về `Result`/nhánh lỗi, không nuốt lỗi im lặng |
| Mỗi app chỉ một mảnh mới | So với cột "Mảnh mới" của app trong `docs/big-picture.md` |
| Comment giải thích *vì sao* | KDoc không chỉ nói lại code; `TRAP:` nói được sai thì sai ra sao |

## 3. Gate theo bước

| Bước | Qua khi |
|:-:|---|
| B1 | Test rules xanh trên JVM **và** iOS; mọi hàm trong `rules/` có KDoc ví dụ vào → ra |
| B2 | Test Core với fake xanh; State không chứa chuỗi hiển thị |
| B3 | Mảnh mới chạy được qua test hoặc log; `PLATFORM:` ghi chỗ hai bên làm khác nhau |
| B4 | Android lấy được core qua `koinViewModel()`, Swift qua `doInitKoin()` |
| B5–B6 | Ảnh chụp hai bên giống nhau, đủ mọi nhánh state (`kmp-run`) |
| B7 | Checklist "Xong khi" tick hết, README đủ 7 mục, `mkdocs build --strict` xanh (`kmp-docs`) |

Kiểm commit: script in ra `git log` gộp cả đường dẫn cũ `<App>/` và đường dẫn mới `apps/<App>/` (các app được dời vào `apps/` ở commit 81a6cd8).
App mới phải có mỗi bước một commit `feat(<scope>): Bx …`. Mấy app cũ làm trước khi có quy ước này nên gộp nhiều bước vào một commit.

## 4. Báo cáo

Báo cáo theo dạng: **Qua / Chưa qua** gate Bx, sau đó liệt kê từng lỗi kèm `file:line`, luật bị vi phạm và cách sửa
trong một câu. Người học tự sửa; chỉ sửa hộ khi họ yêu cầu.
