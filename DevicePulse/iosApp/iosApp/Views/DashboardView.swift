import SwiftUI
import Shared

struct DashboardView: View {
    let model: PulseModel
    let onGrantLocation: () -> Void
    let open: (Route) -> Void
    @State private var showSaved = false

    var body: some View {
        let state = model.state
        ScrollView {
            VStack(spacing: 12) {
                batteryCard(state)
                locationCard(state)
                networkCard(state)
            }
            .padding(16)
        }
        .navigationTitle(L("app_name"))
        .toolbar {
            Button("🕘 " + L("history")) { open(.history) }
        }
        .safeAreaInset(edge: .bottom) {
            Button {
                model.saveSnapshot()
                showSaved = true
            } label: {
                Text(L("save_snapshot")).frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .padding(16)
            .background(.bar)
        }
        .alert(L("saved"), isPresented: $showSaved) {
            Button("OK", role: .cancel) {}
        }
    }

    private func batteryCard(_ state: DashboardState) -> some View {
        let info = (state.battery as? ReadingAvailable<BatteryInfo>)?.value
        return SensorCard(sensor: .battery, grade: info.map { batteryLevel(info: $0).grade }, onTap: { open(.detail(.battery)) }) {
            ReadingView(reading: state.battery) { info in
                HStack(alignment: .lastTextBaseline, spacing: 12) {
                    Text("\(info.levelPercent)%").font(.largeTitle.bold())
                    Text(info.charging.label)
                }
            }
        }
    }

    private func locationCard(_ state: DashboardState) -> some View {
        let fix = (state.location as? ReadingAvailable<LocationFix>)?.value
        return SensorCard(sensor: .location, grade: fix.map { fixQuality(fix: $0).grade }, onTap: { open(.detail(.location)) }) {
            ReadingView(reading: state.location, onGrantPermission: onGrantLocation) { fix in
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(fix.latitude.fixed(5)), \(fix.longitude.fixed(5))").font(.title3)
                    Text("± \(fix.accuracyMeters.fixed(0)) m").foregroundStyle(.secondary)
                }
            }
        }
    }

    private func networkCard(_ state: DashboardState) -> some View {
        let ms = (state.latencyMs as? ReadingAvailable<KotlinLong>)?.value
        return SensorCard(sensor: .network, grade: ms.map { latencyGrade(millis: $0.int64Value).grade }, onTap: { open(.detail(.network)) }) {
            ReadingView(reading: state.network) { status in
                HStack(alignment: .lastTextBaseline, spacing: 12) {
                    Text(status.type.label).font(.title3)
                    Text(L(status.isOnline ? "online" : "offline"))
                        .foregroundStyle((status.isOnline ? Tone.good : Tone.bad).color)
                }
            }
            HStack {
                Text(latencyText(state.latencyMs))
                Spacer()
                Button(L("measure_latency"), action: model.measureLatency)
                    .buttonStyle(.bordered)
                    .disabled(isLoading(state.latencyMs))
            }
        }
    }
}
