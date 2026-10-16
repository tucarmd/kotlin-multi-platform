# 16. Test (K751–K800)

Lỗi khi viết và chạy test cho code chung KMP: test xanh trên JVM mà iOS không compile, `runTest` không chạy job nền,
Turbine chờ mãi một giá trị không bao giờ đến, fake hay mock, database trong RAM. Đọc kèm phụ lục
[Unit test cơ bản](../appendix/unit-basics.md), [Coroutine & Flow](../appendix/unit-coroutines-flow.md) và
[Fake & mock](../appendix/unit-fakes-mocks.md).

### K751 · Tên test có dấu phẩy: JVM chạy, iOS báo "Name contains illegal characters"

`🟢 mới học` · `KMP` · `iOS`

**Gặp:** `testDebugUnitTest` xanh, nhưng `allTests` dừng ở `:shared:compileTestKotlinIosSimulatorArm64` với
`SplitTableTest.kt:53:9 Name contains illegal characters: ",".`

**Vì sao:** tên trong dấu backtick được JVM chấp nhận gần như mọi ký tự. Kotlin/Native (iOS) cấm thêm vài ký tự, trong đó
có dấu phẩy.

**Sửa:** viết tên test bằng chữ, số và dấu cách. Thay dấu phẩy bằng "and".

```kotlin
fun `invariants hold for every bill, tip, head count and rounding`() { }    // ❌ iOS không compile
fun `invariants hold for every bill and tip and head count and rounding`() { }  // ✅
```

**Gặp ở:** [`apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/SplitTableTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/SplitTableTest.kt) · [`apps/ObdLink/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/README.md)
(mục bẫy cuối) · Xem thêm: K752, K753, [Unit test cơ bản](../appendix/unit-basics.md#at-ten-test)

### K752 · Tên test có ngoặc `()`, dấu chấm, hai chấm

`🟢 mới học` · `KMP` · `iOS`

**Gặp:** `TipCoreFlowTest.kt:61:9 Name contains illegal characters: "()".` khi build iOS; hoặc IDE báo đỏ ngay với `.` `:`.

**Vì sao:** `.` `;` `:` `[` `]` `/` `<` `>` bị cấm ở mọi nền tảng. `(` `)` và `,` thì JVM cho qua, Kotlin/Native thì không.
Viết `` `refresh() is ignored` `` hay `` `0.5 rounds up` `` là vấp.

**Sửa:** tả bằng chữ: "refresh is ignored", "half rounds up", "41 0C 1A F8 is 1726 rpm".

```kotlin
fun `state() emits once`() { }        // ❌ iOS
fun `state emits once`() { }          // ✅
```

**Gặp ở:** [`apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt) · Xem thêm: K751

### K753 · Chỉ chạy test JVM, không bao giờ biết iOS hỏng

`🟢 mới học` · `KMP` · `Gradle`

**Gặp:** bấm ▶ trong Android Studio hoặc chạy `testDebugUnitTest` thấy xanh, đẩy lên thì CI (hoặc đồng đội trên Mac khác) đỏ
ở `iosSimulatorArm64Test`.

**Vì sao:** `testDebugUnitTest` chỉ chạy JVM. Lỗi chỉ có trên Kotlin/Native (tên test, API chỉ có trên JVM, khác biệt số
thực, thread) không lộ ra.

**Sửa:** trước khi commit chạy `allTests`, nó chạy mọi target.

```bash
./gradlew :shared:testDebugUnitTest     # ❌ chỉ JVM
./gradlew :shared:allTests              # ✅ JVM + iOS simulator
```

Xem thêm: K751, K796

### K754 · `import org.junit.Test` trong `commonTest`

`🟢 mới học` · `KMP`

**Gặp:** `Unresolved reference 'junit'` khi build iOS, hoặc IDE gợi ý import JUnit và test không chạy ở target iOS.

**Vì sao:** JUnit là thư viện JVM. Code trong `commonTest` chỉ được dùng thứ có trên mọi target.

**Sửa:** dùng annotation và hàm assert của `kotlin.test`. Trên JVM, `kotlin.test` tự chạy bằng JUnit bên dưới.

```kotlin
import org.junit.Test                 // ❌
import org.junit.Assert.assertEquals  // ❌
import kotlin.test.Test               // ✅
import kotlin.test.assertEquals       // ✅
```

Xem thêm: K755

### K755 · `Unresolved reference 'test'` trong `commonTest`

`🟢 mới học` · `Gradle` · `KMP`

**Gặp:** `import kotlin.test.Test` báo `Unresolved reference 'test'`; hoặc `runTest` không có.

**Vì sao:** `commonTest` chưa có dependency. `kotlin.test` và `kotlinx-coroutines-test` không tự có sẵn.

**Sửa:**

```kotlin
commonTest.dependencies {
    implementation(kotlin("test"))                 // @Test, assertEquals…
    implementation(libs.kotlinx.coroutines.test)   // runTest, đồng hồ ảo
    implementation(libs.turbine)                   // flow.test { }
}
```

**Gặp ở:** [`apps/RateWatch/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/build.gradle.kts)

### K756 · `assertEquals` đảo thứ tự: message nói ngược

`🟢 mới học` · `Kotlin`

**Gặp:** test báo `expected:<36666> but was:<36667>`, nhưng 36.667 mới là số đúng. Mất thời gian sửa nhầm chỗ.

**Vì sao:** chữ ký là `assertEquals(expected, actual, message)`. Đặt kết quả thật vào chỗ `expected` là message đảo nghĩa.

**Sửa:** giá trị mong đợi đứng trước, giá trị tính ra đứng sau, thêm message nói đang kiểm gì.

```kotlin
assertEquals(perPerson(110_000, 3), 36_667)                          // ❌
assertEquals(36_667, perPerson(110_000, 3), "110.000đ chia 3 người")  // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbi50ZXN0LioKCmZ1biBwZXJQZXJzb24odG90YWw6IExvbmcsIHBlb3BsZTogSW50KTogTG9uZyA9IHRvdGFsIC8gcGVvcGxlICAgLy8gbOG7l2kgY-G7kSDDvTogY2hpYSBsw6BtIHRyw7JuIHh14buRbmcKCmZ1biBtYWluKCkgewogICAgdHJ5IHsKICAgICAgICBhc3NlcnRFcXVhbHMocGVyUGVyc29uKDExMF8wMDAsIDMpLCAzNl82NjcpICAgLy8g4p2MIMSR4bqjbyB0aOG7qSB04buxOiBhY3R1YWwgxJHhu6luZyB0csaw4bubYwogICAgfSBjYXRjaCAoZTogQXNzZXJ0aW9uRXJyb3IpIHsKICAgICAgICBwcmludGxuKCLinYwgIiArIGUubWVzc2FnZSkKICAgIH0KICAgIHRyeSB7CiAgICAgICAgYXNzZXJ0RXF1YWxzKDM2XzY2NywgcGVyUGVyc29uKDExMF8wMDAsIDMpLCAiMTEwLjAwMMSRIGNoaWEgMyBuZ8aw4budaSIpICAgLy8g4pyFIGV4cGVjdGVkIHRyxrDhu5tjLCBjw7MgbWVzc2FnZQogICAgfSBjYXRjaCAoZTogQXNzZXJ0aW9uRXJyb3IpIHsKICAgICAgICBwcmludGxuKCLinIUgIiArIGUubWVzc2FnZSkKICAgIH0KfQo) · Xem thêm: K758

### K757 · So `Double` bằng `assertEquals` lúc xanh lúc đỏ

`🟢 mới học` · `Kotlin`

**Gặp:** `expected:<0.3> but was:<0.30000000000000004>`.

**Vì sao:** số thực nhị phân không biểu diễn đúng 0.1, 0.2, 0.3. Phép tính khác thứ tự một chút là khác ở chữ số cuối.

**Sửa:** so với sai số cho phép (`absoluteTolerance`). Với tiền, dùng `Long` đơn vị nhỏ nhất thì so bằng tuyệt đối được.

```kotlin
assertEquals(0.3, 0.1 + 0.2)                            // ❌
assertEquals(0.3, 0.1 + 0.2, absoluteTolerance = 1e-9)  // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbi50ZXN0LioKCmZ1biBtYWluKCkgewogICAgdmFsIHRvdGFsID0gMC4xICsgMC4yCiAgICBwcmludGxuKCIwLjEgKyAwLjIgPSAkdG90YWwiKQogICAgdHJ5IHsKICAgICAgICBhc3NlcnRFcXVhbHMoMC4zLCB0b3RhbCkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBzbyBi4bqxbmcgdHV54buHdCDEkeG7kWkKICAgIH0gY2F0Y2ggKGU6IEFzc2VydGlvbkVycm9yKSB7CiAgICAgICAgcHJpbnRsbigi4p2MICIgKyBlLm1lc3NhZ2UpCiAgICB9CiAgICBhc3NlcnRFcXVhbHMoMC4zLCB0b3RhbCwgYWJzb2x1dGVUb2xlcmFuY2UgPSAxZS05KSAgICAgICAgICAgIC8vIOKchSBjaG8gcGjDqXAgc2FpIHPhu5EKICAgIHByaW50bG4oIuKchSBxdWEgduG7m2kgYWJzb2x1dGVUb2xlcmFuY2UgPSAxZS05IikKfQo)

### K758 · Test bảng đỏ mà không biết dòng nào

`🟢 mới học` · `Kotlin`

**Gặp:** test có 20 case trong một vòng `for`, đỏ với đúng một dòng `expected:<50001> but was:<50000>`.

**Vì sao:** `assertEquals` không biết nó đang ở vòng lặp nào. Thiếu `message` là mất thông tin quan trọng nhất.

**Sửa:** mỗi case có tên, và đưa tên vào `message`.

```kotlin
for (c in cases) assertEquals(c.expected, ceilDiv(c.total, c.people))                    // ❌
for (c in cases) assertEquals(c.expected, ceilDiv(c.total, c.people), "case '${c.name}'") // ✅
```

**Gặp ở:** [`apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/SplitTableTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/SplitTableTest.kt) ·
[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbi50ZXN0LioKCmZ1biBjZWlsRGl2KGE6IExvbmcsIGI6IExvbmcpOiBMb25nID0gYSAvIGIgICAvLyBs4buXaSBj4buRIMO9OiBwaOG6o2kgbMOgbSB0csOybiBMw4pOCgpkYXRhIGNsYXNzIENhc2UodmFsIG5hbWU6IFN0cmluZywgdmFsIHRvdGFsOiBMb25nLCB2YWwgcGVvcGxlOiBMb25nLCB2YWwgZXhwZWN0ZWQ6IExvbmcpCgp2YWwgY2FzZXMgPSBsaXN0T2YoCiAgICBDYXNlKCJjaGlhIGjhur90IiwgOTBfMDAwLCAzLCAzMF8wMDApLAogICAgQ2FzZSgiMSBuZ8aw4budaSIsIDUwXzAwMCwgMSwgNTBfMDAwKSwKICAgIENhc2UoImTGsCAxIMSR4buTbmciLCAxMDBfMDAxLCAyLCA1MF8wMDEpLAopCgpmdW4gbWFpbigpIHsKICAgIHRyeSB7CiAgICAgICAgZm9yIChjIGluIGNhc2VzKSBhc3NlcnRFcXVhbHMoYy5leHBlY3RlZCwgY2VpbERpdihjLnRvdGFsLCBjLnBlb3BsZSkpICAgICAgICAgICAgICAgICAgIC8vIOKdjAogICAgfSBjYXRjaCAoZTogQXNzZXJ0aW9uRXJyb3IpIHsgcHJpbnRsbigi4p2MICIgKyBlLm1lc3NhZ2UpIH0KICAgIHRyeSB7CiAgICAgICAgZm9yIChjIGluIGNhc2VzKSBhc3NlcnRFcXVhbHMoYy5leHBlY3RlZCwgY2VpbERpdihjLnRvdGFsLCBjLnBlb3BsZSksICJjYXNlICcke2MubmFtZX0nIikgLy8g4pyFCiAgICB9IGNhdGNoIChlOiBBc3NlcnRpb25FcnJvcikgeyBwcmludGxuKCLinIUgIiArIGUubWVzc2FnZSkgfQp9Cg) · Xem thêm: [Test bảng](../appendix/unit-basics.md#test-bang-mot-test-nhieu-case)

### K759 · Test chưa từng đỏ

`🟡 đã làm app` · `Kotlin`

**Gặp:** code sai rõ ràng mà test vẫn xanh: assert nhầm biến, quên gọi hàm cần kiểm, hoặc `for` trên một list rỗng.

**Vì sao:** test viết xong chạy xanh ngay lần đầu thì chưa có bằng chứng nó kiểm được gì.

**Sửa:** "phá rồi sửa": cố tình làm sai code (đổi `+ b - 1` thành không có), chạy test, phải thấy đỏ đúng case, rồi trả lại.

```kotlin
internal fun ceilDiv(a: Long, b: Long): Long = a / b   // phá cố ý → SplitTableTest phải đỏ
```

**Gặp ở:** [`apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/SplitTableTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/SplitTableTest.kt) ·
Xem thêm: [Phá rồi sửa](../appendix/unit-basics.md#pha-roi-sua-test-co-bat-uoc-loi-that-khong)

### K760 · `@Test suspend fun` không chạy

`🟢 mới học` · `Kotlin` · `KMP`

**Gặp:** test `suspend` bị bỏ qua hoặc JUnit báo `Method … should have no parameters`.

**Vì sao:** test runner gọi hàm không tham số. Hàm `suspend` sau khi compile có thêm tham số `Continuation`.

**Sửa:** hàm test thường, thân là `runTest { }`.

```kotlin
@Test suspend fun loads() { repo.load() }               // ❌
@Test fun loads() = runTest { repo.load() }             // ✅
```

Xem thêm: K761

### K761 · Test có `delay` chạy mất vài giây

`🟢 mới học` · `Kotlin`

**Gặp:** bộ test chạy càng ngày càng lâu; một test "chờ 2 giây rồi làm mới" tốn đúng 2 giây thật.

**Vì sao:** `runBlocking` dùng đồng hồ thật. `runTest` dùng **đồng hồ ảo**: `delay` chỉ đẩy đồng hồ đi, trả về ngay.

**Sửa:** mọi test coroutine dùng `runTest`. Đọc giờ ảo bằng `currentTime` (hoặc `testScheduler.currentTime`).

```kotlin
@Test fun cooldown() = runBlocking { refreshAfterCooldown() }   // ❌ 2 giây thật
@Test fun cooldown() = runTest { refreshAfterCooldown() }       // ✅ vài mili giây
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMudGVzdC4qCmltcG9ydCBrb3RsaW4udGltZS5tZWFzdXJlVGltZQoKc3VzcGVuZCBmdW4gcmVmcmVzaEFmdGVyQ29vbGRvd24oKTogU3RyaW5nIHsgZGVsYXkoMl8wMDApOyByZXR1cm4gInJlZnJlc2hlZCIgfQoKZnVuIG1haW4oKSB7CiAgICB2YWwgcmVhbCA9IG1lYXN1cmVUaW1lIHsgcnVuQmxvY2tpbmcgeyByZWZyZXNoQWZ0ZXJDb29sZG93bigpIH0gfSAgICAgICAgICAvLyDinYwgY2jhu50gdGjhuq10IDIgZ2nDonkKICAgIHByaW50bG4oInJ1bkJsb2NraW5nOiAke3JlYWwuaW5XaG9sZU1pbGxpc2Vjb25kc30gbXMgdGjhuq10IikKICAgIHZhbCB2aXJ0dWFsID0gbWVhc3VyZVRpbWUgewogICAgICAgIHJ1blRlc3QgewogICAgICAgICAgICByZWZyZXNoQWZ0ZXJDb29sZG93bigpCiAgICAgICAgICAgIHByaW50bG4oInJ1blRlc3Q6IMSR4buTbmcgaOG7kyDhuqNvID0gJHtjdXJyZW50VGltZX0gbXMiKQogICAgICAgIH0KICAgIH0KICAgIHByaW50bG4oInJ1blRlc3Q6ICR7dmlydHVhbC5pbldob2xlTWlsbGlzZWNvbmRzfSBtcyB0aOG6rXQiKSAgICAgICAgICAgICAgICAvLyDinIUgZ-G6p24gbmjGsCAwCn0K) · Xem thêm: K765

### K762 · `runTest` không chạy job nền: `launch` xong mà chưa thấy gì

`🟢 mới học` · `Kotlin`

**Gặp:** gọi `core.start()` (bên trong có `scope.launch { … }`) rồi assert ngay, state vẫn là giá trị ban đầu.

**Vì sao:** `runTest` dùng `StandardTestDispatcher`: `launch` chỉ **xếp hàng**, chưa chạy cho tới khi test nhường lượt
(suspend, `runCurrent()`, `advanceUntilIdle()`).

**Sửa:** sau thao tác, gọi `runCurrent()` (chạy việc đến hạn ngay bây giờ) hoặc `advanceUntilIdle()` (chạy hết, tua đồng hồ ảo).

```kotlin
loader.start()
assertTrue(loader.loaded)        // ❌ launch chưa chạy
loader.start()
advanceUntilIdle()               // ✅
assertTrue(loader.loaded)
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMudGVzdC4qCgpjbGFzcyBMb2FkZXIocHJpdmF0ZSB2YWwgc2NvcGU6IENvcm91dGluZVNjb3BlKSB7CiAgICB2YXIgbG9hZGVkID0gZmFsc2UKICAgIGZ1biBzdGFydCgpIHsgc2NvcGUubGF1bmNoIHsgbG9hZGVkID0gdHJ1ZSB9IH0KfQoKZnVuIG1haW4oKSA9IHJ1blRlc3QgewogICAgdmFsIGxvYWRlciA9IExvYWRlcih0aGlzKQogICAgbG9hZGVyLnN0YXJ0KCkKICAgIHByaW50bG4oIm5nYXkgc2F1IHN0YXJ0KCk6IGxvYWRlZCA9ICR7bG9hZGVyLmxvYWRlZH0iKSAgIC8vIOKdjCBmYWxzZTogbGF1bmNoIG3hu5tpIHjhur9wIGjDoG5nLCBjaMawYSBjaOG6oXkKICAgIHJ1bkN1cnJlbnQoKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIOKchSBob-G6t2MgYWR2YW5jZVVudGlsSWRsZSgpCiAgICBwcmludGxuKCJzYXUgcnVuQ3VycmVudCgpOiBsb2FkZWQgPSAke2xvYWRlci5sb2FkZWR9IikKfQo) · Xem thêm: K763, K767

### K763 · Core chạy trong `backgroundScope`: `advanceUntilIdle()` không chờ nó

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** mọi assert đều thấy state ban đầu, dù đã `advanceUntilIdle()`. Bỏ `backgroundScope` thì test lại treo vì core có
vòng lặp không tự dừng.

**Vì sao:** `advanceUntilIdle()` dừng khi không còn việc **foreground**. Việc trong `backgroundScope` không được tính, nên
một `delay` trong đó có thể không bao giờ được tua tới.

**Sửa:** cho core một scope riêng chạy trên `testScheduler` của test, và huỷ nó trong `@AfterTest`. Chỉ dùng
`backgroundScope` khi test tự tua đồng hồ bằng `advanceTimeBy` (như `ObdCoreTest`).

```kotlin
private fun TestScope.core(): RatesCore {
    val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))   // ✅
    coreScope = scope
    return RatesCore(repo, clock, scope)
}
@AfterTest fun stopCore() { coreScope?.cancel() }
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt) ·
[`apps/ObdLink/shared/src/commonTest/kotlin/dev/obdlink/ObdCoreTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/shared/src/commonTest/kotlin/dev/obdlink/ObdCoreTest.kt) ·
[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMudGVzdC4qCgpjbGFzcyBDb3JlKHByaXZhdGUgdmFsIHNjb3BlOiBDb3JvdXRpbmVTY29wZSkgewogICAgdmFyIHJlZnJlc2hlZCA9IDAKICAgIGZ1biBzdGFydCgpIHsgc2NvcGUubGF1bmNoIHsgZGVsYXkoMV8wMDApOyByZWZyZXNoZWQrKyB9IH0KfQoKZnVuIG1haW4oKSA9IHJ1blRlc3QgewogICAgdmFsIHdyb25nID0gQ29yZShiYWNrZ3JvdW5kU2NvcGUpICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCB2aeG7h2MgbuG7gW46IGFkdmFuY2VVbnRpbElkbGUoKSBraMO0bmcgY2jhu50KICAgIHdyb25nLnN0YXJ0KCkKICAgIGFkdmFuY2VVbnRpbElkbGUoKQogICAgcHJpbnRsbigiYmFja2dyb3VuZFNjb3BlOiByZWZyZXNoZWQgPSAke3dyb25nLnJlZnJlc2hlZH0iKQoKICAgIHZhbCBjb3JlU2NvcGUgPSBDb3JvdXRpbmVTY29wZShTdXBlcnZpc29ySm9iKCkgKyBTdGFuZGFyZFRlc3REaXNwYXRjaGVyKHRlc3RTY2hlZHVsZXIpKSAgIC8vIOKchSBuaMawIFJhdGVzQ29yZVRlc3QKICAgIHZhbCByaWdodCA9IENvcmUoY29yZVNjb3BlKQogICAgcmlnaHQuc3RhcnQoKQogICAgYWR2YW5jZVVudGlsSWRsZSgpCiAgICBwcmludGxuKCJzY29wZSByacOqbmcgdHLDqm4gdGVzdFNjaGVkdWxlcjogcmVmcmVzaGVkID0gJHtyaWdodC5yZWZyZXNoZWR9IikKICAgIGNvcmVTY29wZS5jYW5jZWwoKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBk4buNbiBk4bq5cCwgbmjGsCBAQWZ0ZXJUZXN0Cn0K) · Xem thêm: K764

### K764 · Vòng lặp vô hạn trong test: "After waiting for 1m, there were active child jobs"

`🟡 đã làm app` · `Kotlin`

**Gặp:** test treo cả phút rồi đỏ với `UncompletedCoroutinesError: After waiting for 1m, there were active child jobs: […].
Use `TestScope.backgroundScope` to launch the coroutines that need to be cancelled when the test body finishes`.

**Vì sao:** `runTest` chờ mọi coroutine con xong mới kết thúc. Một vòng `while (true) { delay(…) }` (polling, đồng hồ đếm)
launch trong scope của test thì không bao giờ xong. Thời gian chờ mặc định của `runTest` là 60 giây.

**Sửa:** việc không tự dừng thì launch trong `backgroundScope` (bị huỷ khi thân test xong) và tua bằng `advanceTimeBy`,
hoặc truyền scope riêng rồi huỷ trong `@AfterTest` (K763).

```kotlin
@Test fun polls() = runTest {
    launch { while (true) { delay(1_000); poll() } }               // ❌ test không bao giờ xong
    backgroundScope.launch { while (true) { delay(1_000); poll() } } // ✅
    advanceTimeBy(3_500)
}
```

**Gặp ở:** [`apps/ObdLink/shared/src/commonTest/kotlin/dev/obdlink/ObdCoreTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/shared/src/commonTest/kotlin/dev/obdlink/ObdCoreTest.kt) · Xem thêm: K763

### K765 · `withContext(Dispatchers.Default)` trong code: đồng hồ ảo mất tác dụng

`🟡 đã làm app` · `Kotlin` · `KMP`

**Gặp:** đã dùng `runTest` mà test vẫn chạy chậm theo thời gian thật, hoặc assert chạy trước khi kết quả về (flaky).

**Vì sao:** đồng hồ ảo chỉ áp dụng cho coroutine chạy trên dispatcher của test. Code nhảy sang `Dispatchers.Default`/`IO` thì
`delay` ở đó là thật, và chạy song song trên thread khác.

**Sửa:** đưa dispatcher (hoặc scope) vào qua constructor, có giá trị mặc định cho app; test truyền dispatcher của test.

```kotlin
class Repo(private val io: CoroutineDispatcher = Dispatchers.Default) {
    suspend fun load() = withContext(io) { … }
}
Repo(StandardTestDispatcher(testScheduler))     // ✅ trong test
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMudGVzdC4qCmltcG9ydCBrb3RsaW4udGltZS5tZWFzdXJlVGltZQoKY2xhc3MgUmVwbyhwcml2YXRlIHZhbCBpbzogQ29yb3V0aW5lRGlzcGF0Y2hlciA9IERpc3BhdGNoZXJzLkRlZmF1bHQpIHsKICAgIHN1c3BlbmQgZnVuIHNsb3dMb2FkKCk6IFN0cmluZyA9IHdpdGhDb250ZXh0KGlvKSB7IGRlbGF5KDFfNTAwKTsgImRhdGEiIH0KfQoKZnVuIG1haW4oKSB7CiAgICB2YWwgd3JvbmcgPSBtZWFzdXJlVGltZSB7IHJ1blRlc3QgeyBSZXBvKCkuc2xvd0xvYWQoKSB9IH0gICAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBkZWxheSBjaOG6oXkgdHLDqm4gRGVmYXVsdDogdGjhu51pIGdpYW4gdGjhuq10CiAgICBwcmludGxuKCJEaXNwYXRjaGVycy5EZWZhdWx0OiAke3dyb25nLmluV2hvbGVNaWxsaXNlY29uZHN9IG1zIHRo4bqtdCIpCiAgICB2YWwgcmlnaHQgPSBtZWFzdXJlVGltZSB7IHJ1blRlc3QgeyBSZXBvKFN0YW5kYXJkVGVzdERpc3BhdGNoZXIodGVzdFNjaGVkdWxlcikpLnNsb3dMb2FkKCkgfSB9ICAgLy8g4pyFIHRpw6ptIGRpc3BhdGNoZXIKICAgIHByaW50bG4oImRpc3BhdGNoZXIgY-G7p2EgdGVzdDogJHtyaWdodC5pbldob2xlTWlsbGlzZWNvbmRzfSBtcyB0aOG6rXQiKQp9Cg) · Xem thêm: K766, K770

### K766 · Đọc `Clock.System.now()` trong code: test đúng 10 giờ, sai lúc nửa đêm

`🟡 đã làm app` · `Kotlin` · `KMP`

**Gặp:** test "hôm qua" xanh ban ngày, đỏ lúc 00:01; test "cũ hơn 1 giờ" không viết được nếu không chờ 1 giờ.

**Vì sao:** giờ hệ thống là input ẩn. Mỗi lần chạy là một input khác.

**Sửa:** "bây giờ" là tham số. Hàm luật nhận `now`; core nhận `Clock` qua constructor. Test dùng clock đọc một biến, nên
"1 giờ sau" chỉ là một phép gán.

```kotlin
private var nowMillis = 10 * hour
private val clock = object : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(nowMillis)
}
// test: nowMillis += hour
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt) ·
[`apps/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/rules/Time.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/shared/src/commonMain/kotlin/dev/pocketnotes/rules/Time.kt) · Xem thêm: K750

### K767 · `advanceTimeBy(1000)` mà việc hẹn đúng 1000 ms chưa chạy

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** `launch { delay(1_000); fired = true }`, rồi `advanceTimeBy(1_000)`, nhưng `fired` vẫn `false`.

**Vì sao:** `advanceTimeBy(d)` chạy các việc hẹn **trước** mốc `now + d` (khoảng mở bên phải), rồi đặt đồng hồ đúng
bằng mốc đó. Việc hẹn đúng mốc còn nằm trong hàng đợi.

**Sửa:** gọi thêm `runCurrent()` sau `advanceTimeBy`, hoặc tua quá mốc một chút.

```kotlin
advanceTimeBy(1_000)
runCurrent()           // ✅ chạy việc đến hạn đúng tại 1000 ms
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMudGVzdC4qCgpmdW4gbWFpbigpID0gcnVuVGVzdCB7CiAgICB2YXIgZmlyZWQgPSBmYWxzZQogICAgbGF1bmNoIHsgZGVsYXkoMV8wMDApOyBmaXJlZCA9IHRydWUgfQoKICAgIGFkdmFuY2VUaW1lQnkoMV8wMDApCiAgICBwcmludGxuKCJhZHZhbmNlVGltZUJ5KDEwMDApOiBmaXJlZCA9ICRmaXJlZCwgY3VycmVudFRpbWUgPSAkY3VycmVudFRpbWUiKSAgIC8vIOKdjCB2aeG7h2MgxJDDmk5HIG3hu5FjIDEwMDAgY2jGsGEgY2jhuqF5CiAgICBydW5DdXJyZW50KCkKICAgIHByaW50bG4oInRow6ptIHJ1bkN1cnJlbnQoKTogICBmaXJlZCA9ICRmaXJlZCIpICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinIUKfQo) · Xem thêm: K762

### K768 · "Module with the Main dispatcher is missing"

`🟢 mới học` · `Android` · `Kotlin`

**Gặp:** test ViewModel trên JVM: `IllegalStateException: Module with the Main dispatcher is missing. Add dependency providing
the Main dispatcher, e.g. 'kotlinx-coroutines-android' and ensure it has the same version as 'kotlinx-coroutines-core'`.

**Vì sao:** `viewModelScope` chạy trên `Dispatchers.Main`. Unit test JVM không có main thread của Android.

**Sửa:** thay Main bằng dispatcher của test trước mỗi test, trả lại sau mỗi test. Tốt hơn nữa: để logic trong core
nhận `scope` qua constructor và test core, ViewModel chỉ chuyển lời gọi.

```kotlin
@BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
@AfterTest  fun tearDown() = Dispatchers.resetMain()
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMudGVzdC4qCgpjbGFzcyBTY3JlZW5Nb2RlbCB7CiAgICB2YXIgdGV4dCA9ICIiCiAgICBzdXNwZW5kIGZ1biBsb2FkKCkgPSB3aXRoQ29udGV4dChEaXNwYXRjaGVycy5NYWluKSB7IHRleHQgPSAieG9uZyIgfSAgIC8vIGNvZGUgVUkgY2jhuqF5IHRyw6puIE1haW4KfQoKZnVuIG1haW4oKSB7CiAgICB0cnkgewogICAgICAgIHJ1blRlc3QgeyBTY3JlZW5Nb2RlbCgpLmxvYWQoKSB9ICAgICAgICAgICAgLy8g4p2MIHVuaXQgdGVzdCBKVk0ga2jDtG5nIGPDsyBtYWluIHRocmVhZAogICAgfSBjYXRjaCAoZTogSWxsZWdhbFN0YXRlRXhjZXB0aW9uKSB7CiAgICAgICAgcHJpbnRsbigi4p2MICIgKyBlLm1lc3NhZ2UpCiAgICB9CiAgICBEaXNwYXRjaGVycy5zZXRNYWluKFN0YW5kYXJkVGVzdERpc3BhdGNoZXIoKSkgICAvLyDinIUgQEJlZm9yZVRlc3QKICAgIHRyeSB7CiAgICAgICAgcnVuVGVzdCB7CiAgICAgICAgICAgIHZhbCBtID0gU2NyZWVuTW9kZWwoKQogICAgICAgICAgICBtLmxvYWQoKQogICAgICAgICAgICBwcmludGxuKCLinIUgdGV4dCA9ICR7bS50ZXh0fSIpCiAgICAgICAgfQogICAgfSBmaW5hbGx5IHsKICAgICAgICBEaXNwYXRjaGVycy5yZXNldE1haW4oKSAgICAgICAgICAgICAgICAgICAgIC8vIOKchSBAQWZ0ZXJUZXN0CiAgICB9Cn0K) · Xem thêm: K769, [ViewModel Android](../appendix/unit-coroutines-flow.md#viewmodel-android-dispatcherssetmain)

### K769 · Quên `resetMain()`: test sau hỏng theo test trước

`🟡 đã làm app` · `Android` · `Kotlin`

**Gặp:** chạy riêng từng test thì xanh, chạy cả class/module thì vài test đỏ ngẫu nhiên, lỗi dính tới dispatcher hoặc
scheduler của test khác.

**Vì sao:** `Dispatchers.setMain` thay Main cho **cả process**. Không `resetMain()` thì test sau dùng dispatcher (và đồng hồ ảo)
còn sót lại của test trước.

**Sửa:** luôn đặt cặp `@BeforeTest setMain` / `@AfterTest resetMain`. Dùng nhiều thì gom vào một class cha hoặc một JUnit rule
(chỉ trong `androidUnitTest`).

Xem thêm: K768, K791

### K770 · "Detected use of different schedulers"

`🟡 đã làm app` · `Kotlin`

**Gặp:** `IllegalStateException: Detected use of different schedulers. If you need to use several test coroutine dispatchers,
create one `TestCoroutineScheduler` and pass it to each of them.`

**Vì sao:** mỗi `StandardTestDispatcher()` không tham số tạo một scheduler (đồng hồ ảo) mới. Test có hai đồng hồ ảo thì
không biết tua cái nào.

**Sửa:** mọi test dispatcher trong một test dùng chung `testScheduler`.

```kotlin
val other = StandardTestDispatcher()                 // ❌ đồng hồ thứ hai
val shared = StandardTestDispatcher(testScheduler)   // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMudGVzdC4qCgpmdW4gbWFpbigpIHsKICAgIHRyeSB7CiAgICAgICAgcnVuVGVzdCB7CiAgICAgICAgICAgIHZhbCBvdGhlciA9IFN0YW5kYXJkVGVzdERpc3BhdGNoZXIoKSAgICAgICAgICAvLyDinYwgdOG6oW8gc2NoZWR1bGVyIE3hu5pJCiAgICAgICAgICAgIHdpdGhDb250ZXh0KG90aGVyKSB7IGRlbGF5KDEwKSB9CiAgICAgICAgfQogICAgfSBjYXRjaCAoZTogRXhjZXB0aW9uKSB7CiAgICAgICAgcHJpbnRsbigi4p2MICIgKyBlLm1lc3NhZ2UpCiAgICB9CiAgICBydW5UZXN0IHsKICAgICAgICB2YWwgc2hhcmVkID0gU3RhbmRhcmRUZXN0RGlzcGF0Y2hlcih0ZXN0U2NoZWR1bGVyKSAvLyDinIUgZMO5bmcgY2h1bmcgc2NoZWR1bGVyIGPhu6dhIHRlc3QKICAgICAgICB3aXRoQ29udGV4dChzaGFyZWQpIHsgZGVsYXkoMTApIH0KICAgICAgICBwcmludGxuKCLinIUgY3VycmVudFRpbWUgPSAkY3VycmVudFRpbWUiKQogICAgfQp9Cg) · Xem thêm: K765

### K771 · Turbine: "No value produced in 3s"

`🟢 mới học` · `Kotlin`

**Gặp:** `awaitItem()` đỏ với `No value produced in 3s`.

**Vì sao:** flow không phát gì mới trong 3 giây (thời gian **thật**, kể cả trong `runTest`). Thường do thao tác không đổi
state (K774), việc chạy trên scope không ai tua (K763), hoặc phát trước khi Turbine kịp nghe (K776).

**Sửa:** tìm xem thao tác có thật sự đổi state không. Nếu "không phát gì" là đúng ý, khẳng định bằng `expectNoEvents()`.

```kotlin
core.state.test {
    awaitItem()               // state ban đầu
    core.onTipSelected(10)    // 10% đang được chọn sẵn
    awaitItem()               // ❌ No value produced in 3s
    expectNoEvents()          // ✅ đúng ý: không vẽ lại
}
```

**Gặp ở:** [`apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt) · Xem thêm: K772, K774

### K772 · Turbine: quên giá trị đầu tiên của `StateFlow`

`🟢 mới học` · `Kotlin`

**Gặp:** `awaitItem()` đầu tiên trả về state **ban đầu** chứ không phải state sau thao tác; assert so sai giá trị.

**Vì sao:** `StateFlow` luôn phát giá trị hiện tại ngay khi có người nghe. Turbine nhận nó trước mọi thứ khác.

**Sửa:** đọc (và kiểm) giá trị ban đầu trước, rồi mới thao tác.

```kotlin
core.state.test {
    assertEquals(SplitResult.Waiting, awaitItem().result)   // ✅ giá trị hiện tại trước
    core.onBillChanged("100.000")
    assertEquals(55_000, assertIs<SplitResult.Ready>(awaitItem().result).split.perPerson)
}
```

**Gặp ở:** [`apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt)

### K773 · Turbine: "Unconsumed events found"

`🟢 mới học` · `Kotlin`

**Gặp:** test đỏ ở cuối khối `test { }` với `Unconsumed events found:` rồi danh sách `- Item(…)`.

**Vì sao:** Turbine bắt buộc đọc hết mọi giá trị đã phát. Còn sót giá trị nghĩa là flow phát nhiều hơn test nghĩ (thường là
một lần vẽ lại thừa), hoặc test không quan tâm phần còn lại.

**Sửa:** nếu giá trị thừa là bug, sửa code. Nếu đúng là không quan tâm, kết thúc bằng `cancelAndIgnoreRemainingEvents()`.

```kotlin
flow.test {
    assertEquals(1, awaitItem())
    cancelAndIgnoreRemainingEvents()   // ✅ cố ý bỏ phần còn lại
}
```

Xem thêm: K771

### K774 · Chờ một lần phát mà `StateFlow` không phát vì giá trị bằng nhau

`🟡 đã làm app` · `Kotlin`

**Gặp:** `removePerson()` khi đã ở mức tối thiểu, test `awaitItem()` báo `No value produced in 3s`.

**Vì sao:** `StateFlow` bỏ qua giá trị **bằng** giá trị hiện tại (so bằng `equals`, data class so theo nội dung). Đây là
tính năng: UI không vẽ lại khi không có gì đổi.

**Sửa:** viết test khẳng định điều đó bằng `expectNoEvents()`.

```kotlin
core.removePerson()                         // 2 → 1: phát
assertEquals(1, awaitItem().input.people)
core.removePerson()                         // đã là 1: state y hệt
expectNoEvents()                            // ✅
```

**Gặp ở:** [`apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt) (`an action that changes nothing does not emit`)

### K775 · `launch { collect }` trong test thiếu giá trị ở giữa

`🟡 đã làm app` · `Kotlin`

**Gặp:** gõ "1", "10", "100.000" mà list thu được chỉ có `["", "100.000"]`.

**Vì sao:** `StateFlow` chỉ giữ **một** giá trị (conflation). Người thu chạy trên `StandardTestDispatcher` chỉ thức dậy khi
test nhường lượt, lúc đó chỉ thấy giá trị mới nhất.

**Sửa:** muốn thấy từng bước thì dùng Turbine (nó thu ngay khi có giá trị), hoặc nhường lượt sau mỗi thao tác
(`runCurrent()`). Còn nếu đang kiểm "UI không cần thấy trạng thái trung gian" thì đây chính là kết quả đúng.

```kotlin
val job = launch { core.state.toList(seen) }
core.onBillChanged("1"); core.onBillChanged("10"); core.onBillChanged("100.000")
advanceUntilIdle()
assertEquals(listOf("", "100.000"), seen.map { it.input.billText })   // "1" và "10" bị gộp
```

**Gặp ở:** [`apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt) ·
[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMuZmxvdy4qCmltcG9ydCBrb3RsaW54LmNvcm91dGluZXMudGVzdC4qCgpmdW4gbWFpbigpID0gcnVuVGVzdCB7CiAgICB2YWwgc3RhdGUgPSBNdXRhYmxlU3RhdGVGbG93KCIiKQogICAgdmFsIHNlZW4gPSBtdXRhYmxlTGlzdE9mPFN0cmluZz4oKQogICAgdmFsIGpvYiA9IGxhdW5jaCB7IHN0YXRlLnRvTGlzdChzZWVuKSB9ICAgIC8vIG5nxrDhu51pIHRodSBjaOG6oXkgdHLDqm4gU3RhbmRhcmRUZXN0RGlzcGF0Y2hlcgogICAgcnVuQ3VycmVudCgpCgogICAgc3RhdGUudmFsdWUgPSAiMSIgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIGfDtSAzIGzhuqduIGxp4buBbiwga2jDtG5nIG5oxrDhu51uZyBsxrDhu6N0CiAgICBzdGF0ZS52YWx1ZSA9ICIxMCIKICAgIHN0YXRlLnZhbHVlID0gIjEwMC4wMDAiCiAgICBydW5DdXJyZW50KCkKICAgIGpvYi5jYW5jZWwoKQoKICAgIHByaW50bG4oIm5nxrDhu51pIHRodSB0aOG6pXk6ICRzZWVuIikgICAgICAgICAgIC8vICIxIiB2w6AgIjEwIiBi4buLIGfhu5lwIG3huqV0IChjb25mbGF0aW9uKQp9Cg) · Xem thêm: K771

### K776 · `SharedFlow` phát trước khi test kịp nghe

`🟡 đã làm app` · `Kotlin`

**Gặp:** sự kiện một lần (toast, điều hướng) phát qua `MutableSharedFlow()`; test gọi hàm rồi mới `events.test { awaitItem() }`
thì chờ mãi.

**Vì sao:** `SharedFlow` không có `replay` thì giá trị phát lúc chưa ai nghe là mất luôn. Khác `StateFlow`, nó không giữ giá trị
hiện tại.

**Sửa:** mở `test { }` **trước**, thao tác **bên trong** khối đó.

```kotlin
core.save()
core.events.test { awaitItem() }        // ❌ đã phát xong từ trước

core.events.test {
    core.save()                         // ✅ đang nghe rồi mới thao tác
    assertEquals(Event.Saved, awaitItem())
}
```

Xem thêm: K771

### K777 · Fake hay mock?

`🟡 đã làm app` · `KMP`

**Gặp:** test mock nào cũng 15 dòng `every { }` trước khi kiểm được một điều; hoặc ngược lại, fake tự viết dài bằng code thật.

**Vì sao:** hai công cụ kiểm hai thứ khác nhau. **Fake** là bản cài đặt nhỏ chạy thật, test kiểm **kết quả** (state).
**Mock** không có hành vi tới khi được kịch bản hoá, test kiểm **lời gọi** (hàm nào, tham số gì, mấy lần, thứ tự).

**Sửa:** mặc định dùng fake. Chỉ dùng mock khi lời gọi **chính là** hành vi cần bảo vệ: "chỉ tải 1 lần", "lưu base trước khi tải".

```kotlin
verifySuspend(exactly(1)) { repo.refresh("USD", any()) }   // mock: đúng chỗ, số lần gọi là hành vi
assertEquals(RefreshError.Offline, core.state.value.error) // fake: kiểm kết quả
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt) (`FakeRateRepository`) ·
[`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreMockTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreMockTest.kt) ·
Xem thêm: K783, [Fake & mock](../appendix/unit-fakes-mocks.md#fake-hay-mock)

### K778 · MockK trong `commonTest`: iOS không build

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** `Unresolved reference 'mockk'` khi build test iOS, hoặc Gradle không resolve được MockK cho target iOS.

**Vì sao:** MockK (và Mockito) tạo mock bằng reflection của JVM. Không có trên Kotlin/Native.

**Sửa:** trong `commonTest` dùng Mokkery (compiler plugin, sinh mock lúc build, chạy cả iOS), hoặc viết fake.
MockK chỉ dùng trong `androidUnitTest`.

```kotlin
// shared/build.gradle.kts
plugins { alias(libs.plugins.mokkery) }   // ✅ không cần thêm dependency nào
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreMockTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreMockTest.kt) · Xem thêm: K782

### K779 · Mokkery: "… not mocked!"

`🟢 mới học` · `KMP`

**Gặp:** `CallNotMockedException` với message dạng `Call RateRepository(1).observeBase() not mocked!`, ngay khi tạo core,
trước cả phần muốn test.

**Vì sao:** mock của Mokkery mặc định nghiêm ngặt: hàm nào được gọi mà chưa `every { }` là ném lỗi. Core thường gọi vài
hàm lúc khởi tạo (ở đây là các hàm `observe…`).

**Sửa:** kịch bản hoá các hàm khởi tạo ngay trong `mock { }`, phần riêng của từng test thì đặt trong test.

```kotlin
private val repo = mock<RateRepository> {
    every { observeBase() } returns flowOf(null)
    every { observe(any()) } returns flowOf(null)
    every { observeFavorites() } returns flowOf(emptySet())
    everySuspend { setBase(any()) } returns Unit
}
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreMockTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreMockTest.kt) · Xem thêm: K780

### K780 · `every { }` với hàm `suspend`

`🟢 mới học` · `KMP`

**Gặp:** lỗi compile `Suspend function 'refresh' should be called only from a coroutine or another suspend function` trong
`every { repo.refresh(…) }`.

**Vì sao:** lambda của `every` / `verify` không phải `suspend`. Hàm `suspend` có cặp hàm riêng.

**Sửa:** `everySuspend` để kịch bản hoá, `verifySuspend` để kiểm.

```kotlin
every { repo.refresh(any(), any()) } returns Result.success(Unit)          // ❌ không compile
everySuspend { repo.refresh(any(), any()) } returns Result.success(Unit)   // ✅
verifySuspend(exactly(1)) { repo.refresh("USD", any()) }                   // ✅
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreMockTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreMockTest.kt)

### K781 · Mock một class `final`

`🟡 đã làm app` · `KMP`

**Gặp:** `mock<SqlRateRepository>()` báo lỗi lúc compile test: class final không mock được.

**Vì sao:** class Kotlin mặc định `final`. Mock cần tạo lớp con. Mock được interface thì không vướng.

**Sửa:** để core phụ thuộc vào **interface** (`RateRepository`), class thật cài đặt interface đó. Mock (hoặc fake) interface.
Đây cũng là thiết kế tốt hơn: core không biết gì về SQL.

```kotlin
class RatesCore(private val repository: SqlRateRepository)   // ❌ khó thay trong test
class RatesCore(private val repository: RateRepository)      // ✅ interface
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt)

### K782 · Nâng Kotlin xong, test không build vì Mokkery

`🟡 đã làm app` · `Gradle` · `KMP`

**Gặp:** sau khi nâng `kotlin` trong catalog, task compile test lỗi ở plugin Mokkery.

**Vì sao:** Mokkery là compiler plugin, gắn chặt với version Kotlin compiler. Mỗi bản Mokkery chỉ hỗ trợ một dải version Kotlin
(3.5.1 chạy với Kotlin 2.4.20).

**Sửa:** nâng Mokkery **cùng lúc** với Kotlin, xem bảng tương thích trong changelog của Mokkery. Áp dụng chung cho mọi compiler
plugin (SKIE, Compose compiler, serialization).

```toml
kotlin = "2.4.20"
mokkery = "3.5.1"   # nâng cùng lúc với kotlin
```

**Gặp ở:** [`apps/RateWatch/gradle/libs.versions.toml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/gradle/libs.versions.toml) · Xem thêm: K707

### K783 · Test mock đỏ sau khi refactor, dù app vẫn đúng

`🟡 đã làm app` · `KMP`

**Gặp:** gộp hai lời gọi repository thành một (app chạy y như cũ), mười test mock đỏ cùng lúc.

**Vì sao:** `verify` từng lời gọi là kiểm **chi tiết cài đặt**. Đổi cách cài đặt là test đỏ dù hành vi không đổi.

**Sửa:** chỉ `verify` lời gọi khi chính lời gọi là yêu cầu (số lần tải, thứ tự lưu). Còn lại kiểm kết quả bằng fake.

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreMockTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreMockTest.kt) · Xem thêm: K777

### K784 · Fake xanh, app thật hỏng: SQL sai mà không test nào thấy

`🟡 đã làm app` · `KMP`

**Gặp:** test core với fake repository đều xanh, nhưng app thật nháy màn hình trống khi làm mới, hoặc query sai cột.

**Vì sao:** fake chỉ giống code thật ở mức interface. File `.sq`, transaction và đoạn đổi hàng ↔ object không được test nào chạy.

**Sửa:** thêm vài test **tích hợp** cho repository thật với SQLite trong RAM và `MockEngine`. Fake cho test core,
repository thật cho test tầng dữ liệu.

```kotlin
private fun repository() = SqlRateRepository(
    inMemoryRateDbDriver(),                                      // SQLite thật, trong RAM
    RateApi(createHttpClient(MockEngine { respond(USD_OK, HttpStatusCode.OK, jsonHeaders) })),
)
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/SqlRateRepositoryTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/SqlRateRepositoryTest.kt) · Xem thêm: K788

### K785 · Test gọi server thật

`🟢 mới học` · `KMP`

**Gặp:** test lúc xanh lúc đỏ: mất mạng, server chậm, dữ liệu thay đổi mỗi ngày; CI đỏ vì rate limit.

**Vì sao:** server thật không nằm trong tầm kiểm soát của test. Và không có cách nào ép nó trả 500 hay JSON hỏng.

**Sửa:** tạo client bằng **đúng hàm của app** nhưng với `MockEngine`, kịch bản hoá từng response. Ktor `MockEngine` nằm ở
`ktor-client-mock`, thêm vào `commonTest`.

```kotlin
private fun api(handler: suspend MockRequestHandleScope.() -> HttpResponseData) =
    RateApi(createHttpClient(MockEngine { handler() }))

@Test fun `http 500 becomes a server error`() = runTest {
    val api = api { respond("oops", HttpStatusCode.InternalServerError) }
    assertEquals(RefreshError.ServerError, assertFailsWith<RateApiException> { api.latest("USD") }.reason)
}
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt) · Xem thêm: K786, K787

### K786 · `MockEngine` trả JSON mà vẫn `NoTransformationFoundException`

`🟢 mới học` · `KMP`

**Gặp:** test với `MockEngine { respond(json) }` đỏ: `NoTransformationFoundException: Expected response body of the type …`,
dòng `Response header ContentType: null`.

**Vì sao:** `respond(content, status, headers)` mặc định **không có** header nào. Thiếu `Content-Type: application/json` thì
`ContentNegotiation` không nhận body là JSON.

**Sửa:** luôn gửi kèm header.

```kotlin
private val json = headersOf(HttpHeaders.ContentType, "application/json")
MockEngine { respond(SAMPLE_OK) }                           // ❌
MockEngine { respond(SAMPLE_OK, HttpStatusCode.OK, json) }  // ✅
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt) · Xem thêm: K720

### K787 · Muốn test timeout và mất mạng bằng `MockEngine`

`🟡 đã làm app` · `KMP`

**Gặp:** không biết làm sao cho `MockEngine` "chậm" hay "mất mạng"; test timeout thật thì phải chờ 10 giây.

**Vì sao:** handler của `MockEngine` là một lambda `suspend` bình thường: có thể `delay` hoặc ném exception. Với `runTest`,
`delay` là thời gian ảo.

**Sửa:** timeout: tạo client với timeout ngắn và handler `delay` lâu hơn. Mất mạng: handler ném `IOException`. Huỷ: ném
`CancellationException` và kiểm nó đi xuyên qua.

```kotlin
val slow = api(timeoutMillis = 50) { delay(5_000); respond(SAMPLE_OK, HttpStatusCode.OK, json) }
assertEquals(RefreshError.Timeout, failure(slow))

assertEquals(RefreshError.Offline, failure(api { throw IOException("Unable to resolve host") }))
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt) · Xem thêm: K728, K733

### K788 · `JdbcSqliteDriver` trong `commonTest`: iOS không build

`🟡 đã làm app` · `KMP`

**Gặp:** `Unresolved reference 'JdbcSqliteDriver'` khi build test iOS, hoặc phải dời cả test repository sang `androidUnitTest`
(thế là SQL không được kiểm trên iOS).

**Vì sao:** JDBC chỉ có trên JVM. iOS có driver native riêng.

**Sửa:** `expect/actual` dùng được cả trong **source set test**. `commonTest` khai báo hàm tạo driver trong RAM;
`androidUnitTest` cài bằng JDBC, `iosTest` cài bằng `inMemoryDriver`.

```kotlin
// commonTest
expect fun inMemoryRateDbDriver(): SqlDriver
// androidUnitTest
actual fun inMemoryRateDbDriver(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { RateDb.Schema.create(it) }
// iosTest
actual fun inMemoryRateDbDriver(): SqlDriver = inMemoryDriver(RateDb.Schema)
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/TestDriver.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/TestDriver.kt) ·
[`apps/RateWatch/shared/src/androidUnitTest/kotlin/dev/ratewatch/TestDriver.android.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/androidUnitTest/kotlin/dev/ratewatch/TestDriver.android.kt) ·
[`apps/RateWatch/shared/src/iosTest/kotlin/dev/ratewatch/TestDriver.ios.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/iosTest/kotlin/dev/ratewatch/TestDriver.ios.kt) ·
[`apps/PocketNotes/shared/src/androidUnitTest/kotlin/dev/pocketnotes/NoteRepositoryTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/shared/src/androidUnitTest/kotlin/dev/pocketnotes/NoteRepositoryTest.kt) (cách cũ, chỉ JVM)

### K789 · Test DB báo "no such table"

`🟢 mới học` · `KMP`

**Gặp:** `SQLiteException: [SQLITE_ERROR] SQL error or missing database (no such table: noteRecord)` ở câu query đầu tiên.

**Vì sao:** `JdbcSqliteDriver(IN_MEMORY)` mở một database **trống**. Driver Android/iOS tự tạo bảng khi nhận `Schema`, còn
JDBC thì không.

**Sửa:** gọi `Database.Schema.create(driver)` ngay sau khi tạo driver JDBC. (`inMemoryDriver(Schema)` bên iOS đã tự tạo.)

```kotlin
val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)            // ❌ chưa có bảng
val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { NotesDb.Schema.create(it) }  // ✅
```

**Gặp ở:** [`apps/PocketNotes/shared/src/androidUnitTest/kotlin/dev/pocketnotes/NoteRepositoryTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/PocketNotes/shared/src/androidUnitTest/kotlin/dev/pocketnotes/NoteRepositoryTest.kt) (`freshRepository`)

### K790 · Các test DB dính dữ liệu của nhau

`🟡 đã làm app` · `KMP`

**Gặp:** test "danh sách rỗng lúc đầu" đỏ vì thấy dữ liệu do test khác ghi; đổi thứ tự chạy là kết quả đổi.

**Vì sao:** dùng chung một driver (biến top-level, `object`, `companion`) giữa các test, nên database sống qua nhiều test.

**Sửa:** mỗi test một driver mới, đóng trong `@AfterTest`. Đặt driver là property của class test: `kotlin.test` tạo instance
mới cho mỗi test.

```kotlin
class SqlRateRepositoryTest {
    private val driver = inMemoryRateDbDriver()   // ✅ mới cho từng test
    @AfterTest fun close() = driver.close()
}
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/SqlRateRepositoryTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/SqlRateRepositoryTest.kt) · Xem thêm: K791

### K791 · Test chạy riêng thì xanh, chạy chung thì đỏ

`🟡 đã làm app` · `Kotlin` · `KMP`

**Gặp:** bấm ▶ một test: xanh. Chạy cả module: đỏ, và mỗi lần đỏ một test khác.

**Vì sao:** test dùng chung trạng thái sống qua nhiều test: `object` singleton, biến top-level, Koin đã `startKoin`,
`Dispatchers.setMain` chưa reset. Thứ tự chạy test không được đảm bảo.

**Sửa:** mỗi test tự tạo mọi thứ nó cần (fake, driver, core) trong property của class hoặc trong `@BeforeTest`; dọn trong
`@AfterTest`. Đừng `startKoin` trong unit test, truyền phụ thuộc qua constructor.

Xem thêm: K769, K790

### K792 · API chỉ có trên JVM trong `commonTest`

`🟡 đã làm app` · `KMP`

**Gặp:** test đọc file GPX bằng `javax.xml.parsers` compile được trên JVM, iOS báo `Unresolved reference 'javax'`.

**Vì sao:** `java.*`, `javax.*`, `org.w3c.*` không có trên Kotlin/Native.

**Sửa:** test cần thư viện JVM thì đặt trong `androidUnitTest` (chỉ chạy JVM) và ghi rõ lý do. Phần logic vẫn test ở
`commonTest` bằng API chung.

**Gặp ở:** [`apps/TripLog/shared/src/androidUnitTest/kotlin/dev/triplog/GpxParseTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TripLog/shared/src/androidUnitTest/kotlin/dev/triplog/GpxParseTest.kt) · Xem thêm: K788

### K793 · "Method … not mocked" khi unit test gọi `android.*`

`🟡 đã làm app` · `Android`

**Gặp:** `java.lang.RuntimeException: Method d in android.util.Log not mocked.` (kèm link tới trang hướng dẫn của Android).

**Vì sao:** unit test JVM chạy với một `android.jar` chỉ có chữ ký hàm; mọi hàm Android đều ném lỗi khi được gọi.

**Sửa:** tốt nhất là giữ logic khỏi `android.*` (đưa vào `commonMain`, log qua interface). Tạm thời có thể cho các hàm đó
trả giá trị mặc định, hoặc dùng Robolectric khi thật sự cần Android.

```kotlin
android {
    testOptions { unitTests.isReturnDefaultValues = true }   // hàm Android trả 0 / null / false thay vì ném
}
```

### K794 · Chạy một test có dấu cách trong tên

`🟢 mới học` · `Gradle`

**Gặp:** `--tests` không tìm thấy test nào (`No tests found for given includes`) khi gõ tên test có dấu cách.

**Vì sao:** `--tests` nhận tên đầy đủ `package.Class.method` và hỗ trợ `*`. Tên có dấu cách phải nằm trong ngoặc kép.

**Sửa:**

```bash
./gradlew :shared:testDebugUnitTest --tests "dev.tipsplit.SplitTableTest"                   # cả class
./gradlew :shared:testDebugUnitTest --tests "dev.tipsplit.SplitTableTest.every row*"       # một test
./gradlew :shared:testDebugUnitTest --tests "*rounded up*"                                  # theo một phần tên
```

Xem thêm: [Chạy test](../appendix/unit-basics.md#chay-test)

### K795 · Chạy lại test mà Gradle báo `UP-TO-DATE`, không chạy gì

`🟢 mới học` · `Gradle`

**Gặp:** muốn chạy lại để xem test flaky, Gradle in `> Task :shared:testDebugUnitTest UP-TO-DATE` và xong ngay.

**Vì sao:** code và test không đổi thì Gradle dùng kết quả cũ (cache). Đó là tính năng cho build nhanh.

**Sửa:** ép chạy lại đúng task đó bằng `--rerun`.

```bash
./gradlew :shared:testDebugUnitTest --rerun
```

### K796 · Test iOS: chỉ chạy trên Mac, và cần đúng simulator

`🟡 đã làm app` · `KMP` · `iOS`

**Gặp:** CI chạy Linux không có task test iOS; trên Mac, `iosSimulatorArm64Test` báo không tìm thấy thiết bị simulator.

**Vì sao:** target iOS chỉ build được trên macOS có Xcode. Task test chạy trên một simulator mặc định lấy từ Xcode; máy chưa
cài runtime iOS tương ứng thì không có thiết bị đó.

**Sửa:** CI chạy `allTests` trên máy macOS. Chọn simulator có sẵn bằng option `--device`.

```bash
xcrun simctl list devices available                                  # xem tên simulator có trên máy
./gradlew :shared:iosSimulatorArm64Test --device "iPhone 16"
```

Xem thêm: K753

### K797 · `assertFailsWith<Exception>` xanh với mọi lỗi

`🟢 mới học` · `Kotlin`

**Gặp:** test "base lạ thì ném `RateApiException`" xanh, nhưng thật ra code ném `NullPointerException` vì `!!`.

**Vì sao:** `assertFailsWith<Exception>` chấp nhận **mọi** exception con. Test không phân biệt lỗi đúng ý với crash.

**Sửa:** ghi đúng loại exception mong đợi; nó trả về exception nên kiểm tiếp được nội dung.

```kotlin
assertFailsWith<Exception> { api.latest("XYZ") }                       // ❌
val e = assertFailsWith<RateApiException> { api.latest("XYZ") }        // ✅
assertEquals(RefreshError.UnknownBase, e.reason)
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbi50ZXN0LioKCmNsYXNzIFJhdGVBcGlFeGNlcHRpb24odmFsIHJlYXNvbjogU3RyaW5nKSA6IEV4Y2VwdGlvbihyZWFzb24pCgpmdW4gbGF0ZXN0KGJhc2U6IFN0cmluZyk6IERvdWJsZSB7CiAgICB2YWwgcmF0ZXMgPSBtYXBPZigiVVNEIiB0byAxLjApCiAgICByZXR1cm4gcmF0ZXNbYmFzZV0hISAgICAgICAgICAgICAgICAgICAgICAgLy8gbOG7l2k6IGJhc2UgbOG6oSB0aMOsIE51bGxQb2ludGVyRXhjZXB0aW9uLCBraMO0bmcgcGjhuqNpIFJhdGVBcGlFeGNlcHRpb24KfQoKZnVuIG1haW4oKSB7CiAgICBhc3NlcnRGYWlsc1dpdGg8RXhjZXB0aW9uPiB7IGxhdGVzdCgiWFlaIikgfSAgICAgICAgICAgICAgICAgICAgIC8vIOKdjCBxdWE6IEV4Y2VwdGlvbiBuw6BvIGPFqW5nIMSRxrDhu6NjCiAgICBwcmludGxuKCLinYwgYXNzZXJ0RmFpbHNXaXRoPEV4Y2VwdGlvbj4gduG6q24geGFuaCIpCiAgICB0cnkgewogICAgICAgIGFzc2VydEZhaWxzV2l0aDxSYXRlQXBpRXhjZXB0aW9uPiB7IGxhdGVzdCgiWFlaIikgfSAgICAgICAgICAvLyDinIUgYuG6r3QgxJHDum5nIGxv4bqhaSDihpIgxJHhu48sIGzhu5kgbOG7l2kgdGjhuq10CiAgICB9IGNhdGNoIChlOiBBc3NlcnRpb25FcnJvcikgewogICAgICAgIHByaW50bG4oIuKchSDEkeG7jyDEkcO6bmc6ICIgKyBlLm1lc3NhZ2UpCiAgICB9Cn0K) · Xem thêm: K800

### K798 · Muốn test hàm `private`

`🟢 mới học` · `Kotlin` · `KMP`

**Gặp:** `Cannot access 'ceilDiv': it is private in file`.

**Vì sao:** `private` chỉ thấy trong cùng file/class. Test nằm ở file khác.

**Sửa:** hàm phụ đáng test riêng thì để `internal`: thấy được trong cùng module, kể cả `commonTest`, nhưng không lộ ra
ngoài module (Swift cũng không thấy). Đừng dùng reflection.

```kotlin
private fun ceilDiv(a: Long, b: Long): Long = (a + b - 1) / b    // ❌ test không gọi được
internal fun ceilDiv(a: Long, b: Long): Long = (a + b - 1) / b   // ✅
```

**Gặp ở:** [`apps/TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/rules/Rules.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/commonMain/kotlin/dev/tipsplit/rules/Rules.kt)

### K799 · Không có test nào bảo vệ việc huỷ coroutine

`🔴 khó, hiếm nhưng đau` · `KMP`

**Gặp:** ai đó đổi `resultOf` về `runCatching` cho gọn, mọi test vẫn xanh, và bug K731 quay lại.

**Vì sao:** test thường chỉ đi đường thành công và đường lỗi. Đường "bị huỷ" không ai viết.

**Sửa:** viết test riêng: `CancellationException` phải đi xuyên qua hàm bọc lỗi, còn lỗi thường thì thành `Result.failure`.

```kotlin
@Test fun `resultOf lets cancellation through`() = runTest {
    assertFailsWith<CancellationException> { resultOf { throw CancellationException("stop") } }
    assertTrue(resultOf { error("boom") }.isFailure)
}
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RatesCoreTest.kt) ·
[`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt) (`cancellation is passed through and never wrapped`) ·
Xem thêm: K731, K733

### K800 · `assertTrue(x is Ready)` rồi ép kiểu bằng tay

`🟢 mới học` · `Kotlin`

**Gặp:** test đỏ chỉ báo `Expected value to be true.`, không biết thực tế là kiểu gì; dòng sau còn phải `as SplitResult.Ready`.

**Vì sao:** `assertTrue` chỉ biết `true`/`false`. Smart cast cũng không đi qua lời gọi hàm.

**Sửa:** `assertIs<T>(value)` vừa kiểm kiểu, vừa báo kiểu thật khi sai, vừa trả về giá trị đã ép kiểu.

```kotlin
assertTrue(r is SplitResult.Ready)                         // ❌
val ready = assertIs<SplitResult.Ready>(split(100_000, 3)) // ✅
assertEquals(33_334, ready.perPerson)
```

**Gặp ở:** [`apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/TipSplit/shared/src/commonTest/kotlin/dev/tipsplit/TipCoreFlowTest.kt) ·
[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbi50ZXN0LioKCnNlYWxlZCBjbGFzcyBTcGxpdFJlc3VsdCB7CiAgICBkYXRhIG9iamVjdCBXYWl0aW5nIDogU3BsaXRSZXN1bHQoKQogICAgZGF0YSBjbGFzcyBSZWFkeSh2YWwgcGVyUGVyc29uOiBMb25nKSA6IFNwbGl0UmVzdWx0KCkKfQoKZnVuIHNwbGl0KGJpbGw6IExvbmcsIHBlb3BsZTogSW50KTogU3BsaXRSZXN1bHQgPQogICAgaWYgKGJpbGwgPD0gMCkgU3BsaXRSZXN1bHQuV2FpdGluZyBlbHNlIFNwbGl0UmVzdWx0LlJlYWR5KChiaWxsICsgcGVvcGxlIC0gMSkgLyBwZW9wbGUpCgpmdW4gbWFpbigpIHsKICAgIHZhbCByID0gc3BsaXQoMTAwXzAwMCwgMykKICAgIGFzc2VydFRydWUociBpcyBTcGxpdFJlc3VsdC5SZWFkeSkgICAgICAgICAgICAgICAgICAgICAgICAgLy8g4p2MIMSR4buPIHRow6wgY2jhu4kgYsOhbyAiRXhwZWN0ZWQgdmFsdWUgdG8gYmUgdHJ1ZS4iCiAgICBwcmludGxuKChyIGFzIFNwbGl0UmVzdWx0LlJlYWR5KS5wZXJQZXJzb24pICAgICAgICAgICAgICAgIC8vICAgIHLhu5NpIGPDsm4gcGjhuqNpIMOpcCBraeG7g3UgdGF5CgogICAgdmFsIHJlYWR5ID0gYXNzZXJ0SXM8U3BsaXRSZXN1bHQuUmVhZHk-KHNwbGl0KDEwMF8wMDAsIDMpKSAvLyDinIUga2nhu4NtICsgdHLhuqMgduG7gSDEkcO6bmcga2nhu4N1CiAgICBhc3NlcnRFcXVhbHMoMzNfMzM0LCByZWFkeS5wZXJQZXJzb24pCiAgICB0cnkgewogICAgICAgIGFzc2VydElzPFNwbGl0UmVzdWx0LlJlYWR5PihzcGxpdCgwLCAzKSkKICAgIH0gY2F0Y2ggKGU6IEFzc2VydGlvbkVycm9yKSB7CiAgICAgICAgcHJpbnRsbigi4pyFICIgKyBlLm1lc3NhZ2UpCiAgICB9Cn0K) · Xem thêm: K797
