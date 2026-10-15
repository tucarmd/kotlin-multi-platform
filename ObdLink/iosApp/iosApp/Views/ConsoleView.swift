import SwiftUI
import Shared

/// [App 5 · B6] Console tab: raw log (`010C` → `41 0C 1A F8`) and a field for AT / hex commands.
/// Twin of ConsoleScreen.kt; the same quick commands, the same log from the core.
struct ConsoleView: View {
    let model: ObdModel
    @State private var text = ""

    private let quickCommands = ["ATRV", "ATDP", "0100", "010C"]

    var body: some View {
        let state = model.state
        NavigationStack {
            VStack(spacing: 8) {
                ScrollViewReader { proxy in
                    List {
                        if state.console.isEmpty {
                            Text(L("console_empty")).foregroundStyle(.secondary)
                        }
                        // Index as id: the same line (e.g. "OK") can appear many times.
                        ForEach(Array(state.console.enumerated()), id: \.offset) { index, line in
                            Text(line.kind.format(line.text))
                                .font(.caption.monospaced())
                                .foregroundStyle(color(line.kind))
                                .id(index)
                        }
                    }
                    .listStyle(.plain)
                    .onChange(of: state.console.count) { _, count in
                        if count > 0 { proxy.scrollTo(count - 1, anchor: .bottom) }
                    }
                }
                HStack {
                    ForEach(quickCommands, id: \.self) { command in
                        Button(command) { model.sendConsole(command) }
                            .buttonStyle(.bordered)
                            .font(.caption.monospaced())
                    }
                }
                HStack {
                    TextField(L("console_hint"), text: $text)
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                        .textFieldStyle(.roundedBorder)
                        .onSubmit(send)
                    Button(L("console_send"), action: send).buttonStyle(.borderedProminent)
                }
                .padding(.horizontal)
            }
            .padding(.bottom, 8)
            .navigationTitle(L("tab_console"))
            .toolbar { Button(L("console_clear")) { model.clearConsole() } }
        }
    }

    private func send() {
        model.sendConsole(text)
        text = ""
    }

    private func color(_ kind: ConsoleKind) -> Color {
        switch kind {
        case .sent: .accentColor
        case .received: .primary
        case .noAnswer, .invalidInput, .clearBlocked, .notConnected: .red
        }
    }
}
