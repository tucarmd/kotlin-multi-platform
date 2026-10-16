# App 3 · Rate Watch 💱

> Tỷ giá ngoại tệ theo đồng gốc (VND, USD…), đổi tiền nhanh, đánh dấu yêu thích. Mất mạng vẫn xem được tỷ giá lần trước.

## 1. Tóm tắt

| | |
|---|---|
| **Cấp độ** | ⭐⭐⭐ |
| **Mới học** | Ktor client + `kotlinx.serialization` · engine OkHttp / Darwin · **offline-first** (mạng + cache SQLDelight) · dữ liệu "cũ" (stale) · `MockEngine` |
| **Cần trước** | App 2 · [bài 11](../lessons/11-network.md) |
| **Thư mục code** | [`apps/RateWatch/`](https://github.com/tucarmd/kotlin-multi-platform/tree/main/apps/RateWatch) · package `dev.ratewatch` · ✅ đã có code |
| **API** | [open.er-api.com](https://www.exchangerate-api.com/docs/free) `GET /v6/latest/{base}`, miễn phí, không cần key, cập nhật 1 lần / ngày |
| **Thời gian** | 5–6 buổi tối |

So với app 2: repository giờ có **hai nguồn** (mạng và database), và core phải trả lời "đang hiện dữ liệu mới hay cũ".

## 2. Màn hình

| Màn | Hiện gì | Action |
|---|---|---|
| **Rates** | Đồng gốc · "cập nhật 2 giờ trước" (đỏ nếu cũ) · banner "Đang offline" · danh sách tỷ giá, yêu thích ở trên | Kéo để làm mới · bấm sao · chọn đồng gốc |
| **Convert** | Ô số tiền, 2 đồng tiền, nút đảo chiều, kết quả | Gõ số, đổi đồng, đảo |

## 3. Phạm vi

| Có | Làm sau |
|---|---|
| Lấy tỷ giá, cache, xem offline | Biểu đồ tỷ giá theo ngày |
| Yêu thích, đổi đồng gốc | Thông báo khi tỷ giá vượt ngưỡng |
| Kéo để làm mới, tự làm mới khi cache > 1 giờ | Widget |
| Lỗi mạng / server / JSON sai đều thành state | |

## 4. Kiến trúc

```mermaid
flowchart LR
  UI[Rates / Convert] -->|action| CORE[RatesCore]
  CORE -->|StateFlow&lt;RatesState&gt;| UI
  CORE --> RULES["rules: convert · isStale · freshness"]
  CORE --> REPO[RateRepository]
  REPO --> API[RateApi · Ktor]
  REPO --> DB[(SQLDelight · Rate.sq)]
  API -.-> OK[androidMain: OkHttp]
  API -.-> DW[iosMain: Darwin]
```

Luồng **offline-first**: UI luôn đọc từ database. Mạng chỉ ghi vào database. Mạng lỗi thì vẫn còn dữ liệu cũ + thông báo lỗi.

## 5. 7 bước làm

| Bước | Ở app này làm gì |
|:-:|---|
| **B1** | `model/`: `RateTable(base, rates: Map<String, Double>, fetchedAt)`, `RatesState(table, freshness, refreshing, error)`. `rules/`: `convert(amount, from, to, table)` qua đồng gốc, `isStale(fetchedAt, now, maxAge = 1.h)`. |
| **B2** | `RatesCore`: `state` từ `repository.observe(base)`; `refresh()` bật `refreshing`, gọi repository, lỗi → `error` nhưng **giữ** `table`. Tự gọi `refresh()` khi mở app nếu `isStale`. |
| **B3** | `RateApi` (Ktor + `ContentNegotiation` + `@Serializable RateResponse`). `RateRepository.refresh(base)`: gọi API → map sang model → ghi DB trong một transaction. `Rate.sq` + bảng `Favorite`. |
| **B4** | `platformModule`: `HttpClient(OkHttp)` / `HttpClient(Darwin)` + driver DB. `ratesCore()` cho Swift. |
| **B5** | `PullToRefreshBox`, `LazyColumn` có header yêu thích, banner offline bằng `AnimatedVisibility`. |
| **B6** | `.refreshable { }`, `List` + `Section`, `.toolbar` chọn đồng gốc. |
| **B7** | `RateApiTest` với `MockEngine` (200, 500, JSON thiếu field, timeout) · `RatesCoreTest` (refresh lỗi vẫn giữ dữ liệu cũ) · `RulesTest`. |

## 6. Bản đồ function

Bảng đầy đủ (20 hàm, có link tới đúng dòng code) nằm trong
[`apps/RateWatch/README.md`](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/RateWatch/README.md#ban-o-function). Các hàm chính:

| Hàm | File | Làm gì | Vì sao |
|---|---|---|---|
| `convert(amount, from, to, table)` | `rules/Convert.kt` | Đổi chéo qua đồng gốc; `amount` là `Long` tính theo phần trăm đơn vị | API chỉ trả tỷ giá theo 1 đồng gốc; tiền gõ vào vẫn chính xác như app 1 |
| `isStale(fetchedAt, now, maxAge)` | `rules/Freshness.kt` | Cũ hơn 1 giờ → `true` (đúng 1 giờ vẫn còn mới) | Quyết định tự làm mới và tô đỏ "cập nhật lúc…" |
| `parseAmount(text)` / `formatRate(rate)` | `rules/Amount.kt` | Đọc "12,50" / "120.000"; in tỷ giá ~6 chữ số có nghĩa | Hai máy in ra giống nhau |
| `rateRows(table, favorites)` | `rules/Rates.kt` | Yêu thích lên đầu, rồi A→Z | Một luật sắp xếp, có test |
| `createHttpClient(engine)` | `data/HttpClients.kt` | ContentNegotiation + `ignoreUnknownKeys` + timeout | Test `MockEngine` dùng đúng cấu hình thật |
| `RateApi.latest(base)` | `data/RateApi.kt` | `GET /v6/latest/{base}`, lỗi → `RateApiException(reason)` | Chỉ biết HTTP + JSON |
| `RateRepository.observe(base)` | `data/RateRepository.kt` | `Flow<RateTable?>` từ database | UI chỉ đọc một nguồn → offline tự nhiên |
| `RateRepository.refresh(base, now)` | `data/RateRepository.kt` | API → DB trong 1 transaction, trả `Result<Unit>` | Không ném exception qua core, không bao giờ hiện bảng ghi dở |
| `resultOf { }` | `data/RateRepository.kt` | Như `runCatching` nhưng ném lại `CancellationException` | Hủy coroutine không hiện thành lỗi |
| `RatesCore.refresh()` | `core/RatesCore.kt` | Bật `refreshing`; lỗi → `error` nhưng **giữ** bảng cũ | Mất mạng không làm trống màn hình |

!!! note "Code khác plan ở đâu"
    - `RatesState` có thêm `base`, `rows`, `age`, `currencies`, `showOfflineBanner`, `showEmptyError`. Màn Convert có `StateFlow<ConvertState>` riêng, cùng một `RatesCore`.
    - Database chia `rate_table` (base, fetchedAt) + `rate`; thêm `Setting.sq` nhớ đồng gốc đã chọn, nên mở lại khi offline vẫn đúng đồng.
    - `RefreshError`: `Offline`, `Timeout`, `ServerError`, `BadResponse`, `UnknownBase`, `Unknown`. Banner offline hiện cho mọi lỗi làm mới khi đã có bảng cũ, kèm lý do.
    - Độ "cũ" tính theo lúc **máy này** tải về, không theo `time_last_update_unix` của API (API chỉ cập nhật 1 lần / ngày).

Bẫy cần ghi `// TRAP:` trong code:

- `Json { ignoreUnknownKeys = true }`: API thêm field mới thì app cũ không được crash.
- Tỷ giá `Double` là chấp nhận được (hiển thị), nhưng số tiền người dùng nhập vẫn dùng `Money` từ app 1.
- Hủy coroutine khi rời màn: lỗi `CancellationException` không phải lỗi mạng, đừng hiện cho người dùng.
- iOS Simulator dùng mạng của Mac: muốn thử offline thì tắt Wi-Fi của Mac.
- **API báo lỗi bằng HTTP 200**: đồng gốc sai trả `{"result":"error","error-type":"unsupported-code"}`, không có `rates`. Mọi field trừ `result` phải là optional và tự kiểm.
- `flatMapLatest` khi đổi đồng gốc phát lại bảng của đồng cũ một nhịp: core bỏ bảng nào có `base` không khớp.
- Test core chạy trong `backgroundScope` của `runTest` không chạy khi gọi `advanceUntilIdle()`: cho core scope riêng trên test scheduler.
- SwiftUI `Button` trong dòng của `List` cần `.buttonStyle(.borderless)`, không thì bấm đâu trên dòng cũng ăn.

## 7. Test

```bash
cd apps/RateWatch
./gradlew :shared:testDebugUnitTest :shared:iosSimulatorArm64Test
```

Case tối thiểu: JSON đúng · HTTP 500 · JSON thiếu `rates` · timeout · refresh lỗi giữ dữ liệu cũ · đổi chéo EUR → JPY qua USD · stale đúng 1 giờ.

## 8. Xong khi

- [ ] Mở app có mạng → thấy tỷ giá; bật chế độ máy bay, tắt mở lại app → vẫn thấy + banner offline
- [ ] Kéo làm mới khi offline → báo lỗi, danh sách **không** trống
- [ ] Đổi tiền ra cùng kết quả trên 2 máy
- [ ] Test `MockEngine` đủ 4 case lỗi
- [ ] KDoc `[App 3 · Bx]` mọi file · README đủ 7 mục

## 9. Tham khảo

- [kotlin-hands-on/kmp-networking-and-data-storage](https://github.com/kotlin-hands-on/kmp-networking-and-data-storage): Ktor + SQLDelight cùng nhau
- [touchlab/KaMPKit](https://github.com/touchlab/KaMPKit): repository có cache, test với `MockEngine`
- [Ktor client: testing](https://ktor.io/docs/client-testing.html)
- Device Pulse: `sensor/LatencyProbe.kt`, `LatencyProbeTest.kt`

**Trước:** [App 2 · Pocket Notes](02-pocket-notes.md) · **Giữa:** [Device Pulse](../project/index.md) (cảm biến + quyền) · **Tiếp:** [App 4 · Trip Log](04-trip-log.md)
