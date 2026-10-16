# Plan: Phụ lục "Test, Automation, Debug & Tool" cho Kotlin

Date: 2026-10-15 · Status: draft (chờ user duyệt)

## Mục tiêu

Thêm tab **Phụ lục** trên site, gồm 4 mục: Unit test · Automation test · Debug trong Android Studio · Tool hỗ trợ code.
Đọc xong và làm theo thì người học:

1. Tự viết test cho mọi hàm luật, ViewModel, Flow, repository trong `commonMain`, chạy xanh trên JVM **và** iOS.
2. Có một bộ automation chạy bằng **một lệnh** ở máy và trên CI: unit → UI test → E2E 2 nền tảng.
3. Debug được bằng công cụ chứ không đoán: breakpoint có điều kiện, coroutine debugger, Layout Inspector, Database/Network Inspector.
4. Có bộ tool đã cài sẵn và vài tool **tự viết** (live template, Gradle task, plugin) để code nhanh hơn.

## Hiện trạng (đã kiểm repo)

| Có rồi | Chưa có |
|---|---|
| 6 app (DevicePulse, TipSplit, PocketNotes, RateWatch, TripLog, ObdLink) đều có `commonTest`; 4 app dùng Turbine | Không app nào có UI test (`androidTest`), XCUITest, Maestro |
| Bài 13 dạy `kotlin.test`, `runTest`, Turbine, `MockEngine` cho Device Pulse | Không có coverage (Kover), lint (detekt/ktlint), lệnh "chạy hết" |
| kmp-review §7 có bảng tầng automation, mới ở mức liệt kê | Chưa có trang debug, chưa có trang tool |
| Plugin 3V (`tools/studio-variants-plugin/`) | CI chỉ build site, chưa chạy test của app |

Stack đang dùng: Kotlin 2.4.20 · AGP 8.13.2 · coroutines 1.11.0 · Koin 4.2.2 · Turbine 1.2.1 · Ktor 3.5.1 · SQLDelight 2.4.1.
Kiến trúc app: **KMP core + UI native** (Compose cho Android, SwiftUI cho iOS), nên UI test tách theo từng nền tảng, còn E2E dùng chung bằng Maestro.

## Nguyên tắc viết

- **Mọi ví dụ chạy trên app có thật trong repo**, không viết code minh hoạ suông. App mẫu chính là **TipSplit** (đơn giản nhất, UI native). Phần mạng và DB lấy từ RateWatch và PocketNotes.
- Mỗi trang dùng khuôn giống Sơ đồ lộ trình: **Làm · Xong khi · Hay vấp**, có bài tập cuối trang.
- Code đi kèm docs được commit vào app thật (testTag, `androidTest`, `.maestro/`, cấu hình detekt/Kover), đã chạy xanh rồi mới viết docs.
- Công cụ nào còn Alpha/Experimental thì ghi rõ nhãn (Compose Preview Screenshot Testing, Journeys).

## Cấu trúc tab "Phụ lục"

```
Phụ lục
├── index.md                      Tổng quan: kim tự tháp test KMP, chọn công cụ theo tầng, lộ trình 4 mục
├── P1 · Unit test
│   ├── unit-basics.md            kotlin.test, đặt tên, Arrange/Act/Assert, test bảng (table-driven)
│   ├── unit-coroutines-flow.md   runTest, thời gian ảo, Dispatchers.setMain, Turbine
│   ├── unit-fakes-mocks.md       Fake vs mock · Mokkery (KMP) · MockK (chỉ JVM) · Ktor MockEngine · SQLDelight in-memory · Koin verify
│   └── unit-coverage.md          Kover: report HTML, ngưỡng tối thiểu, cái gì không cần cover
├── P2 · Automation test
│   ├── auto-strategy.md          Tự động hoá cái gì, không cái gì; ma trận máy; xử lý test chập chờn (flaky)
│   ├── auto-android-ui.md        Compose UI test: semantics, testTag, createAndroidComposeRule, chờ idle
│   ├── auto-ios-ui.md            XCUITest tối thiểu cho SwiftUI, accessibilityIdentifier trùng tên testTag
│   ├── auto-maestro.md           Một flow YAML chạy cả Android và iOS, quay video, chạy trên CI
│   ├── auto-screenshot.md        Roborazzi (ổn định) vs Compose Preview Screenshot Testing (alpha)
│   └── auto-ci.md                GitHub Actions: ubuntu (unit + Android), macOS (iOS), emulator runner, artifact report
├── P3 · Debug trong Android Studio
│   ├── debug-breakpoints.md      Breakpoint dòng/điều kiện/logpoint/exception/watchpoint, Evaluate, đổi giá trị lúc chạy
│   ├── debug-coroutines.md       Tab Coroutines, chế độ debug của kotlinx.coroutines, debug Flow
│   ├── debug-inspectors.md       Logcat (cú pháp lọc mới), Layout Inspector + số lần recompose, Database/Network/Background Task Inspector
│   ├── debug-ios-kotlin.md       Debug Kotlin trên iOS: plugin KMP trong Studio, xcode-kotlin, đọc crash Kotlin/Native
│   └── debug-bug-hunt.md         Bài tập: 6 bug cài sẵn trong branch, tìm bằng đúng công cụ
└── P4 · Tool hỗ trợ code
    ├── tools-find.md             Tool nên cài, chia theo việc: lint/format, test, adb, JSON → Kotlin, AI (Gemini, Claude Code, Journeys)
    └── tools-build.md            Tự làm tool: live template, file template, Gradle task `checkAll`, script adb, mở rộng plugin 3V thêm "Sinh test"
```

Khoảng 18 trang. Mỗi trang ngắn, khoảng 1 màn hình code và 1 bảng.

## Nội dung chính từng mục

### P1 · Unit test

| Trang | Dạy | Làm trên app | Xong khi |
|---|---|---|---|
| Cơ bản | `@Test`, `assertEquals`/`assertFailsWith`, test bảng nhiều input, đặt tên test an toàn cho Kotlin/Native | Viết lại `RulesTest` TipSplit theo kiểu bảng | `allTests` xanh; giải thích được vì sao tên test không chứa dấu phẩy |
| Coroutine & Flow | `runTest`, `advanceTimeBy`, `StandardTestDispatcher`, `Dispatchers.setMain`, Turbine `test { awaitItem() }` | Test `SplitViewModel` / ViewModel của RateWatch (debounce, loading → data → error) | Test không có `delay` thật, chạy < 1 giây |
| Fake & mock | Khi nào fake, khi nào mock; **Mokkery** (mock chạy được trên cả iOS) vs MockK (chỉ JVM); `MockEngine`; SQLDelight driver in-memory; `Koin.verify()` | RateWatch: mạng lỗi → đọc cache; PocketNotes: CRUD trên DB in-memory | Repository test được không cần mạng, không cần máy |
| Coverage | Kover: `koverHtmlReport`, `koverVerify` với ngưỡng, loại trừ code UI và code sinh | Bật Kover cho TipSplit, đặt ngưỡng 80% cho `rules/` | Report HTML mở được; dưới ngưỡng thì build đỏ |

Hay vấp: test chạy JVM xanh nhưng iOS đỏ (tên test, `Dispatchers.Main`), quên `setMain`, test Flow treo vì `stateIn` không có subscriber, mock thư viện chỉ có cho JVM.

### P2 · Automation test

| Tầng | Công cụ | Ví dụ thật |
|---|---|---|
| UI Android | Compose UI test (`ui-test-junit4`), chạy bằng `connectedAndroidTest` | TipSplit: nhập `100.000`, 3 người → thấy `37.000đ`; input sai → báo lỗi |
| UI iOS | XCUITest, `accessibilityIdentifier` đặt **cùng tên** testTag Android | Cùng kịch bản trên SwiftUI |
| E2E 2 nền tảng | **Maestro**: một file YAML, `maestro test` cho cả emulator lẫn simulator, có `maestro record` để quay video | `.maestro/split-bill.yaml` dùng chung cho cả 2 bản TipSplit; RateWatch tắt mạng → hiện cache |
| Screenshot | Roborazzi (chạy trên JVM, ổn định); Compose Preview Screenshot Testing (alpha, dùng `@Preview`) | Chụp màn Split ở 3 trạng thái, so ảnh khi sửa UI |
| AI | Android Studio **Journeys** (Gemini, thử nghiệm): viết bước test bằng lời | Chỉ giới thiệu và so với Maestro; không dùng làm cổng CI |
| CI | GitHub Actions: job ubuntu chạy `allTests` JVM + Android; job macOS chạy iOS test + Maestro trên simulator; job emulator chạy `connectedAndroidTest` | Workflow `apps-test.yml`, chỉ chạy khi thư mục app đó đổi |

Quy ước then chốt: **một bảng tên id cho mỗi app** (vd `split_bill_input`, `split_people_plus`, `split_result_per_person`), dùng chung cho testTag Compose, accessibilityIdentifier SwiftUI và selector Maestro. Có bảng này thì một flow Maestro chạy được trên cả 2 nền tảng.

### P3 · Debug trong Android Studio

- **Breakpoint đủ loại**: có điều kiện, logpoint (*Evaluate and log*, không dừng app), exception breakpoint (dừng ngay chỗ ném lỗi), field watchpoint. Kèm Evaluate Expression, *Set Value*, *Drop Frame*.
- **Coroutine**: tab Coroutines trong Debugger, bật chế độ debug của kotlinx.coroutines để tên coroutine hiện trong log, debug Flow bằng `onEach` + logpoint.
- **Inspector**: Logcat với cú pháp `package:mine level:warn tag:Battery`; Layout Inspector đếm số lần recompose; Database Inspector mở DB SQLDelight của PocketNotes; Network Inspector cho Ktor engine OkHttp; Background Task Inspector cho TripLog.
- **Kotlin trên iOS**: chạy và đặt breakpoint Kotlin bằng plugin KMP; trong Xcode dùng **xcode-kotlin**; đọc crash Kotlin/Native và dSYM.
- **Bug hunt**: branch `practice/bug-hunt` cài sẵn 6 bug (sai ngưỡng, Flow không hủy, recompose vô hạn, query sai bảng, delegate iOS bị GC, gọi UI ngoài main thread). Mỗi bug ghi công cụ nên dùng để tìm, đáp án để trong mục thu gọn.

### P4 · Tool hỗ trợ code

**Tìm và cài** (bảng: tool · làm gì · dùng khi · trạng thái):
- Có sẵn trong IDE: plugin Kotlin Multiplatform, Compose Hot Reload (repo đã có `tools/hot-reload-demo/`), Gemini / Journeys, App Inspection.
- Chất lượng code: **detekt**, **ktlint** (Gradle + plugin IDE), Kover, plugin cập nhật version catalog.
- Tiện ích: ADB Idea, JSON To Kotlin Class, Key Promoter X, String Manipulation.
- AI: Claude Code trong terminal của Studio, plugin 3V của repo.

**Tự làm** (mỗi tool có code thật trong repo):
1. **Live template**: `ktest` (khung test), `rtest` (`runTest` + Turbine), `ttag` (testTag theo bảng id). Xuất file XML để import.
2. **File template**: "Kotlin Test Class" sinh sẵn package và import.
3. **Gradle task `checkAll`**: detekt → `allTests` → `koverVerify` → `assembleDebug`, một lệnh trước khi commit, dùng chung cho 6 app.
4. **Script `scripts/dev.sh`**: chạy app, xoá dữ liệu, cấp/thu quyền, giả lập pin/GPS, chạy Maestro. Gom các lệnh adb/simctl rải rác trong docs.
5. **Mở rộng plugin 3V**: thêm action *Generate tests* bên cạnh *Show 3 Variants*. Bấm ở gutter của một `fun`, gọi `claude -p` sinh test vào `commonTest`, có đoạn `// TÔI THÊM CASE:` để người học tự viết thêm.

## Thứ tự làm

| Đợt | Nội dung | Ước lượng | Kết quả kiểm được |
|:-:|---|---|---|
| 1 | Khung tab Phụ lục + `index.md` + P1 (4 trang) + Kover cho TipSplit | 1 buổi | `allTests` + `koverHtmlReport` xanh; site build strict |
| 2 | P2: bảng id + testTag/accessibilityIdentifier cho TipSplit, Compose UI test, XCUITest, Maestro flow, trang strategy | 1–2 buổi | `connectedAndroidTest` xanh trên emulator; `maestro test` xanh cả emulator lẫn simulator, có video |
| 3 | P2 tiếp: screenshot (Roborazzi), workflow `apps-test.yml` | 1 buổi | CI xanh trên GitHub cho TipSplit |
| 4 | P3: 5 trang debug + branch bug-hunt | 1 buổi | Mỗi bug tái hiện được và tìm ra được bằng công cụ đã ghi |
| 5 | P4: trang tool + live template, `checkAll`, `dev.sh`, action *Generate tests* | 1–2 buổi | Import template dùng được; `./gradlew checkAll` chạy ở 6 app; plugin build ra zip cài được |
| 6 | Nối vào site: Sơ đồ lộ trình (trạm "Phụ lục" sau chặng D), bài 13 dẫn sang P1, Học bền & thực chiến dẫn sang bug hunt | 30 phút | 0 link hỏng, deploy xanh |

Mỗi đợt kết thúc bằng: build strict, kiểm link, chụp màn hình desktop + 375px, commit, push, theo dõi deploy.

## Rủi ro và việc cần chốt

- **iOS simulator trên CI** cần runner macOS, chạy chậm và tốn phút. Đề xuất: iOS test chỉ chạy khi PR đổi code `shared/` hoặc `iosApp/`.
- **Maestro trên CI cho iOS** cần simulator boot sẵn. Nếu không ổn định thì giữ Maestro ở máy local, CI chỉ chạy Android.
- **Mokkery** phải khớp version Kotlin (theo mokkery.dev, plugin 3.4.x đi với Kotlin 2.4). Kiểm lại lúc cài.
- **Journeys** và Compose Preview Screenshot Testing còn thử nghiệm: chỉ giới thiệu, không đưa vào CI.
- AGP đang ở 8.13.2. Một số tính năng test mới (Journeys trên CI, test suite) cần AGP 9, nên ghi chú chứ không migrate trong plan này.

## Nguồn đã tra

- Mokkery: https://mokkery.dev/
- Maestro: https://maestro.dev/insights/best-mobile-app-testing-frameworks
- Journeys: https://developer.android.com/studio/gemini/journeys
- Compose Preview Screenshot Testing: https://developer.android.com/studio/preview/compose-screenshot-testing
