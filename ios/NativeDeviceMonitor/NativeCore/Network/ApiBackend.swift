import Foundation

/// Which way to connect — the segmented Picker on the API tab. ONE `switch` decides, nothing else in the app knows.
///
/// ```
///  ApiBackend.client
///   switch self
///    ├─ case .urlSession → URLSessionApiClient.shared   (own code · Apple URLSession)
///    └─ case .alamofire  → AlamofireApiClient.shared    (library · Alamofire)
/// ```
///
/// Adding a 3rd way (e.g. Moya) = 1 new case + 1 new file conforming to `ApiClient`; the compiler then
/// points at every `switch` that must handle it.
enum ApiBackend: String, CaseIterable, Identifiable {
    case urlSession
    case alamofire

    var id: String { rawValue }

    var label: String {
        switch self {
        case .urlSession: "URLSession"
        case .alamofire: "Alamofire"
        }
    }

    var client: any ApiClient {
        switch self {
        case .urlSession: URLSessionApiClient.shared
        case .alamofire: AlamofireApiClient.shared
        }
    }
}
