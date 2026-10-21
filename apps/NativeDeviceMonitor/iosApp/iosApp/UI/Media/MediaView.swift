import SwiftUI
import UIKit
import Shared

/// Day 10 — Media tab in SwiftUI: Capture / Pick → image + info. Draws `MediaViewModel.state`.
/// Mirrors Android `ui/media/MediaScreen.kt`.
///
/// ```
///  MediaView
///   └─ ScrollView > VStack
///       ├─ AppCard "Camera"         permission line · [Request] / [Capture photo] / [Open settings]
///       ├─ AppCard "Media library"  [Pick from library]
///       └─ AppCard "Selected image" image + info table · [Clear]
/// ```
///
/// The ViewModel hands over the picture as encoded bytes (`PickedImage.preview`); turning them into a `UIImage` is
/// the View's job — `IosBridgeKt.previewData` copies the Kotlin `ByteArray` into `Data`.
struct MediaView: View {
    private let vm: MediaViewModel
    @StateObject private var model: Observed<MediaUiState>

    init(vm: MediaViewModel) {
        self.vm = vm
        _model = StateObject(wrappedValue: Observed(vm.state))
    }

    private var strings: MediaStrings { MediaStrings.shared }

    var body: some View {
        let state = model.value
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.md) {
                AppHintText(text: strings.HINT, showBullet: false)

                AppCard(title: strings.CAMERA_TITLE) {
                    if state.cameraTextIsError {
                        Text(state.cameraText).font(.system(size: 13)).foregroundStyle(AppColor.danger)
                    } else {
                        AppHintText(text: state.cameraText)
                    }
                    AppPrimaryButton(title: state.captureButtonText, action: { vm.onCaptureClick() }, isEnabled: state.captureEnabled)
                    if state.showOpenSettings {
                        AppSecondaryButton(title: strings.OPEN_SETTINGS) { vm.openSettings() }
                    }
                }

                AppCard(title: strings.LIBRARY_TITLE) {
                    AppHintText(text: strings.PICK_HINT)
                    AppPrimaryButton(title: strings.PICK, action: { vm.onPickClick() }, isEnabled: !state.loading)
                }

                AppCard(title: strings.RESULT_TITLE) {
                    if !state.message.isEmpty {
                        Text(state.message).font(.system(size: 13)).foregroundStyle(AppColor.textSecondary)
                    }
                    if state.loading {
                        AppHintText(text: strings.LOADING)
                    } else if state.image == nil {
                        AppHintText(text: strings.NOTHING_YET)
                    }
                    if let image = state.image, let data = IosBridgeKt.previewData(image: image), let ui = UIImage(data: data) {
                        Image(uiImage: ui)
                            .resizable()
                            .scaledToFit()
                            .frame(maxWidth: .infinity, maxHeight: 320)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    InfoTable(rows: state.infoRows)
                    if state.image != nil {
                        AppSecondaryButton(title: strings.CLEAR) { vm.clearImage() }
                    }
                }
            }
            .padding(AppSpacing.md)
        }
        .background(AppColor.contentBackground)
    }
}
