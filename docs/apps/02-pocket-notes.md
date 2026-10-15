# App 2 · Pocket Notes 📝

> Ghi chú offline: thêm, sửa, xoá, ghim, tìm kiếm. Tắt app mở lại vẫn còn.

## 1. Tóm tắt

| | |
|---|---|
| **Cấp độ** | ⭐⭐ |
| **Mới học** | SQLDelight CRUD · `Flow` tự phát khi bảng đổi · điều hướng list → edit · `kotlinx-datetime` · `platformModule` đầu tiên (driver database) |
| **Cần trước** | App 1 · [bài 8](../lessons/08-viewmodel-navigation.md), [12](../lessons/12-history-sqldelight.md) |
| **Thư mục code** | `PocketNotes/` · package `dev.pocketnotes` |
| **Thời gian** | 4–5 buổi tối |

So với app 1: thêm `data/` và lần đầu có code ở `androidMain` / `iosMain` (tạo `SqlDriver`).

## 2. Màn hình

| Màn | Hiện gì | Action |
|---|---|---|
| **Note list** | Ô tìm kiếm · ghi chú ghim ở trên · mỗi dòng: tiêu đề, 2 dòng đầu, "sửa 5 phút trước" · trạng thái rỗng | Gõ tìm · bấm dòng → Edit · vuốt xoá (có *Hoàn tác*) · nút **+** |
| **Note edit** | Ô tiêu đề, ô nội dung, nút ghim | Tự lưu khi rời màn · nút xoá |

## 3. Phạm vi

| Có | Làm sau |
|---|---|
| CRUD + ghim + tìm theo tiêu đề / nội dung | Đồng bộ cloud |
| Hoàn tác xoá trong 5 giây | Ảnh đính kèm, markdown |
| Thời gian tương đối "5 phút trước" | Nhãn / thư mục |
| Migration lần 1 (thêm cột `pinned`) | |

## 4. Kiến trúc

```mermaid
flowchart LR
  UI[NoteList / NoteEdit] -->|action| CORE[NotesCore]
  CORE -->|StateFlow&lt;NotesState&gt;| UI
  CORE --> RULES["rules: matches · sortNotes · relativeTime"]
  CORE --> REPO[NoteRepository]
  REPO --> DB[(SQLDelight · Note.sq)]
  DB -.-> AD[androidMain: AndroidSqliteDriver]
  DB -.-> ID[iosMain: NativeSqliteDriver]
```

## 5. 7 bước làm

| Bước | Ở app này làm gì |
|:-:|---|
| **B1** | `model/Note(id, title, body, pinned, updatedAt: Instant)`. `rules/`: `matches(note, query)` (bỏ dấu tiếng Việt), `sortNotes()` (ghim trước, mới trước), `relativeTime(from, now)`. |
| **B2** | `NotesCore`: `state` = `combine(repository.observeAll(), query)` → lọc + sắp xếp. Action: `search`, `save`, `delete` (giữ bản xoá 5 s để hoàn tác), `undoDelete`, `togglePin`. Test với `FakeNoteRepository`. |
| **B3** | `Note.sq`: bảng + query `selectAll`, `upsert`, `deleteById`. `NoteRepository` bọc query, `observeAll()` dùng `asFlow().mapToList()`. Migration `1.sqm` thêm cột `pinned`. |
| **B4** | `platformModule` (expect/actual): Android tạo `AndroidSqliteDriver(context)`, iOS tạo `NativeSqliteDriver`. `notesCore()` cho Swift. |
| **B5** | Navigation Compose: `list` → `edit/{id}`. `SwipeToDismissBox` để xoá, `Snackbar` có nút *Hoàn tác*. |
| **B6** | `NavigationStack` + `.swipeActions` + `.searchable`. Lưu khi `onDisappear`. |
| **B7** | `RulesTest`, `NotesCoreTest` (Turbine), `NoteRepositoryTest` ở `androidUnitTest` với `JdbcSqliteDriver` in-memory. |

## 6. Bản đồ function (dự kiến)

| Hàm | File | Làm gì | Vì sao |
|---|---|---|---|
| `matches(note, query)` | `rules/Search.kt` | So khớp không phân biệt hoa thường, bỏ dấu | Gõ "ghi chu" vẫn ra "Ghi chú" |
| `sortNotes(notes)` | `rules/Search.kt` | Ghim lên đầu, rồi `updatedAt` giảm dần | Thứ tự là luật, không để UI tự sắp |
| `relativeTime(from, now)` | `rules/Time.kt` | `Instant` → "vừa xong / 5 phút / hôm qua" | Truyền `now` vào để test được, không gọi `Clock.System` bên trong |
| `NoteRepository.observeAll()` | `data/NoteRepository.kt` | `Flow<List<Note>>` tự phát khi bảng đổi | UI không cần "reload" sau khi lưu |
| `NotesCore.delete(id)` | `core/NotesCore.kt` | Xoá, giữ bản sao 5 s cho `undoDelete()` | Hoàn tác không cần đọc lại database |
| `createDriver()` | `androidMain` / `iosMain` `di/PlatformModule.*.kt` | Tạo `SqlDriver` đúng nền tảng | Driver là thứ duy nhất khác giữa 2 máy |

Bẫy cần ghi `// TRAP:` trong code:

- Quên `-lsqlite3` trong `OTHER_LDFLAGS` của Xcode → link lỗi `_sqlite3_*` (Device Pulse đã gặp).
- Đổi schema mà không viết file `.sqm` → app cũ crash khi cập nhật.
- `Clock.System.now()` gọi trong luật → test không ổn định. Luôn truyền `now` vào.

## 7. Test

```bash
cd PocketNotes
./gradlew :shared:testDebugUnitTest :shared:iosSimulatorArm64Test
```

Case tối thiểu: tìm bỏ dấu · ghim lên đầu · xoá rồi hoàn tác · lưu ghi chú rỗng thì không tạo · migration từ schema 1 lên 2.

## 8. Xong khi

- [ ] Thêm 3 ghi chú, tắt hẳn app (vuốt khỏi đa nhiệm), mở lại vẫn còn, trên cả 2 máy
- [ ] Xoá rồi bấm *Hoàn tác* thì ghi chú quay lại đúng chỗ
- [ ] Tìm "ghi chu" ra "Ghi chú"
- [ ] Test xanh, có test migration
- [ ] KDoc `[App 2 · Bx]` mọi file · README đủ 7 mục

## 9. Tham khảo

- [kotlin-hands-on/kmp-networking-and-data-storage](https://github.com/kotlin-hands-on/kmp-networking-and-data-storage): SQLDelight + Koin
- [SQLDelight: Multiplatform](https://sqldelight.github.io/sqldelight/latest/multiplatform_sqlite/) · [Migrations](https://sqldelight.github.io/sqldelight/latest/multiplatform_sqlite/migrations/)
- Device Pulse: `data/HistoryRepository.kt`, `Snapshot.sq`

**Trước:** [App 1 · Tip Split](01-tip-split.md) · **Tiếp:** [App 3 · Rate Watch](03-rate-watch.md)
