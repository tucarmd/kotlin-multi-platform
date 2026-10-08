import AVFoundation
import PhotosUI
import SwiftUI
import UIKit

/// Day 10 — Camera & Media Manager. API theo Word: `requestCameraPermission()` · `capturePhoto()`
/// (= `CameraPicker` in the UI) · `pickMedia()` (= `PhotosPicker` in the UI) · `mediaInfo(...)`.
///
/// ```
///  Capture: View ─ requestCameraPermission() ─► AVCaptureDevice.requestAccess
///               └ .fullScreenCover { CameraPicker } ─► UIImage? (nil = Cancel) ─► info(fromCamera:)
///  Select : View ─ PhotosPicker(selection:) ─► PhotosPickerItem ─► load(_:) ─► (UIImage, MediaInfo)?
/// ```
///
/// Unlike Android:
/// - No FileProvider/uri: the camera returns a `UIImage` straight in memory.
/// - `PhotosPicker` runs outside the app process → **no Photos permission needed** and the original file name isn't exposed
///   (name = nil, unless you request full Photo Library access — out of scope). Android's Photo Picker still gives the name.
/// - The Simulator **has no camera** → `isCameraAvailable == false`, only Pick can be tested.
enum MediaManager {

    /// `UIImagePickerController.isSourceTypeAvailable(.camera)` — false on the Simulator / iPads without a camera.
    static var isCameraAvailable: Bool { UIImagePickerController.isSourceTypeAvailable(.camera) }

    static var cameraStatus: AVAuthorizationStatus { AVCaptureDevice.authorizationStatus(for: .video) }

    /// `true` = camera allowed. `.notDetermined` → shows the system dialog (only once ever);
    /// `.denied` → iOS does NOT ask again, only the Settings route remains (unlike Android, which allows asking once more).
    static func requestCameraPermission() async -> Bool {
        switch cameraStatus {
        case .authorized: return true
        case .notDetermined: return await AVCaptureDevice.requestAccess(for: .video)
        default: return false
        }
    }

    /// Captured photo: no file yet → size = JPEG size if saved at quality 0.9.
    static func info(fromCamera image: UIImage) -> MediaInfo {
        MediaInfo(name: nil, sizeBytes: image.jpegData(compressionQuality: 0.9)?.count, mimeType: "image/jpeg",
                  width: pixelWidth(image), height: pixelHeight(image), source: .camera)
    }

    /// Load the image from PhotosPicker. `loadTransferable` may take a few seconds (iCloud photos must download) → async.
    /// Error / not an image → `nil`, the View reports it, no crash.
    static func load(_ item: PhotosPickerItem) async -> (UIImage, MediaInfo)? {
        guard let data = try? await item.loadTransferable(type: Data.self),
              let image = UIImage(data: data) else { return nil }
        let type = item.supportedContentTypes.first
        let info = MediaInfo(name: nil, sizeBytes: data.count, mimeType: type?.preferredMIMEType,
                             width: pixelWidth(image), height: pixelHeight(image), source: .library)
        return (image, info)
    }

    // UIImage.size is in points = pixels / scale — multiply back to get real pixels.
    private static func pixelWidth(_ i: UIImage) -> Int { Int(i.size.width * i.scale) }
    private static func pixelHeight(_ i: UIImage) -> Int { Int(i.size.height * i.scale) }
}
