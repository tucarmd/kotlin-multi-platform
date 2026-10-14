# Kotlin làm web được không?

**Được.** Kotlin có 4 đường để làm web, mỗi đường mạnh một kiểu. Trang này tóm tắt nghiên cứu tháng 10/2026 từ docs chính chủ, để quyết định
có nên đưa Device Pulse lên trình duyệt hay không.

!!! info "Kết luận ngắn"
    - **Logic chung** (model, luật, Ktor client) chạy trên web: **Stable** qua Kotlin/JS, **Beta** qua Kotlin/Wasm.
    - **UI Compose chung** chạy trên web: **Beta** (Compose Multiplatform 1.12.x, target `wasmJs`). Dùng được cho app nội bộ, demo, dashboard.
    - Làm **trang công khai cần SEO** thì không nên dùng Compose vẽ canvas. Chọn Compose HTML (Kobweb) hoặc web stack thường.

## 4 đường làm web bằng Kotlin

| Đường | Chạy thế nào | Trạng thái | Hợp với |
|---|---|---|---|
| **Compose Multiplatform for web** | Biên dịch sang **WebAssembly** (`wasmJs`), vẽ UI lên một thẻ `<canvas>` | Beta | Dùng lại **nguyên** UI Compose của app mobile. Dashboard, tool nội bộ, demo |
| **Compose HTML** / [Kobweb](https://github.com/varabyte/kobweb) | Kotlin/JS, composable sinh ra **thẻ HTML thật** (DOM) | Compose HTML ổn định lâu năm; Kobweb là framework cộng đồng | Website, landing page, blog, cần SEO và chọn chữ được |
| **Kotlin/JS + React** ([kotlin-wrappers](https://github.com/JetBrains/kotlin-wrappers)) | Kotlin/JS gọi React, MUI… qua wrapper | Kotlin/JS: Stable | Team đã quen React, muốn dùng thư viện JS sẵn có |
| **Chỉ chia sẻ logic** | Module KMP thêm target `js`, xuất ra thư viện JS/TypeScript cho app web hiện có | Stable | Đã có web viết bằng TS/Angular/React, chỉ muốn dùng chung luật nghiệp vụ |

Ngoài trình duyệt, Kotlin còn viết được **backend** bằng [Ktor](https://ktor.io/) (Stable, JVM). Phần `model/` và `rules/` dùng chung
được giữa app, web và server.

## Compose trên web: ngon ở đâu, vướng ở đâu

```kotlin
// wasmJsMain/kotlin/main.kt — toàn bộ "app web"
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        App()   // đúng composable App() của Android và iOS
    }
}
```

| Ngon | Vướng |
|---|---|
| Dùng lại 100% UI `commonMain`: Material 3, navigation, resources, ViewModel | Vẽ canvas nên **Google không đọc được chữ** → SEO kém |
| JetBrains đo thấy Wasm **nhanh hơn JS**, gần bằng JVM (benchmark trên Chrome) | File tải lần đầu nặng (runtime Skia + font) |
| Một lệnh Gradle là chạy: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` | Chọn chữ, Ctrl+F, chuột phải, screen reader **không tự nhiên** như HTML |
| | Vẫn **Beta**: API có thể đổi giữa các bản |

### Trình duyệt chạy được Kotlin/Wasm

Cần trình duyệt hỗ trợ **WasmGC** ([nguồn](https://kotlinlang.org/docs/wasm-configuration.html#browser-versions)):

| Trình duyệt | Bản tối thiểu |
|---|---|
| Chrome, Edge, Brave, Opera, Samsung Internet | 119 |
| Firefox | 120 |
| Safari | **18.2**, bản cũ hơn không có cách nào chạy |

Không có chế độ tự lùi về JS khi trình duyệt cũ. Cần hỗ trợ iPhone đời cũ thì phải dùng target `js`.

## Nếu đưa Device Pulse lên web

Thử nghĩ xem app của lộ trình chuyển sang web được tới đâu. Phần chung gần như giữ nguyên; phần khó là cảm biến, vì trình duyệt
không cho đọc nhiều thứ như điện thoại.

| Phần | Lên web thế nào |
|---|---|
| UI, ViewModel, `rules/`, navigation, strings vi/en | Giữ nguyên, chỉ thêm target `wasmJs { browser() }` |
| `LatencyProbe` (Ktor) | Ktor client có engine cho trình duyệt. Lưu ý **CORS**: URL đo phải cho phép gọi từ trình duyệt |
| Koin | Hỗ trợ web, thêm `platformModule` bản web |
| 📍 Vị trí | `navigator.geolocation`: chạy trên mọi trình duyệt, cần HTTPS |
| 🔋 Pin | Battery Status API **chỉ có trên Chrome/Edge**; Firefox và Safari → trả `Reading.Unsupported` |
| 📶 Mạng | `navigator.onLine` có mọi nơi; loại mạng (Wi-Fi/4G) chỉ Chrome có → còn lại `Unsupported` |
| 🕘 Lịch sử (SQLDelight) | Phần khó nhất: phải đổi driver sang SQLite chạy trong Web Worker, hoặc lưu `localStorage`. Kiểm tra docs SQLDelight lúc làm |

Thiết kế 5 trạng thái `Reading` của Device Pulse rất hợp với web: cảm biến nào trình duyệt không có thì trả `Unsupported`,
UI không phải sửa gì.

!!! tip "Đề xuất"
    Không đưa web vào 14 bài chính, vì web vẫn Beta và Device Pulse là app cảm biến, vốn là phần web yếu nhất.
    Có thể làm **bài thêm sau bài 14**: thêm target `wasmJs`, cài 3 cảm biến bằng API trình duyệt, đăng lên GitHub Pages
    cạnh site này. Bài đó luyện đúng kỹ năng KMP cốt lõi: thêm một nền tảng mới mà không đụng `commonMain`.

## Repo và nguồn để đọc tiếp

| Repo / trang | Dùng để |
|---|---|
| [Get started with Kotlin/Wasm and Compose Multiplatform](https://kotlinlang.org/docs/wasm-get-started.html) | Tutorial chính chủ, làm đầu tiên |
| [Kotlin/kotlin-wasm-compose-template](https://github.com/Kotlin/kotlin-wasm-compose-template) | Template Compose + Wasm chạy ngay |
| [Kotlin/kotlin-wasm-examples](https://github.com/Kotlin/kotlin-wasm-examples) | Ví dụ Wasm: Compose, trình duyệt, Node, WASI |
| [JetBrains/compose-multiplatform-html-library-template](https://github.com/JetBrains/compose-multiplatform-html-library-template) | Template Compose HTML (DOM thật) |
| [varabyte/kobweb](https://github.com/varabyte/kobweb) | Framework web full-stack trên Compose HTML, có SEO, routing |
| [JetBrains/kotlin-wrappers](https://github.com/JetBrains/kotlin-wrappers) | Dùng React và thư viện JS từ Kotlin |
| [Supported platforms](https://kotlinlang.org/docs/multiplatform/supported-platforms.html) | Bảng trạng thái Stable/Beta từng nền tảng (xem lại khi có bản mới) |
| [Kotlin/Wasm overview](https://kotlinlang.org/docs/wasm-overview.html) | Hiệu năng, giới hạn, WASI |

*Nghiên cứu ngày 14/10/2026. Lúc này bản mới nhất là Compose Multiplatform 1.12.1 và web vẫn Beta. Trạng thái web đổi nhanh,
nên mở trang Supported platforms để xem lại trước khi quyết định.*
