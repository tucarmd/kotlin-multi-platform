# TASK — API client: two ways to connect (own URLSession code vs Alamofire)

> Platforms: iOS only · Created: 2026-10-12 · Builds on the Day 10 API tab

## 1. Requirement

- Keep the existing API features as they are: GET with params · POST JSON · default headers · HTTP error · timeout (8 s) · no network.
- Split "how to connect" into **two interchangeable backends** behind one protocol:
  - **Way 1, own code:** `URLSessionApiClient` (Apple framework, no dependency).
  - **Way 2, library:** `AlamofireApiClient` (Alamofire 5 via SPM).
- Add a **segmented Picker** on the API tab so you can run the same 6 presets through either backend and compare.
- Branch with `switch` so the code is easy to read.
- Out of scope: typed `Codable` models, interceptors/retry, and Android.

## 2. How the API tab runs: one tap, start to finish

```
 ApiView (SwiftUI, @MainActor)
  │ @State backend: ApiBackend = .urlSession      ← the Picker writes this
  │
  │ tap "GET /status/500"
  ▼
 send(preset)
  ├─ sending = true · lastRequest = "[Alamofire] GET https://…"
  ├─ let client = backend.client ───────────────────────────── SWITCH 1: which way to connect
  │     ApiBackend.client
  │      switch self
  │       ├─ case .urlSession → URLSessionApiClient.shared
  │       └─ case .alamofire  → AlamofireApiClient.shared
  │
  └─ Task {
       switch preset.method ────────────────────────────────── SWITCH 2: which request
        ├─ case "POST" → await client.post(url, body:)
        └─ default     → await client.get(url, params:)
       sending = false
     }

 ── inside the chosen backend (runs off the main thread) ─────────────────────────────

 URLSessionApiClient                          AlamofireApiClient
  ApiHelpers.buildURL(url, params)             ApiHelpers.buildURL(url, params)      ← identical URL
  URLRequest + headers + timeout               session.request(url, method:, headers:,
  try await session.data(for:)                   requestModifier: timeout + body)
                                               await .serializingData().response
     │ got HTTPURLResponse?                       │ response.response != nil?
     ├─ yes → ApiHelpers.result(from:data:)       ├─ yes → ApiHelpers.result(from:data:)
     │         switch statusCode                  │         (read response.data — empty body is OK)
     │          ├─ 200...299 → .success           │
     │          └─ default   → .httpError         │
     └─ throws URLError                           └─ no → switch response.result
          → ApiHelpers.classify                          └─ .failure(AFError) → unwrap underlyingError
              switch e.code                                   → ApiHelpers.classify (same rule)
               ├─ .timedOut → .timeout
               ├─ offline/DNS/host → .noNetwork
               └─ default → .failure

 ── back in ApiView ──────────────────────────────────────────────────────────────────
  result = ApiResult  →  ApiStrings.statusLine(result)  switch over 5 cases → green/red line + pretty JSON
```

The View only ever sees `ApiResult`. It never sees an `Error`, a `URLSession`, or Alamofire.

## 3. Files

| File | Role |
|---|---|
| [ApiClient.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Network/ApiClient.swift) | `protocol ApiClient { get · post }` plus default arguments |
| [ApiResult.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Network/ApiResult.swift) | the 5-case result enum (moved here unchanged) |
| [ApiHelpers.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Network/ApiHelpers.swift) | pure shared rules: `buildURL` · `classify` · `result(from:)` · `prettyJSON` · default headers and timeout |
| [URLSessionApiClient.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Network/URLSessionApiClient.swift) | way 1, own code |
| [AlamofireApiClient.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Network/AlamofireApiClient.swift) | way 2, library |
| [ApiBackend.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/NativeCore/Network/ApiBackend.swift) | `enum { urlSession, alamofire }`; the one `switch` that picks the client |
| [ApiView.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/UI/Api/ApiView.swift) · [ApiStrings.swift](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/NativeDeviceMonitor/UI/Api/ApiStrings.swift) | Picker plus `switch preset.method` |
| [project.yml](https://github.com/tucarmd/kotlin-multi-platform/blob/main/apps/NativeDeviceMonitorLegacy/ios/project.yml) | `packages: Alamofire from 5.10.0`, resolved to 5.12.2 (`Package.resolved` committed) |

## 4. URLSession vs Alamofire: what actually differs

| Topic | URLSession (way 1) | Alamofire (way 2) |
|---|---|---|
| Dependency | none | SPM package |
| Error style | `do / try / catch`, throws `URLError` | never throws here; `response.result` = `.failure(AFError)` that wraps the `URLError` |
| HTTP 500 | not an error, so check `statusCode` yourself | same, as long as you **don't** call `.validate()`; `.validate()` turns 500 into `AFError.responseValidationFailed` |
| Empty body (`httpbin /status/500` = 0 bytes) | `data` is just empty | `serializingData()` **fails** with `.inputDataNilOrZeroLength`, so read `response.data` directly |
| Query params | built by hand | has `URLEncoding`, but we pass the URL from `buildURL` so both send identical bytes |
| Timeout | `URLRequest(timeoutInterval:)` | `requestModifier { $0.timeoutInterval = … }` |
| Default headers | only ours | `Session` adds User-Agent / Accept-Language / Accept-Encoding; check with "GET headers echo" |
| Testing | fake `URLProtocol` | the **same** fake `URLProtocol` via `Session(configuration:)` |

## 5. Tests and verification (2026-10-12)

- `ApiClientTests`: 12 tests pass. 7 of them run on **both** backends (`@Test(arguments: ApiBackend.allCases)`): 200 + params + headers · POST 201 · 500 · **empty-body 500** · timeout · offline · bad URL. Plus the switch picks the right client, an `AFError`-wrapped timeout classifies like URLSession, and the pure helper tests.
- Real app on the iPhone 17 Pro Max Simulator with Alamofire selected: `GET /status/500` gives `HTTP ERROR · 500`, and `GET headers echo` gives `SUCCESS · HTTP 200`.

Run the tests:

```bash
cd native-device-monitor/ios && xcodebuild test -project NativeDeviceMonitor.xcodeproj -scheme NativeDeviceMonitor -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' -only-testing:NativeDeviceMonitorTests/ApiClientTests
```

## 6. Adding a 3rd way later

1. Add a new file `XxxApiClient: ApiClient`.
2. Add a new `case xxx` in `ApiBackend`. The compiler then flags every `switch` that must handle it (`label`, `client`, `backendHint`, the test `client(_:)`).
3. The tests run on it automatically through `ApiBackend.allCases`.
