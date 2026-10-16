# P1 · Coverage với Kover

Test xanh chưa nói được test **đủ** chưa. Coverage đo những dòng và nhánh nào của code đã được ít nhất một test chạy
qua. Trang này bật [Kover](https://github.com/Kotlin/kotlinx-kover) (công cụ coverage của JetBrains) cho
[Tip Split](../apps/01-tip-split.md), đặt ngưỡng tối thiểu để build tự đỏ khi ai đó xoá bớt test.

<div class="rmap-legend">
  <span><b class="lg lg--do">Làm</b> thêm Kover vào TipSplit, đặt ngưỡng 80% cho <code>rules/</code>, 60% cho cả module</span>
  <span><b class="lg lg--ok">Xong khi</b> mở được report HTML; hạ test xuống dưới ngưỡng thì <code>koverVerify</code> đỏ</span>
</div>

## Cài

```toml
# gradle/libs.versions.toml
[versions]
kover = "0.9.11"

[plugins]
kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }
```

```kotlin
// TipSplit/build.gradle.kts (gốc)
plugins {
    alias(libs.plugins.kover) apply false
}

// TipSplit/shared/build.gradle.kts
plugins {
    alias(libs.plugins.kover)
}
```

Kover đo bằng cách chạy **unit test Android trên JVM** (`testDebugUnitTest`). Test iOS vẫn chạy như cũ nhưng không được
đo: Kover chỉ hiểu bytecode JVM. Với KMP thì vậy là đủ, vì `commonTest` chạy cùng một bộ test trên cả hai bên.

## Lệnh

| Lệnh | Ra cái gì |
|---|---|
| `./gradlew :shared:koverHtmlReportDebug` | Report HTML: `shared/build/reports/kover/htmlDebug/index.html`, bấm vào từng file thấy dòng xanh / đỏ / vàng |
| `./gradlew :shared:koverLogDebug` | In một dòng `application line coverage: 100%` ra terminal |
| `./gradlew :shared:koverXmlReportDebug` | File XML (định dạng JaCoCo), để CI hoặc Codecov đọc |
| `./gradlew :shared:koverVerifyDebug` | Build **đỏ** nếu dưới ngưỡng của cả module |
| `./gradlew :shared:koverVerifyRules` | Build **đỏ** nếu `rules/` dưới ngưỡng riêng |

Tên task có đuôi `Debug` vì Kover lấy theo **build variant** của Android. Module chỉ có `jvm()` thì task không có đuôi
(`koverHtmlReport`).

## Đặt ngưỡng và loại trừ

```kotlin
// TipSplit/shared/build.gradle.kts
kover {
    currentProject {
        // A second view of the same debug unit tests that only looks at the rules package.
        copyVariant("rules", "debug")
    }
    reports {
        filters {
            excludes {
                packages("dev.tipsplit.di") // Koin wiring: no logic, only declarations
            }
        }
        variant("debug") {
            verify { rule("whole module") { minBound(60) } }
        }
        variant("rules") {
            filters { includes { packages("dev.tipsplit.rules") } }
            verify { rule("money rules") { minBound(80) } }
        }
    }
}
```

Vì sao hai ngưỡng: `rules/` là chỗ sai một dòng là sai tiền thật, nên giữ cao. Phần còn lại chỉ cần một mức sàn để bắt
trường hợp "lỡ xoá cả file test".

!!! warning "Kover 0.9 không có filter trong từng rule"
    Viết `rule { filters { … } }` sẽ báo *can't be called in this context by implicit receiver*. Một variant chỉ có
    một bộ filter, nên muốn ngưỡng riêng cho một package thì tạo variant riêng bằng `copyVariant`, như trên.

Khi dưới ngưỡng, build báo đúng rule nào hỏng:

```text
Execution failed for task ':shared:koverVerifyRules'.
> Rule 'money rules' violated: branches covered percentage is 90.000000, but expected minimum is 95
```

## 100% dòng chưa phải là đủ

Kết quả thật của TipSplit lúc commit trang này:

| File | Dòng | Nhánh |
|---|:-:|:-:|
| `rules/Money.kt` | 11/11 | 12/12 |
| `rules/Rules.kt` | 25/25 | **24/28** |
| `model/Models.kt` (`SplitState`) | 4/4 | **2/4** |
| `core/TipCore.kt` | 13/13 | không có nhánh |

Dòng đạt 100% mà nhánh thì không. Dòng thiếu nhánh nằm ở `Rules.kt:51`:

```kotlin
require(bill > 0 && people > 0 && roundTo > 0 && tipPercent in 0..100) { "validate() the input first" }
```

`&&` dừng ở vế sai đầu tiên. Test hiện có chỉ làm vế `bill > 0` sai, nên các vế `roundTo > 0` và `tipPercent in 0..100`
chưa từng được thử ở nhánh **sai**. Dòng vẫn "đã chạy", nên đếm theo dòng không thấy lỗ hổng này. Vì vậy:

- Đặt ngưỡng theo **dòng** để làm cổng build (ổn định, dễ hiểu).
- Đọc report theo **nhánh** để tìm case còn thiếu. Ô vàng trong HTML là dòng có nhánh chưa chạy.

## Cái gì không cần cover

| Không cần | Vì sao |
|---|---|
| Module Koin (`di/`) | Chỉ khai báo, sai thì app crash ngay lúc mở, test Koin riêng bằng `verify()` |
| `androidApp/`, `iosApp/` | UI kiểm bằng UI test và Maestro (P2), không bằng unit test |
| `data class` sinh `equals` / `hashCode` / `toString` | Kover tự bỏ qua: `Split` không test `equals` mà vẫn 100% |
| Code sinh bởi SQLDelight | Thêm `excludes { packages("...db") }`, test qua repository là đủ |

Coverage cao không có nghĩa test tốt: một test gọi hàm mà không `assert` gì vẫn làm dòng thành xanh. Ngưỡng chỉ là
lưới an toàn, không phải mục tiêu.

## Hay vấp

| Triệu chứng | Nguyên nhân | Sửa |
|---|---|---|
| `Task 'koverHtmlReport' not found` trong module có Android | Task đặt theo variant | Dùng `koverHtmlReportDebug` |
| `can't be called in this context by implicit receiver` | Viết `filters` bên trong `rule` | Tạo variant riêng (`copyVariant`) |
| Test chỉ có trong `iosTest` không làm coverage tăng | Kover chỉ đo bytecode JVM | Đặt test ở `commonTest`, nó chạy cả JVM lẫn iOS |
| Coverage tăng vọt sau khi thêm module DI | Đếm cả code khai báo | Loại trừ package `di` |

## Bài tập

1. Mở report HTML, tìm 4 nhánh thiếu ở `Rules.kt:51`. Viết test bảng cho `splitBill` với `roundTo = 0` và
   `tipPercent = 101`, chạy lại: nhánh lên 28/28.
2. Viết test cho `SplitState.canRemovePerson` khi chỉ còn 1 người. Nhánh của `Models.kt` lên 4/4.
3. Bật Kover cho Rate Watch, loại trừ code SQLDelight sinh ra (`dev.ratewatch.db`). Coverage của `SqlRateRepository`
   là bao nhiêu nhờ `SqlRateRepositoryTest`?

Commit của trang này: `git log --grep "Coverage"`. Tiếp theo: P2 · Automation test.
