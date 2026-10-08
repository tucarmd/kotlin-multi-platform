import Foundation

/// 1 test button = 1 row of the Day 10 test table. Mirrors `android/.../ui/api/ApiStrings.kt` — same URLs, same order.
struct ApiPreset: Identifiable {
    var id: String { label }
    let label: String
    let method: String
    let url: String
    var params: [String: String] = [:]
    var body: String?
}

/// API tab strings (Day 10).
enum ApiStrings {
    static let title = "API"
    static let hint = "Day 10 — GET / POST JSON with headers, query params, HTTP status, timeout (8 s) and no-network handling."

    static let presetsTitle = "Test requests"
    static let customTitle = "Custom GET"
    static let urlPlaceholder = "https://…"
    static let sendGet = "Send GET"
    static let resultTitle = "Response"
    static let nothingYet = "Tap a test request."
    static let sending = "Sending…"
    static let airplaneHint = "No network test: turn on airplane mode, then tap any request."

    static let postBody = #"{"title":"hello","body":"from Native Device Monitor","userId":1}"#

    static let presets: [ApiPreset] = [
        ApiPreset(label: "GET /posts/1 → 200", method: "GET", url: "https://jsonplaceholder.typicode.com/posts/1"),
        ApiPreset(label: "GET /posts?userId=1 (params)", method: "GET", url: "https://jsonplaceholder.typicode.com/posts",
                  params: ["userId": "1"]),
        ApiPreset(label: "POST /posts → 201", method: "POST", url: "https://jsonplaceholder.typicode.com/posts", body: postBody),
        ApiPreset(label: "GET headers echo", method: "GET", url: "https://httpbin.org/headers"),
        ApiPreset(label: "GET /status/500 → HTTP error", method: "GET", url: "https://httpbin.org/status/500"),
        ApiPreset(label: "GET /delay/10 → Timeout", method: "GET", url: "https://httpbin.org/delay/10"),
    ]

    /// Bold status line. `ok` = green, otherwise red.
    static func statusLine(_ r: ApiResult) -> (text: String, ok: Bool) {
        switch r {
        case let .success(status, _, _, ms): return ("SUCCESS · HTTP \(status) · \(ms) ms", true)
        case let .httpError(status, _, ms): return ("HTTP ERROR · \(status) · \(ms) ms", false)
        case let .timeout(seconds, _): return ("TIMEOUT · no response after \(Int(seconds)) s", false)
        case let .noNetwork(message, _): return ("NO NETWORK · \(message)", false)
        case let .failure(message, _): return ("FAILED · \(message)", false)
        }
    }
}
