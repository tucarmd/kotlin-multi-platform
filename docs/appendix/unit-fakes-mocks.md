# P1 · Fake, mock và test không cần mạng hay máy

Core gọi repository, repository gọi mạng và database. Muốn test core **trong vài mili giây, không mạng, không máy**
thì thay phần phụ thuộc bằng bản giả. Trang này dùng [Rate Watch](../apps/03-rate-watch.md), app có đủ mạng (Ktor)
và database (SQLDelight).

<div class="rmap-legend">
  <span><b class="lg lg--do">Làm</b> thêm Mokkery + <code>RatesCoreMockTest</code> vào Rate Watch, so với <code>FakeRateRepository</code></span>
  <span><b class="lg lg--ok">Xong khi</b> nói được khi nào dùng fake, khi nào dùng mock; test xanh 2 nền tảng</span>
</div>

## Thay cái gì bằng cái gì

| Phụ thuộc thật | Bản giả dùng trong test | Có ở đâu |
|---|---|---|
| `RateRepository` (DB + mạng) | **Fake**: `FakeRateRepository` | `RatesCoreTest.kt` |
| `RateRepository` | **Mock**: `mock<RateRepository>()` (Mokkery) | `RatesCoreMockTest.kt` |
| Server HTTP | Ktor **`MockEngine`** | `RateApiTest.kt` |
| File SQLite trên máy | SQLite **trong RAM** (`inMemoryRateDbDriver()`) | `SqlRateRepositoryTest.kt` |
| Giờ hệ thống | `Clock` giả đọc một biến | [Coroutine & Flow](unit-coroutines-flow.md#thoi-gian-ao-va-ong-ho-gia-rate-watch) |

Tất cả chỉ làm được vì core nhận phụ thuộc **qua constructor**, kiểu interface: `RatesCore(repository, clock, scope)`.

## Fake: bản cài đặt nhỏ mà chạy thật

[`FakeRateRepository`](https://github.com/tucarmd/kotlin-multi-platform/blob/e5da90c/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt#L215-L247)
là một `RateRepository` thật, chỉ khác là lưu bằng `MutableStateFlow` thay vì SQLite:

```kotlin
class FakeRateRepository : RateRepository {
    val tables = MutableStateFlow<Map<String, RateTable>>(emptyMap())
    var nextResult: suspend (base: String) -> Result<Unit> = { Result.success(Unit) }
    var refreshCalls = 0

    override fun observe(base: String): Flow<RateTable?> = tables.map { it[base] }
    override suspend fun refresh(base: String, now: Long): Result<Unit> {
        refreshCalls++
        return nextResult(base)
    }
    // …
}
```

Test với fake kiểm **kết quả**: sau khi làm X thì state là Y.

## Mock: không có hành vi cho tới khi bạn kịch bản hoá

Mock trả lời đúng những gì bạn dặn (`every { } returns …`) và **ghi lại mọi lời gọi** để kiểm sau
(`verify { }`). Test với mock kiểm **lời gọi**: gọi hàm nào, tham số gì, mấy lần, thứ tự nào.

```kotlin
private val repo = mock<RateRepository> {
    every { observeBase() } returns flowOf(null)
    every { observe(any()) } returns flowOf(null)
    every { observeFavorites() } returns flowOf(emptySet())
    everySuspend { setBase(any()) } returns Unit
}

@Test
fun `selecting a base saves it before downloading it`() = runTest {
    everySuspend { repo.refresh(any(), any()) } returns Result.success(Unit)
    val core = core()
    core.selectBase("EUR")
    advanceUntilIdle()

    verifySuspend(order) {          // THỨ TỰ là hành vi cần bảo vệ
        repo.setBase("EUR")
        repo.refresh("EUR", any())
    }
}
```

Khi sai, Mokkery in **toàn bộ lời gọi** đã xảy ra. Đây là kết quả thật khi cố tình bỏ đoạn chặn "đang tải thì bỏ qua"
trong `RatesCore`:

```text
AssertionError: Expected exactly 1 calls, but 3 occurred for RateRepository(3).refresh(base = "USD", now = any())!
# Matching calls:
  RateRepository(3).refresh(base = "USD", now = 1792050788830)
  RateRepository(3).refresh(base = "USD", now = 1792050788831)
  RateRepository(3).refresh(base = "USD", now = 1792050788831)
# Other calls to this mock:
  RateRepository(3).observeBase()
  RateRepository(3).observeFavorites()
```

Cùng lỗi đó, test dùng fake (`a second refresh while one is running is ignored`) cũng đỏ. Cả hai cách đều bắt được.

## Fake hay mock?

| | Fake | Mock |
|---|---|---|
| Kiểm | Kết quả (state) | Lời gọi (hàm, tham số, số lần, thứ tự) |
| Viết | Tốn công một lần, dùng lại cho mọi test | Nhanh cho từng test, phải kịch bản hoá lại mỗi lần |
| Khi refactor bên trong | Thường vẫn xanh | Dễ đỏ dù hành vi không đổi |
| Dùng khi | **Mặc định** | Lời gọi **chính là** hành vi: "chỉ tải 1 lần", "lưu trước khi tải", "không gọi mạng khi còn cache" |

Quy ước trong repo này: **fake trước**, mock chỉ khi cần khẳng định về lời gọi.

## Chọn thư viện mock cho KMP

| Thư viện | Chạy trong `commonTest` (cả iOS)? | Ghi chú |
|---|:-:|---|
| **Mokkery** | ✅ | Compiler plugin sinh mock lúc build. Version plugin phải hỗ trợ version Kotlin (3.5.1 chạy với Kotlin 2.4.20) |
| MockK | ❌ chỉ JVM | Dùng reflection của JVM. Chỉ dùng được trong `androidUnitTest` |
| Mockito | ❌ chỉ JVM | Như trên, và viết Kotlin kém tự nhiên hơn |

Cài Mokkery cho Rate Watch:

```toml
# gradle/libs.versions.toml
[versions]
mokkery = "3.5.1"
[plugins]
mokkery = { id = "dev.mokkery", version.ref = "mokkery" }
```

```kotlin
// build.gradle.kts (gốc)
alias(libs.plugins.mokkery) apply false
// shared/build.gradle.kts
plugins { alias(libs.plugins.mokkery) }
```

Chỉ cần plugin, không thêm dependency nào: plugin tự gắn thư viện runtime vào source set test.

## HTTP giả: Ktor `MockEngine`

`RateApi` nhận `HttpClient` từ ngoài vào. Test tạo client bằng **đúng hàm của app** (`createHttpClient`) nhưng với
engine giả, nên cấu hình JSON và timeout được test chính là cấu hình app chạy thật:

```kotlin
private fun api(timeoutMillis: Long = 10_000, handler: suspend MockRequestHandleScope.() -> HttpResponseData) =
    RateApi(createHttpClient(MockEngine { handler() }, timeoutMillis))

private suspend fun failure(api: RateApi): RefreshError =
    assertFailsWith<RateApiException> { api.latest("USD") }.reason

@Test
fun `http 500 becomes a server error`() = runTest {
    assertEquals(RefreshError.ServerError, failure(api { respond("oops", HttpStatusCode.InternalServerError) }))
}
```

[`RateApiTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/e5da90c/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt)
có đủ 9 case: 200, 500, JSON thiếu trường, JSON hỏng, timeout, lỗi gửi kèm status 200, mất mạng, huỷ giữa chừng.

## DB trong RAM: test SQL thật, không cần máy

Fake và mock thay **cả** repository, nên không ai kiểm file `.sq`, transaction hay chỗ đổi dòng DB thành `RateTable`.
`SqlRateRepositoryTest` dùng `SqlRateRepository` **thật**, SQL thật trên SQLite trong RAM, và server giả bằng `MockEngine`.
Mỗi test có DB mới tinh, xong thì `driver.close()`.

Driver mỗi nền tảng một khác, nên khai báo `expect` ngay trong **source set test**:

```kotlin
// commonTest/TestDriver.kt
expect fun inMemoryRateDbDriver(): SqlDriver

// androidUnitTest/TestDriver.android.kt: JDBC, vì driver Android cần Context mà unit test JVM không có
actual fun inMemoryRateDbDriver(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { RateDb.Schema.create(it) }

// iosTest/TestDriver.ios.kt: driver native của app, không ghi file. inMemoryDriver() tự tạo schema
actual fun inMemoryRateDbDriver(): SqlDriver = inMemoryDriver(RateDb.Schema)
```

Bản JVM cần thêm `sqldelight-sqlite-driver` vào `androidUnitTest.dependencies`. Bản iOS dùng `native-driver` đã có trong `iosMain`.

Test đáng giá nhất là test transaction: một lần refresh phải là **một** lần Flow phát.

```kotlin
@Test
fun `a refresh is seen as ONE update thanks to the transaction`() = runTest {
    val repo = repository()
    repo.refresh("USD", now = 1_000)
    repo.observe("USD").test {
        assertEquals(1_000, awaitItem()!!.fetchedAt)
        repo.refresh("USD", now = 2_000)
        assertEquals(2_000, awaitItem()!!.fetchedAt) // thiếu db.transaction { } thì Flow thấy cả bảng đang xoá dở
        expectNoEvents()
    }
}
```

[`SqlRateRepositoryTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/SqlRateRepositoryTest.kt)
có 4 case: refresh lưu bảng với giờ **của mình** · refresh lỗi không đụng bảng cũ · transaction phát đúng 1 lần ·
bấm sao 2 lần là bỏ sao. Chạy trên JVM **và** iOS simulator, nên SQL được kiểm trên SQLite của cả 2 nền tảng.

## Hay vấp

| Triệu chứng | Nguyên nhân | Sửa |
|---|---|---|
| `Unresolved reference: mockk` khi build iOS | MockK đặt trong `commonTest` | Dùng Mokkery, hoặc chuyển test sang `androidUnitTest` |
| Mock ném lỗi "not answered" | Hàm được gọi mà chưa `every { }` | Kịch bản hoá đủ các hàm core gọi lúc khởi tạo (ở đây là 3 hàm `observe…`) |
| `every` báo lỗi với hàm `suspend` | Dùng nhầm `every` / `verify` | Hàm `suspend` dùng `everySuspend` / `verifySuspend` |
| Build lỗi ngay sau khi nâng Kotlin | Version plugin Mokkery chưa hỗ trợ Kotlin mới | Nâng Mokkery cùng lúc với Kotlin |
| `No value passed for parameter 'context'` khi tạo driver trong test | Dùng `AndroidSqliteDriver` trong unit test JVM | `JdbcSqliteDriver(IN_MEMORY)` từ `sqlite-driver` trong `androidUnitTest` |
| Test DB trong RAM báo `no such table` | Driver JDBC không tự tạo schema | Gọi `RateDb.Schema.create(driver)` ngay sau khi tạo |
| Test mock đỏ sau refactor dù app vẫn đúng | Mock kiểm chi tiết cài đặt | Đổi sang fake nếu lời gọi không phải hành vi cần bảo vệ |

## Bài tập

1. Viết test mock: khi bảng đã lưu còn mới (chưa quá 1 giờ) thì `start()` **không** gọi `refresh` (`verifySuspend(exactly(0))`).
2. Viết lại bài 1 bằng `FakeRateRepository`. So hai bản: bản nào dễ đọc hơn?
3. Thêm case `MockEngine` trả về JSON có `rates` rỗng `{}`. App nên coi là lỗi gì?
4. Xoá `db.transaction { }` trong `SqlRateRepository.refresh` rồi chạy lại `SqlRateRepositoryTest`. Test nào đỏ, Turbine báo gì?

Commit của trang này: `git log --grep "Fake & mock"` và `git log --grep "DB trong RAM"`. Tiếp theo: [Coverage với Kover](unit-coverage.md).
