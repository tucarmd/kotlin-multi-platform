# 3. Collections & Sequence (K101–K150)

`List`, `Map`, `Set`, mảng và `Sequence`: thứ dùng nhiều nhất trong mọi app, nên bẫy cũng nhiều nhất. Phần lớn lỗi ở đây
**không** báo lúc compile: app chạy, rồi crash khi list rỗng, hoặc lặng lẽ cho ra kết quả sai.

### K101 · Xoá phần tử khi đang duyệt bằng `for` → `ConcurrentModificationException`

`🟢 mới học` · `Kotlin`

**Gặp:** `java.util.ConcurrentModificationException` ở dòng `for (x in list)`, ngay sau lần `list.remove(...)` đầu tiên.

**Vì sao:** vòng `for` dùng một iterator. Iterator ghi nhớ số lần list bị đổi cấu trúc; bạn thêm/xoá trực tiếp trên list thì
lần `next()` sau nó phát hiện và ném lỗi.

**Sửa:** dùng `removeAll { }` (hoặc `filter` để tạo list mới). Thật sự cần vòng lặp thì dùng `iterator()` và gọi `it.remove()`.

```kotlin
for (p in prices) if (p == 0) prices.remove(p)   // ❌
prices.removeAll { it == 0 }                     // ✅
val nonZero = prices.filter { it != 0 }          // ✅ list mới
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEwMTogeG_DoSBwaOG6p24gdOG7rSB0cm9uZyBsw7pjIMSRYW5nIGR1eeG7h3QgYuG6sW5nIGZvciDihpIgQ29uY3VycmVudE1vZGlmaWNhdGlvbkV4Y2VwdGlvbgpmdW4gbWFpbigpIHsKICAgIHZhbCBwcmljZXMgPSBtdXRhYmxlTGlzdE9mKDEwLCAwLCAyNSwgMCwgNDApCiAgICAvLyDinIUgU-G7rWE6IHRoYXkgY-G6oyB2w7JuZyBmb3IgYuG6sW5nIG3hu5l0IGTDsm5nOiAgcHJpY2VzLnJlbW92ZUFsbCB7IGl0ID09IDAgfQogICAgZm9yIChwIGluIHByaWNlcykgewogICAgICAgIGlmIChwID09IDApIHByaWNlcy5yZW1vdmUocCkgICAvLyDinYwgxJHhu5VpIGxpc3QgbcOgIGl0ZXJhdG9yIGPhu6dhIHbDsm5nIGZvciDEkWFuZyBkw7luZwogICAgfQogICAgcHJpbnRsbihwcmljZXMpCn0K) · Xem thêm: K102, K133

### K102 · Xoá theo index trong vòng chạy xuôi, bỏ sót phần tử

`🟢 mới học` · `Kotlin`

**Gặp:** không crash, nhưng sau khi "xoá hết số 0" list vẫn còn số 0. Thường là khi hai phần tử cần xoá đứng cạnh nhau.

**Vì sao:** `removeAt(i)` làm mọi phần tử phía sau dồn lên một ô. Phần tử mới ở vị trí `i` bị bỏ qua vì `i++`.

**Sửa:** `removeAll { }`; hoặc chạy ngược từ cuối về đầu (`for (i in xs.indices.reversed())`).

```kotlin
while (i < xs.size) { if (xs[i] == 0) xs.removeAt(i); i++ }   // ❌ [1, 0, 0, 2, 0] → [1, 0, 2]
xs.removeAll { it == 0 }                                      // ✅ [1, 2]
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEwMjogeG_DoSB0aGVvIGluZGV4IHRyb25nIHbDsm5nIGNo4bqheSB4dcO0aSDihpIgYuG7jyBzw7N0IHBo4bqnbiB04butIMSR4bupbmcgbGnhu4FuIHNhdQpmdW4gbWFpbigpIHsKICAgIHZhbCB4cyA9IG11dGFibGVMaXN0T2YoMSwgMCwgMCwgMiwgMCkKICAgIHZhciBpID0gMAogICAgd2hpbGUgKGkgPCB4cy5zaXplKSB7CiAgICAgICAgaWYgKHhzW2ldID09IDApIHhzLnJlbW92ZUF0KGkpICAvLyBwaOG6p24gdOG7rSBzYXUgZOG7k24gbMOqbiB24buLIHRyw60gaS4uLgogICAgICAgIGkrKyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIC4uLnLhu5NpIGLhu4sgbmjhuqN5IHF1YQogICAgfQogICAgcHJpbnRsbih4cykgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIFsxLCAwLCAyXSDigJQgY8OybiBzw7N0IG3hu5l0IHPhu5EgMAogICAgdmFsIHlzID0gbXV0YWJsZUxpc3RPZigxLCAwLCAwLCAyLCAwKQogICAgeXMucmVtb3ZlQWxsIHsgaXQgPT0gMCB9ICAgICAgICAgICAgIC8vIOKchQogICAgcHJpbnRsbih5cykKfQo) · Xem thêm: K101

### K103 · `List` chỉ đọc mà nội dung vẫn đổi

`🟡 đã làm app` · `Kotlin`

**Gặp:** giữ một `List<String>` lấy từ class khác, một lúc sau in ra thì nó đã có thêm phần tử, dù không ai gọi lại.

**Vì sao:** `List` chỉ là giao diện "không cho bạn sửa", không phải "không bao giờ đổi". `val items: List<T> get() = _items` trả về
**cùng** object `MutableList`; bên trong class sửa `_items` thì người giữ `items` thấy luôn.

**Sửa:** trả về bản chụp `_items.toList()`, hoặc giữ state trong `StateFlow<List<T>>` và mỗi lần đổi thì gán list mới.

```kotlin
val items: List<String> get() = _items           // ❌ lộ chính MutableList
val items: List<String> get() = _items.toList()  // ✅ bản copy
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEwMzogTGlzdCBjaOG7iSDEkeG7jWMgduG6q24gxJHhu5VpLCB2w6wgbsOzIGNo4buJIGzDoCAiY-G7rWEgc-G7lSIgbmjDrG4gdsOgbyBNdXRhYmxlTGlzdApjbGFzcyBDYXJ0IHsKICAgIHByaXZhdGUgdmFsIF9pdGVtcyA9IG11dGFibGVMaXN0T2YoImPDoCBwaMOqIikKICAgIHZhbCBpdGVtczogTGlzdDxTdHJpbmc-IGdldCgpID0gX2l0ZW1zICAgICAgICAgIC8vIOKdjCBjw7luZyBt4buZdCBvYmplY3QKICAgIC8vIHZhbCBpdGVtczogTGlzdDxTdHJpbmc-IGdldCgpID0gX2l0ZW1zLnRvTGlzdCgpICAvLyDinIUgYuG6o24gY2jhu6VwCiAgICBmdW4gYWRkKHg6IFN0cmluZykgeyBfaXRlbXMgKz0geCB9Cn0KZnVuIG1haW4oKSB7CiAgICB2YWwgY2FydCA9IENhcnQoKQogICAgdmFsIHNob3duID0gY2FydC5pdGVtcwogICAgcHJpbnRsbigidHLGsOG7m2M6ICRzaG93biIpCiAgICBjYXJ0LmFkZCgiYsOhbmggbcOsIikKICAgIHByaW50bG4oInNhdTogICAkc2hvd24iKSAgIC8vIMSR4buVaSB0aGVvIGTDuSBraMO0bmcgYWkgZ-G7jWkgbOG6oWkgY2FydC5pdGVtcwp9Cg) · Xem thêm: [Kotlin khó gì, bẫy gì: mục 2.9](../reference/kotlin-pitfalls.md), K104, K183

### K104 · Ép `List` thành `MutableList` bằng `as` rồi `add`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `java.lang.UnsupportedOperationException` khi `add`/`remove`. Tệ hơn: `set` lại chạy được và sửa luôn list "chỉ đọc".

**Vì sao:** trên JVM, `listOf(1, 2, 3)` là `java.util.Arrays$ArrayList`: implement `MutableList` nên `as` qua được, cho phép
`set` nhưng không cho đổi kích thước. Trên target khác (iOS, JS) hành vi lại có thể khác hẳn.

**Sửa:** không bao giờ ép kiểu để sửa. Cần sửa thì tạo bản copy bằng `toMutableList()`.

```kotlin
(fixed as MutableList<Int>).add(4)        // ❌ UnsupportedOperationException
val copy = fixed.toMutableList(); copy.add(4)  // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEwNDogw6lwIExpc3QgduG7gSBNdXRhYmxlTGlzdCBi4bqxbmcgYGFzYCBy4buTaSBhZGQg4oaSIFVuc3VwcG9ydGVkT3BlcmF0aW9uRXhjZXB0aW9uCmZ1biBtYWluKCkgewogICAgdmFsIGZpeGVkOiBMaXN0PEludD4gPSBsaXN0T2YoMSwgMiwgMykKICAgIHZhbCBoYWNrID0gZml4ZWQgYXMgTXV0YWJsZUxpc3Q8SW50PiAgIC8vIGNvbXBpbGUgxJHGsOG7o2MsIGNo4bqheSBjxaluZyDEkcaw4bujYy4uLgogICAgaGFja1swXSA9IDk5ICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIHNldCB24bqrbiBjaOG6oXkhCiAgICBwcmludGxuKGZpeGVkKSAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gWzk5LCAyLCAzXSDigJQgbGlzdCAiY2jhu4kgxJHhu41jIiBi4buLIMSR4buVaQogICAgaGFjay5hZGQoNCkgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBjcmFzaCDhu58gxJHDonkKICAgIC8vIOKchSBT4butYTogdmFsIGNvcHkgPSBmaXhlZC50b011dGFibGVMaXN0KCk7IGNvcHkuYWRkKDQpCn0K) · Xem thêm: K103

### K105 · `first()` trên list rỗng

`🟢 mới học` · `Kotlin`

**Gặp:** `java.util.NoSuchElementException: List is empty.` Thường ở màn hình đầu tiên khi database chưa có dữ liệu, hoặc khi API trả `[]`.

**Vì sao:** `first()`, `last()` giả định list có ít nhất một phần tử. Lúc viết code thì luôn có dữ liệu mẫu nên không thấy.

**Sửa:** dùng `firstOrNull()` / `lastOrNull()` và xử lý trường hợp rỗng bằng `?:`.

```kotlin
val latest = notes.first()                          // ❌ crash khi chưa có ghi chú
val latest = notes.firstOrNull() ?: return          // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEwNTogZmlyc3QoKSB0csOqbiBsaXN0IHLhu5duZyDihpIgTm9TdWNoRWxlbWVudEV4Y2VwdGlvbjogTGlzdCBpcyBlbXB0eS4KZnVuIG1haW4oKSB7CiAgICB2YWwgbm90ZXMgPSBlbXB0eUxpc3Q8U3RyaW5nPigpCiAgICBwcmludGxuKG5vdGVzLmZpcnN0T3JOdWxsKCkgPzogIihjaMawYSBjw7MgZ2hpIGNow7opIikgIC8vIOKchQogICAgcHJpbnRsbihub3Rlcy5maXJzdCgpKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIGNyYXNoCn0K) · Xem thêm: K108, K131

### K106 · `single()` khi có nhiều hơn một phần tử khớp

`🟡 đã làm app` · `Kotlin`

**Gặp:** `java.lang.IllegalArgumentException: Collection contains more than one matching element.` (hoặc `List has more than one element.`
khi gọi `single()` không điều kiện).

**Vì sao:** `single` nghĩa là "đúng một". Dữ liệu thật có trùng (hai user cùng email, hai ECU cùng trả lời) là crash.

**Sửa:** muốn "lấy cái đầu tiên" thì dùng `firstOrNull { }`. Muốn đúng một mà dữ liệu có thể bẩn thì `singleOrNull { }` (trả `null` khi 0 hoặc
nhiều hơn 1) rồi báo lỗi rõ.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEwNjogc2luZ2xlKCkga2hpIGPDsyBuaGnhu4F1IGjGoW4gbeG7mXQgcGjhuqduIHThu60KZGF0YSBjbGFzcyBVc2VyKHZhbCBpZDogSW50LCB2YWwgZW1haWw6IFN0cmluZykKZnVuIG1haW4oKSB7CiAgICB2YWwgdXNlcnMgPSBsaXN0T2YoVXNlcigxLCAiYUB4LnZuIiksIFVzZXIoMiwgImFAeC52biIpKQogICAgcHJpbnRsbih1c2Vycy5zaW5nbGVPck51bGwgeyBpdC5lbWFpbCA9PSAiYUB4LnZuIiB9KSAgLy8g4pyFIG51bGwga2hpIDAgaG_hurdjID4xIHBo4bqnbiB04butCiAgICBwcmludGxuKHVzZXJzLnNpbmdsZSB7IGl0LmVtYWlsID09ICJhQHgudm4iIH0pICAgICAgICAvLyDinYwgY3Jhc2ggdsOsIGPDsyAyIHVzZXIgdHLDuW5nIGVtYWlsCn0K) · Xem thêm: K105

### K107 · `list[i]` vượt phạm vi

`🟢 mới học` · `Kotlin`

**Gặp:** `java.lang.IndexOutOfBoundsException: Index 3 out of bounds for length 3` (có khi là `ArrayIndexOutOfBoundsException`).

**Vì sao:** index đến từ chỗ không kiểm soát được: tab đã lưu từ bản app cũ, vị trí trong `RecyclerView` sau khi list co lại, số từ server.

**Sửa:** `getOrNull(i)` hoặc `getOrElse(i) { mặcĐịnh }` khi index không chắc chắn.

```kotlin
tabs[saved]                       // ❌
tabs.getOrNull(saved) ?: tabs[0]  // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEwNzogbGlzdFtpXSBuZ2_DoGkgcGjhuqFtIHZpIOKGkiBJbmRleE91dE9mQm91bmRzRXhjZXB0aW9uCmZ1biBtYWluKCkgewogICAgdmFsIHRhYnMgPSBsaXN0T2YoIkhvbWUiLCAiU2VhcmNoIiwgIlByb2ZpbGUiKQogICAgdmFsIHNhdmVkID0gMyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gaW5kZXggbMawdSB04burIGLhuqNuIGFwcCBjxakgY8OzIDQgdGFiCiAgICBwcmludGxuKHRhYnMuZ2V0T3JOdWxsKHNhdmVkKSA_OiB0YWJzWzBdKSAgICAgICAvLyDinIUKICAgIHByaW50bG4odGFicy5nZXRPckVsc2Uoc2F2ZWQpIHsgIkhvbWUiIH0pICAgICAgIC8vIOKchQogICAgcHJpbnRsbih0YWJzW3NhdmVkXSkgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIGNyYXNoCn0K) · Xem thêm: K132, K140

### K108 · `max()` / `min()` trên list rỗng

`🟢 mới học` · `Kotlin`

**Gặp:** `java.util.NoSuchElementException` khi gọi `max()`, `min()`, `maxBy { }`, `minBy { }` lúc chưa có dữ liệu (chuyến đi chưa có điểm GPS nào).

**Vì sao:** từ Kotlin 1.7, `max()` không trả `null` nữa mà ném lỗi khi rỗng. Bản trả `null` đổi tên thành `maxOrNull()`.

**Sửa:** `maxOrNull()`, `minOrNull()`, `maxByOrNull { }`, `maxOfOrNull { }` rồi `?:`.

```kotlin
val top = speeds.max()             // ❌
val top = speeds.maxOrNull() ?: 0  // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEwODogbWF4KCkgLyBtaW4oKSB0csOqbiBsaXN0IHLhu5duZyDihpIgTm9TdWNoRWxlbWVudEV4Y2VwdGlvbgpmdW4gbWFpbigpIHsKICAgIHZhbCBzcGVlZHMgPSBsaXN0T2Y8SW50PigpCiAgICBwcmludGxuKHNwZWVkcy5tYXhPck51bGwoKSA_OiAwKSAgIC8vIOKchQogICAgcHJpbnRsbihzcGVlZHMubWF4KCkpICAgICAgICAgICAgICAvLyDinYwgY3Jhc2gKfQo) · Xem thêm: K105, K109

### K109 · `reduce` trên list rỗng

`🟡 đã làm app` · `Kotlin`

**Gặp:** `java.lang.UnsupportedOperationException: Empty collection can't be reduced.`

**Vì sao:** `reduce` lấy phần tử đầu làm giá trị khởi đầu. Không có phần tử nào thì không có gì để bắt đầu.

**Sửa:** dùng `fold(giáTrịĐầu) { }`, hoặc `sum()` / `sumOf { }` khi chỉ cộng.

```kotlin
bills.reduce { acc, x -> acc + x }   // ❌
bills.fold(0) { acc, x -> acc + x }  // ✅ 0 khi rỗng
bills.sum()                          // ✅ gọn hơn
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEwOTogcmVkdWNlIHRyw6puIGxpc3QgcuG7l25nIOKGkiBVbnN1cHBvcnRlZE9wZXJhdGlvbkV4Y2VwdGlvbgpmdW4gbWFpbigpIHsKICAgIHZhbCBiaWxscyA9IGxpc3RPZjxJbnQ-KCkKICAgIHByaW50bG4oYmlsbHMuZm9sZCgwKSB7IGFjYywgeCAtPiBhY2MgKyB4IH0pICAgICAvLyDinIUgMCwgY8OzIGdpw6EgdHLhu4sga2jhu59pIMSR4bqndQogICAgcHJpbnRsbihiaWxscy5yZWR1Y2UgeyBhY2MsIHggLT4gYWNjICsgeCB9KSAgICAgICAvLyDinYwgY3Jhc2gKfQo) · Xem thêm: K108

### K110 · `sum()` của `List<Int>` tràn số mà không báo

`🟡 đã làm app` · `Kotlin`

**Gặp:** tổng ra số âm hoặc số nhỏ vô lý (`-1294967296`) khi cộng dung lượng file, tổng tiền VND, tổng mét đi được.

**Vì sao:** `List<Int>.sum()` trả `Int`. Vượt `Int.MAX_VALUE` (≈ 2,1 tỉ) là quay vòng sang số âm, không có exception.

**Sửa:** cộng bằng `Long`: `sumOf { it.toLong() }`, hoặc lưu dữ liệu là `Long` ngay từ đầu (tiền tính bằng đồng / cent).

```kotlin
bytes.sum()                     // ❌ -1294967296
bytes.sumOf { it.toLong() }     // ✅ 3000000000
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzExMDogc3VtKCkgY-G7p2EgTGlzdDxJbnQ-IHRyw6BuIEludCBtw6Aga2jDtG5nIGLDoW8gbOG7l2kKZnVuIG1haW4oKSB7CiAgICB2YWwgYnl0ZXMgPSBMaXN0KDMpIHsgMV8wMDBfMDAwXzAwMCB9ICAgICAgICAgICAgICAvLyAzIGZpbGUsIG3hu5dpIGZpbGUgMSBHQgogICAgcHJpbnRsbihieXRlcy5zdW0oKSkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIC0xMjk0OTY3Mjk2CiAgICBwcmludGxuKGJ5dGVzLnN1bU9mIHsgaXQudG9Mb25nKCkgfSkgICAgICAgICAgICAgICAvLyDinIUgMzAwMDAwMDAwMAp9Cg) · Xem thêm: [Cú pháp & kiểu cơ bản](01-basics.md)

### K111 · `average()` của list rỗng là `NaN`

`🟢 mới học` · `Kotlin`

**Gặp:** UI hiện chữ `NaN sao` hoặc `NaN km/h`. Không crash nên lâu mới phát hiện.

**Vì sao:** `average()` = tổng / số phần tử = `0.0 / 0`, theo chuẩn số thực là `NaN`. Khác `max()` (K108), nó không ném lỗi.

**Sửa:** kiểm `isEmpty()` trước và hiện chữ riêng ("chưa có đánh giá"). Đã lỡ có số thì kiểm `avg.isNaN()`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzExMTogYXZlcmFnZSgpIGPhu6dhIGxpc3QgcuG7l25nIGzDoCBOYU4sIGtow7RuZyBwaOG6o2kgMCB2w6Aga2jDtG5nIGNyYXNoCmZ1biBtYWluKCkgewogICAgdmFsIHJhdGluZ3MgPSBlbXB0eUxpc3Q8SW50PigpCiAgICB2YWwgYXZnID0gcmF0aW5ncy5hdmVyYWdlKCkKICAgIHByaW50bG4oYXZnKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIE5hTgogICAgcHJpbnRsbigiJS4xZiBzYW8iLmZvcm1hdChhdmcpKSAgICAgICAgICAgICAgICAgICAvLyDinYwgIk5hTiBzYW8iIGhp4buHbiBsw6puIFVJCiAgICBwcmludGxuKGlmIChyYXRpbmdzLmlzRW1wdHkoKSkgImNoxrBhIGPDsyDEkcOhbmggZ2nDoSIgZWxzZSAiJS4xZiBzYW8iLmZvcm1hdChhdmcpKSAgLy8g4pyFCn0K) · Xem thêm: K108

### K112 · `sortedBy` xong mà list không đổi thứ tự

`🟢 mới học` · `Kotlin`

**Gặp:** gọi `notes.sortedBy { it.updated }` rồi in `notes`, thứ tự vẫn như cũ.

**Vì sao:** các hàm có đuôi `-ed` (`sorted`, `sortedBy`, `reversed`, `shuffled`) **trả về list mới**. Kết quả không gán vào đâu thì mất.
Hàm không đuôi (`sort`, `sortBy`, `reverse`, `shuffle`) mới sửa tại chỗ, và chỉ có trên `MutableList`.

**Sửa:** gán kết quả (`val sorted = notes.sortedBy { }`), hoặc dùng `sortBy { }` nếu đúng là muốn sửa `MutableList` tại chỗ.

```kotlin
notes.sortedBy { it.updated }              // ❌ kết quả bị bỏ
val byTime = notes.sortedBy { it.updated } // ✅
notes.sortBy { it.updated }                // ✅ (MutableList)
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzExMjogc29ydGVkQnkgdHLhuqMgbGlzdCBN4buaSTsgZ-G7jWkgeG9uZyBi4buPIGvhur90IHF14bqjIHRow6wgbGlzdCBn4buRYyBraMO0bmcgxJHhu5VpCmRhdGEgY2xhc3MgTm90ZSh2YWwgdGl0bGU6IFN0cmluZywgdmFsIHVwZGF0ZWQ6IEludCkKZnVuIG1haW4oKSB7CiAgICB2YWwgbm90ZXMgPSBtdXRhYmxlTGlzdE9mKE5vdGUoIkIiLCAxKSwgTm90ZSgiQSIsIDMpLCBOb3RlKCJDIiwgMikpCiAgICBub3Rlcy5zb3J0ZWRCeSB7IGl0LnVwZGF0ZWQgfSAgICAgICAgICAgICAgICAgICAgICAvLyDinYwga-G6v3QgcXXhuqMgYuG7iyB24bupdAogICAgcHJpbnRsbihub3Rlcy5tYXAgeyBpdC50aXRsZSB9KSAgICAgICAgICAgICAgICAgICAgLy8gW0IsIEEsIENdCiAgICBub3Rlcy5zb3J0QnkgeyBpdC51cGRhdGVkIH0gICAgICAgICAgICAgICAgICAgICAgICAvLyDinIUgc-G6r3AgdOG6oWkgY2jhu5cgKE11dGFibGVMaXN0KQogICAgcHJpbnRsbihub3Rlcy5tYXAgeyBpdC50aXRsZSB9KSAgICAgICAgICAgICAgICAgICAgLy8gW0IsIEMsIEFdCn0K) · Xem thêm: K113, K115

### K113 · `Unresolved reference 'sort'` trên `List`

`🟢 mới học` · `Kotlin`

**Gặp:** `Unresolved reference 'sort' on receiver of type 'List<String>'.`

**Vì sao:** `sort()`, `sortBy { }`, `shuffle()` sửa list tại chỗ nên chỉ có trên `MutableList`. `listOf(...)` là `List`.

**Sửa:** `val sorted = names.sorted()`. Hoặc khai báo `mutableListOf` nếu list đúng là cần sửa.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzExMzogc29ydEJ5IC8gc29ydCBjaOG7iSBjw7MgdHLDqm4gTXV0YWJsZUxpc3QsIGtow7RuZyBjw7MgdHLDqm4gTGlzdApmdW4gbWFpbigpIHsKICAgIHZhbCBuYW1lczogTGlzdDxTdHJpbmc-ID0gbGlzdE9mKCJUw7oiLCAiQW4iLCAiQsOsbmgiKQogICAgbmFtZXMuc29ydCgpICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIGNvbXBpbGUgZXJyb3IKICAgIC8vIOKchSB2YWwgc29ydGVkID0gbmFtZXMuc29ydGVkKCkKICAgIHByaW50bG4obmFtZXMpCn0K) · Xem thêm: K112

### K114 · `+=` trên `var` kiểu `List` copy cả list mỗi lần

`🟡 đã làm app` · `Kotlin`

**Gặp:** vòng lặp thêm vài chục nghìn phần tử chạy chậm bất thường; hoặc chỗ khác giữ tham chiếu tới list cũ không thấy phần tử mới.

**Vì sao:** với `var log: List<String>`, `log += x` nghĩa là `log = log + x`: tạo list **mới** chép toàn bộ phần tử cũ. Thêm n phần tử là O(n²).
Với `val xs: MutableList`, `+=` là `plusAssign`, thêm tại chỗ.

**Sửa:** gom nhiều phần tử thì dùng `val` + `MutableList` (hoặc `buildList { }`), xong rồi mới đưa ra ngoài dạng `List`.
Giữ `var` + `List` cho state đổi thưa (vd giá trị của `StateFlow`), vì ở đó bạn **muốn** có object mới.

```kotlin
var big: List<Int> = emptyList(); repeat(20_000) { big += it }   // ❌ ~ vài trăm ms
val fast = buildList { repeat(20_000) { add(it) } }              // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzExNDogYCs9YCB0csOqbiBgdmFyIHhzOiBMaXN0YCB04bqhbyBsaXN0IE3hu5pJIG3hu5dpIGzhuqduIChjb3B5IGPhuqMgbGlzdCksIGtow7RuZyB0aMOqbSB2w6BvIGxpc3QgY8WpCmZ1biBtYWluKCkgewogICAgdmFyIGxvZzogTGlzdDxTdHJpbmc-ID0gbGlzdE9mKCJzdGFydCIpCiAgICB2YWwgc2hvd25PblNjcmVlbiA9IGxvZyAgICAgICAgICAgICAgICAgLy8gY2jhu5cga2jDoWMgZ2nhu68gdGhhbSBjaGnhur91IGPFqQogICAgbG9nICs9ICJzdGVwIDEiICAgICAgICAgICAgICAgICAgICAgICAgICAvLyA9IGxvZyA9IGxvZyArICJzdGVwIDEiCiAgICBwcmludGxuKHNob3duT25TY3JlZW4pICAgICAgICAgICAgICAgICAgIC8vIFtzdGFydF0g4oCUIGtow7RuZyB0aOG6pXkgInN0ZXAgMSIKICAgIHByaW50bG4obG9nID09PSBzaG93bk9uU2NyZWVuKSAgICAgICAgICAgLy8gZmFsc2U6IMSRw6MgbMOgIG9iamVjdCBraMOhYwoKICAgIHZhciBiaWc6IExpc3Q8SW50PiA9IGVtcHR5TGlzdCgpCiAgICB2YWwgdCA9IFN5c3RlbS5uYW5vVGltZSgpCiAgICByZXBlYXQoMjBfMDAwKSB7IGJpZyArPSBpdCB9ICAgICAgICAgICAgIC8vIOKdjCBt4buXaSBs4bqnbiBjb3B5IGPhuqMgbGlzdCDihpIgTyhuwrIpCiAgICBwcmludGxuKCJMaXN0ICsgdmFyOiAkeyhTeXN0ZW0ubmFub1RpbWUoKSAtIHQpIC8gMV8wMDBfMDAwfSBtcyIpCgogICAgdmFsIGZhc3QgPSBtdXRhYmxlTGlzdE9mPEludD4oKQogICAgdmFsIHQyID0gU3lzdGVtLm5hbm9UaW1lKCkKICAgIHJlcGVhdCgyMF8wMDApIHsgZmFzdCArPSBpdCB9ICAgICAgICAgICAgLy8g4pyFIHZhbCArIE11dGFibGVMaXN0OiB0aMOqbSB04bqhaSBjaOG7lwogICAgcHJpbnRsbigiTXV0YWJsZUxpc3Q6ICR7KFN5c3RlbS5uYW5vVGltZSgpIC0gdDIpIC8gMV8wMDBfMDAwfSBtcyIpCn0K) · Xem thêm: K115

### K115 · `list + x` tưởng là thêm vào list

`🟢 mới học` · `Kotlin`

**Gặp:** viết `base + "EUR"` (hoặc `base.plus("EUR")`) rồi in `base`, không thấy `"EUR"`.

**Vì sao:** `+` trên collection không bao giờ sửa list gốc, nó trả về list mới. Câu lệnh đứng một mình thì kết quả bị bỏ, compiler không cảnh báo.

**Sửa:** gán kết quả (`val all = base + "EUR"`), hoặc dùng `MutableList` với `add` / `+=`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzExNTogYGxpc3QgKyB4YCB0cuG6oyB24buBIGxpc3QgbeG7m2ksIGtow7RuZyB0aMOqbSB2w6BvIGxpc3QgY8WpCmZ1biBtYWluKCkgewogICAgdmFsIGJhc2UgPSBsaXN0T2YoIlZORCIsICJVU0QiKQogICAgYmFzZSArICJFVVIiICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIGvhur90IHF14bqjIGLhu4sgYuG7jywgY29tcGlsZXIga2jDtG5nIGPhuqNuaCBiw6FvCiAgICBwcmludGxuKGJhc2UpICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBbVk5ELCBVU0RdCiAgICB2YWwgYWxsID0gYmFzZSArICJFVVIiICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinIUgZ2nhu68ga-G6v3QgcXXhuqMKICAgIHByaW50bG4oYWxsKQp9Cg) · Xem thêm: K112, K114

### K116 · So sánh hai mảng bằng `==` ra `false`

`🟢 mới học` · `Kotlin`

**Gặp:** hai `ByteArray` / `IntArray` cùng nội dung mà `a == b` là `false`; test so sánh gói tin BLE luôn fail.

**Vì sao:** mảng trên JVM không override `equals`, nên `==` so địa chỉ. `List` thì so nội dung.

**Sửa:** `a contentEquals b`; hoặc đổi sang `List` khi không cần hiệu năng của mảng.

```kotlin
a == b              // ❌ false
a contentEquals b   // ✅ true
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzExNjogc28gc8OhbmggaGFpIG3huqNuZyBi4bqxbmcgPT0gbMOgIHNvIHPDoW5oIMSR4buLYSBjaOG7iSwga2jDtG5nIHBo4bqjaSBu4buZaSBkdW5nCmZ1biBtYWluKCkgewogICAgdmFsIGEgPSBpbnRBcnJheU9mKDEsIDIsIDMpCiAgICB2YWwgYiA9IGludEFycmF5T2YoMSwgMiwgMykKICAgIHByaW50bG4oYSA9PSBiKSAgICAgICAgICAgICAgICAgLy8g4p2MIGZhbHNlCiAgICBwcmludGxuKGEgY29udGVudEVxdWFscyBiKSAgICAgIC8vIOKchSB0cnVlCiAgICBwcmludGxuKGxpc3RPZigxLCAyLCAzKSA9PSBsaXN0T2YoMSwgMiwgMykpICAvLyBMaXN0IHRow6wgc28gbuG7mWkgZHVuZzogdHJ1ZQp9Cg) · Xem thêm: K117, K155

### K117 · In mảng ra `[I@1b6d3586`

`🟢 mới học` · `Kotlin`

**Gặp:** log in ra `[B@76ed5528` hoặc `[I@...` thay vì nội dung mảng.

**Vì sao:** mảng dùng `toString()` mặc định của Java: tên kiểu + mã hash. `[B` là `byte[]`, `[I` là `int[]`.

**Sửa:** `contentToString()`, hoặc `joinToString()` khi cần định dạng riêng (hex cho dữ liệu OBD).

```kotlin
println(bytes)                                           // ❌ [B@76ed5528
println(bytes.contentToString())                         // ✅ [65, 13, 60]
println(bytes.joinToString(" ") { "%02X".format(it) })   // ✅ 41 0D 3C
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzExNzogcHJpbnRsbiht4bqjbmcpIGluIHJhIGtp4buDdSBbSUAxYjZkMzU4NiB0aGF5IHbDrCBu4buZaSBkdW5nCmZ1biBtYWluKCkgewogICAgdmFsIGJ5dGVzID0gYnl0ZUFycmF5T2YoMHg0MSwgMHgwRCwgMHgzQykKICAgIHByaW50bG4oYnl0ZXMpICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgW0JALi4uCiAgICBwcmludGxuKGJ5dGVzLmNvbnRlbnRUb1N0cmluZygpKSAgICAgICAgICAgICAgICAgLy8g4pyFIFs2NSwgMTMsIDYwXQogICAgcHJpbnRsbihieXRlcy5qb2luVG9TdHJpbmcoIiAiKSB7ICIlMDJYIi5mb3JtYXQoaXQpIH0pICAvLyDinIUgNDEgMEQgM0MKfQo) · Xem thêm: K116

### K118 · `IntArray` không truyền được vào chỗ cần `Array<Int>`

`🟢 mới học` · `Kotlin`

**Gặp:** `Argument type mismatch: actual type is 'IntArray', but 'Array<Int>' was expected.`

**Vì sao:** `IntArray` là `int[]` (số nguyên thuỷ, không box). `Array<Int>` là `Integer[]` (mỗi phần tử là object). Hai kiểu khác nhau hoàn toàn.

**Sửa:** chọn một kiểu cho cả chuỗi hàm (thường là `IntArray` hoặc `List<Int>`). Bắt buộc đổi thì `toTypedArray()` / `toIntArray()`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzExODogSW50QXJyYXkgdsOgIEFycmF5PEludD4gbMOgIGhhaSBraeG7g3Uga2jDoWMgbmhhdQpmdW4gdG90YWwoeHM6IEFycmF5PEludD4pID0geHMuc3VtKCkKZnVuIG1haW4oKSB7CiAgICB2YWwgcmF3ID0gaW50QXJyYXlPZigxLCAyLCAzKQogICAgcHJpbnRsbih0b3RhbChyYXcpKSAgICAgICAgICAgICAgLy8g4p2MIGNvbXBpbGUgZXJyb3IKICAgIC8vIOKchSBwcmludGxuKHRvdGFsKHJhdy50b1R5cGVkQXJyYXkoKSkpICBob-G6t2MgxJHhu5VpIHRoYW0gc-G7kSB0aMOgbmggSW50QXJyYXkKfQo)

### K119 · Lưới 2 chiều dùng chung một hàng

`🟡 đã làm app` · `Kotlin`

**Gặp:** sửa ô `[0][0]` thì ô đầu của **mọi** hàng đều đổi.

**Vì sao:** `List(3) { row }` gọi lambda 3 lần nhưng lần nào cũng trả cùng object `row`. Lưới có 3 tham chiếu tới một hàng.

**Sửa:** tạo hàng mới **bên trong** lambda.

```kotlin
val row = MutableList(3) { 0 }; val grid = List(3) { row }   // ❌
val grid = List(3) { MutableList(3) { 0 } }                 // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzExOTogdOG6oW8gbMaw4bubaSAyIGNoaeG7gXUgYuG6sW5nIGPDuW5nIG3hu5l0IGjDoG5nIOKGkiBz4butYSBt4buZdCDDtCwgY-G6oyBj4buZdCDEkeG7lWkKZnVuIG1haW4oKSB7CiAgICB2YWwgcm93ID0gTXV0YWJsZUxpc3QoMykgeyAwIH0KICAgIHZhbCBiYWQgPSBMaXN0KDMpIHsgcm93IH0gICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIDMgdGhhbSBjaGnhur91IHThu5tpIEPDmU5HIG3hu5l0IGjDoG5nCiAgICBiYWRbMF1bMF0gPSA5CiAgICBwcmludGxuKGJhZCkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIFtbOSwgMCwgMF0sIFs5LCAwLCAwXSwgWzksIDAsIDBdXQogICAgdmFsIGdvb2QgPSBMaXN0KDMpIHsgTXV0YWJsZUxpc3QoMykgeyAwIH0gfSAgICAgICAvLyDinIUgbeG7l2kgaMOgbmcgbeG7mXQgbGlzdCBt4bubaQogICAgZ29vZFswXVswXSA9IDkKICAgIHByaW50bG4oZ29vZCkKfQo) · Xem thêm: K148

### K120 · `map` trên `Map` trả về `List`, mất key

`🟢 mới học` · `Kotlin`

**Gặp:** `rates.map { it.value * 1.01 }` cho ra `List<Double>`; code phía sau cần `Map` thì báo `Type mismatch` / `Return type mismatch`.

**Vì sao:** `map` luôn trả `List`, kể cả khi gọi trên `Map`.

**Sửa:** `mapValues { }` (giữ key, đổi value), `mapKeys { }` (đổi key), hoặc `associate { }` khi đổi cả hai.

```kotlin
rates.map { it.value * 1.01 }                 // ❌ List<Double>
rates.mapValues { (_, v) -> v * 1.01 }        // ✅ Map<String, Double>
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEyMDogbWFwIHRyw6puIE1hcCB0cuG6oyB24buBIExpc3QsIGtow7RuZyBwaOG6o2kgTWFwCmZ1biBtYWluKCkgewogICAgdmFsIHJhdGVzID0gbWFwT2YoIlVTRCIgdG8gMjZfMDAwLjAsICJFVVIiIHRvIDI4XzAwMC4wKQogICAgdmFsIGJhZCA9IHJhdGVzLm1hcCB7IGl0LnZhbHVlICogMS4wMSB9ICAgICAgICAgICAgICAgICAgICAvLyDinYwgTGlzdDxEb3VibGU-LCBt4bqldCBrZXkKICAgIHByaW50bG4oYmFkKQogICAgdmFsIGdvb2QgPSByYXRlcy5tYXBWYWx1ZXMgeyAoXywgdikgLT4gdiAqIDEuMDEgfSAgICAgICAgICAvLyDinIUgTWFwPFN0cmluZywgRG91YmxlPgogICAgcHJpbnRsbihnb29kKQp9Cg)

### K121 · `map[key]` là kiểu nullable

`🟢 mới học` · `Kotlin`

**Gặp:** `Operator call is prohibited on a nullable receiver of type 'Int?'. Use '?.'-qualified call instead.` (hoặc
`Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'Int?'` khi gọi hàm).

**Vì sao:** key có thể không có trong map, nên `map[key]` trả `V?`.

**Sửa:** `(map[key] ?: 0) + 1`, hoặc `getOrDefault(key, 0)`. Chỉ dùng `getValue(key)` khi chắc chắn key có (xem K122). Tránh `!!`.

```kotlin
stock["apple"] + 1            // ❌
(stock["apple"] ?: 0) + 1     // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEyMTogbWFwW2tleV0gdHLhuqMgduG7gSBraeG7g3UgbnVsbGFibGUKZnVuIG1haW4oKSB7CiAgICB2YWwgc3RvY2sgPSBtYXBPZigiYXBwbGUiIHRvIDMpCiAgICB2YWwgbiA9IHN0b2NrWyJhcHBsZSJdICsgMSAgICAgICAgLy8g4p2MIGNvbXBpbGUgZXJyb3IKICAgIC8vIOKchSB2YWwgbiA9IChzdG9ja1siYXBwbGUiXSA_OiAwKSArIDEKICAgIHByaW50bG4obikKfQo) · Xem thêm: K122, [Null safety](02-null-safety.md)

### K122 · `getValue` với key không có

`🟡 đã làm app` · `Kotlin`

**Gặp:** `java.util.NoSuchElementException: Key JPY is missing in the map.`

**Vì sao:** `getValue` là "tôi chắc chắn key có", giống `!!` cho map. Bảng tỉ giá cũ trong cache không có tiền tệ mới là crash.

**Sửa:** `map[key] ?: giáTrịThayThế`, hoặc trả về một trạng thái riêng ("chưa có tỉ giá") cho UI.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEyMjogZ2V0VmFsdWUga2hpIGtleSBraMO0bmcgY8OzIOKGkiBOb1N1Y2hFbGVtZW50RXhjZXB0aW9uCmZ1biBtYWluKCkgewogICAgdmFsIHJhdGVzID0gbWFwT2YoIlVTRCIgdG8gMjZfMDAwLjApCiAgICBwcmludGxuKHJhdGVzWyJKUFkiXSA_OiAiY2jGsGEgY8OzIHThu4kgZ2nDoSIpICAgICAvLyDinIUKICAgIHByaW50bG4ocmF0ZXMuZ2V0VmFsdWUoIkpQWSIpKSAgICAgICAgICAgICAgICAgLy8g4p2MIGNyYXNoCn0K) · Xem thêm: K121, K127

### K123 · `mapOf` có key trùng, cặp sau ghi đè lặng lẽ

`🟡 đã làm app` · `Kotlin`

**Gặp:** bảng mã lỗi / bảng dịch viết tay có 120 dòng mà `size` chỉ 119; một mã hiện sai mô tả.

**Vì sao:** `mapOf("a" to 1, "a" to 2)` hợp lệ, cặp sau thắng. Compiler không kiểm key trùng.

**Sửa:** với bảng viết tay dài, thêm test `assertEquals(soDong, map.size)`, hoặc dựng từ list rồi kiểm trùng:
`list.groupBy { it.first }.filterValues { it.size > 1 }`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEyMzogbWFwT2YgduG7m2kga2V5IHRyw7luZzogY-G6t3Agc2F1IGdoaSDEkcOoIGPhurdwIHRyxrDhu5tjLCBraMO0bmcgYsOhbyBnw6wKZnVuIG1haW4oKSB7CiAgICB2YWwgY29kZXMgPSBtYXBPZigKICAgICAgICAiUDAzMDAiIHRvICJC4buPIG3DoXkgbmfhuqt1IG5oacOqbiIsCiAgICAgICAgIlAwMTcxIiB0byAiSOG7l24gaOG7o3Agbmdow6hvIiwKICAgICAgICAiUDAzMDAiIHRvICJDb3B5IG5o4bqnbSBkw7JuZyIsICAgICAvLyDinYwgZ2hpIMSRw6ggbOG6t25nIGzhur0KICAgICkKICAgIHByaW50bG4oY29kZXMuc2l6ZSkgICAgICAgICAgICAgICAgICAgLy8gMiwga2jDtG5nIHBo4bqjaSAzCiAgICBwcmludGxuKGNvZGVzWyJQMDMwMCJdKQp9Cg) · Xem thêm: K124

### K124 · `associateBy` làm mất phần tử có key trùng

`🟡 đã làm app` · `Kotlin`

**Gặp:** gom chuyến đi theo ngày bằng `associateBy { it.day }`, ngày có hai chuyến chỉ còn một.

**Vì sao:** `associateBy`, `associate`, `toMap` tạo `Map` một-key-một-value: key trùng thì phần tử **cuối** thắng.

**Sửa:** `groupBy { }` cho ra `Map<K, List<V>>`. Cần đếm hoặc cộng thì `groupBy` rồi `mapValues`, hoặc `groupingBy { }.eachCount()`.

```kotlin
trips.associateBy { it.day }    // ❌ {T2=Trip(T2, 30), ...} mất chuyến 12 km
trips.groupBy { it.day }        // ✅ {T2=[Trip(T2, 12), Trip(T2, 30)], ...}
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEyNDogYXNzb2NpYXRlQnkgZ2nhu68gcGjhuqduIHThu60gQ1Xhu5BJIGtoaSBrZXkgdHLDuW5nOyBtdeG7kW4gZ2nhu68gaOG6v3QgdGjDrCBncm91cEJ5CmRhdGEgY2xhc3MgVHJpcCh2YWwgZGF5OiBTdHJpbmcsIHZhbCBrbTogSW50KQpmdW4gbWFpbigpIHsKICAgIHZhbCB0cmlwcyA9IGxpc3RPZihUcmlwKCJUMiIsIDEyKSwgVHJpcCgiVDIiLCAzMCksIFRyaXAoIlQzIiwgNSkpCiAgICBwcmludGxuKHRyaXBzLmFzc29jaWF0ZUJ5IHsgaXQuZGF5IH0pICAgICAgICAvLyDinYwgbeG6pXQgY2h1eeG6v24gMTIga20KICAgIHByaW50bG4odHJpcHMuZ3JvdXBCeSB7IGl0LmRheSB9KSAgICAgICAgICAgIC8vIOKchSBNYXA8U3RyaW5nLCBMaXN0PFRyaXA-PgogICAgcHJpbnRsbih0cmlwcy5ncm91cEJ5KHsgaXQuZGF5IH0sIHsgaXQua20gfSkubWFwVmFsdWVzIHsgaXQudmFsdWUuc3VtKCkgfSkgIC8vIHThu5VuZyBrbSB0aGVvIG5nw6B5Cn0K) · Xem thêm: K123

### K125 · Gán vào `Map` chỉ đọc

`🟢 mới học` · `Kotlin`

**Gặp:** `No 'set' operator method providing array access.` ở dòng `settings["theme"] = "dark"`.

**Vì sao:** `mapOf` trả `Map`, không có `set`. `map[k] = v` cần `MutableMap`.

**Sửa:** `mutableMapOf(...)`, hoặc tạo map mới: `val next = settings + ("theme" to "dark")`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEyNTogZ8OhbiB2w6BvIE1hcCBjaOG7iSDEkeG7jWMKZnVuIG1haW4oKSB7CiAgICB2YWwgc2V0dGluZ3MgPSBtYXBPZigidGhlbWUiIHRvICJsaWdodCIpCiAgICBzZXR0aW5nc1sidGhlbWUiXSA9ICJkYXJrIiAgICAgICAgICAgLy8g4p2MIGNvbXBpbGUgZXJyb3IKICAgIC8vIOKchSB2YWwgc2V0dGluZ3MgPSBtdXRhYmxlTWFwT2YoInRoZW1lIiB0byAibGlnaHQiKQogICAgLy8gICAgaG_hurdjIHThuqFvIG1hcCBt4bubaTogdmFsIG5leHQgPSBzZXR0aW5ncyArICgidGhlbWUiIHRvICJkYXJrIikKICAgIHByaW50bG4oc2V0dGluZ3MpCn0K) · Xem thêm: K113

### K126 · `getOrElse` tưởng là có cache

`🟡 đã làm app` · `Kotlin`

**Gặp:** hàm tính tốn kém (parse, gọi DB) vẫn chạy mỗi lần dù đã "cache" bằng map.

**Vì sao:** `getOrElse(key) { }` chỉ **trả về** giá trị thay thế, không lưu vào map. `getOrPut(key) { }` mới tính một lần rồi `put`.

**Sửa:** dùng `getOrPut` trên `MutableMap`.

```kotlin
cache.getOrElse("kotlin") { slowLength("kotlin") }   // ❌ tính lại mỗi lần
cache.getOrPut("kotlin") { slowLength("kotlin") }    // ✅ tính 1 lần
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEyNjogZ2V0T3JFbHNlIEtIw5RORyB0aMOqbSB2w6BvIG1hcDsgbXXhu5FuIGNhY2hlIHRow6wgZMO5bmcgZ2V0T3JQdXQKZnVuIG1haW4oKSB7CiAgICB2YWwgY2FjaGUgPSBtdXRhYmxlTWFwT2Y8U3RyaW5nLCBJbnQ-KCkKICAgIHZhciBjYWxscyA9IDAKICAgIGZ1biBzbG93TGVuZ3RoKHM6IFN0cmluZyk6IEludCB7IGNhbGxzKys7IHJldHVybiBzLmxlbmd0aCB9CgogICAgcmVwZWF0KDMpIHsgY2FjaGUuZ2V0T3JFbHNlKCJrb3RsaW4iKSB7IHNsb3dMZW5ndGgoImtvdGxpbiIpIH0gfSAgIC8vIOKdjCB0w61uaCBs4bqhaSBj4bqjIDMgbOG6p24KICAgIHByaW50bG4oImdldE9yRWxzZTogY2FsbHM9JGNhbGxzLCBjYWNoZT0kY2FjaGUiKQoKICAgIGNhbGxzID0gMAogICAgcmVwZWF0KDMpIHsgY2FjaGUuZ2V0T3JQdXQoImtvdGxpbiIpIHsgc2xvd0xlbmd0aCgia290bGluIikgfSB9ICAgIC8vIOKchSB0w61uaCAxIGzhuqduIHLhu5NpIGzGsHUKICAgIHByaW50bG4oImdldE9yUHV0OiAgY2FsbHM9JGNhbGxzLCBjYWNoZT0kY2FjaGUiKQp9Cg)

### K127 · `withDefault` không áp dụng cho `map[key]`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** đã `withDefault { 0 }` mà `counts["b"]` vẫn ra `null`.

**Vì sao:** giá trị mặc định của `withDefault` chỉ được dùng bởi `getValue()` (và property delegate `by map`). `get` / `[]` vẫn theo hợp đồng
của `Map`: không có thì `null`.

**Sửa:** gọi `getValue(key)`; hoặc bỏ `withDefault`, viết thẳng `map[key] ?: 0` cho dễ đọc.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEyNzogd2l0aERlZmF1bHQgY2jhu4kgY8OzIHTDoWMgZOG7pW5nIHbhu5tpIGdldFZhbHVlLCBraMO0bmcgduG7m2kgbWFwW2tleV0KZnVuIG1haW4oKSB7CiAgICB2YWwgY291bnRzID0gbWFwT2YoImEiIHRvIDEpLndpdGhEZWZhdWx0IHsgMCB9CiAgICBwcmludGxuKGNvdW50c1siYiJdKSAgICAgICAgICAgIC8vIOKdjCBudWxsIOKAlCBbXSBi4buPIHF1YSBkZWZhdWx0CiAgICBwcmludGxuKGNvdW50cy5nZXRWYWx1ZSgiYiIpKSAgIC8vIOKchSAwCn0K) · Xem thêm: K122

### K128 · `hashMapOf` / `HashSet` đảo thứ tự

`🟡 đã làm app` · `Kotlin`

**Gặp:** danh sách hiện lên UI bị đảo lung tung so với thứ tự thêm vào; mỗi phiên bản Java/ART lại khác.

**Vì sao:** `HashMap` / `HashSet` xếp theo mã hash. `mutableMapOf()`, `mapOf()`, `setOf()`, `mutableSetOf()` là bản `Linked...`, giữ thứ tự thêm.

**Sửa:** dùng `mutableMapOf` / `mutableSetOf` khi thứ tự quan trọng; cần sắp theo key thì `toSortedMap()`. Đừng dựa vào thứ tự của `HashMap`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEyODogaGFzaE1hcE9mIC8gSGFzaFNldCBraMO0bmcgZ2nhu68gdGjhu6kgdOG7sTsgbXV0YWJsZU1hcE9mIChMaW5rZWRIYXNoTWFwKSB0aMOsIGdp4buvIHRo4bupIHThu7EgdGjDqm0gdsOgbwpmdW4gbWFpbigpIHsKICAgIHZhbCBrZXlzID0gbGlzdE9mKCJ6ZXRhIiwgImFscGhhIiwgIm1pa2UiLCAiYnJhdm8iLCAieC1yYXkiLCAiZGVsdGEiKQogICAgdmFsIGhhc2ggPSBoYXNoTWFwT2Y8U3RyaW5nLCBJbnQ-KCkKICAgIHZhbCBsaW5rZWQgPSBtdXRhYmxlTWFwT2Y8U3RyaW5nLCBJbnQ-KCkKICAgIGtleXMuZm9yRWFjaEluZGV4ZWQgeyBpLCBrIC0-IGhhc2hba10gPSBpOyBsaW5rZWRba10gPSBpIH0KICAgIHByaW50bG4oImhhc2hNYXBPZjogICAgJHtoYXNoLmtleXN9IikgICAgLy8g4p2MIHRo4bupIHThu7EgdGhlbyBoYXNoLCBraMO0bmcgcGjhuqNpIHRo4bupIHThu7EgdGjDqm0KICAgIHByaW50bG4oIm11dGFibGVNYXBPZjogJHtsaW5rZWQua2V5c30iKSAgLy8g4pyFIMSRw7puZyB0aOG7qSB04buxIHRow6ptCn0K)

### K129 · Đổi field của object đang làm key trong `HashMap`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `map[key]` trả `null` dù vừa `put`; `map.size` vẫn đếm phần tử đó, `remove` không xoá được.

**Vì sao:** map cất phần tử vào "ngăn" theo `hashCode()` lúc `put`. `data class` có `var`: đổi field là `hashCode` đổi, map tìm sai ngăn.
Tạo key mới bằng giá trị cũ thì đúng ngăn nhưng `equals` lại sai.

**Sửa:** key (và phần tử của `Set`) chỉ dùng `data class` toàn `val`. Cần đổi thì `remove` key cũ, `copy(...)`, rồi `put` lại.

```kotlin
data class Pos(var x: Int, var y: Int)   // ❌ làm key
data class Pos(val x: Int, val y: Int)   // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEyOTogxJHhu5VpIGZpZWxkIGPhu6dhIG9iamVjdCDEkWFuZyBsw6BtIGtleSB0cm9uZyBIYXNoTWFwIOKGkiBraMO0bmcgdMOsbSBs4bqhaSDEkcaw4bujYwpkYXRhIGNsYXNzIFBvcyh2YXIgeDogSW50LCB2YXIgeTogSW50KQpmdW4gbWFpbigpIHsKICAgIHZhbCBwID0gUG9zKDEsIDEpCiAgICB2YWwgdmlzaXRlZCA9IGhhc2hNYXBPZihwIHRvICLEkcOjIMSRaSBxdWEiKQogICAgcC54ID0gMiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgaGFzaENvZGUgxJHhu5VpLCBtYXAgduG6q24gY-G6pXQg4bufIG5nxINuIGPFqQogICAgcHJpbnRsbih2aXNpdGVkW3BdKSAgICAgICAgICAgICAgICAgICAvLyBudWxsCiAgICBwcmludGxuKHZpc2l0ZWRbUG9zKDEsIDEpXSkgICAgICAgICAgIC8vIGPFqW5nIG51bGw6IGhhc2ggxJHDum5nIG5nxINuIG5oxrBuZyBlcXVhbHMgc2FpCiAgICBwcmludGxuKHZpc2l0ZWQuc2l6ZSkgICAgICAgICAgICAgICAgIC8vIDEg4oCUIG7hurFtIMSRw7MgbcOgIGtow7RuZyBs4bqleSByYSDEkcaw4bujYwogICAgLy8g4pyFIFPhu61hOiBrZXkgZMO5bmcgZGF0YSBjbGFzcyB0b8OgbiBgdmFsYCwgbXXhu5FuIMSR4buVaSB0aMOsIGNvcHkoeCA9IDIpIHbDoCBwdXQgbOG6oWkKfQo) · Xem thêm: K154

### K130 · `distinct()` / `toSet()` không bỏ phần tử trùng

`🟢 mới học` · `Kotlin`

**Gặp:** list tag sau `distinct()` vẫn có hai `"kmp"`.

**Vì sao:** `distinct`, `toSet`, `contains` dựa vào `equals`/`hashCode`. Class thường dùng bản mặc định: so địa chỉ object.

**Sửa:** đổi thành `data class`; hoặc nói rõ tiêu chí trùng bằng `distinctBy { it.name }`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEzMDogZGlzdGluY3QoKSAvIHRvU2V0KCkga2jDtG5nIGLhu48gdHLDuW5nIHbhu5tpIGNsYXNzIHRoxrDhu51uZyAoa2jDtG5nIGPDsyBlcXVhbHMpCmNsYXNzIFRhZyh2YWwgbmFtZTogU3RyaW5nKSAgICAgICAgICAgICAgLy8g4p2MIHRow6ptIGBkYXRhYCBwaMOtYSB0csaw4bubYyBsw6AgaOG6v3QgbOG7l2kKZnVuIG1haW4oKSB7CiAgICB2YWwgdGFncyA9IGxpc3RPZihUYWcoImttcCIpLCBUYWcoImttcCIpLCBUYWcoImlvcyIpKQogICAgcHJpbnRsbih0YWdzLmRpc3RpbmN0KCkuc2l6ZSkgICAgICAgICAvLyAzLCBtb25nIMSR4bujaSAyCiAgICBwcmludGxuKHRhZ3MuZGlzdGluY3RCeSB7IGl0Lm5hbWUgfS5zaXplKSAgLy8g4pyFIDIsIG7hur91IGtow7RuZyDEkeG7lWkgxJHGsOG7o2MgY2xhc3MKfQo) · Xem thêm: K153

### K131 · `first { }` không tìm thấy

`🟢 mới học` · `Kotlin`

**Gặp:** `java.util.NoSuchElementException: Collection contains no element matching the predicate.`

**Vì sao:** `first { điềuKiện }` ném lỗi khi không phần tử nào khớp (dongle chưa bật, user đã bị xoá).

**Sửa:** `firstOrNull { }` hoặc `find { }` (hai hàm giống nhau). Cũng đừng viết `filter { }.first()`: vừa chậm vừa crash như nhau.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEzMTogZmlyc3QgeyB9IGtoaSBraMO0bmcgcGjhuqduIHThu60gbsOgbyBraOG7m3Ag4oaSIE5vU3VjaEVsZW1lbnRFeGNlcHRpb24KZGF0YSBjbGFzcyBEb25nbGUodmFsIG5hbWU6IFN0cmluZykKZnVuIG1haW4oKSB7CiAgICB2YWwgZm91bmQgPSBsaXN0T2YoRG9uZ2xlKCJPQkRJSSIpLCBEb25nbGUoIlZnYXRlIikpCiAgICBwcmludGxuKGZvdW5kLmZpcnN0T3JOdWxsIHsgaXQubmFtZSA9PSAiRUxNMzI3IiB9ID86ICJraMO0bmcgdGjhuqV5IikgIC8vIOKchSAoaG_hurdjIGZpbmQgeyB9KQogICAgcHJpbnRsbihmb3VuZC5maXJzdCB7IGl0Lm5hbWUgPT0gIkVMTTMyNyIgfSkgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIGNyYXNoCn0K) · Xem thêm: K105

### K132 · `indexOf` trả `-1` rồi đem đi làm index

`🟢 mới học` · `Kotlin`

**Gặp:** `IndexOutOfBoundsException: Index -1 out of bounds for length 2`.

**Vì sao:** `indexOf` / `indexOfFirst` trả `-1` khi không thấy, không ném lỗi. Lỗi nổ ra ở dòng **dùng** index, xa chỗ gây lỗi.

**Sửa:** kiểm `if (i >= 0)`, hoặc `getOrNull(i)`. Nhiều khi cái cần là `find { }` chứ không phải index.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEzMjogaW5kZXhPZiB0cuG6oyAtMSBraGkga2jDtG5nIGPDsywgxJFlbSAtMSBsw6BtIGluZGV4IGzDoCBjcmFzaApmdW4gbWFpbigpIHsKICAgIHZhbCB1bml0cyA9IGxpc3RPZigia20iLCAibWkiKQogICAgdmFsIGkgPSB1bml0cy5pbmRleE9mKCJtIikgICAgICAgICAgICAgICAgLy8gLTEKICAgIHByaW50bG4oaSkKICAgIHByaW50bG4odW5pdHMuZ2V0T3JOdWxsKGkpID86ICJrbSIpICAgICAgIC8vIOKchQogICAgcHJpbnRsbih1bml0c1tpXSkgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIGNyYXNoOiBJbmRleCAtMSBvdXQgb2YgYm91bmRzIGZvciBsZW5ndGggMgp9Cg) · Xem thêm: K107

### K133 · `subList` là view, sửa list gốc thì view hỏng

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `ConcurrentModificationException` khi đọc kết quả của `subList` sau khi list gốc được `add`/`remove`. Hoặc: sửa list gốc mà "bản cắt" đổi theo.

**Vì sao:** `subList(from, to)` không copy, nó là cửa sổ nhìn vào list gốc. Đổi phần tử thì view thấy; đổi kích thước list gốc thì view mất hiệu lực.

**Sửa:** cần bản độc lập thì `take(n)`, `drop(n)`, `slice(range)` hoặc `subList(...).toList()`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEzMzogc3ViTGlzdCBsw6AgdmlldyBj4bunYSBsaXN0IGfhu5FjOyB0aMOqbS94b8OhIOG7nyBsaXN0IGfhu5FjIHRow6wgdmlldyBo4buPbmcKZnVuIG1haW4oKSB7CiAgICB2YWwgYWxsID0gbXV0YWJsZUxpc3RPZigxLCAyLCAzLCA0LCA1KQogICAgdmFsIGZpcnN0VGhyZWUgPSBhbGwuc3ViTGlzdCgwLCAzKSAgICAgICAgLy8g4p2MIGtow7RuZyBwaOG6o2kgYuG6o24gY29weQogICAgLy8gdmFsIGZpcnN0VGhyZWUgPSBhbGwudGFrZSgzKSAgICAgICAgICAgLy8g4pyFIGLhuqNuIGNvcHkgxJHhu5ljIGzhuq1wCiAgICBhbGxbMF0gPSAxMDAKICAgIHByaW50bG4oZmlyc3RUaHJlZSkgICAgICAgICAgICAgICAgICAgICAgICAvLyBbMTAwLCAyLCAzXSDigJQgc2V0IHRow6wgdmlldyB0aOG6pXkgbHXDtG4KICAgIGFsbC5hZGQoNikgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyB0aGF5IMSR4buVaSBj4bqldSB0csO6YyBsaXN0IGfhu5FjCiAgICBwcmludGxuKGZpcnN0VGhyZWUpICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIENvbmN1cnJlbnRNb2RpZmljYXRpb25FeGNlcHRpb24KfQo) · Xem thêm: K101, K103

### K134 · `Sequence` không chạy gì cả

`🟢 mới học` · `Kotlin`

**Gặp:** viết `seq.map { println(it) }` hoặc `seq.onEach { save(it) }` mà không có log, không có gì được lưu.

**Vì sao:** `Sequence` lười: `map`, `filter`, `onEach` chỉ ghi lại các bước. Phải có thao tác cuối (`toList`, `first`, `count`, `sum`, `forEach`…)
thì mới chạy.

**Sửa:** thêm thao tác cuối. Chỉ muốn chạy hiệu ứng phụ thì dùng `forEach`, không dùng `map`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEzNDogU2VxdWVuY2UgbMaw4budaToga2jDtG5nIGPDsyB0aGFvIHTDoWMgY3Xhu5FpICh0b0xpc3QsIGZpcnN0LCBzdW0uLi4pIHRow6wga2jDtG5nIGNo4bqheSBnw6wgY-G6owpmdW4gbWFpbigpIHsKICAgIHZhbCBzZXEgPSBsaXN0T2YoMSwgMiwgMykuYXNTZXF1ZW5jZSgpLm1hcCB7IHByaW50bG4oIm1hcCAkaXQiKTsgaXQgKiAyIH0KICAgIHByaW50bG4oImNoxrBhIGluIGTDsm5nICdtYXAnIG7DoG8iKQogICAgcHJpbnRsbihzZXEudG9MaXN0KCkpICAgICAgICAgICAgICAgICAgICAgIC8vIOKchSBsw7pjIG7DoHkgbWFwIG3hu5tpIGNo4bqheQp9Cg) · Xem thêm: K136

### K135 · `This sequence can be consumed only once.`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `java.lang.IllegalStateException: This sequence can be consumed only once.` ở lần duyệt thứ hai.

**Vì sao:** `iterator.asSequence()`, `generateSequence { }` (bản không có seed), `BufferedReader.lineSequence()` đọc từ một nguồn chỉ đi một chiều.
Duyệt lần 2 (vd gọi `count()` rồi `toList()`) là hết dữ liệu, nên Kotlin chặn bằng exception.

**Sửa:** duyệt một lần và chuyển thành `List` (`val lines = seq.toList()`), rồi làm gì tiếp thì làm trên list.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEzNTogU2VxdWVuY2UgdOG6oW8gdOG7qyBpdGVyYXRvciBjaOG7iSBkdXnhu4d0IMSRxrDhu6NjIE3hu5hUIGzhuqduCmZ1biBtYWluKCkgewogICAgdmFsIGxpbmVzID0gbGlzdE9mKCI0MSAwRCAwMCIsICI0MSAwQyAxQSBGOCIpLml0ZXJhdG9yKCkKICAgIHZhbCBzZXEgPSBsaW5lcy5hc1NlcXVlbmNlKCkKICAgIHByaW50bG4oc2VxLmNvdW50KCkpICAgICAgICAgICAgICAgICAgICAgICAvLyAyCiAgICBwcmludGxuKHNlcS50b0xpc3QoKSkgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIElsbGVnYWxTdGF0ZUV4Y2VwdGlvbjogVGhpcyBzZXF1ZW5jZSBjYW4gYmUgY29uc3VtZWQgb25seSBvbmNlLgogICAgLy8g4pyFIMSR4buNYyBt4buZdCBs4bqnbiB2w6BvIGxpc3Q6IHZhbCBsaXN0ID0gbGluZXMuYXNTZXF1ZW5jZSgpLnRvTGlzdCgpCn0K) · Xem thêm: K134

### K136 · Thứ tự chạy của `Sequence` khác `List`

`🟡 đã làm app` · `Kotlin`

**Gặp:** log trong `map` / `filter` in xen kẽ thay vì "map hết rồi filter"; hoặc với `first()`, chỉ vài phần tử được `map`.

**Vì sao:** `List` làm từng bước cho **cả list** (tạo list trung gian mỗi bước). `Sequence` đưa **từng phần tử** qua hết các bước, và
dừng ngay khi thao tác cuối có đủ kết quả.

**Sửa:** không có gì sai, nhưng đừng đặt hiệu ứng phụ (ghi DB, gửi analytics) trong `map`/`filter` rồi trông chờ nó chạy cho mọi phần tử.
Dùng `Sequence` khi list lớn và có nhiều bước hoặc có `first`/`take`; list nhỏ thì `List` thường nhanh hơn.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEzNjogTGlzdCBsw6BtIHThu6tuZyBixrDhu5tjIGNobyBj4bqjIGxpc3Q7IFNlcXVlbmNlIMSRxrBhIHThu6tuZyBwaOG6p24gdOG7rSBxdWEgaOG6v3QgY8OhYyBixrDhu5tjCmZ1biBtYWluKCkgewogICAgcHJpbnRsbigiTGlzdDoiKQogICAgbGlzdE9mKDEsIDIsIDMpLm1hcCB7IHByaW50bG4oIiAgbWFwICRpdCIpOyBpdCAqIDEwIH0uZmlsdGVyIHsgcHJpbnRsbigiICBmaWx0ZXIgJGl0Iik7IGl0ID4gMTAgfS5maXJzdCgpCiAgICBwcmludGxuKCJTZXF1ZW5jZToiKQogICAgbGlzdE9mKDEsIDIsIDMpLmFzU2VxdWVuY2UoKS5tYXAgeyBwcmludGxuKCIgIG1hcCAkaXQiKTsgaXQgKiAxMCB9LmZpbHRlciB7IHByaW50bG4oIiAgZmlsdGVyICRpdCIpOyBpdCA-IDEwIH0uZmlyc3QoKQp9Cg) · Xem thêm: K134, K137

### K137 · `generateSequence` vô hạn + `sorted()` / `toList()` treo app

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** app treo (ANR trên Android) hoặc `OutOfMemoryError: Java heap space`, không có exception nào trước đó.

**Vì sao:** `generateSequence(1) { it + 1 }` không có điểm dừng. Thao tác cần đọc **hết** dữ liệu (`toList`, `sorted`, `count`, `last`,
`max`) chạy mãi. `sorted()` trên `Sequence` là lười giả: nó phải gom hết rồi mới sắp.

**Sửa:** luôn giới hạn trước: `take(n)`, `takeWhile { }`, hoặc cho lambda trả `null` để dừng.

```kotlin
generateSequence(1) { it + 1 }.sorted().take(3).toList()   // ❌ treo
generateSequence(1) { it + 1 }.take(3).toList()            // ✅ [1, 2, 3]
generateSequence(1) { if (it < 100) it * 2 else null }     // ✅ tự dừng
```

Xem thêm: K136

### K138 · `MutableList<Int>.remove(1)` xoá nhầm

`🟡 đã làm app` · `Kotlin`

**Gặp:** muốn xoá phần tử thứ hai mà lại xoá phần tử có **giá trị** 1 (hoặc không xoá gì vì không có số 1).

**Vì sao:** Kotlin tách rõ: `remove(element)` xoá theo giá trị, `removeAt(index)` xoá theo vị trí. Người quen Java (`list.remove(int)`
là xoá theo vị trí) rất hay nhầm.

**Sửa:** xoá theo vị trí luôn dùng `removeAt(i)`.

```kotlin
mutableListOf(5, 9, 1).remove(1)     // ❌ [5, 9]
mutableListOf(5, 9, 1).removeAt(1)   // ✅ [5, 1]
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEzODogTXV0YWJsZUxpc3Q8SW50Pi5yZW1vdmUoMSkgeG_DoSBQSOG6pk4gVOG7rCBjw7MgZ2nDoSB0cuG7iyAxLCBraMO0bmcgcGjhuqNpIHBo4bqnbiB04butIOG7nyB24buLIHRyw60gMQpmdW4gbWFpbigpIHsKICAgIHZhbCB6cyA9IG11dGFibGVMaXN0T2YoNSwgOSwgMSkKICAgIHpzLnJlbW92ZSgxKSAgICAgICAgICAgICAgICAgIC8vIOKdjCBtdeG7kW4geG_DoSB24buLIHRyw60gMSAoc-G7kSA5KSBuaMawbmcgbOG6oWkgeG_DoSBz4buRIDEKICAgIHByaW50bG4oenMpICAgICAgICAgICAgICAgICAgIC8vIFs1LCA5XQogICAgdmFsIHdzID0gbXV0YWJsZUxpc3RPZig1LCA5LCAxKQogICAgd3MucmVtb3ZlQXQoMSkgICAgICAgICAgICAgICAgLy8g4pyFIHhvw6EgdGhlbyB24buLIHRyw60g4oaSIFs1LCAxXQogICAgcHJpbnRsbih3cykKfQo)

### K139 · `break` / `continue` trong `forEach`

`🟢 mới học` · `Kotlin`

**Gặp:** `'break' and 'continue' are only allowed inside loops.`

**Vì sao:** `forEach { }` là một hàm nhận lambda, không phải vòng lặp của ngôn ngữ.

**Sửa:** muốn `continue` thì `return@forEach` (bỏ qua phần tử hiện tại). Muốn `break` thì viết vòng `for` thường; quy ước Kotlin cũng ưu tiên `for`
khi thân vòng có điều khiển luồng.

```kotlin
list.forEach { if (it == 2) continue; println(it) }        // ❌
list.forEach { if (it == 2) return@forEach; println(it) }   // ✅ như continue
for (x in list) { if (x == 4) break; println(x) }           // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzEzOTogYnJlYWsgLyBjb250aW51ZSB0cm9uZyBsYW1iZGEgY-G7p2EgZm9yRWFjaApmdW4gbWFpbigpIHsKICAgIGxpc3RPZigxLCAyLCAzLCA0KS5mb3JFYWNoIHsKICAgICAgICBpZiAoaXQgPT0gMikgY29udGludWUgICAgICAvLyDinYwgY29tcGlsZSBlcnJvcgogICAgICAgIHByaW50bG4oaXQpCiAgICB9CiAgICAvLyDinIUgY29udGludWUg4oaSIHJldHVybkBmb3JFYWNoIDsgY-G6p24gYnJlYWsgdGjDrCBkw7luZyB2w7JuZyBmb3I6CiAgICAvLyBmb3IgKHggaW4gbGlzdE9mKDEsIDIsIDMsIDQpKSB7IGlmICh4ID09IDIpIGNvbnRpbnVlOyBpZiAoeCA9PSA0KSBicmVhazsgcHJpbnRsbih4KSB9Cn0K) · Xem thêm: [Hàm, lambda, generics](05-functions.md) (`return` trong lambda)

### K140 · `for (i in 0..list.size)` dư một vòng

`🟢 mới học` · `Kotlin`

**Gặp:** `IndexOutOfBoundsException: Index 3 out of bounds for length 3` ở vòng cuối.

**Vì sao:** `a..b` là range **đóng**, gồm cả `b`. `0..size` có `size + 1` số.

**Sửa:** `for (i in list.indices)`, hoặc `0..<list.size` / `0 until list.size`. Không cần index thì `for (x in list)`; cần cả hai thì `withIndex()`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE0MDogMC4uc2l6ZSBsw6AgcmFuZ2UgxJDDk05HLCBkxrAgbeG7mXQgdsOybmcg4oaSIEluZGV4T3V0T2ZCb3VuZHNFeGNlcHRpb24KZnVuIG1haW4oKSB7CiAgICB2YWwgeHMgPSBsaXN0T2YoImEiLCAiYiIsICJjIikKICAgIGZvciAoaSBpbiB4cy5pbmRpY2VzKSBwcmludCh4c1tpXSkgICAgICAgLy8g4pyFIChob-G6t2MgMC4uPHhzLnNpemUsIDAgdW50aWwgeHMuc2l6ZSkKICAgIHByaW50bG4oKQogICAgZm9yIChpIGluIDAuLnhzLnNpemUpIHByaW50KHhzW2ldKSAgICAgICAvLyDinYwgaSA9IDMgdGjDrCBjcmFzaAp9Cg) · Xem thêm: K107, K141

### K141 · `for (i in 10..1)` không chạy lần nào

`🟢 mới học` · `Kotlin`

**Gặp:** vòng đếm ngược không in gì, không lỗi.

**Vì sao:** `10..1` là range rỗng vì đầu lớn hơn cuối. `..` chỉ đi lên.

**Sửa:** `10 downTo 1`; bước nhảy thì `step`: `10 downTo 0 step 2`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE0MTogMTAuLjEgbMOgIHJhbmdlIFLhu5ZORywgdsOybmcgZm9yIGtow7RuZyBjaOG6oXkgbOG6p24gbsOgbwpmdW4gbWFpbigpIHsKICAgIGZvciAoaSBpbiAzLi4xKSBwcmludCgiJGkgIikgICAgICAgICAgICAgLy8g4p2MIGtow7RuZyBpbiBnw6wKICAgIHByaW50bG4oInwgaOG6v3QiKQogICAgZm9yIChpIGluIDMgZG93blRvIDEpIHByaW50KCIkaSAiKSAgICAgICAvLyDinIUgMyAyIDEKICAgIHByaW50bG4oKQogICAgZm9yIChpIGluIDAuLjEwIHN0ZXAgNSkgcHJpbnQoIiRpICIpICAgICAgLy8gMCA1IDEwCiAgICBwcmludGxuKCkKfQo) · Xem thêm: K140

### K142 · `zip` cắt mất phần thừa

`🟡 đã làm app` · `Kotlin`

**Gặp:** ghép tên người với số tiền chia, người cuối cùng biến mất khỏi kết quả.

**Vì sao:** `zip` dừng ở list ngắn hơn, phần dư của list dài bị bỏ không báo.

**Sửa:** khi hai list bắt buộc dài bằng nhau, `require(a.size == b.size) { "..." }` trước khi `zip` để lỗi lộ ra sớm, có thông báo rõ.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE0MjogemlwIGThu6tuZyDhu58gbGlzdCBOR-G6rk4gaMahbiwgcGjhuqduIHRo4burYSBi4buLIGLhu48ga2jDtG5nIGLDoW8gZ8OsCmZ1biBtYWluKCkgewogICAgdmFsIG5hbWVzID0gbGlzdE9mKCJBbiIsICJCw6xuaCIsICJDaGkiKQogICAgdmFsIHNoYXJlcyA9IGxpc3RPZigxMDBfMDAwLCAxMjBfMDAwKSAgICAgICAgICAgICAgIC8vIHF1w6puIG3hu5l0IG5nxrDhu51pCiAgICBwcmludGxuKG5hbWVzLnppcChzaGFyZXMpKSAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBbKEFuLCAxMDAwMDApLCAoQsOsbmgsIDEyMDAwMCldIOKAlCBDaGkgYmnhur9uIG3huqV0CiAgICByZXF1aXJlKG5hbWVzLnNpemUgPT0gc2hhcmVzLnNpemUpIHsgIlRoaeG6v3UgcGjhuqduIGNoaWE6ICR7bmFtZXMuc2l6ZX0gbmfGsOG7nWksICR7c2hhcmVzLnNpemV9IHBo4bqnbiIgfSAgLy8g4pyFIGNo4bq3biBz4bubbQp9Cg)

### K143 · `sorted()` xếp tên tiếng Việt sai

`🟡 đã làm app` · `Kotlin` · `Android`

**Gặp:** danh bạ ra `[Bình, Dũng, an, Ánh, Đức]`: chữ thường đứng sau chữ hoa, `Ánh` và `Đức` bị đẩy xuống cuối.

**Vì sao:** `sorted()` so `String` theo mã Unicode từng ký tự. `a` (97) lớn hơn `Z` (90); `Á` (193), `Đ` (272) lớn hơn mọi chữ không dấu.
`CASE_INSENSITIVE_ORDER` chỉ chữa phần hoa/thường.

**Sửa:** trên JVM/Android dùng `java.text.Collator` theo ngôn ngữ. Trong `commonMain` không có `Collator`: hoặc sắp ở tầng platform
(`expect`/`actual`), hoặc sắp theo một khoá đã bỏ dấu (`đ` phải tự đổi thành `d`, NFD không tách được).

```kotlin
names.sorted()                                                        // ❌
names.sortedWith(Collator.getInstance(Locale.forLanguageTag("vi")))  // ✅ [an, Ánh, Bình, Dũng, Đức]
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE0Mzogc29ydGVkKCkgc28gdGhlbyBtw6MgVW5pY29kZTogY2jhu68gY8OzIGThuqV1IHbDoCBjaOG7ryBob2EvdGjGsOG7nW5nIHjhur9wIHNhaSB24bubaSB0aeG6v25nIFZp4buHdApmdW4gbWFpbigpIHsKICAgIHZhbCBuYW1lcyA9IGxpc3RPZigixJDhu6ljIiwgImFuIiwgIkLDrG5oIiwgIsOBbmgiLCAiRMWpbmciKQogICAgcHJpbnRsbihuYW1lcy5zb3J0ZWQoKSkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBbQsOsbmgsIETFqW5nLCBhbiwgw4FuaCwgxJDhu6ljXQogICAgcHJpbnRsbihuYW1lcy5zb3J0ZWRXaXRoKFN0cmluZy5DQVNFX0lOU0VOU0lUSVZFX09SREVSKSkgICAgIC8vIHbhuqtuIHNhaSB24bubaSBjaOG7ryBjw7MgZOG6pXUKICAgIHZhbCB2aSA9IGphdmEudGV4dC5Db2xsYXRvci5nZXRJbnN0YW5jZShqYXZhLnV0aWwuTG9jYWxlLmZvckxhbmd1YWdlVGFnKCJ2aSIpKQogICAgcHJpbnRsbihuYW1lcy5zb3J0ZWRXaXRoKHZpKSkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKchSBjaOG7iSBKVk0vQW5kcm9pZDogQ29sbGF0b3IKfQo)

### K144 · Sắp hai lần để được "theo A rồi theo B"

`🟡 đã làm app` · `Kotlin`

**Gặp:** `sortedBy { it.age }.sortedBy { it.name }` cho ra list sắp theo **tên**, tuổi lộn xộn.

**Vì sao:** lần sắp sau quyết định thứ tự chính. Sắp của Kotlin ổn định (stable), nên muốn "theo tuổi, cùng tuổi thì theo tên" phải sắp theo
tên **trước** rồi theo tuổi; viết ngược là sai. Rất dễ nhầm.

**Sửa:** một lần sắp với comparator nhiều khoá.

```kotlin
ps.sortedWith(compareBy<P> { it.age }.thenBy { it.name })   // ✅
ps.sortedWith(compareBy({ it.age }, { it.name }))           // ✅ viết gọn
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE0NDogc-G6r3AgaGFpIGzhuqduIGtow7RuZyBjaG8gInRoZW8gQSBy4buTaSB0aGVvIEIiOyBs4bqnbiBz4bqvcCBTQVUgbeG7m2kgbMOgIGtob8OhIGNow61uaApkYXRhIGNsYXNzIFAodmFsIG5hbWU6IFN0cmluZywgdmFsIGFnZTogSW50KQpmdW4gbWFpbigpIHsKICAgIHZhbCBwcyA9IGxpc3RPZihQKCJDaGkiLCAyMCksIFAoIkFuIiwgMzApLCBQKCJCw6xuaCIsIDIwKSkKICAgIHByaW50bG4ocHMuc29ydGVkQnkgeyBpdC5hZ2UgfS5zb3J0ZWRCeSB7IGl0Lm5hbWUgfSkgICAgICAgICAgICAgICAgLy8g4p2MIHRo4buxYyByYSBsw6AgdGhlbyBuYW1lCiAgICBwcmludGxuKHBzLnNvcnRlZFdpdGgoY29tcGFyZUJ5PFA-IHsgaXQuYWdlIH0udGhlbkJ5IHsgaXQubmFtZSB9KSkgIC8vIOKchSB0aGVvIGFnZSwgY8O5bmcgYWdlIHRow6wgdGhlbyBuYW1lCn0K) · Xem thêm: K197

### K145 · `map` ra `List<List<T>>`

`🟢 mới học` · `Kotlin`

**Gặp:** `Return type mismatch: expected 'String', actual 'List<String>'.` (hoặc `Type mismatch ... List<List<String>>`) khi lambda của `map`
trả về một list.

**Vì sao:** `map` giữ nguyên cấu trúc: mỗi phần tử thành **một** giá trị, ở đây là một list. Muốn trải phẳng phải nói rõ.

**Sửa:** `flatMap { }`, hoặc `map { }.flatten()`.

```kotlin
trips.map { it.points }       // ❌ List<List<String>>
trips.flatMap { it.points }   // ✅ List<String>
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE0NTogbWFwIHRy4bqjIHbhu4EgTGlzdDxMaXN0PC4uLj4-IGtoaSBsYW1iZGEgdHLhuqMgduG7gSBsaXN0CmRhdGEgY2xhc3MgVHJpcCh2YWwgcG9pbnRzOiBMaXN0PFN0cmluZz4pCmZ1biBhbGxQb2ludHModHJpcHM6IExpc3Q8VHJpcD4pOiBMaXN0PFN0cmluZz4gPQogICAgdHJpcHMubWFwIHsgaXQucG9pbnRzIH0gICAgICAgICAgLy8g4p2MIGNvbXBpbGUgZXJyb3I6IMSRw6J5IGzDoCBMaXN0PExpc3Q8U3RyaW5nPj4KICAgIC8vIHRyaXBzLmZsYXRNYXAgeyBpdC5wb2ludHMgfSAgIC8vIOKchQpmdW4gbWFpbigpID0gcHJpbnRsbihhbGxQb2ludHMobGlzdE9mKFRyaXAobGlzdE9mKCJBIiwgIkIiKSksIFRyaXAobGlzdE9mKCJDIikpKSkpCg)

### K146 · `Platform declaration clash` giữa `List<Int>` và `List<List<Int>>`

`🔴 khó, hiếm nhưng đau` · `Kotlin` · `KMP`

**Gặp:** `Platform declaration clash: The following declarations have the same JVM signature (decode(Ljava/util/List;)I)`.

**Vì sao:** generic bị xoá kiểu (type erasure) trên JVM. Với JVM cả hai đều là `decode(List)`. Trong KMP, lỗi này thường chỉ lộ ra khi
build target Android/JVM.

**Sửa:** đặt tên hàm khác. Nếu thật sự cần cùng tên ở phía Kotlin thì thêm `@JvmName("decodeAll")` cho một hàm.

```kotlin
fun decode(bytes: List<Int>): Int
fun decode(messages: List<List<Int>>): Int        // ❌
fun decodeAll(messages: List<List<Int>>): Int     // ✅
```

**Gặp ở:** [`ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/rules/Pid.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/rules/Pid.kt) (`decodePid` / `decodePidAnswer`) ·
[▶ Chạy thử](../playground/index.html#code=Ly8gSzE0NjogaGFpIG92ZXJsb2FkIGNo4buJIGtow6FjIGtp4buDdSBwaOG6p24gdOG7rSBj4bunYSBMaXN0IOKGkiB0csO5bmcgY2jhu68ga8O9IEpWTQpmdW4gZGVjb2RlKGJ5dGVzOiBMaXN0PEludD4pOiBJbnQgPSBieXRlcy5zdW0oKQpmdW4gZGVjb2RlKG1lc3NhZ2VzOiBMaXN0PExpc3Q8SW50Pj4pOiBJbnQgPSBtZXNzYWdlcy5zdW1PZiB7IGl0LnN1bSgpIH0gIC8vIOKdjCBQbGF0Zm9ybSBkZWNsYXJhdGlvbiBjbGFzaAovLyDinIUgxJHhurd0IHTDqm4ga2jDoWM6IGZ1biBkZWNvZGVBbGwobWVzc2FnZXM6IExpc3Q8TGlzdDxJbnQ-PikgIGhv4bq3YyBASnZtTmFtZSgiZGVjb2RlQWxsIikKZnVuIG1haW4oKSA9IHByaW50bG4oZGVjb2RlKGxpc3RPZigxLCAyKSkpCg) · Xem thêm: K147

### K147 · `Cannot check for instance of erased type 'List<String>'`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Cannot check for instance of erased type 'List<String>'.` trong `when (x) { is List<String> -> ... }`.

**Vì sao:** lúc chạy, object chỉ biết mình là `List`, không biết kiểu phần tử. Compiler chặn phép kiểm không thể làm được.

**Sửa:** kiểm `is List<*>` rồi kiểm từng phần tử (`filterIsInstance<String>()`); tốt hơn là thiết kế lại để không phải đoán kiểu
(sealed class bọc dữ liệu, xem K179). Trong hàm `inline` có `reified T` thì kiểm được `is T` cho chính `T`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE0Nzoga2jDtG5nIGtp4buDbSDEkcaw4bujYyBgaXMgTGlzdDxTdHJpbmc-YCBsw7pjIGNo4bqheSAodHlwZSBlcmFzdXJlKQpmdW4gZGVzY3JpYmUoeDogQW55KSA9IHdoZW4gKHgpIHsKICAgIGlzIExpc3Q8U3RyaW5nPiAtPiAibGlzdCBjaHXhu5dpIiAgICAgICAgIC8vIOKdjCBjb21waWxlIGVycm9yCiAgICAvLyBpcyBMaXN0PCo-IC0-ICJsaXN0LCBwaOG6p24gdOG7rTogJHt4LmZpcnN0T3JOdWxsKCk_LmxldCB7IGl0OjpjbGFzcy5zaW1wbGVOYW1lIH19IiAgIC8vIOKchQogICAgZWxzZSAtPiAia2jDoWMiCn0KZnVuIG1haW4oKSA9IHByaW50bG4oZGVzY3JpYmUobGlzdE9mKCJhIikpKQo) · Xem thêm: K146, [Hàm, lambda, generics](05-functions.md)

### K148 · `toMutableList()` chỉ copy list, không copy phần tử

`🟡 đã làm app` · `Kotlin`

**Gặp:** sửa "bản nháp" trong màn hình chỉnh sửa, bấm Huỷ mà dữ liệu gốc vẫn bị đổi.

**Vì sao:** `toList()`, `toMutableList()`, `ArrayList(list)` tạo list mới chứa **cùng các object**. Phần tử có `var` thì sửa ở đâu cũng là sửa chung.

**Sửa:** phần tử là `data class` toàn `val`, đổi bằng `copy(...)` và thay phần tử: `draft[0] = draft[0].copy(qty = 99)`. Nếu buộc phải mutable
thì copy sâu: `original.map { it.copy() }`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE0ODogdG9NdXRhYmxlTGlzdCgpIC8gdG9MaXN0KCkgY2jhu4kgY29weSBsaXN0LCBraMO0bmcgY29weSBjw6FjIG9iamVjdCBiw6puIHRyb25nCmNsYXNzIEl0ZW0odmFyIHF0eTogSW50KSB7IG92ZXJyaWRlIGZ1biB0b1N0cmluZygpID0gIkl0ZW0oJHF0eSkiIH0KZnVuIG1haW4oKSB7CiAgICB2YWwgb3JpZ2luYWwgPSBsaXN0T2YoSXRlbSgxKSwgSXRlbSgyKSkKICAgIHZhbCBkcmFmdCA9IG9yaWdpbmFsLnRvTXV0YWJsZUxpc3QoKQogICAgZHJhZnRbMF0ucXR5ID0gOTkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgc-G7rWEgb2JqZWN0IGTDuW5nIGNodW5nCiAgICBwcmludGxuKG9yaWdpbmFsKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIFtJdGVtKDk5KSwgSXRlbSgyKV0KICAgIHZhbCBkZWVwID0gb3JpZ2luYWwubWFwIHsgSXRlbShpdC5xdHkpIH0gICAgICAgLy8g4pyFIGNvcHkgY-G6oyBwaOG6p24gdOG7rSAoaG_hurdjIGRhdGEgY2xhc3MgKyBjb3B5KCkpCiAgICBkZWVwWzFdLnF0eSA9IDAKICAgIHByaW50bG4ob3JpZ2luYWwpCn0K) · Xem thêm: K119, K152

### K149 · `mutableListOf()` không suy ra được kiểu

`🟢 mới học` · `Kotlin`

**Gặp:** `Cannot infer type for type parameter 'T'. Specify it explicitly.`

**Vì sao:** list rỗng không có phần tử nào để compiler đoán kiểu.

**Sửa:** ghi kiểu ở hàm hoặc ở biến.

```kotlin
val xs = mutableListOf()                          // ❌
val xs = mutableListOf<String>()                  // ✅
val xs: MutableList<String> = mutableListOf()     // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE0OTogZW1wdHlMaXN0KCkgLyBtdXRhYmxlTGlzdE9mKCkga2jDtG5nIGPDsyBraeG7g3UgcGjhuqduIHThu60gxJHhu4Mgc3V5IHJhCmZ1biBtYWluKCkgewogICAgdmFsIHhzID0gbXV0YWJsZUxpc3RPZigpICAgICAgICAgICAgICAgICAgLy8g4p2MIGNvbXBpbGUgZXJyb3IKICAgIC8vIOKchSB2YWwgeHMgPSBtdXRhYmxlTGlzdE9mPFN0cmluZz4oKSAgIGhv4bq3YyAgIHZhbCB4czogTXV0YWJsZUxpc3Q8U3RyaW5nPiA9IG11dGFibGVMaXN0T2YoKQogICAgcHJpbnRsbih4cykKfQo)

### K150 · `ArrayList(5)` là list rỗng

`🟢 mới học` · `Kotlin`

**Gặp:** `IndexOutOfBoundsException: Index 0 out of bounds for length 0` ngay sau khi tạo `ArrayList<Int>(5)` và gán `slots[0] = 1`.

**Vì sao:** số trong `ArrayList(n)` là **sức chứa ban đầu** (để đỡ cấp phát lại), không phải số phần tử. `size` vẫn là 0.

**Sửa:** muốn n phần tử có sẵn thì `MutableList(n) { 0 }` (hoặc `IntArray(n)`).

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE1MDogQXJyYXlMaXN0KDUpIGzDoCBsaXN0IFLhu5ZORyBjw7Mgc-G7qWMgY2jhu6lhIDUsIGtow7RuZyBwaOG6o2kgNSBwaOG6p24gdOG7rQpmdW4gbWFpbigpIHsKICAgIHZhbCBzbG90cyA9IEFycmF5TGlzdDxJbnQ-KDUpCiAgICBwcmludGxuKHNsb3RzLnNpemUpICAgICAgICAgICAgICAgICAgICAgICAgLy8gMAogICAgdmFsIG9rID0gTXV0YWJsZUxpc3QoNSkgeyAwIH0gICAgICAgICAgICAgIC8vIOKchSA1IHBo4bqnbiB04butIGLhurFuZyAwCiAgICBva1swXSA9IDE7IHByaW50bG4ob2spCiAgICBzbG90c1swXSA9IDEgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIEluZGV4T3V0T2ZCb3VuZHNFeGNlcHRpb24KfQo) · Xem thêm: K107

---

**Nhóm trước / sau:** [2. Null safety](02-null-safety.md) · [4. Class, object, sealed, enum](04-classes.md) · [Mục lục 900 lỗi](index.md)
