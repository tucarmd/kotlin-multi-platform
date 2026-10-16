# 15. Mạng & JSON (K701–K750)

Lỗi khi gọi API bằng Ktor và đọc JSON bằng kotlinx.serialization: server thêm field là app crash, lỗi đội lốt HTTP 200,
timeout, mất mạng, và `CancellationException` bị nuốt khiến coroutine chạy tiếp. Phần lớn ví dụ lấy từ
[Rate Watch](../apps/03-rate-watch.md), app có mạng thật (Ktor 3.5.1, kotlinx.serialization 1.11.0).

### K701 · Server thêm một field là app crash: "Encountered an unknown key"

`🟢 mới học` · `Kotlin` · `KMP`

**Gặp:** `JsonDecodingException: Unexpected JSON token at offset 21: Encountered an unknown key 'provider' at path: $`,
kèm gợi ý `Use 'ignoreUnknownKeys = true' in 'Json {}' builder`. Hôm qua chạy được, hôm nay crash dù không sửa gì.

**Vì sao:** `Json` mặc định rất khắt khe: JSON có key nào mà data class không khai báo là ném lỗi. Server thêm field mới
(chuyện bình thường) là mọi bản app đã cài đều hỏng.

**Sửa:** tạo **một** `Json` dùng chung với `ignoreUnknownKeys = true` và đưa nó cho Ktor. Chỉ khai báo field app thật sự đọc.
Muốn nới cho riêng một class thì dùng annotation `@JsonIgnoreUnknownKeys`.

```kotlin
val RateJson = Json { ignoreUnknownKeys = true }            // ✅ một chỗ cho cả app
install(ContentNegotiation) { json(RateJson) }
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/HttpClients.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/HttpClients.kt) ·
[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlCmRhdGEgY2xhc3MgUmF0ZVJlc3BvbnNlKHZhbCByZXN1bHQ6IFN0cmluZywgdmFsIHJhdGVzOiBNYXA8U3RyaW5nLCBEb3VibGU-KQoKLy8gU2VydmVyIHbhu6thIHRow6ptIGZpZWxkICJwcm92aWRlciIgbcOgIGFwcCBjaMawYSBraGFpIGLDoW8uCnZhbCBib2R5ID0gIiIieyJyZXN1bHQiOiJzdWNjZXNzIiwicHJvdmlkZXIiOiJleGNoYW5nZXJhdGUtYXBpIiwicmF0ZXMiOnsiVVNEIjoxLCJWTkQiOjI2MzEyLjV9fSIiIgoKZnVuIG1haW4oKSB7CiAgICB0cnkgewogICAgICAgIHByaW50bG4oSnNvbi5kZWNvZGVGcm9tU3RyaW5nPFJhdGVSZXNwb25zZT4oYm9keSkpICAgICAgICAgICAgICAvLyDinYwgSnNvbiBt4bq3YyDEkeG7i25oCiAgICB9IGNhdGNoIChlOiBTZXJpYWxpemF0aW9uRXhjZXB0aW9uKSB7CiAgICAgICAgcHJpbnRsbigi4p2MICIgKyBlLm1lc3NhZ2UpCiAgICB9CiAgICB2YWwganNvbiA9IEpzb24geyBpZ25vcmVVbmtub3duS2V5cyA9IHRydWUgfSAgICAgICAgICAgICAgICAgICAgICAgICAvLyDinIUKICAgIHByaW50bG4oIuKchSAiICsganNvbi5kZWNvZGVGcm9tU3RyaW5nPFJhdGVSZXNwb25zZT4oYm9keSkpCn0K) · Xem thêm: K702, K711

### K702 · Thiếu một field: "Field 'rates' is required … but it was missing"

`🟢 mới học` · `Kotlin` · `KMP`

**Gặp:** `MissingFieldException: Field 'rates' is required for type with serial name 'RateResponse', but it was missing at path: $`.
Thường xảy ra khi server trả **body lỗi**, có hình dạng khác body thành công.

**Vì sao:** property **không có giá trị mặc định** là bắt buộc. Body lỗi `{"result":"error","error-type":"…"}` không có `rates`.

**Sửa:** field nào có thể vắng thì cho giá trị mặc định (thường là `null`), rồi tự kiểm sau khi decode.

```kotlin
@Serializable
data class RateResponse(
    val result: String,
    val rates: Map<String, Double>,                 // ❌ body lỗi không có → crash
    val rates: Map<String, Double>? = null,         // ✅ rồi tự kiểm: if (rates.isNullOrEmpty()) …
)
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt) ·
[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlCmRhdGEgY2xhc3MgV3JvbmcodmFsIHJlc3VsdDogU3RyaW5nLCB2YWwgcmF0ZXM6IE1hcDxTdHJpbmcsIERvdWJsZT4pCgpAU2VyaWFsaXphYmxlCmRhdGEgY2xhc3MgUmlnaHQodmFsIHJlc3VsdDogU3RyaW5nLCB2YWwgcmF0ZXM6IE1hcDxTdHJpbmcsIERvdWJsZT4_ID0gbnVsbCkgICAvLyDinIUgY8OzIGdpw6EgdHLhu4sgbeG6t2MgxJHhu4tuaAoKLy8gQm9keSBs4buXaSBj4bunYSBBUEk6IGtow7RuZyBjw7MgInJhdGVzIi4KdmFsIGVycm9yID0gIiIieyJyZXN1bHQiOiJlcnJvciIsImVycm9yLXR5cGUiOiJ1bnN1cHBvcnRlZC1jb2RlIn0iIiIKdmFsIGpzb24gPSBKc29uIHsgaWdub3JlVW5rbm93bktleXMgPSB0cnVlIH0KCmZ1biBtYWluKCkgewogICAgdHJ5IHsKICAgICAgICBwcmludGxuKGpzb24uZGVjb2RlRnJvbVN0cmluZzxXcm9uZz4oZXJyb3IpKQogICAgfSBjYXRjaCAoZTogU2VyaWFsaXphdGlvbkV4Y2VwdGlvbikgewogICAgICAgIHByaW50bG4oIuKdjCAiICsgZS5tZXNzYWdlKQogICAgfQogICAgcHJpbnRsbigi4pyFICIgKyBqc29uLmRlY29kZUZyb21TdHJpbmc8UmlnaHQ-KGVycm9yKSkKfQo) · Xem thêm: K703, K723

### K703 · Đã khai báo `String?` mà vẫn báo thiếu field

`🟢 mới học` · `Kotlin`

**Gặp:** `Field 'nick' is required for type with serial name 'User', but it was missing` dù `nick` là `String?`.

**Vì sao:** với kotlinx.serialization, `?` chỉ nghĩa là "được phép là `null`", **không** có nghĩa là "được phép vắng".
Một field chỉ được vắng khi có giá trị mặc định.

**Sửa:** thêm `= null`.

```kotlin
@Serializable data class User(val name: String, val nick: String?)          // ❌ vẫn bắt buộc
@Serializable data class User(val name: String, val nick: String? = null)   // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlCmRhdGEgY2xhc3MgV3JvbmcodmFsIG5hbWU6IFN0cmluZywgdmFsIG5pY2s6IFN0cmluZz8pICAgICAgICAgIC8vIG51bGxhYmxlIG5oxrBuZyBLSMOUTkcgY8OzIG3hurdjIMSR4buLbmgKCkBTZXJpYWxpemFibGUKZGF0YSBjbGFzcyBSaWdodCh2YWwgbmFtZTogU3RyaW5nLCB2YWwgbmljazogU3RyaW5nPyA9IG51bGwpICAgLy8g4pyFCgpmdW4gbWFpbigpIHsKICAgIHZhbCBib2R5ID0gIiIieyJuYW1lIjoiVMO6In0iIiIKICAgIHRyeSB7CiAgICAgICAgcHJpbnRsbihKc29uLmRlY29kZUZyb21TdHJpbmc8V3Jvbmc-KGJvZHkpKQogICAgfSBjYXRjaCAoZTogU2VyaWFsaXphdGlvbkV4Y2VwdGlvbikgewogICAgICAgIHByaW50bG4oIuKdjCAiICsgZS5tZXNzYWdlKQogICAgfQogICAgcHJpbnRsbigi4pyFICIgKyBKc29uLmRlY29kZUZyb21TdHJpbmc8UmlnaHQ-KGJvZHkpKQp9Cg) · Xem thêm: K702, K704

### K704 · Server gửi `null` cho field không-null

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Unexpected JSON token at offset 22: Expected string literal but 'null' literal was found at path: $.status`,
kèm gợi ý `Use 'coerceInputValues = true'`.

**Vì sao:** `"status": null` khác với "không có status". Field kiểu `String` không nhận `null`, kể cả khi có giá trị mặc định.

**Sửa:** hoặc khai báo `String?`, hoặc bật `coerceInputValues = true` để `null` (và giá trị enum lạ) được thay bằng
giá trị mặc định.

```kotlin
@Serializable data class Profile(val name: String, val status: String = "unknown")
val json = Json { coerceInputValues = true }   // ✅ {"status":null} → status = "unknown"
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlCmRhdGEgY2xhc3MgUHJvZmlsZSh2YWwgbmFtZTogU3RyaW5nLCB2YWwgc3RhdHVzOiBTdHJpbmcgPSAidW5rbm93biIpCgpmdW4gbWFpbigpIHsKICAgIHZhbCBib2R5ID0gIiIieyJuYW1lIjoiVMO6Iiwic3RhdHVzIjpudWxsfSIiIiAgICAgIC8vIHNlcnZlciBn4butaSBudWxsIGNobyBmaWVsZCBraMO0bmctbnVsbAogICAgdHJ5IHsKICAgICAgICBwcmludGxuKEpzb24uZGVjb2RlRnJvbVN0cmluZzxQcm9maWxlPihib2R5KSkKICAgIH0gY2F0Y2ggKGU6IFNlcmlhbGl6YXRpb25FeGNlcHRpb24pIHsKICAgICAgICBwcmludGxuKCLinYwgIiArIGUubWVzc2FnZSkKICAgIH0KICAgIHZhbCBqc29uID0gSnNvbiB7IGNvZXJjZUlucHV0VmFsdWVzID0gdHJ1ZSB9ICAgICAgLy8g4pyFIG51bGwg4oaSIGTDuW5nIGdpw6EgdHLhu4sgbeG6t2MgxJHhu4tuaAogICAgcHJpbnRsbigi4pyFICIgKyBqc29uLmRlY29kZUZyb21TdHJpbmc8UHJvZmlsZT4oYm9keSkpCn0K) · Xem thêm: K708

### K705 · Field snake_case luôn ra `null`, không báo lỗi

`🟢 mới học` · `Kotlin`

**Gặp:** JSON có `"base_code":"USD"` mà `response.baseCode` luôn là `null`. Không crash, không log, chỉ sai.

**Vì sao:** tên property là tên key JSON. `baseCode` ≠ `base_code`. Có `ignoreUnknownKeys` nên `base_code` bị bỏ qua
lặng lẽ, còn `baseCode` lấy giá trị mặc định `null`. Hai thiết lập "an toàn" cộng lại thành một bug im lặng.

**Sửa:** đặt `@SerialName` cho từng field khác tên. Test decode một body thật (copy từ `curl`) để bắt lỗi này.

```kotlin
val baseCode: String? = null                              // ❌ không bao giờ có giá trị
@SerialName("base_code") val baseCode: String? = null     // ✅
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt) (`RateResponse`) ·
[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlCmRhdGEgY2xhc3MgV3JvbmcodmFsIGJhc2VDb2RlOiBTdHJpbmc_ID0gbnVsbCkgICAgICAgICAgICAgICAgICAgICAgLy8gdMOqbiBLb3RsaW4ga2jDoWMgdMOqbiBKU09OCgpAU2VyaWFsaXphYmxlCmRhdGEgY2xhc3MgUmlnaHQoQFNlcmlhbE5hbWUoImJhc2VfY29kZSIpIHZhbCBiYXNlQ29kZTogU3RyaW5nPyA9IG51bGwpICAgLy8g4pyFCgp2YWwganNvbiA9IEpzb24geyBpZ25vcmVVbmtub3duS2V5cyA9IHRydWUgfQoKZnVuIG1haW4oKSB7CiAgICB2YWwgYm9keSA9ICIiInsicmVzdWx0Ijoic3VjY2VzcyIsImJhc2VfY29kZSI6IlVTRCJ9IiIiCiAgICBwcmludGxuKCLinYwgIiArIGpzb24uZGVjb2RlRnJvbVN0cmluZzxXcm9uZz4oYm9keSkpICAgLy8ga2jDtG5nIGzhu5dpLCBjaOG7iSBpbSBs4bq3bmcgcmEgbnVsbAogICAgcHJpbnRsbigi4pyFICIgKyBqc29uLmRlY29kZUZyb21TdHJpbmc8UmlnaHQ-KGJvZHkpKQp9Cg) · Xem thêm: K750

### K706 · "Serializer for class 'X' is not found"

`🟢 mới học` · `Kotlin`

**Gặp:** `SerializationException: Serializer for class 'Note' is not found. Please ensure that class is marked as
'@Serializable' and that the serialization compiler plugin is applied.`

**Vì sao:** serializer được **compiler plugin** sinh ra lúc build cho class có `@Serializable`. Quên annotation (ở chính class
hoặc ở class lồng bên trong, ví dụ phần tử của `List<Item>`) thì không có serializer.

**Sửa:** thêm `@Serializable` cho class đó và mọi class nó chứa. Đã có annotation mà vẫn lỗi: xem K707.

```kotlin
data class Item(val id: Int)                 // ❌
@Serializable data class Item(val id: Int)   // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpkYXRhIGNsYXNzIFdyb25nKHZhbCB4OiBJbnQpICAgICAgICAgICAgICAgIC8vIHF1w6puIEBTZXJpYWxpemFibGUKCkBTZXJpYWxpemFibGUKZGF0YSBjbGFzcyBSaWdodCh2YWwgeDogSW50KSAgICAgICAgICAgICAgIC8vIOKchQoKZnVuIG1haW4oKSB7CiAgICB0cnkgewogICAgICAgIHByaW50bG4oSnNvbi5kZWNvZGVGcm9tU3RyaW5nPFdyb25nPigiIiJ7IngiOjF9IiIiKSkKICAgIH0gY2F0Y2ggKGU6IFNlcmlhbGl6YXRpb25FeGNlcHRpb24pIHsKICAgICAgICBwcmludGxuKCLinYwgIiArIGUubWVzc2FnZSkKICAgIH0KICAgIHByaW50bG4oIuKchSAiICsgSnNvbi5kZWNvZGVGcm9tU3RyaW5nPFJpZ2h0PigiIiJ7IngiOjF9IiIiKSkKfQo) · Xem thêm: K707

### K707 · Có `@Serializable` mà vẫn không có serializer: thiếu plugin

`🟡 đã làm app` · `Gradle` · `KMP`

**Gặp:** lỗi y hệt K706 dù class đã có `@Serializable`; hoặc IDE không thấy `Item.serializer()`.

**Vì sao:** thư viện `kotlinx-serialization-json` chỉ có annotation và runtime. Phần sinh code là plugin
`org.jetbrains.kotlin.plugin.serialization`, phải áp dụng ở **đúng module** chứa class, và phải **cùng version với Kotlin**.

**Sửa:** thêm plugin vào module `shared`, version lấy từ `kotlin` trong catalog.

```toml
[plugins]
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }  # ✅ cùng version Kotlin
```

```kotlin
// shared/build.gradle.kts
plugins { alias(libs.plugins.kotlinSerialization) }
```

**Gặp ở:** [`apps/RateWatch/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/build.gradle.kts) · Xem thêm: K706, K719

### K708 · Server thêm một giá trị enum mới là app cũ crash

`🟡 đã làm app` · `Kotlin`

**Gặp:** `SerializationException: Status does not contain element with name 'PENDING' at path $.status`.

**Vì sao:** enum chỉ nhận đúng các tên đã khai báo. Server thêm trạng thái mới, mọi bản app cũ không đọc được cả response.

**Sửa:** có một giá trị dự phòng làm mặc định và bật `coerceInputValues` (giá trị lạ → mặc định). Hoặc đọc field đó là
`String` rồi tự chuyển.

```kotlin
@Serializable enum class Status { ACTIVE, BANNED, UNKNOWN }
@Serializable data class Account(val status: Status = Status.UNKNOWN)
val json = Json { coerceInputValues = true }   // ✅ "PENDING" → UNKNOWN
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlIGVudW0gY2xhc3MgU3RhdHVzIHsgQUNUSVZFLCBCQU5ORUQsIFVOS05PV04gfQoKQFNlcmlhbGl6YWJsZQpkYXRhIGNsYXNzIEFjY291bnQodmFsIHN0YXR1czogU3RhdHVzID0gU3RhdHVzLlVOS05PV04pCgpmdW4gbWFpbigpIHsKICAgIHZhbCBib2R5ID0gIiIieyJzdGF0dXMiOiJQRU5ESU5HIn0iIiIgICAgICAgICAgICAgICAgICAvLyBzZXJ2ZXIgdGjDqm0gdHLhuqFuZyB0aMOhaSBt4bubaQogICAgdHJ5IHsKICAgICAgICBwcmludGxuKEpzb24uZGVjb2RlRnJvbVN0cmluZzxBY2NvdW50Pihib2R5KSkKICAgIH0gY2F0Y2ggKGU6IFNlcmlhbGl6YXRpb25FeGNlcHRpb24pIHsKICAgICAgICBwcmludGxuKCLinYwgIiArIGUubWVzc2FnZSkKICAgIH0KICAgIHZhbCBqc29uID0gSnNvbiB7IGNvZXJjZUlucHV0VmFsdWVzID0gdHJ1ZSB9ICAgICAgICAgICAvLyDinIUgZ2nDoSB0cuG7iyBlbnVtIGzhuqEg4oaSIG3hurdjIMSR4buLbmgKICAgIHByaW50bG4oIuKchSAiICsganNvbi5kZWNvZGVGcm9tU3RyaW5nPEFjY291bnQ-KGJvZHkpKQp9Cg) · Xem thêm: K704, K712

### K709 · Gửi lên server thiếu field có giá trị mặc định

`🟡 đã làm app` · `Kotlin`

**Gặp:** server báo thiếu `page`, trong khi `SearchRequest("kotlin")` rõ ràng có `page = 1`.

**Vì sao:** mặc định `encodeDefaults = false`: field đang bằng giá trị mặc định **không** được ghi ra JSON. Kết quả là
`{"query":"kotlin"}`.

**Sửa:** bật `encodeDefaults = true` cho `Json` dùng để gửi đi. Không muốn gửi `"lang":null` thì thêm `explicitNulls = false`.
Cũng có thể đánh dấu riêng field bằng `@EncodeDefault`.

```kotlin
Json.encodeToString(SearchRequest("kotlin"))                       // ❌ {"query":"kotlin"}
Json { encodeDefaults = true; explicitNulls = false }.encodeToString(SearchRequest("kotlin"))
                                                                   // ✅ {"query":"kotlin","page":1}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlCmRhdGEgY2xhc3MgU2VhcmNoUmVxdWVzdCh2YWwgcXVlcnk6IFN0cmluZywgdmFsIHBhZ2U6IEludCA9IDEsIHZhbCBsYW5nOiBTdHJpbmc_ID0gbnVsbCkKCmZ1biBtYWluKCkgewogICAgdmFsIHJlcSA9IFNlYXJjaFJlcXVlc3QoImtvdGxpbiIpCiAgICBwcmludGxuKCJt4bq3YyDEkeG7i25oOiAgICAgICAgICAiICsgSnNvbi5lbmNvZGVUb1N0cmluZyhyZXEpKSAgICAgICAgICAgICAgICAgLy8g4p2MIHRoaeG6v3UgInBhZ2UiCiAgICBwcmludGxuKCJlbmNvZGVEZWZhdWx0czogICAgIiArIEpzb24geyBlbmNvZGVEZWZhdWx0cyA9IHRydWUgfS5lbmNvZGVUb1N0cmluZyhyZXEpKQogICAgcHJpbnRsbigiKyBleHBsaWNpdE51bGxzPWY6ICIgKyBKc29uIHsgZW5jb2RlRGVmYXVsdHMgPSB0cnVlOyBleHBsaWNpdE51bGxzID0gZmFsc2UgfS5lbmNvZGVUb1N0cmluZyhyZXEpKQp9Cg)

### K710 · Số trong JSON không vừa kiểu Kotlin

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Unexpected symbol '.' in numeric literal at path: $.amount` (gửi `1.5` vào `Int`), hoặc
`Failed to parse int for input '3000000000' at path: $.id` (id vượt `Int.MAX_VALUE`).

**Vì sao:** decoder không tự làm tròn hay mở rộng kiểu. Id của server thường là số 64-bit, tiền có thể có phần lẻ.

**Sửa:** id dùng `Long` (hoặc `String` nếu server gửi số lớn hơn `Long`); số có phần lẻ dùng `Double` khi chỉ để hiển thị,
còn tiền thì đổi ra số nguyên đơn vị nhỏ nhất. Chuỗi số như `"1"` thì decoder chấp nhận cho `Int`.

```kotlin
@Serializable data class Item(val id: Int)    // ❌ 3000000000 không vừa
@Serializable data class Item(val id: Long)   // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlIGRhdGEgY2xhc3MgUHJpY2UodmFsIGFtb3VudDogSW50KQpAU2VyaWFsaXphYmxlIGRhdGEgY2xhc3MgSXRlbSh2YWwgaWQ6IEludCkKCmZ1biB0cnlEZWNvZGUobGFiZWw6IFN0cmluZywgYmxvY2s6ICgpIC0-IEFueSkgPQogICAgdHJ5IHsgcHJpbnRsbigi4pyFICRsYWJlbDogIiArIGJsb2NrKCkpIH0gY2F0Y2ggKGU6IFNlcmlhbGl6YXRpb25FeGNlcHRpb24pIHsgcHJpbnRsbigi4p2MICRsYWJlbDogIiArIGUubWVzc2FnZT8ubGluZVNlcXVlbmNlKCk_LmZpcnN0KCkpIH0KCmZ1biBtYWluKCkgewogICAgdHJ5RGVjb2RlKCIxLjUgdsOgbyBJbnQiKSB7IEpzb24uZGVjb2RlRnJvbVN0cmluZzxQcmljZT4oIiIieyJhbW91bnQiOjEuNX0iIiIpIH0KICAgIHRyeURlY29kZSgiMyB04bu3IHbDoG8gSW50IikgeyBKc29uLmRlY29kZUZyb21TdHJpbmc8SXRlbT4oIiIieyJpZCI6MzAwMDAwMDAwMH0iIiIpIH0KICAgIHRyeURlY29kZSgiXCIxXCIgdsOgbyBJbnQiKSB7IEpzb24uZGVjb2RlRnJvbVN0cmluZzxQcmljZT4oIiIieyJhbW91bnQiOiIxIn0iIiIpIH0gICAvLyBjaHXhu5dpIHPhu5EgdGjDrCDEkcaw4bujYwp9Cg)

### K711 · Tạo `Json { }` mới trong mỗi lần gọi

`🟡 đã làm app` · `Kotlin`

**Gặp:** IDE cảnh báo `Redundant creation of Json format`, parse chậm hơn cần thiết; mỗi chỗ một cấu hình khác nhau
(chỗ có `ignoreUnknownKeys`, chỗ không).

**Vì sao:** mỗi instance `Json` giữ cache serializer riêng. Tạo trong hàm là bỏ cache mỗi lần, và cấu hình bị chép rải rác.

**Sửa:** khai báo **một** `val` top-level (hoặc trong Koin) rồi dùng lại ở cả Ktor lẫn chỗ tự decode.

```kotlin
fun parse(s: String) = Json { ignoreUnknownKeys = true }.decodeFromString<RateResponse>(s)  // ❌
val RateJson = Json { ignoreUnknownKeys = true }                                            // ✅ một lần
fun parse(s: String) = RateJson.decodeFromString<RateResponse>(s)
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/HttpClients.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/HttpClients.kt) (`RateJson`)

### K712 · Sealed class: thiếu `"type"` hoặc `"type"` lạ

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Class discriminator was missing and no default serializers were registered in the polymorphic scope of 'Message'.`
hoặc `Serializer for subclass 'video' is not found in the polymorphic scope of 'Message'`.

**Vì sao:** để biết decode ra lớp con nào, kotlinx.serialization đọc key `"type"` (class discriminator). Thiếu key đó, hoặc
server gửi một loại app chưa biết, là không chọn được lớp con.

**Sửa:** đặt `@SerialName` ngắn cho từng lớp con và thống nhất với server. Với loại mới mà app cũ chưa biết: thống nhất
với server không gửi loại mới cho app cũ, hoặc decode thành `JsonObject` trước rồi tự rẽ nhánh theo `"type"`.

```kotlin
@Serializable sealed class Message {
    @Serializable @SerialName("text")  data class Text(val body: String) : Message()
    @Serializable @SerialName("image") data class Image(val url: String) : Message()
}
// {"type":"text","body":"hi"} → Message.Text("hi")
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlCnNlYWxlZCBjbGFzcyBNZXNzYWdlIHsKICAgIEBTZXJpYWxpemFibGUgQFNlcmlhbE5hbWUoInRleHQiKSBkYXRhIGNsYXNzIFRleHQodmFsIGJvZHk6IFN0cmluZykgOiBNZXNzYWdlKCkKICAgIEBTZXJpYWxpemFibGUgQFNlcmlhbE5hbWUoImltYWdlIikgZGF0YSBjbGFzcyBJbWFnZSh2YWwgdXJsOiBTdHJpbmcpIDogTWVzc2FnZSgpCn0KCmZ1biB0cnlEZWNvZGUoYm9keTogU3RyaW5nKSA9IHRyeSB7CiAgICBwcmludGxuKCLinIUgIiArIEpzb24uZGVjb2RlRnJvbVN0cmluZzxNZXNzYWdlPihib2R5KSkKfSBjYXRjaCAoZTogU2VyaWFsaXphdGlvbkV4Y2VwdGlvbikgewogICAgcHJpbnRsbigi4p2MICIgKyBlLm1lc3NhZ2U_LmxpbmVTZXF1ZW5jZSgpPy5maXJzdCgpKQp9CgpmdW4gbWFpbigpIHsKICAgIHRyeURlY29kZSgiIiJ7InR5cGUiOiJ0ZXh0IiwiYm9keSI6ImhpIn0iIiIpICAgICAgLy8gY8OzICJ0eXBlIjogxJHGsOG7o2MKICAgIHRyeURlY29kZSgiIiJ7ImJvZHkiOiJoaSJ9IiIiKSAgICAgICAgICAgICAgICAgICAgLy8gdGhp4bq_dSAidHlwZSIKICAgIHRyeURlY29kZSgiIiJ7InR5cGUiOiJ2aWRlbyIsInVybCI6IngifSIiIikgICAgICAgLy8gInR5cGUiIGzhuqE6IGFwcCBjxakgY2jGsGEgYmnhur90IGxv4bqhaSBt4bubaQp9Cg) · Xem thêm: K713, K714

### K713 · Encode lớp con của sealed class mà JSON mất `"type"`

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** gửi `Message.Text("hi")` lên server, server báo không biết loại tin nhắn. JSON chỉ có `{"body":"hi"}`.

**Vì sao:** `"type"` chỉ được ghi khi encode **qua kiểu cha**. `Json.encodeToString(msg)` với `msg: Message.Text` dùng
serializer của `Text`, coi nó như class thường.

**Sửa:** nói rõ kiểu cha khi encode, hoặc khai báo biến/list theo kiểu cha.

```kotlin
Json.encodeToString(Message.Text("hi"))              // ❌ {"body":"hi"}
Json.encodeToString<Message>(Message.Text("hi"))     // ✅ {"type":"text","body":"hi"}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlCnNlYWxlZCBjbGFzcyBNZXNzYWdlIHsKICAgIEBTZXJpYWxpemFibGUgQFNlcmlhbE5hbWUoInRleHQiKSBkYXRhIGNsYXNzIFRleHQodmFsIGJvZHk6IFN0cmluZykgOiBNZXNzYWdlKCkKfQoKZnVuIG1haW4oKSB7CiAgICB2YWwgbXNnID0gTWVzc2FnZS5UZXh0KCJoaSIpCiAgICBwcmludGxuKCLinYwgIiArIEpzb24uZW5jb2RlVG9TdHJpbmcobXNnKSkgICAgICAgICAgICAgIC8vIGtp4buDdSB0xKluaCBsw6AgVGV4dDoga2jDtG5nIGPDsyAidHlwZSIKICAgIHByaW50bG4oIuKchSAiICsgSnNvbi5lbmNvZGVUb1N0cmluZzxNZXNzYWdlPihtc2cpKSAgICAgLy8ga2nhu4N1IHTEqW5oIGzDoCBNZXNzYWdlOiBjw7MgInR5cGUiCiAgICB2YWwgbGlzdDogTGlzdDxNZXNzYWdlPiA9IGxpc3RPZihtc2cpCiAgICBwcmludGxuKCLinIUgIiArIEpzb24uZW5jb2RlVG9TdHJpbmcobGlzdCkpICAgICAgICAgICAgIC8vIExpc3Q8TWVzc2FnZT4gY8WpbmcgY8OzICJ0eXBlIgp9Cg) · Xem thêm: K712

### K714 · Property tên `type` trong sealed class

`🟡 đã làm app` · `Kotlin`

**Gặp:** `Class 'Event.Click' cannot be serialized as base class 'Event' because it has property name that conflicts with JSON class discriminator 'type'.`

**Vì sao:** `"type"` đã được dùng làm discriminator. Một property cùng tên sẽ ghi đè lên nó.

**Sửa:** đổi tên JSON của property bằng `@SerialName`, hoặc đổi tên discriminator cho cả `Json`
(`classDiscriminator = "kind"`, phải khớp với server).

```kotlin
@Serializable data class Click(val type: String) : Event()                            // ❌
@Serializable data class Click(@SerialName("click_type") val type: String) : Event()  // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlCnNlYWxlZCBjbGFzcyBFdmVudCB7CiAgICBAU2VyaWFsaXphYmxlIGRhdGEgY2xhc3MgQ2xpY2sodmFsIHR5cGU6IFN0cmluZykgOiBFdmVudCgpICAgICAvLyDinYwgdHLDuW5nIHTDqm4gduG7m2kgZGlzY3JpbWluYXRvciAidHlwZSIKfQoKQFNlcmlhbGl6YWJsZQpzZWFsZWQgY2xhc3MgRXZlbnQyIHsKICAgIEBTZXJpYWxpemFibGUgZGF0YSBjbGFzcyBDbGljayhAU2VyaWFsTmFtZSgiY2xpY2tfdHlwZSIpIHZhbCB0eXBlOiBTdHJpbmcpIDogRXZlbnQyKCkgICAvLyDinIUgxJHhu5VpIHTDqm4gSlNPTgp9CgpmdW4gbWFpbigpIHsKICAgIHRyeSB7CiAgICAgICAgcHJpbnRsbihKc29uLmVuY29kZVRvU3RyaW5nPEV2ZW50PihFdmVudC5DbGljaygiZG91YmxlIikpKQogICAgfSBjYXRjaCAoZTogU2VyaWFsaXphdGlvbkV4Y2VwdGlvbikgewogICAgICAgIHByaW50bG4oIuKdjCAiICsgZS5tZXNzYWdlKQogICAgfQogICAgcHJpbnRsbigi4pyFICIgKyBKc29uLmVuY29kZVRvU3RyaW5nPEV2ZW50Mj4oRXZlbnQyLkNsaWNrKCJkb3VibGUiKSkpCiAgICBwcmludGxuKCLinIUgIiArIEpzb24geyBjbGFzc0Rpc2NyaW1pbmF0b3IgPSAia2luZCIgfS5lbmNvZGVUb1N0cmluZzxFdmVudD4oRXZlbnQuQ2xpY2soImRvdWJsZSIpKSkKfQo)

### K715 · "Serializer was not found for type 'Date'"

`🟡 đã làm app` · `Kotlin` · `KMP`

**Gặp:** lỗi lúc **compile**: `Serializer was not found for type 'Date'. To use a context serializer as the fallback,
explicitly annotate type or property with '@Contextual'.` Tương tự với `UUID`, `BigDecimal`, `LocalDateTime` của Java.

**Vì sao:** chỉ kiểu Kotlin cơ bản, collection và class `@Serializable` mới có serializer sẵn. Kiểu của Java còn không có
trên iOS, nên cũng không được dùng trong `commonMain`.

**Sửa:** trong model gửi/nhận, dùng kiểu đơn giản mà server gửi (`Long` epoch millis, `String` ISO-8601), rồi đổi sang kiểu
thời gian ở tầng trên. Cần giữ kiểu riêng thì viết `KSerializer` và gắn `@Serializable(with = …)`.

```kotlin
@Serializable data class Trip(val startedAt: java.util.Date)      // ❌ không compile, không có trên iOS
@Serializable data class Trip(val startedAtMillis: Long)          // ✅ đổi sang Instant ở tầng trên
```

### K716 · "Expected start of the array '[', but had '{' instead"

`🟢 mới học` · `Kotlin`

**Gặp:** `Unexpected JSON token at offset 0: Expected start of the array '[', but had '{' instead at path: $`
(hoặc ngược lại: `Expected start of the object '{', but had '['`).

**Vì sao:** kiểu khai báo không khớp **hình dạng** body. API hay bọc mảng trong một object: `{"data":[…],"total":1}`.

**Sửa:** mở response bằng `curl` xem hình dạng thật, rồi khai báo lớp vỏ cho đúng.

```kotlin
client.get(url).body<List<Note>>()                // ❌ body là {"data":[…]}
@Serializable data class NotesPage(val data: List<Note>, val total: Int)
client.get(url).body<NotesPage>().data            // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlIGRhdGEgY2xhc3MgTm90ZSh2YWwgaWQ6IEludCwgdmFsIHRpdGxlOiBTdHJpbmcpCkBTZXJpYWxpemFibGUgZGF0YSBjbGFzcyBOb3Rlc1BhZ2UodmFsIGRhdGE6IExpc3Q8Tm90ZT4sIHZhbCB0b3RhbDogSW50KQoKdmFsIGJvZHkgPSAiIiJ7ImRhdGEiOlt7ImlkIjoxLCJ0aXRsZSI6Ik11YSBz4buvYSJ9XSwidG90YWwiOjF9IiIiCgpmdW4gbWFpbigpIHsKICAgIHRyeSB7CiAgICAgICAgcHJpbnRsbihKc29uLmRlY29kZUZyb21TdHJpbmc8TGlzdDxOb3RlPj4oYm9keSkpICAgICAgICAvLyDinYwgYm9keSBsw6Agb2JqZWN0IGLhu41jIG3huqNuZwogICAgfSBjYXRjaCAoZTogU2VyaWFsaXphdGlvbkV4Y2VwdGlvbikgewogICAgICAgIHByaW50bG4oIuKdjCAiICsgZS5tZXNzYWdlPy5saW5lU2VxdWVuY2UoKT8uZmlyc3QoKSkKICAgIH0KICAgIHByaW50bG4oIuKchSAiICsgSnNvbi5kZWNvZGVGcm9tU3RyaW5nPE5vdGVzUGFnZT4oYm9keSkuZGF0YSkgLy8g4pyFIGtoYWkgYsOhbyDEkcO6bmcgbOG7m3AgduG7jwp9Cg)

### K717 · `@Transient` báo "must have an initializing expression"

`🟢 mới học` · `Kotlin`

**Gặp:** lỗi compile `This property is marked as @Transient and therefore must have an initializing expression.`

**Vì sao:** `@Transient` (của `kotlinx.serialization`) bỏ field khỏi JSON. Khi decode, field đó phải lấy giá trị từ đâu đó,
nên bắt buộc có giá trị mặc định.

**Sửa:** thêm giá trị mặc định, và import đúng `kotlinx.serialization.Transient`.

```kotlin
@Transient val cache: String                         // ❌
@Transient val cache: String = ""                    // ✅ import kotlinx.serialization.Transient
```

### K718 · Property tính toán không có trong JSON

`🟢 mới học` · `Kotlin`

**Gặp:** `Temperature(30.0)` encode ra `{"celsius":30.0}`, không có `fahrenheit` mà server cần.

**Vì sao:** chỉ property có **backing field** mới được serialize. `val fahrenheit get() = …` chỉ là một hàm.

**Sửa:** server cần thì khai báo nó là property thật (thường trong một DTO riêng cho request).

```kotlin
@Serializable data class Temperature(val celsius: Double) {
    val fahrenheit: Double get() = celsius * 9 / 5 + 32      // ❌ không vào JSON
}
@Serializable data class TemperatureDto(val celsius: Double, val fahrenheit: Double)   // ✅
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguc2VyaWFsaXphdGlvbi4qCmltcG9ydCBrb3RsaW54LnNlcmlhbGl6YXRpb24uanNvbi4qCgpAU2VyaWFsaXphYmxlCmRhdGEgY2xhc3MgVGVtcGVyYXR1cmUodmFsIGNlbHNpdXM6IERvdWJsZSkgewogICAgdmFsIGZhaHJlbmhlaXQ6IERvdWJsZSBnZXQoKSA9IGNlbHNpdXMgKiA5IC8gNSArIDMyICAgLy8gZ2V0dGVyLCBraMO0bmcgY8OzIGJhY2tpbmcgZmllbGQ6IEtIw5RORyB2w6BvIEpTT04KfQoKQFNlcmlhbGl6YWJsZQpkYXRhIGNsYXNzIFRlbXBlcmF0dXJlRHRvKHZhbCBjZWxzaXVzOiBEb3VibGUsIHZhbCBmYWhyZW5oZWl0OiBEb3VibGUpICAgLy8g4pyFIHNlcnZlciBj4bqnbiB0aMOsIGtoYWkgYsOhbyBo4bqzbgoKZnVuIG1haW4oKSB7CiAgICB2YWwgdCA9IFRlbXBlcmF0dXJlKDMwLjApCiAgICBwcmludGxuKCLinYwgIiArIEpzb24uZW5jb2RlVG9TdHJpbmcodCkpCiAgICBwcmludGxuKCLinIUgIiArIEpzb24uZW5jb2RlVG9TdHJpbmcoVGVtcGVyYXR1cmVEdG8odC5jZWxzaXVzLCB0LmZhaHJlbmhlaXQpKSkKfQo)

### K719 · `json()` hoặc `Json` báo Unresolved reference

`🟢 mới học` · `Gradle` · `KMP`

**Gặp:** `Unresolved reference 'json'` trong `install(ContentNegotiation) { json(...) }`, hoặc `Unresolved reference 'Json'`.

**Vì sao:** Ktor tách thành nhiều artifact. `ContentNegotiation` nằm ở `ktor-client-content-negotiation`, hàm `json()` nằm ở
`ktor-serialization-kotlinx-json`, còn `Json` ở `kotlinx-serialization-json`. Thiếu một cái, hoặc thiếu import, là đỏ.

**Sửa:** thêm đủ 3 dependency vào `commonMain` (2 cái Ktor cùng version Ktor core) và import đúng.

```kotlin
commonMain.dependencies {
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
}
// import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
// import io.ktor.serialization.kotlinx.json.json
```

**Gặp ở:** [`apps/RateWatch/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/build.gradle.kts) · Xem thêm: K707

### K720 · `NoTransformationFoundException`: "Expected response body of the type …"

`🟢 mới học` · `KMP`

**Gặp:** `io.ktor.client.call.NoTransformationFoundException: Expected response body of the type 'class RateResponse' but
was …`, kèm các dòng `Response status`, `Response header ContentType: …`.

**Vì sao:** `body<T>()` cần một plugin biết đổi byte thành `T`. Không cài `ContentNegotiation`, hoặc cài mà không có
converter cho `Content-Type` của response, thì Ktor không biết làm gì.

**Sửa:** cài `ContentNegotiation` với `json(...)`. Lỗi vẫn còn thì đọc dòng `Response header ContentType` trong message: xem K721.

```kotlin
HttpClient(engine) {
    install(ContentNegotiation) { json(RateJson) }    // ✅
}
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/HttpClients.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/HttpClients.kt) · Xem thêm: K721, K786

### K721 · Server trả JSON nhưng `Content-Type` là `text/plain` hoặc `text/html`

`🟡 đã làm app` · `KMP`

**Gặp:** vẫn `NoTransformationFoundException` dù đã cài `ContentNegotiation`; message có `ContentType: text/plain`.
Hoặc status 200 nhưng body là trang HTML (cổng Wi-Fi khách sạn, proxy, trang bảo trì).

**Vì sao:** `json()` mặc định chỉ nhận `application/json`. Body HTML thì không phải JSON, decode kiểu gì cũng hỏng.

**Sửa:** server gửi sai header mà nội dung đúng là JSON thì đăng ký thêm content type đó. Còn body HTML thì coi là
"response hỏng" (như `RefreshError.BadResponse` của Rate Watch), đừng để crash.

```kotlin
install(ContentNegotiation) {
    json(RateJson)
    json(RateJson, contentType = ContentType.Text.Plain)   // ✅ server gửi sai header
}
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt) (nhánh `BadResponse`) · Xem thêm: K720, K726

### K722 · POST báo "Fail to prepare request body for sending"

`🟢 mới học` · `KMP`

**Gặp:** `IllegalStateException: Fail to prepare request body for sending. The body type is: class Login, with Content-Type: null.`

**Vì sao:** `ContentNegotiation` chọn converter theo `Content-Type` của **request**. Không đặt header đó thì Ktor không biết
phải đổi object thành JSON.

**Sửa:** gọi `contentType(ContentType.Application.Json)` trước `setBody`.

```kotlin
client.post(url) { setBody(Login("tu", "secret")) }          // ❌
client.post(url) {
    contentType(ContentType.Application.Json)                 // ✅
    setBody(Login("tu", "secret"))
}
```

### K723 · API báo lỗi bằng HTTP 200

`🟡 đã làm app` · `KMP`

**Gặp:** chọn đồng tiền không tồn tại, app báo "cập nhật thành công" rồi hiện bảng rỗng. Log thấy `200 OK` với body
`{"result":"error","error-type":"unsupported-code"}`.

**Vì sao:** nhiều API trả lỗi nghiệp vụ trong body với status 200. Chỉ kiểm `status.isSuccess()` là coi lỗi như thành công.

**Sửa:** kiểm cả field báo kết quả trong body, và kiểm dữ liệu cần có (ở đây là `rates`) trước khi lưu.

```kotlin
val body = response.body<RateResponse>()
if (body.result != "success") throw RateApiException(RefreshError.ServerError, body.errorType)   // ✅
if (body.rates.isNullOrEmpty()) throw RateApiException(RefreshError.BadResponse, "no rates")
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt) ·
[`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt) (`an error sent with status 200 is not a success`) ·
Xem thêm: K702, K724

### K724 · HTTP 404/500 mà Ktor không ném lỗi

`🟢 mới học` · `KMP`

**Gặp:** server trả 500 với body `oops`, app lại crash ở chỗ decode JSON, hoặc decode body lỗi ra một object toàn `null`.

**Vì sao:** mặc định `expectSuccess = false`: Ktor trả về **mọi** status như bình thường, không tự ném exception.

**Sửa:** kiểm status trước khi đọc body (cách của Rate Watch), hoặc bật `expectSuccess = true` (K725).

```kotlin
val response = client.get(url)
if (!response.status.isSuccess()) throw RateApiException(RefreshError.ServerError, "HTTP ${response.status.value}")  // ✅
val body = response.body<RateResponse>()
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt) · Xem thêm: K725

### K725 · Bật `expectSuccess = true` rồi không biết bắt lỗi gì

`🟡 đã làm app` · `KMP`

**Gặp:** `ClientRequestException: Client request(GET https://…/users/9) invalid: 404 Not Found. Text: "…"` làm crash
(hoặc `ServerResponseException: Server error(…)` với 5xx).

**Vì sao:** với `expectSuccess = true`, Ktor ném `RedirectResponseException` (3xx), `ClientRequestException` (4xx),
`ServerResponseException` (5xx). Cả ba là `ResponseException`, có sẵn `response` để đọc status và body lỗi.

**Sửa:** bắt `ResponseException` ở tầng gọi API, đổi ra lỗi của app.

```kotlin
try {
    client.get(url).body<User>()
} catch (e: ClientRequestException) {
    if (e.response.status == HttpStatusCode.NotFound) null else throw e   // ✅
}
```

Xem thêm: K724, K747

### K726 · JSON hỏng mà `catch (e: SerializationException)` không bắt được

`🔴 khó, hiếm nhưng đau` · `KMP`

**Gặp:** đã bọc `try/catch (e: SerializationException)` quanh `response.body<T>()` mà app vẫn crash với
`io.ktor.serialization.JsonConvertException: Illegal input: …`.

**Vì sao:** converter của Ktor bọc lỗi của kotlinx.serialization trong `JsonConvertException` (không phải
`SerializationException`). Lỗi gốc nằm ở `e.cause`.

**Sửa:** bắt `CancellationException` trước (ném lại), rồi bắt `Exception` chung quanh `body()` và đổi thành "response hỏng".

```kotlin
val body = try {
    response.body<RateResponse>()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {                                   // ✅ JsonConvertException, body HTML, thiếu field…
    throw RateApiException(RefreshError.BadResponse, e.message, e)
}
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt) · Xem thêm: K721

### K727 · Không cài `HttpTimeout`: mất sóng là spinner quay mãi

`🟢 mới học` · `KMP`

**Gặp:** vào thang máy hay vùng sóng yếu, màn hình "Đang tải" rất lâu mới báo lỗi, hoặc không bao giờ báo.

**Vì sao:** Ktor không tự đặt timeout. Thời gian chờ khi đó là mặc định của từng engine (OkHttp, NSURLSession), mỗi nơi
một kiểu và thường dài hơn người dùng chịu được.

**Sửa:** cài `HttpTimeout` trong hàm tạo client dùng chung, để Android và iOS chờ như nhau.

```kotlin
install(HttpTimeout) {
    requestTimeoutMillis = 10_000      // ✅ cả request
    connectTimeoutMillis = 10_000      // ✅ bước kết nối
}
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/HttpClients.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/HttpClients.kt) · Xem thêm: K728

### K728 · Ba loại timeout, ba exception khác nhau

`🟡 đã làm app` · `KMP`

**Gặp:** đã bắt `HttpRequestTimeoutException` mà vẫn có lúc lọt `ConnectTimeoutException` hoặc `SocketTimeoutException`;
message dạng `Request timeout has expired [url=…, request_timeout=10000 ms]`, `Connect timeout has expired […]`,
`Socket timeout has expired […]`.

**Vì sao:** `requestTimeoutMillis` tính cho cả request, `connectTimeoutMillis` cho bước mở kết nối,
`socketTimeoutMillis` cho khoảng lặng giữa hai gói dữ liệu. Mỗi loại ném một exception riêng.

**Sửa:** bắt cả ba và đổi về **một** lỗi "Timeout" của app. Request riêng cần lâu hơn (upload) thì đặt timeout riêng
bằng `timeout { requestTimeoutMillis = 60_000 }` trong builder của request đó.

```kotlin
} catch (e: HttpRequestTimeoutException) { throw RateApiException(RefreshError.Timeout, e.message, e)
} catch (e: ConnectTimeoutException)     { throw RateApiException(RefreshError.Timeout, e.message, e)
} catch (e: SocketTimeoutException)      { throw RateApiException(RefreshError.Timeout, e.message, e) }
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt) · Xem thêm: K729

### K729 · Timeout bị báo thành "mất mạng"

`🔴 khó, hiếm nhưng đau` · `KMP`

**Gặp:** server chậm mà app hiện "Bạn đang offline" thay vì "Server không trả lời".

**Vì sao:** `HttpRequestTimeoutException` (và hai timeout kia) đều là **`IOException`**. Đặt `catch (e: IOException)` lên
trước thì nhánh đó bắt luôn timeout, các nhánh timeout bên dưới không bao giờ chạy.

**Sửa:** thứ tự `catch` đi từ hẹp tới rộng: `CancellationException` → các timeout → `IOException` → `Exception`.

```kotlin
} catch (e: IOException) { Offline }                  // ❌ đặt trước: timeout cũng thành Offline
} catch (e: HttpRequestTimeoutException) { Timeout }  //    không bao giờ tới

} catch (e: HttpRequestTimeoutException) { Timeout }  // ✅ hẹp trước
} catch (e: IOException) { Offline }
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt) (hàm `send`) · Xem thêm: K728, K744

### K730 · `withTimeout` + `catch (CancellationException)`: bấm Stop mà vẫn thử lại

`🔴 khó, hiếm nhưng đau` · `Kotlin`

**Gặp:** vòng "gửi lệnh, timeout thì thử lại" không dừng khi người dùng bấm Stop; log thấy `JobCancellationException`
bị coi như một lần timeout.

**Vì sao:** `withTimeout` ném `TimeoutCancellationException`, là **con** của `CancellationException`. Bắt
`CancellationException` để xử lý timeout thì bắt luôn lệnh huỷ thật của người dùng.

**Sửa:** bắt đúng `TimeoutCancellationException`. Hoặc dùng `withTimeoutOrNull` và kiểm `null`.

```kotlin
try {
    return withTimeout(limit) { transport.send(text) }
} catch (e: CancellationException) { retry() }          // ❌ nuốt cả lệnh huỷ
} catch (e: TimeoutCancellationException) { retry() }   // ✅ chỉ timeout
```

**Gặp ở:** [`apps/ObdLink/shared/src/commonMain/kotlin/dev/obdlink/core/CommandQueue.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/ObdLink/shared/src/commonMain/kotlin/dev/obdlink/core/CommandQueue.kt) ·
[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgovLyBM4buHbmggZ-G7rWkgdOG7m2kgdGhp4bq_dCBi4buLIGtow7RuZyB0cuG6oyBs4budaSAoZ2nhu5FuZyBPYmRMaW5rIGtoaSBkb25nbGUgaW0gbOG6t25nKS4Kc3VzcGVuZCBmdW4gc2lsZW50U2VuZCgpOiBTdHJpbmcgeyBkZWxheSgxMF8wMDApOyByZXR1cm4gIk9LIiB9CgpzdXNwZW5kIGZ1biBzZW5kV3JvbmcoKTogU3RyaW5nIHsKICAgIHJlcGVhdCgzKSB7IGF0dGVtcHQgLT4KICAgICAgICB0cnkgewogICAgICAgICAgICByZXR1cm4gd2l0aFRpbWVvdXQoMTAwKSB7IHNpbGVudFNlbmQoKSB9CiAgICAgICAgfSBjYXRjaCAoZTogQ2FuY2VsbGF0aW9uRXhjZXB0aW9uKSB7ICAgICAgICAgIC8vIOKdjCBi4bqvdCBj4bqjIGzhu4duaCBodeG7tyB0aOG6rXQgY-G7p2EgbmfGsOG7nWkgZMO5bmcKICAgICAgICAgICAgcHJpbnRsbigiICB3cm9uZzogbOG6p24gJGF0dGVtcHQgaOG7j25nICgke2U6OmNsYXNzLnNpbXBsZU5hbWV9KSwgdGjhu60gbOG6oWkiKQogICAgICAgIH0KICAgIH0KICAgIHJldHVybiAixJHDoyB0aOG7rSAzIGzhuqduIgp9CgpzdXNwZW5kIGZ1biBzZW5kUmlnaHQoKTogU3RyaW5nIHsKICAgIHJlcGVhdCgzKSB7IGF0dGVtcHQgLT4KICAgICAgICB0cnkgewogICAgICAgICAgICByZXR1cm4gd2l0aFRpbWVvdXQoMTAwKSB7IHNpbGVudFNlbmQoKSB9CiAgICAgICAgfSBjYXRjaCAoZTogVGltZW91dENhbmNlbGxhdGlvbkV4Y2VwdGlvbikgeyAgIC8vIOKchSBjaOG7iSBi4bqvdCB0aW1lb3V0CiAgICAgICAgICAgIHByaW50bG4oIiAgcmlnaHQ6IGzhuqduICRhdHRlbXB0IHRpbWVvdXQsIHRo4butIGzhuqFpIikKICAgICAgICB9CiAgICB9CiAgICByZXR1cm4gIsSRw6MgdGjhu60gMyBs4bqnbiIKfQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsKICAgIGZvciAoKG5hbWUsIHNlbmQpIGluIGxpc3RPZigid3JvbmciIHRvIDo6c2VuZFdyb25nLCAicmlnaHQiIHRvIDo6c2VuZFJpZ2h0KSkgewogICAgICAgIHZhbCBqb2IgPSBsYXVuY2ggeyBwcmludGxuKCIgICRuYW1lIHRy4bqjIHbhu4E6ICR7c2VuZCgpfSIpIH0KICAgICAgICBkZWxheSgxNTApICAgICAgICAgIC8vIGzhuqduIDEgdGltZW91dCwgxJFhbmcgY2jhu50gbOG6p24gMiB0aMOsIG5nxrDhu51pIGTDuW5nIGLhuqVtIFN0b3AKICAgICAgICBqb2IuY2FuY2VsKCkKICAgICAgICBqb2Iuam9pbigpCiAgICAgICAgcHJpbnRsbigiJG5hbWUgeG9uZ1xuIikKICAgIH0KfQo) · Xem thêm: K731

### K731 · `runCatching` nuốt `CancellationException`

`🟡 đã làm app` · `Kotlin` · `KMP`

**Gặp:** rời màn hình giữa lúc đang tải, vài giây sau vẫn thấy log "lưu xong" hoặc màn hình khác hiện "Có lỗi xảy ra".

**Vì sao:** `runCatching` bắt **mọi** `Throwable`, kể cả `CancellationException`. Coroutine đã bị huỷ nhận về
`Result.failure` và chạy tiếp như không có gì.

**Sửa:** viết một hàm thay `runCatching` cho code `suspend`, ném lại `CancellationException`.

```kotlin
suspend inline fun <T> resultOf(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e                                   // ✅ để coroutine dừng thật
} catch (e: Exception) {
    Result.failure(e)
}
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt) (`resultOf`) ·
[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpzdXNwZW5kIGZ1biBkb3dubG9hZCgpOiBTdHJpbmcgeyBkZWxheSgxXzAwMCk7IHJldHVybiAiZGF0YSIgfQoKZnVuIG1haW4oKSA9IHJ1bkJsb2NraW5nIHsKICAgIHZhbCBqb2IgPSBsYXVuY2ggewogICAgICAgIHZhbCByZXN1bHQgPSBydW5DYXRjaGluZyB7IGRvd25sb2FkKCkgfSAgICAgIC8vIOKdjCBi4bqvdCBsdcO0biBDYW5jZWxsYXRpb25FeGNlcHRpb24KICAgICAgICBwcmludGxuKCJydW5DYXRjaGluZyB0cuG6oyB24buBOiAkcmVzdWx0IikKICAgICAgICBwcmludGxuKCJjb3JvdXRpbmUgduG6q24gY2jhuqF5IHRp4bq_cCBkw7kgxJHDoyBi4buLIGh14bu3LCBpc0FjdGl2ZSA9ICRpc0FjdGl2ZSIpCiAgICB9CiAgICBkZWxheSgxMDApCiAgICBqb2IuY2FuY2VsKCkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gbmfGsOG7nWkgZMO5bmcgcuG7nWkgbcOgbiBow6xuaAogICAgam9iLmpvaW4oKQoKICAgIC8vIOKchSBIw6BtIG5oxrAgcmVzdWx0T2YgdHJvbmcgUmF0ZVdhdGNoOiBjaG8gQ2FuY2VsbGF0aW9uRXhjZXB0aW9uIMSRaSBxdWEKICAgIHN1c3BlbmQgZnVuIDxUPiByZXN1bHRPZihibG9jazogc3VzcGVuZCAoKSAtPiBUKTogUmVzdWx0PFQ-ID0gdHJ5IHsKICAgICAgICBSZXN1bHQuc3VjY2VzcyhibG9jaygpKQogICAgfSBjYXRjaCAoZTogQ2FuY2VsbGF0aW9uRXhjZXB0aW9uKSB7CiAgICAgICAgdGhyb3cgZQogICAgfSBjYXRjaCAoZTogRXhjZXB0aW9uKSB7CiAgICAgICAgUmVzdWx0LmZhaWx1cmUoZSkKICAgIH0KICAgIHZhbCBqb2IyID0gbGF1bmNoIHsKICAgICAgICB2YWwgcmVzdWx0ID0gcmVzdWx0T2YgeyBkb3dubG9hZCgpIH0KICAgICAgICBwcmludGxuKCJraMO0bmcgYmFvIGdp4budIGluIGTDsm5nIG7DoHk6ICRyZXN1bHQiKQogICAgfQogICAgZGVsYXkoMTAwKQogICAgam9iMi5jYW5jZWwoKQogICAgam9iMi5qb2luKCkKICAgIHByaW50bG4oImpvYjIgZOG7q25nIGjhurNuLCBjYW5jZWxsZWQgPSAke2pvYjIuaXNDYW5jZWxsZWR9IikKfQo) · Xem thêm: K732, K799

### K732 · `catch (e: Exception)` trong vòng lặp: huỷ rồi mà vẫn chạy

`🟡 đã làm app` · `Kotlin`

**Gặp:** vòng "mỗi 30 giây hỏi server một lần" không dừng khi màn hình đóng; log `JobCancellationException` lặp liên tục,
CPU tăng.

**Vì sao:** `CancellationException` là một `Exception`. Bắt rồi lặp tiếp thì coroutine không bao giờ kết thúc; mỗi `delay`
sau đó lại ném ngay lập tức.

**Sửa:** ném lại `CancellationException` trước khi bắt `Exception`, hoặc kiểm `ensureActive()` trong vòng lặp.

```kotlin
while (true) {
    try { poll(); delay(30_000) }
    catch (e: CancellationException) { throw e }     // ✅ phải có dòng này
    catch (e: Exception) { log(e) }
}
```

[▶ Chạy thử](../playground/index.html#code=aW1wb3J0IGtvdGxpbnguY29yb3V0aW5lcy4qCgpmdW4gbWFpbigpID0gcnVuQmxvY2tpbmcgewogICAgdmFyIHBvbGxzID0gMAogICAgdmFsIGpvYiA9IGxhdW5jaCB7CiAgICAgICAgd2hpbGUgKHBvbGxzIDwgNSkgeyAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gaOG7j2kgc2VydmVyIG3hu5dpIDEwMCBtcywgdOG7kWkgxJFhIDUgbOG6p24KICAgICAgICAgICAgdHJ5IHsKICAgICAgICAgICAgICAgIGRlbGF5KDEwMCkKICAgICAgICAgICAgICAgIHByaW50bG4oInBvbGwgJHsrK3BvbGxzfSIpCiAgICAgICAgICAgIH0gY2F0Y2ggKGU6IEV4Y2VwdGlvbikgeyAgICAgICAgICAgICAgICAgLy8g4p2MIG514buRdCBj4bqjIENhbmNlbGxhdGlvbkV4Y2VwdGlvbgogICAgICAgICAgICAgICAgcHJpbnRsbigiYuG6r3QgxJHGsOG7o2MgJHtlOjpjbGFzcy5zaW1wbGVOYW1lfSwgbOG6t3AgdGnhur9wIikKICAgICAgICAgICAgICAgIHBvbGxzKysKICAgICAgICAgICAgfQogICAgICAgIH0KICAgIH0KICAgIGRlbGF5KDI1MCkKICAgIGpvYi5jYW5jZWwoKSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBtdeG7kW4gZOG7q25nIOG7nyDEkcOieSwgc2F1IDIgbOG6p24KICAgIGpvYi5qb2luKCkKICAgIHByaW50bG4oInBvbGxzID0gJHBvbGxzIikKfQo) · Xem thêm: K731

### K733 · Rời màn hình lúc đang tải thì hiện lỗi "Offline"

`🟡 đã làm app` · `KMP`

**Gặp:** bấm Back khi đang tải, quay lại thấy banner "Mất kết nối" dù mạng vẫn tốt.

**Vì sao:** lệnh huỷ đi qua lớp gọi API dưới dạng `CancellationException`. Có `catch (e: Exception)` đổi mọi lỗi thành
`Offline` thì lệnh huỷ cũng thành `Offline`, và state lỗi đó được lưu lại.

**Sửa:** nhánh đầu tiên của mọi `try` quanh lời gọi mạng là `catch (e: CancellationException) { throw e }`.
Viết test cho trường hợp này (`MockEngine` ném `CancellationException`, khẳng định nó đi xuyên qua).

```kotlin
private suspend fun send(url: String): HttpResponse = try {
    client.get(url)
} catch (e: CancellationException) {
    throw e                                             // ✅ không phải lỗi mạng
} catch (e: IOException) {
    throw RateApiException(RefreshError.Offline, e.message, e)
}
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt) ·
[`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/RateApiTest.kt) (`cancellation is passed through and never wrapped`)

### K734 · Gọi API trong `GlobalScope`: request chạy tiếp sau khi đóng màn hình

`🟢 mới học` · `Android` · `KMP`

**Gặp:** đóng màn hình mà request vẫn chạy, kết quả về thì ghi vào state của màn hình đã chết, có khi crash.

**Vì sao:** `GlobalScope` không gắn với vòng đời nào nên không ai huỷ nó. Ktor chỉ huỷ request khi coroutine gọi nó bị huỷ.

**Sửa:** gọi trong scope có chủ: `viewModelScope` cho việc của một màn hình, scope của core (huỷ khi core dừng) cho việc
phải sống qua nhiều màn hình.

```kotlin
GlobalScope.launch { api.latest("USD") }        // ❌ không ai huỷ
viewModelScope.launch { api.latest("USD") }     // ✅ huỷ khi ViewModel bị xoá
```

Xem thêm: K733

### K735 · `NetworkOnMainThreadException`

`🟢 mới học` · `Android`

**Gặp:** `android.os.NetworkOnMainThreadException` ngay khi gọi API.

**Vì sao:** Android cấm I/O mạng **chặn** trên main thread. Lỗi này đến từ API chặn như `HttpURLConnection` hoặc
`OkHttpClient.newCall(...).execute()`, không phải từ Ktor: lời gọi `suspend` của Ktor chạy mạng trên thread của engine.

**Sửa:** dùng Ktor (hoặc API `suspend`) thay cho lời gọi chặn. Bắt buộc phải gọi API chặn thì bọc trong
`withContext(Dispatchers.IO)`.

```kotlin
val text = URL(url).readText()                                          // ❌ chặn main thread
val text = withContext(Dispatchers.IO) { URL(url).readText() }          // ✅ (chỉ Android/JVM)
val text = client.get(url).bodyAsText()                                 // ✅ Ktor, gọi từ đâu cũng được
```

### K736 · Mọi request đều lỗi trên Android, iOS thì chạy

`🟢 mới học` · `Android`

**Gặp:** `SecurityException: Permission denied (missing INTERNET permission?)`; app chỉ hiện "Đang offline".

**Vì sao:** Android cần quyền `INTERNET` khai báo trong Manifest (không cần hỏi người dùng). iOS không cần gì cho HTTPS
nên cùng code `commonMain` chạy được ở iOS.

**Sửa:**

```xml
<uses-permission android:name="android.permission.INTERNET" />   <!-- ✅ trong <manifest>, ngoài <application> -->
```

**Gặp ở:** [`apps/RateWatch/androidApp/src/main/AndroidManifest.xml`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/AndroidManifest.xml)

### K737 · "CLEARTEXT communication … not permitted by network security policy"

`🟢 mới học` · `Android`

**Gặp:** `java.net.UnknownServiceException: CLEARTEXT communication to 192.168.1.10 not permitted by network security policy`
khi gọi server dev bằng `http://`.

**Vì sao:** từ Android 9, app mặc định chỉ được dùng HTTPS.

**Sửa:** dùng HTTPS. Server dev nội bộ thì cho phép HTTP **chỉ cho domain đó**, và chỉ trong bản debug.

```xml
<!-- res/xml/network_security_config.xml (debug) -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
    </domain-config>
</network-security-config>
<!-- AndroidManifest: <application android:networkSecurityConfig="@xml/network_security_config"> -->
```

Xem thêm: K738, K739

### K738 · iOS: "App Transport Security policy requires the use of a secure connection"

`🟢 mới học` · `iOS`

**Gặp:** `The resource could not be loaded because the App Transport Security policy requires the use of a secure connection.`
(NSURLErrorDomain `-1022`) khi gọi `http://`.

**Vì sao:** App Transport Security (ATS) của iOS chặn HTTP thường, giống K737 bên Android.

**Sửa:** dùng HTTPS. Server dev trong mạng LAN thì thêm ngoại lệ trong `Info.plist`, chỉ cho bản debug.

```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsLocalNetworking</key><true/>   <!-- ✅ chỉ mở cho địa chỉ trong mạng nội bộ -->
</dict>
```

Xem thêm: K737

### K739 · `localhost` trong emulator không phải máy Mac

`🟢 mới học` · `Android` · `iOS`

**Gặp:** server dev chạy trên Mac ở `localhost:8080`, trình duyệt Mac mở được, app trên Android emulator báo
`Failed to connect to localhost/127.0.0.1:8080`.

**Vì sao:** `localhost` là **chính thiết bị**. Android emulator là một máy ảo riêng; nó thấy máy Mac ở `10.0.2.2`.
iOS Simulator dùng chung mạng với Mac nên `localhost` chạy được. Máy thật thì phải dùng IP LAN của Mac.

**Sửa:** để base URL là cấu hình theo nền tảng/bản build, không viết cứng `localhost`.

| Chạy trên | Gọi server trên Mac bằng |
|---|---|
| Android emulator | `http://10.0.2.2:8080` |
| iOS Simulator | `http://localhost:8080` |
| Máy thật (cùng Wi-Fi) | `http://<IP LAN của Mac>:8080`, server phải nghe ở `0.0.0.0` |

Xem thêm: K737, K738

### K740 · "Failed to find HTTP client engine implementation"

`🟢 mới học` · `KMP` · `Gradle`

**Gặp:** `Failed to find HTTP client engine implementation: consider adding client engine dependency.`

**Vì sao:** `HttpClient()` không truyền engine thì Ktor tự tìm engine trong dependency. Chưa thêm engine nào cho target đó
(OkHttp cho Android, Darwin cho iOS) là không tìm được.

**Sửa:** thêm engine vào source set của từng nền tảng, và truyền engine tường minh.

```kotlin
androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
iosMain.dependencies     { implementation(libs.ktor.client.darwin) }
// androidMain: createHttpClient(OkHttp.create())   iosMain: createHttpClient(Darwin.create())
```

**Gặp ở:** [`apps/RateWatch/shared/build.gradle.kts`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/build.gradle.kts) · Xem thêm: K741

### K741 · Đặt OkHttp trong `commonMain`

`🟡 đã làm app` · `KMP` · `Gradle`

**Gặp:** Android build được, iOS báo `Unresolved reference 'okhttp'` hoặc Gradle không resolve được `ktor-client-okhttp`
cho target iOS.

**Vì sao:** OkHttp là thư viện JVM. `commonMain` chỉ được dùng thứ có trên **mọi** target.

**Sửa:** `commonMain` chỉ có `ktor-client-core` và một hàm tạo client **nhận engine làm tham số**. Mỗi nền tảng truyền
engine của mình; test truyền `MockEngine`. Ba nơi dùng chung đúng một cấu hình JSON và timeout.

```kotlin
// commonMain
fun createHttpClient(engine: HttpClientEngine, timeoutMillis: Long = 10_000): HttpClient =
    HttpClient(engine) { install(ContentNegotiation) { json(RateJson) } /* … */ }
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/HttpClients.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/HttpClients.kt) ·
[`apps/RateWatch/shared/src/androidMain/kotlin/dev/ratewatch/di/PlatformModule.android.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/androidMain/kotlin/dev/ratewatch/di/PlatformModule.android.kt) ·
[`apps/RateWatch/shared/src/iosMain/kotlin/dev/ratewatch/di/PlatformModule.ios.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/iosMain/kotlin/dev/ratewatch/di/PlatformModule.ios.kt)

### K742 · Tạo `HttpClient` mới cho mỗi request

`🟡 đã làm app` · `KMP`

**Gặp:** app chậm dần, bộ nhớ và số thread tăng sau mỗi lần kéo làm mới; profiler thấy rất nhiều connection pool.

**Vì sao:** mỗi `HttpClient` có engine, connection pool và thread riêng. Tạo mới mỗi lần là bỏ phí kết nối đã mở, và
client không `close()` thì tài nguyên còn treo.

**Sửa:** tạo **một** client cho cả app (`single { }` trong Koin) và dùng lại.

```kotlin
suspend fun latest(base: String) = HttpClient(OkHttp).get(url)   // ❌ mỗi lần một client
single { createHttpClient(OkHttp.create()) }                     // ✅ một client, Koin giữ
```

**Gặp ở:** [`apps/RateWatch/shared/src/androidMain/kotlin/dev/ratewatch/di/PlatformModule.android.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/androidMain/kotlin/dev/ratewatch/di/PlatformModule.android.kt) · Xem thêm: K743

### K743 · "Client already closed"

`🟡 đã làm app` · `KMP`

**Gặp:** `ClientEngineClosedException: Client already closed` ở request thứ hai.

**Vì sao:** client đã bị `close()`, thường do bọc trong `client.use { }` (đóng khi khối kết thúc) rồi vẫn giữ tham chiếu
để dùng tiếp.

**Sửa:** client dùng chung thì không `use { }`, không `close()` giữa chừng; chỉ đóng khi cả app/thành phần sở hữu nó dừng.
`use { }` chỉ hợp với client dùng một lần (script, test).

```kotlin
val client = createHttpClient(OkHttp.create())
client.use { it.get(a) }      // ❌ đóng luôn client dùng chung
client.get(b)                 //    → Client already closed
```

### K744 · Bắt `UnknownHostException` trong `commonMain`

`🟡 đã làm app` · `KMP` · `iOS`

**Gặp:** `Unresolved reference 'UnknownHostException'` khi build iOS. Hoặc tách `expect/actual` chỉ để nhận ra "mất mạng".

**Vì sao:** `UnknownHostException`, `ConnectException` là của Java. Trên iOS, engine Darwin ném `DarwinHttpRequestException`
(mã `NSURLErrorNotConnectedToInternet`…). Nhưng cả hai đều là `IOException` (`kotlinx.io.IOException` trong code chung).

**Sửa:** trong `commonMain` bắt `kotlinx.io.IOException` là phủ cả hai nền tảng (đặt sau các nhánh timeout, xem K729).
Thử offline trên iOS Simulator phải tắt Wi-Fi của **Mac**, vì Simulator dùng mạng của Mac.

```kotlin
} catch (e: java.net.UnknownHostException) { Offline }   // ❌ không có trên iOS
} catch (e: kotlinx.io.IOException) { Offline }          // ✅ OkHttp và Darwin đều vào đây
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt) · Xem thêm: K729

### K745 · Ghép query bằng chuỗi: dấu cách, dấu tiếng Việt làm hỏng URL

`🟢 mới học` · `KMP`

**Gặp:** tìm "bánh mì" thì server nhận `q=b` hoặc báo 400; tìm "a&b" thì server thấy hai tham số.

**Vì sao:** `"$base/search?q=$text"` không mã hoá ký tự đặc biệt (dấu cách, `&`, `#`, chữ có dấu).

**Sửa:** để Ktor mã hoá: `parameter(...)` trong builder của request.

```kotlin
client.get("$base/search?q=$text")                       // ❌
client.get("$base/search") { parameter("q", text) }      // ✅ q=b%C3%A1nh+m%C3%AC
```

### K746 · Log của Ktor in cả token đăng nhập

`🟡 đã làm app` · `KMP`

**Gặp:** Logcat hoặc file log của bản release có dòng `Authorization: Bearer eyJ…`.

**Vì sao:** plugin `Logging` (`ktor-client-logging`) ở mức `HEADERS`/`ALL` in mọi header và body.

**Sửa:** ẩn header nhạy cảm bằng `sanitizeHeader`, và chỉ bật log chi tiết ở bản debug.

```kotlin
install(Logging) {
    level = if (isDebug) LogLevel.HEADERS else LogLevel.NONE
    sanitizeHeader { header -> header == HttpHeaders.Authorization }   // ✅ in thành ***
}
```

### K747 · Hiện thẳng `e.message` cho người dùng

`🟢 mới học` · `KMP`

**Gặp:** màn hình hiện `Unable to resolve host "open.er-api.com": No address associated with hostname` hoặc
`Request timeout has expired [url=…]`, tiếng Anh, khác nhau giữa Android và iOS.

**Vì sao:** message của exception viết cho lập trình viên, và mỗi engine viết một kiểu.

**Sửa:** tầng gọi API đổi mọi lỗi thành một enum/sealed class nhỏ (`Offline`, `Timeout`, `ServerError`, `BadResponse`…).
UI chỉ đổi enum đó ra chuỗi đã dịch. `e.message` chỉ để log.

```kotlin
enum class RefreshError { Offline, Timeout, ServerError, BadResponse, UnknownBase, Unknown }
class RateApiException(val reason: RefreshError, message: String? = null, cause: Throwable? = null) :
    Exception(message ?: reason.name, cause)
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateApi.kt) ·
[`apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/Labels.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/androidApp/src/main/kotlin/dev/ratewatch/android/ui/Labels.kt)

### K748 · `HttpRequestRetry` gửi lại cả POST

`🔴 khó, hiếm nhưng đau` · `KMP`

**Gặp:** server chậm một lần, đơn hàng bị tạo hai, ba lần.

**Vì sao:** `install(HttpRequestRetry)` mặc định thử lại tới 3 lần khi có exception hoặc status 5xx, với **mọi** method.
POST không idempotent: server có thể đã xử lý xong rồi mới lỗi ở bước trả lời. Timeout thì mặc định **không** thử lại
(`retryOnTimeout = false`).

**Sửa:** chỉ thử lại request an toàn (GET), có giới hạn và giãn cách. Cần thử lại POST thì server phải hỗ trợ
idempotency key.

```kotlin
install(HttpRequestRetry) {
    retryIf(maxRetries = 3) { request, response ->
        request.method == HttpMethod.Get && response.status.value in 500..599   // ✅ chỉ GET
    }
    exponentialDelay()
}
```

### K749 · Tải hỏng là mất luôn dữ liệu cũ

`🟡 đã làm app` · `KMP`

**Gặp:** mất mạng lúc làm mới, danh sách đang có biến mất, màn hình trống trơn.

**Vì sao:** code xoá dữ liệu cũ **trước** khi gọi mạng, hoặc gọi mạng **bên trong** transaction: request lỗi thì phần đã xoá
không có gì thay thế (và transaction giữ khoá database suốt lúc chờ mạng).

**Sửa:** gọi mạng trước, ngoài transaction. Chỉ khi có dữ liệu hợp lệ mới mở transaction để xoá-và-ghi một lượt.

```kotlin
val response = api.latest(base)                 // ✅ lỗi ở đây thì bảng cũ còn nguyên
db.rateQueries.transaction {
    db.rateQueries.deleteRates(base)
    response.rates.orEmpty().forEach { (code, rate) -> db.rateQueries.insertRate(base, code, rate) }
}
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt) ·
[`apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/SqlRateRepositoryTest.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonTest/kotlin/dev/ratewatch/SqlRateRepositoryTest.kt) (`a failed refresh leaves the saved table untouched`)

### K750 · Dùng giờ của server để biết dữ liệu "cũ" hay chưa

`🔴 khó, hiếm nhưng đau` · `KMP`

**Gặp:** lần mở app nào cũng tải lại, dù vừa tải 5 phút trước; banner "dữ liệu cũ" không bao giờ tắt.

**Vì sao:** `time_last_update_unix` là lúc **nhà cung cấp** cập nhật (1 lần/ngày), không phải lúc app tải về. So nó với
"cũ hơn 1 giờ" thì gần như luôn đúng.

**Sửa:** đóng dấu thời điểm tải bằng **đồng hồ của app** (`now` truyền vào), lưu cùng dữ liệu. Giờ của server chỉ để hiển thị.
Đồng hồ là tham số nên test được (xem K766).

```kotlin
db.rateQueries.upsertTable(base, response.timeLastUpdateUnix!! * 1000)   // ❌ giờ của nhà cung cấp
db.rateQueries.upsertTable(base, now)                                    // ✅ giờ app tải về
```

**Gặp ở:** [`apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/shared/src/commonMain/kotlin/dev/ratewatch/data/RateRepository.kt) · Xem thêm: K705
