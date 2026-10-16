import SwiftUI
import Shared

/// Draws the four non-value states of a Reading the same way everywhere; `content` handles Available.
/// SKIE's onEnum(of:) turns the Kotlin sealed class into a Swift enum, so the switch is exhaustive.
struct ReadingView<T: AnyObject, Content: View>: View {
    let reading: Reading<T>
    var onGrantPermission: (() -> Void)? = nil
    @ViewBuilder let content: (T) -> Content

    var body: some View {
        switch onEnum(of: reading) {
        case .loading:
            HStack(spacing: 8) {
                ProgressView()
                Text(L("state_loading")).font(.subheadline)
            }
        case .permissionDenied:
            VStack(alignment: .leading, spacing: 8) {
                Text(L("state_permission_denied")).foregroundStyle(.red)
                if let onGrantPermission {
                    Button(L("grant_permission"), action: onGrantPermission).buttonStyle(.bordered)
                }
            }
        case .unsupported:
            Text(L("state_unsupported")).foregroundStyle(.secondary)
        case .error(let error):
            Text(L("state_error", error.message)).foregroundStyle(.red)
        case .available(let available):
            content(available.value)
        }
    }
}

struct GradeChip: View {
    let grade: Grade

    var body: some View {
        Text(grade.label)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .foregroundStyle(grade.tone.color)
            .background(grade.tone.color.opacity(0.14), in: Capsule())
    }
}

struct SensorCard<Body: View>: View {
    let sensor: SensorKind
    let grade: Grade?
    let onTap: () -> Void
    @ViewBuilder let body_: () -> Body

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(sensor.emoji).font(.title2)
                Text(sensor.title).font(.headline)
                Spacer()
                if let grade { GradeChip(grade: grade) }
            }
            body_()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
    }
}

struct FieldRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label).foregroundStyle(.secondary)
            Spacer()
            Text(value).fontWeight(.medium)
        }
        .padding(.vertical, 6)
    }
}
