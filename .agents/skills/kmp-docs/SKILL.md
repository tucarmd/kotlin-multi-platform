---
name: kmp-docs
description: >-
  Viết và cập nhật tài liệu cho app KMP trong repo kotlin-multi-platform (B7): README.md của app đủ 7 mục theo
  khung, trang docs/apps/0N-<app>.md (bản đồ function thành link có số dòng, tick "Xong khi"), bảng ở
  docs/big-picture.md, nav trong mkdocs.yml, rồi mkdocs build --strict (CI chạy --strict). Dùng khi nói "viết
  README app X", "cập nhật docs", "B7", "thêm trang vào site", "sửa nav", "mkdocs strict lỗi", "bản đồ function",
  "chép bẫy vào README".
---

# kmp-docs — README + site

Lưu ý: có thể có session khác đang sửa repo cùng lúc. Trước khi sửa `mkdocs.yml` hay `docs/` thì chạy `git status`
và **chỉ commit file mình đã sửa**.

## 1. README của app (`apps/<App>/README.md`)

Đúng 7 mục, đúng thứ tự (mẫu: `apps/TipSplit/README.md`):

1. Một câu app làm gì + sơ đồ mermaid
2. Bảng Android vs iOS: UI, cách đọc state, API nền tảng
3. **Chạy**: lệnh Gradle, `xcodebuild`, lệnh giả lập
4. **Cấu trúc**: cây thư mục `[git]` / `[máy]`
5. **Bản đồ function**: Hàm · File (link `#L42`) · Làm gì · Vì sao
6. **Version và vì sao**: lấy từ `gradle/libs.versions.toml`
7. **Mấy cái bẫy đã gặp khi làm**: lấy từ `grep -rn "TRAP:" apps/<App>/`, chọn những bẫy quan trọng nhất

README viết tiếng Việt. Comment trong code vẫn viết tiếng Anh.

## 2. Trang site

- `docs/apps/0N-<app>.md`: đổi cột *File* của bản đồ function thành link GitHub có số dòng, tick `- [x]` ở mục
  "Xong khi", ghi `✅ đã có code` trong bảng tóm tắt.
- `docs/big-picture.md`: kiểm lại dòng của app trong bảng mục 2 (Core, mảnh mới, cú dứt điểm).
- `mkdocs.yml`: nav có ba tầng (tab → mục → trang). Trang app nằm trong tab **Thực hành**. Hook
  `hooks/lesson_bar.py` đọc nav đệ quy, nên đổi cấu trúc nav thì phải kiểm lại tooltip "Bài N/14".

## 3. Build

```bash
mkdocs build --strict      # CI dùng --strict: link gãy hay trang lạc nav đều làm deploy fail
mkdocs serve               # mở trang vừa sửa để xem tận mắt
```

Hay gặp lỗi `--strict` khi link tương đối trỏ ra ngoài `docs/` (vd `../apps/TipSplit/...`). Code app không nằm
trong `docs/`, nên phải link bằng URL GitHub đầy đủ `https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/...`.

Commit: `docs(<scope>): B7 README + function map + done checklist`.
