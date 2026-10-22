---
name: kmp-test
description: >-
  Viết và chạy UNIT TEST cho phần shared của app KMP trong repo kotlin-multi-platform: test rules/ (hàm thuần, ví
  dụ vào → ra, test bảng), test <X>Core với fake repository, test StateFlow bằng kotlinx-coroutines-test /
  Turbine-style, chạy trên JVM VÀ iosSimulatorArm64Test, đo coverage bằng Kover (rules ≥ 80 %, module ≥ 60 %).
  Dùng khi nói "viết test cho rules", "test core", "fake repository", "test flow", "chạy commonTest", "test iOS
  simulator", "coverage", "kover", "test đỏ", "Name contains illegal characters".
---

# kmp-test — test commonTest

Đọc trước: `docs/appendix/unit-basics.md`, `unit-fakes-mocks.md`, `unit-coroutines-flow.md`, `unit-coverage.md`.
Mẫu code: `apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/`.

## Test gì, ở đâu

| File test | Giữ cái gì | Mẫu |
|---|---|---|
| `RulesTest.kt` | Mỗi luật một test, tên test là câu mô tả hành vi | `RulesTest.kt` |
| `<X>TableTest.kt` | Nhiều cặp vào → ra trong một bảng (tiền, làm tròn, biên) | `SplitTableTest.kt` |
| `MoneyTest.kt` | Parse / format: `"120.000"` và `"120,000"` phải ra cùng một số | `MoneyTest.kt` |
| `<X>CoreTest.kt` | Gọi event → `state.value` đúng | `TipCoreTest.kt` |
| `<X>CoreFlowTest.kt` | Thứ tự các state phát ra (`runTest`, gom bằng `backgroundScope`) | `TipCoreFlowTest.kt` |

Core có repository thì viết `Fake<X>Repository` (giữ dữ liệu trong `MutableStateFlow`) trong `commonTest`, **không mock**.
Thời gian và id truyền vào qua tham số hoặc qua `Clock` giả.

## Chạy

```bash
cd apps/<App>
./gradlew :shared:testDebugUnitTest                  # JVM, nhanh: chạy liên tục khi đang viết
./gradlew :shared:iosSimulatorArm64Test              # Kotlin/Native: chạy trước khi commit
./gradlew :shared:koverHtmlReportDebug :shared:koverVerifyRules   # shared/build/reports/kover/htmlDebug
```

App mới chưa có Kover thì copy khối `kover { }` và alias plugin từ `apps/TipSplit/shared/build.gradle.kts`, rồi đổi
filter theo package `rules` của app mới.

## Bẫy

- Tên test trong backtick **không được có `,` `.` `:`**. JVM chạy được, nhưng Kotlin/Native báo
  `Name contains illegal characters`.
- `runTest` bỏ qua `delay`; muốn test timeout hay retry thì dùng `advanceTimeBy`, đừng chờ thời gian thật.
- Test xanh trên JVM chưa có nghĩa là xanh trên iOS (khác chỗ làm tròn số, `String.format`). Luôn chạy cả hai.
- Bug nằm ở UI (như bug Undo của PocketNotes) thì unit test core không bắt được. Cái đó là việc của `kmp-auto-test`.
- Test đỏ mà sửa hai lần chưa xanh: dừng đoán, phân loại lỗi theo `docs/reference/kotlin-issue-types.md`.

Thêm `// TEST: RulesTest.<tên>` vào chỗ code mà test đang giữ.
