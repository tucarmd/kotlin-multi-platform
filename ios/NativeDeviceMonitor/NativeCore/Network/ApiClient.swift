import Foundation

/// Day 10 — the contract every way of "calling an API" must follow: `get(_:params:headers:)` · `post(_:body:headers:)`.
///
/// ```
///  ApiView ──► ApiBackend (Picker)  ── switch ──┬─ .urlSession → URLSessionApiClient   (own code, Apple framework)
///                                               └─ .alamofire  → AlamofireApiClient    (3rd-party library)
///                     both return ─► ApiResult (5 cases)  ◄── the View only switches over this
///  shared, no network: ApiHelpers.buildURL · classify · prettyJSON
/// ```
///
/// The View holds `any ApiClient` → swapping the library never touches UI code.
protocol ApiClient {
    func get(_ url: String, params: [String: String], headers: [String: String]) async -> ApiResult
    /// `body` is a JSON string — the backend adds `Content-Type: application/json` automatically.
    func post(_ url: String, body: String, headers: [String: String]) async -> ApiResult
}

/// Default arguments live here — a protocol requirement itself can't declare `= [:]`.
extension ApiClient {
    func get(_ url: String, params: [String: String] = [:]) async -> ApiResult {
        await get(url, params: params, headers: [:])
    }

    func post(_ url: String, body: String) async -> ApiResult {
        await post(url, body: body, headers: [:])
    }
}
