# Học bền & có thực chiến

Học xong 14 bài mà một tháng sau **không tự viết lại được** thì coi như chưa học. Trang này gom các cách luyện
giúp Kotlin "đóng cứng" vào tay, và các chỗ để luyện **như đi làm thật**: có đề, có người review, có hạn, có người dùng.

> **Công thức ngắn:** tự viết trước → có người/máy chấm → ôn lại theo khoảng cách → ship cho người khác dùng.
> Thiếu một trong bốn thì kiến thức trôi.

Muốn nhìn toàn cảnh mình đang ở đâu: [Sơ đồ lộ trình](../roadmap-map.md).

## 5 nguyên tắc để nhớ lâu { #nguyen-tac }

Đây là các nguyên tắc khoa học học tập đã được kiểm chứng nhiều lần (retrieval practice, spacing, interleaving,
deliberate practice, generation effect). Mỗi cái đổi thành một việc cụ thể khi học Kotlin:

| Nguyên tắc | Nghĩa là | Làm gì với Kotlin |
|---|---|---|
| **Tự nhớ lại** (retrieval) | Rút kiến thức ra từ đầu mạnh hơn đọc lại 3 lần | Đóng bài, mở file trắng, viết lại `callbackFlow` / `expect`-`actual` / `StateFlow` không nhìn. Kẹt chỗ nào là chỗ đó chưa thuộc |
| **Giãn cách** (spacing) | Ôn khi sắp quên thì nhớ lâu hơn ôn dồn | Viết lại cùng một tính năng sau **1 – 3 – 7 – 21 ngày** (bảng [ôn theo lịch](#on-theo-lich)) |
| **Trộn bài** (interleaving) | Xen nhiều dạng bài giúp biết *khi nào* dùng cái gì | Một buổi trộn: 1 bài Exercism + 1 bug Gradle + 1 màn Compose, thay vì 3 bài cùng một dạng |
| **Luyện có chủ đích** (deliberate) | Nhắm đúng chỗ yếu, có phản hồi ngay | Mỗi tuần chọn **1 chỗ yếu** từ [nhật ký lỗi](#nhat-ky-loi), luyện riêng nó, nhờ mentor/test chấm |
| **Tự tạo ra trước** (generation) | Tự nghĩ lời giải trước khi xem đáp án thì nhớ sâu hơn | Tự viết → mới bấm **3V** / hỏi AI → điền `TÔI CHỌN` / `VÌ` ([vòng 3 variant](../plan/kmp-review.md)) |

!!! warning "Cảm giác hiểu ≠ hiểu"
    Đọc code AI viết, xem video, đọc lại bài: thấy rất trôi, nhưng đó là *quen mặt*, không phải *nhớ được*.
    Thước đo duy nhất: **file trắng, không mạng, viết lại được, chạy được trên cả 2 nền tảng.**

## Bậc thang luyện tay { #bac-thang }

Đi từ nhỏ lên lớn. Mỗi bậc có **chỗ chấm điểm** riêng, không tự chấm cho mình.

| Bậc | Luyện gì | Ở đâu | Ai/cái gì chấm |
|:-:|---|---|---|
| 1 | Cú pháp, null safety, collection, lambda | [Kotlin Koans](https://play.kotlinlang.org/koans/overview) · [Kotlin tour](https://kotlinlang.org/docs/kotlin-tour-welcome.html) | Test có sẵn trong bài |
| 2 | Viết **idiomatic** Kotlin | [Exercism – Kotlin track](https://exercism.org/tracks/kotlin) (miễn phí, ~90 bài) | Máy phân tích code + **mentor người thật** review miễn phí |
| 3 | Khóa học trong IDE, có bài tập chấm tự động | [Hyperskill Academy plugin](https://blog.jetbrains.com/education/2025/12/09/hyperskill-gets-its-own-plugin-inside-jetbrains-ides/) · [JetBrains Academy plugin](https://plugins.jetbrains.com/plugin/10081-jetbrains-academy) | Checker trong IDE |
| 4 | Giải thuật, đọc đề dài, parse input | [Advent of Code in Kotlin](https://blog.jetbrains.com/kotlin/2025/11/advent-of-code-in-kotlin-2025/) · [template chính chủ](https://github.com/kotlin-hands-on/advent-of-code-kotlin-template) | Đáp án đúng/sai + so lời giải trên GitHub topic `aoc-…-in-kotlin` |
| 5 | App hoàn chỉnh 2 nền tảng | [Device Pulse](../project/index.md) → [5 app luyện tập](../apps/index.md) | Checklist "Xong khi" + test xanh trên JVM **và** iOS |
| 6 | **Thực chiến**: người khác dùng, người khác review | Xem [6 kiểu thực chiến](#thuc-chien) | User thật, maintainer, store review, crash report |

Gợi ý nhịp: bậc 1–2 song song với chặng A–B; bậc 4 làm vào tháng 12 (mùa Advent of Code) hoặc lấy đề năm cũ;
bậc 5–6 là phần chính sau khi xong 14 bài.

## 6 kiểu thực chiến { #thuc-chien }

"Thực chiến" = có **ràng buộc ngoài ý mình**: deadline, người review, người dùng, store. Chọn ít nhất 2.

| # | Kiểu | Làm cụ thể | Học được cái mà bài mẫu không dạy |
|:-:|---|---|---|
| 1 | **Ship lên store** | Đưa 1 trong 5 app lên Google Play *internal testing* + TestFlight, mời 5 người dùng thật | Ký app, privacy manifest, review của store, crash từ máy lạ |
| 2 | **Làm như đi làm** | Mỗi app: viết spec → GitHub Issues → branch → PR → CI xanh → tự review → merge → tag release | Quy trình [spec → sản phẩm](../plan/kmp-review.md), Definition of Done, viết PR mô tả rõ |
| 3 | **Clone tính năng app thật** | Chọn 1 màn của app đang dùng hằng ngày (danh bạ, đồng hồ bấm giờ, ghi chú…), làm lại bằng KMP trong 1 tuần | Tự chia nhỏ việc khi không có bài hướng dẫn |
| 4 | **Đóng góp open source** | Tìm issue nhãn `good first issue` trong repo KMP ([topic kotlin-multiplatform](https://github.com/topics/kotlin-multiplatform)), bắt đầu bằng sửa docs/sample | Đọc code lạ, theo quy ước của người khác, nhận review khó tính |
| 5 | **Thi / sự kiện** | [Kotlin Multiplatform Contest](https://kotlinconf.com/contest/) (sinh viên, mở khoảng tháng 9 – tháng 1), Advent of Code tháng 12 | Hạn chót cứng, làm cho người chấm đọc |
| 6 | **Dạy lại** | Viết 1 bài ngắn / quay 5 phút giải thích một khái niệm (vd `WhileSubscribed`), hoặc trả lời câu hỏi trên `#multiplatform` Kotlin Slack | Chỗ nào nói không ra lời là chỗ chưa hiểu |

!!! tip "Bản thực chiến tối thiểu cho mỗi app"
    Repo public · README có ảnh 2 nền tảng · ≥ 1 test cho mỗi hàm luật · CI GitHub Actions xanh ·
    1 issue thật do người khác mở (nhờ bạn dùng thử) và được bạn sửa đóng lại.

## Vòng tuần ổn định { #vong-tuan }

Học đều quan trọng hơn học nhiều. Vòng này khoảng **8–10 giờ/tuần**, đi được nhiều tháng không kiệt sức:

| Ngày | Việc (60–90 phút) | Kết quả phải có |
|---|---|---|
| T2 | Bài mới / bước mới của app: đọc 15', code phần còn lại | Chạy được trên **Android** |
| T3 | Làm tiếp, chạy **iOS** | Chạy được trên **iOS**, commit |
| T4 | **Phá rồi sửa** 1 lỗi ([danh sách](#pha-roi-sua)) + 1 bài Exercism | 1 dòng nhật ký lỗi, 1 bài nộp mentor |
| T5 | Viết test cho phần tuần này | Test xanh JVM + iOS simulator |
| T6 | **Ôn theo lịch**: viết lại 1 tính năng cũ trên file trắng | Bấm giờ, ghi lại số phút |
| T7 | Thực chiến: PR, ship bản test, hoặc đọc code 1 repo mẫu | 1 thứ người khác nhìn thấy được |
| CN | Nghỉ. Viết 3 dòng: học gì, kẹt gì, tuần sau làm gì | — |

Bỏ lỡ 1 ngày: **không bù gấp đôi**, chỉ đi tiếp. Bỏ lỡ cả tuần: quay lại T6 (ôn) trước khi học mới.

## Ôn theo lịch { #on-theo-lich }

Mỗi khái niệm lớn có một **bài viết lại** (rewrite drill). Ôn sau 1, 3, 7, 21 ngày kể từ lúc học. Đạt khi viết xong
trong thời gian mục tiêu mà **không mở docs**.

| Viết lại từ file trắng | Khái niệm khoá | Mục tiêu |
|---|---|---|
| `TipCalculator` + test ([app 1](../apps/01-tip-split.md)) | Hàm thuần, data class, `commonTest` | 20 phút |
| `BatterySensor` interface + `FakeBatterySensor` ([bài 4](../lessons/04-share-code.md)) | Interface thay `expect class`, `Reading` sealed | 15 phút |
| `callbackFlow` đọc pin Android ([bài 9](../lessons/09-battery.md)) | `callbackFlow`, `awaitClose`, unregister | 20 phút |
| `DashboardViewModel` với `stateIn(WhileSubscribed)` ([bài 8](../lessons/08-viewmodel-navigation.md)) | `StateFlow`, `viewModelScope`, combine | 25 phút |
| Ktor client + `MockEngine` test ([bài 11](../lessons/11-network.md), [bài 13](../lessons/13-testing.md)) | Engine theo nền tảng, serialization | 30 phút |
| Bảng SQLDelight + repository trả `Flow` ([bài 12](../lessons/12-history-sqldelight.md)) | `.sq`, driver theo nền tảng, `asFlow()` | 30 phút |

Mẹo: ghi các mốc ôn vào lịch điện thoại ngay khi học xong bài. Ghi số phút mỗi lần: con số giảm dần là bằng chứng
tiến bộ rõ nhất.

## Phá rồi sửa { #pha-roi-sua }

Cố tình gây lỗi, đọc thông báo lỗi, sửa lại. Gặp lỗi lúc *mình chủ động* thì lúc gặp thật không hoảng.

| Phá thế này | Sẽ thấy | Học được |
|---|---|---|
| Xoá một `actual` trong `iosMain` | Build iOS đỏ, Android vẫn xanh | Vì sao phải build **cả 2** trước khi commit |
| Thêm `koin-android` vào `commonMain` | `Unresolved reference` / sync lỗi | Thư viện thuộc source set nào |
| Bỏ biến giữ `CLLocationManager` delegate | GPS iOS im lặng, không lỗi | Tham chiếu *weak*, GC của Kotlin/Native |
| Bỏ `awaitClose { … }` trong `callbackFlow` | Exception / rò listener | Vòng đời Flow lạnh |
| Bỏ quyền `INTERNET` trong Manifest | Android không gọi được mạng | Manifest, quyền |
| Bỏ plugin serialization | `Serializer for class … is not found` | Plugin compiler khác thư viện |
| Cập nhật state UI từ thread nền trên iOS | Crash / UI không đổi | `Dispatchers.Main` |
| Sửa `<=` thành `<` trong `batteryLevel` | Test ngưỡng đỏ | Test bắt lỗi biên |
| Đổi Gradle JDK sang 25 | `Gradle JVM version incompatible` | Toolchain, `gradle-daemon-jvm.properties` |

Mỗi lần phá xong ghi vào [nhật ký lỗi](#nhat-ky-loi). Đáp án sửa: [Lỗi hay gặp](../reference/troubleshooting.md).

## Nhật ký lỗi { #nhat-ky-loi }

Một file `NOTES.md` trong repo, mỗi lỗi 4 dòng. Sau 2 tháng đây là tài liệu quý nhất bạn có:

```markdown
### 2026-10-15 · GPS iOS không trả toạ độ
- Triệu chứng: không crash, không log, card quay mãi
- Nguyên nhân: delegate CLLocationManager không được giữ → bị GC (weak ref)
- Cách sửa: lưu delegate vào field của class
- Lần sau nhận ra bằng: "callback iOS im lặng" → nghĩ ngay tới weak delegate
```

Dòng cuối là quan trọng nhất: nó biến một lỗi thành **phản xạ**.

## Đo tiến bộ { #do-tien-bo }

Không đo thì không biết mình đang "ổn định" hay đang "trôi". 4 con số, xem lại cuối mỗi tháng:

| Con số | Cách đo | Đang tốt khi |
|---|---|---|
| Thời gian viết lại | Bảng [ôn theo lịch](#on-theo-lich) | Giảm dần, chạm mục tiêu |
| Số lần tra docs / AI cho 1 tính năng | Đếm tay khi làm app mới | App sau ít hơn app trước |
| Test xanh 2 nền tảng | `testDebugUnitTest` + `iosSimulatorArm64Test` | Không có tính năng nào chỉ chạy Android |
| Thứ người khác nhìn thấy | PR merged, bản test trên store, bài viết | ≥ 1 mỗi tháng |

## Bẫy làm học "không bền" { #bay }

<div class="box box--danger" markdown>

- **Tutorial hell**: xem hết khoá này tới khoá khác, chưa ship app nào. → Sau mỗi 2 giờ xem phải có 1 commit.
- **AI viết hộ**: chạy được nhưng không giải thích được. → Luật: dòng nào AI viết thì phải giải thích lại được bằng lời.
- **Chỉ chạy Android**: iOS để "làm sau". → Cổng commit: build iOS xanh.
- **Học quá rộng**: vừa KMP vừa web vừa backend. → Xong 5 app rồi mới sang [Kotlin làm web](../plan/kotlin-web.md).
- **Không ôn**: học bài mới mỗi ngày, bài cũ quên hết. → Giữ T6 là ngày ôn, không đổi.
- **Chạy nước rút rồi bỏ**: 6 tuần 4 giờ/ngày rồi nghỉ 3 tháng. → Thà 1 giờ/ngày trong 6 tháng.

</div>

## Nguồn

- [Exercism – Kotlin track](https://exercism.org/tracks/kotlin): bài tập + mentor miễn phí.
- [Advent of Code in Kotlin 2025](https://blog.jetbrains.com/kotlin/2025/11/advent-of-code-in-kotlin-2025/) · [template](https://github.com/kotlin-hands-on/advent-of-code-kotlin-template).
- [Hyperskill Academy plugin cho IDE JetBrains](https://blog.jetbrains.com/education/2025/12/09/hyperskill-gets-its-own-plugin-inside-jetbrains-ides/).
- [Kotlin Multiplatform Contest](https://kotlinconf.com/contest/) · [bản 2026](https://blog.jetbrains.com/kotlin/2025/10/kotlin-multiplatform-contest-2026/).
- [Kotlin Koans](https://play.kotlinlang.org/koans/overview) · thêm ở [Tài nguyên học chính chủ](../reference/learning-resources.md).
