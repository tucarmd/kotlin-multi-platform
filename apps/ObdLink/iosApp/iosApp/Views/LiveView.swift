import SwiftUI
import Shared

/// [App 5 · B6] Live tab: three SwiftUI `Gauge`s (RPM, km/h, °C), refresh rate, start / pause.
/// Twin of LiveScreen.kt, where the gauges are drawn with Canvas.
struct LiveView: View {
    let model: ObdModel

    var body: some View {
        let state = model.state
        NavigationStack {
            Group {
                if state.isConnected || isReconnecting(state.connection) {
                    content(state)
                } else {
                    ConnectFirstView()
                }
            }
            .navigationTitle(L("tab_live"))
        }
    }

    private func content(_ state: ObdState) -> some View {
        let live = state.live
        return ScrollView {
            VStack(spacing: 20) {
                Text(state.connection.label).font(.subheadline).foregroundStyle(.secondary)
                HStack(spacing: 24) {
                    gauge(L("live_rpm"), L("unit_rpm"), live.rpm, 0...8000)
                    gauge(L("live_speed"), L("unit_kmh"), live.speedKmh, 0...240)
                    gauge(L("live_coolant"), L("unit_celsius"), live.coolantC, -40...140)
                }
                Text(L("live_refresh", String(format: "%.1f", live.refreshHz))).font(.footnote)
                switch onEnum(of: state.connection) {
                case .polling:
                    Button(L("live_pause")) { model.stopLive() }.buttonStyle(.bordered)
                case .ready:
                    Button(L("live_start")) { model.startLive() }.buttonStyle(.borderedProminent)
                default:
                    EmptyView() // Reconnecting: polling resumes by itself
                }
            }
            .padding()
        }
    }

    /// `reading == nil` (no answer yet / NO DATA) shows "—" and an empty gauge.
    private func gauge(_ title: String, _ unit: String, _ reading: Reading?, _ range: ClosedRange<Double>) -> some View {
        // TRAP: Gauge clamps by itself, but only if the value is a Double in the same type as the range.
        // Kotlin Int arrives as Int32: convert once here.
        let value = reading.map { Double($0.value) } ?? range.lowerBound
        return VStack {
            Gauge(value: value, in: range) {
                Text(unit)
            } currentValueLabel: {
                Text(reading.map { "\($0.value)" } ?? L("no_value"))
            }
            .gaugeStyle(.accessoryCircular)
            .scaleEffect(1.4)
            .padding(12)
            Text(title).font(.caption)
        }
    }

    private func isReconnecting(_ connection: ConnectionState) -> Bool {
        if case .reconnecting = onEnum(of: connection) { return true }
        return false
    }
}
