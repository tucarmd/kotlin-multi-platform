import Foundation

/// Pure rules shared by BOTH backends — no network, so unit tests call them directly.
/// Keeping them in one place is what makes URLSession and Alamofire give identical results.
enum ApiHelpers {

    /// Sent with every request — the "Headers" in the spec.
    static let defaultHeaders = ["Accept": "application/json", "X-Client": "NativeDeviceMonitor-iOS"]
    static let jsonContentType = ["Content-Type": "application/json; charset=utf-8"]

    /// 8 s — under 10 s so the `httpbin.org/delay/10` preset reliably ends in Timeout. URLSession's default is 60 s.
    static let defaultTimeout: TimeInterval = 8

    /// `defaultHeaders` + extra, the caller's value wins on a duplicate key.
    static func headers(_ extra: [String: String]) -> [String: String] {
        defaultHeaders.merging(extra) { _, new in new }
    }

    /// `Error` → one of 3 branches. Alamofire callers unwrap `AFError.underlyingError` first, then land here too.
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

    /// The server answered → 2xx is `.success`, anything else is `.httpError`. Same rule for both backends.
    static func result(from http: HTTPURLResponse, data: Data, durationMs: Int) -> ApiResult {
        let text = String(decoding: data, as: UTF8.self)
        switch http.statusCode {
        case 200...299:
            return .success(status: http.statusCode, body: text,
                            contentType: http.value(forHTTPHeaderField: "Content-Type"), durationMs: durationMs)
        default:
            return .httpError(status: http.statusCode, body: text, durationMs: durationMs)
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

    static func elapsedMs(since start: Date) -> Int { Int(Date().timeIntervalSince(start) * 1000) }
}
