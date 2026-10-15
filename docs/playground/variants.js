// 3 cách viết khác cho mỗi ví dụ trong index.html (khớp theo tên ví dụ).
// Mỗi bản là 1 chương trình chạy được, đầu file có Ý tưởng / Học được / Ưu-Nhược / Dùng khi.
// Sửa ở đây là xong — tab trên playground đọc thẳng từ object này.
window.VARIANTS = {
  "Hello, world": [
    { name: "Cơ bản", code:
`// ===== Variant 1: Cơ bản =====
// Ý tưởng: tách lời chào ra hàm riêng, ghép chuỗi bằng dấu +.
// Học được: fun có kiểu trả về (: String), thân hàm { return ... }, nối chuỗi bằng +.
// Ưu: dễ đọc với người mới. Nhược: ghép nhiều biến bằng + dài và dễ thiếu dấu cách.
// Dùng khi: mới học, chưa quen string template.

fun greet(name: String): String {
    return "Hello, " + name + "!"
}

fun main() {
    val name = "Kotlin"
    val message = greet(name)
    println(message)
}` },
    { name: "Idiomatic", code:
`// ===== Variant 2: Idiomatic Kotlin =====
// Ý tưởng: hàm 1 dòng + tham số mặc định + string template.
// Học được:
//   - expression body: fun f() = ... (bỏ { return })
//   - default argument: name: String = "Kotlin"
//   - named argument: greet(name = "KMP")
//   - string template: "$name", "\${expr}"
// Ưu: ngắn, gọi linh hoạt. Nhược: không có.
// Dùng khi: hầu hết mọi lúc — đây là cách viết Kotlin chuẩn.

fun greet(name: String = "Kotlin", mark: String = "!") = "Hello, $name$mark"

fun main() {
    println(greet())
    println(greet(name = "KMP"))
    println(greet("Android", mark = " 👋"))
    println("Độ dài lời chào: \${greet().length} ký tự")
}` },
    { name: "Nâng cao", code:
`// ===== Variant 3: Nâng cao =====
// Ý tưởng: biến lời chào thành extension function trên String và dựng chuỗi bằng buildString.
// Học được:
//   - extension function: fun String.greet() — gọi như "Kotlin".greet()
//   - buildString { append(...) } — dựng chuỗi nhiều phần, không tạo chuỗi tạm
//   - also { } — làm thêm việc phụ (in log) mà vẫn trả về chính giá trị đó
// Ưu: đọc như câu tiếng Anh, dễ nối chuỗi dài. Nhược: lạm dụng extension làm khó tìm hàm.
// Dùng khi: hàm "thuộc về" một kiểu có sẵn mà bạn không sửa được (String, Int...).

fun String.greet(times: Int = 1): String = buildString {
    append("Hello, ")
    append(this@greet)          // this@greet = chuỗi gọi hàm
    repeat(times) { append('!') }
}

fun main() {
    listOf("Kotlin", "Android", "iOS")
        .map { it.greet(times = it.length / 3) }
        .also { println("Có \${it.size} lời chào:") }
        .forEach(::println)     // function reference thay cho { println(it) }
}` },
  ],
  "Biến: val / var": [
    { name: "Cơ bản", code:
`// ===== Variant 1: Cơ bản =====
// Ý tưởng: dùng var cho mọi thứ, ghi rõ kiểu, cập nhật từng bước.
// Học được: var (gán lại được), khai báo kiểu tường minh (: Int, : Double), += và ++.
// Ưu: thấy rõ giá trị đổi qua từng dòng. Nhược: var nhiều thì khó biết ai đổi giá trị lúc nào.
// Dùng khi: vòng lặp đếm / cộng dồn đơn giản.

fun main() {
    var count: Int = 0
    var total: Int = 0

    val prices: List<Int> = listOf(45_000, 120_000, 30_000)
    for (p in prices) {
        count++
        total += p
    }
    val average: Double = total.toDouble() / count

    println("count = " + count)
    println("total = " + total)
    println("average = " + average)
}` },
    { name: "Idiomatic", code:
`// ===== Variant 2: Idiomatic Kotlin =====
// Ý tưởng: không dùng var nào — mỗi giá trị là val tính từ biểu thức.
// Học được:
//   - val mặc định, chỉ dùng var khi bắt buộc
//   - type inference: bỏ kiểu khi đã rõ
//   - size / sum() / average() thay cho vòng lặp cộng dồn
// Ưu: không ai sửa được giá trị giữa chừng → ít bug. Nhược: cần biết hàm stdlib.
// Dùng khi: luôn ưu tiên cách này.

fun main() {
    val prices = listOf(45_000, 120_000, 30_000)
    val count = prices.size
    val total = prices.sum()
    val average = prices.average()

    println("count = $count, total = $total")
    println("average = \${"%.1f".format(average)}")
}` },
    { name: "Nâng cao", code:
`// ===== Variant 3: Nâng cao =====
// Ý tưởng: các kiểu "val" đặc biệt: hằng lúc compile, tính lười, và thuộc tính có getter.
// Học được:
//   - const val: hằng biết lúc compile (chỉ ở top-level / object, kiểu cơ bản)
//   - by lazy { }: chỉ tính lần đầu dùng tới, rồi nhớ lại
//   - val có get(): tính lại mỗi lần đọc
// Ưu: kiểm soát lúc nào giá trị được tính. Nhược: dễ nhầm lazy với getter.
// Dùng khi: cấu hình cố định (const), giá trị đắt tiền (lazy), giá trị phụ thuộc state (get).

const val VAT_PERCENT = 8

class Cart(private val prices: List<Int>) {
    val total: Int
        get() = prices.sum()                  // tính lại mỗi lần đọc

    val report: String by lazy {              // chỉ chạy 1 lần
        println("  (đang dựng report…)")
        "Giỏ \${prices.size} món, VAT $VAT_PERCENT%"
    }
}

fun main() {
    val cart = Cart(listOf(45_000, 120_000, 30_000))
    println(cart.report)
    println(cart.report)                      // không in "(đang dựng…)" lần nữa
    println("Tổng sau VAT: \${cart.total * (100 + VAT_PERCENT) / 100}")
}` },
  ],
  "Null safety": [
    { name: "Cơ bản", code:
`// ===== Variant 1: Cơ bản =====
// Ý tưởng: kiểm tra null bằng if như Java, rồi để smart cast lo phần còn lại.
// Học được: kiểu nullable (String?), if (x != null), smart cast (trong if, x thành String).
// Ưu: rõ ràng từng nhánh. Nhược: dài, lồng if nhiều tầng.
// Dùng khi: mỗi nhánh null / không null làm nhiều việc khác nhau.

fun main() {
    val city: String? = null
    if (city != null) {
        println("Độ dài: " + city.length)
    } else {
        println("Chưa có thành phố")
    }

    val input = "42"
    val number: Int? = input.toIntOrNull()
    if (number != null) {
        println(number + 1)
    } else {
        println("Không phải số")
    }
}` },
    { name: "Idiomatic", code:
`// ===== Variant 2: Idiomatic Kotlin =====
// Ý tưởng: gom kiểm tra null vào toán tử, mỗi việc 1 dòng.
// Học được:
//   - ?. safe call: null thì trả null, không crash
//   - ?: elvis: giá trị dự phòng khi null
//   - ?.let { }: chỉ chạy khối khi khác null
// Ưu: ngắn, đọc theo dòng chảy. Nhược: chuỗi ?. dài thì khó debug.
// Dùng khi: chỉ cần giá trị mặc định hoặc làm 1 việc nhỏ khi khác null.

fun main() {
    val city: String? = null
    println("Độ dài: \${city?.length ?: 0}")
    println(city ?: "Chưa có thành phố")

    val inputs = listOf("42", "abc", "7")
    for (input in inputs) {
        input.toIntOrNull()?.let { println("$input + 1 = \${it + 1}") }
            ?: println("$input không phải số")
    }
}` },
    { name: "Nâng cao", code:
`// ===== Variant 3: Nâng cao =====
// Ý tưởng: lọc null cả danh sách, tự viết extension cho kiểu nullable, và chặn sớm bằng require.
// Học được:
//   - mapNotNull { }: map và bỏ luôn kết quả null
//   - extension trên String?: gọi được cả khi giá trị là null
//   - takeIf { }: giữ giá trị nếu thoả điều kiện, không thì null
//   - requireNotNull: null thì ném lỗi có message rõ ràng (fail fast)
// Ưu: xử lý cả list gọn, lỗi rõ. Nhược: người mới khó đọc takeIf.
// Dùng khi: dữ liệu từ ngoài vào (form, API) có thể thiếu.

fun String?.orDash(): String = if (this.isNullOrBlank()) "—" else this

fun parsePort(raw: String?): Int {
    val text = requireNotNull(raw) { "Thiếu port" }
    return text.toIntOrNull()?.takeIf { it in 1..65535 }
        ?: throw IllegalArgumentException("Port không hợp lệ: $text")
}

fun main() {
    val inputs = listOf("42", "abc", null, "7")
    println(inputs.mapNotNull { it?.toIntOrNull() })    // [42, 7]

    val city: String? = null
    println("Thành phố: \${city.orDash()}")

    println(parsePort("8080"))
    println(runCatching { parsePort("99999") }.exceptionOrNull()?.message)
}` },
  ],
  "if / when là biểu thức": [
    { name: "Cơ bản", code:
`// ===== Variant 1: Cơ bản =====
// Ý tưởng: chuỗi if / else if, mỗi nhánh return một giá trị.
// Học được: if như câu lệnh, return sớm, so sánh >= / >.
// Ưu: ai từng học ngôn ngữ khác cũng đọc được. Nhược: lặp chữ return, dễ quên nhánh.
// Dùng khi: mới học, hoặc mỗi nhánh cần nhiều dòng xử lý.

fun level(battery: Int): String {
    if (battery >= 80) {
        return "Đầy"
    } else if (battery >= 30) {
        return "Ổn"
    } else if (battery > 0) {
        return "Yếu"
    } else {
        return "Hết pin"
    }
}

fun main() {
    val values = listOf(95, 50, 12, 0)
    for (b in values) {
        var icon = "🟢"
        if (b < 30) {
            icon = "🔴"
        }
        println(icon + " " + b + "% → " + level(b))
    }
}` },
    { name: "Idiomatic", code:
`// ===== Variant 2: Idiomatic Kotlin =====
// Ý tưởng: when (giá trị) với khoảng in a..b, và if là biểu thức gán thẳng vào val.
// Học được:
//   - when (x) { in 80..100 -> ... }: so khớp theo khoảng
//   - nhiều giá trị 1 nhánh: 0, -1 -> ...
//   - val icon = if (...) a else b: không cần var
// Ưu: gọn, nhìn ra ngay các mốc. Nhược: khoảng chồng nhau thì nhánh trên thắng — cần cẩn thận.
// Dùng khi: phân loại theo khoảng số hoặc theo giá trị cụ thể.

fun level(battery: Int) = when (battery) {
    in 80..100 -> "Đầy"
    in 30..79 -> "Ổn"
    in 1..29 -> "Yếu"
    0 -> "Hết pin"
    else -> "Sai số liệu"
}

fun main() {
    for (b in listOf(95, 50, 12, 0, 120)) {
        val icon = if (b < 30) "🔴" else "🟢"
        println("$icon $b% → \${level(b)}")
    }
}` },
    { name: "Nâng cao", code:
`// ===== Variant 3: Nâng cao =====
// Ý tưởng: đưa các mốc vào enum class — dữ liệu và nhãn đi cùng nhau, thêm mức mới không sửa logic.
// Học được:
//   - enum class có thuộc tính: Level(val range: IntRange, val icon: String)
//   - entries: danh sách mọi giá trị enum (Kotlin 1.9+)
//   - firstOrNull { }: tìm phần tử đầu tiên khớp
//   - companion object: hàm "static" gắn với class
// Ưu: thêm/sửa mức chỉ ở 1 chỗ, dùng được Level ở chỗ khác (UI, màu...). Nhược: dài hơn cho bài nhỏ.
// Dùng khi: các mốc dùng lại ở nhiều nơi hoặc cần gắn thêm dữ liệu (màu, icon).

enum class Level(val range: IntRange, val icon: String, val label: String) {
    FULL(80..100, "🟢", "Đầy"),
    OK(30..79, "🟢", "Ổn"),
    LOW(1..29, "🔴", "Yếu"),
    EMPTY(0..0, "⚫", "Hết pin");

    companion object {
        fun of(battery: Int): Level? = entries.firstOrNull { battery in it.range }
    }
}

fun main() {
    for (b in listOf(95, 50, 12, 0, 120)) {
        val lv = Level.of(b)
        println(if (lv == null) "❓ $b% → Sai số liệu" else "\${lv.icon} $b% → \${lv.label} (\${lv.name})")
    }
}` },
  ],
  "Collections": [
    { name: "Cơ bản", code:
`// ===== Variant 1: Cơ bản =====
// Ý tưởng: tự viết vòng for cho từng việc: lọc, đổi, cộng, tìm max.
// Học được: mutableListOf, for (x in list), add(), cộng dồn bằng var.
// Ưu: thấy rõ máy làm gì từng bước. Nhược: dài, mỗi việc 1 vòng lặp, dễ sai biến đếm.
// Dùng khi: hiểu cơ chế trước khi dùng hàm có sẵn.

fun main() {
    val prices = listOf(45_000, 120_000, 30_000, 89_000)

    val expensive = mutableListOf<Int>()
    for (p in prices) {
        if (p > 40_000) expensive.add(p)
    }
    println(expensive)

    var sum = 0
    var max = prices[0]
    for (p in prices) {
        sum += p
        if (p > max) max = p
    }
    println("Tổng: " + sum + ", lớn nhất: " + max)
}` },
    { name: "Idiomatic", code:
`// ===== Variant 2: Idiomatic Kotlin =====
// Ý tưởng: nối các hàm collection thành chuỗi, mỗi bước 1 dòng.
// Học được:
//   - filter / map / sumOf / maxOrNull
//   - sortedDescending, take(n)
//   - joinToString(separator, prefix, postfix) { transform }
//   - partition: chia làm 2 list theo điều kiện
// Ưu: đọc như mô tả yêu cầu. Nhược: mỗi bước tạo list mới (list rất lớn thì tốn bộ nhớ).
// Dùng khi: hầu hết thao tác trên list bình thường.

fun main() {
    val prices = listOf(45_000, 120_000, 30_000, 89_000)

    val top2 = prices.sortedDescending().take(2)
    println("Top 2: " + top2.joinToString(" · ") { "\${it / 1000}k" })

    println("Tổng món > 40k: \${prices.filter { it > 40_000 }.sum()}")
    println("Lớn nhất: \${prices.maxOrNull() ?: 0}")

    val (cheap, expensive) = prices.partition { it < 50_000 }
    println("rẻ = $cheap, đắt = $expensive")
}` },
    { name: "Nâng cao", code:
`// ===== Variant 3: Nâng cao =====
// Ý tưởng: thống kê nhiều chiều trên list đơn hàng: gom nhóm, đếm, fold, và sequence cho dữ liệu lớn.
// Học được:
//   - groupBy { } / groupingBy { }.eachCount()
//   - associate { k to v }: dựng Map từ list
//   - fold(init) { acc, x -> }: gom về 1 giá trị tuỳ ý
//   - asSequence(): xử lý lười, từng phần tử đi hết chuỗi — không tạo list trung gian
// Ưu: mạnh, biểu đạt được thống kê phức tạp. Nhược: cần quen kiểu dữ liệu Map.
// Dùng khi: báo cáo, thống kê, hoặc list hàng nghìn phần tử.

data class Order(val item: String, val price: Int)

fun main() {
    val orders = listOf(
        Order("cà phê", 45_000), Order("trà", 30_000),
        Order("cà phê", 45_000), Order("bánh", 89_000), Order("trà", 35_000),
    )

    println(orders.groupingBy { it.item }.eachCount())            // {cà phê=2, trà=2, bánh=1}
    println(orders.groupBy({ it.item }, { it.price }).mapValues { it.value.sum() })
    println(orders.associate { it.item to it.price })              // trùng key thì giữ cái sau

    val (count, total) = orders.fold(0 to 0) { (c, t), o -> (c + 1) to (t + o.price) }
    println("$count đơn, tổng $total")

    val firstBig = (1..1_000_000).asSequence()
        .map { it * 1_000 }
        .first { it > 90_000 }                                     // chỉ chạy tới phần tử thứ 91
    println("Sequence dừng sớm: $firstBig")
}` },
  ],
  "Lambda & hàm bậc cao": [
    { name: "Cơ bản", code:
`// ===== Variant 1: Cơ bản =====
// Ý tưởng: viết đầy đủ kiểu hàm và lambda, chưa dùng cú pháp rút gọn nào.
// Học được:
//   - kiểu hàm: (Int) -> Unit, (Int) -> Int
//   - lambda đầy đủ: { x: Int -> x * x }
//   - function reference: ::printLine truyền hàm có tên vào chỗ cần lambda
// Ưu: thấy rõ "hàm cũng là một giá trị". Nhược: dài.
// Dùng khi: mới học kiểu hàm.

fun repeatTimes(n: Int, action: (Int) -> Unit) {
    for (i in 1..n) {
        action(i)
    }
}

fun printLine(i: Int) {
    println("Lần " + i)
}

fun main() {
    val square: (Int) -> Int = { x: Int -> x * x }
    println(square(7))

    repeatTimes(3, ::printLine)
    repeatTimes(2, { i: Int -> println("Lambda lần " + i) })
}` },
    { name: "Idiomatic", code:
`// ===== Variant 2: Idiomatic Kotlin =====
// Ý tưởng: dùng cú pháp rút gọn và hàm có sẵn của stdlib.
// Học được:
//   - trailing lambda: f(n) { ... } — lambda cuối nằm ngoài ngoặc
//   - it: tên mặc định khi lambda có 1 tham số
//   - repeat(n) { } có sẵn, khỏi tự viết
//   - _ cho tham số không dùng
// Ưu: gọn, đúng style Kotlin. Nhược: lambda lồng nhau với it dễ nhầm — đặt tên khi lồng.
// Dùng khi: luôn luôn.

fun main() {
    val square = { x: Int -> x * x }
    println(square(7))

    repeat(3) { println("Lần \${it + 1}") }

    val words = listOf("kotlin", "android", "ios", "kmp")
    println(words.sortedBy { it.length })
    println(words.filter { it.length > 3 }.map { it.uppercase() })
    words.forEachIndexed { index, _ -> print("#$index ") }
    println()
}` },
    { name: "Nâng cao", code:
`// ===== Variant 3: Nâng cao =====
// Ý tưởng: hàm trả về hàm, ghép hàm, và inline để lambda không tốn object.
// Học được:
//   - hàm trả về lambda: fun multiplier(k): (Int) -> Int
//   - infix fun + generic: f andThen g
//   - inline fun: compiler chép thân hàm vào chỗ gọi → lambda không tạo object, return được từ main
//   - lambda có receiver: StringBuilder.() -> Unit (cách buildString hoạt động)
// Ưu: viết được DSL, tái dùng logic. Nhược: lạm dụng thì khó đọc.
// Dùng khi: tự viết thư viện nhỏ, DSL, hoặc hàm tiện ích nhận lambda gọi nhiều.

fun multiplier(k: Int): (Int) -> Int = { it * k }

infix fun <A, B, C> ((A) -> B).andThen(next: (B) -> C): (A) -> C = { next(this(it)) }

inline fun measure(label: String, block: () -> Unit) {
    val t0 = System.nanoTime()
    block()
    println("$label: \${(System.nanoTime() - t0) / 1000} µs")
}

fun html(build: StringBuilder.() -> Unit) = StringBuilder().apply(build).toString()

fun main() {
    val triple = multiplier(3)
    val tripleThenText = triple andThen { "= $it" }
    println(tripleThenText(14))

    measure("sum 1..100000") { println((1..100_000L).sum()) }

    println(html { append("<b>"); append("Kotlin"); append("</b>") })
}` },
  ],
  "data class & copy": [
    { name: "Cơ bản", code:
`// ===== Variant 1: Cơ bản =====
// Ý tưởng: dùng class thường và tự viết những gì data class làm hộ — để thấy data class tiết kiệm bao nhiêu.
// Học được: class thường so sánh bằng địa chỉ (==), override toString / equals / hashCode, tự viết copy.
// Ưu: hiểu data class sinh ra cái gì. Nhược: dài, thêm field là phải sửa 4 chỗ.
// Dùng khi: để học — code thật thì dùng data class.

class Reading(val battery: Int, val charging: Boolean) {
    override fun toString(): String = "Reading(battery=" + battery + ", charging=" + charging + ")"

    override fun equals(other: Any?): Boolean {
        if (other !is Reading) return false
        return battery == other.battery && charging == other.charging
    }

    override fun hashCode(): Int = 31 * battery + charging.hashCode()

    fun copy(battery: Int = this.battery, charging: Boolean = this.charging): Reading =
        Reading(battery, charging)
}

fun main() {
    val a = Reading(80, false)
    val b = a.copy(charging = true)
    println(a)
    println(b)
    println(a == Reading(80, false))   // true nhờ tự viết equals
}` },
    { name: "Idiomatic", code:
`// ===== Variant 2: Idiomatic Kotlin =====
// Ý tưởng: data class lo hết toString/equals/copy; dùng nó với list như dữ liệu thật.
// Học được:
//   - data class + giá trị mặc định
//   - copy(field = ...): tạo bản mới đổi 1 phần, bản cũ giữ nguyên (immutable)
//   - destructuring: val (a, b) = obj, kể cả trong lambda { (battery, _) -> }
//   - maxBy / sortedByDescending theo field
// Ưu: ngắn, an toàn. Nhược: chỉ field trong constructor chính được tính vào equals.
// Dùng khi: mọi kiểu "gói dữ liệu" — model, state, DTO.

data class Reading(val battery: Int, val charging: Boolean = false, val label: String = "phone")

fun main() {
    val readings = listOf(
        Reading(80), Reading(35, charging = true), Reading(92, label = "watch"),
    )
    println(readings.maxBy { it.battery })
    println(readings.sortedByDescending { it.battery }.map { it.label })

    val plugged = readings.map { it.copy(charging = true) }
    plugged.forEach { (battery, charging, label) -> println("$label $battery% sạc=$charging") }
    println(readings[0] == Reading(80))
}` },
    { name: "Nâng cao", code:
`// ===== Variant 3: Nâng cao =====
// Ý tưởng: kiểm tra dữ liệu ngay lúc tạo; bọc Int thành value class để không truyền nhầm đơn vị.
// Học được:
//   - @JvmInline value class: kiểu riêng mà lúc chạy vẫn chỉ là Int (không tốn object)
//   - init { require(...) }: chặn dữ liệu sai ngay khi tạo
//   - thuộc tính tính toán trong data class (không nằm trong equals)
//   - operator fun compareTo: so sánh 2 value class bằng < >
// Ưu: compiler chặn lỗi "truyền nhầm % pin vào chỗ nhiệt độ". Nhược: thêm kiểu, thêm code.
// Dùng khi: domain có nhiều số cùng kiểu Int nhưng khác ý nghĩa.

@JvmInline
value class Percent(val value: Int) {
    init { require(value in 0..100) { "Percent phải 0..100, nhận $value" } }
    operator fun compareTo(other: Percent) = value.compareTo(other.value)
    override fun toString() = "$value%"
}

data class Reading(val battery: Percent, val charging: Boolean = false) {
    val isLow get() = battery < Percent(20) && !charging
}

fun main() {
    val r = Reading(Percent(15))
    println("$r · yếu? \${r.isLow}")
    println(r.copy(charging = true).isLow)
    println(runCatching { Percent(150) }.exceptionOrNull()?.message)
}` },
  ],
  "sealed class + when": [
    { name: "Cơ bản", code:
`// ===== Variant 1: Cơ bản =====
// Ý tưởng: dùng abstract class thường (không sealed) và chuỗi if (x is ...).
// Học được: abstract class, kế thừa bằng :, kiểm tra kiểu bằng is, smart cast sau is.
// Ưu: quen với người từ Java. Nhược: compiler không biết hết các lớp con → bắt buộc có nhánh else,
//       thêm state mới mà quên xử lý thì không ai báo.
// Dùng khi: tập lớp con mở, người khác có thể thêm (plugin, thư viện).

abstract class UiState
class Loading : UiState()
class Success(val latencyMs: Long) : UiState()
class Error(val message: String) : UiState()

fun render(state: UiState): String {
    if (state is Loading) {
        return "⏳ Đang đo…"
    } else if (state is Success) {
        return "✅ " + state.latencyMs + " ms"
    } else if (state is Error) {
        return "❌ " + state.message
    } else {
        return "?"                      // bắt buộc phải có
    }
}

fun main() {
    val states = listOf(Loading(), Success(42), Error("Mất mạng"))
    for (s in states) println(render(s))
}` },
    { name: "Idiomatic", code:
`// ===== Variant 2: Idiomatic Kotlin =====
// Ý tưởng: thay vì when bên ngoài, mỗi state tự biết hiển thị mình (đa hình).
// Học được:
//   - sealed class với abstract val / fun
//   - override val trong data class / data object
//   - extension property: val UiState.isDone get() = ...
//   - when (state) không cần else khi đủ nhánh của sealed
// Ưu: thêm state mới → compiler bắt override label. Nhược: logic hiển thị dính vào model.
// Dùng khi: mỗi state có hành vi riêng, ít khi đổi.

sealed class UiState {
    abstract val label: String

    data object Loading : UiState() { override val label = "⏳ Đang đo…" }
    data class Success(val latencyMs: Long) : UiState() { override val label = "✅ $latencyMs ms" }
    data class Error(val message: String) : UiState() { override val label = "❌ $message" }
}

val UiState.isDone: Boolean
    get() = when (this) {
        UiState.Loading -> false
        is UiState.Success, is UiState.Error -> true
    }

fun main() {
    listOf(UiState.Loading, UiState.Success(42), UiState.Error("Mất mạng"))
        .forEach { println("\${it.label}  (xong: \${it.isDone})") }
}` },
    { name: "Nâng cao", code:
`// ===== Variant 3: Nâng cao =====
// Ý tưởng: tổng quát hoá thành Result<T> dùng cho mọi màn — generic + variance + map/fold.
// Học được:
//   - sealed interface Result<out T>: out = covariant, Result<Int> dùng được ở chỗ Result<Number>
//   - Nothing: Loading/Error là Result<Nothing> → hợp với mọi T
//   - inline fun <T, R> Result<T>.map { }: biến đổi dữ liệu, giữ nguyên Loading/Error
//   - fold: 1 hàm xử lý đủ 3 nhánh
// Ưu: viết 1 lần, dùng cho mọi API. Nhược: generic làm người mới khó đọc.
// Dùng khi: nhiều màn có cùng kiểu Loading / Success / Error.

sealed interface Result<out T> {
    data object Loading : Result<Nothing>
    data class Success<T>(val data: T) : Result<T>
    data class Error(val message: String) : Result<Nothing>
}

inline fun <T, R> Result<T>.map(f: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(f(data))
    is Result.Error -> this
    Result.Loading -> Result.Loading
}

inline fun <T, R> Result<T>.fold(loading: () -> R, success: (T) -> R, error: (String) -> R): R = when (this) {
    Result.Loading -> loading()
    is Result.Success -> success(data)
    is Result.Error -> error(message)
}

fun main() {
    val states: List<Result<Long>> = listOf(Result.Loading, Result.Success(42L), Result.Error("Mất mạng"))
    states
        .map { r -> r.map { ms -> ms * 1000 } }                // ms → µs
        .forEach { r ->
            println(r.fold({ "⏳ Đang đo…" }, { "✅ $it µs" }, { "❌ $it" }))
        }
}` },
  ],
  "Extension function": [
    { name: "Cơ bản", code:
`// ===== Variant 1: Cơ bản =====
// Ý tưởng: hàm tiện ích thường, nhận giá trị làm tham số — kiểu "Utils" của Java.
// Học được: hàm top-level, vòng for trên chuỗi, StringBuilder, indexOf.
// Ưu: rõ ràng, không có "phép màu". Nhược: gọi formatVnd(x) đọc ngược, phải nhớ tên hàm utils.
// Dùng khi: logic không gắn với kiểu nào cụ thể.

fun formatVnd(amount: Int): String {
    val digits = amount.toString()
    val sb = StringBuilder()
    for (i in digits.indices) {
        if (i > 0 && (digits.length - i) % 3 == 0) sb.append('.')
        sb.append(digits[i])
    }
    return sb.toString() + " ₫"
}

fun isEmail(text: String): Boolean {
    val at = text.indexOf('@')
    if (at <= 0) return false
    return text.indexOf('.', at) > at + 1
}

fun main() {
    println(formatVnd(1250000))
    println(isEmail("tu@gmail.com"))
    println(isEmail("abc"))
}` },
    { name: "Idiomatic", code:
`// ===== Variant 2: Idiomatic Kotlin =====
// Ý tưởng: extension function / property — gọi như hàm có sẵn của Int, String.
// Học được:
//   - fun Int.toVnd(): this là số đang gọi
//   - val String.isEmail get() = ...: extension property
//   - "%,d".format(...): định dạng có dấu phân cách nghìn
//   - Regex + matches
// Ưu: đọc tự nhiên (1250000.toVnd()), IDE gợi ý khi gõ dấu chấm. Nhược: không truy cập được private của class.
// Dùng khi: thêm tiện ích cho kiểu mình không sở hữu.

fun Int.toVnd(): String = "%,d".format(this).replace(',', '.') + " ₫"

private val EMAIL = Regex("""[\\w.+-]+@[\\w-]+\\.[\\w.]+""")
val String.isEmail: Boolean get() = matches(EMAIL)

fun main() {
    println(1250000.toVnd())
    listOf("tu@gmail.com", "abc", "a@b").forEach { println("$it → \${it.isEmail}") }
}` },
    { name: "Nâng cao", code:
`// ===== Variant 3: Nâng cao =====
// Ý tưởng: extension trên kiểu nullable, generic, infix — và extension chỉ có hiệu lực trong 1 phạm vi.
// Học được:
//   - fun Int?.toVndOr(...): gọi được trên giá trị null
//   - fun <T> List<T>.second(): extension generic
//   - infix fun Int.percentOf(total): viết 15 percentOf 200_000
//   - extension là static dispatch: chọn theo kiểu khai báo, không theo kiểu lúc chạy
// Ưu: mở rộng API rất linh hoạt. Nhược: quá nhiều extension toàn cục làm "ô nhiễm" gợi ý code.
// Dùng khi: viết thư viện tiện ích, DSL.

fun Int?.toVndOr(fallback: String = "—"): String =
    this?.let { "%,d".format(it).replace(',', '.') + " ₫" } ?: fallback

fun <T> List<T>.second(): T? = getOrNull(1)

infix fun Int.percentOf(total: Int): Int = total * this / 100

open class Shape
class Circle : Shape()
fun Shape.name() = "Shape"
fun Circle.name() = "Circle"

fun main() {
    val price: Int? = null
    println(price.toVndOr())
    println(1250000.toVndOr())
    println(listOf("a", "b", "c").second())
    println((15 percentOf 200_000).toVndOr())

    val s: Shape = Circle()
    println(s.name())          // "Shape" — static dispatch theo kiểu khai báo
}` },
  ],
  "Coroutines": [
    { name: "Cơ bản", code:
`// ===== Variant 1: Cơ bản =====
// Ý tưởng: gọi tuần tự từng suspend function — chờ cái này xong mới tới cái kia.
// Học được: suspend fun, runBlocking (cầu nối code thường ↔ coroutine), delay không chặn thread.
// Ưu: dễ hiểu, thứ tự rõ ràng. Nhược: chậm — tổng thời gian = cộng dồn (300 + 200 = 500ms).
// Dùng khi: bước sau cần kết quả bước trước.

import kotlinx.coroutines.*

suspend fun fetch(name: String, ms: Long): String {
    delay(ms)
    return name + " xong sau " + ms + "ms"
}

fun main() = runBlocking {
    val start = System.currentTimeMillis()
    val a = fetch("pin", 300)
    val b = fetch("gps", 200)
    println(a)
    println(b)
    println("Tổng ≈ " + (System.currentTimeMillis() - start) + "ms (tuần tự)")
}` },
    { name: "Idiomatic", code:
`// ===== Variant 2: Idiomatic Kotlin =====
// Ý tưởng: chạy song song nhiều việc độc lập trong coroutineScope, chờ tất cả bằng awaitAll.
// Học được:
//   - coroutineScope { }: structured concurrency — 1 con lỗi thì cả nhóm huỷ, không rò coroutine
//   - async { } + awaitAll(): song song, lấy list kết quả
//   - map { async { } }: chạy song song cả danh sách
// Ưu: nhanh (≈ việc lâu nhất), an toàn khi lỗi. Nhược: các việc phải độc lập với nhau.
// Dùng khi: gọi nhiều API / đọc nhiều sensor cùng lúc.

import kotlinx.coroutines.*

suspend fun fetch(name: String, ms: Long): String {
    delay(ms)
    return "$name xong sau \${ms}ms"
}

suspend fun fetchAll(jobs: Map<String, Long>): List<String> = coroutineScope {
    jobs.map { (name, ms) -> async { fetch(name, ms) } }.awaitAll()
}

fun main() = runBlocking {
    val start = System.currentTimeMillis()
    fetchAll(mapOf("pin" to 300L, "gps" to 200L, "mạng" to 250L)).forEach(::println)
    println("Tổng ≈ \${System.currentTimeMillis() - start}ms (song song)")
}` },
    { name: "Nâng cao", code:
`// ===== Variant 3: Nâng cao =====
// Ý tưởng: dữ liệu đến liên tục → dùng Flow; đặt timeout để không treo mãi.
// Học được:
//   - flow { emit(x) }: dòng giá trị lạnh, chỉ chạy khi có người collect
//   - map / filter / take trên Flow, collect { } để nhận
//   - onEach: làm việc phụ (log) giữa chừng
//   - withTimeoutOrNull(ms): quá giờ thì huỷ và trả null thay vì treo
// Ưu: hợp với sensor, websocket, UI state. Nhược: thêm khái niệm (cold/hot, operator).
// Dùng khi: giá trị thay đổi theo thời gian (pin, GPS, tốc độ mạng...).

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun batteryFlow(): Flow<Int> = flow {
    var level = 100
    while (true) {
        emit(level)
        level -= 7
        delay(100)
    }
}

fun main() = runBlocking {
    batteryFlow()
        .map { it.coerceAtLeast(0) }
        .onEach { if (it < 80) println("  ⚠️ pin còn $it%") }
        .take(5)
        .collect { println("pin = $it%") }

    val slow = withTimeoutOrNull(150) { delay(1_000); "xong" }
    println("Gọi chậm: \${slow ?: "quá 150ms → huỷ"}")
}` },
  ],
  "Tip Split (dùng args)": [
    { name: "Cơ bản", code:
`// ===== Variant 1: Cơ bản =====
// Ý tưởng: đọc từng argument bằng if, có giá trị mặc định, tính bằng số nguyên (đồng không có lẻ).
// Học được: args.size, toLong() / toInt(), if-else gán giá trị, chia nguyên Long.
// Ưu: thấy rõ từng bước. Nhược: nhập chữ là crash (NumberFormatException) — thử điền: abc 10 2.
// Dùng khi: script nhỏ, dữ liệu chắc chắn đúng.
// Điền Program arguments: 450000 15 3

fun main(args: Array<String>) {
    var bill: Long = 450_000
    var tipPercent: Int = 10
    var people: Int = 2

    if (args.size > 0) bill = args[0].toLong()
    if (args.size > 1) tipPercent = args[1].toInt()
    if (args.size > 2) people = args[2].toInt()
    if (people < 1) people = 1

    val tip = bill * tipPercent / 100
    val perPerson = (bill + tip) / people

    println("Bill:      " + bill)
    println("Tip " + tipPercent + "%:   " + tip)
    println("Mỗi người: " + perPerson + " (" + people + " người)")
}` },
    { name: "Idiomatic", code:
`// ===== Variant 2: Idiomatic Kotlin =====
// Ý tưởng: gom input thành data class, phần tính toán là hàm thuần (không in, không đọc args) → dễ test.
// Học được:
//   - getOrNull + toXxxOrNull + ?: → không bao giờ crash vì input xấu
//   - data class + thuộc tính tính toán (val tip get() = ...)
//   - tách "đọc input" / "tính" / "in" thành 3 phần
//   - trimIndent() cho chuỗi nhiều dòng
// Ưu: input sai thì dùng mặc định, logic test được riêng. Nhược: dài hơn bản cơ bản.
// Dùng khi: code thật — đây là cách module shared của Tip Split làm.
// Điền Program arguments: 450000 15 3

data class Split(val bill: Long, val tipPercent: Int, val people: Int) {
    val tip get() = bill * tipPercent / 100
    val perPerson get() = (bill + tip) / people
}

fun parse(args: Array<String>) = Split(
    bill = args.getOrNull(0)?.toLongOrNull() ?: 450_000,
    tipPercent = args.getOrNull(1)?.toIntOrNull() ?: 10,
    people = args.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(1) ?: 2,
)

fun Long.vnd() = "%,d ₫".format(this).replace(',', '.')

fun main(args: Array<String>) {
    val s = parse(args)
    println("""
        Bill:      \${s.bill.vnd()}
        Tip \${s.tipPercent}%:   \${s.tip.vnd()}
        Mỗi người: \${s.perPerson.vnd()} (\${s.people} người)
    """.trimIndent())
}` },
    { name: "Nâng cao", code:
`// ===== Variant 3: Nâng cao =====
// Ý tưởng: input xấu thì báo lỗi rõ ràng thay vì lặng lẽ dùng mặc định; chia lẻ không mất đồng nào.
// Học được:
//   - runCatching { } + Result: gom lỗi, xử lý bằng onSuccess / onFailure
//   - require(...) { "message" }: kiểm tra điều kiện, sai thì ném IllegalArgumentException
//   - @JvmInline value class Vnd: tiền là kiểu riêng, không cộng nhầm với số người
//   - chia dư: người đầu trả thêm phần lẻ để tổng khớp đúng bill + tip
// Ưu: không mất tiền vì làm tròn, lỗi dễ hiểu. Nhược: nhiều khái niệm cùng lúc.
// Dùng khi: tính tiền thật — sai 1 đồng cũng là bug.
// Điền Program arguments: 100000 10 3  (rồi thử: 100000 abc 3)

@JvmInline
value class Vnd(val amount: Long) {
    operator fun plus(o: Vnd) = Vnd(amount + o.amount)
    override fun toString() = "%,d ₫".format(amount).replace(',', '.')
}

fun shares(total: Vnd, people: Int): List<Vnd> {
    val base = total.amount / people
    val rest = total.amount % people
    return List(people) { i -> Vnd(base + if (i < rest) 1 else 0) }
}

fun main(args: Array<String>) {
    runCatching {
        val bill = Vnd(args.getOrElse(0) { "100000" }.toLong())
        val tipPercent = args.getOrElse(1) { "10" }.toInt()
        val people = args.getOrElse(2) { "3" }.toInt()
        require(people >= 1) { "Số người phải ≥ 1" }
        require(tipPercent in 0..100) { "Tip phải 0..100%" }

        val total = bill + Vnd(bill.amount * tipPercent / 100)
        total to shares(total, people)
    }.onSuccess { (total, parts) ->
        println("Tổng: $total")
        parts.forEachIndexed { i, p -> println("  Người \${i + 1}: $p") }
        println("Kiểm tra: \${parts.sumOf { it.amount } == total.amount}")
    }.onFailure {
        println("❌ Input sai: \${it.message}")
    }
}` },
  ],
};
