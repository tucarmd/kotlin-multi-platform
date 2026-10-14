# Test

Test viết trong `commonTest` chạy được trên **mọi** target: JVM, Android, iOS simulator. Một test, kiểm cả ba nền tảng.

## Mục tiêu

- Test logic streak và repository trong `commonTest`.
- Chạy test trên JVM và iOS simulator.

## Cài

```kotlin
sourceSets {
    commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
    }
}
```

## Làm trên app MVP

```kotlin
class StreakTest {
    @Test
    fun countsConsecutiveDaysEndingToday() {
        val done = setOf("2026-10-12", "2026-10-13", "2026-10-14")
        assertEquals(3, streak(done, today = "2026-10-14", previousDay = ::previousDay))
    }

    @Test
    fun breaksOnMissingDay() {
        val done = setOf("2026-10-11", "2026-10-13", "2026-10-14")
        assertEquals(2, streak(done, today = "2026-10-14", previousDay = ::previousDay))
    }

    @Test
    fun zeroWhenTodayNotDone() {
        assertEquals(0, streak(setOf("2026-10-13"), today = "2026-10-14", previousDay = ::previousDay))
    }
}
```

Test ViewModel: truyền `InMemoryHabitRepository` (bản giả của bài 8), dùng `runTest` của `kotlinx-coroutines-test`.
Đây là lý do [bài 4](04-share-code.md) khuyên dùng interface: test không cần DB hay mạng thật.

## Chạy

| Lệnh | Chạy test trên |
|---|---|
| `./gradlew :composeApp:allTests` | mọi target, gộp báo cáo |
| `./gradlew :composeApp:desktopTest` | JVM desktop (nhanh nhất khi đang code) |
| `./gradlew :composeApp:iosSimulatorArm64Test` | iOS simulator |

Tên task phụ thuộc tên target; gõ `./gradlew :composeApp:tasks --group verification` để xem danh sách đúng.

## Test UI

Compose Multiplatform có API test UI chung (`runComposeUiTest`) để tìm node theo chữ, bấm và kiểm. Bắt đầu từ một test:
"bấm dòng thói quen → checkbox được tick".

## Kiểm tra

- [ ] 3 test streak xanh trên desktop **và** iOS simulator.
- [ ] Cố tình sửa sai hàm `streak` → test đỏ.

## Tiếp theo?

- [Bài 13: build và phát hành](13-build-publish.md).

## Docs gốc

[Test your multiplatform app](https://kotlinlang.org/docs/multiplatform/multiplatform-run-tests.html) · [Testing Compose Multiplatform UI](https://kotlinlang.org/docs/multiplatform/compose-test.html)
