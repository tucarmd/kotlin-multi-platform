# P1 · Unit test cơ bản & test bảng

Viết test cho hàm luật trong `commonMain`: một test chạy được trên **JVM và iOS simulator**. Trang này dùng
[Tip Split](../apps/01-tip-split.md), app có các luật tiền nong dễ sai nhất: làm tròn, chia, tip.

<div class="rmap-legend">
  <span><b class="lg lg--do">Làm</b> thêm <code>SplitTableTest</code> vào TipSplit</span>
  <span><b class="lg lg--ok">Xong khi</b> <code>allTests</code> xanh 2 nền tảng, cố tình phá thì đỏ đúng case</span>
</div>

## Bộ đồ nghề tối thiểu

`kotlin("test")` trong `commonTest` là đủ, không cần JUnit hay thư viện nào khác:

```kotlin
// TipSplit/shared/build.gradle.kts
commonTest.dependencies {
    implementation(kotlin("test"))
}
```

| Hàm | Dùng khi | Ví dụ trong TipSplit |
|---|---|---|
| `assertEquals(expected, actual, message)` | So giá trị | `assertEquals(37_000, split.perPerson)` |
| `assertTrue(cond, message)` | Kiểm một điều kiện | `assertTrue(split.collected >= split.total)` |
| `assertNull` / `assertNotNull` | Hàm trả `null` khi lỗi | `assertNull(parseMoney("12a"))` |
| `assertIs<T>(value)` | Kiểm kiểu **và** ép kiểu luôn | `assertIs<SplitResult.Ready>(result).split` |
| `assertFailsWith<E> { }` | Code phải ném lỗi | `assertFailsWith<IllegalArgumentException> { splitBill(0, 10, 2) }` |

Mỗi test theo 3 bước **Arrange → Act → Assert**: chuẩn bị input, gọi đúng một hành động, kiểm kết quả.

## Đặt tên test

Tên test là **câu mô tả hành vi**, đọc lên là biết test bảo vệ điều gì:

```kotlin
@Test
fun `shares are rounded up so the group never pays less than the total`() { … }
```

!!! warning "Ký tự cấm trong tên test"
    `.` `;` `:` `[` `]` `/` `<` `>` bị cấm ở mọi nền tảng. Riêng **dấu phẩy `,` và ngoặc `(` `)`** thì JVM chấp nhận
    nhưng Kotlin/Native (iOS) báo lỗi. Cả hai lỗi dưới đây đều gặp thật khi viết phụ lục này:
    ```text
    SplitTableTest.kt:53:9 Name contains illegal characters: ",".
    TipCoreFlowTest.kt:61:9 Name contains illegal characters: "()".
    Execution failed for task ':shared:compileTestKotlinIosSimulatorArm64'.
    ```
    Vì vậy luôn chạy `allTests` (cả 2 nền tảng) chứ không chỉ `testDebugUnitTest`.

## Test bảng: một test, nhiều case

Thay vì 8 hàm `@Test` gần giống nhau, gom dữ liệu vào **một bảng**. Số liệu nằm cạnh nhau nên dễ thấy case nào còn
thiếu (1 người? tip 0%? hoá đơn nhỏ hơn 1.000đ?):

```kotlin
private data class Case(val name: String, val bill: Long, val tipPercent: Int,
                        val people: Int, val roundUp: Boolean, val perPerson: Long)

private val cases = listOf(
    Case("docs example",        bill = 100_000, tipPercent = 10, people = 3, roundUp = true,  perPerson = 37_000),
    Case("one person pays all", bill = 250_000, tipPercent = 0,  people = 1, roundUp = false, perPerson = 250_000),
    Case("bill below ROUND_TO", bill = 500,     tipPercent = 0,  people = 2, roundUp = true,  perPerson = 1_000),
    // … thêm case = thêm 1 dòng
)

@Test
fun `every row of the table gives the expected share`() {
    for (c in cases) {
        val split = splitBill(c.bill, c.tipPercent, c.people, if (c.roundUp) ROUND_TO else 1)
        assertEquals(c.perPerson, split.perPerson, "case '${c.name}'")   // ← message chỉ đúng dòng hỏng
    }
}
```

Tham số thứ ba (`message`) là phần quan trọng nhất. Thiếu nó, test đỏ chỉ báo `expected:<37000> but was:<36000>`,
không biết dòng nào trong bảng.

## Bất biến: luật đúng với **mọi** input

Ví dụ chỉ kiểm vài điểm. Bất biến là luật phải đúng ở **mọi** điểm, nên ta sinh hàng nghìn input và kiểm hết:

```kotlin
for (bill in bills) for (tip in TIP_OPTIONS + 100) for (people in 1..MAX_PEOPLE) for (roundTo in listOf(1L, ROUND_TO)) {
    val s = splitBill(bill, tip, people, roundTo)
    val where = "bill=$bill tip=$tip people=$people roundTo=$roundTo"
    assertTrue(s.collected >= s.total, "group pays less than the total: $where")
    assertTrue(s.perPerson % roundTo == 0L, "share is not a multiple of $roundTo: $where")
    assertTrue(s.extra < people * roundTo, "rounded up too much (extra=${s.extra}): $where")
}
```

4 bất biến × 10.000 tổ hợp, chạy dưới 1 giây. Code đầy đủ:
[`SplitTableTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/SplitTableTest.kt).

## Phá rồi sửa: test có bắt được lỗi thật không?

Test chưa từng đỏ thì chưa chắc đã kiểm gì. Cố tình làm sai `ceilDiv` (chia làm tròn **xuống** thay vì lên):

```kotlin
internal fun ceilDiv(a: Long, b: Long): Long = a / b   // sai cố ý
```

Kết quả thật:

```text
SplitTableTest > every row of the table gives the expected share FAILED
    AssertionError: case 'docs example' expected:<37000> but was:<36000>
SplitTableTest > invariants hold for every bill and tip and head count and rounding FAILED
    AssertionError: group pays less than the total: bill=1 tip=0 people=1 roundTo=1000
```

Đọc message là biết ngay **case nào** và **vì sao**. Trả code về như cũ thì test xanh lại.

## Chạy test

| Cách | Lệnh / thao tác | Chạy trên |
|---|---|---|
| Tất cả | `./gradlew :shared:allTests` | JVM + iOS simulator |
| Chỉ JVM (nhanh) | `./gradlew :shared:testDebugUnitTest` | JVM |
| Chỉ iOS | `./gradlew :shared:iosSimulatorArm64Test` | iOS simulator |
| Một class | `./gradlew :shared:testDebugUnitTest --tests "dev.tipsplit.SplitTableTest"` | JVM |
| Trong IDE | Bấm ▶ ở gutter cạnh `class` hoặc `fun`, chọn target | tuỳ chọn |

Report HTML: `shared/build/reports/tests/<task>/index.html`. File XML (CI đọc): `shared/build/test-results/`.

## Hay vấp

| Triệu chứng | Nguyên nhân | Sửa |
|---|---|---|
| JVM xanh, iOS không compile | Tên test có `,` `(` `)` | Đổi tên, xem [cảnh báo trên](#at-ten-test) |
| Test đỏ nhưng không biết case nào | Thiếu `message` trong `assertEquals` | Thêm `"case '${c.name}'"` |
| So `Double` lúc đỏ lúc xanh | Sai số dấu phẩy động | Dùng `Long` (như TipSplit) hoặc `assertEquals(a, b, absoluteTolerance = 1e-9)` |
| Test xanh dù code sai | Test chưa từng được thấy đỏ | [Phá rồi sửa](#pha-roi-sua-test-co-bat-uoc-loi-that-khong) một lần |

## Bài tập

1. Thêm 2 dòng vào bảng: tip 100% và hoá đơn đúng `MAX_BILL`. Tự tính kết quả bằng tay trước khi chạy.
2. Viết bảng tương tự cho `parseMoney` của TipSplit (input → số hoặc `null`).
3. Áp dụng vào Rate Watch: test bảng cho `convert` trong
   [`rules/Convert.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/rules/Convert.kt),
   kèm một bất biến (đổi A → B rồi B → A thì gần bằng số ban đầu).

Commit của trang này: `git log --grep "test bảng"`. Tiếp theo: Coroutine & Flow.
