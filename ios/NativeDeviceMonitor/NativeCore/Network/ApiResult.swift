import Foundation

/// Day 10 · result of one request — the View only `switch`es over these 5 cases, never sees an `Error`.
/// Both backends (URLSession · Alamofire) return this SAME enum → the View can't tell which one ran.
/// Mirrors `android/.../nativecore/network/ApiClient.kt` (`sealed interface ApiResult`).
enum ApiResult: Equatable {
    case success(status: Int, body: String, contentType: String?, durationMs: Int)
    /// The server DID answer but ≠ 2xx (404, 500…) — very different from "couldn't reach the server".
    case httpError(status: Int, body: String, durationMs: Int)
    case timeout(seconds: Double, durationMs: Int)
    case noNetwork(message: String, durationMs: Int)
    case failure(message: String, durationMs: Int)
}
