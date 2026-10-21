# Checklist hoàn thành

Đánh dấu khi đã **chạy thật** trên máy, không phải khi đã đọc xong. Mỗi dòng ghi bài liên quan.

## Chặng A: Nền

- [ ] Tạo project `DevicePulse` bằng wizard, chạy được trên Android emulator và iOS simulator ([bài 2](../lessons/02-setup.md))
- [ ] Giải thích được `commonMain` / `androidMain` / `iosMain` khác nhau gì ([bài 3](../lessons/03-project-structure.md))
- [ ] Có model, `Reading` và 3 hàm luật `batteryLevel` / `fixQuality` / `latencyGrade` ở `commonMain` ([bài 4](../lessons/04-share-code.md))
- [ ] Koin + Kermit chạy, log hiện trên Logcat và Xcode console ([bài 5](../lessons/05-dependencies.md))

## Chặng B: Giao diện

- [ ] Dashboard 3 thẻ với dữ liệu giả, vẽ đủ 5 trạng thái của `Reading` bằng **Jetpack Compose và SwiftUI** ([bài 6](../lessons/06-compose-basics.md))
- [ ] Đổi máy sang tiếng Việt / tiếng Anh thì app đổi theo ([bài 7](../lessons/07-resources.md))
- [ ] `PulseMonitor` + fake sensor, bọc bằng `PulseViewModel` (Android) và `PulseModel` (iOS); xoay màn không mất state; Dashboard → Detail → back trên cả hai ([bài 8](../lessons/08-viewmodel-navigation.md))

## Chặng C: Cảm biến thật

- [ ] Pin thật: đổi % trong emulator thì thẻ đổi theo; iPhone thật hiện đúng %, Simulator hiện "Không hỗ trợ" ([bài 9](../lessons/09-battery.md))
- [ ] Vị trí thật trên 2 nền tảng; từ chối quyền thì app báo gọn, không crash ([bài 10](../lessons/10-location.md))
- [ ] Bật/tắt Wi-Fi, chế độ máy bay thì thẻ mạng đổi ngay; đo được độ trễ, mất mạng thì báo lỗi ([bài 11](../lessons/11-network.md))
- [ ] Lưu lần đo, tắt hẳn app mở lại vẫn thấy trong Lịch sử ([bài 12](../lessons/12-history-sqldelight.md))

## Chặng D: Ra mắt

- [ ] Test luật + ViewModel + `LatencyProbe` xanh trên JVM và iOS simulator ([bài 13](../lessons/13-testing.md))
- [ ] Build được AAB Android và archive iOS; crash report có stack trace đọc được ([bài 14](../lessons/14-build-publish.md))
- [ ] GitHub Actions chạy test mỗi lần push ([bài 14](../lessons/14-build-publish.md))

## Xong khi

Trên **cả Android và iOS**: mở app → thấy pin, vị trí, mạng thật → đo độ trễ → lưu lần đo → tắt mở lại vẫn còn → test xanh.
Chụp màn hình 2 nền tảng làm bằng chứng.
