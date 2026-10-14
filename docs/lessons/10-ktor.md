# Gọi API với Ktor

Ktor client là thư viện HTTP multiplatform: viết code gọi API một lần trong `commonMain`, mỗi nền tảng chỉ gắn một *engine*.
Ghép với `kotlinx.serialization` để đổi JSON thành data class.

## Mục tiêu

- Habit Tick hiện 3 ngày lễ sắp tới của quốc gia đã chọn.
- Mất mạng thì báo lỗi gọn, không crash.

## Cài

| Thư viện | Source set |
|---|---|
| `ktor-client-core`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json` | `commonMain` |
| `ktor-client-okhttp` | `androidMain` |
| `ktor-client-darwin` | `iosMain` |
| `ktor-client-okhttp` (hoặc `cio`) | `desktopMain` |

Thêm plugin `kotlinx-serialization`. Android cần quyền mạng trong `AndroidManifest.xml`:
`<uses-permission android:name="android.permission.INTERNET"/>`.

## API dùng trong bài

[Nager.Date](https://date.nager.at) — API ngày lễ công khai, không cần key:
`GET https://date.nager.at/api/v3/PublicHolidays/{year}/{countryCode}` trả về mảng JSON có `date`, `localName`, `name`.

## Làm trên app MVP

```kotlin
@Serializable
data class Holiday(val date: String, val localName: String, val name: String)

class HolidayApi(private val client: HttpClient) {
    suspend fun upcoming(country: String, today: String, count: Int = 3): List<Holiday> {
        val year = today.take(4)
        val all: List<Holiday> = client.get("https://date.nager.at/api/v3/PublicHolidays/$year/$country").body()
        return all.filter { it.date >= today }.take(count)   // ngày ISO so sánh chuỗi được
    }
}

fun httpClient() = HttpClient {   // engine tự chọn theo thư viện có trong từng source set
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}
```

Trong ViewModel, bọc lời gọi bằng `runCatching` và đưa kết quả vào state:

```kotlin
sealed interface HolidayUi {
    data object Loading : HolidayUi
    data class Ready(val items: List<Holiday>) : HolidayUi
    data object Offline : HolidayUi
}
```

## Kiểm tra

- [ ] Màn danh sách hiện 3 ngày lễ sắp tới (thử `VN` và `US`).
- [ ] Bật chế độ máy bay → hiện "Không có mạng", app không crash.
- [ ] Không có key hay mật khẩu nào trong code.

## Lỗi hay gặp

- Android báo lỗi bảo mật / không kết nối → quên quyền `INTERNET`.
- `NoTransformationFoundException` → quên `install(ContentNegotiation)` hoặc quên plugin serialization.
- JSON có trường lạ làm crash → thêm `ignoreUnknownKeys = true`.

## Tiếp theo?

- [Bài 11: nối với iOS](11-ios-integration.md).
- Ví dụ docs gốc dùng logic chung + UI native: [Shared logic for REST API requests](https://kotlinlang.org/docs/multiplatform/multiplatform-upgrade-app.html).

## Docs gốc

[Ktor + SQLDelight tutorial](https://kotlinlang.org/docs/multiplatform/multiplatform-ktor-sqldelight.html) · [Ktor client docs](https://ktor.io/docs/client-create-multiplatform-application.html)
