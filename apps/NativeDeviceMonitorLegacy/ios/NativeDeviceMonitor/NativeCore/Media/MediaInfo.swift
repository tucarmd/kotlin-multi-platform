import Foundation

/// Where the image came from. Mirrors `android/.../nativecore/media/MediaInfo.kt`.
enum MediaSource: String { case camera, library }

/// Day 10 · `getMediaInfo()` returns this. `nil` = the system doesn't tell.
/// `width/height` = pixels in display orientation (UIImage already rotated per EXIF).
struct MediaInfo: Equatable {
    var name: String?
    var sizeBytes: Int?
    var mimeType: String?
    var width: Int?
    var height: Int?
    var source: MediaSource
}

/// 1536 → "1.5 KB" (1 KB = 1024 B, like Android). Pure function — unit tested.
func formatBytes(_ bytes: Int) -> String {
    if bytes < 1024 { return "\(bytes) B" }
    if bytes < 1024 * 1024 { return String(format: "%.1f KB", Double(bytes) / 1024) }
    return String(format: "%.1f MB", Double(bytes) / (1024 * 1024))
}
