import AVFoundation
import PhotosUI
import UIKit
import UniformTypeIdentifiers
import Shared

/// P12b — the Apple-SDK half of the Media tab, in Swift. Implements Kotlin `IosMediaNative`; Kotlin `IosMediaPicker`
/// turns the callbacks into `suspend` for `MediaViewModel`.
///
/// ```
///  capture → UIImagePickerController(.camera)  ─┐
///  pick    → PHPickerViewController (images)   ─┴─► UIImage → downscale 1024 px → JPEG preview + original info
///                                                    done(NativeImage) · Cancel → done(nil)
/// ```
/// Presented with UIKit from the top view controller (not a SwiftUI sheet): the call comes from the shared ViewModel,
/// which knows nothing about SwiftUI. PHPicker runs outside the app → no Photos permission, no file name.
final class MediaNative: NSObject, IosMediaNative {

    /// Keep the delegate alive while the picker is on screen (UIKit holds delegates weakly).
    private var pending: PickerDelegate?

    func hasCamera() -> Bool { UIImagePickerController.isSourceTypeAvailable(.camera) }

    func cameraGranted() -> Bool { AVCaptureDevice.authorizationStatus(for: .video) == .authorized }

    func requestCamera(done: @escaping (KotlinBoolean) -> Void) {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async { done(KotlinBoolean(bool: granted)) }
        }
    }

    func capture(done: @escaping (NativeImage?) -> Void) {
        guard hasCamera(), let top = Self.topViewController() else { done(nil); return }
        let delegate = PickerDelegate { [weak self] image, mime, size in
            self?.pending = nil
            done(image.map { Self.nativeImage($0, mimeType: mime, sizeBytes: size) })
        }
        pending = delegate
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = delegate
        top.present(picker, animated: true)
    }

    func pick(done: @escaping (NativeImage?) -> Void) {
        guard let top = Self.topViewController() else { done(nil); return }
        let delegate = PickerDelegate { [weak self] image, mime, size in
            self?.pending = nil
            done(image.map { Self.nativeImage($0, mimeType: mime, sizeBytes: size) })
        }
        pending = delegate
        var config = PHPickerConfiguration()
        config.filter = .images
        config.selectionLimit = 1
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = delegate
        top.present(picker, animated: true)
    }

    func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }

    // MARK: - helpers

    /// Downscale to ≤ 1024 px for the preview (a 12 MP photo as a Kotlin ByteArray is ~5 MB for nothing); info keeps
    /// the ORIGINAL pixel size. `UIImage.size` is in points → × scale = pixels.
    private static func nativeImage(_ image: UIImage, mimeType: String?, sizeBytes: Int) -> NativeImage {
        let w = Int(image.size.width * image.scale), h = Int(image.size.height * image.scale)
        let maxSide: CGFloat = 1024
        let ratio = min(1, maxSide / CGFloat(max(w, h)))
        let target = CGSize(width: CGFloat(w) * ratio, height: CGFloat(h) * ratio)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let small = UIGraphicsImageRenderer(size: target, format: format).image { _ in
            image.draw(in: CGRect(origin: .zero, size: target))   // draw() also applies the EXIF orientation
        }
        return NativeImage(preview: small.jpegData(compressionQuality: 0.85),
                           sizeBytes: Int64(sizeBytes), mimeType: mimeType, width: Int32(w), height: Int32(h))
    }

    static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }
        var top = window?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

/// One delegate for both pickers → `(UIImage?, mime, original byte size)`; nil image = Cancel.
private final class PickerDelegate: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate,
    PHPickerViewControllerDelegate {

    private let finish: (UIImage?, String?, Int) -> Void

    init(finish: @escaping (UIImage?, String?, Int) -> Void) { self.finish = finish }

    // Camera — the photo is in memory, no file yet: size = what a JPEG at 0.9 would weigh (same as the old app).
    func imagePickerController(_ picker: UIImagePickerController,
                               didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
        let image = info[.originalImage] as? UIImage
        picker.dismiss(animated: true)
        finish(image, "image/jpeg", image?.jpegData(compressionQuality: 0.9)?.count ?? 0)
    }

    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true)
        finish(nil, nil, 0)
    }

    // Library — load the original bytes (may download from iCloud, so it's async), then back to main.
    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true)
        guard let provider = results.first?.itemProvider,
              let typeId = provider.registeredTypeIdentifiers.first(where: { UTType($0)?.conforms(to: .image) == true })
        else { finish(nil, nil, 0); return }
        let mime = UTType(typeId)?.preferredMIMEType
        provider.loadDataRepresentation(forTypeIdentifier: typeId) { data, _ in
            let image = data.flatMap(UIImage.init(data:))
            DispatchQueue.main.async { self.finish(image, mime, data?.count ?? 0) }
        }
    }
}
