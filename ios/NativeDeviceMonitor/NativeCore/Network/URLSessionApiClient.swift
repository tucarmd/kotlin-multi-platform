import Foundation

/// Way 1 — OWN CODE on Apple's framework (`URLSession`), zero dependencies.
///
/// ```
///  get/post ─► ApiHelpers.buildURL ─► URLRequest (method · headers · body · timeout)
///    └─ try await session.data(for:)        ← runs off the main thread by itself, no DispatchQueue needed
///         ├─ HTTPURLResponse  → ApiHelpers.result → .success (2xx) / .httpError (else)
///         │                     URLSession does NOT throw for 500 — check statusCode yourself
///         └─ throw URLError   → ApiHelpers.classify → .timeout / .noNetwork / .failure
/// ```
///
/// Unlike Android: `URLSession` treats 404/500 as "a network success" and returns `data` normally —
/// there's no `errorStream` like `HttpURLConnection`. You must read `statusCode` yourself.
///
/// `session` is injectable → unit tests swap in a session with a fake `URLProtocol`, no internet needed.
struct URLSessionApiClient: ApiClient {

    static let shared = URLSessionApiClient()

    let session: URLSession
    let timeout: TimeInterval

    init(session: URLSession = .shared, timeout: TimeInterval = ApiHelpers.defaultTimeout) {
        self.session = session
        self.timeout = timeout
    }

    func get(_ url: String, params: [String: String], headers: [String: String]) async -> ApiResult {
        guard let full = ApiHelpers.buildURL(url, params: params) else {
            return .failure(message: "Invalid URL: \(url)", durationMs: 0)
        }
        return await send(method: "GET", url: full, body: nil, headers: headers)
    }

    func post(_ url: String, body: String, headers: [String: String]) async -> ApiResult {
        guard let full = ApiHelpers.buildURL(url, params: [:]) else {
            return .failure(message: "Invalid URL: \(url)", durationMs: 0)
        }
        let merged = ApiHelpers.jsonContentType.merging(headers) { _, new in new }
        return await send(method: "POST", url: full, body: Data(body.utf8), headers: merged)
    }

    private func send(method: String, url: URL, body: Data?, headers: [String: String]) async -> ApiResult {
        var request = URLRequest(url: url, timeoutInterval: timeout)
        request.httpMethod = method
        request.httpBody = body
        for (k, v) in ApiHelpers.headers(headers) {
            request.setValue(v, forHTTPHeaderField: k)
        }
        let start = Date()
        do {
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else {
                return .failure(message: "Not an HTTP response", durationMs: ApiHelpers.elapsedMs(since: start))
            }
            return ApiHelpers.result(from: http, data: data, durationMs: ApiHelpers.elapsedMs(since: start))
        } catch {
            return ApiHelpers.classify(error, timeout: timeout, durationMs: ApiHelpers.elapsedMs(since: start))
        }
    }
}
