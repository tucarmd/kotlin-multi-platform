# Học Kotlin Multiplatform qua app Device Pulse

[![Site](https://img.shields.io/badge/site-tucarmd.github.io%2Fkotlin--multi--platform-C8643F)](https://tucarmd.github.io/kotlin-multi-platform/)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF)](https://kotlinlang.org/docs/multiplatform/get-started.html)

Lộ trình 14 bài, đi theo mục lục docs Kotlin Multiplatform chính chủ. Mỗi bài thêm một mảnh vào cùng một app:
**Device Pulse**, app đo pin, GPS và mạng của điện thoại, chạy Android và iOS với UI viết chung bằng Compose Multiplatform.

```
 Nền (1–5)          Giao diện (6–8)      Cảm biến thật (9–12)        Ra mắt (13–14)
 KMP · setup ·  →   Compose · res ·  →   pin · GPS · mạng ·      →   test ·
 structure ·        ViewModel · nav      lịch sử (SQLDelight)        publish
 share · deps
```

**Đọc online:** https://tucarmd.github.io/kotlin-multi-platform/

## Chạy site ở máy

```bash
pipx install "mkdocs<2" && pipx inject mkdocs "mkdocs-material==9.*"
mkdocs serve
```

Push lên `main` → GitHub Actions (`.github/workflows/pages.yml`) build và đăng GitHub Pages.

## Cấu trúc

- `docs/roadmap.md` — lộ trình 4 chặng
- `docs/lessons/NN-*.md` — 14 bài (thanh "Bài N/14" do `hooks/lesson_bar.py` chèn)
- `docs/project/` — app Device Pulse + checklist
- `docs/plan/` — đánh giá KMP và kế hoạch học
- `docs/reference/` — tài nguyên học, bản đồ docs gốc, từ điển, lỗi hay gặp
- `specs/` — thiết kế nội dung (không đăng lên site)

## Nguồn

Nội dung tóm tắt và bài tập tự viết bằng tiếng Việt. Kiến thức gốc: [Kotlin Multiplatform docs](https://kotlinlang.org/docs/multiplatform/get-started.html) của JetBrains — khi lệch phiên bản, tin docs gốc.

## License

MIT
