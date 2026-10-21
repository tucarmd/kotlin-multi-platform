[← 06 Camera](06-camera.md) · [↑ Platform API](index.md)

# 07 · Life Cycle (vòng đời app)

Mọi trang 01–06 đều có bảng "khởi động lại, kill, crash". Trang này giải thích **gốc** của các bảng đó: app đi qua những trạng thái nào, ai giết app, giết kiểu gì, và cái gì còn sống sau đó.

!!! info "App này dùng ở đâu"
    - Android: [`MainActivity.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/androidApp/src/main/kotlin/com/learn/ndmkmp/MainActivity.kt) (`onCreate` tạo services + ViewModel, `onDestroy` gọi `vm.clear()`) · [`App.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/androidApp/src/main/kotlin/com/learn/ndmkmp/App.kt) (`LifecycleResumeEffect` → `vm.onResume()` đọc lại quyền) · [`MonitorService.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/shared/src/androidMain/kotlin/com/learn/ndmkmp/core/background/MonitorService.kt) (Foreground Service, `START_STICKY`) · [`MonitorWorker.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/shared/src/androidMain/kotlin/com/learn/ndmkmp/core/background/MonitorWorker.kt) (WorkManager).
    - iOS: [`iOSApp.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/iosApp/iosApp/iOSApp.swift) (`scenePhase == .active` → `vm.onResume()`, đăng ký `BGAppRefresh` trong `init`) · [`IosPlatform.swift`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/iosApp/iosApp/Platform/IosPlatform.swift).
    - Chung: [`AppViewModel.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/shared/src/commonMain/kotlin/com/learn/ndmkmp/presentation/AppViewModel.kt) có `onResume()` cho cả hai bên gọi.
    - Bài nền: [Android cơ bản qua code › `onCreate` chạy khi nào, xoay màn hình mất gì](../README-KMP-ANDROID-BASICS.md).

## 1. Hai vòng đời: màn hình và process

Người mới hay nhầm **màn hình bị huỷ** với **app bị tắt**. Đây là hai chuyện khác nhau:

| | Android | iOS |
|---|---|---|
| Vòng đời **màn hình** | `Activity`: `onCreate → onStart → onResume → onPause → onStop → onDestroy` | `UIViewController` / SwiftUI View: `onAppear` / `onDisappear` |
| Vòng đời **app** | Process + `ProcessLifecycleOwner` (app foreground / background) | `scenePhase`: `.active` / `.inactive` / `.background` (+ `UIApplicationDelegate`) |
| Màn hình bị tạo lại khi xoay / đổi dark mode | **Có** (Activity huỷ + tạo mới) | **Không** |
| Process sống khi màn hình bị huỷ | Có (xoay màn) | — |

=== "Android"

    ```mermaid
    flowchart LR
        C["onCreate<br/>setup 1 lần, đăng ký launcher"] --> S["onStart<br/>thấy được"] --> R["onResume<br/>dùng được · ĐỌC LẠI QUYỀN"]
        R -- "Home / mở Settings / dialog hệ thống" --> P["onPause"] --> ST["onStop<br/>huỷ receiver"]
        ST -- "quay lại" --> S
        ST -- "xoay · finish · Don't keep activities" --> D["onDestroy"]
        ST -. "thiếu RAM: process bị kill,<br/>KHÔNG gọi onDestroy" .-> K["(chết)"]
        K -. "user quay lại từ Recents" .-> C
    ```

    Process có **mức ưu tiên**, thiếu RAM thì giết từ thấp lên: rỗng → nền (cached) → service → **foreground service** / đang hiện → không bao giờ (system).

=== "iOS"

    ```mermaid
    flowchart LR
        NR["Not running"] -- "bấm icon" --> I["Inactive<br/>(đang chuyển)"] --> A["Active<br/>scenePhase .active · ĐỌC LẠI QUYỀN"]
        A -- "Home / App Switcher / cuộc gọi" --> I2["Inactive"] --> B["Background<br/>~5–30 s để dọn dẹp"]
        B --> SU["Suspended<br/>đứng yên trong RAM, không chạy code"]
        SU -- "quay lại" --> A
        SU -. "thiếu RAM (jetsam):<br/>kill, KHÔNG báo trước" .-> NR
        B -- "BGTask / push / BLE / location" --> B
    ```

    `applicationWillTerminate` **gần như không bao giờ chạy**: app thường bị kill khi đang Suspended, lúc đó không còn chạy code được. Muốn lưu thì lưu ở `.background`.

## 2. Ai giết app, giết kiểu gì

| Kiểu chết | Android | iOS | Code được báo? | Cái gì còn sống |
|---|---|---|---|---|
| **Xoay màn / đổi theme** | Activity huỷ + tạo lại, process sống | Không chết | `onDestroy` + `onCreate` mới | `ViewModel` (Jetpack), `rememberSaveable`, `savedInstanceState` |
| **Hệ thống kill khi ở nền** (thiếu RAM) | Process chết **im lặng** | Jetsam kill **im lặng** | **Không** | Android: `savedInstanceState` / `SavedStateHandle` được khôi phục khi user quay lại. iOS: `@SceneStorage`, state restoration |
| **User vuốt khỏi Recents / App Switcher** | Task bị xoá, process thường chết; FGS có thể vẫn chạy (`onTaskRemoved`) | App chết. **iOS coi là "user không muốn app chạy"**: không BGTask, không silent push, không BLE restoration tới khi mở lại | Android: `onTaskRemoved` (service). iOS: không | Android: WorkManager, alarm. iOS: notification local đã hẹn |
| **Force stop** (Android) | Dừng **tất cả**: service, alarm, job, WorkManager bị huỷ lịch; không nhận broadcast / FCM tới khi user mở app | — | Không | Chỉ dữ liệu trên đĩa |
| **Crash** | Process chết; nếu còn Activity bên dưới trong stack, Android có thể **mở lại Activity đó** | App chết, về màn Home | `Thread.setDefaultUncaughtExceptionHandler` (Crashlytics dùng) | Dữ liệu trên đĩa. Prefs ghi bằng `apply()` có thể **chưa kịp ghi** |
| **Thu hồi quyền trong Settings** | Process bị kill | App bị kill (Camera, Location, Photos, Bluetooth…) | Không | Như kill khi ở nền: Android khôi phục `savedInstanceState` |
| **Update app** | Process bị kill, cài bản mới | Như vậy | Không | Dữ liệu, quyền |
| **Reboot máy** | Tất cả chết | Tất cả chết | Không | WorkManager tự chạy lại; FGS thì không (trừ `BOOT_COMPLETED`). iOS: không gì tự chạy tới khi user mở app (trừ push / location) |
| **ANR / watchdog** | Đơ main thread > 5 s → dialog "App isn't responding" | Đơ khi launch / resume quá lâu → kill `0x8badf00d` | Không | — |

## 3. Use case & error case

| Mã | Tình huống | Triệu chứng | Xử lý | Cách giả lập |
|---|---|---|---|---|
| `LC01` | Đổi quyền trong Settings rồi quay lại | UI vẫn hiện trạng thái cũ (khi không bị kill, vd cấp thêm quyền) | Đọc lại quyền mỗi `onResume` / `.active`, không cache (app này: `vm.onResume()`) | Cấp quyền trong Settings, quay lại app |
| `LC02` | Xoay màn hình | Mất text đang nhập, request mạng chạy lại | State trong `ViewModel`; UI state nhỏ dùng `rememberSaveable` | Xoay emulator (`Ctrl+←`) |
| `LC03` | Process bị kill khi ở nền, user quay lại | App mở ra **giữa chừng** (màn sâu) nhưng **dữ liệu trống / crash `lateinit`** | Mọi thứ màn hình cần phải dựng lại được từ `SavedStateHandle` + đĩa; không giữ state quan trọng trong `object` / singleton | Mở màn bất kỳ → Home → `adb shell am kill com.learn.ndmkmp` → mở lại từ Recents |
| `LC04` | Đăng ký launcher sau STARTED | Crash `IllegalStateException … attempting to register while current state is RESUMED` | Đăng ký trong `onCreate` (app này: `createAndroidServices(this)` trước `setContent`) | Gọi `registerForActivityResult` trong `onClick` |
| `LC05` | Receiver / listener không huỷ | Leak Activity, callback vào màn đã chết | Đăng ký `onStart`, huỷ `onStop` (app này: radio receiver trong `AndroidPermissionController`) | Bỏ dòng huỷ, xoay màn 10 lần, xem LeakCanary / Memory Profiler |
| `LC06` | Coroutine của ViewModel sống quá màn hình | Log vẫn chạy sau khi thoát, ghi vào state đã chết | Huỷ scope khi màn chết (app này: `vm.clear()` trong `onDestroy`) | Bỏ `vm.clear()`, thoát app bằng Back, xem logcat |
| `LC07` | Lưu dữ liệu bằng `apply()` ngay trước khi bị kill / crash | Mất dữ liệu vừa ghi | Dùng `commit()` / DataStore khi **ngay sau đó** sẽ rời app (app này: cờ "đã hỏi" quyền) | Ghi bằng `apply()` rồi `adb shell am crash com.learn.ndmkmp` ngay |
| `LC08` | Force stop (Android) | Foreground Service tắt, WorkManager không chạy nữa, không tự sống lại | **Thiết kế của Android**, không vượt; ghi rõ cho user | `adb shell am force-stop com.learn.ndmkmp` rồi chờ chu kỳ Worker |
| `LC09` | Service bị kill, `START_STICKY` tạo lại | `onStartCommand(intent = null)` → crash nếu code đọc extra không kiểm tra null | Xử lý `intent == null` (app này: dùng interval mặc định) | `adb shell am kill` khi service đang chạy và app ở nền |
| `LC10` | Doze / App Standby (Android 6+) | Worker / alarm chạy trễ hàng giờ | Bình thường; cần đúng giờ → FGS hoặc `setExactAndAllowWhileIdle` (có quyền riêng) | `adb shell dumpsys deviceidle force-idle` · `adb shell am set-standby-bucket com.learn.ndmkmp rare` · trả lại: `dumpsys deviceidle unforce` |
| `LC11` | `BGAppRefresh` không bao giờ chạy (iOS) | Không có record "bgRefresh" | Đăng ký handler **trước khi launch xong** (app này: `iOSApp.init`); id có trong `BGTaskSchedulerPermittedIdentifiers`; iOS tự quyết giờ chạy; user vuốt tắt app = không chạy | Xcode: pause app, chạy trong LLDB: `e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateLaunchForTaskWithIdentifier:@"com.learn.ndmkmp.refresh"]` rồi Continue |
| `LC12` | Làm việc lâu ở `.background` (iOS) | Bị kill giữa chừng, upload dở dang | `beginBackgroundTask` xin thêm thời gian (~30 s), hoặc `BGProcessingTask` / `URLSession` background | Bấm Home khi đang upload lớn |
| `LC13` | Đơ main thread | Android ANR; iOS kill `0x8badf00d` khi launch | Đưa IO / decode ra `Dispatchers.IO` / background queue | `Thread.sleep(10_000)` trong `onCreate` / `sleep(30)` trong `init` (chạy **không** debugger) |
| `LC14` | Thiếu RAM khi app đang mở | Ảnh / cache lớn → OOM | Giải phóng cache ở `onTrimMemory` / `didReceiveMemoryWarning` | Android: `adb shell am send-trim-memory com.learn.ndmkmp RUNNING_CRITICAL`. iOS Simulator: Debug › Simulate Memory Warning |
| `LC15` | Crash khi màn sâu, Android mở lại màn dưới | User thấy app "nhảy" về màn trước, state lạ | Màn nào cũng phải tự dựng lại được (xem `LC03`) | Mở 2 Activity, crash ở Activity trên |
| `LC16` | Test kill dưới debugger | Không thấy hành vi thật (app không bị suspend / kill) | Chạy từ icon, xem log bằng `adb logcat` / Console.app | — |

## 4. Lệnh giả lập vòng đời

```bash
# ===== Android =====
PKG=com.learn.ndmkmp
adb shell am start -n $PKG/.MainActivity                 # mở app
adb shell input keyevent KEYCODE_HOME                    # bấm Home (app xuống nền)
adb shell am kill $PKG                                   # kill như thiếu RAM (app phải đang ở nền)
adb shell am force-stop $PKG                             # Force stop
adb shell am crash $PKG                                  # ép crash
adb shell settings put global always_finish_activities 1 # Don't keep activities (0 để tắt)
adb shell am send-trim-memory $PKG RUNNING_CRITICAL      # giả lập thiếu RAM
adb shell dumpsys deviceidle force-idle                  # ép Doze (unforce để trả lại)
adb shell dumpsys activity services $PKG                 # service nào đang chạy
adb shell dumpsys jobscheduler | grep -A5 $PKG           # job / WorkManager đã lên lịch
adb reboot                                               # khởi động lại máy

# ===== iOS Simulator =====
BID=com.learn.ndmkmp
xcrun simctl launch --console booted $BID                # mở + xem log
xcrun simctl terminate booted $BID                       # kill
xcrun simctl spawn booted log stream --predicate 'process == "Device Monitor KMP"'   # log hệ thống của app
# Simulator menu: Device › Home (⇧⌘H), Debug › Simulate Memory Warning
# Xcode menu: Debug › Simulate Background Fetch
```

## 5. Khác biệt cần nhớ

- Android **tạo lại màn hình** khi xoay / đổi theme; iOS thì không. Phần lớn bug "mất state" của Android đến từ đây.
- Android có **force stop**, iOS có **vuốt tắt**: cả hai đều là tín hiệu "user muốn app dừng", hệ thống tắt hết chạy nền tới khi user mở lại.
- Cả hai đều **kill im lặng** khi thiếu RAM: không có callback cuối cùng. Lưu ở `onStop` / `.background`, đừng chờ `onDestroy` / `willTerminate`.
- Cả hai đều **không báo** khi quyền đổi trong Settings: đọc lại ở `onResume` / `.active`, và chuẩn bị cho cold start vì thu hồi quyền = bị kill.
