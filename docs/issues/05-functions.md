# 5. Hàm, lambda, generics (K201–K250)

Lỗi quanh cách Kotlin truyền hàm như giá trị: `return` trong lambda, 5 scope function trông giống nhau, `inline` /
`reified`, extension, và generics với `in` / `out`. Phần lớn là lỗi compile có câu báo rõ, chỉ cần biết nó nói gì;
vài mục còn lại compile được nhưng chạy sai, nên đọc kỹ hơn.

### K201 · `return` trong `forEach` thoát luôn cả hàm

`🟢 mới học` · `Kotlin`

**Gặp:** muốn "bỏ qua phần tử này" như `continue`, nhưng hàm kết thúc luôn, các dòng sau vòng lặp không chạy.

**Vì sao:** `forEach` là hàm `inline`. `return` trần trong lambda của hàm inline là **non-local return**: nó thoát khỏi
hàm bên ngoài (`fun` gần nhất), không phải khỏi lambda.

**Sửa:** dùng `return@forEach` để chỉ kết thúc lần chạy lambda hiện tại, hoặc viết vòng `for` bình thường.

```kotlin
list.forEach {
    if (it % 2 == 0) return           // ❌ thoát cả hàm
    if (it % 2 == 0) return@forEach   // ✅ giống continue
}
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIGZpcnN0RXZlbihsaXN0OiBMaXN0PEludD4pIHsKICAgIGxpc3QuZm9yRWFjaCB7CiAgICAgICAgaWYgKGl0ICUgMiA9PSAwKSByZXR1cm4gICAgICAgICAgLy8g4p2MIHRob8OhdCBsdcO0biBmaXJzdEV2ZW4sICJ4b25nIiBraMO0bmcgYmFvIGdp4budIGluCiAgICAgICAgLy8gaWYgKGl0ICUgMiA9PSAwKSByZXR1cm5AZm9yRWFjaCAgLy8g4pyFIMSR4buVaSBkw7JuZyB0csOqbiB0aMOgbmggZMOybmcgbsOgeTogY2jhu4kgYuG7jyBxdWEgcGjhuqduIHThu60gY2jhurVuCiAgICAgICAgcHJpbnRsbigibOG6uzogJGl0IikKICAgIH0KICAgIHByaW50bG4oInhvbmciKQp9CgpmdW4gbWFpbigpIHsKICAgIGZpcnN0RXZlbihsaXN0T2YoMSwgMiwgMywgNSkpCn0K) · Xem thêm: [Phụ lục 2.7](../reference/kotlin-pitfalls.md), K202, K203

### K202 · `'return' is prohibited here`

`🟢 mới học` · `Kotlin`

**Gặp:** compiler báo `'return' is prohibited here.` (bản cũ: `'return' is not allowed here`) ở một `return` nằm trong
lambda truyền cho callback, listener, `fun interface`.

**Vì sao:** chỉ lambda truyền cho hàm `inline` mới được `return` trần. Lambda thường có thể chạy sau khi hàm ngoài đã
kết thúc, nên "thoát hàm ngoài" không có nghĩa.

**Sửa:** thoát lambda bằng nhãn: `return@tênHàm` (tên hàm nhận lambda), hoặc đảo điều kiện thành `if` bọc phần còn lại.

```kotlin
load { value ->
    if (value < 0) return        // ❌
    if (value < 0) return@load   // ✅
    println(value)
}
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIGludGVyZmFjZSBDYWxsYmFjayB7IGZ1biBvbkRvbmUodmFsdWU6IEludCkgfQoKZnVuIGxvYWQoY2I6IENhbGxiYWNrKSA9IGNiLm9uRG9uZSg0MikKCmZ1biBtYWluKCkgewogICAgbG9hZCB7IHZhbHVlIC0-CiAgICAgICAgaWYgKHZhbHVlIDwgMCkgcmV0dXJuICAgICAgICAgICAgLy8g4p2MICdyZXR1cm4nIGlzIG5vdCBhbGxvd2VkIGhlcmUKICAgICAgICAvLyBpZiAodmFsdWUgPCAwKSByZXR1cm5AbG9hZCAgICAgLy8g4pyFIMSR4buVaSBkw7JuZyB0csOqbiB0aMOgbmggZMOybmcgbsOgeQogICAgICAgIHByaW50bG4oIm5o4bqtbiAkdmFsdWUiKQogICAgfQp9Cg) · Xem thêm: K201

### K203 · Trả giá trị sớm từ lambda của `map` / `filter`

`🟡 đã làm app` · `Kotlin`

**Gặp:** trong `map { }` muốn "gặp dữ liệu hỏng thì trả 0" nhưng `return 0` báo lỗi hoặc thoát luôn hàm ngoài.

**Vì sao:** giá trị của lambda là **biểu thức cuối cùng**. Muốn trả sớm phải dùng `return@nhãn giá trị`.

**Sửa:** `return@map 0`. Khi lồng nhiều lambda, tự đặt nhãn `tên@{ ... }` cho rõ đang thoát lambda nào.

```kotlin
val numbers = raw.map {
    val n = it.toIntOrNull() ?: return@map 0   // ✅
    n * 10
}
val pairs = a.flatMap outer@{ x -> b.map inner@{ y -> if (y == 0) return@inner -1; x / y } }
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgcmF3ID0gbGlzdE9mKCIxMiIsICJhYmMiLCAiNyIpCiAgICB2YWwgbnVtYmVycyA9IHJhdy5tYXAgewogICAgICAgIHZhbCBuID0gaXQudG9JbnRPck51bGwoKQogICAgICAgIGlmIChuID09IG51bGwpIHJldHVybkBtYXAgMCAgICAgIC8vIOKchSB0cuG6oyAwIGNobyBwaOG6p24gdOG7rSBo4buPbmcsIG1hcCB24bqrbiBjaOG6oXkgdGnhur9wCiAgICAgICAgbiAqIDEwCiAgICB9CiAgICBwcmludGxuKG51bWJlcnMpICAgICAgICAgICAgICAgICAgICAgLy8gWzEyMCwgMCwgNzBdCgogICAgLy8gTmjDo24gdOG7sSDEkeG6t3Qga2hpIGzhu5NuZyBuaGnhu4F1IGxhbWJkYQogICAgdmFsIHBhaXJzID0gbGlzdE9mKDEsIDIpLmZsYXRNYXAgb3V0ZXJAeyBhIC0-CiAgICAgICAgbGlzdE9mKDEwLCAyMCkubWFwIGlubmVyQHsgYiAtPgogICAgICAgICAgICBpZiAoYiA9PSAyMCkgcmV0dXJuQGlubmVyIC0xCiAgICAgICAgICAgIGEgKyBiCiAgICAgICAgfQogICAgfQogICAgcHJpbnRsbihwYWlycykgICAgICAgICAgICAgICAgICAgICAgIC8vIFsxMSwgLTEsIDEyLCAtMV0KfQo)

### K204 · Lambda trả `Unit` vì dòng cuối là `println`

`🟢 mới học` · `Kotlin`

**Gặp:** `Return type mismatch: expected 'Int', actual 'Unit'.` ở lambda khai báo kiểu `(Int) -> Int`, hoặc ở chỗ khác
nhận được `kotlin.Unit` thay vì con số.

**Vì sao:** lambda không có `return`: giá trị trả về là **dòng cuối cùng**. Thêm `println` hay `log` vào cuối để debug
là đổi luôn kết quả của lambda thành `Unit`.

**Sửa:** để biểu thức kết quả ở dòng cuối, log đặt trước nó (hoặc dùng `.also { println(it) }`).

```kotlin
val double: (Int) -> Int = { x ->
    val result = x * 2
    println(result)        // ❌ dòng cuối → Unit
    result                 // ✅ dòng cuối là giá trị trả về
}
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgZG91YmxlOiAoSW50KSAtPiBJbnQgPSB7IHggLT4KICAgICAgICB2YWwgcmVzdWx0ID0geCAqIDIKICAgICAgICBwcmludGxuKCJkb3VibGUoJHgpID0gJHJlc3VsdCIpICAvLyDinYwgZMOybmcgY3Xhu5FpIGzDoCBwcmludGxuIOKGkiBsYW1iZGEgdHLhuqMgVW5pdAogICAgICAgIC8vIHJlc3VsdCAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinIUgdGjDqm0gZMOybmcgbsOgeSBsw6BtIGTDsm5nIGN14buRaQogICAgfQogICAgcHJpbnRsbihkb3VibGUoNCkpCn0K)

### K205 · `break` trong `forEach`: `'break' and 'continue' are only allowed inside loops`

`🟢 mới học` · `Kotlin`

**Gặp:** `'break' and 'continue' are only allowed inside loops.` khi viết `break` trong `list.forEach { }`.

**Vì sao:** `forEach` là một **hàm** nhận lambda, không phải vòng lặp. Từ Kotlin 2.2, `break`/`continue` trong lambda
inline chỉ được phép khi nó nhắm tới một vòng `for`/`while` **bao bên ngoài** lambda.

**Sửa:** cần dừng giữa chừng thì viết `for`. Cần "bỏ qua" thì `return@forEach`. Cần tìm phần tử đầu tiên thì dùng
`first { }`, `takeWhile { }`.

```kotlin
list.forEach { if (it == 4) break }        // ❌
for (x in list) { if (x == 4) break }       // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgbGlzdCA9IGxpc3RPZigxLCAyLCAzLCA0LCA1KQogICAgbGlzdC5mb3JFYWNoIHsKICAgICAgICBpZiAoaXQgPT0gNCkgYnJlYWsgICAgICAgICAgICAgICAvLyDinYwgZm9yRWFjaCBraMO0bmcgcGjhuqNpIHbDsm5nIGzhurdwCiAgICAgICAgcHJpbnRsbihpdCkKICAgIH0KICAgIC8vIOKchSDEkeG7lWkga2jhu5FpIHRyw6puIHRow6BuaCB2w7JuZyBmb3I6CiAgICAvLyBmb3IgKHggaW4gbGlzdCkgewogICAgLy8gICAgIGlmICh4ID09IDQpIGJyZWFrCiAgICAvLyAgICAgcHJpbnRsbih4KQogICAgLy8gfQp9Cg) · [▶ Chạy thử: `break` nhắm vòng `for` bên ngoài](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgcm93cyA9IGxpc3RPZihsaXN0T2YoMSwgMiksIGxpc3RPZigzLCAtMSwgNCksIGxpc3RPZig1KSkKICAgIGZvciAocm93IGluIHJvd3MpIHsKICAgICAgICByb3cuZm9yRWFjaCB7CiAgICAgICAgICAgIGlmIChpdCA8IDApIGJyZWFrICAgICAgICAgICAgLy8gS290bGluIDIuMis6IHRob8OhdCB2w7JuZyBmb3IgYsOqbiBuZ2_DoGkKICAgICAgICAgICAgcHJpbnRsbihpdCkKICAgICAgICB9CiAgICB9Cn0K) · Xem thêm: K201

### K206 · `val double = { it * 2 }` báo `Unresolved reference 'it'`

`🟢 mới học` · `Kotlin`

**Gặp:** `Unresolved reference 'it'.` kèm vài lỗi lạ khác (`Too many arguments for 'fun invoke()'`) ở một lambda gán
vào `val`.

**Vì sao:** `it` chỉ có khi compiler **biết trước** lambda nhận đúng một tham số, nhờ kiểu mong đợi (tham số của `map`,
kiểu của biến). Gán lambda trần vào `val` thì không có kiểu nào để đoán, nên lambda bị hiểu là không có tham số.

**Sửa:** ghi kiểu tham số, hoặc ghi kiểu của biến.

```kotlin
val double = { it * 2 }                    // ❌
val double = { x: Int -> x * 2 }           // ✅
val double: (Int) -> Int = { it * 2 }      // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgZG91YmxlID0geyBpdCAqIDIgfSAgICAgICAgICAgICAgLy8g4p2MIGNvbXBpbGVyIGtow7RuZyBiaeG6v3QgYGl0YCBsw6Aga2nhu4N1IGfDrAogICAgLy8gdmFsIGRvdWJsZSA9IHsgeDogSW50IC0-IHggKiAyIH0gIC8vIOKchSBnaGkga2nhu4N1IHRoYW0gc-G7kQogICAgLy8gdmFsIGRvdWJsZTogKEludCkgLT4gSW50ID0geyBpdCAqIDIgfSAvLyDinIUgaG_hurdjIGdoaSBraeG7g3UgY-G7p2EgYmnhur9uCiAgICBwcmludGxuKGRvdWJsZSgyMSkpCn0K) · Xem thêm: [Lambda & hàm bậc cao](../concepts/lambda.md)

### K207 · Lambda hai tham số không có `it`

`🟢 mới học` · `Kotlin`

**Gặp:** `Argument type mismatch: actual type is '() -> Unit', but '(Int, String) -> Unit' was expected.` và
`Unresolved reference 'it'.` khi dùng `forEachIndexed`, `mapIndexed`, `Map.forEach`, `fold`.

**Vì sao:** `it` chỉ tồn tại cho lambda **một** tham số. Lambda nhận 2 tham số phải đặt tên cả hai.

**Sửa:** đặt tên tham số. Không dùng tham số nào thì đặt `_`.

```kotlin
names.forEachIndexed { println(it) }                    // ❌
names.forEachIndexed { index, name -> println("$index: $name") } // ✅
names.forEachIndexed { _, name -> println(name) }       // ✅ bỏ index
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgbmFtZXMgPSBsaXN0T2YoIkFuIiwgIkLDrG5oIikKICAgIG5hbWVzLmZvckVhY2hJbmRleGVkIHsKICAgICAgICBwcmludGxuKCIkaXQiKSAgICAgICAgICAgICAgICAgICAvLyDinYwgbGFtYmRhIDIgdGhhbSBz4buRIGtow7RuZyBjw7MgYGl0YAogICAgfQogICAgLy8gbmFtZXMuZm9yRWFjaEluZGV4ZWQgeyBpbmRleCwgbmFtZSAtPiBwcmludGxuKCIkaW5kZXg6ICRuYW1lIikgfSAvLyDinIUKfQo)

### K208 · `val total = { 1 + 2 }` in ra chữ lạ thay vì `3`

`🟢 mới học` · `Kotlin`

**Gặp:** log in ra kiểu `FileKt$$Lambda$1/0x...@4517d9a3` hoặc `() -> kotlin.Int` thay vì con số.

**Vì sao:** cặp ngoặc nhọn `{ }` ở vị trí biểu thức tạo ra **một lambda** (một hàm chưa chạy), không phải khối lệnh
như Java/Swift. Biến đó giữ hàm, chưa giữ kết quả.

**Sửa:** bỏ ngoặc nhọn. Cần nhiều dòng để tính thì dùng `run { }`, nó chạy khối lệnh ngay và trả dòng cuối.

```kotlin
val total = { 1 + 2 }        // ❌ một lambda
val total = 1 + 2            // ✅
val total = run { 1 + 2 }    // ✅ khối lệnh chạy ngay
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgdG90YWwgPSB7IDEgKyAyIH0gICAgICAgICAgICAgICAgLy8g4p2MIMSRw6J5IGzDoCBt4buZdCBsYW1iZGEsIGNoxrBhIHBo4bqjaSBz4buRIDMKICAgIHByaW50bG4odG90YWwpICAgICAgICAgICAgICAgICAgICAgICAvLyBpbiByYSBraeG7g3UgaMOgbSwga2jDtG5nIHBo4bqjaSAzCiAgICBwcmludGxuKHRvdGFsKCkpICAgICAgICAgICAgICAgICAgICAgLy8gMyDigJQgcGjhuqNpIGfhu41pCiAgICB2YWwgc3VtID0gMSArIDIgICAgICAgICAgICAgICAgICAgICAgLy8g4pyFIG114buRbiBnacOhIHRy4buLIHRow6wgYuG7jyBuZ2_hurdjIG5o4buNbgogICAgcHJpbnRsbihzdW0pCiAgICB2YWwgc3VtMiA9IHJ1biB7IDEgKyAyIH0gICAgICAgICAgICAgLy8g4pyFIGPhuqduIGto4buRaSBs4buHbmggdGjDrCBkw7luZyBydW4geyB9CiAgICBwcmludGxuKHN1bTIpCn0K)

### K209 · `fun onSave() = { ... }`: gọi hàm mà không có gì xảy ra

`🟢 mới học` · `Kotlin`

**Gặp:** bấm nút gọi `onSave()` nhưng code bên trong không chạy, không lỗi, không log.

**Vì sao:** `fun f() = { ... }` là hàm một biểu thức mà biểu thức là **lambda**. Gọi `f()` chỉ tạo lambda rồi bỏ đi,
phải `f()()` mới chạy. Hay gặp ở người quen cú pháp arrow function của JavaScript.

**Sửa:** bỏ dấu `=` khi thân hàm là khối lệnh.

```kotlin
fun onSave() = { println("đã lưu") }   // ❌ trả về () -> Unit
fun onSave() { println("đã lưu") }     // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG9uU2F2ZUNsaWNrKCkgPSB7ICAgICAgICAgICAgICAgICAgICAvLyDinYwgYD0ge2Ag4oaSIGjDoG0gVFLhuqIgVuG7gCBt4buZdCBsYW1iZGEKICAgIHByaW50bG4oIsSRw6MgbMawdSIpCn0KLy8gZnVuIG9uU2F2ZUNsaWNrKCkgeyAgICAgICAgICAgICAgICAgICAvLyDinIUgYuG7jyBk4bqldSA9Ci8vICAgICBwcmludGxuKCLEkcOjIGzGsHUiKQovLyB9CgpmdW4gbWFpbigpIHsKICAgIG9uU2F2ZUNsaWNrKCkgICAgICAgICAgICAgICAgICAgICAgICAvLyBraMO0bmcgaW4gZ8OsOiBjaOG7iSB04bqhbyBsYW1iZGEgcuG7k2kgduG7qXQKICAgIHByaW50bG4oIi0tLSBn4buNaSB0aMOqbSAoKSBt4bubaSBjaOG6oXk6IikKICAgIG9uU2F2ZUNsaWNrKCkoKQp9Cg)

### K210 · Truyền `sayHi()` thay vì `::sayHi` cho callback

`🟢 mới học` · `Kotlin`

**Gặp:** `Argument type mismatch: actual type is 'Unit', but '() -> Unit' was expected.` Nếu tham số có kiểu `Any`
thì không lỗi, nhưng hàm chạy ngay lúc đăng ký thay vì lúc bấm.

**Vì sao:** `sayHi()` là **gọi** hàm, kết quả (`Unit`) mới được truyền đi. Callback cần chính cái hàm.

**Sửa:** truyền tham chiếu hàm `::sayHi`, hoặc bọc trong lambda `{ sayHi() }`.

```kotlin
button.setOnClick(sayHi())     // ❌ chạy ngay
button.setOnClick(::sayHi)     // ✅
button.setOnClick { sayHi() }  // ✅
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgQnV0dG9uIHsKICAgIHByaXZhdGUgdmFyIG9uQ2xpY2s6ICgpIC0-IFVuaXQgPSB7fQogICAgZnVuIHNldE9uQ2xpY2soYWN0aW9uOiAoKSAtPiBVbml0KSB7IG9uQ2xpY2sgPSBhY3Rpb24gfQogICAgZnVuIHRhcCgpID0gb25DbGljaygpCn0KCmZ1biBzYXlIaSgpIHsgcHJpbnRsbigieGluIGNow6BvIikgfQoKZnVuIG1haW4oKSB7CiAgICB2YWwgYnV0dG9uID0gQnV0dG9uKCkKICAgIGJ1dHRvbi5zZXRPbkNsaWNrKHNheUhpKCkpICAgICAgICAgICAvLyDinYwgZ-G7jWkgc2F5SGkoKSBOR0FZLCBy4buTaSB0cnV54buBbiBVbml0CiAgICAvLyBidXR0b24uc2V0T25DbGljayg6OnNheUhpKSAgICAgICAgLy8g4pyFIHRoYW0gY2hp4bq_dSBow6BtLCBjaMawYSBjaOG6oXkKICAgIC8vIGJ1dHRvbi5zZXRPbkNsaWNrIHsgc2F5SGkoKSB9ICAgICAvLyDinIUgaG_hurdjIGxhbWJkYQogICAgYnV0dG9uLnRhcCgpCn0K)

### K211 · Lambda bắt biến `var`: tất cả in cùng một giá trị

`🟡 đã làm app` · `Kotlin`

**Gặp:** tạo 3 callback trong vòng `while`, lúc chạy cả 3 đều in `i = 3`.

**Vì sao:** lambda bắt **biến**, không chụp lại giá trị. Khi lambda chạy, nó đọc giá trị hiện tại của biến, lúc đó đã
là 3. (Vòng `for (i in 0..2)` không bị vì mỗi vòng có một `i` mới.)

**Sửa:** chép ra một `val` trong vòng lặp rồi cho lambda dùng `val` đó, hoặc dùng `for`/`repeat`.

```kotlin
while (i < 3) {
    actions += { println(i) }        // ❌ đọc i lúc chạy
    val copy = i
    actions += { println(copy) }     // ✅ mỗi vòng một val
    i++
}
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgYWN0aW9ucyA9IG11dGFibGVMaXN0T2Y8KCkgLT4gVW5pdD4oKQogICAgdmFyIGkgPSAwCiAgICB3aGlsZSAoaSA8IDMpIHsKICAgICAgICBhY3Rpb25zICs9IHsgcHJpbnRsbigi4p2MIGkgPSAkaSIpIH0gICAvLyBi4bqvdCBCSeG6vk4gaSwga2jDtG5nIHBo4bqjaSBnacOhIHRy4buLIGzDumMgbsOgeQogICAgICAgIHZhbCBjb3B5ID0gaQogICAgICAgIGFjdGlvbnMgKz0geyBwcmludGxuKCLinIUgY29weSA9ICRjb3B5IikgfSAvLyBi4bqvdCBt4buZdCB2YWwgbeG7m2kgbeG7l2kgdsOybmcKICAgICAgICBpKysKICAgIH0KICAgIGFjdGlvbnMuZm9yRWFjaCB7IGl0KCkgfQp9Cg)

### K212 · Gọi callback nullable: `Reference has a nullable type`

`🟢 mới học` · `Kotlin`

**Gặp:** `Reference has a nullable type '(() -> Unit)?'. Use explicit '?.invoke' to make a function-like call instead.`

**Vì sao:** `onClose: (() -> Unit)?` có thể null, nên không gọi thẳng `onClose()` được.

**Sửa:** `onClose?.invoke()`. Hay hơn nữa: cho tham số mặc định là lambda rỗng để khỏi nullable.

```kotlin
class Dialog(val onClose: (() -> Unit)? = null)  // phải gọi onClose?.invoke()
class Dialog(val onClose: () -> Unit = {})       // ✅ gọi onClose() thoải mái
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgRGlhbG9nKHZhbCBvbkNsb3NlOiAoKCkgLT4gVW5pdCk_ID0gbnVsbCkKCmZ1biBtYWluKCkgewogICAgdmFsIGQgPSBEaWFsb2coKQogICAgZC5vbkNsb3NlKCkgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBraMO0bmcgZ-G7jWkgdHLhu7FjIHRp4bq_cCDEkcaw4bujYyBraeG7g3UgaMOgbSBudWxsYWJsZQogICAgLy8gZC5vbkNsb3NlPy5pbnZva2UoKSAgICAgICAgICAgICAgIC8vIOKchSDEkeG7lWkgZMOybmcgdHLDqm4gdGjDoG5oIGTDsm5nIG7DoHkKICAgIHByaW50bG4oIsSRw7NuZyB4b25nIikKfQo)

### K213 · Truyền lambda cho `interface` Kotlin thường bị báo lỗi

`🟢 mới học` · `Kotlin`

**Gặp:** `Argument type mismatch: actual type is '(???) -> ???', but 'OnTap' was expected.` và
`Cannot infer type for value parameter 'x'.` Cùng kiểu đó với interface Java thì lại được.

**Vì sao:** SAM conversion (lambda → interface một hàm) áp dụng cho interface Java, còn interface Kotlin phải khai báo
là `fun interface`. `fun interface` chỉ được có **đúng một** hàm abstract.

**Sửa:** thêm `fun` trước `interface`, hoặc đổi tham số sang kiểu hàm `(Int) -> Unit`.

```kotlin
interface OnTap { fun tap(x: Int) }       // ❌ không nhận lambda
fun interface OnTap { fun tap(x: Int) }   // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW50ZXJmYWNlIE9uVGFwIHsgZnVuIHRhcCh4OiBJbnQpIH0gICAgICAgIC8vIOKdjCBpbnRlcmZhY2UgdGjGsOG7nW5nOiBraMO0bmcgbmjhuq1uIGxhbWJkYQovLyBmdW4gaW50ZXJmYWNlIE9uVGFwIHsgZnVuIHRhcCh4OiBJbnQpIH0gLy8g4pyFIHRow6ptIGBmdW5gIHRyxrDhu5tjIGludGVyZmFjZQoKZnVuIGxpc3RlbihsOiBPblRhcCkgPSBsLnRhcCg3KQoKZnVuIG1haW4oKSB7CiAgICBsaXN0ZW4geyB4IC0-IHByaW50bG4oInRhcCAkeCIpIH0KfQo) · Xem thêm: K214

### K214 · Lambda cho interface Java nhiều hàm: `AbstractMethodError` trên máy cũ

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** app chạy tốt trên Android mới, nhưng trên Android 10 trở xuống crash với `java.lang.AbstractMethodError` khi hệ
thống gọi một hàm của listener (vd `LocationListener.onStatusChanged`).

**Vì sao:** từ API 30, `LocationListener` có default method cho 3 hàm phụ, nên compile thấy nó là SAM và cho viết lambda.
Lambda chỉ cài **một** hàm. Trên máy cũ 3 hàm kia vẫn abstract, hệ thống gọi tới là crash.

**Sửa:** với listener Java có nhiều hàm qua các bản Android, viết `object :` cài đủ mọi hàm.

```kotlin
val listener = LocationListener { loc -> trySend(loc) }   // ❌ crash API < 30
val listener = object : LocationListener {                 // ✅
    override fun onLocationChanged(location: Location) { trySend(location) }
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit
    @Deprecated("API 29") override fun onStatusChanged(p: String?, s: Int, e: Bundle?) = Unit
}
```

**Gặp ở:** [`apps/TripLog/shared/src/androidMain/kotlin/dev/triplog/platform/AndroidLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/androidMain/kotlin/dev/triplog/platform/AndroidLocationStream.kt)

### K215 · `let` trả về kết quả lambda, không trả về object

`🟢 mới học` · `Kotlin`

**Gặp:** `val user = User().let { it.name = "Tú" }` rồi `user.name` báo lỗi, hoặc in ra `kotlin.Unit`.

**Vì sao:** 5 scope function khác nhau ở 2 điểm: gọi object là `this` hay `it`, và trả về **object** hay **kết quả
lambda**. `let`, `run`, `with` trả kết quả lambda; `apply`, `also` trả object.

| Hàm | Trong lambda | Trả về |
|---|---|---|
| `apply` | `this` | object |
| `also` | `it` | object |
| `let` | `it` | kết quả lambda |
| `run` | `this` | kết quả lambda |
| `with(x)` | `this` | kết quả lambda |

**Sửa:** cấu hình object vừa tạo thì dùng `apply`. Làm việc phụ (log) mà vẫn giữ object thì `also`.

```kotlin
val user = User().let { it.name = "Tú" }     // ❌ user là Unit
val user = User().apply { name = "Tú" }      // ✅ user là User
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgVXNlciB7IHZhciBuYW1lID0gIiI7IHZhciBhZ2UgPSAwIH0KCmZ1biBtYWluKCkgewogICAgdmFsIHVzZXIgPSBVc2VyKCkubGV0IHsgICAgICAgICAgICAgIC8vIOKdjCBsZXQgdHLhuqMgduG7gSBr4bq_dCBxdeG6oyBj4bunYSBsYW1iZGEgKOG7nyDEkcOieSBsw6AgVW5pdCkKICAgICAgICBpdC5uYW1lID0gIlTDuiIKICAgICAgICBpdC5hZ2UgPSAzMAogICAgfQogICAgcHJpbnRsbih1c2VyKSAgICAgICAgICAgICAgICAgICAgICAgIC8vIGtvdGxpbi5Vbml0CgogICAgdmFsIHVzZXIyID0gVXNlcigpLmFwcGx5IHsgICAgICAgICAgIC8vIOKchSBhcHBseSB0cuG6oyB24buBIGNow61uaCBvYmplY3QKICAgICAgICBuYW1lID0gIlTDuiIKICAgICAgICBhZ2UgPSAzMAogICAgfQogICAgcHJpbnRsbigiJHt1c2VyMi5uYW1lfSAke3VzZXIyLmFnZX0iKQp9Cg) · Xem thêm: [Phụ lục 2.12](../reference/kotlin-pitfalls.md), K216

### K216 · `this` trong `apply` không còn là class bên ngoài

`🟡 đã làm app` · `Kotlin`

**Gặp:** trong `Sensor().apply { owner = this }` tưởng gán màn hình hiện tại, thực ra gán chính `Sensor`. Listener tự
trỏ vào mình, sự kiện không bao giờ tới màn hình.

**Vì sao:** `apply`, `run`, `with` đổi `this` thành object đang cấu hình. `this` của class bên ngoài bị che.

**Sửa:** gọi rõ `this@TênClass`, hoặc dùng `also` (object là `it`, `this` giữ nguyên).

```kotlin
fun setup() = Sensor().apply {
    owner = this              // ❌ this là Sensor
    owner = this@Screen       // ✅
}
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgU2Vuc29yIHsgdmFyIG93bmVyOiBBbnk_ID0gbnVsbCB9CgpjbGFzcyBTY3JlZW4gewogICAgZnVuIHNldHVwKCk6IFNlbnNvciA9IFNlbnNvcigpLmFwcGx5IHsKICAgICAgICBvd25lciA9IHRoaXMgICAgICAgICAgICAgICAgICAgICAvLyDinYwgdHJvbmcgYXBwbHksIGB0aGlzYCBsw6AgU2Vuc29yCiAgICAgICAgLy8gb3duZXIgPSB0aGlzQFNjcmVlbiAgICAgICAgICAgLy8g4pyFIGfhu41pIHLDtSBvYmplY3QgYsOqbiBuZ2_DoGkKICAgIH0KICAgIG92ZXJyaWRlIGZ1biB0b1N0cmluZygpID0gIlNjcmVlbiIKfQoKZnVuIG1haW4oKSB7CiAgICB2YWwgc2Vuc29yID0gU2NyZWVuKCkuc2V0dXAoKQogICAgcHJpbnRsbigib3duZXIgbMOgOiAke3NlbnNvci5vd25lcn0iKQogICAgcHJpbnRsbigib3duZXIgPT09IHNlbnNvcjogJHtzZW5zb3Iub3duZXIgPT09IHNlbnNvcn0iKQp9Cg) · Xem thêm: [Extension & lambda có receiver](../concepts/extension-receiver.md)

### K217 · `x?.let { } ?: run { }` chạy cả hai nhánh

`🟡 đã làm app` · `Kotlin`

**Gặp:** có dữ liệu, nhánh `let` đã chạy, nhưng nhánh `?: run { }` ("không có dữ liệu") cũng chạy theo.

**Vì sao:** `?:` xét **kết quả của `let`**, không xét `x`. Dòng cuối trong `let` trả `null` (vd một hàm trả `String?`)
thì vế phải của `?:` cũng chạy.

**Sửa:** nghĩa là "if/else" thì viết `if (x != null) ... else ...`. Smart cast vẫn hoạt động với `val`.

```kotlin
text?.let { save(it) } ?: run { showEmpty() }   // ❌ save trả null → chạy cả showEmpty
if (text != null) save(text) else showEmpty()   // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIHNhdmUoczogU3RyaW5nKTogU3RyaW5nPyB7IHByaW50bG4oImzGsHUgJHMiKTsgcmV0dXJuIG51bGwgfSAvLyB0cuG6oyBudWxsIGtoaSAia2jDtG5nIGPDsyBs4buXaSIKCmZ1biBtYWluKCkgewogICAgdmFsIHRleHQ6IFN0cmluZz8gPSAiZ2hpIGNow7oiCiAgICB0ZXh0Py5sZXQgeyBzYXZlKGl0KSB9ID86IHJ1biB7IHByaW50bG4oIuKdjCBraMO0bmcgY8OzIHRleHQiKSB9ICAvLyBjaOG6oXkgQ-G6oiBoYWk6IGxldCB0cuG6oyBudWxsCgogICAgLy8g4pyFIGlmL2Vsc2U6IGNo4buJIG3hu5l0IG5ow6FuaCBjaOG6oXkKICAgIGlmICh0ZXh0ICE9IG51bGwpIHNhdmUodGV4dCkgZWxzZSBwcmludGxuKCJraMO0bmcgY8OzIHRleHQiKQp9Cg)

### K218 · `with(nullable)`: `Only safe (?.) or non-null asserted (!!.) calls are allowed`

`🟢 mới học` · `Kotlin`

**Gặp:** `Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'Profile?'.` ở mọi
dòng bên trong `with(p) { }`.

**Vì sao:** `with` không kiểm null. `this` trong khối có kiểu `Profile?`, nên từng lần dùng phải `?.`.

**Sửa:** dùng `p?.run { }`: chỉ chạy khi `p` khác null, và bên trong `this` là `Profile` không null.

```kotlin
with(p) { println(name) }      // ❌
p?.run { println(name) }       // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZGF0YSBjbGFzcyBQcm9maWxlKHZhbCBuYW1lOiBTdHJpbmcsIHZhbCBjaXR5OiBTdHJpbmcpCgpmdW4gbWFpbigpIHsKICAgIHZhbCBwOiBQcm9maWxlPyA9IFByb2ZpbGUoIlTDuiIsICLEkMOgIE7hurVuZyIpCiAgICB3aXRoKHApIHsKICAgICAgICBwcmludGxuKG5hbWUpICAgICAgICAgICAgICAgICAgICAvLyDinYwgcmVjZWl2ZXIgbnVsbGFibGUKICAgICAgICBwcmludGxuKGNpdHkpCiAgICB9CiAgICAvLyBwPy5ydW4geyAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4pyFIGNo4buJIGNo4bqheSBraGkgcCBraMOhYyBudWxsLCBiw6puIHRyb25nIGB0aGlzYCBraMO0bmcgbnVsbAogICAgLy8gICAgIHByaW50bG4obmFtZSkKICAgIC8vICAgICBwcmludGxuKGNpdHkpCiAgICAvLyB9Cn0K)

### K219 · `it` lồng `it`: đang dùng `it` của lambda nào?

`🟢 mới học` · `Kotlin`

**Gặp:** cảnh báo `Implicit parameter 'it' of enclosing lambda is shadowed`, và code đọc nhầm `it` bên trong là phần tử
bên ngoài.

**Vì sao:** lambda trong cùng một lambda khác đều có `it`; `it` gần nhất che `it` bên ngoài.

**Sửa:** lambda lồng nhau thì đặt tên cho tham số, ít nhất ở tầng ngoài. Lồng hơn 2 tầng scope function thì tách hàm.

```kotlin
orders.forEach { it.forEach { println(it) } }                  // ❌ khó đọc
orders.forEach { order -> order.forEach { price -> println(price) } } // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgb3JkZXJzID0gbGlzdE9mKGxpc3RPZigxMDAsIDIwMCksIGxpc3RPZig1MCkpCiAgICBvcmRlcnMuZm9yRWFjaCB7CiAgICAgICAgaXQuZm9yRWFjaCB7CiAgICAgICAgICAgIHByaW50bG4oImTDsm5nICRpdCIpICAgICAgICAgIC8vIOKdjCBpdCBiw6puIHRyb25nIGNoZSBpdCBiw6puIG5nb8OgaQogICAgICAgIH0KICAgIH0KICAgIC8vIOKchSDEkeG6t3QgdMOqbgogICAgb3JkZXJzLmZvckVhY2hJbmRleGVkIHsgaW5kZXgsIG9yZGVyIC0-CiAgICAgICAgb3JkZXIuZm9yRWFjaCB7IHByaWNlIC0-IHByaW50bG4oIsSRxqFuICRpbmRleDogJHByaWNlIikgfQogICAgfQp9Cg)

### K220 · `apply` sửa luôn object gốc, không tạo bản mới

`🟡 đã làm app` · `Kotlin`

**Gặp:** `val draft = saved.apply { items += "bánh mì" }` rồi thấy `saved` cũng đổi; state cũ trong UI đổi theo,
undo không còn bản cũ để quay về.

**Vì sao:** `apply` trả về **chính** object, `draft === saved`. Không có bản copy nào được tạo.

**Sửa:** state nên là `data class` với `val` và `List` chỉ đọc; muốn đổi thì `copy(...)` ra bản mới.

```kotlin
val draft = saved.apply { items += "bánh mì" }              // ❌ sửa saved
val draft = saved.copy(items = saved.items + "bánh mì")     // ✅ saved giữ nguyên
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgQ2FydCB7IHZhbCBpdGVtcyA9IG11dGFibGVMaXN0T2Y8U3RyaW5nPigpIH0KCmZ1biBtYWluKCkgewogICAgdmFsIHNhdmVkID0gQ2FydCgpLmFwcGx5IHsgaXRlbXMgKz0gImPDoCBwaMOqIiB9CiAgICB2YWwgZHJhZnQgPSBzYXZlZC5hcHBseSB7IGl0ZW1zICs9ICJiw6FuaCBtw6wiIH0gIC8vIOKdjCBhcHBseSBz4butYSBDSMONTkggb2JqZWN0IHNhdmVkCiAgICBwcmludGxuKCJzYXZlZCA9ICR7c2F2ZWQuaXRlbXN9IikKICAgIHByaW50bG4oImRyYWZ0ID09PSBzYXZlZDogJHtkcmFmdCA9PT0gc2F2ZWR9IikKCiAgICAvLyDinIUgbXXhu5FuIGLhuqNuIG3hu5tpIHRow6wgdOG6oW8gYuG6o24gbeG7m2ksIGdp4buvIGLhuqNuIGPFqSBuZ3V5w6puCiAgICBkYXRhIGNsYXNzIENhcnQyKHZhbCBpdGVtczogTGlzdDxTdHJpbmc-KQogICAgdmFsIGEgPSBDYXJ0MihsaXN0T2YoImPDoCBwaMOqIikpCiAgICB2YWwgYiA9IGEuY29weShpdGVtcyA9IGEuaXRlbXMgKyAiYsOhbmggbcOsIikKICAgIHByaW50bG4oImEgPSAke2EuaXRlbXN9LCBiID0gJHtiLml0ZW1zfSIpCn0K) · Xem thêm: [Phụ lục 2.8](../reference/kotlin-pitfalls.md)

### K221 · Tham số mặc định đứng trước: `No value passed for parameter`

`🟢 mới học` · `Kotlin`

**Gặp:** `No value passed for parameter 'name'.` khi gọi `greet("Tú")` với `fun greet(greeting: String = "Xin chào", name: String)`.

**Vì sao:** đối số theo vị trí được điền từ trái sang. `"Tú"` vào `greeting`, còn `name` không có giá trị.

**Sửa:** đưa tham số có mặc định ra cuối, hoặc gọi bằng tên `greet(name = "Tú")`.

```kotlin
fun greet(name: String, greeting: String = "Xin chào")  // ✅ mặc định để cuối
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIGdyZWV0KGdyZWV0aW5nOiBTdHJpbmcgPSAiWGluIGNow6BvIiwgbmFtZTogU3RyaW5nKSA9IHByaW50bG4oIiRncmVldGluZywgJG5hbWUiKQoKZnVuIG1haW4oKSB7CiAgICBncmVldCgiVMO6IikgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCAiVMO6IiB2w6BvIGdyZWV0aW5nLCB0aGnhur91IG5hbWUKICAgIC8vIGdyZWV0KG5hbWUgPSAiVMO6IikgICAgICAgICAgICAgICAgLy8g4pyFIGfhu41pIGLhurFuZyB0w6puCn0K)

### K222 · Truyền mảng / list vào `vararg`

`🟢 mới học` · `Kotlin`

**Gặp:** `Argument type mismatch: actual type is 'Array<String>', but 'String' was expected.`

**Vì sao:** `vararg items: String` nhận từng `String` riêng. Mảng phải được "trải" ra; `List` thì phải đổi sang mảng.

**Sửa:** toán tử spread `*`.

```kotlin
tags(fromApi)                     // ❌
tags(*fromApi)                    // ✅ Array
tags(*list.toTypedArray())        // ✅ List
tags("new", *fromApi)             // ✅ trộn được
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIHRhZ3ModmFyYXJnIGl0ZW1zOiBTdHJpbmcpID0gcHJpbnRsbigiJHtpdGVtcy5zaXplfSB0YWc6ICR7aXRlbXMuam9pblRvU3RyaW5nKCl9IikKCmZ1biBtYWluKCkgewogICAgdmFsIGZyb21BcGkgPSBhcnJheU9mKCJrb3RsaW4iLCAia21wIikKICAgIHZhbCBsaXN0ID0gbGlzdE9mKCJpb3MiLCAiYW5kcm9pZCIpCiAgICB0YWdzKGZyb21BcGkpICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIHRydXnhu4FuIGPhuqMgbeG6o25nIG5oxrAgbeG7mXQgcGjhuqduIHThu60KICAgIC8vIHRhZ3MoKmZyb21BcGkpICAgICAgICAgICAgICAgICAgICAvLyDinIUgdHLhuqNpIG3huqNuZyByYSBi4bqxbmcgKgogICAgLy8gdGFncygqbGlzdC50b1R5cGVkQXJyYXkoKSkgICAgICAgIC8vIOKchSBMaXN0IHRow6wgxJHhu5VpIHNhbmcgbeG6o25nIHRyxrDhu5tjCn0K)

### K223 · `Named arguments are prohibited for non-Kotlin functions`

`🟢 mới học` · `Kotlin`

**Gặp:** `Named arguments are prohibited for non-Kotlin functions.` khi gọi `Math.max(a = 3, b = 7)` hay một API
Android/Java bằng tên tham số.

**Vì sao:** file `.class` của Java không chắc giữ tên tham số, nên Kotlin cấm gọi hàm Java bằng tên.

**Sửa:** gọi theo vị trí, hoặc dùng hàm Kotlin tương đương (`maxOf`). Cần tên cho dễ đọc thì viết hàm Kotlin bọc lại.

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgYmlnZ2VyID0gTWF0aC5tYXgoYSA9IDMsIGIgPSA3KSAgLy8g4p2MIGjDoG0gSmF2YSBraMO0bmcgY8OzIHTDqm4gdGhhbSBz4buRCiAgICAvLyB2YWwgYmlnZ2VyID0gbWF4T2YoMywgNykgICAgICAgICAgLy8g4pyFIGTDuW5nIGjDoG0gS290bGluLCBob-G6t2MgYuG7jyB0w6puIHRoYW0gc-G7kQogICAgcHJpbnRsbihiaWdnZXIpCn0K)

### K224 · Sợ giá trị mặc định `mutableListOf()` bị dùng chung như Python

`🟢 mới học` · `Kotlin`

**Gặp:** người từng viết Python né `fun add(item: String, cart: MutableList<String> = mutableListOf())` vì sợ lần gọi sau
thấy dữ liệu lần trước.

**Vì sao:** Kotlin tính lại biểu thức mặc định **mỗi lần gọi**. Mỗi lần gọi không truyền `cart` là một list mới. Bẫy
thật nằm ở chỗ khác: nếu giá trị mặc định là một biến dùng chung (`= sharedCart`) thì nó mới bị dùng chung.

**Sửa:** không cần làm gì với `mutableListOf()`. Tránh đặt mặc định là object toàn cục có thể đổi.

[▶ Chạy thử](../playground/index.html#code=ZnVuIGFkZEl0ZW0oaXRlbTogU3RyaW5nLCBjYXJ0OiBNdXRhYmxlTGlzdDxTdHJpbmc-ID0gbXV0YWJsZUxpc3RPZigpKTogTGlzdDxTdHJpbmc-IHsKICAgIGNhcnQgKz0gaXRlbQogICAgcmV0dXJuIGNhcnQKfQoKZnVuIG1haW4oKSB7CiAgICBwcmludGxuKGFkZEl0ZW0oImPDoCBwaMOqIikpCiAgICBwcmludGxuKGFkZEl0ZW0oImLDoW5oIG3DrCIpKSAgICAgICAgICAvLyBbYsOhbmggbcOsXSDigJQgZGVmYXVsdCB04bqhbyBN4buaSSBt4buXaSBs4bqnbiBn4buNaSAoa2jDoWMgUHl0aG9uKQp9Cg)

### K225 · Đảo thứ tự hai tham số cùng kiểu, compiler không báo

`🟡 đã làm app` · `Kotlin`

**Gặp:** `transfer(shop, me, 500)` chạy được nhưng tiền đi ngược chiều. Code review cũng khó thấy.

**Vì sao:** hai tham số cùng kiểu `Int` thì đổi chỗ vẫn đúng kiểu. Compiler không biết ý bạn.

**Sửa:** gọi bằng tên khi có từ hai tham số cùng kiểu trở lên. Với ID quan trọng, dùng `value class` riêng để compiler
chặn luôn (K239).

```kotlin
transfer(shop, me, 500)                              // ❌ ngược
transfer(fromId = me, toId = shop, amount = 500)     // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIHRyYW5zZmVyKGZyb21JZDogSW50LCB0b0lkOiBJbnQsIGFtb3VudDogSW50KSA9IHByaW50bG4oImNodXnhu4NuICRhbW91bnQgdOG7qyAjJGZyb21JZCBzYW5nICMkdG9JZCIpCgpmdW4gbWFpbigpIHsKICAgIHZhbCBtZSA9IDE7IHZhbCBzaG9wID0gMgogICAgdHJhbnNmZXIoc2hvcCwgbWUsIDUwMCkgICAgICAgICAgICAgIC8vIOKdjCDEkeG6o28gdGjhu6kgdOG7sSwgY29tcGlsZXIga2jDtG5nIGLDoW8gdsOsIGPDuW5nIGtp4buDdSBJbnQKICAgIHRyYW5zZmVyKGZyb21JZCA9IG1lLCB0b0lkID0gc2hvcCwgYW1vdW50ID0gNTAwKSAvLyDinIUgZ-G7jWkgYuG6sW5nIHTDqm4ga2hpIG5oaeG7gXUgdGhhbSBz4buRIGPDuW5nIGtp4buDdQp9Cg) · Xem thêm: K239

### K226 · Extension không override được

`🟡 đã làm app` · `Kotlin`

**Gặp:** `fun Shape.name()` và `fun Circle.name()`, biến kiểu `Shape` chứa `Circle` vẫn gọi bản của `Shape`.

**Vì sao:** extension là hàm tĩnh, được chọn **lúc compile** theo kiểu khai báo của biến, không theo object thật lúc chạy.

**Sửa:** hành vi khác nhau theo lớp con thì viết `open fun` trong class rồi `override`, hoặc `when (shape) { is Circle -> }`.

[▶ Chạy thử](../playground/index.html#code=b3BlbiBjbGFzcyBTaGFwZQpjbGFzcyBDaXJjbGUgOiBTaGFwZSgpCgpmdW4gU2hhcGUubmFtZSgpID0gInNoYXBlIgpmdW4gQ2lyY2xlLm5hbWUoKSA9ICJjaXJjbGUiCgpmdW4gbWFpbigpIHsKICAgIHZhbCBzOiBTaGFwZSA9IENpcmNsZSgpCiAgICBwcmludGxuKHMubmFtZSgpKSAgICAgICAgICAgICAgICAgICAgLy8gInNoYXBlIjogZXh0ZW5zaW9uIGNo4buNbiB0aGVvIGtp4buDdSBraGFpIGLDoW8KICAgIHByaW50bG4oQ2lyY2xlKCkubmFtZSgpKSAgICAgICAgICAgICAvLyAiY2lyY2xlIgp9Cg) · Xem thêm: [Phụ lục 2.10](../reference/kotlin-pitfalls.md), K227

### K227 · Extension trùng tên member: không bao giờ được gọi

`🟡 đã làm app` · `Kotlin`

**Gặp:** viết `fun Money.toString()` để in đẹp, nhưng log vẫn ra `Money(12345)`. IDE gạch vàng
`Extension is shadowed by a member`.

**Vì sao:** member của class luôn thắng extension cùng chữ ký. Extension đó là code chết.

**Sửa:** đặt tên khác (`format()`), hoặc override member trong class nếu bạn sở hữu class.

```kotlin
fun Money.toString() = "..."     // ❌ bị member che
fun Money.format() = "..."       // ✅
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgTW9uZXkodmFsIGNlbnRzOiBMb25nKSB7CiAgICBvdmVycmlkZSBmdW4gdG9TdHJpbmcoKSA9ICJNb25leSgkY2VudHMpIgp9CgpmdW4gTW9uZXkudG9TdHJpbmcoKTogU3RyaW5nID0gIiR7Y2VudHMgLyAxMDB9LiR7Y2VudHMgJSAxMDB9IMSRIiAvLyDinYwgbWVtYmVyIGx1w7RuIHRo4bqvbmcsIGjDoG0gbsOgeSBraMO0bmcgYmFvIGdp4budIMSRxrDhu6NjIGfhu41pCgpmdW4gTW9uZXkuZm9ybWF0KCk6IFN0cmluZyA9ICIke2NlbnRzIC8gMTAwfS4ke2NlbnRzICUgMTAwfSDEkSIgICAgLy8g4pyFIMSR4bq3dCB0w6puIGtow6FjCgpmdW4gbWFpbigpIHsKICAgIHZhbCBtID0gTW9uZXkoMTIzNDUpCiAgICBwcmludGxuKG0udG9TdHJpbmcoKSkKICAgIHByaW50bG4obS5mb3JtYXQoKSkKfQo)

### K228 · `Extension property cannot be initialized because it has no backing field`

`🟢 mới học` · `Kotlin`

**Gặp:** `Extension property cannot be initialized because it has no backing field.` khi viết `val String.wordCount: Int = 0`.

**Vì sao:** extension không thêm được ô nhớ vào class có sẵn. Extension property chỉ là cặp getter/setter tính từ object.

**Sửa:** viết `get()` tính mỗi lần đọc. Cần lưu thêm dữ liệu thật thì bọc class (`class WordStats(val text: String)`)
hoặc dùng một `Map` bên ngoài.

```kotlin
val String.wordCount: Int = 0                                  // ❌
val String.wordCount: Int get() = split(" ").count { it.isNotBlank() } // ✅
```

[▶ Chạy thử](../playground/index.html#code=dmFsIFN0cmluZy53b3JkQ291bnQ6IEludCA9IDAgICAgICAgICAgIC8vIOKdjCBleHRlbnNpb24gcHJvcGVydHkga2jDtG5nIGPDsyBjaOG7lyBsxrB1IGdpw6EgdHLhu4sKLy8gdmFsIFN0cmluZy53b3JkQ291bnQ6IEludCAgICAgICAgICAgICAvLyDinIUgdMOtbmggbeG7l2kgbOG6p24gxJHhu41jIGLhurFuZyBnZXR0ZXIKLy8gICAgIGdldCgpID0gc3BsaXQoIiAiKS5jb3VudCB7IGl0LmlzTm90QmxhbmsoKSB9CgpmdW4gbWFpbigpIHsKICAgIHByaW50bG4oImjhu41jIEtvdGxpbiBt4buXaSBuZ8OgeSIud29yZENvdW50KQp9Cg)

### K229 · Muốn gọi helper trên biến nullable mà khỏi `?.`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'String?'.` khi gọi
`note.orDash()` với `note: String?`.

**Vì sao:** extension khai báo trên `String` chỉ nhận `String`. Extension có thể khai báo trên **kiểu nullable**, khi đó
bên trong `this` có thể là `null` và phải tự kiểm.

**Sửa:** khai báo receiver `String?`, giống `isNullOrBlank()` của thư viện chuẩn.

```kotlin
fun String.orDash() = ifBlank { "—" }                              // ❌ không nhận null
fun String?.orDash() = if (isNullOrBlank()) "—" else this          // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIFN0cmluZy5vckRhc2goKTogU3RyaW5nID0gaWZCbGFuayB7ICLigJQiIH0gICAgICAgLy8g4p2MIHJlY2VpdmVyIGtow7RuZyBudWxsCi8vIGZ1biBTdHJpbmc_Lm9yRGFzaCgpOiBTdHJpbmcgPSBpZiAoaXNOdWxsT3JCbGFuaygpKSAi4oCUIiBlbHNlIHRoaXMgLy8g4pyFIHJlY2VpdmVyIG51bGxhYmxlCgpmdW4gbWFpbigpIHsKICAgIHZhbCBub3RlOiBTdHJpbmc_ID0gbnVsbAogICAgcHJpbnRsbihub3RlLm9yRGFzaCgpKQp9Cg)

### K230 · `map.forEach { k, v -> }` chạy trên Android, lỗi trong `commonMain`

`🟡 đã làm app` · `KMP`

**Gặp:** code chép từ Android sang `commonMain` báo lỗi kiểu lambda ở `map.forEach { key, value -> }`. Trên Android
`minSdk` < 24, Lint báo `NewApi` ở cùng dòng.

**Vì sao:** lambda **2 tham số** khớp với `java.util.Map.forEach(BiConsumer)` của Java 8, chỉ có trên JVM. `forEach` của
Kotlin trên `Map` nhận **một** `Map.Entry`.

**Sửa:** dùng destructuring trong ngoặc tròn: một tham số được tách làm hai. Chạy mọi nền tảng.

```kotlin
map.forEach { key, value -> println("$key=$value") }     // ❌ API Java, chỉ JVM
map.forEach { (key, value) -> println("$key=$value") }   // ✅ Kotlin, mọi target
```

### K231 · `'operator' modifier is required`

`🟢 mới học` · `Kotlin`

**Gặp:** `'operator' modifier is required on 'fun plus(other: Money): Money' defined in 'Money'.` khi viết `a + b`.

**Vì sao:** Kotlin chỉ cho dùng toán tử (`+`, `[]`, `in`, `()`...) với hàm đánh dấu `operator`. Đặt đúng tên `plus`
chưa đủ.

**Sửa:** thêm `operator`. Dùng toán tử khi nghĩa thật sự rõ (cộng tiền), đừng lạm dụng.

```kotlin
operator fun plus(other: Money) = Money(cents + other.cents)  // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZGF0YSBjbGFzcyBNb25leSh2YWwgY2VudHM6IExvbmcpIHsKICAgIGZ1biBwbHVzKG90aGVyOiBNb25leSkgPSBNb25leShjZW50cyArIG90aGVyLmNlbnRzKSAgICAgICAgICAvLyDinYwgdGhp4bq_dSBgb3BlcmF0b3JgCiAgICAvLyBvcGVyYXRvciBmdW4gcGx1cyhvdGhlcjogTW9uZXkpID0gTW9uZXkoY2VudHMgKyBvdGhlci5jZW50cykgLy8g4pyFCn0KCmZ1biBtYWluKCkgewogICAgcHJpbnRsbihNb25leSg1MDApICsgTW9uZXkoMjUwKSkKfQo)

### K232 · Lambda có receiver vs lambda có tham số: `Unresolved reference 'append'`

`🟡 đã làm app` · `Kotlin`

**Gặp:** viết hàm kiểu DSL `html { append("...") }` thì báo `Unresolved reference 'append'.`

**Vì sao:** `block: (StringBuilder) -> Unit` là lambda có **tham số** (gọi bằng `it.append`). Muốn gọi thẳng `append`
như trong `buildString { }` thì cần lambda có **receiver**: `StringBuilder.() -> Unit`, trong đó `this` là StringBuilder.

**Sửa:** đổi kiểu tham số sang `StringBuilder.() -> Unit`.

```kotlin
fun html(block: (StringBuilder) -> Unit): String       // ❌ phải viết it.append
fun html(block: StringBuilder.() -> Unit): String      // ✅ append trực tiếp
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIGh0bWwoYmxvY2s6IChTdHJpbmdCdWlsZGVyKSAtPiBVbml0KTogU3RyaW5nIHsgICAgICAgICAgICAvLyDinYwgbGFtYmRhIGPDsyBUSEFNIFPhu5AKLy8gZnVuIGh0bWwoYmxvY2s6IFN0cmluZ0J1aWxkZXIuKCkgLT4gVW5pdCk6IFN0cmluZyB7ICAgICAgICAvLyDinIUgbGFtYmRhIGPDsyBSRUNFSVZFUgogICAgdmFsIHNiID0gU3RyaW5nQnVpbGRlcigpCiAgICBibG9jayhzYikKICAgIHJldHVybiBzYi50b1N0cmluZygpCn0KCmZ1biBtYWluKCkgewogICAgcHJpbnRsbihodG1sIHsKICAgICAgICBhcHBlbmQoIjxiPiIpICAgICAgICAgICAgICAgICAgICAvLyBj4bqnbiBgdGhpc2AgbMOgIFN0cmluZ0J1aWxkZXIKICAgICAgICBhcHBlbmQoIktvdGxpbiIpCiAgICAgICAgYXBwZW5kKCI8L2I-IikKICAgIH0pCn0K) · Xem thêm: [Extension & lambda có receiver](../concepts/extension-receiver.md)

### K233 · `Illegal usage of inline parameter`: lưu lambda của hàm `inline`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Illegal usage of inline parameter 'block: () -> Unit'. Add 'noinline' modifier to the parameter declaration.`

**Vì sao:** lambda truyền cho hàm `inline` bị chép thẳng vào chỗ gọi, nó không tồn tại như một object. Muốn gán vào
field, truyền cho hàm không inline, hay trả về thì phải có object thật.

**Sửa:** thêm `noinline` cho đúng tham số đó. Nếu mọi lambda đều `noinline` thì bỏ luôn `inline`.

```kotlin
inline fun run(noinline block: () -> Unit) { last = block; block() }  // ✅
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgUmV0cnkgewogICAgdmFyIGxhc3Q6ICgoKSAtPiBVbml0KT8gPSBudWxsCiAgICBpbmxpbmUgZnVuIHJ1bihibG9jazogKCkgLT4gVW5pdCkgeyAgICAgLy8g4p2MIGxhbWJkYSBpbmxpbmUga2jDtG5nIGzGsHUgbOG6oWkgxJHGsOG7o2MKICAgIC8vIGlubGluZSBmdW4gcnVuKG5vaW5saW5lIGJsb2NrOiAoKSAtPiBVbml0KSB7IC8vIOKchSBub2lubGluZSBjaG8gbGFtYmRhIGPhuqduIGzGsHUKICAgICAgICBsYXN0ID0gYmxvY2sKICAgICAgICBibG9jaygpCiAgICB9Cn0KCmZ1biBtYWluKCkgewogICAgdmFsIHIgPSBSZXRyeSgpCiAgICByLnJ1biB7IHByaW50bG4oImfhu61pIikgfQogICAgci5sYXN0Py5pbnZva2UoKQp9Cg)

### K234 · `Cannot inline ... it might contain non-local returns`: cần `crossinline`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `Cannot inline 'block: () -> Unit' here: it might contain non-local returns. Add 'crossinline' modifier to
parameter declaration 'block: () -> Unit'.`

**Vì sao:** trong hàm `inline`, bạn gọi `block()` bên trong **một lambda khác** (`Thread { }`, `Runnable`, callback). Nếu
`block` có `return` trần (K201), nó sẽ phải thoát một hàm đã kết thúc từ lâu.

**Sửa:** thêm `crossinline`: vẫn inline, nhưng cấm `return` trần trong lambda của người gọi.

```kotlin
inline fun onBackground(crossinline block: () -> Unit) = Thread { block() }.start()  // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW5saW5lIGZ1biBvbkJhY2tncm91bmQoYmxvY2s6ICgpIC0-IFVuaXQpIHsgICAgICAgICAvLyDinYwKLy8gaW5saW5lIGZ1biBvbkJhY2tncm91bmQoY3Jvc3NpbmxpbmUgYmxvY2s6ICgpIC0-IFVuaXQpIHsgLy8g4pyFCiAgICB2YWwgdCA9IFRocmVhZCB7IGJsb2NrKCkgfQogICAgdC5zdGFydCgpOyB0LmpvaW4oKQp9CgpmdW4gbWFpbigpIHsKICAgIG9uQmFja2dyb3VuZCB7IHByaW50bG4oImNo4bqheSB0csOqbiAke1RocmVhZC5jdXJyZW50VGhyZWFkKCkubmFtZX0iKSB9Cn0K) · Xem thêm: K201

### K235 · `Public-API inline function cannot access non-public-API property`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `Public-API inline function cannot access non-public-API property.` khi hàm `inline` public dùng field `private`.

**Vì sao:** thân hàm `inline` được chép vào code của người gọi (có khi ở module khác). Ở đó field `private` không truy cập
được.

**Sửa:** đánh dấu field `@PublishedApi internal` (vẫn ẩn với người dùng, nhưng code inline dùng được), hoặc bỏ `inline`
nếu không có lambda / `reified`.

```kotlin
@PublishedApi internal val tag = "APP"   // ✅
inline fun log(msg: () -> String) = println("[$tag] ${msg()}")
```

[▶ Chạy thử](../playground/index.html#code=Y2xhc3MgTG9nZ2VyIHsKICAgIHByaXZhdGUgdmFsIHRhZyA9ICJBUFAiICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIHByaXZhdGUKICAgIC8vIEBQdWJsaXNoZWRBcGkgaW50ZXJuYWwgdmFsIHRhZyA9ICJBUFAiICAgICAgICAgLy8g4pyFIGhv4bq3YyDEkeG7gyBwdWJsaWMKICAgIGlubGluZSBmdW4gbG9nKG1zZzogKCkgLT4gU3RyaW5nKSA9IHByaW50bG4oIlskdGFnXSAke21zZygpfSIpCn0KCmZ1biBtYWluKCkgewogICAgTG9nZ2VyKCkubG9nIHsgIm3hu58gYXBwIiB9Cn0K)

### K236 · Hàm luôn `throw` nhưng khai báo trả `Unit`: elvis ra kiểu `Any`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Initializer type mismatch: expected 'String', actual 'Any'.` ở `val text: String = input ?: fail("thiếu")`.

**Vì sao:** `fun fail(msg: String) { throw ... }` có kiểu trả `Unit`. Compiler không biết nó không bao giờ trả về, nên
`String ?: Unit` thành `Any`.

**Sửa:** khai báo kiểu trả về `Nothing`. Thư viện chuẩn đã có sẵn: `error("...")`, `throw`, `TODO()` đều là `Nothing`.

```kotlin
fun fail(msg: String): Nothing = throw IllegalArgumentException(msg)   // ✅
val text: String = input ?: fail("thiếu input")
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIGZhaWwobXNnOiBTdHJpbmcpIHsgICAgICAgICAgICAgICAgICAvLyDinYwga2nhu4N1IHRy4bqjIHbhu4EgbMOgIFVuaXQKLy8gZnVuIGZhaWwobXNnOiBTdHJpbmcpOiBOb3RoaW5nIHsgICAgICAvLyDinIUgTm90aGluZzogY29tcGlsZXIgYmnhur90IGjDoG0ga2jDtG5nIHRy4bqjIHbhu4EKICAgIHRocm93IElsbGVnYWxBcmd1bWVudEV4Y2VwdGlvbihtc2cpCn0KCmZ1biBtYWluKCkgewogICAgdmFsIGlucHV0OiBTdHJpbmc_ID0gIjQyIgogICAgdmFsIHRleHQ6IFN0cmluZyA9IGlucHV0ID86IGZhaWwoInRoaeG6v3UgaW5wdXQiKQogICAgcHJpbnRsbih0ZXh0Lmxlbmd0aCkKfQo)

### K237 · `NotImplementedError: An operation is not implemented`

`🟢 mới học` · `Kotlin`

**Gặp:** `kotlin.NotImplementedError: An operation is not implemented: làm sau` khi bấm vào một chức năng.

**Vì sao:** `TODO()` compile được (kiểu `Nothing`) nên IDE hay tự sinh ra khi tạo hàm override. Nó ném lỗi ngay khi chạy.

**Sửa:** tìm `TODO(` trước khi phát hành (`grep -rn "TODO(" */src`). Nếu chưa làm thật, trả giá trị an toàn và ghi
comment `// TODO:` thay vì gọi hàm `TODO()`.

[▶ Chạy thử](../playground/index.html#code=ZnVuIGRpc2NvdW50KHRvdGFsOiBJbnQpOiBJbnQgPSBUT0RPKCJsw6BtIHNhdSIpICAgIC8vIOKdjCBxdcOqbiB0aGF5IFRPRE8KCmZ1biBtYWluKCkgewogICAgcHJpbnRsbigidOG7lW5nOiAxMDAiKQogICAgcHJpbnRsbigiZ2nhuqNtOiAke2Rpc2NvdW50KDEwMCl9IikKfQo)

### K238 · Đệ quy sâu: `StackOverflowError`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `java.lang.StackOverflowError` khi đệ quy trên dữ liệu lớn (danh sách dài, cây sâu). Thêm `tailrec` thì
IDE cảnh báo `A function is marked as tail-recursive but no tail calls are found`.

**Vì sao:** mỗi lời gọi chiếm một khung stack. `tailrec` chỉ biến đệ quy thành vòng lặp khi lời gọi là **việc cuối cùng**
của hàm; `n + sumTo(n - 1)` còn phải cộng sau khi gọi nên không phải tail call.

**Sửa:** chuyển kết quả trung gian vào tham số (accumulator) rồi đánh dấu `tailrec`, hoặc viết vòng lặp.

```kotlin
fun sumTo(n: Long): Long = if (n == 0L) 0 else n + sumTo(n - 1)          // ❌
tailrec fun sumTo(n: Long, acc: Long = 0): Long =
    if (n == 0L) acc else sumTo(n - 1, acc + n)                         // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIHN1bVRvKG46IExvbmcpOiBMb25nID0gaWYgKG4gPT0gMEwpIDAgZWxzZSBuICsgc3VtVG8obiAtIDEpICAvLyDinYwgcGjDqXAgY-G7mW5nIHNhdSBs4budaSBn4buNaSDihpIga2jDtG5nIHBo4bqjaSB0YWlsIGNhbGwKCnRhaWxyZWMgZnVuIHN1bVRvVGFpbChuOiBMb25nLCBhY2M6IExvbmcgPSAwKTogTG9uZyA9ICAgICAgICAgICAgICAgLy8g4pyFIGzhu51pIGfhu41pIGzDoCB2aeG7h2MgY3Xhu5FpIGPDuW5nCiAgICBpZiAobiA9PSAwTCkgYWNjIGVsc2Ugc3VtVG9UYWlsKG4gLSAxLCBhY2MgKyBuKQoKZnVuIG1haW4oKSB7CiAgICBwcmludGxuKHN1bVRvVGFpbCgxXzAwMF8wMDApKQogICAgcHJpbnRsbihzdW1UbygxXzAwMF8wMDApKQp9Cg)

### K239 · `typealias` không tạo kiểu mới

`🟡 đã làm app` · `Kotlin`

**Gặp:** `typealias UserId = String` và `typealias Email = String`, nhưng truyền `UserId` vào hàm cần `Email` vẫn compile.

**Vì sao:** `typealias` chỉ là tên khác cho cùng một kiểu. Compiler coi cả hai là `String`.

**Sửa:** cần compiler chặn nhầm lẫn thì dùng `value class`: kiểu riêng thật sự, lúc chạy vẫn gần như không tốn thêm.

```kotlin
@JvmInline value class UserId(val value: String)
@JvmInline value class Email(val value: String)
fun sendMail(to: Email) { }
sendMail(UserId("u_1"))   // ✅ giờ thì lỗi compile
```

[▶ Chạy thử](../playground/index.html#code=dHlwZWFsaWFzIFVzZXJJZCA9IFN0cmluZwp0eXBlYWxpYXMgRW1haWwgPSBTdHJpbmcKCmZ1biBzZW5kTWFpbCh0bzogRW1haWwpID0gcHJpbnRsbigiZ-G7rWkgbWFpbCB04bubaSAkdG8iKQoKLy8gQEp2bUlubGluZSB2YWx1ZSBjbGFzcyBVc2VySWQyKHZhbCB2YWx1ZTogU3RyaW5nKSAgIC8vIOKchSBraeG7g3UgcmnDqm5nIHRo4bqtdCBz4buxCi8vIEBKdm1JbmxpbmUgdmFsdWUgY2xhc3MgRW1haWwyKHZhbCB2YWx1ZTogU3RyaW5nKQoKZnVuIG1haW4oKSB7CiAgICB2YWwgaWQ6IFVzZXJJZCA9ICJ1XzEyMyIKICAgIHNlbmRNYWlsKGlkKSAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgY29tcGlsZXIga2jDtG5nIGNo4bq3bjogdHlwZWFsaWFzIGNo4buJIGzDoCB0w6puIGtow6FjIGPhu6dhIFN0cmluZwp9Cg) · Xem thêm: K225

### K240 · `Cannot infer type for type parameter 'T'. Specify it explicitly.`

`🟢 mới học` · `Kotlin`

**Gặp:** `Cannot infer type for type parameter 'T'. Specify it explicitly.` ở `val items = emptyList()`,
`mutableListOf()`, `MutableStateFlow(null)`.

**Vì sao:** không có phần tử nào và không có kiểu mong đợi, compiler không biết `T` là gì.

**Sửa:** ghi kiểu ở hàm hoặc ở biến.

```kotlin
val items = emptyList<String>()                         // ✅
val items: List<String> = emptyList()                   // ✅
val selected = MutableStateFlow<Note?>(null)            // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgaXRlbXMgPSBlbXB0eUxpc3QoKSAgICAgICAgICAgICAgLy8g4p2MIGxpc3QgcuG7l25nIHRow6wga2jDtG5nIMSRb8OhbiDEkcaw4bujYyBraeG7g3UgcGjhuqduIHThu60KICAgIC8vIHZhbCBpdGVtcyA9IGVtcHR5TGlzdDxTdHJpbmc-KCkgICAvLyDinIUKICAgIC8vIHZhbCBpdGVtczogTGlzdDxTdHJpbmc-ID0gZW1wdHlMaXN0KCkgLy8g4pyFCiAgICBwcmludGxuKGl0ZW1zLnNpemUpCn0K) · Xem thêm: [Generics](../concepts/generics.md)

### K241 · `listOf(1, "hai")` ra kiểu `Comparable<*> & Serializable`

`🟢 mới học` · `Kotlin`

**Gặp:** `Initializer type mismatch: expected 'Int', actual 'Comparable<*> & Serializable'.` khi lấy phần tử ra dùng.

**Vì sao:** list lẫn kiểu thì compiler chọn **kiểu chung gần nhất** của mọi phần tử, không báo lỗi lúc tạo list. Lỗi
chỉ hiện ở chỗ dùng, xa chỗ sai.

**Sửa:** ghi kiểu khi tạo list (`listOf<Int>(...)` hoặc `val xs: List<Int>`) để lẫn kiểu là báo ngay tại chỗ.

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgbWl4ZWQgPSBsaXN0T2YoMSwgImhhaSIpICAgICAgICAgLy8gY29tcGlsZXIgc3V5IHJhIGtp4buDdSBjaHVuZywga2jDtG5nIHBo4bqjaSBMaXN0PEludD4KICAgIHZhbCBuOiBJbnQgPSBtaXhlZFswXSAgICAgICAgICAgICAgICAvLyDinYwgcGjhuqduIHThu60ga2jDtG5nIGPDsm4gbMOgIEludAogICAgLy8gdmFsIG51bWJlcnM6IExpc3Q8SW50PiA9IGxpc3RPZigxLCAyKSAvLyDinIUgZ2hpIGtp4buDdTogbOG6q24gImhhaSIgdsOgbyBsw6AgYsOhbyBs4buXaSBuZ2F5IGNo4buXIHThuqFvIGxpc3QKICAgIHByaW50bG4obikKfQo)

### K242 · `T` mặc định cho phép `null`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Cache<T>` dùng `get(k): T?` để báo "không có", nhưng ai đó tạo `Cache<String?>` rồi lưu `null`: không phân
biệt được "chưa có" với "có, giá trị là null".

**Vì sao:** `<T>` không ghi bound thì bound là `Any?`, nên `T` có thể là kiểu nullable.

**Sửa:** `<T : Any>` khi `T` không được null. Bên trong `T?` mới thật sự có nghĩa "có thể không có".

```kotlin
class Cache<T>          // ❌ Cache<String?> hợp lệ
class Cache<T : Any>    // ✅ Cache<String?> báo lỗi
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIDxUPiBkZXNjcmliZSh2YWx1ZTogVCk6IFN0cmluZyA9IHZhbHVlLmhhc2hDb2RlKCkudG9TdHJpbmcoKSAgICAgIC8vIFQgY8OzIHRo4buDIGzDoCBudWxsCgpjbGFzcyBDYWNoZTxUPiB7ICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIFQgbeG6t2MgxJHhu4tuaCBsw6AgQW55PwovLyBjbGFzcyBDYWNoZTxUIDogQW55PiB7ICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4pyFIGPhuqVtIGtp4buDdSBudWxsYWJsZQogICAgcHJpdmF0ZSB2YWwgbWFwID0gbXV0YWJsZU1hcE9mPFN0cmluZywgVD4oKQogICAgZnVuIHB1dChrOiBTdHJpbmcsIHY6IFQpIHsgbWFwW2tdID0gdiB9CiAgICBmdW4gZ2V0KGs6IFN0cmluZyk6IFQ_ID0gbWFwW2tdCn0KCmZ1biBtYWluKCkgewogICAgdmFsIGMgPSBDYWNoZTxTdHJpbmc_PigpCiAgICBjLnB1dCgidG9rZW4iLCBudWxsKQogICAgcHJpbnRsbigiZ2V0ID0gJHtjLmdldCgidG9rZW4iKX0g4oaSIGtow7RuZyBwaMOibiBiaeG7h3QgxJHGsOG7o2MgJ2NoxrBhIGPDsycgdsOgICdjw7MgbmjGsG5nIG51bGwnIikKICAgIHByaW50bG4oZGVzY3JpYmUobnVsbCkpCn0K)

### K243 · `Cannot use 'T' as reified type parameter` / `Cannot check for instance of erased type 'T'`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Cannot use 'T' as reified type parameter. Use a class instead.` ở `T::class`, hoặc
`Cannot check for instance of erased type 'T (of fun <T> filterType)'.` ở `x is T`.

**Vì sao:** trên JVM, kiểu generic bị **xoá** lúc chạy (type erasure). Trong thân hàm, `T` không còn là kiểu cụ thể nào.

**Sửa:** hàm `inline` với `reified T`: compiler chép thân hàm vào chỗ gọi và thay `T` bằng kiểu thật. Nếu không inline
được (hàm public lớn, gọi từ Java/Swift), nhận `KClass<T>` làm tham số.

```kotlin
fun <T> filterType(items: List<Any>) = items.filter { it is T }            // ❌
inline fun <reified T> filterType(items: List<Any>) = items.filterIsInstance<T>() // ✅
fun <T : Any> parse(json: String, type: KClass<T>): T                       // ✅ cách không inline
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIDxUPiBmaWx0ZXJUeXBlKGl0ZW1zOiBMaXN0PEFueT4pOiBMaXN0PFQ-ID0gICAgICAvLyDinYwgVCBi4buLIHhvw6EgbMO6YyBjaOG6oXkKICAgIGl0ZW1zLmZpbHRlciB7IGl0IGlzIFQgfS5tYXAgeyBpdCBhcyBUIH0KLy8gaW5saW5lIGZ1biA8cmVpZmllZCBUPiBmaWx0ZXJUeXBlKGl0ZW1zOiBMaXN0PEFueT4pOiBMaXN0PFQ-ID0gLy8g4pyFIGlubGluZSArIHJlaWZpZWQKLy8gICAgIGl0ZW1zLmZpbHRlcklzSW5zdGFuY2U8VD4oKQoKZnVuIG1haW4oKSB7CiAgICB2YWwgbWl4ZWQgPSBsaXN0T2YoMSwgImEiLCAyLCAiYiIpCiAgICBwcmludGxuKGZpbHRlclR5cGU8U3RyaW5nPihtaXhlZCkpCn0K) · Xem thêm: K247, [Generics](../concepts/generics.md)

### K244 · `Cannot check for instance of erased type 'List<Int>'`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Cannot check for instance of erased type 'List<Int>'.` ở `if (x is List<Int>)`.

**Vì sao:** lúc chạy chỉ biết `x` là `List`, không biết phần tử là gì (type erasure).

**Sửa:** kiểm `is List<*>`, rồi lọc phần tử: `x.filterIsInstance<Int>()`.

```kotlin
if (x is List<Int>) x.sum()                            // ❌
if (x is List<*>) x.filterIsInstance<Int>().sum()      // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIHRvdGFsKHg6IEFueSk6IEludCA9CiAgICBpZiAoeCBpcyBMaXN0PEludD4pIHguc3VtKCkgZWxzZSAwICAgIC8vIOKdjCBsw7pjIGNo4bqheSBjaOG7iSBjw7JuIExpc3QsIGtow7RuZyBiaeG6v3QgPEludD4KLy8gaWYgKHggaXMgTGlzdDwqPikgeC5maWx0ZXJJc0luc3RhbmNlPEludD4oKS5zdW0oKSBlbHNlIDAgICAvLyDinIUKCmZ1biBtYWluKCkgewogICAgcHJpbnRsbih0b3RhbChsaXN0T2YoMSwgMiwgMykpKQp9Cg)

### K245 · Ép `as List<String>` không crash, crash ở chỗ khác

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `java.lang.ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String` ở một dòng
trông vô hại như `val first: String = names[0]`, cách chỗ ép kiểu rất xa. Lúc ép chỉ có cảnh báo `Unchecked cast`.

**Vì sao:** `as List<String>` chỉ kiểm được phần `List` (phần `<String>` đã bị xoá), nên luôn "thành công". Lỗi chỉ lộ
khi lấy phần tử ra.

**Sửa:** đừng `@Suppress("UNCHECKED_CAST")` cho qua. Kiểm `as? List<*>` rồi `filterIsInstance<String>()`, hoặc giữ
kiểu đúng ngay từ đầu (đọc JSON bằng serializer có kiểu).

```kotlin
val names = fromCache as List<String>                                    // ❌
val names = (fromCache as? List<*>)?.filterIsInstance<String>().orEmpty() // ✅
```

[▶ Chạy thử](../playground/index.html#code=QFN1cHByZXNzKCJVTkNIRUNLRURfQ0FTVCIpCmZ1biBtYWluKCkgewogICAgdmFsIGZyb21DYWNoZTogQW55ID0gbGlzdE9mKDEsIDIsIDMpCiAgICB2YWwgbmFtZXMgPSBmcm9tQ2FjaGUgYXMgTGlzdDxTdHJpbmc-ICAgLy8g4p2MIGtow7RuZyBjcmFzaCDhu58gxJHDonk6IDxTdHJpbmc-IMSRw6MgYuG7iyB4b8OhCiAgICBwcmludGxuKCLDqXAga2nhu4N1IHhvbmcsIHNpemUgPSAke25hbWVzLnNpemV9IikKICAgIHZhbCBmaXJzdDogU3RyaW5nID0gbmFtZXNbMF0gICAgICAgICAgICAvLyDwn5KlIGNyYXNoIOG7nyBjaOG7lyBEw5lORywgY8OhY2ggeGEgY2jhu5cgc2FpCiAgICBwcmludGxuKGZpcnN0KQogICAgLy8g4pyFIHZhbCBuYW1lcyA9IChmcm9tQ2FjaGUgYXM_IExpc3Q8Kj4pPy5maWx0ZXJJc0luc3RhbmNlPFN0cmluZz4oKS5vckVtcHR5KCkKfQo) · Xem thêm: K244

### K246 · `Platform declaration clash: The following declarations have the same JVM signature`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Platform declaration clash: The following declarations have the same JVM signature (average(Ljava/util/List;)D)`
khi viết 2 overload `average(List<Int>)` và `average(List<Double>)`.

**Vì sao:** sau type erasure, cả hai đều là `average(List)` trên JVM. Kotlin phân biệt được, bytecode thì không.

**Sửa:** đặt tên khác (dễ đọc nhất, và dễ gọi từ Swift), hoặc thêm `@JvmName("averageDouble")` cho một hàm.

```kotlin
fun decodePid(pid: ObdPid, bytes: List<Int>): PidReading?
fun decodePidAnswer(pid: ObdPid, messages: List<List<Int>>): PidReading?   // ✅ tên khác
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIGF2ZXJhZ2UodmFsdWVzOiBMaXN0PEludD4pOiBEb3VibGUgPSB2YWx1ZXMuYXZlcmFnZSgpCmZ1biBhdmVyYWdlKHZhbHVlczogTGlzdDxEb3VibGU-KTogRG91YmxlID0gdmFsdWVzLmF2ZXJhZ2UoKSAgIC8vIOKdjCBjw7luZyBjaOG7ryBrw70gSlZNOiBhdmVyYWdlKExpc3QpCi8vIEBKdm1OYW1lKCJhdmVyYWdlRG91YmxlIikgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKchSB0aMOqbSBkw7JuZyBuw6B5IHRyw6puIGjDoG0gdGjhu6kgaGFpCi8vIGhv4bq3YyDEkeG6t3QgdMOqbiBraMOhYzogZnVuIGF2ZXJhZ2VPZkRvdWJsZXModmFsdWVzOiBMaXN0PERvdWJsZT4pCgpmdW4gbWFpbigpIHsKICAgIHByaW50bG4oYXZlcmFnZShsaXN0T2YoMSwgMiwgMykpKQp9Cg)

**Gặp ở:** [`apps/ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/rules/Pid.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/rules/Pid.kt)

### K247 · Swift không gọi được hàm `reified` (như `get<T>()` của Koin)

`🟡 đã làm app` · `KMP` · `iOS`

**Gặp:** trong Swift không thấy `get()` / `inject()` của Koin, hay một hàm `inline fun <reified T>` của bạn: không có
trong header của framework.

**Vì sao:** hàm `inline reified` chỉ tồn tại lúc compile Kotlin: nó được chép vào chỗ gọi. Swift gọi qua header
Objective-C, ở đó không có gì để gọi.

**Sửa:** trong `iosMain`, viết hàm thường cho từng kiểu Swift cần.

```kotlin
// iosMain
fun tipCore(): TipCore = KoinPlatform.getKoin().get()   // ✅ Swift gọi tipCore()
```

**Gặp ở:** [`apps/TipSplit/shared/src/iosMain/kotlin/dev/tipsplit/di/KoinIos.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/iosMain/kotlin/dev/tipsplit/di/KoinIos.kt) ·
Xem thêm: K243

### K248 · `MutableList<Dog>` không truyền được cho `MutableList<Animal>`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Argument type mismatch: actual type is 'MutableList<Dog>', but 'MutableList<Animal>' was expected.`
Trong khi `List<Dog>` truyền cho `List<Animal>` thì được.

**Vì sao:** `Dog` là `Animal` không có nghĩa `MutableList<Dog>` là `MutableList<Animal>`: nếu được, hàm kia có thể
`add(Cat())` vào list chó. `MutableList<E>` là **bất biến** (invariant). `List<out E>` chỉ đọc nên an toàn, được
khai báo `out`.

**Sửa:** hàm chỉ đọc thì nhận `List<Animal>`. Hàm cần ghi thì nhận đúng kiểu, hoặc dùng `MutableList<in Dog>` (chỉ
thêm `Dog` vào).

[▶ Chạy thử](../playground/index.html#code=b3BlbiBjbGFzcyBBbmltYWwgeyBvcGVuIGZ1biBuYW1lKCkgPSAiYW5pbWFsIiB9CmNsYXNzIERvZyA6IEFuaW1hbCgpIHsgb3ZlcnJpZGUgZnVuIG5hbWUoKSA9ICJkb2ciIH0KY2xhc3MgQ2F0IDogQW5pbWFsKCkgeyBvdmVycmlkZSBmdW4gbmFtZSgpID0gImNhdCIgfQoKZnVuIGFkZENhdChsaXN0OiBNdXRhYmxlTGlzdDxBbmltYWw-KSB7IGxpc3QuYWRkKENhdCgpKSB9CmZ1biBwcmludEFsbChsaXN0OiBMaXN0PEFuaW1hbD4pID0gcHJpbnRsbihsaXN0Lm1hcCB7IGl0Lm5hbWUoKSB9KQoKZnVuIG1haW4oKSB7CiAgICB2YWwgZG9ncyA9IG11dGFibGVMaXN0T2YoRG9nKCksIERvZygpKQogICAgcHJpbnRBbGwoZG9ncykgICAgICAgICAgICAgICAgICAgICAgIC8vIOKchSBMaXN0PG91dCBFPiBjaOG7iSDEkeG7jWM6IG5o4bqtbiBMaXN0PERvZz4KICAgIGFkZENhdChkb2dzKSAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgTXV0YWJsZUxpc3QgYuG6pXQgYmnhur9uOiBraMO0bmcgdGjhu4MgbmjDqXQgQ2F0IHbDoG8gbGlzdCBEb2cKfQo) · Xem thêm: [Generics](../concepts/generics.md), K249, K250

### K249 · `Type parameter 'T' is declared as 'out' but occurs in 'in' position`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `Type parameter 'T' is declared as 'out' but occurs in 'in' position in type 'T (of interface Source<out T>)'.`

**Vì sao:** `out T` hứa "chỉ **đưa** `T` ra" (kiểu trả về). Một hàm nhận `T` làm tham số phá lời hứa đó. Ngược lại
`in T` chỉ được nhận `T` vào.

**Sửa:** tách interface đọc (`out`) và ghi (`in`). Nếu chắc chắn an toàn (vd `contains(x: T)` không lưu `x`), dùng
`@UnsafeVariance` ở tham số đó, như `List.contains` của thư viện chuẩn.

```kotlin
interface Source<out T> { fun next(): T }      // ✅ chỉ ra
interface Sink<in T> { fun push(item: T) }     // ✅ chỉ vào
```

[▶ Chạy thử](../playground/index.html#code=aW50ZXJmYWNlIFNvdXJjZTxvdXQgVD4gewogICAgZnVuIG5leHQoKTogVCAgICAgICAgICAgICAgICAgICAgICAvLyDinIUgVCDhu58gduG7iyB0csOtIHRy4bqjIHbhu4EgKG91dCkKICAgIGZ1biBwdXNoKGl0ZW06IFQpICAgICAgICAgICAgICAgICAgLy8g4p2MIFQg4bufIHbhu4sgdHLDrSB0aGFtIHPhu5EgKGluKQp9CgpmdW4gbWFpbigpIHsgcHJpbnRsbigiY29tcGlsZSB0aOG7rSIpIH0K)

### K250 · Thiếu `in`, và `MutableList<*>` không cho thêm phần tử

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `Argument type mismatch: actual type is '<anonymous>', but 'Handler<Dog>' was expected.` khi truyền
`Handler<Animal>` cho chỗ cần `Handler<Dog>`. Hoặc `Receiver type 'MutableList<*>' contains star projection which
prohibits the use of 'fun add(element: E): Boolean'.`

**Vì sao:** một handler xử lý được mọi `Animal` thì xử lý được `Dog`, nhưng compiler chỉ biết điều đó khi `T` khai báo
`in` (kiểu chỉ **nhận** `T`). Còn `<*>` nghĩa là "không biết kiểu phần tử": đọc ra được (`Any?`), thêm vào thì không.

**Sửa:** interface chỉ nhận `T` vào thì khai báo `in T` (như `Comparator<in T>`). Cần thêm phần tử thì dùng kiểu cụ thể,
không dùng `<*>`.

```kotlin
interface Handler<T> { fun handle(x: T) }      // ❌ bất biến
interface Handler<in T> { fun handle(x: T) }   // ✅ Handler<Animal> dùng được như Handler<Dog>
```

[▶ Chạy thử](../playground/index.html#code=b3BlbiBjbGFzcyBBbmltYWwKY2xhc3MgRG9nIDogQW5pbWFsKCkKCmludGVyZmFjZSBIYW5kbGVyPFQ-IHsgZnVuIGhhbmRsZSh4OiBUKSB9ICAgICAgICAgIC8vIOKdjCB0aGnhur91IGBpbmAKLy8gaW50ZXJmYWNlIEhhbmRsZXI8aW4gVD4geyBmdW4gaGFuZGxlKHg6IFQpIH0gICAgLy8g4pyFIGNo4buJIG5o4bqtbiBUIHbDoG8g4oaSIGtoYWkgYsOhbyBpbgoKZnVuIGZlZWREb2dzKGg6IEhhbmRsZXI8RG9nPikgPSBoLmhhbmRsZShEb2coKSkKCmZ1biBtYWluKCkgewogICAgdmFsIGFueUFuaW1hbCA9IG9iamVjdCA6IEhhbmRsZXI8QW5pbWFsPiB7CiAgICAgICAgb3ZlcnJpZGUgZnVuIGhhbmRsZSh4OiBBbmltYWwpID0gcHJpbnRsbigiY2hvIMSDbiAke3g6OmNsYXNzLnNpbXBsZU5hbWV9IikKICAgIH0KICAgIGZlZWREb2dzKGFueUFuaW1hbCkKCiAgICB2YWwgdW5rbm93bjogTXV0YWJsZUxpc3Q8Kj4gPSBtdXRhYmxlTGlzdE9mKDEsIDIpCiAgICBwcmludGxuKHVua25vd24uc2l6ZSkgICAgICAgICAgICAgIC8vIMSR4buNYyDEkcaw4bujYwogICAgLy8gdW5rbm93bi5hZGQoMykgICAgICAgICAgICAgICAgICAvLyDinYwgPCo-IGtow7RuZyBjaG8gdGjDqm06ICIuLi4gYnV0ICdOb3RoaW5nJyB3YXMgZXhwZWN0ZWQiCn0K) · [▶ Chạy thử: `MutableList<*>`](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgdW5rbm93bjogTXV0YWJsZUxpc3Q8Kj4gPSBtdXRhYmxlTGlzdE9mKDEsIDIpCiAgICB1bmtub3duLmFkZCgzKQp9Cg) · Xem thêm: K248, K249
