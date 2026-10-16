# 2. Null safety (K051–K100)

Kotlin tách `String` (không bao giờ null) và `String?` (có thể null), và bắt bạn xử lý `null` ngay lúc compile. Lỗi ở nhóm này
chia hai loại: compiler từ chối (đọc thông báo là biết sửa), và crash lúc chạy ở đúng những chỗ ta tự tắt bảo vệ: `!!`,
`lateinit`, `as`, giá trị đến từ Java. Đọc kèm [Phụ lục 2.4 và 2.5](../reference/kotlin-pitfalls.md).

### K051 · `Null cannot be a value of a non-null type 'String'`

`🟢 mới học` · `Kotlin`

**Gặp:** `Null cannot be a value of a non-null type 'String'.` khi gán `null` hoặc truyền `null` vào hàm.

**Vì sao:** kiểu không có dấu `?` không bao giờ được chứa `null`. Đây là điểm khác lớn nhất so với Java.

**Sửa:** nếu "không có giá trị" là trường hợp hợp lệ thì khai báo `String?`. Nếu không, đừng gán `null` mà dùng giá trị mặc định (`""`, `0`, `emptyList()`).

```kotlin
var nickname: String = "Tú"
nickname = null                 // ❌
var nickname: String? = "Tú"    // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YXIgbmlja25hbWU6IFN0cmluZyA9ICJUw7oiCiAgICBuaWNrbmFtZSA9IG51bGwgICAgICAgICAgICAgICAgIC8vIOKdjCBOdWxsIGNhbm5vdCBiZSBhIHZhbHVlIG9mIGEgbm9uLW51bGwgdHlwZSAnU3RyaW5nJy4KICAgIC8vIFPhu61hOiB2YXIgbmlja25hbWU6IFN0cmluZz8gPSAiVMO6IgogICAgcHJpbnRsbigiJG5pY2tuYW1lIikKfQo)

### K052 · `!!` làm app crash với NullPointerException

`🟢 mới học` · `Kotlin`

**Gặp:** `java.lang.NullPointerException` ở dòng có `!!`, thường sau khi dữ liệu từ server hoặc Intent thiếu field.

**Vì sao:** `!!` nghĩa là "tôi chắc chắn không null". Sai một lần là crash, và compiler không còn bảo vệ bạn nữa.

**Sửa:** dùng `?.` + `?:` để có giá trị thay thế, hoặc `requireNotNull(x) { "lý do" }` để crash có thông báo rõ.

```kotlin
val name: String? = intent.getStringExtra("name")
val len = name!!.length          // ❌ crash nếu không có extra
val len = name?.length ?: 0      // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgZXh0cmFzID0gbWFwT2YoImlkIiB0byAiNDIiKSAgICAgICAgICAgICAgLy8gZ2nhuqMgbOG6rXAgSW50ZW50IHRoaeG6v3UgIm5hbWUiCiAgICB2YWwgbmFtZTogU3RyaW5nPyA9IGV4dHJhc1sibmFtZSJdCiAgICBwcmludGxuKG5hbWU_Lmxlbmd0aCA_OiAwKSAgICAgICAgICAgICAgICAgICAgLy8g4pyFIDAKICAgIHByaW50bG4obmFtZSEhLmxlbmd0aCkgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgTnVsbFBvaW50ZXJFeGNlcHRpb24KfQo) · Xem thêm: K061

### K053 · `Only safe (?.) or non-null asserted (!!.) calls are allowed`

`🟢 mới học` · `Kotlin`

**Gặp:** `Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'String?'.`

**Vì sao:** gọi hàm trên biến `String?` có thể gặp `null`. Compiler bắt bạn nói rõ khi đó làm gì.

**Sửa:** chọn một: `?.` (null thì cả biểu thức ra null), kiểm `if (x != null)` trước (smart cast), hoặc `?:` cho giá trị mặc định.
Đừng bấm theo gợi ý "Add non-null asserted (!!.) call" của IDE cho xong.

```kotlin
city.uppercase()               // ❌
city?.uppercase()              // ✅ String?
city?.uppercase() ?: "—"       // ✅ String
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgY2l0eTogU3RyaW5nPyA9IGxpc3RPZigiSHXhur8iLCBudWxsKS5yYW5kb20oKQogICAgcHJpbnRsbihjaXR5LnVwcGVyY2FzZSgpKSAgICAgICAgICAvLyDinYwgT25seSBzYWZlICg_Likgb3Igbm9uLW51bGwgYXNzZXJ0ZWQgKCEhLikgY2FsbHMgYXJlIGFsbG93ZWQuLi4KICAgIC8vIHByaW50bG4oY2l0eT8udXBwZXJjYXNlKCkpICAgICAgLy8g4pyFIGluIG51bGwgbuG6v3UgY2l0eSBudWxsCn0K) · Xem thêm: K052

### K054 · `Argument type mismatch: actual type is 'String?', but 'String' was expected`

`🟢 mới học` · `Kotlin`

**Gặp:** `Argument type mismatch: actual type is 'String?', but 'String' was expected.` khi truyền biến nullable vào hàm.

**Vì sao:** hàm hứa không bao giờ nhận `null`, biến của bạn thì có thể null.

**Sửa:** xử lý `null` **ở chỗ gọi** (giá trị mặc định, `return` sớm), hoặc nếu hàm thật sự chịu được `null` thì đổi tham số thành `String?`.

```kotlin
greet(typed)                   // ❌
greet(typed ?: "bạn")          // ✅
typed?.let { greet(it) }       // ✅ null thì bỏ qua
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIGdyZWV0KG5hbWU6IFN0cmluZykgPSBwcmludGxuKCJDaMOgbyAkbmFtZSIpCgpmdW4gbWFpbigpIHsKICAgIHZhbCB0eXBlZDogU3RyaW5nPyA9IHJlYWRsbk9yTnVsbCgpCiAgICBncmVldCh0eXBlZCkgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBBcmd1bWVudCB0eXBlIG1pc21hdGNoOiBhY3R1YWwgdHlwZSBpcyAnU3RyaW5nPycsIGJ1dCAnU3RyaW5nJyB3YXMgZXhwZWN0ZWQuCiAgICAvLyBncmVldCh0eXBlZCA_OiAiYuG6oW4iKSAgICAgICAgICAgLy8g4pyFCn0K)

### K055 · Màn hình hiện chữ "null"

`🟢 mới học` · `Kotlin`

**Gặp:** UI hiện `Thành phố: null`. Không crash, nhưng người dùng thấy chữ "null".

**Vì sao:** `?.` không ném lỗi mà cho ra `null`, và string template in `null` thành chữ `"null"`.

**Sửa:** chuỗi nào hiện cho người dùng thì phải có `?:` ở cuối chuỗi `?.`.

```kotlin
"Thành phố: ${user.address?.city}"               // ❌
"Thành phố: ${user.address?.city ?: "chưa có"}"  // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZGF0YSBjbGFzcyBBZGRyZXNzKHZhbCBjaXR5OiBTdHJpbmc_KQpkYXRhIGNsYXNzIFVzZXIodmFsIGFkZHJlc3M6IEFkZHJlc3M_KQoKZnVuIG1haW4oKSB7CiAgICB2YWwgdXNlciA9IFVzZXIoQWRkcmVzcyhudWxsKSkKICAgIHByaW50bG4oIlRow6BuaCBwaOG7kTogJHt1c2VyLmFkZHJlc3M_LmNpdHl9IikgICAgICAgICAgICAgIC8vIOKdjCBUaMOgbmggcGjhu5E6IG51bGwKICAgIHByaW50bG4oIlRow6BuaCBwaOG7kTogJHt1c2VyLmFkZHJlc3M_LmNpdHkgPzogImNoxrBhIGPDsyJ9IikgLy8g4pyFCn0K) · Xem thêm: K056

### K056 · Ghép tên ra `Nguyễn null Tú`

`🟢 mới học` · `Kotlin`

**Gặp:** tên đệm trống thì tên hiển thị thành `Nguyễn null Tú`; `x.toString()` trên giá trị null ra chuỗi `"null"` dài 4 ký tự.

**Vì sao:** `toString()` được định nghĩa cho cả `Any?`, nên template và `toString()` không bao giờ báo lỗi null.

**Sửa:** `orEmpty()` cho chuỗi null, hoặc `listOfNotNull(...).joinToString(" ")` để bỏ hẳn phần thiếu.

```kotlin
"Nguyễn $middle Tú"                                       // ❌
listOfNotNull("Nguyễn", middle, "Tú").joinToString(" ")  // ✅ Nguyễn Tú
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgbWlkZGxlTmFtZTogU3RyaW5nPyA9IG51bGwKICAgIHByaW50bG4oIk5ndXnhu4VuICRtaWRkbGVOYW1lIFTDuiIpICAgICAgICAgICAgICAgICAvLyDinYwgTmd1eeG7hW4gbnVsbCBUw7oKICAgIHByaW50bG4obWlkZGxlTmFtZS50b1N0cmluZygpLmxlbmd0aCkgICAgICAgICAgICAgLy8gNDogY2jhu68gIm51bGwiCiAgICBwcmludGxuKCJOZ3V54buFbiAke21pZGRsZU5hbWUub3JFbXB0eSgpfSBUw7oiKSAgICAgIC8vIOKchSBOZ3V54buFbiAgVMO6CiAgICBwcmludGxuKGxpc3RPZk5vdE51bGwoIk5ndXnhu4VuIiwgbWlkZGxlTmFtZSwgIlTDuiIpLmpvaW5Ub1N0cmluZygiICIpKSAgLy8g4pyFIE5ndXnhu4VuIFTDugp9Cg) · Xem thêm: K093

### K057 · `a ?: 0 + b` tính sai

`🟡 đã làm app` · `Kotlin`

**Gặp:** thưởng 5 + lương 10 ra `5` chứ không phải `15`.

**Vì sao:** `?:` có độ ưu tiên rất thấp, thấp hơn `+`. `bonus ?: 0 + salary` được hiểu là `bonus ?: (0 + salary)`.

**Sửa:** luôn đặt ngoặc quanh `x ?: mặcĐịnh` khi nó nằm trong biểu thức lớn hơn.

```kotlin
bonus ?: 0 + salary         // ❌ bonus ?: (0 + salary)
(bonus ?: 0) + salary       // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgYm9udXM6IEludD8gPSA1CiAgICB2YWwgc2FsYXJ5ID0gMTAKICAgIHByaW50bG4oYm9udXMgPzogMCArIHNhbGFyeSkgICAgICAgIC8vIOKdjCA1OiDEkcaw4bujYyBoaeG7g3UgbMOgIGJvbnVzID86ICgwICsgc2FsYXJ5KQogICAgcHJpbnRsbigoYm9udXMgPzogMCkgKyBzYWxhcnkpICAgICAgLy8g4pyFIDE1Cn0K)

### K058 · Kiểm null rồi thoát sớm bằng `?: return`

`🟢 mới học` · `Kotlin`

**Gặp:** code lồng `if (x != null) { if (y != null) { ... } }` nhiều tầng, khó đọc.

**Vì sao:** chưa biết `return` và `throw` cũng là biểu thức (kiểu `Nothing`), dùng được ngay sau `?:`.

**Sửa:** lấy giá trị bằng `?: return` / `?: throw` / `?: continue` ở đầu hàm. Từ dòng sau, biến đã là kiểu không null.

```kotlin
fun total(text: String): Int {
    val n = text.toIntOrNull() ?: return 0    // ✅ từ đây n là Int
    return n * 1000
}
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIHRvdGFsKHRleHQ6IFN0cmluZyk6IEludCB7CiAgICB2YWwgbiA9IHRleHQudG9JbnRPck51bGwoKSA_OiByZXR1cm4gMCAgICAgICAgLy8g4pyFIHRob8OhdCBz4bubbSwgdOG7qyDEkcOieSBuIGzDoCBJbnQKICAgIHJldHVybiBuICogMTAwMAp9CgpmdW4gbWFpbigpIHsKICAgIHByaW50bG4odG90YWwoIjEyIikpICAgLy8gMTIwMDAKICAgIHByaW50bG4odG90YWwoImFiYyIpKSAgLy8gMAp9Cg)

### K059 · `Smart cast to 'String' is impossible, because 'x' is a mutable property`

`🟢 mới học` · `Kotlin`

**Gặp:** đã `if (email != null)` mà dòng dưới vẫn báo `Smart cast to 'String' is impossible, because 'email' is a mutable property that could be mutated concurrently.`

**Vì sao:** `email` là `var` của class. Giữa lúc kiểm và lúc dùng, thread khác (hoặc chính hàm bạn gọi) có thể gán lại thành `null`,
nên compiler không dám tin kết quả kiểm.

**Sửa:** chụp vào một `val` cục bộ rồi kiểm cái đó, hoặc dùng `?.let`.

```kotlin
if (email != null) send(email)        // ❌
val e = email
if (e != null) send(e)                // ✅
email?.let { send(it) }               // ✅
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgRm9ybSB7CiAgICB2YXIgZW1haWw6IFN0cmluZz8gPSBudWxsCgogICAgZnVuIGNoZWNrKCkgewogICAgICAgIGlmIChlbWFpbCAhPSBudWxsKSB7CiAgICAgICAgICAgIHByaW50bG4oZW1haWwubGVuZ3RoKSAgICAgICAvLyDinYwgU21hcnQgY2FzdCB0byAnU3RyaW5nJyBpcyBpbXBvc3NpYmxlLi4uCiAgICAgICAgfQogICAgICAgIC8vIOKchSBjaOG7pXAgdsOgbyBiaeG6v24gY-G7pWMgYuG7mToKICAgICAgICAvLyB2YWwgZSA9IGVtYWlsCiAgICAgICAgLy8gaWYgKGUgIT0gbnVsbCkgcHJpbnRsbihlLmxlbmd0aCkKICAgIH0KfQoKZnVuIG1haW4oKSA9IEZvcm0oKS5hcHBseSB7IGVtYWlsID0gImFAYi5jIiB9LmNoZWNrKCkK) · Xem thêm: K060, K062, K097

### K060 · Smart cast không được vì custom getter, `open`, hay module khác

`🟡 đã làm app` · `Kotlin` · `KMP`

**Gặp:** `Smart cast to 'Int' is impossible, because 'reading' is a property that has an open or custom getter.` Hoặc
`... because 'x' is a public API property declared in different module.` dù property là `val`.

**Vì sao:** smart cast chỉ an toàn khi đọc hai lần chắc chắn ra cùng giá trị. Getter tự viết có thể trả giá trị khác mỗi lần;
property `open` có thể bị override; property của module khác có thể đổi cách cài đặt mà module của bạn không biết.

**Sửa:** giống K059: đọc một lần vào biến cục bộ, hoặc `?.let`.

```kotlin
if (s.reading != null) use(s.reading)    // ❌
s.reading?.let { use(it) }               // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbi5yYW5kb20uUmFuZG9tCgpjbGFzcyBTZW5zb3IgewogICAgdmFsIHJlYWRpbmc6IEludD8KICAgICAgICBnZXQoKSA9IGlmIChSYW5kb20ubmV4dEJvb2xlYW4oKSkgNDIgZWxzZSBudWxsICAgLy8gbeG7l2kgbOG6p24gxJHhu41jIG3hu5l0IGdpw6EgdHLhu4sga2jDoWMKfQoKZnVuIG1haW4oKSB7CiAgICB2YWwgcyA9IFNlbnNvcigpCiAgICBpZiAocy5yZWFkaW5nICE9IG51bGwpIHsKICAgICAgICBwcmludGxuKHMucmVhZGluZyArIDEpICAgICAgICAvLyDinYwgU21hcnQgY2FzdCB0byAnSW50JyBpcyBpbXBvc3NpYmxlLi4uIGN1c3RvbSBnZXR0ZXIKICAgIH0KICAgIC8vIOKchSBzLnJlYWRpbmc_LmxldCB7IHByaW50bG4oaXQgKyAxKSB9Cn0K)

### K061 · `requireNotNull` / `checkNotNull` thay cho `!!`

`🟢 mới học` · `Kotlin`

**Gặp:** log crash chỉ có `NullPointerException` trần, không biết thiếu cái gì.

**Vì sao:** `!!` không mang thông báo. Khi "null ở đây là lỗi lập trình" thì crash là đúng, nhưng phải crash có nghĩa.

**Sửa:** `requireNotNull(x) { "..." }` (đầu vào sai, ném `IllegalArgumentException`), `checkNotNull(x) { "..." }` (trạng thái sai,
ném `IllegalStateException`), hoặc `x ?: error("...")`. Cả ba đều trả về giá trị không null để dùng tiếp.

```kotlin
val url = config["baseUrl"]!!                                           // ❌ NPE trần
val url = requireNotNull(config["baseUrl"]) { "Thiếu baseUrl trong config" }   // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgY29uZmlnID0gbWFwT2YoImJhc2VVcmwiIHRvICJodHRwczovL2FwaS5leGFtcGxlLmNvbSIpCiAgICB2YWwgdXJsID0gcmVxdWlyZU5vdE51bGwoY29uZmlnWyJiYXNlVXJsIl0pIHsgIlRoaeG6v3UgYmFzZVVybCB0cm9uZyBjb25maWciIH0gICAvLyDinIUgdXJsOiBTdHJpbmcKICAgIHByaW50bG4odXJsKQogICAgdmFsIGtleSA9IGNoZWNrTm90TnVsbChjb25maWdbImFwaUtleSJdKSB7ICJUaGnhur91IGFwaUtleSB0cm9uZyBjb25maWciIH0gICAgICAgLy8g4p2MIGPDsyB0aMO0bmcgYsOhbyByw7UKICAgIHByaW50bG4oa2V5KQp9Cg) · Xem thêm: K052

### K062 · Smart cast mất khi biến bị lambda sửa

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Smart cast to 'String' is impossible, because 'name' is a local variable that is mutated in a capturing closure.`

**Vì sao:** một lambda ở đâu đó gán lại `name`. Lambda có thể chạy bất cứ lúc nào (callback, thread khác), nên kể cả biến cục bộ
cũng không còn an toàn để smart cast.

**Sửa:** chụp giá trị vào một `val` mới trước khi kiểm; hoặc thiết kế lại để lambda không sửa biến bên ngoài (trả kết quả về thay vì gán).

```kotlin
val n = name
if (n != null) println(n.length)      // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YXIgbmFtZTogU3RyaW5nPyA9ICJUw7oiCiAgICB2YWwgcmVzZXQgPSB7IG5hbWUgPSBudWxsIH0gICAgICAgICAgLy8gbGFtYmRhIGPDsyB0aOG7gyDEkeG7lWkgbmFtZSBi4bqldCBj4bupIGzDumMgbsOgbwogICAgaWYgKG5hbWUgIT0gbnVsbCkgewogICAgICAgIHByaW50bG4obmFtZS5sZW5ndGgpICAgICAgICAgICAgIC8vIOKdjCBTbWFydCBjYXN0IHRvICdTdHJpbmcnIGlzIGltcG9zc2libGUuLi4KICAgIH0KICAgIC8vIOKchSB2YWwgbiA9IG5hbWU7IGlmIChuICE9IG51bGwpIHByaW50bG4obi5sZW5ndGgpCiAgICByZXNldCgpCn0K)

### K063 · Compiler không hiểu "đã kiểm ở hàm khác"

`🟡 đã làm app` · `Kotlin`

**Gặp:** `validate(bill)` trả về "không lỗi" nghĩa là `bill` khác null, nhưng compiler vẫn bắt `bill` là `Long?` và ta phải viết `bill!!`.

**Vì sao:** smart cast chỉ nhìn điều kiện viết ngay tại chỗ (và hàm có contract như `isNullOrEmpty`). Quan hệ "hàm A trả null thì
biến B khác null" nằm trong đầu bạn, compiler không biết.

**Sửa:** chấp nhận `!!` kèm comment giải thích (như app TipSplit), hoặc tốt hơn: cho hàm kiểm **trả về luôn giá trị đã kiểm** dưới
dạng không null (sealed `Ok(value)` / `Error(reason)`), khỏi phải hứa.

```kotlin
val bill = text.toLongOrNull() ?: return Parsed.Error("Không phải số")
return Parsed.Ok(bill)          // ✅ bill: Long, không cần !!
```

**Gặp ở:** [`TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/rules/Rules.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/rules/Rules.kt) (`bill = bill!!` sau `validate()`)

[▶ Chạy thử](../playground/index.html#code=c2VhbGVkIGludGVyZmFjZSBQYXJzZWQgewogICAgZGF0YSBjbGFzcyBPayh2YWwgYmlsbDogTG9uZykgOiBQYXJzZWQKICAgIGRhdGEgY2xhc3MgRXJyb3IodmFsIHJlYXNvbjogU3RyaW5nKSA6IFBhcnNlZAp9CgpmdW4gdmFsaWRhdGUoYmlsbDogTG9uZz8pOiBTdHJpbmc_ID0gaWYgKGJpbGwgPT0gbnVsbCkgIktow7RuZyBwaOG6o2kgc-G7kSIgZWxzZSBudWxsCgpmdW4gcGFyc2UodGV4dDogU3RyaW5nKTogUGFyc2VkIHsgICAgICAgICAgICAgICAgICAgICAgLy8g4pyFIHRy4bqjIGx1w7RuIGdpw6EgdHLhu4sgxJHDoyBraeG7g20KICAgIHZhbCBiaWxsID0gdGV4dC50b0xvbmdPck51bGwoKSA_OiByZXR1cm4gUGFyc2VkLkVycm9yKCJLaMO0bmcgcGjhuqNpIHPhu5EiKQogICAgcmV0dXJuIFBhcnNlZC5PayhiaWxsKQp9CgpmdW4gbWFpbigpIHsKICAgIHZhbCBiaWxsID0gIjEyMDAwMCIudG9Mb25nT3JOdWxsKCkKICAgIGlmICh2YWxpZGF0ZShiaWxsKSA9PSBudWxsKSB7CiAgICAgICAgLy8gcHJpbnRsbihiaWxsICsgMSkgICAgICAgICAgIC8vIOKdjCBjb21waWxlciBraMO0bmcgYmnhur90IHZhbGlkYXRlKCk9PW51bGwgbmdoxKlhIGzDoCBiaWxsICE9IG51bGwKICAgICAgICBwcmludGxuKGJpbGwhISArIDEpICAgICAgICAgICAgLy8gY2jhuqF5IMSRxrDhu6NjLCBuaMawbmcgbMOgIGzhu51pIGjhu6lhIGPhu6dhIGLhuqFuLCBraMO0bmcgcGjhuqNpIGNvbXBpbGVyCiAgICB9CiAgICBwcmludGxuKHBhcnNlKCIxMjAwMDAiKSkKfQo) · Xem thêm: K064

### K064 · Hàm kiểm null tự viết không smart cast được

`🟡 đã làm app` · `Kotlin`

**Gặp:** `if (!note.isNullOrBlank()) note.length` chạy được, nhưng `if (note.hasText()) note.length` (hàm tự viết y hệt) lại báo
`Only safe (?.) or non-null asserted (!!.) calls are allowed...`.

**Vì sao:** hàm của thư viện chuẩn có **contract** báo cho compiler "trả `false` nghĩa là không null". Hàm của bạn thì không.

**Sửa:** đơn giản nhất: cho hàm trả luôn giá trị (`fun String?.textOrNull(): String?`) rồi dùng `?.let`. Hoặc tự viết contract
(`@OptIn(ExperimentalContracts::class)` + `contract { returns(true) implies (this@hasText != null) }`), nhưng đây vẫn là API thử nghiệm.

[▶ Chạy thử](../playground/index.html#code=ZnVuIFN0cmluZz8uaGFzVGV4dCgpOiBCb29sZWFuID0gdGhpcyAhPSBudWxsICYmIHRoaXMuaXNOb3RCbGFuaygpCgpmdW4gbWFpbigpIHsKICAgIHZhbCBub3RlOiBTdHJpbmc_ID0gIk11YSBz4buvYSIKICAgIGlmICghbm90ZS5pc051bGxPckJsYW5rKCkpIHByaW50bG4obm90ZS5sZW5ndGgpICAgLy8g4pyFIHN0ZGxpYiBjw7MgY29udHJhY3Qg4oaSIHNtYXJ0IGNhc3QKICAgIGlmIChub3RlLmhhc1RleHQoKSkgcHJpbnRsbihub3RlLmxlbmd0aCkgICAgICAgICAgLy8g4p2MIGjDoG0gdOG7sSB2aeG6v3Qga2jDtG5nIHNtYXJ0IGNhc3QgxJHGsOG7o2MKfQo)

### K065 · `NullPointerException: getProperty(...) must not be null`

`🟡 đã làm app` · `Kotlin` · `Android`

**Gặp:** crash `NullPointerException: getProperty(...) must not be null` (hoặc `getString(...) must not be null`) ở dòng gán kết quả
một hàm Java vào biến Kotlin.

**Vì sao:** hàm Java không có annotation null trả về **platform type** (`String!`). Bạn ghi `val x: String = javaCall()`, Kotlin chèn
một phép kiểm ngay tại đó, và giá trị null làm nó nổ.

**Sửa:** nhận giá trị từ Java vào biến **`String?`** rồi xử lý như mọi giá trị nullable.

```kotlin
val home: String = System.getProperty("khong.co")     // ❌ NPE ngay đây
val home: String? = System.getProperty("khong.co")    // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgb2s6IFN0cmluZz8gPSBTeXN0ZW0uZ2V0UHJvcGVydHkoImtob25nLmNvIikgICAgICAvLyDinIUgZ2hpIHLDtSBTdHJpbmc_CiAgICBwcmludGxuKG9rID86ICJt4bq3YyDEkeG7i25oIikKICAgIHZhbCBob21lOiBTdHJpbmcgPSBTeXN0ZW0uZ2V0UHJvcGVydHkoImtob25nLmNvIikgICAgIC8vIOKdjCBOdWxsUG9pbnRlckV4Y2VwdGlvbjogZ2V0UHJvcGVydHkoLi4uKSBtdXN0IG5vdCBiZSBudWxsCiAgICBwcmludGxuKGhvbWUpCn0K) · Xem thêm: [Phụ lục 2.4](../reference/kotlin-pitfalls.md), K068

### K066 · `map[key]` luôn trả kiểu nullable

`🟢 mới học` · `Kotlin`

**Gặp:** vừa `put` key vào map xong, `map["k"]` vẫn là `Int?` và không cộng được; dùng `getValue` thì gặp
`NoSuchElementException: Key cơm is missing in the map.`

**Vì sao:** compiler không theo dõi map có key nào. `get` trả `null` khi thiếu key, `getValue` ném lỗi khi thiếu key.

**Sửa:** chọn theo ý nghĩa: `getOrElse(k) { mặcĐịnh }` khi thiếu là bình thường; `getValue(k)` khi thiếu là lỗi lập trình;
`getOrPut(k) { ... }` khi muốn tạo nếu chưa có.

```kotlin
prices["phở"] + 5_000                  // ❌ Int?
prices.getOrElse("cơm") { 0 }          // ✅
prices.getValue("phở") + 5_000         // ✅ chắc chắn có key
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgcHJpY2VzID0gbWFwT2YoInBo4bufIiB0byA0NV8wMDAsICJiw7puIiB0byA0MF8wMDApCiAgICBwcmludGxuKHByaWNlc1siY8ahbSJdKSAgICAgICAgICAgICAgICAgICAgLy8gbnVsbAogICAgcHJpbnRsbihwcmljZXMuZ2V0T3JFbHNlKCJjxqFtIikgeyAwIH0pICAgIC8vIOKchSAwCiAgICBwcmludGxuKHByaWNlcy5nZXRWYWx1ZSgicGjhu58iKSArIDVfMDAwKSAgIC8vIOKchSA1MDAwMDogY2jhuq9jIGPDsyBrZXkgdGjDrCBkw7luZyBnZXRWYWx1ZQogICAgcHJpbnRsbihwcmljZXMuZ2V0VmFsdWUoImPGoW0iKSkgICAgICAgICAgIC8vIOKdjCBOb1N1Y2hFbGVtZW50RXhjZXB0aW9uOiBLZXkgY8ahbSBpcyBtaXNzaW5nIGluIHRoZSBtYXAuCn0K) · Xem thêm: K095

### K067 · `getStringExtra` trả `null` ở màn hình thứ hai

`🟢 mới học` · `Android`

**Gặp:** màn hình chi tiết crash `NullPointerException` ở `intent.getStringExtra("id")!!`, hoặc `requireArguments()` báo
`IllegalStateException: Fragment ... does not have any arguments.` khi mở từ notification hay deep link.

**Vì sao:** `getStringExtra` trả `String?`: mọi nơi mở màn hình (launcher, notification, deep link, app khác) đều có thể không gửi extra đó.

**Sửa:** xử lý thiếu dữ liệu như một trường hợp bình thường: `?: run { finish(); return }`, hoặc dùng Navigation Compose với
argument có kiểu và giá trị mặc định.

```kotlin
val id = intent.getStringExtra("id")!!                     // ❌
val id = intent.getStringExtra("id") ?: run { finish(); return }  // ✅
```

### K068 · Nâng thư viện Java xong thì hiện lỗi null mới

`🟡 đã làm app` · `Android` · `Gradle`

**Gặp:** nâng `compileSdk` hay một thư viện Java, code cũ đang chạy bỗng báo `Argument type mismatch: actual type is 'String?'...`
hoặc cảnh báo mới về nullability ở chỗ gọi API Java.

**Vì sao:** thư viện vừa thêm annotation null (`@Nullable`, `@NonNull`, JSpecify, `@RecentlyNullable` của Android SDK). Kotlin
đọc các annotation đó và không coi giá trị là platform type nữa, nên chỗ trước đây "lách" được giờ bị bắt kiểm.

**Sửa:** đừng tắt cảnh báo. Lỗi mới thường chỉ đúng chỗ code cũ có thể crash: thêm xử lý `null` theo kiểu mới.

### K069 · `'onCreate' overrides nothing`

`🟢 mới học` · `Android`

**Gặp:** `'onCreate' overrides nothing.` sau khi tự gõ lại hàm override và bỏ dấu `?` ở `Bundle?` (hoặc thêm `?` vào tham số vốn không null).

**Vì sao:** `Bundle` và `Bundle?` là hai kiểu khác nhau, nên chữ ký hàm không còn khớp hàm cha.

**Sửa:** dùng **Code → Override Methods** (Ctrl + O) để IDE sinh đúng chữ ký, rồi giữ nguyên nullability.

```kotlin
override fun onCreate(savedInstanceState: Bundle) { }    // ❌ overrides nothing
override fun onCreate(savedInstanceState: Bundle?) { }   // ✅
```

### K070 · `lateinit property has not been initialized`

`🟢 mới học` · `Kotlin` · `Android`

**Gặp:** `kotlin.UninitializedPropertyAccessException: lateinit property title has not been initialized`.

**Vì sao:** `lateinit` là lời hứa "sẽ gán trước khi đọc". Một đường đi (mở từ notification, xoay máy, process bị huỷ rồi khôi phục,
test quên `setUp`) đọc trước khi gán.

**Sửa:** ưu tiên truyền qua constructor hoặc `by lazy`. Chỉ giữ `lateinit` khi framework gán giúp (DI, `@BeforeTest`), và gán
ngay đầu `onCreate`, không gán trong callback chạy muộn.

```kotlin
lateinit var title: String          // ❌ đọc trước khi gán là crash
class Screen(val title: String)     // ✅ không thể quên
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgU2NyZWVuIHsKICAgIGxhdGVpbml0IHZhciB0aXRsZTogU3RyaW5nCiAgICBmdW4gcmVuZGVyKCkgPSBwcmludGxuKCJUacOqdSDEkeG7gTogJHRpdGxlIikKfQoKZnVuIG1haW4oKSB7CiAgICB2YWwgcyA9IFNjcmVlbigpCiAgICAvLyBzLnRpdGxlID0gIkdoaSBjaMO6IiAgICAgICAgLy8g4pyFIGLhu48gLy8gxJHhu4MgZ8OhbiB0csaw4bubYyBraGkgxJHhu41jCiAgICBzLnJlbmRlcigpICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIFVuaW5pdGlhbGl6ZWRQcm9wZXJ0eUFjY2Vzc0V4Y2VwdGlvbgp9Cg) · Xem thêm: K071, K074

### K071 · Kiểm `lateinit` đã gán chưa bằng `::x.isInitialized`

`🟡 đã làm app` · `Kotlin`

**Gặp:** cần dọn dẹp trong `onDestroy`/`stop()` nhưng có đường đi không bao giờ gán biến `lateinit`, nên dọn dẹp lại crash K070.

**Vì sao:** đọc biến `lateinit` chưa gán là ném lỗi, kể cả chỉ để kiểm.

**Sửa:** `if (::track.isInitialized)`. Nếu phải kiểm ở nhiều chỗ thì nghĩa là biến thật ra là nullable: đổi thành `var track: String? = null`.

```kotlin
if (::track.isInitialized) player.stop(track)    // ✅
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgUGxheWVyIHsKICAgIGxhdGVpbml0IHZhciB0cmFjazogU3RyaW5nCgogICAgZnVuIHN0b3AoKSB7CiAgICAgICAgaWYgKDo6dHJhY2suaXNJbml0aWFsaXplZCkgcHJpbnRsbigiROG7q25nICR0cmFjayIpICAgLy8g4pyFIGtp4buDbSB0csaw4bubYyBraGkgxJHhu41jCiAgICAgICAgZWxzZSBwcmludGxuKCJDaMawYSBwaMOhdCBiw6BpIG7DoG8iKQogICAgfQp9CgpmdW4gbWFpbigpIHsKICAgIFBsYXllcigpLnN0b3AoKQogICAgUGxheWVyKCkuYXBwbHkgeyB0cmFjayA9ICJCw6BpIDEiIH0uc3RvcCgpCn0K) · Xem thêm: K098

### K072 · `'lateinit' modifier is not allowed on properties of primitive types`

`🟢 mới học` · `Kotlin`

**Gặp:** `'lateinit' modifier is not allowed on properties of primitive types.` với `lateinit var count: Int` (hay `Boolean`, `Double`, `Long`).

**Vì sao:** `lateinit` dùng `null` ở bên dưới làm dấu "chưa gán". Kiểu nguyên thuỷ trên JVM không chứa được `null`.

**Sửa:** cho giá trị ban đầu (`var count = 0`), dùng `by lazy`, hoặc `Delegates.notNull<Int>()` nếu thật sự phải gán muộn.

```kotlin
lateinit var count: Int                       // ❌
var count: Int by Delegates.notNull()         // ✅ đọc trước khi gán sẽ ném IllegalStateException
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgQ291bnRlciB7CiAgICBsYXRlaW5pdCB2YXIgY291bnQ6IEludCAgICAgICAgICAvLyDinYwgJ2xhdGVpbml0JyBtb2RpZmllciBpcyBub3QgYWxsb3dlZCBvbiBwcm9wZXJ0aWVzIG9mIHByaW1pdGl2ZSB0eXBlcy4KICAgIC8vIHZhciBjb3VudDogSW50ID0gMCAgICAgICAgICAgIC8vIOKchQp9CgpmdW4gbWFpbigpID0gcHJpbnRsbihDb3VudGVyKCkpCg)

### K073 · `lateinit` với `val` hoặc kiểu nullable

`🟢 mới học` · `Kotlin`

**Gặp:** `'lateinit' modifier is allowed only on mutable properties.` hoặc
`'lateinit' modifier is not allowed on properties of a type with nullable upper bound.`

**Vì sao:** `lateinit` cần gán sau nên phải là `var`. Và vì `null` đã được dùng làm dấu "chưa gán", kiểu `String?` không có nghĩa.

**Sửa:** `val` gán muộn thì dùng `by lazy`. Kiểu nullable thì bỏ `lateinit`, khởi tạo `= null`.

```kotlin
lateinit var cache: String?     // ❌
var cache: String? = null       // ✅
lateinit val name: String       // ❌
val name: String by lazy { load() }  // ✅
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgUmVwbyB7CiAgICBsYXRlaW5pdCB2YXIgY2FjaGU6IFN0cmluZz8gICAgICAvLyDinYwgLi4uIG5vdCBhbGxvd2VkIG9uIHByb3BlcnRpZXMgb2YgbnVsbGFibGUgdHlwZXMuCiAgICBsYXRlaW5pdCB2YWwgbmFtZTogU3RyaW5nICAgICAgICAvLyDinYwgLi4uIGFsbG93ZWQgb25seSBvbiBtdXRhYmxlIHByb3BlcnRpZXMuCn0KCmZ1biBtYWluKCkgPSBwcmludGxuKFJlcG8oKSkK)

### K074 · `by lazy` hay `lateinit`, và `lazy` chạy lại khi lỗi

`🟡 đã làm app` · `Kotlin`

**Gặp:** không biết chọn cái nào; hoặc khối `lazy` (đọc file, mở DB) bị gọi lần hai sau một lần lỗi, log hiện hai lần "đọc config".

**Vì sao:** `by lazy` tính giá trị ở lần đọc đầu và nhớ mãi, dùng cho `val`. Nếu khối tính **ném lỗi** thì không có gì được nhớ,
lần đọc sau chạy lại khối. `lateinit` cho `var` được gán từ bên ngoài.

**Sửa:** giá trị tự tính được → `by lazy`. Giá trị do người khác gán (DI, test) → `lateinit`. Khối `lazy` có thể lỗi thì bắt lỗi
ngay trong khối, hoặc chuyển sang hàm `suspend` trả kết quả rõ ràng.

[▶ Chạy thử](../playground/index.html#code=dmFyIGF0dGVtcHRzID0gMAoKdmFsIGNvbmZpZzogU3RyaW5nIGJ5IGxhenkgewogICAgYXR0ZW1wdHMrKwogICAgcHJpbnRsbigiICDEkeG7jWMgY29uZmlnIGzhuqduICRhdHRlbXB0cyIpCiAgICBpZiAoYXR0ZW1wdHMgPT0gMSkgZXJyb3IoIm3huqFuZyBs4buXaSIpCiAgICAib2siCn0KCmZ1biBtYWluKCkgewogICAgcnVuQ2F0Y2hpbmcgeyBjb25maWcgfS5vbkZhaWx1cmUgeyBwcmludGxuKCJs4buXaTogJHtpdC5tZXNzYWdlfSIpIH0KICAgIHByaW50bG4oY29uZmlnKSAgICAgIC8vIGxhenkgY2jhuqF5IEzhuqBJIGto4buRaSBpbml0IHbDrCBs4bqnbiB0csaw4bubYyBuw6ltIGzhu5dpCiAgICBwcmludGxuKGNvbmZpZykgICAgICAvLyBs4bqnbiBuw6B5IGTDuW5nIGdpw6EgdHLhu4sgxJHDoyBuaOG7mywga2jDtG5nIGNo4bqheSBs4bqhaQp9Cg)

### K075 · Dùng `Context` ở field của `Activity` thì crash

`🟡 đã làm app` · `Android`

**Gặp:** crash ngay khi mở màn hình:
`NullPointerException: Attempt to invoke virtual method '... getSharedPreferences(java.lang.String, int)' on a null object reference`.

**Vì sao:** field (`val prefs = getSharedPreferences(...)`) chạy lúc **tạo object** `Activity`, trước khi Android gắn `Context`
cho nó. Lúc đó `Activity` là `Context` rỗng.

**Sửa:** khởi tạo trong `onCreate`, hoặc `by lazy` (chỉ chạy khi dùng lần đầu, lúc đó đã có `Context`).

```kotlin
private val prefs = getSharedPreferences("app", MODE_PRIVATE)            // ❌
private val prefs by lazy { getSharedPreferences("app", MODE_PRIVATE) }  // ✅
```

Xem thêm: K074, [Android: lifecycle](11-android.md)

### K076 · Property không null mà vẫn đọc ra `null`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `NullPointerException: Cannot invoke "String.length()" because "this.label" is null` dù `label` khai báo `String` và có giá trị.
IDE cảnh báo `Calling non-final function describe in constructor`.

**Vì sao:** constructor lớp cha chạy **trước** khi property của lớp con được gán. Lớp cha gọi một hàm `open` mà lớp con override,
hàm đó đọc property của lớp con, lúc này vẫn là `null`.

**Sửa:** không gọi hàm `open` (hay để lộ `this`) trong `init`/constructor của lớp cha. Truyền dữ liệu qua constructor, hoặc gọi
hàm đó sau khi tạo xong object.

[▶ Chạy thử](../playground/index.html#code=b3BlbiBjbGFzcyBCYXNlIHsKICAgIGluaXQgeyBkZXNjcmliZSgpIH0gICAgICAgICAgICAgICAgICAvLyDimqDvuI8gZ-G7jWkgaMOgbSBvcGVuIHRyb25nIGNvbnN0cnVjdG9yCiAgICBvcGVuIGZ1biBkZXNjcmliZSgpIHt9Cn0KCmNsYXNzIENoaWxkIDogQmFzZSgpIHsKICAgIHZhbCBsYWJlbDogU3RyaW5nID0gImNvbiIgICAgICAgICAgICAvLyBjaMawYSDEkcaw4bujYyBnw6FuIGtoaSBCYXNlLmluaXQgY2jhuqF5CiAgICBvdmVycmlkZSBmdW4gZGVzY3JpYmUoKSA9IHByaW50bG4oImxhYmVsID0gJGxhYmVsLCBsZW5ndGggPSAke2xhYmVsLmxlbmd0aH0iKQp9CgpmdW4gbWFpbigpIHsKICAgIENoaWxkKCkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgbGFiZWwgPSBudWxsLCBy4buTaSBOdWxsUG9pbnRlckV4Y2VwdGlvbgp9Cg) · Xem thêm: [Class, object](04-classes.md)

### K077 · `?.let { } ?: run { }` chạy cả hai nhánh

`🟡 đã làm app` · `Kotlin`

**Gặp:** draft có nội dung, app vừa lưu vừa hiện thông báo "không có draft".

**Vì sao:** `?:` xét giá trị **trả về của `let`**, không phải biến ban đầu. Lambda trong `let` trả `null` (ở đây `save()` trả `null`)
thì nhánh `run` cũng chạy.

**Sửa:** có hai nhánh thì viết `if (x != null) ... else ...`. Chỉ dùng `?.let { } ?: ...` khi chắc lambda không trả `null`.

```kotlin
draft?.let { save(it) } ?: showEmpty()             // ❌ save() trả null → showEmpty() cũng chạy
if (draft != null) save(draft) else showEmpty()    // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIHNhdmUodGV4dDogU3RyaW5nKTogU3RyaW5nPyB7IHByaW50bG4oIiAgbMawdTogJHRleHQiKTsgcmV0dXJuIG51bGwgfSAgIC8vIHRy4bqjIG51bGwga2hpLi4uIHRow6BuaCBjw7RuZwoKZnVuIG1haW4oKSB7CiAgICB2YWwgZHJhZnQ6IFN0cmluZz8gPSAiR2hpIGNow7oiCiAgICBkcmFmdD8ubGV0IHsgc2F2ZShpdCkgfSA_OiBydW4geyBwcmludGxuKCIgIGtow7RuZyBjw7MgZHJhZnQiKSB9ICAgLy8g4p2MIGNo4bqheSBD4bqiIEhBSSBuaMOhbmgKICAgIHByaW50bG4oIi0tLSIpCiAgICBpZiAoZHJhZnQgIT0gbnVsbCkgc2F2ZShkcmFmdCkgZWxzZSBwcmludGxuKCIgIGtow7RuZyBjw7MgZHJhZnQiKSAgLy8g4pyFCn0K) · Xem thêm: [Hàm, lambda: scope function](05-functions.md)

### K078 · `null cannot be cast to non-null type kotlin.String`

`🟢 mới học` · `Kotlin`

**Gặp:** `NullPointerException: null cannot be cast to non-null type kotlin.String` ở dòng có `as String`.

**Vì sao:** `as String` đòi giá trị **là** `String`. `null` không phải `String` nên ép kiểu thất bại.

**Sửa:** `as? String` (sai kiểu hoặc null thì ra `null`) rồi `?:` cho giá trị thay thế.

```kotlin
val text = raw as String              // ❌
val text = raw as? String ?: ""       // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgcmF3OiBBbnk_ID0gbnVsbAogICAgcHJpbnRsbihyYXcgYXM_IFN0cmluZyA_OiAidHLhu5FuZyIpICAgICAvLyDinIUgdHLhu5FuZwogICAgdmFsIHRleHQgPSByYXcgYXMgU3RyaW5nICAgICAgICAgICAgICAgLy8g4p2MIE51bGxQb2ludGVyRXhjZXB0aW9uOiBudWxsIGNhbm5vdCBiZSBjYXN0IHRvIG5vbi1udWxsIHR5cGUga290bGluLlN0cmluZwogICAgcHJpbnRsbih0ZXh0KQp9Cg) · Xem thêm: K079

### K079 · `as String?` không phải `as? String`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String` dù đã viết `as String?` để "an toàn".

**Vì sao:** `as String?` chấp nhận `null` nhưng vẫn ném lỗi khi sai kiểu. Chỉ `as?` mới đổi mọi thất bại thành `null`.

**Sửa:** muốn "ép được thì ép, không thì null" luôn viết `as?`.

```kotlin
raw as String?      // ❌ Integer → ClassCastException
raw as? String      // ✅ null
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgcmF3OiBBbnk_ID0gNDIKICAgIHByaW50bG4ocmF3IGFzPyBTdHJpbmcpICAgICAgICAgICAgICAgIC8vIOKchSBudWxsOiBzYWkga2nhu4N1IHRow6wgcmEgbnVsbAogICAgcHJpbnRsbihyYXcgYXMgU3RyaW5nPykgICAgICAgICAgICAgICAgLy8g4p2MIENsYXNzQ2FzdEV4Y2VwdGlvbjogSW50ZWdlciBjYW5ub3QgYmUgY2FzdCB0byBTdHJpbmcKfQo)

### K080 · `Condition type mismatch: inferred type is 'Boolean?'`

`🟢 mới học` · `Kotlin`

**Gặp:** `Condition type mismatch: inferred type is 'Boolean?' but 'Boolean' was expected.` ở `if (dark)`.

**Vì sao:** `Boolean?` có ba giá trị: `true`, `false`, `null`. `if` cần biết `null` tính là gì.

**Sửa:** so sánh rõ: `dark == true` (null tính như false), `dark != false` (null tính như true), hoặc `dark ?: false`.

```kotlin
if (dark) ...              // ❌
if (dark == true) ...      // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgc2V0dGluZ3MgPSBtYXBPZigiZGFya01vZGUiIHRvIHRydWUpCiAgICB2YWwgZGFyazogQm9vbGVhbj8gPSBzZXR0aW5nc1siZGFya01vZGUiXQogICAgaWYgKGRhcmspIHByaW50bG4oInThu5FpIikgICAgICAgICAgICAgIC8vIOKdjCBDb25kaXRpb24gdHlwZSBtaXNtYXRjaC4uLgogICAgLy8gaWYgKGRhcmsgPT0gdHJ1ZSkgcHJpbnRsbigidOG7kWkiKSAgIC8vIOKchSBudWxsIGNvaSBuaMawIGZhbHNlCn0K)

### K081 · `Operator call is prohibited on a nullable receiver of type 'Int?'`

`🟢 mới học` · `Kotlin`

**Gặp:** cộng hai `Int?` báo `Operator call is prohibited on a nullable receiver of type 'Int?'. Use '?.'-qualified call instead.`

**Vì sao:** `a + b` là `a.plus(b)`. Gọi hàm trên `Int?` cần xử lý null như K053.

**Sửa:** đổi về `Int` trước khi tính: `(a ?: 0) + (b ?: 0)`. Nếu "thiếu một số thì không có kết quả", trả `null` có chủ đích.

```kotlin
a + b                          // ❌
(a ?: 0) + (b ?: 0)            // ✅
if (a != null && b != null) a + b else null   // ✅ giữ nghĩa "thiếu dữ liệu"
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgYTogSW50PyA9ICIzIi50b0ludE9yTnVsbCgpCiAgICB2YWwgYjogSW50PyA9ICI0Ii50b0ludE9yTnVsbCgpCiAgICBwcmludGxuKGEgKyBiKSAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwga2jDtG5nIGPhu5luZyDEkcaw4bujYyBJbnQ_CiAgICAvLyBwcmludGxuKChhID86IDApICsgKGIgPzogMCkpICAgICAgICAvLyDinIUgNwp9Cg) · Xem thêm: K092

### K082 · `List<String?>` khác `List<String>?`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `tags.map { it.length }` báo `Only safe (?.) ... calls are allowed on a nullable receiver of type 'String?'.` dù `tags` không null.

**Vì sao:** `List<String?>`: list luôn có, **phần tử** có thể null. `List<String>?`: **cả list** có thể null, phần tử thì không.

**Sửa:** đọc kỹ vị trí dấu `?`. Phần tử nullable thì lọc bằng `filterNotNull()`. Hạn chế `List<...>?`: "không có gì" thường là `emptyList()`.

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgdGFnczogTGlzdDxTdHJpbmc_PiA9IGxpc3RPZigia290bGluIiwgbnVsbCkgICAvLyBsaXN0IGPDsyB0aOG7gyBjaOG7qWEgbnVsbAogICAgdmFsIG1heWJlOiBMaXN0PFN0cmluZz4_ID0gbnVsbCAgICAgICAgICAgICAgICAgICAgIC8vIGPhuqMgbGlzdCBjw7MgdGjhu4MgbMOgIG51bGwKICAgIHByaW50bG4odGFncy5zaXplKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyAyOiBn4buNaSB0aOG6s25nIMSRxrDhu6NjCiAgICBwcmludGxuKG1heWJlPy5zaXplID86IDApICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gMDogcGjhuqNpID8uCiAgICBwcmludGxuKHRhZ3MubWFwIHsgaXQubGVuZ3RoIH0pICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIHBo4bqnbiB04butIGzDoCBTdHJpbmc_Cn0K) · Xem thêm: K083, K087

### K083 · `filter { it != null }` mà kiểu vẫn nullable

`🟢 mới học` · `Kotlin`

**Gặp:** sau khi lọc null, list vẫn là `List<Int?>`, `sum()` không gọi được.

**Vì sao:** `filter` không đổi kiểu phần tử; compiler không suy ra từ điều kiện lọc.

**Sửa:** `filterNotNull()` hoặc gộp hai bước bằng `mapNotNull { ... }`.

```kotlin
inputs.map { it.toIntOrNull() }.filter { it != null }   // ❌ List<Int?>
inputs.mapNotNull { it.toIntOrNull() }                  // ✅ List<Int>
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgaW5wdXRzID0gbGlzdE9mKCIxIiwgIngiLCAiMyIpCiAgICB2YWwgYSA9IGlucHV0cy5tYXAgeyBpdC50b0ludE9yTnVsbCgpIH0uZmlsdGVyIHsgaXQgIT0gbnVsbCB9CiAgICAvLyBwcmludGxuKGEuc3VtKCkpICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgYSB24bqrbiBsw6AgTGlzdDxJbnQ_PgogICAgdmFsIGIgPSBpbnB1dHMubWFwTm90TnVsbCB7IGl0LnRvSW50T3JOdWxsKCkgfSAgICAgIC8vIOKchSBMaXN0PEludD4KICAgIHByaW50bG4oYi5zdW0oKSkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyA0CiAgICBwcmludGxuKGEuZmlsdGVyTm90TnVsbCgpLnN1bSgpKSAgICAgICAgICAgICAgICAgICAgLy8g4pyFIDQKfQo) · Xem thêm: [Collections](03-collections.md)

### K084 · `bio?.isBlank() == true` cho qua bio null

`🟡 đã làm app` · `Kotlin`

**Gặp:** form bắt buộc mà vẫn lưu được khi trường còn `null`: điều kiện "trống" ra `false`.

**Vì sao:** `bio?.isBlank()` ra `null` khi `bio` null, và `null == true` là `false`, nên null bị coi là "có chữ".

**Sửa:** dùng hàm viết sẵn cho nullable: `isNullOrBlank()`, `isNullOrEmpty()`, `orEmpty()`.

```kotlin
if (bio?.isBlank() == true) error()     // ❌ null lọt qua
if (bio.isNullOrBlank()) error()        // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgYmlvOiBTdHJpbmc_ID0gbnVsbAogICAgcHJpbnRsbihiaW8uaXNOdWxsT3JCbGFuaygpKSAgICAgICAgIC8vIOKchSB0cnVlCiAgICBwcmludGxuKGJpbz8uaXNCbGFuaygpKSAgICAgICAgICAgICAgLy8g4p2MIG51bGwsIGtow7RuZyBwaOG6o2kgdHJ1ZQogICAgcHJpbnRsbihiaW8_LmlzQmxhbmsoKSA9PSB0cnVlKSAgICAgIC8vIOKdjCBmYWxzZTogYmlvIG51bGwgbcOgIHbhuqtuIGNvaSBsw6AgImPDsyBjaOG7ryIKfQo) · Xem thêm: K038

### K085 · Generic `<T>` nhận cả `null`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `Box<T>` hay cache generic bị đưa `null` vào; hàm trả `T` mà ra `null` dù không có dấu `?`.

**Vì sao:** `T` không có giới hạn thì giới hạn trên là `Any?`, tức `T` có thể là `String?`.

**Sửa:** cấm null bằng `T : Any`. Cần nói "phiên bản không null của `T`" trong một hàm thì dùng `T & Any`.

```kotlin
class Box<T>(val value: T)            // T có thể nullable
class SafeBox<T : Any>(val value: T)  // ✅ SafeBox<String?> báo lỗi
fun <T> firstOrFail(list: List<T>): T & Any = list.first() ?: error("phần tử null")
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgQm94PFQ-KHZhbCB2YWx1ZTogVCkgICAgICAgICAgICAgICAgICAgIC8vIFQgY8OzIHRo4buDIGzDoCBraeG7g3UgbnVsbGFibGUKY2xhc3MgU2FmZUJveDxUIDogQW55Pih2YWwgdmFsdWU6IFQpICAgICAgICAgIC8vIOKchSBUIDogQW55IGPhuqVtIG51bGwKCmZ1biA8VD4gZmlyc3RPckZhaWwobGlzdDogTGlzdDxUPik6IFQgJiBBbnkgPSBsaXN0LmZpcnN0KCkgPzogZXJyb3IoInBo4bqnbiB04butIG51bGwiKSAgLy8gVCAmIEFueTogY2jhuq9jIGNo4bqvbiBraMO0bmcgbnVsbAoKZnVuIG1haW4oKSB7CiAgICB2YWwgYiA9IEJveDxTdHJpbmc_PihudWxsKQogICAgcHJpbnRsbihiLnZhbHVlPy5sZW5ndGgpCiAgICBwcmludGxuKGZpcnN0T3JGYWlsKGxpc3RPZigiYSIsIG51bGwpKSkKICAgIC8vIFNhZmVCb3g8U3RyaW5nPz4obnVsbCkgICAgICAgICAgICAgICAgIC8vIOKdjCBi4buPIC8vIDogVHlwZSBhcmd1bWVudCBpcyBub3Qgd2l0aGluIGl0cyBib3VuZHMKfQo) · Xem thêm: [Generics](05-functions.md)

### K086 · `var x = null` rồi không gán được gì

`🟢 mới học` · `Kotlin`

**Gặp:** `Assignment type mismatch: actual type is 'String', but 'Nothing?' was expected.`

**Vì sao:** từ `null` Kotlin chỉ suy ra được kiểu `Nothing?`, kiểu chỉ chứa đúng giá trị `null`.

**Sửa:** khai báo `null` ban đầu thì luôn ghi rõ kiểu.

```kotlin
var selected = null              // ❌ Nothing?
var selected: String? = null     // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YXIgc2VsZWN0ZWQgPSBudWxsICAgICAgICAgICAgICAgICAvLyBraeG7g3Ugc3V5IHJhIGzDoCBOb3RoaW5nPwogICAgc2VsZWN0ZWQgPSAiUGjhu58iICAgICAgICAgICAgICAgICAgICAvLyDinYwgQXNzaWdubWVudCB0eXBlIG1pc21hdGNoOiBhY3R1YWwgdHlwZSBpcyAnU3RyaW5nJywgYnV0ICdOb3RoaW5nPycgd2FzIGV4cGVjdGVkLgogICAgLy8gdmFyIHNlbGVjdGVkOiBTdHJpbmc_ID0gbnVsbCAgICAgLy8g4pyFCiAgICBwcmludGxuKCIkc2VsZWN0ZWQiKQp9Cg)

### K087 · Dùng `null` cho "chưa tải" và cả "không có dữ liệu"

`🟡 đã làm app` · `Kotlin` · `Compose`

**Gặp:** màn hình hiện "Không có ghi chú nào" trong nửa giây đầu khi đang tải; hoặc vòng xoay tải chạy mãi khi danh sách thật sự rỗng.

**Vì sao:** `List<Note>?` với `null` = đang tải, `emptyList()` = rỗng, lỗi thì… cũng `null`. Ba trạng thái dồn vào hai giá trị,
và mỗi chỗ đọc tự đoán một kiểu.

**Sửa:** dùng `sealed` cho trạng thái màn hình (`Loading`, `Empty`, `Ready(items)`, `Failed(reason)`); danh sách bên trong `Ready` không bao giờ null.

```kotlin
sealed interface NotesUi {
    data object Loading : NotesUi
    data class Ready(val notes: List<Note>) : NotesUi    // ✅ rỗng là Ready(emptyList())
    data class Failed(val reason: String) : NotesUi
}
```

Xem thêm: [Class, sealed](04-classes.md)

### K088 · Gson để `null` lọt vào field không null

`🔴 khó, hiếm nhưng đau` · `Android` · `Kotlin`

**Gặp:** `data class User(val name: String)` mà `user.name` lại là `null`; crash `NullPointerException` ở chỗ dùng, cách xa chỗ parse JSON.

**Vì sao:** Gson tạo object bằng cách lách constructor (không gọi constructor Kotlin), nên phép kiểm null và giá trị mặc định của
Kotlin không chạy. JSON thiếu field thì field đó là `null`, bất kể kiểu khai báo.

**Sửa:** dùng `kotlinx.serialization` (hoặc Moshi với codegen Kotlin): thiếu field thì báo lỗi ngay khi parse, hoặc lấy giá trị mặc
định. Field server có thể bỏ thì khai báo `String? = null`.

Xem thêm: [Mạng & JSON](15-networking.md)

### K089 · `Parameter specified as non-null is null`

`🟡 đã làm app` · `Kotlin` · `Android`

**Gặp:** `java.lang.NullPointerException: Parameter specified as non-null is null: method FileKt.greet, parameter name`.
Thường từ callback của thư viện Java, reflection, hoặc code Java cũ gọi sang Kotlin.

**Vì sao:** hàm Kotlin `public` có tham số không null được chèn phép kiểm ở đầu hàm. Kotlin không gọi được với `null`, nhưng Java thì được.

**Sửa:** hàm nào được Java (hay framework) gọi thì khai báo tham số là `String?` rồi tự xử lý. Override interface Java thì giữ đúng
nullability IDE sinh ra (K069).

[▶ Chạy thử](../playground/index.html#code=ZnVuIGdyZWV0KG5hbWU6IFN0cmluZykgPSAiQ2jDoG8gJG5hbWUiCgpmdW4gbWFpbigpIHsKICAgIC8vIEdp4bqjIGzhuq1wIGNvZGUgSmF2YSAoaG_hurdjIHJlZmxlY3Rpb24pIGfhu41pIGjDoG0gS290bGluIHbhu5tpIG51bGw6CiAgICB2YWwgbWV0aG9kID0gQ2xhc3MuZm9yTmFtZSgiRmlsZUt0IikuZ2V0TWV0aG9kKCJncmVldCIsIFN0cmluZzo6Y2xhc3MuamF2YSkKICAgIHRyeSB7CiAgICAgICAgbWV0aG9kLmludm9rZShudWxsLCBudWxsKQogICAgfSBjYXRjaCAoZTogamF2YS5sYW5nLnJlZmxlY3QuSW52b2NhdGlvblRhcmdldEV4Y2VwdGlvbikgewogICAgICAgIHByaW50bG4oZS5jYXVzZSkgICAvLyBOdWxsUG9pbnRlckV4Y2VwdGlvbjogUGFyYW1ldGVyIHNwZWNpZmllZCBhcyBub24tbnVsbCBpcyBudWxsLi4uCiAgICB9Cn0K)

### K090 · `Int?` thành `KotlinInt?` trong Swift

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** property Kotlin `val count: Int?` hiện bên Swift là `KotlinInt?`; không cộng được với `Int` của Swift, phải `.intValue`.

**Vì sao:** Objective-C không có số nguyên nullable. Kotlin/Native đóng hộp `Int?` thành object `KotlinInt` (lớp con `NSNumber`).
`Int` không null thì vẫn thành `Int32` bình thường.

**Sửa:** hạn chế để lộ số nullable trong API cho Swift: dùng giá trị mặc định, hoặc `sealed` class. Phải để lộ thì bên Swift viết
`count?.intValue`.

Xem thêm: [Swift ↔ Kotlin](10-ios-interop.md)

### K091 · `Optional` của Java trong code Kotlin

`🟡 đã làm app` · `Kotlin`

**Gặp:** API Java trả `Optional<String>`, gọi `.get()` crash `NoSuchElementException: No value present`.

**Vì sao:** `Optional` là cách Java nói "có thể không có". Kotlin dùng `String?` cho việc đó, nhưng không tự đổi giúp.

**Sửa:** `getOrNull()` (import `kotlin.jvm.optionals.getOrNull`) rồi dùng `?:` như mọi giá trị nullable. Đừng tự viết hàm trả `Optional` trong Kotlin.

```kotlin
findUser(2).get()                    // ❌
findUser(2).getOrNull() ?: "khách"   // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGphdmEudXRpbC5PcHRpb25hbAppbXBvcnQga290bGluLmp2bS5vcHRpb25hbHMuZ2V0T3JOdWxsCgpmdW4gZmluZFVzZXIoaWQ6IEludCk6IE9wdGlvbmFsPFN0cmluZz4gPSBpZiAoaWQgPT0gMSkgT3B0aW9uYWwub2YoIlTDuiIpIGVsc2UgT3B0aW9uYWwuZW1wdHkoKQoKZnVuIG1haW4oKSB7CiAgICBwcmludGxuKGZpbmRVc2VyKDIpLmdldE9yTnVsbCgpID86ICJraMOhY2giKSAgIC8vIOKchSBraMOhY2gKICAgIHByaW50bG4oZmluZFVzZXIoMikuZ2V0KCkpICAgICAgICAgICAgICAgICAgICAvLyDinYwgTm9TdWNoRWxlbWVudEV4Y2VwdGlvbjogTm8gdmFsdWUgcHJlc2VudAp9Cg)

### K092 · So sánh `count > 0` với `Int?`

`🟢 mới học` · `Kotlin`

**Gặp:** `if (unread > 0)` báo `Operator call is prohibited on a nullable receiver of type 'Int?'.`

**Vì sao:** `>` là `compareTo`, không gọi được trên `Int?`. (Riêng `==` thì so được với `null`, nên `unread == 0` không báo gì.)

**Sửa:** quyết định `null` nghĩa là gì: `(unread ?: 0) > 0`.

```kotlin
if (unread > 0)              // ❌
if ((unread ?: 0) > 0)       // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgdW5yZWFkOiBJbnQ_ID0gIjMiLnRvSW50T3JOdWxsKCkKICAgIGlmICh1bnJlYWQgPiAwKSBwcmludGxuKCJjw7MgdGluIG3hu5tpIikgICAgICAgICAgICAvLyDinYwga2jDtG5nIHNvIHPDoW5oIMSRxrDhu6NjIEludD8KICAgIC8vIGlmICgodW5yZWFkID86IDApID4gMCkgcHJpbnRsbigiY8OzIHRpbiBt4bubaSIpICAvLyDinIUKfQo) · Xem thêm: K081

### K093 · `String? + "x"` ra `"nullx"`

`🟡 đã làm app` · `Kotlin`

**Gặp:** nhãn hiện `null Nguyễn`; compiler không báo gì dù `first` là `String?`.

**Vì sao:** thư viện chuẩn có hàm `String?.plus(Any?)`: ghép chuỗi nullable được phép, và `null` thành chữ `"null"`.

**Sửa:** xử lý null trước khi ghép (`(first ?: "")`, `orEmpty()`), hoặc dùng `listOfNotNull(...).joinToString()` như K056.

```kotlin
first + " Nguyễn"            // ❌ "null Nguyễn"
first.orEmpty() + " Nguyễn"  // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgZmlyc3Q6IFN0cmluZz8gPSBudWxsCiAgICB2YWwgbGFiZWwgPSBmaXJzdCArICIgTmd1eeG7hW4iICAgICAvLyBjb21waWxlIMSRxrDhu6NjOiBTdHJpbmc_LnBsdXMoQW55PykKICAgIHByaW50bG4obGFiZWwpICAgICAgICAgICAgICAgICAgICAvLyDinYwgbnVsbCBOZ3V54buFbgogICAgcHJpbnRsbigoZmlyc3QgPzogIiIpICsgIiBOZ3V54buFbiIpLy8g4pyFCn0K)

### K094 · `arrayOfNulls` cho ra `Array<String?>`

`🟢 mới học` · `Kotlin`

**Gặp:** tạo mảng 3 phần tử rồi không truyền vào hàm nhận `Array<String>` được; mọi chỗ đọc đều phải `?.`.

**Vì sao:** `arrayOfNulls<String>(3)` tạo mảng chứa sẵn `null`, nên kiểu là `Array<String?>`.

**Sửa:** tạo mảng có giá trị ban đầu: `Array(3) { "" }`. Thường thì `List`/`MutableList` hợp hơn mảng.

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgc2xvdHMgPSBhcnJheU9mTnVsbHM8U3RyaW5nPigzKSAgIC8vIEFycmF5PFN0cmluZz8-LCBraMO0bmcgcGjhuqNpIEFycmF5PFN0cmluZz4KICAgIHNsb3RzWzBdID0gIkFuIgogICAgcHJpbnRsbihzbG90cy5jb250ZW50VG9TdHJpbmcoKSkgICAgICAvLyBbQW4sIG51bGwsIG51bGxdCiAgICBwcmludGxuKHNsb3RzLmZpbHRlck5vdE51bGwoKSkgICAgICAgIC8vIOKchSBbQW5dCiAgICB2YWwgb2sgPSBBcnJheSgzKSB7ICIiIH0gICAgICAgICAgICAgIC8vIOKchSBBcnJheTxTdHJpbmc-IGPDsyBnacOhIHRy4buLIGJhbiDEkeG6p3UKICAgIHByaW50bG4ob2suc2l6ZSkKfQo)

### K095 · `map[key] == null`: thiếu key hay giá trị là null?

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `Map<String, String?>` trả `null` cho cả key không có và key có giá trị `null`; logic "đã lưu chưa" sai.

**Vì sao:** `get` dùng `null` để báo "không có key", trùng với giá trị `null` hợp lệ.

**Sửa:** kiểm `containsKey(k)` khi phân biệt quan trọng. Tốt hơn: tránh value nullable trong map, xoá key thay vì lưu `null`.

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgbmlja25hbWVzOiBNYXA8U3RyaW5nLCBTdHJpbmc_PiA9IG1hcE9mKCJUw7oiIHRvIG51bGwpCiAgICBwcmludGxuKG5pY2tuYW1lc1siVMO6Il0pICAgICAgICAgICAgICAgICAgIC8vIG51bGw6IGPDsyBrZXksIGdpw6EgdHLhu4sgbnVsbAogICAgcHJpbnRsbihuaWNrbmFtZXNbIkxhbiJdKSAgICAgICAgICAgICAgICAgIC8vIG51bGw6IGtow7RuZyBjw7Mga2V5CiAgICBwcmludGxuKG5pY2tuYW1lcy5jb250YWluc0tleSgiVMO6IikpICAgICAgIC8vIOKchSB0cnVlCiAgICBwcmludGxuKG5pY2tuYW1lcy5jb250YWluc0tleSgiTGFuIikpICAgICAgLy8g4pyFIGZhbHNlCn0K) · Xem thêm: K066

### K096 · `?.` bỏ qua cả việc tính đối số

`🟡 đã làm app` · `Kotlin`

**Gặp:** `logger?.log(buildReport())`: khi `logger` null thì `buildReport()` không chạy, mà hàm đó có việc phụ (ghi file, tăng bộ đếm).

**Vì sao:** `a?.f(x)` khi `a` null thì dừng ngay, không tính `x`. Có lợi khi `x` tốn kém, nhưng sai nếu bạn trông vào việc phụ của `x`.

**Sửa:** việc phải luôn chạy thì tách ra dòng riêng trước `?.`.

```kotlin
val report = buildReport()     // ✅ luôn chạy
logger?.log(report)
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIGV4cGVuc2l2ZSgpOiBJbnQgeyBwcmludGxuKCIgIHTDrW5oIHRvw6FuIG7hurduZyIpOyByZXR1cm4gMSB9CgpmdW4gbWFpbigpIHsKICAgIHZhbCBsaXN0OiBNdXRhYmxlTGlzdDxJbnQ-PyA9IG51bGwKICAgIGxpc3Q_LmFkZChleHBlbnNpdmUoKSkgICAgICAgICAgLy8ga2jDtG5nIGluIGfDrDogbGlzdCBudWxsIG7Dqm4gxJHhu5FpIHPhu5EgS0jDlE5HIMSRxrDhu6NjIHTDrW5oCiAgICBwcmludGxuKCItLS0iKQogICAgdmFsIHYgPSBleHBlbnNpdmUoKSAgICAgICAgICAgICAvLyBtdeG7kW4gbHXDtG4gY2jhuqF5IHRow6wgdMOhY2ggcmEgdHLGsOG7m2MKICAgIGxpc3Q_LmFkZCh2KQp9Cg)

### K097 · Smart cast không được với property `by` (vd `mutableStateOf`)

`🟡 đã làm app` · `Kotlin` · `Compose`

**Gặp:** `Smart cast to 'String' is impossible, because 'email' is a delegated property.` Hay gặp với
`var email by mutableStateOf<String?>(null)` trong ViewModel/Compose.

**Vì sao:** mỗi lần đọc property `by` là gọi `getValue()` của delegate, có thể ra giá trị khác. Compiler không smart cast được.

**Sửa:** đọc một lần vào biến cục bộ, hoặc `?.let`.

```kotlin
if (email != null) send(email)        // ❌
email?.let { send(it) }               // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbi5wcm9wZXJ0aWVzLkRlbGVnYXRlcwoKY2xhc3MgRm9ybSB7CiAgICB2YXIgZW1haWw6IFN0cmluZz8gYnkgRGVsZWdhdGVzLm9ic2VydmFibGUobnVsbCkgeyBfLCBfLCBuZXcgLT4gcHJpbnRsbigiICDEkeG7lWk6ICRuZXciKSB9Cn0KCmZ1biBtYWluKCkgewogICAgdmFsIGYgPSBGb3JtKCkKICAgIGYuZW1haWwgPSAiYUBiLmMiCiAgICBpZiAoZi5lbWFpbCAhPSBudWxsKSB7CiAgICAgICAgcHJpbnRsbihmLmVtYWlsLmxlbmd0aCkgICAgICAgIC8vIOKdjCBTbWFydCBjYXN0IHRvICdTdHJpbmcnIGlzIGltcG9zc2libGUuLi4gZGVsZWdhdGVkIHByb3BlcnR5CiAgICB9CiAgICAvLyDinIUgZi5lbWFpbD8ubGV0IHsgcHJpbnRsbihpdC5sZW5ndGgpIH0KfQo) · Xem thêm: K059, [Compose: state](12-compose-state.md)

### K098 · `Backing field of 'var token: String' is not accessible at this point`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** gọi `session::token.isInitialized` từ ngoài class báo `Backing field of 'var token: String' is not accessible at this point.`

**Vì sao:** `isInitialized` cần đọc thẳng backing field, nên chỉ dùng được ở nơi thấy field đó (trong chính class, hoặc cùng file
với property top-level).

**Sửa:** thêm một property trong class: `val hasToken get() = ::token.isInitialized`. Hoặc đổi thành `var token: String? = null`.

```kotlin
class Session {
    lateinit var token: String
    val isLoggedIn get() = ::token.isInitialized   // ✅
}
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgU2Vzc2lvbiB7CiAgICBsYXRlaW5pdCB2YXIgdG9rZW46IFN0cmluZwp9CgpmdW4gaXNMb2dnZWRJbihzOiBTZXNzaW9uKSA9IHM6OnRva2VuLmlzSW5pdGlhbGl6ZWQgICAvLyDinYwga2jDtG5nIMSRxrDhu6NjIGtp4buDbSBsYXRlaW5pdCBj4bunYSBjbGFzcyBraMOhYwoKZnVuIG1haW4oKSA9IHByaW50bG4oaXNMb2dnZWRJbihTZXNzaW9uKCkpKQo) · Xem thêm: K071

### K099 · `NotImplementedError: An operation is not implemented`

`🟢 mới học` · `Kotlin`

**Gặp:** `kotlin.NotImplementedError: An operation is not implemented: chưa làm phần avatar` dù ngay sau có `?: "mặc định"`.

**Vì sao:** `TODO()` (IDE hay tự sinh khi implement interface) **ném lỗi**, không trả `null`. `?:` chỉ bắt giá trị `null`, không bắt exception.

**Sửa:** trước khi chạy thử, tìm hết `TODO(` còn sót (IDE: **View → Tool Windows → TODO**). Chưa làm thì trả giá trị tạm có chủ đích (`null`, `emptyList()`).

```kotlin
fun loadAvatar(id: Int): String? = TODO()     // ❌ ném lỗi
fun loadAvatar(id: Int): String? = null       // ✅ tạm, chưa làm
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIGxvYWRBdmF0YXIodXNlcklkOiBJbnQpOiBTdHJpbmc_ID0gVE9ETygiY2jGsGEgbMOgbSBwaOG6p24gYXZhdGFyIikKCmZ1biBtYWluKCkgewogICAgcHJpbnRsbigiYuG6r3QgxJHhuqd1IikKICAgIHByaW50bG4obG9hZEF2YXRhcigxKSA_OiAibeG6t2MgxJHhu4tuaCIpICAgLy8g4p2MIE5vdEltcGxlbWVudGVkRXJyb3IsID86IGtow7RuZyBj4bupdSDEkcaw4bujYwp9Cg)

### K100 · `readLine()!!` crash, `readln()` ném `ReadAfterEOFException`

`🟢 mới học` · `Kotlin`

**Gặp:** bài tập console chạy trong IDE thì được, chạy trong Playground, test hay khi chuyển input từ file thì crash `NullPointerException`
hoặc `kotlin.io.ReadAfterEOFException: EOF has already been reached`.

**Vì sao:** `readLine()` trả `String?`, `null` khi hết input (EOF). `readln()` ném lỗi khi hết input. Playground không có bàn phím nên luôn là EOF.

**Sửa:** `readlnOrNull()` rồi xử lý `null`, và `toIntOrNull()` cho phần đổi số.

```kotlin
val n = readLine()!!.toInt()                     // ❌
val n = readlnOrNull()?.toIntOrNull() ?: 0       // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICAvLyBQbGF5Z3JvdW5kIGtow7RuZyBjw7MgYsOgbiBwaMOtbTogcmVhZExpbmUoKSB0cuG6oyBudWxsLCBnaeG7kW5nIGjhur90IGlucHV0IChFT0YpCiAgICB2YWwgbGluZSA9IHJlYWRMaW5lKCkKICAgIHByaW50bG4oIsSR4buNYyDEkcaw4bujYzogJGxpbmUiKQogICAgcHJpbnRsbihyZWFkbG5Pck51bGwoKT8udG9JbnRPck51bGwoKSA_OiAwKSAgIC8vIOKchSAwCiAgICBwcmludGxuKHJlYWRsbigpLnRvSW50KCkpICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIFJlYWRBZnRlckVPRkV4Y2VwdGlvbgp9Cg) · Xem thêm: K009
