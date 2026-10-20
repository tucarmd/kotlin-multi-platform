import SwiftUI
import UIKit

/// The blue header with white text on EVERY screen — Permissions, Scanner, Media, API, Monitor and pushed screens (Pomodoro).
///
/// Set ONCE at app start through UIKit's `UINavigationBarAppearance`: the SwiftUI way (`.toolbarBackground` +
/// `.toolbarColorScheme` per View) had to be repeated on each screen, only the Scanner had it → the other headers were
/// transparent. Android: `CenterAlignedTopAppBar` colors in `MainActivity`.
enum AppHeader {

    static func apply() {
        let text = UIColor(AppColor.headerText)
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(AppColor.primary)
        appearance.shadowColor = .clear
        appearance.titleTextAttributes = [.foregroundColor: text]
        appearance.largeTitleTextAttributes = [.foregroundColor: text]
        // Back button ("< Monitor") — white arrow + white text, otherwise it takes the blue tint and disappears on the blue bar.
        let back = UIImage(systemName: "chevron.backward")?.withTintColor(text, renderingMode: .alwaysOriginal)
        appearance.setBackIndicatorImage(back, transitionMaskImage: back)
        appearance.backButtonAppearance.normal.titleTextAttributes = [.foregroundColor: text]
        appearance.buttonAppearance.normal.titleTextAttributes = [.foregroundColor: text]

        let bar = UINavigationBar.appearance()
        bar.standardAppearance = appearance
        bar.compactAppearance = appearance
        bar.scrollEdgeAppearance = appearance   // at the top of a scroll view iOS uses THIS one (transparent by default)
        bar.tintColor = text
    }
}

extension ToolbarContent {
    /// iOS 26 wraps every bar button in a light "glass" circle → a white icon on it disappears. Hide the circle so the icon
    /// sits straight on the blue header like the title. iOS 17–18 has no circle → nothing to do.
    @ToolbarContentBuilder
    func withoutGlassBackground() -> some ToolbarContent {
        if #available(iOS 26, *) {
            sharedBackgroundVisibility(.hidden)
        } else {
            self
        }
    }
}
