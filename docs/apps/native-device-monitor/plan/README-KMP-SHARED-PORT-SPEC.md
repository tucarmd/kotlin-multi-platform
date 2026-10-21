[🏠 README](../index.md) · Plan + bảng phase (P0–P16): [README-KMP-MIGRATION.md](README-KMP-MIGRATION.md) · Spec trước: [UI native (P8–P12)](README-KMP-NATIVE-UI-SPEC.md)

# Spec: tập trung port phần dùng chung vào KMP (`shared/`)

> Tạo: 2026-10-21 · Trạng thái: **Chờ duyệt** (D6–D8 ở §5) · Chưa viết dòng code nào cho P13–P15.
> Các phase nằm chung bảng P0–P16 của [Plan §3](README-KMP-MIGRATION.md#3-phases-each-ends-with-something-you-can-run-screenshot). P13 Final cũ dời thành **P16**.

## 1. Trả lời nhanh: đã port hết app cũ chưa?

**5 tab (task 001–006): code đã port hết**, cả 2 nền tảng. Còn 2 việc *kiểm tra* chưa xong: chạy iOS trên iPhone thật,
và BLE GATT end-to-end (connect → read → notify → write → MTU) với nRF Connect trên máy thật.

**Chưa port** (app cũ có, repo này chưa có):

| Thứ trong app cũ | Ở đâu (`apps/NativeDeviceMonitorLegacy/`) | Vì sao chưa có | Đi về đâu |
|---|---|---|---|
| ⏱ **Pomodoro** (task 007, MVVM lesson 01) | `ios/…/NativeCore/Pomodoro/` + `UI/Pomodoro/` + 2 file test (~550 dòng, **chỉ iOS**) | Plan cũ để backlog (D5) | **P13**: engine + ViewModel vào `shared/`, Android có Pomodoro lần đầu |
| Header xanh, chữ trắng mọi màn | `AppHeader.swift` · `headerText` Android (commit `0e92cbf`, `6e2e8e7`) | Làm ở app cũ **sau** lúc copy sang KMP | **P16** (chỉ UI, nhỏ) |
| API 2 backend URLSession / Alamofire (task 007 api) | `NativeCore/Network/ApiBackend.swift`, `AlamofireApiClient.swift` | **Cố ý không port**: Ktor (OkHttp / Darwin) thay cả hai | — |

**Đã port nhưng vẫn viết 2 lần** (phần đáng đưa vào `shared/` nhất):

| Chỗ | Android | iOS | Trùng gì |
|---|---|---|---|
| BLE client | `AndroidBleClient.kt` 779 dòng | `IosBleClient.kt` 443 dòng | 12 `StateFlow` giống hệt, khử trùng lặp thiết bị, lọc + đếm thiết bị ẩn, ước lượng chu kỳ quảng bá, hẹn giờ quét 10 s, hộp *Received Data* tối đa 50 dòng, dọn GATT khi ngắt / tắt radio |
| Gọi SDK Apple | — | `iosApp/iosApp/Platform/*.swift` 544 dòng Swift | Không trùng, nhưng **viết bằng Swift chứ không phải KMP** (quyết định N2 ở P12) |

Tỉ lệ hôm nay (dòng `.kt` / `.swift`, không tính test):

| Tầng | Dòng | Dùng chung? |
|---|--:|:-:|
| `shared/commonMain` (core + ViewModel + strings + fake) | 3 343 | ✅ |
| `shared/androidMain` | 1 908 | 🤖 |
| `shared/iosMain` (adapter Kotlin) | 843 | 🍎 |
| `androidApp/` (Compose) | 1 576 | 🤖 |
| `iosApp/` (SwiftUI 1 186 + Platform 544 + cầu nối ~110) | 1 843 | 🍎 |

## 2. Mục tiêu đợt này

Viết **càng nhiều càng tốt bằng Kotlin trong `shared/`**, UI vẫn native. Đo bằng 3 con số ở cuối mỗi phase:

1. Luật nào còn viết 2 lần thì đưa về `commonMain` một lần, có test trong `commonTest` (chạy cả JVM lẫn iOS simulator).
2. Tính năng còn thiếu (Pomodoro) port **thẳng vào `shared/`**, không viết lại bằng Swift.
3. Swift chỉ còn: SwiftUI + cầu nối (`AppModel`, `Observed<T>`) — nếu duyệt D6.

UI (Compose + SwiftUI) chỉ đụng khi phase cần màn hình mới. Không thêm tính năng ngoài app cũ.

## 3. Cách viết một tính năng vào `shared/` (công thức dùng cho cả 3 phase)

```
 ① luật thuần        shared/commonMain/core/<feature>/        model + hàm thuần, KHÔNG gọi Date()/SDK  →  test commonTest
 ② cần nền tảng      core/<feature>/<Xxx>.kt   (interface)    →  androidMain/ cài bằng Android SDK
                                                               →  iosMain/     cài bằng Kotlin/Native (import platform.*)
 ③ ViewModel         shared/commonMain/presentation/<feature>/ state: StateFlow<XxxUiState> + action + XxxStrings
 ④ UI chỉ vẽ         androidApp/…/ui/<feature>/  (Compose, collectAsStateWithLifecycle)
                     iosApp/iosApp/UI/<Feature>/  (SwiftUI, Observed<T> qua SKIE)
 ⑤ nối dây           AndroidServices.kt / IosServices.kt  — chỗ DUY NHẤT chọn thật hay fake
```

Thứ tự luôn là ① → ⑤, và **① + ③ có test trước khi viết UI**. Một bước = một lần build + test xanh.

Bảng dịch Swift → Kotlin hay gặp khi port từ app cũ:

| Swift (app cũ) | Kotlin trong `shared/` |
|---|---|
| `enum PomodoroStatus { case running(endDate: Date) }` | `sealed interface PomodoroStatus { data class Running(val endAt: Instant) }` |
| `struct … : Equatable` | `data class` |
| `protocol PomodoroSessionStore` | `interface PomodoroSessionStore` |
| `Date`, `TimeInterval` | `kotlin.time.Instant`, `kotlin.time.Duration` |
| `now: () -> Date = Date.init` (đồng hồ tiêm vào) | `clock: Clock = Clock.System` · test dùng `FakeClock` |
| `Calendar.isDate(_:inSameDayAs:)` | `kotlinx-datetime`: `instant.toLocalDateTime(tz).date == …` (D7) |
| `@Observable final class …ViewModel` | `class …ViewModel : SharedViewModel()` + `StateFlow<…UiState>` |
| `String(format: "%02d:%02d", m, s)` | `"${m.toString().padStart(2, '0')}:…"` (không có `String.format` ở common) |
| `UserDefaults.standard` | interface trong core → 🤖 `SharedPreferences` · 🍎 `NSUserDefaults` (Kotlin/Native) |
| `CBCentralManagerDelegate` | `class … : NSObject(), CBCentralManagerDelegateProtocol` trong `iosMain` (D6) |

## 4. Thiết kế từng phase

### P13 · Pomodoro vào `shared/` (port thẳng, Android có lần đầu)

- ① `core/pomodoro/PomodoroEngine.kt`: `PomodoroPhase` (25 / 5 phút), `PomodoroStatus` (Idle · Running(endAt) · Paused),
  `PomodoroState`, `PomodoroTick`, các hàm `start / pause / reset / skip / tick / countOnSameDay`. Giữ đúng luật cũ:
  tính bằng `endAt` (không đếm lùi từng giây, để khoá máy rồi mở lại vẫn đúng giờ), skip không tính phiên, phiên xong
  ghi theo `endAt` chứ không theo `now`.
- ② `core/pomodoro/PomodoroSessionStore.kt` (interface) + `InMemoryPomodoroSessionStore` (test / preview).
  🤖 `androidMain`: `SharedPreferences`. 🍎 `iosMain`: `NSUserDefaults` **viết bằng Kotlin**, không có file Swift nào.
  Cùng key `pomodoro.completedFocus`, giữ 200 mốc gần nhất.
- ③ `presentation/pomodoro/PomodoroViewModel.kt` + `PomodoroStrings.kt`: `timeText` (làm tròn lên), `progress`,
  `phaseTitle`, `primaryTitle`, `canReset`, `todayText`; action `primaryTapped / reset / skip / tick / appBecameActive`.
  Vòng `tick` mỗi giây chạy trong `viewModelScope` (khác app cũ: View tự lặp), dừng khi không chạy.
- Test: port `PomodoroEngineTests.swift` (84 dòng) + `PomodoroViewModelTests.swift` (100 dòng) sang `commonTest`, đồng hồ giả.
- ④ UI: Compose `PomodoroScreen` (mới) + SwiftUI `PomodoroView` đọc ViewModel chung. Vào từ nút ⏱ ở tab Monitor như app cũ.
- Tách: **P13a** ①②③ + test (chưa có UI) · **P13b** 2 màn hình + nối dây.

### P14 · BLE: luật chung về `commonMain`, mỗi nền tảng chỉ còn gọi SDK

- Mới `core/bluetooth/BleSession.kt` (commonMain): giữ 12 `StateFlow` của `BleClient` và các luật đang chép 2 nơi:
  `onAdvertisement(...)` (khử trùng lặp, lọc, đếm ẩn, chu kỳ quảng bá), hẹn giờ quét 10 s, hẹn giờ connect,
  `appendReceived` (tối đa 50), `markNotifying`, `clearGattState`, log write.
- `AndroidBleClient` / `IosBleClient` chỉ còn: gọi SDK + đổi callback SDK thành lời gọi `session.xxx()`.
  Riêng Android vẫn giữ hàng đợi GATT (một thao tác một lúc) vì đó là luật của Android, không phải của app.
- Test mới trong `commonTest`: `BleSessionTest` với `runTest` + thời gian ảo (10 s tự dừng, 15 s timeout, radio off dọn sạch).
- Ước lượng: Android 779 → ~480 dòng, iOS adapter 443 → ~180 dòng, commonMain +~300 dòng có test.
- Kiểm: 2 app không đổi gì trên màn hình (emulator scan + simulator), Maestro qua tab Scanner.

### P15 · iOS gọi SDK Apple bằng Kotlin/Native thay cho Swift (nếu duyệt D6)

Lật lại N2 của P12 để `iosApp/` chỉ còn UI. Interface `core/` **không đổi**, ViewModel không đổi, UI không đổi.

- **P15a** quyền + thiết bị: `CBManager.authorization`, `AVCaptureDevice`, `UIDevice` pin, `nw_path_monitor`,
  `BGTaskScheduler` gọi thẳng từ `iosMain`. Xoá `PermissionNative.swift`, `DeviceNative.swift`.
- **P15b** CoreBluetooth: `CBCentralManagerDelegateProtocol` + `CBPeripheralDelegateProtocol` trong Kotlin (lớp con `NSObject`,
  giữ ref mạnh tới `CBPeripheral`). Xoá `BleNative.swift`. Interface callback `IosBleNative` không cần nữa.
- **P15c** camera / thư viện ảnh: `UIImagePickerController` + `PHPickerViewController` từ Kotlin (cần tìm
  `UIViewController` đang hiển thị). Đây là phần khó nhất; nếu dài quá thì giữ `MediaNative.swift` và ghi lý do.
- So sánh trước / sau: bản Swift ở commit `82f45c6` (P12d) để đọc đối chiếu.

### P16 · Final (P13 cũ)

Chạy iOS trên iPhone thật (chụp ảnh, BLE scan → GATT với nRF Connect, BGAppRefresh qua LLDB), BLE GATT end-to-end
cả 2 máy, checklist task 006, header xanh cho 2 UI (tiêu đề lấy từ `*Strings` chung), README bảng "cũ vs mới" và bảng
tỉ lệ dòng dùng chung sau P13–P15.

## 5. Quyết định cho bạn

| # | Câu hỏi | Đề xuất | Phương án khác |
|---|---|---|---|
| D6 | Code SDK Apple: giữ Swift (N2) hay viết lại bằng Kotlin/Native | **Viết lại bằng Kotlin/Native (P15)**, đúng mục tiêu "tập trung KMP"; Media làm sau cùng, được phép giữ Swift nếu quá dài | Giữ Swift, bỏ P15 (ít việc hơn, nhưng `iosApp/` vẫn còn 544 dòng logic SDK) |
| D7 | Thời gian cho Pomodoro | `kotlin.time` (Clock, Instant) + **`kotlinx-datetime`** cho "cùng ngày" theo múi giờ máy | Tự cộng offset múi giờ bằng expect/actual (ít thư viện, dễ sai khi đổi giờ) |
| D8 | Thứ tự | **P13 Pomodoro → P14 BLE → P15 SDK → P16**: Pomodoro là luật thuần, thấy trọn công thức ①–⑤ trước | BLE trước (lợi lớn nhất về dòng trùng, nhưng khó, khó kiểm khi chưa có máy thật) |

## 6. Rủi ro

- **P14 đụng BLE Android đã chạy thật:** test `commonTest` phải phủ luật trước khi dời; mỗi bước so ảnh tab Scanner.
- **Kotlin/Native interop (P15):** delegate phải là lớp con `NSObject`, con trỏ C (`nw_path_*`) cần `memScoped`;
  lỗi chỉ hiện lúc link iOS (chậm). Skill `kotlin-tooling-native-build-performance` nếu build quá lâu.
- **BLE iOS chưa chạy máy thật:** simulator không có Bluetooth, nên P15b chỉ kiểm được build + test adapter.
  Cần iPhone thật để biết bản Kotlin chạy bằng bản Swift.
- **Pomodoro chạy nền:** giữ cách cũ (không chạy nền, mở lại thì `tick` bắt kịp theo `endAt`). Không thêm thông báo.

## 7. Ngoài phạm vi

Thông báo khi hết giờ Pomodoro, SwiftData, desktop/web, store, tính năng mới, Kable (D2 vẫn là tự viết `BleClient`).
