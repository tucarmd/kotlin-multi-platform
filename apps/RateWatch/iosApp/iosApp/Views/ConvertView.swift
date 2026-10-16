import SwiftUI
import Shared

/// [App 3 · B6] Convert tab: amount, two currency pickers, swap, result. Computes nothing itself:
/// the result and its formatted text come from RatesCore.convert, so both phones print the same digits.
/// Its Android twin is ConvertScreen.kt.
struct ConvertView: View {
    let model: RatesModel

    var body: some View {
        let state = model.convert
        let input = state.input
        NavigationStack {
            Form {
                Section(L("amount")) {
                    // Binding(get:set:) because the value lives in Kotlin: reading comes from the state,
                    // writing goes through the core, which publishes a new state.
                    TextField("100", text: Binding(get: { input.amountText }, set: model.onAmountChanged))
                        // PLATFORM: .decimalPad shows the separator of the phone's region; parseAmount accepts both.
                        .keyboardType(.decimalPad)
                    if case .invalid(let invalid) = onEnum(of: state.result) {
                        Text(invalid.error.label).font(.footnote).foregroundStyle(.red)
                    }
                }

                Section {
                    currencyPicker(L("from"), input.from, state.currencies, model.onFromSelected)
                    Button {
                        model.swapCurrencies()
                    } label: {
                        Label(L("swap"), systemImage: "arrow.up.arrow.down")
                    }
                    currencyPicker(L("to"), input.to, state.currencies, model.onToSelected)
                }

                Section {
                    resultView(state)
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle(L("tab_convert"))
        }
    }

    private func currencyPicker(_ title: String, _ code: String, _ codes: [String], _ onPick: @escaping (String) -> Void) -> some View {
        Picker(title, selection: Binding(get: { code }, set: onPick)) {
            ForEach(codes, id: \.self) { Text($0).tag($0) }
        }
        .pickerStyle(.navigationLink)
    }

    /// One branch per ConvertResult case. SKIE's `onEnum(of:)` makes the Kotlin sealed class a Swift enum,
    /// so this `switch` stops compiling if a case is added in Kotlin later.
    @ViewBuilder
    private func resultView(_ state: ConvertState) -> some View {
        switch onEnum(of: state.result) {
        case .waiting:
            Text(L("convert_waiting")).foregroundStyle(.secondary)
        case .invalid(let invalid):
            Text(invalid.error.label).foregroundStyle(.red)
        case .noRates:
            Text(L("convert_no_rates")).foregroundStyle(.secondary)
        case .ready(let ready):
            VStack(alignment: .leading, spacing: 8) {
                Text("\(ready.amountText) \(state.input.to)").font(.largeTitle.bold())
                Text(L("rate_line", state.input.from, ready.rateText, state.input.to))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
    }
}
