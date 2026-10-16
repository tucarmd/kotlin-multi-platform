# 9. KMP: source set, expect/actual (K401–K450)

Lỗi riêng của project Kotlin Multiplatform: code đặt sai source set, `expect`/`actual` không khớp, target không build, phần Android
và iOS của module `shared` mỗi bên một kiểu. Lỗi Gradle chung xem [nhóm 8](08-gradle.md); lỗi khi Swift gọi Kotlin xem nhóm 10.

### K401 · `Unresolved reference 'Context'` trong `commonMain`

`🟢 mới học` · `KMP`

**Gặp:** `Unresolved reference 'Context'.` (hoặc `'android'`, `'Log'`, `'Build'`) khi compile `commonMain`, dù cùng dòng đó chạy được
trong `androidApp`.

**Vì sao:** `commonMain` được compile cho **mọi** target, kể cả iOS. Ở đó chỉ có stdlib Kotlin và thư viện multiplatform; không có
Android SDK, không có JDK.

**Sửa:** giữ `commonMain` thuần Kotlin. Phần cần API Android chuyển sang `androidMain`, nối với common bằng interface (khuyên dùng)
hoặc `expect`/`actual`.

```kotlin
// commonMain
fun saveNote(context: Context, note: Note)          // ❌ Context là của Android
interface NoteStore { fun save(note: Note) }        // ✅ common chỉ biết interface
// androidMain
class AndroidNoteStore(private val context: Context) : NoteStore { … }
```

Xem thêm: K423, [Chia sẻ code](../lessons/04-share-code.md)

### K402 · `System.currentTimeMillis()` không có trong `commonMain`

`🟢 mới học` · `KMP`

**Gặp:** `Unresolved reference 'System'.` trong `commonMain`.

**Vì sao:** `System` là class của JDK, chỉ có trên JVM/Android. Tương tự `java.util.Date`, `Calendar`, `LocalDateTime` của Java.

**Sửa:** dùng `kotlin.time.Clock` / `kotlin.time.Instant` của stdlib (Kotlin 2.4 không cần opt-in; bản cũ hơn cần
`@OptIn(ExperimentalTime::class)`). Ngày giờ theo múi giờ thì thêm `kotlinx-datetime`.

```kotlin
val ms = System.currentTimeMillis()                          // ❌ chỉ JVM
val ms = Clock.System.now().toEpochMilliseconds()            // ✅ common
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzQwMjogdGjhu51pIGdpYW4gdHJvbmcgY29tbW9uTWFpbjoga290bGluLnRpbWUuQ2xvY2sgdGhheSBjaG8gU3lzdGVtLmN1cnJlbnRUaW1lTWlsbGlzKCkuCi8vIFBsYXlncm91bmQgY2jhuqF5IEpWTSBuw6puIFN5c3RlbSB24bqrbiBjw7M7IHRyb25nIGNvbW1vbk1haW4gZMOybmcg4p2MIGLDoW8gVW5yZXNvbHZlZCByZWZlcmVuY2UgJ1N5c3RlbScuCmltcG9ydCBrb3RsaW4udGltZS5DbG9jawoKZnVuIG1haW4oKSB7CiAgICAvLyB2YWwgbXMgPSBTeXN0ZW0uY3VycmVudFRpbWVNaWxsaXMoKSAgICAgICAgICAvLyDinYwgY2jhu4kgSlZNCiAgICB2YWwgbm93ID0gQ2xvY2suU3lzdGVtLm5vdygpICAgICAgICAgICAgICAgICAgICAgLy8g4pyFIGNo4bqheSBj4bqjIEFuZHJvaWQgbOG6q24gaU9TIChLb3RsaW4gMi40IGtow7RuZyBj4bqnbiBvcHQtaW4pCiAgICBwcmludGxuKCJlcG9jaCBtcyA9ICR7bm93LnRvRXBvY2hNaWxsaXNlY29uZHMoKX0iKQogICAgcHJpbnRsbigiSVNPICAgICAgPSAkbm93IikKfQo)

### K403 · `String.format` báo `Unresolved reference 'format'`

`🟢 mới học` · `KMP`

**Gặp:** `"%.2f".format(x)` hoặc `String.format(...)` chạy trên Android, nhưng `commonMain` báo `Unresolved reference 'format'.`

**Vì sao:** `format` dùng `java.util.Formatter`, chỉ có trên JVM. Stdlib common không có hàm format kiểu printf.

**Sửa:** tự làm tròn và đệm số trong common (`roundToLong`, `padStart`), hoặc để UI mỗi nền tảng tự format theo locale
(`NumberFormat` trên Android, `NumberFormatter` trên iOS). Tiền thì giữ `Long` đơn vị nhỏ nhất rồi mới format.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzQwMzogU3RyaW5nLmZvcm1hdCBjaOG7iSBjw7MgdHLDqm4gSlZNLiBUcm9uZyBjb21tb25NYWluIHThu7EgbMOgbSB0csOybiB2w6AgxJHhu4dtIHPhu5EuCmltcG9ydCBrb3RsaW4ubWF0aC5hYnMKaW1wb3J0IGtvdGxpbi5tYXRoLnJvdW5kVG9Mb25nCgovKiogMiBjaOG7ryBz4buRIHRo4bqtcCBwaMOibiwga2jDtG5nIGPhuqduIFN0cmluZy5mb3JtYXQuICovCmZ1biBEb3VibGUuZm9ybWF0MigpOiBTdHJpbmcgewogICAgdmFsIGNlbnRzID0gKHRoaXMgKiAxMDApLnJvdW5kVG9Mb25nKCkKICAgIHZhbCBzaWduID0gaWYgKGNlbnRzIDwgMCkgIi0iIGVsc2UgIiIKICAgIHZhbCBhID0gYWJzKGNlbnRzKQogICAgcmV0dXJuICIkc2lnbiR7YSAvIDEwMH0uJHsoYSAlIDEwMCkudG9TdHJpbmcoKS5wYWRTdGFydCgyLCAnMCcpfSIKfQoKZnVuIG1haW4oKSB7CiAgICAvLyBwcmludGxuKCIlLjJmIi5mb3JtYXQoMy4xNDE1OSkpICAgLy8g4p2MIGNvbW1vbk1haW46IFVucmVzb2x2ZWQgcmVmZXJlbmNlICdmb3JtYXQnCiAgICBwcmludGxuKDMuMTQxNTkuZm9ybWF0MigpKSAgICAgICAgICAgLy8g4pyFIDMuMTQKICAgIHByaW50bG4oMi41LmZvcm1hdDIoKSkgICAgICAgICAgICAgICAvLyAyLjUwCiAgICBwcmludGxuKCgtMC4wNykuZm9ybWF0MigpKSAgICAgICAgICAgLy8gLTAuMDcKICAgIHByaW50bG4oNy50b1N0cmluZygpLnBhZFN0YXJ0KDMsICcwJykpICAvLyAwMDcKfQo)

### K404 · `synchronized`, `AtomicInteger`, `ConcurrentHashMap` trong `commonMain`

`🟡 đã làm app` · `KMP`

**Gặp:** `Unresolved reference 'synchronized'.` hoặc `Unresolved reference 'java'.` khi đưa code xử lý đa luồng lên `commonMain`.

**Vì sao:** đó là API của JDK. Kotlin/Native có cơ chế riêng; common không có `synchronized`.

**Sửa:** trong coroutine dùng `Mutex` (`kotlinx.coroutines.sync`) bọc phần ghi; biến đếm dùng `kotlin.concurrent.atomics.AtomicInt` /
`AtomicReference` của stdlib. Đơn giản nhất: chỉ cho một coroutine sửa state (hàng đợi lệnh qua `Channel`).

[▶ Chạy thử](../playground/index.html#code=Ly8gSzQwNDoga2jDtG5nIGPDsyBzeW5jaHJvbml6ZWQgLyBBdG9taWNJbnRlZ2VyIHRyb25nIGNvbW1vbk1haW4uIETDuW5nIE11dGV4IChjb3JvdXRpbmVzKSBob-G6t2MgYXRvbWljcyBj4bunYSBzdGRsaWIuCmltcG9ydCBrb3RsaW4uY29uY3VycmVudC5hdG9taWNzLkF0b21pY0ludAppbXBvcnQga290bGluLmNvbmN1cnJlbnQuYXRvbWljcy5pbmNyZW1lbnRBbmRGZXRjaAppbXBvcnQga290bGlueC5jb3JvdXRpbmVzLioKaW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy5zeW5jLk11dGV4CmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuc3luYy53aXRoTG9jawoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nPFVuaXQ-IHsKICAgIHZhbCBjb3VudGVyID0gQXRvbWljSW50KDApICAgICAgICAgICAgICAgICAvLyDinIUgY29tbW9uLCB0aGF5IGphdmEudXRpbC5jb25jdXJyZW50LmF0b21pYy5BdG9taWNJbnRlZ2VyCiAgICB2YWwgbXV0ZXggPSBNdXRleCgpICAgICAgICAgICAgICAgICAgICAgICAgLy8g4pyFIGNvbW1vbiwgdGhheSBzeW5jaHJvbml6ZWQobG9jaykgeyB9CiAgICB2YWwgbGlzdCA9IG11dGFibGVMaXN0T2Y8SW50PigpCiAgICB3aXRoQ29udGV4dChEaXNwYXRjaGVycy5EZWZhdWx0KSB7CiAgICAgICAgcmVwZWF0KDEwMDApIHsgaSAtPgogICAgICAgICAgICBsYXVuY2ggewogICAgICAgICAgICAgICAgY291bnRlci5pbmNyZW1lbnRBbmRGZXRjaCgpCiAgICAgICAgICAgICAgICBtdXRleC53aXRoTG9jayB7IGxpc3QgKz0gaSB9CiAgICAgICAgICAgIH0KICAgICAgICB9CiAgICB9CiAgICBwcmludGxuKCJjb3VudGVyID0gJHtjb3VudGVyLmxvYWQoKX0sIGxpc3Quc2l6ZSA9ICR7bGlzdC5zaXplfSIpCn0K)

### K405 · `Dispatchers.IO` báo `Unresolved reference 'IO'` trong `commonMain`

`🟡 đã làm app` · `KMP`

**Gặp:** `Unresolved reference 'IO'.` ở `Dispatchers.IO`, trong khi `Dispatchers.Default` và `Dispatchers.Main` thì được.

**Vì sao:** trong code common, `Dispatchers.IO` là **extension property** của kotlinx.coroutines (có cho JVM và Native, không có cho JS/Wasm),
nên phải import riêng. Nếu module có target web thì nó không có trong `commonMain`.

**Sửa:** thêm `import kotlinx.coroutines.IO`. Có target JS/Wasm thì dùng `Dispatchers.Default`, hoặc inject dispatcher từ từng nền tảng.

```kotlin
import kotlinx.coroutines.IO      // ✅
withContext(Dispatchers.IO) { db.load() }
```

Xem thêm: [Dispatcher](../concepts/dispatchers.md)

### K406 · `java.util.UUID` trong `commonMain`

`🟢 mới học` · `KMP`

**Gặp:** `Unresolved reference 'java'.` ở dòng `UUID.randomUUID()`.

**Vì sao:** `java.*` không tồn tại ngoài JVM.

**Sửa:** `kotlin.uuid.Uuid.random()` của stdlib (Kotlin 2.4 không cần opt-in; bản cũ cần `@OptIn(ExperimentalUuidApi::class)`).
Tương tự: `java.util.Random` → `kotlin.random.Random`, `Math.max` → `kotlin.math.max`.

[▶ Chạy thử](../playground/index.html#code=Ly8gSzQ0ODogamF2YS51dGlsLlVVSUQgY2jhu4kgY8OzIHRyw6puIEpWTS4ga290bGluLnV1aWQuVXVpZCBjaOG6oXkg4bufIGNvbW1vbk1haW4uCmltcG9ydCBrb3RsaW4udXVpZC5VdWlkCgpmdW4gbWFpbigpIHsKICAgIC8vIHZhbCBpZCA9IGphdmEudXRpbC5VVUlELnJhbmRvbVVVSUQoKS50b1N0cmluZygpICAgLy8g4p2MIGNvbW1vbk1haW46IFVucmVzb2x2ZWQgcmVmZXJlbmNlICdqYXZhJwogICAgdmFsIGlkID0gVXVpZC5yYW5kb20oKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4pyFIChLb3RsaW4gMi40IGtow7RuZyBj4bqnbiBvcHQtaW4pCiAgICBwcmludGxuKGlkKQogICAgcHJpbnRsbihVdWlkLnBhcnNlKCI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiKSkKfQo)

### K407 · Đọc/ghi file bằng `java.io.File` trong `commonMain`

`🟡 đã làm app` · `KMP`

**Gặp:** `Unresolved reference 'File'.` khi viết hàm xuất GPX / log ra file trong code chung.

**Vì sao:** `java.io` chỉ có trên JVM. Chỗ lưu file cũng khác nhau: Android có `context.filesDir`, iOS có thư mục Documents của sandbox.

**Sửa:** phần **tạo nội dung** (chuỗi GPX, JSON) để ở common, trả về `String` / `ByteArray`. Phần **ghi ra đâu** để ở `androidMain` /
`iosMain` (hoặc dùng `kotlinx-io` với đường dẫn do nền tảng truyền vào).

### K408 · Thêm thư viện chỉ có JVM (OkHttp, Gson, Retrofit) vào `commonMain`

`🟢 mới học` · `KMP` · `Gradle`

**Gặp:** build Android được, build iOS lỗi khi resolve dependency (`Could not resolve …`, `No matching variant of … was found`), hoặc
`Unresolved reference` khi compile metadata của `commonMain`.

**Vì sao:** thư viện JVM chỉ có file `.jar`/`.aar`, không có bản cho Kotlin/Native. `commonMain` chỉ nhận thư viện **multiplatform**
có bản cho mọi target bạn khai báo.

**Sửa:** dùng bản multiplatform: Ktor thay Retrofit/OkHttp, kotlinx.serialization thay Gson, SQLDelight/Room KMP thay Room cũ. Thư viện
chỉ Android thì đặt ở `androidMain.dependencies`.

Xem thêm: K409, K450, K370

### K409 · Ktor: engine OkHttp đặt trong `commonMain`

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** `ktor-client-okhttp` trong `commonMain` làm build iOS lỗi; hoặc không khai báo engine nào thì `HttpClient()` không tìm thấy
engine trên một nền tảng.

**Vì sao:** `ktor-client-core` là common, còn **engine** là của từng nền tảng: OkHttp (JVM/Android), Darwin (iOS).

**Sửa:** core ở `commonMain`, engine ở source set của nền tảng. Tạo client ở common với engine truyền vào, mỗi nền tảng đưa engine của mình.

```kotlin
commonMain.dependencies { implementation(libs.ktor.client.core) }
androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
iosMain.dependencies { implementation(libs.ktor.client.darwin) }
```

**Gặp ở:** [`DevicePulse/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/DevicePulse/shared/build.gradle.kts)

### K410 · Thêm thư viện vào `androidMain` rồi gọi từ `commonMain`

`🟢 mới học` · `KMP` · `Gradle`

**Gặp:** đã thêm dependency mà `commonMain` vẫn `Unresolved reference`; trong `androidMain` thì gọi được.

**Vì sao:** dependency khai báo trong source set nào thì chỉ source set đó (và các source set con của nó) thấy. `commonMain` là "cha",
không thấy thư viện của "con".

**Sửa:** thư viện multiplatform dùng ở code chung → `commonMain.dependencies { }`. Còn nếu thư viện chỉ có cho Android thì code dùng nó
phải nằm ở `androidMain` (K401).

### K411 · File `.kt` đặt sai thư mục, class "biến mất"

`🟢 mới học` · `KMP`

**Gặp:** viết class xong, nơi khác báo `Unresolved reference`; IDE hiện file như file thường, không tô màu Kotlin.

**Vì sao:** source set đọc code ở `src/<tên source set>/kotlin/`. File nằm ở `src/commonMain/` (thiếu `kotlin/`), `src/main/kotlin/`
(thói quen module Android thường) hay `src/IosMain/` (sai hoa thường) đều không được compile.

**Sửa:** đặt đúng `src/commonMain/kotlin/<package>/`, `src/androidMain/kotlin/…`, `src/iosMain/kotlin/…`.

```text
shared/src/commonMain/MyRules.kt                     ❌ không được compile
shared/src/commonMain/kotlin/dev/app/MyRules.kt      ✅
```

Xem thêm: [Cấu trúc project](../lessons/03-project-structure.md)

### K412 · "The Default Kotlin Hierarchy Template was not applied"

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** cảnh báo khi sync, dạng `The Default Kotlin Hierarchy Template was not applied to 'project ':shared'': Explicit .dependsOn() edges were configured for the following source sets: [iosMain]`

**Vì sao:** từ Kotlin 1.9.20, KMP tự dựng cây `commonMain → … → iosMain → iosArm64Main` theo **default hierarchy template**. Script
copy từ hướng dẫn cũ còn tự `dependsOn(...)`, nên plugin bỏ template, và các source set trung gian khác (`appleMain`, `nativeMain`)
không được tạo.

**Sửa:** xoá các khối tự tạo `iosMain` + `dependsOn`. Chỉ khai báo target, rồi dùng thẳng `iosMain.dependencies { }`.

```kotlin
val iosMain by creating {                       // ❌ kiểu cũ
    dependsOn(commonMain)
    iosArm64Main.dependsOn(this)
}
iosMain.dependencies { … }                      // ✅ template tự tạo iosMain
```

### K413 · "KotlinSourceSet with name 'iosMain' not found"

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** `KotlinSourceSet with name 'iosMain' not found.` ngay khi sync.

**Vì sao:** `val iosMain by getting` tìm source set **đã có**. Source set iOS chỉ được tạo khi có target iOS. Lỗi xảy ra khi khối
`sourceSets` đứng trước phần khai báo target, gõ sai tên, hoặc tạm xoá target iOS.

**Sửa:** khai báo target trước. Với Kotlin 2.x dùng accessor có sẵn (`iosMain.dependencies { }`) thay vì `by getting`.

### K414 · Viết code iOS hai lần cho `iosArm64Main` và `iosSimulatorArm64Main`

`🟢 mới học` · `KMP`

**Gặp:** cùng một file copy vào hai thư mục; sửa một bên quên bên kia, máy thật và simulator chạy khác nhau.

**Vì sao:** hai target này chỉ khác kiến trúc chip, cùng API Apple. Default hierarchy template đã có `iosMain` dùng chung cho cả hai.

**Sửa:** đặt code ở `src/iosMain/kotlin`. Chỉ dùng source set theo từng target khi thật sự khác nhau (hầu như không bao giờ).

Xem thêm: K412, [Cấu trúc project](../lessons/03-project-structure.md)

### K415 · `import platform.UIKit…` báo `Unresolved reference 'platform'`

`🟢 mới học` · `KMP` · `iOS`

**Gặp:** `Unresolved reference 'platform'.` ở `import platform.UIKit.UIDevice` hoặc `platform.Foundation.NSDate`.

**Vì sao:** các gói `platform.*` (Foundation, UIKit, CoreLocation…) là platform library của Kotlin/Native cho Apple. Chúng chỉ có trong
`iosMain` (và `appleMain`, các source set Apple), không có ở `commonMain`.

**Sửa:** chuyển file sang `src/iosMain/kotlin`. Trên Windows/Linux các gói này cũng không có (K430).

### K416 · "This declaration needs opt-in" khi gọi API C/Objective-C

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** `This declaration needs opt-in. Its usage must be marked with '@kotlinx.cinterop.ExperimentalForeignApi' or '@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)'`
khi dùng `memScoped`, `alloc`, `CPointer`, `useContents` hoặc nhiều hàm của `platform.*`.

**Vì sao:** API interop với C của Kotlin/Native chưa ổn định, nên bắt opt-in.

**Sửa:** `@OptIn(ExperimentalForeignApi::class)` ở hàm/file dùng nó. Dùng nhiều thì bật cho riêng các source set Apple:

```kotlin
sourceSets.iosMain {
    languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
}
```

### K417 · `expect` thiếu `actual` cho một nền tảng

`🟢 mới học` · `KMP`

**Gặp:** build iOS (hoặc Android) báo kiểu `Expected function 'platformName' has no actual declaration in module … for Native`;
IDE gạch đỏ chữ `expect`.

**Vì sao:** mỗi `expect` trong `commonMain` phải có `actual` ở **mọi** target được build. Thường là quên tạo file ở `iosMain`, hoặc tạo
nhầm ở `iosArm64Main` nên `iosSimulatorArm64` không có.

**Sửa:** tạo `actual` ở `androidMain` và `iosMain` (cùng package, cùng chữ ký). IDE có quick-fix (Alt+Enter trên `expect`) tạo sẵn `actual` còn thiếu.

```kotlin
// commonMain
expect fun platformName(): String
// androidMain
actual fun platformName(): String = "Android ${Build.VERSION.SDK_INT}"
// iosMain
actual fun platformName(): String = UIDevice.currentDevice.systemName()
```

Xem thêm: K418, K425, [Chia sẻ code](../lessons/04-share-code.md)

### K418 · `actual` không tìm thấy `expect` tương ứng

`🟢 mới học` · `KMP`

**Gặp:** lỗi kiểu `… has no corresponding expected declaration` ở dòng `actual`, dù `expect` nằm ngay trong `commonMain`.

**Vì sao:** `expect` và `actual` phải khớp **tên đầy đủ**: cùng package, cùng tên, cùng tham số và kiểu trả về. Hay sai nhất là
package: file `actual` nằm thư mục khác và khai báo `package` khác.

**Sửa:** copy dòng `package` và chữ ký từ file `expect` sang. Đặt tên file `Platform.android.kt`, `Platform.ios.kt` cạnh
`Platform.kt` cho dễ tìm.

### K419 · "Declaration must be marked with 'actual'"

`🟢 mới học` · `KMP`

**Gặp:** `Declaration must be marked with 'actual'` ở hàm trong `androidMain` / `iosMain`.

**Vì sao:** bạn viết đúng chữ ký của một `expect` nhưng quên từ khoá `actual`. Với `actual class`, **mọi** thành viên ứng với thành viên
của `expect class` cũng phải có `actual`.

**Sửa:** thêm `actual` vào khai báo (và vào từng thành viên, constructor của class).

### K420 · "Expected declaration must not have a body"

`🟢 mới học` · `KMP`

**Gặp:** `Expected declaration must not have a body.`

**Vì sao:** `expect` chỉ là lời hứa, không có thân. Thân hàm nằm ở các `actual`.

**Sửa:** bỏ thân khỏi `expect`. Nếu phần lớn logic giống nhau, để logic chung trong một hàm thường ở common và chỉ `expect` đúng mẩu
nhỏ khác nhau.

```kotlin
expect fun platformName(): String = "?"     // ❌
expect fun platformName(): String           // ✅
```

### K421 · Tham số mặc định đặt ở `actual`

`🟡 đã làm app` · `KMP`

**Gặp:** `Actual function cannot have default argument values, they should be declared in the expected function.`

**Vì sao:** giá trị mặc định là một phần của "lời hứa" mà code common nhìn thấy, nên chỉ được khai báo ở `expect`.

**Sửa:** chuyển `= …` sang `expect`, xoá ở `actual`.

```kotlin
expect fun log(msg: String, tag: String = "App")       // ✅
actual fun log(msg: String, tag: String) { … }         // ✅ không lặp lại default
```

### K422 · `expect class` báo cảnh báo Beta, test khó

`🟡 đã làm app` · `KMP`

**Gặp:** cảnh báo dạng `'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta.`
kèm gợi ý cờ `-Xexpect-actual-classes`; và trong `commonTest` không có cách thay class đó bằng bản giả.

**Vì sao:** `expect class` buộc mỗi nền tảng có **đúng một** cài đặt, chọn lúc compile. Test chung chạy với bản thật của nền tảng.

**Sửa:** với "dịch vụ" của nền tảng (cảm biến, lưu trữ, mạng) dùng **interface** ở common + class cài đặt ở từng nền tảng + DI (Koin).
Test dùng bản giả. Chỉ giữ `expect` cho thứ nhỏ, không cần giả (`platformName()`, một `expect val` module DI).

Xem thêm: [Sao không dùng `expect class`](../lessons/04-share-code.md)

### K423 · Muốn `expect fun` nhận `Context` của Android

`🟡 đã làm app` · `KMP` · `Android`

**Gặp:** `expect fun createDriver(context: Context)` không viết được ở `commonMain` (K401); hoặc `actual` Android cần `Context` mà
`expect` không có tham số đó.

**Vì sao:** chữ ký `expect` phải viết bằng kiểu common, nên không thể nhắc tới `Context`. iOS cũng không cần nó.

**Sửa:** đừng đưa `Context` qua common. Tạo object cần `Context` ngay trong `androidMain`, lấy `Context` từ DI (`androidContext()` của
Koin), rồi chỉ đưa ra common phần kết quả (`SqlDriver`, `HttpClient`).

```kotlin
// androidMain
actual val platformModule: Module = module {
    single<SqlDriver> { AndroidSqliteDriver(NotesDb.Schema, androidContext(), "notes.db") }
}
```

**Gặp ở:** [`PocketNotes/shared/src/androidMain/kotlin/dev/pocketnotes/di/PlatformModule.android.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/androidMain/kotlin/dev/pocketnotes/di/PlatformModule.android.kt)

### K424 · Mỗi chỗ khác nhau lại thêm một `expect`

`🟡 đã làm app` · `KMP`

**Gặp:** `commonMain` có chục `expect fun` rải rác; mỗi lần thêm nền tảng (desktop) phải viết lại chục `actual`.

**Vì sao:** `expect`/`actual` là cơ chế cấp ngôn ngữ, mỗi cái là một điểm nối riêng. Rải nhiều thì khó thấy toàn cảnh "nền tảng cung
cấp gì".

**Sửa:** gom về **một** điểm nối: `expect val platformModule: Module` (Koin), mỗi nền tảng khai báo mọi cài đặt của mình trong đó;
code common chỉ xin interface qua DI.

**Gặp ở:** [`TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/di/Koin.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/di/Koin.kt)
(cùng một mẫu trong cả 6 app)

### K425 · Thêm target mới, mọi `expect` đòi `actual` mới

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** thêm `jvm()` (desktop) hoặc `wasmJs()` vào `kotlin { }`, build nổ hàng loạt lỗi "has no actual declaration".

**Vì sao:** `expect` ở `commonMain` hứa với **mọi** target. Target mới chưa có `actual` nào. Thư viện cũng phải có bản cho target mới (K450).

**Sửa:** viết `actual` cho target mới (hoặc cho source set trung gian phù hợp). Chưa sẵn sàng thì đừng thêm target vào module đó.

Xem thêm: K417

### K426 · `expect` trong `commonTest` thiếu `actual` ở một bên test

`🟡 đã làm app` · `KMP`

**Gặp:** `iosSimulatorArm64Test` (hoặc `testDebugUnitTest`) lỗi compile "has no actual declaration", dù test Android chạy được.

**Vì sao:** `expect`/`actual` dùng được cả trong source set test, nhưng cũng phải đủ cặp: `expect` ở `commonTest` cần `actual` ở
`androidUnitTest` **và** `iosTest`. Ví dụ hay gặp: hàm tạo driver SQLite trong RAM cho test.

**Sửa:** thêm file `actual` ở source set test còn thiếu.

```kotlin
// commonTest
expect fun inMemoryDriver(): SqlDriver
// androidUnitTest
actual fun inMemoryDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { Db.Schema.create(it) }
// iosTest
actual fun inMemoryDriver(): SqlDriver = inMemoryDriver(Db.Schema)
```

### K427 · "Duplicate JVM class name" khi `commonMain` và `androidMain` có file cùng tên

`🔴 khó, hiếm nhưng đau` · `KMP` · `Android`

**Gặp:** build Android báo `Duplicate JVM class name 'dev/app/PlatformKt' …`; iOS build bình thường.

**Vì sao:** hàm top-level trong `Platform.kt` được gom vào class JVM `PlatformKt`. `commonMain/…/Platform.kt` và
`androidMain/…/Platform.kt` cùng package thì sinh ra hai class `PlatformKt` trong cùng một lần compile Android.

**Sửa:** đặt tên file theo nền tảng: `Platform.kt` (common), `Platform.android.kt`, `Platform.ios.kt`. Hoặc thêm
`@file:JvmName("PlatformAndroid")` ở file Android.

### K428 · "Platform declaration clash": iOS build được, Android thì không

`🔴 khó, hiếm nhưng đau` · `KMP` · `Kotlin`

**Gặp:** `Platform declaration clash: The following declarations have the same JVM signature (decode(Ljava/util/List;)I)`

**Vì sao:** trên JVM, kiểu generic bị xoá khi chạy: `List<Int>` và `List<List<Int>>` đều thành `java.util.List`. Hai hàm cùng tên chỉ khác
kiểu phần tử của `List` thì trùng chữ ký JVM. Kotlin/Native không bị nên chỉ Android lỗi.

**Sửa:** đặt hai tên khác nhau (cách repo làm), hoặc `@JvmName` (K429).

**Gặp ở:** [`ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/rules/Pid.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/rules/Pid.kt) (`decodePidAnswer`) ·
[▶ Chạy thử](../playground/index.html#code=Ly8gSzQyOTogaGFpIGjDoG0ga2jDoWMga2nhu4N1IExpc3Q8Li4uPiBjaOG6oXkgxJHGsOG7o2MgdHLDqm4gaU9TIG5oxrBuZyB0csOqbiBKVk0vQW5kcm9pZCB0csO5bmcgY2jhu68ga8O9LgpmdW4gZGVjb2RlKGJ5dGVzOiBMaXN0PEludD4pOiBJbnQgPSBieXRlcy5zdW0oKQpmdW4gZGVjb2RlKG1lc3NhZ2VzOiBMaXN0PExpc3Q8SW50Pj4pOiBJbnQgPSBtZXNzYWdlcy5zdW1PZiB7IGl0LnN1bSgpIH0gICAvLyDinYwgUGxhdGZvcm0gZGVjbGFyYXRpb24gY2xhc2gKCmZ1biBtYWluKCkgewogICAgcHJpbnRsbihkZWNvZGUobGlzdE9mKDEsIDIpKSkKfQo) · Xem thêm: K429

### K429 · Không biết `@JvmName` dùng được trong `commonMain`

`🟡 đã làm app` · `KMP`

**Gặp:** muốn giữ hai overload ở K428 nhưng tưởng annotation `kotlin.jvm.*` không đặt được ở code chung.

**Vì sao:** `@JvmName`, `@JvmStatic`, `@JvmField`, `@JvmOverloads` là annotation "tuỳ chọn" trong stdlib common: target JVM dùng chúng,
target khác bỏ qua.

**Sửa:**

```kotlin
fun decode(bytes: List<Int>): Int = …
@JvmName("decodeMessages")                       // ✅ chỉ đổi tên trên JVM
fun decode(messages: List<List<Int>>): Int = …
```

[▶ Chạy thử](../playground/index.html#code=Ly8gSzQzMDogQEp2bU5hbWUgZMO5bmcgxJHGsOG7o2MgbmdheSB0cm9uZyBjb21tb25NYWluOiDEkeG6t3QgdMOqbiBKVk0ga2jDoWMgY2hvIG3hu5l0IG92ZXJsb2FkLgpmdW4gZGVjb2RlKGJ5dGVzOiBMaXN0PEludD4pOiBJbnQgPSBieXRlcy5zdW0oKQoKQEp2bU5hbWUoImRlY29kZU1lc3NhZ2VzIikgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinIUgY2jhu4kgxJHhu5VpIHTDqm4gdHLDqm4gSlZNCmZ1biBkZWNvZGUobWVzc2FnZXM6IExpc3Q8TGlzdDxJbnQ-Pik6IEludCA9IG1lc3NhZ2VzLnN1bU9mIHsgaXQuc3VtKCkgfQoKZnVuIG1haW4oKSB7CiAgICBwcmludGxuKGRlY29kZShsaXN0T2YoMSwgMikpKQogICAgcHJpbnRsbihkZWNvZGUobGlzdE9mKGxpc3RPZigxLCAyKSwgbGlzdE9mKDMpKSkpCn0K)

### K430 · Target iOS bị tắt khi build trên Windows / Linux

`🟢 mới học` · `KMP` · `iOS`

**Gặp:** cảnh báo dạng `The following Kotlin/Native targets cannot be built on this machine and are disabled: iosArm64, iosSimulatorArm64`;
task `link…Ios…` không có.

**Vì sao:** link framework iOS cần Xcode, chỉ có trên macOS. Trên máy khác, Gradle vẫn build phần Android và bỏ qua iOS.

**Sửa:** build iOS trên Mac (hoặc CI macOS). Không cần iOS trên máy đó thì ẩn cảnh báo bằng `kotlin.native.ignoreDisabledTargets=true`
trong `gradle.properties`.

### K431 · Xcode: "Requested Architecture Not Configured in Gradle"

`🟡 đã làm app` · `KMP` · `iOS`

**Gặp:** build bằng `xcodebuild -destination 'generic/platform=iOS Simulator'` (hoặc "Any iOS Simulator Device") lỗi
`Xcode Requested Architecture Not Configured in Gradle`.

**Vì sao:** "generic simulator" đòi cả `arm64` lẫn `x86_64`, nhưng `shared/` chỉ khai báo `iosSimulatorArm64()`. Gradle không có
framework x86_64 để đưa.

**Sửa:** cho app iOS bỏ x86_64 khi build simulator, hoặc chọn một simulator cụ thể.

```yaml
# iosApp/project.yml (XcodeGen)
EXCLUDED_ARCHS[sdk=iphonesimulator*]: x86_64
```

**Gặp ở:** [`PocketNotes/iosApp/project.yml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/iosApp/project.yml)
(và cả 5 app còn lại) · Xem thêm: K433

### K432 · "building for 'iOS-simulator', but linking in object file built for 'iOS'"

`🔴 khó, hiếm nhưng đau` · `iOS` · `KMP`

**Gặp:** Xcode báo lỗi link kiểu `ld: building for 'iOS-simulator', but linking in object file (…/Shared.framework/Shared) built for 'iOS'`.

**Vì sao:** Xcode đang dùng framework của `iosArm64` (máy thật) để build cho simulator. Thường do tự trỏ Framework Search Paths tới một
thư mục build cố định thay vì để task Gradle chọn đúng target.

**Sửa:** để build phase gọi `./gradlew :shared:embedAndSignAppleFrameworkForXcode` (task tự chọn target theo `SDK_NAME`/`ARCHS` của
Xcode), xoá đường dẫn framework cứng. Clean build folder rồi build lại.

### K433 · Mac Intel: simulator không chạy được framework

`🟡 đã làm app` · `KMP` · `iOS`

**Gặp:** trên Mac chip Intel, build simulator báo thiếu kiến trúc `x86_64`, trong khi máy Apple Silicon cùng project chạy tốt.

**Vì sao:** simulator trên Mac Intel là `x86_64`, ứng với target `iosX64()`. Project chỉ có `iosArm64()` và `iosSimulatorArm64()`.

**Sửa:** thêm `iosX64()` vào danh sách target nếu nhóm còn dùng Mac Intel (build lâu hơn), hoặc chạy trên máy thật.

```kotlin
listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "Shared" } }
```

### K434 · Chỉ build Android, lỗi iOS lộ ra muộn

`🟢 mới học` · `KMP` · `Gradle`

**Gặp:** cả tuần build Android xanh; lúc mở Xcode mới thấy hàng loạt lỗi compile trong `iosMain` hoặc thiếu `actual`.

**Vì sao:** chạy app Android chỉ compile `commonMain` + `androidMain`. Code `iosMain` và phần `actual` cho iOS không được kiểm.

**Sửa:** trước khi commit, compile thêm target iOS (nhanh hơn link framework):

```bash
./gradlew :shared:compileKotlinIosSimulatorArm64
```

Xem thêm: K398, K443

### K435 · Swift thấy type của module con với tiền tố lạ (`Core_obdDtc`)

`🟡 đã làm app` · `KMP` · `iOS`

**Gặp:** `shared` phụ thuộc `:core:obd`; Swift thấy `Core_obdDtc` thay vì `Dtc`, và không gọi được hàm của module đó.

**Vì sao:** framework chỉ **xuất** API của chính `shared`. Type của module khác lọt vào chỉ vì xuất hiện trong chữ ký, nên được đặt tên kèm
tiền tố module.

**Sửa:** `export(project(":core:obd"))` trong khối `framework { }`, kèm `api(project(":core:obd"))` (K436).

```kotlin
iosTarget.binaries.framework {
    baseName = "Shared"
    export(project(":core:obd"))      // ✅
}
```

**Gặp ở:** [`ObdLink/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/shared/build.gradle.kts) ·
Xem thêm: K368

### K436 · `export()` báo dependency chưa phải `api`

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** `Following dependencies exported in the debugFramework binary are not specified as API-dependencies of a corresponding source set: …`

**Vì sao:** chỉ xuất được vào framework những module là một phần API của `shared`, tức là khai báo bằng `api(...)`.

**Sửa:** đổi `implementation(project(":core:obd"))` thành `api(project(":core:obd"))` trong `commonMain.dependencies`.

### K437 · AGP 9: cấu hình Android của module `shared` chuyển vào `kotlin { }`

`🔴 khó, hiếm nhưng đau` · `KMP` · `Gradle`

**Gặp:** sau khi đổi sang plugin `com.android.kotlin.multiplatform.library`, khối `android { namespace … }` cũ và `androidTarget { }`
báo lỗi hoặc không có tác dụng.

**Vì sao:** plugin Android dành riêng cho KMP đặt cấu hình Android **bên trong** `kotlin { }` (khối `android { }`; bản đầu của plugin
gọi là `androidLibrary { }`). Không còn `defaultConfig`, không có build type/flavor.

**Sửa:** chuyển `namespace`, `compileSdk`, `minSdk` vào trong khối mới; `jvmTarget` vào `compilerOptions` của nó.

```kotlin
kotlin {
    android {                               // thay cho androidTarget { } + android { } ở ngoài
        namespace = "dev.app.shared"
        compileSdk = 36
        minSdk = 24
    }
}
```

Xem thêm: K365, K438, K439, K440

### K438 · Plugin KMP library: `Unresolved reference 'BuildConfig'` trong `androidMain`

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** sau khi chuyển module `shared` sang `com.android.kotlin.multiplatform.library`, `BuildConfig.DEBUG` trong `androidMain` báo
`Unresolved reference 'BuildConfig'.`

**Vì sao:** plugin này không sinh `BuildConfig` cho module thư viện (chỉ có một variant, không có debug/release).

**Sửa:** để app quyết định: app truyền cờ debug / URL vào `shared` qua DI hoặc tham số `initKoin(...)`. Cần hằng số lúc compile cho
mọi nền tảng thì dùng plugin sinh code như BuildKonfig.

Xem thêm: K395, K437

### K439 · Plugin KMP library: `R.string…` của module `shared` không có

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** file trong `src/androidMain/res/` không được đóng gói; `R.string.x` của module báo unresolved, không có lỗi nào khác.

**Vì sao:** với `com.android.kotlin.multiplatform.library`, Android resources **tắt mặc định**.

**Sửa:** bật trong khối Android của `kotlin { }`. (Compose Multiplatform resources ở `composeResources/` là chuyện khác, không cần cờ này.)

```kotlin
kotlin { android { androidResources { enable = true } } }
```

### K440 · Plugin KMP library: test Android của `shared` không chạy

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** test trong `androidUnitTest` / `androidHostTest` không bao giờ chạy, task test Android không có, build vẫn xanh.

**Vì sao:** plugin này không bật host test (unit test JVM) và device test mặc định. Source set đổi tên: `androidHostTest`
(tên cũ `androidUnitTest` vẫn nhận) và `androidDeviceTest`.

**Sửa:**

```kotlin
kotlin {
    android {
        withHostTest { }      // unit test chạy trên JVM của máy
    }
}
```

Xem thêm: K441

### K441 · Test JVM của `shared` đặt sai thư mục nên không chạy

`🟢 mới học` · `KMP`

**Gặp:** viết test ở `src/test/kotlin` hay `src/androidTest/kotlin` theo thói quen module Android; `./gradlew :shared:testDebugUnitTest`
xanh nhưng không chạy test nào của bạn.

**Vì sao:** module KMP dùng tên source set của KMP: test chung ở `commonTest`, test JVM của Android ở `androidUnitTest`, test trên
máy/emulator ở `androidInstrumentedTest`, test iOS ở `iosTest`.

**Sửa:** để test không cần API Android ở `src/commonTest/kotlin` (chạy cả hai nền tảng). Test cần JVM / driver JDBC ở
`src/androidUnitTest/kotlin`.

### K442 · `import org.junit.Test` trong `commonTest`

`🟢 mới học` · `KMP`

**Gặp:** `Unresolved reference 'junit'.` trong `commonTest`.

**Vì sao:** JUnit là thư viện JVM. Test chung phải chạy được trên iOS.

**Sửa:** `kotlin("test")` trong `commonTest.dependencies`, rồi dùng `kotlin.test.Test`, `assertEquals`… Gradle tự nối với JUnit trên
JVM và trình chạy test của Kotlin/Native trên iOS.

```kotlin
import org.junit.Test          // ❌
import kotlin.test.Test        // ✅
import kotlin.test.assertEquals
```

### K443 · Test chỉ chạy trên JVM, quên chạy trên iOS

`🟡 đã làm app` · `KMP`

**Gặp:** test chung xanh trên JVM; CI build iOS mới đỏ, ví dụ `Name contains illegal characters` vì tên test có dấu phẩy.

**Vì sao:** Kotlin/Native có quy định khác (tên hàm, số học, regex, định dạng số) và thư viện có bản Native riêng. Test JVM xanh chưa
chắc iOS xanh.

**Sửa:** chạy cả hai.

```bash
./gradlew :shared:testDebugUnitTest :shared:iosSimulatorArm64Test
./gradlew :shared:allTests          # mọi target
```

**Gặp ở:** [`TipSplit/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TipSplit/README.md) (mục "Mấy cái bẫy") ·
Xem thêm: K434

### K444 · Android: "Module with the Main dispatcher is missing"

`🟡 đã làm app` · `KMP` · `Android`

**Gặp:** `IllegalStateException: Module with the Main dispatcher is missing. Add dependency providing the Main dispatcher, e.g. 'kotlinx-coroutines-android' …`
khi code của `shared` dùng `Dispatchers.Main` (hoặc `MainScope()`) trên Android; thường gặp trong unit test JVM.

**Vì sao:** trên JVM, `Dispatchers.Main` do một module riêng cung cấp (`kotlinx-coroutines-android`). iOS có sẵn main queue trong
`kotlinx-coroutines-core`.

**Sửa:** thêm `kotlinx-coroutines-android` vào `androidMain`. Trong unit test JVM thì `Dispatchers.setMain(StandardTestDispatcher())`
(từ `kotlinx-coroutines-test`), hoặc inject dispatcher vào core thay vì gọi `Dispatchers.Main` trực tiếp.

### K445 · Bắt `java.net.UnknownHostException` trong code chung

`🟡 đã làm app` · `KMP`

**Gặp:** `Unresolved reference 'java'.` ở `catch`; hoặc bắt kiểu của OkHttp trong `androidMain` mà iOS mất mạng thì app hiện "Lỗi không rõ".

**Vì sao:** mỗi engine ném exception của nền tảng mình: OkHttp ném `UnknownHostException` / `ConnectException`, Darwin ném
`DarwinHttpRequestException`. Cả hai đều là `IOException` của `kotlinx.io`.

**Sửa:** trong common bắt `kotlinx.io.IOException` (và các exception timeout của Ktor), ném lại `CancellationException`.

```kotlin
} catch (e: CancellationException) { throw e
} catch (e: IOException) {                     // kotlinx.io.IOException: cả OkHttp lẫn Darwin
    throw RateApiException(RefreshError.Offline, e.message, e)
}
```

**Gặp ở:** [`RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt)

### K446 · Cùng một field, hai nền tảng cho giá trị khác bản chất

`🔴 khó, hiếm nhưng đau` · `KMP` · `iOS` · `Android`

**Gặp:** lưu "dongle quen" theo id: Android nhận lại được, iOS lúc được lúc không; so id giữa hai máy không bao giờ khớp.

**Vì sao:** `id` của thiết bị BLE là **địa chỉ MAC** trên Android, nhưng là **UUID ngẫu nhiên riêng của từng iPhone** trên iOS (Apple
giấu MAC). Model chung chỉ thấy một `String` nên không ai để ý.

**Sửa:** ghi rõ ý nghĩa trong KDoc của model chung (comment `PLATFORM:`), và không xây logic dựa trên giả định của một nền tảng.

**Gặp ở:** [`ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/model/Dongle.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/model/Dongle.kt)

### K447 · Giá trị "không hợp lệ" mỗi nền tảng biểu diễn một kiểu

`🔴 khó, hiếm nhưng đau` · `KMP` · `iOS` · `Android`

**Gặp:** trên iOS, điểm GPS tệ nhất lại được coi là chính xác nhất; bộ lọc ở common nhận hết.

**Vì sao:** iOS báo `horizontalAccuracy` **âm** khi vị trí không hợp lệ; Android dùng `hasAccuracy() == false`. Đưa thẳng số âm vào code
chung thì "âm" trông như "sai số nhỏ hơn 0 m", tức là hoàn hảo.

**Sửa:** chuẩn hoá ở code nền tảng trước khi vào common (ví dụ đổi "không hợp lệ" thành một số rất lớn hoặc `null`), để luật chung
chỉ thấy một kiểu.

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/rules/Geo.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/rules/Geo.kt) ·
[`IosLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt)

### K448 · Code mạng trong `shared` chạy trên iOS, Android thì không

`🟢 mới học` · `KMP` · `Android`

**Gặp:** cùng một `RateApi` trong `commonMain`; iOS tải được, Android mọi request ném `SecurityException` và app chỉ hiện "Đang offline".

**Vì sao:** Android cần quyền `android.permission.INTERNET` trong Manifest; iOS gọi HTTPS không cần khai báo gì. Code chung giống nhau,
cấu hình app thì không.

**Sửa:** thêm vào `androidApp/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

**Gặp ở:** [`RateWatch/shared/src/androidMain/kotlin/dev/ratewatch/di/PlatformModule.android.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/androidMain/kotlin/dev/ratewatch/di/PlatformModule.android.kt) (comment `PLATFORM:`)

### K449 · Làm theo hướng dẫn cũ: `freeze()`, `InvalidMutabilityException`

`🟡 đã làm app` · `KMP` · `iOS`

**Gặp:** blog / StackOverflow cũ bảo phải `freeze()` object trước khi đưa sang thread khác, dùng `@SharedImmutable`, `AtomicReference`
của `kotlin.native.concurrent`; code chép về báo deprecated hoặc không còn tồn tại.

**Vì sao:** đó là **memory model cũ** của Kotlin/Native (trước 1.7.20). Memory manager hiện tại cho chia sẻ object giữa các thread như
trên JVM; các API freezing đã deprecated hoặc bị xoá.

**Sửa:** bỏ hết `freeze()` và các annotation đó. Xử lý đồng bộ như code JVM bình thường (`Mutex`, atomics, một coroutine sở hữu state).
Khi đọc tài liệu KMP, xem ngày viết: bài trước 2022 thường sai về phần này.

Xem thêm: K404

### K450 · Thư viện multiplatform nhưng thiếu target bạn dùng

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** `commonMain` resolve được, riêng một target lỗi kiểu `No matching variant of … was found` / `Could not resolve …` cho
`iosSimulatorArm64` (hoặc `wasmJs`).

**Vì sao:** thư viện "KMP" không có nghĩa là có **mọi** target. Thư viện cũ thường có `iosArm64`, `iosX64` nhưng thiếu
`iosSimulatorArm64` (simulator trên Mac Apple Silicon).

**Sửa:** xem trang thư viện / klib.io để biết target được hỗ trợ, nâng lên bản có target đó. Không có thì tìm thư viện khác hoặc bọc
phần đó sau interface và cài đặt riêng ở từng nền tảng.

Xem thêm: K408, K425
