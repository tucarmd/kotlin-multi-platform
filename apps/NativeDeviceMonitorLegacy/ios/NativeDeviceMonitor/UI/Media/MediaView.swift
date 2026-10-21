import AVFoundation
import PhotosUI
import SwiftUI

/// Day 10 — Media tab: Capture / Pick → show the image + info. Mirrors `android/.../ui/media/MediaScreen.kt`.
///
/// ```
///  MediaView
///   └─ ScrollView > VStack
///       ├─ AppCard "Camera"         permission · [Request] / [Capture photo] / [Open settings]
///       │                            └─ .fullScreenCover { CameraPicker }
///       ├─ AppCard "Media library"  PhotosPicker [Pick from library]
///       └─ AppCard "Selected image" Image + info table · [Clear]
/// ```
///
/// All AVFoundation / PhotosUI work goes through `MediaManager` (Day 11: the View holds no device logic).
struct MediaView: View {

    @Environment(\.scenePhase) private var scenePhase

    @State private var cameraStatus = MediaManager.cameraStatus
    @State private var showCamera = false
    @State private var pickerItem: PhotosPickerItem?

    @State private var image: UIImage?
    @State private var info: MediaInfo?
    @State private var loading = false
    @State private var message = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.md) {
                AppHintText(text: MediaStrings.hint, showBullet: false)
                cameraCard
                libraryCard
                resultCard
            }
            .padding(AppSpacing.md)
        }
        .background(AppColor.contentBackground)
        .navigationTitle(MediaStrings.title)
        .navigationBarTitleDisplayMode(.inline)
        // Back from Settings → re-read the permission (iOS fires no event when the user changes it outside the app).
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { cameraStatus = MediaManager.cameraStatus }
        }
        .fullScreenCover(isPresented: $showCamera) {
            CameraPicker { captured in
                showCamera = false
                if let captured {
                    image = captured
                    info = MediaManager.info(fromCamera: captured)
                    message = ""
                } else {
                    message = MediaStrings.captureCancelled
                }
            }
            .ignoresSafeArea()
        }
        // Picked a photo → selection changes → load. If the user cancels the selection does NOT change → nothing happens (intended).
        .onChange(of: pickerItem) { _, item in
            guard let item else { return }
            Task { await loadPicked(item) }
        }
    }

    // MARK: - Cards

    private var cameraCard: some View {
        AppCard(title: MediaStrings.cameraTitle) {
            if !MediaManager.isCameraAvailable {
                AppHintText(text: MediaStrings.noCamera)
            } else {
                switch cameraStatus {
                case .authorized: AppHintText(text: MediaStrings.cameraGranted)
                case .denied, .restricted:
                    Text(MediaStrings.cameraDenied).font(.system(size: 13)).foregroundStyle(AppColor.danger)
                default: AppHintText(text: MediaStrings.cameraNotGranted)
                }
            }
            AppPrimaryButton(
                title: cameraStatus == .authorized ? MediaStrings.capture : MediaStrings.requestCamera,
                action: { Task { await captureTapped() } },
                isEnabled: MediaManager.isCameraAvailable && cameraStatus != .denied && cameraStatus != .restricted
            )
            if cameraStatus == .denied {
                AppSecondaryButton(title: MediaStrings.openSettings) { PermissionService.shared.openAppSettings() }
            }
        }
    }

    private var libraryCard: some View {
        AppCard(title: MediaStrings.libraryTitle) {
            AppHintText(text: MediaStrings.pickHint)
            PhotosPicker(selection: $pickerItem, matching: .images) {
                Text(MediaStrings.pick)
                    .font(.system(size: 16, weight: .semibold))
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .foregroundStyle(.white)
                    .background(AppColor.primary, in: Capsule())
            }
        }
    }

    private var resultCard: some View {
        AppCard(title: MediaStrings.resultTitle) {
            if !message.isEmpty {
                Text(message).font(.system(size: 13)).foregroundStyle(AppColor.textSecondary)
            }
            if loading {
                AppHintText(text: MediaStrings.loading)
            } else if image == nil {
                AppHintText(text: MediaStrings.nothingYet)
            }
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: .infinity, maxHeight: 320)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            if let info {
                VStack(spacing: 2) {
                    ForEach(MediaStrings.infoRows(info), id: \.0) { k, v in
                        HStack {
                            Text(k).font(.system(size: 13)).foregroundStyle(AppColor.textSecondary)
                            Spacer()
                            Text(v).font(.system(size: 13, design: .monospaced)).foregroundStyle(AppColor.textPrimary)
                        }
                    }
                }
            }
            if image != nil {
                AppSecondaryButton(title: MediaStrings.clear) {
                    image = nil; info = nil; pickerItem = nil; message = ""
                }
            }
        }
    }

    // MARK: - Actions

    /// One button: no permission → request → if granted open the camera right away (tapping means they want a photo).
    private func captureTapped() async {
        let granted = await MediaManager.requestCameraPermission()
        cameraStatus = MediaManager.cameraStatus
        if granted { showCamera = true }
    }

    private func loadPicked(_ item: PhotosPickerItem) async {
        loading = true
        defer { loading = false }
        if let (picked, pickedInfo) = await MediaManager.load(item) {
            image = picked
            info = pickedInfo
            message = ""
        } else {
            message = MediaStrings.loadFailed
        }
    }
}

#Preview {
    NavigationStack { MediaView() }
}
