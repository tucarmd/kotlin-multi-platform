import Foundation

/// Media tab strings (Day 10). Mirrors `android/.../ui/media/MediaStrings.kt`.
enum MediaStrings {
    static let title = "Media"
    static let hint = "Day 10 — capture a photo with the system camera or pick one from the library, then show it with its info."

    static let cameraTitle = "Camera"
    static let capture = "Capture photo"
    static let requestCamera = "Request camera permission"
    static let openSettings = "Open app settings"
    static let cameraGranted = "Camera permission: GRANTED"
    static let cameraNotGranted = "Camera permission: not granted — Capture asks first."
    static let cameraDenied = "Camera permission denied. iOS will not ask again — allow it in Settings."
    static let noCamera = "No camera on this device (Simulator) — use Pick from library."

    static let libraryTitle = "Media library"
    static let pick = "Pick from library"
    static let pickHint = "System Photos picker — no Photos permission needed, the app only gets the photo you choose (no file name)."

    static let resultTitle = "Selected image"
    static let nothingYet = "No image yet."
    static let loading = "Loading image…"
    static let captureCancelled = "Capture cancelled — nothing changed."
    static let loadFailed = "Could not read the image."
    static let clear = "Clear"

    static func infoRows(_ info: MediaInfo) -> [(String, String)] {
        let dims: String
        if let w = info.width, let h = info.height { dims = "\(w) × \(h) px" } else { dims = "null" }
        return [
            ("source", info.source.rawValue),
            ("name", info.name ?? "null"),
            ("size", info.sizeBytes.map { "\(formatBytes($0)) (\($0) B)" } ?? "null"),
            ("mime", info.mimeType ?? "null"),
            ("dimensions", dims),
        ]
    }
}
