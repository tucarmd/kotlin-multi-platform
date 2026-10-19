import SwiftUI

/// LAYER 1: shared spacing scale. Mirrors Android `ui/theme/AppSpacing.kt`.
///
/// Why: without this file Views scatter `.padding(16)`, `.padding(8)` at whim — the 2 platforms
/// easily drift a few pt apart unnoticed until 2 screenshots are put side by side.
enum AppSpacing {
    static let xs: CGFloat = 4
    static let sm: CGFloat = 8
    static let md: CGFloat = 16
    /// CarMD's `--app-page-padding: 1.5rem` — left/right margin of every page.
    static let lg: CGFloat = 24
    static let xl: CGFloat = 32
}

/// Corner radii. CarMD doesn't declare `--app-control-radius` as a number in `variables.scss`, so the 3 values
/// below are MEASURED FROM THE DESIGN, not copied from CSS — noted so whoever checks against Figma later knows
/// which parts are guesses.
enum AppRadius {
    static let card: CGFloat = 16
    static let field: CGFloat = 10
    static let pill: CGFloat = 28
}
