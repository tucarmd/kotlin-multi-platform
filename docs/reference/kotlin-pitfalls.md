# Phụ lục: Kotlin khó ở đâu, bẫy gì, hạn chế gì

Trang này nói về **chính ngôn ngữ Kotlin**: chỗ khó khi mới học, các bẫy mà code vẫn compile được nhưng chạy sai, và những gì Kotlin chưa làm tốt.

Các trang khác lo phần còn lại:

- Lỗi Gradle, Xcode, sync: [Lỗi hay gặp](troubleshooting.md).
- Hạn chế của KMP (interop Swift, build iOS, hệ sinh thái): [Đánh giá KMP, mục 10](../plan/kmp-review.md#10-han-che-cua-kmp-hien-nay).
- Cách viết code cho đúng chuẩn: [Quy ước code Kotlin](kotlin-conventions.md).

Mỗi bẫy có link **▶ Chạy thử** để mở [Playground](../playground/index.html) với code nạp sẵn. Kết quả ghi trong trang này đều đã chạy thật trên Kotlin 2.4.21.

## 1. Khó ở đâu khi mới học

| Chỗ khó | Vì sao khó | Cách vượt |
|---|---|---|
| **Một việc có nhiều cách viết** | `let`, `run`, `with`, `apply`, `also` trông gần giống nhau; `for` và `forEach`; `if` và `when` | Học bảng chọn nhanh ở [mục 2.12](#212-5-scope-function-trong-giong-nhau), mỗi tuần chỉ dùng thêm một loại |
| **Lambda và DSL** | Trailing lambda, `it`, lambda có receiver làm Compose và `build.gradle.kts` trông như "cú pháp ma thuật" | Viết lại một đoạn DSL thành gọi hàm bình thường để thấy nó chỉ là hàm nhận lambda |
| **Null safety** | Lúc đầu thấy phiền vì phải xử lý `?` ở khắp nơi, dễ "chữa cháy" bằng `!!` | Dùng `?.`, `?:`, `toIntOrNull()`. Coi mỗi `!!` là một chỗ có thể crash |
| **Coroutines** | `suspend`, scope, dispatcher, huỷ, `Flow` lạnh và nóng: nhiều khái niệm mới cùng lúc | Học theo thứ tự `suspend` → `launch`/`async` → huỷ → `Flow`. Không dùng `GlobalScope` |
| **Generics** | `in`/`out`, `*`, `reified`, xoá kiểu khi chạy | Lúc đầu chỉ cần đọc hiểu. Tự viết generic khi đã quen collection |
| **Gradle Kotlin DSL** | Build script cũng là Kotlin nhưng lỗi khó đọc, gợi ý code chậm | Copy từ project wizard. Mỗi lần chỉ sửa một dòng rồi Sync |
| **Lẫn với Java** | Tài liệu và thư viện Android cũ viết bằng Java; kiểu từ Java không rõ có null hay không | Đọc [mục 2.4](#24-kieu-tu-java-lach-null-safety). Ghi rõ kiểu khi nhận giá trị từ Java |
| **Code chạy ở JVM chưa chắc chạy ở KMP** | `java.*`, `String.format`, `Thread.sleep` chạy được trên Android và Playground nhưng không có trong `commonMain` | Viết luật chung chỉ bằng Kotlin thuần; phần cần API nền tảng thì để `expect`/`actual` |

## 2. Các bẫy hay dính

Điểm chung của các bẫy này: **compile được, không cảnh báo, nhưng chạy sai**.

### 2.1. Int tràn số mà không báo lỗi

```kotlin
val thirtyDaysMs = 30 * 24 * 60 * 60 * 1000   // -1702967296
val fixed = 30L * 24 * 60 * 60 * 1000          // 2592000000
```

Int nhân Int ra Int. Vượt `Int.MAX_VALUE` thì quay vòng sang số âm, không ném lỗi.

**Tránh:** thêm `L` vào số đầu tiên khi tính mili giây hoặc tiền lớn. Cần chặn tràn thì dùng `Math.addExact`/`multiplyExact` (chỉ có trên JVM).
[▶ Chạy thử][p-overflow]

### 2.2. Chia số nguyên và cộng số thực

| Code | Kết quả |
|---|---|
| `5 / 2` | `2` |
| `-7 / 2` | `-3` (làm tròn về 0, không phải xuống) |
| `-7 % 2` | `-1` |
| `0.1 + 0.2` | `0.30000000000000004` |
| `0.1 + 0.2 == 0.3` | `false` |

**Tránh:** tiền lưu `Long` theo đơn vị nhỏ nhất (đồng, cent) hoặc `BigDecimal`. Cần phần dư luôn dương thì dùng `Math.floorMod`. App [Tip Split](../apps/01-tip-split.md) đi sâu vào bẫy này.
[▶ Chạy thử][p-division]

### 2.3. `==` và `===`

- `==` so **giá trị**, `===` so **có phải cùng một object** không.
- `Int?` bằng 127 thì `===` ra `true`, bằng 1000 thì ra `false`, vì JVM chỉ dùng lại object cho số trong khoảng -128..127.
- `Array` không so theo nội dung: `arrayOf(1, 2) == arrayOf(1, 2)` là `false`. `List` thì so theo nội dung.

**Tránh:** luôn dùng `==` cho số và chuỗi. So mảng bằng `contentEquals`, hoặc dùng `List` thay cho `Array`.
[▶ Chạy thử][p-equality]

### 2.4. Kiểu từ Java "lách" null safety

```kotlin
val home = System.getenv("KHONG_CO")   // kiểu String! — Kotlin không biết có null hay không
println(home.length)                   // compile được → NullPointerException khi chạy
```

Giá trị trả về từ code Java có **platform type** (`String!`). Kotlin cho bạn dùng như kiểu không null và không bắt bạn kiểm tra.

**Tránh:** ghi rõ kiểu ngay khi nhận: `val home: String? = System.getenv("X")`. Từ đó compiler bắt bạn xử lý `null`.
[▶ Chạy thử][p-platform]

### 2.5. `!!` và `lateinit`: hẹn giờ crash

- `x!!` ném `NullPointerException` nếu `x` là `null`.
- Đọc biến `lateinit` trước khi gán sẽ ném `UninitializedPropertyAccessException`.

Cả hai đều tắt null safety ở đúng chỗ đó.

**Tránh:** `?:` kèm giá trị mặc định; `requireNotNull(x) { "lý do" }` để lỗi có thông báo rõ; `lateinit` chỉ dùng cho biến chắc chắn được gán trước khi đọc (vd trong `setUp()` của test).
[▶ Chạy thử][p-bangbang]

### 2.6. `$` trong chuỗi chỉ lấy tới hết tên biến

```kotlin
println("Xin chào $u.name")    // Xin chào User(name=Tú, age=30).name
println("Xin chào ${u.name}")  // Xin chào Tú
```

**Tránh:** có dấu `.` hoặc phép tính thì luôn dùng `${…}`. Muốn in chữ `$` ngay trước một tên biến thì viết `\$`.
[▶ Chạy thử][p-template]

### 2.7. `return` trong lambda thoát luôn hàm bên ngoài

```kotlin
list.forEach {
    if (it % 2 == 0) return          // thoát cả hàm đang chứa forEach
    if (it % 2 == 0) return@forEach  // chỉ bỏ qua phần tử này, giống continue
}
```

`forEach` là hàm `inline`, nên `return` trần trong lambda của nó thoát khỏi hàm bên ngoài.

**Tránh:** dùng `return@forEach`, hoặc viết vòng `for` bình thường (quy ước Kotlin cũng ưu tiên `for` khi chỉ để lặp).
[▶ Chạy thử][p-return]

### 2.8. `data class`: chỉ tính property trong constructor, `copy` là copy nông

- Property khai báo trong thân class không được đưa vào `equals`, `hashCode`, `toString`, `copy`. Hai object khác `note` vẫn `==` nhau.
- `copy()` không copy list bên trong. Sửa list của bản copy là sửa luôn list của bản gốc.

**Tránh:** đưa mọi property có ý nghĩa vào constructor. Trong `data class` dùng `List` (chỉ đọc) và tạo list mới khi cần đổi: `cart.copy(items = cart.items + "bánh mì")`.
[▶ Chạy thử][p-dataclass]

### 2.9. `List` chỉ đọc không có nghĩa là không đổi

`val view: List<Int> = mutableList` chỉ là một góc nhìn chỉ đọc của **cùng một** list. Ai giữ `mutableList` vẫn sửa được, và `view` đổi theo.

**Tránh:** khi đưa dữ liệu ra ngoài (vd từ ViewModel lên UI), trả về một bản copy bằng `toList()`, hoặc giữ state trong `StateFlow<List<…>>` và mỗi lần đổi thì gán một list mới.
[▶ Chạy thử][p-readonly]

### 2.10. Extension function không override được

```kotlin
fun Animal.sound() = "..."
fun Dog.sound() = "Gâu"
val a: Animal = Dog()
a.sound()   // "..." — chọn theo kiểu khai báo Animal, không theo object thật
```

Extension được chọn **lúc compile** theo kiểu khai báo của biến.

**Tránh:** hành vi cần khác nhau theo lớp con thì viết `open fun` trong class rồi `override`.
[▶ Chạy thử][p-extension]

### 2.11. Coroutine: `catch (e: Exception)` nuốt lệnh huỷ

Coroutine bị huỷ bằng cách ném `CancellationException` ở chỗ đang treo (`delay`, gọi mạng…). Bọc `try { … } catch (e: Exception)` sẽ bắt luôn exception đó, nên coroutine không dừng mà chạy hết vòng lặp:

```
bad: vòng 0
bad: nuốt JobCancellationException   ← huỷ rồi mà vẫn lặp tiếp
bad: nuốt JobCancellationException
…
good: vòng 0                         ← ném lại thì dừng đúng lúc
```

**Tránh:** bắt `CancellationException` riêng và `throw` lại, hoặc chỉ bắt đúng loại lỗi cần bắt (`IOException`…). Không dùng `runCatching` bọc quanh hàm `suspend`, vì nó cũng nuốt lệnh huỷ.
[▶ Chạy thử][p-cancel]

### 2.12. 5 scope function trông giống nhau

| Hàm | Trong lambda gọi object là | Trả về | Dùng khi |
|---|---|---|---|
| `apply` | `this` | chính object | Cấu hình object vừa tạo |
| `also` | `it` | chính object | Làm thêm việc phụ: log, kiểm tra |
| `let` | `it` | kết quả lambda | Xử lý giá trị có thể null: `x?.let { … }` |
| `run` | `this` | kết quả lambda | Tính một kết quả từ nhiều thuộc tính |
| `with` | `this` | kết quả lambda | Giống `run`, nhưng object truyền vào làm tham số |

**Tránh:** lồng scope function vào nhau, hoặc nối dài `?.let { … } ?: …` thay cho `if` dễ đọc hơn.
[▶ Chạy thử][p-scope]

### 2.13. `Thread.sleep` trong coroutine chặn cả thread

3 coroutine cùng `Thread.sleep(300)` mất khoảng **900 ms** vì phải chạy lần lượt. Đổi sang `delay(300)` thì chỉ mất khoảng **300 ms**, vì `delay` chỉ treo coroutine và nhường thread cho coroutine khác.

**Tránh:** trong coroutine dùng `delay`. Code chặn thread (đọc file lớn, thư viện Java cũ) thì bọc trong `withContext(Dispatchers.IO)`.
[▶ Chạy thử][p-blocking]

## 3. Hạn chế của Kotlin

| Hạn chế | Ảnh hưởng | Cách sống chung |
|---|---|---|
| **Compile chậm hơn Java** | Build lớn, sửa một dòng chờ lâu. K2 (từ Kotlin 2.0) đã nhanh hơn nhiều nhưng vẫn chưa bằng Java | Bật Gradle build cache và configuration cache; chia module; dùng KSP thay cho kapt |
| **Không có checked exception** | Compiler không nhắc bạn xử lý lỗi; gọi từ Swift mà không có `@Throws` thì app iOS crash | Trả lỗi bằng `Result` hoặc sealed class; hàm Swift gọi tới thì thêm `@Throws` |
| **Generic bị xoá kiểu khi chạy (JVM)** | Không viết được `x is List<String>` | Hàm `inline` với `reified T`, hoặc `filterIsInstance<T>()`. [▶ Chạy thử][p-erasure] |
| **Không có `static`, không có package-private** | Người từ Java hay lúng túng | Dùng hàm top-level hoặc `companion object`; giới hạn phạm vi bằng `internal` (theo module) hoặc `private` (theo file) |
| **Class mặc định là `final`** | Framework cần kế thừa class (Spring, một số thư viện mock) không chạy nếu không cấu hình | Plugin `kotlin-spring` / `all-open`; khi test thì mock bằng MockK |
| **Không có toán tử 3 ngôi `a ? b : c`** | Chỉ khác thói quen | `if` là biểu thức: `val x = if (ok) 1 else 0` |
| **Interop Java có kẽ hở** | Platform type ([mục 2.4](#24-kieu-tu-java-lach-null-safety)), thư viện Java thiếu annotation `@Nullable` | Ghi rõ kiểu ngay khi nhận giá trị từ Java |
| **Mỗi target có thư viện chuẩn khác nhau** | `java.*`, `String.format`, `Thread` không có trong `commonMain` | Chỉ dùng API Kotlin thuần trong code chung; cần định dạng số thì tự viết hoặc dùng thư viện multiplatform |
| **Công cụ gắn với JetBrains** | Hỗ trợ tốt nhất ở IntelliJ IDEA và Android Studio; các editor khác kém hơn rõ rệt | Dùng Android Studio cho Android/KMP; Playground để thử nhanh |
| **Reflection nặng** | `kotlin-reflect` làm app to thêm và chạy chậm | Sinh code lúc compile (KSP, kotlinx.serialization) thay cho reflection |
| **Ngoài Android, cộng đồng nhỏ hơn Java/JS** | Backend, web ít ví dụ và ít việc làm hơn | Xem [Kotlin làm web](../plan/kotlin-web.md) trước khi chọn Kotlin cho web |

## 4. Hạn chế của Playground

Playground của site này dùng compiler chính chủ (api.kotlinlang.org), chạy trên **JVM**. Các giới hạn dưới đây đã thử thật:

| Được | Không được |
|---|---|
| Thư viện chuẩn, `kotlinx.coroutines`, `kotlinx.serialization`, `kotlinx.datetime` | Thêm thư viện ngoài (vd `okhttp3` báo `Unresolved reference`) |
| Đọc tham số bằng ô **Program arguments** → `args` | Nhập từ bàn phím: `readln()` trả về `null` |
| Chương trình chạy dưới khoảng 10 giây | Lâu hơn thì bị dừng: `Evaluation stopped while it's taking too long` |
| Một file `File.kt`, nhiều class trong cùng file | Nhiều file, nhiều module, Gradle |
| | Gọi mạng (bị treo rồi hết giờ) |
| | Chạy Compose, Android, iOS |

Code chạy được ở Playground chỉ chứng minh nó đúng **trên JVM**. Code có `java.*` hoặc `String.format` sẽ không compile trong `commonMain` của KMP.

## 5. Gặp bẫy mới thì làm gì

1. Thu nhỏ thành một hàm `main` ngắn nhất vẫn còn lỗi, rồi dán vào Playground.
2. Bấm **Share** để có link, rồi thêm một mục vào trang này theo đúng khung: code ngắn, kết quả thật, cách tránh, link ▶.
3. Lỗi build hoặc lỗi môi trường thì ghi vào [Lỗi hay gặp](troubleshooting.md).

## Nguồn

- [Null safety](https://kotlinlang.org/docs/null-safety.html) và [platform types](https://kotlinlang.org/docs/java-interop.html#null-safety-and-platform-types)
- [Numbers](https://kotlinlang.org/docs/numbers.html), [Equality](https://kotlinlang.org/docs/equality.html), [Strings](https://kotlinlang.org/docs/strings.html)
- [Returns and jumps](https://kotlinlang.org/docs/returns.html), [Inline functions](https://kotlinlang.org/docs/inline-functions.html)
- [Data classes](https://kotlinlang.org/docs/data-classes.html), [Collections overview](https://kotlinlang.org/docs/collections-overview.html)
- [Extensions](https://kotlinlang.org/docs/extensions.html), [Scope functions](https://kotlinlang.org/docs/scope-functions.html), [Generics](https://kotlinlang.org/docs/generics.html)
- [Cancellation and timeouts](https://kotlinlang.org/docs/cancellation-and-timeouts.html), [Coroutine exceptions handling](https://kotlinlang.org/docs/exception-handling.html)
- [Comparison to Java](https://kotlinlang.org/docs/comparison-to-java.html), [KSP overview](https://kotlinlang.org/docs/ksp-overview.html)

<!-- Link ▶ Chạy thử: code được mã hoá base64url trong #code=, giống nút Share của Playground. -->

[p-overflow]: ../playground/index.html#code=Ly8gQuG6q3k6IEludCB0csOgbiBz4buRIG3DoCBraMO0bmcgYsOhbyBs4buXaQpmdW4gbWFpbigpIHsKICAgIHZhbCB0aGlydHlEYXlzTXMgPSAzMCAqIDI0ICogNjAgKiA2MCAqIDEwMDAgICAgICAgLy8gSW50IMOXIEludCDihpIgSW50LCB0csOgbiEKICAgIHByaW50bG4oIlNhaTogJHRoaXJ0eURheXNNcyIpCgogICAgdmFsIGZpeGVkID0gMzBMICogMjQgKiA2MCAqIDYwICogMTAwMCAgICAgICAgICAgICAvLyBjw7MgTCDihpIgY-G6oyBwaMOpcCB0w61uaCBsw6AgTG9uZwogICAgcHJpbnRsbigixJDDum5nOiAkZml4ZWQiKQoKICAgIHByaW50bG4oSW50Lk1BWF9WQUxVRSArIDEpICAgICAgICAgICAgICAgICAgICAgICAgLy8gcXVheSB2w7JuZyBzYW5nIHPhu5Egw6JtCiAgICBwcmludGxuKE1hdGguYWRkRXhhY3QoMSwgMikpICAgICAgICAgICAgICAgICAgICAgIC8vIGFkZEV4YWN0IG7DqW0gbOG7l2kgdGhheSB2w6wgdHLDoG4gaW0gbOG6t25nCn0K
[p-division]: ../playground/index.html#code=Ly8gQuG6q3k6IGNoaWEgc-G7kSBuZ3V5w6puIHbDoCBj4buZbmcgc-G7kSB0aOG7sWMKZnVuIG1haW4oKSB7CiAgICBwcmludGxuKDUgLyAyKSAgICAgICAgICAgIC8vIDIg4oCUIEludCBjaGlhIEludCBi4buPIHBo4bqnbiBs4bq7CiAgICBwcmludGxuKDUgLyAyLjApICAgICAgICAgIC8vIDIuNQogICAgcHJpbnRsbigtNyAvIDIpICAgICAgICAgICAvLyAtMyDigJQgbMOgbSB0csOybiB24buBIDAsIGtow7RuZyBwaOG6o2kgeHXhu5FuZwogICAgcHJpbnRsbigtNyAlIDIpICAgICAgICAgICAvLyAtMQogICAgcHJpbnRsbihNYXRoLmZsb29yTW9kKC03LCAyKSkgIC8vIDEKCiAgICBwcmludGxuKDAuMSArIDAuMikgICAgICAgIC8vIDAuMzAwMDAwMDAwMDAwMDAwMDQKICAgIHByaW50bG4oMC4xICsgMC4yID09IDAuMykgLy8gZmFsc2Ug4oaSIHRp4buBbiB0aMOsIGTDuW5nIExvbmcgKMSR4buTbmcpIGhv4bq3YyBCaWdEZWNpbWFsCn0K
[p-equality]: ../playground/index.html#code=Ly8gQuG6q3k6ID09IHNvIGdpw6EgdHLhu4ssID09PSBzbyAiY8O5bmcgbeG7mXQgb2JqZWN0IgpmdW4gbWFpbigpIHsKICAgIHZhbCBhID0gImtvdCIgKyAibGluIi5sb3dlcmNhc2UoKQogICAgdmFsIGIgPSAia290bGluIgogICAgcHJpbnRsbihhID09IGIpICAgIC8vIHRydWUKICAgIHByaW50bG4oYSA9PT0gYikgICAvLyBmYWxzZSDigJQgMiBvYmplY3Qga2jDoWMgbmhhdQoKICAgIHZhbCB4OiBJbnQ_ID0gMTI3OyB2YWwgeTogSW50PyA9IDEyNwogICAgdmFsIHA6IEludD8gPSAxMDAwOyB2YWwgcTogSW50PyA9IDEwMDAKICAgIHByaW50bG4oeCA9PT0geSkgICAvLyB0cnVlICDigJQgSlZNIGNhY2hlIHPhu5Egbmjhu48gKC0xMjguLjEyNykKICAgIHByaW50bG4ocCA9PT0gcSkgICAvLyBmYWxzZSDigJQgbmdvw6BpIGtob-G6o25nIGNhY2hlCiAgICBwcmludGxuKHAgPT0gcSkgICAgLy8gdHJ1ZSAg4oCUIGx1w7RuIGTDuW5nID09IGNobyBz4buRCgogICAgdmFsIGFycjEgPSBhcnJheU9mKDEsIDIpOyB2YWwgYXJyMiA9IGFycmF5T2YoMSwgMikKICAgIHByaW50bG4oYXJyMSA9PSBhcnIyKSAgICAgICAgICAgICAgLy8gZmFsc2Ug4oCUIEFycmF5IGtow7RuZyBzbyB0aGVvIG7hu5lpIGR1bmcKICAgIHByaW50bG4oYXJyMS5jb250ZW50RXF1YWxzKGFycjIpKSAgLy8gdHJ1ZQogICAgcHJpbnRsbihsaXN0T2YoMSwgMikgPT0gbGlzdE9mKDEsIDIpKSAvLyB0cnVlIOKAlCBMaXN0IHRow6wgY8OzCn0K
[p-platform]: ../playground/index.html#code=Ly8gQuG6q3k6IGtp4buDdSB04burIEphdmEgKHBsYXRmb3JtIHR5cGUpICJsw6FjaCIgbnVsbCBzYWZldHkKZnVuIG1haW4oKSB7CiAgICB2YWwgb2s6IFN0cmluZz8gPSBTeXN0ZW0uZ2V0ZW52KCJLSE9OR19DT19CSUVOX05BWSIpICAvLyBnaGkgcsO1IFN0cmluZz8g4oaSIGFuIHRvw6BuCiAgICBwcmludGxuKG9rPy5sZW5ndGggPzogImtow7RuZyBjw7MgYmnhur9uIG3DtGkgdHLGsOG7nW5nIikKCiAgICB2YWwgaG9tZSA9IFN5c3RlbS5nZXRlbnYoIktIT05HX0NPX0JJRU5fTkFZIikgICAgICAgICAvLyBraeG7g3UgbMOgIFN0cmluZyEgKGtow7RuZyByw7UgbnVsbCkKICAgIHByaW50bG4oaG9tZS5sZW5ndGgpICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIGNvbXBpbGUgxJHGsOG7o2PigKYgcuG7k2kgTlBFIGtoaSBjaOG6oXkKfQo
[p-bangbang]: ../playground/index.html#code=Ly8gQuG6q3k6ICEhIHbDoCBsYXRlaW5pdCA9IGjhurluIGdp4budIGNyYXNoCmNsYXNzIFNjcmVlbiB7CiAgICBsYXRlaW5pdCB2YXIgdGl0bGU6IFN0cmluZwogICAgZnVuIHNob3coKSA9IHByaW50bG4oIk3DoG46ICR0aXRsZSIpCn0KCmZ1biBtYWluKCkgewogICAgdmFsIGlucHV0ID0gImFiYyIKICAgIHByaW50bG4oaW5wdXQudG9JbnRPck51bGwoKSA_OiAwKSAgICAgICAgLy8gYW4gdG_DoG4KCiAgICB2YWwgcyA9IFNjcmVlbigpCiAgICBwcmludGxuKHM6OmNsYXNzLnNpbXBsZU5hbWUpCiAgICAvLyBwcmludGxuKHMudGl0bGUpICDihpIgVW5pbml0aWFsaXplZFByb3BlcnR5QWNjZXNzRXhjZXB0aW9uCiAgICBzLnRpdGxlID0gIkhvbWUiCiAgICBzLnNob3coKQoKICAgIHZhbCBuID0gaW5wdXQudG9JbnRPck51bGwoKSEhICAgICAgICAgICAgLy8gTnVsbFBvaW50ZXJFeGNlcHRpb24g4bufIMSRw6J5CiAgICBwcmludGxuKG4pCn0K
[p-template]: ../playground/index.html#code=Ly8gQuG6q3k6ICQgdHJvbmcgY2h14buXaSBjaOG7iSDEg24gdOG7m2kgaOG6v3QgdMOqbiBiaeG6v24KZGF0YSBjbGFzcyBVc2VyKHZhbCBuYW1lOiBTdHJpbmcsIHZhbCBhZ2U6IEludCkKCmZ1biBtYWluKCkgewogICAgdmFsIHUgPSBVc2VyKCJUw7oiLCAzMCkKICAgIHByaW50bG4oIlhpbiBjaMOgbyAkdS5uYW1lIikgICAgICAvLyBpbiBj4bqjIG9iamVjdCBy4buTaSBt4bubaSB04bubaSAiLm5hbWUiCiAgICBwcmludGxuKCJYaW4gY2jDoG8gJHt1Lm5hbWV9IikgICAgLy8gxJHDum5nOiBjw7Mgbmdv4bq3YyBuaOG7jW4KCiAgICB2YWwgcHJpY2UgPSA1CiAgICBwcmludGxuKCJHacOhOiAkcHJpY2UkIikgICAgICAgICAgIC8vICQg4bufIGN14buRaSB0aMOsIGluIGLDrG5oIHRoxrDhu51uZwogICAgcHJpbnRsbigiQmnhur9uIHTDqm4gbMOgIFwkcHJpY2UiKSAgIC8vIG114buRbiBpbiBjaOG7ryAkcHJpY2UgdGjDrCBlc2NhcGUKfQo
[p-return]: ../playground/index.html#code=Ly8gQuG6q3k6IHJldHVybiB0cm9uZyBsYW1iZGEgdGhvw6F0IGx1w7RuIGPhuqMgaMOgbSBuZ2_DoGkKZnVuIGZpcnN0RXZlbk9sZChsaXN0OiBMaXN0PEludD4pIHsKICAgIGxpc3QuZm9yRWFjaCB7CiAgICAgICAgaWYgKGl0ICUgMiA9PSAwKSB7CiAgICAgICAgICAgIHByaW50bG4oImfhurdwIHPhu5EgY2jhurVuICRpdCIpCiAgICAgICAgICAgIHJldHVybiAgICAgICAgICAgICAgICAgICAgLy8gdGhvw6F0IGto4buPaSBmaXJzdEV2ZW5PbGQsIGtow7RuZyBjaOG7iSBsYW1iZGEKICAgICAgICB9CiAgICB9CiAgICBwcmludGxuKCJkw7JuZyBuw6B5IGtow7RuZyBiYW8gZ2nhu50gY2jhuqF5IG7hur91IGPDsyBz4buRIGNo4bq1biIpCn0KCmZ1biBza2lwRXZlbihsaXN0OiBMaXN0PEludD4pIHsKICAgIGxpc3QuZm9yRWFjaCB7CiAgICAgICAgaWYgKGl0ICUgMiA9PSAwKSByZXR1cm5AZm9yRWFjaCAgIC8vIGNo4buJIGLhu48gcXVhIHBo4bqnbiB04butIG7DoHkgKG5oxrAgY29udGludWUpCiAgICAgICAgcHJpbnRsbigibOG6uzogJGl0IikKICAgIH0KICAgIHByaW50bG4oInhvbmcgc2tpcEV2ZW4iKQp9CgpmdW4gbWFpbigpIHsKICAgIGZpcnN0RXZlbk9sZChsaXN0T2YoMSwgMiwgMykpCiAgICBza2lwRXZlbihsaXN0T2YoMSwgMiwgMykpCn0K
[p-dataclass]: ../playground/index.html#code=Ly8gQuG6q3k6IGRhdGEgY2xhc3MgY2jhu4kgdMOtbmggY8OhYyBwcm9wZXJ0eSB0cm9uZyBjb25zdHJ1Y3RvciwgY29weSBsw6AgY29weSBuw7RuZwpkYXRhIGNsYXNzIENhcnQodmFsIG93bmVyOiBTdHJpbmcsIHZhbCBpdGVtczogTXV0YWJsZUxpc3Q8U3RyaW5nPikgewogICAgdmFyIG5vdGU6IFN0cmluZyA9ICIiICAgICAgICAgIC8vIG7hurFtIHRyb25nIHRow6JuIGNsYXNzIOKGkiBraMO0bmcgdsOgbyBlcXVhbHMvdG9TdHJpbmcKfQoKZnVuIG1haW4oKSB7CiAgICB2YWwgYSA9IENhcnQoIlTDuiIsIG11dGFibGVMaXN0T2YoImPDoCBwaMOqIikpLmFwcGx5IHsgbm90ZSA9ICJnaWFvIGfhuqVwIiB9CiAgICB2YWwgYiA9IENhcnQoIlTDuiIsIG11dGFibGVMaXN0T2YoImPDoCBwaMOqIikpLmFwcGx5IHsgbm90ZSA9ICJraMO0bmcgZ-G6pXAiIH0KICAgIHByaW50bG4oYSA9PSBiKSAgICAgICAgICAgICAgICAvLyB0cnVlIGTDuSBub3RlIGtow6FjIG5oYXUKICAgIHByaW50bG4oYSkgICAgICAgICAgICAgICAgICAgICAvLyB0b1N0cmluZyBraMO0bmcgY8OzIG5vdGUKCiAgICB2YWwgYyA9IGEuY29weSgpCiAgICBjLml0ZW1zICs9ICJiw6FuaCBtw6wiICAgICAgICAgICAvLyBjb3B5IG7DtG5nOiBjIHbDoCBhIGTDuW5nIENIVU5HIG3hu5l0IGxpc3QKICAgIHByaW50bG4oYS5pdGVtcykKfQo
[p-readonly]: ../playground/index.html#code=Ly8gQuG6q3k6IExpc3QgImNo4buJIMSR4buNYyIga2jDtG5nIGPDsyBuZ2jEqWEgbMOgICJraMO0bmcgxJHhu5VpIgpmdW4gbWFpbigpIHsKICAgIHZhbCBzb3VyY2UgPSBtdXRhYmxlTGlzdE9mKDEsIDIsIDMpCiAgICB2YWwgdmlldzogTGlzdDxJbnQ-ID0gc291cmNlICAgICAvLyBjaOG7iSBsw6AgZ8OzYyBuaMOsbiByZWFkLW9ubHkgY-G7p2EgY8O5bmcgbeG7mXQgbGlzdAogICAgc291cmNlICs9IDQKICAgIHByaW50bG4odmlldykgICAgICAgICAgICAgICAgICAgIC8vIFsxLCAyLCAzLCA0XSDigJQgdmlldyDEkeG7lWkgdGhlbwoKICAgIHZhbCBzbmFwc2hvdCA9IHNvdXJjZS50b0xpc3QoKSAgIC8vIG114buRbiBi4bqjbiBjaOG7pXAgdGjDrCBjb3B5CiAgICBzb3VyY2UuY2xlYXIoKQogICAgcHJpbnRsbigidmlldz0kdmlldyBzbmFwc2hvdD0kc25hcHNob3QiKQp9Cg
[p-extension]: ../playground/index.html#code=Ly8gQuG6q3k6IGV4dGVuc2lvbiBmdW5jdGlvbiBjaOG7jW4gdGhlbyBLSeG7glUgS0hBSSBCw4FPLCBraMO0bmcgdGhlbyBvYmplY3QgdGjhuq10Cm9wZW4gY2xhc3MgQW5pbWFsCmNsYXNzIERvZyA6IEFuaW1hbCgpCgpmdW4gQW5pbWFsLnNvdW5kKCkgPSAiLi4uIgpmdW4gRG9nLnNvdW5kKCkgPSAiR8OidSIKCm9wZW4gY2xhc3MgQW5pbWFsMiB7IG9wZW4gZnVuIHNvdW5kKCkgPSAiLi4uIiB9CmNsYXNzIERvZzIgOiBBbmltYWwyKCkgeyBvdmVycmlkZSBmdW4gc291bmQoKSA9ICJHw6J1IiB9CgpmdW4gbWFpbigpIHsKICAgIHZhbCBhOiBBbmltYWwgPSBEb2coKQogICAgcHJpbnRsbihhLnNvdW5kKCkpICAgICAgIC8vICIuLi4iIOKAlCBleHRlbnNpb24ga2jDtG5nIG92ZXJyaWRlIMSRxrDhu6NjCgogICAgdmFsIGI6IEFuaW1hbDIgPSBEb2cyKCkKICAgIHByaW50bG4oYi5zb3VuZCgpKSAgICAgICAvLyAiR8OidSIg4oCUIG1lbWJlciBmdW5jdGlvbiB0aMOsIGPDswp9Cg
[p-cancel]: ../playground/index.html#code=Ly8gQuG6q3k6IGNhdGNoIChlOiBFeGNlcHRpb24pIG514buRdCBsdcO0biBs4buHbmggaHXhu7cgY29yb3V0aW5lCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsKICAgIHZhbCBiYWQgPSBsYXVuY2ggewogICAgICAgIHJlcGVhdCg1KSB7IGkgLT4KICAgICAgICAgICAgdHJ5IHsKICAgICAgICAgICAgICAgIGRlbGF5KDEwMCkKICAgICAgICAgICAgICAgIHByaW50bG4oImJhZDogdsOybmcgJGkiKQogICAgICAgICAgICB9IGNhdGNoIChlOiBFeGNlcHRpb24pIHsgICAgICAgIC8vIGLhuq90IGPhuqMgQ2FuY2VsbGF0aW9uRXhjZXB0aW9uIQogICAgICAgICAgICAgICAgcHJpbnRsbigiYmFkOiBudeG7kXQgJHtlOjpjbGFzcy5zaW1wbGVOYW1lfSIpCiAgICAgICAgICAgIH0KICAgICAgICB9CiAgICB9CiAgICBkZWxheSgxNTApOyBiYWQuY2FuY2VsQW5kSm9pbigpICAgICAgICAvLyBodeG7tyBy4buTaSBtw6AgduG6q24gY2jhuqF5IHRp4bq_cCB04bubaSB2w7JuZyA0CgogICAgdmFsIGdvb2QgPSBsYXVuY2ggewogICAgICAgIHJlcGVhdCg1KSB7IGkgLT4KICAgICAgICAgICAgdHJ5IHsKICAgICAgICAgICAgICAgIGRlbGF5KDEwMCkKICAgICAgICAgICAgICAgIHByaW50bG4oImdvb2Q6IHbDsm5nICRpIikKICAgICAgICAgICAgfSBjYXRjaCAoZTogQ2FuY2VsbGF0aW9uRXhjZXB0aW9uKSB7CiAgICAgICAgICAgICAgICB0aHJvdyBlICAgICAgICAgICAgICAgICAgICAgLy8gbHXDtG4gbsOpbSBs4bqhaQogICAgICAgICAgICB9IGNhdGNoIChlOiBFeGNlcHRpb24pIHsKICAgICAgICAgICAgICAgIHByaW50bG4oImdvb2Q6IGzhu5dpIHRo4bqtdCAkZSIpCiAgICAgICAgICAgIH0KICAgICAgICB9CiAgICB9CiAgICBkZWxheSgxNTApOyBnb29kLmNhbmNlbEFuZEpvaW4oKQogICAgcHJpbnRsbigieG9uZyIpCn0K
[p-blocking]: ../playground/index.html#code=Ly8gQuG6q3k6IFRocmVhZC5zbGVlcCB0cm9uZyBjb3JvdXRpbmUgY2jhurduIGPhuqMgdGhyZWFkCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuKgppbXBvcnQga290bGluLnN5c3RlbS5tZWFzdXJlVGltZU1pbGxpcwoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsKICAgIHZhbCBzbG93ID0gbWVhc3VyZVRpbWVNaWxsaXMgewogICAgICAgIHZhbCBqb2JzID0gTGlzdCgzKSB7IGxhdW5jaCB7IFRocmVhZC5zbGVlcCgzMDApIH0gfSAgIC8vIGNo4bq3bjogY2jhuqF5IGzhuqduIGzGsOG7o3QKICAgICAgICBqb2JzLmpvaW5BbGwoKQogICAgfQogICAgdmFsIGZhc3QgPSBtZWFzdXJlVGltZU1pbGxpcyB7CiAgICAgICAgdmFsIGpvYnMgPSBMaXN0KDMpIHsgbGF1bmNoIHsgZGVsYXkoMzAwKSB9IH0gICAgICAgICAgLy8gdHJlbzogY2jhuqF5IGPDuW5nIGzDumMKICAgICAgICBqb2JzLmpvaW5BbGwoKQogICAgfQogICAgcHJpbnRsbigiVGhyZWFkLnNsZWVwOiB-JHtzbG93IC8gMTAwICogMTAwfSBtcyIpCiAgICBwcmludGxuKCJkZWxheTogICAgICAgIH4ke2Zhc3QgLyAxMDAgKiAxMDB9IG1zIikKfQo
[p-scope]: ../playground/index.html#code=Ly8gS2jDszogNSBzY29wZSBmdW5jdGlvbiB0csO0bmcgZ2nhu5FuZyBuaGF1CmRhdGEgY2xhc3MgUHJvZmlsZSh2YXIgbmFtZTogU3RyaW5nID0gIiIsIHZhciBhZ2U6IEludCA9IDApCgpmdW4gbWFpbigpIHsKICAgIHZhbCBwID0gUHJvZmlsZSgpLmFwcGx5IHsgbmFtZSA9ICJUw7oiOyBhZ2UgPSAzMCB9ICAgICAgLy8gYXBwbHk6IGPhuqV1IGjDrG5oLCB0cuG6oyB24buBIG9iamVjdAogICAgdmFsIGxlbiA9IHAubmFtZS5sZXQgeyBpdC5sZW5ndGggfSAgICAgICAgICAgICAgICAgICAgLy8gbGV0OiBiaeG6v24gxJHhu5VpLCB0cuG6oyB24buBIGvhur90IHF14bqjIGxhbWJkYQogICAgdmFsIGRlc2MgPSB3aXRoKHApIHsgIiRuYW1lICgkYWdlKSIgfSAgICAgICAgICAgICAgICAgLy8gd2l0aDogZMO5bmcgbmhp4buBdSB0aHXhu5ljIHTDrW5oCiAgICB2YWwgb2sgPSBwLnJ1biB7IGFnZSA-PSAxOCB9ICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBydW46IG5oxrAgd2l0aCBuaMawbmcgZ-G7jWkgdHLDqm4gb2JqZWN0CiAgICBwLmFsc28geyBwcmludGxuKCJsb2c6ICRpdCIpIH0gICAgICAgICAgICAgICAgICAgICAgICAvLyBhbHNvOiBsw6BtIHRow6ptIHZp4buHYyBwaOG7pSwgdHLhuqMgduG7gSBvYmplY3QKCiAgICBwcmludGxuKCIkbGVuIHwgJGRlc2MgfCAkb2siKQoKICAgIHZhbCBtYXliZTogU3RyaW5nPyA9IG51bGwKICAgIG1heWJlPy5sZXQgeyBwcmludGxuKCJraMO0bmcgaW4gdsOsIG51bGwiKSB9ID86IHByaW50bG4oImxldCArID8uIGzDoCBjw6FjaCB44butIGzDvSBudWxsIGhheSBn4bq3cCIpCn0K
[p-erasure]: ../playground/index.html#code=Ly8gSOG6oW4gY2jhur86IGdlbmVyaWMgYuG7iyB4b8OhIGtp4buDdSBraGkgY2jhuqF5IHRyw6puIEpWTQppbmxpbmUgZnVuIDxyZWlmaWVkIFQ-IExpc3Q8Kj4ub25seU9mKCk6IExpc3Q8VD4gPSBmaWx0ZXJJc0luc3RhbmNlPFQ-KCkKCmZ1biBtYWluKCkgewogICAgdmFsIG1peGVkOiBMaXN0PEFueT4gPSBsaXN0T2YoMSwgImEiLCAyLjUsICJiIikKICAgIC8vIGlmIChtaXhlZCBpcyBMaXN0PFN0cmluZz4pIOKApiAgIOKGkiBs4buXaSBjb21waWxlOiBraMO0bmcga2nhu4NtIMSRxrDhu6NjIGtp4buDdSBnZW5lcmljIGzDumMgY2jhuqF5CiAgICBwcmludGxuKG1peGVkIGlzIExpc3Q8Kj4pICAgICAgICAgIC8vIGNo4buJIGtp4buDbSDEkcaw4bujYyAibMOgIExpc3QiCgogICAgcHJpbnRsbihtaXhlZC5vbmx5T2Y8U3RyaW5nPigpKSAgICAvLyByZWlmaWVkIChow6BtIGlubGluZSkgbeG7m2kgZ2nhu68gxJHGsOG7o2MgVAogICAgcHJpbnRsbihtaXhlZC5vbmx5T2Y8SW50PigpKQp9Cg
