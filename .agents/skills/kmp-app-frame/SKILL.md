---
name: kmp-app-frame
description: >-
  CỬA VÀO của bộ skill kmp-* cho repo kotlin-multi-platform (apps/DevicePulse, TipSplit, PocketNotes, RateWatch,
  TripLog, ObdLink): xem app đang ở bước nào trong B1–B7 rồi chuyển sang đúng skill con (kmp-new · kmp-plan ·
  kmp-implement · kmp-test · kmp-auto-test · kmp-run · kmp-review · kmp-docs). Dùng khi nói "làm app KMP",
  "app X đang ở bước nào", "làm tiếp app X", "bước tiếp theo", "check app có đúng khung không", hoặc khi
  không rõ nên gọi skill kmp nào. Kiến thức KMP chung (Ktor, SQLDelight, expect/actual) → kotlin-kmp.
---

# KMP app frame — cửa vào bộ kmp-*

Mọi app nằm ở `apps/<App>/`, cùng một vòng: **UI bắn event → Core đổi input → rules tính lại → Core phát state mới
→ UI vẽ lại.** Luật gốc ở trong repo, skill chỉ trỏ tới, không chép lại:

- `docs/apps/conventions.md`: kiến trúc, cây thư mục, B1–B7, quy ước comment, README 7 mục
- `docs/big-picture.md`: app nào khác app nào ở đâu, lúc nào bung sức
- `docs/appendix/`: kim tự tháp test, fake/mock, coroutine test, coverage

## Bộ skill

```text
kmp-new ──► kmp-plan ──► kmp-implement (B1…B6) ──► kmp-docs (B7)
               │              │   ▲                    ▲
               │              ▼   │ sửa                │
               │          kmp-test · kmp-auto-test · kmp-run
               │              │
               └──────────► kmp-review (gate cuối mỗi bước)
```

| Đang cần | Skill |
|---|---|
| Tạo `apps/<App>/` mới từ app có sẵn | `kmp-new` |
| Chốt State / Event / mảnh mới, viết trang `docs/apps/0N-<app>.md` | `kmp-plan` |
| Code một bước Bx, một commit | `kmp-implement` |
| Test `commonTest`: rules, Core với fake, Kover | `kmp-test` |
| UI test hai nền tảng: Compose test, XCUITest, Maestro | `kmp-auto-test` |
| Build, cài, chạy, chụp màn hình Android + iOS | `kmp-run` |
| Kiểm app có đúng khung, qua gate bước chưa | `kmp-review` |
| README, trang site, nav, `mkdocs build --strict` | `kmp-docs` |

## Tìm bước đang dở

```bash
git log --oneline -- apps/<App> <App> | grep -oE 'B[1-7]' | sort -u   # bước đã commit (gộp cả đường dẫn cũ trước khi dời vào apps/)
ls apps/<App>/shared/src/commonMain/kotlin/dev/<app>/              # model rules core data platform di
grep -n '\- \[ \]' docs/apps/0N-<app>.md                           # checklist "Xong khi" còn trống
```

Bước nhỏ nhất chưa có commit là bước đang làm. Chưa có trang plan thì về `kmp-plan`, kể cả khi đã có code.

## Skill ngoài bộ

| Việc | Skill |
|---|---|
| Ktor, SQLDelight, expect/actual, ViewModel chung | `kotlin-kmp` |
| Lên AGP 9 · bỏ CocoaPods sang SPM · build iOS chậm | `kotlin-tooling-agp9-migration` · `kotlin-tooling-cocoapods-spm-migration` · `kotlin-tooling-native-build-performance` |
| Lỗi Kotlin lạ | `docs/issues/` (K001–K900), phân loại bằng `docs/reference/kotlin-issue-types.md` |
