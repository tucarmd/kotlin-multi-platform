[← 02 Bluetooth](02-bluetooth.md) · [↑ Platform API](index.md) · [04 Thư viện ảnh →](04-library.md)

# 03 · Notification Permission

!!! info "App này dùng ở đâu"
    - **Android**: tab Monitor, nút "Chạy nền (Foreground Service)". Trước khi start service, app xin `POST_NOTIFICATIONS` (Android 13+): [`NotificationGatedMonitor.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/shared/src/androidMain/kotlin/com/learn/ndmkmp/core/background/NotificationGatedMonitor.kt) · kênh + notification: [`MonitorService.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitor/shared/src/androidMain/kotlin/com/learn/ndmkmp/core/background/MonitorService.kt).
    - **iOS**: chưa dùng notification (chạy nền bằng `BGAppRefresh`, không cần quyền).

## 1. Notification khác các quyền khác chỗ nào

- **Không chặn tính năng**: bị từ chối thì app vẫn chạy, chỉ là user không thấy thông báo. Đừng bao giờ khoá tính năng vì thiếu quyền này (app này: từ chối vẫn start service).
- **Có nhiều tầng tắt**: quyền chung của app → kênh (Android) / kiểu hiển thị (iOS) → Do Not Disturb / Focus. Quyền `GRANTED` **không có nghĩa** user thấy thông báo.
- **Push ≠ quyền hiển thị**: nhận token push và silent push **không cần** quyền này (iOS), quyền chỉ quyết định có hiện banner / âm thanh / badge không.

## 2. API

=== "Android"

    ```xml
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />  <!-- runtime từ API 33 -->
    ```

    | Android | Hành vi |
    |---|---|
    | ≤ 12 (API ≤ 32) | Mặc định bật, không hỏi. User tự tắt trong Settings |
    | 13+ app target ≥ 33 | App tự xin bằng `RequestPermission(POST_NOTIFICATIONS)` |
    | 13+ app target ≤ 32 | Hệ thống **tự hiện dialog** lúc app tạo notification channel đầu tiên |
    | Nâng cấp lên 13 khi app đã cài | Thường được cấp sẵn (theo trạng thái cũ) |

    ```kotlin
    // Kiểm tra: tổng hợp mọi tầng (quyền + công tắc app), dùng cái này thay vì chỉ checkSelfPermission
    NotificationManagerCompat.from(context).areNotificationsEnabled()

    // Kênh (API 26+): bắt buộc, user tắt riêng từng kênh được
    val channel = NotificationChannel("monitor", "Monitor", NotificationManager.IMPORTANCE_LOW)
    manager.createNotificationChannel(channel)        // gọi lại nhiều lần không sao, nhưng KHÔNG đổi được importance sau khi tạo
    manager.getNotificationChannel("monitor").importance == IMPORTANCE_NONE   // user tắt kênh này
    ```

    Mở trang cài đặt notification của app: `Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)`.

=== "iOS"

    **Không cần key `Info.plist`.** Câu trong dialog do iOS viết sẵn.

    ```swift
    let center = UNUserNotificationCenter.current()
    center.delegate = self      // gán TRƯỚC khi didFinishLaunching kết thúc, không thì mất sự kiện bấm notification lúc cold start

    let granted = try await center.requestAuthorization(options: [.alert, .sound, .badge])

    let settings = await center.notificationSettings()
    switch settings.authorizationStatus {
    case .notDetermined: break
    case .authorized:    break
    case .provisional:   break   // giao lặng lẽ vào Notification Center, không hỏi (xin bằng option .provisional)
    case .ephemeral:     break   // App Clip
    case .denied:        break   // chỉ còn Settings
    @unknown default:    break
    }

    // Mặc định: app đang mở thì notification KHÔNG hiện. Muốn hiện phải trả về trong delegate:
    func userNotificationCenter(_ c: UNUserNotificationCenter, willPresent n: UNNotification) async
        -> UNNotificationPresentationOptions { [.banner, .sound] }
    ```

    Push: `UIApplication.shared.registerForRemoteNotifications()` + capability Push Notifications. Gọi được cả khi user từ chối (vẫn có token, vẫn nhận silent push).

## 3. Trạng thái: Android ↔ iOS

| Ý nghĩa | Android 13+ | iOS |
|---|---|---|
| Chưa hỏi | DENIED + chưa lưu cờ | `.notDetermined` |
| Cho | GRANTED **và** `areNotificationsEnabled()` | `.authorized` |
| Cho kiểu lặng lẽ | (không có; gần nhất: kênh `IMPORTANCE_LOW`) | `.provisional` |
| Từ chối | rationale / đã hỏi 2 lần như quyền khác | `.denied` |
| Cho nhưng không thấy | kênh bị tắt, DND, app bị "pause" | Focus, Scheduled Summary, tắt Lock Screen / Banner riêng |

## 4. Vòng đời: khởi động lại, kill, crash

| Chuyện xảy ra | Android | iOS |
|---|---|---|
| Khởi động lại app | Quyền giữ nguyên; kênh giữ nguyên | Giữ nguyên |
| Hệ thống kill khi ở nền | Notification đã hiện **vẫn còn** (của FGS thì biến mất cùng service). FCM vẫn đánh thức app | Push thường vẫn hiện (iOS hiện hộ). Silent push đánh thức app ở nền |
| User vuốt tắt | FGS thường vẫn chạy (`onTaskRemoved`), notification vẫn còn | Push thường vẫn hiện. **Silent push không được giao** tới khi user mở app |
| Force stop (Android) | **Không nhận FCM, không alarm** tới khi user mở app lại | (không có) |
| Crash | Notification của FGS biến mất; `START_STICKY` có thể tạo lại service | Không ảnh hưởng push đã hẹn (`UNNotificationRequest` local vẫn nổ) |
| Đổi quyền trong Settings | Thu hồi `POST_NOTIFICATIONS` → process bị kill | Đổi công tắc notification **không kill app** (khác Camera / Location). Đọc lại khi `.active` |
| Xoá app cài lại | Mất quyền, mất kênh | Mất quyền. Xoá rồi cài lại ngay, đôi khi iOS không hỏi lại (nhớ ~1 ngày); muốn chắc: Reset Location & Privacy |

## 5. Use case & error case

| Mã | Tình huống | Triệu chứng | Xử lý | Cách giả lập |
|---|---|---|---|---|
| `N01` | Bị từ chối, Android 13+, Foreground Service | Service vẫn chạy nhưng **không thấy notification** trong khay (vẫn thấy trong Task Manager) | Đúng như thiết kế; hiện hint trong app | Từ chối dialog khi bấm "Chạy nền"; hoặc `adb shell pm revoke com.learn.ndmkmp android.permission.POST_NOTIFICATIONS` |
| `N02` | Quyền có nhưng kênh bị tắt | `areNotificationsEnabled()` = true, vẫn không thấy | Kiểm tra `channel.importance == IMPORTANCE_NONE`, dẫn user tới `ACTION_CHANNEL_NOTIFICATION_SETTINGS` | Long-press notification › tắt kênh |
| `N03` | Muốn tăng importance kênh đã tạo | Code đổi `IMPORTANCE_HIGH` không có tác dụng | Kênh **bất biến** sau khi tạo: tạo kênh **id mới**, xoá kênh cũ | Đổi importance trong code, cài đè (không gỡ) |
| `N04` | `startForeground` quá 5–10 s sau `startForegroundService` | Crash `ForegroundServiceDidNotStartInTimeException` | Gọi `startForeground` ngay đầu `onStartCommand` (app này làm) | Thêm `delay(15_000)` trước `startForeground` |
| `N05` | Start FGS khi app ở nền (Android 12+) | `ForegroundServiceStartNotAllowedException` | Chỉ start khi app đang mở, hoặc dùng WorkManager | Gọi start từ `Handler.postDelayed` 10 s rồi bấm Home |
| `N06` | Thiếu `foregroundServiceType` (Android 14+) | `MissingForegroundServiceTypeException` | Khai type trong manifest + quyền `FOREGROUND_SERVICE_<TYPE>` | Xoá `android:foregroundServiceType` |
| `N07` | iOS app đang mở, notification không hiện | Không banner khi app foreground | Implement `willPresent` trả `[.banner, .sound]` | Gửi push khi app đang mở (xem lệnh dưới) |
| `N08` | iOS bấm notification lúc app đã tắt không vào đúng màn | Mở app ở màn mặc định | Gán `UNUserNotificationCenter.delegate` trong `init` của App / `didFinishLaunching` | Tắt app, gửi push, bấm vào |
| `N09` | Hỏi ngay lúc mở app lần đầu | Tỉ lệ đồng ý thấp, iOS chỉ có 1 lần | Hỏi đúng lúc user bật tính năng cần thông báo; iOS cân nhắc `.provisional` | — |
| `N10` | Force stop rồi gửi FCM (Android) | Không nhận | Đúng như thiết kế, không có cách vượt | `adb shell am force-stop com.learn.ndmkmp` rồi gửi từ Firebase console |
| `N11` | Notification local trùng id | Cái sau đè cái trước / cái trước biến mất | Mỗi notification một id (Android `notify(id)`, iOS `identifier`) | Gửi 2 lần cùng id |

## 6. Lệnh giả lập

```bash
# Android: tạo 1 notification thử từ shell (không cần code)
adb shell cmd notification post -S bigtext -t "Tiêu đề" test_tag "Nội dung thử"
adb shell dumpsys notification --noredact | grep -A3 com.learn.ndmkmp     # notification + kênh đang có
adb shell cmd notification set_dnd on      # bật Do Not Disturb (off để tắt)

# iOS Simulator: gửi push giả (không cần server, không cần APNs)
cat > /tmp/push.apns <<'EOF'
{ "Simulator Target Bundle": "com.learn.ndmkmp",
  "aps": { "alert": { "title": "Test", "body": "Push giả từ simctl" }, "sound": "default" } }
EOF
xcrun simctl push booted com.learn.ndmkmp /tmp/push.apns   # hoặc kéo file .apns thả vào cửa sổ Simulator
```

## 7. Khác biệt cần nhớ

- Android **từ 13** mới hỏi; dưới 13 mặc định bật. iOS **luôn** phải hỏi.
- Android có **kênh**; iOS có **kiểu hiển thị** (banner, lock screen, badge, sound) user bật tắt riêng.
- iOS app đang mở **không tự hiện** notification; Android thì hiện.
- Đổi quyền notification trong Settings: Android bị kill (khi thu hồi), iOS **không** bị kill.
