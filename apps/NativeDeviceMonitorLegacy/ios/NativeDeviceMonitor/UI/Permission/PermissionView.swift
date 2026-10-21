import SwiftUI

/// Day 1 — iOS runtime permission test screen. Strings live in `PermissionStrings`.
/// Mirrors `android/.../ui/permission/PermissionScreen.kt`.
///
/// ```
///  PermissionView
///   └─ ScrollView > VStack
///       ├─ AppHintText  (how to test)
///       ├─ last result  (monospace)
///       ├─ AppCard "Bluetooth"       -> StatusChip + Info.plist key + button per state
///       ├─ AppCard "Camera"          -> ... (Day 1 exercise: a 2nd permission that isn't BLE)
///       ├─ AppCard "Bluetooth radio" -> chip ON/OFF + "Ready to scan" + [Ask iOS]
///       └─ AppCard "Debug"           -> Request all · Open app settings · how to reset
///
///  which button shows (DIFFERENT from Android — see PermissionStatus.swift):
///    .granted        -> no button
///    .notDetermined  -> [Request permission]     the system WILL show a dialog
///    .denied         -> [Open app settings]      the dialog is dead for good
///    .restricted     -> no button, just says it's locked (Settings is pointless too)
/// ```
///
/// The View doesn't read permissions itself: it asks `PermissionService.shared` — `UI/` calls down into `NativeCore/`.
struct PermissionView: View {

    /// `@Observable` (iOS 17+): just READING a property in `body` makes SwiftUI track it.
    private let service = PermissionService.shared
    /// Bluetooth radio on/off — also `@Observable`.
    private let bluetooth = BluetoothService.shared

    /// Re-read permissions every time the app returns to the foreground — the user may have just turned one off in Settings.
    /// Android does this in `onResume`; SwiftUI uses `scenePhase`.
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.md) {
                AppHintText(text: PermissionStrings.hint, showBullet: false)

                if !service.lastResult.isEmpty {
                    Text(PermissionStrings.lastResultFormat(service.lastResult))
                        .font(.system(size: 13, design: .monospaced))  // raw result: read by eye
                        .foregroundStyle(AppColor.textPrimary)
                }

                ForEach(PermissionGroups.all) { group in
                    groupCard(group)
                }

                radioCard()

                AppCard(title: PermissionStrings.debugTitle) {
                    AppHintText(text: PermissionStrings.debugHintQueue)
                    AppHintText(text: PermissionStrings.debugHintReset)
                    AppPrimaryButton(title: PermissionStrings.requestAllButton) {
                        Task { await service.requestAll(); bluetooth.start() }
                    }
                    AppSecondaryButton(title: PermissionStrings.openSettingsButton) {
                        service.openAppSettings()
                    }
                }
            }
            .padding(AppSpacing.md)
        }
        .background(AppColor.contentBackground)
        .navigationTitle(PermissionStrings.title)
        .navigationBarTitleDisplayMode(.inline)
        .task { await service.refreshAll(); bluetooth.start() }          // first draw
        .onChange(of: scenePhase) { _, newPhase in
            // .active = back from Settings. bluetooth.start() after refresh: if the user just granted the permission in
            // Settings, only now may we create the CBCentralManager to read the radio.
            if newPhase == .active {
                Task { await service.refreshAll(); bluetooth.start() }
            }
        }
    }

    /// 1 card = 1 feature. A separate function because the cards are identical stroke for stroke — hand-copying invites drift.
    @ViewBuilder
    private func groupCard(_ group: PermissionGroup) -> some View {
        // Not read yet (first draw) → treat as not asked, no `!` so it can't crash.
        let status = service.states[group.id]?.status ?? .notDetermined

        AppCard(title: PermissionStrings.label(group.id)) {
            AppHintText(text: PermissionStrings.why(group.id))
            statusChip(status)
            // Show the key: without it the app CRASHES the moment it requests, not a denied result.
            Text(group.infoPlistKey)
                .font(.system(size: 12, design: .monospaced))
                .foregroundStyle(AppColor.textSecondary)

            switch status {
            case .granted:
                EmptyView()          // nothing to do

            case .notDetermined:
                AppPrimaryButton(title: PermissionStrings.requestButton) {
                    // Once Bluetooth is requested start reading the radio right away
                    Task { await service.request(group); bluetooth.start() }
                }

            case .denied:
                AppHintText(text: PermissionStrings.deniedHint)
                AppSecondaryButton(title: PermissionStrings.openSettingsButton) {
                    service.openAppSettings()
                }

            case .restricted:
                // Deliberately NO button: with Screen Time / MDM locking it the app's Settings page has no switch.
                AppHintText(text: PermissionStrings.restrictedHint)
            }
        }
    }

    /// Last card: not a permission but the Bluetooth RADIO — the last gate before the Scanner (Day 2) scans.
    @ViewBuilder
    private func radioCard() -> some View {
        let granted = service.states[PermissionGroups.bluetooth]?.status == .granted
        let readiness = BluetoothService.readiness(radio: bluetooth.radio, permissionGranted: granted)

        AppCard(title: PermissionStrings.radioTitle) {
            AppHintText(text: PermissionStrings.radioHint)
            chip(radioLabel(bluetooth.radio))

            Text(readinessText(readiness))
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(readiness == .ready ? AppColor.success : AppColor.textPrimary)

            // Only show the button when tapping it DOES something: permission granted but radio off.
            if readiness == .radioOff {
                AppPrimaryButton(title: PermissionStrings.showAlertButton) {
                    bluetooth.showSystemPowerAlert()
                }
            }
            #if targetEnvironment(simulator)
            AppHintText(text: PermissionStrings.simulatorNote)
            #endif
        }
    }

    private func radioLabel(_ radio: BluetoothRadioState) -> (String, Color) {
        switch radio {
        case .poweredOn: (PermissionStrings.radioOn, AppColor.success)
        case .poweredOff: (PermissionStrings.radioOff, AppColor.warning)
        case .unauthorized: (PermissionStrings.radioUnauthorized, AppColor.danger)
        case .unsupported: (PermissionStrings.radioUnsupported, AppColor.danger)
        case .unknown: (PermissionStrings.radioUnknown, AppColor.textSecondary)
        }
    }

    private func readinessText(_ readiness: BleReadiness) -> String {
        switch readiness {
        case .ready: PermissionStrings.readyYes
        case .missingPermission: PermissionStrings.readyNoPermission
        case .radioOff: PermissionStrings.readyRadioOff
        case .unsupported: PermissionStrings.readyUnsupported
        }
    }

    /// Color chip — readable at a glance. Colors come from `AppColor`, no hex typed inline.
    private func statusChip(_ status: PermissionStatus) -> some View {
        let item: (String, Color) = switch status {
        case .granted: (PermissionStrings.statusGranted, AppColor.success)
        case .notDetermined: (PermissionStrings.statusNotRequested, AppColor.textSecondary)
        case .denied: (PermissionStrings.statusDenied, AppColor.danger)
        case .restricted: (PermissionStrings.statusRestricted, AppColor.warning)
        }
        return chip(item)
    }

    private func chip(_ item: (String, Color)) -> some View {
        let (text, color) = item
        return Text(text)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, AppSpacing.sm)
            .padding(.vertical, AppSpacing.xs)
            .background(color)
            .clipShape(Capsule())
    }
}

#Preview {
    NavigationStack { PermissionView() }
}
