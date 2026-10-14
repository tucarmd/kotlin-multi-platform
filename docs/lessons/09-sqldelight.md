# Lưu dữ liệu với SQLDelight

SQLDelight cho bạn **viết SQL thật** trong file `.sq`, rồi sinh ra code Kotlin an toàn kiểu dùng được trong `commonMain`.
Chỉ phần *driver* (cách mở file SQLite) là khác theo nền tảng.

## Mục tiêu

- Thói quen và các ngày đã tick được lưu vào SQLite trên máy.
- `HabitRepository` thật thay bản in-memory, ViewModel không phải sửa.

## Cài

Trong `build.gradle.kts` của `composeApp`: thêm plugin `app.cash.sqldelight`, rồi khai báo database:

```kotlin
sqldelight {
    databases {
        create("HabitDb") { packageName.set("dev.habittick.db") }
    }
}
```

Thư viện: `coroutines-extensions` ở `commonMain`; driver `android-driver` ở `androidMain`, `native-driver` ở `iosMain`,
`sqlite-driver` ở `desktopMain`.

## Viết SQL

`src/commonMain/sqldelight/dev/habittick/db/Habit.sq`:

```sql
CREATE TABLE habit (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL
);

CREATE TABLE done_day (
  habit_id INTEGER NOT NULL,
  day TEXT NOT NULL,
  PRIMARY KEY (habit_id, day)
);

selectAll:
SELECT * FROM habit;

selectDays:
SELECT day FROM done_day WHERE habit_id = ?;

insertHabit:
INSERT INTO habit(title) VALUES (?);

markDone:
INSERT OR IGNORE INTO done_day(habit_id, day) VALUES (?, ?);

unmarkDone:
DELETE FROM done_day WHERE habit_id = ? AND day = ?;
```

Build lại → SQLDelight sinh `HabitDb` và `HabitQueries`.

## Driver theo nền tảng

```kotlin
// commonMain
interface DriverFactory { fun create(): SqlDriver }

// androidMain
class AndroidDriverFactory(private val context: Context) : DriverFactory {
    override fun create() = AndroidSqliteDriver(HabitDb.Schema, context, "habit.db")
}

// iosMain
class IosDriverFactory : DriverFactory {
    override fun create() = NativeSqliteDriver(HabitDb.Schema, "habit.db")
}

// desktopMain
class DesktopDriverFactory : DriverFactory {
    override fun create(): SqlDriver =
        JdbcSqliteDriver("jdbc:sqlite:habit.db").also { HabitDb.Schema.create(it) }
}
```

Đây là cách 2 của [bài 4](04-share-code.md): interface + truyền vào.

## Làm trên app MVP

```kotlin
class SqlHabitRepository(db: HabitDb) : HabitRepository {
    private val q = db.habitQueries

    override fun observeHabits(): Flow<List<Habit>> =
        q.selectAll().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { Habit(it.id, it.title, q.selectDays(it.id).executeAsList().toSet()) }
        }

    override suspend fun toggleToday(id: Long) {
        val today = todayIso()
        if (today in q.selectDays(id).executeAsList()) q.unmarkDone(id, today) else q.markDone(id, today)
    }
}
```

!!! warning "Danh sách không tự cập nhật khi tick?"
    `asFlow()` của `selectAll` chỉ phát lại khi bảng `habit` đổi. Tick đổi bảng `done_day`, nên hãy cho flow
    lắng nghe cả hai — ví dụ viết thêm query JOIN `habit` với `done_day` và observe query đó.

## Kiểm tra

- [ ] Thêm 2 thói quen, tick 1 cái, tắt hẳn app, mở lại → vẫn còn.
- [ ] Streak hiện đúng sau khi tick nhiều ngày (sửa ngày máy để thử, hoặc chờ test ở bài 12).

## Tiếp theo?

- [Bài 10: gọi API với Ktor](10-ktor.md).

## Docs gốc

[Share data access layer: Ktor + SQLDelight](https://kotlinlang.org/docs/multiplatform/multiplatform-ktor-sqldelight.html) · [SQLDelight docs](https://sqldelight.github.io/sqldelight/)
