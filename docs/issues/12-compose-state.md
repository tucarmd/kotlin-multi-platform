# 12. Compose: state & recomposition (K551–K600)

Lỗi quanh câu hỏi "vì sao màn hình không đổi" và "vì sao nó đổi hoài": state không được Compose theo dõi, state mất khi xoay
hay khi cuộn, state được lưu ở chỗ không nên lưu, và recompose quá nhiều. Viết cho Jetpack Compose bản ổn định hiện tại
(Compose compiler đi kèm Kotlin 2.x, strong skipping bật sẵn).

### K551 · Đổi biến `var` thường mà màn hình không vẽ lại

`🟢 mới học` · `Compose`

**Gặp:** bấm nút, log in ra `count = 3`, nhưng `Text` vẫn hiện `0`.

**Vì sao:** Compose chỉ vẽ lại khi một **state** mà nó đã đọc thay đổi. `var count = 0` là biến Kotlin thường, Compose không
biết nó đổi.

**Sửa:** dùng `mutableStateOf` (hoặc state từ `ViewModel`), bọc trong `remember`.

```kotlin
var count = 0                                   // ❌ đổi mà không ai biết
var count by remember { mutableIntStateOf(0) }  // ✅
Button(onClick = { count++ }) { Text("$count") }
```

Xem thêm: K552

### K552 · Có `mutableStateOf` mà bấm xong vẫn về 0

`🟢 mới học` · `Compose`

**Gặp:** `val count = mutableStateOf(0)` trong composable; bấm thì nháy một cái rồi vẫn là 0.

**Vì sao:** mỗi lần recompose, hàm composable chạy lại từ đầu và tạo một `mutableStateOf(0)` **mới**. Giá trị vừa đổi nằm ở
object cũ đã bị bỏ.

**Sửa:** `remember { }` để giữ object qua các lần recompose. Android Studio cũng cảnh báo lint
`Creating a state object during composition without using remember`.

```kotlin
val count = mutableStateOf(0)                 // ❌ tạo lại mỗi lần
val count = remember { mutableStateOf(0) }    // ✅
```

### K553 · State trong nhánh `if` mất khi nhánh bị ẩn rồi hiện lại

`🟢 mới học` · `Compose`

**Gặp:** ô ghi chú nằm trong `if (expanded) { … }`; thu gọn rồi mở lại thì chữ đã gõ biến mất.

**Vì sao:** `remember` gắn với **vị trí** trong cây composition. Khi nhánh `if` không còn được gọi, mọi thứ nó `remember` bị
huỷ. Hiện lại là một lần `remember` mới.

**Sửa:** đưa state lên trên `if` (state hoisting), hoặc lên `ViewModel` nếu cần giữ lâu hơn.

```kotlin
if (expanded) { var note by remember { mutableStateOf("") } /* ❌ mất khi ẩn */ }

var note by rememberSaveable { mutableStateOf("") }        // ✅ nằm ngoài if
if (expanded) { TextField(note, onValueChange = { note = it }) }
```

### K554 · `remember` mất khi xoay màn hình

`🟢 mới học` · `Compose` · `Android`

**Gặp:** tab đang chọn, dialog đang mở, chữ đang gõ trở về mặc định khi xoay máy hay đổi dark mode.

**Vì sao:** xoay máy tạo lại `Activity`, cả composition bị dựng lại từ đầu. `remember` chỉ sống trong một composition.
`rememberSaveable` lưu giá trị vào saved state (`Bundle`) và đọc lại sau khi tạo lại.

**Sửa:** state nhỏ của UI (tab, chữ gõ, dialog mở) dùng `rememberSaveable`. Dữ liệu màn hình thì để ở `ViewModel`.

```kotlin
var tab by remember { mutableStateOf(Tab.Connect) }          // ❌ về Connect khi xoay
var tab by rememberSaveable { mutableStateOf(Tab.Connect) }  // ✅
```

**Gặp ở:** [`apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/App.kt) ·
Xem thêm: K501, K513

### K555 · `rememberSaveable` crash: "cannot be saved using the current SaveableStateRegistry"

`🟡 đã làm app` · `Compose` · `Android`

**Gặp:** `IllegalArgumentException: MutableState containing Note(…) cannot be saved using the current SaveableStateRegistry.
The default implementation only supports types which can be stored inside the Bundle. Please consider implementing a custom
Saver for this class and pass it as a stateSaver parameter to rememberSaveable().`

**Vì sao:** `rememberSaveable` chỉ tự lưu được kiểu đặt vào `Bundle` được: số, chuỗi, enum, `Parcelable`, `Serializable`… Một
`data class` thường thì không.

**Sửa:** lưu ít thôi: lưu `id` thay vì cả object. Cần lưu object nhỏ thì `@Parcelize` (Android) hoặc viết `Saver`
(`listSaver`, `mapSaver`) – cách này cũng chạy được với class trong `commonMain`.

```kotlin
var selected by rememberSaveable { mutableStateOf<Note?>(null) }   // ❌ Note không vào Bundle được
var selectedId by rememberSaveable { mutableStateOf<Long?>(null) } // ✅ lưu id, tìm lại từ ViewModel
```

Xem thêm: K519

### K556 · Lưu cả danh sách bằng `rememberSaveable` → `TransactionTooLargeException`

`🟡 đã làm app` · `Compose` · `Android`

**Gặp:** app chạy bình thường, nhưng bấm Home (app vào nền) thì crash `TransactionTooLargeException`, hoặc log báo
`data parcel size … bytes`.

**Vì sao:** mọi thứ `rememberSaveable` lưu đi vào saved state của `Activity`, có giới hạn khoảng 1 MB cho cả app. Danh sách
dài hay ảnh vượt quá.

**Sửa:** `rememberSaveable` chỉ cho state nhỏ của UI. Danh sách thì để `ViewModel` giữ và đọc lại từ DB/mạng.

Xem thêm: K515

### K557 · Undo xong, dòng vừa khôi phục lại tự xoá lần nữa

`🔴 khó, hiếm nhưng đau` · `Compose`

**Gặp:** vuốt để xoá ghi chú, bấm *Undo* trên snackbar: ghi chú hiện lại rồi biến mất ngay, như bị vuốt lần hai.

**Vì sao:** `rememberSwipeToDismissBoxState()` dùng `rememberSaveable`. `LazyColumn` giữ saved state **theo `key`** kể cả khi
dòng đã biến mất. Undo trả ghi chú về với **cùng id**, dòng đó khôi phục trạng thái "đã vuốt", và `onDismiss` chạy lại. Đây là
state "saveable sai chỗ": trạng thái tạm của một cử chỉ không nên sống lâu hơn dòng đó.

**Sửa:** tạo state vuốt bằng `remember` thường.

```kotlin
val swipe = rememberSwipeToDismissBoxState()   // ❌ saveable, sống lại sau Undo
val swipe = remember {                          // ✅ mất cùng với dòng
    SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, density, positionalThreshold = threshold)
}
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt) (hàm `SwipeToDelete`) ·
Xem thêm: K558, K591

### K558 · `LazyColumn` không có `key`: xoá một dòng, dòng dưới hiện nửa vuốt

`🟡 đã làm app` · `Compose`

**Gặp:** xoá một dòng thì dòng trượt lên thế chỗ hiện ở trạng thái đang vuốt dở, checkbox nhảy sang dòng khác, hoặc bị xoá
luôn.

**Vì sao:** không có `key`, `LazyColumn` gắn state của mỗi dòng theo **vị trí**. Dòng ở vị trí 3 bị xoá thì dòng mới ở vị trí 3
nhận state cũ.

**Sửa:** luôn truyền `key` ổn định và duy nhất (id trong DB), không dùng index.

```kotlin
items(items) { item -> SwipeToDelete { NoteRow(item) } }                     // ❌
items(items, key = { it.note.id }) { item -> SwipeToDelete { NoteRow(item) } } // ✅
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt) ·
[`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/TripsScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/TripsScreen.kt)

### K559 · "Key … was already used" trong `LazyColumn`

`🟢 mới học` · `Compose`

**Gặp:** `IllegalArgumentException: Key "1" was already used. If you are using LazyColumn/Row please make sure you provide a
unique key for each item.`

**Vì sao:** hai phần tử có cùng `key`: dữ liệu bị trùng (gộp hai nguồn), hoặc dùng field không duy nhất (`title`, `date`).

**Sửa:** dùng id thật sự duy nhất. Gộp nhiều loại phần tử trong một list thì ghép thêm tiền tố loại.

```kotlin
items(rows, key = { it.title })              // ❌ hai ghi chú cùng tiêu đề là crash
items(rows, key = { "${it.kind}-${it.id}" }) // ✅
```

### K560 · Vòng `for` trong `Column` không có `key()`

`🟡 đã làm app` · `Compose`

**Gặp:** danh sách ngắn vẽ bằng `Column { list.forEach { … } }`; thêm phần tử lên đầu thì ô đang gõ / trạng thái mở rộng nhảy
sang phần tử khác.

**Vì sao:** giống K558: không có `key`, Compose khớp state theo thứ tự gọi. Chèn lên đầu là mọi phần tử lệch một vị trí.

**Sửa:** bọc mỗi phần tử trong `key(id) { }`.

```kotlin
Column {
    people.forEach { person ->
        key(person.id) { PersonRow(person) }   // ✅ state đi theo id
    }
}
```

### K561 · `remember` không có key: đổi đầu vào mà giá trị vẫn cũ

`🟡 đã làm app` · `Compose`

**Gặp:** mở ghi chú A rồi chuyển sang ghi chú B (cùng composable), màn hình vẫn hiện nội dung A.

**Vì sao:** `remember { }` chỉ chạy khối tính **một lần** cho vị trí đó. Tham số `id` đổi thì composable recompose, nhưng
`remember` vẫn trả giá trị cũ.

**Sửa:** truyền những giá trị dùng trong khối tính làm key của `remember`.

```kotlin
val original = remember { viewModel.find(id) }      // ❌ kẹt ở id đầu tiên
val original = remember(id) { viewModel.find(id) }  // ✅ id đổi là tính lại
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteEditScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteEditScreen.kt)

### K562 · `rememberSaveable` lấy giá trị đầu từ dữ liệu chưa tải xong

`🟡 đã làm app` · `Compose`

**Gặp:** màn sửa hiện ô trống; vài trăm ms sau dữ liệu đã tải xong mà ô vẫn trống.

**Vì sao:** giá trị trong `{ mutableStateOf(x) }` chỉ được dùng **lần đầu**. Lúc đó `x` còn `null`/rỗng vì DB chưa trả về. Lần
sau `x` có rồi nhưng `remember` không tính lại.

**Sửa:** chỉ dựng phần form khi đã có dữ liệu (hiện loading trước), hoặc lấy dữ liệu đồng bộ từ state đã có sẵn trong
`ViewModel`. Đừng thêm `x` làm key `rememberSaveable` khi người dùng đang gõ: đổi key là xoá chữ họ gõ.

```kotlin
when (val note = state.note) {
    null -> CircularProgressIndicator()
    else -> NoteForm(note)   // ✅ rememberSaveable bên trong có giá trị đầu đúng
}
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteEditScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteEditScreen.kt) (đọc `find(id)` một lần từ list đã có sẵn)

### K563 · Ghi state ngay trong thân composable → recompose vô hạn

`🟢 mới học` · `Compose`

**Gặp:** màn hình giật, CPU 100%, log in liên tục; hoặc app treo.

**Vì sao:** composable đọc state rồi ghi state đó trong lúc đang compose. Ghi state → lên lịch recompose → chạy lại → ghi tiếp…
Một vòng không có điểm dừng.

**Sửa:** thân composable chỉ **đọc** state và mô tả UI. Ghi state trong event (`onClick`) hoặc trong `LaunchedEffect`. Giá
trị tính từ state khác thì tính thẳng, không cần lưu.

```kotlin
var total by remember { mutableIntStateOf(0) }
total = items.sumOf { it.price }        // ❌ ghi trong composition
val total = items.sumOf { it.price }    // ✅ chỉ tính
```

### K564 · Gọi API, ghi log, gửi analytics trong thân composable

`🟢 mới học` · `Compose`

**Gặp:** request bị gửi 5–10 lần khi mở màn hình; analytics đếm `screen_view` gấp nhiều lần.

**Vì sao:** thân composable có thể chạy lại bất cứ lúc nào (mỗi lần state đổi, animation chạy) và không đảm bảo số lần chạy.

**Sửa:** việc "làm một lần khi vào màn" đặt trong `LaunchedEffect(key)`, hoặc tốt hơn trong `init` của `ViewModel`.

```kotlin
@Composable fun RatesScreen(vm: RatesViewModel) {
    vm.refresh()                                   // ❌ gọi mỗi lần recompose
    LaunchedEffect(Unit) { vm.refresh() }          // ✅ một lần khi vào composition
}
```

### K565 · `list.add()` mà `LazyColumn` không hiện dòng mới

`🟢 mới học` · `Compose`

**Gặp:** `val items = remember { mutableStateOf(mutableListOf<String>()) }`, gọi `items.value.add("x")`, danh sách trên màn hình
không đổi.

**Vì sao:** `mutableStateOf` chỉ theo dõi việc **gán giá trị mới**. Sửa bên trong list cũ không phải là gán, Compose không biết.

**Sửa:** gán list mới (`items = items + "x"`), hoặc dùng `mutableStateListOf()` (list mà Compose theo dõi được từng thay đổi).

```kotlin
var items by remember { mutableStateOf(listOf<String>()) }
items = items + "x"                           // ✅ list mới
val items2 = remember { mutableStateListOf<String>() }
items2.add("x")                               // ✅ list có theo dõi
```

### K566 · Đổi field `var` của `data class` mà UI không cập nhật

`🟢 mới học` · `Compose` · `Kotlin`

**Gặp:** `state.value.note.title = "Mới"` chạy không lỗi, nhưng màn hình không đổi tới khi có việc khác gây recompose.

**Vì sao:** Compose không theo dõi field `var` thường bên trong object. State vẫn trỏ tới cùng object, không có lần gán nào.

**Sửa:** model cho UI dùng `val` hết, đổi bằng `copy()` rồi gán object mới.

```kotlin
state.value.note.title = "Mới"                                      // ❌
state.value = state.value.copy(note = state.value.note.copy(title = "Mới")) // ✅
```

### K567 · `StateFlow` phát cùng object đã sửa → màn hình đứng yên

`🟡 đã làm app` · `Compose` · `Kotlin`

**Gặp:** `ViewModel` sửa list bên trong rồi `_state.value = current`; `collectAsStateWithLifecycle()` không nhận gì mới.

**Vì sao:** `StateFlow` bỏ qua giá trị `==` giá trị hiện tại. Cùng một object đã sửa thì luôn bằng chính nó, nên không phát.
Compose cũng so sánh như vậy trước khi recompose.

**Sửa:** state bất biến; mỗi lần đổi tạo object mới bằng `copy()` / list mới, cập nhật bằng `_state.update { }`.

```kotlin
_state.value.items.add(item); _state.value = _state.value         // ❌ không phát
_state.update { it.copy(items = it.items + item) }                // ✅
```

### K568 · Đặt lại đúng giá trị cũ để "kích hoạt lại" mà không có gì xảy ra

`🟡 đã làm app` · `Compose`

**Gặp:** lỗi "Mất mạng" hiện snackbar lần đầu; lần mất mạng thứ hai `error = "Mất mạng"` mà snackbar không hiện lại.

**Vì sao:** `mutableStateOf` mặc định dùng `structuralEqualityPolicy()`: gán giá trị `==` giá trị cũ thì không coi là thay đổi.
`LaunchedEffect(error)` cũng không chạy lại vì key không đổi.

**Sửa:** cho mỗi sự kiện một định danh riêng (id tăng dần, thời điểm), và xoá sự kiện sau khi đã hiện (K569).

```kotlin
data class UiMessage(val id: Long, val text: String)   // ✅ hai lần lỗi là hai object khác nhau
```

### K569 · Sự kiện một lần lưu trong state → hiện lại sau khi xoay máy

`🟡 đã làm app` · `Compose` · `Android`

**Gặp:** snackbar "Đã xoá" hoặc lệnh điều hướng chạy lại mỗi lần xoay máy hay quay lại màn hình.

**Vì sao:** state sống lâu (trong `ViewModel`) nhưng không ai đánh dấu "đã xử lý". Composition mới đọc lại state đó và làm lại.

**Sửa:** sau khi UI xử lý xong, báo `ViewModel` xoá sự kiện; hoặc để core tự xoá (PocketNotes: core giữ `recentlyDeleted`
5 giây rồi xoá, snackbar biến mất theo).

```kotlin
LaunchedEffect(deleted) {
    if (deleted == null) return@LaunchedEffect
    val r = snackbar.showSnackbar(deletedText, actionLabel = undoText, duration = SnackbarDuration.Indefinite)
    if (r == SnackbarResult.ActionPerformed) onUndo() else onDismissUndo()   // ✅ luôn báo đã xử lý
}
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt)

### K570 · `collectAsState()` vẫn chạy khi app ở nền

`🟡 đã làm app` · `Compose` · `Android`

**Gặp:** app đã vào nền mà GPS/BLE/DB vẫn được đọc, pin hao; log của flow vẫn in.

**Vì sao:** `collectAsState()` thu thập suốt khi composable còn trong composition, kể cả khi `Activity` đã `onStop`.
Với `stateIn(WhileSubscribed(…))`, việc vẫn thu thập giữ luôn nguồn dữ liệu trên.

**Sửa:** trên Android dùng `collectAsStateWithLifecycle()` (thư viện `lifecycle-runtime-compose`): ngừng thu thập khi dưới
`STARTED`, thu thập lại khi quay lại.

```kotlin
val state by viewModel.state.collectAsState()               // ❌ chạy cả khi ở nền
val state by viewModel.state.collectAsStateWithLifecycle()  // ✅
```

**Gặp ở:** [`apps/TipSplit/androidApp/src/main/kotlin/dev/tipsplit/android/ui/SplitScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/androidApp/src/main/kotlin/dev/tipsplit/android/ui/SplitScreen.kt) ·
cùng mẫu ở cả 6 app

### K571 · Gọi hàm trả `Flow` ngay trong composable → loading mãi

`🟡 đã làm app` · `Compose`

**Gặp:** `viewModel.observeNotes().collectAsStateWithLifecycle(emptyList())` nháy rỗng liên tục, query chạy lại không ngừng.

**Vì sao:** mỗi lần recompose lại gọi `observeNotes()`, nhận một `Flow` **mới**. Compose thấy flow khác thì huỷ cái cũ, thu thập
cái mới từ giá trị đầu `emptyList()` → gây recompose → lại gọi hàm…

**Sửa:** `ViewModel` expose một property `StateFlow` tạo một lần. Nếu buộc phải tạo flow trong composable thì `remember(key)`.

```kotlin
val notes by vm.observeNotes().collectAsStateWithLifecycle(emptyList())      // ❌
val notes by vm.notes.collectAsStateWithLifecycle()                         // ✅ val notes = ….stateIn(…)
```

Xem thêm: K572

### K572 · `stateIn` trong getter hay trong hàm → mỗi lần đọc một `StateFlow` mới

`🟡 đã làm app` · `Compose` · `Kotlin`

**Gặp:** giống K571, hoặc mỗi màn hình thấy dữ liệu khác nhau, nhiều query chạy song song.

**Vì sao:** `val state get() = repo.flow().stateIn(viewModelScope, …)` tạo một coroutine thu thập mới **mỗi lần đọc** property.

**Sửa:** khởi tạo một lần với `=`, không dùng `get()`.

```kotlin
val notes: StateFlow<List<Note>> get() = repo.observe().stateIn(viewModelScope, WhileSubscribed(5_000), emptyList()) // ❌
val notes: StateFlow<List<Note>> = repo.observe().stateIn(viewModelScope, WhileSubscribed(5_000), emptyList())       // ✅
```

### K573 · Tạo `ViewModel` bằng constructor trong composable

`🟢 mới học` · `Compose` · `Android`

**Gặp:** xoay máy là mất hết dữ liệu; mỗi lần recompose lại tải dữ liệu; `viewModelScope` không bao giờ bị huỷ.

**Vì sao:** `NotesViewModel(repo)` chỉ là một object thường: không được lưu trong `ViewModelStore`, không sống qua xoay máy,
không có `onCleared()`.

**Sửa:** lấy qua `viewModel()` / `koinViewModel()` / `hiltViewModel()`, chúng lấy đúng instance gắn với `Activity` hoặc
back stack entry.

```kotlin
val vm = remember { NotesViewModel(repo) }     // ❌ không phải ViewModel thật
val vm: NotesViewModel = koinViewModel()       // ✅
```

Xem thêm: K516

### K574 · Mỗi màn hình gọi `koinViewModel()` riêng → hai màn không thấy nhau

`🟡 đã làm app` · `Compose` · `Android`

**Gặp:** xoá ghi chú trong màn sửa, quay về danh sách thì không thấy snackbar "Undo"; hai màn thấy hai state khác nhau.

**Vì sao:** gọi `koinViewModel()` bên trong mỗi `composable(route)` thì ViewModel gắn với **back stack entry** của route đó.
Hai route là hai instance, hai core.

**Sửa:** hai màn cần chung state thì lấy ViewModel một lần ở trên `NavHost` (hoặc từ entry của graph cha) rồi truyền xuống.

```kotlin
val viewModel: NotesViewModel = koinViewModel()     // ✅ một lần, trên NavHost
NavHost(nav, startDestination = Routes.LIST) {
    composable(Routes.LIST) { NoteListRoute(viewModel, …) }
    composable(Routes.EDIT) { NoteEditRoute(viewModel, …) }
}
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/App.kt)

### K575 · Truyền `ViewModel` xuống mọi composable → Preview không chạy

`🟢 mới học` · `Compose`

**Gặp:** `@Preview` báo lỗi hoặc trắng vì không tạo được ViewModel/Koin; component con khó dùng lại, khó test.

**Vì sao:** composable con phụ thuộc cả ViewModel, trong khi nó chỉ cần vài giá trị và vài callback.

**Sửa:** tách *Route* (lấy ViewModel, thu thập state) và *Screen* (nhận giá trị thường + lambda). State đi xuống, sự kiện đi lên.

```kotlin
@Composable fun NoteListRoute(viewModel: NotesViewModel, …) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NoteListScreen(state = state, onSearch = viewModel::search, onDelete = viewModel::delete, …)
}
@Composable fun NoteListScreen(state: NotesState, onSearch: (String) -> Unit, …) { … } // ✅ preview được
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt)

### K576 · `TextField` gõ không lên chữ

`🟢 mới học` · `Compose`

**Gặp:** bấm vào ô, bàn phím hiện, gõ mà ô vẫn trống (hoặc chỉ hiện ký tự cuối).

**Vì sao:** `TextField` không tự giữ chữ. Nó hiện đúng `value` bạn đưa; `onValueChange` chỉ báo chữ mới. Không cập nhật `value`
thì ô luôn hiện giá trị cũ.

**Sửa:** `onValueChange` phải ghi vào state mà `value` đang đọc.

```kotlin
TextField(value = "", onValueChange = { })                      // ❌
var text by rememberSaveable { mutableStateOf("") }
TextField(value = text, onValueChange = { text = it })          // ✅
```

### K577 · `TextField` nối với `StateFlow` → con trỏ nhảy, mất chữ khi gõ nhanh

`🔴 khó, hiếm nhưng đau` · `Compose`

**Gặp:** gõ nhanh thì mất ký tự, con trỏ nhảy về cuối, bộ gõ tiếng Việt (Telex) bị lặp dấu.

**Vì sao:** `onValueChange` gửi chữ vào `ViewModel`, ViewModel cập nhật state **bất đồng bộ** (qua coroutine, `debounce`, DB),
rồi mới trả về `value`. Giữa chừng ô hiện giá trị cũ, bộ gõ và ô chữ lệch nhau.

**Sửa:** giữ chữ đang gõ trong state Compose cập nhật **đồng bộ** (`mutableStateOf` trong composable hoặc trong ViewModel, ghi
ngay trong `onValueChange`); phần chậm (tìm kiếm, lưu) chạy sau từ state đó. Hoặc dùng API dựa trên `TextFieldState`
(`rememberTextFieldState()` với `BasicTextField`; Material3 bản mới cũng có overload nhận `state`).

```kotlin
onValueChange = { viewModelScope.launch { repo.save(it) } }           // ❌ value về sau
var query by rememberSaveable { mutableStateOf("") }
TextField(query, onValueChange = { query = it; vm.search(it) })       // ✅ ô chữ cập nhật ngay
```

### K578 · Dùng `derivedStateOf` cho mọi phép tính

`🟡 đã làm app` · `Compose`

**Gặp:** code đầy `remember { derivedStateOf { a + b } }` mà không nhanh hơn; hoặc kết quả không cập nhật khi `a` là tham số.

**Vì sao:** `derivedStateOf` chỉ có ích khi state đầu vào đổi **nhiều hơn** kết quả (ví dụ vị trí cuộn đổi liên tục, kết quả
"có hiện nút lên đầu không" thì hiếm khi đổi). Với phép tính thường nó chỉ thêm chi phí.

**Sửa:** tính thẳng cho phép tính rẻ; `remember(key) { }` cho phép tính đắt; `derivedStateOf` cho trường hợp lọc bớt thay đổi.

```kotlin
val showTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } } // ✅ đúng chỗ
val fullName = "$first $last"                                                      // ✅ tính thẳng
```

### K579 · `derivedStateOf` dùng tham số thường → kẹt ở giá trị cũ

`🔴 khó, hiếm nhưng đau` · `Compose`

**Gặp:** `remember { derivedStateOf { items.filter { it.price > limit } } }`, đổi `limit` (tham số composable) mà kết quả không đổi.

**Vì sao:** `derivedStateOf` chỉ tính lại khi **state** mà nó đọc đổi. `limit` là tham số thường, không phải state, và
`remember` không có key nên khối giữ `limit` của lần đầu.

**Sửa:** thêm các tham số thường làm key của `remember`.

```kotlin
val visible by remember(limit) { derivedStateOf { items.filter { it.price > limit } } } // ✅
```

### K580 · Đọc vị trí cuộn ở composable cha → cả màn hình recompose mỗi frame

`🟡 đã làm app` · `Compose`

**Gặp:** cuộn giật; Layout Inspector cho thấy số recompose của cả màn hình tăng liên tục khi cuộn.

**Vì sao:** state được đọc ở đâu thì chỗ đó recompose. Đọc `listState.firstVisibleItemScrollOffset` ở composable cha để tính
`offset` cho header là bắt cả cha recompose mỗi frame.

**Sửa:** hoãn việc đọc xuống tầng thấp nhất: dùng bản modifier nhận lambda (`Modifier.offset { }`, `graphicsLayer { }`,
`drawBehind { }`), state chỉ được đọc ở bước layout/draw.

```kotlin
Modifier.offset(y = (-scroll.value).dp)                      // ❌ đọc trong composition
Modifier.offset { IntOffset(0, -scroll.value) }               // ✅ đọc trong layout
```

### K581 · Strong skipping: tạo list mới mỗi lần → composable con không được bỏ qua

`🟡 đã làm app` · `Compose`

**Gặp:** `NoteList(items = state.items.filter { … })` recompose mỗi lần composable cha recompose, dù dữ liệu không đổi.

**Vì sao:** từ Kotlin 2.0.20, Compose compiler bật strong skipping sẵn: composable có thể bỏ qua khi tham số không đổi. Tham số
kiểu "không ổn định" (như `List`) được so bằng **cùng instance** (`===`). `filter` tạo list mới mỗi lần nên luôn khác.

**Sửa:** tính list trong `ViewModel`/core (state đã lọc sẵn), hoặc `remember(items, query) { items.filter { … } }`.

```kotlin
NoteList(state.items.filter { it.pinned })                                    // ❌ instance mới mỗi lần
val pinned = remember(state.items) { state.items.filter { it.pinned } }       // ✅
NoteList(pinned)
```

Xem thêm: K582

### K582 · Model từ module `shared` bị coi là "không ổn định"

`🔴 khó, hiếm nhưng đau` · `Compose` · `KMP`

**Gặp:** compiler report (hoặc Layout Inspector) cho thấy composable nhận `NotesState` không bao giờ được bỏ qua bằng `equals`,
dù đó là `data class` toàn `val`.

**Vì sao:** Compose compiler chỉ suy ra độ ổn định cho class được compile **cùng** plugin Compose. Model trong `commonMain` của
module `shared` (không có plugin Compose) bị coi là không ổn định, chỉ so bằng `===`.

**Sửa:** khai báo các class đó là ổn định trong file cấu hình, thay vì thêm phụ thuộc Compose vào `shared`.

```kotlin
// androidApp/build.gradle.kts
composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
}
// stability_config.conf
// dev.pocketnotes.model.*
```

Xem thêm: K362

### K583 · Bọc mọi lambda trong `remember` "cho đỡ recompose"

`🟡 đã làm app` · `Compose`

**Gặp:** code đầy `val onClick = remember(id) { { vm.open(id) } }` theo một bài viết cũ.

**Vì sao:** lời khuyên này có từ trước strong skipping. Nay compiler tự `remember` lambda trong composable theo các giá trị nó
bắt (capture), nên tự bọc thêm chỉ làm code khó đọc.

**Sửa:** viết lambda bình thường, `viewModel::search` cũng được. Chỉ tối ưu khi đã đo (Layout Inspector, compiler report) và
thấy vấn đề thật.

### K584 · Gắn `@Stable` / `@Immutable` cho class thật ra có thể đổi

`🔴 khó, hiếm nhưng đau` · `Compose`

**Gặp:** sau khi thêm `@Immutable` "cho nhanh", có lúc UI không cập nhật dù dữ liệu đã đổi.

**Vì sao:** annotation là lời hứa với compiler: "đổi gì tôi cũng báo" (`@Stable`) hoặc "không bao giờ đổi" (`@Immutable`).
Class có `var` thường hay `MutableList` bên trong phá lời hứa, Compose bỏ qua recompose dựa trên thông tin sai.

**Sửa:** chỉ gắn khi đúng là bất biến (toàn `val`, kiểu bất biến). Không chắc thì đừng gắn; dùng file cấu hình (K582) cho model
bất biến từ module khác.

### K585 · `println` trong composable in nhiều lần, thứ tự lạ

`🟢 mới học` · `Compose`

**Gặp:** log trong composable in 3–4 lần cho một lần bấm; composable con in trước/sau không như dự đoán.

**Vì sao:** composable có thể chạy nhiều lần, có thể bị bỏ qua, và Compose không hứa thứ tự giữa các composable anh em. Đó
là lý do không đặt side effect trong thân (K564).

**Sửa:** đừng suy luận từ số lần log. Đếm recompose bằng Layout Inspector (bật *Show Recomposition Counts*); xem lý do không bỏ
qua được bằng Compose compiler report.

```kotlin
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}
```

### K586 · `LaunchedEffect(Unit)` gọi callback cũ

`🔴 khó, hiếm nhưng đau` · `Compose`

**Gặp:** effect chạy lâu (đếm giờ, chờ snackbar) xong rồi gọi `onTimeout()`, nhưng đó là callback của lần compose đầu, dùng
dữ liệu đã cũ.

**Vì sao:** khối `LaunchedEffect(Unit)` bắt giá trị của tham số **lúc nó bắt đầu** và không chạy lại khi tham số đổi.

**Sửa:** `rememberUpdatedState` để effect luôn đọc bản mới nhất mà không bị khởi động lại.

```kotlin
val currentOnTimeout by rememberUpdatedState(onTimeout)
LaunchedEffect(Unit) {
    delay(5_000)
    currentOnTimeout()   // ✅ luôn là lambda mới nhất
}
```

### K587 · `LazyListState()` / `SnackbarHostState()` không có `remember`

`🟢 mới học` · `Compose`

**Gặp:** danh sách nhảy về đầu sau mỗi thay đổi; snackbar vừa hiện đã biến mất.

**Vì sao:** object giữ trạng thái (vị trí cuộn, hàng đợi snackbar) bị tạo mới mỗi lần recompose, giống K552.

**Sửa:** dùng hàm `remember…` có sẵn hoặc bọc trong `remember`.

```kotlin
val listState = LazyListState()                  // ❌
val listState = rememberLazyListState()          // ✅ (saveable, sống qua xoay)
val snackbar = remember { SnackbarHostState() }  // ✅
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt) (`remember { SnackbarHostState() }`)

### K588 · Lint "AutoboxingStateCreation" với `mutableStateOf(0)`

`🟢 mới học` · `Compose`

**Gặp:** Android Studio gạch vàng `mutableStateOf(0)`, gợi ý dùng `mutableIntStateOf`.

**Vì sao:** `mutableStateOf<Int>` giữ số dưới dạng object (`Integer`), mỗi lần ghi là một lần đóng hộp. Có bản riêng cho số
primitive để tránh việc đó.

**Sửa:** `mutableIntStateOf`, `mutableLongStateOf`, `mutableFloatStateOf`, `mutableDoubleStateOf`. Với `by` vẫn đọc ghi như `Int`.

```kotlin
var count by remember { mutableStateOf(0) }      // ⚠️ boxing
var count by remember { mutableIntStateOf(0) }   // ✅
```

### K589 · `by remember { mutableStateOf(…) }` báo thiếu `getValue`

`🟢 mới học` · `Compose`

**Gặp:** `Type 'MutableState<String>' has no method 'getValue(Nothing?, KProperty<*>)' and thus it cannot serve as a delegate`
(với `var` còn thêm `setValue`).

**Vì sao:** `by` cần hàm `getValue`/`setValue` dạng extension, nằm trong `androidx.compose.runtime`. File chưa import chúng.

**Sửa:** thêm import (Alt+Enter thường gợi ý đúng).

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
```

### K590 · State lưu trong ViewModel mà không sống qua process death

`🟡 đã làm app` · `Compose` · `Android`

**Gặp:** chữ đang gõ (`rememberSaveable`) còn sau khi hệ thống giết app ở nền, nhưng bộ lọc/tab lưu trong `ViewModel` thì về
mặc định; hai phần lệch nhau.

**Vì sao:** `ViewModel` sống qua xoay máy nhưng **không** sống qua process death. Chỉ saved state (`rememberSaveable`,
`SavedStateHandle`) được hệ thống giữ lại.

**Sửa:** state nhỏ của ViewModel cần giữ (id đang chọn, chuỗi tìm kiếm) để trong `SavedStateHandle`. Dữ liệu lớn thì đọc lại từ DB.
Thử bằng cách giết process khi app ở nền (K514).

```kotlin
class NotesViewModel(private val handle: SavedStateHandle) : ViewModel() {
    val query: StateFlow<String> = handle.getStateFlow("query", "")
    fun search(text: String) { handle["query"] = text }
}
```

Xem thêm: K513, K514

### K591 · Cuộn dòng ra khỏi màn hình là mất state của dòng

`🟡 đã làm app` · `Compose`

**Gặp:** mở rộng một dòng trong `LazyColumn`, cuộn xuống xa rồi quay lại, dòng đã thu gọn.

**Vì sao:** dòng ra khỏi vùng nhìn bị bỏ khỏi composition, `remember` của nó mất. `rememberSaveable` thì được `LazyColumn` lưu
theo `key` và trả lại khi dòng quay lại.

**Sửa:** state cần giữ khi cuộn: `rememberSaveable` trong dòng (có `key`), hoặc đưa lên ViewModel (`expandedIds: Set<Long>`).
State **không** nên sống lại (trạng thái vuốt, K557) thì cố ý dùng `remember`.

Xem thêm: K557, K558

### K592 · Sang màn khác rồi Back là mất state của màn trước

`🟡 đã làm app` · `Compose`

**Gặp:** gõ ô tìm kiếm ở màn danh sách, mở chi tiết, bấm Back: ô tìm kiếm trống, danh sách cuộn về đầu.

**Vì sao:** khi điều hướng đi, composable của màn cũ rời composition, `remember` mất. Navigation Compose chỉ lưu saved state
(`rememberSaveable`, `rememberLazyListState`) của từng back stack entry.

**Sửa:** dùng `rememberSaveable` cho state của màn hình, hoặc để trong ViewModel của entry đó (sống tới khi entry bị pop).

### K593 · `remember(key)` với key là object mới mỗi lần → không bao giờ dùng lại

`🟡 đã làm app` · `Compose`

**Gặp:** `remember(listOf(a, b)) { tínhĐắt() }` hay `remember(Filter(a, b))` vẫn chạy lại mỗi lần recompose.

**Vì sao:** key được so bằng `equals`, nhưng nếu key là class không có `equals` (class thường, lambda) thì mỗi lần là một key mới.
Lambda tạo mới mỗi lần gọi cũng vậy.

**Sửa:** truyền thẳng các giá trị đơn làm key: `remember(a, b) { … }`. Key là class thì dùng `data class`.

### K594 · Sắp xếp, lọc list lớn ngay trong thân composable

`🟡 đã làm app` · `Compose`

**Gặp:** gõ một ký tự vào ô tìm kiếm thì máy khựng; mỗi recompose (kể cả do animation) lại sort 5.000 phần tử.

**Vì sao:** thân composable chạy lại mỗi lần recompose, mọi phép tính trong đó cũng chạy lại.

**Sửa:** để core/ViewModel tính sẵn (PocketNotes lọc và sắp trong `NotesCore`). Chỉ còn cách tính ở UI
thì `remember(items) { … }`.

```kotlin
val sorted = items.sortedByDescending { it.updatedAt }                      // ❌ mỗi lần recompose
val sorted = remember(items) { items.sortedByDescending { it.updatedAt } }  // ✅
```

### K595 · Expose `MutableStateFlow` / `SnapshotStateList` cho UI tự sửa

`🟡 đã làm app` · `Compose`

**Gặp:** composable nào cũng `vm.state.value = …` hoặc `vm.items.add(…)`; lỗi state sai không biết ai ghi.

**Vì sao:** state có thể ghi được từ khắp nơi thì không còn một nguồn sự thật. Kiểm tra, log, test đều phải đi theo mọi chỗ ghi.

**Sửa:** ViewModel giữ bản ghi được ở `private`, expose bản chỉ đọc; UI đổi state bằng cách gọi hàm.

```kotlin
val state = MutableStateFlow(NotesState())                    // ❌
private val _state = MutableStateFlow(NotesState())
val state: StateFlow<NotesState> = _state.asStateFlow()       // ✅
fun search(text: String) = _state.update { it.copy(query = text) }
```

### K596 · Dialog đang mở tự đóng khi xoay máy

`🟢 mới học` · `Compose` · `Android`

**Gặp:** người dùng mở dialog chọn đồng tiền, xoay máy, dialog biến mất.

**Vì sao:** `var open by remember { mutableStateOf(false) }` mất khi `Activity` được tạo lại (K554).

**Sửa:** `rememberSaveable` cho cờ mở dialog.

```kotlin
var open by rememberSaveable { mutableStateOf(false) }   // ✅ dialog còn mở sau khi xoay
```

**Gặp ở:** [`apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/Components.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/Components.kt)

### K597 · Bật quyền / Bluetooth trong Settings, quay lại app vẫn hiện "chưa có quyền"

`🟡 đã làm app` · `Compose` · `Android`

**Gặp:** `var granted by remember { mutableStateOf(hasAll(context)) }`; người dùng bật quyền trong Settings rồi quay về, banner
"Cần quyền" vẫn nằm đó.

**Vì sao:** khối trong `remember` chỉ chạy một lần. Quay lại từ Settings không tạo lại composition, nên không ai đọc lại quyền.

**Sửa:** đọc lại mỗi khi màn hình quay lại foreground.

```kotlin
LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    granted = hasAll(context)      // ✅ cập nhật sau khi về từ Settings
    radioOn = isRadioOn(context)
}
```

**Gặp ở:** [`apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/BlePermission.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/BlePermission.kt) ·
Xem thêm: K533

### K598 · "5 phút trước" đứng yên mãi

`🟡 đã làm app` · `Compose`

**Gặp:** danh sách hiện "Sửa 1 phút trước"; 2 tiếng sau mở lại app vẫn là "1 phút trước".

**Vì sao:** nhãn được tính từ `now` lúc state được tạo. Thời gian trôi không phải là thay đổi state, nên không có gì recompose.

**Sửa:** coi "bây giờ" là một input của state, cập nhật khi màn hình hiện lại (hoặc theo nhịp khi cần).

```kotlin
LifecycleResumeEffect(Unit) {
    viewModel.refreshClock()   // ✅ core tính lại nhãn với now mới
    onPauseOrDispose { }
}
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt) ·
[`apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/App.kt)

### K599 · Lưu ghi chú trong `onDispose` → tạo ghi chú mới mỗi lần xoay

`🟡 đã làm app` · `Compose` · `Android`

**Gặp:** mở màn tạo ghi chú, xoay máy 3 lần rồi Back: danh sách có 4 ghi chú giống nhau.

**Vì sao:** `DisposableEffect { onDispose { save() } }` chạy mỗi khi composable rời composition, và xoay máy cũng làm vậy. Ghi
chú mới (`id = 0`) được chèn mỗi lần. Bấm Back hai lần nhanh cũng lưu hai lần.

**Sửa:** lưu khi người dùng **rời đi** thật (`BackHandler`, nút Back), và chặn lần thứ hai bằng một cờ.

```kotlin
var left by remember { mutableStateOf(false) }
val leave = { if (!left) { left = true; viewModel.save(id, title, body, pinned); onDone() } }
BackHandler(onBack = leave)    // ✅ không chạy khi xoay máy
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteEditScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteEditScreen.kt)

### K600 · State để trong class thường: lúc vẽ lại, lúc không

`🟡 đã làm app` · `Compose`

**Gặp:** class `TrackingPermissions` có `var issue: PermissionIssue?`; đổi giá trị thì banner không hiện, tới khi có gì khác làm
màn hình recompose.

**Vì sao:** Compose theo dõi **snapshot state** ở bất cứ đâu nó được đọc trong lúc compose, kể cả trong class thường. Nhưng
`var` Kotlin thường thì không được theo dõi.

**Sửa:** property mà UI đọc dùng `mutableStateOf` (setter `private`), class giữ trong `remember`.

```kotlin
class TrackingPermissions internal constructor(private val context: Context) {
    var issue by mutableStateOf<PermissionIssue?>(null)   // ✅ UI tự vẽ lại khi đổi
        private set
}
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt)
