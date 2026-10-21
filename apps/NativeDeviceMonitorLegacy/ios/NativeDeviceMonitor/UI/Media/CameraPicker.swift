import SwiftUI
import UIKit

/// Day 10 — wraps `UIImagePickerController` (UIKit) for use in SwiftUI. SwiftUI has no built-in photo-capturing camera view
/// (PhotosPicker only PICKS photos). The Android equivalent: `ActivityResultContracts.TakePicture()`.
///
/// `UIViewControllerRepresentable` = the UIKit → SwiftUI bridge; `Coordinator` = where UIKit delegate callbacks land.
/// `onFinish(nil)` = the user tapped Cancel.
struct CameraPicker: UIViewControllerRepresentable {
    let onFinish: (UIImage?) -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera     // calling this when MediaManager.isCameraAvailable == false crashes → the View already guards it
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onFinish: onFinish) }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onFinish: (UIImage?) -> Void
        init(onFinish: @escaping (UIImage?) -> Void) { self.onFinish = onFinish }

        func imagePickerController(_ picker: UIImagePickerController,
                                   didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            onFinish(info[.originalImage] as? UIImage)
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) { onFinish(nil) }
    }
}
