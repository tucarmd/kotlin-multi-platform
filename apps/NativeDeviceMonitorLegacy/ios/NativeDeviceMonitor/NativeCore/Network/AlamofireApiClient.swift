import Alamofire
import Foundation

/// Way 2 — 3rd-party LIBRARY: Alamofire (SPM, 5.x). Under the hood it is still `URLSession`, just wrapped.
///
/// ```
///  get/post ─► ApiHelpers.buildURL ─► session.request(url, method:, headers:, requestModifier:)
///    └─ await .serializingData().response          ← DataResponse: request · response · data · result
///         ├─ response != nil → ApiHelpers.result → .success (2xx) / .httpError (else)
///         └─ response == nil → AFError.underlyingError (URLError) → ApiHelpers.classify
/// ```
///
/// What differs from URLSessionApiClient — worth learning:
/// - No `do/try/catch`: Alamofire never throws here, the outcome sits in `response.result` (`.success` / `.failure(AFError)`).
/// - Every error is wrapped in `AFError` (e.g. `.sessionTaskFailed(error: URLError)`) → unwrap before `classify`.
/// - We do NOT call `.validate()`: with it, 500 becomes `AFError.responseValidationFailed` and we'd lose the
///   "server answered" vs "network failed" split. So read `response.response?.statusCode` ourselves, same as way 1.
/// - `serializingData()` FAILS on an empty body (httpbin `/status/500` sends 0 bytes → `.inputDataNilOrZeroLength`),
///   so we read `response.data` straight, never `result`, whenever an HTTP response exists.
/// - Query params: Alamofire has its own `URLEncoding` (`parameters:`) but it escapes slightly differently →
///   we pass the URL already built by `ApiHelpers.buildURL` so both backends send byte-identical URLs.
/// - Timeout: there's no `timeout:` argument → set it on the URLRequest via `requestModifier`.
/// - `Session` also adds its own default headers (User-Agent, Accept-Language, Accept-Encoding); ours override `Accept`.
///
/// `session` is injectable → tests build `Session(configuration:)` with the same fake `URLProtocol` as way 1.
struct AlamofireApiClient: ApiClient {

    static let shared = AlamofireApiClient()

    let session: Session
    let timeout: TimeInterval

    init(session: Session = .default, timeout: TimeInterval = ApiHelpers.defaultTimeout) {
        self.session = session
        self.timeout = timeout
    }

    func get(_ url: String, params: [String: String], headers: [String: String]) async -> ApiResult {
        guard let full = ApiHelpers.buildURL(url, params: params) else {
            return .failure(message: "Invalid URL: \(url)", durationMs: 0)
        }
        return await send(.get, url: full, body: nil, headers: headers)
    }

    func post(_ url: String, body: String, headers: [String: String]) async -> ApiResult {
        guard let full = ApiHelpers.buildURL(url, params: [:]) else {
            return .failure(message: "Invalid URL: \(url)", durationMs: 0)
        }
        let merged = ApiHelpers.jsonContentType.merging(headers) { _, new in new }
        return await send(.post, url: full, body: Data(body.utf8), headers: merged)
    }

    private func send(_ method: HTTPMethod, url: URL, body: Data?, headers: [String: String]) async -> ApiResult {
        let timeout = timeout
        let start = Date()
        let response = await session.request(url, method: method, headers: HTTPHeaders(ApiHelpers.headers(headers)),
                                             requestModifier: { request in
                                                 request.timeoutInterval = timeout
                                                 request.httpBody = body   // raw JSON string, no Alamofire encoder
                                             })
            .serializingData()
            .response
        let ms = ApiHelpers.elapsedMs(since: start)

        // The server answered (any status) → same rule as way 1, body may be empty.
        if let http = response.response {
            return ApiHelpers.result(from: http, data: response.data ?? Data(), durationMs: ms)
        }
        // No HTTP response at all → it's a network-level error.
        switch response.result {
        case let .failure(afError):
            return ApiHelpers.classify(afError.underlyingError ?? afError, timeout: timeout, durationMs: ms)
        case .success:
            return .failure(message: "Not an HTTP response", durationMs: ms)
        }
    }
}
