import SwiftUI
import Shared

/// Day 1 — Permissions tab in SwiftUI. Which button each card shows is decided in the shared `PermissionViewModel`
/// (`PermissionCardUi.action`) — the same rule Android draws. Mirrors Android `ui/permission/PermissionScreen.kt`.
///
/// ```
///  PermissionView
///   └─ ScrollView > VStack
///       ├─ AppHintText · last result (monospace)
///       ├─ AppCard per feature   -> chip + real permission names + 0/1 button
///       ├─ AppCard "Bluetooth radio" -> chip ON/OFF + "Ready to scan" + [Turn on]
///       └─ AppCard "Debug"           -> Request all · Reset asked flags
/// ```
struct PermissionView: View {
    private let vm: PermissionViewModel
    @StateObject private var model: Observed<PermissionUiState>

    init(vm: PermissionViewModel) {
        self.vm = vm
        _model = StateObject(wrappedValue: Observed(vm.state))
    }

    private var strings: PermissionStrings { PermissionStrings.shared }

    var body: some View {
        let state = model.value
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.md) {
                AppHintText(text: strings.HINT, showBullet: false)

                if let last = state.lastResult {
                    Text(last).font(.system(size: 13, design: .monospaced)).foregroundStyle(AppColor.textPrimary)
                }

                ForEach(state.cards, id: \.groupId) { card in
                    groupCard(card)
                }

                radioCard(state)

                AppCard(title: strings.DEBUG_TITLE) {
                    AppHintText(text: strings.DEBUG_HINT)
                    AppPrimaryButton(title: strings.REQUEST_ALL_BUTTON) { vm.requestAll() }
                    AppSecondaryButton(title: strings.RESET_FLAGS_BUTTON) { vm.resetAskedFlags() }
                }
            }
            .padding(AppSpacing.md)
        }
        .background(AppColor.contentBackground)
    }

    private func groupCard(_ card: PermissionCardUi) -> some View {
        AppCard(title: card.title) {
            AppHintText(text: card.why)
            AppChip(text: PermissionViewModelKt.statusLabel(status: card.status), color: statusColor(card.status))
            Text(card.permissionNames).font(.system(size: 12, design: .monospaced)).foregroundStyle(AppColor.textSecondary)
            if let note = card.installTimeNote {
                Text(note).font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
            }
            if let hint = card.hint { AppHintText(text: hint) }
            switch card.action {
            case .request:
                AppPrimaryButton(title: strings.REQUEST_BUTTON) { vm.request(groupId: card.groupId) }
            case .openSettings:
                AppPrimaryButton(title: strings.OPEN_SETTINGS_BUTTON) { vm.openSettings() }
            case .none:
                EmptyView()
            }
        }
    }

    /// Not a permission but the Bluetooth RADIO — the last gate before the Scanner can scan.
    private func radioCard(_ state: PermissionUiState) -> some View {
        AppCard(title: strings.RADIO_TITLE) {
            AppHintText(text: strings.RADIO_HINT)
            AppChip(text: PermissionViewModelKt.radioLabel(radio: state.radio), color: radioColor(state.radio))
            Text(state.readinessText)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(state.readiness == .ready ? AppColor.success : AppColor.textPrimary)
            if state.showTurnOnBluetooth {
                AppPrimaryButton(title: strings.TURN_ON_BUTTON) { vm.turnOnBluetooth() }
            }
        }
    }

    private func statusColor(_ status: PermissionStatus) -> Color {
        switch status {
        case .granted: AppColor.success
        case .denied: AppColor.warning
        case .permanentlyDenied: AppColor.danger
        case .notRequested: AppColor.textSecondary
        }
    }

    private func radioColor(_ radio: BluetoothRadioState) -> Color {
        switch radio {
        case .on: AppColor.success
        case .off: AppColor.warning
        case .unsupported: AppColor.danger
        }
    }
}
