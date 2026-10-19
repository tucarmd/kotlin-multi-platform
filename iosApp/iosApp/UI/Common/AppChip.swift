import SwiftUI
import Shared

/// Small coloured label (permission status, radio ON/OFF, RUNNING…). Mirrors Android `LabelChip` in PermissionScreen.kt.
struct AppChip: View {
    let text: String
    let color: Color

    var body: some View {
        Text(text)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, AppSpacing.sm)
            .padding(.vertical, AppSpacing.xs)
            .background(color)
            .clipShape(Capsule())
    }
}

/// "label ........ value" table (media info, device status). Rows come from the shared ViewModel as `InfoRow`.
struct InfoTable: View {
    let rows: [InfoRow]

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            ForEach(rows, id: \.label) { row in
                HStack(alignment: .firstTextBaseline) {
                    Text(row.label).font(.system(size: 13)).foregroundStyle(AppColor.textSecondary)
                    Spacer()
                    Text(row.value).font(.system(size: 13, design: .monospaced)).foregroundStyle(AppColor.textPrimary)
                        .multilineTextAlignment(.trailing)
                }
            }
        }
    }
}

/// Dark terminal box for raw lines (Received Data, monitor log). Mirrors Android `LogTerminal`.
struct TerminalBox: View {
    let lines: [String]
    var placeholder: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            if lines.isEmpty, let placeholder {
                Text(placeholder).font(.system(size: 13, design: .monospaced)).foregroundStyle(AppColor.terminalText)
            }
            ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                Text(line).font(.system(size: 11, design: .monospaced)).foregroundStyle(AppColor.terminalText)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(AppSpacing.sm)
        .background(AppColor.terminalBackground, in: RoundedRectangle(cornerRadius: 8))
        .textSelection(.enabled)
    }
}
