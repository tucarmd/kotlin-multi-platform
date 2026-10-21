# App 2 · Pocket Notes 📝

> Ghi chú offline: thêm, sửa, xoá, ghim, tìm kiếm. Tắt app mở lại vẫn còn.

## Demo: làm xong trông thế nào

Ảnh chụp **app thật trong repo** (không phải mockup), chạy bằng Maestro trên emulator Pixel 8 (Android 14) và Simulator iPhone 17 (iOS 26.5), ngày 2026-10-21.
Cùng một core KMP, mỗi bên một UI native: so hai tab là thấy **logic giống hệt, giao diện theo chuẩn từng nền tảng**.

=== "🤖 Android · Jetpack Compose"

    <div class="phones phones--demo" markdown>
    <figure markdown>
    ![1. Lần đầu: chưa có ghi chú](assets/pocketnotes/android-1-empty.jpg)
    <figcaption>1. Lần đầu: chưa có ghi chú</figcaption>
    </figure>
    <figure markdown>
    ![2. 3 ghi chú, "Đi chợ" được ghim lên đầu](assets/pocketnotes/android-2-list.jpg)
    <figcaption>2. 3 ghi chú, "Đi chợ" được ghim lên đầu</figcaption>
    </figure>
    <figure markdown>
    ![3. Màn sửa: tự lưu khi quay lại](assets/pocketnotes/android-3-edit.jpg)
    <figcaption>3. Màn sửa: tự lưu khi quay lại</figcaption>
    </figure>
    <figure markdown>
    ![4. Gõ "y tuong" ra "Ý tưởng app"](assets/pocketnotes/android-4-search.jpg)
    <figcaption>4. Gõ "y tuong" ra "Ý tưởng app"</figcaption>
    </figure>
    </div>

=== "🍎 iOS · SwiftUI"

    <div class="phones phones--demo" markdown>
    <figure markdown>
    ![1. Lần đầu: chưa có ghi chú](assets/pocketnotes/ios-1-empty.jpg)
    <figcaption>1. Lần đầu: chưa có ghi chú</figcaption>
    </figure>
    <figure markdown>
    ![2. 3 ghi chú, "Đi chợ" được ghim lên đầu](assets/pocketnotes/ios-2-list.jpg)
    <figcaption>2. 3 ghi chú, "Đi chợ" được ghim lên đầu</figcaption>
    </figure>
    <figure markdown>
    ![3. Màn sửa: tự lưu khi quay lại](assets/pocketnotes/ios-3-edit.jpg)
    <figcaption>3. Màn sửa: tự lưu khi quay lại</figcaption>
    </figure>
    <figure markdown>
    ![4. Gõ "y tuong" ra "Ý tưởng app"](assets/pocketnotes/ios-4-search.jpg)
    <figcaption>4. Gõ "y tuong" ra "Ý tưởng app"</figcaption>
    </figure>
    </div>

Bước 4 là điểm khó nhất của app: **tìm không dấu** (`y tuong` khớp `Ý tưởng`) nằm trong luật chung `matches()`, nên 2 nền tảng giống nhau. Android ghim bằng ⭐, iOS bằng 📌 (vuốt từ trái sang): khác icon, cùng một hàm `togglePin`.

## Ước lượng thời gian

Cho người **đã học xong 14 bài** (hoặc app trước), gõ lại từ đầu theo 7 bước, mỗi buổi tối khoảng 2–3 giờ.
Lần đầu làm KMP thì nhân 1,5.

| Việc | Thời gian người làm |
|---|---|
| Đọc trang + xem lại SQLDelight (bài 12) | 1 giờ |
| B1 model + luật (`matches`, `sortNotes`, `relativeTime`, `planSave`) + test | 1,5 giờ |
| B2 `NotesCore` (Flow từ DB, hoàn tác xoá) | 2 giờ |
| B3 `.sq` + migration + driver Android / iOS | 2,5 giờ |
| B4 Koin `platformModule` | 30 phút |
| B5 UI Android (NavHost, vuốt xoá, Snackbar) | 2,5 giờ |
| B6 UI iOS (NavigationStack, `.searchable`, swipeActions) | 2,5 giờ |
| B7 test migration, README | 1,5 giờ |
| **Chạy + test trên 2 máy** (mục [10](#10-how-to-test)) | 1 giờ |
| **Tổng** | **≈ 14 giờ · 5 buổi tối** |

Thời gian **máy chạy**, đo trên MacBook Pro M1 Pro 16 GB, Xcode 26.5, JDK 17:

| Việc | Đo được |
|---|---|
| Build app iOS cho Simulator (`xcodebuild`, Gradle đã có cache) | 37 giây |
| Máy chạy toàn bộ unit test (JVM + iOS Simulator, build lại từ đầu bằng `--rerun-tasks`) | **23 giây** · 54 test (JVM + iOS cộng lại), 0 lỗi |
| Build + cài Android khi đã build 1 lần (`installDebug`) | dưới 30 giây |

## 1. Tóm tắt

| | |
|---|---|
| **Cấp độ** | ⭐⭐ |
| **Mới học** | SQLDelight CRUD · `Flow` tự phát khi bảng đổi · điều hướng list → edit · `kotlinx-datetime` · `platformModule` đầu tiên (driver database) |
| **Cần trước** | App 1 · [bài 8](../lessons/08-viewmodel-navigation.md), [12](../lessons/12-history-sqldelight.md) |
| **Thư mục code** | [`apps/PocketNotes/`](https://github.com/tucarmd/kotlin-multi-platform/tree/main/apps/PocketNotes) · package `dev.pocketnotes` · ✅ đã có code |
| **Thời gian** | 4–5 buổi tối · [chi tiết](#uoc-luong-thoi-gian) |

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

## 6. Bản đồ function

Bảng đầy đủ (20 hàm, có link tới đúng dòng code) nằm trong
[`apps/PocketNotes/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/README.md#ban-o-function). Các hàm chính:

| Hàm | File | Làm gì | Vì sao |
|---|---|---|---|
| `fold(text)` | `rules/Search.kt` | "ĐẶT Hàng" → "dat hang" | `đ/Đ` không phải "d + dấu", phải map tay |
| `matches(note, query)` | `rules/Search.kt` | Tìm trong tiêu đề + nội dung, bỏ dấu, không phân biệt hoa thường | Gõ "ghi chu" vẫn ra "Ghi chú" |
| `sortNotes(notes)` | `rules/Search.kt` | Ghim lên đầu, rồi `updatedAt` mới trước | Thứ tự là luật, không để UI hay SQL tự sắp |
| `relativeTime(from, now, timeZone)` | `rules/Time.kt` | Trả `RelativeTime` (vừa xong / phút / giờ / hôm qua / ngày) | Truyền `now` vào để test được; "hôm qua" tính theo ngày lịch |
| `planSave(existing, draft, now)` | `rules/Save.kt` | Quyết định Insert / Update / Delete / Skip | Ghi chú rỗng không được tạo; ghim không đẩy ghi chú lên đầu |
| `SqlNoteRepository.observeAll()` | `data/NoteRepository.kt` | `asFlow().mapToList()` | List tự cập nhật sau khi lưu |
| `NotesCore.delete(id)` / `undoDelete()` | `core/NotesCore.kt` | Xoá, giữ bản sao 5 s; hoàn tác ghi lại đúng id, ghim, thời gian | Hoàn tác không đọc lại database, ghi chú về đúng chỗ |
| `NotesCore.write(block)` | `core/NotesCore.kt` | Mọi lệnh ghi đi qua một `Mutex` | Xoá rồi hoàn tác thật nhanh không bị đảo thứ tự |
| `createDriver()` | `androidMain` / `iosMain` `di/PlatformModule.*.kt` | `AndroidSqliteDriver` / `NativeSqliteDriver` | Driver là thứ duy nhất khác giữa 2 máy |

!!! note "Code khác plan ở đâu"
    - `NoteRepository` là **interface**, bản thật là `SqlNoteRepository`, để test core bằng `FakeNoteRepository`.
    - Bảng tên `noteRecord`, database `NotesDb`. Đặt tên bảng là `note` thì SQLDelight sinh thêm một class `Note`, Swift sẽ thấy `Note_`.
    - `NotesState` là sealed: `Loading` / `Failed` / `Ready`. `NotesCore` **không** phải singleton: Android tạo trong ViewModel với `viewModelScope`, iOS lấy từ `notesCore()` với `MainScope()`.
    - Android lưu ghi chú trong `BackHandler`, không phải `onDispose` (vì `onDispose` cũng chạy khi xoay màn → lưu trùng). iOS lưu ở `onDisappear`.
    - Xoá hết tiêu đề và nội dung của một ghi chú cũ = xoá ghi chú (có hoàn tác). Ghim được ngay từ danh sách.
    - App bị kill khi đang mở màn sửa thì bản nháp chưa lưu sẽ mất.

Bẫy cần ghi `// TRAP:` trong code:

- Quên `-lsqlite3` trong `OTHER_LDFLAGS` của Xcode → link lỗi `_sqlite3_*` (Device Pulse đã gặp).
- Đổi schema mà không viết file `.sqm` → app cũ crash khi cập nhật.
- `Clock.System.now()` gọi trong luật → test không ổn định. Luôn truyền `now` vào.
- Build iOS với "Any iOS Simulator" đòi cả x86_64 mà Gradle không build → thêm `EXCLUDED_ARCHS[sdk=iphonesimulator*]: x86_64` vào `project.yml`.
- **Undo xong ghi chú bị xoá lần nữa**: `rememberSwipeToDismissBoxState()` là *saveable*, `LazyColumn` nhớ state theo `key` nên dòng quay lại vẫn ở trạng thái "đã vuốt" và tự xoá tiếp. Dùng `remember { SwipeToDismissBoxState(...) }`. Chỉ phát hiện khi chạy trên emulator, test core không thấy.
- `LazyColumn` phải có `key = id`, không thì vuốt xoá một dòng sẽ làm dòng kế tiếp mang trạng thái vuốt của dòng đã xoá.

## 7. Test

```bash
cd apps/PocketNotes
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


## 10. How to test

### 10.1 Unit test (tự động)

```bash
cd apps/PocketNotes
./gradlew :shared:testDebugUnitTest :shared:iosSimulatorArm64Test
```

`NoteRepositoryTest` chạy SQLite **thật** (in-memory, `JdbcSqliteDriver` trên JVM) với đúng file `Note.sq` máy thật dùng, không mock repository.

### 10.2 Chạy trên 2 máy

```bash
./gradlew :androidApp:installDebug && adb shell monkey -p dev.pocketnotes 1
cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' \
  -derivedDataPath build/dd CODE_SIGNING_ALLOWED=NO build && \
  xcrun simctl install booted build/dd/Build/Products/Debug-iphonesimulator/PocketNotes.app && xcrun simctl launch booted dev.pocketnotes.ios
```

### 10.3 Kịch bản test tay

| # | Làm gì | Kết quả đúng |
|:-:|---|---|
| 1 | Mở app lần đầu | "Chưa có ghi chú nào" + nút **+** / ✏️ |
| 2 | Thêm 3 ghi chú, ghim 1 cái | Cái ghim nằm trên cùng, còn lại mới nhất ở trên; dòng phụ "vừa xong" |
| 3 | Mở 1 ghi chú, sửa, bấm Back | Danh sách hiện nội dung mới, **không** tạo bản trùng |
| 4 | Bấm **+** rồi Back ngay (không gõ gì) | Không có ghi chú rỗng nào được tạo |
| 5 | Tìm `ghi chu`, `Y TUONG`, `y tuong` | Đều ra ghi chú có dấu tương ứng |
| 6 | Vuốt xoá → bấm **Hoàn tác** trong 5 giây | Ghi chú quay lại **đúng chỗ cũ** |
| 7 | Tắt hẳn app (vuốt khỏi đa nhiệm), mở lại | Đủ 3 ghi chú, ghim vẫn giữ |

### 10.4 Cố làm hư

| Thử | Mong đợi | Nếu sai thì nghi |
|---|---|---|
| Xoay màn hình khi đang gõ ở màn sửa (Android) | Chữ đang gõ còn nguyên, không lưu 2 lần | Lưu trong `onDispose` (chạy cả khi xoay) |
| Vuốt xoá rồi rời màn trước 5 giây | Ghi chú bị xoá hẳn, Snackbar tắt | `recentlyDeleted` không được dọn |
| Process bị kill khi đang ở màn sửa: `adb shell am kill dev.pocketnotes` sau khi bấm Home | Ghi chú đã lưu còn nguyên. Chữ đang gõ dở (chưa Back) có thể mất | Nếu muốn giữ cả bản nháp: lưu nháp vào `SavedStateHandle` / `@SceneStorage` |
| Cài bản cũ (schema 1) rồi cài đè bản mới | Ghi chú cũ còn, có cột `pinned = 0` | Thiếu file migration `1.sqm` |

### 10.5 Maestro (flow mẫu dùng để chụp ảnh demo)

??? example "pocketnotes.yaml: thêm 3 ghi chú, ghim, mở, tìm"

    ```yaml
    appId: ${APP_ID}            # dev.pocketnotes hoặc dev.pocketnotes.ios
    ---
    - launchApp
    - extendedWaitUntil: { visible: "Chưa có ghi chú nào", timeout: 20000 }
    - tapOn: "Ghi chú mới"      # contentDescription (Android) = accessibilityLabel (iOS)
    - tapOn: "Tiêu đề"
    - inputText: "Đi chợ"       # Maestro gõ được tiếng Việt có dấu trên cả 2 nền tảng
    - tapOn: "Ghim"
    - runFlow: { when: { platform: Android }, commands: [ hideKeyboard, { tapOn: "Quay lại" } ] }
    - runFlow: { when: { platform: iOS }, commands: [ { tapOn: "Pocket Notes" } ] }   # nút back mang tên màn trước
    - tapOn: "Tìm ghi chú"
    - inputText: "di cho"
    - assertVisible: "Đi chợ"
    ```

## 11. Achievement: xong app này bạn có gì

| 🏅 | Đạt được | Bằng chứng |
|:-:|---|---|
| 🗄️ | Database **một schema dùng chung** cho Android + iOS bằng SQLDelight | `Note.sq`, ảnh demo: tắt app mở lại vẫn còn |
| 🌊 | UI tự cập nhật khi bảng đổi (`Flow` từ query), không gọi "reload" bằng tay | `NotesCore.state` |
| 🧭 | Điều hướng list → edit trên cả Compose Navigation và SwiftUI `NavigationStack` | 2 màn trong ảnh demo |
| 🔤 | Tìm tiếng Việt không dấu, viết một lần trong `commonMain` | `RulesTest`, ảnh "y tuong" |
| 🔄 | Viết và test **migration** schema lần đầu | `1.sqm`, test migration xanh |
| 🧩 | `expect`/`actual` qua Koin `platformModule` (driver Android / iOS) | `PlatformModule.android.kt`, `PlatformModule.ios.kt` |

**Mở khoá:** [App 3 · Rate Watch](03-rate-watch.md): thêm mạng + cache.

**Trước:** [App 1 · Tip Split](01-tip-split.md) · **Tiếp:** [App 3 · Rate Watch](03-rate-watch.md)
