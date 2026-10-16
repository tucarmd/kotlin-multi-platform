# Lỗi Kotlin phân loại theo bản chất

Trang này gom **khoảng 100 lỗi và vấn đề hay gặp nhất** khi viết Kotlin (ngôn ngữ, coroutines, Android, Compose, KMP, iOS), rồi xếp chúng vào **8 nhóm theo bản chất**: lỗi do mình viết sai, code dở, chậm, hay do OS / ngôn ngữ / bản chất bài toán giới hạn.

Biết lỗi thuộc nhóm nào thì biết ngay **sửa ở đâu** và **có sửa hết được không**: lỗi code thì sửa là hết, còn hạn chế của OS thì chỉ thiết kế để sống chung.

Các trang liên quan:

- Tra theo **chủ đề** (null safety, collections, coroutines…): [900 lỗi Kotlin hay gặp](../issues/index.md).
- Bẫy ngôn ngữ có code chạy thử: [Kotlin khó gì, bẫy gì](kotlin-pitfalls.md).
- Lỗi Gradle, Xcode của repo này: [Lỗi hay gặp](troubleshooting.md).

!!! info "Số liệu lấy ở đâu"
    Số **vote / lượt xem** lấy từ Stack Exchange API (api.stackexchange.com) ngày **15/10/2026**, sắp theo vote. `1.2k` = 1 200.
    Câu hỏi KMP và Kotlin/Native ít vote vì cộng đồng hỏi chủ yếu trên Kotlin Slack và YouTrack, nên các mục đó dẫn docs chính chủ thay cho vote.
    Tag `kotlin` trên StackOverflow có khoảng 97 600 câu hỏi, `kotlin-coroutines` khoảng 4 800, `kotlin-flow` khoảng 1 000.

## 8 nhóm

| # | Nhóm | Dấu hiệu nhận ra | Sửa ở đâu | Sửa hết được không |
|:-:|---|---|---|---|
| 1 | [Lỗi biên dịch & build](#1-loi-bien-dich-build) | Không chạy được, báo lỗi khi build hoặc Sync | Code, `build.gradle.kts`, phiên bản thư viện | ✅ Có. Compiler chỉ đúng chỗ |
| 2 | [Lỗi code: chạy sai / crash](#2-loi-code-chay-sai-crash) | Build được, chạy thì crash hoặc ra kết quả sai | Code của mình | ✅ Có. Thêm test để không tái phát |
| 3 | [Bất đồng bộ & vòng đời](#3-bat-dong-bo-vong-oi) | Lúc được lúc không, phụ thuộc thời điểm, xoay màn, ra nền | Scope, dispatcher, cách collect | ✅ Có, nhưng khó tái hiện. Phải hiểu mô hình |
| 4 | [Code dở](#4-code-do) | Chạy đúng nhưng khó đọc, khó test, sửa chỗ này hỏng chỗ kia | Thiết kế, cách chia lớp | ✅ Có. Refactor dần |
| 5 | [Hiệu năng kém](#5-hieu-nang-kem) | Giật, ANR, build lâu, tốn pin | Thread, cấu trúc dữ liệu, cấu hình build | 🟡 Phần lớn. Đo trước rồi mới sửa |
| 6 | [Hạn chế của OS](#6-han-che-cua-os-android-ios) | Code đúng nhưng OS chặn, chỉ hỏng trên một số phiên bản Android/iOS | Manifest, Info.plist, quyền, kiến trúc chạy nền | ❌ Không. OS quyết, mình thiết kế quanh nó |
| 7 | [Hạn chế của ngôn ngữ & interop](#7-han-che-cua-ngon-ngu-interop) | "Kotlin không cho viết thế này", Swift thấy API Kotlin rất xấu | Cách viết khác, thư viện hỗ trợ (SKIE…) | ❌ Không. Chờ ngôn ngữ cải tiến hoặc né |
| 8 | [Giới hạn logic / bản chất](#8-gioi-han-logic-ban-chat) | Không phải lỗi của Kotlin: ngôn ngữ nào cũng bị | Thuật toán, kiểu dữ liệu, cách lưu | ❌ Không. Chọn cách biểu diễn đúng |

### Lỗi này thuộc nhóm nào?

```
Build có chạy không?
├─ Không ─────────────────────────────────────────▶ 1. Biên dịch & build
└─ Có → Có sai / crash không?
        ├─ Sai mọi lần, ở mọi máy ───────────────────▶ 2. Lỗi code   (hoặc 8 nếu viết bằng ngôn ngữ nào cũng sai)
        ├─ Lúc được lúc không, theo thời điểm ──────▶ 3. Bất đồng bộ & vòng đời
        ├─ Chỉ hỏng ở một số phiên bản OS / khi ra nền ▶ 6. Hạn chế OS
        └─ Không sai
             ├─ Chậm, giật, nóng máy ─────────────────▶ 5. Hiệu năng
             ├─ Khó đọc, khó test, khó sửa ───────────▶ 4. Code dở
             └─ Muốn viết mà ngôn ngữ không cho ────▶ 7. Hạn chế ngôn ngữ
```

## Top 20 được hỏi nhiều nhất

Xếp theo vote trên StackOverflow (ngôn ngữ và Android gộp chung).

| # | Lỗi | Nhóm | Vote / xem |
|:-:|---|---|---|
| 1 | `NetworkOnMainThreadException` | 2 · Lỗi code | [2.7k / 1.55M](https://stackoverflow.com/questions/6343166) |
| 2 | `Cleartext HTTP traffic to X not permitted` | 6 · OS | [1.8k / 1.67M](https://stackoverflow.com/questions/45940861) |
| 3 | `Activity has leaked window` | 2 · Lỗi code | [1.3k / 717k](https://stackoverflow.com/questions/2850573) |
| 4 | `Only the original thread that created a view hierarchy can touch its views` | 2 · Lỗi code | [1.2k / 840k](https://stackoverflow.com/questions/5161951) |
| 5 | Kotlin không có `static`, `companion object` để làm gì | 7 · Ngôn ngữ | [1.1k / 630k](https://stackoverflow.com/questions/40352684) |
| 6 | `lateinit property has not been initialized` | 2 · Lỗi code | [885 / 411k](https://stackoverflow.com/questions/37618738) |
| 7 | `Cannot inline bytecode built with JVM target 1.8 into … 1.6` | 1 · Build | [791 / 290k](https://stackoverflow.com/questions/48988778) |
| 8 | `Can not perform this action after onSaveInstanceState` | 3 · Vòng đời | [579 / 439k](https://stackoverflow.com/questions/7575921) |
| 9 | `by lazy` hay `lateinit` | 4 · Code dở | [517 / 259k](https://stackoverflow.com/questions/36623177) |
| 10 | `Not allowed to start service Intent` (Android 8) | 6 · OS | [480 / 336k](https://stackoverflow.com/questions/46445265) |
| 11 | `Duplicate class kotlin.collections.jdk8…` | 1 · Build | [453 / 308k](https://stackoverflow.com/questions/75263047) |
| 12 | `Smart cast to 'X' is impossible, because 'y' is a mutable property` | 1 · Build | [434 / 196k](https://stackoverflow.com/questions/44595529) |
| 13 | `A failure occurred while executing …KaptExecution` | 1 · Build | [414 / 620k](https://stackoverflow.com/questions/62131564) |
| 14 | `Module was compiled with an incompatible version of Kotlin` | 1 · Build | [408 / 675k](https://stackoverflow.com/questions/67699823) |
| 15 | `startForegroundService() did not then call startForeground()` | 6 · OS | [400 / 373k](https://stackoverflow.com/questions/44425584) |
| 16 | `!!` dùng khi nào | 4 · Code dở | [345 / 193k](https://stackoverflow.com/questions/34342413) |
| 17 | `TransactionTooLargeException` | 6 · OS | [338 / 293k](https://stackoverflow.com/questions/11451393) |
| 18 | `compileJava (11) and compileKotlin (1.8) jvm target … same Java version` | 1 · Build | [335 / 345k](https://stackoverflow.com/questions/69079963) |
| 19 | `break` / `continue` trong `forEach` | 2 · Lỗi code | [330 / 282k](https://stackoverflow.com/questions/32540947) |
| 20 | `BadTokenException: Unable to add window` | 2 · Lỗi code | [310 / 201k](https://stackoverflow.com/questions/2634991) |

**Đọc từ số liệu:**

- **6/20 là lỗi build và phiên bản** (JVM target, Kotlin metadata, kapt, trùng class). Người mới mất nhiều thời gian vì Gradle hơn vì chính Kotlin. Khảo sát của JetBrains cũng cho thấy hiệu năng IDE và build là nỗi đau lớn nhất ([JetBrains, 2022](https://blog.jetbrains.com/kotlin/2022/11/how-kotlin-is-going-to-fix-your-pains-in-2023/)).
- **5/20 là "sai thread"**: chạy mạng trên main, sửa UI từ thread khác, làm việc sau khi Activity đã chết. Coroutine sinh ra để giải quyết đúng nhóm này.
- **4/20 là OS chặn.** Mỗi bản Android siết thêm một chút, code cũ bỗng hỏng dù không ai sửa.
- Câu hỏi về coroutines ít vote hơn hẳn, nhưng bug coroutine thường khó tìm nhất vì không phải lần nào cũng xảy ra (nhóm 3).

## 1. Lỗi biên dịch & build

Không chạy được. Đây là nhóm dễ sửa nhất vì compiler chỉ đúng chỗ, chỉ cần đọc đúng dòng lỗi **đầu tiên**.

| Thông báo lỗi | Vì sao | Sửa | Nguồn |
|---|---|---|---|
| `Execution failed for task ':app:compileDebugKotlin'. Compilation error` | Gradle gói lỗi thật ở chỗ khác | Mở Build Output, tìm dòng `e:` đầu tiên | [281 / 600k](https://stackoverflow.com/questions/43848845) |
| `Smart cast to 'X' is impossible, because 'y' is a mutable property…` | `var`, property khác module hoặc có getter riêng: compiler không chắc giá trị không đổi sau khi kiểm tra null | Copy ra `val` cục bộ, hoặc `y?.let { }` | [434 / 196k](https://stackoverflow.com/questions/44595529) |
| `Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver` | Gọi thẳng vào kiểu `X?` | `?.`, `?:`, kiểm tra null. Đừng chữa bằng `!!` | [208 / 101k](https://stackoverflow.com/questions/34498562) |
| `Type mismatch: inferred type is String? but String was expected` | Truyền giá trị có thể null vào chỗ không nhận null | `x ?: ""` hoặc cho tham số nhận `String?` | [docs](https://kotlinlang.org/docs/null-safety.html) |
| `Unresolved reference: X` | Thiếu import/thư viện, sai source set (KMP), chưa Sync, cache IDE cũ | Thêm dependency đúng source set → Sync → Invalidate Caches | [178 / 118k](https://stackoverflow.com/questions/33165324) |
| `Suspend function 'x' should be called only from a coroutine…` | Gọi hàm `suspend` từ hàm thường | Đánh dấu hàm gọi là `suspend`, hoặc `scope.launch { }`. Không dùng `runBlocking` để chữa | [82 / 104k](https://stackoverflow.com/questions/53928668) |
| `'when' expression must be exhaustive` | Thiếu nhánh của enum/sealed (từ 1.7 áp cả cho `when` dạng câu lệnh) | Thêm nhánh còn thiếu. Với sealed đừng thêm `else`, để compiler nhắc khi có loại mới | [29k xem](https://stackoverflow.com/questions/52812997) |
| `Operator '==' cannot be applied to 'Long' and 'Int'` | Kotlin không tự đổi kiểu số | `x == 1L` hoặc `x.toInt()` | [65 / 55k](https://stackoverflow.com/questions/43721362) |
| `None of the following functions can be called with the arguments supplied` | Sai kiểu tham số, hay gặp `Int`/`Long`/`Float` | Đổi kiểu tường minh: `toLong()`… | [28 / 85k](https://stackoverflow.com/questions/44255630) |
| `Overload resolution ambiguity` | Hai hàm cùng khớp (import `*`, extension trùng tên) | Ghi rõ kiểu hoặc tên đầy đủ | [52 / 63k](https://stackoverflow.com/questions/46045377) |
| `Val cannot be reassigned` (gán vào tham số hàm) | Tham số hàm luôn là `val` | Tạo `var` cục bộ | [49 / 88k](https://stackoverflow.com/questions/42540035) |
| `Expecting member declaration` | Viết câu lệnh thẳng trong thân class | Đưa vào `init { }` hoặc một hàm | [18 / 111k](https://stackoverflow.com/questions/44368741) |
| `'lateinit' modifier is not allowed on properties of primitive types` | `lateinit` không dùng với `Int`, `Boolean`… | Giá trị mặc định hoặc `Delegates.notNull()` | [61 / 39k](https://stackoverflow.com/questions/44381791) |
| `Platform declaration clash: … same JVM signature` | Sau khi xoá kiểu generic, 2 hàm trùng chữ ký trên JVM | `@JvmName("…")` hoặc đổi tên | [103 / 71k](https://stackoverflow.com/questions/29268526) |
| `Cannot inline bytecode built with JVM target 1.8 into … 1.6` | Thư viện build cho JVM mới hơn module | Đặt cùng `jvmTarget` ở mọi module | [791 / 290k](https://stackoverflow.com/questions/48988778) |
| `Inconsistent JVM-target compatibility detected` / `compileJava (11) and compileKotlin (1.8)…` | Java và Kotlin nhắm 2 bản JVM khác nhau | `kotlin { jvmToolchain(17) }` | [335 / 345k](https://stackoverflow.com/questions/69079963), [96 / 102k](https://stackoverflow.com/questions/77520506) |
| `Module was compiled with an incompatible version of Kotlin` | Thư viện dùng Kotlin mới hơn plugin Kotlin của project | Nâng plugin Kotlin ≥ bản thư viện dùng | [408 / 675k](https://stackoverflow.com/questions/67699823) |
| `Duplicate class kotlin.collections.jdk8…` | Trộn `kotlin-stdlib-jdk7/8` cũ với stdlib 1.8+ | Đồng bộ phiên bản Kotlin (BOM) | [453 / 308k](https://stackoverflow.com/questions/75263047) |
| `This version of the Compose Compiler requires Kotlin version X` | Compose compiler và Kotlin lệch bản | Kotlin 2.0+: dùng plugin `org.jetbrains.kotlin.plugin.compose` | [63 / 72k](https://stackoverflow.com/questions/75128041) |
| `A failure occurred while executing …KaptExecution` | Annotation processor (Room, Dagger) lỗi, lỗi thật nằm phía trên log | Đọc lỗi đầu tiên; chuyển sang KSP | [414 / 620k](https://stackoverflow.com/questions/62131564) |
| `Could not find method kapt()` | Chưa áp plugin `kotlin-kapt` | Thêm plugin, hoặc dùng KSP | [160 / 97k](https://stackoverflow.com/questions/53823451) |
| `android:exported needs to be explicitly specified` | Target SDK 31+ có `intent-filter` | Ghi rõ `android:exported` | [256 / 289k](https://stackoverflow.com/questions/68554294) |
| `building for iOS Simulator, but linking in object file built for iOS` | Framework thiếu bản cho simulator Apple Silicon | Thêm target `iosSimulatorArm64()` | [25 / 43k](https://stackoverflow.com/questions/69962551) |

## 2. Lỗi code: chạy sai / crash

Build được nhưng chạy sai. Đây là lỗi **của mình**: sửa xong nên viết một test để nó không quay lại.

### 2a. Null và khởi tạo

| Triệu chứng | Vì sao | Sửa | Nguồn |
|---|---|---|---|
| `UninitializedPropertyAccessException: lateinit property x has not been initialized` | Đọc trước khi gán (sai thứ tự vòng đời, DI) | Gán sớm hơn, `::x.isInitialized`, hoặc đổi sang nullable / `by lazy` | [885 / 411k](https://stackoverflow.com/questions/37618738) |
| `NullPointerException` ở dòng có `!!` | `!!` tắt null safety ở đúng chỗ đó | `?:` kèm mặc định, `requireNotNull(x) { "lý do" }` | [345 / 193k](https://stackoverflow.com/questions/34342413) · [pitfalls 2.5](kotlin-pitfalls.md#25-va-lateinit-hen-gio-crash) |
| `Parameter specified as non-null is null` | Java/reflection truyền null vào tham số Kotlin không nhận null (platform type) | Khai báo tham số `T?` ở ranh giới với Java | [90 / 141k](https://stackoverflow.com/questions/47849219) · [pitfalls 2.4](kotlin-pitfalls.md#24-kieu-tu-java-lach-null-safety) |
| Gson tạo data class có `null` ở field không-null, giá trị mặc định bị bỏ qua | Gson tạo object không qua constructor | Dùng kotlinx.serialization hoặc Moshi | [155 / 163k](https://stackoverflow.com/questions/44117970) |
| `Serializer for class 'X' is not found` / `Polymorphic serializer was not found` | Thiếu plugin serialization, thiếu `@Serializable`, lớp con sealed chưa đăng ký | Áp plugin, đánh dấu mọi lớp con | [67 / 77k](https://stackoverflow.com/questions/71988144) |

### 2b. Collection, so sánh, luồng điều khiển

| Triệu chứng | Vì sao | Sửa | Nguồn |
|---|---|---|---|
| `ConcurrentModificationException` khi xoá trong `forEach` | Sửa list khi đang duyệt nó | `removeAll { }`, `filter`, hoặc iterator | [75 / 55k](https://stackoverflow.com/questions/50032000) |
| `return` trong `forEach` thoát cả hàm; `break`/`continue` không compile | `return` trần trong lambda inline là non-local return | `return@forEach`, hoặc vòng `for` thường | [330 / 282k](https://stackoverflow.com/questions/32540947) · [pitfalls 2.7](kotlin-pitfalls.md#27-return-trong-lambda-thoat-luon-ham-ben-ngoai) |
| `arr1 == arr2` ra `false` dù nội dung giống | `Array` so theo địa chỉ | `contentEquals`, hoặc dùng `List` | [65 / 64k](https://stackoverflow.com/questions/35272761) |
| Sửa bản `copy()` làm đổi luôn bản gốc | `copy()` là copy nông | Dùng `List` chỉ đọc, tạo list mới khi đổi | [85 / 107k](https://stackoverflow.com/questions/47359496) |
| 2 data class `==` nhau dù khác một field | `equals` chỉ tính property trong constructor chính | Đưa field vào constructor | [82 / 76k](https://stackoverflow.com/questions/37524422) |
| Object "biến mất" khỏi `HashSet`/key của `Map` sau khi sửa | Data class có `var`, `hashCode` đổi sau khi đã thêm vào | Chỉ dùng `val` cho object làm key | [docs](https://kotlinlang.org/docs/data-classes.html) |
| `StackOverflowError` ở `toString`/`hashCode` | 2 data class trỏ qua lại nhau | Bỏ tham chiếu ngược ra khỏi constructor | [SO](https://stackoverflow.com/questions/48926704) |
| `split(".")` không tách như Java | `split(String)` của Kotlin tách theo chuỗi thường, không phải regex | `split(Regex("\\s+"))`, rồi `filter { it.isNotEmpty() }` | [30 / 38k](https://stackoverflow.com/questions/48379981) |

### 2c. Android và Compose: sai thread, sai vòng đời

| Triệu chứng | Vì sao | Sửa | Nguồn |
|---|---|---|---|
| `NetworkOnMainThreadException` | Gọi mạng trên UI thread | `withContext(Dispatchers.IO)`, Retrofit/Ktor `suspend` | [2.7k / 1.55M](https://stackoverflow.com/questions/6343166) |
| `Only the original thread that created a view hierarchy can touch its views` | Sửa UI từ thread nền | Cập nhật state trên `Dispatchers.Main` | [1.2k / 840k](https://stackoverflow.com/questions/5161951) |
| `Activity has leaked window` | Dialog còn mở khi Activity đã huỷ | Đóng trong `onDestroy`, hoặc dùng `DialogFragment` | [1.3k / 717k](https://stackoverflow.com/questions/2850573) |
| `BadTokenException: Unable to add window -- token null` | Mở dialog bằng Application context hoặc sau khi Activity chết | Dùng Activity context, kiểm tra vòng đời | [310 / 201k](https://stackoverflow.com/questions/2634991) |
| `LifecycleOwners must call register before they are STARTED` | Gọi `registerForActivityResult` trong `onClick` | Đăng ký ở field hoặc `onCreate` | [124 / 77k](https://stackoverflow.com/questions/64476827) |
| `Cannot create an instance of class …ViewModel` | ViewModel có tham số mà không có Factory | `ViewModelProvider.Factory`, Koin, hoặc `@HiltViewModel` | [SO](https://stackoverflow.com/questions/50969022) |
| Mất dữ liệu khi app bị OS kill dù đã dùng ViewModel | ViewModel sống qua xoay màn nhưng **không** sống qua process death | `SavedStateHandle`, `rememberSaveable` cho state nhỏ | [docs](https://developer.android.com/topic/libraries/architecture/saving-states) |
| `shouldShowRequestPermissionRationale` trả `false` cả lần đầu lẫn khi "Không hỏi lại" | API được thiết kế mơ hồ | Tự lưu đã hỏi chưa; bị từ chối hẳn thì đưa người dùng vào Settings | [228 / 127k](https://stackoverflow.com/questions/32347532) |
| `Targeting S+ requires FLAG_IMMUTABLE or FLAG_MUTABLE` | `PendingIntent` thiếu cờ | Thêm `FLAG_IMMUTABLE` | [208 / 182k](https://stackoverflow.com/questions/68473542) |
| State Compose bị reset; cảnh báo `Creating a state object during composition without using remember` | `mutableStateOf` không có `remember` | `remember { mutableStateOf(…) }` | [111 / 61k](https://stackoverflow.com/questions/66169601) |
| List đổi mà UI Compose không vẽ lại | Sửa `ArrayList` bên trong state, Compose không thấy | Gán list mới, hoặc `mutableStateListOf` | [39 / 54k](https://stackoverflow.com/questions/69718059) |
| `Calls to launch should happen inside a LaunchedEffect` | Chạy side effect thẳng trong composable | `LaunchedEffect`; trong `onClick` dùng `rememberCoroutineScope` | [84 / 47k](https://stackoverflow.com/questions/66474049) |
| `LaunchedEffect` chạy lại khi quay về màn, hoặc dùng lambda cũ | Chọn sai key; lambda bị giữ bản cũ | Chọn key có chủ đích, `rememberUpdatedState` | [docs](https://developer.android.com/develop/ui/compose/side-effects) |
| `Key was already used` trong `LazyColumn` | Trùng key | Key duy nhất, ổn định (id) | [20 / 14k](https://stackoverflow.com/questions/69843588) |
| Crash `infinite height constraints` | `LazyColumn` lồng trong vùng cuộn khác | Một `LazyColumn` duy nhất, chia section | [64 / 40k](https://stackoverflow.com/questions/72271130) |

## 3. Bất đồng bộ & vòng đời

Lúc được lúc không. Đây là nhóm khó nhất vì **lỗi phụ thuộc thời điểm**: chạy test thì qua, lên máy thật mới hỏng. Phải hiểu mô hình thì mới sửa tận gốc được.

| Triệu chứng | Vì sao | Sửa | Nguồn |
|---|---|---|---|
| `catch (e: Exception)` làm coroutine không dừng khi bị huỷ | Huỷ coroutine = ném `CancellationException`, bị catch nuốt | Ném lại `CancellationException`, hoặc chỉ bắt loại lỗi cụ thể | [docs](https://kotlinlang.org/docs/cancellation-and-timeouts.html) · [pitfalls 2.11](kotlin-pitfalls.md#211-coroutine-catch-e-exception-nuot-lenh-huy) |
| `GlobalScope.launch` rò rỉ, chạy tiếp khi màn hình đã đóng | Không gắn với vòng đời nào | `viewModelScope`, `lifecycleScope`, scope được inject | [129 / 68k](https://stackoverflow.com/questions/65008486) |
| `runBlocking` làm đơ UI hoặc deadlock | Nó chặn thread, chỉ dành cho `main()` và test cũ | `launch` / `coroutineScope`; test thì `runTest` | [123 / 53k](https://stackoverflow.com/questions/53535977) |
| `CoroutineExceptionHandler` không được gọi; `try/catch` quanh `launch` không bắt được gì | Handler chỉ có tác dụng ở coroutine gốc; `async` giữ lỗi tới khi `await` | Gắn handler vào scope gốc; bọc `await()`; `supervisorScope` | [docs](https://kotlinlang.org/docs/exception-handling.html) |
| Scope đã `cancel()` thì `launch` không chạy gì nữa | Job đã huỷ không dùng lại được | `cancelChildren()`, hoặc tạo scope mới | [22 / 19k](https://stackoverflow.com/questions/65529095) |
| Gán `StateFlow` cùng giá trị thì không phát | `StateFlow` bỏ qua giá trị `==` giá trị cũ | Sự kiện 1 lần dùng `Channel`/`SharedFlow`, hoặc đưa sự kiện vào state | [24 / 28k](https://stackoverflow.com/questions/72760708) |
| Code sau `collect` không bao giờ chạy; test treo | Flow nóng (`StateFlow`, `SharedFlow`) không bao giờ kết thúc | Mỗi `collect` một `launch`; test dùng `first()`, `take(n)`, Turbine | [70 / 43k](https://stackoverflow.com/questions/61224047) |
| `Flow exception transparency is violated` | `emit` từ coroutine khác | `channelFlow` / `callbackFlow` | [35 / 19k](https://stackoverflow.com/questions/67582210) |
| `synchronized` quanh đoạn có `suspend` không khoá được | Lock gắn với thread, coroutine có thể tiếp tục ở thread khác | `Mutex().withLock { }` | [docs](https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html) |
| `Can not perform this action after onSaveInstanceState` | Callback bất đồng bộ về sau khi Activity đã lưu state | Chỉ thao tác khi ≥ `STARTED`, collect bằng `repeatOnLifecycle` | [579 / 439k](https://stackoverflow.com/questions/7575921) |
| Flow vẫn collect khi app ở nền → crash hoặc tốn pin | `launch`/`launchWhenStarted` không huỷ khi ra nền | `repeatOnLifecycle(STARTED)`, `collectAsStateWithLifecycle()` | [26 / 22k](https://stackoverflow.com/questions/71113597) |
| Fragment nhận dữ liệu 2 lần, rò rỉ observer | Vòng đời Fragment ≠ vòng đời view của nó | Observe bằng `viewLifecycleOwner` | [38 / 22k](https://stackoverflow.com/questions/54766112) |

## 4. Code dở

Chạy đúng, nhưng khó đọc, khó test, và sửa chỗ này dễ hỏng chỗ kia. Compiler không báo gì, chỉ code review và lint bắt được.

| Dấu hiệu | Vì sao dở | Viết thế nào thì tốt | Nguồn |
|---|---|---|---|
| `!!` rải khắp nơi | Mỗi `!!` là một chỗ có thể crash | `?:`, `requireNotNull` có message | [345 / 193k](https://stackoverflow.com/questions/34342413) |
| `lateinit` cho mọi thứ | Lỗi khởi tạo chỉ lộ ra khi chạy | `val` qua constructor; `by lazy` khi tính tốn kém | [517 / 259k](https://stackoverflow.com/questions/36623177) |
| Khoá xoay màn hình để khỏi mất dữ liệu | Che lỗi chứ không sửa; vẫn mất khi đổi ngôn ngữ, chia màn hình, process death | State nằm trong ViewModel + saved state | [359 / 279k](https://stackoverflow.com/questions/2730855) |
| Truyền ViewModel xuống mọi composable con | Composable không preview, không test được | State hoisting: truyền state + lambda | [64 / 44k](https://stackoverflow.com/questions/68548488) |
| Sự kiện 1 lần (navigate, toast) bắn qua `SharedFlow`/`Channel` | Dễ mất hoặc lặp sự kiện khi xoay màn, ra nền | Đưa sự kiện vào UI state, UI xử lý xong thì báo đã dùng | [bài viết](https://proandroiddev.com/viewmodel-events-as-state-are-an-antipattern-35ff4fbc6fb6) |
| Luật nghiệp vụ viết trong UI (sắp xếp, kiểm tra rỗng, tính tiền) | Phải viết 2 lần cho Android và iOS, không unit test được | Đưa vào `shared/rules`, như [Pocket Notes `planSave`](../apps/02-pocket-notes.md) | repo này |
| Lộ `MutableStateFlow`/`MutableList` ra ngoài ViewModel | Ai cũng sửa được state | Giữ `private val _state`, lộ ra `StateFlow` | [pitfalls 2.9](kotlin-pitfalls.md#29-list-chi-oc-khong-co-nghia-la-khong-oi) |
| Lồng `let`/`apply`/`also` nhiều tầng | `it`/`this` đổi nghĩa ở mỗi tầng | `if` thường, đặt tên tham số lambda | [pitfalls 2.12](kotlin-pitfalls.md#212-5-scope-function-trong-giong-nhau) |
| `static` kiểu Java nhét hết vào `companion object` | Không cần thiết, khó đọc | Hàm top-level, `const val`, `object` | [1.1k / 630k](https://stackoverflow.com/questions/40352684) |

## 5. Hiệu năng kém

Chạy đúng nhưng chậm, giật hoặc tốn pin. **Đo trước khi sửa**: dùng Profiler, Layout Inspector (đếm recomposition) và Build Analyzer.

| Triệu chứng | Vì sao | Sửa | Nguồn |
|---|---|---|---|
| ANR `Input dispatching timed out` | Main thread bị chặn quá 5 giây | Đưa việc nặng ra khỏi main; bật StrictMode khi dev | [191 / 194k](https://stackoverflow.com/questions/704311) |
| `Cannot access database on the main thread` | Truy vấn Room trên main | DAO `suspend` hoặc trả `Flow` | [238 / 270k](https://stackoverflow.com/questions/44167111) |
| Cảnh báo `Inappropriate blocking method call`; `Thread.sleep` trong coroutine | Code chặn thread chạy trên dispatcher không dành cho nó | `withContext(Dispatchers.IO)`, `delay()` | [262 / 105k](https://stackoverflow.com/questions/58680028) · [pitfalls 2.13](kotlin-pitfalls.md#213-threadsleep-trong-coroutine-chan-ca-thread) |
| Composable recompose liên tục, không bao giờ được bỏ qua | Tham số không ổn định (`List`, class có `var`, class từ module khác) | `@Immutable`/`@Stable`, `ImmutableList`, xem compiler report | [75 / 33k](https://stackoverflow.com/questions/68575936) · [docs](https://developer.android.com/develop/ui/compose/performance/stability/diagnose) |
| Recompose vì truyền method reference của ViewModel | Lambda không ổn định | `remember` lambda; bật strong skipping | [SO](https://stackoverflow.com/questions/76505894) |
| Tính lại mỗi khung hình khi cuộn | Đọc state đổi liên tục để suy ra giá trị ít đổi | `derivedStateOf` | [75 / 27k](https://stackoverflow.com/questions/70144298) |
| `LazyColumn` không có `key`: animation giật, sai state dòng sau khi xoá | Item được nhận diện theo vị trí | `items(list, key = { it.id })` | [SO](https://stackoverflow.com/questions/72512303) |
| `by lazy` chậm ở chỗ gọi rất nhiều | Mặc định có khoá đồng bộ | `lazy(LazyThreadSafetyMode.NONE)` nếu chỉ dùng một thread | [517 / 259k](https://stackoverflow.com/questions/36623177) |
| Build chậm vì kapt | kapt sinh stub Java trước rồi mới xử lý | Chuyển sang KSP | [docs](https://kotlinlang.org/docs/ksp-kapt-migration.html) |
| Build iOS (Kotlin/Native) mất nhiều phút | Bước link LLVM nặng, build mọi target | Khi dev chỉ build target simulator đang dùng, bật configuration cache | [docs](https://kotlinlang.org/docs/native-improving-compilation-time.html) |
| Chuỗi `map`/`filter` trên list rất lớn tốn bộ nhớ | Mỗi bước tạo một list mới | `asSequence()` khi list lớn và dừng sớm được | [docs](https://kotlinlang.org/docs/sequences.html) |

## 6. Hạn chế của OS (Android, iOS)

Code đúng, nhưng OS không cho. Thường chỉ hỏng trên **một số phiên bản**, vì mỗi bản Android/iOS lại siết thêm. Không "sửa" được, chỉ thiết kế để sống chung với nó.

### Android

| Triệu chứng | Từ bản | Luật của OS | Cách sống chung | Nguồn |
|---|---|---|---|---|
| `Cleartext HTTP traffic to X not permitted` | 9 | Chặn `http://` mặc định | Dùng HTTPS; cần thì mở riêng domain trong `network_security_config` | [1.8k / 1.67M](https://stackoverflow.com/questions/45940861) |
| `Not allowed to start service Intent` | 8 | App ở nền không được chạy service thường | WorkManager hoặc foreground service | [480 / 336k](https://stackoverflow.com/questions/46445265) |
| `startForegroundService() did not then call startForeground()` | 8 | Phải gọi `startForeground` trong khoảng 5 giây | Gọi ngay trong `onStartCommand` | [400 / 373k](https://stackoverflow.com/questions/44425584) |
| `ForegroundServiceStartNotAllowedException` | 12 | Không được mở foreground service từ nền | Mở khi app đang hiện; hoặc WorkManager expedited | [26](https://stackoverflow.com/questions/70711950) |
| `MissingForegroundServiceTypeException` | 14 | Foreground service phải khai loại + quyền `FOREGROUND_SERVICE_<TYPE>` | Khai `foregroundServiceType`, gọi `ServiceCompat.startForeground(…, type)` như [Trip Log](../apps/04-trip-log.md) | [docs](https://developer.android.com/about/versions/14/changes/fgs-types-required) |
| `TransactionTooLargeException` | mọi bản | Bundle/Intent tối đa khoảng 1 MB | Truyền id, không truyền cả dữ liệu | [338 / 293k](https://stackoverflow.com/questions/11451393) |
| Báo thức trễ hoặc không kêu; `SecurityException` ở `setExact` | 6 / 12 / 14 | Doze gom việc lại; bản 14 mặc định từ chối báo thức chính xác | `canScheduleExactAlarms()`; không cần chính xác thì WorkManager | [docs](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms) · [48 / 85k](https://stackoverflow.com/questions/71031091) |
| Không có lựa chọn "Cho phép mọi lúc" khi xin vị trí | 10 / 11 | Vị trí nền phải xin riêng, sau vị trí khi dùng; người dùng tự bật trong Settings | Xin 2 bước, có màn giải thích | [docs](https://developer.android.com/develop/sensors-and-location/location/permissions/background) |
| BLE scan/connect ném `SecurityException` | 12 | `BLUETOOTH_SCAN`/`CONNECT` thành quyền runtime | Xin lúc chạy; thêm `neverForLocation` nếu không cần vị trí | [docs](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions) |
| Không ghi được file dù có `WRITE_EXTERNAL_STORAGE` | 10 / 11 | Scoped storage | MediaStore, SAF, hoặc thư mục riêng của app | [121 / 259k](https://stackoverflow.com/questions/62782648) |
| Thông báo không hiện | 13 | `POST_NOTIFICATIONS` thành quyền runtime | Xin lúc chạy | [22 / 59k](https://stackoverflow.com/questions/73940694) |
| `queryIntentActivities` trả list rỗng | 11 | Package visibility | Khai `<queries>` trong manifest | [SO](https://stackoverflow.com/questions/65629268) |

### iOS

| Triệu chứng | Luật của OS | Cách sống chung | Nguồn |
|---|---|---|---|
| Vị trí nền dừng sau vài phút | iOS treo app ở nền | Bật Background Modes → Location, `allowsBackgroundLocationUpdates = true` | [212 / 195k](https://stackoverflow.com/questions/6347503) |
| `requestAlwaysAuthorization` không hiện hộp thoại | Thiếu key trong Info.plist; phải xin "Khi dùng" trước | Thêm key `NSLocation…UsageDescription`, xin 2 bước | [38 / 41k](https://stackoverflow.com/questions/26005122) |
| Việc nền bị giết, `BGTask` không chạy đúng giờ | `beginBackgroundTask` chỉ cho khoảng 30 giây; hệ thống tự chọn lúc chạy `BGTask` | Luôn gọi `endBackgroundTask`; không hứa giờ chính xác | [docs](https://developer.apple.com/documentation/uikit/uiapplication/beginbackgroundtask(expirationhandler:)) |
| BLE ở nền quét chậm, không quét kiểu wildcard được | Giới hạn CoreBluetooth khi ở nền | Quét theo service UUID, bật `bluetooth-central`, state restoration | [80](https://stackoverflow.com/questions/9896562) |

## 7. Hạn chế của ngôn ngữ & interop

"Kotlin không cho viết thế này", hoặc Swift nhìn thấy API Kotlin rất xấu. Đây là thiết kế của ngôn ngữ hoặc giới hạn của cầu nối Objective-C, nên chỉ né được.

### Kotlin trên JVM

| Hạn chế | Hệ quả | Cách né | Nguồn |
|---|---|---|---|
| Không có `static` | Người từ Java lúng túng | Hàm top-level, `companion object`, `@JvmStatic`, `const val` | [1.1k / 630k](https://stackoverflow.com/questions/40352684) |
| Generic bị xoá kiểu khi chạy | Không viết được `x is List<String>` hay `T::class` | `inline fun <reified T>`, `filterIsInstance<T>()` | [458 / 169k](https://stackoverflow.com/questions/45949584) · [pitfalls 3](kotlin-pitfalls.md#3-han-che-cua-kotlin) |
| Class mặc định `final` | Mockito không mock được, Spring/JPA cần `open` | MockK / Mokkery; plugin `all-open`, `kotlin-spring` | [29 / 32k](https://stackoverflow.com/questions/57536196) |
| Không có checked exception | Compiler không nhắc xử lý `IOException` | `Result`/sealed class; `@Throws` khi Java hoặc Swift gọi | [35](https://stackoverflow.com/questions/58639126) |
| Không tự đổi kiểu số | `Long == Int` không compile | Đổi tường minh | [65 / 55k](https://stackoverflow.com/questions/43721362) |
| `List` chỉ đọc không phải bất biến | Ai giữ `MutableList` gốc vẫn sửa được | `toList()` khi đưa ra ngoài | [249](https://stackoverflow.com/questions/37913252) |

### Kotlin/Native và Swift (KMP)

| Hạn chế | Hệ quả | Cách né | Nguồn |
|---|---|---|---|
| Exception Kotlin không có `@Throws` | App iOS **crash**: `Uncaught Kotlin exception` | `@Throws(Exception::class)` trên hàm Swift gọi, hoặc trả `Result` | [docs](https://kotlinlang.org/docs/native-objc-interop.html#errors-and-exceptions) |
| Generic chỉ giữ được trên class, mất trên interface/hàm | Swift thấy `Any`, `T?` | Lớp bọc, SKIE | [docs](https://kotlinlang.org/docs/native-objc-interop.html#generics) |
| `sealed class`/`enum` thành class Objective-C | `switch` trong Swift không phủ đủ nhánh, phải có `default` | SKIE sinh enum Swift (dùng `onEnum(of:)`) | [SKIE](https://skie.touchlab.co/) |
| `suspend` thành completion handler; `Flow` khó collect, không huỷ được | Cầu nối Objective-C không hiểu coroutine | SKIE hoặc KMP-NativeCoroutines | [SO](https://stackoverflow.com/questions/64175099) · [Interopedia](https://github.com/kotlin-hands-on/kotlin-swift-interopedia) |
| Không có tham số mặc định trong Swift | Phải truyền đủ mọi tham số | Overload, SKIE | [SKIE](https://skie.touchlab.co/features/default-arguments) |
| Objective-C không có package | Tên bị đổi thành `Note_`, có thêm tiền tố | Đặt tên không trùng, `@ObjCName` | [docs](https://kotlinlang.org/docs/native-objc-interop.html) |
| `InvalidMutabilityException` (tài liệu cũ) | Memory model cũ bắt "freeze" object | Đã bỏ từ Kotlin 1.9.20. Gặp trong bài hướng dẫn cũ thì bỏ qua | [docs](https://kotlinlang.org/docs/native-migration-guide.html) |

## 8. Giới hạn logic / bản chất

Không phải lỗi của Kotlin: viết bằng ngôn ngữ nào cũng gặp. Không sửa được, chỉ **chọn cách biểu diễn dữ liệu đúng** ngay từ đầu.

| Giới hạn | Ví dụ | Cách làm đúng | Nguồn |
|---|---|---|---|
| Số thực nhị phân | `0.1 + 0.2 != 0.3`; làm tròn `Double` ra số lạ | Tiền: `Long` theo đơn vị nhỏ nhất hoặc `BigDecimal` + `RoundingMode`; so sánh `Double` có sai số | [129 / 224k](https://stackoverflow.com/questions/49011924) · [pitfalls 2.2](kotlin-pitfalls.md#22-chia-so-nguyen-va-cong-so-thuc) |
| Tràn số | `30 * 24 * 60 * 60 * 1000` ra số âm | Dùng `Long` từ số đầu tiên; `Math.multiplyExact` để báo lỗi | [docs](https://kotlinlang.org/docs/numbers.html) · [pitfalls 2.1](kotlin-pitfalls.md#21-int-tran-so-ma-khong-bao-loi) |
| Chia nguyên, chia không hết | `100 000 / 3` mất 1 đồng | Chia phần dư cho vài người đầu (như bản Nâng cao của Tip Split trong Playground) | [Tip Split](../apps/01-tip-split.md) |
| Múi giờ, giờ mùa hè, locale | Giờ lệch khi đổi máy, số in ra `1.234,5` thay vì `1,234.5` | Lưu `Instant` UTC; chỉ đổi sang giờ địa phương khi hiển thị; định dạng cho máy đọc thì cố định locale | [192 / 207k](https://stackoverflow.com/questions/7672597) |
| "Hôm qua" không phải "24 giờ trước" | 23:59 → 00:01 đã là hôm qua | Tính theo ngày lịch trong múi giờ người dùng, như `relativeTime` của Pocket Notes | [Pocket Notes](../apps/02-pocket-notes.md) |
| Unicode | Emoji có `length` = 2; `đ` không phải "d + dấu" nên bỏ dấu kiểu thường không ra `d` | Đếm theo code point/grapheme; tự map `đ → d` như `fold()` của Pocket Notes | [SO](https://stackoverflow.com/questions/72030560) |
| Sai số GPS | Vị trí nhảy, sai số vài trăm mét khi dùng Wi-Fi/sóng di động; `accuracy` là bán kính 68% | Lọc điểm theo `accuracy`, bỏ điểm nhảy xa bất thường | [106 / 89k](https://stackoverflow.com/questions/3052517) |
| Đồng hồ máy không tin được | Người dùng đổi giờ máy; giờ tường nhảy | Đo khoảng thời gian bằng đồng hồ đơn điệu (`TimeSource.Monotonic`), không trừ 2 mốc giờ tường | [docs](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-time-source/-monotonic/) |
| Mạng không bao giờ chắc chắn | Request treo, trả 2 lần, mất giữa chừng | Timeout, retry có giới hạn, offline-first như [Rate Watch](../apps/03-rate-watch.md) | [docs](https://developer.android.com/topic/architecture/data-layer/offline-first) |

## Gặp lỗi mới thì xếp vào đâu

1. Đi theo [cây quyết định](#loi-nay-thuoc-nhom-nao) ở đầu trang.
2. Tìm câu báo lỗi trong [900 lỗi Kotlin hay gặp](../issues/index.md). Chưa có thì thêm vào nhóm chủ đề ở đó.
3. Lỗi được hỏi nhiều (trên vài trăm vote) thì thêm một dòng vào bảng của nhóm tương ứng ở trang này, kèm link StackOverflow.

Kiểm tra lại số vote bằng API: `https://api.stackexchange.com/2.3/search/advanced?order=desc&sort=votes&title=<câu lỗi>&site=stackoverflow`.

## Nguồn tổng hợp

- StackOverflow, tag [kotlin](https://stackoverflow.com/questions/tagged/kotlin?tab=Votes) sắp theo vote (qua Stack Exchange API, 15/10/2026).
- [Android vitals](https://developer.android.com/topic/performance/vitals): ngưỡng xấu là tỷ lệ crash 1,09% và ANR 0,47%.
- [JetBrains: How Kotlin is going to fix your pains in 2023](https://blog.jetbrains.com/kotlin/2022/11/how-kotlin-is-going-to-fix-your-pains-in-2023/): khảo sát dev Kotlin, nỗi đau lớn nhất là hiệu năng IDE và build.
- [Kotlin–Swift Interopedia](https://github.com/kotlin-hands-on/kotlin-swift-interopedia): danh mục mọi chỗ hở khi Swift gọi Kotlin.
- [Objective-C/Swift interop](https://kotlinlang.org/docs/native-objc-interop.html), [Exception handling](https://kotlinlang.org/docs/exception-handling.html), [Shared mutable state](https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html).
