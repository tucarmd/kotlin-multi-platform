import SwiftUI
import Shared

struct HistoryView: View {
    let model: PulseModel

    var body: some View {
        Group {
            if model.snapshots.isEmpty {
                Text(L("history_empty"))
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
                    .padding(32)
            } else {
                List(model.snapshots, id: \.id) { SnapshotRow(snapshot: $0) }
            }
        }
        .navigationTitle("🕘 " + L("history"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if !model.snapshots.isEmpty {
                Button(L("clear_history"), action: model.clearHistory)
            }
        }
    }
}

private struct SnapshotRow: View {
    let snapshot: Snapshot

    var body: some View {
        let s = snapshot
        VStack(alignment: .leading, spacing: 4) {
            Text(s.takenAt.clockText).fontWeight(.semibold)
            Text("🔋 " + (s.batteryPercent.map { "\($0.intValue)% · \(s.charging?.label ?? "")" } ?? "—"))
            Text("📍 " + location(s))
            Text("📶 " + network(s) + (s.latencyMs.map { " · \($0.int64Value) ms" } ?? ""))
        }
        .padding(.vertical, 4)
    }

    private func location(_ s: Snapshot) -> String {
        guard let lat = s.latitude?.doubleValue, let lon = s.longitude?.doubleValue else { return "—" }
        let accuracy = s.accuracyMeters.map { $0.doubleValue.fixed(0) } ?? "?"
        return "\(lat.fixed(5)), \(lon.fixed(5)) (± \(accuracy) m)"
    }

    private func network(_ s: Snapshot) -> String {
        guard let connection = s.connection else { return "—" }
        return s.online?.boolValue == false ? "\(connection.label) · \(L("offline"))" : connection.label
    }
}
