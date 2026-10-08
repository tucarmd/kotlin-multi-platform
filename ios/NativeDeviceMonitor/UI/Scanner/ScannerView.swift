import SwiftUI

/// Day 2 → Day 4 — Scanner screen per Learning Plan §4 mockup ("main screen — BLE + status + received data").
/// Mirrors `android/.../ui/scanner/ScannerScreen.kt` — same block order, same strings.
///
/// ```
///  ScannerView   (nav bar xanh "Native Device Monitor")
///   └─ ScrollView > VStack
///       ├─ AppHintText                 how to start the peripheral
///       ├─ [not ready] AppCard         missing permission / radio off / Simulator → [Open Permissions]
///       ├─ Device Name   [🔍 Search device…    ]   ← matchesName(): filters the current list as you type
///       │                                           Return / tap outside / drag the screen / tap a button = hide keyboard
///       │                [CarMD tools only ○]      ← filters DURING SCAN (BluetoothScanFilter) — a different thing
///       │                [Hide unknown devices ○]  ← isDeviceShown(): filters the current list, combined
///       │                [Hide audio devices ○]       with the Device Name box (headphones / speakers / earbuds)
///       ├─ [ Scan Bluetooth ]
///       ├─ Discovered Devices          3 of 7
///       │   ┌──────────────────────────────────┐
///       │   │ Device A                 -58 dBm │
///       │   │ Device B ◄ selected      -63 dBm │  ← tap = select · tap again = show advertising packet
///       │   │ Device C                 -77 dBm │
///       │   └──────────────────────────────────┘
///       ├─ [ Connect ] [ Disconnect ]  ← scannerButtonsOf()
///       ├─ ● Status: Connected to …    + raw lastEvent
///       ├─ GattSection                 ← Day 3: service/characteristic · target ★ · Read → raw + value
///       └─ ReceivedDataSection         ← Day 4: one line per notify packet (max 50) + Clear
/// ```
struct ScannerView: View {

    /// Tab-switch callback — the View doesn't know about the TabView, the App passes it in.
    var onOpenPermissions: () -> Void = {}

    private let service = BluetoothClientService.shared
    private let permissions = PermissionService.shared
    private let radio = BluetoothService.shared

    /// Text in the Device Name box.
    @State private var query = ""
    /// Does the Device Name box hold the keyboard? Set `false` = hide the keyboard (≈ Android `focusManager.clearFocus()`).
    @FocusState private var searchFocused: Bool
    /// The scan-time filter defaults to OFF: Day 2 tests with nRF Connect / ESP32, whose names aren't CarMD2 / CMD-*.
    @State private var carmdOnly = false
    /// 2 switches that filter the LIST already seen (scanning unchanged) — default OFF so nRF / ESP32 rows don't vanish.
    @State private var hideUnknown = false
    @State private var hideAudio = false
    /// Selected row — store the id, not the whole device: an RSSI change means a new struct.
    @State private var selectedId: UUID?
    /// Tapping the selected row AGAIN = open the advertising packet.
    @State private var showDetails = false
    private var scanFilter: BluetoothScanFilter { carmdOnly ? .carmdTools : .none }

    @Environment(\.scenePhase) private var scenePhase

    private var readiness: BleReadiness {
        let granted = permissions.states[PermissionGroups.bluetooth]?.status == .granted
        return BluetoothService.readiness(radio: radio.radio, permissionGranted: granted)
    }

    /// Closure filter just like the Day 02 exercise — the rules live in `isDeviceShown` (Device Name box + 2 switches), this only calls it.
    private var shown: [BluetoothDeviceInfo] {
        service.devices.filter {
            isDeviceShown($0, query: query, hideUnknown: hideUnknown, hideAudio: hideAudio,
                          isPinned: $0.id == selectedId || $0.id == service.connectedId)
        }
    }
    private var trimmedQuery: String { query.trimmingCharacters(in: .whitespaces) }
    private var isFiltering: Bool { !trimmedQuery.isEmpty || hideUnknown || hideAudio }
    private var selected: BluetoothDeviceInfo? { service.devices.first { $0.id == selectedId } }

    private var buttons: ScannerButtons {
        scannerButtonsOf(hasSelection: selected != nil,
                         selectedIsConnectable: selected?.advertisement?.isConnectable,
                         selectedIsConnectedDevice: selected != nil && selected?.id == service.connectedId,
                         connection: service.connection,
                         ready: readiness == .ready)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.sm) {
                AppHintText(text: ScannerStrings.hint, showBullet: false)

                if readiness != .ready {
                    AppCard(title: ScannerStrings.notReadyTitle) {
                        AppHintText(text: notReadyText)
                        #if targetEnvironment(simulator)
                        AppHintText(text: ScannerStrings.simulatorNote)
                        #endif
                        if readiness != .unsupported {
                            AppSecondaryButton(title: ScannerStrings.openPermissionsButton, action: onOpenPermissions)
                        }
                    }
                }

                searchSection
                scanSection
                devicesSection
                connectButtons
                statusSection
                // Day 3 + Day 4 — child Views only take state + closures, they don't know the service
                GattSection(isConnected: isConnected,
                            isDiscovering: service.isDiscovering,
                            services: service.services,
                            lastRead: service.lastRead,
                            gattError: service.gattError,
                            mtu: service.mtu,
                            onRead: { service.read($0) },
                            onNotify: { service.setNotify($0, $1) },
                            onWrite: { service.write($0, $1, withResponse: $2) })
                ReceivedDataSection(received: service.received, onClear: { service.clearReceived() })
            }
            .padding(AppSpacing.md)
            // Tap empty space = hide the keyboard. Buttons / Toggles / device rows have their own gestures that win over this
            // → those places set searchFocused = false in their own action.
            .contentShape(Rectangle())
            .onTapGesture { searchFocused = false }
        }
        // Dragging the list drops the keyboard right away — it doesn't cover the Scan button / device list
        .scrollDismissesKeyboard(.immediately)
        .background(AppColor.surface)
        .navigationTitle(ScannerStrings.screenTitle)
        .navigationBarTitleDisplayMode(.inline)
        // Blue nav bar with white text like the mockup header (Android: TopAppBar in MainActivity)
        .toolbarBackground(AppColor.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        // Create the CBCentralManager when the tab OPENS, not at app launch — see `start()` in the service.
        .task { await permissions.refreshAll(); radio.start(); service.start() }
        .onChange(of: scenePhase) { _, phase in
            // Back from Settings: permission/radio may have changed
            if phase == .active { Task { await permissions.refreshAll(); radio.start() } }
        }
    }

    private var notReadyText: String {
        switch readiness {
        case .missingPermission: ScannerStrings.notReadyPermission
        case .radioOff: ScannerStrings.notReadyRadioOff
        case .unsupported: ScannerStrings.notReadyUnsupported
        case .ready: ""
        }
    }

    // MARK: - Device Name

    private var searchSection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.sm) {
            SectionLabel(text: ScannerStrings.deviceNameLabel)
            HStack(spacing: AppSpacing.sm) {
                Image(systemName: "magnifyingglass").foregroundStyle(AppColor.textSecondary)
                TextField(ScannerStrings.searchPlaceholder, text: $query)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()      // device names like "CMD-3C71" being autocorrected would break filtering
                    .focused($searchFocused)
                    .submitLabel(.search)          // the Return key shows "search"
                    .onSubmit { searchFocused = false }   // filtering already runs per keystroke → Return only needs to hide the keyboard
            }
            .padding(.horizontal, 12)
            .frame(height: 48)
            .overlay(RoundedRectangle(cornerRadius: AppRadius.field).stroke(searchFocused ? AppColor.primary : AppColor.border))
            // Tapping the whole field (including the magnifier) = type. The child's gesture beats the parent VStack's "hide keyboard" onTapGesture.
            .contentShape(Rectangle())
            .onTapGesture { searchFocused = true }

            // Changing the filter while scanning has no effect until the next Scan → disable while scanning
            Toggle(ScannerStrings.filterToggle, isOn: $carmdOnly)
                .font(.system(size: 13))
                .foregroundStyle(AppColor.textSecondary)
                .padding(4)   // loosen the switch rows a bit so they're easier to tap
                .disabled(service.isScanning)
            // Filters the list already seen → changing it while scanning takes effect immediately, no need to disable
            Toggle(ScannerStrings.hideUnknownToggle, isOn: $hideUnknown)
                .font(.system(size: 13))
                .foregroundStyle(AppColor.textSecondary)
                .padding(4)   // loosen the switch rows a bit so they're easier to tap
            Toggle(ScannerStrings.hideAudioToggle, isOn: $hideAudio)
                .font(.system(size: 13))
                .foregroundStyle(AppColor.textSecondary)
                .padding(4)   // loosen the switch rows a bit so they're easier to tap
            if carmdOnly {
                Text(ScannerStrings.filterOnFormat(scanFilter.label))
                    .font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
                if service.hiddenCount > 0 {
                    Text(ScannerStrings.hiddenFormat(service.hiddenCount))
                        .font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
                }
            }
        }
        // Flipping a filter switch = done typing → hide the keyboard (Toggle swallows the tap, the parent's onTapGesture never fires)
        .onChange(of: [carmdOnly, hideUnknown, hideAudio]) { searchFocused = false }
    }

    // MARK: - Scan

    private var scanSection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.sm) {
            AppPrimaryButton(
                title: service.isScanning ? ScannerStrings.stopScanButton : ScannerStrings.scanButton,
                action: {
                    searchFocused = false
                    service.isScanning ? service.stopScan() : service.startScan(filter: scanFilter)
                },
                isEnabled: readiness == .ready
            )
            if service.isScanning {
                HStack(spacing: AppSpacing.sm) {
                    ProgressView()
                    Text(ScannerStrings.scanningFormat(service.devices.count)).font(.system(size: 13))
                }
            }
        }
    }

    // MARK: - Discovered Devices

    private var devicesSection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.sm) {
            HStack {
                SectionLabel(text: ScannerStrings.devicesTitle)
                Spacer()
                if isFiltering && !service.devices.isEmpty {
                    Text(ScannerStrings.matchCountFormat(shown.count, service.devices.count))
                        .font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
                }
            }
            Group {
                if service.devices.isEmpty {
                    emptyText(ScannerStrings.noDevices)
                } else if shown.isEmpty {
                    emptyText(trimmedQuery.isEmpty ? ScannerStrings.allHiddenFormat(service.devices.count)
                                                   : ScannerStrings.noMatchFormat(trimmedQuery))
                } else {
                    // TimelineView = the list's "clock", redraws every second → rows fade when a device goes quiet, "Last seen" counts by itself.
                    TimelineView(.periodic(from: .now, by: 1)) { context in
                        // LazyVStack ≈ LazyColumn: only builds visible rows (a crowded place gives 50–100 devices)
                        LazyVStack(spacing: 0) {
                            ForEach(Array(shown.enumerated()), id: \.element.id) { index, device in
                                if index > 0 { Divider().overlay(AppColor.divider) }
                                deviceRow(device, now: context.date)
                            }
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .clipShape(RoundedRectangle(cornerRadius: AppRadius.field))
            .overlay(RoundedRectangle(cornerRadius: AppRadius.field).stroke(AppColor.divider))

            if !shown.isEmpty {
                Text(ScannerStrings.tapToExpand).font(.system(size: 11)).foregroundStyle(AppColor.textSecondary)
            }
        }
    }

    private func emptyText(_ text: String) -> some View {
        Text(text).font(.system(size: 13)).foregroundStyle(AppColor.textSecondary).padding(AppSpacing.md)
    }

    /// One row: name on the left, dBm on the right (as in the mockup). Selected row = light-blue background + dark-blue text.
    private func deviceRow(_ device: BluetoothDeviceInfo, now: Date) -> some View {
        let isThisDevice = device.id == service.connectedId
        let isSelected = device.id == selectedId
        let isConnectedRow: Bool = {
            if isThisDevice, case .connected = service.connection { return true }
            return false
        }()
        // Fade only WHILE scanning (scan over = we stopped listening, the device didn't vanish); the connected row never fades.
        let isStale = service.isScanning && !isThisDevice && BluetoothLookup.isStale(lastSeen: device.lastSeen, now: now)

        return VStack(alignment: .leading, spacing: AppSpacing.sm) {
            HStack(spacing: AppSpacing.xs) {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: AppSpacing.xs) {
                        Text(device.name ?? ScannerStrings.unknownDevice)
                            .font(.system(size: 15, weight: isSelected ? .bold : .regular))
                            .foregroundStyle(isSelected ? AppColor.primary : AppColor.textPrimary)
                            .lineLimit(1)
                        if isConnectedRow {
                            Text(ScannerStrings.connectedTag)
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 1)
                                .background(AppColor.success)
                                .clipShape(Capsule())
                        }
                    }
                    // iOS has no MAC — this is a UUID assigned by THIS iPhone (see BluetoothDeviceInfo)
                    Text(device.id.uuidString)
                        .font(.system(size: 11, design: .monospaced))
                        .foregroundStyle(AppColor.textSecondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                SignalBars(rssi: device.rssi)
                Text(ScannerStrings.rssiFormat(device.rssi))
                    .font(.system(size: 12))
                    .foregroundStyle(AppColor.textSecondary)
            }
            if isSelected && showDetails, let adv = device.advertisement {
                VStack(alignment: .leading, spacing: 2) {
                    ForEach(detailLines(adv, device: device, now: now), id: \.self) { line in
                        Text(line)
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundStyle(AppColor.textSecondary)
                    }
                    Text(ScannerStrings.iosLimits).font(.system(size: 11)).foregroundStyle(AppColor.textSecondary)
                }
            }
        }
        .padding(.horizontal, AppSpacing.md)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(isSelected ? AppColor.contentBackground : AppColor.surface)
        .opacity(isStale ? 0.4 : 1)
        .contentShape(Rectangle())
        .onTapGesture {
            searchFocused = false
            if isSelected {
                showDetails.toggle()
            } else {
                selectedId = device.id
                showDetails = false
            }
        }
    }

    /// Builds the detail lines (pure function, split out of body to keep it tidy).
    private func detailLines(_ adv: BluetoothAdvertisement, device: BluetoothDeviceInfo, now: Date) -> [String] {
        [ScannerStrings.connectableFormat(adv.isConnectable),
         ScannerStrings.txPowerFormat(adv.txPower),
         ScannerStrings.intervalFormat(adv.intervalMs, adv.packetCount),
         ScannerStrings.lastSeenFormat(Int(now.timeIntervalSince(device.lastSeen))),
         ScannerStrings.servicesFormat(adv.serviceUuids, nameOf: BluetoothLookup.serviceName)]
        + adv.manufacturerData.map(ScannerStrings.manufacturerFormat)
        + adv.serviceData.map(ScannerStrings.serviceDataFormat)
    }

    // MARK: - Connect / Disconnect

    private var connectButtons: some View {
        let b = buttons
        return HStack(spacing: AppSpacing.sm) {
            PanelButton(title: ScannerStrings.connectButton, isEnabled: b.connectEnabled, isLoading: b.connecting,
                        container: AppColor.contentBackground, content: AppColor.primary, border: AppColor.secondary) {
                searchFocused = false
                if let device = selected { service.connect(device) }
            }
            PanelButton(title: ScannerStrings.disconnectButton, isEnabled: b.disconnectEnabled, isLoading: false,
                        container: AppColor.disabled, content: AppColor.textPrimary, border: nil) {
                searchFocused = false
                service.disconnect()
            }
        }
    }

    // MARK: - Status

    private var statusSection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            HStack(spacing: AppSpacing.sm) {
                Circle().fill(connectionColor).frame(width: 8, height: 8)
                Text(ScannerStrings.statusFormat(connectionLabel))
                    .font(.system(size: 14))
                    .foregroundStyle(AppColor.textPrimary)
            }
            // lastEvent is the only readable thing when connect fails → keep it, in small text
            if !service.lastEvent.isEmpty {
                Text(service.lastEvent)
                    .font(.system(size: 11, design: .monospaced))
                    .foregroundStyle(AppColor.textSecondary)
            }
        }
    }

    private var connectionLabel: String {
        switch service.connection {
        case .idle: ScannerStrings.stateIdle
        case .connecting: ScannerStrings.stateConnecting
        case .connected(let name): ScannerStrings.stateConnectedFormat(name)
        case .disconnected: ScannerStrings.stateDisconnected
        case .error(let message): message
        }
    }

    /// Color of the dot before "Status:".
    private var connectionColor: Color {
        switch service.connection {
        case .connected: AppColor.success
        case .error: AppColor.danger                 // signal lost / radio off: red
        case .connecting: AppColor.warning
        case .idle, .disconnected: AppColor.textSecondary   // our own action → grey, nothing scary
        }
    }

    private var isConnected: Bool {
        if case .connected = service.connection { return true }
        return false
    }
}

/// Small bold label above each block ("Device Name", "Discovered Devices"…) like the mockup.
private struct SectionLabel: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(AppColor.textPrimary)
            .padding(.top, AppSpacing.xs)
    }
}

/// Half-width button for the Connect / Disconnect pair. Not `AppPrimaryButton` because that one is full width +
/// solid blue; the mockup needs 2 buttons side by side, light background. Only this screen uses it so it lives here.
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

/// 3 small signal bars next to the dBm number. Thresholds live in `BleSignal.of`, not here.
private struct SignalBars: View {
    let rssi: Int

    var body: some View {
        let signal = BleSignal.of(rssi)
        let color: Color = switch signal {
        case .strong: AppColor.success
        case .medium: AppColor.warning
        case .weak: AppColor.danger
        }
        HStack(alignment: .bottom, spacing: 2) {
            ForEach(Array([5.0, 8.0, 11.0].enumerated()), id: \.offset) { i, h in
                RoundedRectangle(cornerRadius: 1)
                    .fill(i < signal.bars ? color : AppColor.disabled)
                    .frame(width: 3, height: h)
            }
        }
    }
}

#Preview {
    NavigationStack { ScannerView() }
}
