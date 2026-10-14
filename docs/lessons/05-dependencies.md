# Thêm thư viện

Thư viện **multiplatform** thêm vào `commonMain` một lần là dùng được mọi nơi. Thư viện chỉ của một nền tảng thì thêm vào source set của nền tảng đó.

## Mục tiêu

- Thêm thư viện qua version catalog đúng source set.
- Biết tìm thư viện multiplatform ở đâu.

## Version catalog

Phiên bản thư viện nằm ở `gradle/libs.versions.toml`:

```toml
[versions]
kotlinx-datetime = "…"   # lấy bản mới nhất trên trang thư viện

[libraries]
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
```

Rồi dùng trong `build.gradle.kts`:

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(libs.kotlinx.datetime)
    }
}
```

## Thư viện nào cho source set nào

| Thư viện | Thêm vào | Ghi chú |
|---|---|---|
| `kotlinx-coroutines-core`, `kotlinx-datetime`, `kotlinx-serialization-json` | `commonMain` | multiplatform |
| `ktor-client-core` | `commonMain` | engine riêng thêm ở từng nền tảng (bài 10) |
| `ktor-client-okhttp` | `androidMain` | engine Android |
| `ktor-client-darwin` | `iosMain` | engine Apple |
| Thư viện Android thuần (vd Play Services) | `androidMain` | không gọi được từ common |
| Pod / Swift package | xem bài 11 | CocoaPods hoặc SwiftPM |

Tìm thư viện multiplatform: [klibs.io](https://kotlinlang.org/docs/multiplatform/klibs-io.html) — danh mục thư viện KMP, lọc theo nền tảng hỗ trợ.

## Làm trên app MVP

Thêm `kotlinx-datetime` vào `commonMain`, rồi viết:

```kotlin
import kotlinx.datetime.*

fun todayIso(): String =
    Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

fun previousDay(iso: String): String =
    LocalDate.parse(iso).minus(1, DateTimeUnit.DAY).toString()
```

Nối vào hàm `streak` của bài 4. Màn chào hiện "Hôm nay: 2026-10-14".

!!! note "API có thể đổi theo phiên bản"
    Một số API thời gian (như `Clock`) đang chuyển dần vào thư viện chuẩn Kotlin. IDE gạch vàng thì làm theo gợi ý import mới.

## Kiểm tra

- [ ] Ngày hôm nay hiện đúng trên cả 3 nền tảng.
- [ ] Giải thích được vì sao `ktor-client-darwin` không thêm vào `commonMain`.

## Tiếp theo?

- [Bài 6: Compose Multiplatform cơ bản](06-compose-basics.md).
- Đọc thêm: [Android dependencies](https://kotlinlang.org/docs/multiplatform/multiplatform-android-dependencies.html) · [iOS dependencies](https://kotlinlang.org/docs/multiplatform/multiplatform-ios-dependencies.html).

## Docs gốc

[Adding dependencies](https://kotlinlang.org/docs/multiplatform/multiplatform-add-dependencies.html)
