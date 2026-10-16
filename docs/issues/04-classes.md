# 4. Class, object, sealed, enum (K151–K200)

Cách Kotlin dựng kiểu dữ liệu: `class` mặc định `final`, `data class` tự sinh `equals`/`copy`, `object` là singleton, `sealed` và `enum`
để compiler bắt bạn xử lý đủ trường hợp. Bẫy chủ yếu nằm ở chỗ "tự sinh": sinh ra cái gì, và **không** sinh cái gì.

### K151 · `data class` so sánh bằng nhau dù khác một field

`🟢 mới học` · `Kotlin`

**Gặp:** hai object khác `pinned` mà `a == b` vẫn `true`; `copy()` làm mất giá trị `pinned`; `toString()` không in field đó.

**Vì sao:** `equals`, `hashCode`, `toString`, `copy`, `componentN` chỉ dùng property khai báo trong **constructor chính**. Property trong thân class
bị bỏ qua hết.

**Sửa:** đưa mọi property có ý nghĩa vào constructor, kèm giá trị mặc định nếu cần.

```kotlin
data class Note(val id: Long, val title: String) { var pinned = false }   // ❌
data class Note(val id: Long, val title: String, val pinned: Boolean = false)  // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE1MTogZGF0YSBjbGFzcyBjaOG7iSBzbyBzw6FuaCBwcm9wZXJ0eSB0cm9uZyBjb25zdHJ1Y3RvciBjaMOtbmgKZGF0YSBjbGFzcyBOb3RlKHZhbCBpZDogTG9uZywgdmFsIHRpdGxlOiBTdHJpbmcpIHsKICAgIHZhciBwaW5uZWQ6IEJvb2xlYW4gPSBmYWxzZSAgICAgICAgICAvLyDinYwgbuG6sW0gdHJvbmcgdGjDom4g4oaSIGtow7RuZyB2w6BvIGVxdWFscy9oYXNoQ29kZS90b1N0cmluZy9jb3B5Cn0KLy8g4pyFIGRhdGEgY2xhc3MgTm90ZSh2YWwgaWQ6IExvbmcsIHZhbCB0aXRsZTogU3RyaW5nLCB2YWwgcGlubmVkOiBCb29sZWFuID0gZmFsc2UpCmZ1biBtYWluKCkgewogICAgdmFsIGEgPSBOb3RlKDEsICJNdWEgc-G7r2EiKS5hcHBseSB7IHBpbm5lZCA9IHRydWUgfQogICAgdmFsIGIgPSBOb3RlKDEsICJNdWEgc-G7r2EiKQogICAgcHJpbnRsbihhID09IGIpICAgICAgICAgICAgICAgICAgICAgICAvLyB0cnVlIGTDuSBwaW5uZWQga2jDoWMgbmhhdQogICAgcHJpbnRsbihhKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBraMO0bmcgdGjhuqV5IHBpbm5lZAogICAgcHJpbnRsbihhLmNvcHkoKS5waW5uZWQpICAgICAgICAgICAgICAvLyBmYWxzZSDigJQgY29weSBsw6BtIG3huqV0IHBpbm5lZAp9Cg) · Xem thêm: [Kotlin khó gì, bẫy gì: mục 2.8](../reference/kotlin-pitfalls.md), K152

### K152 · `copy()` là copy nông

`🟡 đã làm app` · `Kotlin`

**Gặp:** sửa list trong bản `copy()` mà bản gốc cũng đổi; StateFlow không phát vì "state cũ" đã bị sửa giống hệt state mới.

**Vì sao:** `copy()` chép **tham chiếu** của từng property. `MutableList` bên trong vẫn là một object dùng chung giữa hai bản.

**Sửa:** trong `data class` dùng `List` (chỉ đọc) và tạo list mới khi đổi.

```kotlin
data class Bill(val people: MutableList<String>)               // ❌
data class Bill(val people: List<String>)                      // ✅
val next = bill.copy(people = bill.people + "Chi")             // list mới
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE1MjogY29weSgpIGzDoCBjb3B5IG7DtG5nOiBsaXN0IG11dGFibGUgYsOqbiB0cm9uZyB24bqrbiBkw7luZyBjaHVuZwpkYXRhIGNsYXNzIEJpbGwodmFsIHBlb3BsZTogTXV0YWJsZUxpc3Q8U3RyaW5nPikgICAvLyDinYwgTXV0YWJsZUxpc3QgdHJvbmcgZGF0YSBjbGFzcwpmdW4gbWFpbigpIHsKICAgIHZhbCBvcmlnaW5hbCA9IEJpbGwobXV0YWJsZUxpc3RPZigiQW4iLCAiQsOsbmgiKSkKICAgIHZhbCBlZGl0ZWQgPSBvcmlnaW5hbC5jb3B5KCkKICAgIGVkaXRlZC5wZW9wbGUgKz0gIkNoaSIKICAgIHByaW50bG4ob3JpZ2luYWwucGVvcGxlKSAgICAgICAgICAgICAgICAgICAgICAgLy8gW0FuLCBCw6xuaCwgQ2hpXSDigJQgYuG6o24gZ-G7kWMgY8WpbmcgxJHhu5VpCiAgICAvLyDinIUgZGF0YSBjbGFzcyBCaWxsKHZhbCBwZW9wbGU6IExpc3Q8U3RyaW5nPikgcuG7k2kgb3JpZ2luYWwuY29weShwZW9wbGUgPSBvcmlnaW5hbC5wZW9wbGUgKyAiQ2hpIikKfQo) · Xem thêm: K148, [Flow, StateFlow](07-flow.md)

### K153 · Class thường: `==` ra `false` dù cùng giá trị

`🟢 mới học` · `Kotlin`

**Gặp:** `Money(100, "VND") == Money(100, "VND")` là `false`; test `assertEquals` fail với thông báo `expected: <Money@24d46ca6> but was: <Money@5a2e4553>`.

**Vì sao:** class thường kế thừa `equals` của `Any`: so địa chỉ. `toString` cũng là tên class + mã hash.

**Sửa:** class chỉ chở dữ liệu thì khai báo `data class`. Class có hành vi (repository, service) thì so địa chỉ là đúng, không cần đổi.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE1MzogY2xhc3MgdGjGsOG7nW5nIHNvIHPDoW5oID09IHRoZW8gxJHhu4thIGNo4buJIG9iamVjdApjbGFzcyBNb25leSh2YWwgYW1vdW50OiBMb25nLCB2YWwgY3VycmVuY3k6IFN0cmluZykgICAgICAgIC8vIOKdjApkYXRhIGNsYXNzIE1vbmV5Mih2YWwgYW1vdW50OiBMb25nLCB2YWwgY3VycmVuY3k6IFN0cmluZykgIC8vIOKchQpmdW4gbWFpbigpIHsKICAgIHByaW50bG4oTW9uZXkoMTAwLCAiVk5EIikgPT0gTW9uZXkoMTAwLCAiVk5EIikpICAgICAvLyBmYWxzZQogICAgcHJpbnRsbihNb25leTIoMTAwLCAiVk5EIikgPT0gTW9uZXkyKDEwMCwgIlZORCIpKSAgIC8vIHRydWUKICAgIHByaW50bG4oTW9uZXkoMTAwLCAiVk5EIikpICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gTW9uZXlAeHh4eAp9Cg) · Xem thêm: K130, K154

### K154 · Override `equals` mà quên `hashCode`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `a == b` là `true` mà `hashSetOf(a).contains(b)` là `false`; `map[key]` trả `null`; `distinct()` không bỏ trùng.

**Vì sao:** `HashSet`/`HashMap` tìm theo `hashCode()` trước rồi mới `equals()`. Hai object "bằng nhau" mà hash khác là nằm ở hai ngăn khác nhau.
Quy tắc: `a == b` thì bắt buộc `a.hashCode() == b.hashCode()`.

**Sửa:** override cả hai, dùng cùng các field. Dễ nhất là dùng `data class` để compiler sinh cả hai.

```kotlin
override fun equals(other: Any?) = other is Code && other.value == value
override fun hashCode() = value.hashCode()   // ✅ đừng quên dòng này
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE1NDogb3ZlcnJpZGUgZXF1YWxzIG3DoCBxdcOqbiBoYXNoQ29kZSDihpIgSGFzaFNldCAvIEhhc2hNYXAga2jDtG5nIHTDrG0gdGjhuqV5CmNsYXNzIENvZGUodmFsIHZhbHVlOiBTdHJpbmcpIHsKICAgIG92ZXJyaWRlIGZ1biBlcXVhbHMob3RoZXI6IEFueT8pID0gb3RoZXIgaXMgQ29kZSAmJiBvdGhlci52YWx1ZSA9PSB2YWx1ZQogICAgLy8g4p2MIHRoaeG6v3U6IG92ZXJyaWRlIGZ1biBoYXNoQ29kZSgpID0gdmFsdWUuaGFzaENvZGUoKQp9CmZ1biBtYWluKCkgewogICAgcHJpbnRsbihDb2RlKCJQMDMwMCIpID09IENvZGUoIlAwMzAwIikpICAgICAgICAgICAgICAgLy8gdHJ1ZQogICAgcHJpbnRsbihoYXNoU2V0T2YoQ29kZSgiUDAzMDAiKSkuY29udGFpbnMoQ29kZSgiUDAzMDAiKSkpICAvLyBmYWxzZQp9Cg) · Xem thêm: K129, K196

### K155 · `data class` chứa `Array` so sánh sai

`🟡 đã làm app` · `Kotlin`

**Gặp:** hai `Frame` cùng `bytes` mà `==` là `false`. Compiler có cảnh báo `Property with 'Array' type in a 'data' class: it is recommended to override 'equals()' and 'hashCode()'`.

**Vì sao:** `equals` sinh ra gọi `equals` của từng property, mà mảng thì so địa chỉ (K116).

**Sửa:** dùng `List<Byte>` thay `ByteArray` khi dữ liệu nhỏ; giữ mảng thì tự override `equals`/`hashCode` bằng `contentEquals`/`contentHashCode`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE1NTogZGF0YSBjbGFzcyBjw7MgQXJyYXk6IGVxdWFscyBzbyDEkeG7i2EgY2jhu4kgbeG6o25nLCBraMO0bmcgc28gbuG7mWkgZHVuZwpkYXRhIGNsYXNzIEZyYW1lKHZhbCBpZDogSW50LCB2YWwgYnl0ZXM6IEJ5dGVBcnJheSkgICAgICAgLy8g4p2MIChjb21waWxlciBjw7MgY-G6o25oIGLDoW8pCmRhdGEgY2xhc3MgRnJhbWUyKHZhbCBpZDogSW50LCB2YWwgYnl0ZXM6IExpc3Q8Qnl0ZT4pICAgICAvLyDinIUgTGlzdCBzbyBu4buZaSBkdW5nCmZ1biBtYWluKCkgewogICAgcHJpbnRsbihGcmFtZSgxLCBieXRlQXJyYXlPZigweDQxKSkgPT0gRnJhbWUoMSwgYnl0ZUFycmF5T2YoMHg0MSkpKSAgIC8vIGZhbHNlCiAgICBwcmludGxuKEZyYW1lMigxLCBsaXN0T2YoMHg0MSkpID09IEZyYW1lMigxLCBsaXN0T2YoMHg0MSkpKSAgICAgICAgICAgLy8gdHJ1ZQp9Cg) · Xem thêm: [K116](03-collections.md)

### K156 · Kế thừa `data class`

`🟢 mới học` · `Kotlin`

**Gặp:** `This type is final, so it cannot be extended.` Thêm `open` vào `data class` thì gặp `Modifier 'open' is incompatible with 'data'`.

**Vì sao:** `data class` luôn `final`: `equals`/`copy` sinh ra không thể đúng khi lớp con thêm field.

**Sửa:** lớp cha là `sealed interface` (hoặc `sealed class`/`abstract class` không phải data), các lớp con là `data class`.

```kotlin
sealed interface Shape
data class Circle(val r: Double) : Shape
data class Rect(val w: Double, val h: Double) : Shape
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE1NjogZGF0YSBjbGFzcyBraMO0bmcgbMOgbSBs4bubcCBjaGEgxJHGsOG7o2MKZGF0YSBjbGFzcyBTaGFwZSh2YWwgbmFtZTogU3RyaW5nKQpjbGFzcyBDaXJjbGUodmFsIHI6IERvdWJsZSkgOiBTaGFwZSgiY2lyY2xlIikgICAvLyDinYwgY29tcGlsZSBlcnJvcgovLyDinIUgc2VhbGVkIGludGVyZmFjZSBTaGFwZTsgZGF0YSBjbGFzcyBDaXJjbGUodmFsIHI6IERvdWJsZSkgOiBTaGFwZQpmdW4gbWFpbigpID0gcHJpbnRsbihDaXJjbGUoMS4wKSkK) · Xem thêm: K157

### K157 · `This type is final, so it cannot be extended.`

`🟢 mới học` · `Kotlin`

**Gặp:** `This type is final, so it cannot be extended.` khi viết `class Dog : Animal("dog")`.

**Vì sao:** khác Java, class Kotlin mặc định `final`. Kế thừa phải được lớp cha cho phép.

**Sửa:** thêm `open` (hoặc `abstract`/`sealed`) cho lớp cha. Không sửa được lớp cha (thư viện) thì dùng composition hoặc extension function.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE1NzogY2xhc3MgS290bGluIG3hurdjIMSR4buLbmggbMOgIGZpbmFsCmNsYXNzIEFuaW1hbCh2YWwgbmFtZTogU3RyaW5nKSAgICAgICAgICAgICAgICAgLy8g4pyFIG9wZW4gY2xhc3MgQW5pbWFsKC4uLikKY2xhc3MgRG9nIDogQW5pbWFsKCJkb2ciKSAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgY29tcGlsZSBlcnJvcgpmdW4gbWFpbigpID0gcHJpbnRsbihEb2coKS5uYW1lKQo) · Xem thêm: K158

### K158 · Override hàm không `open`

`🟢 mới học` · `Kotlin`

**Gặp:** `'sound' in 'Animal' is final and cannot be overridden.`

**Vì sao:** hàm và property cũng mặc định `final`, kể cả trong `open class`.

**Sửa:** `open fun sound()` ở lớp cha. Ở lớp con, hàm đã `override` thì tự động `open` tiếp; muốn chặn thì `final override`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE1ODogaMOgbSBt4bq3YyDEkeG7i25oIGZpbmFsLCBvdmVycmlkZSBwaOG6o2kgY8OzIG9wZW4g4bufIGzhu5twIGNoYQpvcGVuIGNsYXNzIEFuaW1hbCB7IGZ1biBzb3VuZCgpID0gIi4uLiIgfSAgICAgIC8vIOKchSBvcGVuIGZ1biBzb3VuZCgpID0gIi4uLiIKY2xhc3MgRG9nIDogQW5pbWFsKCkgeyBvdmVycmlkZSBmdW4gc291bmQoKSA9ICJHw6J1IiB9ICAgLy8g4p2MIGNvbXBpbGUgZXJyb3IKZnVuIG1haW4oKSA9IHByaW50bG4oRG9nKCkuc291bmQoKSkK) · Xem thêm: K157

### K159 · Gọi hàm `open` trong `init` của lớp cha

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `NullPointerException` ở một property `val` không-null của lớp con, ví dụ
`Cannot invoke "String.toUpperCase(java.util.Locale)" because "this.label" is null`. IDE có cảnh báo `Calling non-final function title in constructor`.

**Vì sao:** lớp cha khởi tạo **trước** lớp con. `init` của lớp cha gọi hàm đã override; hàm đó đọc field của lớp con lúc field còn chưa được gán.
Null safety không bảo vệ được trường hợp này.

**Sửa:** không gọi hàm `open` (hay đọc property `open`) trong constructor/`init`. Truyền giá trị qua tham số constructor, hoặc tính lười (`by lazy`).

```kotlin
open class Screen(val title: String) { init { println(title) } }   // ✅ nhận qua constructor
class Settings : Screen("Cài đặt")
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE1OTogZ-G7jWkgaMOgbSBvcGVuIHRyb25nIGluaXQgY-G7p2EgbOG7m3AgY2hhIOKGkiBs4bubcCBjb24gY2jGsGEga2jhu59pIHThuqFvIHhvbmcKb3BlbiBjbGFzcyBTY3JlZW4gewogICAgaW5pdCB7IHByaW50bG4oInRpdGxlID0gJHt0aXRsZSgpfSIpIH0gICAgICAvLyDinYwgZ-G7jWkgaMOgbSBvcGVuIHRyb25nIGNvbnN0cnVjdG9yCiAgICBvcGVuIGZ1biB0aXRsZSgpOiBTdHJpbmcgPSAiPyIKfQpjbGFzcyBTZXR0aW5ncyA6IFNjcmVlbigpIHsKICAgIHByaXZhdGUgdmFsIGxhYmVsID0gIkPDoGkgxJHhurd0IgogICAgb3ZlcnJpZGUgZnVuIHRpdGxlKCkgPSBsYWJlbC51cHBlcmNhc2UoKSAgIC8vIGxhYmVsIGzDumMgbsOgeSBjw7JuIG51bGwKfQpmdW4gbWFpbigpIHsgU2V0dGluZ3MoKSB9Cg) · Xem thêm: K160

### K160 · Property dùng property khai báo bên dưới → `null`

`🟡 đã làm app` · `Kotlin`

**Gặp:** in ra `app=null` dù `name` là `val name = "TripLog"` không-null.

**Vì sao:** property initializer và `init` chạy **từ trên xuống** theo thứ tự viết. Đọc trực tiếp property chưa gán thì compiler báo lỗi,
nhưng đọc **qua một hàm** thì compiler không thấy, và giá trị lúc đó là `null`/`0`.

**Sửa:** đặt property phụ thuộc xuống dưới; hoặc biến nó thành getter (`val summary get() = ...`) hay `by lazy { }`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE2MDogcHJvcGVydHkgdsOgIGluaXQgY2jhuqF5IHThu6sgdHLDqm4geHXhu5FuZzsgxJHhu41jIHByb3BlcnR5IGtoYWkgYsOhbyBiw6puIGTGsOG7m2kgcXVhIGjDoG0g4oaSIG51bGwvMApjbGFzcyBDb25maWcgewogICAgdmFsIHN1bW1hcnkgPSBkZXNjcmliZSgpICAgICAgICAgICAgICAgICAgICAvLyDinYwgY2jhuqF5IHRyxrDhu5tjIGtoaSBgbmFtZWAgxJHGsOG7o2MgZ8OhbgogICAgdmFsIG5hbWUgPSAiVHJpcExvZyIKICAgIHByaXZhdGUgZnVuIGRlc2NyaWJlKCkgPSAiYXBwPSRuYW1lIgogICAgLy8g4pyFIMSRxrBhIGBuYW1lYCBsw6puIHRyw6puLCBob-G6t2M6IHZhbCBzdW1tYXJ5IGdldCgpID0gZGVzY3JpYmUoKSAgLyAgdmFsIHN1bW1hcnkgYnkgbGF6eSB7IGRlc2NyaWJlKCkgfQp9CmZ1biBtYWluKCkgPSBwcmludGxuKENvbmZpZygpLnN1bW1hcnkpICAgICAgICAgICAvLyBhcHA9bnVsbAo) · Xem thêm: K159, K180

### K161 · `Primary constructor call expected`

`🟢 mới học` · `Kotlin`

**Gặp:** `Primary constructor call expected.` ở constructor phụ.

**Vì sao:** class có constructor chính thì mọi constructor phụ phải gọi tới nó (trực tiếp hoặc qua constructor phụ khác) bằng `: this(...)`.

**Sửa:** `constructor(both: Int) : this(both, both)`. Thường còn gọn hơn: bỏ constructor phụ, dùng giá trị mặc định cho tham số.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE2MTogY29uc3RydWN0b3IgcGjhu6UgcGjhuqNpIGfhu41pIGNvbnN0cnVjdG9yIGNow61uaCBi4bqxbmcgdGhpcyguLi4pCmNsYXNzIFBvaW50KHZhbCB4OiBJbnQsIHZhbCB5OiBJbnQpIHsKICAgIGNvbnN0cnVjdG9yKGJvdGg6IEludCkgeyB9ICAgICAgICAgICAgICAgICAgICAvLyDinYwgY29tcGlsZSBlcnJvcgogICAgLy8g4pyFIGNvbnN0cnVjdG9yKGJvdGg6IEludCkgOiB0aGlzKGJvdGgsIGJvdGgpCn0KZnVuIG1haW4oKSA9IHByaW50bG4oUG9pbnQoMSkueCkK)

### K162 · Tham số constructor không phải property

`🟢 mới học` · `Kotlin`

**Gặp:** `Unresolved reference 'name' on receiver of type 'User'.` khi gọi `user.name`.

**Vì sao:** `class User(name: String)` chỉ có **tham số**, dùng được trong `init` và initializer, rồi mất. Thêm `val`/`var` mới tạo property.

**Sửa:** `class User(val name: String)`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE2MjogdGhhbSBz4buRIGNvbnN0cnVjdG9yIGtow7RuZyBjw7MgdmFsL3ZhciBjaOG7iSBsw6AgdGhhbSBz4buRLCBraMO0bmcgcGjhuqNpIHByb3BlcnR5CmNsYXNzIFVzZXIobmFtZTogU3RyaW5nKSAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4pyFIGNsYXNzIFVzZXIodmFsIG5hbWU6IFN0cmluZykKZnVuIG1haW4oKSB7CiAgICB2YWwgdSA9IFVzZXIoIlTDuiIpCiAgICBwcmludGxuKHUubmFtZSkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIGNvbXBpbGUgZXJyb3IKfQo)

### K163 · `object` giữ state dùng chung cho cả app

`🟡 đã làm app` · `Kotlin` · `KMP`

**Gặp:** test chạy riêng thì pass, chạy cả bộ thì fail; giỏ hàng / phiên đăng nhập của "người trước" còn lại sau khi đăng xuất.

**Vì sao:** `object` là singleton sống suốt process. State `var`/`MutableList` trong đó là biến toàn cục, mọi test và mọi màn hình dùng chung.

**Sửa:** để state trong class thường và truyền vào (DI: `single { }` trong Koin), test tạo instance mới cho mỗi case. `object` chỉ nên chứa hằng
và hàm thuần.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE2Mzogb2JqZWN0IGzDoCBzaW5nbGV0b24g4oCUIHN0YXRlIHRyb25nIMSRw7MgZMO5bmcgY2h1bmcgY2hvIGPhuqMgYXBwICh2w6AgbeG7jWkgdGVzdCkKb2JqZWN0IFNlc3Npb24gewogICAgdmFsIGNhcnQgPSBtdXRhYmxlTGlzdE9mPFN0cmluZz4oKSAgICAgICAgICAgIC8vIOKdjCBzdGF0ZSB0b8OgbiBj4bulYwp9CmZ1biBjaGVja291dCgpOiBJbnQgeyB2YWwgbiA9IFNlc3Npb24uY2FydC5zaXplOyByZXR1cm4gbiB9CmZ1biBtYWluKCkgewogICAgU2Vzc2lvbi5jYXJ0ICs9ICJjw6AgcGjDqiIgICAgICAgICAgICAgICAgICAgICAgIC8vICJ0ZXN0IDEiCiAgICBwcmludGxuKGNoZWNrb3V0KCkpICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIDEKICAgIC8vICJ0ZXN0IDIiIHTGsOG7n25nIGdp4buPIHLhu5duZyBuaMawbmcgduG6q24gY8OybiDEkeG7kyBj4bunYSB0ZXN0IDEKICAgIHByaW50bG4oY2hlY2tvdXQoKSkgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gMSwga2jDtG5nIHBo4bqjaSAwCiAgICAvLyDinIUgxJHGsGEgc3RhdGUgdsOgbyBjbGFzcyB0aMaw4budbmcsIHRydXnhu4FuIHbDoG8gKGhv4bq3YyBzaW5nbGUgdHJvbmcgS29pbiksIHRlc3QgdOG6oW8gaW5zdGFuY2UgbeG7m2kKfQo) · Xem thêm: K178

### K164 · `object` khởi tạo ở lần dùng đầu tiên

`🟡 đã làm app` · `Kotlin` · `Android`

**Gặp:** cấu hình trong `init { }` của `object` không chạy lúc app mở như mong đợi; hoặc lần bấm nút đầu tiên bị khựng vì `object` làm việc nặng trong `init`.

**Vì sao:** trên JVM, `object` khởi tạo **lười**, khi code chạm tới nó lần đầu (thread-safe). Không ai dùng thì `init` không bao giờ chạy.

**Sửa:** việc cần chạy lúc khởi động thì gọi rõ ràng (vd `Application.onCreate()` gọi `Analytics.init(context)`). Đừng để việc nặng
(đọc file, mở DB) trong `init` của `object`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE2NDogb2JqZWN0IGto4bufaSB04bqhbyBsxrDhu51pOiBpbml0IGNo4bqheSDhu58gbOG6p24gZMO5bmcgxJDhuqZVIFRJw4pOLCBraMO0bmcgcGjhuqNpIGzDumMgYXBwIG3hu58Kb2JqZWN0IEFuYWx5dGljcyB7CiAgICBpbml0IHsgcHJpbnRsbigiQW5hbHl0aWNzIGluaXQiKSB9CiAgICBmdW4gbG9nKGU6IFN0cmluZykgPSBwcmludGxuKCJsb2cgJGUiKQp9CmZ1biBtYWluKCkgewogICAgcHJpbnRsbigiYXBwIHN0YXJ0IikKICAgIEFuYWx5dGljcy5sb2coIm9wZW4iKSAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gaW5pdCBjaOG6oXkg4bufIMSRw6J5CiAgICBBbmFseXRpY3MubG9nKCJ0YXAiKSAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIGtow7RuZyBjaOG6oXkgbOG6oWkKfQo)

### K165 · Hàm public trả `object { }` → chỉ còn `Any`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `Unresolved reference 'count' on receiver of type 'Any'.`

**Vì sao:** object ẩn danh chỉ giữ kiểu "thật" khi hàm/property là `private` (hoặc local). Public thì kiểu trả về bị nâng lên lớp cha
khai báo được, ở đây là `Any`.

**Sửa:** để `private`, hoặc khai báo kiểu rõ ràng (`interface Counter { var count: Int }` rồi `object : Counter { ... }`).

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE2NTogaMOgbSBwdWJsaWMgdHLhuqMgduG7gSBvYmplY3Qg4bqpbiBkYW5oIOKGkiBiw6puIG5nb8OgaSBjaOG7iSB0aOG6pXkga2nhu4N1IEFueQpmdW4gbWFrZUNvdW50ZXIoKSA9IG9iamVjdCB7IHZhciBjb3VudCA9IDAgfSAgICAgIC8vIOKdjCBwdWJsaWM6IGtp4buDdSB0cuG6oyB24buBIGzDoCBBbnkKLy8g4pyFIHByaXZhdGUgZnVuIG1ha2VDb3VudGVyKCkgPSAuLi4gIChwcml2YXRlIHRow6wgZ2nhu68ga2nhu4N1IOG6qW4gZGFuaCkgaG_hurdjIGtoYWkgYsOhbyBjbGFzcy9pbnRlcmZhY2UgcsO1IHLDoG5nCmZ1biBtYWluKCkgewogICAgdmFsIGMgPSBtYWtlQ291bnRlcigpCiAgICBjLmNvdW50KysgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBjb21waWxlIGVycm9yCiAgICBwcmludGxuKGMuY291bnQpCn0K)

### K166 · `when` trên `sealed` thiếu nhánh

`🟢 mới học` · `Kotlin`

**Gặp:** `'when' expression must be exhaustive. Add the 'is Failed' branch or an 'else' branch.`

**Vì sao:** đây là tính năng chứ không phải lỗi: compiler biết đủ các lớp con của `sealed` và bắt bạn xử lý hết. Thường gặp ngay sau khi
thêm một trạng thái mới.

**Sửa:** thêm nhánh còn thiếu. **Đừng** thêm `else` cho nhanh (xem K167).

```kotlin
fun label(s: LoadState) = when (s) {
    LoadState.Loading -> "Đang tải"
    is LoadState.Ready -> "${s.items.size} mục"
    is LoadState.Failed -> "Lỗi: ${s.message}"   // ✅
}
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE2Njogd2hlbiB0csOqbiBzZWFsZWQgdGhp4bq_dSBuaMOhbmgKc2VhbGVkIGNsYXNzIExvYWRTdGF0ZSB7CiAgICBkYXRhIG9iamVjdCBMb2FkaW5nIDogTG9hZFN0YXRlKCkKICAgIGRhdGEgY2xhc3MgUmVhZHkodmFsIGl0ZW1zOiBMaXN0PFN0cmluZz4pIDogTG9hZFN0YXRlKCkKICAgIGRhdGEgY2xhc3MgRmFpbGVkKHZhbCBtZXNzYWdlOiBTdHJpbmcpIDogTG9hZFN0YXRlKCkKfQpmdW4gbGFiZWwoczogTG9hZFN0YXRlKTogU3RyaW5nID0gd2hlbiAocykgewogICAgTG9hZFN0YXRlLkxvYWRpbmcgLT4gIsSQYW5nIHThuqNpIgogICAgaXMgTG9hZFN0YXRlLlJlYWR5IC0-ICIke3MuaXRlbXMuc2l6ZX0gbeG7pWMiCiAgICAvLyDinYwgdGhp4bq_dSBGYWlsZWQg4oaSIGNvbXBpbGUgZXJyb3IuICDinIUgdGjDqm06IGlzIExvYWRTdGF0ZS5GYWlsZWQgLT4gIkzhu5dpOiAke3MubWVzc2FnZX0iCn0KZnVuIG1haW4oKSA9IHByaW50bG4obGFiZWwoTG9hZFN0YXRlLkxvYWRpbmcpKQo) · Xem thêm: K167, K168

### K167 · `else` trong `when` trên `sealed` che mất trạng thái mới

`🟡 đã làm app` · `Kotlin`

**Gặp:** thêm `Reconnecting` vào sealed, build qua hết, nhưng màn hình xử lý nó như trạng thái lỗi/ngắt kết nối.

**Vì sao:** `else` khớp mọi thứ, kể cả lớp con thêm sau. Compiler không còn lý do để nhắc bạn.

**Sửa:** với `sealed` và `enum`, liệt kê đủ từng nhánh, bỏ `else`. Lần sau thêm trạng thái, mọi `when` chưa xử lý sẽ đỏ.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE2NzogYGVsc2VgIHRyb25nIHdoZW4gdHLDqm4gc2VhbGVkOiB0aMOqbSB0cuG6oW5nIHRow6FpIG3hu5tpLCBjb21waWxlciBraMO0bmcgbmjhuq9jCnNlYWxlZCBpbnRlcmZhY2UgQ29ubiB7CiAgICBkYXRhIG9iamVjdCBJZGxlIDogQ29ubgogICAgZGF0YSBvYmplY3QgQ29ubmVjdGVkIDogQ29ubgogICAgZGF0YSBvYmplY3QgUmVjb25uZWN0aW5nIDogQ29ubiAgICAgIC8vIHbhu6thIHRow6ptCn0KZnVuIGNhblNlbmQoYzogQ29ubikgPSB3aGVuIChjKSB7CiAgICBDb25uLkNvbm5lY3RlZCAtPiB0cnVlCiAgICBlbHNlIC0-IGZhbHNlICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIFJlY29ubmVjdGluZyByxqFpIHbDoG8gxJHDonkgbcOgIGtow7RuZyBhaSBiaeG6v3QKfQovLyDinIUgbGnhu4d0IGvDqiDEkeG7pzogQ29ubi5JZGxlIC0-IGZhbHNlOyBDb25uLkNvbm5lY3RlZCAtPiB0cnVlOyBDb25uLlJlY29ubmVjdGluZyAtPiAuLi4gIChi4buPIGVsc2UpCmZ1biBtYWluKCkgPSBwcmludGxuKGNhblNlbmQoQ29ubi5SZWNvbm5lY3RpbmcpKQo) · Xem thêm: K166

### K168 · `when` không lấy giá trị vẫn bị bắt đủ nhánh

`🟢 mới học` · `Kotlin`

**Gặp:** `'when' expression must be exhaustive. Add the 'Profile' branch or an 'else' branch.` dù `when` chỉ là câu lệnh, không gán vào đâu.

**Vì sao:** từ Kotlin 1.7, `when` trên `sealed`, `enum` và `Boolean` phải đủ nhánh cả khi dùng như câu lệnh.

**Sửa:** thêm nhánh còn thiếu; nhánh không làm gì thì viết rõ `Tab.Profile -> Unit` (hoặc `-> {}`) để người đọc biết là cố ý.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE2ODogd2hlbiB0csOqbiBzZWFsZWQvZW51bSBwaOG6o2kgxJHhu6cgbmjDoW5oIGPhuqMga2hpIEtIw5RORyBkw7luZyBnacOhIHRy4buLICh3aGVuIHN0YXRlbWVudCkKZW51bSBjbGFzcyBUYWIgeyBIb21lLCBTZWFyY2gsIFByb2ZpbGUgfQpmdW4gb3Blbih0OiBUYWIpIHsKICAgIHdoZW4gKHQpIHsgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIGNvbXBpbGUgZXJyb3IgZMO5IGtow7RuZyBnw6FuIGvhur90IHF14bqjCiAgICAgICAgVGFiLkhvbWUgLT4gcHJpbnRsbigiaG9tZSIpCiAgICAgICAgVGFiLlNlYXJjaCAtPiBwcmludGxuKCJzZWFyY2giKQogICAgfQp9CmZ1biBtYWluKCkgPSBvcGVuKFRhYi5Ib21lKQo) · Xem thêm: K166

### K169 · Lớp con của `sealed` đặt ở package / module khác

`🟡 đã làm app` · `Kotlin` · `KMP`

**Gặp:** lỗi kiểu `Inheritor of sealed class or interface declared in package ... but it must be in package ... where base class is declared`,
hoặc `Inheritance of sealed classes or interfaces from different module is prohibited`.

**Vì sao:** compiler chỉ biết "đủ các lớp con" khi mọi lớp con nằm cùng **module** và cùng **package** với lớp `sealed`. Trong KMP, tách
`core/obd` và `shared` thành hai module là hai module khác nhau.

**Sửa:** đặt mọi lớp con cạnh lớp `sealed` (cùng package, có thể khác file). Module khác cần mở rộng thì lớp cha không nên là `sealed`
(dùng `interface` thường) hoặc bọc trạng thái của module kia vào một lớp con có sẵn.

### K170 · `sealed interface` sang Swift mất thông tin lớp con

`🔴 khó, hiếm nhưng đau` · `KMP` · `iOS`

**Gặp:** Android dùng `when` bình thường; bên Swift, `onEnum(of:)` của SKIE hoặc `switch` không liệt kê được các trường hợp như mong đợi,
phải `as?` từng kiểu.

**Vì sao:** Kotlin/Native xuất sang Objective-C. Objective-C giữ được quan hệ lớp cha / lớp con của **class**, còn interface Kotlin thành
protocol và mất phần lớn thông tin đó.

**Sửa:** kiểu `sealed` sẽ được Swift dùng thì khai báo `sealed class`, lớp con là `data object` / `data class`.

```kotlin
sealed interface ConvertResult               // ❌ cho kiểu Swift sẽ switch
sealed class ConvertResult {                 // ✅
    data object Waiting : ConvertResult()
    data class Ready(val amount: Double) : ConvertResult()
}
```

**Gặp ở:** [`RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/model/Models.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/model/Models.kt) ·
[`TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/model/Models.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/model/Models.kt) ·
Xem thêm: [Swift ↔ Kotlin, iOS, Xcode](10-ios-interop.md)

### K171 · Trạng thái không dữ liệu viết bằng `class` → `==` ra `false`

`🟡 đã làm app` · `Kotlin` · `Compose`

**Gặp:** mỗi lần phát `Loading()` UI lại nháy / recompose dù trạng thái "y hệt"; test `assertEquals(UiState.Loading(), state)` fail.

**Vì sao:** `class Loading : UiState()` không có field nên không thể là `data class`; mỗi lần gọi `Loading()` là một object mới, `==` so địa chỉ.

**Sửa:** trạng thái không mang dữ liệu thì dùng `data object`: một instance duy nhất, `==` đúng, `toString()` là `Loading`.

```kotlin
class Loading : UiState()          // ❌
data object Loading : UiState()    // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE3MTogdHLhuqFuZyB0aMOhaSBraMO0bmcgY8OzIGThu68gbGnhu4d1IHZp4bq_dCBi4bqxbmcgY2xhc3Mg4oaSIG3hu5dpIGzhuqduIGzDoCBt4buZdCBvYmplY3QgbeG7m2ksID09IHJhIGZhbHNlCnNlYWxlZCBjbGFzcyBVaVN0YXRlIHsKICAgIGNsYXNzIExvYWRpbmcgOiBVaVN0YXRlKCkgICAgICAgICAgICAvLyDinYwKICAgIC8vIGRhdGEgb2JqZWN0IExvYWRpbmcgOiBVaVN0YXRlKCkgICAvLyDinIUgbeG7mXQgaW5zdGFuY2UgZHV5IG5o4bqldCwgPT0gxJHDum5nLCB0b1N0cmluZyDEkeG6uXAKfQpmdW4gbWFpbigpIHsKICAgIHZhbCBhID0gVWlTdGF0ZS5Mb2FkaW5nKCkKICAgIHZhbCBiID0gVWlTdGF0ZS5Mb2FkaW5nKCkKICAgIHByaW50bG4oYSA9PSBiKSAgICAgICAgICAgICAgICAgICAgICAgLy8gZmFsc2Ug4oaSIFN0YXRlRmxvdy9Db21wb3NlIGNvaSBsw6Agc3RhdGUgbeG7m2kKfQo) · Xem thêm: K172

### K172 · `object` in ra `Ev$Refresh@3fee733d`

`🟢 mới học` · `Kotlin`

**Gặp:** log / test failure in ra `Ev$Refresh@3fee733d` thay vì tên trạng thái.

**Vì sao:** `object` thường dùng `toString` mặc định. `data object` (Kotlin 1.9+) sinh `toString()` trả về đúng tên, và `equals`/`hashCode` ổn định.

**Sửa:** trong `sealed` hierarchy, dùng `data object` cho trường hợp không có dữ liệu.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE3Mjogb2JqZWN0IHRoxrDhu51uZyBpbiByYSB0w6puIGzhuqE7IGRhdGEgb2JqZWN0IGluIHJhIHTDqm4gZ-G7jW4Kc2VhbGVkIGludGVyZmFjZSBFdiB7CiAgICBvYmplY3QgUmVmcmVzaCA6IEV2ICAgICAgICAgICAgICAgICAgIC8vIOKdjCB0b1N0cmluZzogRXYkUmVmcmVzaEAxYjZkMzU4NgogICAgZGF0YSBvYmplY3QgUmV0cnkgOiBFdiAgICAgICAgICAgICAgICAvLyDinIUgdG9TdHJpbmc6IFJldHJ5Cn0KZnVuIG1haW4oKSB7CiAgICBwcmludGxuKEV2LlJlZnJlc2gpCiAgICBwcmludGxuKEV2LlJldHJ5KQp9Cg) · Xem thêm: K171

### K173 · `valueOf` với chuỗi lạ

`🟢 mới học` · `Kotlin`

**Gặp:** `java.lang.IllegalArgumentException: No enum constant Currency.usd`.

**Vì sao:** `valueOf` (và `enumValueOf`) chỉ chấp nhận đúng tên hằng, phân biệt hoa thường. Dữ liệu từ server, prefs hay deeplink thì không chắc.

**Sửa:** tự viết hàm tìm trả về `null` (hoặc giá trị mặc định).

```kotlin
Currency.valueOf(s)                                              // ❌
Currency.entries.find { it.name.equals(s, ignoreCase = true) }   // ✅ null khi không có
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE3MzogdmFsdWVPZiB24bubaSBjaHXhu5dpIGtow7RuZyBraOG7m3Ag4oaSIElsbGVnYWxBcmd1bWVudEV4Y2VwdGlvbgplbnVtIGNsYXNzIEN1cnJlbmN5IHsgVk5ELCBVU0QsIEVVUiB9CmZ1biBwYXJzZShzOiBTdHJpbmcpOiBDdXJyZW5jeT8gPSBDdXJyZW5jeS5lbnRyaWVzLmZpbmQgeyBpdC5uYW1lID09IHMudXBwZXJjYXNlKCkgfSAgIC8vIOKchSB0cuG6oyBudWxsCmZ1biBtYWluKCkgewogICAgcHJpbnRsbihwYXJzZSgidXNkIikpCiAgICBwcmludGxuKHBhcnNlKCJqcHkiKSkKICAgIHByaW50bG4oQ3VycmVuY3kudmFsdWVPZigidXNkIikpICAgICAgLy8g4p2MIGNyYXNoOiBwaMOibiBiaeG7h3QgaG9hIHRoxrDhu51uZwp9Cg) · Xem thêm: K175, K176

### K174 · Vẫn dùng `values()` thay vì `entries`

`🟢 mới học` · `Kotlin`

**Gặp:** IDE gợi ý đổi `values()` thành `entries`; code duyệt enum trong vòng lặp nóng tạo rác liên tục.

**Vì sao:** `values()` trả về một **mảng mới** mỗi lần gọi (vì mảng sửa được). `entries` (Kotlin 1.9+) là `List` chỉ đọc dựng sẵn một lần.

**Sửa:** dùng `Enum.entries` ở mọi chỗ; hàm generic thì `enumEntries<T>()`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE3NDogZMO5bmcgZW50cmllcyAoS290bGluIDEuOSspIHRoYXkgdsOsIHZhbHVlcygpCmVudW0gY2xhc3MgVW5pdCB7IEttLCBNaSB9CmZ1biBtYWluKCkgewogICAgcHJpbnRsbihVbml0LnZhbHVlcygpID09PSBVbml0LnZhbHVlcygpKSAgIC8vIGZhbHNlOiB2YWx1ZXMoKSB04bqhbyBt4bqjbmcgTeG7mkkgbeG7l2kgbOG6p24gZ-G7jWkKICAgIHByaW50bG4oVW5pdC5lbnRyaWVzID09PSBVbml0LmVudHJpZXMpICAgICAvLyB0cnVlOiBlbnRyaWVzIGzDoCBsaXN0IGThu7FuZyBz4bq1biwgY2jhu4kgxJHhu41jCiAgICBwcmludGxuKFVuaXQuZW50cmllcy5tYXAgeyBpdC5uYW1lIH0pCn0K)

### K175 · Lưu `ordinal` của enum xuống máy

`🔴 khó, hiếm nhưng đau` · `Kotlin` · `Android`

**Gặp:** sau khi cập nhật app, người dùng chọn "Tối" thì giờ thành "Theo hệ thống"; dữ liệu cũ trong DB/DataStore bị đọc lệch.

**Vì sao:** `ordinal` là vị trí của hằng trong khai báo. Chèn hằng mới vào giữa hoặc đổi thứ tự là mọi số đã lưu chỉ sai chỗ, không có lỗi nào.

**Sửa:** không lưu `ordinal`. Lưu `name`, hoặc tốt hơn là một mã cố định riêng (K176).

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE3NTogbMawdSBvcmRpbmFsIHh14buRbmcgbcOheSwgYuG6o24gc2F1IGNow6huIGjhurFuZyB2w6BvIGdp4buvYSDihpIgxJHhu41jIHJhIHNhaQplbnVtIGNsYXNzIFRoZW1lVjEgeyBMaWdodCwgRGFyayB9CmVudW0gY2xhc3MgVGhlbWVWMiB7IExpZ2h0LCBTeXN0ZW0sIERhcmsgfSAgIC8vIGLhuqNuIGPhuq1wIG5o4bqtdCB0aMOqbSBTeXN0ZW0gdsOgbyBnaeG7r2EKZnVuIG1haW4oKSB7CiAgICB2YWwgc2F2ZWQgPSBUaGVtZVYxLkRhcmsub3JkaW5hbCAgICAgICAgICAgICAgLy8g4p2MIGzGsHUgc-G7kSAxCiAgICBwcmludGxuKCJC4bqjbiAxIGzGsHU6ICRzYXZlZCIpCiAgICBwcmludGxuKCJC4bqjbiAyIMSR4buNYzogJHtUaGVtZVYyLmVudHJpZXNbc2F2ZWRdfSIpICAgLy8gU3lzdGVtLCBraMO0bmcgcGjhuqNpIERhcmsKICAgIC8vIOKchSBsxrB1IG5hbWUgKCJEYXJrIiksIGhv4bq3YyB04buRdCBoxqFuOiBt4buZdCBtw6MgY-G7kSDEkeG7i25oIHJpw6puZyAoeGVtIEsxNzYpCn0K) · Xem thêm: K176, [Lưu dữ liệu](14-persistence.md)

### K176 · Lưu `name` rồi đổi tên hằng

`🟡 đã làm app` · `Kotlin`

**Gặp:** đổi `System` thành `FollowSystem` cho rõ nghĩa, người dùng cũ mở app bị crash ở `valueOf` hoặc mất cài đặt.

**Vì sao:** `name` gắn chặt với tên trong code. Đổi tên là phá dữ liệu đã lưu (và JSON đã gửi lên server).

**Sửa:** cho enum một property mã cố định và hàm đọc an toàn. Với kotlinx.serialization, `@SerialName("system")` trên hằng làm việc tương tự cho JSON.

```kotlin
enum class Theme(val code: String) {
    Light("light"), Dark("dark"), FollowSystem("system");
    companion object { fun fromCode(c: String?) = entries.find { it.code == c } ?: Light }
}
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE3NjogbMawdSBtw6MgY-G7kSDEkeG7i25oIHRoYXkgdsOsIG5hbWUvb3JkaW5hbDogxJHhu5VpIHTDqm4gaOG6sW5nIGtow7RuZyBsw6BtIGjhu49uZyBk4buvIGxp4buHdSBjxakKZW51bSBjbGFzcyBUaGVtZSh2YWwgY29kZTogU3RyaW5nKSB7CiAgICBMaWdodCgibGlnaHQiKSwgRGFyaygiZGFyayIpLCBGb2xsb3dTeXN0ZW0oInN5c3RlbSIpOyAgIC8vIMSR4buVaSB0w6puIFN5c3RlbSDihpIgRm9sbG93U3lzdGVtIHbhuqtuIMSR4buNYyDEkcaw4bujYwogICAgY29tcGFuaW9uIG9iamVjdCB7CiAgICAgICAgZnVuIGZyb21Db2RlKGNvZGU6IFN0cmluZz8pID0gZW50cmllcy5maW5kIHsgaXQuY29kZSA9PSBjb2RlIH0gPzogTGlnaHQKICAgIH0KfQpmdW4gbWFpbigpIHsKICAgIHZhbCBzYXZlZCA9ICJzeXN0ZW0iICAgICAgICAgICAgICAgICAgICAvLyBk4buvIGxp4buHdSB04burIGLhuqNuIGPFqQogICAgcHJpbnRsbihUaGVtZS5mcm9tQ29kZShzYXZlZCkpCiAgICBwcmludGxuKFRoZW1lLmZyb21Db2RlKCJs4bqhIikpICAgICAgICAgICAvLyBtw6Mga2jDtG5nIGJp4bq_dCDihpIgZ2nDoSB0cuG7iyBt4bq3YyDEkeG7i25oLCBraMO0bmcgY3Jhc2gKfQo) · Xem thêm: K173, K175

### K177 · Enum có hàm mà thiếu dấu `;`

`🟢 mới học` · `Kotlin`

**Gặp:** `Syntax error: Expecting ';' after the last enum entry or '}' to close enum class body.`

**Vì sao:** khi enum có thêm thân (hàm, property, companion), danh sách hằng phải kết thúc bằng `;`. Đây là chỗ hiếm hoi Kotlin bắt buộc dấu chấm phẩy.

**Sửa:** `Rpm(0x0C), Speed(0x0D);` rồi mới tới `fun hex() = ...`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE3NzogZW51bSBjw7MgaMOgbS90aMOibiBtw6AgdGhp4bq_dSBk4bqldSA7IHNhdSBo4bqxbmcgY3Xhu5FpCmVudW0gY2xhc3MgUGlkKHZhbCBjb2RlOiBJbnQpIHsKICAgIFJwbSgweDBDKSwgU3BlZWQoMHgwRCkgICAgICAgICAgICAgICAgICAvLyDinYwgdGhp4bq_dSA7CiAgICBmdW4gaGV4KCkgPSAiJTAyWCIuZm9ybWF0KGNvZGUpCn0KZnVuIG1haW4oKSA9IHByaW50bG4oUGlkLlJwbS5oZXgoKSkK)

### K178 · `var` trong enum là state toàn cục

`🟡 đã làm app` · `Kotlin`

**Gặp:** mở khoá màn chơi cho tài khoản A, đăng xuất, tài khoản B cũng thấy đã mở; test này làm hỏng test kia.

**Vì sao:** mỗi hằng enum là một singleton. `var` trong đó giống `var` trong `object` (K163): một bản cho cả process.

**Sửa:** enum chỉ chứa `val` (dữ liệu cố định). State đổi được lưu chỗ khác, ví dụ `val unlocked: Set<Level>` trong state của người chơi.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE3ODogZW51bSBsw6Agc2luZ2xldG9uOiBgdmFyYCB0cm9uZyBlbnVtIGzDoCBzdGF0ZSB0b8OgbiBj4bulYwplbnVtIGNsYXNzIExldmVsKHZhciB1bmxvY2tlZDogQm9vbGVhbikgeyBFYXN5KHRydWUpLCBIYXJkKGZhbHNlKSB9ICAgLy8g4p2MIHZhciB0cm9uZyBlbnVtCmZ1biBtYWluKCkgewogICAgTGV2ZWwuSGFyZC51bmxvY2tlZCA9IHRydWUgICAgICAgICAgICAvLyBuZ8aw4budaSBjaMahaSBBIG3hu58ga2hvw6EKICAgIC8vIC4uLiDhu58gY2jhu5cga2jDoWMsICJuZ8aw4budaSBjaMahaSBCIiBt4bubaSB2w6BvOgogICAgcHJpbnRsbihMZXZlbC5IYXJkLnVubG9ja2VkKSAgICAgICAgICAvLyB0cnVlIOKAlCBkw61uaCBzdGF0ZSBj4bunYSBBCiAgICAvLyDinIUgZW51bSBjaOG7iSBjaOG7qWEgdmFsOyB0aeG6v24gxJHhu5kgbMawdSByacOqbmc6IHZhbCB1bmxvY2tlZDogU2V0PExldmVsPgp9Cg) · Xem thêm: K163

### K179 · Dùng `enum` khi từng trường hợp cần dữ liệu riêng

`🟡 đã làm app` · `Kotlin`

**Gặp:** enum `Result { Ok, Error }` cộng thêm các biến `errorMessage: String?`, `rate: Double?` nằm bên cạnh, và nhiều chỗ phải `!!`.

**Vì sao:** hằng enum là cố định, không chở được dữ liệu khác nhau cho mỗi lần dùng. Dữ liệu bị đẩy ra ngoài thành các field nullable,
và compiler không biết field nào có giá trị khi nào.

**Sửa:** dùng `sealed interface`/`sealed class`: mỗi trường hợp là một kiểu, mang đúng dữ liệu của nó. `when` vẫn bắt đủ nhánh như enum.
Enum vẫn hợp với tập giá trị cố định không kèm dữ liệu (đơn vị đo, tab, tiền tệ).

```kotlin
sealed interface Result {
    data class Ok(val rate: Double) : Result
    data class Error(val message: String) : Result
}
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE3OTogZW51bSBraMO0bmcgY2jhu58gxJHGsOG7o2MgZOG7ryBsaeG7h3UgcmnDqm5nIGNobyB04burbmcgbOG6p247IHNlYWxlZCB0aMOsIMSRxrDhu6NjCmVudW0gY2xhc3MgUmVzdWx0RSB7IE9rLCBFcnJvciB9ICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIEVycm9yIGtow7RuZyBtYW5nIMSRxrDhu6NjIG1lc3NhZ2UKc2VhbGVkIGludGVyZmFjZSBSZXN1bHQgeyAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinIUKICAgIGRhdGEgY2xhc3MgT2sodmFsIHJhdGU6IERvdWJsZSkgOiBSZXN1bHQKICAgIGRhdGEgY2xhc3MgRXJyb3IodmFsIG1lc3NhZ2U6IFN0cmluZykgOiBSZXN1bHQKfQpmdW4gc2hvdyhyOiBSZXN1bHQpID0gd2hlbiAocikgewogICAgaXMgUmVzdWx0Lk9rIC0-ICIxIFVTRCA9ICR7ci5yYXRlfSIKICAgIGlzIFJlc3VsdC5FcnJvciAtPiAiTOG7l2k6ICR7ci5tZXNzYWdlfSIKfQpmdW4gbWFpbigpIHsKICAgIHByaW50bG4oc2hvdyhSZXN1bHQuT2soMjZfMzEyLjUpKSkKICAgIHByaW50bG4oc2hvdyhSZXN1bHQuRXJyb3IoInVuc3VwcG9ydGVkLWNvZGUiKSkpCn0K) · Xem thêm: K166

### K180 · Property có initializer không tự cập nhật

`🟢 mới học` · `Kotlin`

**Gặp:** `cart.total` luôn là `0` dù đã thêm hàng vào `cart.items`.

**Vì sao:** `val total = items.sum()` tính **một lần** lúc tạo object và lưu lại. Nó không theo dõi `items`.

**Sửa:** getter tính mỗi lần đọc: `val total get() = items.sum()`. Cần tính một lần nhưng lười thì `by lazy { }` (vẫn chỉ một lần).

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE4MDogcHJvcGVydHkgY8OzIGluaXRpYWxpemVyIGNo4buJIHTDrW5oIE3hu5hUIGzhuqduIGzDumMgdOG6oW8gb2JqZWN0OyBtdeG7kW4gdMOtbmggbOG6oWkgdGjDrCBkw7luZyBnZXR0ZXIKY2xhc3MgQ2FydCB7CiAgICB2YWwgaXRlbXMgPSBtdXRhYmxlTGlzdE9mPEludD4oKQogICAgdmFsIHRvdGFsID0gaXRlbXMuc3VtKCkgICAgICAgICAgICAgICAvLyDinYwgdMOtbmggbMO6YyB04bqhbyBDYXJ0LCBtw6NpIGzDoCAwCiAgICB2YWwgdG90YWwyIGdldCgpID0gaXRlbXMuc3VtKCkgICAgICAgIC8vIOKchSB0w61uaCBt4buXaSBs4bqnbiDEkeG7jWMKfQpmdW4gbWFpbigpIHsKICAgIHZhbCBjID0gQ2FydCgpCiAgICBjLml0ZW1zICs9IGxpc3RPZigxMCwgMjApCiAgICBwcmludGxuKCJ0b3RhbD0ke2MudG90YWx9IHRvdGFsMj0ke2MudG90YWwyfSIpCn0K) · Xem thêm: K160

### K181 · Setter gán vào chính nó → `StackOverflowError`

`🟡 đã làm app` · `Kotlin`

**Gặp:** `java.lang.StackOverflowError` ngay khi gán property có setter tự viết.

**Vì sao:** trong `set(value) { name = value }`, `name = ...` lại gọi setter, đệ quy vô tận.

**Sửa:** trong getter/setter, dùng `field` để đọc/ghi giá trị thật.

```kotlin
var name = ""
    set(value) { name = value.trim() }    // ❌
    set(value) { field = value.trim() }   // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE4MTogc2V0dGVyIHThu7EgZ8OhbiB2w6BvIGNow61uaCBwcm9wZXJ0eSDihpIgZ-G7jWkgbOG6oWkgc2V0dGVyIG3Do2kg4oaSIFN0YWNrT3ZlcmZsb3dFcnJvcgpjbGFzcyBQcm9maWxlIHsKICAgIHZhciBuYW1lOiBTdHJpbmcgPSAiIgogICAgICAgIHNldCh2YWx1ZSkgeyBuYW1lID0gdmFsdWUudHJpbSgpIH0gICAgICAgIC8vIOKdjCDinIUgxJHhu5VpIHRow6BuaDogZmllbGQgPSB2YWx1ZS50cmltKCkKfQpmdW4gbWFpbigpIHsKICAgIHZhbCBwID0gUHJvZmlsZSgpCiAgICBwLm5hbWUgPSAiICBUw7ogIgogICAgcHJpbnRsbigiWyR7cC5uYW1lfV0iKQp9Cg)

### K182 · `Initializer is prohibited here because this property has no backing field`

`🟢 mới học` · `Kotlin`

**Gặp:** `Initializer is prohibited here because this property has no backing field.`

**Vì sao:** getter tự viết không dùng `field` thì property không có chỗ lưu, nên `= 0` không có ý nghĩa.

**Sửa:** bỏ initializer: `val area: Int get() = w * h`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE4MjogcHJvcGVydHkgY2jhu4kgY8OzIGdldHRlciAoa2jDtG5nIGJhY2tpbmcgZmllbGQpIG3DoCBs4bqhaSBnw6FuIGluaXRpYWxpemVyCmNsYXNzIEJveCh2YWwgdzogSW50LCB2YWwgaDogSW50KSB7CiAgICB2YWwgYXJlYTogSW50ID0gMCAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBjb21waWxlIGVycm9yCiAgICAgICAgZ2V0KCkgPSB3ICogaAogICAgLy8g4pyFIHZhbCBhcmVhOiBJbnQgZ2V0KCkgPSB3ICogaAp9CmZ1biBtYWluKCkgPSBwcmludGxuKEJveCgyLCAzKS5hcmVhKQo) · Xem thêm: K180

### K183 · `private set` mà vẫn sửa được list từ ngoài

`🟡 đã làm app` · `Kotlin`

**Gặp:** đã `private set` cho `var songs: MutableList<String>` mà màn hình khác vẫn `p.songs.add(...)` được.

**Vì sao:** `private set` chỉ chặn **gán lại** property. Nội dung của object `MutableList` thì ai có tham chiếu cũng sửa được.

**Sửa:** giữ `MutableList` private, chỉ đưa ra ngoài `List` (bản copy hoặc qua `StateFlow`).

```kotlin
private val _songs = mutableListOf<String>()
val songs: List<String> get() = _songs.toList()   // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE4MzogYHByaXZhdGUgc2V0YCBjaOG7iSBjaOG6t24gR8OBTiBs4bqhaSwga2jDtG5nIGNo4bq3biBz4butYSBu4buZaSBkdW5nIE11dGFibGVMaXN0CmNsYXNzIFBsYXlsaXN0IHsKICAgIHZhciBzb25nczogTXV0YWJsZUxpc3Q8U3RyaW5nPiA9IG11dGFibGVMaXN0T2YoKQogICAgICAgIHByaXZhdGUgc2V0ICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgdMaw4bufbmcgbMOgIMSRw6Mga2hvw6EKfQovLyDinIUgcHJpdmF0ZSB2YWwgX3NvbmdzID0gbXV0YWJsZUxpc3RPZjxTdHJpbmc-KCk7IHZhbCBzb25nczogTGlzdDxTdHJpbmc-IGdldCgpID0gX3NvbmdzLnRvTGlzdCgpCmZ1biBtYWluKCkgewogICAgdmFsIHAgPSBQbGF5bGlzdCgpCiAgICBwLnNvbmdzLmFkZCgiYsOgaSBs4bqhIikgICAgICAgICAgICAgICAgICAgICAgIC8vIHbhuqtuIHPhu61hIMSRxrDhu6NjIHThu6sgbmdvw6BpCiAgICBwcmludGxuKHAuc29uZ3MpCn0K) · Xem thêm: [K103](03-collections.md)

### K184 · Lớp con quên implement thành viên `abstract`

`🟢 mới học` · `Kotlin`

**Gặp:** `Class 'Thermo' is not abstract and does not implement abstract base class member: fun read(): Double`
(với interface: `... does not implement abstract member ...`).

**Vì sao:** lớp con cụ thể phải có thân cho mọi hàm/property `abstract` của lớp cha và các interface.

**Sửa:** trong Android Studio đặt con trỏ vào tên class, `Alt+Enter` → **Implement members**. Hoặc khai báo lớp con là `abstract` nếu nó cũng chỉ là khung.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE4NDogY2xhc3MgY29uIHF1w6puIGltcGxlbWVudCB0aMOgbmggdmnDqm4gYWJzdHJhY3QKYWJzdHJhY3QgY2xhc3MgU2Vuc29yIHsgYWJzdHJhY3QgZnVuIHJlYWQoKTogRG91YmxlIH0KY2xhc3MgVGhlcm1vIDogU2Vuc29yKCkgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIGNvbXBpbGUgZXJyb3IKLy8g4pyFIGNsYXNzIFRoZXJtbyA6IFNlbnNvcigpIHsgb3ZlcnJpZGUgZnVuIHJlYWQoKSA9IDI1LjAgfQpmdW4gbWFpbigpID0gcHJpbnRsbihUaGVybW8oKS5yZWFkKCkpCg)

### K185 · Interface không có state

`🟢 mới học` · `Kotlin`

**Gặp:** `Property initializers in interfaces are prohibited.`

**Vì sao:** interface không có backing field. Nó có thể có hàm có thân và property có getter, nhưng không lưu giá trị.

**Sửa:** `val name: String get() = "?"` (giá trị mặc định qua getter), hoặc để abstract cho class implement tự lưu. Cần state dùng chung thì
dùng `abstract class`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE4NTogaW50ZXJmYWNlIGtow7RuZyBnaeG7ryBzdGF0ZTogcHJvcGVydHkga2jDtG5nIMSRxrDhu6NjIGPDsyBpbml0aWFsaXplcgppbnRlcmZhY2UgTmFtZWQgewogICAgdmFsIG5hbWU6IFN0cmluZyA9ICI_IiAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBjb21waWxlIGVycm9yCiAgICAvLyDinIUgdmFsIG5hbWU6IFN0cmluZyBnZXQoKSA9ICI_IiAgIChnZXR0ZXIgbeG6t2MgxJHhu4tuaCksIGhv4bq3YyDEkeG7gyBhYnN0cmFjdDogdmFsIG5hbWU6IFN0cmluZwp9CmZ1biBtYWluKCkgPSBwcmludGxuKG9iamVjdCA6IE5hbWVkIHt9Lm5hbWUpCg)

### K186 · Hai interface có cùng hàm mặc định

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Class 'Service' must override 'hello' because it inherits multiple interface methods for it.`

**Vì sao:** compiler không đoán bạn muốn bản nào.

**Sửa:** override và chọn rõ bằng `super<Tên>`.

```kotlin
class Service : Logger, Tracker {
    override fun hello() = super<Logger>.hello()
}
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE4NjogaGFpIGludGVyZmFjZSBjw7MgY8O5bmcgaMOgbSBt4bq3YyDEkeG7i25oIOKGkiBjbGFzcyBwaOG6o2kgdOG7sSBjaOG7jW4KaW50ZXJmYWNlIExvZ2dlciB7IGZ1biBoZWxsbygpID0gImxvZyIgfQppbnRlcmZhY2UgVHJhY2tlciB7IGZ1biBoZWxsbygpID0gInRyYWNrIiB9CmNsYXNzIFNlcnZpY2UgOiBMb2dnZXIsIFRyYWNrZXIgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBjb21waWxlIGVycm9yCi8vIOKchSBjbGFzcyBTZXJ2aWNlIDogTG9nZ2VyLCBUcmFja2VyIHsgb3ZlcnJpZGUgZnVuIGhlbGxvKCkgPSBzdXBlcjxMb2dnZXI-LmhlbGxvKCkgKyAiKyIgKyBzdXBlcjxUcmFja2VyPi5oZWxsbygpIH0KZnVuIG1haW4oKSA9IHByaW50bG4oU2VydmljZSgpLmhlbGxvKCkpCg)

### K187 · `This type has a constructor, so it must be initialized here.`

`🟢 mới học` · `Kotlin`

**Gặp:** `This type has a constructor, so it must be initialized here.` (kèm `No value passed for parameter 'tag'.`) ở `class Child : Base`.

**Vì sao:** kế thừa **class** phải gọi constructor của nó, kể cả khi không có tham số: `: Base()`. Interface thì không có constructor nên không có ngoặc.

**Sửa:** `class Child : Base("child")`. Lớp con có constructor phụ thì mỗi constructor gọi `super(...)`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE4Nzoga-G6vyB0aOG7q2EgY2xhc3MgY8OzIGNvbnN0cnVjdG9yIHBo4bqjaSBn4buNaSBjb25zdHJ1Y3RvciDEkcOzCm9wZW4gY2xhc3MgQmFzZSh2YWwgdGFnOiBTdHJpbmcpCmNsYXNzIENoaWxkIDogQmFzZSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinYwgY29tcGlsZSBlcnJvcgovLyDinIUgY2xhc3MgQ2hpbGQgOiBCYXNlKCJjaGlsZCIpCmZ1biBtYWluKCkgPSBwcmludGxuKENoaWxkKCkudGFnKQo)

### K188 · Class lồng không thấy thành viên của class ngoài

`🟡 đã làm app` · `Kotlin` · `Android`

**Gặp:** `Outer class 'class Outer : Any' of non-inner class cannot be used as receiver.` (hoặc `Unresolved reference`).

**Vì sao:** class khai báo trong class khác mặc định là **nested** (như `static` trong Java), không giữ tham chiếu tới object bên ngoài.

**Sửa:** thêm `inner` nếu thật sự cần. Trên Android cẩn thận: `inner class` (và object ẩn danh, lambda) giữ tham chiếu tới Activity/Fragment
bên ngoài, sống lâu hơn là leak. Thường tốt hơn là truyền đúng giá trị cần vào constructor của class lồng.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE4ODogY2xhc3MgbOG7k25nIGLDqm4gdHJvbmcgKG5lc3RlZCkga2jDtG5nIHRo4bqleSB0aMOgbmggdmnDqm4gY-G7p2EgY2xhc3Mgbmdvw6BpCmNsYXNzIE91dGVyIHsKICAgIHZhbCBuYW1lID0gIm91dGVyIgogICAgY2xhc3MgTmVzdGVkIHsgZnVuIHNob3coKSA9IG5hbWUgfSAgICAgICAgICAgICAgICAgLy8g4p2MIGNvbXBpbGUgZXJyb3IKICAgIC8vIOKchSBpbm5lciBjbGFzcyBOZXN0ZWQgeyBmdW4gc2hvdygpID0gbmFtZSB9ICAobmjGsG5nIGlubmVyIGdp4buvIHRoYW0gY2hp4bq_dSB04bubaSBPdXRlciwgY-G6qW4gdGjhuq1uIGxlYWspCn0KZnVuIG1haW4oKSA9IHByaW50bG4oT3V0ZXIuTmVzdGVkKCkuc2hvdygpKQo) · Xem thêm: [Android: lifecycle](11-android.md)

### K189 · `value class` có hai property

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Value class must have exactly one primary constructor parameter.` Quên `@JvmInline` trên JVM thì gặp lỗi đòi annotation đó.

**Vì sao:** `value class` được compiler "bóc vỏ" thành chính giá trị bên trong lúc chạy, nên chỉ chứa được **một** giá trị.

**Sửa:** một giá trị thì `@JvmInline value class Cents(val value: Long)`; nhiều giá trị thì `data class`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE4OTogdmFsdWUgY2xhc3MgY2jhu4kgxJHGsOG7o2MgxJHDum5nIE3hu5hUIHByb3BlcnR5IHRyb25nIGNvbnN0cnVjdG9yIGNow61uaApASnZtSW5saW5lCnZhbHVlIGNsYXNzIE1vbmV5KHZhbCBhbW91bnQ6IExvbmcsIHZhbCBjdXJyZW5jeTogU3RyaW5nKSAgIC8vIOKdjCBjb21waWxlIGVycm9yCi8vIOKchSBASnZtSW5saW5lIHZhbHVlIGNsYXNzIENlbnRzKHZhbCB2YWx1ZTogTG9uZyk7ICBuaGnhu4F1IGZpZWxkIHRow6wgZMO5bmcgZGF0YSBjbGFzcwpmdW4gbWFpbigpID0gcHJpbnRsbihNb25leSgxLCAiVk5EIikpCg) · Xem thêm: K190

### K190 · `typealias` không chặn được truyền nhầm

`🟡 đã làm app` · `Kotlin`

**Gặp:** truyền `userId` vào hàm cần `orderId`, compile bình thường, chạy ra kết quả sai.

**Vì sao:** `typealias UserId = String` chỉ là tên gọi khác của `String`. Mọi `String` đều hợp.

**Sửa:** khi cần compiler phân biệt, dùng `@JvmInline value class OrderId(val raw: String)`: kiểu riêng, gần như không tốn thêm bộ nhớ.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE5MDogdHlwZWFsaWFzIGNo4buJIGzDoCB0w6puIGtow6FjLCBraMO0bmcgY2jhurduIMSRxrDhu6NjIHRydXnhu4FuIG5o4bqnbQp0eXBlYWxpYXMgVXNlcklkID0gU3RyaW5nCnR5cGVhbGlhcyBPcmRlcklkID0gU3RyaW5nCmZ1biBjYW5jZWwob3JkZXI6IE9yZGVySWQpID0gcHJpbnRsbigiaHXhu7cgxJHGoW4gJG9yZGVyIikKQEp2bUlubGluZSB2YWx1ZSBjbGFzcyBPcmRlcklkMih2YWwgcmF3OiBTdHJpbmcpICAgICAvLyDinIUga2nhu4N1IHJpw6puZywgY29tcGlsZXIgY2jhurduIG5o4bqnbQpmdW4gY2FuY2VsMihvcmRlcjogT3JkZXJJZDIpID0gcHJpbnRsbigiaHXhu7cgxJHGoW4gJHtvcmRlci5yYXd9IikKZnVuIG1haW4oKSB7CiAgICB2YWwgbWU6IFVzZXJJZCA9ICJ1c2VyLTQyIgogICAgY2FuY2VsKG1lKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBjb21waWxlIMSRxrDhu6NjLCBodeG7tyBuaOG6p20KICAgIC8vIGNhbmNlbDIobWUpICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinIUgYuG7jyBjb21tZW50OiBjb21waWxlIGVycm9yLCBuaOG6p20gYuG7iyBi4bqvdAogICAgY2FuY2VsMihPcmRlcklkMigib3JkZXItNyIpKQp9Cg) · Xem thêm: K189

### K191 · `toString` của `data class` in lộ mật khẩu, token

`🟡 đã làm app` · `Kotlin` · `Android`

**Gặp:** log, Crashlytics, thông báo lỗi chứa nguyên `password=hunter2` hay `token=eyJ...`.

**Vì sao:** `toString()` sinh ra in **mọi** property trong constructor. Ai log cả object (hoặc exception có object trong message) là lộ.

**Sửa:** override `toString()` che field nhạy cảm, hoặc tách dữ liệu nhạy cảm ra khỏi `data class` hay dùng để log.

```kotlin
data class Login(val email: String, val password: String) {
    override fun toString() = "Login(email=$email, password=***)"   // ✅
}
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE5MTogdG9TdHJpbmcgY-G7p2EgZGF0YSBjbGFzcyBpbiBo4bq_dCBmaWVsZCwga-G7gyBj4bqjIG3huq10IGto4bqpdSAvIHRva2VuCmRhdGEgY2xhc3MgTG9naW4odmFsIGVtYWlsOiBTdHJpbmcsIHZhbCBwYXNzd29yZDogU3RyaW5nKSB7CiAgICAvLyDinIUgb3ZlcnJpZGUgZnVuIHRvU3RyaW5nKCkgPSAiTG9naW4oZW1haWw9JGVtYWlsLCBwYXNzd29yZD0qKiopIgp9CmZ1biBtYWluKCkgewogICAgdmFsIHJlcSA9IExvZ2luKCJ0dUB4LnZuIiwgImh1bnRlcjIiKQogICAgcHJpbnRsbigiZ-G7rWkgJHJlcSIpICAgICAgICAgICAgICAgICAgICAvLyDinYwgcGFzc3dvcmQgbOG7jXQgdsOgbyBsb2cgLyBjcmFzaCByZXBvcnQKfQo)

### K192 · Destructuring theo vị trí gán nhầm sau khi đổi thứ tự field

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** sau một lần refactor, màn hình hiện `1 VND = 26312.5 USD` thay vì ngược lại. Không có lỗi compile.

**Vì sao:** `val (from, to, value) = rate` gọi `component1()`, `component2()`, `component3()` theo **thứ tự field** trong constructor.
Tên biến bên trái không liên quan gì tới tên property.

**Sửa:** với `data class` có nhiều field cùng kiểu, đọc theo tên (`rate.from`, `rate.to`). Chỉ destructuring với kiểu ngắn, thứ tự hiển nhiên
(`Pair`, `Map.Entry`, `(index, value)`).

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE5MjogZGVzdHJ1Y3R1cmluZyB0aGVvIFbhu4ogVFLDjTogxJHhu5VpIHRo4bupIHThu7EgZmllbGQgbMOgIGfDoW4gbmjhuqdtLCBraMO0bmcgbOG7l2kgY29tcGlsZQovLyBC4bqjbiBjxak6IGRhdGEgY2xhc3MgUmF0ZSh2YWwgZnJvbTogU3RyaW5nLCB2YWwgdG86IFN0cmluZywgdmFsIHZhbHVlOiBEb3VibGUpCmRhdGEgY2xhc3MgUmF0ZSh2YWwgdG86IFN0cmluZywgdmFsIGZyb206IFN0cmluZywgdmFsIHZhbHVlOiBEb3VibGUpICAgLy8gYWkgxJHDsyDEkeG6o28gMiBmaWVsZApmdW4gbWFpbigpIHsKICAgIHZhbCAoZnJvbSwgdG8sIHZhbHVlKSA9IFJhdGUodG8gPSAiVk5EIiwgZnJvbSA9ICJVU0QiLCB2YWx1ZSA9IDI2XzMxMi41KSAgIC8vIOKdjCB0w6puIGJp4bq_biBraMO0bmcgcXV54bq_dCDEkeG7i25oIGfDrAogICAgcHJpbnRsbigiMSAkZnJvbSA9ICR2YWx1ZSAkdG8iKSAgICAgICAgICAgICAgLy8gMSBWTkQgPSAyNjMxMi41IFVTRAogICAgdmFsIHIgPSBSYXRlKHRvID0gIlZORCIsIGZyb20gPSAiVVNEIiwgdmFsdWUgPSAyNl8zMTIuNSkKICAgIHByaW50bG4oIjEgJHtyLmZyb219ID0gJHtyLnZhbHVlfSAke3IudG99IikgIC8vIOKchSDEkeG7jWMgdGhlbyB0w6puCn0K)

### K193 · `copy()` lách constructor `private` của `data class`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** Kotlin 2.4 báo ngay ở khai báo: `Non-public primary constructor is exposed via the generated 'copy()' method of the 'data' class.`
Gọi `copy()` từ ngoài thì gặp `This 'copy()' exposes the non-public primary constructor of a 'data class'.` Ở bản Kotlin cũ hơn chỉ là
cảnh báo (hoặc không có gì), và `copy(value = 999)` tạo được giá trị mà hàm `of()` đã cố chặn.

**Vì sao:** trước đây `copy()` luôn public, bất kể constructor là `private`. Kotlin đang chuyển dần để `copy()` có cùng mức truy cập với constructor.

**Sửa:** thêm `@ConsistentCopyVisibility` cho class (khi đó `copy()` cũng `private`). Hoặc kiểm tra dữ liệu trong `init { require(...) }`,
vì `copy()` cũng chạy qua constructor chính.

```kotlin
@ConsistentCopyVisibility
data class Percent private constructor(val value: Int) { ... }
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE5MzogZGF0YSBjbGFzcyBjw7MgY29uc3RydWN0b3IgcHJpdmF0ZSwgbmjGsG5nIGNvcHkoKSBzaW5oIHJhIGzhuqFpIGzDoCBwdWJsaWMg4oaSIGzDoWNoIMSRxrDhu6NjIGtp4buDbSB0cmEKLy8gS290bGluIDIuNCBiw6FvIGzhu5dpIG5nYXkg4bufIGtoYWkgYsOhbyBiw6puIGTGsOG7m2kuCi8vIOKchSBi4buPIGNvbW1lbnQgZMOybmcgQENvbnNpc3RlbnRDb3B5VmlzaWJpbGl0eTogY29weSgpIHRow6BuaCBwcml2YXRlIG5oxrAgY29uc3RydWN0b3IKLy8gQENvbnNpc3RlbnRDb3B5VmlzaWJpbGl0eQpkYXRhIGNsYXNzIFBlcmNlbnQgcHJpdmF0ZSBjb25zdHJ1Y3Rvcih2YWwgdmFsdWU6IEludCkgewogICAgY29tcGFuaW9uIG9iamVjdCB7CiAgICAgICAgZnVuIG9mKHY6IEludCkgPSBQZXJjZW50KHYuY29lcmNlSW4oMCwgMTAwKSkgICAvLyBjaOG7iSBjaG8gdOG6oW8gZ2nDoSB0cuG7iyAwLi4xMDAKICAgIH0KfQpmdW4gbWFpbigpIHsKICAgIHZhbCBwID0gUGVyY2VudC5vZigxNTApCiAgICBwcmludGxuKHApICAgICAgICAgICAgICAgICAgICAgICAgIC8vIFBlcmNlbnQodmFsdWU9MTAwKQogICAgLy8gcHJpbnRsbihwLmNvcHkodmFsdWUgPSA5OTkpKSAgICAvLyBu4bq_dSBjb3B5KCkgcHVibGljIHRow6wgdOG6oW8gxJHGsOG7o2MgUGVyY2VudCg5OTkpCn0K) · Xem thêm: K198

### K194 · `const val` với `List`

`🟢 mới học` · `Kotlin`

**Gặp:** `Const 'val' has type 'List<String>'. Only primitive types and 'String' are allowed.`

**Vì sao:** `const` là hằng lúc compile, được chép thẳng vào chỗ dùng. Chỉ số, `Boolean`, `Char` và `String` làm được việc đó.

**Sửa:** bỏ `const`: `val CURRENCIES = listOf("VND", "USD")`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE5NDogY29uc3QgdmFsIGNo4buJIGNobyBraeG7g3Ugbmd1ecOqbiB0aHXhu7cgdsOgIFN0cmluZwpvYmplY3QgQ29uZmlnIHsKICAgIGNvbnN0IHZhbCBUSU1FT1VUX01TID0gMTBfMDAwICAgICAgICAgICAgICAgIC8vIOKchQogICAgY29uc3QgdmFsIENVUlJFTkNJRVMgPSBsaXN0T2YoIlZORCIsICJVU0QiKSAgLy8g4p2MIGNvbXBpbGUgZXJyb3IKICAgIC8vIOKchSB2YWwgQ1VSUkVOQ0lFUyA9IGxpc3RPZigiVk5EIiwgIlVTRCIpCn0KZnVuIG1haW4oKSA9IHByaW50bG4oQ29uZmlnLkNVUlJFTkNJRVMpCg) · Xem thêm: K195

### K195 · `const val` trong class thường

`🟢 mới học` · `Kotlin`

**Gặp:** `Const 'val' is only allowed on top level, in named objects, in companion objects or companion blocks.`

**Vì sao:** `const` không thuộc về từng instance. Nó chỉ sống ở chỗ có đúng một bản: top-level, `object`, `companion object`.

**Sửa:** chuyển vào `companion object { const val BASE = "..." }` hoặc lên top-level của file.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE5NTogY29uc3QgdmFsIGtow7RuZyDEkeG6t3QgxJHGsOG7o2MgdHJvbmcgY2xhc3MgdGjGsOG7nW5nCmNsYXNzIEFwaSB7CiAgICBjb25zdCB2YWwgQkFTRSA9ICJodHRwczovL29wZW4uZXItYXBpLmNvbSIgICAvLyDinYwgY29tcGlsZSBlcnJvcgp9Ci8vIOKchSDEkeG6t3Qg4bufIHRvcC1sZXZlbCwgdHJvbmcgb2JqZWN0LCBob-G6t2MgdHJvbmcgY29tcGFuaW9uIG9iamVjdApmdW4gbWFpbigpID0gcHJpbnRsbihBcGkoKS5CQVNFKQo) · Xem thêm: K194

### K196 · Viết `equals(other: Point)` thành overload, không phải override

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** gọi `a.equals(b)` ra `true` nhưng `a == b` và `list.contains(b)` ra `false`.

**Vì sao:** `==` luôn gọi `equals(other: Any?)`. Hàm `equals(other: Point)` là một hàm **khác** (overload), không ai gọi trừ bạn.

**Sửa:** override đúng chữ ký `override fun equals(other: Any?): Boolean`, cùng với `hashCode()`. Hoặc dùng `data class`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE5Njogdmnhur90IGVxdWFscyhvdGhlcjogUG9pbnQpIGzDoCBPVkVSTE9BRCwga2jDtG5nIHBo4bqjaSBvdmVycmlkZSDihpIgPT0ga2jDtG5nIGTDuW5nIG7DswpjbGFzcyBQb2ludCh2YWwgeDogSW50LCB2YWwgeTogSW50KSB7CiAgICBmdW4gZXF1YWxzKG90aGVyOiBQb2ludCkgPSB4ID09IG90aGVyLnggJiYgeSA9PSBvdGhlci55ICAgICAvLyDinYwKICAgIC8vIOKchSBvdmVycmlkZSBmdW4gZXF1YWxzKG90aGVyOiBBbnk_KSA9IG90aGVyIGlzIFBvaW50ICYmIG90aGVyLnggPT0geCAmJiBvdGhlci55ID09IHkgICgrIGhhc2hDb2RlKQp9CmZ1biBtYWluKCkgewogICAgcHJpbnRsbihQb2ludCgxLCAyKS5lcXVhbHMoUG9pbnQoMSwgMikpKSAgIC8vIHRydWUgKGfhu41pIG92ZXJsb2FkKQogICAgcHJpbnRsbihQb2ludCgxLCAyKSA9PSBQb2ludCgxLCAyKSkgICAgICAgIC8vIGZhbHNlICg9PSBn4buNaSBlcXVhbHMoQW55PykpCiAgICBwcmludGxuKGxpc3RPZihQb2ludCgxLCAyKSkuY29udGFpbnMoUG9pbnQoMSwgMikpKSAgLy8gZmFsc2UKfQo) · Xem thêm: K154

### K197 · Comparator chỉ so một field làm `TreeSet` mất phần tử

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** bảng xếp hạng dùng `sortedSetOf(compareByDescending { it.score }, ...)`, hai người cùng điểm thì mất một người.

**Vì sao:** `TreeSet`/`TreeMap` coi hai phần tử là **trùng** khi comparator trả `0`, không dùng `equals`. Cùng `score` → coi là một.

**Sửa:** comparator phải phân biệt mọi phần tử khác nhau: thêm khoá phụ (`thenBy { it.name }`, `thenBy { it.id }`). Hoặc giữ `List` rồi
`sortedWith(...)` khi hiển thị.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE5NzogQ29tcGFyYXRvciBjaOG7iSBzbyBt4buZdCBmaWVsZCDihpIgVHJlZVNldCAvIHNvcnRlZFNldE9mIGNvaSBoYWkgcGjhuqduIHThu60ga2jDoWMgbmhhdSBsw6AgdHLDuW5nCmRhdGEgY2xhc3MgUGxheWVyKHZhbCBuYW1lOiBTdHJpbmcsIHZhbCBzY29yZTogSW50KQpmdW4gbWFpbigpIHsKICAgIHZhbCBib2FyZCA9IHNvcnRlZFNldE9mKGNvbXBhcmVCeURlc2NlbmRpbmc8UGxheWVyPiB7IGl0LnNjb3JlIH0sICAgICAvLyDinYwgY2jhu4kgdGhlbyBzY29yZQogICAgICAgIFBsYXllcigiQW4iLCAxMCksIFBsYXllcigiQsOsbmgiLCAxMCksIFBsYXllcigiQ2hpIiwgNykpCiAgICBwcmludGxuKGJvYXJkKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIELDrG5oIGJp4bq_biBt4bqldAogICAgdmFsIGZpeGVkID0gc29ydGVkU2V0T2YoY29tcGFyZUJ5RGVzY2VuZGluZzxQbGF5ZXI-IHsgaXQuc2NvcmUgfS50aGVuQnkgeyBpdC5uYW1lIH0sICAvLyDinIUKICAgICAgICBQbGF5ZXIoIkFuIiwgMTApLCBQbGF5ZXIoIkLDrG5oIiwgMTApLCBQbGF5ZXIoIkNoaSIsIDcpKQogICAgcHJpbnRsbihmaXhlZCkKfQo) · Xem thêm: K144

### K198 · `Failed requirement.` không nói gì

`🟢 mới học` · `Kotlin`

**Gặp:** crash log chỉ có `java.lang.IllegalArgumentException: Failed requirement.`, không biết giá trị nào sai.

**Vì sao:** `require(điềuKiện)` không có lambda message thì dùng câu mặc định. Tương tự `check()` → `Check failed.`

**Sửa:** luôn kèm message có giá trị thật.

```kotlin
require(people > 0)                                          // ❌
require(people > 0) { "people phải > 0, nhận $people" }      // ✅
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE5ODogcmVxdWlyZSBraMO0bmcgY8OzIG1lc3NhZ2Ug4oaSICJGYWlsZWQgcmVxdWlyZW1lbnQuIiBraMO0bmcgbsOzaSBnw6wKY2xhc3MgU3BsaXQodmFsIHBlb3BsZTogSW50KSB7CiAgICBpbml0IHsKICAgICAgICByZXF1aXJlKHBlb3BsZSA-IDApICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MCiAgICAgICAgLy8gcmVxdWlyZShwZW9wbGUgPiAwKSB7ICJwZW9wbGUgcGjhuqNpID4gMCwgbmjhuq1uICRwZW9wbGUiIH0gICAvLyDinIUKICAgIH0KfQpmdW4gbWFpbigpIHsgU3BsaXQoMCkgfQo)

### K199 · Tạo instance của `abstract class` / interface

`🟢 mới học` · `Kotlin`

**Gặp:** `Cannot create an instance of an abstract class.` (với interface: `Interface ... does not have constructors.`)

**Vì sao:** lớp `abstract` còn thiếu phần cài đặt, không dùng trực tiếp được.

**Sửa:** tạo một lớp con thật (`class FakeRepo : Repo()`), hoặc object ẩn danh tại chỗ: `object : Repo() { override fun load() = ... }`.
Trong test, fake viết tay thường dễ đọc hơn object ẩn danh.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzE5OToga2jDtG5nIHThuqFvIMSRxrDhu6NjIGluc3RhbmNlIGPhu6dhIGFic3RyYWN0IGNsYXNzIC8gaW50ZXJmYWNlCmFic3RyYWN0IGNsYXNzIFJlcG8geyBhYnN0cmFjdCBmdW4gbG9hZCgpOiBMaXN0PFN0cmluZz4gfQpmdW4gbWFpbigpIHsKICAgIHZhbCByID0gUmVwbygpICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBjb21waWxlIGVycm9yCiAgICAvLyDinIUgdmFsIHIgPSBvYmplY3QgOiBSZXBvKCkgeyBvdmVycmlkZSBmdW4gbG9hZCgpID0gbGlzdE9mKCJhIikgfSAgIGhv4bq3YyBt4buZdCBjbGFzcyBjb24gdGjhuq10CiAgICBwcmludGxuKHIubG9hZCgpKQp9Cg) · Xem thêm: K200

### K200 · Truyền lambda cho interface Kotlin thường

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Interface 'interface OnPick : Any' does not have constructors.` khi viết `OnPick { id -> ... }`.

**Vì sao:** cú pháp lambda thay cho interface (SAM conversion) chỉ có với interface Java một hàm, và interface Kotlin khai báo `fun interface`.

**Sửa:** khai báo `fun interface OnPick { fun pick(id: Int) }`. Nhiều khi chỉ cần kiểu hàm `(Int) -> Unit` là đủ, không cần interface.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzIwMDogdHJ1eeG7gW4gbGFtYmRhIGNobyBpbnRlcmZhY2UgS290bGluIHRoxrDhu51uZyDihpIgcGjhuqNpIGzDoCBmdW4gaW50ZXJmYWNlCmludGVyZmFjZSBPblBpY2sgeyBmdW4gcGljayhpZDogSW50KSB9ICAgICAgICAgICAgICAgLy8g4pyFIGZ1biBpbnRlcmZhY2UgT25QaWNrIHsgZnVuIHBpY2soaWQ6IEludCkgfQpmdW4gbWFpbigpIHsKICAgIHZhbCBsID0gT25QaWNrIHsgaWQgLT4gcHJpbnRsbigiY2jhu41uICRpZCIpIH0gICAgICAvLyDinYwgY29tcGlsZSBlcnJvcgogICAgbC5waWNrKDEpCn0K) · Xem thêm: [Hàm, lambda, generics](05-functions.md)

---

**Nhóm trước / sau:** [3. Collections & Sequence](03-collections.md) · [5. Hàm, lambda, generics](05-functions.md) · [Mục lục 900 lỗi](index.md)
