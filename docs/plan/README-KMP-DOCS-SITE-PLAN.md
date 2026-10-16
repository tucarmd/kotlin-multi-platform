[🏠 README](../../README.md) · Spec: [README-KMP-DOCS-SITE-SPEC.md](README-KMP-DOCS-SITE-SPEC.md)

# Plan — trang web tài liệu (MkDocs)

> Tạo: 2026-10-16 · Mỗi phase là **một commit** `docs(site): S<n> — …`.
> Đánh số **S** (Site) để khỏi trùng D1–D5 (các quyết định) và P0–P7 (phase của app) trong
> [README-KMP-MIGRATION.md](README-KMP-MIGRATION.md).

| Phase | Làm gì | File | Vì sao | Xong khi |
|---|---|---|---|---|
| **S1** | Ghi spec + plan | `docs/plan/README-KMP-DOCS-SITE-SPEC.md`, `README-KMP-DOCS-SITE-PLAN.md` | Plan nằm trong repo, đọc git log là biết phase nào làm gì | 2 file đọc được trên GitHub |
| **S2** | Cấu hình tối thiểu + trang chủ | `mkdocs.yml`, `docs/index.md`, `.gitignore` (`site/`) | Có khung để `mkdocs serve` lên web | `mkdocs build` chạy; còn warning link code (S3 xử lý) |
| **S3** | Hook đổi link code → GitHub | `hooks/code_links.py` (gốc repo, để file .py không bị chép vào site), `mkdocs.yml` (`hooks:`) | `--strict` không bị chặn mà `.md` vẫn đọc đúng trên GitHub | `mkdocs build --strict` 0 warning; link `#Lxx` mở đúng dòng |
| **S4** | Nav, theme, mermaid | `mkdocs.yml` | Menu theo thứ tự học; 4 sơ đồ vẽ thành hình | `mkdocs serve` + screenshot: 4 sơ đồ hiện |
| **S5** | Deploy GitHub Pages (nếu gói GitHub cho phép Pages trên repo private) | `.github/workflows/pages.yml` | Push lên `main` là site tự cập nhật | Lần chạy Actions đầu tiên xanh, mở được URL |

## Chạy thử

```bash
mkdocs serve            # http://127.0.0.1:8000
mkdocs build --strict   # giống CI
```

## Tiến độ

- [x] S1 · [x] S2 · [x] S3 · [x] S4 · [ ] S5
