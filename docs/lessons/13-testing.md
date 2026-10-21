# Test

Test viết trong `commonTest` chạy trên **mọi** target: JVM của Android và iOS simulator. Một test, kiểm cả hai nền tảng.
Nhờ sensor là interface ([bài 4](04-share-code.md)), phần lớn Device Pulse test được mà không cần GPS, pin hay mạng thật.

## Mục tiêu

- Test các luật `batteryLevel`, `fixQuality`, `latencyGrade` ở đúng ngưỡng.
- Test `PulseMonitor` (state chung của hai UI) với sensor giả, dùng `kotlinx-coroutines-test` + Turbine.
- Test `LatencyProbe` với `MockEngine` của Ktor — không gọi mạng thật.
- Có một bảng test tay cho phần phụ thuộc phần cứng.

## Cài

```kotlin
sourceSets {
    commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.turbine)              // app.cash.turbine:turbine
        implementation(libs.ktor.client.mock)
    }
}
```

## Test luật: kiểm đúng ngưỡng

Lỗi hay nằm ở biên (`<` hay `<=`), nên test ngay hai bên mỗi ngưỡng:

```kotlin
class RulesTest {
    private fun battery(p: Int) = BatteryInfo(p, ChargingState.Discharging)
    private fun fix(acc: Double) = LocationFix(0.0, 0.0, acc, null, Instant.fromEpochMilliseconds(0))

    @Test fun batteryThresholds() {
        assertEquals(BatteryLevel.Critical, batteryLevel(battery(10)))
        assertEquals(BatteryLevel.Low, batteryLevel(battery(11)))
        assertEquals(BatteryLevel.Low, batteryLevel(battery(20)))
        assertEquals(BatteryLevel.Ok, batteryLevel(battery(21)))
    }

    @Test fun fixThresholds() {
        assertEquals(FixQuality.Good, fixQuality(fix(20.0)))
        assertEquals(FixQuality.Fair, fixQuality(fix(20.5)))
        assertEquals(FixQuality.Fair, fixQuality(fix(100.0)))
        assertEquals(FixQuality.Poor, fixQuality(fix(100.5)))
    }

    @Test fun latencyThresholds() {
        assertEquals(LatencyGrade.Fast, latencyGrade(99))
        assertEquals(LatencyGrade.Ok, latencyGrade(100))
        assertEquals(LatencyGrade.Ok, latencyGrade(299))
        assertEquals(LatencyGrade.Slow, latencyGrade(300))
    }
}
```

## Sensor giả

```kotlin
class FakeBatterySensor : BatterySensor {
    val readings = MutableStateFlow<Reading<BatteryInfo>>(Reading.Loading)
    override fun observe(): Flow<Reading<BatteryInfo>> = readings
}
// FakeLocationSensor, FakeNetworkSensor viết y hệt; FakeHistoryRepository giữ một MutableStateFlow<List<Snapshot>>
```

Test đẩy giá trị vào `readings` để giả lập "pin vừa tụt", "mất quyền vị trí", "rớt mạng".

## Test `LatencyProbe` với `MockEngine`

```kotlin
class LatencyProbeTest {
    @Test fun okResponseGivesMillis() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.NoContent) })
        assertIs<Reading.Available<Long>>(LatencyProbe(client).measure())
    }

    @Test fun failureBecomesError() = runTest {
        val client = HttpClient(MockEngine { error("offline") })
        assertIs<Reading.Error>(LatencyProbe(client).measure())
    }
}
```

## Test `PulseMonitor`

`PulseMonitor` chạy trên `scope` được truyền vào, nên test đưa một scope dùng chung bộ lập lịch của test: không cần `Dispatchers.setMain`, không cần Android.

```kotlin
class PulseMonitorTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)   // đóng vai viewModelScope
    @AfterTest fun tearDown() = scope.cancel()

    @Test fun batteryReadingReachesState() = runTest(dispatcher) {
        val battery = FakeBatterySensor()
        val probe = LatencyProbe(HttpClient(MockEngine { respond("", HttpStatusCode.NoContent) }))
        val vm = PulseMonitor(battery, FakeLocationSensor(), FakeNetworkSensor(), probe, FakeHistoryRepository(), scope)

        vm.state.test {
            assertEquals(Reading.Loading, awaitItem().battery)
            battery.readings.value = Reading.Available(BatteryInfo(15, ChargingState.Discharging))
            assertEquals(Reading.Available(BatteryInfo(15, ChargingState.Discharging)), awaitItem().battery)
        }
    }
}
```

Viết thêm theo mẫu đó: `measureLatency()` → `latencyMs` đi từ `Loading` sang `Available`; `saveSnapshot()` → `FakeHistoryRepository` nhận đúng một lần lưu.

## Chạy

| Lệnh | Chạy test trên |
|---|---|
| `./gradlew :shared:allTests` | mọi target, gộp báo cáo HTML trong `shared/build/reports` |
| `./gradlew :shared:testDebugUnitTest` | JVM (Android) — nhanh nhất khi đang code |
| `./gradlew :shared:iosSimulatorArm64Test` | iOS simulator (chỉ chạy trên macOS) |

Tên task phụ thuộc target; gõ `./gradlew :shared:tasks --group verification` để xem danh sách đúng.

## Làm trên Device Pulse: test tay

Phần chạm phần cứng (`AndroidLocationSensor`, `IosNetworkSensor`…) không unit test được dễ dàng — kiểm bằng tay theo bảng:

| Tình huống | Android emulator | iOS Simulator | Mong đợi |
|---|---|---|---|
| Pin thay đổi | Extended Controls → *Battery* | không giả lập được | Android: level + charging đổi; iOS: `Unsupported` |
| Vị trí di chuyển | Extended Controls → *Location* | *Features → Location → City Run* | toạ độ đổi, `fixQuality` đúng |
| Từ chối quyền | từ chối trong hộp thoại | *Settings → Privacy → Location* | card hiện `PermissionDenied` |
| Mất mạng | chế độ máy bay | tắt Wi-Fi của Mac | `None`, đo độ trễ ra lỗi |

Muốn tự động hoá các luồng bấm (mở app → cho quyền → lưu lần đo → mở History), xem **Maestro**: viết kịch bản YAML,
chạy được trên cả Android emulator và iOS Simulator.

## Kiểm tra

- [ ] `RulesTest`, `LatencyProbeTest`, `PulseMonitorTest` xanh với `testDebugUnitTest` (Android/JVM).
- [ ] Cùng các test đó xanh với `iosSimulatorArm64Test`.
- [ ] Cố tình sửa `<=` thành `<` trong `batteryLevel` → `batteryThresholds` đỏ.
- [ ] Đi hết bảng test tay trên Android emulator và iOS Simulator.

Kẹt? Xem [Lỗi hay gặp](../reference/troubleshooting.md).

## Tiếp theo?

- [Bài 14: build và phát hành](14-build-publish.md).

## Docs gốc

[Test your multiplatform app](https://kotlinlang.org/docs/multiplatform/multiplatform-run-tests.html) · [kotlin.test](https://kotlinlang.org/api/core/kotlin-test/) · [Testing coroutines](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/) · [Ktor MockEngine](https://ktor.io/docs/client-testing.html) · [Testing Compose Multiplatform UI](https://kotlinlang.org/docs/multiplatform/compose-test.html) · [Test your Android app](https://developer.android.com/training/testing)
