# 17. Hiệu năng & chạy ngầm (K801–K850)

Hai loại lỗi khó thấy khi test trên bàn: app **chậm hoặc giật** vì làm việc sai thread, sai cấu trúc dữ liệu, và app
**chết, im lặng hoặc hao pin** khi màn hình tắt. Phần sau (K827 trở đi) đổi theo từng bản Android và iOS: đọc kỹ dòng
"Android 12+ / 14+" trước khi sửa.

### K801 · App treo vài giây rồi hiện "App isn't responding" (ANR)

`🟢 mới học` · `Android`

**Gặp:** hộp thoại *"<App> isn't responding"*, Logcat có `ANR in dev.xxx` và `Input dispatching timed out`.

**Vì sao:** main thread vừa vẽ UI vừa nhận chạm. Bị chặn khoảng 5 giây (đọc file lớn, query DB, gọi mạng đồng bộ,
`Thread.sleep`, vòng lặp nặng) thì hệ thống coi app là treo.

**Sửa:** việc nào có thể lâu thì đưa khỏi main thread bằng `withContext(Dispatchers.IO)` (đĩa, mạng) hoặc
`Dispatchers.Default` (tính toán). Main thread chỉ đọc kết quả và cập nhật state.

```kotlin
fun load() = viewModelScope.launch {
    val notes = repository.readAllBlocking()                         // ❌ chặn main
    val notes = withContext(Dispatchers.IO) { repository.readAllBlocking() } // ✅
    _state.value = notes
}
```

Xem thêm: K802, K805

### K802 · Logcat: "Skipped 63 frames! The application may be doing too much work on its main thread."

`🟢 mới học` · `Android`

**Gặp:** cuộn danh sách bị khựng, animation giật; Logcat có dòng `Choreographer: Skipped NN frames!`.

**Vì sao:** mỗi khung hình chỉ có khoảng 16 ms (60 Hz) hoặc 8 ms (120 Hz). Việc gì trên main thread dài hơn thế là rớt
khung. Đây là "ANR nhỏ": chưa đủ 5 giây nhưng người dùng đã thấy.

**Sửa:** tìm việc nặng bằng *Profiler → CPU* hoặc `Debug.startMethodTracing`. Thường gặp: parse JSON, sort list lớn,
decode ảnh, đọc `SharedPreferences` lần đầu. Đưa ra `Dispatchers.Default`/`IO`, và đo lại trên bản release (K814).

### K803 · Không biết code nào đang đọc đĩa trên main thread

`🟡 đã làm app` · `Android`

**Gặp:** app hơi giật lúc mở màn hình mà không tìm ra chỗ nào; máy yếu thì ANR thỉnh thoảng.

**Vì sao:** đọc đĩa nhỏ thường nhanh trên máy dev, nên không ai thấy. Trên máy rẻ hoặc khi bộ nhớ đầy, cùng lời gọi đó
mất hàng trăm ms.

**Sửa:** bật `StrictMode` ở bản debug. Mỗi lần vi phạm, Logcat in `StrictMode policy violation` kèm
`android.os.strictmode.DiskReadViolation` / `DiskWriteViolation` và stack trace chỉ đúng dòng.

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
        }
    }
}
```

**Gặp ở:** [`apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt) (mọi lời gọi SQLite đi qua `io { }`)

### K804 · Chọn sai `Dispatchers.IO` / `Dispatchers.Default`

`🟡 đã làm app` · `Kotlin`

**Gặp:** tính toán nặng trên `IO` làm CPU nóng và các việc khác chậm theo; hoặc đọc file trên `Default` làm mọi việc
tính toán khác phải xếp hàng.

**Vì sao:** `Default` có số thread bằng số nhân CPU, hợp cho việc **tính**. `IO` có nhiều thread hơn (mặc định tới 64),
hợp cho việc **chờ** (đĩa, mạng, SQLite). Chặn thread của `Default` bằng I/O là chiếm chỗ của việc tính.

**Sửa:** chờ thì `IO`, tính thì `Default`. Thư viện đã là `suspend` thật (Ktor, Room/SQLDelight với coroutines) thì
không cần bọc thêm.

```kotlin
val rows = withContext(Dispatchers.IO) { db.selectAll() }          // chờ đĩa
val stats = withContext(Dispatchers.Default) { computeStats(rows) } // tính
```

### K805 · Tưởng `launch { }` là chạy nền

`🟢 mới học` · `Kotlin`

**Gặp:** đã bọc việc nặng trong `viewModelScope.launch { }` mà UI vẫn giật.

**Vì sao:** coroutine không tự đổi thread. `launch` chạy trên dispatcher của scope, mà `viewModelScope` và
`lifecycleScope` dùng `Dispatchers.Main`. Việc nặng trong đó vẫn chặn main thread.

**Sửa:** bọc đúng đoạn nặng bằng `withContext(Dispatchers.Default)` (hoặc `IO`). Tốt hơn nữa: hàm làm việc nặng tự đổi
dispatcher bên trong, để người gọi khỏi phải nhớ.

```kotlin
viewModelScope.launch { heavy() }                                     // ❌ vẫn trên Main
viewModelScope.launch { withContext(Dispatchers.Default) { heavy() } } // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgovLyBC4bqreTogbGF1bmNoIGtow7RuZyB04buxIMSRxrBhIHZp4buHYyByYSB0aHJlYWQga2jDoWMuIE7DsyBjaOG6oXkgdHLDqm4gZGlzcGF0Y2hlciBj4bunYSBzY29wZS4KZnVuIGhlYXZ5KCk6IExvbmcgewogICAgdmFyIHN1bSA9IDBMCiAgICBmb3IgKGkgaW4gMS4uNTBfMDAwXzAwMCkgc3VtICs9IGkKICAgIHJldHVybiBzdW0KfQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsKICAgIHByaW50bG4oIm1haW4gY2jhuqF5IHRyw6puOiAke1RocmVhZC5jdXJyZW50VGhyZWFkKCkubmFtZX0iKQogICAgbGF1bmNoIHsKICAgICAgICAvLyDinYwgduG6q24gbMOgIHRocmVhZCBj4bunYSBydW5CbG9ja2luZyAodHLDqm4gQW5kcm9pZDogRGlzcGF0Y2hlcnMuTWFpbiDihpIgZ2nhuq10L0FOUikKICAgICAgICBwcmludGxuKCJsYXVuY2gge30gY2jhuqF5IHRyw6puOiAke1RocmVhZC5jdXJyZW50VGhyZWFkKCkubmFtZX0iKQogICAgICAgIGhlYXZ5KCkKICAgIH0uam9pbigpCiAgICB2YWwgcmVzdWx0ID0gd2l0aENvbnRleHQoRGlzcGF0Y2hlcnMuRGVmYXVsdCkgewogICAgICAgIC8vIOKchSB2aeG7h2MgbuG6t25nIGNodXnhu4NuIHNhbmcgdGhyZWFkIG7hu4FuCiAgICAgICAgcHJpbnRsbigid2l0aENvbnRleHQoRGVmYXVsdCkgY2jhuqF5IHRyw6puOiAke1RocmVhZC5jdXJyZW50VGhyZWFkKCkubmFtZX0iKQogICAgICAgIGhlYXZ5KCkKICAgIH0KICAgIHByaW50bG4oImvhur90IHF14bqjOiAkcmVzdWx0IikKfQo) · Xem thêm: K801

### K806 · iOS: hàm `suspend` gọi từ Swift làm UI đứng

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** trên Android mượt, trên iOS bấm nút thì UI đứng một lúc rồi mới chạy tiếp, dù Swift đã gọi bằng `await`.

**Vì sao:** `await` của Swift không đưa việc sang thread khác. Hàm `suspend` của Kotlin bắt đầu chạy trên thread gọi
nó, thường là main thread. Nếu bên trong không có `withContext`, mọi việc tính toán chạy ngay trên main.

**Sửa:** để code chung tự chọn thread: hàm `suspend` nào nặng thì bọc `withContext(Dispatchers.Default)` hoặc
`Dispatchers.IO` (có trên Kotlin/Native từ coroutines 1.7) trong `commonMain`. Hai nền tảng cùng được lợi.

```kotlin
// commonMain
suspend fun buildReport(trips: List<Trip>): Report =
    withContext(Dispatchers.Default) { /* tính nặng */ }   // ✅ Swift gọi từ main vẫn không đứng UI
```

**Gặp ở:** [`apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt) (comment `TRAP: SQLite calls block the thread`)

### K807 · Nối chuỗi bằng `+=` trong vòng lặp

`🟢 mới học` · `Kotlin`

**Gặp:** xuất CSV/GPX vài nghìn dòng mất vài giây, càng nhiều dòng càng chậm hơn hẳn (không tăng đều).

**Vì sao:** `String` không đổi được. Mỗi `+=` tạo chuỗi mới và chép lại toàn bộ phần cũ, nên tổng công việc tăng theo
bình phương số dòng.

**Sửa:** dùng `buildString { append(...) }` (một `StringBuilder`), hoặc `joinToString` khi nối một list.

```kotlin
var csv = ""
for (t in trips) csv += "${t.id},${t.distance}\n"   // ❌ O(n²)
val csv = buildString {
    for (t in trips) append(t.id).append(',').append(t.distance).append('\n') // ✅
}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbi50aW1lLm1lYXN1cmVUaW1lCgovLyBC4bqreTogKz0gdHLDqm4gU3RyaW5nIHRyb25nIHbDsm5nIGzhurdwIHThuqFvIGNodeG7l2kgbeG7m2kgbeG7l2kgbOG6p24sIGNow6lwIGzhuqFpIHRvw6BuIGLhu5kgcGjhuqduIMSRw6MgY8OzLgpmdW4gbWFpbigpIHsKICAgIHZhbCBuID0gMzBfMDAwCiAgICB2YXIgc2xvdyA9ICIiCiAgICB2YWwgdDEgPSBtZWFzdXJlVGltZSB7CiAgICAgICAgZm9yIChpIGluIDAgdW50aWwgbikgc2xvdyArPSAieCIgICAgICAgICAgLy8g4p2MIE8obsKyKTogbeG7l2kgdsOybmcgY2jDqXAgbOG6oWkgY-G6oyBjaHXhu5dpCiAgICB9CiAgICB2YWwgdDIgPSBtZWFzdXJlVGltZSB7CiAgICAgICAgdmFsIGZhc3QgPSBidWlsZFN0cmluZyB7ICAgICAgICAgICAgICAgICAgLy8g4pyFIG3hu5l0IGLhu5kgxJHhu4dtLCBu4buRaSB2w6BvIGN14buRaQogICAgICAgICAgICBmb3IgKGkgaW4gMCB1bnRpbCBuKSBhcHBlbmQoIngiKQogICAgICAgIH0KICAgICAgICBjaGVjayhmYXN0ID09IHNsb3cpCiAgICB9CiAgICBwcmludGxuKCIrPSB0cm9uZyB2w7JuZyBs4bq3cDogJHQxIikKICAgIHByaW50bG4oImJ1aWxkU3RyaW5nOiAgICAgICAkdDIiKQp9Cg)

### K808 · `Array<Int>` / `List<Int>` cho mảng số lớn

`🟡 đã làm app` · `Kotlin`

**Gặp:** xử lý vài triệu mẫu (âm thanh, cảm biến, pixel) chậm và tốn RAM hơn dự kiến; Profiler thấy rất nhiều
`java.lang.Integer`.

**Vì sao:** `Array<Int>`, `List<Int>`, `Int?` đều chứa object `Integer` (boxing): mỗi số là một object riêng trên heap.
`IntArray` chứa `int` thật, nằm liền nhau. `IntArray.map { }` trả về `List<Int>` nên lại box.

**Sửa:** mảng số lớn dùng `IntArray`, `LongArray`, `DoubleArray`; tạo bằng `IntArray(size) { … }`. List nhỏ vài chục
phần tử thì không cần bận tâm.

```kotlin
val samples: Array<Int> = Array(1_000_000) { 0 }   // ❌ một triệu object Integer
val samples = IntArray(1_000_000)                  // ✅ một mảng int
```

[▶ Chạy thử](../playground/index.html#code=Ly8gQuG6q3k6IEFycmF5PEludD4gdsOgIExpc3Q8SW50PiBjaOG7qWEgb2JqZWN0IEludGVnZXIgKGJveGluZyksIEludEFycmF5IGNo4bupYSBpbnQgdGjhuq10LgpmdW4gbWFpbigpIHsKICAgIHZhbCBib3hlZDogQXJyYXk8SW50PiA9IGFycmF5T2YoMSwgMiwgMykKICAgIHZhbCBwcmltaXRpdmU6IEludEFycmF5ID0gaW50QXJyYXlPZigxLCAyLCAzKQogICAgcHJpbnRsbigiQXJyYXk8SW50PiB0csOqbiBKVk0gbMOgOiAke2JveGVkLmphdmFDbGFzcy5uYW1lfSIpICAgICAvLyBbTGphdmEubGFuZy5JbnRlZ2VyOwogICAgcHJpbnRsbigiSW50QXJyYXkgdHLDqm4gSlZNIGzDoDogICAke3ByaW1pdGl2ZS5qYXZhQ2xhc3MubmFtZX0iKSAvLyBbSQoKICAgIC8vIG1hcCB0csOqbiBJbnRBcnJheSB0cuG6oyB24buBIExpc3Q8SW50PiAoQXJyYXlMaXN0IGNo4bupYSBJbnRlZ2VyKSDihpIgYm94IHThu6tuZyBwaOG6p24gdOG7rQogICAgdmFsIGRvdWJsZWQgPSBwcmltaXRpdmUubWFwIHsgaXQgKiAyIH0KICAgIHByaW50bG4oIkludEFycmF5Lm1hcCB0cuG6oyB24buBOiAgICAke2RvdWJsZWQuamF2YUNsYXNzLnNpbXBsZU5hbWV9IikKICAgIC8vIOKchSBnaeG7ryBraeG7g3Ugbmd1ecOqbiB0aHXhu7cga2hpIGPhuqduIG3huqNuZyBz4buRIGzhu5tuCiAgICB2YWwgZG91YmxlZFByaW1pdGl2ZSA9IEludEFycmF5KHByaW1pdGl2ZS5zaXplKSB7IHByaW1pdGl2ZVtpdF0gKiAyIH0KICAgIHByaW50bG4oIkludEFycmF5KHNpemUpIHsgfTogICAgICR7ZG91YmxlZFByaW1pdGl2ZS5qYXZhQ2xhc3MubmFtZX0iKQp9Cg)

### K809 · Chuỗi `map`/`filter` dài trên list lớn

`🟡 đã làm app` · `Kotlin`

**Gặp:** `list.map { }.filter { }.first()` trên list lớn chạy lâu dù chỉ cần một kết quả.

**Vì sao:** mỗi bước trên `List` chạy cho **mọi** phần tử và tạo một list trung gian, rồi bước sau mới bắt đầu.
`first()` ở cuối không làm các bước trước ngắn lại.

**Sửa:** thêm `asSequence()`: từng phần tử đi hết chuỗi rồi mới tới phần tử sau, và dừng ngay khi `first()` có kết quả.

```kotlin
val hit = numbers.map { it * it }.filter { it % 7 == 0 }.first()          // 2000 lần gọi lambda
val hit = numbers.asSequence().map { it * it }.filter { it % 7 == 0 }.first() // 14 lần
```

[▶ Chạy thử](../playground/index.html#code=Ly8gQuG6q3k6IGNodeG7l2kgbWFwL2ZpbHRlciB0csOqbiBMaXN0IGNo4bqheSBo4bq_dCB04burbmcgYsaw4bubYyBjaG8gTeG7jEkgcGjhuqduIHThu60sIHThuqFvIGxpc3QgdHJ1bmcgZ2lhbi4KZnVuIG1haW4oKSB7CiAgICB2YWwgbnVtYmVycyA9ICgxLi4xXzAwMCkudG9MaXN0KCkKCiAgICB2YXIgbGlzdENhbGxzID0gMAogICAgdmFsIGEgPSBudW1iZXJzCiAgICAgICAgLm1hcCB7IGxpc3RDYWxscysrOyBpdCAqIGl0IH0gICAgICAvLyBjaOG6oXkgMTAwMCBs4bqnbiwgdOG6oW8gbGlzdCAxMDAwIHBo4bqnbiB04butCiAgICAgICAgLmZpbHRlciB7IGxpc3RDYWxscysrOyBpdCAlIDcgPT0gMCB9CiAgICAgICAgLmZpcnN0KCkKICAgIHByaW50bG4oIkxpc3Q6ICAgICBr4bq_dCBxdeG6oyAkYSBzYXUgJGxpc3RDYWxscyBs4bqnbiBn4buNaSBsYW1iZGEiKQoKICAgIHZhciBzZXFDYWxscyA9IDAKICAgIHZhbCBiID0gbnVtYmVycy5hc1NlcXVlbmNlKCkKICAgICAgICAubWFwIHsgc2VxQ2FsbHMrKzsgaXQgKiBpdCB9ICAgICAgIC8vIHThu6tuZyBwaOG6p24gdOG7rSDEkWkgaOG6v3QgY2h14buXaSwgZOG7q25nIGtoaSBjw7Mga-G6v3QgcXXhuqMKICAgICAgICAuZmlsdGVyIHsgc2VxQ2FsbHMrKzsgaXQgJSA3ID09IDAgfQogICAgICAgIC5maXJzdCgpCiAgICBwcmludGxuKCJTZXF1ZW5jZToga-G6v3QgcXXhuqMgJGIgc2F1ICRzZXFDYWxscyBs4bqnbiBn4buNaSBsYW1iZGEiKQp9Cg) · Xem thêm: K810, K811

### K810 · Thêm `asSequence()` ở mọi nơi "cho nhanh"

`🟢 mới học` · `Kotlin`

**Gặp:** đổi hết sang `Sequence` mà app không nhanh hơn, có chỗ còn chậm hơn.

**Vì sao:** `Sequence` có chi phí riêng cho mỗi phần tử (iterator, lambda bọc lớp). Với list nhỏ, hoặc chuỗi chỉ một
bước, `List` thường nhanh hơn. `Sequence` lợi khi list lớn, nhiều bước, hoặc có thao tác dừng sớm (`first`, `take`).

**Sửa:** mặc định dùng `List`. Chỉ đổi sang `Sequence` khi đo thấy chậm và chuỗi đúng kiểu "nhiều bước + list lớn +
dừng sớm".

### K811 · Viết chuỗi `Sequence` mà không có gì chạy

`🟢 mới học` · `Kotlin`

**Gặp:** `asSequence().map { println(it) }` không in gì, `onEach { save(it) }` không lưu gì.

**Vì sao:** `Sequence` lười. `map`, `filter`, `onEach` chỉ mô tả việc cần làm; phải có thao tác cuối (`toList`, `first`,
`count`, `forEach`, `sum`…) thì chuỗi mới chạy.

**Sửa:** kết thúc bằng một thao tác cuối. Nếu chỉ cần tác dụng phụ thì dùng `forEach`, không dùng `map`/`onEach`.

```kotlin
items.asSequence().map { save(it) }          // ❌ không lưu gì
items.asSequence().forEach { save(it) }      // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gQuG6q3k6IFNlcXVlbmNlIGzGsOG7nWkuIEtow7RuZyBjw7MgdGhhbyB0w6FjIGN14buRaSAodG9MaXN0LCBmaXJzdCwgY291bnQsIGZvckVhY2jigKYpIHRow6wga2jDtG5nIGNo4bqheSBnw6wuCmZ1biBtYWluKCkgewogICAgdmFsIG5hbWVzID0gbGlzdE9mKCJhbiIsICJiaW5oIiwgImNoaSIpCgogICAgbmFtZXMuYXNTZXF1ZW5jZSgpLm1hcCB7IHByaW50bG4oIuKdjCBraMO0bmcgYmFvIGdp4budIGluOiAkaXQiKTsgaXQudXBwZXJjYXNlKCkgfQoKICAgIHZhbCB1cHBlciA9IG5hbWVzLmFzU2VxdWVuY2UoKQogICAgICAgIC5tYXAgeyBwcmludGxuKCLinIUgeOG7rSBsw70gJGl0Iik7IGl0LnVwcGVyY2FzZSgpIH0KICAgICAgICAudG9MaXN0KCkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIHRoYW8gdMOhYyBjdeG7kWkga8OtY2ggaG_huqF0IGPhuqMgY2h14buXaQogICAgcHJpbnRsbih1cHBlcikKfQo)

### K812 · Tạo `Regex`, formatter, object nặng trong vòng lặp

`🟡 đã làm app` · `Kotlin`

**Gặp:** parse vài nghìn dòng log hay validate từng ký tự gõ vào mà CPU tăng vọt; Profiler thấy nhiều thời gian trong
`Pattern.compile` hoặc `DateTimeFormatter.ofPattern`.

**Vì sao:** `"…".toRegex()` và `Regex("…")` biên dịch biểu thức mỗi lần gọi. Đặt trong vòng lặp hoặc trong hàm gọi
mỗi lần recompose là biên dịch lại hàng nghìn lần.

**Sửa:** tạo một lần, để ở `private val` cấp file hoặc trong `companion object`.

```kotlin
fun isPlate(s: String) = Regex("""\d{2}[A-Z]-\d{4,5}""").matches(s)   // ❌ biên dịch mỗi lần
private val PLATE = Regex("""\d{2}[A-Z]-\d{4,5}""")
fun isPlate(s: String) = PLATE.matches(s)                             // ✅
```

### K813 · Đo thời gian bằng `System.currentTimeMillis()` và chỉ chạy một lần

`🟡 đã làm app` · `Kotlin`

**Gặp:** cùng một hàm đo lần này 120 ms, lần sau 8 ms; kết luận "cách A nhanh hơn B" rồi đổi lại ngược.

**Vì sao:** đồng hồ hệ thống có thể bị chỉnh (NTP, người dùng đổi giờ). Trên JVM/ART, lần chạy đầu còn chậm vì class
loading và JIT chưa tối ưu. Một lần đo không nói lên gì.

**Sửa:** đo bằng `measureTime { }` / `TimeSource.Monotonic` (đồng hồ không lùi), chạy nhiều lần, bỏ vài lần đầu. Đo hiệu
năng thật trên Android thì dùng thư viện Jetpack Microbenchmark / Macrobenchmark.

```kotlin
val start = System.currentTimeMillis(); work(); println(System.currentTimeMillis() - start) // ❌
val took = measureTime { work() }; println(took)                                           // ✅
```

### K814 · Kết luận "app chậm" từ bản debug

`🟡 đã làm app` · `Android` · `Compose`

**Gặp:** cuộn `LazyColumn` giật trên bản chạy từ Android Studio, nhưng người dùng bản release không thấy; hoặc tối ưu
mãi một chỗ mà release vốn đã nhanh.

**Vì sao:** bản debug là `debuggable`: ART tối ưu ít hơn, không có R8, Compose bật thêm các công cụ hỗ trợ debug. Đo trên
debug có thể chậm hơn release nhiều lần.

**Sửa:** đo trên build `release` (hoặc một build type riêng `benchmark` kế thừa release, ký bằng debug key, thêm
`<profileable android:shell="true" />`). Chỉ tối ưu chỗ mà bản đó vẫn chậm.

### K815 · Lần mở đầu tiên sau khi cài giật hơn hẳn các lần sau

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** người dùng mới cài thấy app khởi động chậm và cuộn giật vài phút đầu; sau đó tự hết.

**Vì sao:** ART lúc đầu chạy code ở chế độ thông dịch/JIT, vài ngày sau mới biên dịch trước (AOT) những đoạn hay dùng.
Không có **Baseline Profile**, mọi người dùng đều trải qua giai đoạn chậm đó.

**Sửa:** tạo Baseline Profile bằng module Macrobenchmark (`BaselineProfileRule`), kèm thư viện
`androidx.profileinstaller` để profile được cài cùng app. Thư viện Compose đã có profile riêng; profile của app phủ
thêm luồng mở app và màn hình chính.

### K816 · `Application.onCreate` làm quá nhiều việc

`🟡 đã làm app` · `Android`

**Gặp:** màn trắng lâu khi mở app; *Vitals* trên Play Console báo khởi động lạnh chậm.

**Vì sao:** mọi thứ trong `Application.onCreate` chạy trên main thread **trước** màn hình đầu tiên: khởi tạo SDK,
mở DB, đọc config, tải dữ liệu.

**Sửa:** trong `onCreate` chỉ giữ việc rẻ và bắt buộc (Koin `startKoin` chỉ khai báo module, chưa tạo object). Mở DB,
SDK phân tích, tải dữ liệu thì để lười (`by lazy`, Koin `single` tạo khi dùng lần đầu) hoặc chạy nền sau khi màn hình
đầu đã vẽ.

```kotlin
override fun onCreate() {
    super.onCreate()
    initKoin { androidContext(this@App); modules(appModule) } // ✅ rẻ: object chỉ tạo khi cần
    // database.warmUp()   ❌ chặn main trước màn đầu tiên
}
```

### K817 · Dựng chuỗi log dù log đã tắt

`🟡 đã làm app` · `Kotlin`

**Gặp:** Profiler bản release thấy thời gian trong `toString()` của object lớn, dù log debug đã tắt.

**Vì sao:** đối số của hàm được tính **trước** khi gọi. `log("state = $bigState")` dựng xong chuỗi rồi hàm `log` mới
kiểm "có in không".

**Sửa:** dùng API log nhận lambda (Kermit `Logger.d { "…" }`, hoặc tự viết `inline fun logD(msg: () -> String)`).
Lambda chỉ chạy khi log thật sự được in.

```kotlin
logger.d("state = ${state.dump()}")     // ❌ dump() chạy dù không in
logger.d { "state = ${state.dump()}" }  // ✅ chỉ chạy khi level debug bật
```

[▶ Chạy thử](../playground/index.html#code=Ly8gQuG6q3k6IGNodeG7l2kgbG9nIMSRxrDhu6NjIGThu7FuZyB4b25nIHLhu5NpIG3hu5tpIGjhu49pICJjw7MgaW4ga2jDtG5nIi4gQuG6o24gcmVsZWFzZSB24bqrbiB0cuG6oyBnacOhLgp2YXIgZGVidWdFbmFibGVkID0gZmFsc2UKdmFyIGJ1aWxkcyA9IDAKCmZ1biBleHBlbnNpdmVEdW1wKCk6IFN0cmluZyB7IGJ1aWxkcysrOyByZXR1cm4gIngiLnJlcGVhdCgxMF8wMDApIH0KCmZ1biBsb2dFYWdlcihtZXNzYWdlOiBTdHJpbmcpIHsgaWYgKGRlYnVnRW5hYmxlZCkgcHJpbnRsbihtZXNzYWdlKSB9CmZ1biBsb2dMYXp5KG1lc3NhZ2U6ICgpIC0-IFN0cmluZykgeyBpZiAoZGVidWdFbmFibGVkKSBwcmludGxuKG1lc3NhZ2UoKSkgfQoKZnVuIG1haW4oKSB7CiAgICByZXBlYXQoMV8wMDApIHsgbG9nRWFnZXIoInN0YXRlID0gJHtleHBlbnNpdmVEdW1wKCl9IikgfSAgIC8vIOKdjCBk4buxbmcgY2h14buXaSAxMDAwIGzhuqduCiAgICBwcmludGxuKCJsb2cgdGjGsOG7nW5nOiBk4buxbmcgY2h14buXaSAkYnVpbGRzIGzhuqduIGTDuSBraMO0bmcgaW4gZ8OsIikKCiAgICBidWlsZHMgPSAwCiAgICByZXBlYXQoMV8wMDApIHsgbG9nTGF6eSB7ICJzdGF0ZSA9ICR7ZXhwZW5zaXZlRHVtcCgpfSIgfSB9IC8vIOKchSBsYW1iZGEgY2jhu4kgY2jhuqF5IGtoaSBj4bqnbgogICAgcHJpbnRsbigibG9nIGxhbWJkYTogZOG7sW5nIGNodeG7l2kgJGJ1aWxkcyBs4bqnbiIpCn0K) · Xem thêm: K880

### K818 · `OutOfMemoryError` khi hiện ảnh chụp từ camera

`🟡 đã làm app` · `Android`

**Gặp:** `java.lang.OutOfMemoryError: Failed to allocate a 48000012 byte allocation with … free bytes` khi mở ảnh
12 MP vào một ô nhỏ.

**Vì sao:** `BitmapFactory.decodeFile` giải nén ảnh ở kích thước gốc: 4000×3000 pixel × 4 byte ≈ 48 MB cho một ảnh, dù
ô hiển thị chỉ 200×200.

**Sửa:** dùng thư viện ảnh (Coil, Glide) để tự thu nhỏ theo kích thước ô và cache. Tự decode thì đọc kích thước trước
(`inJustDecodeBounds = true`) rồi đặt `inSampleSize`.

### K819 · Đăng ký receiver / listener mà không huỷ đăng ký

`🟡 đã làm app` · `Android`

**Gặp:** LeakCanary báo leak; pin tụt dù app đã đóng màn hình; callback chạy cho màn hình không còn hiện.

**Vì sao:** `registerReceiver`, `registerListener` (cảm biến), `requestLocationUpdates` giữ tham chiếu tới listener
cho tới khi bạn huỷ. Quên huỷ là listener và mọi thứ nó giữ sống mãi, cảm biến vẫn chạy.

**Sửa:** bọc API callback trong `callbackFlow` và huỷ đăng ký trong `awaitClose { }`. Hết người collect là tự huỷ, không
còn chỗ nào để quên.

```kotlin
fun observe() = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { trySend(i.toReading()) }
    }
    context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    awaitClose { context.unregisterReceiver(receiver) }   // ✅ luôn chạy khi flow dừng
}
```

**Gặp ở:** [`apps/DevicePulse/shared/src/androidMain/kotlin/dev/devicepulse/sensor/AndroidBatterySensor.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/DevicePulse/shared/src/androidMain/kotlin/dev/devicepulse/sensor/AndroidBatterySensor.kt)

### K820 · Chuyến đi đã dừng mà GPS vẫn bật

`🟡 đã làm app` · `Android`

**Gặp:** biểu tượng vị trí trên thanh trạng thái vẫn sáng sau khi bấm *Dừng*; pin tụt cả đêm.

**Vì sao:** `requestLocationUpdates` giữ GPS bật cho tới khi gọi `removeUpdates(listener)` hoặc process chết. Huỷ
coroutine không tự tắt GPS nếu không ai gọi `removeUpdates`.

**Sửa:** gọi `removeUpdates` trong `awaitClose { }` của `callbackFlow`, và cho job collect vị trí bị huỷ khi dừng chuyến.

```kotlin
manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, listener, Looper.getMainLooper())
awaitClose { manager.removeUpdates(listener) }   // ✅ tắt GPS khi không còn ai nghe
```

**Gặp ở:** [`apps/TripLog/shared/src/androidMain/kotlin/dev/triplog/platform/AndroidLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/androidMain/kotlin/dev/triplog/platform/AndroidLocationStream.kt) · Xem thêm: K819

### K821 · Xin GPS chính xác nhất, mỗi giây, cho việc không cần

`🟡 đã làm app` · `Android` · `iOS`

**Gặp:** app chỉ cần biết "đang ở thành phố nào" mà pin hao như app dẫn đường.

**Vì sao:** GPS chính xác cao, chu kỳ 1 giây là chế độ tốn pin nhất. Ghi lộ trình chạy bộ/lái xe cần nó; hiện thời
tiết, gợi ý cửa hàng thì không.

**Sửa:** chọn theo nhu cầu: chu kỳ dài hơn, độ chính xác thấp hơn (Android: `Priority.PRIORITY_BALANCED_POWER_ACCURACY`
của Fused Location, hoặc provider `NETWORK`; iOS: `kCLLocationAccuracyKilometer`, hoặc *significant-change*). Cần một
điểm thì lấy một lần rồi tắt, không mở luồng liên tục.

### K822 · Ghi từng điểm GPS xuống DB ngay khi nhận

`🟡 đã làm app` · `KMP`

**Gặp:** ghi chuyến đi vài giờ thì máy ấm, pin tụt nhanh; đo thấy mỗi giây một lần ghi đĩa.

**Vì sao:** mỗi `INSERT` ngoài transaction là một commit riêng, kèm một lần đồng bộ đĩa (fsync). Một điểm mỗi giây trong
3 giờ là hơn 10.000 lần đồng bộ.

**Sửa:** gom điểm vào bộ nhớ, ghi theo lô (ví dụ 10 điểm) trong **một** transaction; nhớ ghi nốt lô cuối khi dừng.

```kotlin
pending.add(point)
if (pending.size >= 10) {
    db.transaction { pending.forEach { insertPoint(it) } }   // ✅ 10 điểm, 1 lần đồng bộ
    pending.clear()
}
```

**Gặp ở:** [`apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/core/TripRecorder.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/core/TripRecorder.kt) · [`apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt)

### K823 · Collect Flow trong `lifecycleScope.launch` → cảm biến chạy cả khi app ở nền

`🟡 đã làm app` · `Android`

**Gặp:** về màn hình chính rồi mà Logcat vẫn chạy log cập nhật vị trí/pin; Android cảnh báo app dùng vị trí ở nền.

**Vì sao:** `lifecycleScope.launch { flow.collect { } }` chỉ dừng khi `Activity` bị **huỷ**, không dừng khi app chỉ bị
**ẩn**. Flow lạnh (cảm biến, `callbackFlow`) vẫn được nghe và vẫn chạy.

**Sửa:** trong Compose dùng `collectAsStateWithLifecycle()`; ngoài Compose dùng `repeatOnLifecycle(STARTED)`. Cả hai
ngừng collect khi app xuống dưới `STARTED`.

```kotlin
lifecycleScope.launch { viewModel.state.collect { render(it) } }   // ❌ chạy cả khi ẩn
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.state.collect { render(it) } } // ✅
}
```

Xem thêm: K824

### K824 · `stateIn(SharingStarted.Eagerly)` giữ cảm biến bật mãi

`🟡 đã làm app` · `KMP`

**Gặp:** đã dùng `collectAsStateWithLifecycle` mà GPS/receiver vẫn bật khi app ở nền.

**Vì sao:** UI ngừng nghe nhưng `StateFlow` vẫn được chia sẻ. Với `Eagerly` hoặc `Lazily`, flow gốc chạy tiếp cho tới khi
scope chết, dù không còn ai nghe.

**Sửa:** `SharingStarted.WhileSubscribed(5_000)`: hết người nghe 5 giây thì dừng flow gốc (5 giây đủ để xoay máy không
khởi động lại cảm biến).

```kotlin
val state = combine(battery.observe(), location.observe()) { b, l -> Dashboard(b, l) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5_000), Dashboard())   // ✅
```

**Gặp ở:** [`apps/DevicePulse/shared/src/commonMain/kotlin/dev/devicepulse/core/PulseMonitor.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/DevicePulse/shared/src/commonMain/kotlin/dev/devicepulse/core/PulseMonitor.kt)

### K825 · Vòng lặp nặng không dừng khi đã huỷ

`🟡 đã làm app` · `Kotlin`

**Gặp:** rời màn hình (scope bị huỷ) mà CPU vẫn 100 % một lúc; vòng lặp xử lý vẫn chạy hết rồi mới thôi.

**Vì sao:** huỷ coroutine chỉ đặt cờ. Code chỉ thấy cờ đó tại điểm treo (`delay`, `yield`, `withContext`…) hoặc khi tự
kiểm. Vòng lặp tính toán thuần không có điểm treo nào nên chạy tới cùng.

**Sửa:** trong vòng lặp dài gọi `ensureActive()` (hoặc `yield()`) định kỳ. Vòng lặp polling thì có `delay` là đủ.

```kotlin
while (i < n) {
    if (i % 10_000 == 0) ensureActive()   // ✅ ném CancellationException khi đã huỷ
    process(i++)
}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgovLyBC4bqreTogY2FuY2VsKCkgY2jhu4kgIsSR4bq3dCBj4budIi4gVsOybmcgbOG6t3Aga2jDtG5nIGPDsyDEkWnhu4NtIHRyZW8gKHN1c3BlbmQpIHRow6wga2jDtG5nIGJhbyBnaeG7nSB0aOG6pXkgY-G7nSDEkcOzLgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFsIHN0dWJib3JuID0gbGF1bmNoKERpc3BhdGNoZXJzLkRlZmF1bHQpIHsKICAgICAgICB2YXIgaSA9IDBMCiAgICAgICAgd2hpbGUgKGkgPCAzMDBfMDAwXzAwMEwpIGkrKyAgICAgICAgICAgICAgICAgLy8g4p2MIGtow7RuZyBraeG7g20gdHJhIGh14bu3CiAgICAgICAgcHJpbnRsbigi4p2MIHbDsm5nIGzhurdwIGPhu6luZyDEkeG6p3UgduG6q24gY2jhuqF5IGjhur90OiBpID0gJGkiKQogICAgfQogICAgZGVsYXkoMTApCiAgICBzdHViYm9ybi5jYW5jZWxBbmRKb2luKCkKCiAgICB2YWwgcG9saXRlID0gbGF1bmNoKERpc3BhdGNoZXJzLkRlZmF1bHQpIHsKICAgICAgICB2YXIgaSA9IDBMCiAgICAgICAgd2hpbGUgKGkgPCAzMDBfMDAwXzAwMEwpIHsKICAgICAgICAgICAgaWYgKGkgJSAxXzAwMF8wMDAgPT0gMEwpIGVuc3VyZUFjdGl2ZSgpICAvLyDinIUgdGjhuqV5IGzhu4duaCBodeG7tyBsw6AgZOG7q25nCiAgICAgICAgICAgIGkrKwogICAgICAgIH0KICAgICAgICBwcmludGxuKCJraMO0bmcgYmFvIGdp4budIGluIGTDsm5nIG7DoHkiKQogICAgfQogICAgZGVsYXkoMTApCiAgICBwb2xpdGUuY2FuY2VsQW5kSm9pbigpCiAgICBwcmludGxuKCLinIUgdsOybmcgbOG6t3AgY8OzIGVuc3VyZUFjdGl2ZSgpIGThu6tuZyDEkcO6bmcgbMO6YyBodeG7tyIpCn0K)

### K826 · Thử GPS trên emulator: tốc độ luôn 0 km/h

`🟢 mới học` · `Android`

**Gặp:** gửi điểm bằng `adb emu geo fix` thì quãng đường đúng mà *tốc độ hiện tại* và *tốc độ tối đa* là 0; hoặc điểm
nhảy sang tận Nam Cực.

**Vì sao:** điểm từ `geo fix` có `hasSpeed() == true` nhưng `speed = 0`, nên app tin con số đó. Thêm nữa, lệnh nhận
**kinh độ trước, vĩ độ sau** (`geo fix <longitude> <latitude>`), ngược với thói quen.

**Sửa:** chỉ tự tính tốc độ (quãng đường / thời gian) khi chip không báo tốc độ; kiểm tốc độ trên máy thật, hoặc dùng
tuyến đường có sẵn trong *Extended Controls → Location → Routes* của emulator. Gửi điểm thì nhớ thứ tự:

```bash
adb emu geo fix 105.8542 21.0285   # kinh độ, vĩ độ (Hà Nội)
```

**Gặp ở:** [`apps/TripLog/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/README.md) (mục "Mấy cái bẫy")

### K827 · `Context.startForegroundService() did not then call Service.startForeground()`

`🟡 đã làm app` · `Android`

**Gặp:** crash vài giây sau khi bấm *Bắt đầu*; Android 12+ báo
`ForegroundServiceDidNotStartInTimeException: Context.startForegroundService() did not then call Service.startForeground()`
(bản cũ hơn là `RemoteServiceException` cùng câu đó).

**Vì sao:** gọi `startForegroundService` là hứa với hệ thống: service sẽ gọi `startForeground(...)` trong vòng khoảng
5 giây. Không gọi (vì `return` sớm, chờ mạng trước, ném lỗi trước đó) là crash.

**Sửa:** gọi `startForeground` **đầu tiên** trong `onStartCommand`, trước mọi việc có thể chậm hoặc thất bại. Nếu sau đó
thấy không cần chạy thì `stopSelf()`.

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), FOREGROUND_SERVICE_TYPE_LOCATION) // ✅ trước tiên
    recorder.start()
    return START_NOT_STICKY
}
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt)

### K828 · Android 14: `MissingForegroundServiceTypeException`

`🟡 đã làm app` · `Android`

**Gặp:** nâng `targetSdk` lên 34 là crash khi bắt đầu ghi:
`android.app.MissingForegroundServiceTypeException: Starting FGS without a type`.

**Vì sao:** từ Android 14 (target 34), mọi foreground service phải khai báo **loại** (`location`, `dataSync`,
`mediaPlayback`, `connectedDevice`…) trong manifest, và mỗi loại cần quyền `FOREGROUND_SERVICE_<LOẠI>` riêng.

**Sửa:** khai `android:foregroundServiceType` cho `<service>` và thêm quyền tương ứng.

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

<service
    android:name=".service.TrackingService"
    android:exported="false"
    android:foregroundServiceType="location" />
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/AndroidManifest.xml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/AndroidManifest.xml) · Xem thêm: K829, K830, K893

### K829 · Android 14: `SecurityException: Starting FGS with type location … requires permissions`

`🟡 đã làm app` · `Android`

**Gặp:** `java.lang.SecurityException: Starting FGS with type location callerApp=… targetSDK=34 requires permissions:
all of the permissions allOf=true [android.permission.FOREGROUND_SERVICE_LOCATION] any of the permissions allOf=false
[android.permission.ACCESS_COARSE_LOCATION, android.permission.ACCESS_FINE_LOCATION]`.

**Vì sao:** service loại `location` cần **hai** thứ: quyền cài đặt `FOREGROUND_SERVICE_LOCATION` trong manifest, và quyền
runtime vị trí **đã được cấp** lúc gọi `startForeground`. Thiếu một trong hai là `SecurityException`.

**Sửa:** thêm quyền vào manifest, và chỉ khởi động service sau khi người dùng đã cấp quyền vị trí (kiểm ngay trước khi
gọi, vì người dùng có thể thu hồi trong Settings).

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt) (`TrackingService.start`) · Xem thêm: K529

### K830 · `foregroundServiceType 0x00000008 is not a subset of foregroundServiceType attribute`

`🟡 đã làm app` · `Android`

**Gặp:** `java.lang.IllegalArgumentException: foregroundServiceType 0x00000008 is not a subset of foregroundServiceType
attribute 0x00000000 in service element of manifest file`.

**Vì sao:** loại truyền vào `startForeground(…, type)` phải nằm trong các loại đã khai ở manifest. `0x8` là `location`,
còn manifest không khai loại nào (`0x0`) hoặc khai loại khác.

**Sửa:** để hai chỗ khớp nhau. Dùng `ServiceCompat.startForeground` để khỏi tự rẽ nhánh theo bản Android (nó bỏ tham số
type trên Android 9 trở xuống, nơi chưa có khái niệm này).

```kotlin
ServiceCompat.startForeground(
    this, NOTIFICATION_ID, notification,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
)
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt)

### K831 · Android 12+: `ForegroundServiceStartNotAllowedException`

`🟡 đã làm app` · `Android`

**Gặp:** `android.app.ForegroundServiceStartNotAllowedException: startForegroundService() not allowed due to
mAllowStartForeground false`, thường khi khởi động service từ receiver, từ push, hoặc sau một `delay` lúc app đã xuống
nền.

**Vì sao:** từ Android 12 (target 31), app ở nền không được tự mở foreground service, trừ vài trường hợp miễn (người dùng
bấm vào notification, push FCM ưu tiên cao, một số broadcast hệ thống…).

**Sửa:** khởi động service khi người dùng đang ở trong app (ngay trong `onClick`). Việc cần chạy khi app ở nền thì giao cho
WorkManager (K840, K844); muốn nhanh thì dùng *expedited work*.

### K832 · Service vị trí khởi động từ nền không nhận được vị trí

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** service mở từ `BOOT_COMPLETED` hoặc từ WorkManager chạy được, notification hiện, nhưng không có điểm GPS nào; trên
Android 14 có thể crash `SecurityException` như K829.

**Vì sao:** từ Android 11, foreground service được mở khi app **ở nền** không được dùng quyền "chỉ khi dùng app" (vị trí,
camera, micro). Quyền đó chỉ có hiệu lực nếu service được mở lúc app đang hiện.

**Sửa:** mở service ghi vị trí từ UI (người dùng bấm *Bắt đầu*). Nếu thật sự cần vị trí khi không mở app thì phải có quyền
`ACCESS_BACKGROUND_LOCATION` (K846) và qua được duyệt của Play.

### K833 · Android 8+: `Not allowed to start service Intent { … }: app is in background`

`🟡 đã làm app` · `Android`

**Gặp:** `java.lang.IllegalStateException: Not allowed to start service Intent { cmp=dev.xxx/.SyncService }: app is in
background uid …`.

**Vì sao:** từ Android 8, `startService()` (service thường, không foreground) chỉ được gọi khi app ở tiền cảnh. Code cũ
khởi động service đồng bộ từ receiver hay alarm sẽ crash.

**Sửa:** việc nền ngắn hoặc định kỳ → WorkManager. Việc người dùng thấy và cần chạy lâu → foreground service, mở từ UI.

### K834 · Android 13: không thấy notification của foreground service

`🟢 mới học` · `Android`

**Gặp:** service chạy, ghi chuyến đi được, nhưng không có notification nào trên thanh trạng thái để bấm *Dừng*.

**Vì sao:** từ Android 13, `POST_NOTIFICATIONS` là quyền runtime. Người dùng từ chối thì service **vẫn chạy**, chỉ là
notification không hiện trong ngăn kéo (nó vẫn có trong *Task Manager* của hệ thống).

**Sửa:** xin `POST_NOTIFICATIONS` trước khi bắt đầu, và trong app luôn có nút *Dừng* riêng, không chỉ dựa vào nút trên
notification.

**Gặp ở:** [`apps/TripLog/androidApp/src/main/AndroidManifest.xml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/AndroidManifest.xml) · Xem thêm: K538

### K835 · Android 12+: notification của service hiện chậm 10 giây

`🟢 mới học` · `Android`

**Gặp:** bấm *Bắt đầu* mà vài giây sau notification "Đang ghi" mới hiện; người dùng tưởng chưa bấm được.

**Vì sao:** từ Android 12, hệ thống có thể hoãn notification của foreground service tới 10 giây, để service chạy rất ngắn
không làm phiền.

**Sửa:** với service người dùng chủ động bật và cần thấy ngay, đặt `FOREGROUND_SERVICE_IMMEDIATE`.

```kotlin
NotificationCompat.Builder(this, CHANNEL_ID)
    .setOngoing(true)
    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // ✅
    .build()
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt)

### K836 · Service vừa mở đã tự tắt

`🟡 đã làm app` · `Android` · `KMP`

**Gặp:** bấm *Bắt đầu*, notification nháy lên rồi biến mất, service `onDestroy` ngay.

**Vì sao:** service theo dõi `state` của recorder và `stopSelf()` khi thấy `Idle`. Nhưng `recorder.start()` xử lý bất đồng
bộ, nên giá trị **đầu tiên** service thấy thường vẫn là `Idle` cũ.

**Sửa:** chỉ coi `Idle` là "chuyến đã xong" khi trước đó đã thấy `Recording`/`Paused`.

```kotlin
var seenActive = false
recorder.state.collect { state ->
    when (state) {
        is RecordState.Recording, is RecordState.Paused -> seenActive = true
        RecordState.Idle -> if (seenActive) stopSelf()   // ✅ bỏ qua Idle lúc vừa mở
        else -> Unit
    }
}
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt)

### K837 · Ghi chuyến đi trong `viewModelScope` → bấm Back là mất chuyến

`🟢 mới học` · `Android` · `KMP`

**Gặp:** đang ghi, bấm Back hoặc vuốt app khỏi màn hình thì chuyến dừng giữa chừng; có service chạy rồi mà vẫn mất.

**Vì sao:** `viewModelScope` bị huỷ khi màn hình rời back stack. Service giữ cho **process** sống, nhưng không giữ được
coroutine nằm trong một `ViewModel` đã chết.

**Sửa:** đưa việc chạy lâu vào một object sống theo app (Koin `single`) với `CoroutineScope` riêng của app. Service và
`ViewModel` chỉ ra lệnh và đọc `state` của object đó.

```kotlin
val commonModule = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) } // ✅ sống theo process
    single { TripRecorder(location = get(), repository = get(), scope = get()) }
}
```

**Gặp ở:** [`apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/di/Koin.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/di/Koin.kt) · [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/RecordViewModel.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/RecordViewModel.kt) · Xem thêm: K511

### K838 · `START_STICKY` rồi crash vì `intent!!`

`🟡 đã làm app` · `Android`

**Gặp:** vài giờ sau, hệ thống tự khởi động lại service và app crash với `NullPointerException` trong `onStartCommand`.

**Vì sao:** `START_STICKY` bảo hệ thống tạo lại service sau khi giết process, nhưng gọi `onStartCommand` với
`intent = null`. Lúc đó mọi state trong RAM (chuyến đang ghi) cũng đã mất.

**Sửa:** xử lý `intent == null` (đọc `intent?.action`). Nếu sau khi process chết không có gì để chạy tiếp thì trả về
`START_NOT_STICKY` cho rõ.

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) { recorder.stop(); return START_NOT_STICKY }
    // ...
    return START_NOT_STICKY   // ✅ recorder khởi động lại là Idle, không có gì để tiếp tục
}
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/service/TrackingService.kt)

### K839 · Doze: `delay`, `Handler`, `Timer` ở nền ngừng chạy khi tắt màn hình lâu

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** vòng "mỗi 5 phút gọi API" chạy tốt khi test, nhưng để máy nằm yên qua đêm thì cách nhau hàng giờ; request trong
lúc đó báo lỗi mạng.

**Vì sao:** máy nằm yên, tắt màn hình, không sạc một lúc thì vào **Doze**: chặn mạng, bỏ qua wakelock, hoãn alarm và job
tới các "cửa sổ bảo trì" thưa dần. Coroutine `delay` trong process nền không được đánh thức đúng giờ.

**Sửa:** việc định kỳ dùng WorkManager (chấp nhận giờ chạy không chính xác). Cần đúng giờ thì xem K845. Thử Doze bằng adb:

```bash
adb shell dumpsys battery unplug
adb shell dumpsys deviceidle force-idle   # vào Doze ngay
adb shell dumpsys deviceidle unforce && adb shell dumpsys battery reset
```

### K840 · WorkManager: đặt chu kỳ 1 phút mà chạy 15 phút một lần

`🟢 mới học` · `Android`

**Gặp:** `PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.MINUTES)` nhưng worker cách 15 phút mới chạy; Logcat có
`Interval duration lesser than minimum allowed value; Changed to 900000`.

**Vì sao:** chu kỳ tối thiểu của periodic work là 15 phút (`PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS`). Giá trị nhỏ
hơn bị nâng lên. Ngay cả 15 phút cũng không được đảm bảo: Doze và standby bucket (K841) có thể hoãn thêm.

**Sửa:** thiết kế cho chu kỳ ≥ 15 phút và giờ chạy không chính xác. Cần cập nhật liên tục khi app đang mở thì dùng
coroutine trong app; cần liên tục ở nền thì là việc của foreground service, không phải WorkManager.

### K841 · Worker định kỳ chạy ngày một thưa dần

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** người dùng ít mở app thì việc đồng bộ nền chạy vài lần một ngày, rồi gần như không chạy.

**Vì sao:** **App Standby Buckets** (Android 9+) xếp app theo mức dùng: `active`, `working_set`, `frequent`, `rare`,
`restricted`. Bucket càng thấp, job và alarm càng bị giới hạn. Đây là thiết kế, không phải lỗi.

**Sửa:** đừng hứa "đồng bộ mỗi 15 phút" với người dùng. Làm việc nặng khi app mở; dùng push (FCM) để đánh thức khi server
có dữ liệu mới. Thử bằng adb:

```bash
adb shell am set-standby-bucket dev.xxx rare
adb shell am get-standby-bucket dev.xxx
```

### K842 · Mỗi lần mở app lại `enqueue` thêm một periodic work

`🟢 mới học` · `Android`

**Gặp:** worker chạy 2, 3, 10 lần mỗi chu kỳ; số lần tăng theo số lần mở app.

**Vì sao:** `WorkManager.enqueue(periodicRequest)` luôn thêm một work **mới**. Work cũ vẫn còn, kể cả qua khởi động lại
máy, vì WorkManager lưu vào DB.

**Sửa:** dùng `enqueueUniquePeriodicWork` với tên cố định và `ExistingPeriodicWorkPolicy.KEEP` (giữ cái cũ) hoặc
`UPDATE` (cập nhật cấu hình, không reset chu kỳ).

```kotlin
WorkManager.getInstance(context).enqueue(request)                       // ❌ thêm bản sao
WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, request) // ✅
```

### K843 · Worker bị dừng giữa chừng sau 10 phút

`🟡 đã làm app` · `Android`

**Gặp:** tải file lớn trong `CoroutineWorker` thì bị huỷ (`CancellationException`), worker tự chạy lại từ đầu, mãi không
xong.

**Vì sao:** một lần chạy của worker thường bị giới hạn khoảng 10 phút. Hết thời gian, hệ thống dừng worker và lên lịch
chạy lại.

**Sửa:** chia việc thành phần nhỏ có thể tiếp tục (lưu tiến độ). Việc dài thật sự thì gọi `setForeground(ForegroundInfo)`
trong worker để chạy như foreground service; Android 14+ cần thêm loại service (ví dụ `dataSync`) cho
`androidx.work.impl.foreground.SystemForegroundService` trong manifest và quyền `FOREGROUND_SERVICE_DATA_SYNC`.

Xem thêm: K847

### K844 · Expedited work crash trên Android 11 trở xuống

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** `setExpedited(...)` chạy tốt trên máy Android 13, nhưng crash trên Android 11:
`Expedited WorkRequests require a ListenableWorker to provide an implementation for getForegroundInfoAsync()`.

**Vì sao:** trước Android 12, WorkManager chạy expedited work bằng một foreground service, nên cần notification. Worker
không cung cấp `ForegroundInfo` thì không có gì để hiện.

**Sửa:** override `getForegroundInfo()` trong `CoroutineWorker` (trả notification + loại service).

```kotlin
override suspend fun getForegroundInfo(): ForegroundInfo =
    ForegroundInfo(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
```

### K845 · Alarm đúng giờ: `SecurityException` về `SCHEDULE_EXACT_ALARM`

`🟡 đã làm app` · `Android`

**Gặp:** `java.lang.SecurityException: Caller dev.xxx needs to hold android.permission.SCHEDULE_EXACT_ALARM or
android.permission.USE_EXACT_ALARM to set exact alarms.`

**Vì sao:** từ Android 12, `setExact…` cần quyền đặc biệt. Từ Android 14, `SCHEDULE_EXACT_ALARM` **không còn được cấp sẵn**
cho đa số app mới cài; người dùng phải bật trong Settings. `USE_EXACT_ALARM` được cấp sẵn nhưng Play chỉ cho app báo
thức/lịch dùng.

**Sửa:** hỏi trước bằng `canScheduleExactAlarms()`, chưa có thì mở `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` hoặc lùi về
alarm không chính xác. Phần lớn nhắc nhở không cần đúng tới phút.

```kotlin
val am = context.getSystemService(AlarmManager::class.java)
if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
} else {
    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)   // ✅ lùi về không chính xác
}
```

### K846 · Xin "Luôn cho phép" vị trí như mọi quyền khác

`🟡 đã làm app` · `Android`

**Gặp:** xin `ACCESS_BACKGROUND_LOCATION` mà không có hộp thoại; hoặc app bị Play từ chối với lý do vị trí nền.

**Vì sao:** từ Android 11, vị trí nền phải xin **riêng**, **sau khi** đã có vị trí thường, và hệ thống đưa người dùng sang
trang Settings chứ không hiện hộp thoại. Play còn đòi khai báo trong Console (kèm video) và chỉ duyệt khi tính năng chính
thật sự cần.

**Sửa:** đa số app chỉ cần vị trí lúc đang dùng, cộng một foreground service loại `location` mở từ UI khi cần ghi lúc tắt
màn hình (cách TripLog làm). Chỉ xin vị trí nền khi app phải tự lấy vị trí mà người dùng không mở app (geofence…).

**Gặp ở:** [`apps/TripLog/androidApp/src/main/AndroidManifest.xml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/AndroidManifest.xml) (không có `ACCESS_BACKGROUND_LOCATION`) · Xem thêm: K537, K832

### K847 · Android 15: service `dataSync` bị dừng sau 6 giờ, không mở được từ `BOOT_COMPLETED`

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** target 35, service đồng bộ dài bị hệ thống gọi `onTimeout(...)` rồi crash nếu không dừng; service mở từ receiver
`BOOT_COMPLETED` báo `ForegroundServiceStartNotAllowedException`.

**Vì sao:** Android 15 giới hạn service loại `dataSync` (và loại mới `mediaProcessing`) khoảng 6 giờ trong 24 giờ, và cấm
receiver `BOOT_COMPLETED` mở foreground service loại `dataSync`, `camera`, `mediaPlayback`, `phoneCall`,
`mediaProjection`, `microphone`.

**Sửa:** override `Service.onTimeout(startId, fgsType)` và `stopSelf()` trong vài giây. Đồng bộ dữ liệu ưu tiên WorkManager;
sau khởi động máy thì lên lịch work thay vì mở service ngay.

### K848 · iOS: crash `Invalid parameter not satisfying: !stayUp || CLClientIsBackgroundable`

`🟡 đã làm app` · `iOS`

**Gặp:** vừa bắt đầu ghi là crash với `NSInternalInconsistencyException`, lý do
`Invalid parameter not satisfying: !stayUp || CLClientIsBackgroundable(internal->fClient)`.

**Vì sao:** đặt `allowsBackgroundLocationUpdates = true` mà `Info.plist` không có `UIBackgroundModes` chứa `location`.
iOS không có foreground service; background mode là cách app xin chạy tiếp khi tắt màn hình.

**Sửa:** thêm background mode *Location updates* (*Signing & Capabilities → Background Modes*, hoặc `project.yml`). Quyền
*When In Use* là đủ nếu bắt đầu ghi lúc app đang mở; iOS hiện viên chỉ báo vị trí màu xanh khi app chạy nền.

```yaml
# iosApp/project.yml
info:
  properties:
    UIBackgroundModes: [location]
```

**Gặp ở:** [`apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt) · [`apps/TripLog/iosApp/project.yml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/iosApp/project.yml) · Xem thêm: K896

### K849 · iOS: dừng đèn đỏ là GPS tắt luôn, không bật lại

`🔴 khó, hiếm nhưng đau` · `iOS`

**Gặp:** chuyến đi trên iOS bị cắt ở chỗ đứng chờ vài phút; đoạn sau không có điểm nào, cho tới khi mở lại app.

**Vì sao:** `pausesLocationUpdatesAutomatically` mặc định là `true`: iOS tự tạm dừng cập nhật khi thấy máy đứng yên để tiết
kiệm pin, và khi app đang ở nền thì không tự chạy lại.

**Sửa:** app ghi lộ trình đặt `false`, cùng `activityType` hợp lý. Dừng ghi thì nhớ `stopUpdatingLocation()`.

```kotlin
manager.allowsBackgroundLocationUpdates = true
manager.pausesLocationUpdatesAutomatically = false   // ✅ không tự tắt khi đứng yên
manager.activityType = CLActivityTypeFitness
```

**Gặp ở:** [`apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt)

### K850 · iOS: việc nền không chạy, hoặc `All launch handlers must be registered before application finishes launching`

`🟡 đã làm app` · `iOS`

**Gặp:** app vào nền là mọi coroutine dừng sau vài giây; `BGTaskScheduler` không bao giờ gọi handler, hoặc crash với
`NSInternalInconsistencyException: All launch handlers must be registered before application finishes launching`.

**Vì sao:** iOS treo (suspend) app gần như ngay khi vào nền, trừ khi có background mode được phép. `beginBackgroundTask`
chỉ xin thêm khoảng 30 giây. `BGTaskScheduler` cần: id khai trong `BGTaskSchedulerPermittedIdentifiers`, background mode
`fetch`/`processing`, và `register` **trước khi** `didFinishLaunching` kết thúc. Hệ thống quyết định giờ chạy, và không
tự chạy trên Simulator.

**Sửa:** đăng ký handler sớm trong `init` của `App` hoặc `application(_:didFinishLaunchingWithOptions:)`; thử trên máy thật
bằng lệnh debugger dừng app rồi gõ
`e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateLaunchForTaskWithIdentifier:@"dev.xxx.refresh"]`.
Đừng hứa giờ chạy chính xác với người dùng.
