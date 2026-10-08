import SwiftUI

/// Day 10 — API tab: a "simple test interface" per the spec. Mirrors `android/.../ui/api/ApiScreen.kt`.
///
/// ```
///  ApiView
///   └─ ScrollView > VStack
///       ├─ AppCard "Test requests"  6 buttons = 6 rows of the test table (200 · params · POST 201 · headers · 500 · timeout)
///       ├─ AppCard "Custom GET"     TextField URL + [Send GET]
///       └─ AppCard "Response"       request · STATUS (green/red) · content-type · indented JSON body
/// ```
///
/// `Task { result = await client.get(...) }`: the View is `@MainActor` so assigning state is safe; the network part is
/// run by URLSession on its own threads → the UI doesn't freeze. While sending every button is disabled.
struct ApiView: View {

    private let client = ApiClient.shared
    private let bodyPreview = 3000
    private let resultAnchor = "result"

    @State private var customURL = "https://jsonplaceholder.typicode.com/users/1"
    @State private var lastRequest = ""
    @State private var result: ApiResult?
    @State private var sending = false

    var body: some View {
        // ScrollViewReader: the Response card is at the bottom → after tapping a button, scroll down to show the result.
        ScrollViewReader { proxy in
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.md) {
                AppHintText(text: ApiStrings.hint, showBullet: false)
                AppCard(title: ApiStrings.presetsTitle) {
                    ForEach(ApiStrings.presets) { preset in
                        AppSecondaryButton(title: preset.label, action: { send(preset) }, isEnabled: !sending)
                    }
                    AppHintText(text: ApiStrings.airplaneHint)
                }
                AppCard(title: ApiStrings.customTitle) {
                    TextField(ApiStrings.urlPlaceholder, text: $customURL)
                        .keyboardType(.URL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.system(size: 14, design: .monospaced))
                        .padding(AppSpacing.sm)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(AppColor.border))
                    AppPrimaryButton(title: ApiStrings.sendGet,
                                     action: { send(ApiPreset(label: "custom", method: "GET",
                                                              url: customURL.trimmingCharacters(in: .whitespaces))) },
                                     isEnabled: !customURL.isEmpty, isLoading: sending)
                }
                resultCard.id(resultAnchor)
            }
            .padding(AppSpacing.md)
        }
        // keyed on `sending` (not the URL) → tapping the same button again still scrolls
        .onChange(of: sending) { _, isSending in
            if isSending { withAnimation { proxy.scrollTo(resultAnchor, anchor: .top) } }
        }
        }
        .scrollDismissesKeyboard(.interactively)
        .background(AppColor.contentBackground)
        .navigationTitle(ApiStrings.title)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var resultCard: some View {
        AppCard(title: ApiStrings.resultTitle) {
            if !lastRequest.isEmpty {
                Text(lastRequest).font(.system(size: 12, design: .monospaced)).foregroundStyle(AppColor.textPrimary)
            }
            if sending {
                AppHintText(text: ApiStrings.sending)
            } else if let result {
                let line = ApiStrings.statusLine(result)
                Text(line.text)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(line.ok ? AppColor.success : AppColor.danger)
                if case let .success(_, _, contentType?, _) = result {
                    Text("content-type: \(contentType)").font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
                }
                let body = responseBody(result)
                if !body.isEmpty {
                    let pretty = ApiClient.prettyJSON(body)
                    Text(pretty.count > bodyPreview ? String(pretty.prefix(bodyPreview)) + "\n… (\(pretty.count) chars)" : pretty)
                        .font(.system(size: 11, design: .monospaced))
                        .foregroundStyle(AppColor.terminalText)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(AppSpacing.sm)
                        .background(AppColor.terminalBackground, in: RoundedRectangle(cornerRadius: 8))
                        .textSelection(.enabled)
                }
            } else {
                AppHintText(text: ApiStrings.nothingYet)
            }
        }
    }

    private func responseBody(_ r: ApiResult) -> String {
        switch r {
        case let .success(_, body, _, _), let .httpError(_, body, _): return body
        default: return ""
        }
    }

    private func send(_ preset: ApiPreset) {
        sending = true
        result = nil
        let shown = ApiClient.buildURL(preset.url, params: preset.params)?.absoluteString ?? preset.url
        lastRequest = "\(preset.method) \(shown)"
        Task {
            if preset.method == "POST" {
                result = await client.post(preset.url, body: preset.body ?? "")
            } else {
                result = await client.get(preset.url, params: preset.params)
            }
            sending = false
        }
    }
}

#Preview {
    NavigationStack { ApiView() }
}
