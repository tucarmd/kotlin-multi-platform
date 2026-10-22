---
name: kmp-new
description: >-
  Tạo app KMP mới trong apps/ của repo kotlin-multi-platform bằng cách clone một app đã chạy được (mặc định
  TipSplit) và đổi tên tự động: rootProject, package dev.<app>, bundleId, tên file, thư mục. Sau đó sinh lại
  .xcodeproj bằng xcodegen và build xanh trước khi sang B1. Dùng khi nói "tạo app KMP mới", "app thứ 6",
  "clone khung TipSplit", "copy PocketNotes thành app mới", "new kmp app". Bước sau là kmp-plan.
---

# kmp-new — clone khung thành app mới

## 1. Chọn app nguồn theo mảnh mới

| App mới cần | Clone từ | Lý do |
|---|---|---|
| Chỉ tính toán, không lưu | `TipSplit` | Nhỏ nhất, có sẵn Kover + UI test + Maestro |
| Database | `PocketNotes` | Có SQLDelight + driver hai nền tảng |
| Mạng | `RateWatch` | Có Ktor + cache |
| GPS / callback → Flow | `TripLog` | Có `platform/LocationStream` |
| BLE | `ObdLink` | Có Kable + module `core/` |

Nguồn càng gần thì càng ít phải xoá. Chưa chắc thì dùng TipSplit.

## 2. Chạy script

```bash
.Codex/skills/kmp-new/scripts/new-app.sh <NewApp> [SourceApp]   # vd: FuelLog PocketNotes
```

Script làm:

- chỉ copy file git đang track (bỏ `build/`, `.gradle/`, DerivedData), cộng thêm `local.properties`;
- đổi `<SourceApp>`→`<NewApp>` và `dev.<source>`→`dev.<new>` trong nội dung, tên file và thư mục;
- in ra danh sách file còn mang tên nghiệp vụ cũ (`TipCore`, `SplitView`…). Những tên này **phải đổi tay**,
  vì đó là từ của domain, không đổi máy móc được.

## 3. Dọn và build xanh

1. Đổi tên các class nghiệp vụ theo danh sách script in ra. Xoá phần thân trong `model/`, `rules/`, `core/`,
   test và `ui/`, chỉ để lại vỏ class rỗng. Giữ nguyên Gradle, Koin, SKIE và `project.yml`.
2. Bỏ thư viện app mới không dùng trong `gradle/libs.versions.toml` và `shared/build.gradle.kts`.
3. `cd apps/<NewApp>/iosApp && xcodegen` để sinh lại `.xcodeproj` (Xcode không tự đọc lại `project.yml`).
4. Phải xanh hết trước khi sang bước sau:
   ```bash
   cd apps/<NewApp>
   ./gradlew :shared:testDebugUnitTest :androidApp:assembleDebug
   ```
5. Commit: `chore(<scope>): scaffold from <SourceApp>`.

## Bẫy

- Script cần bash 3.2 của macOS, nên trong `${var//pat/rep}` **không được bọc dấu nháy**: bash 3.2 coi dấu nháy là
  ký tự thật, đường dẫn sẽ thành `dev\/fuellog\/`.
- Gradle daemon JVM lấy từ `gradle/gradle-daemon-jvm.properties`; sync lỗi JDK thì xem `docs/reference/troubleshooting.md`.

Tiếp theo: `kmp-plan`.
