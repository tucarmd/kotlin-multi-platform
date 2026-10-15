import SwiftUI
import Shared

/// [App 1 · B6] The whole screen. Draws `model.state` and fires actions; computes nothing itself.
/// Its Android twin is SplitScreen.kt, with the same sections in the same order.
struct SplitView: View {
    let model: SplitModel

    var body: some View {
        let state = model.state
        let input = state.input
        NavigationStack {
            Form {
                Section {
                    // Binding(get:set:) because the value lives in Kotlin: reading comes from the state,
                    // writing goes through the core, which publishes a new state.
                    TextField("120.000", text: Binding(get: { input.billText }, set: model.onBillChanged))
                        // PLATFORM: .numberPad has no "Done" key; tapping outside the field dismisses it (see below).
                        .keyboardType(.numberPad)
                    if case .invalid(let invalid) = onEnum(of: state.result) {
                        Text(invalid.error.label).font(.footnote).foregroundStyle(.red)
                    }
                } header: {
                    Text(L("bill"))
                }

                Section(L("tip")) {
                    Picker(L("tip"), selection: Binding(get: { Int(input.tipPercent) }, set: model.onTipSelected)) {
                        ForEach(model.tipOptions, id: \.self) { Text("\($0)%").tag($0) }
                    }
                    .pickerStyle(.segmented)
                }

                Section {
                    // Stepper with explicit increment / decrement: the limits are enforced by TipCore, not here.
                    Stepper {
                        Text("\(L("people")): \(input.people)")
                    } onIncrement: {
                        model.addPerson()
                    } onDecrement: {
                        model.removePerson()
                    }
                    Toggle(L("round_up"), isOn: Binding(get: { input.roundUp }, set: { _ in model.onRoundUpToggled() }))
                }

                Section {
                    resultView(state.result)
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle(L("app_name"))
            .toolbar {
                Button(L("reset")) { model.reset() }
            }
        }
    }

    /// One branch per SplitResult case. SKIE's `onEnum(of:)` makes the Kotlin sealed class a Swift enum,
    /// so this `switch` stops compiling if a case is added in Kotlin later.
    @ViewBuilder
    private func resultView(_ result: SplitResult) -> some View {
        switch onEnum(of: result) {
        case .waiting:
            Text(L("waiting")).foregroundStyle(.secondary)
        case .invalid:
            Text(L("fix_input")).foregroundStyle(.red)
        case .ready(let ready):
            let split = ready.split
            VStack(alignment: .leading, spacing: 8) {
                Text(L("per_person")).font(.subheadline).foregroundStyle(.secondary)
                Text(money(split.perPerson)).font(.largeTitle.bold())
                amountRow(L("tip_amount"), split.tip)
                amountRow(L("total"), split.total)
                if split.extra > 0 {
                    Text(L("extra", formatMoney(amount: split.extra))).font(.footnote).foregroundStyle(.secondary)
                }
            }
        }
    }

    private func amountRow(_ label: String, _ amount: Int64) -> some View {
        HStack {
            Text(label)
            Spacer()
            Text(money(amount))
        }
    }
}
