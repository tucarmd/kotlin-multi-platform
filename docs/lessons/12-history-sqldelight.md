# Lưu lịch sử với SQLDelight

Bấm "Lưu lần đo" để chụp lại pin, vị trí, mạng và độ trễ lúc đó, rồi xem lại trên màn History.
SQLDelight cho bạn **viết SQL thật** trong file `.sq` và sinh ra code Kotlin an toàn kiểu dùng được trong `commonMain`.
Chỉ phần *driver* (cách mở file SQLite) là khác theo nền tảng.

## Mục tiêu

- Database `PulseDb` với bảng `snapshot`, chạy trên Android và iOS.
- `SqlHistoryRepository` cài đặt `HistoryRepository`; `PulseMonitor` chỉ biết interface.
- Màn History liệt kê các lần đo, mới nhất ở trên.

## Cài

Thêm plugin `app.cash.sqldelight` vào `shared/build.gradle.kts`, rồi khai báo database:

```kotlin
sqldelight {
    databases {
        create("PulseDb") { packageName.set("dev.devicepulse.db") }
    }
}
```

| Thư viện | Source set |
|---|---|
| `app.cash.sqldelight:coroutines-extensions` | `commonMain` |
| `app.cash.sqldelight:android-driver` | `androidMain` |
| `app.cash.sqldelight:native-driver` | `iosMain` |

## Viết SQL

`shared/src/commonMain/sqldelight/dev/devicepulse/db/Snapshot.sq`:

```sql
CREATE TABLE snapshot (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  taken_at TEXT NOT NULL,
  battery_percent INTEGER,
  charging TEXT,
  latitude REAL,
  longitude REAL,
  accuracy_m REAL,
  connection TEXT,
  latency_ms INTEGER
);

insert:
INSERT INTO snapshot(taken_at, battery_percent, charging, latitude, longitude, accuracy_m, connection, latency_ms)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

selectAll:
SELECT * FROM snapshot ORDER BY taken_at DESC;

deleteAll:
DELETE FROM snapshot;
```

Build lại → SQLDelight sinh `PulseDb`, `SnapshotQueries` và data class `Snapshot`. Cột sensor để **nullable**: lần đo mà vị trí
đang `PermissionDenied` vẫn lưu được phần còn lại. `taken_at` lưu `Instant.toString()` (ISO-8601, UTC) nên sắp xếp chuỗi cũng đúng thứ tự thời gian.

## Driver theo nền tảng

```kotlin
// commonMain
interface DriverFactory { fun create(): SqlDriver }

// androidMain
class AndroidDriverFactory(private val context: Context) : DriverFactory {
    override fun create(): SqlDriver = AndroidSqliteDriver(PulseDb.Schema, context, "pulse.db")
}

// iosMain
class IosDriverFactory : DriverFactory {
    override fun create(): SqlDriver = NativeSqliteDriver(PulseDb.Schema, "pulse.db")
}
```

Đây là cách "interface + truyền vào" của [bài 4](04-share-code.md). Koin: `platformModule` cung cấp `DriverFactory`,
`commonModule` thêm `single { PulseDb(get<DriverFactory>().create()) }`.

## Repository

```kotlin
// commonMain
class SqlHistoryRepository(db: PulseDb) : HistoryRepository {
    private val q = db.snapshotQueries

    override fun observe(): Flow<List<Snapshot>> =
        q.selectAll().asFlow().mapToList(Dispatchers.Default)

    override suspend fun save(state: DashboardState): Unit = withContext(Dispatchers.Default) {
        val battery = (state.battery as? Reading.Available)?.value
        val fix = (state.location as? Reading.Available)?.value
        val net = (state.network as? Reading.Available)?.value
        val latency = (state.latencyMs as? Reading.Available)?.value
        q.insert(
            taken_at = Clock.System.now().toString(),
            battery_percent = battery?.levelPercent?.toLong(),
            charging = battery?.charging?.name,
            latitude = fix?.latitude,
            longitude = fix?.longitude,
            accuracy_m = fix?.accuracyMeters,
            connection = net?.type?.name,
            latency_ms = latency,
        )
    }
}
```

- Chỉ giá trị `Reading.Available` được lưu; `Loading`, `Error`, `PermissionDenied`, `Unsupported` thành `null`.
- `asFlow()` phát lại mỗi khi bảng `snapshot` đổi → màn History tự cập nhật sau mỗi lần lưu.
- `INTEGER` trong SQL thành `Long` trong Kotlin, `REAL` thành `Double`.

## Làm trên Device Pulse

1. Thêm `private val history: HistoryRepository` vào constructor của `PulseMonitor` (và `PulseCore`), khai báo `single<HistoryRepository> { SqlHistoryRepository(get()) }` trong Koin.
2. Thêm vào `PulseMonitor`: `fun saveSnapshot() { scope.launch { history.save(state.value) } }` và
   `val snapshots = history.observe().stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())`.
3. Nút "Lưu lần đo" trên Dashboard gọi `saveSnapshot()`; nút lịch sử trên thanh trên cùng mở màn Lịch sử (route `history` / `Route.history`).
4. Màn History viết 2 lần: `LazyColumn` (Compose) và `List` (SwiftUI) các `Snapshot` (iOS đọc `snapshots` qua `for await` như `state`), mỗi dòng hiện giờ đo, `battery_percent`%, loại mạng, `latency_ms` ms; giá trị `null` hiện "—".
   Query `deleteAll` để dành cho nút "Xoá hết" nếu bạn muốn làm thêm.

## Kiểm tra

- [ ] Android: lưu 3 lần đo, tắt hẳn app (vuốt khỏi Recents), mở lại → History vẫn còn đủ 3 dòng, mới nhất ở trên.
- [ ] Android: lưu khi đang chế độ máy bay → dòng đó có loại mạng `None`, latency "—".
- [ ] iOS Simulator: lưu vài lần, dừng app trong Xcode, chạy lại → dữ liệu vẫn còn.
- [ ] iOS Simulator: pin hiện `Unsupported` → cột pin của dòng lưu là "—", không crash.
- [ ] Lưu xong, màn History cập nhật ngay mà không cần mở lại.

Kẹt? Xem [Lỗi hay gặp](../reference/troubleshooting.md).

## Tiếp theo?

- [Bài 13: test](13-testing.md).

## Docs gốc

[SQLDelight: Multiplatform](https://sqldelight.github.io/sqldelight/latest/multiplatform_sqlite/) · [SQLDelight: Coroutines](https://sqldelight.github.io/sqldelight/latest/multiplatform_sqlite/coroutines/) · [Share data access layer: Ktor + SQLDelight](https://kotlinlang.org/docs/multiplatform/multiplatform-ktor-sqldelight.html)
