import SwiftUI

/// primary button: full width, pill-shaped, with disabled and loading.
///
/// In the design these are "Search" and "Save vehicle" — both shown **grey disabled**,
/// i.e. the design treats disabled as a NORMAL state (form not filled yet), not an exception.
/// So it's a first-class parameter, not something bolted on later.
///
/// `isLoading` disables the button: tapping again while sending sends twice — blocking it in the component
/// means no screen can forget. Mirrors Android `ui/common/AppPrimaryButton.kt`.
struct AppPrimaryButton: View {
    let title: String
    let action: () -> Void
    var isEnabled: Bool = true
    var isLoading: Bool = false

    private var active: Bool { isEnabled && !isLoading }

    var body: some View {
        Button(action: action) {
            Group {
                if isLoading {
                    ProgressView().tint(.white)
                } else {
                    Text(title).font(.system(size: 16, weight: .semibold))
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .foregroundStyle(active ? Color.white : AppColor.textSecondary)
            .background(active ? AppColor.primary : AppColor.disabled)
            .clipShape(Capsule())
        }
        .disabled(!active)
    }
}

/// Secondary button: same shape, outlined — Cancel, Back. Same file as the primary button because the two
/// must always change shape together.
struct AppSecondaryButton: View {
    let title: String
    let action: () -> Void
    var isEnabled: Bool = true

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .foregroundStyle(isEnabled ? AppColor.primary : AppColor.textSecondary)
                .overlay(Capsule().stroke(isEnabled ? AppColor.primary : AppColor.disabled, lineWidth: 1))
                // No background → without this only the glyphs are tappable; the whole pill should be.
                .contentShape(Capsule())
        }
        .disabled(!isEnabled)
    }
}

#Preview {
    VStack(spacing: AppSpacing.sm) {
        AppPrimaryButton(title: "Save vehicle", action: {})
        AppPrimaryButton(title: "Save vehicle", action: {}, isEnabled: false)
        AppPrimaryButton(title: "Saving…", action: {}, isLoading: true)
        AppSecondaryButton(title: "Cancel", action: {})
    }
    .padding()
    .background(AppColor.contentBackground)
}
