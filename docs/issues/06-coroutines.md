# 6. Coroutines (K251–K300)

Lỗi khi chạy việc bất đồng bộ bằng coroutine: gọi `suspend` sai chỗ, scope không ai huỷ, `CancellationException` bị
nuốt, lỗi lan sang coroutine anh em, chọn sai dispatcher. Nhiều mục không báo lỗi gì cả, app chỉ chạy sai, treo hoặc
tốn pin, nên mục nào có **▶ Chạy thử** thì nên bấm để tận mắt thấy.

### K251 · `Suspend function ... can only be called from a coroutine or another suspend function`

`🟢 mới học` · `Kotlin`

**Gặp:** `Suspend function 'suspend fun loadUser(): String' can only be called from a coroutine or another suspend
function.`

**Vì sao:** hàm `suspend` có thể tạm dừng, nên chỉ chạy được bên trong một coroutine. Hàm thường (click handler,
`onCreate`, `main`) không có coroutine nào để dừng.

**Sửa:** hoặc biến hàm gọi thành `suspend`, hoặc mở coroutine bằng `scope.launch { }` ở tầng UI/ViewModel. Đừng bọc
`runBlocking` cho hết lỗi (K255).

```kotlin
fun onClick() { val user = loadUser() }                         // ❌
fun onClick() { viewModelScope.launch { val user = loadUser() } } // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpzdXNwZW5kIGZ1biBsb2FkVXNlcigpOiBTdHJpbmcgeyBkZWxheSgxMDApOyByZXR1cm4gIlTDuiIgfQoKZnVuIG9uQ2xpY2soKSB7CiAgICB2YWwgdXNlciA9IGxvYWRVc2VyKCkgICAgICAgICAgICAgICAgLy8g4p2MIGjDoG0gdGjGsOG7nW5nIGtow7RuZyBn4buNaSDEkcaw4bujYyBow6BtIHN1c3BlbmQKICAgIHByaW50bG4odXNlcikKfQovLyDinIUgY2hvIGjDoG0gdGjDoG5oIHN1c3BlbmQsIGhv4bq3YyBt4bufIGNvcm91dGluZToKLy8gZnVuIG9uQ2xpY2soc2NvcGU6IENvcm91dGluZVNjb3BlKSB7IHNjb3BlLmxhdW5jaCB7IHByaW50bG4obG9hZFVzZXIoKSkgfSB9CgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgeyBvbkNsaWNrKCkgfQo) · Xem thêm: [Coroutine & suspend](../concepts/coroutines.md)

### K252 · `Suspension functions can only be called within coroutine body`

`🟢 mới học` · `Kotlin`

**Gặp:** `Suspension functions can only be called within coroutine body.` ở `delay` / hàm suspend đặt trong lambda của
`Thread { }`, callback, listener, `setOnClickListener { }`. Trong `forEach { }` thì lại được.

**Vì sao:** lambda của hàm `inline` (`forEach`, `map`, `let`) được chép vào hàm ngoài nên vẫn ở trong coroutine. Lambda
thường là một hàm riêng, không phải `suspend`.

**Sửa:** mở coroutine ngay trong callback (`scope.launch { }`), hoặc đổi API callback thành hàm suspend
(`suspendCancellableCoroutine`, K297).

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIG5hbWVzID0gbGlzdE9mKCJhIiwgImIiKQogICAgbmFtZXMuZm9yRWFjaCB7IGRlbGF5KDEwKTsgcHJpbnRsbihpdCkgfSAgIC8vIOKchSBmb3JFYWNoIGzDoCBpbmxpbmU6IMSRxrDhu6NjIGfhu41pIHN1c3BlbmQgYsOqbiB0cm9uZwogICAgdmFsIHQgPSBUaHJlYWQgewogICAgICAgIGRlbGF5KDEwKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBsYW1iZGEgY-G7p2EgVGhyZWFkIGtow7RuZyBwaOG6o2kgY29yb3V0aW5lCiAgICAgICAgcHJpbnRsbigieG9uZyIpCiAgICB9CiAgICB0LnN0YXJ0KCk7IHQuam9pbigpCn0K)

### K253 · `GlobalScope.launch`: việc chạy mồ côi, không ai đợi, không ai huỷ

`🟢 mới học` · `Kotlin`

**Gặp:** IDE cảnh báo `This is a delicate API`. Việc trong `GlobalScope` vẫn chạy sau khi đóng màn hình (tốn pin,
cập nhật UI đã chết), hoặc ngược lại bị cắt giữa chừng khi `main` / test kết thúc.

**Vì sao:** `GlobalScope` không gắn với vòng đời nào. Không có cha nên không ai đợi nó xong, không ai huỷ nó khi màn hình
đóng, lỗi bên trong không lan về đâu.

**Sửa:** dùng scope có vòng đời: `viewModelScope`, `lifecycleScope`, scope của app (K267), hoặc `coroutineScope { }`
trong hàm suspend.

```kotlin
GlobalScope.launch { save() }            // ❌
viewModelScope.launch { save() }         // ✅ huỷ khi ViewModel chết
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpAT3B0SW4oRGVsaWNhdGVDb3JvdXRpbmVzQXBpOjpjbGFzcykKZnVuIG1haW4oKSB7CiAgICBHbG9iYWxTY29wZS5sYXVuY2ggeyAgICAgICAgICAgICAgICAgLy8g4p2MIGtow7RuZyBhaSDEkeG7o2ksIGtow7RuZyBhaSBodeG7twogICAgICAgIGRlbGF5KDEwMCkKICAgICAgICBwcmludGxuKCLEkcOjIGzGsHUiKSAgICAgICAgICAgICAgICAgLy8ga2jDtG5nIGJhbyBnaeG7nSBpbjogbWFpbiDEkcOjIHRob8OhdAogICAgfQogICAgcHJpbnRsbigibWFpbiB4b25nIikKfQovLyDinIUgZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsgbGF1bmNoIHsgZGVsYXkoMTAwKTsgcHJpbnRsbigixJHDoyBsxrB1IikgfTsgcHJpbnRsbigibWFpbiB4b25nIikgfQo) · Xem thêm: [Scope, huỷ và lỗi](../concepts/structured-concurrency.md), K254

### K254 · Tự tạo `CoroutineScope(...)` / `MainScope()` mà không bao giờ `cancel()`

`🟡 đã làm app` · `KMP`

**Gặp:** class tự tạo `CoroutineScope(Dispatchers.Default)` làm field. Đóng màn hình rồi mà vẫn thấy log, flow vẫn
collect, timer vẫn chạy.

**Vì sao:** `CoroutineScope(...)` chỉ là một `Job` cha. Không ai gọi `cancel()` thì nó sống tới khi process chết, giống
`GlobalScope`.

**Sửa:** ai tạo scope thì phải có chỗ huỷ: thêm `close()` gọi `scope.cancel()` và gọi nó khi chủ sở hữu biến mất. Trong
code chung, cho class **nhận** scope từ ngoài: Android truyền `viewModelScope` (tự huỷ), iOS truyền `MainScope()` và gọi
`close()`.

```kotlin
class NotesCore(private val repository: NoteRepository, private val scope: CoroutineScope) {
    fun close() { scope.cancel() }   // iOS gọi khi màn hình biến mất
}
```

**Gặp ở:** [`PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/core/NotesCore.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/core/NotesCore.kt) ·
Xem thêm: K294

### K255 · `runBlocking` trên main thread Android

`🟢 mới học` · `Android`

**Gặp:** bấm nút là UI đứng hình; lâu quá 5 giây thì hiện hộp thoại "App isn't responding" (ANR, logcat có
`Input dispatching timed out`).

**Vì sao:** `runBlocking` **chặn** thread đang gọi tới khi khối bên trong xong. Trên main thread, nghĩa là không vẽ,
không nhận chạm trong suốt thời gian đó. Hay gặp khi dùng `runBlocking` để "hết lỗi K251".

**Sửa:** ở UI dùng `lifecycleScope.launch` / `viewModelScope.launch`. `runBlocking` chỉ dùng ở `main()` của chương trình
console và trong vài chỗ test cũ.

```kotlin
button.setOnClickListener { runBlocking { repo.sync() } }        // ❌ đứng hình
button.setOnClickListener { lifecycleScope.launch { repo.sync() } } // ✅
```

Xem thêm: K298

### K256 · `Unresolved reference 'viewModelScope'` trong `commonMain`

`🟡 đã làm app` · `KMP`

**Gặp:** chuyển ViewModel sang `commonMain` thì `viewModelScope` báo `Unresolved reference 'viewModelScope'.`

**Vì sao:** thư viện `androidx.lifecycle` mới được khai báo trong `androidMain` (hoặc bản cũ, trước khi lifecycle hỗ trợ
KMP). Code chung không thấy nó.

**Sửa:** cách đơn giản và dễ test: để class chung **nhận** `CoroutineScope` qua constructor (K254). Nếu muốn
`ViewModel` chung thật, thêm `androidx.lifecycle:lifecycle-viewmodel` (bản có KMP) vào `commonMain.dependencies`.

Xem thêm: K254

### K257 · `catch (e: Exception)` nuốt lệnh huỷ

`🟡 đã làm app` · `Kotlin` · `KMP`

**Gặp:** đã `cancel()` (đóng màn hình) mà vòng lặp vẫn chạy tiếp, log in `JobCancellationException` liên tục; hoặc UI
báo "Mất mạng" khi người dùng chỉ rời màn hình.

**Vì sao:** huỷ coroutine = ném `CancellationException` ở điểm treo. Nó là con của `Exception`, nên `catch (e: Exception)`
bắt luôn và coroutine không dừng.

**Sửa:** bắt `CancellationException` trước và ném lại, rồi mới tới các lỗi khác. Hay hơn: chỉ bắt đúng loại lỗi cần
(`IOException`...).

```kotlin
try { client.get(url) }
catch (e: CancellationException) { throw e }          // ✅ để lệnh huỷ đi qua
catch (e: IOException) { throw RateApiException(Offline) }
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIGpvYiA9IGxhdW5jaCB7CiAgICAgICAgcmVwZWF0KDUpIHsgaSAtPgogICAgICAgICAgICB0cnkgewogICAgICAgICAgICAgICAgcHJpbnRsbigidsOybmcgJGkiKQogICAgICAgICAgICAgICAgZGVsYXkoMTAwKQogICAgICAgICAgICB9IGNhdGNoIChlOiBFeGNlcHRpb24pIHsgICAgICAvLyDinYwgYuG6r3QgbHXDtG4gQ2FuY2VsbGF0aW9uRXhjZXB0aW9uCiAgICAgICAgICAgICAgICBwcmludGxuKCJudeG7kXQgJHtlOjpjbGFzcy5zaW1wbGVOYW1lfSIpCiAgICAgICAgICAgIH0KICAgICAgICAgICAgLy8g4pyFIHRow6ptIHRyxrDhu5tjIG5ow6FuaCBFeGNlcHRpb246ICBjYXRjaCAoZTogQ2FuY2VsbGF0aW9uRXhjZXB0aW9uKSB7IHRocm93IGUgfQogICAgICAgIH0KICAgIH0KICAgIGRlbGF5KDE1MCkKICAgIGpvYi5jYW5jZWxBbmRKb2luKCkKICAgIHByaW50bG4oIsSRw6MgaHXhu7ciKQp9Cg)

**Gặp ở:** [`RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt) ·
Xem thêm: [Phụ lục 2.11](../reference/kotlin-pitfalls.md), K258, K259

### K258 · `runCatching { }` quanh hàm suspend cũng nuốt lệnh huỷ

`🟡 đã làm app` · `Kotlin` · `KMP`

**Gặp:** `runCatching { fetch() }` trả `Failure(JobCancellationException: StandaloneCoroutine was cancelled)` và code chạy
tiếp, hiện "Lỗi không xác định" dù người dùng chỉ thoát màn hình.

**Vì sao:** `runCatching` của thư viện chuẩn bắt **mọi** `Throwable`, kể cả `CancellationException`.

**Sửa:** viết hàm riêng ném lại `CancellationException`, dùng thay `runCatching` trong code suspend.

```kotlin
suspend inline fun <T> resultOf(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpzdXNwZW5kIGZ1biBmZXRjaCgpOiBTdHJpbmcgeyBkZWxheSgxXzAwMCk7IHJldHVybiAiZGF0YSIgfQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsKICAgIHZhbCBqb2IgPSBsYXVuY2ggewogICAgICAgIHZhbCByID0gcnVuQ2F0Y2hpbmcgeyBmZXRjaCgpIH0gIC8vIOKdjCBydW5DYXRjaGluZyBi4bqvdCBj4bqjIENhbmNlbGxhdGlvbkV4Y2VwdGlvbgogICAgICAgIHByaW50bG4oImvhur90IHF14bqjOiAkciDihpIgduG6q24gY2jhuqF5IHRp4bq_cCBzYXUga2hpIGLhu4sgaHXhu7ciKQogICAgICAgIHByaW50bG4oImhp4buHbiBs4buXaSAnVW5rbm93biBlcnJvcicgY2hvIG5nxrDhu51pIGTDuW5nIikKICAgIH0KICAgIGRlbGF5KDEwMCkKICAgIGpvYi5jYW5jZWxBbmRKb2luKCkKICAgIHByaW50bG4oIsSRw6MgaHXhu7ciKQp9Ci8vIOKchSB2aeG6v3QgaMOgbSByacOqbmcgbsOpbSBs4bqhaSBDYW5jZWxsYXRpb25FeGNlcHRpb24gKHhlbSByZXN1bHRPZiB0cm9uZyBSYXRlV2F0Y2gpCg)

**Gặp ở:** [`RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt) ·
Xem thêm: K257

### K259 · Bắt `CancellationException` để xử lý timeout: nút Stop không dừng được

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** vòng "thử lại khi timeout" chạy tiếp sau khi người dùng bấm Stop, log `JobCancellationException, thử lại` liên
tục.

**Vì sao:** `withTimeout` ném `TimeoutCancellationException`, là con của `CancellationException`. Bắt
`CancellationException` cho tiện là bắt luôn lệnh huỷ thật từ bên ngoài.

**Sửa:** bắt **đúng** `TimeoutCancellationException`, hoặc dùng `withTimeoutOrNull` rồi kiểm `null`.

```kotlin
catch (e: CancellationException) { retry() }          // ❌ nuốt cả lệnh Stop
catch (e: TimeoutCancellationException) { retry() }   // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpzdXNwZW5kIGZ1biBzZW5kQ29tbWFuZCgpOiBTdHJpbmcgeyBkZWxheSg1MDApOyByZXR1cm4gIk9LIiB9CgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIGpvYiA9IGxhdW5jaCB7CiAgICAgICAgcmVwZWF0KDYpIHsgYXR0ZW1wdCAtPgogICAgICAgICAgICB0cnkgewogICAgICAgICAgICAgICAgcHJpbnRsbih3aXRoVGltZW91dCgyMDApIHsgc2VuZENvbW1hbmQoKSB9KQogICAgICAgICAgICB9IGNhdGNoIChlOiBDYW5jZWxsYXRpb25FeGNlcHRpb24pIHsgICAgICAgICAgIC8vIOKdjCBi4bqvdCBj4bqjIGzhu4duaCBodeG7tyB0aOG6rXQKICAgICAgICAgICAgLy8gfSBjYXRjaCAoZTogVGltZW91dENhbmNlbGxhdGlvbkV4Y2VwdGlvbikgeyAgLy8g4pyFIGNo4buJIGLhuq90IHRpbWVvdXQKICAgICAgICAgICAgICAgIHByaW50bG4oImzhuqduICRhdHRlbXB0OiAke2U6OmNsYXNzLnNpbXBsZU5hbWV9LCB0aOG7rSBs4bqhaSIpCiAgICAgICAgICAgIH0KICAgICAgICB9CiAgICB9CiAgICBkZWxheSgzMDApCiAgICBwcmludGxuKCItLS0gbmfGsOG7nWkgZMO5bmcgYuG6pW0gU3RvcCIpCiAgICBqb2IuY2FuY2VsQW5kSm9pbigpCiAgICBwcmludGxuKCJr4bq_dCB0aMO6YyIpCn0K)

**Gặp ở:** [`ObdLink/shared/src/commonMain/kotlin/dev/obdlink/core/CommandQueue.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/shared/src/commonMain/kotlin/dev/obdlink/core/CommandQueue.kt) ·
Xem thêm: K260

### K260 · `TimeoutCancellationException: Timed out waiting for 100 ms`

`🟢 mới học` · `Kotlin`

**Gặp:** app crash với `kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 100 ms`.

**Vì sao:** `withTimeout` **ném exception** khi hết giờ. Không ai bắt thì nó lan lên như lỗi thường.

**Sửa:** muốn "hết giờ thì thôi" dùng `withTimeoutOrNull`, trả `null` khi hết giờ.

```kotlin
val data = withTimeout(100) { load() }                      // ❌ ném khi hết giờ
val data = withTimeoutOrNull(100) { load() } ?: fallback    // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIHJlc3VsdCA9IHdpdGhUaW1lb3V0KDEwMCkgeyAgICAgIC8vIOKdjCBo4bq_dCBnaeG7nSBsw6AgbsOpbSBleGNlcHRpb24sIGNyYXNoIG7hur91IGtow7RuZyBi4bqvdAogICAgICAgIGRlbGF5KDUwMCk7ICJkYXRhIgogICAgfQogICAgLy8gdmFsIHJlc3VsdCA9IHdpdGhUaW1lb3V0T3JOdWxsKDEwMCkgeyBkZWxheSg1MDApOyAiZGF0YSIgfSA_OiAiaOG6v3QgZ2nhu50iIC8vIOKchQogICAgcHJpbnRsbihyZXN1bHQpCn0K) · Xem thêm: K259, K293

### K261 · `Thread.sleep` trong coroutine chặn cả thread

`🟢 mới học` · `Kotlin`

**Gặp:** 3 coroutine cùng chờ 300 ms mất ~900 ms thay vì ~300 ms; trên main thread thì UI giật.

**Vì sao:** `Thread.sleep` chặn **thread**. Các coroutine khác cùng thread phải đợi. `delay` chỉ treo **coroutine** và
nhường thread.

**Sửa:** trong coroutine dùng `delay`. Code chặn không tránh được (thư viện Java, file) thì bọc
`withContext(Dispatchers.IO)`.

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW4udGltZS5tZWFzdXJlVGltZQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsKICAgIHZhbCB0ID0gbWVhc3VyZVRpbWUgewogICAgICAgIGNvcm91dGluZVNjb3BlIHsKICAgICAgICAgICAgcmVwZWF0KDMpIHsKICAgICAgICAgICAgICAgIGxhdW5jaCB7CiAgICAgICAgICAgICAgICAgICAgVGhyZWFkLnNsZWVwKDMwMCkgICAgLy8g4p2MIGNo4bq3biB0aHJlYWQgY-G7p2EgcnVuQmxvY2tpbmcKICAgICAgICAgICAgICAgICAgICAvLyBkZWxheSgzMDApICAgICAgICAvLyDinIUgY2jhu4kgdHJlbyBjb3JvdXRpbmUKICAgICAgICAgICAgICAgIH0KICAgICAgICAgICAgfQogICAgICAgIH0KICAgIH0KICAgIHByaW50bG4oIm3huqV0ICR0IikKfQo) · Xem thêm: [Phụ lục 2.13](../reference/kotlin-pitfalls.md), K274

### K262 · Vòng lặp tính toán không dừng khi bị huỷ

`🟡 đã làm app` · `Kotlin`

**Gặp:** `job.cancel()` rồi mà vòng lặp xử lý ảnh / tính toán vẫn chạy tới hết, CPU vẫn cao.

**Vì sao:** huỷ trong coroutine là **hợp tác**: coroutine chỉ biết mình bị huỷ ở điểm treo (`delay`, `withContext`,
`yield`...). Vòng lặp không gọi hàm suspend nào thì không bao giờ kiểm.

**Sửa:** trong vòng lặp dài, gọi `ensureActive()` (ném nếu đã huỷ), kiểm `isActive`, hoặc `yield()` (nhường thread).

```kotlin
while (hasMore()) {
    ensureActive()      // ✅
    processChunk()
}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIGpvYiA9IGxhdW5jaChEaXNwYXRjaGVycy5EZWZhdWx0KSB7CiAgICAgICAgdmFyIGkgPSAwTAogICAgICAgIHZhbCBlbmQgPSBTeXN0ZW0uY3VycmVudFRpbWVNaWxsaXMoKSArIDYwMAogICAgICAgIHdoaWxlIChTeXN0ZW0uY3VycmVudFRpbWVNaWxsaXMoKSA8IGVuZCkgeyAgIC8vIOKdjCBraMO0bmcgY8OzIMSRaeG7g20gdHJlbyDihpIga2jDtG5nIGJp4bq_dCBi4buLIGh14bu3CiAgICAgICAgLy8gd2hpbGUgKGlzQWN0aXZlICYmIFN5c3RlbS5jdXJyZW50VGltZU1pbGxpcygpIDwgZW5kKSB7IC8vIOKchSBob-G6t2MgZ-G7jWkgZW5zdXJlQWN0aXZlKCkKICAgICAgICAgICAgaSsrCiAgICAgICAgfQogICAgICAgIHByaW50bG4oInbDsm5nIGzhurdwIGNo4bqheSBo4bq_dCwgaSA9ICRpIikKICAgIH0KICAgIGRlbGF5KDEwMCkKICAgIGpvYi5jYW5jZWxBbmRKb2luKCkKICAgIHByaW50bG4oIsSRw6MgaHXhu7ciKQp9Cg)

### K263 · `cancel()` không đợi coroutine dừng hẳn

`🟡 đã làm app` · `Kotlin`

**Gặp:** `job.cancel()` rồi đọc kết quả / xoá file ngay dòng sau, nhưng phần `finally` của job vẫn đang chạy và ghi đè.

**Vì sao:** `cancel()` chỉ **gửi** lệnh huỷ rồi trả về ngay. Job còn chạy `finally`, dọn dẹp, tới khi xong mới hết.

**Sửa:** cần chắc job đã dừng thì `job.cancelAndJoin()` (hàm suspend).

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFyIHNhdmVkID0gZmFsc2UKICAgIHZhbCBqb2IgPSBsYXVuY2goRGlzcGF0Y2hlcnMuRGVmYXVsdCkgewogICAgICAgIHRyeSB7IGRlbGF5KDEwXzAwMCkgfSBmaW5hbGx5IHsgVGhyZWFkLnNsZWVwKDEwMCk7IHNhdmVkID0gdHJ1ZSB9ICAvLyBk4buNbiBk4bq5cCBt4bqldCAxMDAgbXMKICAgIH0KICAgIGRlbGF5KDUwKQogICAgam9iLmNhbmNlbCgpICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgY2jhu4kgR-G7rEkgbOG7h25oIGh14bu3LCBraMO0bmcgxJHhu6NpCiAgICBwcmludGxuKCJzYXUgY2FuY2VsKCk6IHNhdmVkID0gJHNhdmVkLCBpc0NvbXBsZXRlZCA9ICR7am9iLmlzQ29tcGxldGVkfSIpCiAgICBqb2Iuam9pbigpICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKchSBob-G6t2MgZMO5bmcgam9iLmNhbmNlbEFuZEpvaW4oKQogICAgcHJpbnRsbigic2F1IGpvaW4oKTogICBzYXZlZCA9ICRzYXZlZCwgaXNDb21wbGV0ZWQgPSAke2pvYi5pc0NvbXBsZXRlZH0iKQp9Cg)

### K264 · Gọi hàm suspend trong `finally` sau khi bị huỷ: không chạy

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** "lưu nháp khi thoát" đặt trong `finally`, nhưng bản nháp không bao giờ được lưu; dòng log sau đó cũng không in.

**Vì sao:** coroutine đã ở trạng thái huỷ. Hàm suspend nào kiểm huỷ (`delay`, `withContext`, ghi DB) trong `finally` sẽ
ném `CancellationException` ngay lập tức.

**Sửa:** bọc phần dọn dẹp cần suspend trong `withContext(NonCancellable) { }`. Giữ phần này ngắn.

```kotlin
finally {
    withContext(NonCancellable) { saveDraft() }   // ✅
}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpzdXNwZW5kIGZ1biBzYXZlRHJhZnQoKSB7IGRlbGF5KDEwKTsgcHJpbnRsbigixJHDoyBsxrB1IG5ow6FwIikgfQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsKICAgIHZhbCBqb2IgPSBsYXVuY2ggewogICAgICAgIHRyeSB7CiAgICAgICAgICAgIGRlbGF5KDEwXzAwMCkKICAgICAgICB9IGZpbmFsbHkgewogICAgICAgICAgICBzYXZlRHJhZnQoKSAgICAgICAgICAgICAgICAgICAvLyDinYwgY29yb3V0aW5lIMSRw6MgYuG7iyBodeG7tzogZGVsYXkgbsOpbSBs4bqhaSBDYW5jZWxsYXRpb25FeGNlcHRpb24KICAgICAgICAgICAgLy8gd2l0aENvbnRleHQoTm9uQ2FuY2VsbGFibGUpIHsgc2F2ZURyYWZ0KCkgfSAvLyDinIUKICAgICAgICAgICAgcHJpbnRsbigiZmluYWxseSB4b25nIikKICAgICAgICB9CiAgICB9CiAgICBkZWxheSg1MCkKICAgIGpvYi5jYW5jZWxBbmRKb2luKCkKICAgIHByaW50bG4oIm1haW4geG9uZyIpCn0K)

### K265 · Một coroutine con lỗi kéo cả nhóm bị huỷ

`🟡 đã làm app` · `Kotlin`

**Gặp:** ảnh đại diện tải lỗi, thế là phần tải tên, tải danh sách cùng màn hình cũng bị huỷ và cả màn báo lỗi.

**Vì sao:** với `Job` thường (`coroutineScope`, `launch` lồng nhau), con lỗi thì huỷ cha, cha huỷ mọi con khác. Đó là
structured concurrency: một việc hỏng thì cả nhóm dừng.

**Sửa:** các việc độc lập thì đặt trong `supervisorScope { }` (hoặc scope có `SupervisorJob`) và bắt lỗi trong từng
`launch`.

```kotlin
supervisorScope {
    launch { try { loadAvatar() } catch (e: IOException) { showPlaceholder() } } // lỗi ở đây không huỷ việc dưới
    launch { loadName() }
}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpOiBVbml0ID0gcnVuQmxvY2tpbmcgewogICAgdHJ5IHsKICAgICAgICBjb3JvdXRpbmVTY29wZSB7CiAgICAgICAgICAgIGxhdW5jaCB7IGRlbGF5KDUwKTsgZXJyb3IoInThuqNpIOG6o25oIGzhu5dpIikgfQogICAgICAgICAgICBsYXVuY2ggewogICAgICAgICAgICAgICAgdHJ5IHsgZGVsYXkoMV8wMDApOyBwcmludGxuKCJsxrB1IHhvbmciKSB9CiAgICAgICAgICAgICAgICBmaW5hbGx5IHsgcHJpbnRsbigiYW5oIGVtIGLhu4sgaHXhu7cgdGhlbyIpIH0KICAgICAgICAgICAgfQogICAgICAgIH0KICAgIH0gY2F0Y2ggKGU6IEV4Y2VwdGlvbikgewogICAgICAgIHByaW50bG4oInNjb3BlIGNoYSBuaOG6rW46ICR7ZS5tZXNzYWdlfSIpCiAgICB9CiAgICAvLyDinIUgbXXhu5FuIGPDoWMgdmnhu4djIMSR4buZYyBs4bqtcDogc3VwZXJ2aXNvclNjb3BlIHsgLi4uIH0gdsOgIGLhuq90IGzhu5dpIHRyb25nIHThu6tuZyBsYXVuY2gKfQo) · Xem thêm: [Scope, huỷ và lỗi](../concepts/structured-concurrency.md), K266

### K266 · `launch(SupervisorJob())` không làm các con độc lập

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** đã thêm `SupervisorJob()` vào `launch` mà việc A lỗi vẫn huỷ việc B bên trong.

**Vì sao:** `SupervisorJob()` truyền vào `launch` chỉ làm **cha** của chính coroutine đó. Các `launch` bên trong vẫn là
con của một `Job` thường. Thêm nữa, coroutine đó bị tách khỏi scope gốc (K282).

**Sửa:** `SupervisorJob` đặt khi **tạo scope**: `CoroutineScope(SupervisorJob() + ...)`, hoặc dùng `supervisorScope { }`
bên trong.

```kotlin
scope.launch(SupervisorJob()) { launch { a() }; launch { b() } }    // ❌
scope.launch { supervisorScope { launch { a() }; launch { b() } } } // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIGhhbmRsZXIgPSBDb3JvdXRpbmVFeGNlcHRpb25IYW5kbGVyIHsgXywgZSAtPiBwcmludGxuKCJoYW5kbGVyOiAke2UubWVzc2FnZX0iKSB9CiAgICB2YWwgc2NvcGUgPSBDb3JvdXRpbmVTY29wZShKb2IoKSArIGhhbmRsZXIpCiAgICBzY29wZS5sYXVuY2goU3VwZXJ2aXNvckpvYigpKSB7ICAgICAgLy8g4p2MIFN1cGVydmlzb3JKb2Ig4bufIMSRw6J5IGNo4buJIGzDoG0gY2hhIGPhu6dhIGxhdW5jaCBuw6B5CiAgICAgICAgbGF1bmNoIHsgZGVsYXkoNTApOyBlcnJvcigiQSBs4buXaSIpIH0KICAgICAgICBsYXVuY2ggeyBkZWxheSgyMDApOyBwcmludGxuKCJCIHhvbmciKSB9ICAvLyB24bqrbiBi4buLIGh14bu3OiBBIHbDoCBCIGzDoCBjb24gY-G7p2EgbGF1bmNoIHRoxrDhu51uZwogICAgfS5qb2luKCkKICAgIC8vIOKchSB2YWwgc2NvcGUgPSBDb3JvdXRpbmVTY29wZShTdXBlcnZpc29ySm9iKCkgKyBoYW5kbGVyKTsgc2NvcGUubGF1bmNoIHsgQSB9OyBzY29wZS5sYXVuY2ggeyBCIH0KICAgIC8vIOKchSBob-G6t2MgYsOqbiB0cm9uZzogc3VwZXJ2aXNvclNjb3BlIHsgbGF1bmNoIHsgQSB9OyBsYXVuY2ggeyBCIH0gfQogICAgZGVsYXkoMzAwKQogICAgcHJpbnRsbigiQiBjw7MgaW4ga2jDtG5nPyIpCn0K) · Xem thêm: K265, K282

### K267 · Việc phải sống lâu đặt trong `viewModelScope`: bấm Back là dừng

`🟡 đã làm app` · `Android` · `KMP`

**Gặp:** đang ghi chuyến đi / tải file, người dùng bấm Back về màn trước là việc dừng giữa chừng.

**Vì sao:** `viewModelScope` bị huỷ khi ViewModel bị xoá (rời khỏi màn hình đó). Việc của **app** (ghi GPS, đồng bộ) không
thuộc về một màn hình.

**Sửa:** việc thuộc app đặt trong một scope sống cùng process (một `single` trong DI) với `SupervisorJob`, và đặt state
trong class đó thay vì trong ViewModel. Việc phải sống cả khi app bị kill thì dùng WorkManager / Foreground Service.

```kotlin
single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }  // scope của app
single { TripRecorder(location = get(), repository = get(), scope = get()) }
```

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/di/Koin.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/di/Koin.kt)

### K268 · `try/catch` quanh `launch` không bắt được lỗi bên trong

`🟢 mới học` · `Kotlin`

**Gặp:** bọc `try { scope.launch { ... } } catch (e: Exception)` mà app vẫn crash với lỗi từ bên trong `launch`.

**Vì sao:** `launch` chỉ **khởi động** coroutine rồi trả về ngay. Lúc lỗi xảy ra, khối `try` đã qua từ lâu.

**Sửa:** đặt `try/catch` **bên trong** `launch`, quanh đúng lời gọi có thể lỗi.

```kotlin
try { scope.launch { load() } } catch (e: IOException) { }   // ❌
scope.launch { try { load() } catch (e: IOException) { } }   // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIHNjb3BlID0gQ29yb3V0aW5lU2NvcGUoU3VwZXJ2aXNvckpvYigpICsgQ29yb3V0aW5lRXhjZXB0aW9uSGFuZGxlciB7IF8sIGUgLT4gcHJpbnRsbigia2jDtG5nIGFpIGLhuq90OiAke2UubWVzc2FnZX0iKSB9KQogICAgdHJ5IHsKICAgICAgICBzY29wZS5sYXVuY2ggeyBlcnJvcigibeG6oW5nIGzhu5dpIikgfSAgIC8vIOKdjCB0cnkvY2F0Y2gg4bufIE5HT8OASSBsYXVuY2gga2jDtG5nIGLhuq90IMSRxrDhu6NjCiAgICB9IGNhdGNoIChlOiBFeGNlcHRpb24pIHsKICAgICAgICBwcmludGxuKCJi4bqvdCDEkcaw4bujYzogJHtlLm1lc3NhZ2V9IikgICAgICAvLyBraMO0bmcgYmFvIGdp4budIGluCiAgICB9CiAgICBzY29wZS5sYXVuY2ggewogICAgICAgIHRyeSB7IGVycm9yKCJt4bqhbmcgbOG7l2kgMiIpIH0gICAgICAgICAgLy8g4pyFIHRyeS9jYXRjaCBCw4pOIFRST05HIGNvcm91dGluZQogICAgICAgIGNhdGNoIChlOiBJbGxlZ2FsU3RhdGVFeGNlcHRpb24pIHsgcHJpbnRsbigiYuG6r3QgxJHGsOG7o2M6ICR7ZS5tZXNzYWdlfSIpIH0KICAgIH0KICAgIGRlbGF5KDEwMCkKfQo)

### K269 · `async` lỗi mà không `await` vẫn làm hỏng cả scope

`🟡 đã làm app` · `Kotlin`

**Gặp:** tưởng `async` giữ lỗi lại tới khi `await`, nên không `await` cái tải phụ. Nhưng nó lỗi là cả `coroutineScope`
bị huỷ.

**Vì sao:** trong scope có `Job` thường, `async` lỗi vẫn huỷ cha ngay lúc lỗi, giống `launch`. Lỗi giữ trong `Deferred`
chỉ là thêm, không thay cho việc lan lên cha.

**Sửa:** việc phụ được phép lỗi thì đặt trong `supervisorScope` và `try/catch` quanh `await()`, hoặc bắt lỗi ngay trong
`async`.

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdHJ5IHsKICAgICAgICBjb3JvdXRpbmVTY29wZSB7CiAgICAgICAgICAgIHZhbCBhdmF0YXIgPSBhc3luYyB7IGRlbGF5KDUwKTsgZXJyb3IoImF2YXRhciBs4buXaSIpIH0KICAgICAgICAgICAgbGF1bmNoIHsgZGVsYXkoMzAwKTsgcHJpbnRsbigidOG6o2kgdMOqbiB4b25nIikgfQogICAgICAgICAgICBwcmludGxuKCJraMO0bmcgYXdhaXQgYXZhdGFyIikgICAgLy8g4p2MIHTGsOG7n25nIGtow7RuZyBhd2FpdCB0aMOsIGzhu5dpIGLhu4sgYuG7jyBxdWEKICAgICAgICB9CiAgICB9IGNhdGNoIChlOiBFeGNlcHRpb24pIHsKICAgICAgICBwcmludGxuKCJ24bqrbiBs4buXaSBj4bqjIHNjb3BlOiAke2UubWVzc2FnZX0iKQogICAgfQp9Cg) · Xem thêm: K265, K273

### K270 · `CoroutineExceptionHandler` đặt ở coroutine con không có tác dụng

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** đã gắn handler vào `launch` bên trong mà lỗi vẫn in ra stderr `Exception in thread "DefaultDispatcher-worker-2"`
(hoặc crash app trên Android).

**Vì sao:** lỗi của coroutine con được chuyển lên cha. Chỉ coroutine **gốc** (con trực tiếp của scope) mới dùng handler.
Handler cũng không bắt lỗi của `async` (lỗi đó nằm trong `Deferred`).

**Sửa:** gắn handler vào scope (`CoroutineScope(SupervisorJob() + handler)`) hoặc vào `launch` gốc. Handler chỉ để
**ghi log** lỗi chưa ai xử lý; xử lý lỗi thật thì dùng `try/catch` trong coroutine.

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIGhhbmRsZXIgPSBDb3JvdXRpbmVFeGNlcHRpb25IYW5kbGVyIHsgXywgZSAtPiBwcmludGxuKCJoYW5kbGVyIGLhuq90OiAke2UubWVzc2FnZX0iKSB9CiAgICB2YWwgc2NvcGUgPSBDb3JvdXRpbmVTY29wZShTdXBlcnZpc29ySm9iKCkpCiAgICBzY29wZS5sYXVuY2ggewogICAgICAgIGxhdW5jaChoYW5kbGVyKSB7IGVycm9yKCJs4buXaSDhu58gY29uIikgfSAgIC8vIOKdjCBoYW5kbGVyIOG7nyBjb3JvdXRpbmUgY29uIGLhu4sgYuG7jyBxdWEKICAgIH0uam9pbigpCiAgICBzY29wZS5sYXVuY2goaGFuZGxlcikgeyAgICAgICAgICAgICAgICAgICAgICAvLyDinIUgxJHhurd0IOG7nyBjb3JvdXRpbmUgZ-G7kWMgKGxhdW5jaCBj4bunYSBzY29wZSkKICAgICAgICBsYXVuY2ggeyBlcnJvcigibOG7l2kg4bufIGNvbiAyIikgfQogICAgfS5qb2luKCkKfQo)

### K271 · `lifecycleScope.launch { flow.collect { } }` vẫn chạy khi app ở nền

`🟡 đã làm app` · `Android`

**Gặp:** về màn hình chính rồi mà app vẫn nhận cập nhật GPS / socket, vẫn cập nhật UI không ai thấy, tốn pin.

**Vì sao:** `lifecycleScope` chỉ bị huỷ khi Activity **bị huỷ** (`DESTROYED`), không phải khi vào nền (`STOPPED`).

**Sửa:** collect trong `repeatOnLifecycle(Lifecycle.State.STARTED)`: tự dừng khi vào nền, tự chạy lại khi quay lại.
Trong Compose dùng `collectAsStateWithLifecycle()`.

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect { render(it) }    // ✅
    }
}
```

### K272 · `async { }.await()` ngay lập tức: chạy tuần tự

`🟢 mới học` · `Kotlin`

**Gặp:** dùng `async` để tải 2 thứ song song nhưng tổng thời gian vẫn bằng cộng hai lần tải.

**Vì sao:** `async { a() }.await()` đợi `a` xong rồi mới tới dòng sau tạo `async` thứ hai.

**Sửa:** tạo tất cả `async` trước, `await` sau. Danh sách thì `items.map { async { load(it) } }.awaitAll()`.

```kotlin
val a = async { loadA() }.await(); val b = async { loadB() }.await()  // ❌ tuần tự
val a = async { loadA() }; val b = async { loadB() }
val sum = a.await() + b.await()                                       // ✅ song song
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW4udGltZS5tZWFzdXJlVGltZQoKc3VzcGVuZCBmdW4gbG9hZEEoKTogSW50IHsgZGVsYXkoMzAwKTsgcmV0dXJuIDEgfQpzdXNwZW5kIGZ1biBsb2FkQigpOiBJbnQgeyBkZWxheSgzMDApOyByZXR1cm4gMiB9CgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIHQxID0gbWVhc3VyZVRpbWUgewogICAgICAgIHZhbCBhID0gYXN5bmMgeyBsb2FkQSgpIH0uYXdhaXQoKSAgIC8vIOKdjCBhd2FpdCBuZ2F5IOKGkiDEkeG7o2kgeG9uZyBBIG3hu5tpIGLhuq90IMSR4bqndSBCCiAgICAgICAgdmFsIGIgPSBhc3luYyB7IGxvYWRCKCkgfS5hd2FpdCgpCiAgICAgICAgcHJpbnRsbihhICsgYikKICAgIH0KICAgIHZhbCB0MiA9IG1lYXN1cmVUaW1lIHsKICAgICAgICB2YWwgYSA9IGFzeW5jIHsgbG9hZEEoKSB9ICAgICAgICAgICAvLyDinIUga2jhu59pIMSR4buZbmcgY-G6oyBoYWkgdHLGsOG7m2MKICAgICAgICB2YWwgYiA9IGFzeW5jIHsgbG9hZEIoKSB9CiAgICAgICAgcHJpbnRsbihhLmF3YWl0KCkgKyBiLmF3YWl0KCkpCiAgICB9CiAgICBwcmludGxuKCJ0deG6p24gdOG7sTogJHQxLCBzb25nIHNvbmc6ICR0MiIpCn0K)

### K273 · `async` không ai `await`: lỗi biến mất không dấu vết

`🟡 đã làm app` · `Kotlin`

**Gặp:** trong `supervisorScope` (hoặc scope có `SupervisorJob`), một việc chạy bằng `async` lỗi nhưng không có log,
không crash, chỉ là việc đó không xảy ra.

**Vì sao:** với supervisor, lỗi của `async` không lan lên cha mà nằm trong `Deferred`, chờ `await()` lấy ra. Không ai
`await` thì không ai thấy.

**Sửa:** không cần kết quả thì dùng `launch` (lỗi được báo qua handler). Dùng `async` thì luôn `await`.

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgc3VwZXJ2aXNvclNjb3BlIHsKICAgICAgICBhc3luYyB7IGVycm9yKCJn4butaSBhbmFseXRpY3MgbOG7l2kiKSB9ICAgLy8g4p2MIGtow7RuZyBhaSBhd2FpdCDihpIgbOG7l2kgYmnhur9uIG3huqV0LCBraMO0bmcgbG9nCiAgICAgICAgLy8gbGF1bmNoIHsgZXJyb3IoIi4uLiIpIH0gICAgICAgICAgICAgLy8g4pyFIGtow7RuZyBj4bqnbiBr4bq_dCBxdeG6oyB0aMOsIGTDuW5nIGxhdW5jaDogbOG7l2kgxJHGsOG7o2MgYsOhbwogICAgICAgIGRlbGF5KDEwMCkKICAgICAgICBwcmludGxuKCJjaOG6oXkgdGnhur9wIG5oxrAga2jDtG5nIGPDsyBnw6wiKQogICAgfQp9Cg) · Xem thêm: K269

### K274 · Đọc DB / file trên main thread trong coroutine

`🟢 mới học` · `KMP`

**Gặp:** Android báo `StrictMode` vi phạm hoặc giật khi mở danh sách; iOS bản đồ khựng lại. Code đã nằm trong
`viewModelScope.launch` rồi.

**Vì sao:** `viewModelScope` chạy trên **main thread**. Lời gọi chặn (SQLite, đọc file, thư viện Java đồng bộ) bên
trong vẫn chặn main thread; `suspend` hay `launch` không tự chuyển thread (K286).

**Sửa:** tầng repository bọc lời gọi chặn bằng `withContext(Dispatchers.IO)`, để người gọi từ đâu cũng an toàn.

```kotlin
private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gcmVhZERiKCk6IFN0cmluZyB7CiAgICBUaHJlYWQuc2xlZXAoMjAwKSAgICAgICAgICAgICAgICAgICAgLy8gZ2nhuqMgbOG6rXAgU1FMaXRlOiBjaOG6t24gdGhyZWFkCiAgICByZXR1cm4gIjMgY2h1eeG6v24gxJFpIgp9CgpzdXNwZW5kIGZ1biBsb2FkVHJpcHMoKTogU3RyaW5nID0KICAgIHdpdGhDb250ZXh0KERpc3BhdGNoZXJzLklPKSB7ICAgICAgICAvLyDinIUgY2h1eeG7g24gdmnhu4djIGNo4bq3biBzYW5nIElPCiAgICAgICAgcHJpbnRsbigixJHhu41jIERCIHRyw6puICR7VGhyZWFkLmN1cnJlbnRUaHJlYWQoKS5uYW1lfSIpCiAgICAgICAgcmVhZERiKCkKICAgIH0KCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZyB7CiAgICBwcmludGxuKCJn4buNaSB04burICR7VGhyZWFkLmN1cnJlbnRUaHJlYWQoKS5uYW1lfSIpCiAgICBwcmludGxuKGxvYWRUcmlwcygpKQp9Cg)

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt) ·
Xem thêm: [Dispatcher](../concepts/dispatchers.md), K277

### K275 · `Module with the Main dispatcher is missing`

`🟢 mới học` · `Kotlin` · `KMP`

**Gặp:** `java.lang.IllegalStateException: Module with the Main dispatcher is missing. Add dependency providing the Main
dispatcher, e.g. 'kotlinx-coroutines-android' and ensure it has the same version as 'kotlinx-coroutines-core'`. Hay gặp
khi chạy unit test của code dùng `Dispatchers.Main` / `viewModelScope`.

**Vì sao:** JVM thuần (unit test, `main()` console) không có main thread UI. `Dispatchers.Main` chỉ có khi có
`kotlinx-coroutines-android` (Android), Swing/JavaFX, hoặc trên iOS.

**Sửa:** trong test gọi `Dispatchers.setMain(StandardTestDispatcher())` trước và `Dispatchers.resetMain()` sau (thư viện
`kotlinx-coroutines-test`). Tốt hơn: cho code chung nhận scope / dispatcher qua constructor để test truyền vào.

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgd2l0aENvbnRleHQoRGlzcGF0Y2hlcnMuTWFpbikgeyAgICAgIC8vIOKdjCBKVk0gdGh14bqnbiAvIHVuaXQgdGVzdCBraMO0bmcgY8OzIG1haW4gdGhyZWFkIFVJCiAgICAgICAgcHJpbnRsbigiY-G6rXAgbmjhuq10IFVJIikKICAgIH0KfQo)

### K276 · Việc chặn (I/O) chạy trên `Dispatchers.Default`

`🟡 đã làm app` · `Kotlin`

**Gặp:** 32 việc chặn 200 ms trên `Default` mất hơn 3 giây trên máy 2 nhân; trên `IO` chỉ ~200 ms. Trong app: tính toán
khác bị nghẽn khi đang gọi mạng/đọc file.

**Vì sao:** `Dispatchers.Default` có số thread bằng số nhân CPU, dành cho việc **tính toán**. Việc chặn chiếm hết các
thread đó. `Dispatchers.IO` có tới 64 thread (hoặc số nhân, lấy số lớn hơn), dành cho việc **chờ**.

**Sửa:** tính toán (sort, parse JSON lớn) dùng `Default`; chặn (file, SQLite, thư viện đồng bộ) dùng `IO`.

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW4udGltZS5tZWFzdXJlVGltZQoKZnVuIGJsb2NraW5nQ2FsbCgpID0gVGhyZWFkLnNsZWVwKDIwMCkgIC8vIGfhu41pIHRoxrAgdmnhu4duIGPFqSAvIGZpbGUgLyBzb2NrZXQKCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZyB7CiAgICB2YWwgY29yZXMgPSBSdW50aW1lLmdldFJ1bnRpbWUoKS5hdmFpbGFibGVQcm9jZXNzb3JzKCkKICAgIHZhbCBuID0gMzIKICAgIHZhbCBvbkRlZmF1bHQgPSBtZWFzdXJlVGltZSB7IGNvcm91dGluZVNjb3BlIHsgcmVwZWF0KG4pIHsgbGF1bmNoKERpc3BhdGNoZXJzLkRlZmF1bHQpIHsgYmxvY2tpbmdDYWxsKCkgfSB9IH0gfQogICAgdmFsIG9uSW8gPSBtZWFzdXJlVGltZSB7IGNvcm91dGluZVNjb3BlIHsgcmVwZWF0KG4pIHsgbGF1bmNoKERpc3BhdGNoZXJzLklPKSB7IGJsb2NraW5nQ2FsbCgpIH0gfSB9IH0KICAgIHByaW50bG4oIiRjb3JlcyBuaMOibiwgJG4gdmnhu4djIGNo4bq3biAyMDAgbXMiKQogICAgcHJpbnRsbigiRGVmYXVsdDogJG9uRGVmYXVsdCAgIOKdjCBjaOG7iSB-JGNvcmVzIHRocmVhZCIpCiAgICBwcmludGxuKCJJTzogICAgICAkb25JbyAgIOKchSB04bubaSA2NCB0aHJlYWQiKQp9Cg) · Xem thêm: [Dispatcher](../concepts/dispatchers.md)

### K277 · `Unresolved reference 'IO'` trong `commonMain`

`🟡 đã làm app` · `KMP`

**Gặp:** `Dispatchers.IO` dùng được trong `androidMain` nhưng trong `commonMain` báo `Unresolved reference 'IO'.`

**Vì sao:** trong code chung, `Dispatchers.IO` là một **extension property** khai báo riêng (có từ kotlinx.coroutines 1.7,
cho JVM và Native), nên cần import riêng. Trên JVM nó là member nên không cần import, vì vậy code Android không bị.
Nếu project có target JS / Wasm thì `IO` không có cho các target đó.

**Sửa:** thêm `import kotlinx.coroutines.IO`. Có target JS/Wasm thì khai báo `expect val ioDispatcher` và `actual` cho
từng nền tảng.

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO        // ✅ cần trong commonMain
```

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt)

### K278 · Nhiều coroutine cùng ghi một biến: kết quả thiếu

`🟡 đã làm app` · `Kotlin`

**Gặp:** 4 coroutine mỗi cái cộng 1.000.000 lần, kết quả ra khoảng 3 triệu thay vì 4 triệu. Mỗi lần chạy một số khác.

**Vì sao:** trên `Default` / `IO` các coroutine chạy **song song** trên nhiều thread. `count++` là đọc–cộng–ghi, hai
thread chen nhau thì mất lượt cộng. `@Volatile` không sửa được điều này.

**Sửa:** gom việc ghi state vào một chỗ: `MutableStateFlow.update { }`, `Mutex.withLock { }`, biến atomic, hoặc cho mọi
lần ghi chạy trên một thread (`Dispatchers.Main`, `limitedParallelism(1)`).

```kotlin
val mutex = Mutex()
mutex.withLock { count++ }               // ✅
_state.update { it.copy(count = it.count + 1) }  // ✅ StateFlow
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuc3luYy4qCgovLyBAVm9sYXRpbGUgY2jhu4kgxJHhu4MgSklUIGtow7RuZyBn4buZcCB2w7JuZyBs4bq3cDsgbsOzIEtIw5RORyBsw6BtIHZhbHVlKysgYW4gdG_DoG4KY2xhc3MgQ291bnRlciB7IEBWb2xhdGlsZSB2YXIgdmFsdWUgPSAwIH0KCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZyB7CiAgICB2YWwgYyA9IENvdW50ZXIoKQogICAgd2l0aENvbnRleHQoRGlzcGF0Y2hlcnMuRGVmYXVsdCkgewogICAgICAgIHJlcGVhdCg0KSB7CiAgICAgICAgICAgIGxhdW5jaCB7IHJlcGVhdCgxXzAwMF8wMDApIHsgYy52YWx1ZSsrIH0gfSAgICAgICAgICAvLyDinYwgbmhp4buBdSB0aHJlYWQgY8O5bmcgZ2hpIG3hu5l0IGJp4bq_bgogICAgICAgIH0KICAgIH0KICAgIHByaW50bG4oImtow7RuZyBraG_DoTogJHtjLnZhbHVlfSAoxJHDum5nIHBo4bqjaSBsw6AgNDAwMDAwMCkiKQoKICAgIHZhbCBtdXRleCA9IE11dGV4KCkKICAgIHZhbCBzYWZlID0gQ291bnRlcigpCiAgICB3aXRoQ29udGV4dChEaXNwYXRjaGVycy5EZWZhdWx0KSB7CiAgICAgICAgcmVwZWF0KDQpIHsKICAgICAgICAgICAgbGF1bmNoIHsgcmVwZWF0KDI1XzAwMCkgeyBtdXRleC53aXRoTG9jayB7IHNhZmUudmFsdWUrKyB9IH0gfSAvLyDinIUgTXV0ZXgKICAgICAgICB9CiAgICB9CiAgICBwcmludGxuKCJjw7MgTXV0ZXg6ICAgJHtzYWZlLnZhbHVlfSAoxJHDum5nIHBo4bqjaSBsw6AgMTAwMDAwKSIpCn0K) · Xem thêm: K279

### K279 · `The 'delay' suspension point is inside a critical section`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `The 'delay' suspension point is inside a critical section.` khi gọi hàm suspend bên trong `synchronized(lock) { }`.

**Vì sao:** `synchronized` khoá theo **thread**. Coroutine treo ở giữa có thể chạy tiếp trên thread khác, lúc đó thread
khác phải mở khoá mà nó không giữ. Compiler chặn từ đầu.

**Sửa:** dùng `Mutex` của kotlinx.coroutines: khoá theo coroutine, chờ bằng cách treo chứ không chặn thread, và có trên
mọi nền tảng KMP.

```kotlin
synchronized(lock) { delay(10); save() }       // ❌
mutex.withLock { delay(10); save() }           // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgp2YWwgbG9jayA9IEFueSgpCgpzdXNwZW5kIGZ1biBzYXZlKG5vdGU6IFN0cmluZykgewogICAgc3luY2hyb25pemVkKGxvY2spIHsKICAgICAgICBkZWxheSgxMCkgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwga2jDtG5nIMSRxrDhu6NjIHRyZW8gY29yb3V0aW5lIGtoaSDEkWFuZyBnaeG7ryBraG_DoSBj4bunYSB0aHJlYWQKICAgICAgICBwcmludGxuKCJsxrB1ICRub3RlIikKICAgIH0KfQovLyDinIUgdmFsIG11dGV4ID0gTXV0ZXgoKTsgIHN1c3BlbmQgZnVuIHNhdmUobm90ZTogU3RyaW5nKSA9IG11dGV4LndpdGhMb2NrIHsgZGVsYXkoMTApOyBwcmludGxuKCJsxrB1ICRub3RlIikgfQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsgc2F2ZSgiYSIpIH0K) · Xem thêm: K280

### K280 · `Mutex` gọi lồng: treo mãi

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** app treo, không lỗi, không crash. Dump coroutine thấy một coroutine đợi `Mutex` mà chính nó đang giữ.

**Vì sao:** `Mutex` **không reentrant**: coroutine đang giữ khoá gọi `withLock` lần nữa (qua một hàm khác) sẽ đợi chính
mình. Khác `synchronized` của Java (cho phép lấy lại).

**Sửa:** chỉ khoá ở hàm ngoài cùng (public). Hàm nội bộ đã chạy dưới khoá thì không khoá lại; đặt tên kiểu
`updateIndexLocked()` để nhớ.

```kotlin
suspend fun saveNote() = mutex.withLock { writeNote(); updateIndexLocked() }  // ✅
private suspend fun updateIndexLocked() { /* không withLock */ }
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuc3luYy4qCgp2YWwgbXV0ZXggPSBNdXRleCgpCgpzdXNwZW5kIGZ1biBzYXZlTm90ZSgpID0gbXV0ZXgud2l0aExvY2sgewogICAgcHJpbnRsbigibMawdSBub3RlIikKICAgIHVwZGF0ZUluZGV4KCkgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgZ-G7jWkgaMOgbSBraMOhYyBjxaluZyBs4bqleSBjw7luZyBtdXRleAp9CgpzdXNwZW5kIGZ1biB1cGRhdGVJbmRleCgpID0gbXV0ZXgud2l0aExvY2sgeyBwcmludGxuKCJj4bqtcCBuaOG6rXQgaW5kZXgiKSB9Ci8vIOKchSB0w6FjaCBi4bqjbiBraMO0bmcga2hvw6E6IHByaXZhdGUgc3VzcGVuZCBmdW4gdXBkYXRlSW5kZXhMb2NrZWQoKSB7IC4uLiB9IHbDoCBjaOG7iSBraG_DoSDhu58gaMOgbSBuZ2_DoGkgY8O5bmcKCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZyB7CiAgICB3aXRoVGltZW91dE9yTnVsbCgxXzAwMCkgeyBzYXZlTm90ZSgpIH0gPzogcHJpbnRsbigi4p2MIHRyZW86IE11dGV4IGtow7RuZyBjaG8gbOG6pXkgbOG6oWkgbOG6p24gaGFpIikKfQo)

### K281 · Hai lần ghi DB tới sai thứ tự: xoá rồi hoàn tác, note vẫn mất

`🔴 khó, hiếm nhưng đau` · `KMP`

**Gặp:** bấm xoá rồi bấm "Hoàn tác" thật nhanh, thỉnh thoảng note vẫn bị xoá. Không tái hiện được khi debug chậm.

**Vì sao:** mỗi lần ghi là một `launch` chạy trên dispatcher nền nhiều thread. Hai `launch` gửi liền nhau **không** chắc
chạy theo thứ tự đã gửi; "khôi phục" có thể tới SQLite trước "xoá".

**Sửa:** cho mọi lần ghi đi qua một `Mutex` (công bằng, ai tới trước được trước), hoặc một `Channel` có một coroutine
xử lý lần lượt (actor), hoặc dispatcher một thread.

```kotlin
private val writeLock = Mutex()
private fun write(block: suspend () -> Unit) {
    scope.launch { writeLock.withLock { block() } }
}
```

**Gặp ở:** [`PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/core/NotesCore.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/core/NotesCore.kt) ·
Xem thêm: K278, K288

### K282 · `launch(Job())` cắt đứt quan hệ cha–con

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** đóng màn hình (huỷ scope) rồi mà một việc vẫn chạy và log tiếp, trong khi các việc khác đã dừng.

**Vì sao:** truyền `Job()` (hay bất kỳ `Job` nào) vào `launch` / `async` / `withContext` thay cha của coroutine đó bằng
job mới. Nó không còn là con của scope: không bị huỷ theo, cha cũng không đợi nó.

**Sửa:** đừng truyền `Job` vào builder. Cần chạy độc lập có chủ đích thì dùng scope khác có vòng đời rõ (K267).

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIHNjcmVlbiA9IGxhdW5jaCB7CiAgICAgICAgbGF1bmNoKEpvYigpKSB7ICAgICAgICAgICAgICAgICAgLy8g4p2MIEpvYigpIG3hu5tpIGPhuq90IMSR4bupdCBxdWFuIGjhu4cgY2hh4oCTY29uCiAgICAgICAgICAgIHJlcGVhdCg1KSB7IGRlbGF5KDEwMCk7IHByaW50bG4oIuKdjCB24bqrbiB04bqjaSBzYXUga2hpIG3DoG4gaMOsbmggxJHDs25nICgkaXQpIikgfQogICAgICAgIH0KICAgICAgICBsYXVuY2ggeyAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinIUgY29uIHRo4bqtdDogYuG7iyBodeG7tyBjw7luZyBjaGEKICAgICAgICAgICAgcmVwZWF0KDUpIHsgZGVsYXkoMTAwKTsgcHJpbnRsbigi4pyFIGNvbiB0aOG6rXQgKCRpdCkiKSB9CiAgICAgICAgfQogICAgfQogICAgZGVsYXkoMjUwKQogICAgc2NyZWVuLmNhbmNlbCgpCiAgICBwcmludGxuKCItLS0gxJHDs25nIG3DoG4gaMOsbmgiKQogICAgZGVsYXkoNDAwKQp9Cg) · Xem thêm: K266

### K283 · Hàm suspend tự `CoroutineScope(...).launch` rồi trả về ngay

`🟡 đã làm app` · `Kotlin`

**Gặp:** `loadBoth()` trả về, màn hình hiện ra trống; một lúc sau dữ liệu mới về (hoặc không bao giờ về nếu màn hình
đã đóng).

**Vì sao:** `CoroutineScope(...)` (chữ C hoa) tạo một scope **mới**, không liên quan coroutine đang chạy. `launch` trên
đó chạy riêng, hàm suspend không đợi.

**Sửa:** trong hàm suspend dùng `coroutineScope { }` (chữ c thường): nó đợi mọi con xong rồi mới trả về, và bị huỷ theo
người gọi.

```kotlin
suspend fun loadBoth() = coroutineScope {      // ✅ đợi cả hai
    launch { loadA() }
    launch { loadB() }
}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpzdXNwZW5kIGZ1biBsb2FkQm90aCgpIHsKICAgIENvcm91dGluZVNjb3BlKERpc3BhdGNoZXJzLkRlZmF1bHQpLmxhdW5jaCB7ICAgLy8g4p2MIHNjb3BlIG3hu5tpOiBow6BtIHRy4bqjIHbhu4EgbmdheSwga2jDtG5nIMSR4bujaQogICAgICAgIGRlbGF5KDEwMCk7IHByaW50bG4oIuKdjCB04bqjaSB4b25nIChxdcOhIG114buZbikiKQogICAgfQogICAgLy8gY29yb3V0aW5lU2NvcGUgeyBsYXVuY2ggeyBkZWxheSgxMDApOyBwcmludGxuKCLinIUgdOG6o2kgeG9uZyIpIH0gfSAvLyDinIUgxJHhu6NpIG3hu41pIGNvbiB4b25nCn0KCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZyB7CiAgICBsb2FkQm90aCgpCiAgICBwcmludGxuKCJsb2FkQm90aCgpIMSRw6MgdHLhuqMgduG7gSDihpIgaGnhu4duIG3DoG4gaMOsbmgiKQogICAgZGVsYXkoMjAwKQp9Cg) · Xem thêm: [Scope, huỷ và lỗi](../concepts/structured-concurrency.md)

### K284 · Code trong `launch` chưa chạy ngay

`🟢 mới học` · `Kotlin`

**Gặp:** log trong `launch` in ra **sau** dòng nằm bên dưới `launch`; biến gán trong `launch` vẫn là giá trị cũ ở dòng
ngay sau.

**Vì sao:** `launch` chỉ **xếp lịch** coroutine trên dispatcher. Code gọi `launch` chạy tiếp tới khi nó treo hoặc kết
thúc, lúc đó coroutine mới được chạy.

**Sửa:** đừng dựa vào thứ tự giữa code sau `launch` và code trong `launch`. Cần kết quả thì `async` + `await`, hoặc gọi
thẳng hàm suspend (không `launch`).

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgcHJpbnRsbigiMSIpCiAgICBsYXVuY2ggeyBwcmludGxuKCIzIOKAlCBsYXVuY2ggY2jGsGEgY2jhuqF5IG5nYXksIGNo4buJIMSRxrDhu6NjIHjhur9wIGzhu4tjaCIpIH0KICAgIHByaW50bG4oIjIiKQogICAgeWllbGQoKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIG5oxrDhu51uZyB0aHJlYWQ6IGzDumMgbsOgeSBsYXVuY2ggbeG7m2kgY2jhuqF5CiAgICBwcmludGxuKCI0IikKfQo)

### K285 · Nhiều `launch` trong `runBlocking` không chạy song song thật

`🟡 đã làm app` · `Kotlin`

**Gặp:** chia việc tính toán nặng ra nhiều `launch` mà tổng thời gian không giảm.

**Vì sao:** `launch` thừa hưởng dispatcher của scope. `runBlocking` (và `Dispatchers.Main`) chỉ có **một** thread, nên
các coroutine chạy xen kẽ, không chạy cùng lúc. Việc tính toán không có điểm treo thì còn chạy lần lượt.

**Sửa:** việc tính toán nặng chạy trên `Dispatchers.Default`: `launch(Dispatchers.Default) { }` hoặc
`withContext(Dispatchers.Default)`. Trên máy nhiều nhân sẽ nhanh hơn rõ.

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW4udGltZS5tZWFzdXJlVGltZQoKZnVuIGhlYXZ5KCk6IExvbmcgeyB2YXIgcyA9IDBMOyBmb3IgKGkgaW4gMCB1bnRpbCAzMDBfMDAwXzAwMCkgcyArPSBpICUgNzsgcmV0dXJuIHMgfQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsKICAgIHZhbCB0MSA9IG1lYXN1cmVUaW1lIHsKICAgICAgICBjb3JvdXRpbmVTY29wZSB7IHJlcGVhdCgyKSB7IGxhdW5jaCB7IGhlYXZ5KCkgfSB9IH0gICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBjw7luZyAxIHRocmVhZCBj4bunYSBydW5CbG9ja2luZwogICAgfQogICAgdmFsIHQyID0gbWVhc3VyZVRpbWUgewogICAgICAgIGNvcm91dGluZVNjb3BlIHsgcmVwZWF0KDIpIHsgbGF1bmNoKERpc3BhdGNoZXJzLkRlZmF1bHQpIHsgaGVhdnkoKSB9IH0gfSAgLy8g4pyFIGNo4bqheSB0csOqbiBuaGnhu4F1IG5ow6JuCiAgICB9CiAgICBwcmludGxuKCJydW5CbG9ja2luZzogJHQxLCBEZWZhdWx0OiAkdDIiKQp9Cg) · Xem thêm: [Dispatcher](../concepts/dispatchers.md)

### K286 · Tưởng `suspend` là "chạy nền"

`🟢 mới học` · `Kotlin`

**Gặp:** viết `suspend fun parseBig()` rồi gọi từ UI, vẫn giật. Log tên thread thấy vẫn là `main`.

**Vì sao:** `suspend` chỉ có nghĩa "hàm này **có thể** tạm dừng". Nó chạy trên thread của người gọi tới khi chính nó
đổi bằng `withContext`.

**Sửa:** hàm làm việc nặng tự chọn dispatcher bên trong (`withContext(Dispatchers.Default / IO)`). Quy ước: hàm suspend
phải an toàn khi gọi từ main thread (main-safe).

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpzdXNwZW5kIGZ1biBwYXJzZUJpZygpOiBJbnQgeyAgICAgICAgICAgIC8vIGBzdXNwZW5kYCBLSMOUTkcgdOG7sSDEkcawYSB2aeG7h2MgcmEgdGhyZWFkIGtow6FjCiAgICBwcmludGxuKCJwYXJzZSB0csOqbiAke1RocmVhZC5jdXJyZW50VGhyZWFkKCkubmFtZX0iKQogICAgVGhyZWFkLnNsZWVwKDEwMCkgICAgICAgICAgICAgICAgICAgIC8vIHbhuqtuIGNo4bq3biB0aHJlYWQgZ-G7jWkgbsOzCiAgICByZXR1cm4gNDIKfQovLyDinIUgc3VzcGVuZCBmdW4gcGFyc2VCaWcoKSA9IHdpdGhDb250ZXh0KERpc3BhdGNoZXJzLkRlZmF1bHQpIHsgLi4uIH0KCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZyB7CiAgICBwcmludGxuKCJn4buNaSB04burICR7VGhyZWFkLmN1cnJlbnRUaHJlYWQoKS5uYW1lfSIpCiAgICBwcmludGxuKHBhcnNlQmlnKCkpCn0K) · Xem thêm: K274

### K287 · `for (x in channel)` treo mãi vì channel không được đóng

`🟡 đã làm app` · `Kotlin`

**Gặp:** đã nhận hết dữ liệu nhưng code sau vòng `for` không bao giờ chạy; test treo tới timeout.

**Vì sao:** vòng `for` trên `Channel` chỉ kết thúc khi channel **đóng**. Bên gửi gửi xong mà không `close()` thì bên nhận
đợi phần tử tiếp theo mãi.

**Sửa:** bên gửi gọi `close()` khi xong (đặt trong `finally`). Hoặc dùng `produce { }`: tự đóng khi khối kết thúc.

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuY2hhbm5lbHMuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsKICAgIHZhbCBjaCA9IENoYW5uZWw8SW50PigpCiAgICBsYXVuY2ggewogICAgICAgIGZvciAoaSBpbiAxLi4zKSBjaC5zZW5kKGkpCiAgICAgICAgLy8gY2guY2xvc2UoKSAgICAgICAgICAgICAgICAgICAgLy8g4pyFIGLDoW8gImjhur90IGThu68gbGnhu4d1IgogICAgfQogICAgdmFsIGRvbmUgPSB3aXRoVGltZW91dE9yTnVsbCg1MDApIHsKICAgICAgICBmb3IgKHggaW4gY2gpIHByaW50bG4oIm5o4bqtbiAkeCIpIC8vIOKdjCBraMO0bmcgY2xvc2Ug4oaSIGZvciDEkeG7o2kgbcOjaQogICAgICAgICJ4b25nIgogICAgfQogICAgcHJpbnRsbihkb25lID86ICLinYwgdHJlbzogY2hhbm5lbCBjaMawYSDEkcOzbmciKQogICAgY29yb3V0aW5lQ29udGV4dC5jYW5jZWxDaGlsZHJlbigpCn0K)

### K288 · `trySend` từ callback thất bại với `Channel()` mặc định

`🟡 đã làm app` · `Kotlin` · `KMP`

**Gặp:** sự kiện từ callback GPS / nút bấm thỉnh thoảng bị mất. `trySend(...).isSuccess` trả `false`.

**Vì sao:** `Channel()` mặc định là **rendezvous**: không có chỗ chứa, `send` phải đợi tới khi có người nhận. `trySend`
(không treo, dùng được trong callback thường) thất bại nếu lúc đó chưa ai đang nhận.

**Sửa:** callback không được mất sự kiện thì `Channel(Channel.UNLIMITED)`. Chỉ cần giá trị mới nhất thì
`Channel(Channel.CONFLATED)` hoặc `MutableStateFlow`.

```kotlin
private val commands = Channel<Command>(Channel.UNLIMITED)   // ✅ trySend luôn thành công
fun start() { commands.trySend(Command.Start) }
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5jaGFubmVscy4qCgpmdW4gbWFpbigpIHsKICAgIHZhbCByZW5kZXp2b3VzID0gQ2hhbm5lbDxTdHJpbmc-KCkgICAgICAgICAgICAgICAgICAgLy8g4p2MIG3hurdjIMSR4buLbmgga2jDtG5nIGPDsyBjaOG7lyBjaOG7qWEKICAgIHByaW50bG4oInRyeVNlbmQgdsOgbyBDaGFubmVsKCk6ICR7cmVuZGV6dm91cy50cnlTZW5kKCJmaXggMSIpLmlzU3VjY2Vzc30iKSAgLy8gZmFsc2U6IGNoxrBhIGFpIG5o4bqtbgoKICAgIHZhbCB1bmxpbWl0ZWQgPSBDaGFubmVsPFN0cmluZz4oQ2hhbm5lbC5VTkxJTUlURUQpICAgLy8g4pyFIGNhbGxiYWNrIEdQUyAvIG7DunQgYuG6pW0gbHXDtG4gZ-G7rWkgxJHGsOG7o2MKICAgIHByaW50bG4oInRyeVNlbmQgdsOgbyBVTkxJTUlURUQ6ICR7dW5saW1pdGVkLnRyeVNlbmQoImZpeCAxIikuaXNTdWNjZXNzfSIpCn0K)

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/core/TripRecorder.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/core/TripRecorder.kt)

### K289 · Phần tử cũ vẫn nằm trong channel sau khi đã huỷ bên gửi

`🔴 khó, hiếm nhưng đau` · `KMP`

**Gặp:** bấm Tạm dừng rồi, vẫn có một điểm GPS được cộng vào quãng đường; sau khi tiếp tục, có đoạn thẳng nối từ chỗ cũ.

**Vì sao:** huỷ job gửi không xoá những gì **đã** nằm trong channel. Bên nhận vẫn đọc ra các phần tử gửi trước lúc huỷ.

**Sửa:** gắn "phiên" (segment, id lệnh) vào mỗi phần tử, bên nhận bỏ những phần tử không thuộc phiên hiện tại. Hoặc
tạo channel mới cho mỗi phiên.

```kotlin
is Command.Fix -> if (state is Recording && command.segment == segment) onFix(command.point)  // ✅ bỏ điểm cũ
```

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/core/TripRecorder.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/core/TripRecorder.kt) ·
Xem thêm: K288

### K290 · `job.join()` không ném lỗi của job

`🟡 đã làm app` · `Kotlin`

**Gặp:** `join()` xong, code chạy tiếp như thể upload thành công, dù bên trong đã lỗi.

**Vì sao:** `join()` chỉ **đợi** job kết thúc, dù kết thúc vì xong, vì huỷ hay vì lỗi. Chỉ `await()` của `Deferred` mới
ném lại exception.

**Sửa:** cần biết kết quả hay lỗi thì dùng `async` + `await()` (bọc `try/catch`), hoặc để hàm suspend trả `Result` và
gọi thẳng, không `launch`.

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgc3VwZXJ2aXNvclNjb3BlIHsKICAgICAgICB2YWwgam9iID0gbGF1bmNoKENvcm91dGluZUV4Y2VwdGlvbkhhbmRsZXIgeyBfLCBfIC0-IH0pIHsgZXJyb3IoInVwbG9hZCBs4buXaSIpIH0KICAgICAgICBqb2Iuam9pbigpICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgam9pbiBraMO0bmcgbsOpbTogY2jhu4kgxJHhu6NpIGpvYiBr4bq_dCB0aMO6YwogICAgICAgIHByaW50bG4oImpvaW4geG9uZywgaXNDYW5jZWxsZWQgPSAke2pvYi5pc0NhbmNlbGxlZH0g4oaSIHBo4bqjaSB04buxIGtp4buDbSB0cmEiKQoKICAgICAgICB2YWwgZGVmZXJyZWQgPSBhc3luYyB7IGVycm9yKCJ1cGxvYWQgbOG7l2kiKSB9CiAgICAgICAgdHJ5IHsgZGVmZXJyZWQuYXdhaXQoKSB9ICAgICAgICAgLy8g4pyFIGF3YWl0IG7DqW0gbOG6oWkgZXhjZXB0aW9uCiAgICAgICAgY2F0Y2ggKGU6IElsbGVnYWxTdGF0ZUV4Y2VwdGlvbikgeyBwcmludGxuKCJhd2FpdCBuw6ltOiAke2UubWVzc2FnZX0iKSB9CiAgICB9Cn0K)

### K291 · Swift gọi hàm suspend, Kotlin ném lỗi: app crash

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** Swift `try await core.load(id)` có `catch` hẳn hoi, nhưng app vẫn crash khi Kotlin ném exception (log có
`Uncaught Kotlin exception`).

**Vì sao:** hàm suspend xuất sang Swift chỉ chuyển `CancellationException` thành lỗi Swift. Exception khác không khai
báo trong `@Throws` thì Kotlin/Native coi là lỗi không xử lý và dừng app.

**Sửa:** thêm `@Throws(Exception::class)` (hoặc loại cụ thể) cho hàm Swift gọi, hoặc bắt lỗi trong Kotlin và trả
`null` / một state lỗi. Nhớ vẫn ném lại `CancellationException`.

```kotlin
suspend fun load(tripId: Long): TripDetail? = try {
    repository.loadTrip(tripId)?.let { TripDetail(it, repository.loadPoints(tripId)) }
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null                      // ✅ Swift nhận nil, không crash
}
```

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/core/TripLibrary.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/core/TripLibrary.kt)

### K292 · Lỗi nghiệp vụ kế thừa `CancellationException`: bị bỏ qua im lặng

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** ném `EmptyCartException` trong `launch` mà không có log, không crash, handler không được gọi. Coroutine cứ thế
dừng.

**Vì sao:** mọi exception là con của `CancellationException` được coroutine coi là "huỷ bình thường", không phải lỗi.
Hay gặp khi kế thừa nhầm (hoặc dùng lại `TimeoutCancellationException`) cho lỗi của mình.

**Sửa:** lỗi nghiệp vụ kế thừa `Exception` / `IllegalStateException`. Chỉ dùng `CancellationException` khi đúng là huỷ.

```kotlin
class EmptyCartException : CancellationException("giỏ hàng trống")  // ❌
class EmptyCartException : Exception("giỏ hàng trống")              // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpjbGFzcyBFbXB0eUNhcnRFeGNlcHRpb24gOiBDYW5jZWxsYXRpb25FeGNlcHRpb24oImdp4buPIGjDoG5nIHRy4buRbmciKSAgLy8g4p2MIGzhu5dpIG5naGnhu4dwIHbhu6Uga-G6vyB0aOG7q2EgQ2FuY2VsbGF0aW9uRXhjZXB0aW9uCi8vIGNsYXNzIEVtcHR5Q2FydEV4Y2VwdGlvbiA6IEV4Y2VwdGlvbigiZ2nhu48gaMOgbmcgdHLhu5FuZyIpICAgICAgICAgICAvLyDinIUKCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZyB7CiAgICB2YWwgc2NvcGUgPSBDb3JvdXRpbmVTY29wZShTdXBlcnZpc29ySm9iKCkgKyBDb3JvdXRpbmVFeGNlcHRpb25IYW5kbGVyIHsgXywgZSAtPiBwcmludGxuKCJiw6FvIGzhu5dpOiAke2UubWVzc2FnZX0iKSB9KQogICAgc2NvcGUubGF1bmNoIHsgdGhyb3cgRW1wdHlDYXJ0RXhjZXB0aW9uKCkgfS5qb2luKCkKICAgIHByaW50bG4oImtow7RuZyBjw7MgZMOybmcgJ2LDoW8gbOG7l2knIG7DoG86IGNvcm91dGluZSBi4buLIGNvaSBsw6AgaHXhu7cgYsOsbmggdGjGsOG7nW5nIikKfQo)

### K293 · `withTimeout` không ngắt được code chặn

`🟡 đã làm app` · `Kotlin`

**Gặp:** đặt `withTimeout(100)` mà vẫn đợi cả giây; timeout chỉ "có hiệu lực" sau khi việc chặn xong.

**Vì sao:** timeout cũng là một kiểu huỷ, chỉ có tác dụng ở điểm treo. `Thread.sleep`, socket đồng bộ, vòng lặp tính
toán không có điểm treo nên không bị ngắt giữa chừng.

**Sửa:** dùng API suspend thật (Ktor, `delay`), đặt timeout ở tầng thư viện (timeout của HTTP client, socket), hoặc
dùng `runInterruptible(Dispatchers.IO) { }` cho code Java chặn có hỗ trợ interrupt (chỉ JVM).

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW4udGltZS5tZWFzdXJlVGltZQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsKICAgIHZhbCB0ID0gbWVhc3VyZVRpbWUgewogICAgICAgIHZhbCByID0gd2l0aFRpbWVvdXRPck51bGwoMTAwKSB7CiAgICAgICAgICAgIFRocmVhZC5zbGVlcCgxXzAwMCkgICAgICAgICAgLy8g4p2MIGNvZGUgY2jhurduOiB0aW1lb3V0IGtow7RuZyBuZ-G6r3QgxJHGsOG7o2MKICAgICAgICAgICAgLy8gZGVsYXkoMV8wMDApICAgICAgICAgICAgICAvLyDinIUgY8OzIMSRaeG7g20gdHJlbyB0aMOsIGh14bu3IMSRw7puZyBnaeG7nQogICAgICAgICAgICAieG9uZyIKICAgICAgICB9CiAgICAgICAgcHJpbnRsbigia-G6v3QgcXXhuqM6ICRyIikKICAgIH0KICAgIHByaW50bG4oIm3huqV0ICR0ICjEkeG6t3QgdGltZW91dCAxMDAgbXMpIikKfQo) · Xem thêm: K262

### K294 · Dùng lại scope đã `cancel()`: `launch` không chạy, không báo lỗi

`🔴 khó, hiếm nhưng đau` · `Kotlin` · `KMP`

**Gặp:** đóng rồi mở lại màn hình, nút "Tải lại" không làm gì cả. Không exception, không log.

**Vì sao:** scope bị huỷ thì mọi `launch` mới trên nó bị huỷ ngay lúc tạo. `launch` vẫn trả `Job` bình thường nên không
ai để ý.

**Sửa:** mỗi vòng đời một scope mới: tạo object (và scope) mới khi màn hình mở lại, hoặc khi cần dùng lại thì tạo lại
scope sau `cancel()`. Huỷ con mà giữ scope thì dùng `scope.coroutineContext.cancelChildren()`.

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpjbGFzcyBDb3JlIHsKICAgIHByaXZhdGUgdmFyIHNjb3BlID0gQ29yb3V0aW5lU2NvcGUoU3VwZXJ2aXNvckpvYigpICsgRGlzcGF0Y2hlcnMuRGVmYXVsdCkKICAgIGZ1biByZWZyZXNoKCkgPSBzY29wZS5sYXVuY2ggeyBwcmludGxuKCJyZWZyZXNoIGNo4bqheSIpIH0KICAgIGZ1biBjbG9zZSgpID0gc2NvcGUuY2FuY2VsKCkKICAgIC8vIOKchSBj4bqnbiBkw7luZyBs4bqhaSBzYXUgY2xvc2UoKSB0aMOsIHThuqFvIHNjb3BlIG3hu5tpOiBzY29wZSA9IENvcm91dGluZVNjb3BlKFN1cGVydmlzb3JKb2IoKSArIERpc3BhdGNoZXJzLkRlZmF1bHQpCn0KCmZ1biBtYWluKCkgPSBydW5CbG9ja2luZyB7CiAgICB2YWwgY29yZSA9IENvcmUoKQogICAgY29yZS5yZWZyZXNoKCkuam9pbigpCiAgICBjb3JlLmNsb3NlKCkgICAgICAgICAgICAgICAgICAgICAgICAgLy8gbcOgbiBow6xuaCDEkcOzbmcKICAgIHZhbCBqb2IgPSBjb3JlLnJlZnJlc2goKSAgICAgICAgICAgICAvLyDinYwgbcOgbiBow6xuaCBt4bufIGzhuqFpLCBkw7luZyBzY29wZSDEkcOjIGh14bu3CiAgICBqb2Iuam9pbigpCiAgICBwcmludGxuKCJqb2IuaXNDYW5jZWxsZWQgPSAke2pvYi5pc0NhbmNlbGxlZH0g4oCUIHJlZnJlc2gga2jDtG5nIGNo4bqheSwga2jDtG5nIGLDoW8gbOG7l2kiKQp9Cg) · Xem thêm: K254, K295

### K295 · Scope tự tạo không có `SupervisorJob`: lỗi một lần, chết luôn

`🔴 khó, hiếm nhưng đau` · `Kotlin` · `KMP`

**Gặp:** lần tải đầu mất mạng báo lỗi, các lần bấm "Tải lại" sau đó không chạy nữa, dù mạng đã có lại.

**Vì sao:** `CoroutineScope(Dispatchers.Default)` tự thêm một `Job()` thường. Một coroutine con lỗi thì `Job` đó bị huỷ,
cả scope chết (giống K294), mọi `launch` sau không chạy.

**Sửa:** scope sống lâu (của core, của app) luôn tạo với `SupervisorJob()`: con lỗi không kéo scope chết theo.

```kotlin
val scope = CoroutineScope(Dispatchers.Default)                    // ❌
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)  // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIHNpbGVudCA9IENvcm91dGluZUV4Y2VwdGlvbkhhbmRsZXIgeyBfLCBlIC0-IHByaW50bG4oImzhu5dpOiAke2UubWVzc2FnZX0iKSB9CiAgICB2YWwgc2NvcGUgPSBDb3JvdXRpbmVTY29wZShEaXNwYXRjaGVycy5EZWZhdWx0ICsgc2lsZW50KSAgICAgICAgICAgICAgICAgIC8vIOKdjCBKb2IgdGjGsOG7nW5nICht4bq3YyDEkeG7i25oKQogICAgLy8gdmFsIHNjb3BlID0gQ29yb3V0aW5lU2NvcGUoU3VwZXJ2aXNvckpvYigpICsgRGlzcGF0Y2hlcnMuRGVmYXVsdCArIHNpbGVudCkgLy8g4pyFCgogICAgc2NvcGUubGF1bmNoIHsgZXJyb3IoInJlZnJlc2ggbOG6p24gMSBs4buXaSBt4bqhbmciKSB9LmpvaW4oKQogICAgdmFsIHNlY29uZCA9IHNjb3BlLmxhdW5jaCB7IHByaW50bG4oInJlZnJlc2ggbOG6p24gMiBjaOG6oXkiKSB9CiAgICBzZWNvbmQuam9pbigpCiAgICBwcmludGxuKCJzY29wZSBjw7JuIHPhu5FuZzogJHtzY29wZS5pc0FjdGl2ZX0sIGzhuqduIDIgYuG7iyBodeG7tzogJHtzZWNvbmQuaXNDYW5jZWxsZWR9IikKfQo) · Xem thêm: K265, K267

### K296 · `async(start = CoroutineStart.LAZY)` không ai `start`: scope treo

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** màn hình loading mãi, `coroutineScope { }` không trả về, không lỗi.

**Vì sao:** coroutine `LAZY` chỉ chạy khi có `start()`, `join()` hoặc `await()`. Nó vẫn là con của scope, và scope đợi
mọi con xong, kể cả con chưa bao giờ chạy.

**Sửa:** luôn `await()` / `start()` coroutine lazy, hoặc `cancel()` nó khi không cần. Phần lớn trường hợp không cần
`LAZY`, chỉ cần tạo `async` lúc cần.

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIHJlc3VsdCA9IHdpdGhUaW1lb3V0T3JOdWxsKDUwMCkgewogICAgICAgIGNvcm91dGluZVNjb3BlIHsKICAgICAgICAgICAgdmFsIGxhenlKb2IgPSBhc3luYyhzdGFydCA9IENvcm91dGluZVN0YXJ0LkxBWlkpIHsgcHJpbnRsbigidOG6o2kiKTsgMSB9CiAgICAgICAgICAgIHByaW50bG4oInF1w6puIGF3YWl0IC8gc3RhcnQiKSAgLy8g4p2MIExBWlkga2jDtG5nIHThu7EgY2jhuqF5IOKGkiBjb3JvdXRpbmVTY29wZSDEkeG7o2kgbcOjaQogICAgICAgICAgICAvLyBsYXp5Sm9iLnN0YXJ0KCkgICAgICAgICAgICAgLy8g4pyFIGhv4bq3YyBhd2FpdCgpCiAgICAgICAgICAgICJ4b25nIgogICAgICAgIH0KICAgIH0KICAgIHByaW50bG4ocmVzdWx0ID86ICLinYwgdHJlbyB2w6wgYXN5bmMgTEFaWSBjaMawYSDEkcaw4bujYyBzdGFydCIpCn0K)

### K297 · `IllegalStateException: Already resumed` khi bọc callback

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `java.lang.IllegalStateException: Already resumed, but proposed with update ...` khi đổi API callback sang
hàm suspend bằng `suspendCancellableCoroutine`.

**Vì sao:** một continuation chỉ được `resume` **một lần**. API cũ gọi callback hai lần (cache rồi mạng, progress rồi
done, lỗi rồi complete) là lần thứ hai crash.

**Sửa:** chỉ resume khi `cont.isActive`, hoặc gỡ listener ngay sau lần đầu. API gọi callback nhiều lần thì nó là một
dòng dữ liệu: dùng `callbackFlow` thay vì hàm suspend.

```kotlin
legacyLoad { v -> if (cont.isActive) cont.resume(v) }   // ✅ chỉ nhận kết quả đầu
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW4uY29yb3V0aW5lcy5yZXN1bWUKCmZ1biBpbnRlcmZhY2UgQ2FsbGJhY2sgeyBmdW4gb25SZXN1bHQodjogU3RyaW5nKSB9CmZ1biBsZWdhY3lMb2FkKGNiOiBDYWxsYmFjaykgeyBjYi5vblJlc3VsdCgidOG7qyBjYWNoZSIpOyBjYi5vblJlc3VsdCgidOG7qyBt4bqhbmciKSB9ICAvLyBn4buNaSBjYWxsYmFjayAyIGzhuqduCgpzdXNwZW5kIGZ1biBsb2FkKCk6IFN0cmluZyA9IHN1c3BlbmRDYW5jZWxsYWJsZUNvcm91dGluZSB7IGNvbnQgLT4KICAgIGxlZ2FjeUxvYWQgeyB2IC0-IGNvbnQucmVzdW1lKHYpIH0gICAgICAgICAgIC8vIOKdjCBs4bqnbiByZXN1bWUgdGjhu6kgaGFpIGNyYXNoCiAgICAvLyBsZWdhY3lMb2FkIHsgdiAtPiBpZiAoY29udC5pc0FjdGl2ZSkgY29udC5yZXN1bWUodikgfSAvLyDinIUgY2jhu4kgbmjhuq1uIGvhur90IHF14bqjIMSR4bqndQp9CgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgeyBwcmludGxuKGxvYWQoKSkgfQo) · Xem thêm: K298

### K298 · Bọc callback mà quên `invokeOnCancellation`: cảm biến không tắt

`🟡 đã làm app` · `Kotlin` · `Android` · `iOS`

**Gặp:** rời màn hình trước khi có kết quả, nhưng GPS / Bluetooth / listener vẫn bật, pin tụt.

**Vì sao:** coroutine bị huỷ khi đang đợi callback. Không có `invokeOnCancellation`, không ai gỡ listener, và API bên
dưới vẫn chạy.

**Sửa:** trong `suspendCancellableCoroutine` luôn đăng ký `cont.invokeOnCancellation { stop() }`. Với `callbackFlow` thì
phải kết thúc bằng `awaitClose { removeListener() }`.

```kotlin
suspend fun firstReading(sensor: Sensor): Int = suspendCancellableCoroutine { cont ->
    sensor.start { v -> sensor.stop(); cont.resume(v) }
    cont.invokeOnCancellation { sensor.stop() }   // ✅
}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW4uY29yb3V0aW5lcy5yZXN1bWUKCmNsYXNzIFNlbnNvciB7CiAgICB2YXIgbGlzdGVuZXI6ICgoSW50KSAtPiBVbml0KT8gPSBudWxsCiAgICBmdW4gc3RhcnQobDogKEludCkgLT4gVW5pdCkgeyBsaXN0ZW5lciA9IGw7IHByaW50bG4oImPhuqNtIGJp4bq_biBC4bqsVCIpIH0KICAgIGZ1biBzdG9wKCkgeyBsaXN0ZW5lciA9IG51bGw7IHByaW50bG4oImPhuqNtIGJp4bq_biBU4bquVCIpIH0KfQoKc3VzcGVuZCBmdW4gZmlyc3RSZWFkaW5nKHNlbnNvcjogU2Vuc29yKTogSW50ID0gc3VzcGVuZENhbmNlbGxhYmxlQ29yb3V0aW5lIHsgY29udCAtPgogICAgc2Vuc29yLnN0YXJ0IHsgdiAtPiBzZW5zb3Iuc3RvcCgpOyBjb250LnJlc3VtZSh2KSB9CiAgICBjb250Lmludm9rZU9uQ2FuY2VsbGF0aW9uIHsgc2Vuc29yLnN0b3AoKSB9ICAvLyDinIUgeG_DoSBkw7JuZyBuw6B5IMSR4buDIHRo4bqleSBj4bqjbSBiaeG6v24ga2jDtG5nIGJhbyBnaeG7nSB04bqvdAp9CgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIHNlbnNvciA9IFNlbnNvcigpCiAgICB2YWwgam9iID0gbGF1bmNoIHsgZmlyc3RSZWFkaW5nKHNlbnNvcikgfSAgICAvLyBjaMawYSBjw7Mgc-G7kSDEkW8gbsOgbyB0aMOsIG5nxrDhu51pIGTDuW5nIHRob8OhdAogICAgZGVsYXkoMTAwKQogICAgam9iLmNhbmNlbEFuZEpvaW4oKQogICAgcHJpbnRsbigibGlzdGVuZXIgY8OybiBnaeG7rzogJHtzZW5zb3IubGlzdGVuZXIgIT0gbnVsbH0iKQp9Cg) · Xem thêm: K297

### K299 · `runBlocking` bên trong hàm suspend

`🟡 đã làm app` · `Kotlin`

**Gặp:** 3 việc chạy "song song" trên main thread mất ~900 ms thay vì ~300 ms; UI đứng mỗi lần một việc chạy.

**Vì sao:** `runBlocking` chặn thread đang chạy coroutine hiện tại. Trên dispatcher một thread (như main), các coroutine
khác phải đợi. Hay gặp khi gọi một hàm cũ có `runBlocking` bên trong từ code coroutine.

**Sửa:** đã ở trong hàm suspend thì gọi thẳng hàm suspend. Hàm cũ có `runBlocking` thì sửa nó thành `suspend`.

```kotlin
suspend fun loadItem() { runBlocking { delay(300) } }   // ❌
suspend fun loadItem() { delay(300) }                   // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW4udGltZS5tZWFzdXJlVGltZQoKLy8gR2nhuqMgbOG6rXAgbWFpbiB0aHJlYWQgY-G7p2EgYXBwOiBjaOG7iSBt4buZdCB0aHJlYWQKdmFsIG1haW4gPSBEaXNwYXRjaGVycy5EZWZhdWx0LmxpbWl0ZWRQYXJhbGxlbGlzbSgxKQoKc3VzcGVuZCBmdW4gbG9hZEl0ZW0oKSB7CiAgICBydW5CbG9ja2luZyB7IGRlbGF5KDMwMCkgfSAgICAgICAgICAgLy8g4p2MIGNo4bq3biBsdcO0biAibWFpbiB0aHJlYWQiIHRyb25nIDMwMCBtcwogICAgLy8gZGVsYXkoMzAwKSAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKchSDEkcOjIOG7nyB0cm9uZyBzdXNwZW5kIHRow6wgZ-G7jWkgdGjhurNuZwp9CgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIHQgPSBtZWFzdXJlVGltZSB7IHdpdGhDb250ZXh0KG1haW4pIHsgcmVwZWF0KDMpIHsgbGF1bmNoIHsgbG9hZEl0ZW0oKSB9IH0gfSB9CiAgICBwcmludGxuKCIzIHZp4buHYyBt4bqldCAkdCIpCn0K) · Xem thêm: K255

### K300 · Sau điểm treo, coroutine có thể chạy tiếp trên thread khác

`🔴 khó, hiếm nhưng đau` · `Kotlin` · `iOS`

**Gặp:** giá trị `ThreadLocal` (MDC log, transaction) mất sau `delay` / `withContext`; trên iOS `CLLocationManager` tạo
trong coroutine không bao giờ gọi delegate.

**Vì sao:** với `Dispatchers.Default` / `IO`, sau mỗi lần treo coroutine có thể được tiếp tục trên **bất kỳ** thread nào
của pool. Object gắn với một thread (ThreadLocal, API cần run loop của main thread) không đi theo.

**Sửa:** truyền dữ liệu qua tham số hoặc `CoroutineContext` (trên JVM: `threadLocal.asContextElement()`). API cần đúng
một thread thì chạy trên dispatcher đó: `withContext(Dispatchers.Main)` hoặc `flowOn(Dispatchers.Main)`.

```kotlin
callbackFlow { /* tạo CLLocationManager, đăng ký delegate */ }
    .flowOn(Dispatchers.Main)   // ✅ delegate cần run loop của main thread
```

**Gặp ở:** [`TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt) ·
Xem thêm: K279
