import SwiftUI

/// LAYER 1: the WHOLE APP's color palette.
///
/// Source: copied verbatim from the CarMD Connect Ionic app — `src/theme/variables.scss`.
/// Change it here and you MUST change `ui/theme/AppColor.kt` on Android, otherwise the 2 apps drift apart.
///
/// An `enum` with no cases = a namespace of constants, nobody can call `AppColor()` (unlike a `struct`).
enum AppColor {

    // --- Brand ---
    /// `--ion-color-primary`
    static let primary = Color(hex: 0x1A75CF)
    /// `--ion-color-secondary`
    static let secondary = Color(hex: 0x4B9CDF)

    // --- Backgrounds ---
    /// `--app-content-bg` — very light blue, background of every page (not white).
    static let contentBackground = Color(hex: 0xEFF5FF)
    static let surface = Color.white

    // --- Header ---
    /// Title + bar buttons on the blue `primary` nav bar of every screen (set once in `AppHeader.apply()`).
    static let headerText = Color.black

    // --- Text ---
    /// `--ion-text-color`
    static let textPrimary = Color(hex: 0x2F3134)
    /// `--app-color-light-1` (the SCSS literally says "Secondary Text")
    static let textSecondary = Color(hex: 0xA2A7AB)

    // --- States ---
    static let danger = Color(hex: 0xEC5B5B)
    static let success = Color(hex: 0x38D298)
    static let warning = Color(hex: 0xF18641)

    // --- Borders / disabled ---
    static let disabled = Color(hex: 0xDFE4E8)
    /// `--app-color-light-2`
    static let border = Color(hex: 0xCBCFD2)
    /// `--app-color-splitter-line`
    static let divider = Color(hex: 0xD1DCED)

    // MARK: - Raw data box (not in CarMD)
    /// Terminal-style "Received Data" background — from the Learning Plan §4 mockup, not the SCSS.
    static let terminalBackground = Color(hex: 0x111827)
    /// Green hex text on the terminal background.
    static let terminalText = Color(hex: 0x4ADE80)
}

extension Color {
    /// SwiftUI has no built-in init from a hex code (Compose has `Color(0xFF1A75CF)`) → hand-written so CSS colors copy over exactly.
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}
