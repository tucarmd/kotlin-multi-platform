import SwiftUI
import Shared

/// [App 1 · B6] The whole screen. Draws `model.state` and fires actions; computes nothing itself.
/// Its Android twin is SplitScreen.kt, with the same sections in the same order.
/// [Bài 6] SwiftUI plays the role Compose plays on Android: describe the UI for the current state, redraw on change.
///
/// [Phụ lục P2] Every tappable / readable view carries an id from the shared Kotlin `UiIds`, the same strings Android
/// uses as testTag. XCUITest and the shared Maestro flow (.maestro/) find views by these ids.
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
                        .accessibilityIdentifier(UiIds.shared.BILL_INPUT)
                    if case .invalid(let invalid) = onEnum(of: state.result) {
                        Text(invalid.error.label).font(.footnote).foregroundStyle(.red)
                            .accessibilityIdentifier(UiIds.shared.BILL_ERROR)
                    }
                } header: {
                    Text(L("bill"))
                }

                Section(L("tip")) {
                    Picker(L("tip"), selection: Binding(get: { Int(input.tipPercent) }, set: model.onTipSelected)) {
                        ForEach(model.tipOptions, id: \.self) { Text("\($0)%").tag($0) }
                    }
                    .pickerStyle(.segmented)
                    // TRAP: a segmented Picker has no id per segment. Tests pick a tip by its text ("10%").
                }

                Section {
                    // TRAP: not a Stepper. Its − and + are one element to accessibility, so they cannot get the ids
                    // Android's two buttons have, and the shared Maestro flow could not tap "+" by id.
                    // The limits are enforced by TipCore, not here; `disabled` only mirrors them.
                    HStack {
                        Text(L("people"))
                        Spacer()
                        Button("−") { model.removePerson() }
                            .disabled(!state.canRemovePerson)
                            .accessibilityIdentifier(UiIds.shared.PEOPLE_MINUS)
                        Text("\(input.people)")
                            .font(.title3.monospacedDigit())
                            .frame(minWidth: 36)
                            .accessibilityIdentifier(UiIds.shared.PEOPLE_COUNT)
                        Button("+") { model.addPerson() }
                            .disabled(!state.canAddPerson)
                            .accessibilityIdentifier(UiIds.shared.PEOPLE_PLUS)
                    }
                    // Without this the whole Form row is one button and a tap on "+" may hit "−".
                    .buttonStyle(.bordered)
                    Toggle(L("round_up"), isOn: Binding(get: { input.roundUp }, set: { _ in model.onRoundUpToggled() }))
                        .accessibilityIdentifier(UiIds.shared.ROUND_UP)
                }

                Section {
                    resultView(state.result)
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle(L("app_name"))
            .toolbar {
                Button(L("reset")) { model.reset() }
                    .accessibilityIdentifier(UiIds.shared.RESET)
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
                .accessibilityIdentifier(UiIds.shared.RESULT_MESSAGE)
        case .invalid:
            Text(L("fix_input")).foregroundStyle(.red)
                .accessibilityIdentifier(UiIds.shared.RESULT_MESSAGE)
        case .ready(let ready):
            let split = ready.split
            VStack(alignment: .leading, spacing: 8) {
                Text(L("per_person")).font(.subheadline).foregroundStyle(.secondary)
                Text(money(split.perPerson)).font(.largeTitle.bold())
                    .accessibilityIdentifier(UiIds.shared.RESULT_PER_PERSON)
                amountRow(L("tip_amount"), split.tip, UiIds.shared.RESULT_TIP)
                amountRow(L("total"), split.total, UiIds.shared.RESULT_TOTAL)
                if split.extra > 0 {
                    Text(L("extra", formatMoney(amount: split.extra))).font(.footnote).foregroundStyle(.secondary)
                }
            }
        }
    }

    private func amountRow(_ label: String, _ amount: Int64, _ id: String) -> some View {
        HStack {
            Text(label)
            Spacer()
            Text(money(amount)).accessibilityIdentifier(id)
        }
    }
}
