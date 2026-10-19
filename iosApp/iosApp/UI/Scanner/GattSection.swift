import SwiftUI
import Shared

/// Day 3 + Day 4 + task 004 — the GATT block and the Write box. Targets, write plan, labels: all computed in the
/// shared `ScannerViewModel`; this file only draws `ScannerUiState`. Mirrors Android `ui/scanner/GattSection.kt`.
///
/// ```
///  GATT                                   3 services · 9 characteristics
///  Read target / Notify target / Write target / MTU
///  ┌ 180F Battery ─────────────────────────────────┐
///  │   2A19 Battery Level  ★ target                │
///  │   READ NOTIFY        [Read] [Notify] [Write]  │
///  └───────────────────────────────────────────────┘
///  Write  To FFE1 UART data · with response
///  [ ATZ\r ]   [Text][Hex]  [ATZ\r] [ATI\r] [010C\r]
///  → 41 54 5A 0D (4 bytes)                  [Send]
/// ```
struct GattSection: View {
    let state: ScannerUiState
    let vm: ScannerViewModel
    let hideKeyboard: () -> Void

    private var strings: ScannerStrings { ScannerStrings.shared }

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            HStack {
                SectionLabel(text: strings.GATT_TITLE)
                Spacer()
                if let count = state.gattCountText {
                    Text(count).font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
                }
            }
            if let message = state.gattMessage {
                HStack(spacing: AppSpacing.sm) {
                    if state.isDiscovering { ProgressView().controlSize(.small) }
                    hint(message)
                }
            }
            ForEach(state.targetLines, id: \.self) { hint($0) }
            if !state.services.isEmpty {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(state.services.enumerated()), id: \.offset) { index, service in
                        if index > 0 { Divider().overlay(AppColor.divider) }
                        serviceBlock(service)
                    }
                }
                .padding(.vertical, AppSpacing.xs)
                .overlay(RoundedRectangle(cornerRadius: AppRadius.field).stroke(AppColor.divider))
            }
            if let write = state.write { writeBlock(write) }

            if let title = state.lastReadLines.first {
                Text(title).font(.system(size: 13)).foregroundStyle(AppColor.textPrimary).padding(.top, AppSpacing.xs)
                ForEach(state.lastReadLines.dropFirst(), id: \.self) { line in
                    Text(line).font(.system(size: 12, design: .monospaced)).foregroundStyle(AppColor.textSecondary)
                        .padding(.leading, AppSpacing.sm)
                }
            }
            // Read / subscribe errors don't drop the connection → red here, not over the Status
            if let error = state.gattError {
                Text(error).font(.system(size: 12)).foregroundStyle(AppColor.danger)
            }
        }
    }

    private func serviceBlock(_ service: GattServiceUi) -> some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            Text(service.title).font(.system(size: 13, weight: .semibold)).foregroundStyle(AppColor.primary)
            ForEach(service.characteristics, id: \.info.key) { c in
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: AppSpacing.xs) {
                        Text(c.label).font(.system(size: 12)).foregroundStyle(AppColor.textPrimary)
                        if c.isTarget {
                            Text(strings.TARGET_TAG).font(.system(size: 10)).foregroundStyle(AppColor.warning)
                        }
                    }
                    HStack(spacing: AppSpacing.xs) {
                        Text(c.propertiesText).font(.system(size: 10, design: .monospaced)).foregroundStyle(AppColor.textSecondary)
                        Spacer()
                        if c.canRead { SmallButton(title: strings.READ_BUTTON, active: false) { vm.read(c: c.info) } }
                        if c.canNotify {
                            SmallButton(title: c.notifyButtonText, active: c.isNotifying) { vm.setNotify(c: c.info, enabled: !c.isNotifying) }
                        }
                        if c.canWrite { SmallButton(title: strings.WRITE_BUTTON, active: c.isWriteTarget) { vm.selectWriteTarget(c: c.info) } }
                    }
                }
                .padding(.leading, AppSpacing.sm)
            }
        }
        .padding(.horizontal, AppSpacing.md)
        .padding(.vertical, AppSpacing.xs)
    }

    /// Task 004 — command box + Send. The rules (writable? valid hex? longer than MTU?) live in shared `planWrite()`.
    private func writeBlock(_ write: WriteUi) -> some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            HStack(spacing: AppSpacing.sm) {
                Text(strings.WRITE_TITLE).font(.system(size: 13, weight: .semibold)).foregroundStyle(AppColor.textPrimary)
                hint(write.toText)
            }
            TextField(write.placeholder, text: Binding(get: { write.input }, set: { vm.onWriteInputChange(text: $0) }))
                .font(.system(size: 14, design: .monospaced))
                // AT commands are UPPERCASE; autocorrect off ("ATZ" → "Atz" and the dongle won't understand)
                .textInputAutocapitalization(.characters)
                .autocorrectionDisabled()
                .padding(AppSpacing.sm)
                .overlay(RoundedRectangle(cornerRadius: AppRadius.field).stroke(AppColor.border))
            HStack(spacing: AppSpacing.xs) {
                SmallButton(title: strings.MODE_TEXT, active: !write.asHex) { vm.setWriteHex(hex: false) }
                SmallButton(title: strings.MODE_HEX, active: write.asHex) { vm.setWriteHex(hex: true) }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: AppSpacing.xs) {
                        ForEach(strings.WRITE_PRESETS, id: \.self) { preset in
                            SmallButton(title: preset, active: false) { vm.applyWritePreset(preset: preset) }
                        }
                    }
                }
            }
            HStack {
                Text(write.previewText)
                    .font(.system(size: 12, design: write.canSend ? .monospaced : .default))
                    .foregroundStyle(write.previewIsError ? AppColor.danger : AppColor.textSecondary)
                Spacer()
                SmallButton(title: strings.SEND_BUTTON, active: write.canSend, enabled: write.canSend) {
                    hideKeyboard()
                    vm.send()
                }
            }
            if write.showEscapeHint { hint(strings.WRITE_ESCAPE_HINT) }
        }
        .padding(.top, AppSpacing.xs)
    }

    private func hint(_ text: String) -> some View {
        Text(text).font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
    }
}

/// The *Received Data* box: dark terminal background, old lines on top, new ones at the bottom (like a log).
struct ReceivedDataSection: View {
    let state: ScannerUiState
    let onClear: () -> Void

    private var strings: ScannerStrings { ScannerStrings.shared }

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            HStack {
                SectionLabel(text: strings.RECEIVED_TITLE)
                Spacer()
                if let count = state.receivedCountText {
                    Text(count).font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
                    SmallButton(title: strings.CLEAR_BUTTON, active: false, action: onClear)
                }
            }
            TerminalBox(lines: state.receivedLines, placeholder: strings.RECEIVED_PLACEHOLDER)
        }
    }
}

/// Small Read / Notify / Stop / Clear / Write / Send button. `active` = blue background.
private struct SmallButton: View {
    let title: String
    let active: Bool
    var enabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(active ? Color.white : AppColor.primary)
                .padding(.horizontal, 10)
                .frame(height: 28)
                .background(active ? AppColor.primary : AppColor.contentBackground)
                .overlay(RoundedRectangle(cornerRadius: AppRadius.field).stroke(AppColor.secondary))
                .clipShape(RoundedRectangle(cornerRadius: AppRadius.field))
                .opacity(enabled ? 1 : 0.4)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}
