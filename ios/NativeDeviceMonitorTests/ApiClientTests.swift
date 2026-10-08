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
@Suite(.serialized)
struct ApiClientTests {

    private let base = "https://stub.test"

    private func client() -> ApiClient {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [StubURLProtocol.self]
        return ApiClient(session: URLSession(configuration: config), timeout: 8)
    }

    @Test func get200IsSuccessAndSendsParamsAndDefaultHeaders() async {
        let r = await client().get("\(base)/ok", params: ["userId": "1"])
        guard case let .success(status, body, contentType, _) = r else { Issue.record("got \(r)"); return }
        #expect(status == 200)
        #expect(body == #"{"ok":true}"#)
        #expect(contentType == "application/json")
        #expect(StubURLProtocol.lastRequest?.url?.query == "userId=1")
        #expect(StubURLProtocol.lastRequest?.value(forHTTPHeaderField: "X-Client") == "NativeDeviceMonitor-iOS")
        #expect(StubURLProtocol.lastRequest?.httpMethod == "GET")
    }

    @Test func post201SendsJsonBodyWithContentType() async {
        let r = await client().post("\(base)/posts", body: #"{"title":"hi"}"#)
        guard case let .success(status, body, _, _) = r else { Issue.record("got \(r)"); return }
        #expect(status == 201)
        #expect(body.contains("101"))
        #expect(StubURLProtocol.lastRequest?.httpMethod == "POST")
        #expect(StubURLProtocol.lastRequest?.value(forHTTPHeaderField: "Content-Type")?.hasPrefix("application/json") == true)
        #expect(StubURLProtocol.lastBody.map { String(decoding: $0, as: UTF8.self) } == #"{"title":"hi"}"#)
    }

    @Test func status500IsHttpErrorWithBody() async {
        let r = await client().get("\(base)/fail")
        guard case let .httpError(status, body, _) = r else { Issue.record("got \(r)"); return }
        #expect(status == 500)
        #expect(body == #"{"error":"boom"}"#)
    }

    @Test func timedOutIsTimeout() async {
        let r = await client().get("\(base)/timeout")
        guard case let .timeout(seconds, _) = r else { Issue.record("got \(r)"); return }
        #expect(seconds == 8)
    }

    @Test func offlineIsNoNetwork() async {
        let r = await client().get("\(base)/offline")
        guard case .noNetwork = r else { Issue.record("got \(r)"); return }
    }

    @Test func malformedUrlIsFailureNotCrash() async {
        let r = await client().get("not a url")
        guard case .failure = r else { Issue.record("got \(r)"); return }
    }

    @Test func classifyMapsErrors() {
        #expect(ApiClient.classify(URLError(.timedOut), timeout: 8, durationMs: 1) == .timeout(seconds: 8, durationMs: 1))
        guard case .noNetwork = ApiClient.classify(URLError(.cannotFindHost), timeout: 8, durationMs: 1) else {
            Issue.record("cannotFindHost should be noNetwork"); return
        }
        guard case .failure = ApiClient.classify(URLError(.badServerResponse), timeout: 8, durationMs: 1) else {
            Issue.record("badServerResponse should be failure"); return
        }
    }

    @Test func buildURLSortsAndEncodes() {
        #expect(ApiClient.buildURL("https://a.b/p", params: [:])?.absoluteString == "https://a.b/p")
        #expect(ApiClient.buildURL("https://a.b/p", params: ["userId": "1", "q": "a b&c"])?.absoluteString
                == "https://a.b/p?q=a%20b%26c&userId=1")
        #expect(ApiClient.buildURL("https://a.b/p?x=1", params: ["y": "2"])?.absoluteString == "https://a.b/p?x=1&y=2")
    }

    @Test func prettyJSONIndentsJsonKeepsNonJson() {
        let pretty = ApiClient.prettyJSON(#"{"a":1,"url":"https://x.y/z"}"#)
        #expect(pretty.contains("\n  \"a\" : 1"))
        #expect(pretty.contains("https://x.y/z"))   // no "\/" left
        #expect(ApiClient.prettyJSON("<html>oops</html>") == "<html>oops</html>")
        #expect(ApiClient.prettyJSON("{broken") == "{broken")
    }
}
