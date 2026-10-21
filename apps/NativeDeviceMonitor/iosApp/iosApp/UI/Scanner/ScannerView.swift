import SwiftUI
import Shared

/// Day 2 → Day 4 — Scanner tab in SwiftUI. Which rows show, which buttons are enabled, every label: decided in the
/// shared `ScannerViewModel`. Only View things stay here: keyboard focus, scrolling, colours.
/// Mirrors Android `ui/scanner/ScannerScreen.kt` — same block order, same strings.
///
/// ```
///  ScannerView
///   └─ ScrollView > VStack
///       ├─ AppHintText                 how to start the peripheral
///       ├─ [not ready] AppCard         missing permission / radio off → [Open Permissions]
///       ├─ Device Name   [🔍 Search device…]  + 3 toggles (CarMD only · Hide unknown · Hide audio)
///       ├─ [ Scan Bluetooth ]
///       ├─ Discovered Devices          3 of 7     (tap = select · tap again = advertising packet)
///       ├─ [ Connect ] [ Disconnect ]
///       ├─ ● Status: Connected to …    + raw lastEvent
///       ├─ GattSection                 Day 3 + task 004
///       └─ ReceivedDataSection         Day 4
/// ```
struct ScannerView: View {
    private let vm: ScannerViewModel
    private let onOpenPermissions: () -> Void
    @StateObject private var model: Observed<ScannerUiState>
    /// Does the Device Name box hold the keyboard? `false` = hide it (≈ Android `focusManager.clearFocus()`).
    @FocusState private var searchFocused: Bool

    init(vm: ScannerViewModel, onOpenPermissions: @escaping () -> Void) {
        self.vm = vm
        self.onOpenPermissions = onOpenPermissions
        _model = StateObject(wrappedValue: Observed(vm.state))
    }

    private var strings: ScannerStrings { ScannerStrings.shared }

    var body: some View {
        let state = model.value
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.sm) {
                AppHintText(text: strings.HINT, showBullet: false)

                if let notReady = state.notReadyText {
                    AppCard(title: strings.NOT_READY_TITLE) {
                        AppHintText(text: notReady)
                        if state.showOpenPermissions {
                            AppSecondaryButton(title: strings.OPEN_PERMISSIONS_BUTTON, action: onOpenPermissions)
                        }
                    }
                }

                searchSection(state)
                scanSection(state)
                devicesSection(state)
                connectButtons(state)
                statusSection(state)
                GattSection(state: state, vm: vm, hideKeyboard: { searchFocused = false })
                ReceivedDataSection(state: state, onClear: { vm.clearReceived() })
            }
            .padding(AppSpacing.md)
            // Tap empty space = hide the keyboard (buttons / toggles / rows win over this with their own gestures).
            .contentShape(Rectangle())
            .onTapGesture { searchFocused = false }
        }
        .scrollDismissesKeyboard(.immediately)
        .background(AppColor.surface)
    }

    // MARK: - Device Name + filters

    private func searchSection(_ state: ScannerUiState) -> some View {
        VStack(alignment: .leading, spacing: AppSpacing.sm) {
            SectionLabel(text: strings.DEVICE_NAME_LABEL)
            HStack(spacing: AppSpacing.sm) {
                Image(systemName: "magnifyingglass").foregroundStyle(AppColor.textSecondary)
                TextField(strings.SEARCH_PLACEHOLDER, text: Binding(get: { state.query }, set: { vm.onQueryChange(text: $0) }))
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()      // "CMD-3C71" autocorrected would break filtering
                    .focused($searchFocused)
                    .submitLabel(.search)
                    .onSubmit { searchFocused = false }
            }
            .padding(.horizontal, 12)
            .frame(height: 48)
            .overlay(RoundedRectangle(cornerRadius: AppRadius.field).stroke(searchFocused ? AppColor.primary : AppColor.border))
            .contentShape(Rectangle())
            .onTapGesture { searchFocused = true }

            toggle(strings.FILTER_TOGGLE, isOn: state.carmdOnly, enabled: state.carmdToggleEnabled) { vm.setCarmdOnly(on: $0) }
            toggle(strings.HIDE_UNKNOWN_TOGGLE, isOn: state.hideUnknown) { vm.setHideUnknown(on: $0) }
            toggle(strings.HIDE_AUDIO_TOGGLE, isOn: state.hideAudio) { vm.setHideAudio(on: $0) }
            ForEach(state.filterLines, id: \.self) { line in
                Text(line).font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
            }
        }
    }

    private func toggle(_ label: String, isOn: Bool, enabled: Bool = true, set: @escaping (Bool) -> Void) -> some View {
        Toggle(label, isOn: Binding(get: { isOn }, set: { searchFocused = false; set($0) }))
            .font(.system(size: 13))
            .foregroundStyle(AppColor.textSecondary)
            .padding(4)
            .disabled(!enabled)
    }

    // MARK: - Scan

    private func scanSection(_ state: ScannerUiState) -> some View {
        VStack(alignment: .leading, spacing: AppSpacing.sm) {
            AppPrimaryButton(title: state.scanButtonText, action: { searchFocused = false; vm.onScanClick() }, isEnabled: state.scanEnabled)
            if let scanning = state.scanningText {
                HStack(spacing: AppSpacing.sm) {
                    ProgressView()
                    Text(scanning).font(.system(size: 13))
                }
            }
        }
    }

    // MARK: - Discovered Devices

    private func devicesSection(_ state: ScannerUiState) -> some View {
        VStack(alignment: .leading, spacing: AppSpacing.sm) {
            HStack {
                SectionLabel(text: strings.DEVICES_TITLE)
                Spacer()
                if let count = state.matchCountText {
                    Text(count).font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
                }
            }
            Group {
                if let empty = state.emptyListText {
                    Text(empty).font(.system(size: 13)).foregroundStyle(AppColor.textSecondary).padding(AppSpacing.md)
                } else {
                    // LazyVStack ≈ LazyColumn: only builds visible rows (a crowded place gives 50–100 devices)
                    LazyVStack(spacing: 0) {
                        ForEach(Array(state.rows.enumerated()), id: \.element.address) { index, row in
                            if index > 0 { Divider().overlay(AppColor.divider) }
                            deviceRow(row)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .clipShape(RoundedRectangle(cornerRadius: AppRadius.field))
            .overlay(RoundedRectangle(cornerRadius: AppRadius.field).stroke(AppColor.divider))

            if state.showTapHint {
                Text(strings.TAP_TO_EXPAND).font(.system(size: 11)).foregroundStyle(AppColor.textSecondary)
            }
        }
    }

    private func deviceRow(_ row: DeviceRowUi) -> some View {
        VStack(alignment: .leading, spacing: AppSpacing.sm) {
            HStack(spacing: AppSpacing.xs) {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: AppSpacing.xs) {
                        Text(row.name)
                            .font(.system(size: 15, weight: row.isSelected ? .bold : .regular))
                            .foregroundStyle(row.isSelected ? AppColor.primary : AppColor.textPrimary)
                            .lineLimit(1)
                        if row.isConnected {
                            Text(strings.CONNECTED_TAG)
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 1)
                                .background(AppColor.success)
                                .clipShape(Capsule())
                        }
                    }
                    // iOS has no MAC — this is a UUID assigned by THIS iPhone
                    Text(row.address)
                        .font(.system(size: 11, design: .monospaced))
                        .foregroundStyle(AppColor.textSecondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                SignalBars(signal: row.signal)
                Text(row.rssiText).font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
            }
            if !row.detailLines.isEmpty {
                VStack(alignment: .leading, spacing: 2) {
                    ForEach(row.detailLines, id: \.self) { line in
                        Text(line).font(.system(size: 11, design: .monospaced)).foregroundStyle(AppColor.textSecondary)
                    }
                }
            }
        }
        .padding(.horizontal, AppSpacing.md)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(row.isSelected ? AppColor.contentBackground : AppColor.surface)
        .opacity(row.isStale ? 0.4 : 1)
        .contentShape(Rectangle())
        .onTapGesture {
            searchFocused = false
            vm.onDeviceClick(address: row.address)
        }
    }

    // MARK: - Connect / Disconnect + Status

    private func connectButtons(_ state: ScannerUiState) -> some View {
        HStack(spacing: AppSpacing.sm) {
            PanelButton(title: strings.CONNECT_BUTTON, isEnabled: state.buttons.connectEnabled, isLoading: state.buttons.connecting,
                        container: AppColor.contentBackground, content: AppColor.primary, border: AppColor.secondary) {
                searchFocused = false
                vm.connect()
            }
            PanelButton(title: strings.DISCONNECT_BUTTON, isEnabled: state.buttons.disconnectEnabled, isLoading: false,
                        container: AppColor.disabled, content: AppColor.textPrimary, border: nil) {
                searchFocused = false
                vm.disconnect()
            }
        }
    }

    private func statusSection(_ state: ScannerUiState) -> some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            HStack(spacing: AppSpacing.sm) {
                Circle().fill(toneColor(state.statusTone)).frame(width: 8, height: 8)
                Text(state.statusText).font(.system(size: 14)).foregroundStyle(AppColor.textPrimary)
            }
            if let event = state.lastEvent {
                Text(event).font(.system(size: 11, design: .monospaced)).foregroundStyle(AppColor.textSecondary)
            }
        }
    }

    private func toneColor(_ tone: StatusTone) -> Color {
        switch tone {
        case .connected: AppColor.success
        case .error: AppColor.danger
        case .connecting: AppColor.warning
        case .neutral: AppColor.textSecondary
        }
    }
}

/// Small bold label above each block ("Device Name", "Discovered Devices"…) like the mockup.
struct SectionLabel: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(AppColor.textPrimary)
            .padding(.top, AppSpacing.xs)
    }
}

/// Half-width button for the Connect / Disconnect pair (mockup: 2 buttons side by side, light background).
private struct PanelButton: View {
    let title: String
    let isEnabled: Bool
    let isLoading: Bool
    let container: Color
    let content: Color
    let border: Color?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: AppSpacing.sm) {
                if isLoading { ProgressView().controlSize(.small).tint(content) }
                Text(title).font(.system(size: 15, weight: .semibold))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .foregroundStyle(content)
            .background(container)
            .overlay(RoundedRectangle(cornerRadius: AppRadius.field).stroke(border ?? .clear))
            .clipShape(RoundedRectangle(cornerRadius: AppRadius.field))
            .opacity(isEnabled || isLoading ? 1 : 0.45)
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
    }
}

/// 3 small signal bars next to the dBm number. Thresholds live in the shared `BleSignal.of`.
private struct SignalBars: View {
    let signal: BleSignal

    var body: some View {
        let color: Color = switch signal {
        case .strong: AppColor.success
        case .medium: AppColor.warning
        case .weak: AppColor.danger
        }
        HStack(alignment: .bottom, spacing: 2) {
            ForEach(Array([5.0, 8.0, 11.0].enumerated()), id: \.offset) { i, h in
                RoundedRectangle(cornerRadius: 1)
                    .fill(i < Int(signal.bars) ? color : AppColor.disabled)
                    .frame(width: 3, height: h)
            }
        }
    }
}
