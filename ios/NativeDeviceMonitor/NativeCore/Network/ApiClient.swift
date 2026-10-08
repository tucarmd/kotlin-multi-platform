import Foundation

/// Day 10 · result of one request — the View only `switch`es over these 5 cases, never sees an `Error`.
/// Mirrors `android/.../nativecore/network/ApiClient.kt` (`sealed interface ApiResult`).
enum ApiResult: Equatable {
    case success(status: Int, body: String, contentType: String?, durationMs: Int)
    /// The server DID answer but ≠ 2xx (404, 500…) — very different from "couldn't reach the server".
    case httpError(status: Int, body: String, durationMs: Int)
    case timeout(seconds: Double, durationMs: Int)
    case noNetwork(message: String, durationMs: Int)
    case failure(message: String, durationMs: Int)
}

/// Day 10 — minimal API Client: `get(_:params:headers:)` · `post(_:body:headers:)`.
///
/// ```
///  View (tab API) ──► await ApiClient.get/post
///                       └─ URLSession.data(for:)   ← runs off the main thread by itself, no DispatchQueue needed
///                            ├─ HTTPURLResponse 2xx → .success
///                            ├─ otherwise           → .httpError   (URLSession does NOT throw for 500 — check it yourself)
///                            └─ throw URLError      → classify → .timeout / .noNetwork / .failure
/// ```
///
/// Unlike Android: `URLSession` treats 404/500 as "a network success" and returns `data` normally —
/// there's no `errorStream` like `HttpURLConnection`. You must read `statusCode` yourself.
///
/// `session` is injectable → unit tests swap in a session with a fake `URLProtocol`, no internet needed.
struct ApiClient {

    static let shared = ApiClient()

    /// Sent with every request — the "Headers" in the spec.
    static let defaultHeaders = ["Accept": "application/json", "X-Client": "NativeDeviceMonitor-iOS"]

    let session: URLSession
    /// 8 s — under 10 s so the `httpbin.org/delay/10` preset reliably ends in Timeout. URLSession's default is 60 s.
    let timeout: TimeInterval

    init(session: URLSession = .shared, timeout: TimeInterval = 8) {
        self.session = session
        self.timeout = timeout
    }

    func get(_ url: String, params: [String: String] = [:], headers: [String: String] = [:]) async -> ApiResult {
        guard let full = Self.buildURL(url, params: params) else {
            return .failure(message: "Invalid URL: \(url)", durationMs: 0)
        }
        return await send(method: "GET", url: full, body: nil, headers: headers)
    }

    /// `body` is a JSON string — adds `Content-Type: application/json` automatically.
    func post(_ url: String, body: String, headers: [String: String] = [:]) async -> ApiResult {
        guard let full = Self.buildURL(url, params: [:]) else {
            return .failure(message: "Invalid URL: \(url)", durationMs: 0)
        }
        let merged = ["Content-Type": "application/json; charset=utf-8"].merging(headers) { _, new in new }
        return await send(method: "POST", url: full, body: Data(body.utf8), headers: merged)
    }

    private func send(method: String, url: URL, body: Data?, headers: [String: String]) async -> ApiResult {
        var request = URLRequest(url: url, timeoutInterval: timeout)
        request.httpMethod = method
        request.httpBody = body
        for (k, v) in Self.defaultHeaders.merging(headers, uniquingKeysWith: { _, new in new }) {
            request.setValue(v, forHTTPHeaderField: k)
        }
        let start = Date()
        func elapsed() -> Int { Int(Date().timeIntervalSince(start) * 1000) }
        do {
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else {
                return .failure(message: "Not an HTTP response", durationMs: elapsed())
            }
            let text = String(decoding: data, as: UTF8.self)
            if (200...299).contains(http.statusCode) {
                return .success(status: http.statusCode, body: text,
                                contentType: http.value(forHTTPHeaderField: "Content-Type"), durationMs: elapsed())
            }
            return .httpError(status: http.statusCode, body: text, durationMs: elapsed())
        } catch {
            return Self.classify(error, timeout: timeout, durationMs: elapsed())
        }
    }

    // MARK: - Pure rules (unit tests need no network)

    /// `Error` → one of 3 branches.
    static func classify(_ error: Error, timeout: TimeInterval, durationMs: Int) -> ApiResult {
        guard let e = error as? URLError else {
            return .failure(message: error.localizedDescription, durationMs: durationMs)
        }
        switch e.code {
        case .timedOut:
            return .timeout(seconds: timeout, durationMs: durationMs)
        // Airplane mode → .notConnectedToInternet · broken DNS → .cannotFindHost · server down → .cannotConnectToHost
        case .notConnectedToInternet, .networkConnectionLost, .cannotFindHost, .cannotConnectToHost,
             .dataNotAllowed, .internationalRoamingOff:
            return .noNetwork(message: "URLError \(e.code.rawValue): \(e.localizedDescription)", durationMs: durationMs)
        default:
            return .failure(message: "URLError \(e.code.rawValue): \(e.localizedDescription)", durationMs: durationMs)
        }
    }

    /// Append query `?a=1&b=2`, keys sorted alphabetically (Android does the same → tests can compare).
    /// Encoded by hand: `URLQueryItem` leaves `&` / `+` in values untouched → `a&b` would be read by the server as 2 params.
    static func buildURL(_ base: String, params: [String: String]) -> URL? {
        guard var comps = URLComponents(string: base), comps.scheme != nil, comps.host != nil else { return nil }
        guard !params.isEmpty else { return comps.url }
        var allowed = CharacterSet.urlQueryAllowed
        allowed.remove(charactersIn: "&=+?/")
        func enc(_ s: String) -> String { s.addingPercentEncoding(withAllowedCharacters: allowed) ?? s }
        let extra = params.sorted { $0.key < $1.key }.map { "\(enc($0.key))=\(enc($0.value))" }.joined(separator: "&")
        comps.percentEncodedQuery = [comps.percentEncodedQuery, extra].compactMap { $0 }.joined(separator: "&")
        return comps.url
    }

    /// JSON → indented for reading; not JSON → returned as-is.
    static func prettyJSON(_ body: String) -> String {
        guard let data = body.data(using: .utf8),
              let obj = try? JSONSerialization.jsonObject(with: data),
              JSONSerialization.isValidJSONObject(obj),
              let out = try? JSONSerialization.data(withJSONObject: obj, options: [.prettyPrinted, .withoutEscapingSlashes])
        else { return body }
        return String(decoding: out, as: UTF8.self)
    }
}
