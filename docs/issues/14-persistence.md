# 14. Lưu dữ liệu (K651–K700)

Lỗi khi lưu dữ liệu trên máy bằng SQLDelight, Room (bản KMP) và DataStore: quên migration, link thiếu `-lsqlite3` trên iOS,
query chạy trên main thread, transaction đặt sai chỗ. Đặc điểm chung: test trên máy cài mới thì xanh, lỗi chỉ hiện ở máy
người dùng **đã có dữ liệu cũ**.

### K651 · Đổi bảng trong `.sq` mà không viết `.sqm` → `no such column: pinned`

`🟢 mới học` · `KMP`

**Gặp:** máy cài mới chạy tốt. Người dùng cập nhật từ bản cũ thì crash ngay khi mở:
`android.database.sqlite.SQLiteException: no such column: pinned (code 1 SQLITE_ERROR)`.

**Vì sao:** `CREATE TABLE` trong file `.sq` chỉ chạy khi DB **chưa có**. Máy cũ đã có bảng phiên bản 1, driver chỉ chạy
các file migration `.sqm` để nâng lên. Không có `.sqm` thì bảng cũ giữ nguyên, query mới đọc cột không tồn tại.

**Sửa:** mỗi lần đổi schema làm **hai** việc: sửa `CREATE TABLE` trong `.sq` (cho máy cài mới) và thêm file `N.sqm` (cho
máy đang ở phiên bản N).

```sql
-- Note.sq: schema mới nhất
CREATE TABLE noteRecord (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    pinned INTEGER NOT NULL DEFAULT 0
);
-- 1.sqm: nâng máy cũ từ 1 lên 2
ALTER TABLE noteRecord ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0;
```

**Gặp ở:** [`PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/Note.sq`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/Note.sq) · [`PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/1.sqm`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/1.sqm)

Xem thêm: K652, K657

### K652 · Đặt tên file migration sai số

`🟡 đã làm app` · `KMP`

**Gặp:** viết `2.sqm` cho lần đổi schema đầu tiên, máy cũ vẫn crash; hoặc `Schema.version` không như mình nghĩ.

**Vì sao:** với SQLDelight, `N.sqm` là bước **từ N lên N + 1**, và phiên bản schema = số lớn nhất của file `.sqm` + 1.
Bản đầu tiên phát hành (chưa có `.sqm`) là phiên bản 1, nên lần đổi đầu tiên phải là `1.sqm`. Đặt `2.sqm` thì không có
bước 1 → 2 và schema nhảy lên 3.

**Sửa:** đánh số liên tục từ 1; ghi chú cạnh khai báo database để người sau khỏi đoán.

```kotlin
sqldelight {
    databases {
        create("NotesDb") {
            packageName.set("dev.pocketnotes.db")
            // Schema version = highest .sqm number + 1. 1.sqm exists, so NotesDb.Schema.version == 2.
        }
    }
}
```

**Gặp ở:** [`PocketNotes/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/build.gradle.kts)

### K653 · Máy cài mới và máy nâng cấp có schema khác nhau

`🔴 khó, hiếm nhưng đau` · `KMP`

**Gặp:** lỗi chỉ xảy ra trên một số máy: cột có `DEFAULT` ở máy này mà không có ở máy kia, index thiếu, query chậm bất
thường ở người dùng lâu năm.

**Vì sao:** máy mới chạy `CREATE TABLE` trong `.sq`, máy cũ chạy chuỗi `.sqm`. Hai đường này do người viết tay, lệch
nhau mà không ai biết.

**Sửa:** bật kiểm migration của SQLDelight: lưu file schema `.db` của từng phiên bản (`schemaOutputDirectory`), bật
`verifyMigrations`, và chạy task `verify…Migration` trong CI. Kèm một test migrate thật (K657).

```kotlin
create("NotesDb") {
    packageName.set("dev.pocketnotes.db")
    schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
    verifyMigrations.set(true)
}
```

### K654 · Sửa một file `.sqm` đã phát hành

`🔴 khó, hiếm nhưng đau` · `KMP`

**Gặp:** sửa lỗi trong `1.sqm` rồi phát hành bản mới; máy đã cập nhật trước đó vẫn mang lỗi cũ, máy cập nhật sau thì đúng.

**Vì sao:** driver ghi số phiên bản vào DB (`PRAGMA user_version`) sau khi migrate. Máy đã ở phiên bản 2 sẽ không chạy lại
`1.sqm`, dù nội dung đổi.

**Sửa:** coi `.sqm` đã phát hành là bất biến. Cần sửa thì thêm migration **mới** (`2.sqm`) để vá dữ liệu / schema.

### K655 · `Cannot add a NOT NULL column with default value NULL`

`🟢 mới học` · `KMP`

**Gặp:** migration crash với `SQLiteException: Cannot add a NOT NULL column with default value NULL`.

**Vì sao:** `ALTER TABLE ... ADD COLUMN x INTEGER NOT NULL` phải điền giá trị cho mọi dòng đã có. Không có `DEFAULT` thì
SQLite không biết điền gì.

**Sửa:** cột `NOT NULL` thêm sau phải có `DEFAULT`. Giữ `DEFAULT` giống nhau ở `.sq` và `.sqm`.

```sql
ALTER TABLE noteRecord ADD COLUMN pinned INTEGER NOT NULL;              -- ❌
ALTER TABLE noteRecord ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0;    -- ✅
```

**Gặp ở:** [`PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/1.sqm`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/1.sqm)

### K656 · Muốn đổi kiểu cột / xoá cột bằng `ALTER TABLE`

`🟡 đã làm app` · `KMP`

**Gặp:** `near "COLUMN": syntax error` hoặc `near "DROP": syntax error` khi migrate trên máy Android cũ, dù chạy được trên
máy mới.

**Vì sao:** `ALTER TABLE` của SQLite rất hạn chế: không đổi kiểu cột, không thêm ràng buộc. `DROP COLUMN` và `RENAME
COLUMN` chỉ có từ các bản SQLite mới, còn Android cũ đi kèm SQLite cũ hơn.

**Sửa:** dùng mẫu "tạo bảng mới, chép dữ liệu, xoá bảng cũ, đổi tên", trong một migration.

```sql
CREATE TABLE trip_new (id INTEGER PRIMARY KEY AUTOINCREMENT, startedAtMillis INTEGER NOT NULL);
INSERT INTO trip_new(id, startedAtMillis) SELECT id, startedAtMillis FROM trip;
DROP TABLE trip;
ALTER TABLE trip_new RENAME TO trip;
```

Xem thêm: K682

### K657 · Không có test cho migration

`🟡 đã làm app` · `KMP`

**Gặp:** test repository xanh (chạy trên DB tạo mới), vẫn phát hành bản crash với người dùng cũ.

**Vì sao:** test tạo DB bằng `Schema.create` chỉ thử đường "cài mới", không bao giờ chạy `.sqm`.

**Sửa:** viết test dựng bảng phiên bản cũ bằng SQL tay, chèn dữ liệu cũ, gọi `Schema.migrate`, rồi đọc qua repository.

```kotlin
@Test
fun `migration from schema 1 keeps old notes and adds pinned`() = runTest {
    driver.execute(null, "CREATE TABLE noteRecord (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, ...)", 0)
    driver.execute(null, "INSERT INTO noteRecord(title, ...) VALUES ('old', ...)", 0)
    NotesDb.Schema.migrate(driver, oldVersion = 1, newVersion = NotesDb.Schema.version)
    // đọc lại qua SqlNoteRepository: ghi chú cũ còn, pinned = false
}
```

**Gặp ở:** [`PocketNotes/shared/src/androidUnitTest/kotlin/dev/pocketnotes/NoteRepositoryTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/androidUnitTest/kotlin/dev/pocketnotes/NoteRepositoryTest.kt)

### K658 · iOS link lỗi `Undefined symbols … _sqlite3_*`

`🟢 mới học` · `KMP` · `iOS`

**Gặp:** Android chạy tốt, build iOS dừng ở bước link: `Undefined symbols for architecture arm64: "_sqlite3_open_v2"`,
`"_sqlite3_prepare_v2"`…

**Vì sao:** `NativeSqliteDriver` của SQLDelight gọi thẳng SQLite của hệ thống, nhưng không tự kéo thư viện đó vào khi link.

**Sửa:** thêm `-lsqlite3` vào `linkerOpts` của framework trong `shared/build.gradle.kts` (và vào Xcode, K659).

```kotlin
iosTarget.binaries.framework {
    baseName = "Shared"
    isStatic = true
    linkerOpts("-lsqlite3")     // ✅
}
```

**Gặp ở:** [`PocketNotes/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/build.gradle.kts)

Xem thêm: K659

### K659 · Đã có `-lsqlite3` trong Gradle mà Xcode vẫn báo `_sqlite3_*`

`🟡 đã làm app` · `KMP` · `iOS`

**Gặp:** `./gradlew linkDebugFrameworkIosSimulatorArm64` thành công, nhưng build app trong Xcode vẫn lỗi
`Undefined symbols: _sqlite3_*`.

**Vì sao:** framework `Shared` là **static** (`isStatic = true`). Framework tĩnh không mang theo thư viện hệ thống nó cần;
bước link cuối cùng do target app trong Xcode làm, và target đó phải tự link `libsqlite3`.

**Sửa:** thêm `-lsqlite3` vào `OTHER_LDFLAGS` của target app (hoặc thêm `libsqlite3.tbd` vào "Link Binary With Libraries").

```yaml
# iosApp/project.yml (XcodeGen)
OTHER_LDFLAGS: ["$(inherited)", "-framework", "Shared", "-lsqlite3"]
```

**Gặp ở:** [`PocketNotes/iosApp/project.yml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/iosApp/project.yml) · [`RateWatch/shared/src/iosMain/kotlin/dev/ratewatch/di/PlatformModule.ios.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/iosMain/kotlin/dev/ratewatch/di/PlatformModule.ios.kt)

### K660 · Tên bảng trùng tên model → Swift thấy `Note_`

`🟡 đã làm app` · `KMP` · `iOS`

**Gặp:** trong Swift, model của bạn tên `Note_` thay vì `Note`; hoặc Kotlin phải viết tên đầy đủ package để phân biệt hai
class `Note`.

**Vì sao:** SQLDelight sinh một class public cho mỗi bảng, đặt theo **tên bảng**. Bảng `note` sinh class `Note`, đụng
với `dev.pocketnotes.model.Note`. Trong framework iOS mọi tên nằm chung một chỗ, nên một cái bị đổi thành `Note_`.

**Sửa:** đặt tên bảng khác tên model (`noteRecord`, `tripRow`…), rồi map sang model trong repository.

```sql
CREATE TABLE note (...);          -- ❌ sinh class Note
CREATE TABLE noteRecord (...);    -- ✅ sinh class NoteRecord
```

**Gặp ở:** [`PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/Note.sq`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/Note.sq)

### K661 · `Unresolved reference: noteRecordQueries`

`🟢 mới học` · `KMP`

**Gặp:** tìm `db.noteRecordQueries` không thấy, dù bảng tên `noteRecord`.

**Vì sao:** class query đặt theo tên **file** `.sq`, không theo tên bảng. `Note.sq` sinh `NoteQueries`, truy cập bằng
`db.noteQueries`. Class dòng dữ liệu thì theo tên bảng (`NoteRecord`).

**Sửa:** gõ `db.` rồi để IDE gợi ý, hoặc nhớ quy tắc: file → `XxxQueries`, bảng → class dòng.

```kotlin
private val queries = NotesDb(driver).noteQueries     // từ Note.sq
```

**Gặp ở:** [`PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/data/NoteRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/data/NoteRepository.kt)

### K662 · `Unresolved reference: NotesDb` (code SQLDelight không được sinh)

`🟢 mới học` · `KMP` · `Gradle`

**Gặp:** viết xong file `.sq`, class database và query vẫn đỏ; build báo `Unresolved reference`.

**Vì sao:** thường do một trong ba: file `.sq` không nằm dưới `src/commonMain/sqldelight/<thư mục theo package>`; khối
`sqldelight { databases { create("NotesDb") { packageName… } } }` thiếu hoặc khác tên; hoặc IDE chưa chạy bước sinh code.

**Sửa:** đặt file đúng chỗ (ví dụ `src/commonMain/sqldelight/dev/pocketnotes/db/Note.sq`), khai báo database, rồi chạy
`./gradlew :shared:generateCommonMainNotesDbInterface` hoặc build lại. Lỗi cú pháp SQL cũng làm bước này thất bại: đọc
lỗi của task đó trước.

### K663 · Import `com.squareup.sqldelight…` không tìm thấy

`🟢 mới học` · `KMP` · `Gradle`

**Gặp:** copy code từ bài viết cũ: `Unresolved reference: squareup` ở `import com.squareup.sqldelight.android.AndroidSqliteDriver`.

**Vì sao:** SQLDelight 1.x dùng group `com.squareup.sqldelight`. Từ 2.0 mọi thứ chuyển sang `app.cash.sqldelight` (cả
plugin, dependency lẫn package).

**Sửa:** dùng toàn bộ 2.x, cùng một phiên bản cho plugin và mọi driver.

```kotlin
import com.squareup.sqldelight.android.AndroidSqliteDriver            // ❌ 1.x
import app.cash.sqldelight.driver.android.AndroidSqliteDriver         // ✅ 2.x
```

### K664 · `ResultSet returned null for …` khi gọi `executeAsOne()`

`🟢 mới học` · `KMP`

**Gặp:** `NullPointerException: ResultSet returned null for selectById` khi mở một id đã bị xoá.

**Vì sao:** `executeAsOne()` đòi **đúng một** dòng. Không có dòng nào là ném lỗi; nhiều hơn một dòng cũng ném lỗi.

**Sửa:** dữ liệu có thể không tồn tại thì dùng `executeAsOneOrNull()` và xử lý `null`.

```kotlin
tripQueries.selectById(id).executeAsOne()            // ❌ crash nếu đã xoá
tripQueries.selectById(id).executeAsOneOrNull()      // ✅ null = không có
```

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt)

### K665 · Query chạy trên main thread

`🟢 mới học` · `KMP` · `Android`

**Gặp:** cuộn giật, mở màn chậm, ANR khi DB lớn; StrictMode báo `DiskReadViolation`. Trên iOS bản đồ khựng lại.

**Vì sao:** lời gọi SQLite **chặn** thread đang chạy. `executeAsList()` gọi từ composable, từ `viewModelScope.launch`
(mặc định `Dispatchers.Main`) hay từ Swift trên main thread là chặn UI.

**Sửa:** mọi lời gọi DB trong repository bọc `withContext(Dispatchers.IO)` (hoặc `Default`); hàm repository là `suspend`
hoặc trả `Flow`, để chỗ gọi không phải nhớ.

```kotlin
private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

override suspend fun loadTrip(tripId: Long): Trip? =
    io { tripQueries.selectById(tripId, ::toTrip).executeAsOneOrNull() }
```

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt)

Xem thêm: K666, K687

### K666 · `asFlow().mapToList(...)` vẫn query trên main thread

`🟡 đã làm app` · `KMP`

**Gặp:** đã dùng `Flow`, vẫn thấy query chạy trên main thread khi danh sách cập nhật.

**Vì sao:** `asFlow()` chỉ phát tín hiệu "bảng đã đổi"; câu query thật chạy trong `mapToList(context)`. Truyền context của
main (hoặc context rỗng rồi collect trên main) thì query chạy trên main.

**Sửa:** luôn truyền dispatcher nền cho `mapToList` / `mapToOne` / `mapToOneOrNull`.

```kotlin
queries.selectAll().asFlow().mapToList(Dispatchers.Default)   // ✅ query chạy trên Default
```

**Gặp ở:** [`PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/data/NoteRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/data/NoteRepository.kt)

### K667 · `Unresolved reference: IO` trong `commonMain`

`🟡 đã làm app` · `KMP`

**Gặp:** `Dispatchers.IO` đỏ trong `commonMain`, dù trên Android vẫn dùng được.

**Vì sao:** `Dispatchers.IO` chỉ có trong code chung từ kotlinx.coroutines 1.7. Bản cũ hơn chỉ có nó ở JVM / Android.

**Sửa:** nâng kotlinx.coroutines (≥ 1.7). Nếu chưa nâng được thì dùng `Dispatchers.Default` trong code chung, hoặc nhận
dispatcher qua constructor (dễ test hơn nữa).

```kotlin
class SqlNoteRepository(
    driver: SqlDriver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,   // test truyền dispatcher khác
) : NoteRepository
```

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt)

### K668 · `last_insert_rowid()` trả id của dòng khác

`🔴 khó, hiếm nhưng đau` · `KMP` · `iOS`

**Gặp:** tạo chuyến mới xong, app ghi điểm GPS vào **chuyến khác**; hoặc id trả về là 0. Chỉ thấy trên iOS hoặc khi ghi
song song.

**Vì sao:** `last_insert_rowid()` thuộc về **connection**. Driver native có pool nhiều connection; câu `INSERT` và câu
`SELECT last_insert_rowid()` chạy riêng có thể nằm trên hai connection khác nhau.

**Sửa:** chạy cả hai trong **một** transaction (một transaction = một connection).

```kotlin
tripQueries.insertTrip(startedAt)
val id = tripQueries.lastInsertId().executeAsOne()        // ❌ có thể khác connection
val id = tripQueries.transactionWithResult {              // ✅
    tripQueries.insertTrip(startedAt)
    tripQueries.lastInsertId().executeAsOne()
}
```

**Gặp ở:** [`TripLog/shared/src/commonMain/sqldelight/dev/triplog/db/Trip.sq`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/sqldelight/dev/triplog/db/Trip.sq) · [`PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/data/NoteRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/data/NoteRepository.kt)

### K669 · Chèn từng dòng một → chậm và tốn pin

`🟡 đã làm app` · `KMP`

**Gặp:** ghi 1.000 dòng mất vài giây; app ghi GPS mỗi giây làm máy nóng, pin tụt.

**Vì sao:** ngoài transaction, mỗi `INSERT` là một transaction riêng: một lần commit + đồng bộ xuống đĩa (fsync).

**Sửa:** gom thành lô và ghi trong **một** transaction: cả lô chỉ một lần đồng bộ đĩa.

```kotlin
pointQueries.transaction {
    points.forEach { p -> pointQueries.insertPoint(tripId, p.segment.toLong(), p.latitude, p.longitude, ...) }
}
```

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt)

### K670 · Danh sách nháy rỗng khi làm mới dữ liệu

`🟡 đã làm app` · `KMP`

**Gặp:** mỗi lần tải lại tỷ giá, danh sách chớp trắng một nhịp; đôi khi thấy tỷ giá mới nhưng giờ cập nhật cũ.

**Vì sao:** `asFlow()` query lại sau **mỗi** câu lệnh đổi bảng. "Xoá hết rồi chèn lại" không có transaction thì UI kịp
thấy trạng thái giữa chừng (đã xoá, chưa chèn).

**Sửa:** gói các bước thay dữ liệu vào một transaction: flow chỉ phát một lần, sau commit, với dữ liệu đầy đủ.

```kotlin
db.rateQueries.transaction {
    db.rateQueries.deleteRates(base)
    rates.forEach { (code, rate) -> db.rateQueries.insertRate(base, code, rate) }
    db.rateQueries.upsertTable(base, now)
}
```

**Gặp ở:** [`RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt)

### K671 · Gọi mạng bên trong transaction

`🟡 đã làm app` · `KMP`

**Gặp:** trong lúc tải dữ liệu (mạng chậm), mọi thao tác ghi khác bị treo; có lúc `database is locked`.

**Vì sao:** transaction giữ khoá ghi suốt thời gian nó chạy. Đặt request mạng (vài giây) bên trong là giữ khoá vài giây.

**Sửa:** tải xong ở ngoài, rồi mới mở transaction chỉ để ghi.

```kotlin
val response = api.latest(base)          // ✅ mạng trước, ngoài transaction
withContext(Dispatchers.Default) {
    db.rateQueries.transaction { /* chỉ ghi */ }
}
```

**Gặp ở:** [`RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt)

### K672 · Gọi hàm `suspend` trong `transaction { }` của SQLDelight

`🟢 mới học` · `KMP`

**Gặp:** `Suspension functions can be called only within coroutine body` khi gọi `api.fetch()` hay `delay()` trong khối
`transaction { }`.

**Vì sao:** với driver đồng bộ (Android, native, JDBC), lambda của `transaction` là hàm thường, chạy liền một mạch trên
thread hiện tại. Không được treo giữa transaction.

**Sửa:** làm phần `suspend` trước, đưa kết quả vào transaction (K671). Cả khối transaction đặt trong
`withContext(Dispatchers.IO)`.

### K673 · Thêm `WHERE … IS NOT NULL` thì SQLDelight sinh class mới

`🟡 đã làm app` · `KMP`

**Gặp:** query `selectFinished` trả về `SelectFinished` chứ không phải `Trip`, code map bị nhân đôi; hoặc type mismatch
khi gán vào biến kiểu bảng.

**Vì sao:** SQLDelight suy kiểu cột từ câu query. `WHERE endedAtMillis IS NOT NULL` làm cột đó thành **không null** trong
kết quả, khác class của bảng (cột nullable), nên nó sinh class riêng cho query.

**Sửa:** truyền hàm mapper vào query (`selectFinished(::toTrip)`) để mọi query cùng ra một kiểu domain.

```kotlin
override fun observeTrips(): Flow<List<Trip>> =
    tripQueries.selectFinished(::toTrip).asFlow().mapToList(Dispatchers.IO)
```

**Gặp ở:** [`TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/TripLog/shared/src/commonMain/kotlin/dev/triplog/data/TripRepository.kt)

### K674 · Thêm `AS Instant` vào cột thì constructor database đòi adapter

`🟡 đã làm app` · `KMP`

**Gặp:** sau khi viết `updatedAt INTEGER AS Instant NOT NULL`, `NotesDb(driver)` báo `No value passed for parameter
'noteRecordAdapter'`.

**Vì sao:** cột có kiểu tuỳ chỉnh cần một `ColumnAdapter` để đổi qua lại với kiểu SQLite. SQLDelight bắt bạn truyền
adapter cho từng bảng có cột như vậy khi tạo database.

**Sửa:** viết `ColumnAdapter` và truyền qua `Adapter` sinh sẵn. Hoặc giữ cột kiểu cơ bản (`INTEGER`) và map trong
repository như PocketNotes, đơn giản hơn khi chỉ có vài cột.

```kotlin
val instantAdapter = object : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long) = Instant.fromEpochMilliseconds(databaseValue)
    override fun encode(value: Instant) = value.toEpochMilliseconds()
}
val db = NotesDb(driver, noteRecordAdapter = NoteRecord.Adapter(updatedAtAdapter = instantAdapter))
```

### K675 · Lưu thời gian dạng chuỗi giờ địa phương

`🟡 đã làm app` · `KMP`

**Gặp:** sắp xếp theo thời gian sai sau khi đổi múi giờ hoặc qua giờ mùa hè; "hôm qua" tính sai; so sánh chuỗi ngày ra
thứ tự lạ.

**Vì sao:** chuỗi như `"16/10/2026 08:00"` không có múi giờ và không so sánh được theo thứ tự thời gian.

**Sửa:** lưu **epoch millis UTC** (`INTEGER`), chỉ đổi sang giờ địa phương lúc hiển thị, với múi giờ là tham số.

```sql
updatedAtMillis INTEGER NOT NULL    -- ✅ Instant.toEpochMilliseconds()
```

**Gặp ở:** [`PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/Note.sq`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/Note.sq)

### K676 · `Unresolved reference: JdbcSqliteDriver` trong `commonTest`

`🟡 đã làm app` · `KMP`

**Gặp:** test repository viết trong `commonTest` không biên dịch được vì `JdbcSqliteDriver`.

**Vì sao:** `JdbcSqliteDriver` (artifact `sqlite-driver`) chỉ chạy trên JVM. `commonTest` còn được biên dịch cho iOS.

**Sửa:** đặt test dùng DB thật trong `androidUnitTest` (hoặc `jvmTest`), dependency `sqldelight.sqlite.driver` khai báo ở
source set đó. Muốn chạy trên cả iOS thì viết `expect fun testDriver(): SqlDriver` với `actual` dùng `JdbcSqliteDriver`
(JVM) và `inMemoryDriver` (native).

```kotlin
androidUnitTest.dependencies {
    implementation(libs.sqldelight.sqlite.driver)    // JdbcSqliteDriver, JVM only
}
```

**Gặp ở:** [`PocketNotes/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/build.gradle.kts) · [`PocketNotes/shared/src/androidUnitTest/kotlin/dev/pocketnotes/NoteRepositoryTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/androidUnitTest/kotlin/dev/pocketnotes/NoteRepositoryTest.kt)

### K677 · Test với DB trong bộ nhớ: `no such table: noteRecord`

`🟢 mới học` · `KMP`

**Gặp:** `SQLiteException: [SQLITE_ERROR] SQL error or missing database (no such table: noteRecord)` ngay câu query đầu
trong test.

**Vì sao:** `AndroidSqliteDriver` và `NativeSqliteDriver` nhận `Schema` và tự tạo bảng. `JdbcSqliteDriver(IN_MEMORY)` thì
không: nó mở một DB trống.

**Sửa:** gọi `Schema.create(driver)` sau khi tạo driver trong test.

```kotlin
private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
@BeforeTest fun setUp() { NotesDb.Schema.create(driver) }       // ✅
```

**Gặp ở:** [`PocketNotes/shared/src/androidUnitTest/kotlin/dev/pocketnotes/NoteRepositoryTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/androidUnitTest/kotlin/dev/pocketnotes/NoteRepositoryTest.kt)

### K678 · Mở hai driver vào cùng một file DB

`🔴 khó, hiếm nhưng đau` · `KMP`

**Gặp:** `database is locked` lúc ngẫu nhiên; dữ liệu vừa ghi ở màn này không hiện ở màn kia (flow không phát).

**Vì sao:** mỗi lần tạo `SqlDriver` là mở thêm kết nối riêng. Cơ chế "bảng đổi → query lại" của SQLDelight chỉ biết về
lệnh ghi qua **cùng** driver; ghi qua driver khác thì flow bên này không biết.

**Sửa:** một driver cho cả app (Koin `single`), mọi repository dùng chung.

```kotlin
actual val platformModule: Module = module {
    single<SqlDriver> { createDriver(androidContext()) }   // ✅ single, không phải factory
}
```

**Gặp ở:** [`PocketNotes/shared/src/androidMain/kotlin/dev/pocketnotes/di/PlatformModule.android.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/androidMain/kotlin/dev/pocketnotes/di/PlatformModule.android.kt)

### K679 · Tạo driver SQLDelight trong `commonMain`

`🟢 mới học` · `KMP`

**Gặp:** `Unresolved reference: AndroidSqliteDriver` trong `commonMain`; hoặc không biết lấy `Context` ở đâu trong code chung.

**Vì sao:** driver là phần riêng của từng nền tảng: Android cần `Context` để tìm thư mục dữ liệu, iOS thì không.
`commonMain` chỉ biết interface `SqlDriver`.

**Sửa:** code chung nhận `SqlDriver` qua constructor; mỗi nền tảng tạo driver của mình trong `androidMain` / `iosMain`
(qua `expect`/`actual` hoặc module Koin riêng).

```kotlin
// androidMain
fun createDriver(context: Context): SqlDriver = AndroidSqliteDriver(NotesDb.Schema, context, "notes.db")
// iosMain
fun createDriver(): SqlDriver = NativeSqliteDriver(NotesDb.Schema, "notes.db")
```

**Gặp ở:** [`PocketNotes/shared/src/androidMain/kotlin/dev/pocketnotes/di/PlatformModule.android.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/androidMain/kotlin/dev/pocketnotes/di/PlatformModule.android.kt) · [`PocketNotes/shared/src/iosMain/kotlin/dev/pocketnotes/di/PlatformModule.ios.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/iosMain/kotlin/dev/pocketnotes/di/PlatformModule.ios.kt)

### K680 · Sắp xếp vừa bằng `ORDER BY` vừa bằng Kotlin

`🟡 đã làm app` · `KMP`

**Gặp:** thứ tự hiển thị lúc đúng lúc sai: ghi chú ghim không lên đầu, hoặc Android và iOS sắp khác nhau.

**Vì sao:** một luật (ví dụ "ghim lên đầu, rồi mới nhất") được viết hai lần ở hai nơi (SQL và Kotlin). Sửa một chỗ, quên
chỗ kia.

**Sửa:** chọn **một** nơi giữ luật. Luật nghiệp vụ có test thì để trong Kotlin (`sortNotes`), query trả về không sắp xếp.
Chỉ dùng `ORDER BY` khi cần cho phân trang / `LIMIT`.

**Gặp ở:** [`PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/Note.sq`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/Note.sq)

### K681 · `INSERT OR REPLACE` xoá mất dữ liệu liên quan

`🔴 khó, hiếm nhưng đau` · `KMP`

**Gặp:** cập nhật một chuyến đi bằng `INSERT OR REPLACE` thì toàn bộ điểm GPS của chuyến biến mất; hoặc các cột không
có trong câu lệnh bị về giá trị mặc định.

**Vì sao:** khi trùng khoá, `REPLACE` **xoá dòng cũ rồi chèn dòng mới**. Khoá ngoại `ON DELETE CASCADE` chạy theo, và cột
không ghi trong câu lệnh nhận `DEFAULT`.

**Sửa:** chỉ dùng `INSERT OR REPLACE` khi câu lệnh ghi **đủ mọi cột** và không có bảng con. Còn lại dùng `UPDATE`, hoặc
`INSERT ... ON CONFLICT(id) DO UPDATE SET ...` (cần dialect SQLite 3.24+, K682).

```sql
INSERT INTO noteRecord(id, title, body, updatedAtMillis, pinned) VALUES (?, ?, ?, ?, ?)
ON CONFLICT(id) DO UPDATE SET title = excluded.title, body = excluded.body;
```

**Gặp ở:** [`PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/Note.sq`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/sqldelight/dev/pocketnotes/db/Note.sq) (`upsert` ghi đủ mọi cột, bảng không có bảng con)

### K682 · SQLDelight báo lỗi cú pháp với `ON CONFLICT`, `RETURNING`, window function

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** câu SQL chạy được trong DB Browser nhưng SQLDelight báo lỗi cú pháp khi build; hoặc build được nhưng máy Android
cũ crash `near "RETURNING": syntax error`.

**Vì sao:** SQLDelight kiểm SQL theo một **dialect** (mặc định là SQLite cũ, hợp với mọi máy Android). Cú pháp mới hơn cần
khai báo dialect mới hơn, và máy chạy app phải có SQLite đủ mới.

**Sửa:** khai báo dialect trong khối database (ví dụ `dialect("app.cash.sqldelight:sqlite-3-24-dialect:<phiên bản
SQLDelight>")`), và đối chiếu `minSdk` với phiên bản SQLite đi kèm Android. Không chắc thì viết lại bằng cú pháp cũ.

### K683 · Room: `Cannot find implementation for AppDatabase. AppDatabase_Impl does not exist`

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** app crash lúc tạo database với `RuntimeException: Cannot find implementation for dev.app.AppDatabase.
AppDatabase_Impl does not exist`.

**Vì sao:** code của Room được sinh bởi KSP. Trong KMP, KSP chạy **theo từng target**; khai báo `ksp(libs.room.compiler)`
kiểu Android thuần hoặc chỉ cho `commonMain` thì không target nào được sinh code.

**Sửa:** áp plugin KSP và plugin Room, thêm compiler cho từng target.

```kotlin
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}
```

### K684 · Room KMP: build iOS báo thiếu `@ConstructedBy`

`🟡 đã làm app` · `KMP` · `iOS`

**Gặp:** Android chạy được, build iOS lỗi KSP liên quan tới `RoomDatabaseConstructor`; hoặc IDE báo `expect` không có
`actual`.

**Vì sao:** ngoài Android không có reflection để tìm `AppDatabase_Impl`. Room KMP cần một `expect object` để nó sinh
`actual` cho từng nền tảng, nối qua `@ConstructedBy`.

**Sửa:** khai báo đúng mẫu của Room KMP. Không tự viết `actual`: Room sinh nó.

```kotlin
@Database(entities = [TripEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() { abstract fun tripDao(): TripDao }

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
```

### K685 · Room KMP: quên `setDriver` và dispatcher khi dựng database

`🟡 đã làm app` · `KMP`

**Gặp:** iOS crash ngay khi build database, báo chưa có SQLite driver; hoặc query chạy trên thread không mong muốn.

**Vì sao:** trên Android Room tự dùng SQLite của hệ thống; ngoài Android bạn phải chọn driver (`BundledSQLiteDriver` hoặc
`NativeSQLiteDriver`). Dispatcher cho query cũng nên khai báo rõ.

**Sửa:** dựng database ở code chung với driver và dispatcher rõ ràng; phần nền tảng chỉ trả về builder (đường dẫn file).

```kotlin
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
```

### K686 · Room KMP: DAO không `suspend` không dùng được trên iOS

`🟡 đã làm app` · `KMP`

**Gặp:** DAO có `fun getAll(): List<TripEntity>` biên dịch được cho Android nhưng lỗi khi thêm target iOS.

**Vì sao:** Room KMP chỉ hỗ trợ DAO chặn (blocking) trên Android / JVM. Code chung cho mọi target phải là `suspend` hoặc
trả `Flow`.

**Sửa:** hàm đọc một lần → `suspend`; hàm theo dõi → `Flow`.

```kotlin
@Dao
interface TripDao {
    @Query("SELECT * FROM TripEntity") suspend fun getAll(): List<TripEntity>     // ✅
    @Query("SELECT * FROM TripEntity") fun observeAll(): Flow<List<TripEntity>>    // ✅
}
```

### K687 · Room: `Cannot access database on the main thread`

`🟢 mới học` · `Android`

**Gặp:** `IllegalStateException: Cannot access database on the main thread since it may potentially lock the UI for a
long period of time.`

**Vì sao:** Room trên Android chủ động chặn truy vấn chặn trên main thread.

**Sửa:** dùng DAO `suspend` / `Flow` (Room tự chạy nền), gọi từ coroutine. Đừng dùng `allowMainThreadQueries()` ngoài test.

Xem thêm: K665

### K688 · Room: `Room cannot verify the data integrity`

`🟢 mới học` · `Android` · `KMP`

**Gặp:** `IllegalStateException: Room cannot verify the data integrity. Looks like you've changed schema but forgot to
update the version number. You can simply fix this by increasing the version number.`

**Vì sao:** Room lưu mã băm của schema trong DB. Bạn đổi `@Entity` mà `version` trong `@Database` giữ nguyên, nên schema
thật và mã băm không khớp.

**Sửa:** tăng `version` **và** cung cấp migration (K689). Trên máy dev có thể gỡ app cài lại, nhưng người dùng thật thì
không.

### K689 · Room: `A migration from 1 to 2 was required but not found`

`🟡 đã làm app` · `Android` · `KMP`

**Gặp:** tăng `version` xong, người dùng cũ crash: `IllegalStateException: A migration from 1 to 2 was required but not
found.` Hoặc "sửa" bằng `fallbackToDestructiveMigration()` và người dùng mất sạch dữ liệu.

**Vì sao:** Room cần biết cách đi từ mỗi phiên bản cũ lên phiên bản mới. Destructive migration nghĩa là **xoá DB tạo lại**.

**Sửa:** thêm `Migration(1, 2)` viết tay, hoặc `@AutoMigration(from = 1, to = 2)` (cần file schema đã export, K690). Chỉ
dùng destructive cho dữ liệu cache tải lại được.

```kotlin
@Database(entities = [TripEntity::class], version = 2, autoMigrations = [AutoMigration(from = 1, to = 2)])
abstract class AppDatabase : RoomDatabase()
```

Xem thêm: K651

### K690 · Room: `Schema export directory was not provided`

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** cảnh báo (hoặc lỗi) khi build: `Schema export directory was not provided to the annotation processor so Room
cannot export the schema.` Sau đó `@AutoMigration` báo không tìm thấy schema cũ.

**Vì sao:** Room xuất schema của mỗi phiên bản ra JSON để so sánh và sinh auto migration. Không chỉ thư mục thì không có
file nào để so.

**Sửa:** áp plugin Room (`androidx.room`) và khai báo thư mục; commit thư mục đó vào git.

```kotlin
room {
    schemaDirectory("$projectDir/schemas")
}
```

### K691 · Room: `Cannot figure out how to save this field into database`

`🟢 mới học` · `KMP`

**Gặp:** build lỗi `Cannot figure out how to save this field into database. You can consider adding a type converter
for it.` ở field `Instant`, `List<String>`, enum lồng…

**Vì sao:** Room chỉ biết lưu kiểu cơ bản (số, chuỗi, `ByteArray`…). Kiểu khác cần `@TypeConverter`.

**Sửa:** viết converter và gắn `@TypeConverters` lên database. Danh sách thì cân nhắc tách bảng con thay vì nhét chuỗi JSON.

```kotlin
class Converters {
    @TypeConverter fun fromMillis(value: Long): Instant = Instant.fromEpochMilliseconds(value)
    @TypeConverter fun toMillis(value: Instant): Long = value.toEpochMilliseconds()
}
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase()
```

### K692 · Chọn SQLDelight hay Room cho KMP?

`🟡 đã làm app` · `KMP`

**Gặp:** câu hỏi đầu tiên khi thêm DB vào app KMP; đổi giữa chừng thì tốn công viết lại tầng dữ liệu.

**Vì sao:** hai cách nghĩ khác nhau. SQLDelight: viết SQL trong `.sq`, sinh code Kotlin; migration là file `.sqm`. Room:
viết class `@Entity` / `@Dao` (annotation + KSP), quen với người đã làm Android; có auto migration.

**Sửa:** chọn theo đội và dữ liệu: thích kiểm soát SQL, muốn compiler kiểm câu query → SQLDelight (5 app trong repo này).
Đội đã quen Room trên Android → Room KMP (2.7+). Dù chọn gì, giấu nó sau một interface repository để core test được bằng
fake.

**Gặp ở:** [`PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/data/NoteRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/data/NoteRepository.kt) (interface `NoteRepository`, `FakeNoteRepository` cho test)

### K693 · `There are multiple DataStores active for the same file`

`🟡 đã làm app` · `Android` · `KMP`

**Gặp:** `IllegalStateException: There are multiple DataStores active for the same file: …/settings.preferences_pb. You
should either maintain your DataStore as a singleton or confirm that there is no two DataStore's active on the same file
(by confirming that the scope is cancelled).`

**Vì sao:** mỗi lần gọi `PreferenceDataStoreFactory.create…` là một instance mới. Tạo nó trong `ViewModel`, trong
composable hay trong hàm được gọi nhiều lần là có hai instance cùng file.

**Sửa:** một `DataStore` cho mỗi file trong cả process: Koin `single`, hoặc thuộc tính top-level.

```kotlin
val Context.settings: DataStore<Preferences> by preferencesDataStore(name = "settings")   // ✅ top-level, một lần

class SettingsViewModel(context: Context) : ViewModel() {
    val store = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }  // ❌ mỗi VM một bản
}
```

### K694 · Khai báo `by preferencesDataStore` bên trong class

`🟢 mới học` · `Android`

**Gặp:** lỗi giống K693 sau khi xoay máy; hoặc dữ liệu ghi ở màn này không thấy ở màn kia.

**Vì sao:** delegate `preferencesDataStore` chỉ đảm bảo một instance khi được khai báo **một lần ở top-level**. Đặt trong
`Activity` thì mỗi `Activity` mới tạo một delegate mới.

**Sửa:** khai báo ở top-level của một file (ngoài mọi class), rồi dùng qua `context.settings`.

### K695 · DataStore KMP: `File extension … does not match required extension for Preferences file`

`🟢 mới học` · `KMP`

**Gặp:** `IllegalStateException: File extension for file: …/settings.json does not match required extension for
Preferences file: preferences_pb`.

**Vì sao:** `PreferenceDataStoreFactory` kiểm đuôi file và chỉ nhận `.preferences_pb`.

**Sửa:** đặt tên file đúng đuôi; ở KMP tạo bằng `createWithPath` với đường dẫn do từng nền tảng cung cấp.

```kotlin
fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })

const val SETTINGS_FILE = "settings.preferences_pb"    // ✅ đúng đuôi
```

### K696 · Đọc DataStore bằng `runBlocking { first() }` lúc khởi động → ANR

`🟡 đã làm app` · `Android`

**Gặp:** app mở chậm, đôi khi ANR ở máy yếu; stack trace dừng ở `runBlocking` trong `onCreate` hoặc `Application`.

**Vì sao:** lần đọc đầu tiên phải mở và đọc file trên đĩa. `runBlocking` trên main thread chặn UI cho tới khi xong.

**Sửa:** đọc bất đồng bộ: expose `Flow`, UI hiện trạng thái "đang tải" (hoặc giá trị mặc định) cho tới khi có giá trị
đầu tiên. Nếu thật sự cần trước khi vẽ (theme), dùng splash screen và đọc trong coroutine.

```kotlin
val theme = runBlocking { dataStore.data.first()[THEME] }            // ❌ chặn main
val theme: StateFlow<Theme?> = dataStore.data.map { it[THEME]?.let(Theme::valueOf) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)   // ✅
```

### K697 · File DataStore hỏng làm app crash mỗi lần mở

`🔴 khó, hiếm nhưng đau` · `Android` · `KMP`

**Gặp:** vài người dùng crash ngay khi mở app với `CorruptionException` hoặc `IOException` từ DataStore, gỡ cài lại mới hết.

**Vì sao:** file có thể hỏng (hết dung lượng giữa lúc ghi, tắt máy đột ngột). Flow `data` ném lỗi; không ai bắt thì app
crash mỗi lần đọc.

**Sửa:** bắt `IOException` khi đọc và khai báo `corruptionHandler` để thay file hỏng bằng giá trị rỗng.

```kotlin
val store = PreferenceDataStoreFactory.createWithPath(
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    produceFile = { path.toPath() },
)
val settings = store.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
```

### K698 · Nhét danh sách lớn vào DataStore Preferences

`🟡 đã làm app` · `KMP`

**Gặp:** mỗi lần thêm một mục yêu thích, app khựng; file `preferences_pb` vài MB; dữ liệu cần lọc / sắp xếp phải đọc hết
ra rồi parse JSON.

**Vì sao:** DataStore ghi lại **toàn bộ file** cho mỗi lần sửa, và không có query. Nó dành cho vài cài đặt nhỏ.

**Sửa:** danh sách, lịch sử, dữ liệu cần tìm kiếm → bảng trong DB. Cài đặt nhỏ mà cần theo dõi cùng dữ liệu khác cũng có
thể là một bảng key/value trong DB (RateWatch lưu đồng tiền gốc trong bảng `setting`, đồng tiền yêu thích trong bảng
`favorite`).

**Gặp ở:** [`RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt)

### K699 · Đổi kiểu key DataStore → `ClassCastException`

`🟡 đã làm app` · `Android` · `KMP`

**Gặp:** sau khi cập nhật app, đọc cài đặt thì crash `java.lang.ClassCastException: java.lang.Integer cannot be cast to
java.lang.Long`.

**Vì sao:** bản cũ ghi `intPreferencesKey("last_trip")`, bản mới đọc `longPreferencesKey("last_trip")`: cùng tên, khác
kiểu. Dữ liệu cũ trong file vẫn là `Int`.

**Sửa:** coi tên key + kiểu là "schema": đổi kiểu thì đổi **tên** key mới và chép giá trị cũ sang một lần (một bước
migrate nhỏ khi khởi động), rồi xoá key cũ.

```kotlin
val LAST_TRIP_OLD = intPreferencesKey("last_trip")
val LAST_TRIP = longPreferencesKey("last_trip_id")    // ✅ tên mới cho kiểu mới
```

### K700 · Lưu token / mật khẩu dạng chữ thường trong DataStore hoặc SQLite

`🟡 đã làm app` · `Android` · `iOS`

**Gặp:** kiểm tra bảo mật phát hiện token đăng nhập đọc được trong file `.preferences_pb` / `.db` (máy root, bản sao
lưu, công cụ debug).

**Vì sao:** DataStore và SQLite không mã hoá. File nằm trong thư mục riêng của app, nhưng vẫn lộ qua backup hoặc máy đã
root / jailbreak.

**Sửa:** bí mật lưu bằng kho khoá của hệ thống: Keychain trên iOS, khoá trong Android Keystore để mã hoá trước khi lưu
(thư viện `security-crypto` với `EncryptedSharedPreferences` đã bị Google đánh dấu deprecated, đừng chọn cho code mới).
Trong KMP, đặt sau một `expect`/`actual` hoặc interface `SecretStore`.

---

**Nhóm trước / sau:** [13. Compose: layout, navigation, side effect](13-compose-ui.md) · [15. Mạng & JSON](15-networking.md) · [Mục lục 900 lỗi](index.md)
