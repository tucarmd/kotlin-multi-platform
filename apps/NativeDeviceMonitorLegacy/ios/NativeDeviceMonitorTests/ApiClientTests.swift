import Alamofire
import Foundation
import Testing
@testable import NativeDeviceMonitor

/// A fake `URLProtocol`: intercepts EVERY request of the test session and answers by path — no internet, no mock library.
/// The Android version spins up a real server on 127.0.0.1 (`ApiClientTest.kt`); same cases.
final class StubURLProtocol: URLProtocol {
    /// The last request received — tests read it back to check the headers/body the client sent.
    nonisolated(unsafe) static var lastRequest: URLRequest?
    nonisolated(unsafe) static var lastBody: Data?

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func stopLoading() {}

    override func startLoading() {
        Self.lastRequest = request
        // URLSession turns httpBody into a stream by the time it reaches URLProtocol → read it back from the stream
        Self.lastBody = request.httpBody ?? request.httpBodyStream.map(Self.read)
        let url = request.url!
        switch url.path {
        case "/timeout": client?.urlProtocol(self, didFailWithError: URLError(.timedOut)); return
        case "/offline": client?.urlProtocol(self, didFailWithError: URLError(.notConnectedToInternet)); return
        default: break
        }
        let (status, body): (Int, String) = switch url.path {
        case "/ok": (200, #"{"ok":true}"#)
        case "/posts": (201, #"{"id":101}"#)
        case "/fail": (500, #"{"error":"boom"}"#)
        case "/empty500": (500, "")   // like httpbin /status/500 — 0 bytes, trips Alamofire's serializingData()
        default: (404, "{}")
        }
        let response = HTTPURLResponse(url: url, statusCode: status, httpVersion: "HTTP/1.1",
                                       headerFields: ["Content-Type": "application/json"])!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(body.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    private static func read(_ stream: InputStream) -> Data {
        stream.open(); defer { stream.close() }
        var data = Data()
        var buffer = [UInt8](repeating: 0, count: 1024)
        while stream.hasBytesAvailable {
            let n = stream.read(&buffer, maxLength: buffer.count)
            if n <= 0 { break }
            data.append(buffer, count: n)
        }
        return data
    }
}

/// `.serialized`: the tests share `StubURLProtocol.lastRequest` → run one after another.
/// Every network test runs TWICE — `arguments: ApiBackend.allCases` — so both ways must behave identically.
@Suite(.serialized)
struct ApiClientTests {

    private let base = "https://stub.test"

    /// Same fake URLProtocol for both — Alamofire is URLSession underneath, so it goes through the stub too.
    private func client(_ backend: ApiBackend) -> any ApiClient {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [StubURLProtocol.self]
        switch backend {
        case .urlSession: return URLSessionApiClient(session: URLSession(configuration: config), timeout: 8)
        case .alamofire: return AlamofireApiClient(session: Session(configuration: config), timeout: 8)
        }
    }

    @Test(arguments: ApiBackend.allCases)
    func get200IsSuccessAndSendsParamsAndDefaultHeaders(_ backend: ApiBackend) async {
        let r = await client(backend).get("\(base)/ok", params: ["userId": "1"])
        guard case let .success(status, body, contentType, _) = r else { Issue.record("\(backend.label) got \(r)"); return }
        #expect(status == 200)
        #expect(body == #"{"ok":true}"#)
        #expect(contentType == "application/json")
        #expect(StubURLProtocol.lastRequest?.url?.query == "userId=1")
        #expect(StubURLProtocol.lastRequest?.value(forHTTPHeaderField: "X-Client") == "NativeDeviceMonitor-iOS")
        #expect(StubURLProtocol.lastRequest?.httpMethod == "GET")
    }

    @Test(arguments: ApiBackend.allCases)
    func post201SendsJsonBodyWithContentType(_ backend: ApiBackend) async {
        let r = await client(backend).post("\(base)/posts", body: #"{"title":"hi"}"#)
        guard case let .success(status, body, _, _) = r else { Issue.record("\(backend.label) got \(r)"); return }
        #expect(status == 201)
        #expect(body.contains("101"))
        #expect(StubURLProtocol.lastRequest?.httpMethod == "POST")
        #expect(StubURLProtocol.lastRequest?.value(forHTTPHeaderField: "Content-Type")?.hasPrefix("application/json") == true)
        #expect(StubURLProtocol.lastBody.map { String(decoding: $0, as: UTF8.self) } == #"{"title":"hi"}"#)
    }

    @Test(arguments: ApiBackend.allCases)
    func status500IsHttpErrorWithBody(_ backend: ApiBackend) async {
        let r = await client(backend).get("\(base)/fail")
        guard case let .httpError(status, body, _) = r else { Issue.record("\(backend.label) got \(r)"); return }
        #expect(status == 500)
        #expect(body == #"{"error":"boom"}"#)
    }

    @Test(arguments: ApiBackend.allCases)
    func emptyBody500IsStillHttpError(_ backend: ApiBackend) async {
        let r = await client(backend).get("\(base)/empty500")
        guard case let .httpError(status, body, _) = r else { Issue.record("\(backend.label) got \(r)"); return }
        #expect(status == 500)
        #expect(body.isEmpty)
    }

    @Test(arguments: ApiBackend.allCases)
    func timedOutIsTimeout(_ backend: ApiBackend) async {
        let r = await client(backend).get("\(base)/timeout")
        guard case let .timeout(seconds, _) = r else { Issue.record("\(backend.label) got \(r)"); return }
        #expect(seconds == 8)
    }

    @Test(arguments: ApiBackend.allCases)
    func offlineIsNoNetwork(_ backend: ApiBackend) async {
        let r = await client(backend).get("\(base)/offline")
        guard case .noNetwork = r else { Issue.record("\(backend.label) got \(r)"); return }
    }

    @Test(arguments: ApiBackend.allCases)
    func malformedUrlIsFailureNotCrash(_ backend: ApiBackend) async {
        let r = await client(backend).get("not a url")
        guard case .failure = r else { Issue.record("\(backend.label) got \(r)"); return }
    }

    @Test func backendSwitchPicksTheRightClient() {
        #expect(ApiBackend.urlSession.client is URLSessionApiClient)
        #expect(ApiBackend.alamofire.client is AlamofireApiClient)
    }

    @Test func alamofireWrappedURLErrorClassifiesLikeURLSession() {
        let wrapped = AFError.sessionTaskFailed(error: URLError(.timedOut))
        #expect(ApiHelpers.classify(wrapped.underlyingError ?? wrapped, timeout: 8, durationMs: 1)
                == .timeout(seconds: 8, durationMs: 1))
    }

    @Test func classifyMapsErrors() {
        #expect(ApiHelpers.classify(URLError(.timedOut), timeout: 8, durationMs: 1) == .timeout(seconds: 8, durationMs: 1))
        guard case .noNetwork = ApiHelpers.classify(URLError(.cannotFindHost), timeout: 8, durationMs: 1) else {
            Issue.record("cannotFindHost should be noNetwork"); return
        }
        guard case .failure = ApiHelpers.classify(URLError(.badServerResponse), timeout: 8, durationMs: 1) else {
            Issue.record("badServerResponse should be failure"); return
        }
    }

    @Test func buildURLSortsAndEncodes() {
        #expect(ApiHelpers.buildURL("https://a.b/p", params: [:])?.absoluteString == "https://a.b/p")
        #expect(ApiHelpers.buildURL("https://a.b/p", params: ["userId": "1", "q": "a b&c"])?.absoluteString
                == "https://a.b/p?q=a%20b%26c&userId=1")
        #expect(ApiHelpers.buildURL("https://a.b/p?x=1", params: ["y": "2"])?.absoluteString == "https://a.b/p?x=1&y=2")
    }

    @Test func prettyJSONIndentsJsonKeepsNonJson() {
        let pretty = ApiHelpers.prettyJSON(#"{"a":1,"url":"https://x.y/z"}"#)
        #expect(pretty.contains("\n  \"a\" : 1"))
        #expect(pretty.contains("https://x.y/z"))   // no "\/" left
        #expect(ApiHelpers.prettyJSON("<html>oops</html>") == "<html>oops</html>")
        #expect(ApiHelpers.prettyJSON("{broken") == "{broken")
    }
}
