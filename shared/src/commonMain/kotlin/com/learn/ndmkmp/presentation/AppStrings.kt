package com.learn.ndmkmp.presentation

/**
 * App-wide strings (Rule 5.3: no strings.xml). Only the app frame labels — tabs + title.
 * Each tab's strings live next to its ViewModel (`presentation/scanner/ScannerStrings.kt`…) → Compose and SwiftUI show the same text.
 */
object AppStrings {
    const val APP_TITLE = "Native Device Monitor"
    const val TAB_PERMISSIONS = "Permissions"
    const val TAB_SCANNER = "Scanner"
    const val TAB_MEDIA = "Media"
    const val TAB_API = "API"
    const val TAB_MONITOR = "Monitor"
}
