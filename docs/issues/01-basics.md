# 1. Cú pháp & kiểu cơ bản (K001–K050)

Lỗi của những dòng code đầu tiên: `val`/`var`, kiểu số, phép chia, chuỗi, `if`/`when`, vòng lặp. Phần lớn compile được và
chạy ra số sai mà không báo gì, nên đáng đọc kỹ trước khi viết app đầu tiên. Bẫy lớn nhất đã giải thích dài ở
[Phụ lục: Kotlin khó gì, bẫy gì](../reference/kotlin-pitfalls.md).

### K001 · `Val cannot be reassigned`

`🟢 mới học` · `Kotlin`

**Gặp:** compiler báo `'val' cannot be reassigned.` (bản cũ: `Val cannot be reassigned`) ở dòng gán lần hai.

**Vì sao:** `val` chỉ được gán một lần. Biến nào cần đổi giá trị thì phải khai báo bằng `var`.

**Sửa:** mặc định viết `val`, chỉ đổi sang `var` khi thật sự cần gán lại. Thường có cách viết lại bằng `val` gọn hơn
(tính giá trị mới vào biến khác).

```kotlin
val total = 100
total = total + 50          // ❌
var total = 100             // ✅ hoặc: val newTotal = total + 50
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgdG90YWwgPSAxMDAKICAgIHRvdGFsID0gdG90YWwgKyA1MCAgICAgICAgICAvLyDinYwgVmFsIGNhbm5vdCBiZSByZWFzc2lnbmVkLiBT4butYTogxJHhu5VpIGB2YWwgdG90YWxgIHRow6BuaCBgdmFyIHRvdGFsYAogICAgcHJpbnRsbih0b3RhbCkKfQo)

### K002 · `val` mà list vẫn đổi

`🟢 mới học` · `Kotlin`

**Gặp:** khai báo `val cart = mutableListOf(...)` tưởng là hằng, nhưng chỗ khác vẫn `cart.add(...)` được và dữ liệu đổi bất ngờ.

**Vì sao:** `val` chỉ cấm **gán lại biến**, không cấm sửa **object** mà biến trỏ tới. `MutableList` thì luôn sửa được.

**Sửa:** muốn không ai sửa được nội dung thì dùng kiểu chỉ đọc `List` (tạo bằng `listOf`), và chỉ để lộ `List` ra ngoài class.

```kotlin
val cart = mutableListOf("cà phê")
cart.add("bánh mì")                        // compile được: val không bảo vệ nội dung
val menu: List<String> = listOf("cà phê")  // ✅ không có add()
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgY2FydCA9IG11dGFibGVMaXN0T2YoImPDoCBwaMOqIikKICAgIGNhcnQuYWRkKCJiw6FuaCBtw6wiKSAgICAgICAgIC8vIHZhbCB24bqrbiBjaG8gc-G7rWEgTuG7mEkgRFVORyBsaXN0CiAgICBwcmludGxuKGNhcnQpICAgICAgICAgICAgICAgLy8gW2PDoCBwaMOqLCBiw6FuaCBtw6xdCgogICAgdmFsIHJlYWRPbmx5OiBMaXN0PFN0cmluZz4gPSBsaXN0T2YoImPDoCBwaMOqIikKICAgIC8vIHJlYWRPbmx5LmFkZCgidHLDoCIpICAgICAgLy8g4pyFIExpc3QgKGtow7RuZyBNdXRhYmxlKSBt4bubaSBjaOG6t24gxJHGsOG7o2MgYWRkOiBi4buPIC8vIMSR4buDIHRo4bqleSBs4buXaQogICAgcHJpbnRsbihyZWFkT25seSkKfQo) · Xem thêm: [Phụ lục 2.9](../reference/kotlin-pitfalls.md), [Collections](03-collections.md)

### K003 · `var total = 0` rồi cộng số thập phân

`🟢 mới học` · `Kotlin`

**Gặp:** `Assignment type mismatch: actual type is 'Double', but 'Int' was expected.` ở dòng `total += 1.5`.

**Vì sao:** Kotlin suy ra kiểu từ giá trị đầu tiên. `0` là `Int`, nên `total` mãi là `Int`; kiểu của biến không đổi theo giá trị gán sau.

**Sửa:** cho giá trị đầu đúng kiểu (`0.0`, `0L`) hoặc ghi rõ kiểu.

```kotlin
var total = 0       // ❌ Int
var total = 0.0     // ✅ Double
var total: Long = 0 // ✅ ghi rõ kiểu
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YXIgdG90YWwgPSAwICAgICAgICAgICAgICAgLy8g4p2MIEtvdGxpbiBzdXkgcmEgSW50LiBT4butYTogdmFyIHRvdGFsID0gMC4wCiAgICB0b3RhbCArPSAxLjUKICAgIHByaW50bG4oIiR0b3RhbCIpCn0K)

### K004 · `Initializer type mismatch: expected 'Long', actual 'Int'`

`🟢 mới học` · `Kotlin`

**Gặp:** `val millis: Long = seconds * 1000` báo `Initializer type mismatch: expected 'Long', actual 'Int'.`

**Vì sao:** Kotlin không tự đổi `Int` thành `Long` (hay `Double`) như Java. Chỉ **hằng số viết thẳng** (`val x: Long = 5`) mới
được tự hiểu thành `Long`; một biểu thức `Int` thì không.

**Sửa:** đổi kiểu rõ ràng bằng `toLong()`, `toDouble()`, hoặc thêm `L` vào hằng số để cả phép tính thành `Long`.

```kotlin
val millis: Long = seconds * 1000          // ❌
val millis: Long = seconds * 1000L         // ✅
val millis = seconds.toLong() * 1000       // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIHNhdmUobWlsbGlzOiBMb25nKSA9IHByaW50bG4oImzGsHUgJG1pbGxpcyIpCgpmdW4gbWFpbigpIHsKICAgIHZhbCBzZWNvbmRzOiBJbnQgPSA5MAogICAgdmFsIG1pbGxpczogTG9uZyA9IHNlY29uZHMgKiAxMDAwICAgICAgICAvLyDinYwgSW50IGtow7RuZyB04buxIHRow6BuaCBMb25nCiAgICAvLyB2YWwgbWlsbGlzOiBMb25nID0gc2Vjb25kcyAqIDEwMDBMICAgIC8vIOKchSBob-G6t2Mgc2Vjb25kcy50b0xvbmcoKSAqIDEwMDAKICAgIHNhdmUobWlsbGlzKQp9Cg)

### K005 · Tính phần trăm luôn ra 0

`🟢 mới học` · `Kotlin`

**Gặp:** thanh tiến độ đứng ở 0%, `done / total * 100` ra `0` dù đã xong 3/4.

**Vì sao:** `Int / Int` là chia lấy phần nguyên. `3 / 4` ra `0` trước khi kịp nhân 100.

**Sửa:** nhân trước rồi chia, hoặc đổi một vế sang `Double`.

```kotlin
done / total * 100              // ❌ 0
done * 100 / total              // ✅ 75
done.toDouble() / total * 100   // ✅ 75.0
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgZG9uZSA9IDMKICAgIHZhbCB0b3RhbCA9IDQKICAgIHByaW50bG4oZG9uZSAvIHRvdGFsICogMTAwKSAgICAgICAgICAgICAgIC8vIOKdjCAwICAoMyAvIDQgPSAwIHRyxrDhu5tjIGtoaSBuaMOibikKICAgIHByaW50bG4oZG9uZSAqIDEwMCAvIHRvdGFsKSAgICAgICAgICAgICAgIC8vIOKchSA3NSAobmjDom4gdHLGsOG7m2MsIGNoaWEgc2F1KQogICAgcHJpbnRsbihkb25lLnRvRG91YmxlKCkgLyB0b3RhbCAqIDEwMCkgICAgLy8g4pyFIDc1LjAKfQo) · Xem thêm: [Phụ lục 2.2](../reference/kotlin-pitfalls.md), K006

### K006 · Trung bình cộng: `expected 'Double', actual 'Int'`

`🟢 mới học` · `Kotlin`

**Gặp:** `val avg: Double = scores.sum() / scores.size` báo `Initializer type mismatch: expected 'Double', actual 'Int'.`

**Vì sao:** vế phải là `Int / Int`, ra `Int`. Ghi kiểu `Double` ở vế trái không làm phép chia thành chia số thực.

**Sửa:** đổi một vế của phép chia sang `Double`. Với list số thì có sẵn `average()`.

```kotlin
val avg: Double = scores.sum() / scores.size              // ❌
val avg: Double = scores.sum().toDouble() / scores.size   // ✅
val avg = scores.average()                                // ✅ Double
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgc2NvcmVzID0gbGlzdE9mKDcsIDgsIDgpCiAgICB2YWwgYXZnOiBEb3VibGUgPSBzY29yZXMuc3VtKCkgLyBzY29yZXMuc2l6ZSAgICAgICAgLy8g4p2MIEluaXRpYWxpemVyIHR5cGUgbWlzbWF0Y2gKICAgIC8vIHZhbCBhdmc6IERvdWJsZSA9IHNjb3Jlcy5zdW0oKS50b0RvdWJsZSgpIC8gc2NvcmVzLnNpemUgICAvLyDinIUgNy42NjYuLi4KICAgIHByaW50bG4oYXZnKQp9Cg)

### K007 · Cộng nhiều số tiền ra số âm

`🟢 mới học` · `Kotlin`

**Gặp:** tổng hai hoá đơn 1,5 tỷ đồng ra `-1294967296`. Không có exception nào.

**Vì sao:** `Int` chỉ tới khoảng 2,1 tỷ (`Int.MAX_VALUE = 2147483647`). Vượt qua thì quay vòng sang số âm, không báo lỗi.
`sum()` trên `List<Int>` cũng trả `Int`.

**Sửa:** tiền và mili giây dùng `Long`. Cộng list `Int` mà tổng có thể lớn thì `sumOf { it.toLong() }`.

```kotlin
prices.sum()                    // ❌ -1294967296
prices.sumOf { it.toLong() }    // ✅ 3000000000
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgcHJpY2VzID0gbGlzdE9mKDFfNTAwXzAwMF8wMDAsIDFfNTAwXzAwMF8wMDApICAgLy8gMiBob8OhIMSRxqFuIDEsNSB04bu3IMSR4buTbmcKICAgIHByaW50bG4ocHJpY2VzLnN1bSgpKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIC0xMjk0OTY3Mjk2CiAgICBwcmludGxuKHByaWNlcy5zdW1PZiB7IGl0LnRvTG9uZygpIH0pICAgICAgICAgICAgICAgIC8vIOKchSAzMDAwMDAwMDAwCiAgICBwcmludGxuKEludC5NQVhfVkFMVUUgKyAxKSAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIC0yMTQ3NDgzNjQ4LCBraMO0bmcgbsOpbSBs4buXaQp9Cg) · Xem thêm: [Phụ lục 2.1](../reference/kotlin-pitfalls.md), K008

### K008 · `Long` cũng tràn khi đổi sang đơn vị nhỏ hơn

`🟡 đã làm app` · `Kotlin`

**Gặp:** người dùng gõ một số tiền rất dài, app đổi sang "xu" (nhân 100) và hiện ra số âm.

**Vì sao:** `Long` lớn (khoảng 9,2 × 10¹⁸) nhưng vẫn có giới hạn, và tràn cũng quay vòng im lặng như `Int`.

**Sửa:** chặn độ dài đầu vào **trước** khi nhân (đặt mức tối đa hợp lý), hoặc trên JVM dùng `Math.multiplyExact` để tràn thì
ném `ArithmeticException: long overflow` thay vì ra số sai.

```kotlin
amount * 100                         // ❌ số âm khi amount quá lớn
Math.multiplyExact(amount, 100L)     // ✅ (JVM) ném lỗi thay vì sai
if (digits.length > 15) return null  // ✅ chặn từ đầu vào, chạy được cả iOS
```

**Gặp ở:** [`RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/rules/Amount.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/rules/Amount.kt)

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgYW1vdW50ID0gMTAwXzAwMF8wMDBfMDAwXzAwMF8wMDBMICAgICAgIC8vIDEwXjE3IMSR4buTbmcsIHbhuqtuIHbhu6thIExvbmcKICAgIHByaW50bG4oYW1vdW50ICogMTAwKSAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIMSR4buVaSBzYW5nICJ4dSIgbMOgIHRyw6BuOiByYSBz4buRIMOibQogICAgcHJpbnRsbihNYXRoLm11bHRpcGx5RXhhY3QoYW1vdW50LCAxMDBMKSkgICAvLyDinIUgKGNo4buJIEpWTSkgbsOpbSBBcml0aG1ldGljRXhjZXB0aW9uIHRoYXkgdsOsIHJhIHPhu5Egc2FpCn0K)

### K009 · `NumberFormatException: For input string`

`🟢 mới học` · `Kotlin`

**Gặp:** app crash với `java.lang.NumberFormatException: For input string: "12a"` khi người dùng gõ chữ vào ô số, hoặc xoá
trắng ô (`For input string: ""`).

**Vì sao:** `toInt()`, `toLong()`, `toDouble()` ném exception khi chuỗi không phải số. Ô nhập liệu thì luôn có lúc chứa chữ.

**Sửa:** với dữ liệu người dùng gõ, luôn dùng bản `...OrNull()` rồi xử lý `null`.

```kotlin
val n = typed.toInt()               // ❌ crash
val n = typed.toIntOrNull() ?: 0    // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgdHlwZWQgPSAiMTJhIgogICAgcHJpbnRsbih0eXBlZC50b0ludE9yTnVsbCgpID86IDApICAgLy8g4pyFIDAsIGtow7RuZyBjcmFzaAogICAgcHJpbnRsbih0eXBlZC50b0ludCgpKSAgICAgICAgICAgICAgLy8g4p2MIE51bWJlckZvcm1hdEV4Y2VwdGlvbjogRm9yIGlucHV0IHN0cmluZzogIjEyYSIKfQo) · Xem thêm: K010, K058

### K010 · Số quá dài làm `toInt()` crash

`🟢 mới học` · `Kotlin`

**Gặp:** `NumberFormatException: For input string: "99999999999999999999"` dù chuỗi toàn chữ số.

**Vì sao:** số vượt giới hạn của kiểu đích cũng bị coi là "không phải số". `toInt()` ném lỗi, còn `toLongOrNull()` /
`toIntOrNull()` trả `null` chứ **không** tràn ra số sai.

**Sửa:** dùng `toLongOrNull()` cho số tiền, và báo "số quá lớn" riêng nếu cần phân biệt với "không phải số".

**Gặp ở:** [`TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/rules/Money.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/rules/Money.kt)

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgdHlwZWQgPSAiOTk5OTk5OTk5OTk5OTk5OTk5OTkiICAgLy8gMjAgY2jhu68gc-G7kQogICAgcHJpbnRsbih0eXBlZC50b0xvbmdPck51bGwoKSkgICAgICAgICAvLyDinIUgbnVsbCwga2jDtG5nIHRyw6BuCiAgICBwcmludGxuKHR5cGVkLnRvSW50KCkpICAgICAgICAgICAgICAgIC8vIOKdjCBOdW1iZXJGb3JtYXRFeGNlcHRpb24KfQo)

### K011 · `"1.000".toInt()` và `"1,5".toDouble()` đều thất bại

`🟢 mới học` · `Kotlin`

**Gặp:** người Việt gõ `1.000` (một nghìn) hay `1,5` (một rưỡi), `toIntOrNull()` / `toDoubleOrNull()` trả `null`.

**Vì sao:** các hàm `toXxx()` chỉ hiểu dạng "máy": không có dấu phân cách hàng nghìn, dấu thập phân là `.`. Cách viết số theo
ngôn ngữ của máy không được tính tới.

**Sửa:** tự làm sạch chuỗi trước khi đổi (bỏ dấu hàng nghìn, đổi `,` thành `.`), và viết test cho từng kiểu gõ.

```kotlin
"1.000".filter { it != '.' }.toInt()      // ✅ 1000
"1,5".replace(',', '.').toDouble()        // ✅ 1.5
```

**Gặp ở:** [`RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/rules/Amount.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/rules/Amount.kt) (`parseAmount` nhận cả `12,50` và `1,234.56`)

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICBwcmludGxuKCIxLjAwMCIudG9JbnRPck51bGwoKSkgICAgICAgICAgICAgICAgICAgICAgIC8vIG51bGw6IGThuqV1IGNo4bqlbSBow6BuZyBuZ2jDrG4ga2nhu4N1IFZp4buHdAogICAgcHJpbnRsbigiMSw1Ii50b0RvdWJsZU9yTnVsbCgpKSAgICAgICAgICAgICAgICAgICAgICAvLyBudWxsOiBk4bqldSBwaOG6qXkgdGjhuq1wIHBow6JuIGtp4buDdSBWaeG7h3QKICAgIHByaW50bG4oIjEuMDAwIi5maWx0ZXIgeyBpdCAhPSAnLicgfS50b0ludCgpKSAgICAgICAgLy8g4pyFIDEwMDAKICAgIHByaW50bG4oIjEsNSIucmVwbGFjZSgnLCcsICcuJykudG9Eb3VibGUoKSkgICAgICAgICAgLy8g4pyFIDEuNQp9Cg)

### K012 · `isDigit()` nhận cả chữ số Ả Rập

`🟡 đã làm app` · `Kotlin`

**Gặp:** ô "chỉ nhận số" để lọt `١٢` (12 gõ bằng bàn phím Ả Rập) hoặc chữ số Devanagari; dữ liệu đó đi tiếp xuống DB, server.

**Vì sao:** `Char.isDigit()` trả `true` cho **mọi** chữ số Unicode, không chỉ `0`–`9`.

**Sửa:** muốn đúng chữ số ASCII thì so khoảng ký tự `it in '0'..'9'`.

```kotlin
text.all { it.isDigit() }      // ❌ "١٢" lọt qua
text.all { it in '0'..'9' }    // ✅
```

**Gặp ở:** [`RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/rules/Amount.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/rules/Amount.kt)

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgdHlwZWQgPSAi2aHZoiIgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vICIxMiIgZ8O1IGLhurFuZyBiw6BuIHBow61tIHRp4bq_bmcg4bqiIFLhuq1wCiAgICBwcmludGxuKHR5cGVkLmFsbCB7IGl0LmlzRGlnaXQoKSB9KSAgICAgICAgICAgICAgICAvLyB0cnVlOiBpc0RpZ2l0IG5o4bqtbiBt4buNaSBjaOG7ryBz4buRIFVuaWNvZGUKICAgIHByaW50bG4odHlwZWQuYWxsIHsgaXQgaW4gJzAnLi4nOScgfSkgICAgICAgICAgICAgIC8vIOKchSBmYWxzZTogY2jhu4kgbmjhuq1uIDAtOQp9Cg)

### K013 · `0.1 + 0.2 == 0.3` ra `false`

`🟢 mới học` · `Kotlin`

**Gặp:** điều kiện so sánh hai `Double` không bao giờ đúng; test `assertEquals(0.3, a + b)` fail với `0.30000000000000004`.

**Vì sao:** `Double` lưu số ở hệ nhị phân, `0.1` không biểu diễn chính xác được. Sai số nhỏ cộng dồn lại.

**Sửa:** so sánh với sai số cho phép. Trong test dùng `assertEquals(expected, actual, absoluteTolerance)`.

```kotlin
a + b == 0.3                    // ❌
abs((a + b) - 0.3) < 1e-9       // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbi5tYXRoLmFicwoKZnVuIG1haW4oKSB7CiAgICB2YWwgc3VtID0gMC4xICsgMC4yCiAgICBwcmludGxuKHN1bSkgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIDAuMzAwMDAwMDAwMDAwMDAwMDQKICAgIHByaW50bG4oc3VtID09IDAuMykgICAgICAgICAgICAgICAgICAgLy8g4p2MIGZhbHNlCiAgICBwcmludGxuKGFicyhzdW0gLSAwLjMpIDwgMWUtOSkgICAgICAgIC8vIOKchSB0cnVlCn0K) · Xem thêm: K014

### K014 · Tính tiền tip bị hụt 1 đồng

`🟡 đã làm app` · `Kotlin`

**Gặp:** 15% của 99.999 đồng ra 14.999 thay vì 15.000; tổng hoá đơn lệch so với máy tính tay.

**Vì sao:** tiền lưu `Long` (đúng), nhưng `bill * percent / 100` là chia nguyên nên **cắt** phần lẻ (14.999,85 → 14.999), không làm tròn.

**Sửa:** cộng nửa số chia trước khi chia để làm tròn nửa lên. Không chuyển sang `Double` để "cho tròn": tiền dùng `Double` sẽ dính K013.

```kotlin
bill * tipPercent / 100           // ❌ 14999
(bill * tipPercent + 50) / 100    // ✅ 15000
```

**Gặp ở:** [`TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/rules/Rules.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/rules/Rules.kt)

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgYmlsbCA9IDk5Xzk5OUwgICAgICAgICAgICAgICAgICAgIC8vIMSR4buTbmcKICAgIHZhbCB0aXBQZXJjZW50ID0gMTUKICAgIHByaW50bG4oYmlsbCAqIHRpcFBlcmNlbnQgLyAxMDApICAgICAgICAgIC8vIOKdjCAxNDk5OSAoMTQ5OTksODUgYuG7iyBj4bqvdCkKICAgIHByaW50bG4oKGJpbGwgKiB0aXBQZXJjZW50ICsgNTApIC8gMTAwKSAgIC8vIOKchSAxNTAwMCAobMOgbSB0csOybiBu4butYSBsw6puKQp9Cg) · Xem thêm: [App Tip Split](../apps/01-tip-split.md)

### K015 · Chia đều tiền, cả nhóm trả thiếu

`🟡 đã làm app` · `Kotlin`

**Gặp:** 100.000 chia 3 người, mỗi người 33.333, thu về 99.999: thiếu 1 đồng.

**Vì sao:** `/` trên số nguyên luôn làm tròn **về 0** (với số dương là tròn xuống). Phần dư bị bỏ.

**Sửa:** chia làm tròn lên: `(a + b - 1) / b` với `a`, `b` dương. Thừa vài đồng tốt hơn thiếu.

```kotlin
total / people                    // ❌ 33333
(total + people - 1) / people     // ✅ 33334
```

**Gặp ở:** [`TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/rules/Rules.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/rules/Rules.kt)

[▶ Chạy thử](../playground/index.html#code=ZnVuIGNlaWxEaXYoYTogTG9uZywgYjogTG9uZykgPSAoYSArIGIgLSAxKSAvIGIKCmZ1biBtYWluKCkgewogICAgdmFsIHRvdGFsID0gMTAwXzAwMEwKICAgIHZhbCBwZW9wbGUgPSAzTAogICAgdmFsIHNoYXJlID0gdG90YWwgLyBwZW9wbGUgICAgICAgICAgICAgICAgIC8vIOKdjCAzMzMzMwogICAgcHJpbnRsbigidGh1IMSRxrDhu6NjICR7c2hhcmUgKiBwZW9wbGV9IikgICAgICAvLyA5OTk5OTogdGhp4bq_dSAxIMSR4buTbmcKICAgIHZhbCBmYWlyID0gY2VpbERpdih0b3RhbCwgcGVvcGxlKSAgICAgICAgICAvLyDinIUgMzMzMzQKICAgIHByaW50bG4oInRodSDEkcaw4bujYyAke2ZhaXIgKiBwZW9wbGV9IikgICAgICAgLy8gMTAwMDAyOiDEkeG7pwp9Cg)

### K016 · Kiểm số lẻ bằng `% 2 == 1` sai với số âm

`🟢 mới học` · `Kotlin`

**Gặp:** `isOdd(-7)` trả `false`.

**Vì sao:** `%` trong Kotlin là phép **lấy dư** cùng dấu với số bị chia: `-7 % 2 = -1`, không phải `1`.

**Sửa:** kiểm `% 2 != 0`, hoặc dùng `mod()` (kết quả luôn cùng dấu với số chia, giống toán học).

```kotlin
n % 2 == 1        // ❌ sai với n âm
n % 2 != 0        // ✅
(-7).mod(2)       // ✅ 1
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIGlzT2RkKG46IEludCkgPSBuICUgMiA9PSAxICAgICAgICAgICAgLy8g4p2MIHNhaSB24bubaSBz4buRIMOibQoKZnVuIG1haW4oKSB7CiAgICBwcmludGxuKC03ICUgMikgICAgICAgICAgICAgICAgICAgICAgICAvLyAtMQogICAgcHJpbnRsbihpc09kZCgtNykpICAgICAgICAgICAgICAgICAgICAgLy8gZmFsc2UgKCEpCiAgICBwcmludGxuKCgtNykubW9kKDIpKSAgICAgICAgICAgICAgICAgICAvLyDinIUgMQogICAgcHJpbnRsbigtNyAlIDIgIT0gMCkgICAgICAgICAgICAgICAgICAgLy8g4pyFIHRydWUKfQo)

### K017 · `round(2.5)` ra `2.0`

`🟡 đã làm app` · `Kotlin`

**Gặp:** làm tròn số tiền `x,5` khi lên khi xuống: `round(2.5) = 2.0` nhưng `round(3.5) = 4.0`.

**Vì sao:** `kotlin.math.round` làm tròn **về số chẵn** khi đúng giữa (banker's rounding). `roundToInt()`/`roundToLong()`
thì làm tròn nửa lên.

**Sửa:** muốn "nửa lên" kiểu sách giáo khoa thì dùng `roundToInt()` / `roundToLong()`. Với tiền thì tốt nhất làm tròn trên số
nguyên như K014.

```kotlin
round(2.5)         // 2.0
2.5.roundToInt()   // 3
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbi5tYXRoLnJvdW5kCmltcG9ydCBrb3RsaW4ubWF0aC5yb3VuZFRvSW50CgpmdW4gbWFpbigpIHsKICAgIHByaW50bG4ocm91bmQoMi41KSkgICAgICAgICAgLy8gMi4wICAobMOgbSB0csOybiB24buBIHPhu5EgY2jhurVuKQogICAgcHJpbnRsbihyb3VuZCgzLjUpKSAgICAgICAgICAvLyA0LjAKICAgIHByaW50bG4oMi41LnJvdW5kVG9JbnQoKSkgICAgLy8gMyAgIChu4butYSBsw6puKQogICAgcHJpbnRsbihNYXRoLnJvdW5kKDIuNSkpICAgICAvLyAzICAgKGphdmEubGFuZy5NYXRoLCBu4butYSBsw6puKQp9Cg)

### K018 · `Cannot round NaN value.`

`🟡 đã làm app` · `Kotlin`

**Gặp:** crash `IllegalArgumentException: Cannot round NaN value.` khi màn hình mới mở, tổng còn bằng 0.

**Vì sao:** `0.0 / 0` ra `NaN`, và `roundToInt()` / `roundToLong()` từ chối `NaN`. Ngược lại số quá lớn thì **không** lỗi mà bị kẹp
ở `Long.MAX_VALUE`, ra số sai im lặng.

**Sửa:** kiểm `isNaN()` / `isInfinite()` (hoặc chặn mẫu số bằng 0) trước khi làm tròn, và chặn đầu vào quá lớn.

```kotlin
if (value.isNaN() || value.isInfinite()) return "—"   // ✅
```

**Gặp ở:** [`RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/rules/Amount.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/rules/Amount.kt)

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbi5tYXRoLnJvdW5kVG9JbnQKaW1wb3J0IGtvdGxpbi5tYXRoLnJvdW5kVG9Mb25nCgpmdW4gbWFpbigpIHsKICAgIHByaW50bG4oMWUzMC5yb3VuZFRvTG9uZygpKSAgICAgICAgICAvLyA5MjIzMzcyMDM2ODU0Nzc1ODA3OiBi4buLIGvhurlwLCBraMO0bmcgYsOhbwogICAgdmFsIHJhdGlvID0gMC4wIC8gMC4wICAgICAgICAgICAgICAgIC8vIE5hTiwgdmQgY2hpYSBjaG8gdOG7lW5nID0gMAogICAgcHJpbnRsbihyYXRpby5yb3VuZFRvSW50KCkpICAgICAgICAgIC8vIOKdjCBJbGxlZ2FsQXJndW1lbnRFeGNlcHRpb246IENhbm5vdCByb3VuZCBOYU4gdmFsdWUuCn0K) · Xem thêm: K019, K020

### K019 · Kết quả ra `NaN` và lan khắp nơi

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** khoảng cách GPS hiện `NaN km`, tổng quãng đường thành `NaN` từ một điểm trở đi.

**Vì sao:** sai số làm tròn đẩy giá trị ra ngoài miền của hàm (`asin` nhận `1.0000000000000002`, `sqrt` nhận `-1e-17`) thì ra `NaN`.
Mọi phép tính có `NaN` đều ra `NaN`, và `NaN == NaN` là `false` nên so sánh `== Double.NaN` không bắt được.

**Sửa:** kẹp đầu vào về đúng miền (`min(1.0, h)`, `max(0.0, x)`), kiểm bằng `isNaN()`.

```kotlin
asin(h)                 // ❌ NaN khi h nhỉnh hơn 1
asin(min(1.0, h))       // ✅
x == Double.NaN         // ❌ luôn false
x.isNaN()               // ✅
```

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/rules/Geo.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/rules/Geo.kt)

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbi5tYXRoLmFzaW4KaW1wb3J0IGtvdGxpbi5tYXRoLm1pbgoKZnVuIG1haW4oKSB7CiAgICB2YWwgaCA9IDAuMSAqIDMgLyAwLjMgICAgICAgICAgICAgICAgICAgICAgIC8vIHNhaSBz4buRIGzDoG0gdHLDsm4gxJHhuql5IGggbmjhu4luaCBoxqFuIDEKICAgIHByaW50bG4oaCkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gMS4wMDAwMDAwMDAwMDAwMDAyCiAgICBwcmludGxuKGFzaW4oaCkpICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBOYU4KICAgIHByaW50bG4oYXNpbihtaW4oMS4wLCBoKSkpICAgICAgICAgICAgICAgICAgLy8g4pyFIDEuNTcwNzk2MzI2Nzk0ODk2NgogICAgdmFsIG5hbiA9IERvdWJsZS5OYU4KICAgIHByaW50bG4obmFuID09IG5hbikgICAgICAgICAgICAgICAgICAgICAgICAgLy8gZmFsc2U6IE5hTiBraMO0bmcgYuG6sW5nIGNow61uaCBuw7MKICAgIHByaW50bG4obmFuLmlzTmFOKCkpICAgICAgICAgICAgICAgICAgICAgICAgLy8g4pyFIHRydWUKfQo) · Xem thêm: K021

### K020 · `ArithmeticException: / by zero`

`🟢 mới học` · `Kotlin`

**Gặp:** crash `java.lang.ArithmeticException: / by zero` khi số người bằng 0. Cùng phép đó với `Double` thì không crash mà ra `Infinity` hoặc `NaN`.

**Vì sao:** chia số nguyên cho 0 thì ném lỗi; chia số thực cho 0 thì ra giá trị đặc biệt và chạy tiếp.

**Sửa:** kiểm mẫu số trước khi chia (`require(people > 0)` hoặc trả kết quả "chưa đủ dữ liệu").

```kotlin
100 / people                                  // ❌ crash khi people = 0
if (people == 0) return null else 100 / people // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICBwcmludGxuKDEwLjAgLyAwKSAgICAgICAgICAvLyBJbmZpbml0eQogICAgcHJpbnRsbigwLjAgLyAwKSAgICAgICAgICAgLy8gTmFOCiAgICB2YWwgcGVvcGxlID0gMAogICAgcHJpbnRsbigxMDAgLyBwZW9wbGUpICAgICAgLy8g4p2MIEFyaXRobWV0aWNFeGNlcHRpb246IC8gYnkgemVybwp9Cg)

### K021 · `-0.0` và `NaN` trong list, set

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `listOf(-0.0).contains(0.0)` là `false`, `setOf(0.0, -0.0)` có 2 phần tử, nhưng `listOf(Double.NaN).contains(Double.NaN)` lại `true`.

**Vì sao:** `==` giữa hai `Double` có kiểu tĩnh là `Double` dùng so sánh số học (IEEE 754): `0.0 == -0.0`, `NaN != NaN`. Khi số bị
đóng hộp (trong `List`, `Set`, `Any`) thì so bằng `equals()`, và `equals()` coi `-0.0 != 0.0`, `NaN == NaN`.

**Sửa:** không dùng `Double` làm key hay phần tử để tìm kiếm. Cần thì làm tròn về `Long` (vd. đơn vị nhỏ nhất) trước khi đưa vào set/map.

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICBwcmludGxuKDAuMCA9PSAtMC4wKSAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gdHJ1ZQogICAgcHJpbnRsbihsaXN0T2YoLTAuMCkuY29udGFpbnMoMC4wKSkgICAgICAgICAgIC8vIGZhbHNlICghKQogICAgcHJpbnRsbihEb3VibGUuTmFOID09IERvdWJsZS5OYU4pICAgICAgICAgICAgIC8vIGZhbHNlCiAgICBwcmludGxuKGxpc3RPZihEb3VibGUuTmFOKS5jb250YWlucyhEb3VibGUuTmFOKSkgLy8gdHJ1ZSAoISkKICAgIHByaW50bG4oc2V0T2YoMC4wLCAtMC4wKS5zaXplKSAgICAgICAgICAgICAgICAvLyAyCn0K) · Xem thêm: K019

### K022 · `Unresolved reference 'sqrt'`

`🟢 mới học` · `Kotlin`

**Gặp:** `Unresolved reference 'sqrt'.` (hoặc `'Locale'`, `'roundToInt'`, `'Random'`…) dù hàm đó có thật.

**Vì sao:** chỉ một phần nhỏ của thư viện chuẩn được import sẵn. `kotlin.math.*`, `kotlin.random.*`, `java.util.*` phải import.
Lỗi này cũng hiện khi gõ sai hoa thường (`Println`), hoặc khi gọi API Java trong `commonMain` (KMP).

**Sửa:** đặt con trỏ vào chữ đỏ, bấm **Alt + Enter** (macOS: **⌥ + Enter**) → **Import**. Không có gợi ý import thì kiểm tên,
kiểm dependency, kiểm source set.

```kotlin
import kotlin.math.sqrt     // ✅
val side = sqrt(16.0)
```

[▶ Chạy thử](../playground/index.html#code=Ly8gaW1wb3J0IGtvdGxpbi5tYXRoLnNxcnQgICAgICAgICAgLy8g4pyFIGLhu48gLy8g4bufIGTDsm5nIG7DoHkgbMOgIGjhur90IGzhu5dpCgpmdW4gbWFpbigpIHsKICAgIHZhbCBzaWRlID0gc3FydCgxNi4wKSAgICAgICAgICAgLy8g4p2MIFVucmVzb2x2ZWQgcmVmZXJlbmNlICdzcXJ0Jy4KICAgIHByaW50bG4oc2lkZSkKfQo) · Xem thêm: [KMP: source set](09-kmp.md)

### K023 · `Double` in ra `1.0E-4`

`🟡 đã làm app` · `Kotlin`

**Gặp:** file GPX/CSV có `lat="1.0E-4"`, giá hiện `1.23456789E7`; app khác đọc file thì lỗi hoặc ra sai.

**Vì sao:** `Double.toString()` (và string template `"$x"`) chuyển sang dạng khoa học khi số rất nhỏ (< 10⁻³) hoặc rất lớn (≥ 10⁷).

**Sửa:** khi ghi ra file hay hiện cho người dùng, luôn định dạng với số chữ số cố định. Trên JVM:
`"%.7f".format(Locale.ROOT, x)` hoặc `x.toBigDecimal().toPlainString()`; trong `commonMain` tự viết hàm định dạng.

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/data/GpxWriter.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/GpxWriter.kt)

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgbGF0ID0gMC4wMDAxCiAgICBwcmludGxuKGxhdCkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIDEuMEUtNAogICAgcHJpbnRsbigxMjM0NTY3OC45KSAgICAgICAgICAgICAgICAgICAgICAgICAvLyAxLjIzNDU2Nzg5RTcKICAgIHByaW50bG4oIiUuN2YiLmZvcm1hdChqYXZhLnV0aWwuTG9jYWxlLlJPT1QsIGxhdCkpICAgLy8g4pyFIDAuMDAwMTAwMCAoSlZNKQogICAgcHJpbnRsbihsYXQudG9CaWdEZWNpbWFsKCkudG9QbGFpblN0cmluZygpKSAgICAgICAgICAvLyDinIUgMC4wMDAxIChKVk0pCn0K) · Xem thêm: K024

### K024 · `"%.2f".format(x)` ra `1,50` trên máy tiếng Việt

`🟡 đã làm app` · `Kotlin` · `Android`

**Gặp:** trên máy dev ra `1.50`, trên máy người dùng đặt tiếng Việt/Đức ra `1,50`; JSON, URL hay file CSV gửi đi bị hỏng.

**Vì sao:** `String.format` (JVM) dùng `Locale` mặc định của máy để chọn dấu thập phân.

**Sửa:** dữ liệu cho máy đọc (JSON, file, query) thì truyền `Locale.ROOT`. Chỉ chữ hiện cho người đọc mới dùng locale của máy.

```kotlin
"%.2f".format(1.5)                 // ❌ "1,50" trên máy vi-VN
"%.2f".format(Locale.ROOT, 1.5)    // ✅ "1.50"
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGphdmEudXRpbC5Mb2NhbGUKCmZ1biBtYWluKCkgewogICAgTG9jYWxlLnNldERlZmF1bHQoTG9jYWxlLmZvckxhbmd1YWdlVGFnKCJ2aS1WTiIpKSAgIC8vIGdp4buRbmcgbcOheSDEkeG6t3QgdGnhur9uZyBWaeG7h3QKICAgIHByaW50bG4oIiUuMmYiLmZvcm1hdCgxLjUpKSAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gMSw1MCAoISkKICAgIHByaW50bG4oIiUuMmYiLmZvcm1hdChMb2NhbGUuUk9PVCwgMS41KSkgICAgICAgICAgICAgLy8g4pyFIDEuNTAKfQo)

### K025 · Mã hex in ra thiếu số 0

`🟢 mới học` · `Kotlin`

**Gặp:** gửi lệnh `"01" + pid.toString(16)` ra `"01a"` thay vì `"010A"`.

**Vì sao:** `toString(16)` không thêm số 0 ở đầu và dùng chữ thường.

**Sửa:** `padStart` cho đủ độ dài, rồi `uppercase()` nếu thiết bị cần chữ hoa.

```kotlin
0x0A.toString(16)                                // ❌ "a"
0x0A.toString(16).padStart(2, '0').uppercase()   // ✅ "0A"
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgYiA9IDB4MEEKICAgIHByaW50bG4oYi50b1N0cmluZygxNikpICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIGEgIOKdjCB0aGnhur91IHPhu5EgMCwgY2jhu68gdGjGsOG7nW5nCiAgICBwcmludGxuKGIudG9TdHJpbmcoMTYpLnBhZFN0YXJ0KDIsICcwJykudXBwZXJjYXNlKCkpICAgICAvLyDinIUgMEEKfQo)

### K026 · Byte đọc từ thiết bị ra số âm

`🟡 đã làm app` · `Kotlin`

**Gặp:** đọc dữ liệu Bluetooth/file nhị phân, byte `0xC8` thành `-56`; nhiệt độ, tốc độ tính ra số âm vô lý.

**Vì sao:** `Byte` trong Kotlin (như Java) có dấu: -128..127. Byte nào ≥ `0x80` đều thành số âm khi đổi sang `Int`.

**Sửa:** lấy giá trị không dấu bằng `toInt() and 0xFF`, hoặc dùng `toUByte()`.

```kotlin
raw[1].toInt()              // ❌ -56
raw[1].toInt() and 0xFF     // ✅ 200
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgcmF3ID0gYnl0ZUFycmF5T2YoMHg0MSwgMHhDOC50b0J5dGUoKSkKICAgIHByaW50bG4ocmF3WzFdKSAgICAgICAgICAgICAgICAgICAgLy8gLTU2OiBCeXRlIGPDsyBk4bqldSAoLTEyOC4uMTI3KQogICAgcHJpbnRsbihyYXdbMV0gKyA0MCkgICAgICAgICAgICAgICAvLyDinYwgLTE2CiAgICBwcmludGxuKChyYXdbMV0udG9JbnQoKSBhbmQgMHhGRikpIC8vIOKchSAyMDAKICAgIHByaW50bG4ocmF3WzFdLnRvVUJ5dGUoKSkgICAgICAgICAgLy8g4pyFIDIwMAp9Cg) · Xem thêm: K027

### K027 · Ghép hai byte bằng `shl` ra số sai

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `a shl 8 + b` ra `26` thay vì `6904`; viết `a << 8 | b` kiểu Java thì compiler báo lỗi cú pháp.

**Vì sao:** Kotlin không có toán tử `<<`, `|`, `&`; thay bằng hàm infix `shl`, `or`, `and`. Hàm infix có **độ ưu tiên thấp hơn**
`+`, `*`, nên `a shl 8 + b` là `a shl (8 + b)`.

**Sửa:** luôn đặt ngoặc quanh `shl`/`and`/`or`, hoặc viết bằng phép nhân cho dễ đọc.

```kotlin
a shl 8 + b         // ❌ a shl (8 + b)
(a shl 8) or b      // ✅
256 * a + b         // ✅ cách app ObdLink viết
```

**Gặp ở:** [`ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/rules/Pid.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/rules/Pid.kt) (RPM ghép từ hai byte A, B)

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgYSA9IDB4MUEKICAgIHZhbCBiID0gMHhGOAogICAgcHJpbnRsbihhIHNobCA4ICsgYikgICAgICAgICAgLy8g4p2MIDI2OiBoaeG7g3UgbMOgIGEgc2hsICg4ICsgYikKICAgIHByaW50bG4oKGEgc2hsIDgpICsgYikgICAgICAgIC8vIOKchSA2OTA0CiAgICBwcmludGxuKChhIHNobCA4KSBvciBiKSAgICAgICAvLyDinIUgNjkwNAogICAgcHJpbnRsbigyNTYgKiBhICsgYikgICAgICAgICAgLy8g4pyFIDY5MDQKfQo)

### K028 · `Operator '==' cannot be applied to 'Long' and 'Int'`

`🟢 mới học` · `Kotlin`

**Gặp:** so `id == count` (một bên `Long`, một bên `Int`) báo `Operator '==' cannot be applied to 'Long' and 'Int'.`
Tương tự với `Int` và `Double`.

**Vì sao:** Kotlin không tự đổi kiểu số khi so sánh bằng. Hai kiểu khác nhau thì coi như không bao giờ bằng nhau, nên compiler chặn luôn.

**Sửa:** đổi về cùng kiểu trước khi so. (So lớn/nhỏ `<`, `>` giữa `Int` và `Long` thì được.)

```kotlin
id == count             // ❌
id == count.toLong()    // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgaWQ6IExvbmcgPSAxCiAgICB2YWwgY291bnQ6IEludCA9IDEKICAgIHByaW50bG4oaWQgPT0gY291bnQpICAgICAgICAgICAgICAvLyDinYwgT3BlcmF0b3IgJz09JyBjYW5ub3QgYmUgYXBwbGllZCB0byAnTG9uZycgYW5kICdJbnQnCiAgICAvLyBwcmludGxuKGlkID09IGNvdW50LnRvTG9uZygpKSAgLy8g4pyFCn0K)

### K029 · `Variable 'total' must be initialized`

`🟢 mới học` · `Kotlin`

**Gặp:** `Variable 'total' must be initialized.` ở dòng đọc biến.

**Vì sao:** biến cục bộ khai báo không có giá trị, và có ít nhất một đường đi (một nhánh `if` không gán) tới chỗ đọc mà chưa gán.

**Sửa:** cho giá trị ban đầu, hoặc gán ở mọi nhánh. Gọn nhất là viết `if`/`when` thành biểu thức.

```kotlin
var total: Int
if (vip) total = 1
println(total)                      // ❌
val total = if (vip) 1 else 0       // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YXIgdG90YWw6IEludAogICAgaWYgKE1hdGgucmFuZG9tKCkgPiAyKSB0b3RhbCA9IDEKICAgIHByaW50bG4odG90YWwpICAgICAgICAgICAgICAgLy8g4p2MIFZhcmlhYmxlICd0b3RhbCcgbXVzdCBiZSBpbml0aWFsaXplZAogICAgLy8gU-G7rWE6IHZhciB0b3RhbCA9IDAgIChob-G6t2MgZ8OhbiDhu58gTeG7jEkgbmjDoW5oKQp9Cg) · Xem thêm: K046

### K030 · `println(array)` ra `[I@1b6d3586`

`🟢 mới học` · `Kotlin`

**Gặp:** in một `Array` ra chuỗi lạ kiểu `[Ljava.lang.Integer;@76ed5528`, log không đọc được.

**Vì sao:** `Array` là mảng JVM, `toString()` của nó chỉ in kiểu và địa chỉ. `List` mới in nội dung.

**Sửa:** `contentToString()`, `joinToString()`, hoặc dùng `List` thay cho `Array`. So sánh hai mảng cũng phải dùng `contentEquals`.

```kotlin
println(ids)                      // ❌ [Ljava.lang.Integer;@...
println(ids.contentToString())    // ✅ [1, 2, 3]
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgaWRzID0gYXJyYXlPZigxLCAyLCAzKQogICAgcHJpbnRsbihpZHMpICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBbTGphdmEubGFuZy5JbnRlZ2VyO0AuLi4KICAgIHByaW50bG4oaWRzLmNvbnRlbnRUb1N0cmluZygpKSAgICAvLyDinIUgWzEsIDIsIDNdCiAgICBwcmludGxuKGlkcy50b0xpc3QoKSkgICAgICAgICAgICAgLy8g4pyFIFsxLCAyLCAzXQp9Cg) · Xem thêm: [Phụ lục 2.3](../reference/kotlin-pitfalls.md)

### K031 · In ký tự `$` trong chuỗi

`🟢 mới học` · `Kotlin`

**Gặp:** muốn in đúng chữ `$price` thì ra giá trị của biến; trong raw string `"""..."""` viết `\$` cũng không ăn.

**Vì sao:** `$` theo sau là tên biến luôn là string template. Raw string không có escape `\`.

**Sửa:** chuỗi thường viết `\$`. Raw string viết `${'$'}`, hoặc (Kotlin 2.2+) dùng chuỗi nhiều dấu `$`: `$$"""..."""` thì chỉ
`$$name` mới là template, `$name` giữ nguyên.

```kotlin
"Mã: \$price"                  // ✅ Mã: $price
"""Mã: ${'$'}price"""          // ✅
$$"""{"a": "$ref", "b": $$price}"""   // ✅ Kotlin 2.2+
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgcHJpY2UgPSA1MAogICAgcHJpbnRsbigiR2nDoTogJHByaWNlJCIpICAgICAgICAgICAgICAgICAvLyBHacOhOiA1MCQgICgkIGN14buRaSBraMO0bmcgdGhlbyBzYXUgdMOqbiBuw6BvKQogICAgcHJpbnRsbigiTcOjOiBcJHByaWNlIikgICAgICAgICAgICAgICAgICAvLyDinIUgTcOjOiAkcHJpY2UgKGVzY2FwZSkKICAgIHByaW50bG4oIiIiTcOjOiAkeyckJ31wcmljZSIiIikgICAgICAgICAgLy8g4pyFIHJhdyBzdHJpbmcga2jDtG5nIGPDsyBcLCBkw7luZyAkeyckJ30KICAgIHByaW50bG4oJCQiIiJNw6M6ICRwcmljZSwgZ2nDoTogJCRwcmljZSIiIikgLy8g4pyFIEtvdGxpbiAyLjIrOiBjaOG7iSAkJCBt4bubaSBsw6AgdGVtcGxhdGUKfQo) · Xem thêm: [Phụ lục 2.6](../reference/kotlin-pitfalls.md)

### K032 · Raw string giữ cả thụt lề

`🟢 mới học` · `Kotlin`

**Gặp:** SQL hay JSON viết bằng `"""..."""` bị thừa xuống dòng đầu/cuối và một đống dấu cách; `\n` trong đó in ra nguyên văn.

**Vì sao:** raw string giữ đúng từng ký tự giữa hai dấu `"""`, kể cả thụt lề của code và không xử lý escape.

**Sửa:** gọi `trimIndent()` (bỏ thụt lề chung) hoặc `trimMargin()` (bỏ tới dấu `|`). Cần xuống dòng thì xuống dòng thật.

```kotlin
val sql = """
    SELECT *
    FROM note
""".trimIndent()      // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgc3FsID0gIiIiCiAgICAgICAgU0VMRUNUICoKICAgICAgICBGUk9NIG5vdGUKICAgICIiIgogICAgcHJpbnRsbigiWyRzcWxdIikgICAgICAgICAgICAgICAgIC8vIOKdjCBjw7JuIHh14buRbmcgZMOybmcgxJHhuqd1L2N14buRaSB2w6AgOCBk4bqldSBjw6FjaAogICAgcHJpbnRsbigiWyR7c3FsLnRyaW1JbmRlbnQoKX1dIikgIC8vIOKchQogICAgcHJpbnRsbigiIiJhXG5iIiIiKSAgICAgICAgICAgICAgIC8vIFxuIEtIw5RORyB4deG7kW5nIGTDsm5nIHRyb25nIHJhdyBzdHJpbmcKfQo)

### K033 · Gọi `trim()` mà chuỗi không đổi

`🟢 mới học` · `Kotlin`

**Gặp:** `name.trim()` hay `text.replace("a", "b")` xong, in ra vẫn như cũ.

**Vì sao:** `String` không bao giờ đổi. Mọi hàm "sửa" chuỗi trả về **chuỗi mới**, bỏ kết quả thì như chưa gọi.

**Sửa:** gán kết quả vào biến (tốt nhất là biến `val` mới).

```kotlin
name.trim()                 // ❌ kết quả bị bỏ
val clean = name.trim()     // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YXIgbmFtZSA9ICIgIFTDuiAgIgogICAgbmFtZS50cmltKCkgICAgICAgICAgICAgICAgIC8vIOKdjCBr4bq_dCBxdeG6oyBi4buLIGLhu48sIG5hbWUga2jDtG5nIMSR4buVaQogICAgcHJpbnRsbigiWyRuYW1lXSIpCiAgICBuYW1lID0gbmFtZS50cmltKCkgICAgICAgICAgLy8g4pyFIGfDoW4gbOG6oWkKICAgIHByaW50bG4oIlskbmFtZV0iKQp9Cg)

### K034 · `StringIndexOutOfBoundsException` khi cắt chuỗi

`🟢 mới học` · `Kotlin`

**Gặp:** `java.lang.StringIndexOutOfBoundsException: begin 0, end 10, length 3` khi cắt tiêu đề cho ngắn.

**Vì sao:** `substring(0, 10)` đòi chuỗi có đủ 10 ký tự.

**Sửa:** `take(n)` lấy **tối đa** n ký tự, không bao giờ ném lỗi. Tương tự `drop`, `takeLast`.

```kotlin
title.substring(0, 10)       // ❌
title.take(10)               // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgdGl0bGUgPSAiUGjhu58iCiAgICBwcmludGxuKHRpdGxlLnRha2UoMTApKSAgICAgICAgIC8vIOKchSBQaOG7nzogbOG6pXkgdOG7kWkgxJFhIDEwIGvDvSB04buxCiAgICBwcmludGxuKHRpdGxlLnN1YnN0cmluZygwLCAxMCkpIC8vIOKdjCBTdHJpbmdJbmRleE91dE9mQm91bmRzRXhjZXB0aW9uCn0K)

### K035 · Hai chữ "ú" trông giống mà `==` ra `false`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** tìm kiếm không ra ghi chú dù gõ đúng chữ; `length` của `"ú"` lúc là 1 lúc là 2; emoji `👍` có `length = 2`.

**Vì sao:** cùng một chữ có thể lưu hai cách: dựng sẵn (`ú` = U+00FA) hoặc tổ hợp (`u` + dấu sắc U+0301), chữ dán từ macOS hay một số
bàn phím thường là dạng tổ hợp. `length` đếm đơn vị UTF-16, không đếm "chữ" người nhìn thấy.

**Sửa:** chuẩn hoá trước khi so sánh hay lưu: trên JVM `Normalizer.normalize(s, Normalizer.Form.NFC)`; trong `commonMain` thì
bỏ dấu tổ hợp U+0300..U+036F như app PocketNotes làm.

**Gặp ở:** [`PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/rules/Search.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/rules/Search.kt)

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgY29tcG9zZWQgPSAiXHUwMEZBIiAgICAgICAgICAvLyDDujogMSBrw70gdOG7sQogICAgdmFsIGRlY29tcG9zZWQgPSAidVx1MDMwMSIgICAgICAgLy8gdSArIGThuqV1IHPhuq9jOiB0csO0bmcgeSBo4buHdAogICAgcHJpbnRsbihjb21wb3NlZC5sZW5ndGgpICAgICAgICAgLy8gMQogICAgcHJpbnRsbihkZWNvbXBvc2VkLmxlbmd0aCkgICAgICAgLy8gMgogICAgcHJpbnRsbihjb21wb3NlZCA9PSBkZWNvbXBvc2VkKSAgLy8gZmFsc2UKICAgIHByaW50bG4oIvCfkY0iLmxlbmd0aCkgICAgICAgICAgICAgLy8gMjogZW1vamkgbMOgIDIgQ2hhciBVVEYtMTYKICAgIHZhbCBuID0gamF2YS50ZXh0Lk5vcm1hbGl6ZXIubm9ybWFsaXplKGRlY29tcG9zZWQsIGphdmEudGV4dC5Ob3JtYWxpemVyLkZvcm0uTkZDKQogICAgcHJpbnRsbihuID09IGNvbXBvc2VkKSAgICAgICAgICAgLy8g4pyFIHRydWUgKEpWTSkKfQo) · Xem thêm: K036

### K036 · Bỏ dấu tiếng Việt vẫn còn chữ `đ`

`🟡 đã làm app` · `Kotlin`

**Gặp:** tìm "da nang" không ra "Đà Nẵng"; hàm bỏ dấu ra `Đa Nang`.

**Vì sao:** `đ`/`Đ` là chữ riêng, không phải "d + dấu". Tách Unicode (NFD) rồi bỏ dấu tổ hợp không đụng tới nó.

**Sửa:** sau khi bỏ dấu, map tay `đ → d`, `Đ → D`.

```kotlin
stripMarks("Đà Nẵng")                                   // ❌ Đa Nang
stripMarks("Đà Nẵng").replace('Đ', 'D').replace('đ', 'd') // ✅ Da Nang
```

**Gặp ở:** [`PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/rules/Search.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/rules/Search.kt)

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGphdmEudGV4dC5Ob3JtYWxpemVyCgpmdW4gc3RyaXBNYXJrcyhzOiBTdHJpbmcpID0KICAgIE5vcm1hbGl6ZXIubm9ybWFsaXplKHMsIE5vcm1hbGl6ZXIuRm9ybS5ORkQpLnJlcGxhY2UoUmVnZXgoIlxccHtNbn0rIiksICIiKQoKZnVuIG1haW4oKSB7CiAgICBwcmludGxuKHN0cmlwTWFya3MoIlBo4bufIEjDoCBO4buZaSIpKSAgICAgICAgLy8gUGhvIEhhIE5vaQogICAgcHJpbnRsbihzdHJpcE1hcmtzKCLEkMOgIE7hurVuZyIpKSAgICAgICAgICAgLy8g4p2MIMSQYSBOYW5nOiDEkCBraMO0bmcgYuG7iyBi4buPCiAgICBwcmludGxuKHN0cmlwTWFya3MoIsSQw6AgTuG6tW5nIikucmVwbGFjZSgnxJAnLCAnRCcpLnJlcGxhY2UoJ8SRJywgJ2QnKSkgIC8vIOKchSBEYSBOYW5nCn0K)

### K037 · `toLowerCase()` báo deprecated, và chữ `i` của tiếng Thổ

`🟢 mới học` · `Kotlin`

**Gặp:** code copy từ Java/tutorial cũ báo `'fun String.toLowerCase(): String' is deprecated. Use lowercase() instead.`
(là lỗi, không còn là cảnh báo); `capitalize()` thì bị gạch ngang. Hoặc trên máy đặt tiếng Thổ Nhĩ Kỳ, `"TITLE"` hạ chữ thường thành `tıtle` và so sánh key hỏng.

**Vì sao:** các hàm cũ phụ thuộc locale của máy nên đã bị thay thế. Hàm mới `lowercase()`/`uppercase()` luôn dùng
`Locale.ROOT`, ra cùng kết quả trên mọi máy.

**Sửa:** `lowercase()`, `uppercase()`, `replaceFirstChar { it.uppercase() }` thay cho `capitalize()`. Chỉ truyền locale khi
đang làm chữ hiện cho người đọc.

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGphdmEudXRpbC5Mb2NhbGUKCmZ1biBtYWluKCkgewogICAgTG9jYWxlLnNldERlZmF1bHQoTG9jYWxlLmZvckxhbmd1YWdlVGFnKCJ0ci1UUiIpKSAgLy8gbcOheSDEkeG6t3QgdGnhur9uZyBUaOG7lSBOaMSpIEvhu7MKICAgIHByaW50bG4oIlRJVExFIi5sb3dlcmNhc2UoKSkgICAgICAgICAgICAgICAgICAgICAgICAvLyDinIUgdGl0bGU6IGxvd2VyY2FzZSgpIGx1w7RuIGTDuW5nIExvY2FsZS5ST09UCiAgICBwcmludGxuKCJUSVRMRSIubG93ZXJjYXNlKExvY2FsZS5nZXREZWZhdWx0KCkpKSAgICAgLy8gdMSxdGxlIChpIGtow7RuZyBjaOG6pW0pCiAgICAvLyBwcmludGxuKCJUSVRMRSIudG9Mb3dlckNhc2UoKSkgICAgICAgICAgICAgICAgICAgLy8g4p2MIGLhu48gLy8gOiBow6BtIGPFqSDEkcOjIGLhu4sga2hvw6EKfQo)

### K038 · `isEmpty()` cho qua chuỗi toàn dấu cách

`🟢 mới học` · `Kotlin`

**Gặp:** form cho lưu tên `"   "`; danh sách hiện dòng trống.

**Vì sao:** `isEmpty()` chỉ kiểm độ dài bằng 0. Dấu cách, tab, xuống dòng vẫn là ký tự.

**Sửa:** kiểm dữ liệu người dùng gõ bằng `isBlank()` / `isNotBlank()`, và `trim()` trước khi lưu.

```kotlin
if (name.isEmpty()) error()      // ❌ "   " lọt qua
if (name.isBlank()) error()      // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgbmFtZSA9ICIgICAiCiAgICBwcmludGxuKG5hbWUuaXNFbXB0eSgpKSAgICAgICAvLyDinYwgZmFsc2U6IGPDsyAzIGThuqV1IGPDoWNoCiAgICBwcmludGxuKG5hbWUuaXNCbGFuaygpKSAgICAgICAvLyDinIUgdHJ1ZQogICAgcHJpbnRsbihuYW1lLmlzTm90QmxhbmsoKSkgICAgLy8gZmFsc2UKfQo) · Xem thêm: K084

### K039 · `split("\\.")` không tách được

`🟢 mới học` · `Kotlin`

**Gặp:** `"2.4.21".split("\\.")` ra `[2.4.21]`, một phần tử. Code y hệt chạy đúng trong Java.

**Vì sao:** `String.split(String)` của Kotlin tìm **đúng chuỗi** đó, không coi là regex. Java thì coi là regex nên phải escape dấu chấm.

**Sửa:** bỏ escape: `split(".")`. Thật sự cần regex thì bọc `Regex(...)`.

```kotlin
version.split("\\.")          // ❌ kiểu Java
version.split(".")            // ✅
text.split(Regex("\\s+"))     // ✅ regex
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgdmVyc2lvbiA9ICIyLjQuMjEiCiAgICBwcmludGxuKHZlcnNpb24uc3BsaXQoIlxcLiIpKSAgICAgICAvLyDinYwgWzIuNC4yMV06IEtvdGxpbiB0w6xtIMSRw7puZyBjaHXhu5dpICJcLiIgY2jhu6kga2jDtG5nIHBo4bqjaSByZWdleAogICAgcHJpbnRsbih2ZXJzaW9uLnNwbGl0KCIuIikpICAgICAgICAgLy8g4pyFIFsyLCA0LCAyMV0KICAgIHByaW50bG4odmVyc2lvbi5zcGxpdChSZWdleCgiXFwuIikpKS8vIOKchSByZWdleCB0aMOsIGLhu41jIFJlZ2V4KC4uLikKfQo)

### K040 · Tách dòng bằng `"\n"` ra một dòng dài

`🟡 đã làm app` · `Kotlin`

**Gặp:** đọc phản hồi từ thiết bị (ELM327, modem, file Windows), `split("\n")` ra đúng một dòng, hoặc mỗi dòng dính `\r` ở cuối nên so sánh chuỗi sai.

**Vì sao:** không phải ai cũng xuống dòng bằng `\n`. ELM327 dùng `\r`, Windows dùng `\r\n`.

**Sửa:** `lines()` tách được cả `\n`, `\r\n`, `\r`. Hoặc `split('\r', '\n')` rồi lọc dòng trống.

```kotlin
raw.split("\n")                              // ❌
raw.lines().filter { it.isNotBlank() }       // ✅
```

**Gặp ở:** [`ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/rules/Parser.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/rules/Parser.kt)

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgcmF3ID0gIjQxIDBDIDFBIEY4XHI0MSAwRCAzMlxyXHI-IiAgICAgIC8vIEVMTTMyNyB4deG7kW5nIGTDsm5nIGLhurFuZyBccgogICAgcHJpbnRsbihyYXcuc3BsaXQoIlxuIikuc2l6ZSkgICAgICAgICAgICAgICAvLyDinYwgMSBkw7JuZyBkw6BpCiAgICBwcmludGxuKHJhdy5saW5lcygpLmZpbHRlciB7IGl0LmlzTm90QmxhbmsoKSAmJiBpdCAhPSAiPiIgfSkgICAvLyDinIUgWzQxIDBDIDFBIEY4LCA0MSAwRCAzMl0KfQo)

### K041 · Sắp xếp số lưu dạng chuỗi: `10` đứng trước `2`

`🟢 mới học` · `Kotlin`

**Gặp:** danh sách trang, mã đơn ra `[10, 2, 9]`; `"10" < "9"` là `true`.

**Vì sao:** chuỗi so từng ký tự từ trái sang (thứ tự từ điển): `'1' < '9'` nên `"10" < "9"`.

**Sửa:** sắp xếp theo giá trị số: `sortedBy { it.toInt() }`. Tốt hơn: lưu số bằng kiểu số ngay từ đầu.

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICBwcmludGxuKCIxMCIgPCAiOSIpICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIHRydWU6IHNvIHThu6tuZyBrw70gdOG7sSwgJzEnIDwgJzknCiAgICB2YWwgcGFnZXMgPSBsaXN0T2YoIjEwIiwgIjkiLCAiMiIpCiAgICBwcmludGxuKHBhZ2VzLnNvcnRlZCgpKSAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBbMTAsIDIsIDldCiAgICBwcmludGxuKHBhZ2VzLnNvcnRlZEJ5IHsgaXQudG9JbnQoKSB9KSAgICAgICAgICAgIC8vIOKchSBbMiwgOSwgMTBdCn0K)

### K042 · `'7'.toInt()` ra 55

`🟢 mới học` · `Kotlin`

**Gặp:** đổi một ký tự số ra số thì được `55` (mã ký tự) thay vì `7`; code cũ `c.toInt()` bị báo deprecated.
Viết `'ab'` thì compiler báo `Too many characters in a character literal`.

**Vì sao:** `Char` là ký tự, không phải số. `code` cho mã Unicode; muốn giá trị chữ số phải đổi riêng. Dấu nháy đơn chỉ chứa một ký tự.

**Sửa:** `digitToInt()` (ném lỗi nếu không phải chữ số) hoặc `digitToIntOrNull()`. Chuỗi thì dùng nháy kép.

```kotlin
'7'.code            // 55
'7'.digitToInt()    // ✅ 7
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgYyA9ICI3IlswXQogICAgcHJpbnRsbihjLmNvZGUpICAgICAgICAgICAgICAgLy8gNTU6IG3DoyBVbmljb2RlIGPhu6dhICc3Jywga2jDtG5nIHBo4bqjaSA3CiAgICBwcmludGxuKGMuZGlnaXRUb0ludCgpKSAgICAgICAvLyDinIUgNwogICAgcHJpbnRsbihjIC0gJzAnKSAgICAgICAgICAgICAgLy8g4pyFIDcKICAgIC8vIHByaW50bG4oYy50b0ludCgpKSAgICAgICAgIC8vIOKdjCBi4buPIC8vIDogdG9JbnQoKSB0csOqbiBDaGFyIMSRw6MgYuG7iyBraG_DoQp9Cg)

### K043 · `'when' expression must be exhaustive`

`🟢 mới học` · `Kotlin`

**Gặp:** `'when' expression must be exhaustive. Add the 'Fifteen' branch or an 'else' branch.` sau khi thêm một giá trị enum.

**Vì sao:** `when` dùng làm biểu thức (trả về giá trị), hoặc chạy trên `enum`/`sealed`/`Boolean`, phải phủ hết mọi trường hợp.
Đây là lỗi **tốt**: compiler chỉ ra mọi chỗ cần xử lý giá trị mới.

**Sửa:** thêm nhánh còn thiếu (Alt + Enter → **Add remaining branches**). Với enum/sealed, ưu tiên liệt kê đủ thay vì thêm `else`.

```kotlin
when (tip) {
    Tip.None -> "0%"
    Tip.Ten -> "10%"
    Tip.Fifteen -> "15%"   // ✅ nhánh mới
}
```

[▶ Chạy thử](../playground/index.html#code=ZW51bSBjbGFzcyBUaXAgeyBOb25lLCBUZW4sIEZpZnRlZW4gfQoKZnVuIGxhYmVsKHQ6IFRpcCk6IFN0cmluZyA9IHdoZW4gKHQpIHsgICAgICAvLyDinYwgdGhp4bq_dSBuaMOhbmggRmlmdGVlbgogICAgVGlwLk5vbmUgLT4gIjAlIgogICAgVGlwLlRlbiAtPiAiMTAlIgogICAgLy8gVGlwLkZpZnRlZW4gLT4gIjE1JSIgICAgICAgICAgICAgICAgICAvLyDinIUgYuG7jyAvLyDEkeG7gyBo4bq_dCBs4buXaQp9CgpmdW4gbWFpbigpID0gcHJpbnRsbihsYWJlbChUaXAuVGVuKSkK) · Xem thêm: K044, [Class, sealed, enum](04-classes.md)

### K044 · `else` trong `when` nuốt giá trị enum mới

`🟡 đã làm app` · `Kotlin`

**Gặp:** thêm trạng thái `Refunded`, app compile ngon, nhưng đơn hoàn tiền hiện màu xanh "đã thanh toán".

**Vì sao:** nhánh `else` khớp mọi giá trị chưa liệt kê, kể cả giá trị thêm sau. Compiler không còn lý do để báo K043.

**Sửa:** với `enum`/`sealed`, liệt kê từng giá trị, không dùng `else`. Thêm giá trị mới thì mọi `when` cần sửa sẽ đỏ.

```kotlin
when (s) { Draft -> ...; Sent -> ...; else -> "xanh" }          // ❌
when (s) { Draft -> ...; Sent -> ...; Paid -> "xanh"; Refunded -> "đỏ" }  // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZW51bSBjbGFzcyBTdGF0dXMgeyBEcmFmdCwgU2VudCwgUGFpZCwgUmVmdW5kZWQgfSAgIC8vIFJlZnVuZGVkIG3hu5tpIHRow6ptCgpmdW4gY29sb3IoczogU3RhdHVzKSA9IHdoZW4gKHMpIHsKICAgIFN0YXR1cy5EcmFmdCAtPiAieMOhbSIKICAgIFN0YXR1cy5TZW50IC0-ICJ2w6BuZyIKICAgIGVsc2UgLT4gInhhbmgiICAgICAgICAgICAgLy8g4p2MIFJlZnVuZGVkIGzhurduZyBs4bq9IHRow6BuaCAieGFuaCIKfQoKZnVuIG1haW4oKSA9IHByaW50bG4oY29sb3IoU3RhdHVzLlJlZnVuZGVkKSkK)

### K045 · Nhánh `when` không bao giờ chạy

`🟢 mới học` · `Kotlin`

**Gặp:** điểm 10 vẫn xếp loại "Đạt" chứ không phải "Giỏi". IDE có thể gạch nhánh đó là không tới được.

**Vì sao:** `when` chạy **nhánh đầu tiên khớp** rồi dừng, không xét tiếp (không "fall through"). `score >= 5` đứng trước nên bắt luôn 9, 10.

**Sửa:** đặt điều kiện hẹp nhất lên trước, rộng nhất (và `else`) xuống cuối.

```kotlin
when {
    score >= 9 -> "Giỏi"      // ✅ hẹp trước
    score >= 5 -> "Đạt"
    else -> "Chưa đạt"
}
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIGdyYWRlKHNjb3JlOiBJbnQpID0gd2hlbiB7CiAgICBzY29yZSA-PSA1IC0-ICLEkOG6oXQiICAgICAgICAgICAvLyDinYwgbmjDoW5oIG7DoHkgYuG6r3QgbHXDtG4gOSwgMTAKICAgIHNjb3JlID49IDkgLT4gIkdp4buPaSIgICAgICAgICAgLy8ga2jDtG5nIGJhbyBnaeG7nSB04bubaQogICAgZWxzZSAtPiAiQ2jGsGEgxJHhuqF0Igp9CgpmdW4gbWFpbigpIHsKICAgIHByaW50bG4oZ3JhZGUoMTApKSAgICAgICAgICAgIC8vIMSQ4bqhdAogICAgLy8gU-G7rWE6IMSR4bq3dCDEkWnhu4F1IGtp4buHbiBo4bq5cCBuaOG6pXQgKD49IDkpIGzDqm4gdHLGsOG7m2MKfQo)

### K046 · `'if' must have both main and 'else' branches`

`🟢 mới học` · `Kotlin`

**Gặp:** `'if' must have both main and 'else' branches when used as an expression.`

**Vì sao:** Kotlin không có toán tử `? :` ba ngôi; `if` chính là biểu thức. Đã lấy giá trị của `if` thì phải có giá trị cho cả trường hợp sai.

**Sửa:** thêm `else`.

```kotlin
val discount = if (vip) 10             // ❌
val discount = if (vip) 10 else 0      // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgdmlwID0gdHJ1ZQogICAgdmFsIGRpc2NvdW50ID0gaWYgKHZpcCkgMTAgICAgICAvLyDinYwgJ2lmJyBtdXN0IGhhdmUgYm90aCBtYWluIGFuZCAnZWxzZScgYnJhbmNoZXMuLi4KICAgIC8vIHZhbCBkaXNjb3VudCA9IGlmICh2aXApIDEwIGVsc2UgMCAgIC8vIOKchQogICAgcHJpbnRsbihkaXNjb3VudCkKfQo)

### K047 · `for (i in 10..1)` không chạy lần nào

`🟢 mới học` · `Kotlin`

**Gặp:** vòng lặp đếm ngược không in gì; hoặc vòng lặp chạy thừa một lần vì `..` gồm cả số cuối.

**Vì sao:** `a..b` chỉ đếm lên, `3..1` là khoảng rỗng. `..` gồm cả `b`; `..<` (hay `until`) thì không.

**Sửa:** đếm ngược dùng `downTo`, bỏ số cuối dùng `..<`, nhảy bước dùng `step`.

```kotlin
for (i in 3..1)      // ❌ không chạy
for (i in 3 downTo 1) // ✅ 3 2 1
for (i in 0..<n)     // ✅ 0..n-1
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICBmb3IgKGkgaW4gMy4uMSkgcHJpbnQoaSkgICAgICAgICAgICAvLyDinYwga2jDtG5nIGluIGfDrDogMy4uMSBy4buXbmcKICAgIHByaW50bG4oKQogICAgZm9yIChpIGluIDMgZG93blRvIDEpIHByaW50KGkpICAgICAgLy8g4pyFIDMyMQogICAgcHJpbnRsbigpCiAgICBmb3IgKGkgaW4gMC4uPDMpIHByaW50KGkpICAgICAgICAgICAvLyAwMTI6IGtow7RuZyBn4buTbSAzIChnaeG7kW5nIDAgdW50aWwgMykKICAgIHByaW50bG4oKQogICAgZm9yIChpIGluIDAuLjEwIHN0ZXAgNSkgcHJpbnQoIiRpICIpLy8gMCA1IDEwCn0K) · Xem thêm: K048

### K048 · `Index 3 out of bounds for length 3`

`🟢 mới học` · `Kotlin`

**Gặp:** `IndexOutOfBoundsException: Index 3 out of bounds for length 3` ở vòng lặp cuối.

**Vì sao:** `0..list.size` chạy tới `list.size`, mà chỉ số lớn nhất là `size - 1`.

**Sửa:** dùng `list.indices`, `0..<list.size`, hoặc lặp thẳng phần tử (`for (x in list)`, `forEachIndexed`).

```kotlin
for (i in 0..names.size)   // ❌
for (i in names.indices)   // ✅
for (name in names)        // ✅ khỏi cần chỉ số
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIG1haW4oKSB7CiAgICB2YWwgbmFtZXMgPSBsaXN0T2YoIkFuIiwgIkLDrG5oIiwgIkNoaSIpCiAgICBmb3IgKGkgaW4gbmFtZXMuaW5kaWNlcykgcHJpbnRsbihuYW1lc1tpXSkgICAvLyDinIUgMC4uMgogICAgZm9yIChpIGluIDAuLm5hbWVzLnNpemUpIHByaW50bG4obmFtZXNbaV0pICAgLy8g4p2MIEluZGV4T3V0T2ZCb3VuZHNFeGNlcHRpb24g4bufIGkgPSAzCn0K) · Xem thêm: [Collections](03-collections.md)

### K049 · `and` / `or` vẫn chạy vế sau

`🟡 đã làm app` · `Kotlin`

**Gặp:** điều kiện `!offline and loadUser()...` vẫn gọi server khi offline; hoặc `x != null and x.length > 0` báo lỗi null.

**Vì sao:** `and`, `or` là hàm infix trên `Boolean` (và trên số là phép bit), **luôn tính cả hai vế**. Chỉ `&&`, `||` mới dừng sớm
khi đã biết kết quả.

**Sửa:** điều kiện logic luôn viết `&&`, `||`. `and`/`or` để dành cho phép bit trên số.

```kotlin
if (!offline and loadUser().isNotEmpty())   // ❌ gọi server dù offline
if (!offline && loadUser().isNotEmpty())    // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIGxvYWRVc2VyKCk6IFN0cmluZyB7IHByaW50bG4oIiAgZ-G7jWkgc2VydmVyISIpOyByZXR1cm4gIlTDuiIgfQoKZnVuIG1haW4oKSB7CiAgICB2YWwgb2ZmbGluZSA9IHRydWUKICAgIGlmICghb2ZmbGluZSAmJiBsb2FkVXNlcigpLmlzTm90RW1wdHkoKSkgcHJpbnRsbigiY8OzIHVzZXIiKSAgIC8vIOKchSBraMO0bmcgZ-G7jWkgc2VydmVyCiAgICBwcmludGxuKCItLS0iKQogICAgaWYgKCFvZmZsaW5lIGFuZCBsb2FkVXNlcigpLmlzTm90RW1wdHkoKSkgcHJpbnRsbigiY8OzIHVzZXIiKSAgLy8g4p2MIGBhbmRgIHbhuqtuIGfhu41pIHNlcnZlcgp9Cg) · Xem thêm: K027

### K050 · `fun sum(a, b) = { a + b }` không trả về số

`🟡 đã làm app` · `Kotlin`

**Gặp:** in ra `FileKt$$Lambda$1/...` (bản Kotlin cũ: `() -> kotlin.Int`) thay vì kết quả; hoặc lỗi kiểu ở chỗ gọi vì hàm trả về `() -> Int`.

**Vì sao:** sau dấu `=` là **biểu thức**. `{ a + b }` ở đó là một lambda, nên hàm trả về chính lambda chứ không chạy nó.
Thói quen từ JavaScript (`() => { ... }`) hay gây lỗi này.

**Sửa:** chọn một trong hai: `= a + b` (thân là biểu thức) hoặc `{ return a + b }` (thân là khối, không có `=`).

```kotlin
fun sum(a: Int, b: Int) = { a + b }        // ❌ trả về lambda
fun sum(a: Int, b: Int) = a + b            // ✅
fun sum(a: Int, b: Int): Int { return a + b } // ✅
```

[▶ Chạy thử](../playground/index.html#code=ZnVuIHN1bShhOiBJbnQsIGI6IEludCkgPSB7IGEgKyBiIH0gICAgIC8vIOKdjCB0cuG6oyB24buBIG3hu5l0IGxhbWJkYQpmdW4gc3VtT2soYTogSW50LCBiOiBJbnQpID0gYSArIGIgICAgICAgLy8g4pyFCgpmdW4gbWFpbigpIHsKICAgIHByaW50bG4oc3VtKDEsIDIpKSAgICAgICAgICAgICAgICAgICAvLyBGaWxlS3QkJExhbWJkYS4uLjogaW4gcmEgbeG7mXQgbGFtYmRhLCBraMO0bmcgcGjhuqNpIDMKICAgIHByaW50bG4oc3VtT2soMSwgMikpICAgICAgICAgICAgICAgICAvLyAzCn0K) · Xem thêm: [Hàm, lambda](05-functions.md)
