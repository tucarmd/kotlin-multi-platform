import Foundation

/// App-wide strings (Rule 5.3, Swift version). Only the app frame labels — tabs + title.
/// Each screen's strings live next to the screen: `UI/Permission/PermissionStrings.swift`, `UI/Scanner/ScannerStrings.swift`.
enum AppStrings {
    static let tabPermissions = "Permissions"
    static let tabScanner = "Scanner"
    static let tabMedia = "Media"
    static let tabApi = "API"
    static let tabMonitor = "Monitor"
}
