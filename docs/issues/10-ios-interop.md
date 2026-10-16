# 10. Swift ↔ Kotlin, iOS, Xcode (K451–K500)

Lỗi ở ranh giới giữa code Kotlin chung và app SwiftUI: kiểu số đổi tên, tên hàm bị đổi, generic biến mất, framework
`Shared` không tìm thấy, link lỗi trong Xcode. Mọi mục viết cho **Objective-C export** (cách Kotlin 2.4 xuất framework mặc
định) cộng **SKIE** như 5 app luyện tập; Swift export trực tiếp vẫn đang thử nghiệm (K500).

### K451 · Kotlin `Int` sang Swift thành `Int32`

`🟢 mới học` · `iOS` · `KMP`

**Gặp:** `Cannot convert value of type 'Int' to expected argument type 'Int32'` khi gọi hàm Kotlin từ Swift, hoặc ngược lại
khi gán kết quả cho biến `Int`.

**Vì sao:** Kotlin `Int` luôn 32 bit, Swift `Int` là 64 bit trên iPhone. Framework giữ đúng kích thước: `Int` → `Int32`,
`Long` → `Int64`, `Short` → `Int16`, `Byte` → `Int8`, `Char` → `unichar` (`UInt16`). `Double`, `Float`, `Boolean` → `Double`,
`Float`, `Bool` thì không đổi.

**Sửa:** đổi kiểu đúng một chỗ, ở lớp model Swift bọc core, không rải `Int32(...)` khắp view.

```swift
core.onTipSelected(percent: percent)          // ❌ percent là Int
core.onTipSelected(percent: Int32(percent))   // ✅ đổi một lần trong SplitModel
```

**Gặp ở:** [`apps/TipSplit/iosApp/iosApp/SplitModel.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/iosApp/iosApp/SplitModel.swift) ·
Xem thêm: K452, K453

### K452 · Chuỗi `%ld` / `%d` hiện số rác khi truyền `Int32` / `Int64`

`🟡 đã làm app` · `iOS`

**Gặp:** `Localizable.strings` có `"%ld lần thử"`, truyền `s.attempt` (Kotlin `Int`) thì ra số khổng lồ hoặc 0. Không có
lỗi compile.

**Vì sao:** `String(format:)` không kiểm kiểu. `%ld` đọc 64 bit, mà Kotlin `Int` sang Swift là `Int32` (32 bit). Ngược lại
`%d` với `Int64` (Kotlin `Long`) đọc thiếu nửa số.

**Sửa:** đổi sang `Int` của Swift trước khi format, hoặc dùng `String(describing:)` với `%@`.

```swift
L("state_reconnecting", s.attempt, s.maxAttempts)            // ❌ Int32 gặp %ld
L("state_reconnecting", Int(s.attempt), Int(s.maxAttempts))  // ✅
```

**Gặp ở:** [`apps/ObdLink/iosApp/iosApp/Views/Labels.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/iosApp/iosApp/Views/Labels.swift) ·
[`apps/RateWatch/iosApp/iosApp/Views/Labels.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/iosApp/iosApp/Views/Labels.swift) (`Long` → `Int64`)

### K453 · `List<Int>` sang Swift thành `[KotlinInt]`

`🟢 mới học` · `iOS` · `KMP`

**Gặp:** `Cannot convert value of type '[KotlinInt]' to expected argument type '[Int]'`; `ForEach(core.tipOptions)` không
chạy được với `Text("\($0)%")` như mong đợi.

**Vì sao:** Objective-C chỉ chứa object trong `NSArray`, không chứa số trần. Phần tử số trong `List`, `Set`, `Map` bị đóng hộp
thành `KotlinInt`, `KotlinLong`, `KotlinDouble`… (lớp con của `NSNumber`).

**Sửa:** đổi một lần bằng `.intValue`, `.int64Value`, `.doubleValue` khi nhận dữ liệu, giữ kiểu Swift thuần cho view.

```swift
self.tipOptions = core.tipOptions                    // ❌ [KotlinInt]
self.tipOptions = core.tipOptions.map { $0.intValue } // ✅ [Int]
```

**Gặp ở:** [`apps/TipSplit/iosApp/iosApp/SplitModel.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/iosApp/iosApp/SplitModel.swift) ·
[`apps/TripLog/iosApp/iosApp/TripsModel.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/iosApp/iosApp/TripsModel.swift) (`List<Double>` → `[KotlinDouble]`)

### K454 · `Int?` / `Double?` sang Swift thành `KotlinInt?` / `KotlinDouble?`

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** property Kotlin `val currentSpeedMps: Double?` hiện trong Swift là `KotlinDouble?`; truyền vào
`String(format:)`, `Gauge(value:)` hay hàm nhận `Double` đều báo lỗi kiểu.

**Vì sao:** Objective-C không có số primitive nullable. Số nullable phải là object, nên bị đóng hộp giống K453.

**Sửa:** unwrap rồi lấy giá trị (`?.doubleValue`). Hoặc thiết kế API Kotlin trả kiểu không null (giá trị mặc định, hoặc bọc
trong một class như `Reading(value)` của ObdLink) để Swift nhận `Double` thật.

```swift
speedText(trip.currentSpeedMps)                                   // ❌ KotlinDouble?
trip.currentSpeedMps.map { speedText($0.doubleValue) } ?? "–"     // ✅
```

**Gặp ở:** [`apps/TripLog/iosApp/iosApp/Views/RecordView.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/iosApp/iosApp/Views/RecordView.swift) ·
[`apps/ObdLink/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/README.md) (mục "Kotlin `Int?` sang Swift là `KotlinInt?`")

### K455 · `ByteArray` sang Swift là `KotlinByteArray`, không phải `Data`

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** hàm Kotlin trả `ByteArray` (ảnh, gói BLE, file), Swift nhận `KotlinByteArray` và không đưa thẳng vào
`UIImage(data:)` hay `write(to:)` được.

**Vì sao:** `ByteArray` là class của Kotlin, không có cầu nối tự động sang `Data`/`NSData`. Đọc từng byte bằng `get(index:)`
từ Swift thì rất chậm.

**Sửa:** viết hàm đổi trong `iosMain`, copy cả khối một lần bằng `usePinned`.

```kotlin
// iosMain
@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
```

### K456 · `Instant`, `Duration` của Kotlin không thành `Date` / `TimeInterval`

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** Swift thấy `Note.updatedAt` là một class Kotlin lạ (hoặc một số `Int64` không rõ đơn vị), không dùng được với
`Text(date, style: .relative)` hay `DateFormatter`.

**Vì sao:** Objective-C export chỉ cầu nối kiểu cơ bản và collection. `kotlin.time.Instant` là class Kotlin; `Duration` là
`value class`, sang Objective-C mất tên kiểu và chỉ còn giá trị bên dưới.

**Sửa:** ở ranh giới Swift, đưa ra kiểu đơn giản có đơn vị rõ trong tên (`updatedAtMillis: Long`), hoặc để core tính sẵn
chuỗi/nhãn ("5 phút trước") như `RelativeTime` của PocketNotes. Swift đổi `Int64` millis sang `Date` một chỗ.

```swift
let date = Date(timeIntervalSince1970: Double(note.updatedAtMillis) / 1000) // ✅ đơn vị rõ ràng
```

### K457 · Gọi API iOS trong `iosMain` bị đòi `@OptIn(ExperimentalForeignApi::class)`

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** `This declaration needs opt-in. Its usage must be marked with '@kotlinx.cinterop.ExperimentalForeignApi'` khi dùng
`CLLocation.coordinate`, `usePinned`, `CPointer`…; hoặc không biết đọc struct C như `CLLocationCoordinate2D`.

**Vì sao:** phần cinterop (con trỏ, struct C, bộ nhớ native) vẫn được Kotlin đánh dấu thử nghiệm. Struct C trả về là
`CValue<…>`, không đọc field trực tiếp được.

**Sửa:** opt-in ở class/file dùng cinterop, đọc struct bằng `useContents { }`, và giữ phần này gọn trong `iosMain`.

```kotlin
@OptIn(ExperimentalForeignApi::class)
class IosLocationStream : LocationStream {
    private fun CLLocation.latLon() = coordinate.useContents { latitude to longitude } // ✅
}
```

**Gặp ở:** [`apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt)

### K458 · Cài delegate iOS trong Kotlin báo "Conflicting overloads"

`🔴 khó, hiếm nhưng đau` · `iOS` · `KMP`

**Gặp:** viết `object : NSObject(), CLLocationManagerDelegateProtocol` với cả `locationManager(manager, didUpdateLocations)`
và `locationManager(manager, didFailWithError)`, compiler báo `Conflicting overloads`.

**Vì sao:** trong Objective-C hai hàm khác tên (`locationManager:didUpdateLocations:` và `locationManager:didFailWithError:`),
nhưng sang Kotlin cùng tên `locationManager` và cùng số tham số. Nếu kiểu tham số cũng trùng sau khi map, Kotlin coi là
khai báo trùng.

**Sửa:** đánh dấu override bị trùng bằng `@ObjCSignatureOverride` (có từ Kotlin 2.0), compiler sẽ phân biệt theo selector
Objective-C.

```kotlin
@ObjCSignatureOverride
override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) { /* … */ } // ✅
```

**Gặp ở:** [`apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt)

### K459 · `switch` trên enum Kotlin bắt thêm `default` (không có SKIE)

`🟢 mới học` · `iOS` · `KMP`

**Gặp:** `Switch must be exhaustive` dù đã liệt kê đủ các case; hoặc phải viết `if error == InputError.notNumeric`.

**Vì sao:** Objective-C không có enum mang object. `enum class` của Kotlin sang Swift là một **class** với các hằng
`InputError.notNumeric`… Swift không biết danh sách là đủ, nên luôn đòi `default`, và thêm case mới bên Kotlin không làm
Swift báo lỗi.

**Sửa:** dùng SKIE (như cả 5 app): SKIE sinh một Swift `enum` thật, `switch` phải phủ hết và thêm case là compile lỗi đúng
chỗ. Không có SKIE thì giữ `default` và viết test bên Kotlin.

```swift
switch error {            // với SKIE: enum Swift thật, không cần default
case .notNumeric: "Nhập số"
case .notPositive: "Phải lớn hơn 0"
case .tooLarge, .noPeople, .tooManyPeople: "…"
}
```

Xem thêm: K460, K472

### K460 · SKIE đổi `NotANumber` thành `.notAnumber`

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** `Type 'InputError' has no member 'notANumber'`; autocomplete chỉ gợi `.notAnumber`.

**Vì sao:** tên case Swift viết thường chữ đầu. Với một dãy chữ hoa liền nhau (`ANumber`, `URLError`), quy tắc đổi tên coi
cả dãy là một từ viết tắt và hạ chữ thường không như bạn đoán.

**Sửa:** tránh chữ hoa đơn lẻ hoặc dãy chữ hoa giữa tên enum entry. Đặt `NotNumeric`, `UrlInvalid` thay vì `NotANumber`,
`URLInvalid`; hai bên đều đọc tự nhiên.

```kotlin
enum class InputError { NotANumber }  // ❌ Swift: .notAnumber
enum class InputError { NotNumeric }  // ✅ Swift: .notNumeric
```

**Gặp ở:** [`apps/TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/model/Models.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/model/Models.kt)

### K461 · Tên bắt đầu bằng `init` bị đổi: `InitFailed`, `initKoin()` → `doInitKoin()`

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** Swift không có `initKoin()` mà chỉ có `doInitKoin()`; enum case `InitFailed` sang Swift mang tên khác hẳn tên bạn gõ.

**Vì sao:** Objective-C coi hàm có tên bắt đầu bằng `init` là hàm khởi tạo (initializer), có luật quản lý bộ nhớ riêng.
Kotlin/Native đổi tên những thành viên như vậy (thêm tiền tố `do`) để khỏi bị hiểu nhầm.

**Sửa:** chấp nhận `doInitKoin()` cho hàm khởi động (cả 5 app gọi vậy trong `iOSApp.swift`), còn tên do mình đặt thì tránh
tiền tố `init`: ObdLink đổi `InitFailed` thành `SetupFailed`.

```kotlin
enum class ConnectionError { ScanFailed, ConnectFailed, InitFailed }   // ❌ tên Swift bị đổi
enum class ConnectionError { ScanFailed, ConnectFailed, SetupFailed }  // ✅
```

**Gặp ở:** [`apps/ObdLink/shared/src/commonMain/kotlin/dev/obdlink/model/ObdState.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/shared/src/commonMain/kotlin/dev/obdlink/model/ObdState.kt) ·
[`apps/TipSplit/iosApp/iosApp/iOSApp.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/iosApp/iosApp/iOSApp.swift) (`doInitKoin()`)

### K462 · `newMonitor()` thành `doNewMonitor()`, `copy()` thành `doCopy()`

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** Swift không thấy `newMonitor()` hay `note.copy(title:)`, chỉ thấy `doNewMonitor()`, `doCopy(...)`; và `doCopy` bắt
truyền **mọi** tham số.

**Vì sao:** cùng lý do K461: Objective-C dành các tiền tố `alloc`, `copy`, `mutableCopy`, `new`, `init` cho hàm tạo object.
`copy` của `data class` cũng trúng luật này. Tham số mặc định thì Objective-C không có (K474).

**Sửa:** đặt tên khác cho hàm của mình (`startMonitor()`). Muốn "sửa một field" từ Swift thì viết hàm Kotlin chuyên dụng
(`fun Note.withTitle(t: String)`), hoặc để việc đó cho core Kotlin làm.

```kotlin
fun newMonitor(): PulseMonitor     // ❌ Swift: doNewMonitor()
fun startMonitor(): PulseMonitor   // ✅
```

**Gặp ở:** [`apps/DevicePulse/shared/src/commonMain/kotlin/dev/devicepulse/core/PulseCore.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/DevicePulse/shared/src/commonMain/kotlin/dev/devicepulse/core/PulseCore.kt)

### K463 · Property `description` sang Swift thành `description_`

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** `dtc.description` trong Swift in ra kiểu `Dtc(code=P0301, …)` chứ không phải mô tả lỗi; field thật nằm ở
`dtc.description_`.

**Vì sao:** mọi object Objective-C đã có `description` (tương đương `toString()`). Property Kotlin trùng tên bị đổi thành
`description_`, còn `description` vẫn là `toString()`. Tương tự với `hash`.

**Sửa:** đặt tên khác ngay bên Kotlin: `summary`, `detail`, `label`.

```kotlin
data class Dtc(val code: String, val description: String?) // ❌ Swift: description_
data class Dtc(val code: String, val summary: String?)     // ✅
```

**Gặp ở:** [`apps/ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/model/Dtc.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/core/obd/src/commonMain/kotlin/dev/obdlink/obd/model/Dtc.kt)

### K464 · Đặt tên `Date`, `Data`, `Error` → Swift lẫn với Foundation

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** class Kotlin top-level tên `Date` làm mọi file Swift có `import Shared` và `import Foundation` báo `'Date' is
ambiguous for type lookup in this context`. Case lồng như `RelativeTime.Date` thì không lỗi ngay nhưng dễ đọc nhầm với
`Foundation.Date`, nhất là trong `extension RelativeTime`.

**Vì sao:** Swift gộp tên từ mọi module được import. Class/case Kotlin tên `Date`, `Data`, `Error`, `Result`, `Task`… đụng
tên của Foundation hay thư viện chuẩn Swift ở **mọi** file view.

**Sửa:** đổi tên bên Kotlin cho khỏi trùng (`DateLabel`, `Payload`, `LoadError`). Đừng bắt mọi file Swift viết
`Foundation.Date`.

```kotlin
sealed class RelativeTime { data class Date(...) : RelativeTime() }     // ❌
sealed class RelativeTime { data class OnDate(...) : RelativeTime() }   // ✅
```

**Gặp ở:** [`apps/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/model/Models.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/model/Models.kt)

### K465 · Hai class cùng tên ở hai package → Swift thấy `Note_`

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** Swift có cả `Note` và `Note_`, và cái bạn cần lại là `Note_`.

**Vì sao:** framework Objective-C không có package; mọi class Kotlin public nằm chung một không gian tên. Hai class cùng
tên đơn (ví dụ `model.Note` và `Note` do SQLDelight sinh từ bảng `note`) thì một cái bị thêm `_`. Cái nào bị đổi phụ thuộc
thứ tự compile, có thể đổi giữa các lần build.

**Sửa:** giữ tên class public là duy nhất trong cả framework. PocketNotes đặt tên bảng `noteRecord` để SQLDelight sinh
`NoteRecord`, nhường `Note` cho model. Class chỉ dùng nội bộ thì để `internal` (không xuất sang Swift).

**Gặp ở:** [`apps/PocketNotes/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/README.md) (mục "Tên bảng `noteRecord`")

### K466 · Muốn đặt tên riêng cho Swift hoặc giấu API khỏi Swift

`🔴 khó, hiếm nhưng đau` · `iOS` · `KMP`

**Gặp:** tên Kotlin hợp lý nhưng sang Swift xấu (`ShapeKt.circle(radius:)`), hoặc hàm chỉ dành cho Android lại hiện trong
autocomplete Xcode.

**Vì sao:** Objective-C export xuất mọi thứ `public`, theo tên Kotlin.

**Sửa:** `@ObjCName(swiftName = "…")` để đổi tên phía Swift (Objective-C), `@HiddenFromObjC` để giấu khỏi framework. Cả hai
cần opt-in. Đơn giản nhất vẫn là đặt `internal` cho thứ Swift không cần.

```kotlin
@OptIn(ExperimentalObjCName::class)
@ObjCName(swiftName = "RateTable")
data class RatesTableModel(val base: String)

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
fun debugDump(): String = "…"
```

### K467 · Swift thấy `Core_obdDtc` thay vì `Dtc`

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** kiểu của module Kotlin khác (`:core:obd`) hiện trong Swift với tiền tố tên module, ví dụ `Core_obdDtc`; hàm tiện
ích viết cho `Dtc` không gọi được từ Swift.

**Vì sao:** framework chỉ xuất đầy đủ API của chính module `:shared`. Kiểu của dependency chỉ được xuất "vừa đủ dùng",
mang tiền tố module, và không kèm các hàm khác của module đó.

**Sửa:** `export()` module đó vào framework, kèm dependency `api()` tương ứng.

```kotlin
iosTarget.binaries.framework {
    baseName = "Shared"
    export(project(":core:obd"))          // ✅ Swift thấy Dtc, Dongle… đúng tên
}
sourceSets.commonMain.dependencies {
    api(project(":core:obd"))             // export() cần api(), không phải implementation()
}
```

**Gặp ở:** [`apps/ObdLink/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/shared/build.gradle.kts) ·
Xem thêm: K468

### K468 · `export()` báo dependency chưa phải `api`

`🟡 đã làm app` · `Gradle` · `KMP`

**Gặp:** task link framework lỗi, câu báo có đoạn `… are not specified as API-dependencies of a corresponding source sets`.

**Vì sao:** `export(x)` chỉ hợp lệ khi `x` là dependency `api` của source set (để kiểu của nó là một phần API công khai).
Khai báo bằng `implementation` thì Gradle từ chối.

**Sửa:** đổi dependency đó sang `api(...)`, chỉ cho module thật sự cần xuất. Đừng `export` mọi thư viện: framework phình to
và Swift thấy cả đống API không dùng.

Xem thêm: K467, K369

### K469 · Swift không gọi được `get<T>()` của Koin

`🟢 mới học` · `iOS` · `KMP`

**Gặp:** từ Swift không có `KoinPlatform.getKoin().get()` dùng được; gọi hàm generic thì nhận `Any?`, hoặc hàm `inline reified`
hoàn toàn không xuất hiện.

**Vì sao:** `reified` nghĩa là kiểu `T` được chép vào chỗ gọi lúc **compile Kotlin**. Swift gọi qua framework đã compile xong,
không còn chỗ nào để chép kiểu. Hàm `inline` cũng không có thân hàm để Swift gọi.

**Sửa:** viết trong `iosMain` một hàm thường cho mỗi kiểu Swift cần, gọi `get()` từ phía Kotlin.

```kotlin
// iosMain/di/KoinIos.kt
fun tipCore(): TipCore = KoinPlatform.getKoin().get()   // ✅ Swift (SKIE): tipCore()
```

**Gặp ở:** [`apps/TipSplit/shared/src/iosMain/kotlin/dev/tipsplit/di/KoinIos.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/iosMain/kotlin/dev/tipsplit/di/KoinIos.kt) ·
cùng mẫu trong PocketNotes, RateWatch, TripLog, ObdLink

### K470 · Hàm generic `fun <T> …` sang Swift trả `Any?`

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** `fun <T : Any> decode(json: String, type: KClass<T>): T` sang Swift thành hàm trả `Any`, phải `as!` mới dùng được.

**Vì sao:** Objective-C chỉ có generic "nhẹ" cho **class** (`NSArray<Foo *>`). Tham số kiểu của **hàm** và của **interface**
bị xoá về giới hạn trên (`Any`/`Any?`).

**Sửa:** ở ranh giới Swift, viết hàm cụ thể (`decodeNote(json)`) thay vì hàm generic; generic dùng bên trong Kotlin thôi.

Xem thêm: K471

### K471 · `sealed interface Reading<T>` sang Swift mất kiểu generic

`🔴 khó, hiếm nhưng đau` · `iOS` · `KMP`

**Gặp:** Swift thấy `Reading` không có `<BatteryInfo>`; `value` là `Any?`, phải ép kiểu ở mọi chỗ dùng.

**Vì sao:** Objective-C giữ generic cho class, không giữ cho protocol. `interface` của Kotlin thành protocol, nên
`sealed interface Reading<out T>` mất `T`. SKIE cũng chỉ sinh enum Swift đầy đủ từ thông tin mà Objective-C header có.

**Sửa:** dùng `sealed class` cho kiểu kết quả mà Swift phải `switch`.

```kotlin
sealed interface Reading<out T : Any>   // ❌ Swift: mất T
sealed class Reading<out T : Any> {     // ✅ Swift: Reading<BatteryInfo>
    data object Loading : Reading<Nothing>()
    data class Available<out T : Any>(val value: T) : Reading<T>()
}
```

**Gặp ở:** [`apps/DevicePulse/shared/src/commonMain/kotlin/dev/devicepulse/model/Models.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/DevicePulse/shared/src/commonMain/kotlin/dev/devicepulse/model/Models.kt) ·
[`apps/TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/model/Models.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/model/Models.kt)

### K472 · `sealed class` trong Swift chỉ có `as?` (không có SKIE)

`🟢 mới học` · `iOS` · `KMP`

**Gặp:** không `switch` được trên `SplitResult`; phải viết chuỗi `if let r = result as? SplitResult.Ready … else if …`, và
quên một nhánh thì không ai báo.

**Vì sao:** sang Objective-C, `sealed class` chỉ là class cha và các class con. Swift không biết danh sách con là đóng.

**Sửa:** với SKIE, dùng `onEnum(of:)`: SKIE sinh một enum Swift có associated value, `switch` phải phủ hết.

```swift
switch onEnum(of: state.result) {          // ✅ thiếu case là compile lỗi
case .waiting: Text("Nhập số tiền")
case .invalid(let s): Text(s.error.label)
case .ready(let s): SplitSummary(s.split)
}
```

**Gặp ở:** [`apps/ObdLink/iosApp/iosApp/Views/Labels.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/iosApp/iosApp/Views/Labels.swift)

### K473 · Không tìm thấy `object`, `companion`, hàm top-level từ Swift

`🟢 mới học` · `iOS` · `KMP`

**Gặp:** `Cannot find 'tipCore' in scope`; `Settings.defaultTip` báo không có member; `Waiting()` không tạo được.

**Vì sao:** Objective-C không có hàm rời hay singleton kiểu Kotlin, nên:

- hàm/biến top-level nằm trong class đặt theo tên file: `KoinIosKt.tipCore()` (SKIE thêm bản global `tipCore()`);
- `object Foo` → `Foo.shared`; `data object Waiting` → `SplitResult.Waiting.shared`;
- `companion object` → `Foo.companion.bar` (hoặc `Foo.Companion.shared`).

**Sửa:** tra trong header của framework (`Shared.framework/Headers/Shared.h`) hay để Xcode autocomplete sau `Shared.`.

```swift
let core = KoinIosKt.tipCore()          // không có SKIE
let core = tipCore()                    // có SKIE
let waiting = SplitResult.Waiting.shared
```

### K474 · Tham số mặc định của Kotlin không có trong Swift

`🟢 mới học` · `iOS` · `KMP`

**Gặp:** `fun search(text: String, limit: Int = 20)`, Swift báo `Missing argument for parameter 'limit' in call`.

**Vì sao:** Objective-C không có tham số mặc định, nên mọi tham số đều bắt buộc. Constructor của `data class` cũng vậy.
SKIE có tính năng sinh overload cho default argument nhưng **tắt mặc định** (làm build chậm), phải bật riêng.

**Sửa:** với API Swift hay gọi, thêm overload rõ ràng hoặc hàm có tên khác; hoặc bật default arguments của SKIE cho riêng
class đó.

```kotlin
fun search(text: String, limit: Int): List<Note> = …
fun search(text: String): List<Note> = search(text, 20)   // ✅ Swift gọi được search(text:)
```

### K475 · Class Swift cài interface Kotlin phải viết lại cả hàm có thân mặc định

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** `Type 'SwiftClock' does not conform to protocol 'Clock'`, dù interface Kotlin đã có sẵn thân hàm cho method đó.

**Vì sao:** interface Kotlin sang Swift là protocol Objective-C. Protocol Objective-C không mang code, nên phần thân mặc
định của Kotlin không đi theo.

**Sửa:** giữ interface mà Swift phải cài thật nhỏ (một, hai hàm không có default). Logic dùng chung đặt ở hàm Kotlin nhận
interface đó, không đặt trong default method.

### K476 · App iOS crash với "Uncaught Kotlin exception"

`🟢 mới học` · `iOS` · `KMP`

**Gặp:** console Xcode in `Uncaught Kotlin exception: kotlin.IllegalStateException: …` rồi app dừng, dù Swift đã bọc
`do { try … } catch`.

**Vì sao:** Swift chỉ bắt được lỗi mà Kotlin khai báo bằng `@Throws`. Hàm không có `@Throws` sẽ không thành `throws` trong
Swift; exception thoát ra ranh giới là chương trình bị dừng. Exception không thuộc các lớp liệt kê trong `@Throws` cũng crash.

**Sửa:** hàm Swift gọi mà có thể lỗi thì hoặc khai báo `@Throws(…)`, hoặc (tốt hơn) bắt lỗi ngay trong Kotlin và trả về kết
quả có kiểu (`null`, `sealed class Result`).

```kotlin
@Throws(IOException::class)
fun exportGpx(id: Long): String = …       // Swift: try core.exportGpx(id:)
```

Xem thêm: K477

### K477 · `suspend fun` gọi từ Swift ném lỗi là crash

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** `try? await library.load(tripId:)` vẫn crash khi DB lỗi; chỉ khi coroutine bị huỷ thì Swift mới nhận một `Error`.

**Vì sao:** `suspend fun` không có `@Throws` thì chỉ `CancellationException` được chuyển thành `Error` cho Swift. Mọi exception
khác thoát ra là chương trình dừng như K476.

**Sửa:** bắt lỗi bên trong hàm Kotlin, ném lại `CancellationException`, trả `null` hoặc kết quả có kiểu.

```kotlin
suspend fun load(tripId: Long): TripDetail? = try {
    repository.loadTrip(tripId)?.let { TripDetail(it, repository.loadPoints(tripId)) }
} catch (e: CancellationException) {
    throw e                          // ✅ huỷ vẫn là huỷ
} catch (_: Exception) {
    null                             // ✅ không exception nào thoát sang Swift
}
```

**Gặp ở:** [`apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/core/TripLibrary.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/commonMain/kotlin/dev/triplog/core/TripLibrary.kt) ·
[`apps/TripLog/iosApp/iosApp/TripsModel.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/iosApp/iosApp/TripsModel.swift)

### K478 · Không biết đọc `Flow` / `StateFlow` từ Swift

`🟢 mới học` · `iOS` · `KMP`

**Gặp:** không có SKIE thì `core.state` chỉ có `collect(collector:completionHandler:)`, phải tự viết class `FlowCollector`
bằng Swift; `value` trả `Any?`.

**Vì sao:** `Flow` là interface Kotlin có generic và có `suspend fun collect`: Objective-C mất generic (K471) và chỉ thấy
callback.

**Sửa:** dùng SKIE: `Flow` thành `AsyncSequence`, `StateFlow` có `value` đúng kiểu. Đọc bằng `for await` trong một `Task`.
Không dùng SKIE thì dùng thư viện khác (KMP-NativeCoroutines) hoặc tự viết wrapper callback trong `iosMain`.

```swift
self.state = core.state.value
Task { [weak self] in
    for await state in core.state { self?.state = state }   // ✅ SKIE
}
```

**Gặp ở:** [`apps/PocketNotes/iosApp/iosApp/NotesModel.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/iosApp/iosApp/NotesModel.swift)

### K479 · `for await` trên `StateFlow` không bao giờ kết thúc

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** code sau vòng `for await state in core.state { … }` không bao giờ chạy; rời màn hình rồi mà vòng lặp vẫn nhận giá
trị; nhiều lần mở màn hình là nhiều vòng song song.

**Vì sao:** `StateFlow` không bao giờ hoàn thành, nên vòng `for await` chỉ dừng khi `Task` chứa nó bị huỷ. `Task { }` tạo
trong `init` không tự huỷ khi view biến mất.

**Sửa:** đặt mỗi vòng `for await` trong `Task` riêng, không viết code sau nó. Gắn vòng đọc với vòng đời view bằng modifier
`.task { }` (SwiftUI huỷ khi view biến mất), hoặc giữ `Task` và gọi `cancel()` trong `deinit`. SKIE chuyển việc huỷ `Task`
thành huỷ coroutine bên Kotlin.

```swift
.task {                                        // ✅ huỷ khi view biến mất
    for await s in model.core.state { model.state = s }
}
```

### K480 · "Publishing changes from background threads is not allowed"

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** cảnh báo tím trong Xcode `Publishing changes from background threads is not allowed; make sure to publish values from
the main thread…`, hoặc UI cập nhật chậm, thỉnh thoảng crash.

**Vì sao:** core Kotlin có thể phát giá trị từ thread nền (`Dispatchers.Default`, callback BLE). Vòng `for await` không có
`@MainActor` chạy trên thread đó và gán thẳng vào state của SwiftUI.

**Sửa:** đánh dấu model Swift là `@MainActor`, để `Task` tạo bên trong cũng chạy trên main, phép gán luôn ở main thread.

```swift
@MainActor @Observable
final class ObdModel {
    private(set) var state: ObdState
    // Task { for await … } bên trong chạy trên MainActor ✅
}
```

**Gặp ở:** [`apps/ObdLink/iosApp/iosApp/ObdModel.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/iosApp/iosApp/ObdModel.swift)

### K481 · Core Kotlin bên iOS chạy mãi vì không ai huỷ scope

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** đóng màn hình rồi mà timer/collect của core vẫn chạy, log vẫn in, pin hao.

**Vì sao:** Android đưa `viewModelScope` vào core và scope đó tự huỷ. Swift không tạo được `CoroutineScope`, nên hàm `iosMain`
tự tạo `MainScope()` cho core. Không ai gọi `cancel()` thì nó sống tới khi app chết.

**Sửa:** core có `close()` huỷ scope; model Swift gọi trong `deinit` (hoặc khi màn hình đóng).

```swift
final class NotesModel {
    @ObservationIgnored private let core: NotesCore
    deinit { core.close() }        // ✅ huỷ MainScope bên Kotlin
}
```

**Gặp ở:** [`apps/PocketNotes/iosApp/iosApp/NotesModel.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/iosApp/iosApp/NotesModel.swift) ·
[`apps/DevicePulse/shared/src/commonMain/kotlin/dev/devicepulse/core/PulseCore.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/DevicePulse/shared/src/commonMain/kotlin/dev/devicepulse/core/PulseCore.kt)

### K482 · Closure Swift đưa cho Kotlin giữ `self` → model không bao giờ `deinit`

`🔴 khó, hiếm nhưng đau` · `iOS` · `KMP`

**Gặp:** `deinit` của model Swift không chạy; Memory Graph của Xcode thấy model còn sống sau khi đóng màn hình.

**Vì sao:** closure Swift truyền vào Kotlin (callback, listener) được Kotlin giữ mạnh. Closure lại giữ `self` mạnh. Chu trình
đi qua ranh giới Swift (đếm tham chiếu) và Kotlin (GC) nên không bên nào tự gỡ được.

**Sửa:** mọi closure gửi sang Kotlin hoặc chạy lâu dùng `[weak self]`; và gỡ listener khi đóng.

```swift
core.onEvent { event in self.handle(event) }               // ❌ giữ self mãi
core.onEvent { [weak self] event in self?.handle(event) }  // ✅
```

### K483 · Swift 6: "non-sendable type" khi dùng object Kotlin trong `Task`

`🔴 khó, hiếm nhưng đau` · `iOS` · `KMP`

**Gặp:** bật Swift 6 language mode (hoặc strict concurrency) thì hàng loạt cảnh báo/lỗi như
`Capture of 'core' with non-sendable type 'NotesCore' in a '@Sendable' closure` hay `Sending 'state' risks causing data races`.

**Vì sao:** class Kotlin trong header Objective-C không khai báo `Sendable`. Swift 6 không biết chúng an toàn giữa các
thread nên chặn việc chuyển chúng qua actor khác.

**Sửa:** giữ object Kotlin trong một model `@MainActor` và chỉ dùng chúng trên main actor (như 5 app). Còn lại có thể
`@preconcurrency import Shared` để hạ mức kiểm tra cho module này. Đừng rải `@unchecked Sendable` khắp nơi: đó là che lỗi.

```swift
@preconcurrency import Shared
@MainActor @Observable final class NotesModel { /* mọi truy cập core ở đây */ }
```

### K484 · Xcode báo `No such module 'Shared'`

`🟢 mới học` · `iOS` · `Gradle`

**Gặp:** `import Shared` gạch đỏ, build lỗi `No such module 'Shared'`.

**Vì sao:** framework chưa được build, hoặc Xcode không biết tìm ở đâu. Hay gặp nhất:

- thiếu Run Script gọi `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, hoặc script nằm **sau** bước *Compile Sources*;
- `FRAMEWORK_SEARCH_PATHS` thiếu `$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`;
- `baseName` trong Gradle khác tên trong `import` (phân biệt hoa thường: `shared` ≠ `Shared`).

**Sửa:** kiểm ba chỗ trên, rồi build một lần (⌘B) trước khi tin vào gạch đỏ của editor.

```kotlin
iosTarget.binaries.framework { baseName = "Shared" }   // Swift: import Shared
```

**Gặp ở:** [`apps/PocketNotes/iosApp/project.yml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/iosApp/project.yml) (script + search path) ·
Xem thêm: [Lỗi hay gặp](../reference/troubleshooting.md)

### K485 · Run Script Gradle lỗi "Sandbox: … deny(1) file-read-data"

`🟢 mới học` · `iOS` · `Gradle`

**Gặp:** bước chạy `./gradlew` trong Xcode lỗi với các dòng `Sandbox: bash(…) deny(1) file-read-data …/gradlew` hoặc
`deny(1) file-write-create …`.

**Vì sao:** từ Xcode 15, project mới bật `ENABLE_USER_SCRIPT_SANDBOXING = YES`: script chỉ được đọc/ghi file khai báo trong
input/output. Gradle cần đọc cả project và ghi vào `build/`, `~/.gradle`.

**Sửa:** tắt sandbox cho script của target app.

```yaml
settings:
  base:
    ENABLE_USER_SCRIPT_SANDBOXING: NO   # ✅ project.yml (XcodeGen) hoặc Build Settings
```

**Gặp ở:** [`apps/PocketNotes/iosApp/project.yml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/iosApp/project.yml)

### K486 · Build từ Xcode: "Unable to locate a Java Runtime"

`🟢 mới học` · `iOS` · `Gradle`

**Gặp:** terminal chạy `./gradlew` được, nhưng build trong Xcode thì Run Script báo `The operation couldn't be completed. Unable
to locate a Java Runtime.` hoặc dùng sai bản JDK.

**Vì sao:** Xcode chạy script với môi trường tối giản, không đọc `~/.zshrc`, nên không có `JAVA_HOME` bạn đặt ở đó.

**Sửa:** ghim JDK cho Gradle daemon bằng `gradle/gradle-daemon-jvm.properties` (K351), hoặc đặt `JAVA_HOME` ngay trong Run Script.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

Xem thêm: K351, K352

### K487 · Lỗi Kotlin trong Xcode chỉ hiện "Command PhaseScriptExecution failed"

`🟡 đã làm app` · `iOS` · `Gradle`

**Gặp:** Xcode báo `Command PhaseScriptExecution failed with a nonzero exit code`, không rõ lỗi Kotlin nào. Chạy
`embedAndSignAppleFrameworkForXcode` trong terminal thì lại báo thiếu `SDK_NAME`, `CONFIGURATION`… và bảo chạy từ Xcode.

**Vì sao:** task `embedAndSign…` đọc biến môi trường Xcode truyền vào, nên chỉ chạy đúng từ Xcode. Lỗi compiler nằm trong log
của bước script, không ở dòng tóm tắt.

**Sửa:** mở Report navigator (⌘9), chọn lần build, mở rộng bước Run Script để đọc dòng `e:` đầu tiên. Hoặc tự compile/link
trong terminal bằng task không cần biến Xcode.

```bash
./gradlew :shared:compileKotlinIosSimulatorArm64       # chỉ xem lỗi compile
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64  # thêm bước link
```

Xem thêm: K398, K399

### K488 · "Requested Architecture Not Configured" khi build "Any iOS Simulator"

`🟡 đã làm app` · `iOS` · `Gradle`

**Gặp:** `xcodebuild -destination 'generic/platform=iOS Simulator'` (hoặc archive/CI) lỗi, Gradle báo kiến trúc Xcode yêu cầu
chưa được cấu hình (*Requested Architecture Not Configured*).

**Vì sao:** đích "generic" build cả `arm64` lẫn `x86_64` cho simulator. `shared` chỉ khai báo `iosSimulatorArm64()` (Mac
Apple Silicon), không có `iosX64()`.

**Sửa:** bảo Xcode bỏ `x86_64` khi build cho simulator. Chỉ thêm `iosX64()` nếu thật sự còn phải chạy trên Mac Intel.

```yaml
settings:
  base:
    EXCLUDED_ARCHS[sdk=iphonesimulator*]: x86_64
```

**Gặp ở:** [`apps/TripLog/iosApp/project.yml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/iosApp/project.yml) ·
[`apps/ObdLink/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/README.md) ·
[`apps/PocketNotes/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/README.md)

### K489 · "building for 'iOS-simulator', but linking in object file built for 'iOS'"

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** link lỗi với câu trên (hoặc ngược lại `built for 'iOS-simulator'` khi chạy máy thật) sau khi copy tay một framework
Kotlin vào project.

**Vì sao:** framework cho máy thật (`iosArm64`) và cho simulator (`iosSimulatorArm64`) là hai binary khác nhau. Xcode chọn
theo đích chạy; framework copy tay chỉ đúng cho một loại.

**Sửa:** để `embedAndSignAppleFrameworkForXcode` build đúng bản theo `SDK_NAME` mỗi lần (như 5 app), hoặc nếu phát hành
binary thì đóng XCFramework chứa cả hai.

```kotlin
val xcf = XCFramework("Shared")
listOf(iosArm64(), iosSimulatorArm64()).forEach {
    it.binaries.framework { baseName = "Shared"; xcf.add(this) }
}
// ./gradlew :shared:assembleSharedXCFramework
```

### K490 · Link lỗi `_sqlite3_open_v2` khi dùng SQLDelight trên iOS

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** `Undefined symbols for architecture arm64: "_sqlite3_open_v2"` (hoặc `Undefined symbol: _sqlite3_…`) lúc link app iOS.

**Vì sao:** `NativeSqliteDriver` gọi SQLite của hệ thống. Framework `Shared` là **static**, nên phần link với `libsqlite3`
phải do app làm; `linkerOpts` trong Gradle không đủ.

**Sửa:** thêm `-lsqlite3` ở **cả hai** chỗ.

```kotlin
// shared/build.gradle.kts
iosTarget.binaries.framework { isStatic = true; linkerOpts("-lsqlite3") }
```

```yaml
# iosApp/project.yml
OTHER_LDFLAGS: ["$(inherited)", "-framework", "Shared", "-lsqlite3"]
```

**Gặp ở:** [`apps/PocketNotes/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/shared/build.gradle.kts) ·
[`apps/PocketNotes/iosApp/project.yml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/iosApp/project.yml) ·
[`apps/RateWatch/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/README.md)

### K491 · Mở app là crash: "Library not loaded: @rpath/Shared.framework/Shared"

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** build được, mở app thì crash ngay với `dyld[…]: Library not loaded: @rpath/Shared.framework/Shared`.

**Vì sao:** framework **dynamic** (`isStatic = false`) phải được nhúng (embed) và ký vào bundle app. Link được không có
nghĩa là đã nhúng.

**Sửa:** dùng `embedAndSignAppleFrameworkForXcode` (task này nhúng và ký giúp), hoặc trong Xcode chọn *Embed & Sign* cho
framework. Không cần dynamic thì dùng `isStatic = true` như 5 app: không có bước nhúng nào để quên.

### K492 · Hai framework Kotlin trong một app iOS

`🔴 khó, hiếm nhưng đau` · `iOS` · `KMP`

**Gặp:** app có `Auth.framework` và `Notes.framework` đều viết bằng Kotlin; `User` lấy từ cái này không đưa vào hàm của cái
kia được (Swift thấy hai kiểu khác nhau), app phình to.

**Vì sao:** mỗi framework Kotlin/Native mang runtime và bản copy riêng của stdlib, coroutines và mọi dependency. Hai framework
là hai thế giới Kotlin độc lập.

**Sửa:** gom thành **một** framework "umbrella" cho app iOS: một module `:shared` phụ thuộc các module kia và `export()` những
gì Swift cần (K467). ObdLink làm vậy với `:core:obd` và `:core:ble`.

Xem thêm: K467

### K493 · Thêm cấu hình Xcode "Staging" thì Gradle không biết build debug hay release

`🟡 đã làm app` · `iOS` · `Gradle`

**Gặp:** Run Script báo kiểu `Unable to detect Kotlin framework build type for CONFIGURATION=Staging automatically`.

**Vì sao:** `embedAndSign…` đoán debug/release từ tên cấu hình Xcode. Chỉ `Debug` và `Release` là đoán được.

**Sửa:** đặt build setting `KOTLIN_FRAMEWORK_BUILD_TYPE` cho cấu hình mới.

```yaml
configs:
  Staging:
    KOTLIN_FRAMEWORK_BUILD_TYPE: release   # hoặc debug
```

### K494 · Sửa Kotlin rồi mà app iOS vẫn chạy code cũ

`🟢 mới học` · `iOS` · `KMP`

**Gặp:** đổi tên hàm trong `commonMain`, Swift vẫn gọi được tên cũ; sửa logic mà simulator vẫn chạy như trước.

**Vì sao:** Run Script bị tắt hoặc bị bỏ qua (`OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES`), Xcode dùng framework cũ trong
`build/xcode-frameworks`, hoặc editor Xcode chưa đọc lại header mới.

**Sửa:** build lại bằng ⌘B (không chỉ chờ editor), xem log có chạy task Gradle không. Vẫn cũ thì *Product › Clean Build
Folder* (⇧⌘K) rồi build lại.

Xem thêm: [Lỗi hay gặp](../reference/troubleshooting.md)

### K495 · Không đặt được breakpoint trong file `.kt` từ Xcode

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** app iOS chạy sai trong code Kotlin nhưng Xcode không mở được `.kt`, không dừng ở breakpoint; stack trace crash chỉ
có địa chỉ.

**Vì sao:** Xcode không biết Kotlin. Thông tin debug có trong framework, nhưng Xcode cần plugin để hiểu file và kiểu Kotlin.

**Sửa:** cài plugin **xcode-kotlin** (Touchlab) để đặt breakpoint, xem biến Kotlin trong Xcode. Với crash bản release,
giữ và upload file dSYM của framework cùng dSYM của app để crash report có tên hàm. Logic thuần thì debug nhanh hơn bằng test
`commonTest` chạy trên JVM.

### K496 · Dùng CocoaPods: "No such module" dù đã `pod install`

`🟢 mới học` · `iOS` · `Gradle`

**Gặp:** project dùng plugin `kotlin("native.cocoapods")`, mở `iosApp.xcodeproj` thì `No such module 'shared'`.

**Vì sao:** CocoaPods gắn pod vào `.xcworkspace`. Mở `.xcodeproj` thì Xcode không thấy pod nào.

**Sửa:** chạy `pod install` trong `iosApp/` rồi luôn mở `iosApp.xcworkspace`. Project mới thì cân nhắc tích hợp trực tiếp
(`embedAndSign…`) như 5 app: CocoaPods đã công bố trunk sẽ chuyển sang chỉ đọc, và JetBrains có hướng dẫn chuyển từ
CocoaPods sang tích hợp trực tiếp.

### K497 · Crash khi xin quyền: Info.plist thiếu usage description hoặc background mode

`🟢 mới học` · `iOS`

**Gặp:** `This app has crashed because it attempted to access privacy-sensitive data without a usage description. The app's
Info.plist must contain an NSLocationWhenInUseUsageDescription key…`; hoặc `NSInternalInconsistencyException` khi đặt
`allowsBackgroundLocationUpdates = true`.

**Vì sao:** iOS bắt app khai báo lý do cho từng loại dữ liệu riêng tư (vị trí, Bluetooth, camera…) và khai báo background
mode trước khi dùng. Thiếu là crash ngay, không có hộp thoại.

**Sửa:** thêm key vào Info.plist (qua `project.yml`): `NSLocationWhenInUseUsageDescription`,
`NSBluetoothAlwaysUsageDescription`…, và `UIBackgroundModes: [location]` nếu ghi vị trí khi tắt màn hình.

**Gặp ở:** [`apps/TripLog/iosApp/project.yml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/iosApp/project.yml) ·
[`apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt)

### K498 · Delegate viết bằng Kotlin ngừng nhận callback sau vài giây

`🔴 khó, hiếm nhưng đau` · `iOS` · `KMP`

**Gặp:** `CLLocationManager` gọi delegate vài lần rồi im, không lỗi gì. Hoặc compiler không cho class Kotlin cài
`CLLocationManagerDelegateProtocol`.

**Vì sao:** class Kotlin cài protocol Objective-C phải kế thừa một lớp Objective-C (`NSObject`). Và `delegate` của nhiều API
Apple là tham chiếu **weak**: nếu chỉ manager trỏ tới object delegate, Kotlin GC thu hồi nó và callback dừng.

**Sửa:** kế thừa `NSObject()`, giữ một tham chiếu mạnh tới delegate suốt thời gian dùng (field, hoặc biến trong
`callbackFlow` được dùng trong `awaitClose`).

```kotlin
val delegate = object : NSObject(), CLLocationManagerDelegateProtocol { /* … */ }
manager.delegate = delegate
awaitClose {
    manager.delegate = null
    delegate.hashCode()   // ✅ giữ delegate sống tới khi flow đóng
}
```

**Gặp ở:** [`apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt)

### K499 · `CLLocationManager` tạo trên `Dispatchers.Default` không bao giờ gọi delegate

`🔴 khó, hiếm nhưng đau` · `iOS` · `KMP`

**Gặp:** đã có quyền, đã `startUpdatingLocation()`, nhưng không một điểm GPS nào về; không lỗi.

**Vì sao:** `CLLocationManager` gửi callback lên run loop của thread đã tạo nó. Thread của `Dispatchers.Default` không có run
loop, nên callback không bao giờ được giao.

**Sửa:** tạo và dùng manager trên main thread: `flowOn(Dispatchers.Main)` cho flow vị trí.

```kotlin
override fun points(): Flow<TrackPoint> = callbackFlow { /* CLLocationManager() … */ }
    .flowOn(Dispatchers.Main)   // ✅ thread có run loop
```

**Gặp ở:** [`apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/iosMain/kotlin/dev/triplog/platform/IosLocationStream.kt)

### K500 · Tưởng Swift export đã thay được Objective-C export + SKIE

`🟡 đã làm app` · `iOS` · `KMP`

**Gặp:** đọc tin "Kotlin xuất thẳng sang Swift", bỏ SKIE, bật Swift export, rồi gặp API thiếu hoặc khác, thư viện không chạy,
hướng dẫn trên mạng không khớp.

**Vì sao:** Swift export (xuất thẳng sang Swift, không qua header Objective-C) vẫn là tính năng **thử nghiệm**, chưa Stable,
chưa hỗ trợ mọi thứ mà Objective-C export + SKIE đang làm được (flow, sealed, suspend…). Mặc định Kotlin vẫn xuất framework qua
Objective-C, và mọi mục trong trang này nói về cách đó.

**Sửa:** app thật vẫn dùng Objective-C export + SKIE. Muốn thử Swift export thì làm trên nhánh riêng, đọc trang "Swift export"
trong tài liệu Kotlin đúng phiên bản đang dùng, và kiểm từng API quan trọng từ Swift.

Xem thêm: K361
