import SwiftUI

/// Day 3 + Day 4 — the GATT block under Status, and the *Received Data* box. Takes state + closures, NOT the service:
/// readable means drawable, `#Preview`-able without Bluetooth. Mirrors `android/.../ui/scanner/GattSection.kt`.
///
/// ```
///  GATT                                   3 services · 9 characteristics
///  Read target: 2A19 Battery Level        ← readTargetOf()
///  Notify target: FFE1 UART data          ← notifyTargetOf()
///  Write target: FFE1 UART data           ← writeTargetOf() / the row whose [Write] was just tapped      (task 004)
///  MTU 185 · max 182 bytes per packet     ← service.mtu                                 (task 004)
///  ┌────────────────────────────────────────────────┐
///  │ 180F Battery                                   │
///  │   2A19 Battery Level  ★ target                 │
///  │   READ NOTIFY        [Read] [Notify] [Write]   │  ← buttons only show when the property allows
///  └────────────────────────────────────────────────┘
///  Write  To FFE1 UART data · with response          ← WriteBlock (task 004)
///  [ ATZ\r                                      ]
///  [Text][Hex]   [ATZ\r] [ATI\r] [010C\r]
///  → 41 54 5A 0D (4 bytes)                  [Send]   ← planWrite(): preview or a red reason
///  Last read: 2A19 Battery Level
///    raw:   5A
///    value: 90 %
///  (red error: "Read 2A29 failed: …")
///
///  Received Data                          12 / 50  [Clear]
///  ┌ 12:03:04.123 NOTIFY FFE1  41 54 0D → "AT\r" ┐   ← one line per notify packet
/// ```
struct GattSection: View {
    let isConnected: Bool
    let isDiscovering: Bool
    let services: [GattServiceInfo]
    let lastRead: GattValue?
    let gattError: String?
    let mtu: Int
    let onRead: (GattCharacteristicInfo) -> Void
    let onNotify: (GattCharacteristicInfo, Bool) -> Void
    let onWrite: (GattCharacteristicInfo, Data, Bool) -> Void

    /// Task 004 — the write target the user just tapped [Write] on. Only the KEY is kept; looked up again in the new `services` on every draw.
    @State private var writeKey: String?

    /// The row the user chose (if still there and writable) → otherwise auto-pick via `writeTargetOf`.
    private var writeTarget: GattCharacteristicInfo? {
        services.flatMap(\.characteristics).first { $0.key == writeKey && $0.properties.canWrite } ?? writeTargetOf(services)
    }

    var body: some View {
        let readTarget = readTargetOf(services)
        let notifyTarget = notifyTargetOf(services)
        let writeTarget = writeTarget
        let targetKeys = Set([readTarget?.key, notifyTarget?.key, writeTarget?.key].compactMap { $0 })

        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            HStack {
                Text(ScannerStrings.gattTitle)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(AppColor.textPrimary)
                    .padding(.top, AppSpacing.xs)
                Spacer()
                if !services.isEmpty {
                    Text(ScannerStrings.servicesCountFormat(services.count, services.reduce(0) { $0 + $1.characteristics.count }))
                        .font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
                }
            }

            if isDiscovering && services.isEmpty {
                HStack(spacing: AppSpacing.sm) {
                    ProgressView().controlSize(.small)
                    hint(ScannerStrings.gattDiscovering)
                }
            } else if !isConnected && services.isEmpty {
                hint(ScannerStrings.gattNotConnected)
            } else if services.isEmpty {
                hint(ScannerStrings.gattEmpty)
            } else {
                hint(ScannerStrings.readTargetFormat(readTarget.map(characteristicLabel)))
                hint(ScannerStrings.notifyTargetFormat(notifyTarget.map(characteristicLabel)))
                hint(ScannerStrings.writeTargetFormat(writeTarget.map(characteristicLabel)))
                hint(ScannerStrings.mtuFormat(mtu, maxPayloadOf(mtu)))
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(services.enumerated()), id: \.element.id) { index, service in
                        if index > 0 { Divider().overlay(AppColor.divider) }
                        serviceBlock(service, targetKeys: targetKeys, writeKey: writeTarget?.key)
                    }
                }
                .padding(.vertical, AppSpacing.xs)
                .frame(maxWidth: .infinity, alignment: .leading)
                .overlay(RoundedRectangle(cornerRadius: AppRadius.field).stroke(AppColor.divider))
                if let writeTarget {
                    WriteBlock(target: writeTarget, mtu: mtu, onSend: onWrite)
                }
            }

            if let lastRead {
                Text(ScannerStrings.lastReadTitleFormat(
                    ScannerStrings.uuidWithName(lastRead.uuid, BluetoothLookup.characteristicName(lastRead.uuid))))
                    .font(.system(size: 13))
                    .foregroundStyle(AppColor.textPrimary)
                    .padding(.top, AppSpacing.xs)
                mono(ScannerStrings.rawFormatLine(lastRead.hex))
                mono(ScannerStrings.decodedFormatLine(lastRead.decoded))
            }
            // Read / subscribe errors don't drop the connection → red here, not over the Status
            if let gattError {
                Text(gattError).font(.system(size: 12)).foregroundStyle(AppColor.danger)
            }
        }
    }

    private func serviceBlock(_ service: GattServiceInfo, targetKeys: Set<String>, writeKey: String?) -> some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            Text(ScannerStrings.uuidWithName(service.uuid, BluetoothLookup.serviceName(service.uuid)))
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(AppColor.primary)
                .lineLimit(1)
                .truncationMode(.middle)
            ForEach(service.characteristics) { c in
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: AppSpacing.xs) {
                        Text(characteristicLabel(c))
                            .font(.system(size: 12))
                            .foregroundStyle(AppColor.textPrimary)
                            .lineLimit(1)
                            .truncationMode(.middle)
                        if targetKeys.contains(c.key) {
                            Text(ScannerStrings.targetTag).font(.system(size: 10)).foregroundStyle(AppColor.warning)
                        }
                    }
                    HStack(spacing: AppSpacing.xs) {
                        Text(c.properties.labels.joined(separator: " "))
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundStyle(AppColor.textSecondary)
                        Spacer()
                        if c.properties.read {
                            SmallButton(title: ScannerStrings.readButton, isActive: false) { onRead(c) }
                        }
                        if c.properties.canSubscribe {
                            SmallButton(title: c.isNotifying ? ScannerStrings.stopNotifyButton : ScannerStrings.notifyButton,
                                        isActive: c.isNotifying) { onNotify(c, !c.isNotifying) }
                        }
                        // Doesn't write right away: only SELECTS the target — the content is typed in WriteBlock. Blue background = current write target.
                        if c.properties.canWrite {
                            SmallButton(title: ScannerStrings.writeButton, isActive: c.key == writeKey) { self.writeKey = c.key }
                        }
                    }
                }
                .padding(.leading, AppSpacing.sm)
            }
        }
        .padding(.horizontal, AppSpacing.md)
        .padding(.vertical, AppSpacing.xs)
    }

    private func characteristicLabel(_ c: GattCharacteristicInfo) -> String {
        ScannerStrings.uuidWithName(c.uuid, BluetoothLookup.characteristicName(c.uuid))
    }

    private func hint(_ text: String) -> some View {
        Text(text).font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
    }

    private func mono(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 12, design: .monospaced))
            .foregroundStyle(AppColor.textSecondary)
            .padding(.leading, AppSpacing.sm)
    }
}

/// Task 004 — command input + Send for `target`. The rules (writable? valid hex? longer than MTU?) all live in
/// `planWrite` — here we only print the preview / red reason and disable Send.
///
/// The input state lives in the View's `@State`, not pushed into the service: half-typed text is the screen's business,
/// the service only needs the final bytes when Send is tapped.
private struct WriteBlock: View {
    let target: GattCharacteristicInfo
    let mtu: Int
    let onSend: (GattCharacteristicInfo, Data, Bool) -> Void

    @State private var input = ""
    @State private var asHex = false
    @FocusState private var focused: Bool

    var body: some View {
        let plan = planWrite(input, asHex: asHex, properties: target.properties, maxPayload: maxPayloadOf(mtu))

        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            HStack(spacing: AppSpacing.sm) {
                Text(ScannerStrings.writeTitle)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(AppColor.textPrimary)
                Text(ScannerStrings.writeToFormat(
                        ScannerStrings.uuidWithName(target.uuid, BluetoothLookup.characteristicName(target.uuid)),
                        target.properties.write))
                    .font(.system(size: 12))
                    .foregroundStyle(AppColor.textSecondary)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
            TextField(asHex ? ScannerStrings.writePlaceholderHex : ScannerStrings.writePlaceholderText, text: $input)
                .font(.system(size: 14, design: .monospaced))
                // AT commands are UPPERCASE; autocorrect off (ATZ corrected to "Atz" means the dongle won't understand)
                .textInputAutocapitalization(.characters)
                .autocorrectionDisabled()
                .focused($focused)
                .submitLabel(.send)
                .onSubmit { send(plan) }
                .padding(.horizontal, 12)
                .frame(height: 44)
                .overlay(RoundedRectangle(cornerRadius: AppRadius.field).stroke(focused ? AppColor.primary : AppColor.border))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: AppSpacing.xs) {
                    SmallButton(title: ScannerStrings.modeText, isActive: !asHex) { asHex = false }
                    SmallButton(title: ScannerStrings.modeHex, isActive: asHex) { asHex = true }
                    Spacer().frame(width: AppSpacing.sm)
                    ForEach(ScannerStrings.writePresets, id: \.self) { preset in
                        SmallButton(title: preset, isActive: false) { asHex = false; input = preset }
                    }
                }
            }
            HStack {
                switch plan {
                case .ok(let bytes, _):
                    Text(ScannerStrings.writePreviewFormat(BluetoothDecoder.toHex(bytes), bytes.count))
                        .font(.system(size: 12, design: .monospaced))
                        .foregroundStyle(AppColor.textSecondary)
                case .invalid(let reason):
                    // An empty field isn't an error yet → grey text; everything else (bad hex, too long) → red
                    Text(reason)
                        .font(.system(size: 12))
                        .foregroundStyle(input.isEmpty ? AppColor.textSecondary : AppColor.danger)
                }
                Spacer()
                SmallButton(title: ScannerStrings.sendButton, isActive: plan.isOk) { send(plan) }
                    .disabled(!plan.isOk)
                    .opacity(plan.isOk ? 1 : 0.4)
            }
            if !asHex {
                Text(ScannerStrings.writeEscapeHint).font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
            }
        }
        .padding(.top, AppSpacing.xs)
    }

    private func send(_ plan: WritePlan) {
        guard case .ok(let bytes, let withResponse) = plan else { return }
        focused = false                      // after sending drop the keyboard so the WRITE / NOTIFY reply lines below are visible
        onSend(target, bytes, withResponse)
    }
}

private extension WritePlan {
    var isOk: Bool {
        if case .ok = self { return true }
        return false
    }
}

/// The *Received Data* box: dark terminal-style background, old lines on top, new ones at the bottom (like a log).
/// A plain VStack, not Lazy: at most 50 lines, inside the screen's ScrollView.
struct ReceivedDataSection: View {
    let received: [GattValue]
    let onClear: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            HStack {
                Text(ScannerStrings.receivedTitle)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(AppColor.textPrimary)
                    .padding(.top, AppSpacing.xs)
                Spacer()
                if !received.isEmpty {
                    Text(ScannerStrings.receivedCountFormat(received.count, receivedLogMax))
                        .font(.system(size: 12)).foregroundStyle(AppColor.textSecondary)
                    SmallButton(title: ScannerStrings.clearButton, isActive: false, action: onClear)
                }
            }
            VStack(alignment: .leading, spacing: 2) {
                if received.isEmpty {
                    Text(ScannerStrings.receivedPlaceholder).font(.system(size: 13, design: .monospaced))
                }
                ForEach(received) { v in
                    Text(ScannerStrings.receivedLineFormat(v.at.formatted(Self.clock), v.source.rawValue, v.uuid, v.hex, v.decoded))
                        .font(.system(size: 11, design: .monospaced))   // hex bytes: only readable when columns line up
                }
            }
            .foregroundStyle(AppColor.terminalText)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(AppSpacing.md)
            .background(AppColor.terminalBackground)
            .clipShape(RoundedRectangle(cornerRadius: AppRadius.field))
        }
    }

    /// `12:03:04.123` — fixed 24h time, locale-independent (easy to compare with Logcat / console).
    private static let clock = Date.VerbatimFormatStyle(
        format: "\(hour: .twoDigits(clock: .twentyFourHour, hourCycle: .zeroBased)):\(minute: .twoDigits):\(second: .twoDigits).\(secondFraction: .fractional(3))",
        timeZone: .current,
        calendar: .current)
}

/// Small Read / Notify / Stop / Clear / Write / Send button. `isActive` = notifying / selected → blue background.
private struct SmallButton: View {
    let title: String
    let isActive: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(isActive ? .white : AppColor.primary)
                .padding(.horizontal, 10)
                .frame(height: 28)
                .background(isActive ? AppColor.primary : AppColor.contentBackground)
                .overlay(RoundedRectangle(cornerRadius: AppRadius.field).stroke(AppColor.secondary))
                .clipShape(RoundedRectangle(cornerRadius: AppRadius.field))
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    let services = [
        GattServiceInfo(uuid: "180F", characteristics: [
            GattCharacteristicInfo(serviceUuid: "180F", uuid: "2A19", properties: .of(0x12)),
        ]),
        GattServiceInfo(uuid: "FFE0", characteristics: [
            GattCharacteristicInfo(serviceUuid: "FFE0", uuid: "FFE1", properties: .of(0x1C), isNotifying: true),
        ]),
    ]
    let value = GattValue(serviceUuid: "180F", uuid: "2A19", hex: "5A", decoded: "90 %", source: .read, at: .now)
    return ScrollView {
        VStack(alignment: .leading) {
            GattSection(isConnected: true, isDiscovering: false, services: services, lastRead: value, gattError: nil,
                        mtu: 185, onRead: { _ in }, onNotify: { _, _ in }, onWrite: { _, _, _ in })
            ReceivedDataSection(received: [value], onClear: {})
        }
        .padding()
    }
}
