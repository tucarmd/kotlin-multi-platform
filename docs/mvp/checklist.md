# Checklist hoàn thành

Đánh dấu khi đã **chạy thật** trên máy, không phải khi đã đọc xong. Mỗi dòng ghi bài liên quan.

## Chặng A — Nền

- [ ] Tạo project bằng wizard, chạy được Android, iOS, Desktop ([bài 2](../lessons/02-setup.md))
- [ ] Giải thích được `commonMain` / `androidMain` / `iosMain` khác nhau gì ([bài 3](../lessons/03-project-structure.md))
- [ ] Màn chào hiện tên nền tảng qua `expect`/`actual` ([bài 4](../lessons/04-share-code.md))
- [ ] Thêm `kotlinx-datetime` vào `commonMain`, hiện ngày hôm nay ([bài 5](../lessons/05-dependencies.md))

## Chặng B — Giao diện

- [ ] Màn danh sách thói quen với dữ liệu giả ([bài 6](../lessons/06-compose-basics.md))
- [ ] Chữ lấy từ `strings.xml`, đổi máy sang tiếng Việt thì app đổi theo ([bài 7](../lessons/07-resources.md))
- [ ] `HabitListViewModel` giữ state, xoay màn hình không mất dữ liệu ([bài 8](../lessons/08-viewmodel-navigation.md))
- [ ] Bấm một thói quen mở màn chi tiết, nút back quay lại ([bài 8](../lessons/08-viewmodel-navigation.md))

## Chặng C — Dữ liệu

- [ ] Thêm / tick / xoá thói quen, tắt app mở lại vẫn còn ([bài 9](../lessons/09-sqldelight.md))
- [ ] Streak tính đúng (test được) ([bài 9](../lessons/09-sqldelight.md))
- [ ] Hiện 3 ngày lễ sắp tới, mất mạng thì báo lỗi gọn, không crash ([bài 10](../lessons/10-ktor.md))

## Chặng D — Ra mắt

- [ ] Màn Compose nằm trong SwiftUI, có một màn SwiftUI gọi code Kotlin ([bài 11](../lessons/11-ios-integration.md))
- [ ] Test `commonTest` chạy xanh trên JVM và iOS simulator ([bài 12](../lessons/12-testing.md))
- [ ] Build được AAB Android, archive iOS, file DMG Desktop ([bài 13](../lessons/13-build-publish.md))
- [ ] GitHub Actions chạy test mỗi lần push ([bài 13](../lessons/13-build-publish.md))

## Xong MVP khi

Cả 3 nền tảng: mở app → thêm thói quen → tick → tắt mở lại vẫn còn → thấy ngày lễ → test xanh.
Chụp màn hình 3 nền tảng để làm bằng chứng.
