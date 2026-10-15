import SwiftUI
import Shared

/// [App 5 · B6] Codes tab: read / clear buttons, code list, clear confirmation. Twin of CodesScreen.kt.
struct CodesView: View {
    let model: ObdModel

    var body: some View {
        let state = model.state
        NavigationStack {
            Group {
                if state.isConnected { content(state.codes) } else { ConnectFirstView() }
            }
            .navigationTitle(L("tab_codes"))
        }
    }

    private func content(_ codes: CodesState) -> some View {
        let busy: Bool = {
            switch onEnum(of: codes) {
            case .reading, .clearing: true
            default: false
            }
        }()
        return List {
            Section {
                HStack {
                    Button(L("codes_read")) { model.readCodes() }.buttonStyle(.borderedProminent)
                    Button(L("codes_clear"), role: .destructive) { model.requestClearCodes() }.buttonStyle(.bordered)
                }
                .disabled(busy)
            }
            Section { codesBody(codes) }
        }
        // TRAP: mode 04 also resets the readiness monitors. The alert explains it; never clear on one tap.
        // The alert is driven by the core state (ConfirmClear), so Android and iOS cannot disagree.
        .alert(L("clear_title"), isPresented: Binding(get: { isConfirming(codes) }, set: { shown in
            if !shown && isConfirming(model.state.codes) { model.cancelClearCodes() }
        })) {
            Button(L("clear_confirm"), role: .destructive) { model.confirmClearCodes() }
            Button(L("cancel"), role: .cancel) { model.cancelClearCodes() }
        } message: {
            Text(L("clear_message"))
        }
    }

    /// One branch per CodesState case; the `switch` stops compiling if Kotlin adds a case.
    @ViewBuilder
    private func codesBody(_ codes: CodesState) -> some View {
        switch onEnum(of: codes) {
        case .notRead:
            Text(L("codes_not_read")).foregroundStyle(.secondary)
        case .reading:
            ProgressView(L("codes_reading"))
        case .clearing:
            ProgressView(L("codes_clearing"))
        case .failed(let failed):
            Text(failed.reason.label).foregroundStyle(.red)
        case .loaded(let loaded):
            codeRows(loaded.codes)
        case .confirmClear(let confirm):
            codeRows(confirm.codes)
        }
    }

    @ViewBuilder
    private func codeRows(_ codes: [Dtc]) -> some View {
        if codes.isEmpty {
            Text(L("codes_none"))
        } else {
            ForEach(codes, id: \.code) { dtc in
                HStack {
                    VStack(alignment: .leading) {
                        Text(dtc.code).font(.body.monospaced().bold())
                        Text(dtc.summary ?? L("codes_unknown")).font(.footnote).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Text(dtc.system.label).font(.caption2)
                }
            }
        }
    }

    private func isConfirming(_ codes: CodesState) -> Bool {
        if case .confirmClear = onEnum(of: codes) { return true }
        return false
    }
}
