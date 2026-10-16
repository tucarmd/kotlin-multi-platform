# 18. Style, Java interop, tooling, phát hành (K851–K900)

Nhóm cuối gom những thứ nằm quanh code: Java nhìn code Kotlin ra sao, cảnh báo và công cụ kiểm tra hay bị lờ đi, và
chặng **phát hành**: R8, ký app, Google Play, App Store. Lỗi ở đây thường chỉ lộ ra ở bản release hoặc lúc nộp app, nên
đọc trước khi tới ngày phát hành.

### K851 · Java phải gọi `Config.Companion.load()` thay vì `Config.load()`

`🟢 mới học` · `Kotlin`

**Gặp:** code Java (hoặc thư viện đọc method qua reflection) không thấy `Config.load()`; phải viết
`Config.Companion.load()`.

**Vì sao:** `companion object` là một object thật, tên `Companion`. Hàm trong đó là method của object đó, không phải
method `static` của class ngoài.

**Sửa:** thêm `@JvmStatic` cho hàm Java cần gọi kiểu static. Code thuần Kotlin thì không cần.

```kotlin
class Config {
    companion object {
        fun plain() = "…"              // Java: Config.Companion.plain()
        @JvmStatic fun load() = "…"    // ✅ Java: Config.load()
    }
}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGphdmEubGFuZy5yZWZsZWN0Lk1vZGlmaWVyCgovLyBKYXZhIG5ow6xuIGNsYXNzIEtvdGxpbiB0aOG6vyBuw6BvOiBpbiByYSBtZXRob2Qgc3RhdGljIG3DoCBKYXZhIGfhu41pIMSRxrDhu6NjIGLhurFuZyBDb25maWcueHh4KCkKY2xhc3MgQ29uZmlnIHsKICAgIGNvbXBhbmlvbiBvYmplY3QgewogICAgICAgIGZ1biBwbGFpbigpID0gInBsYWluIiAgICAgICAgICAgIC8vIOKdjCBKYXZhIHBo4bqjaSBn4buNaSBDb25maWcuQ29tcGFuaW9uLnBsYWluKCkKICAgICAgICBASnZtU3RhdGljIGZ1biB3aXRoU3RhdGljKCkgPSAic3RhdGljIiAvLyDinIUgSmF2YSBn4buNaSBDb25maWcud2l0aFN0YXRpYygpCiAgICB9Cn0KCmZ1biBtYWluKCkgewogICAgdmFsIHN0YXRpY3MgPSBDb25maWc6OmNsYXNzLmphdmEuZGVjbGFyZWRNZXRob2RzCiAgICAgICAgLmZpbHRlciB7IE1vZGlmaWVyLmlzU3RhdGljKGl0Lm1vZGlmaWVycykgfQogICAgICAgIC5tYXAgeyBpdC5uYW1lIH0KICAgIHByaW50bG4oIk1ldGhvZCBzdGF0aWMgdHLDqm4gY2xhc3MgQ29uZmlnOiAkc3RhdGljcyIpCiAgICB2YWwgb25Db21wYW5pb24gPSBDb25maWcuQ29tcGFuaW9uOjpjbGFzcy5qYXZhLmRlY2xhcmVkTWV0aG9kcy5tYXAgeyBpdC5uYW1lIH0uc29ydGVkKCkKICAgIHByaW50bG4oIk1ldGhvZCB0csOqbiBDb25maWcuQ29tcGFuaW9uOiAgICAkb25Db21wYW5pb24iKQp9Cg) · Xem thêm: K857

### K852 · Java thấy `getTIMEOUT()` thay vì hằng `TIMEOUT`

`🟢 mới học` · `Kotlin`

**Gặp:** Java viết `Api.TIMEOUT` báo không có; chỉ có `Api.Companion.getTIMEOUT()`. Annotation Java cần hằng số
(`@Something(timeout = Api.TIMEOUT)`) không biên dịch.

**Vì sao:** `val` trong Kotlin là property: field `private` + getter. Java chỉ thấy getter.

**Sửa:** giá trị nguyên thuỷ / `String` biết lúc biên dịch → `const val` (Java thấy hằng `static final`). Object khác cần lộ
field → `@JvmField`.

```kotlin
companion object {
    val TIMEOUT = 30                     // ❌ Java: Api.Companion.getTIMEOUT()
    const val VERSION = 2                // ✅ Java: Api.VERSION
    @JvmField val DEFAULT_HEADERS = listOf("Accept") // ✅ Java: Api.DEFAULT_HEADERS
}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGphdmEubGFuZy5yZWZsZWN0Lk1vZGlmaWVyCgovLyBKYXZhIHRo4bqleSBnw6w6IGZpZWxkIHB1YmxpYyBoYXkgY2jhu4kgZ2V0dGVyPwpjbGFzcyBBcGkgewogICAgY29tcGFuaW9uIG9iamVjdCB7CiAgICAgICAgdmFsIFRJTUVPVVQgPSAzMCAgICAgICAgICAgICAgICAgIC8vIOKdjCBKYXZhOiBBcGkuQ29tcGFuaW9uLmdldFRJTUVPVVQoKQogICAgICAgIGNvbnN0IHZhbCBWRVJTSU9OID0gMiAgICAgICAgICAgICAvLyDinIUgSmF2YTogQXBpLlZFUlNJT04gKGjhurFuZyBz4buRIGNvbXBpbGUtdGltZSkKICAgICAgICBASnZtRmllbGQgdmFsIEJBU0VfVVJMID0gImh0dHBzOi8vZXhhbXBsZS5jb20iIC8vIOKchSBKYXZhOiBBcGkuQkFTRV9VUkwKICAgIH0KfQoKZnVuIG1haW4oKSB7CiAgICBBcGk6OmNsYXNzLmphdmEuZGVjbGFyZWRGaWVsZHMuZm9yRWFjaCB7CiAgICAgICAgdmFsIHZpcyA9IGlmIChNb2RpZmllci5pc1B1YmxpYyhpdC5tb2RpZmllcnMpKSAicHVibGljIiBlbHNlICJwcml2YXRlIgogICAgICAgIHByaW50bG4oImZpZWxkICR7aXQubmFtZX06ICR2aXMiKQogICAgfQogICAgcHJpbnRsbigiZ2V0dGVyIHRyw6puIENvbXBhbmlvbjogIiArCiAgICAgICAgQXBpLkNvbXBhbmlvbjo6Y2xhc3MuamF2YS5kZWNsYXJlZE1ldGhvZHMubWFwIHsgaXQubmFtZSB9LmZpbHRlciB7IGl0LnN0YXJ0c1dpdGgoImdldCIpIH0pCn0K)

### K853 · Java gọi hàm có tham số mặc định phải truyền đủ mọi tham số

`🟢 mới học` · `Kotlin`

**Gặp:** Java báo `show(String,boolean,int) cannot be applied to (String)`; custom `View` viết bằng Kotlin crash khi inflate
từ XML với `NoSuchMethodException: <init> [class android.content.Context, interface android.util.AttributeSet]`.

**Vì sao:** Java không có tham số mặc định. Kotlin chỉ sinh **một** method đủ tham số (cộng một method `$default` Java
không gọi được).

**Sửa:** `@JvmOverloads` sinh thêm các bản rút gọn, bỏ dần tham số mặc định từ cuối lên.

```kotlin
class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr)   // ✅ có đủ constructor 1, 2, 3 tham số
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSmF2YSBraMO0bmcgaGnhu4N1IHRoYW0gc-G7kSBt4bq3YyDEkeG7i25oOiBjaOG7iSB0aOG6pXkgTeG7mFQgaMOgbSDEkeG7pyB0aGFtIHPhu5EsIHRy4burIGtoaSBjw7MgQEp2bU92ZXJsb2Fkcy4KY2xhc3MgVG9hc3RlciB7CiAgICBmdW4gc2hvdyh0ZXh0OiBTdHJpbmcsIGxvbmc6IEJvb2xlYW4gPSBmYWxzZSwgZ3Jhdml0eTogSW50ID0gMCkgPSBVbml0CiAgICBASnZtT3ZlcmxvYWRzCiAgICBmdW4gc2hvd0phdmEodGV4dDogU3RyaW5nLCBsb25nOiBCb29sZWFuID0gZmFsc2UsIGdyYXZpdHk6IEludCA9IDApID0gVW5pdAp9CgpmdW4gbWFpbigpIHsKICAgIFRvYXN0ZXI6OmNsYXNzLmphdmEuZGVjbGFyZWRNZXRob2RzCiAgICAgICAgLmZpbHRlciB7ICFpdC5pc1N5bnRoZXRpYyB9CiAgICAgICAgLnNvcnRlZEJ5IHsgaXQubmFtZSArIGl0LnBhcmFtZXRlckNvdW50IH0KICAgICAgICAuZm9yRWFjaCB7IG0gLT4gcHJpbnRsbigiJHttLm5hbWV9KCR7bS5wYXJhbWV0ZXJUeXBlcy5qb2luVG9TdHJpbmcgeyBpdC5zaW1wbGVOYW1lIH19KSIpIH0KfQo)

### K854 · Hàm top-level gọi từ Java qua class `UtilsKt`

`🟢 mới học` · `Kotlin`

**Gặp:** Java phải viết `StringUtilsKt.slugify(...)`; đổi tên file `.kt` là mọi chỗ gọi từ Java hỏng.

**Vì sao:** JVM không có hàm đứng ngoài class. Kotlin gom hàm top-level của file `StringUtils.kt` vào class
`StringUtilsKt`.

**Sửa:** đặt tên class cho Java bằng `@file:JvmName` ở dòng đầu file (trước `package`).

```kotlin
@file:JvmName("TextUtils")
package dev.app.util

fun slugify(s: String) = s.lowercase().replace(' ', '-')   // Java: TextUtils.slugify(...)
```

[▶ Chạy thử](../playground/index.html#code=QGZpbGU6SnZtTmFtZSgiVGV4dFV0aWxzIikKCi8vIEjDoG0gdG9wLWxldmVsIG7hurFtIHRyb25nIGNsYXNzIHTDqm4gPFTDqm5GaWxlPkt0LiBAZmlsZTpKdm1OYW1lIMSR4buVaSB0w6puIMSRw7MgY2hvIEphdmEuCmZ1biBzbHVnaWZ5KHM6IFN0cmluZykgPSBzLmxvd2VyY2FzZSgpLnJlcGxhY2UoJyAnLCAnLScpCgpmdW4gbWFpbigpIHsKICAgIHZhbCBjbGFzc05hbWUgPSBUaHJlYWQuY3VycmVudFRocmVhZCgpLnN0YWNrVHJhY2VbMV0uY2xhc3NOYW1lCiAgICBwcmludGxuKCJKYXZhIGfhu41pIGjDoG0gbsOgeSBxdWEgY2xhc3M6ICRjbGFzc05hbWUiKSAgIC8vIFRleHRVdGlscyB0aGF5IHbDrCBGaWxlS3QKICAgIHByaW50bG4oc2x1Z2lmeSgiVHJpcCBMb2ciKSkKfQo)

### K855 · Java không bắt được `IOException` do hàm Kotlin ném

`🟡 đã làm app` · `Kotlin`

**Gặp:** Java viết `try { store.load(); } catch (IOException e)` thì báo
`exception IOException is never thrown in body of corresponding try statement`; bỏ `catch` thì app crash.

**Vì sao:** Kotlin không có checked exception, nên method sinh ra không khai `throws`. Java tin vào khai báo đó.

**Sửa:** thêm `@Throws(IOException::class)` cho hàm Java gọi. (Với Swift/Objective-C, `@Throws` cũng là cách để lỗi thành
`throws` thay vì crash.)

```kotlin
@Throws(IOException::class)
fun load(): String = file.readText()   // ✅ Java: bắt buộc try/catch IOException
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGphdmEuaW8uSU9FeGNlcHRpb24KCi8vIEtvdGxpbiBraMO0bmcgY8OzIGNoZWNrZWQgZXhjZXB0aW9uLiBKYXZhIGNo4buJIGJp4bq_dCBow6BtICJ0aHJvd3MgSU9FeGNlcHRpb24iIGtoaSBjw7MgQFRocm93cy4KY2xhc3MgU3RvcmUgewogICAgZnVuIGxvYWQoKTogU3RyaW5nID0gdGhyb3cgSU9FeGNlcHRpb24oImRpc2siKSAgICAgICAgICAgIC8vIOKdjCBKYXZhOiBjYXRjaCAoSU9FeGNlcHRpb24pIGLDoW8gbOG7l2kgYmnDqm4gZOG7i2NoCiAgICBAVGhyb3dzKElPRXhjZXB0aW9uOjpjbGFzcykKICAgIGZ1biBsb2FkSmF2YSgpOiBTdHJpbmcgPSB0aHJvdyBJT0V4Y2VwdGlvbigiZGlzayIpICAgICAgICAvLyDinIUgSmF2YTogYuG6r3QgYnXhu5ljIHRyeS9jYXRjaCBuaMawIGjDoG0gSmF2YQp9CgpmdW4gbWFpbigpIHsKICAgIFN0b3JlOjpjbGFzcy5qYXZhLmRlY2xhcmVkTWV0aG9kcy5zb3J0ZWRCeSB7IGl0Lm5hbWUgfS5mb3JFYWNoIHsgbSAtPgogICAgICAgIHByaW50bG4oIiR7bS5uYW1lfSB0aHJvd3MgJHttLmV4Y2VwdGlvblR5cGVzLm1hcCB7IGl0LnNpbXBsZU5hbWUgfX0iKQogICAgfQp9Cg)

### K856 · Java tìm `getIsOnline()` mà không thấy

`🟢 mới học` · `Kotlin`

**Gặp:** Java, Data Binding hoặc thư viện serialization dựa trên getter không thấy property `isOnline`.

**Vì sao:** property tên bắt đầu bằng `is` được đối xử riêng: getter là `isOnline()`, setter là `setOnline()`. Các tên
khác (`hasGps`, `canEdit`) vẫn theo kiểu thường `getHasGps()`.

**Sửa:** gọi đúng tên `isOnline()` / `setOnline(...)` từ Java. Muốn tên khác thì đặt `@get:JvmName("…")`.

```kotlin
var isOnline = false   // Java: isOnline() / setOnline(boolean)
var hasGps = false     // Java: getHasGps() / setHasGps(boolean)
```

[▶ Chạy thử](../playground/index.html#code=Ly8gVMOqbiBnZXR0ZXIvc2V0dGVyIEphdmEgc2luaCB04burIHByb3BlcnR5IEtvdGxpbgpjbGFzcyBEZXZpY2UgewogICAgdmFyIG5hbWU6IFN0cmluZyA9ICIiICAgICAgICAgICAgLy8gZ2V0TmFtZSgpIC8gc2V0TmFtZSgpCiAgICB2YXIgaXNPbmxpbmU6IEJvb2xlYW4gPSBmYWxzZSAgICAvLyBpc09ubGluZSgpIC8gc2V0T25saW5lKCkgIOKGkCBraMO0bmcgcGjhuqNpIGdldElzT25saW5lKCkKICAgIHZhciBoYXNHcHM6IEJvb2xlYW4gPSBmYWxzZSAgICAgIC8vIGdldEhhc0dwcygpIC8gc2V0SGFzR3BzKCkg4oaQIGNo4buJIHRp4buBbiB04buRICJpcyIgxJHGsOG7o2MgxJHhu5FpIHjhu60gcmnDqm5nCiAgICB2YWwgVVJMOiBTdHJpbmcgPSAiIiAgICAgICAgICAgICAvLyBnZXRVUkwoKQp9CgpmdW4gbWFpbigpIHsKICAgIERldmljZTo6Y2xhc3MuamF2YS5kZWNsYXJlZE1ldGhvZHMubWFwIHsgaXQubmFtZSB9LnNvcnRlZCgpLmZvckVhY2goOjpwcmludGxuKQp9Cg)

### K857 · Java gọi hàm của `object` phải qua `INSTANCE`

`🟢 mới học` · `Kotlin`

**Gặp:** Java viết `Analytics.track("x")` báo không có method static; phải viết `Analytics.INSTANCE.track("x")`.

**Vì sao:** `object` là singleton: một class có field tĩnh `INSTANCE` giữ object duy nhất. Hàm là method của object đó.

**Sửa:** gọi qua `INSTANCE`, hoặc thêm `@JvmStatic` cho hàm Java hay gọi.

```kotlin
object Analytics {
    fun track(event: String) = Unit        // Java: Analytics.INSTANCE.track("x")
    @JvmStatic fun flush() = Unit          // Java: Analytics.flush()
}
```

[▶ Chạy thử](../playground/index.html#code=Ly8gb2JqZWN0IHRyb25nIEtvdGxpbiBsw6Agc2luZ2xldG9uLiBKYXZhIGfhu41pIHF1YSBmaWVsZCB0xKluaCBJTlNUQU5DRSwgdHLhu6sgaMOgbSBjw7MgQEp2bVN0YXRpYy4Kb2JqZWN0IEFuYWx5dGljcyB7CiAgICBmdW4gdHJhY2soZXZlbnQ6IFN0cmluZykgPSAidHJhY2sgJGV2ZW50IgogICAgQEp2bVN0YXRpYyBmdW4gZmx1c2goKSA9ICJmbHVzaCIKfQoKZnVuIG1haW4oKSB7CiAgICB2YWwgY2xzID0gQW5hbHl0aWNzOjpjbGFzcy5qYXZhCiAgICBwcmludGxuKCJmaWVsZDogIiArIGNscy5kZWNsYXJlZEZpZWxkcy5tYXAgeyBpdC5uYW1lIH0pCiAgICBjbHMuZGVjbGFyZWRNZXRob2RzLnNvcnRlZEJ5IHsgaXQubmFtZSB9LmZvckVhY2ggewogICAgICAgIHZhbCBzdGF0aWMgPSBqYXZhLmxhbmcucmVmbGVjdC5Nb2RpZmllci5pc1N0YXRpYyhpdC5tb2RpZmllcnMpCiAgICAgICAgcHJpbnRsbigiJHtpdC5uYW1lfTogJHtpZiAoc3RhdGljKSAiQW5hbHl0aWNzLiR7aXQubmFtZX0oKSIgZWxzZSAiQW5hbHl0aWNzLklOU1RBTkNFLiR7aXQubmFtZX0oKSJ9IikKICAgIH0KfQo) · Xem thêm: K851

### K858 · `Interface 'interface OnTap : Any' does not have constructors`

`🟢 mới học` · `Kotlin`

**Gặp:** viết `OnTap { … }` hoặc truyền lambda vào chỗ cần `OnTap` thì báo lỗi trên, dù với interface Java (`Runnable`,
`View.OnClickListener`) thì viết được.

**Vì sao:** chuyển lambda thành interface (SAM conversion) chỉ tự có với interface Java. Interface Kotlin phải khai là
`fun interface` (đúng một hàm abstract).

**Sửa:** thêm `fun` trước `interface`, hoặc dùng kiểu hàm `(Int) -> Unit` nếu không cần tên interface.

```kotlin
interface OnTap { fun onTap(x: Int) }        // ❌ OnTap { } không biên dịch
fun interface OnTap { fun onTap(x: Int) }    // ✅ val t = OnTap { println(it) }
```

[▶ Chạy thử](../playground/index.html#code=Ly8gaW50ZXJmYWNlIHRoxrDhu51uZyBj4bunYSBLb3RsaW4ga2jDtG5nIG5o4bqtbiBsYW1iZGEuIFRow6ptIGBmdW5gIMSR4buDIGPDsyBTQU0gY29udmVyc2lvbi4KaW50ZXJmYWNlIE9uVGFwIHsgZnVuIG9uVGFwKHg6IEludCkgfQpmdW4gaW50ZXJmYWNlIE9uVGFwRnVuIHsgZnVuIG9uVGFwKHg6IEludCkgfQoKZnVuIG1haW4oKSB7CiAgICAvLyB2YWwgYSA9IE9uVGFwIHsgcHJpbnRsbihpdCkgfSAgICAgICAgLy8g4p2MIEludGVyZmFjZSAnaW50ZXJmYWNlIE9uVGFwIDogQW55JyBkb2VzIG5vdCBoYXZlIGNvbnN0cnVjdG9ycwogICAgdmFsIGEgPSBvYmplY3QgOiBPblRhcCB7IG92ZXJyaWRlIGZ1biBvblRhcCh4OiBJbnQpID0gcHJpbnRsbigib2JqZWN0OiAkeCIpIH0KICAgIHZhbCBiID0gT25UYXBGdW4geyBwcmludGxuKCJsYW1iZGE6ICRpdCIpIH0gICAvLyDinIUgZnVuIGludGVyZmFjZQogICAgYS5vblRhcCgxKQogICAgYi5vblRhcCgyKQp9Cg)

### K859 · `Platform declaration clash: The following declarations have the same JVM signature`

`🟡 đã làm app` · `Kotlin`

**Gặp:** hai hàm cùng tên, khác kiểu tham số (`List<Int>` và `List<List<Int>>`) thì báo
`Platform declaration clash: The following declarations have the same JVM signature (decode(Ljava/util/List;)I)`.

**Vì sao:** generic bị xoá khi chạy trên JVM (type erasure): cả hai đều thành `decode(java.util.List)`. Với Kotlin
chúng khác nhau, với JVM thì trùng.

**Sửa:** đặt tên JVM khác bằng `@JvmName` (Kotlin vẫn gọi chung một tên), hoặc đặt hẳn tên khác cho rõ nghĩa.

```kotlin
fun decode(bytes: List<Int>): Int = bytes.sum()
@JvmName("decodeFrames")
fun decode(frames: List<List<Int>>): Int = frames.sumOf { it.sum() }   // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gTGlzdDxJbnQ-IHbDoCBMaXN0PExpc3Q8SW50Pj4gxJHhu4F1IHRow6BuaCBqYXZhLnV0aWwuTGlzdCBzYXUgdHlwZSBlcmFzdXJlLgovLyBIYWkgaMOgbSBjw7luZyB0w6puLCBjw7luZyAiaMOsbmggZOG6oW5nIiB0csOqbiBKVk0g4oaSICJQbGF0Zm9ybSBkZWNsYXJhdGlvbiBjbGFzaCIuIEBKdm1OYW1lIHTDoWNoIHTDqm4gSlZNLgpmdW4gZGVjb2RlKGJ5dGVzOiBMaXN0PEludD4pOiBJbnQgPSBieXRlcy5zdW0oKQoKQEp2bU5hbWUoImRlY29kZUZyYW1lcyIpCmZ1biBkZWNvZGUoZnJhbWVzOiBMaXN0PExpc3Q8SW50Pj4pOiBJbnQgPSBmcmFtZXMuc3VtT2YgeyBpdC5zdW0oKSB9CgpmdW4gbWFpbigpIHsKICAgIHByaW50bG4oZGVjb2RlKGxpc3RPZigxLCAyLCAzKSkpCiAgICBwcmludGxuKGRlY29kZShsaXN0T2YobGlzdE9mKDEsIDIpLCBsaXN0T2YoMykpKSkKfQo) · **Gặp ở:** [`apps/ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/rules/Pid.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/rules/Pid.kt) (chọn cách đặt tên khác: `decodePidAnswer`)

### K860 · Hàm `internal` hiện ra trong Java với tên lạ `cacheKey$app_debug`

`🟡 đã làm app` · `Kotlin`

**Gặp:** từ Java (hoặc stack trace) thấy method tên `cacheKey$app_debug`; Java gọi được dù Kotlin coi là `internal`.

**Vì sao:** Java không có `internal`. Kotlin để method `public` trên JVM nhưng gắn tên module vào để khó gọi nhầm. Đó
không phải bảo vệ thật.

**Sửa:** đừng dựa vào `internal` để giấu code khỏi Java trong cùng app. Cần giấu thật thì dùng `private`, hoặc tách
module. Gặp tên `$…` trong stack trace thì biết đó là hàm `internal`.

[▶ Chạy thử](../playground/index.html#code=Ly8gaW50ZXJuYWwga2jDtG5nIGPDsyDhu58gSmF2YS4gS290bGluIMSR4buDIG7DsyBwdWJsaWMgdHLDqm4gSlZNIG5oxrBuZyAieMOhbyIgdMOqbiDEkeG7gyBKYXZhIGtow7MgZ-G7jWkgbmjhuqdtLgpjbGFzcyBSZXBvIHsKICAgIGZ1biBsb2FkKCkgPSAicHVibGljIgogICAgaW50ZXJuYWwgZnVuIGNhY2hlS2V5KCkgPSAiaW50ZXJuYWwiCn0KCmZ1biBtYWluKCkgewogICAgUmVwbzo6Y2xhc3MuamF2YS5kZWNsYXJlZE1ldGhvZHMubWFwIHsgaXQubmFtZSB9LnNvcnRlZCgpLmZvckVhY2goOjpwcmludGxuKQp9Cg)

### K861 · Java truyền lambda cho hàm Kotlin nhận `(String) -> Unit`

`🟡 đã làm app` · `Kotlin`

**Gặp:** Java viết `api.fetch(s -> log(s));` báo `bad return type in lambda expression: void cannot be converted to Unit`.

**Vì sao:** `(String) -> Unit` trên JVM là `Function1<String, Unit>`. `Unit` là một object thật, nên lambda Java phải trả
về nó.

**Sửa:** bên Java thêm `return Unit.INSTANCE;`. Nếu API chủ yếu cho Java gọi thì khai `fun interface` trả về `Unit` (Java
thấy `void`).

```java
api.fetch(s -> { log(s); return Unit.INSTANCE; });   // ✅
```

### K862 · Dagger/Hilt báo thiếu binding cho `List<? extends Plugin>`

`🔴 khó, hiếm nhưng đau` · `Kotlin` · `Android`

**Gặp:** inject `Set<Plugin>`/`List<Plugin>` trong constructor Kotlin thì Dagger báo
`[Dagger/MissingBinding] java.util.Set<? extends Plugin> cannot be provided`, dù đã có `@IntoSet`.

**Vì sao:** `List<out E>` có variance khai báo sẵn. Với `E` là class `open`/interface, Kotlin cho Java thấy tham số là
`List<? extends Plugin>`, còn Dagger tìm đúng `List<Plugin>`.

**Sửa:** thêm `@JvmSuppressWildcards` vào kiểu (hoặc cả class).

```kotlin
class Registry @Inject constructor(
    plugins: Set<@JvmSuppressWildcards Plugin>,   // ✅ Java thấy Set<Plugin>
)
```

[▶ Chạy thử](../playground/index.html#code=Ly8gTGlzdDxvdXQgRT4gY8OzIHZhcmlhbmNlIGtoYWkgYsOhbyBz4bq1bi4gS2hpIEUgbMOgIGNsYXNzICJvcGVuIiwgS290bGluIGNobyBKYXZhIHRo4bqleSBMaXN0PD8gZXh0ZW5kcyBFPi4Kb3BlbiBjbGFzcyBQbHVnaW4KaW50ZXJmYWNlIFJlZ2lzdHJ5IHsKICAgIGZ1biBpbnN0YWxsKHBsdWdpbnM6IExpc3Q8UGx1Z2luPikgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBKYXZhL0RhZ2dlciB0aOG6pXkgTGlzdDw_IGV4dGVuZHMgUGx1Z2luPgogICAgZnVuIGluc3RhbGxFeGFjdChwbHVnaW5zOiBMaXN0PEBKdm1TdXBwcmVzc1dpbGRjYXJkcyBQbHVnaW4-KSAvLyDinIUgSmF2YSB0aOG6pXkgTGlzdDxQbHVnaW4-Cn0KCmZ1biBtYWluKCkgewogICAgUmVnaXN0cnk6OmNsYXNzLmphdmEuZGVjbGFyZWRNZXRob2RzLnNvcnRlZEJ5IHsgaXQubmFtZSB9LmZvckVhY2ggewogICAgICAgIHByaW50bG4oIiR7aXQubmFtZX06ICR7aXQuZ2VuZXJpY1BhcmFtZXRlclR5cGVzLmZpcnN0KCkudHlwZU5hbWV9IikKICAgIH0KfQo)

### K863 · Mỗi người format một kiểu, diff toàn khoảng trắng

`🟢 mới học` · `Kotlin`

**Gặp:** pull request đổi 300 dòng mà chỉ 5 dòng là logic; *Reformat Code* trên máy này ra khác máy kia.

**Vì sao:** IDE dùng style riêng của từng máy nếu project không ghi rõ. Thụt lề, import, dấu phẩy cuối khác nhau là diff
lớn.

**Sửa:** ghi `kotlin.code.style=official` vào `gradle.properties` và commit một `.editorconfig`, rồi format cả project
một lần trong một commit riêng.

```properties
# gradle.properties
kotlin.code.style=official
```

Xem thêm: [Quy ước code Kotlin](../reference/kotlin-conventions.md)

### K864 · Lờ cảnh báo vàng của compiler

`🟢 mới học` · `Kotlin`

**Gặp:** build có 80 warning nên không ai đọc; bug thật nằm ngay trong đó: `Variable 'result' is never used`,
`Condition is always 'true'`, `Unchecked cast`, `'xxx' is deprecated`.

**Vì sao:** warning là compiler đã phát hiện điều đáng ngờ nhưng không đủ chắc để dừng build. Khi chúng chất đống, cái
quan trọng chìm giữa cái vô hại.

**Sửa:** giữ số warning bằng 0: sửa ngay khi mới xuất hiện. Muốn ép cả nhóm thì bật biến warning thành lỗi (thường chỉ
trên CI).

```kotlin
kotlin {
    compilerOptions { allWarningsAsErrors.set(true) }
}
```

Xem thêm: K865, K866

### K865 · `@Suppress("UNCHECKED_CAST")` khắp nơi cho hết vàng

`🟡 đã làm app` · `Kotlin`

**Gặp:** hết warning nhưng app crash với `ClassCastException` ở chỗ đã `@Suppress`.

**Vì sao:** `@Suppress` chỉ tắt thông báo, không sửa vấn đề. Unchecked cast nghĩa là kiểu không được kiểm khi chạy;
sai thì lỗi nổ ở chỗ khác, xa chỗ cast.

**Sửa:** chỉ suppress khi chứng minh được là đúng, ở phạm vi nhỏ nhất (một biểu thức, không phải cả file), kèm comment vì
sao. Thường có cách không cần cast: `filterIsInstance<T>()`, `as? T`, hàm `reified`.

```kotlin
@Suppress("UNCHECKED_CAST") val ids = items as List<Long>   // ❌ không kiểm phần tử
val ids = items.filterIsInstance<Long>()                    // ✅
```

### K866 · Để API deprecated tới khi nó bị xoá

`🟡 đã làm app` · `Kotlin` · `Gradle`

**Gặp:** nâng Kotlin/AGP/thư viện thì cả loạt lỗi `Unresolved reference` cùng lúc; trước đó chúng là warning
`'xxx' is deprecated` suốt mấy tháng.

**Vì sao:** deprecation đi theo 3 bước: `WARNING` → `ERROR` → xoá hẳn. Mỗi bước là một bản phát hành. Lờ đi ở bước đầu
thì phải sửa tất cả cùng lúc ở bước cuối.

**Sửa:** sửa deprecation ngay khi nâng phiên bản. IntelliJ thường có quick-fix theo `ReplaceWith` (Alt+Enter → *Replace
with …*).

Xem thêm: K394

### K867 · `import kotlinx.coroutines.*` và lỗi `no-wildcard-imports`

`🟢 mới học` · `Kotlin`

**Gặp:** ktlint báo `Wildcard import (standard:no-wildcard-imports)`; hoặc nâng thư viện thì một tên trùng nhau chen vào
qua `*` và gọi nhầm hàm.

**Vì sao:** import `*` kéo mọi tên của package vào file. Style chính thức của Kotlin (và ktlint) cấm, trừ vài package như
`java.util.*` tuỳ cấu hình.

**Sửa:** để IDE import từng tên (*Settings → Editor → Code Style → Kotlin → Imports*: chọn *Use single name import*), rồi
*Optimize Imports* (Ctrl+Alt+O).

### K868 · Có ktlint/detekt mà chỉ chạy trên máy một người

`🟡 đã làm app` · `Kotlin` · `Gradle`

**Gặp:** lỗi style lọt vào `main` liên tục; bật detekt lần đầu thì báo 600 vấn đề, không ai dám sửa.

**Vì sao:** công cụ không chạy tự động thì không ai chạy. Bật trên project cũ mà không có baseline thì quá nhiều lỗi cũ,
lỗi mới chìm theo.

**Sửa:** chạy trong CI ở mỗi pull request (`./gradlew ktlintCheck detekt`). Project cũ thì tạo baseline
(`./gradlew detektBaseline`) để chỉ chặn lỗi **mới**, rồi dọn dần lỗi cũ.

### K869 · ktlint bắt đổi tên hàm `@Composable` thành chữ thường

`🟢 mới học` · `Compose`

**Gặp:** `Function name should start with a lowercase letter (except factory methods) and use camel case` cho
`fun NoteList()`.

**Vì sao:** quy ước Kotlin là hàm viết thường; quy ước Compose là hàm `@Composable` trả `Unit` viết hoa như tên class.
ktlint mặc định không biết ngoại lệ đó.

**Sửa:** đừng đổi tên hàm Compose. Báo ktlint bỏ qua hàm có annotation `Composable` trong `.editorconfig`.

```ini
[*.{kt,kts}]
ktlint_function_naming_ignore_when_annotated_with = Composable
```

### K870 · `@SuppressLint("MissingPermission")` để hết đỏ

`🟡 đã làm app` · `Android`

**Gặp:** Lint báo `Call requires permission which may be rejected by user`; thêm `@SuppressLint` cho hết, rồi app crash
`SecurityException` trên máy người dùng từ chối quyền.

**Vì sao:** Lint không biết bạn đã kiểm quyền ở chỗ khác. Suppress là nhận trách nhiệm kiểm quyền thay nó; không kiểm
thì lỗi chạy thật vẫn còn.

**Sửa:** chỉ suppress khi ngay trước lời gọi đã kiểm `checkSelfPermission`, và vẫn bắt `SecurityException` (người dùng
có thể thu hồi quyền giữa lúc kiểm và lúc gọi). Ghi comment chỉ chỗ đã kiểm.

```kotlin
@SuppressLint("MissingPermission") // checked by hasPermission() right before requestLocationUpdates
override fun points() = callbackFlow {
    if (!hasPermission()) { close(PermissionDenied); return@callbackFlow }
    try { manager.requestLocationUpdates(/* … */) } catch (_: SecurityException) { close(PermissionDenied) }
    // …
}
```

**Gặp ở:** [`apps/TripLog/shared/src/androidMain/kotlin/dev/triplog/platform/AndroidLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/androidMain/kotlin/dev/triplog/platform/AndroidLocationStream.kt) · Xem thêm: K529

### K871 · `Lint found fatal errors while assembling a release target`

`🟡 đã làm app` · `Android` · `Gradle`

**Gặp:** `assembleDebug` chạy được, `assembleRelease`/`bundleRelease` dừng ở task `lintVitalRelease` với câu trên.

**Vì sao:** bản release chạy một phần Lint (lỗi mức *fatal*: `NewApi` gọi API cao hơn `minSdk`, thiếu bản dịch bắt buộc,
`MissingPermission`…) và chặn build. Bản debug không chạy bước này.

**Sửa:** mở báo cáo trong `build/reports/` (đường dẫn in ngay dưới lỗi), sửa lỗi thật. Đừng tắt bằng
`checkReleaseBuilds = false`: chúng là crash đang chờ trên máy người dùng.

Xem thêm: K546

### K872 · Một file `Utils.kt` chứa 15 class, tên file không khớp nội dung

`🟢 mới học` · `Kotlin`

**Gặp:** không tìm ra class `TripStats` nằm ở file nào; `Utils.kt` dài 1.200 dòng, ai cũng sợ sửa.

**Vì sao:** Kotlin cho đặt nhiều class trong một file và tên file tuỳ ý. Không có quy ước thì project lớn lên rất khó lần.

**Sửa:** file chứa một class thì đặt tên đúng tên class (`TripStats.kt`). File gom hàm top-level hoặc vài class nhỏ liên
quan thì đặt theo chủ đề (`Geo.kt`, `Format.kt`), không đặt `Utils`/`Helpers`.

Xem thêm: [Quy ước code Kotlin](../reference/kotlin-conventions.md)

### K873 · `Visibility must be specified in explicit API mode`

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** bật `explicitApi()` cho module thư viện thì hàng trăm lỗi `Visibility must be specified in explicit API mode`
và `Return type must be specified in explicit API mode`.

**Vì sao:** mặc định mọi thứ trong Kotlin là `public`. Explicit API mode bắt khai rõ, để không vô tình lộ chi tiết nội bộ
ra API của thư viện (và ra header Objective-C của framework iOS).

**Sửa:** dùng cho module thư viện/`shared` muốn giữ API gọn; thêm `public`/`internal` và kiểu trả về cho từng khai báo.
Muốn bật dần thì dùng `explicitApiWarning()` trước.

```kotlin
kotlin { explicitApi() }
```

### K874 · `This declaration needs opt-in. Its usage must be marked with '@kotlin.time.ExperimentalTime'`

`🟢 mới học` · `Kotlin` · `Gradle`

**Gặp:** dùng API thử nghiệm (`kotlin.time.Clock`, hàm coroutines có `@ExperimentalCoroutinesApi`, API Compose mới) thì báo lỗi hoặc cảnh báo
opt-in.

**Vì sao:** API thử nghiệm có thể đổi. Kotlin bắt bạn ghi rõ "tôi biết điều đó" ở chỗ dùng.

**Sửa:** thêm `@OptIn(ExperimentalTime::class)` ở hàm/file dùng. Dùng khắp module thì khai một lần trong Gradle, đừng
rải annotation khắp nơi.

```kotlin
kotlin {
    compilerOptions { optIn.add("kotlin.time.ExperimentalTime") }
}
```

**Gặp ở:** [`apps/DevicePulse/androidApp/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/DevicePulse/androidApp/build.gradle.kts)

### K875 · Vẫn dùng `kapt` với Kotlin 2.x

`🟡 đã làm app` · `Gradle`

**Gặp:** build chậm ở task `kaptGenerateStubs…`; Kotlin 2.0 in `Kapt currently doesn't support language version 2.0+.
Falling back to 1.9.`

**Vì sao:** kapt sinh stub Java cho cả module rồi mới chạy annotation processor, chậm và chỉ dành cho JVM. kapt đã ở chế
độ bảo trì; KSP chạy thẳng trên code Kotlin và có cho KMP.

**Sửa:** chuyển thư viện hỗ trợ KSP (Room, Moshi, Hilt/Dagger…) sang `ksp(...)`; chỉ giữ kapt cho thư viện chưa có KSP.

```kotlin
plugins { alias(libs.plugins.ksp) }
dependencies {
    kapt(libs.room.compiler)   // ❌
    ksp(libs.room.compiler)    // ✅
}
```

### K876 · Bản release crash `ClassNotFoundException`, bản debug thì không

`🟡 đã làm app` · `Android`

**Gặp:** release crash với `ClassNotFoundException`, `NoSuchMethodException` hoặc `NoSuchFieldException` ở chỗ dùng
reflection (`Class.forName`, thư viện tạo object theo tên class).

**Vì sao:** `isMinifyEnabled = true` bật R8: xoá class/method không ai gọi trực tiếp và đổi tên phần còn lại. Code chỉ
gọi qua reflection thì R8 không thấy, nên xoá hoặc đổi tên.

**Sửa:** thêm luật giữ lại trong `proguard-rules.pro`, càng hẹp càng tốt. Kiểm lại trên bản release trước khi nộp, không
chỉ bản debug.

```proguard
-keep class dev.app.plugins.** { <init>(); }
```

Xem thêm: K877, K879

### K877 · `Missing classes detected while running R8`

`🟡 đã làm app` · `Android` · `Gradle`

**Gặp:** `bundleRelease` dừng với `ERROR: Missing classes detected while running R8. Please add the missing classes or
apply additional rules that are generated in …/build/outputs/mapping/release/missing_rules.txt.`

**Vì sao:** một thư viện tham chiếu class không có trong app (thường là phụ thuộc tuỳ chọn: SLF4J, Conscrypt, annotation
chỉ dùng lúc biên dịch). AGP 8 coi đó là lỗi thay vì cảnh báo.

**Sửa:** mở `missing_rules.txt`, xem từng class. Nếu đúng là tuỳ chọn thì chép các dòng `-dontwarn` tương ứng vào
`proguard-rules.pro`. Nếu là class thật sự cần thì thêm dependency còn thiếu.

### K878 · Release: `Serializer for class 'X' is not found`

`🔴 khó, hiếm nhưng đau` · `Android` · `KMP`

**Gặp:** debug parse JSON bình thường, release ném `SerializationException: Serializer for class 'X' is not found`.

**Vì sao:** kotlinx.serialization tìm serializer sinh sẵn của class; R8 có thể xoá hoặc đổi tên nó khi luật giữ lại không
khớp (thư viện bản cũ, class nằm trong module không bật plugin serialization, hoặc gọi qua generic/`KClass` ở runtime).

**Sửa:** nâng kotlinx.serialization (bản mới kèm sẵn luật R8), kiểm plugin `kotlinx-serialization` được bật ở **module
chứa class**, và ưu tiên gọi `serializer<T>()` với kiểu cụ thể. Còn lỗi thì thêm luật từ README của thư viện.

Xem thêm: K876

### K879 · JSON ra `{"a":…,"b":…}` ở bản release (Gson)

`🟡 đã làm app` · `Android`

**Gặp:** release gửi lên server body có key `a`, `b`, `c`; parse về thì mọi field `null`/0.

**Vì sao:** Gson đọc tên field qua reflection. R8 đổi tên field thành `a`, `b`, nên tên JSON đổi theo.

**Sửa:** thêm `@SerializedName("…")` cho mọi field (Gson bản mới tự giữ field có annotation này), hoặc `@Keep` class model.
Với code Kotlin mới, dùng kotlinx.serialization: tên được sinh lúc biên dịch, không phụ thuộc reflection.

```kotlin
data class TripDto(
    @SerializedName("distance_m") val distanceMeters: Double,   // ✅ tên không đổi theo R8
)
```

### K880 · `Log.d` vẫn chạy trong bản release

`🟢 mới học` · `Android`

**Gặp:** Logcat của bản release vẫn in dữ liệu người dùng, token, tọa độ; app chậm vì log trong vòng lặp.

**Vì sao:** `Log.d` không tự tắt ở release. Chỉ R8 mới bỏ được lời gọi, và chỉ khi có luật bảo nó.

**Sửa:** dùng thư viện log có mức (Kermit, Timber) và chỉ gắn writer khi `BuildConfig.DEBUG`. Có thể thêm luật R8 bỏ lời
gọi `Log.d`/`Log.v`; nhưng chuỗi đã dựng sẵn có thể vẫn còn, nên dùng log dạng lambda (K817).

```proguard
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
```

### K881 · Stack trace release toàn `a.b.c(Unknown Source)`

`🟢 mới học` · `Android`

**Gặp:** Play Console / Crashlytics hiện crash với tên class `a.a.b`, method `c`, dòng `Unknown Source`.

**Vì sao:** R8 đổi tên để thu nhỏ app. Bảng đổi tên nằm trong `build/outputs/mapping/release/mapping.txt`, khác nhau ở
mỗi lần build.

**Sửa:** lưu `mapping.txt` của **mỗi** bản phát hành. Upload lên Play Console (*App bundle explorer → Downloads →
ReTrace mapping file*), hoặc để plugin Crashlytics/Sentry tự upload. Giải mã tay bằng `retrace` trong Android SDK.

### K882 · `Removing unused resources requires unused code shrinking to be turned on`

`🟢 mới học` · `Android` · `Gradle`

**Gặp:** bật `isShrinkResources = true` thì Gradle báo lỗi trên.

**Vì sao:** resource được coi là "không dùng" khi không còn code nào tham chiếu. Phải bật thu code (R8) thì mới biết code
nào còn.

**Sửa:** bật cả hai cùng lúc trong build type `release`.

```kotlin
buildTypes {
    getByName("release") {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

**Gặp ở:** [`apps/DevicePulse/androidApp/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/DevicePulse/androidApp/build.gradle.kts) (release đang `isMinifyEnabled = false`)

### K883 · `INSTALL_FAILED_UPDATE_INCOMPATIBLE: … signatures do not match`

`🟢 mới học` · `Android`

**Gặp:** cài bản release đè lên bản debug: `INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package dev.xxx signatures do not
match newer version; ignoring!`

**Vì sao:** Android chỉ cho cập nhật app khi bản mới ký bằng **cùng** chứng chỉ. Debug ký bằng key debug của từng máy,
release ký bằng key riêng.

**Sửa:** gỡ bản cũ (`adb uninstall dev.xxx`) rồi cài lại. Muốn cài song song thì cho debug `applicationIdSuffix = ".debug"`.

### K884 · Mất file keystore

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** đổi máy hoặc hỏng ổ cứng, không còn file `.jks`/mật khẩu; Play từ chối bản mới vì sai chữ ký.

**Vì sao:** chữ ký là bằng chứng bản mới đến từ cùng tác giả. Không có **Play App Signing**, mất key là không bao giờ cập
nhật được app nữa.

**Sửa:** bật Play App Signing (mặc định cho app mới): Google giữ key ký app, key bạn giữ chỉ là *upload key*; mất thì xin
đặt lại trong Play Console. Vẫn sao lưu keystore và mật khẩu ở nơi an toàn (trình quản lý mật khẩu, kho bí mật của CI).

### K885 · Commit keystore và mật khẩu vào git

`🟢 mới học` · `Android` · `Gradle`

**Gặp:** `build.gradle.kts` có `storePassword = "123456"`; file `release.jks` nằm trong repo.

**Vì sao:** ai đọc được repo (kể cả repo riêng tư bị lộ) là ký được app giả mạo mang tên bạn.

**Sửa:** đọc mật khẩu từ biến môi trường (hoặc `~/.gradle/gradle.properties` ngoài repo), thêm `*.jks`, `*.keystore` vào
`.gitignore`. Trên CI, keystore lưu dưới dạng secret (base64) và giải ra lúc build.

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")   // ✅ không nằm trong git
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

Xem thêm: [Bài 14: Build và phát hành](../lessons/14-build-publish.md)

### K886 · `Version code 1 has already been used. Try another version code.`

`🟢 mới học` · `Android`

**Gặp:** upload AAB lên Play Console bị từ chối với câu trên.

**Vì sao:** Play dùng `versionCode` (số nguyên) để biết bản nào mới hơn; mỗi bản upload phải lớn hơn mọi bản trước, kể cả
bản chỉ ở track nội bộ. `versionName` chỉ là chữ hiện cho người dùng.

**Sửa:** tăng `versionCode` mỗi lần upload. Trên CI có thể lấy từ số build (`GITHUB_RUN_NUMBER`) để khỏi quên.

```kotlin
defaultConfig {
    versionCode = 2          // ✅ tăng mỗi lần upload
    versionName = "1.0.1"
}
```

### K887 · Play: "Your app currently targets API level … and must target at least API level …"

`🟡 đã làm app` · `Android`

**Gặp:** Play Console chặn upload bản cập nhật vì `targetSdk` quá cũ.

**Vì sao:** mỗi năm (hạn thường là 31/8) Play nâng mức `targetSdk` tối thiểu, bám theo bản Android ra năm trước. Từ
31/8/2025, app mới và bản cập nhật phải target API 35.

**Sửa:** nâng `targetSdk` sớm, đọc *behavior changes* của bản đó và test lại các chỗ nhạy cảm: foreground service (K828),
quyền, edge-to-edge, broadcast receiver (K527).

Xem thêm: K364

### K888 · Upload file `.apk` lên Play cho app mới

`🟢 mới học` · `Android`

**Gặp:** Play Console không nhận APK cho app mới tạo, đòi Android App Bundle.

**Vì sao:** từ tháng 8/2021, app mới trên Play phải nộp bằng AAB. Play dùng bundle để sinh APK riêng cho từng máy (theo
ngôn ngữ, mật độ màn hình, CPU).

**Sửa:** build bằng `./gradlew :androidApp:bundleRelease`, lấy file trong `build/outputs/bundle/release/`. APK vẫn dùng
được để cài thử trực tiếp.

### K889 · "You uploaded a debuggable APK or Android App Bundle"

`🟢 mới học` · `Android`

**Gặp:** Play từ chối: *You uploaded a debuggable APK or Android App Bundle. For security reasons you need to disable
debugging before it can be published in Google Play.*

**Vì sao:** đã upload bản `debug`, hoặc build type release có `isDebuggable = true`, hoặc manifest có
`android:debuggable="true"`.

**Sửa:** upload output của `bundleRelease`; xoá `isDebuggable`/`android:debuggable` khỏi release. Để Gradle tự quản lý cờ
này, đừng ghi trong manifest.

### K890 · Play cảnh báo app chưa hỗ trợ trang bộ nhớ 16 KB

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** Play Console báo bundle không hỗ trợ *16 KB page sizes*; trên máy Android 15 cấu hình 16 KB, app crash khi nạp
thư viện native (`dlopen failed`).

**Vì sao:** một số máy Android 15+ dùng trang bộ nhớ 16 KB. File `.so` phải căn lề 16 KB. Từ 1/11/2025, app mới và bản cập
nhật target Android 15+ trên Play phải đáp ứng. Code Kotlin/Java thuần không bị ảnh hưởng; thủ phạm là thư viện có
`.so` (SQLite bản riêng, SQLCipher, xử lý ảnh, SDK quảng cáo…).

**Sửa:** dùng AGP 8.5.1+ (tự căn lề khi đóng gói), nâng các thư viện có `.so` lên bản đã hỗ trợ 16 KB, kiểm bằng
*APK Analyzer* hoặc emulator ảnh hệ thống 16 KB.

### K891 · App bị gỡ vì mục "Data safety" không khớp

`🟡 đã làm app` · `Android`

**Gặp:** Play gửi email yêu cầu sửa khai báo *Data safety*, hoặc bản cập nhật bị từ chối.

**Vì sao:** form *Data safety* phải liệt kê mọi dữ liệu app (và **SDK trong app**: analytics, crash, quảng cáo) thu thập
hay chia sẻ. Thêm Crashlytics, vị trí, mà không cập nhật form là lệch.

**Sửa:** mỗi lần thêm SDK hoặc quyền mới, xem lại form. Đọc tài liệu *Data safety* của từng SDK (đa số có sẵn bảng khai).

### K892 · Phát hành ngay cho 100 % người dùng

`🟡 đã làm app` · `Android`

**Gặp:** bản mới crash trên một dòng máy, cả triệu người nhận cùng lúc; gỡ bản không được, phải gấp rút ra bản sửa.

**Vì sao:** không có cách thu hồi bản đã cài. Lỗi chỉ có trên máy thật hiếm gặp không lộ ở test nội bộ.

**Sửa:** đi qua track *Internal testing* / *Closed testing* trước, rồi *staged rollout* (ví dụ 5 % → 20 % → 100 %) và theo
dõi crash rate trong *Android vitals*. Thấy lỗi thì **Halt rollout**, chưa nhận bản thì không bị.

### K893 · Play đòi khai báo foreground service

`🟡 đã làm app` · `Android`

**Gặp:** target 34+ có foreground service thì Play Console chặn phát hành ở mục *App content → Foreground service
permissions*.

**Vì sao:** Play yêu cầu giải thích từng loại foreground service app dùng (`location`, `dataSync`…), kèm mô tả tính năng và
thường là video quay màn hình, để chống lạm dụng chạy nền.

**Sửa:** khai báo đúng loại trong manifest (K828), điền form với mô tả việc người dùng thấy được (ví dụ "ghi lộ trình chuyến
đi khi màn hình tắt, có notification và nút Dừng") và video ngắn.

**Gặp ở:** [`apps/TripLog/androidApp/src/main/AndroidManifest.xml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/AndroidManifest.xml) (`foregroundServiceType="location"`) · Xem thêm: K846

### K894 · Xcode: `Signing for "iosApp" requires a development team`

`🟢 mới học` · `iOS`

**Gặp:** build lên máy thật hoặc Archive thì báo `Signing for "iosApp" requires a development team. Select a development
team in the Signing & Capabilities editor.`; hoặc `No profiles for 'dev.xxx' were found`.

**Vì sao:** app iOS chạy trên máy thật phải được ký bằng chứng chỉ của một team, kèm provisioning profile khớp bundle id.
Simulator thì không cần.

**Sửa:** *target iosApp → Signing & Capabilities*: chọn Team, bật *Automatically manage signing*, đặt bundle id riêng (bundle
id đã có người dùng thì không tạo profile được). Dùng `project.yml` (XcodeGen) thì đặt `DEVELOPMENT_TEAM` ở đó để không mất
khi sinh lại project.

### K895 · `ITMS-90683: Missing purpose string in Info.plist`

`🟢 mới học` · `iOS`

**Gặp:** upload lên App Store Connect thì nhận email *ITMS-90683: Missing purpose string in Info.plist*; hoặc app crash ngay
khi xin quyền vị trí/Bluetooth/camera.

**Vì sao:** mỗi quyền nhạy cảm cần một câu giải thích trong `Info.plist` (`NSLocationWhenInUseUsageDescription`,
`NSBluetoothAlwaysUsageDescription`, `NSCameraUsageDescription`…). Thiếu câu là crash khi hỏi quyền và bị từ chối khi nộp.
Có khi thư viện (không phải code của bạn) tham chiếu API cần quyền.

**Sửa:** thêm câu cụ thể nói **dùng để làm gì**. Câu chung chung kiểu "App cần vị trí" dễ bị App Review từ chối.

```yaml
NSLocationWhenInUseUsageDescription: Trip Log records your route, distance and speed while a trip is running, also with the screen off.
```

**Gặp ở:** [`apps/TripLog/iosApp/project.yml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/iosApp/project.yml)

### K896 · App Review từ chối vì khai background mode `location`

`🟡 đã làm app` · `iOS`

**Gặp:** bị từ chối theo *Guideline 2.5.4*: app khai `UIBackgroundModes` có `location` nhưng reviewer không thấy tính năng nào
cần vị trí lúc chạy nền.

**Vì sao:** background mode chỉ được khai cho đúng mục đích của nó. Reviewer cần thấy tính năng đó (ví dụ ghi lộ trình khi
tắt màn hình) và cách bật nó.

**Sửa:** chỉ khai mode thật sự dùng. Ghi rõ trong *App Review Information → Notes* các bước bật tính năng (bấm *Bắt đầu*, khoá
màn hình, đi vài bước), kèm video nếu cần.

**Gặp ở:** [`apps/TripLog/iosApp/project.yml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/iosApp/project.yml) (`UIBackgroundModes: [location]`) · Xem thêm: K848

### K897 · `ITMS-91053: Missing API declaration`

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** upload xong nhận email *ITMS-91053: Missing API declaration* nhắc tới `NSPrivacyAccessedAPICategoryUserDefaults`,
`…FileTimestamp`, `…SystemBootTime`…

**Vì sao:** từ 5/2024 Apple yêu cầu file `PrivacyInfo.xcprivacy` khai lý do dùng các *required reason API*. Framework Kotlin
cũng có thể chạm tới chúng (thời gian file, uptime hệ thống, `NSUserDefaults` qua thư viện settings).

**Sửa:** thêm `PrivacyInfo.xcprivacy` vào target app, khai từng loại API được nêu trong email kèm mã lý do phù hợp. Nâng các
thư viện bên thứ ba lên bản có privacy manifest riêng.

Xem thêm: [Bài 14: Build và phát hành](../lessons/14-build-publish.md)

### K898 · `ITMS-4238: Redundant Binary Upload`

`🟢 mới học` · `iOS`

**Gặp:** upload archive lần hai bị từ chối: *You've already uploaded a build with build number '1' for version number
'1.0.0'.*

**Vì sao:** App Store Connect phân biệt build bằng cặp *Version* (`CFBundleShortVersionString`) và *Build*
(`CFBundleVersion`). Cùng version thì build phải tăng mỗi lần upload, kể cả bản lỗi đã bỏ.

**Sửa:** tăng *Build* trước mỗi lần Archive (hoặc để CI dùng số build: `agvtool new-version -all $BUILD_NUMBER`).

Xem thêm: K886

### K899 · Mỗi bản TestFlight đều bị hỏi "Missing Compliance"

`🟢 mới học` · `iOS`

**Gặp:** build lên App Store Connect đứng ở trạng thái *Missing Compliance*, tester không tải được cho tới khi trả lời câu
hỏi mã hoá.

**Vì sao:** Apple hỏi app có dùng mã hoá ngoài mức được miễn (chuẩn của hệ điều hành như HTTPS) không. Không khai trong
`Info.plist` thì hỏi lại mỗi build.

**Sửa:** app chỉ dùng HTTPS/mã hoá của hệ thống thì khai sẵn `ITSAppUsesNonExemptEncryption = NO`.

```yaml
ITSAppUsesNonExemptEncryption: false
```

### K900 · Crash iOS trên dashboard chỉ là địa chỉ bộ nhớ

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** Crashlytics/Sentry báo crash iOS với stack trace `0x0000000104a1c2f8`, không có tên hàm Kotlin; Crashlytics ghi
*Missing dSYM*.

**Vì sao:** bản release bỏ thông tin debug khỏi file chạy. Để đọc lại tên hàm cần file **dSYM** của đúng bản build đó. Nếu
framework Kotlin là *dynamic*, nó có dSYM riêng; exception Kotlin không bắt còn cần CrashKiOS (hoặc SDK Sentry KMP) để
thành báo cáo đọc được.

**Sửa:** đặt *Debug Information Format* = `DWARF with dSYM File` cho Release, upload dSYM của app (và của framework nếu là
dynamic) sau mỗi lần Archive, bằng script của Crashlytics/Sentry trong Build Phases.

Xem thêm: K881, [Bài 14: Build và phát hành](../lessons/14-build-publish.md)
