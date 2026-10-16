import SwiftUI
import Shared

struct DetailView: View {
    let sensor: SensorKind
    let model: PulseModel
    let onGrantLocation: () -> Void

    var body: some View {
        let state = model.state
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                switch sensor {
                case .battery:
                    ReadingView(reading: state.battery) { info in
                        VStack(spacing: 0) {
                            FieldRow(label: L("field_level"), value: "\(info.levelPercent)%")
                            FieldRow(label: L("field_status"), value: info.charging.label)
                            FieldRow(label: L("field_rating"), value: batteryLevel(info: info).grade.label)
                        }
                    }
                case .location:
                    ReadingView(reading: state.location, onGrantPermission: onGrantLocation) { fix in
                        VStack(spacing: 0) {
                            FieldRow(label: L("field_latitude"), value: fix.latitude.fixed(6))
                            FieldRow(label: L("field_longitude"), value: fix.longitude.fixed(6))
                            FieldRow(label: L("field_accuracy"), value: "\(fix.accuracyMeters.fixed(1)) m")
                            FieldRow(label: L("field_speed"), value: fix.speedMps.map { "\(($0.doubleValue * 3.6).fixed(1)) km/h" } ?? "—")
                            FieldRow(label: L("field_updated"), value: fix.timestamp.clockText)
                            FieldRow(label: L("field_rating"), value: fixQuality(fix: fix).grade.label)
                        }
                    }
                case .network:
                    ReadingView(reading: state.network) { status in
                        VStack(spacing: 0) {
                            FieldRow(label: L("field_type"), value: status.type.label)
                            FieldRow(label: L("field_status"), value: L(status.isOnline ? "online" : "offline"))
                        }
                    }
                    FieldRow(label: L("field_latency"), value: latencyText(state.latencyMs))
                    if let ms = (state.latencyMs as? ReadingAvailable<KotlinLong>)?.value {
                        FieldRow(label: L("field_rating"), value: latencyGrade(millis: ms.int64Value).grade.label)
                    }
                    Button(L("measure_latency"), action: model.measureLatency)
                        .buttonStyle(.borderedProminent)
                        .disabled(isLoading(state.latencyMs))
                        .padding(.top, 12)
                }
                Divider().padding(.vertical, 16)
                Text(sensor.rule).font(.footnote).foregroundStyle(.secondary)
            }
            .padding(16)
        }
        .navigationTitle("\(sensor.emoji)  \(sensor.title)")
        .navigationBarTitleDisplayMode(.inline)
    }
}
