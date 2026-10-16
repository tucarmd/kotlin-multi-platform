[🏠 README](../../README.md) · Plan: [README-KMP-DOCS-SITE-PLAN.md](README-KMP-DOCS-SITE-PLAN.md)

# Spec — trang web tài liệu (MkDocs) cho native-device-monitor-kmp

> Tạo: 2026-10-16 · Trạng thái: **Đã duyệt 2026-10-16**

## 1. Mục tiêu

Đọc tài liệu học của repo này thành **một trang web** (menu, tìm kiếm, sơ đồ mermaid vẽ thành hình) bằng
`mkdocs serve` / `mkdocs build --strict`, **không phải viết lại** các file `.md` đang có. Trên GitHub các file đó vẫn
đọc được y như cũ.

## 2. Hiện trạng

| Thứ | Có gì |
|---|---|
| Tài liệu | `docs/plan/README-KMP-MIGRATION.md` (plan + tiến độ), `docs/README-KMP-HOW-IT-WORKS.md` (sơ đồ, luồng hàm, bảng SDK) |
| Ảnh | 6 ảnh trong `docs/assets/` |
| Sơ đồ | 4 khối ` ```mermaid ` trong trang how-it-works |
| Công cụ | mkdocs 1.6.1 + mkdocs-material 9.7.7 (pipx) đã có sẵn trên máy |

**Vướng chính:** khoảng 20 link trỏ ra code **ngoài** `docs/` (vd `../shared/src/commonMain/.../App.kt#L22`) và
`../README.md`. MkDocs chỉ biết file trong `docs/`, nên `--strict` báo lỗi các link này.

## 3. Thiết kế

- `mkdocs.yml` ở gốc repo, `docs_dir: docs`, xuất ra `site/` (bỏ vào `.gitignore`).
- Theme **material**, tiếng Việt, sáng/tối, cấu hình giống site
  [kotlin-multi-platform](https://tucarmd.github.io/kotlin-multi-platform/) cho quen tay.
- Mermaid: `pymdownx.superfences` với `custom_fences` cho ` ```mermaid ` (Material tự tải mermaid.js).
- `docs/index.md`: trang chủ của site, tóm tắt README gốc và link tới các trang.
- **Hook `docs/hooks/code_links.py`** (hook của MkDocs, `on_page_markdown`): lúc build, link tương đối nào trỏ
  **ra ngoài** `docs/` thì đổi thành
  `https://github.com/tucarmd/native-device-monitor-kmp/blob/main/<đường dẫn>#Lxx`. File `.md` gốc không đổi.
  Link trong `docs/` giữ nguyên để MkDocs tự kiểm tra.
- Nav theo thứ tự học: Trang chủ → Kế hoạch → Chạy thế nào → Kế hoạch site (spec/plan này).

## 4. Ngoài phạm vi

- Viết thêm bài học mới. Site này chỉ dựng khung cho tài liệu đang có.
- Đổi repo sang public.

## 5. Lưu ý

- Repo đang **private**: link code trên site chỉ mở được với ai có quyền vào repo.
- GitHub Pages cho repo private cần gói trả phí. Nếu gói hiện tại không có thì chỉ xem ở máy bằng `mkdocs serve`.
