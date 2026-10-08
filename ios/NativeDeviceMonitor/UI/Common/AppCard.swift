import SwiftUI

/// a white rounded block with a shadow, sitting on the page's light-blue background.
///
/// The most repeated shape in the CarMD design. In Ionic it's `ion-card`;
/// here it's wrapped to lock corner radius + shadow + padding to the tokens, screens don't set their own.
///
/// Generic `Content` + `@ViewBuilder` = `<ng-content>`. Mirrors `ui/common/AppCard.kt`.
struct AppCard<Content: View>: View {
    var title: String?
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.sm) {
            if let title {
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(AppColor.textPrimary)
            }
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(AppSpacing.md)
        .background(AppColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: AppRadius.card, style: .continuous))
        // `--box-shadow-1: 0 4px 12px rgba(0,0,0,.12)` in CarMD — SwiftUI takes all 3 parameters
        // (radius/x/y) so it matches the CSS more closely than Compose, which only has one `elevation` number.
        .shadow(color: .black.opacity(0.12), radius: 6, x: 0, y: 4)
    }
}

#Preview {
    VStack(spacing: AppSpacing.md) {
        AppCard(title: "With title") { Text("Body content") }
        AppCard { Text("No title") }
    }
    .padding()
    .background(AppColor.contentBackground)
}
