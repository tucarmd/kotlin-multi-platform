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

## Demo mẫu

**[`DevicePulse/`](DevicePulse/)**: bản code hoàn chỉnh của app sau bài 14, chạy Android + iOS. Có pin, GPS, mạng, đo độ trễ,
lịch sử (SQLDelight) và test chung.
Kiến trúc **KMP core + UI native**: logic trong `shared/`, Android vẽ bằng Jetpack Compose, iOS vẽ bằng SwiftUI.

```bash
cd DevicePulse
./gradlew :androidApp:installDebug     # Android
open iosApp/iosApp.xcodeproj           # iOS: bấm Run
```

- Cách chạy, version thư viện, lệnh giả lập cảm biến: [DevicePulse/README.md](DevicePulse/README.md)
- Bài nào ứng với file nào: [Code mẫu](https://tucarmd.github.io/kotlin-multi-platform/project/source-code/)
- Mockup 8 màn: [DevicePulse/design/mockups/](DevicePulse/design/mockups/README-MOCKUPS.md)

## 5 app luyện tập

Làm xong Device Pulse thì luyện thêm 5 app, từ dễ đến khó, **cùng kiến trúc, cùng cây thư mục, cùng 7 bước, cùng quy ước comment**:

| # | App | Mới học thêm |
|:-:|---|---|
| 1 | Tip Split | logic thuần `commonMain`, `StateFlow`, test |
| 2 | Pocket Notes | SQLDelight CRUD, điều hướng |
| 3 | Rate Watch | Ktor + cache offline-first |
| 4 | Trip Log | GPS chạy ngầm, bản đồ, xuất GPX |
| 5 | OBD Link | BLE (Kable), hàng đợi lệnh, nhiều module |

Chi tiết từng app và quy ước comment: [5 app luyện tập](https://tucarmd.github.io/kotlin-multi-platform/apps/).

## Tham khảo

| | |
|---|---|
| [Tài nguyên học chính chủ](https://tucarmd.github.io/kotlin-multi-platform/reference/learning-resources/) | Kotlin for Education, Koans, khóa Android của Google |
| [Repo mẫu tham khảo](https://tucarmd.github.io/kotlin-multi-platform/reference/sample-repos/) | Template chính chủ, đáp án tutorial JetBrains, app KMP thật (KotlinConf, KaMPKit, PeopleInSpace…) gắn theo bài |
| [Bản đồ docs chính chủ](https://tucarmd.github.io/kotlin-multi-platform/reference/docs-map/) | Trang docs kotlinlang.org nào ứng với bài nào |
| [Lỗi hay gặp](https://tucarmd.github.io/kotlin-multi-platform/reference/troubleshooting/) | Gradle sync, Xcode, framework iOS |

Repo hay mở nhất:

- [Kotlin/KMP-App-Template](https://github.com/Kotlin/KMP-App-Template): template chính chủ, UI chung
- [kotlin-hands-on/kmp-networking-and-data-storage](https://github.com/kotlin-hands-on/kmp-networking-and-data-storage): Ktor + SQLDelight + Koin
- [touchlab/KaMPKit](https://github.com/touchlab/KaMPKit): kiến trúc gần Device Pulse nhất
- [terrakok/kmp-awesome](https://github.com/terrakok/kmp-awesome): tìm thư viện KMP

## Chạy site ở máy

```bash
pipx install "mkdocs<2" && pipx inject mkdocs "mkdocs-material==9.*"
mkdocs serve
```

Push lên `main` → GitHub Actions (`.github/workflows/pages.yml`) build và đăng GitHub Pages.

## Cấu trúc

- `docs/roadmap.md` — lộ trình 4 chặng
- `docs/lessons/NN-*.md` — 14 bài (thanh "Bài N/14" do `hooks/lesson_bar.py` chèn)
- `DevicePulse/` — code hoàn chỉnh của app (Android + iOS)
- `docs/project/` — app Device Pulse, code mẫu, checklist
- `docs/apps/` — 5 app luyện tập (dễ → khó) + khung chung, quy ước comment
- `docs/plan/` — đánh giá KMP, kế hoạch học, nghiên cứu Kotlin làm web
- `docs/reference/` — tài nguyên học, repo mẫu, bản đồ docs gốc, từ điển, lỗi hay gặp
- `specs/` — thiết kế nội dung (không đăng lên site)

## Nguồn

Nội dung tóm tắt và bài tập tự viết bằng tiếng Việt. Kiến thức gốc: [Kotlin Multiplatform docs](https://kotlinlang.org/docs/multiplatform/get-started.html) của JetBrains — khi lệch phiên bản, tin docs gốc.

## License

MIT
