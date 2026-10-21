# Thêm thư viện

Thư viện **multiplatform** thêm vào `commonMain` một lần là dùng được mọi nơi. Thư viện chỉ của một nền tảng thì thêm vào source set của nền tảng đó.
Bài này cho Device Pulse bốn thư viện nền: coroutines, datetime, Koin (DI) và Kermit (log).

## Mục tiêu

- Thêm thư viện qua version catalog, đúng source set.
- Dựng Koin với `commonModule` + `platformModule` để sau này cắm cảm biến thật.
- Ghi log chung cho cả Android và iOS bằng Kermit.

## Version catalog

Phiên bản thư viện nằm ở `gradle/libs.versions.toml`. Không chép số phiên bản từ đây — **xem phiên bản mới nhất** trên trang từng thư viện:

```toml
[versions]
kotlinx-coroutines = "…"
kotlinx-datetime = "…"
koin = "…"
kermit = "…"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
kermit = { module = "co.touchlab:kermit", version.ref = "kermit" }
```

## Thư viện nào cho source set nào

Dấu `-` trong alias thành dấu `.`: `kotlinx-coroutines-core` → `libs.kotlinx.coroutines.core`.

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.datetime)
        implementation(libs.koin.core)
        implementation(libs.kermit)
    }
    androidMain.dependencies {
        implementation(libs.kotlinx.coroutines.android)   // Dispatchers.Main cho Android
        implementation(libs.koin.android)                 // androidContext()
    }
}
```

| Thư viện | Thêm vào | Ghi chú |
|---|---|---|
| coroutines-core, datetime, koin-core, kermit | `commonMain` | multiplatform |
| coroutines-android, koin-android | `androidMain` | chỉ Android |
| Ktor engine OkHttp / Darwin | `androidMain` / `iosMain` | bài 11 |

Tìm thư viện multiplatform: [klibs.io](https://kotlinlang.org/docs/multiplatform/klibs-io.html) — lọc theo nền tảng hỗ trợ.

## Koin: commonModule + platformModule

Cảm biến cài đặt khác nhau trên mỗi nền tảng, nhưng state chung (`PulseMonitor`, bài 8) chỉ biết interface. Koin nối hai đầu:

=== "commonMain · di/Koin.kt"

    ```kotlin
    import org.koin.core.context.startKoin
    import org.koin.core.module.Module
    import org.koin.dsl.KoinAppDeclaration
    import org.koin.dsl.module

    val commonModule = module {
        // bài 8: single { PulseCore(get(), get(), get()) }
    }

    expect val platformModule: Module

    fun initKoin(config: KoinAppDeclaration? = null) = startKoin {
        config?.invoke(this)
        modules(commonModule, platformModule)
    }
    ```

=== "androidMain"

    ```kotlin
    actual val platformModule: Module = module {
        // bài 9–11: single<BatterySensor> { AndroidBatterySensor(androidContext()) } ...
    }

    class DevicePulseApp : Application() {
        override fun onCreate() {
            super.onCreate()
            initKoin { androidContext(this@DevicePulseApp) }
        }
    }
    ```

=== "iosMain"

    ```kotlin
    actual val platformModule: Module = module {
        // bài 9–11: single<BatterySensor> { IosBatterySensor() } ...
    }
    ```

Android: khai báo `android:name=".DevicePulseApp"` trong `AndroidManifest.xml`.
iOS: trong `iosApp/iosApp/iOSApp.swift`, thêm `init() { KoinKt.doInitKoin() }` (file `Koin.kt` → class `KoinKt`; hàm bắt đầu bằng `init` được Swift thấy là `doInitKoin`). Từ bài 6 bật SKIE thì gọi gọn `doInitKoin()`.

## Kermit: log chung

```kotlin
import co.touchlab.kermit.Logger

private val log = Logger.withTag("DevicePulse")
log.i { "Đang chạy trên ${platformName()}" }
```

Log ra Logcat trên Android, ra console Xcode trên iOS — một dòng code.

## Làm trên Device Pulse

1. Thêm các alias vào `libs.versions.toml`, khai báo dependency như trên, *Sync Gradle*.
2. Tạo `di/Koin.kt` + hai `actual val platformModule` (để trống), gọi `initKoin` ở Android và iOS.
3. Ghi một dòng log Kermit lúc khởi động, kèm giờ hiện tại:

```kotlin
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
log.i { "Device Pulse khởi động lúc $now" }
```

!!! note "API thời gian đang chuyển chỗ"
    `Clock` và `Instant` đã vào thư viện chuẩn (`kotlin.time`); `kotlinx-datetime` lo múi giờ, `LocalDateTime`, định dạng. IDE gạch vàng thì làm theo gợi ý import mới.

## Kiểm tra

- [ ] Gradle sync xanh; app vẫn chạy trên **Android** và **iOS**.
- [ ] Thấy dòng log "Device Pulse khởi động lúc …" trong Logcat (**Android**) và console Xcode (**iOS**).
- [ ] Koin khởi động không lỗi trên cả hai nền tảng (không có `KoinApplication has not been started`).
- [ ] Giải thích được vì sao `koin-android` không thêm vào `commonMain`.

## Tiếp theo?

- [Bài 6: UI native — Jetpack Compose + SwiftUI](06-compose-basics.md) — dựng Dashboard ba thẻ hai lần trên cùng dữ liệu giả.
- Đọc thêm: [Android dependencies](https://kotlinlang.org/docs/multiplatform/multiplatform-android-dependencies.html) · [iOS dependencies](https://kotlinlang.org/docs/multiplatform/multiplatform-ios-dependencies.html).

## Docs gốc

[Adding dependencies](https://kotlinlang.org/docs/multiplatform/multiplatform-add-dependencies.html) · [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
