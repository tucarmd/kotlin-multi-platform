import SwiftUI
import Shared

/// [App 5 · B6] Connect tab: connection state, scan button, dongle list. Twin of ConnectScreen.kt.
///
/// PLATFORM: no permission row here. iOS shows its own Bluetooth prompt on the first scan (text from
/// NSBluetoothAlwaysUsageDescription); if the user refuses, the scan fails and the core reports BluetoothOff.
struct ConnectView: View {
    let model: ObdModel

    var body: some View {
        let state = model.state
        let connection = state.connection
        NavigationStack {
            List {
                Section {
                    Text(connection.label)
                        .font(.headline)
                        .foregroundStyle(isError(connection) ? .red : .primary)
                    if state.isConnected && !state.adapterVersion.isEmpty {
                        Text(L("adapter_version", state.adapterVersion)).font(.footnote).foregroundStyle(.secondary)
                    }
                    if isBusy(connection) { ProgressView() }
                    actionButton(connection)
                }

                Section(L("dongles_title")) {
                    if state.dongles.isEmpty {
                        Text(L("dongles_empty")).foregroundStyle(.secondary)
                    }
                    let canPick = isIdleOrScanning(connection)
                    ForEach(state.dongles, id: \.id) { dongle in
                        Button {
                            model.connect(dongle)
                        } label: {
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(dongle.displayName)
                                    // PLATFORM: a per-phone UUID on iOS, a MAC address on Android.
                                    Text(dongle.id).font(.caption2).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text(L("rssi", Int(dongle.rssi))).font(.footnote).foregroundStyle(.secondary)
                            }
                        }
                        .disabled(!canPick)
                    }
                }
            }
            .navigationTitle(L("app_name"))
        }
    }

    /// One branch per state: which button makes sense right now. Same choice as the Android Row.
    @ViewBuilder
    private func actionButton(_ connection: ConnectionState) -> some View {
        switch onEnum(of: connection) {
        case .idle:
            Button(L("scan")) { model.scan() }
        case .scanning:
            Button(L("stop_scan")) { model.stopScan() }
        case .error:
            Button(L("try_again")) { model.reset() }
        case .connecting, .initializing, .ready, .polling, .reconnecting:
            Button(L("disconnect"), role: .destructive) { model.disconnect() }
        }
    }

    private func isBusy(_ connection: ConnectionState) -> Bool {
        switch onEnum(of: connection) {
        case .scanning, .connecting, .initializing, .reconnecting: true
        case .idle, .ready, .polling, .error: false
        }
    }

    private func isError(_ connection: ConnectionState) -> Bool {
        if case .error = onEnum(of: connection) { return true }
        return false
    }

    private func isIdleOrScanning(_ connection: ConnectionState) -> Bool {
        switch onEnum(of: connection) {
        case .idle, .scanning: true
        default: false
        }
    }
}
