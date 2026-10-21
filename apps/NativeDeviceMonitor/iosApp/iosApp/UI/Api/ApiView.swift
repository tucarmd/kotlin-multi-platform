import SwiftUI
import Shared

/// Day 10 — API tab in SwiftUI. Draws the shared `ApiViewModel.state`; every tap calls the ViewModel.
/// Mirrors Android `ui/api/ApiScreen.kt` — same ViewModel, same strings, only the drawing differs.
///
/// ```
///  ApiView
///   └─ ScrollView > VStack
///       ├─ AppCard "Test requests"  6 buttons = 6 rows of the test table (200 · params · POST 201 · headers · 500 · timeout)
///       ├─ AppCard "Custom GET"     TextField URL + [Send GET]
///       └─ AppCard "Response"       request · STATUS (green/red) · content-type · indented JSON body
/// ```
struct ApiView: View {
    private let vm: ApiViewModel
    @StateObject private var api: Observed<ApiUiState>
    private let resultAnchor = "result"

    init(vm: ApiViewModel) {
        self.vm = vm
        _api = StateObject(wrappedValue: Observed(vm.state))
    }

    var body: some View {
        let state = api.value
        // ScrollViewReader: the Response card is at the bottom → after tapping a button, scroll down to show the result.
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: AppSpacing.md) {
                    AppHintText(text: ApiStrings.shared.HINT, showBullet: false)
                    AppCard(title: ApiStrings.shared.PRESETS_TITLE) {
                        ForEach(state.presets, id: \.label) { preset in
                            AppSecondaryButton(title: preset.label, action: { vm.send(preset: preset) }, isEnabled: !state.sending)
                        }
                        AppHintText(text: ApiStrings.shared.AIRPLANE_HINT)
                    }
                    AppCard(title: ApiStrings.shared.CUSTOM_TITLE) {
                        // The text lives in the ViewModel → a Binding that reads state and writes through the action.
                        TextField(ApiStrings.shared.URL_LABEL, text: Binding(get: { state.customUrl }, set: { vm.onCustomUrlChange(url: $0) }))
                            .keyboardType(.URL)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .font(.system(size: 14, design: .monospaced))
                            .padding(AppSpacing.sm)
                            .overlay(RoundedRectangle(cornerRadius: AppRadius.field).stroke(AppColor.border))
                        AppPrimaryButton(title: ApiStrings.shared.SEND_GET, action: { vm.sendCustom() },
                                         isEnabled: state.canSendCustom, isLoading: state.sending)
                    }
                    resultCard(state).id(resultAnchor)
                }
                .padding(AppSpacing.md)
            }
            // keyed on `sending` → tapping the same button again still scrolls
            .onChange(of: state.sending) { _, isSending in
                if isSending { withAnimation { proxy.scrollTo(resultAnchor, anchor: .top) } }
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .background(AppColor.contentBackground)
    }

    private func resultCard(_ state: ApiUiState) -> some View {
        AppCard(title: ApiStrings.shared.RESULT_TITLE) {
            if !state.lastRequest.isEmpty {
                Text(state.lastRequest).font(.system(size: 12, design: .monospaced)).foregroundStyle(AppColor.textPrimary)
            }
            if state.sending {
                AppHintText(text: ApiStrings.shared.SENDING)
            } else if let r = state.result {
                Text(r.statusLine)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(r.ok ? AppColor.success : AppColor.danger)
                if let contentType = r.contentType {
                    Text(contentType).font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
                }
                if let body = r.body {
                    Text(body)
                        .font(.system(size: 11, design: .monospaced))
                        .foregroundStyle(AppColor.terminalText)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(AppSpacing.sm)
                        .background(AppColor.terminalBackground, in: RoundedRectangle(cornerRadius: 8))
                        .textSelection(.enabled)
                }
            } else {
                AppHintText(text: ApiStrings.shared.NOTHING_YET)
            }
        }
    }
}
