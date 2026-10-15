# P1 · Test coroutine & Flow

Core của mọi app trong repo phát state qua `StateFlow`. Test `state.value` chỉ cho biết **kết quả cuối**. Test Flow
cho biết **UI thật sự nhận được gì**: mấy lần, theo thứ tự nào, và một thao tác không đổi gì có làm UI vẽ lại không.

<div class="rmap-legend">
  <span><b class="lg lg--do">Làm</b> thêm <code>TipCoreFlowTest</code> (Turbine) vào TipSplit, đọc <code>RatesCoreTest</code> của Rate Watch</span>
  <span><b class="lg lg--ok">Xong khi</b> test không có <code>delay</code> thật nào, chạy dưới 1 giây, xanh 2 nền tảng</span>
</div>

## Cài

```toml
# gradle/libs.versions.toml
turbine = "1.2.1"
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
```

```kotlin
// shared/build.gradle.kts
commonTest.dependencies {
    implementation(kotlin("test"))
    implementation(libs.kotlinx.coroutines.test) // runTest, thời gian ảo
    implementation(libs.turbine)                 // flow.test { awaitItem() }
}
```

## `runTest`: coroutine trong test

Mọi test có `suspend` hoặc `launch` đều bọc trong `runTest { }`. Nó cho 3 thứ:

| Thứ | Nghĩa là |
|---|---|
| Thời gian ảo | `delay(60_000)` trả về ngay. `advanceTimeBy(ms)` / `advanceUntilIdle()` đẩy đồng hồ ảo |
| Chạy theo lịch | `launch` không chạy liền mà chờ test nhường (`advanceUntilIdle()`, `await`, `awaitItem()`) |
| Bắt lỗi rò | Coroutine còn chạy hoặc exception bị nuốt khi test kết thúc → test đỏ |

## Turbine: kiểm từng giá trị Flow phát ra

```kotlin
@Test
fun `each action emits exactly one new state in order`() = runTest {
    val core = TipCore()
    core.state.test {
        assertEquals(SplitResult.Waiting, awaitItem().result)        // StateFlow luôn phát giá trị hiện tại trước

        core.onBillChanged("100.000")
        assertEquals(55_000, assertIs<SplitResult.Ready>(awaitItem().result).split.perPerson)

        core.addPerson()
        assertEquals(37_000, assertIs<SplitResult.Ready>(awaitItem().result).split.perPerson)

        expectNoEvents()                                              // thêm 1 lần phát = thêm 1 lần vẽ lại thừa
    }
}
```

| Hàm Turbine | Dùng khi |
|---|---|
| `awaitItem()` | Chờ giá trị kế tiếp (quá 3 giây ảo thì đỏ) |
| `expectNoEvents()` | Khẳng định không có gì mới, bắt lỗi phát thừa |
| `skipItems(n)` | Bỏ qua n giá trị không quan tâm |
| `awaitComplete()` / `awaitError()` | Flow hữu hạn: kết thúc đúng / ném lỗi đúng |
| `cancelAndIgnoreRemainingEvents()` | Flow vô hạn (như `StateFlow`) còn giá trị chưa đọc mà test không quan tâm |

## Hai tính chất của `StateFlow` cần test

**1. Giá trị bằng nhau thì không phát.** `TipCore` tính lại `SplitState` sau mỗi thao tác. Nếu kết quả **bằng**
state hiện tại (data class so theo nội dung) thì `StateFlow` bỏ qua, nên UI không vẽ lại:

```kotlin
core.removePerson()          // 2 → 1: phát
assertEquals(1, awaitItem().input.people)
core.removePerson()          // đã ở mức tối thiểu: state y hệt
core.onTipSelected(10)       // 10% đang được chọn sẵn
expectNoEvents()             // không phát gì
```

**2. Người thu chậm chỉ thấy giá trị mới nhất (conflation).** `StateFlow` chỉ giữ **một** giá trị. Cập nhật 3 lần
liền mà người thu chưa kịp chạy thì nó chỉ thấy lần cuối:

```kotlin
val seen = mutableListOf<SplitState>()
val job = launch { core.state.toList(seen) }   // chạy trên StandardTestDispatcher: chỉ chạy khi test nhường
advanceUntilIdle()

core.onBillChanged("1"); core.onBillChanged("10"); core.onBillChanged("100.000")
advanceUntilIdle()
job.cancel()

assertEquals(listOf("", "100.000"), seen.map { it.input.billText })   // "1" và "10" bị gộp mất
```

Đây là **tính năng**, không phải lỗi: UI không cần vẽ trạng thái trung gian. Nhưng nó giải thích vì sao test muốn thấy
**từng bước** phải dùng Turbine (Turbine thu ngay lập tức) chứ không dùng `launch { collect }` thường.

Code đầy đủ: [`TipCoreFlowTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt).

## Thời gian ảo và đồng hồ giả (Rate Watch)

`RatesCore` của Rate Watch tự tải lại khi bảng tỷ giá cũ hơn 1 giờ. Test kiểu này mà chờ thật 1 giờ thì vô dụng.
Cách làm trong [`RatesCoreTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/e5da90c/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt):

1. **Đồng hồ là tham số**: `RatesCore(repository, clock, scope)`. Test truyền một `Clock` đọc biến `nowMillis`, nên
   "1 giờ sau" chỉ là một phép gán
   ([dòng 45](https://github.com/tucarmd/kotlin-multi-platform/blob/e5da90c/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt#L45-L47)).
2. **Scope là tham số**: core chạy trên `StandardTestDispatcher(testScheduler)`, nên `advanceUntilIdle()` điều khiển được nó
   ([dòng 53–59](https://github.com/tucarmd/kotlin-multi-platform/blob/e5da90c/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt#L53-L59)).
3. **Cổng chặn** (`CompletableDeferred`) để dừng giữa chừng một lần tải, kiểm spinner bật rồi tắt
   ([`refreshing turns on and off around the download`](https://github.com/tucarmd/kotlin-multi-platform/blob/e5da90c/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt#L103-L117)).

```kotlin
@Test
fun `start does not refresh a table exactly one hour old`() = runTest {
    repo.tables.value = mapOf("USD" to usd(fetchedAt = nowMillis - hour))   // đúng 1 giờ: chưa cũ
    val core = core()
    core.start()
    advanceUntilIdle()
    assertEquals(0, repo.refreshCalls)
}
```

!!! tip "Quy tắc thiết kế để test được"
    Thứ gì **không đoán trước được** (giờ hệ thống, dispatcher, mạng, database) thì đưa vào qua constructor.
    App dùng giá trị mặc định thật; test truyền bản giả. Đó là lý do `RatesCore` có `clock` và `scope` với giá trị mặc định.

## ViewModel Android: `Dispatchers.setMain`

ViewModel dùng `viewModelScope`, chạy trên `Dispatchers.Main`. Trong unit test JVM không có main thread, nên phải thay:

```kotlin
@BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
@AfterTest  fun tearDown() = Dispatchers.resetMain()
```

Các app trong repo **không cần** bước này: ViewModel chỉ chuyển lời gọi sang core (`SplitViewModel` → `TipCore`), còn
core nhận `scope` qua constructor. Test core là đủ, đó là lợi ích của kiến trúc "core dùng chung, UI mỏng".

## Hay vấp

| Triệu chứng | Nguyên nhân | Sửa |
|---|---|---|
| `awaitItem()` báo *No value produced in 3s* | Thao tác không đổi state (giá trị bằng nhau không phát) | Dùng `expectNoEvents()` nếu đó là ý muốn |
| Test thấy thiếu giá trị ở giữa | Conflation, đang thu bằng `launch { collect }` | Dùng Turbine |
| Assertion luôn thấy state ban đầu | Code chạy trong `backgroundScope`, `advanceUntilIdle()` không chờ nó | Đưa scope chạy trên `testScheduler` như `RatesCoreTest` |
| *Module with the Main dispatcher is missing* | Test ViewModel trên JVM | `Dispatchers.setMain(...)` |
| Test dùng `delay` thật chạy mất vài giây | Đang dùng `runBlocking` thay vì `runTest` | Đổi sang `runTest` |
| JVM xanh, iOS không compile | Tên test có `(` `)` `,` `.` `:` | Đổi tên ([P1 · cơ bản](unit-basics.md#at-ten-test)) |

## Bài tập

1. Thêm vào `TipCoreFlowTest`: bấm `reset()` khi đang ở state mặc định thì **không** phát gì.
2. Pocket Notes: dùng Turbine kiểm danh sách ghi chú phát lại sau khi thêm một ghi chú.
3. Rate Watch: viết test "đổi base currency khi đang tải dở thì bảng cũ không hiện dưới tên base mới"
   (gợi ý: `CompletableDeferred` + `flatMapLatest`, xem TRAP trong `RatesCore.state`).

Commit của trang này: `git log --grep "Coroutine & Flow"`. Tiếp theo: Fake & mock.
