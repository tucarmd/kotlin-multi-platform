# 11. Android: lifecycle, quyền, Manifest (K501–K550)

Lỗi của phần "Android thuần" quanh Compose: Activity bị tạo lại, `Context` bị giữ quá lâu, `Intent`, Manifest và quyền
runtime đổi theo từng bản Android. Người mới gặp ở tuần đầu; người đã làm app gặp lại mỗi lần nâng `targetSdk`.

### K501 · Xoay màn hình là mất dữ liệu đang hiện

`🟢 mới học` · `Android`

**Gặp:** xoay máy, đổi dark mode, hay đổi cỡ chữ là biến trong `Activity` về giá trị ban đầu, danh sách tải lại từ đầu.

**Vì sao:** mỗi configuration change, Android huỷ `Activity` cũ và tạo `Activity` mới. Mọi field của object cũ mất theo.

**Sửa:** giữ state màn hình trong `ViewModel` (sống qua xoay máy), giữ chữ người dùng gõ bằng `rememberSaveable` hoặc
`SavedStateHandle`. `Activity` chỉ giao cửa sổ cho Compose.

```kotlin
class MainActivity : ComponentActivity() {
    private var items = listOf<String>()          // ❌ mất khi xoay
}
class ListViewModel : ViewModel() {
    val items = MutableStateFlow(listOf<String>()) // ✅ sống qua xoay
}
```

Xem thêm: K513, K554

### K502 · Code "chỉ chạy một lần" trong `onCreate` chạy lại mỗi lần xoay

`🟢 mới học` · `Android`

**Gặp:** log "app started" hiện nhiều lần, analytics đếm trùng, request khởi động gửi lại khi xoay máy.

**Vì sao:** `onCreate` chạy cho **mỗi** `Activity` mới, kể cả bản tạo lại sau configuration change.

**Sửa:** việc một lần cho cả app đặt trong `Application.onCreate`. Việc một lần cho màn hình đặt trong `init` của
`ViewModel`, hoặc kiểm `savedInstanceState == null` (lần tạo đầu tiên).

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState == null) logAppOpened()   // ✅ không chạy lại khi xoay
}
```

### K503 · Khởi động Koin trong `MainActivity` → crash khi xoay máy

`🟢 mới học` · `Android`

**Gặp:** lần mở đầu chạy được, xoay máy thì crash với `KoinApplicationAlreadyStartedException`.

**Vì sao:** `startKoin` chỉ được gọi một lần trong mỗi process. `MainActivity.onCreate` chạy lại sau mỗi lần xoay.

**Sửa:** khởi động Koin trong `Application.onCreate`, chạy đúng một lần trước mọi màn hình, và truyền `androidContext`
là chính `Application`.

```kotlin
class TipSplitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {                               // ✅ một lần cho cả process
            androidContext(this@TipSplitApp)
            modules(appModule)
        }
    }
}
```

**Gặp ở:** [`apps/TipSplit/androidApp/src/main/kotlin/dev/tipsplit/android/TipSplitApp.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/androidApp/src/main/kotlin/dev/tipsplit/android/TipSplitApp.kt)

### K504 · Viết class `Application` mà không khai báo trong Manifest

`🟢 mới học` · `Android`

**Gặp:** màn đầu tiên crash với `KoinApplication has not been started`, hoặc thư viện báo chưa được khởi tạo; breakpoint
trong `Application.onCreate` không bao giờ dừng.

**Vì sao:** Android chỉ dùng class `Application` của bạn khi `<application android:name>` trỏ tới nó. Thiếu dòng đó,
hệ thống dùng `Application` mặc định và `onCreate` của bạn không chạy.

**Sửa:** thêm `android:name` (tên đầy đủ hoặc bắt đầu bằng dấu chấm, tính từ `namespace`).

```xml
<application
    android:name=".TipSplitApp"
    android:label="@string/app_name">
```

### K505 · Tưởng chỉ xoay máy mới làm `Activity` tạo lại

`🟢 mới học` · `Android`

**Gặp:** test xoay máy kỹ rồi mà người dùng vẫn mất dữ liệu khi đổi dark mode, ngôn ngữ, cỡ chữ, chia đôi màn hình
hoặc gập/mở máy gập.

**Vì sao:** đó đều là configuration change. Mặc định mọi loại đều huỷ và tạo lại `Activity`.

**Sửa:** đừng vá riêng từng trường hợp. Để state ở nơi sống qua việc tạo lại (`ViewModel`, `rememberSaveable`, DB),
rồi kiểm nhanh bằng cách đổi dark mode trong Quick Settings khi app đang mở.

Xem thêm: K506

### K506 · Thêm `android:configChanges` để "sửa" lỗi xoay máy

`🟡 đã làm app` · `Android`

**Gặp:** thêm `configChanges="orientation|screenSize"` thì hết mất dữ liệu khi xoay, nhưng đổi ngôn ngữ / dark mode vẫn
mất, và layout ngang không đổi resource `-land`.

**Vì sao:** `configChanges` chỉ bảo hệ thống không tạo lại `Activity` cho **các loại được liệt kê**. Lỗi gốc (state
nằm sai chỗ) vẫn còn, và process death (K513) vẫn xoá sạch.

**Sửa:** bỏ `configChanges`, đưa state vào `ViewModel` / `SavedStateHandle`. Chỉ dùng `configChanges` khi có lý do rõ
(ví dụ màn video tự xử lý xoay) và hiểu mình đang tắt cái gì.

```xml
<activity android:name=".MainActivity"
    android:configChanges="orientation|screenSize" /> <!-- ❌ che lỗi, không sửa lỗi -->
```

### K507 · `ViewModel` giữ `Activity`, `View` hoặc `Context` của màn hình

`🟢 mới học` · `Android`

**Gặp:** LeakCanary báo leak `MainActivity` sau khi xoay; bộ nhớ tăng dần mỗi lần xoay.

**Vì sao:** `ViewModel` sống lâu hơn `Activity`. Giữ `Activity` cũ trong nó thì cả cây View/Compose cũ không được giải
phóng.

**Sửa:** không truyền `Activity`/`View` vào `ViewModel`. Cần `Context` cho tài nguyên app thì dùng `applicationContext`
(qua DI hoặc `AndroidViewModel`). Việc cần `Activity` (xin quyền, mở share sheet) làm ở tầng UI.

```kotlin
class NotesViewModel(private val activity: Activity) : ViewModel()   // ❌ leak
class NotesViewModel(private val repository: NoteRepository) : ViewModel() // ✅
```

### K508 · Singleton hay `companion object` giữ `Context` của `Activity`

`🟡 đã làm app` · `Android`

**Gặp:** Android Studio cảnh báo `Do not place Android context classes in static fields; this is a memory leak`, hoặc
LeakCanary báo leak từ một `object`.

**Vì sao:** singleton sống bằng cả process. Giữ `Activity` trong đó thì `Activity` không bao giờ được giải phóng.

**Sửa:** singleton chỉ giữ `applicationContext`. Trong Koin, `androidContext(this)` gọi từ `Application` là đủ cho mọi
`single`.

```kotlin
object Prefs { lateinit var context: Context }
Prefs.context = this                     // ❌ trong Activity
Prefs.context = applicationContext       // ✅
```

### K509 · Mở dialog bằng `applicationContext` → `BadTokenException`

`🟢 mới học` · `Android`

**Gặp:** `android.view.WindowManager$BadTokenException: Unable to add window -- token null is not valid; is your activity running?`

**Vì sao:** dialog là một cửa sổ gắn vào `Activity`. `Application` không có window token.

**Sửa:** dialog dùng `Context` của `Activity` (trong Compose: `AlertDialog` composable, không cần `Context`). Ngược lại,
đừng giữ `Context` đó lâu hơn màn hình (K507).

### K510 · `startActivity` từ `Context` không phải `Activity` → crash

`🟢 mới học` · `Android`

**Gặp:** `android.util.AndroidRuntimeException: Calling startActivity() from outside of an Activity context requires the FLAG_ACTIVITY_NEW_TASK flag. Is this really what you want?`

**Vì sao:** `Context` của `Application` (hoặc service) không thuộc task nào, nên Android đòi bạn nói rõ là mở task mới.

**Sửa:** gọi từ `Activity` nếu có thể; nếu chỉ có `Context` chung thì thêm `FLAG_ACTIVITY_NEW_TASK`, như khi mở trang
Settings của app.

```kotlin
context.startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),   // ✅
)
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt)

### K511 · Việc chạy dài nằm trong `ViewModel` → bấm Back là dừng

`🟡 đã làm app` · `Android`

**Gặp:** chuyến đi đang ghi GPS dừng ngay khi người dùng bấm Back khỏi màn ghi, dù xoay máy thì không sao.

**Vì sao:** `ViewModel` sống qua xoay máy nhưng bị huỷ khi màn hình rời back stack (`onCleared`, `viewModelScope` bị huỷ).
Việc phải sống lâu hơn màn hình không được nằm ở đó.

**Sửa:** đưa state và coroutine của việc dài vào một object của cả app (`single` trong Koin, scope riêng của app), để
`ViewModel` chỉ là lớp mỏng điều khiển nó. Muốn chạy khi tắt màn hình thì thêm Foreground Service (nhóm 17).

```kotlin
class RecordViewModel(private val recorder: TripRecorder) : ViewModel() {
    val state = recorder.state     // ✅ state ở TripRecorder (Koin single), không ở đây
    fun stop() = recorder.stop()
}
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/RecordViewModel.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/RecordViewModel.kt)

### K512 · Ngắt kết nối dùng chung trong `onCleared()`

`🟡 đã làm app` · `Android`

**Gặp:** người dùng bấm Back rồi mở lại màn hình thì dongle Bluetooth đã bị ngắt, phải kết nối lại từ đầu.

**Vì sao:** `onCleared()` chạy mỗi khi `ViewModel` chết, tức là mỗi lần rời màn hình. Tài nguyên của cả app (kết nối
BLE, chuyến đi) không thuộc về một màn hình.

**Sửa:** tài nguyên dùng chung sống trong singleton; đóng nó bằng hành động rõ ràng (nút "Ngắt kết nối"), không phải
trong `onCleared()`. `onCleared()` chỉ dọn thứ chính `ViewModel` tạo ra.

**Gặp ở:** [`apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/ObdViewModel.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/ObdViewModel.kt)

### K513 · Process death: quay lại app thì màn hình còn nhưng dữ liệu mất

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** để app ở nền lâu, mở lại: Android đưa về đúng màn hình cũ, nhưng `ViewModel` trống, singleton trống, đôi khi
crash vì `lateinit` chưa gán.

**Vì sao:** khi thiếu RAM, Android giết process của app ở nền nhưng vẫn giữ back stack và `Bundle` đã lưu. Mở lại thì
`Activity` được tạo lại từ `Bundle`, còn mọi thứ chỉ nằm trong RAM (`ViewModel`, Koin `single`) đã mất.

**Sửa:** thứ người dùng gõ dở → `rememberSaveable` / `SavedStateHandle`; dữ liệu thật → DB; màn hình con nhận **id**
qua route và tự tải lại. Đừng giả định màn hình trước đã nạp sẵn dữ liệu.

```kotlin
class EditViewModel(private val handle: SavedStateHandle) : ViewModel() {
    val draft = handle.getStateFlow("draft", "")         // ✅ sống qua process death
    fun type(text: String) { handle["draft"] = text }
}
```

Xem thêm: K514, K515

### K514 · Không biết cách thử process death

`🟡 đã làm app` · `Android`

**Gặp:** lỗi K513 chỉ xảy ra trên máy người dùng, không tái hiện được trên emulator.

**Vì sao:** máy dev nhiều RAM, app hiếm khi bị giết. Bấm "Force stop" trong Settings **không** giống: nó xoá luôn back
stack và state đã lưu.

**Sửa:** đưa app xuống nền (Home), rồi giết process bằng `adb`; mở lại từ Recents. Hoặc bật Developer options →
"Don't keep activities" để thử phần `Activity` bị huỷ.

```bash
adb shell am kill dev.pocketnotes.android   # app phải đang ở nền
# mở lại app từ Recents: phải về đúng màn hình, đủ dữ liệu
```

### K515 · Lưu cả danh sách vào `Bundle` → `TransactionTooLargeException`

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** crash `android.os.TransactionTooLargeException` lúc app xuống nền (khi `onSaveInstanceState` chạy), không phải
lúc bấm nút nào.

**Vì sao:** `Bundle` của saved state đi qua Binder, giới hạn khoảng 1 MB cho cả giao dịch. Lưu list dài, ảnh, JSON lớn
vào `SavedStateHandle` / `rememberSaveable` là vượt.

**Sửa:** saved state chỉ giữ thứ nhỏ: id, chữ đang gõ, vị trí cuộn. Dữ liệu lớn để trong DB/file và tải lại theo id.

```kotlin
handle["notes"] = ArrayList(allNotes)   // ❌ có thể vượt ~1 MB
handle["openNoteId"] = noteId           // ✅
```

### K516 · `Cannot create an instance of class …ViewModel`

`🟢 mới học` · `Android`

**Gặp:** `java.lang.RuntimeException: Cannot create an instance of class dev.app.NotesViewModel` khi mở màn hình.

**Vì sao:** `viewModel()` mặc định chỉ tạo được `ViewModel` có constructor rỗng (hoặc chỉ nhận `Application` /
`SavedStateHandle`). Constructor có `repository` thì cần factory.

**Sửa:** dùng DI (`koinViewModel()` + `viewModelOf(::NotesViewModel)` trong module) hoặc viết `ViewModelProvider.Factory`.

```kotlin
val appModule = module { viewModelOf(::NotesViewModel) }   // ✅ Koin biết cách tạo
val vm: NotesViewModel = koinViewModel()
```

### K517 · Gửi `Long` qua `Intent` rồi đọc bằng `getIntExtra` → luôn ra 0

`🟡 đã làm app` · `Android`

**Gặp:** màn chi tiết luôn mở ghi chú id 0; Logcat có cảnh báo dạng `Key id expected Integer but value was a java.lang.Long. The default value 0 was returned.`

**Vì sao:** `Bundle` lưu kiểu thật của giá trị. Đọc sai kiểu không crash, chỉ trả giá trị mặc định.

**Sửa:** đọc đúng kiểu đã gửi, và đặt tên key là hằng số dùng chung cho cả hai đầu.

```kotlin
intent.putExtra(EXTRA_ID, note.id)              // id là Long
val id = intent.getIntExtra(EXTRA_ID, 0)        // ❌ luôn 0
val id = intent.getLongExtra(EXTRA_ID, 0L)      // ✅
```

### K518 · `getParcelableExtra(name)` bị gạch "deprecated"

`🟡 đã làm app` · `Android`

**Gặp:** cảnh báo deprecated trên `getParcelableExtra` / `getSerializableExtra` khi `compileSdk` ≥ 33.

**Vì sao:** từ API 33 có bản an toàn kiểu `getParcelableExtra(name, Class)`, nhưng bản đó không có trên máy cũ hơn.

**Sửa:** dùng `IntentCompat` của `androidx.core`, nó tự chọn API theo bản Android. Tốt hơn nữa: chỉ gửi id rồi tải lại.

```kotlin
val trip = intent.getParcelableExtra<Trip>("trip")                          // ❌ deprecated
val trip = IntentCompat.getParcelableExtra(intent, "trip", Trip::class.java) // ✅
```

### K519 · `@Parcelize` báo không tìm thấy

`🟢 mới học` · `Android` · `Gradle`

**Gặp:** `Unresolved reference` ở `Parcelize`, hoặc class có `@Parcelize` vẫn bị báo chưa implement `Parcelable`.

**Vì sao:** `@Parcelize` cần plugin Gradle `kotlin-parcelize`; thiếu plugin thì annotation không tồn tại và không có code
nào được sinh.

**Sửa:** thêm plugin `org.jetbrains.kotlin.plugin.parcelize` vào module, class implement `Parcelable`.

```kotlin
plugins { id("org.jetbrains.kotlin.plugin.parcelize") }

@Parcelize
data class Filter(val query: String, val pinnedOnly: Boolean) : Parcelable
```

### K520 · `singleTop`: mở lại qua notification nhưng `intent` vẫn là cái cũ

`🟡 đã làm app` · `Android`

**Gặp:** bấm notification khi app đang mở, `Activity` không đọc được extra mới, vẫn thấy extra của lần mở đầu.

**Vì sao:** với `launchMode="singleTop"` mà `Activity` đang ở đỉnh, Android không tạo `Activity` mới mà gọi
`onNewIntent(intent)`. `getIntent()` vẫn trả intent ban đầu.

**Sửa:** override `onNewIntent`, gọi `setIntent(intent)` rồi xử lý lại.

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)           // ✅ để code đọc this.intent thấy cái mới
    handle(intent)
}
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/AndroidManifest.xml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/AndroidManifest.xml) (`launchMode="singleTop"`)

### K521 · `ActivityNotFoundException` khi mở link / email / share

`🟢 mới học` · `Android`

**Gặp:** `android.content.ActivityNotFoundException: No Activity found to handle Intent { act=android.intent.action.VIEW … }`.

**Vì sao:** intent ngầm (`ACTION_VIEW`, `ACTION_SENDTO`) chỉ chạy khi máy có app nhận nó. Emulator, máy công ty, máy bị
gỡ trình duyệt thì không có.

**Sửa:** bọc `startActivity` trong `try/catch` và báo người dùng, hoặc dùng `Intent.createChooser` cho share.

```kotlin
try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
} catch (_: ActivityNotFoundException) {
    showMessage(R.string.no_browser)   // ✅
}
```

### K522 · Android 11+: `resolveActivity` trả `null` dù máy có app

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** từ khi `targetSdk` ≥ 30, nút "Mở bằng Maps" biến mất hoặc báo không có app, dù máy có Google Maps.

**Vì sao:** package visibility của Android 11: app không thấy app khác khi query (`resolveActivity`,
`queryIntentActivities`, `getPackageInfo`) trừ khi khai báo trong `<queries>`.

**Sửa:** khai báo `<queries>` cho intent cần kiểm. Nếu chỉ cần mở, gọi thẳng `startActivity` trong `try/catch` (K521),
không cần query trước.

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="geo" />
    </intent>
</queries>
```

### K523 · Build lỗi: thiếu `android:exported`

`🟢 mới học` · `Android`

**Gặp:** Manifest merger báo `android:exported needs to be explicitly specified for element <activity#…>. Apps targeting Android 12 and higher are required to specify an explicit value for android:exported …`.

**Vì sao:** từ `targetSdk` 31, mọi `activity`/`service`/`receiver` có `intent-filter` phải khai báo rõ có cho app khác
gọi hay không.

**Sửa:** launcher activity: `exported="true"`. Service, receiver chỉ app mình dùng: `exported="false"`.

```xml
<activity android:name=".MainActivity" android:exported="true"> … </activity>
<service android:name=".service.TrackingService" android:exported="false" />
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/AndroidManifest.xml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/AndroidManifest.xml)

### K524 · Cài xong không thấy icon app / "Default Activity not found"

`🟢 mới học` · `Android`

**Gặp:** Android Studio báo `Default Activity not found` khi Run, hoặc cài được mà launcher không có icon.

**Vì sao:** launcher chỉ hiện `Activity` có `intent-filter` với `action.MAIN` **và** `category.LAUNCHER`. Thiếu một
trong hai (hay gõ sai) là không có điểm vào.

**Sửa:** kiểm lại `intent-filter` của `MainActivity`.

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
</intent-filter>
```

### K525 · `Manifest merger failed` khi thêm thư viện

`🟡 đã làm app` · `Android` · `Gradle`

**Gặp:** `Manifest merger failed : Attribute application@allowBackup value=(true) from AndroidManifest.xml … is also present at [lib] … Suggestion: add 'tools:replace="android:allowBackup"' …`.

**Vì sao:** Manifest của app được gộp với Manifest của mọi thư viện. Cùng một thuộc tính có hai giá trị khác nhau thì
Gradle không tự chọn.

**Sửa:** chọn giá trị của app bằng `tools:replace` (khai báo `xmlns:tools`). Xem Manifest sau khi gộp ở tab
"Merged Manifest" của Android Studio.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <application android:allowBackup="false" tools:replace="android:allowBackup">
```

### K526 · `PendingIntent` crash khi `targetSdk` ≥ 31

`🟡 đã làm app` · `Android`

**Gặp:** `IllegalArgumentException: … Targeting S+ (version 31 and above) requires that one of FLAG_IMMUTABLE or FLAG_MUTABLE be specified when creating a PendingIntent.`

**Vì sao:** từ Android 12 phải nói rõ app khác có được sửa intent bên trong không.

**Sửa:** mặc định dùng `FLAG_IMMUTABLE`; chỉ dùng `FLAG_MUTABLE` khi hệ thống cần điền thêm (ví dụ inline reply).

```kotlin
PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_IMMUTABLE)   // ✅
```

### K527 · Android 14: `registerReceiver` crash vì thiếu cờ exported

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** `SecurityException: … One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED should be specified when a receiver isn't being registered exclusively for system broadcasts`.

**Vì sao:** từ `targetSdk` 34, receiver đăng ký bằng code phải nói rõ app khác có gửi broadcast tới được không (trừ khi
chỉ nghe broadcast của hệ thống).

**Sửa:** dùng `ContextCompat.registerReceiver` với cờ phù hợp; broadcast nội bộ thì `RECEIVER_NOT_EXPORTED`.

```kotlin
ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)   // ✅
```

### K528 · Xin quyền mà không có hộp thoại nào hiện lên

`🟢 mới học` · `Android`

**Gặp:** gọi `launcher.launch(...)` xong callback trả "denied" ngay, người dùng chưa thấy hộp thoại nào.

**Vì sao:** quyền chưa được khai báo bằng `<uses-permission>` trong Manifest thì hệ thống từ chối luôn, không hỏi.
(Một lý do khác: người dùng đã từ chối hai lần, xem K531.)

**Sửa:** khai báo đủ quyền trong Manifest của module app (xem "Merged Manifest" nếu quyền nằm ở thư viện).

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### K529 · Gọi API cần quyền mà không kiểm quyền trước

`🟢 mới học` · `Android`

**Gặp:** lint báo `MissingPermission`; chạy thì `SecurityException` khi người dùng chưa cấp hoặc đã thu hồi quyền.

**Vì sao:** khai báo trong Manifest chưa đủ với quyền nguy hiểm (vị trí, camera, Bluetooth 12+…): người dùng cấp lúc
chạy và có thể thu hồi bất cứ lúc nào.

**Sửa:** `checkSelfPermission` ngay trước khi gọi, và vẫn bắt `SecurityException` cho trường hợp bị thu hồi giữa hai
dòng. Trả về một trạng thái lỗi rõ ràng cho UI thay vì crash.

```kotlin
if (!hasPermission()) { close(LocationUnavailableException(PermissionDenied)); return@callbackFlow }
try {
    manager.requestLocationUpdates(GPS_PROVIDER, 1_000L, 0f, listener, Looper.getMainLooper())
} catch (_: SecurityException) { close(LocationUnavailableException(PermissionDenied)) }  // ✅
```

**Gặp ở:** [`apps/TripLog/shared/src/androidMain/kotlin/dev/triplog/platform/AndroidLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/androidMain/kotlin/dev/triplog/platform/AndroidLocationStream.kt)

### K530 · `registerForActivityResult` gọi muộn → `IllegalStateException`

`🟡 đã làm app` · `Android`

**Gặp:** `IllegalStateException: LifecycleOwner … is attempting to register while current state is RESUMED. LifecycleOwners must call register before they are STARTED.`

**Vì sao:** launcher phải được đăng ký khi `Activity`/`Fragment` đang được tạo, để kết quả về đúng chỗ kể cả sau khi
`Activity` bị tạo lại. Đăng ký trong `onClick` là quá muộn.

**Sửa:** đăng ký thành field (View) hoặc `rememberLauncherForActivityResult` ở đầu composable; trong `onClick` chỉ
gọi `launch`.

```kotlin
val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
    permissions.onResult()                 // ✅ đăng ký trong composition
}
Button(onClick = { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) }) { … }
```

### K531 · Từ chối hai lần là hộp thoại xin quyền không hiện nữa

`🟢 mới học` · `Android`

**Gặp:** nút "Cấp quyền" bấm không có phản ứng gì; callback trả "denied" ngay lập tức.

**Vì sao:** từ Android 11, người dùng bấm "Don't allow" hai lần cho cùng một quyền thì hệ thống coi như "đừng hỏi nữa"
và tự từ chối mọi lần xin sau.

**Sửa:** khi đã ở trạng thái này, nút phải mở trang cài đặt của app thay vì xin quyền lần nữa.

```kotlin
fun openSettings() = context.startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
)
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt) · Xem thêm: K532

### K532 · Không phân biệt được "chưa hỏi lần nào" và "đã bị từ chối vĩnh viễn"

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** mở app lần đầu đã hiện nút "Mở Cài đặt", hoặc ngược lại: bị chặn vĩnh viễn mà nút vẫn gọi hộp thoại không
bao giờ hiện.

**Vì sao:** Android không có API trả thẳng "đã chặn vĩnh viễn". `shouldShowRequestPermissionRationale` trả `false` cả
khi **chưa hỏi lần nào** lẫn khi **đã bị từ chối hai lần**.

**Sửa:** tự lưu một cờ "đã hỏi" (DataStore/SharedPreferences) ngay trước khi `launch`. Chưa cấp + đã hỏi + rationale
`false` ⇒ chặn vĩnh viễn → mở Settings. Chưa hỏi ⇒ hiện hộp thoại.

```kotlin
fun state(activity: Activity, perm: String, asked: Boolean) = when {
    activity.checkSelfPermission(perm) == PERMISSION_GRANTED -> Granted
    activity.shouldShowRequestPermissionRationale(perm) -> Denied        // từ chối 1 lần, còn hỏi được
    asked -> PermanentlyDenied                                            // ✅ chỉ biết nhờ cờ tự lưu
    else -> NotAsked
}
```

**Gặp ở:** [`apps/DevicePulse/androidApp/src/main/kotlin/dev/devicepulse/android/ui/LocationPermission.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/DevicePulse/androidApp/src/main/kotlin/dev/devicepulse/android/ui/LocationPermission.kt) (chỉ biết `granted`, chưa phân biệt) · [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt)

### K533 · Người dùng thu hồi quyền trong Settings, quay lại app thấy state cũ

`🟡 đã làm app` · `Android`

**Gặp:** từ Settings quay lại, màn hình vẫn ghi "Đã cấp quyền" hoặc vẫn ghi "Chưa cấp" dù vừa bật; hoặc app mở lại từ
đầu vì process đã bị giết.

**Vì sao:** thu hồi quyền runtime trong Settings làm hệ thống giết process của app. Cấp thêm quyền thì không giết, nhưng
cũng không ai báo cho app biết. State đọc một lần lúc mở màn hình là cũ.

**Sửa:** đọc lại quyền (và trạng thái Bluetooth/GPS) mỗi lần màn hình `ON_RESUME`.

```kotlin
LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    granted = hasAll(context)          // ✅ đọc lại sau khi người dùng quay về từ Settings
    radioOn = isRadioOn(context)
}
```

**Gặp ở:** [`apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/BlePermission.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/BlePermission.kt)

### K534 · Quyền "Chỉ lần này" biến mất sau một lúc

`🟡 đã làm app` · `Android`

**Gặp:** hôm qua cấp quyền vị trí, hôm nay mở app lại bị hỏi; tester nói "app quên quyền".

**Vì sao:** từ Android 11, vị trí / micro / camera có lựa chọn "Only this time". Quyền tự bị thu hồi sau khi app rời
foreground một thời gian. Android 11+ cũng tự thu hồi quyền của app lâu không dùng.

**Sửa:** không lưu "đã có quyền" vào storage. Luôn `checkSelfPermission` trước khi dùng (K529), coi việc mất quyền là
chuyện bình thường.

### K535 · Android 12+: xin mỗi `ACCESS_FINE_LOCATION` thì không có hộp thoại

`🟡 đã làm app` · `Android`

**Gặp:** trên Android 12+ xin quyền vị trí không thấy hộp thoại; Logcat ghi rằng `ACCESS_FINE_LOCATION` phải được xin
cùng `ACCESS_COARSE_LOCATION`.

**Vì sao:** từ `targetSdk` 31, người dùng được chọn "chính xác" hoặc "gần đúng", nên hệ thống đòi app xin cả hai quyền
trong cùng một yêu cầu.

**Sửa:** luôn xin cặp FINE + COARSE cùng lúc, và khai báo cả hai trong Manifest.

```kotlin
launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))                                       // ❌
launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) // ✅
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt)

### K536 · Người dùng chỉ cho "vị trí gần đúng" → app ghi được chuyến trống

`🟡 đã làm app` · `Android`

**Gặp:** quyền vị trí "đã cấp", GPS chạy, nhưng chuyến đi không có điểm nào; không có lỗi nào.

**Vì sao:** chọn "Approximate" (Android 12+) chỉ cấp COARSE. Điểm khi đó rộng cỡ km, bộ lọc nhiễu 30 m bỏ hết. Kiểm
"có quyền vị trí" chung chung thì không thấy khác biệt.

**Sửa:** kiểm riêng FINE và COARSE sau khi có kết quả; chỉ COARSE là một trường hợp riêng, giải thích cho người dùng và
mời họ chọn "Precise".

```kotlin
issue = when {
    fine -> null
    coarse -> PermissionIssue.PreciseDenied   // ✅ báo riêng, không coi là "đã có quyền"
    else -> PermissionIssue.LocationDenied
}
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt)

### K537 · Xin vị trí nền cùng lúc với vị trí thường → bị bỏ qua

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** trên Android 11+, mảng quyền có thêm `ACCESS_BACKGROUND_LOCATION` thì không có hộp thoại nào, tất cả bị từ chối.

**Vì sao:** từ Android 11, vị trí nền phải xin **riêng**, sau khi đã có vị trí foreground, và người dùng cấp nó trong
trang Settings ("Allow all the time"), không phải trong hộp thoại thường.

**Sửa:** xin FINE + COARSE trước. Chỉ khi thật cần, giải thích rồi xin `ACCESS_BACKGROUND_LOCATION` ở bước sau. Nhiều
app (như ghi chuyến đi) không cần quyền này: Foreground Service loại `location` đã đủ khi đang ghi.

### K538 · `POST_NOTIFICATIONS` không phải là quyền runtime trên mọi bản Android

`🟢 mới học` · `Android`

**Gặp:** app không bao giờ hiện notification trên Android 13+; hoặc code xin `POST_NOTIFICATIONS` trên Android 12 và
luôn nhận "denied".

**Vì sao:** thông báo chỉ là quyền runtime từ Android 13 (API 33). Máy cũ hơn không có quyền này để xin; thông báo được
phép sẵn.

**Sửa:** khai báo trong Manifest, chỉ xin khi `SDK_INT >= 33`, và coi máy cũ là "đã có".

```kotlin
if (Build.VERSION.SDK_INT >= 33 && !has(Manifest.permission.POST_NOTIFICATIONS)) {
    add(Manifest.permission.POST_NOTIFICATIONS)
}
val notifications = Build.VERSION.SDK_INT < 33 || has(Manifest.permission.POST_NOTIFICATIONS)  // ✅
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/Permissions.kt)

### K539 · Android ≤ 11: quét BLE ra danh sách rỗng, không lỗi gì

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** trên máy Android 11 trở xuống, quét Bluetooth LE chạy "thành công" nhưng không thấy thiết bị nào. Máy Android
12+ thì thấy.

**Vì sao:** tới API 30, kết quả quét BLE bị coi là thông tin vị trí: thiếu `ACCESS_FINE_LOCATION` (runtime) thì hệ thống
lặng lẽ không trả kết quả. Dịch vụ vị trí (công tắc GPS) của máy cũng phải bật.

**Sửa:** chọn quyền theo API level: ≤ 30 xin `ACCESS_FINE_LOCATION`, 31+ xin `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT`.
Xin quyền **trước** khi quét.

```kotlin
fun blePermissions(sdk: Int = Build.VERSION.SDK_INT): Array<String> =
    if (sdk >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
```

**Gặp ở:** [`apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/BlePermission.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/BlePermission.kt)

### K540 · Android 12+ vẫn đòi quyền vị trí cho Bluetooth

`🟡 đã làm app` · `Android`

**Gặp:** app chỉ nói chuyện với một dongle OBD mà vẫn phải xin quyền vị trí trên máy mới; người dùng nghi ngờ app theo dõi.

**Vì sao:** `BLUETOOTH_SCAN` mặc định vẫn được coi là có thể suy ra vị trí. Muốn bỏ hẳn vị trí trên 31+ thì phải cam
kết bằng `neverForLocation`. Quyền cũ cũng nên giới hạn bằng `maxSdkVersion="30"` để không lọt sang máy mới.

**Sửa:** khai báo hai nhánh quyền trong Manifest, mỗi nhánh cho đúng dải API.

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

**Gặp ở:** [`apps/ObdLink/androidApp/src/main/AndroidManifest.xml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/androidApp/src/main/AndroidManifest.xml)

### K541 · Nhờ hệ thống bật Bluetooth → `SecurityException` trên Android 12+

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** bấm "Bật Bluetooth" thì crash `SecurityException` trên Android 12+, chạy bình thường trên máy cũ.

**Vì sao:** từ API 31, mở `BluetoothAdapter.ACTION_REQUEST_ENABLE` cần quyền runtime `BLUETOOTH_CONNECT`. Máy cũ không có
quyền này nên không lỗi.

**Sửa:** chưa có quyền thì xin quyền trước, có rồi mới mở hộp thoại bật Bluetooth.

```kotlin
askRadioOn = {
    if (hasAll(context)) enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))  // ✅
    else permissionLauncher.launch(blePermissions())
}
```

**Gặp ở:** [`apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/BlePermission.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/androidApp/src/main/kotlin/dev/obdlink/android/ui/BlePermission.kt)

### K542 · `LocationListener { }` dạng lambda crash trên Android < 11

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** chạy tốt trên máy mới; trên Android 10 trở xuống crash `AbstractMethodError` khi hệ thống gọi
`onStatusChanged` / `onProviderEnabled`.

**Vì sao:** `LocationListener` chỉ có default cho 3 hàm phụ từ API 30. Viết bằng lambda (SAM) thì chỉ có
`onLocationChanged`; máy cũ gọi một hàm trừu tượng không có code.

**Sửa:** viết `object : LocationListener` và override đủ 4 hàm.

```kotlin
val listener = LocationListener { trySend(it.toPoint()) }   // ❌ AbstractMethodError trên API < 30
val listener = object : LocationListener {                  // ✅
    override fun onLocationChanged(location: Location) { trySend(location.toPoint()) }
    override fun onProviderDisabled(provider: String) { close(LocationUnavailableException(LocationOff)) }
    override fun onProviderEnabled(provider: String) = Unit
    @Deprecated("Deprecated in API 29, still called on older versions")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}
```

**Gặp ở:** [`apps/TripLog/shared/src/androidMain/kotlin/dev/triplog/platform/AndroidLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/androidMain/kotlin/dev/triplog/platform/AndroidLocationStream.kt)

### K543 · Chia sẻ file bằng `file://` → `FileUriExposedException`

`🟢 mới học` · `Android`

**Gặp:** `android.os.FileUriExposedException: file:///data/user/0/…/trip.gpx exposed beyond app through ClipData.Item.getUri()`.

**Vì sao:** từ Android 7, đưa đường dẫn `file://` cho app khác bị cấm: app kia cũng không đọc được thư mục riêng của bạn.

**Sửa:** dùng `FileProvider`: nó cấp một `content://` uri kèm quyền đọc tạm thời.

```kotlin
val uri = Uri.fromFile(file)                                                          // ❌
val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)  // ✅
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/GpxShare.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/GpxShare.kt)

### K544 · `FileProvider`: `Failed to find configured root that contains …`

`🟡 đã làm app` · `Android`

**Gặp:** `IllegalArgumentException: Failed to find configured root that contains /data/data/…/cache/gpx/trip.gpx`; hoặc
`Couldn't find meta-data for provider with authority …`.

**Vì sao:** `FileProvider` chỉ chia sẻ thư mục được liệt kê trong `res/xml/file_paths.xml`, và authority trong code phải
trùng `android:authorities` trong Manifest.

**Sửa:** khai báo thư mục chứa file (ở đây `cacheDir/gpx/`) và dùng cùng một authority ở hai chỗ.

```xml
<!-- res/xml/file_paths.xml -->
<paths>
    <cache-path name="gpx" path="gpx/" />
</paths>
<!-- AndroidManifest.xml -->
<provider android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false" android:grantUriPermissions="true"> … </provider>
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/res/xml/file_paths.xml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/res/xml/file_paths.xml)

### K545 · Share qua chooser: app nhận báo "permission denied" trên uri

`🔴 khó, hiếm nhưng đau` · `Android`

**Gặp:** chia sẻ thẳng tới Gmail thì được, qua `Intent.createChooser` thì vài app nhận báo không đọc được file.

**Vì sao:** quyền đọc uri tạm thời được cấp theo `data`/`ClipData` của intent. Uri chỉ nằm trong `EXTRA_STREAM` thì
quyền không phải lúc nào cũng đi theo qua chooser.

**Sửa:** thêm `FLAG_GRANT_READ_URI_PERMISSION` **và** đặt `clipData` chứa cùng uri.

```kotlin
val send = Intent(Intent.ACTION_SEND)
    .setType("application/gpx+xml")
    .putExtra(Intent.EXTRA_STREAM, uri)
    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
send.clipData = ClipData.newRawUri(fileName, uri)       // ✅ quyền đi theo ClipData
context.startActivity(Intent.createChooser(send, chooserTitle))
```

**Gặp ở:** [`apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/GpxShare.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/kotlin/dev/triplog/android/ui/GpxShare.kt)

### K546 · `Call requires API level 26 (current min is 24)`

`🟢 mới học` · `Android`

**Gặp:** lint `NewApi` báo `Call requires API level 26 (current min is 24)`; bỏ qua thì crash `NoSuchMethodError` trên
máy cũ.

**Vì sao:** `minSdk` 24 nghĩa là app cài được trên Android 7. API thêm sau đó không có trên những máy này.

**Sửa:** bọc bằng `if (Build.VERSION.SDK_INT >= …)` (có nhánh thay thế), dùng bản `…Compat` của `androidx.core`, hoặc
đánh dấu hàm `@RequiresApi` để caller phải kiểm.

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    manager.createNotificationChannel(channel)   // ✅ channel chỉ có từ API 26
}
```

### K547 · Chữ cứng trong code, `%d` gặp `String` → crash

`🟢 mới học` · `Android`

**Gặp:** cảnh báo `Hardcoded string`; hoặc `java.util.IllegalFormatConversionException: d != java.lang.String` khi
format chuỗi.

**Vì sao:** chữ cứng không dịch được. Placeholder trong `strings.xml` phải khớp kiểu tham số: `%d` cho số, `%s` cho chuỗi.

**Sửa:** mọi chữ hiện lên màn hình nằm trong `strings.xml`, có placeholder đánh số (`%1$s`, `%2$d`) để bản dịch đổi
thứ tự được.

```xml
<string name="no_results">Không có ghi chú nào khớp "%1$s"</string>
```

```kotlin
Text(stringResource(R.string.no_results, state.query.trim()))   // ✅
```

### K548 · `plurals` tiếng Việt: viết `quantity="one"` mà không bao giờ dùng tới

`🟡 đã làm app` · `Android`

**Gặp:** bản tiếng Việt luôn ra câu của nhánh `other`, kể cả khi số là 1; nhánh `one` vô dụng.

**Vì sao:** quy tắc số nhiều của tiếng Việt (CLDR) chỉ có loại `other`. Android chọn nhánh theo ngôn ngữ, không theo
"số có bằng 1 không".

**Sửa:** bản tiếng Việt chỉ cần `other`. Bản tiếng Anh vẫn cần `one` + `other`. Gọi bằng `pluralStringResource`
(Compose) hoặc `getQuantityString`, nhớ truyền số hai lần nếu chuỗi có `%d`.

```xml
<!-- values-vi/strings.xml -->
<plurals name="trips"><item quantity="other">%d chuyến đi</item></plurals>
```

### K549 · `Toast.makeText(context, count, …)` → `Resources$NotFoundException`

`🟢 mới học` · `Android`

**Gặp:** `android.content.res.Resources$NotFoundException: String resource ID #0x5`.

**Vì sao:** có bản `makeText(Context, Int, Int)` nhận **id tài nguyên**. Truyền một số `Int` bình thường thì Android đi
tìm chuỗi có id đó. `TextView.setText(Int)` cũng vậy.

**Sửa:** đổi số thành chuỗi, hoặc dùng chuỗi có placeholder.

```kotlin
Toast.makeText(context, count, Toast.LENGTH_SHORT).show()             // ❌ count bị hiểu là R.string.xxx
Toast.makeText(context, count.toString(), Toast.LENGTH_SHORT).show()  // ✅
```

### K550 · `uses-feature` làm app biến mất khỏi Play trên một số máy

`🟡 đã làm app` · `Android`

**Gặp:** app không hiện trên Google Play ở máy tính bảng / Chromebook; hoặc ngược lại hiện trên máy không có phần cứng
cần thiết rồi chạy không được.

**Vì sao:** `<uses-feature required="true">` khiến Play lọc máy không có phần cứng đó. Một số quyền (như vị trí, camera)
còn **ngầm** sinh `uses-feature` bắt buộc nếu bạn không khai báo rõ.

**Sửa:** khai báo rõ từng feature: bắt buộc khi app vô dụng nếu thiếu (OBD qua BLE), `required="false"` khi chỉ là
tính năng phụ, và kiểm lúc chạy bằng `packageManager.hasSystemFeature`.

```xml
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />   <!-- ObdLink -->
<uses-feature android:name="android.hardware.location.gps" android:required="false" />  <!-- TripLog -->
```

**Gặp ở:** [`apps/ObdLink/androidApp/src/main/AndroidManifest.xml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/androidApp/src/main/AndroidManifest.xml) · [`apps/TripLog/androidApp/src/main/AndroidManifest.xml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/androidApp/src/main/AndroidManifest.xml)

---

**Nhóm trước / sau:** [10. Swift ↔ Kotlin, iOS, Xcode](10-ios-interop.md) · [12. Compose: state & recomposition](12-compose-state.md) · [Mục lục 900 lỗi](index.md)
