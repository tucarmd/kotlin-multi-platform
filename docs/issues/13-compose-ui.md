# 13. Compose: layout, navigation, side effect (K601–K650)

Lỗi của phần "chạy việc ngoài UI" trong Compose (`LaunchedEffect`, `DisposableEffect`, coroutine scope), của danh sách
`LazyColumn`, của layout (thứ tự `Modifier`, inset, bàn phím) và của Navigation Compose. Phần lớn không crash mà làm UI
"hơi sai": dòng nhảy state, snackbar hiện lại, màn trắng sau khi bấm Back hai lần.

### K601 · Gọi hàm `suspend` ngay trong thân composable

`🟢 mới học` · `Compose`

**Gặp:** `Suspend function 'loadNotes' should be called only from a coroutine or another suspend function` ngay trong hàm
`@Composable`. Hoặc sửa "cho hết đỏ" bằng `GlobalScope.launch { }` trong thân hàm, rồi thấy request gửi đi hàng chục lần.

**Vì sao:** thân composable chạy lại mỗi lần recompose, có khi nhiều lần trong một giây. Nó không phải nơi để chạy việc
một lần, và không có coroutine nào bao quanh.

**Sửa:** việc tải dữ liệu đặt trong `ViewModel` (`init` hoặc hàm được gọi từ sự kiện). Việc gắn với vòng đời của một
composable thì đặt trong `LaunchedEffect(key)`.

```kotlin
@Composable
fun NotesScreen(vm: NotesViewModel) {
    GlobalScope.launch { vm.load() }        // ❌ chạy lại mỗi lần recompose
    LaunchedEffect(Unit) { vm.load() }      // ✅ chạy khi màn vào composition
}
```

Xem thêm: K602, K604

### K602 · `LaunchedEffect(Unit)` không chạy lại khi tham số đổi

`🟢 mới học` · `Compose`

**Gặp:** mở chi tiết ghi chú 5, quay lại mở ghi chú 7 (cùng một composable, chỉ đổi `id`) mà vẫn thấy nội dung ghi
chú 5. Hoặc đã cấp quyền rồi mà màn hình không tải lại vị trí.

**Vì sao:** `LaunchedEffect` chỉ huỷ và chạy lại khi **key** đổi. Key là `Unit` (hay `true`) thì không bao giờ đổi, block
dùng `id` cũ mãi mãi.

**Sửa:** đưa mọi giá trị mà block đọc và cần "làm lại khi đổi" vào key.

```kotlin
LaunchedEffect(Unit) { vm.load(id) }                 // ❌ id đổi cũng không tải lại
LaunchedEffect(id) { vm.load(id) }                   // ✅
LaunchedEffect(permission.granted) {                 // ✅ chạy lại khi vừa được cấp quyền
    if (permission.granted) monitor.refreshLocation()
}
```

**Gặp ở:** [`apps/DevicePulse/androidApp/src/main/kotlin/dev/devicepulse/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/DevicePulse/androidApp/src/main/kotlin/dev/devicepulse/android/ui/App.kt)

Xem thêm: K603, K615

### K603 · Key của `LaunchedEffect` đổi sau mỗi lần recompose

`🟡 đã làm app` · `Compose`

**Gặp:** effect bị huỷ rồi chạy lại liên tục: animation giật, request bị cancel giữa chừng, log "start" in ra không dứt.

**Vì sao:** key là object mới mỗi lần recompose: `listOf(...)` tạo trong thân hàm, một lambda, một `data class` dựng lại
với field thời gian, `Modifier`… Compose so key bằng `equals`, khác là restart.

**Sửa:** chọn key nhỏ và ổn định (id, `Boolean`, `String`), không truyền lambda hay object tạo tại chỗ làm key. Lambda
cần dùng bên trong thì bọc bằng `rememberUpdatedState` (K605).

```kotlin
LaunchedEffect(onDone) { ... }                   // ❌ lambda mới mỗi lần cha recompose → restart
LaunchedEffect(Filter(from, to, now)) { ... }    // ❌ now đổi mỗi giây → restart mỗi giây
LaunchedEffect(from, to) { convert() }           // ✅ nhiều key thì truyền thẳng
```

### K604 · Tưởng `LaunchedEffect(Unit)` chỉ chạy một lần trong đời app

`🟢 mới học` · `Compose`

**Gặp:** analytics "mở màn danh sách" đếm trùng; dữ liệu tải lại mỗi khi xoay máy hoặc quay về từ màn chi tiết.

**Vì sao:** "một lần" ở đây là một lần **mỗi khi composable vào composition**. Xoay máy tạo `Activity` mới (composition
mới); navigate đi rồi quay lại cũng làm destination rời và vào lại composition.

**Sửa:** việc một lần cho màn hình đặt trong `init` của `ViewModel` (sống qua xoay máy và qua việc đi màn khác rồi
về). Chỉ dùng `LaunchedEffect(Unit)` cho việc thật sự gắn với lúc màn đang hiện (ví dụ xin quyền).

Xem thêm: K501, K502

### K605 · Effect chạy lâu gọi lambda cũ

`🟡 đã làm app` · `Compose`

**Gặp:** splash chờ 3 giây rồi gọi `onTimeout`, nhưng callback được gọi là bản cũ (trỏ tới state cũ, `NavController`
cũ). Hoặc thêm `onTimeout` vào key thì timer bị reset mỗi lần cha recompose.

**Vì sao:** block của `LaunchedEffect` bắt (capture) giá trị lúc nó bắt đầu chạy. Tham số mới truyền vào sau đó không đi
vào block đang chạy.

**Sửa:** `rememberUpdatedState` giữ "bản mới nhất" mà không restart effect.

```kotlin
@Composable
fun Splash(onTimeout: () -> Unit) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(Unit) {
        delay(3_000)
        currentOnTimeout()          // ✅ luôn là lambda mới nhất
    }
}
```

### K606 · Gọi `navigate()` ngay trong thân composable

`🟢 mới học` · `Compose`

**Gặp:** đăng nhập xong màn hình nhấp nháy, back stack có 5 bản màn Home, hoặc app treo vì navigate lặp vô hạn.

**Vì sao:** thân composable chạy lại mỗi lần recompose. `if (state.loggedIn) nav.navigate(Home)` sẽ navigate lại mỗi lần
màn đó recompose, và navigate chính nó gây recompose.

**Sửa:** navigate trong callback sự kiện (`onClick`) hoặc trong `LaunchedEffect` có key là điều kiện, và để màn cũ ra
khỏi back stack bằng `popUpTo`.

```kotlin
if (state.loggedIn) nav.navigate(Home)                 // ❌ chạy lại mỗi lần recompose
LaunchedEffect(state.loggedIn) {                       // ✅ một lần khi điều kiện thành true
    if (state.loggedIn) nav.navigate(Home) { popUpTo<Login> { inclusive = true } }
}
```

Xem thêm: K647

### K607 · Sự kiện một lần (snackbar, toast) nằm trong state → hiện lại khi xoay máy

`🟡 đã làm app` · `Compose`

**Gặp:** snackbar "Đã xoá" hiện lại sau khi xoay máy hoặc quay lại màn hình, dù người dùng đã đóng nó.

**Vì sao:** state sống qua xoay máy (đó là mục đích của `ViewModel`). Nếu "cần hiện snackbar" là một field không ai
xoá, `LaunchedEffect(field)` ở composition mới sẽ thấy nó và hiện lại.

**Sửa:** sau khi xử lý xong, báo lại cho `ViewModel` để xoá field đó (`onUndo` / `onDismissUndo`), hoặc để core tự xoá
sau một thời gian. Màn hình chỉ vẽ cái state đang nói.

```kotlin
LaunchedEffect(deleted) {
    if (deleted == null) return@LaunchedEffect
    val result = snackbar.showSnackbar(deletedText, actionLabel = undoText, duration = SnackbarDuration.Indefinite)
    if (result == SnackbarResult.ActionPerformed) onUndo() else onDismissUndo()   // ✅ state được xoá
}
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt)

### K608 · `showSnackbar` trong `onClick` báo đỏ

`🟢 mới học` · `Compose`

**Gặp:** `Suspend function 'showSnackbar' should be called only from a coroutine or another suspend function` khi gọi
trong `onClick = { }`.

**Vì sao:** `onClick` là lambda thường, không phải coroutine. `LaunchedEffect` cũng không gọi được trong `onClick` vì
nó là composable.

**Sửa:** lấy scope bằng `rememberCoroutineScope()` ở thân composable, rồi `launch` trong callback. Scope này tự huỷ khi
composable rời màn hình.

```kotlin
val scope = rememberCoroutineScope()
Button(onClick = {
    onSave()
    scope.launch { snackbar.showSnackbar(savedText) }   // ✅
}) { Text("Lưu") }
```

**Gặp ở:** [`apps/DevicePulse/androidApp/src/main/kotlin/dev/devicepulse/android/ui/DashboardScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/DevicePulse/androidApp/src/main/kotlin/dev/devicepulse/android/ui/DashboardScreen.kt)

### K609 · Lưu dữ liệu bằng `rememberCoroutineScope` rồi navigate đi → lưu không xong

`🟡 đã làm app` · `Compose`

**Gặp:** bấm "Lưu" rồi về danh sách, thỉnh thoảng thay đổi không được ghi, không có lỗi nào.

**Vì sao:** scope của `rememberCoroutineScope` bị huỷ ngay khi composable rời composition. `scope.launch { repo.save() }`
rồi `nav.popBackStack()` thì màn bị gỡ và coroutine ghi DB bị cancel giữa chừng.

**Sửa:** việc phải chạy xong dù người dùng rời màn (ghi DB, gửi lên server) giao cho `ViewModel` (`viewModelScope`) hoặc
scope của cả app. Màn hình chỉ gọi `vm.save(...)` rồi navigate.

```kotlin
onClick = { scope.launch { repo.save(note) }; nav.popBackStack() }   // ❌ bị huỷ khi màn rời
onClick = { vm.save(note); nav.popBackStack() }                      // ✅ vm chạy trong viewModelScope
```

Xem thêm: K511

### K610 · `DisposableEffect` báo type mismatch

`🟢 mới học` · `Compose`

**Gặp:** compiler báo kiểu trả về sai: cần `DisposableEffectResult` mà khối lệnh trả `Unit`.

**Vì sao:** `DisposableEffect` bắt buộc dòng cuối là `onDispose { }`. Đó là cách nó biết phải dọn gì khi key đổi hoặc
composable rời đi.

**Sửa:** luôn kết thúc bằng `onDispose`, kể cả khi trống, và gỡ đúng cái vừa đăng ký.

```kotlin
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_START) vm.refresh() }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }   // ✅ bắt buộc là dòng cuối
}
```

### K611 · Lưu ghi chú trong `onDispose` → mỗi lần xoay máy tạo thêm một ghi chú

`🔴 khó, hiếm nhưng đau` · `Compose`

**Gặp:** viết ghi chú mới, xoay máy hai lần, quay về danh sách thấy ba ghi chú giống nhau.

**Vì sao:** `onDispose` chạy mỗi khi composable rời composition, và xoay máy cũng làm composition cũ bị gỡ. "Rời màn
hình" theo nghĩa của người dùng không phải là `onDispose`.

**Sửa:** lưu ở hành động rời màn rõ ràng: nút Back trên toolbar và `BackHandler` cho cử chỉ back. Thêm cờ để bấm Back
hai lần không lưu hai lần.

```kotlin
DisposableEffect(Unit) { onDispose { vm.save(id, title, body) } }   // ❌ chạy cả khi xoay
var left by remember { mutableStateOf(false) }
val leave = { if (!left) { left = true; vm.save(id, title, body); onDone() } }
BackHandler(onBack = leave)                                         // ✅
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteEditScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteEditScreen.kt)

Xem thêm: K646

### K612 · `LocalLifecycleOwner` bị gạch "deprecated"

`🟡 đã làm app` · `Compose`

**Gặp:** `androidx.compose.ui.platform.LocalLifecycleOwner` bị gạch ngang sau khi nâng Compose BOM.

**Vì sao:** `LocalLifecycleOwner` đã chuyển sang thư viện lifecycle (`androidx.lifecycle:lifecycle-runtime-compose`),
dùng được cả trong Compose Multiplatform. Bản trong `compose.ui` chỉ còn để tương thích.

**Sửa:** đổi import. Nếu chỉ cần "làm gì đó khi màn resume/start" thì dùng luôn `LifecycleResumeEffect` /
`LifecycleStartEffect` thay vì tự gắn observer (K613).

```kotlin
import androidx.compose.ui.platform.LocalLifecycleOwner      // ❌ deprecated
import androidx.lifecycle.compose.LocalLifecycleOwner        // ✅
```

### K613 · Muốn làm mới dữ liệu mỗi khi màn hình hiện lại

`🟡 đã làm app` · `Compose`

**Gặp:** chữ "5 phút trước" đứng yên khi mở app lại sau một giờ; tỷ giá không tự tải lại khi quay về app. `LaunchedEffect`
không giúp vì composable không rời composition khi app vào nền.

**Vì sao:** app vào nền rồi quay lại là sự kiện lifecycle (`ON_PAUSE` / `ON_RESUME`), không phải composition.

**Sửa:** `LifecycleResumeEffect` chạy mỗi lần resume. Khối lệnh phải kết thúc bằng `onPauseOrDispose { }`.

```kotlin
LifecycleResumeEffect(Unit) {
    viewModel.refreshClock()
    onPauseOrDispose { }        // bắt buộc, giống onDispose của DisposableEffect
}
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt) · [`apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/App.kt)

### K614 · `collectAsState` vẫn chạy khi app ở nền

`🟡 đã làm app` · `Compose`

**Gặp:** app đã vào nền mà flow GPS / cảm biến / polling vẫn chạy, pin tụt; log cho thấy vẫn có giá trị mới.

**Vì sao:** `collectAsState()` collect chừng nào composable còn trong composition, mà app vào nền thì composition vẫn
còn. Flow `stateIn(WhileSubscribed)` vì vậy không bao giờ thấy "hết người nghe".

**Sửa:** trên Android dùng `collectAsStateWithLifecycle()` (thư viện `lifecycle-runtime-compose`): dừng collect khi
lifecycle xuống dưới `STARTED`.

```kotlin
val state by viewModel.state.collectAsState()                 // ❌ collect cả khi ở nền
val state by viewModel.state.collectAsStateWithLifecycle()    // ✅
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt)

### K615 · `produceState` thiếu key → mở chuyến khác vẫn thấy chuyến cũ

`🟡 đã làm app` · `Compose`

**Gặp:** màn chi tiết tải dữ liệu một lần bằng `produceState`, nhưng khi `id` đổi (màn được dùng lại) vẫn hiện dữ liệu
cũ.

**Vì sao:** `produceState(initial) { }` không key thì giống `LaunchedEffect(Unit)`: chỉ chạy một lần mỗi composition.

**Sửa:** truyền các giá trị đầu vào làm key, sau giá trị khởi đầu.

```kotlin
val ui by produceState<DetailUi>(DetailUi.Loading, tripId) {      // ✅ tripId là key
    value = viewModel.load(tripId)?.let { DetailUi.Loaded(it) } ?: DetailUi.Missing
}
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/TripDetailScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/TripDetailScreen.kt)

### K616 · Đọc state Compose trong `LaunchedEffect` mà không thấy nó đổi

`🔴 khó, hiếm nhưng đau` · `Compose`

**Gặp:** muốn log / tải thêm khi người dùng cuộn gần cuối danh sách, viết
`LaunchedEffect(Unit) { if (listState.firstVisibleItemIndex > 20) loadMore() }`, không bao giờ chạy.

**Vì sao:** block chỉ đọc giá trị **một lần** lúc chạy. Đọc state trong coroutine không tự đăng ký theo dõi như đọc
trong thân composable.

**Sửa:** biến state Compose thành `Flow` bằng `snapshotFlow`, rồi collect.

```kotlin
LaunchedEffect(listState) {
    snapshotFlow { listState.firstVisibleItemIndex }
        .map { it > items.size - 5 }
        .distinctUntilChanged()
        .filter { it }
        .collect { vm.loadMore() }        // ✅ chạy mỗi khi chạm gần cuối
}
```

### K617 · Tự cuộn xuống dòng mới nhất

`🟡 đã làm app` · `Compose`

**Gặp:** console / chat thêm dòng mới ở cuối nhưng danh sách đứng yên, người dùng phải tự cuộn. Gọi `scrollToItem`
trong thân composable thì báo lỗi `suspend`.

**Vì sao:** cuộn là việc `suspend` (có animation) và cần chạy lại mỗi khi có dòng mới.

**Sửa:** `LaunchedEffect` với key là **số dòng**, bên trong gọi `animateScrollToItem(lastIndex)`.

```kotlin
val listState = rememberLazyListState()
LaunchedEffect(state.console.size) {
    if (state.console.isNotEmpty()) listState.animateScrollToItem(state.console.lastIndex)
}
LazyColumn(state = listState) { /* ... */ }
```

**Gặp ở:** [`apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/ConsoleScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/ConsoleScreen.kt)

### K618 · `LazyColumn` thiếu `key` → xoá một dòng, dòng bên dưới bị "vuốt dở"

`🟢 mới học` · `Compose`

**Gặp:** vuốt xoá dòng thứ 2, dòng thứ 3 trượt lên và hiện ở trạng thái đã vuốt một nửa, hoặc bị xoá luôn. Checkbox,
ô đang mở rộng "nhảy" sang dòng khác sau khi sắp xếp.

**Vì sao:** không có `key`, `LazyColumn` gắn state (`remember` trong item) theo **vị trí**. Dòng mới ở vị trí cũ nhận
state của dòng vừa mất.

**Sửa:** luôn truyền `key` là id ổn định của dữ liệu.

```kotlin
items(items) { item -> NoteRow(item) }                      // ❌ state theo vị trí
items(items, key = { it.note.id }) { item -> NoteRow(item) } // ✅ state theo id
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt) · [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/TripsScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/TripsScreen.kt)

Xem thêm: K619, K621

### K619 · `Key "USD" was already used`

`🟡 đã làm app` · `Compose`

**Gặp:** crash `java.lang.IllegalArgumentException: Key "USD" was already used. If you are using LazyColumn/Row please
make sure you provide a unique key for each item.`

**Vì sao:** key phải duy nhất trong **cả** `LazyColumn`, không chỉ trong một lời gọi `items`. Một đồng tiền vừa nằm ở
mục "Yêu thích" vừa ở "Tất cả" thì key trùng. Dữ liệu trùng id (API trả hai dòng giống nhau) cũng vậy.

**Sửa:** thêm tiền tố theo nhóm cho key; với dữ liệu ngoài thì lọc trùng (`distinctBy { it.id }`) trước khi vẽ.

```kotlin
item(key = "h-fav") { SectionHeader("Yêu thích") }
items(favorites, key = { "f-" + it.code }) { RateLine(it) }
item(key = "h-all") { SectionHeader("Tất cả") }
items(others, key = { "a-" + it.code }) { RateLine(it) }      // ✅ không trùng với "f-USD"
```

**Gặp ở:** [`apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/RatesScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/RatesScreen.kt)

### K620 · Dùng object làm key → `Type of the key … is not supported`

`🟡 đã làm app` · `Compose` · `Android`

**Gặp:** `IllegalArgumentException: Type of the key Note(id=1, …) is not supported. On Android you can only use types
which can be stored inside the Bundle.`

**Vì sao:** trên Android, key của lazy list được lưu vào `Bundle` để giữ vị trí cuộn qua xoay máy. Một `data class` thường
không vào `Bundle` được.

**Sửa:** dùng id (`Long`, `String`, `Int`) làm key, không dùng cả object.

```kotlin
items(notes, key = { it }) { ... }        // ❌ cả object
items(notes, key = { it.id }) { ... }     // ✅
```

### K621 · Hoàn tác xong, dòng vừa khôi phục bị xoá lần nữa

`🔴 khó, hiếm nhưng đau` · `Compose`

**Gặp:** vuốt xoá ghi chú, bấm "Hoàn tác", ghi chú hiện lại rồi biến mất ngay. Chỉ thấy khi chạy thật, test core vẫn xanh.

**Vì sao:** `rememberSwipeToDismissBoxState()` dùng `rememberSaveable`, và `LazyColumn` giữ state đã lưu **theo key** kể
cả sau khi dòng biến mất. Hoàn tác trả ghi chú về với cùng id, dòng đó khôi phục trạng thái "đã vuốt", `onDismiss` chạy
lại và xoá tiếp.

**Sửa:** state vuốt tạo bằng `remember` thường, để nó mất theo dòng.

```kotlin
val swipe = rememberSwipeToDismissBoxState()                       // ❌ được khôi phục theo key
val swipe = remember {                                             // ✅ mất khi dòng rời danh sách
    SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, density, positionalThreshold = threshold)
}
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt)

Xem thêm: K618

### K622 · `LazyColumn` trong `Column(Modifier.verticalScroll())` → crash

`🟢 mới học` · `Compose`

**Gặp:** `IllegalStateException: Vertically scrollable component was measured with an infinity maximum height
constraints, which is disallowed. One of the common reasons is nesting layouts like LazyColumn and
Column(Modifier.verticalScroll()).`

**Vì sao:** `verticalScroll` cho con chiều cao vô hạn, còn `LazyColumn` cần biết chiều cao để biết vẽ bao nhiêu dòng.

**Sửa:** chỉ một thứ cuộn. Đưa phần đầu (header, form) vào chính `LazyColumn` bằng `item { }`, hoặc cho `LazyColumn`
chiều cao xác định (`Modifier.weight(1f)` trong `Column` không cuộn).

```kotlin
Column(Modifier.verticalScroll(rememberScrollState())) {
    Header(); LazyColumn { items(rows) { Row(it) } }            // ❌
}
LazyColumn {
    item { Header() }
    items(rows, key = { it.id }) { Row(it) }                    // ✅
}
```

### K623 · `Column` + `forEach` cho danh sách dài → giật

`🟢 mới học` · `Compose`

**Gặp:** danh sách vài trăm dòng mở chậm, cuộn giật, bộ nhớ tăng.

**Vì sao:** `Column { list.forEach { Row(it) } }` dựng và đo **mọi** dòng ngay lập tức, kể cả dòng ngoài màn hình.

**Sửa:** danh sách không cố định số dòng thì dùng `LazyColumn`, chỉ vẽ dòng đang thấy. `Column` hợp cho vài mục cố định
(một form, vài thẻ).

### K624 · Dùng chỉ số làm key

`🟡 đã làm app` · `Compose`

**Gặp:** đã có `key` mà lỗi state nhảy dòng (K618) vẫn còn.

**Vì sao:** `items(list.size, key = { it })` hay `itemsIndexed(list, key = { i, _ -> i })` dùng **vị trí** làm key,
chẳng khác gì không có key.

**Sửa:** key lấy từ dữ liệu (id trong DB, mã đồng tiền, MAC của thiết bị), không lấy từ vị trí.

```kotlin
itemsIndexed(codes, key = { index, _ -> index }) { ... }   // ❌
items(codes, key = { it.code }) { ... }                    // ✅
```

### K625 · Sắp xếp, lọc, format trong item của `LazyColumn`

`🟡 đã làm app` · `Compose`

**Gặp:** cuộn giật trên máy yếu; profiler thấy `sortedBy` / `format` chạy liên tục.

**Vì sao:** thân item chạy lại mỗi lần item recompose hoặc cuộn vào màn hình. Việc tính toán trên cả danh sách đặt ở đó
lặp lại hàng trăm lần.

**Sửa:** sắp xếp / lọc trong `ViewModel` hoặc core (kết quả là `List` sẵn để vẽ). Trong composable, việc đắt mà phụ thuộc
input thì bọc `remember(input) { }`.

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt) (màn hình chỉ vẽ `state.items` đã sắp sẵn)

### K626 · Kéo để làm mới không phản ứng khi danh sách rỗng

`🟡 đã làm app` · `Compose`

**Gặp:** `PullToRefreshBox` chạy tốt khi có dữ liệu; lúc lỗi / rỗng (hiện một `Box` thông báo) thì kéo không có gì xảy ra.

**Vì sao:** pull-to-refresh dựa vào nested scroll của nội dung bên trong. Một `Box` / `Text` không cuộn được thì không có
cử chỉ kéo nào được báo lên.

**Sửa:** luôn đặt nội dung trong một thứ cuộn được (`LazyColumn` với một `item { }` thông báo, hoặc `Column` có
`verticalScroll`).

```kotlin
PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh) {
    LazyColumn(Modifier.fillMaxSize()) {
        if (state.table == null) item { EmptyError(onRefresh) }   // ✅ vẫn trong LazyColumn
        else rateList(state)
    }
}
```

**Gặp ở:** [`apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/RatesScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/RatesScreen.kt)

### K627 · Thứ tự `Modifier` làm padding, nền, vùng bấm sai

`🟢 mới học` · `Compose`

**Gặp:** nền chỉ tô phần chữ chứ không tô cả padding; vùng bấm nhỏ hơn hình nhìn thấy; ripple bị cắt.

**Vì sao:** `Modifier` áp dụng **từ trái sang phải**, mỗi cái bọc những cái phía sau. `padding` trước `background` thì nền
nằm bên trong padding.

**Sửa:** đọc chuỗi như "lớp ngoài → lớp trong" và đặt theo ý muốn.

```kotlin
Modifier.padding(16.dp).background(Color.Yellow)   // nền chỉ ở phần trong padding
Modifier.background(Color.Yellow).padding(16.dp)   // nền phủ cả padding
Modifier.clickable { }.padding(16.dp)              // ✅ vùng bấm gồm cả padding
```

### K628 · `fillMaxWidth()` trong `Row` đẩy nút bên cạnh ra ngoài

`🟢 mới học` · `Compose`

**Gặp:** `Row { Text(..., Modifier.fillMaxWidth()); IconButton(...) }` thì icon biến mất khỏi màn hình.

**Vì sao:** `Row` đo con theo thứ tự. Con đầu chiếm hết chiều rộng, con sau không còn chỗ.

**Sửa:** phần co giãn dùng `Modifier.weight(1f)`: nó được đo **sau cùng**, chiếm phần còn lại.

```kotlin
Row(verticalAlignment = Alignment.CenterVertically) {
    Text(title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)   // ✅
    IconButton(onClick = onPin) { Icon(Icons.Filled.Star, contentDescription = "Ghim") }
}
```

Xem thêm: K637

### K629 · `Unresolved reference: weight`

`🟢 mới học` · `Compose`

**Gặp:** `Modifier.weight(1f)` báo đỏ khi viết trong một composable tách riêng, dù chỗ gọi nằm trong `Row`.

**Vì sao:** `weight` là hàm mở rộng của `RowScope` / `ColumnScope`, chỉ có trong lambda nội dung của `Row` / `Column`.
Hàm composable tách ra không còn scope đó.

**Sửa:** truyền `modifier` từ chỗ gọi vào, hoặc khai báo composable là extension của scope.

```kotlin
@Composable
fun Label(text: String, modifier: Modifier = Modifier) = Text(text, modifier)
Row { Label("Tổng", Modifier.weight(1f)) }                          // ✅ weight ở chỗ gọi

@Composable
fun RowScope.Tab(label: String) = Text(label, Modifier.weight(1f))  // ✅ extension của RowScope
```

**Gặp ở:** [`apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/App.kt) (`RowScope.Tab`)

### K630 · Nội dung chui dưới `TopAppBar` của `Scaffold`

`🟢 mới học` · `Compose`

**Gặp:** dòng đầu của danh sách bị thanh tiêu đề che; Android Studio cảnh báo lint `UnusedMaterial3ScaffoldPaddingParameter`
(tham số padding của content không được dùng).

**Vì sao:** `Scaffold` không tự đẩy nội dung xuống. Nó đưa vào lambda một `PaddingValues` (chiều cao top bar, bottom
bar, system bar) và bạn phải áp dụng nó.

**Sửa:** dùng `padding` ở phần tử ngoài cùng của content (hoặc `contentPadding` của `LazyColumn` để danh sách cuộn được
dưới thanh trong suốt).

```kotlin
Scaffold(topBar = { TopAppBar(title = { Text("Ghi chú") }) }) { _ ->
    LazyColumn { ... }                                   // ❌ bị che
}
Scaffold(topBar = { TopAppBar(title = { Text("Ghi chú") }) }) { padding ->
    LazyColumn(Modifier.padding(padding)) { ... }        // ✅
}
```

### K631 · Edge-to-edge: chữ nằm dưới thanh trạng thái / thanh điều hướng

`🟡 đã làm app` · `Compose` · `Android`

**Gặp:** sau khi gọi `enableEdgeToEdge()` hoặc nâng `targetSdk` lên 35 (Android 15 bật edge-to-edge bắt buộc), nút ở đáy
nằm dưới thanh điều hướng, tiêu đề đè lên giờ và pin.

**Vì sao:** edge-to-edge nghĩa là app vẽ cả dưới system bar. Phần nào không dùng `Scaffold` (hoặc không áp dụng inset)
thì bị che.

**Sửa:** dùng `Scaffold` + padding của nó (K630); màn tự vẽ thì thêm `Modifier.safeDrawingPadding()` /
`windowInsetsPadding(WindowInsets.safeDrawing)` cho phần chứa nội dung bấm được.

**Gặp ở:** [`apps/TipSplit/androidApp/src/main/kotlin/dev/tipsplit/android/ui/SplitScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/androidApp/src/main/kotlin/dev/tipsplit/android/ui/SplitScreen.kt) (`MainActivity` gọi `enableEdgeToEdge()`, màn dùng padding của `Scaffold`)

### K632 · Hai `Scaffold` lồng nhau → khoảng trống thừa ở trên / dưới

`🟡 đã làm app` · `Compose`

**Gặp:** màn có bottom navigation (Scaffold ngoài) và mỗi tab có `TopAppBar` riêng (Scaffold trong): xuất hiện một dải
trống bằng chiều cao thanh trạng thái hoặc thanh điều hướng.

**Vì sao:** cả hai `Scaffold` đều tính inset của system bar vào padding, nên inset bị cộng hai lần.

**Sửa:** chỉ một `Scaffold` lo mỗi inset. `Scaffold` trong đặt `contentWindowInsets = WindowInsets(0)`; `Scaffold` ngoài
chỉ truyền padding đáy xuống `NavHost`.

```kotlin
NavHost(nav, startDestination = Rates, modifier = Modifier.padding(bottom = padding.calculateBottomPadding()))
// trong từng tab:
Scaffold(topBar = { TopAppBar(...) }, contentWindowInsets = WindowInsets(0)) { ... }   // ✅
```

**Gặp ở:** [`apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/RatesScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/RatesScreen.kt) · [`apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/App.kt)

### K633 · Bàn phím che ô nhập / nút "Lưu"

`🟢 mới học` · `Compose` · `Android`

**Gặp:** chạm vào ô số tiền, bàn phím hiện lên che mất ô đó và thẻ kết quả bên dưới; không cuộn lên được.

**Vì sao:** khi app edge-to-edge, cửa sổ không tự co lại vì bàn phím. Compose chỉ biết bàn phím chiếm bao nhiêu qua
inset `ime`, và bạn phải dùng nó.

**Sửa:** khai báo `android:windowSoftInputMode="adjustResize"` cho `Activity`, thêm `Modifier.imePadding()` cho khung chứa
form, và cho khung đó cuộn được.

```kotlin
Column(
    Modifier.fillMaxSize()
        .padding(padding)
        .imePadding()                          // ✅ chừa chỗ cho bàn phím
        .verticalScroll(rememberScrollState()) // ✅ còn cuộn được tới ô dưới cùng
        .padding(16.dp),
) { /* các ô nhập */ }
```

**Gặp ở:** [`apps/TipSplit/androidApp/src/main/kotlin/dev/tipsplit/android/ui/SplitScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/androidApp/src/main/kotlin/dev/tipsplit/android/ui/SplitScreen.kt) · [`apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/ConvertScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/ConvertScreen.kt)

Xem thêm: K634

### K634 · Có `imePadding()` mà bàn phím vẫn che

`🟡 đã làm app` · `Compose`

**Gặp:** đã thêm `imePadding()` nhưng ô nhập ở cuối form vẫn nằm dưới bàn phím, hoặc khoảng trống bị nhân đôi.

**Vì sao:** vị trí trong chuỗi `Modifier` quan trọng. `imePadding()` đặt **sau** `verticalScroll` thì padding nằm trong
vùng cuộn, không làm vùng nhìn thấy nhỏ lại. Đặt nó lên một phần tử con nằm dưới ô nhập (ví dụ chỉ nút "Lưu") thì chỉ
phần tử đó được đẩy lên. Thiếu `adjustResize` trong Manifest thì trên một số bản Android inset `ime` không được báo đúng.

**Sửa:** `imePadding()` một lần, ở khung ngoài cùng chứa form, **trước** `verticalScroll`; với `LazyColumn` thì đặt lên
chính `LazyColumn` hoặc khung chứa nó. Kiểm lại `android:windowSoftInputMode="adjustResize"`.

### K635 · Bấm "Done" trên bàn phím mà bàn phím không ẩn

`🟢 mới học` · `Compose`

**Gặp:** đặt `imeAction = ImeAction.Done` nhưng bấm nút đó không có gì xảy ra; con trỏ vẫn nhấp nháy.

**Vì sao:** `ImeAction` chỉ đổi **hình** của nút. Việc làm khi bấm phải khai báo trong `keyboardActions`.

**Sửa:** xử lý `onDone` / `onSend`, bỏ focus để ẩn bàn phím.

```kotlin
val focusManager = LocalFocusManager.current
OutlinedTextField(
    value = text, onValueChange = { text = it },
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
    keyboardActions = KeyboardActions(onSend = { send(); focusManager.clearFocus() }),   // ✅
)
```

**Gặp ở:** [`apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/ConsoleScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/ConsoleScreen.kt)

### K636 · `TextField` mất chữ, con trỏ nhảy về cuối khi gõ nhanh

`🔴 khó, hiếm nhưng đau` · `Compose`

**Gặp:** gõ nhanh thì mất ký tự, gõ ở giữa chuỗi thì con trỏ nhảy về cuối. Chỉ xảy ra khi `value` của `TextField` đến từ
`StateFlow` của `ViewModel`.

**Vì sao:** `TextField` cần state được cập nhật **đồng bộ** trong `onValueChange`. Nếu đường đi là
`onValueChange → viewModelScope.launch { ... } → flow có debounce/flatMap → collect`, giá trị cũ quay về ghi đè chữ
người dùng vừa gõ.

**Sửa:** giữ chữ đang gõ trong state cập nhật ngay (`mutableStateOf` trong `ViewModel` hoặc `rememberSaveable` ở màn
hình), và chỉ đẩy phần "xử lý" (tìm kiếm, validate) đi bất đồng bộ.

```kotlin
// ViewModel
var query by mutableStateOf("")
    private set
fun onQueryChange(text: String) {
    query = text                       // ✅ đồng bộ, TextField đọc ngay
    searchJob?.cancel(); searchJob = viewModelScope.launch { delay(300); search(text) }
}
```

### K637 · Chữ dài đẩy icon ra khỏi màn hình

`🟢 mới học` · `Compose`

**Gặp:** tiêu đề ghi chú dài 200 ký tự làm nút ghim biến mất hoặc dòng cao bất thường.

**Vì sao:** `Text` mặc định xuống dòng không giới hạn và chiếm hết chỗ nó cần.

**Sửa:** cho `Text` phần co giãn (`weight(1f)`), giới hạn `maxLines` và cắt bằng `TextOverflow.Ellipsis`.

```kotlin
Text(note.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt)

### K638 · Cỡ chữ đặt bằng `dp`, hoặc hộp cao cố định cắt chữ khi phóng to

`🟡 đã làm app` · `Compose`

**Gặp:** người dùng tăng cỡ chữ hệ thống thì chữ app không đổi (dùng `dp`), hoặc chữ bị cắt nửa trong ô có
`Modifier.height(40.dp)`.

**Vì sao:** `sp` co theo cài đặt cỡ chữ, `dp` thì không. Chữ to hơn mà khung cố định thì tràn.

**Sửa:** cỡ chữ dùng `sp` (tốt nhất là `MaterialTheme.typography`), khung chứa chữ dùng `heightIn(min = …)` thay vì
`height`. Thử bằng cỡ chữ 200% trong Settings.

### K639 · Icon bấm khó trúng

`🟢 mới học` · `Compose`

**Gặp:** người dùng bấm nút sao / nút xoá nhiều lần mới ăn; báo cáo Accessibility Scanner "Touch target too small".

**Vì sao:** `Icon(...).clickable { }` chỉ có vùng bấm bằng icon (24 dp). Khuyến nghị tối thiểu là 48 dp.

**Sửa:** dùng `IconButton` (đã có vùng bấm 48 dp), hoặc thêm `Modifier.minimumInteractiveComponentSize()`. Icon bấm được
phải có `contentDescription` để TalkBack đọc.

```kotlin
Icon(Icons.Filled.Delete, null, Modifier.clickable(onClick = onDelete))           // ❌ 24 dp, không tên
IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Xoá") }  // ✅
```

### K640 · `@Preview` không render vì composable nhận `ViewModel`

`🟢 mới học` · `Compose`

**Gặp:** khung Preview báo lỗi render, hoặc trống, khi composable có tham số `viewModel: NotesViewModel = koinViewModel()`.

**Vì sao:** Preview không chạy `Application`, không có Koin, không có DB. Tạo `ViewModel` trong Preview thất bại.

**Sửa:** tách hai tầng: `XxxRoute` lấy `ViewModel` và collect state, `XxxScreen` chỉ nhận giá trị thường và lambda.
Preview vẽ `XxxScreen` với dữ liệu giả.

```kotlin
@Composable
fun NoteListRoute(viewModel: NotesViewModel, onOpen: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NoteListScreen(state = state, onOpen = onOpen)            // Screen không biết ViewModel
}
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteListScreen.kt) · [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteEditScreen.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/NoteEditScreen.kt)

### K641 · `Navigation destination that matches request NavDeepLinkRequest … cannot be found`

`🟢 mới học` · `Compose`

**Gặp:** `IllegalArgumentException: Navigation destination that matches request NavDeepLinkRequest{ uri=android-app://androidx.navigation/note/5 } cannot be found in the navigation graph`.

**Vì sao:** với route dạng chuỗi, chuỗi truyền vào `navigate()` phải khớp đúng mẫu đã khai báo trong `composable(...)`.
Gõ `"note/5"` trong khi route là `"edit/{id}"`, hoặc quên khai báo destination, là không khớp.

**Sửa:** gom route vào một chỗ (hàm tạo chuỗi cạnh mẫu). Tốt hơn: dùng **route kiểu an toàn** (Navigation 2.8+):
route là `@Serializable` class/object, compiler kiểm thay bạn.

```kotlin
@Serializable object NoteList
@Serializable data class NoteEdit(val id: Long)

NavHost(nav, startDestination = NoteList) {
    composable<NoteList> { NoteListRoute(onOpen = { id -> nav.navigate(NoteEdit(id)) }) }
    composable<NoteEdit> { entry -> NoteEditRoute(id = entry.toRoute<NoteEdit>().id) }
}
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/App.kt) (route chuỗi gom trong `object Routes`)

Xem thêm: K642

### K642 · Route kiểu an toàn: `Serializer for class 'NoteEdit' is not found`

`🟢 mới học` · `Compose` · `Gradle`

**Gặp:** `SerializationException: Serializer for class 'NoteEdit' is not found. Please ensure that class is marked as
'@Serializable' and that the serialization compiler plugin is applied.`

**Vì sao:** route kiểu an toàn được biến thành chuỗi bằng kotlinx.serialization. Thiếu `@Serializable` hoặc module chưa
áp plugin serialization thì không có serializer.

**Sửa:** thêm `@Serializable` cho mọi route và áp plugin `org.jetbrains.kotlin.plugin.serialization` (cùng phiên bản Kotlin)
ở module chứa route.

```kotlin
plugins {
    alias(libs.plugins.kotlinSerialization)      // ✅ trong build.gradle.kts của module có route
}
```

Xem thêm: K360

### K643 · Route chuỗi: `getLong("id")` luôn ra 0

`🟡 đã làm app` · `Compose`

**Gặp:** mở ghi chú nào cũng ra màn "ghi chú mới"; log có cảnh báo `ClassCastException` khi đọc `Bundle`.

**Vì sao:** không khai báo `navArgument`, mọi tham số trong route chuỗi là `String`. `getLong` trên một giá trị `String`
trả mặc định `0`.

**Sửa:** khai báo kiểu cho từng tham số, hoặc chuyển sang route kiểu an toàn (K641) để khỏi đọc `Bundle` bằng tay.

```kotlin
composable("edit/{id}") { e -> e.arguments?.getLong("id") }                                // ❌ luôn 0
composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
    e -> e.arguments?.getLong("id") ?: 0L                                                  // ✅
}
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/App.kt) · [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/App.kt)

### K644 · Route có field là class tự viết → `could not find any NavType`

`🟡 đã làm app` · `Compose`

**Gặp:** `IllegalArgumentException: Route NoteEdit could not find any NavType for argument filter of type Filter -
typeMap received was {}`.

**Vì sao:** route kiểu an toàn tự hỗ trợ kiểu nguyên thuỷ, `String`, enum và danh sách của chúng. Kiểu tự định nghĩa
cần một `NavType` riêng truyền qua `typeMap`.

**Sửa:** cách tốt nhất là **không truyền object**: truyền id, màn đích tự đọc từ repository (K645). Nếu thật sự cần thì
viết `NavType` và khai báo `composable<NoteEdit>(typeMap = mapOf(typeOf<Filter>() to FilterNavType))`.

### K645 · Truyền cả object qua navigation

`🟡 đã làm app` · `Compose`

**Gặp:** màn chi tiết hiện dữ liệu cũ sau khi sửa ở màn khác; process death xong thì crash hoặc mất dữ liệu; route dài
hàng nghìn ký tự.

**Vì sao:** tham số navigation được lưu vào `Bundle` (có giới hạn kích thước, K515) và là **bản sao** lúc navigate, không
theo kịp dữ liệu thật trong DB.

**Sửa:** chỉ truyền khoá (id). Màn đích lấy dữ liệu từ nguồn chung (repository, core dùng chung).

```kotlin
nav.navigate(TripDetail(trip))        // ❌ bản sao, nặng
nav.navigate(TripDetail(trip.id))     // ✅ màn đích load(tripId)
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/App.kt)

### K646 · Bấm Back hai lần nhanh → màn trắng

`🟡 đã làm app` · `Compose`

**Gặp:** bấm nút mũi tên Back trên toolbar hai lần thật nhanh, app hiện màn trắng (NavHost rỗng), phải bấm Back hệ thống
để thoát.

**Vì sao:** `popBackStack()` không tham số bỏ **màn trên cùng**, kể cả khi đó là màn đầu tiên. Lần bấm thứ hai bỏ luôn
danh sách.

**Sửa:** pop **về** một đích cụ thể (`popBackStack(List, inclusive = false)`), hoặc chặn bấm hai lần bằng cờ.

```kotlin
onBack = { nav.popBackStack() }                                   // ❌ lần 2 pop mất màn đầu
onBack = { nav.popBackStack(Routes.LIST, inclusive = false) }     // ✅ đã ở LIST thì không làm gì
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/App.kt)

Xem thêm: K611

### K647 · Bấm một dòng hai lần → mở hai màn chi tiết chồng nhau

`🟡 đã làm app` · `Compose`

**Gặp:** chạm nhanh hai lần vào một dòng, phải bấm Back hai lần mới về danh sách.

**Vì sao:** mỗi `navigate()` đẩy thêm một bản lên back stack. Trong lúc animation chuyển màn, dòng cũ vẫn nhận chạm.

**Sửa:** `launchSingleTop = true` để không chồng hai bản cùng một đích; hoặc chỉ navigate khi màn hiện tại đang
`RESUMED`.

```kotlin
nav.navigate(NoteEdit(id)) { launchSingleTop = true }
// hoặc
if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) nav.navigate(NoteEdit(id))
```

### K648 · Bottom navigation: đổi tab là mất vị trí cuộn, back stack dài mãi

`🟡 đã làm app` · `Compose`

**Gặp:** chuyển qua lại hai tab vài lần thì bấm Back phải đi ngược qua mọi lần chuyển; quay lại tab cũ thì danh sách về
đầu.

**Vì sao:** `navigate(tab)` trơn đẩy thêm một bản mới mỗi lần, và không lưu state của tab cũ.

**Sửa:** mẫu chuẩn: pop về đích đầu kèm `saveState`, `launchSingleTop`, `restoreState`.

```kotlin
private fun NavController.switchTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

**Gặp ở:** [`apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/App.kt)

### K649 · Hai màn gọi `koinViewModel()` → hai `ViewModel` khác nhau

`🟡 đã làm app` · `Compose`

**Gặp:** xoá ghi chú ở màn sửa, quay về danh sách không thấy snackbar "Hoàn tác"; đổi đồng gốc ở tab Tỷ giá, tab Quy đổi
không biết.

**Vì sao:** `koinViewModel()` / `viewModel()` gọi **trong** `composable<…> { }` gắn `ViewModel` với back stack entry của
destination đó. Mỗi destination có một bản riêng.

**Sửa:** state dùng chung cho nhiều màn: tạo `ViewModel` **phía trên** `NavHost` rồi truyền xuống, hoặc lấy theo back stack
entry của graph cha. State đặt trong core dùng chung (Koin `single`) cũng được.

```kotlin
val viewModel: NotesViewModel = koinViewModel()        // ✅ một bản, trên NavHost
NavHost(nav, startDestination = NoteList) {
    composable<NoteList> { NoteListRoute(viewModel, ...) }
    composable<NoteEdit> { NoteEditRoute(viewModel, ...) }
}
```

**Gặp ở:** [`apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/androidApp/src/main/kotlin/dev/pocketnotes/android/ui/App.kt) · [`apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/App.kt)

### K650 · Truyền `NavController` vào `ViewModel` hoặc xuống tận composable con

`🟡 đã làm app` · `Compose`

**Gặp:** leak `Activity` sau khi xoay máy (LeakCanary chỉ vào `NavHostController`); composable con không preview được và
không test được vì cần `NavController` thật.

**Vì sao:** `NavController` gắn với `Activity` và composition hiện tại. `ViewModel` sống lâu hơn cả hai nên giữ nó là giữ
`Activity` cũ.

**Sửa:** màn hình nhận lambda (`onOpen: (Long) -> Unit`, `onBack: () -> Unit`); chỉ composable gốc (chỗ có `NavHost`)
biết `NavController`. `ViewModel` báo "đã lưu xong" bằng state, màn hình đọc state rồi gọi lambda.

```kotlin
class NotesViewModel(private val nav: NavController) : ViewModel()      // ❌ giữ Activity cũ
fun NoteListRoute(viewModel: NotesViewModel, onOpen: (Long) -> Unit)    // ✅ chỉ nhận lambda
```

Xem thêm: K507

---

**Nhóm trước / sau:** [12. Compose: state & recomposition](12-compose-state.md) · [14. Lưu dữ liệu](14-persistence.md) · [Mục lục 900 lỗi](index.md)
