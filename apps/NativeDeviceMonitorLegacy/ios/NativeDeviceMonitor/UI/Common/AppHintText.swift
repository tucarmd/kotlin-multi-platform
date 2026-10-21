import SwiftUI

/// a small grey hint line with a bullet in front.
///
/// In the design: "• Report numbers are on the top of the page (e.g. top right or top center)".
///
/// Why compose `HStack { Text("•"); Text(...) }` instead of prefixing the string with "• ":
/// that way, wrapped text snaps back to the margin instead of lining up with the line above.
/// `alignment: .top` keeps the bullet level with the FIRST line when the content spans several lines.
struct AppHintText: View {
    let text: String
    var showBullet: Bool = true

    var body: some View {
        HStack(alignment: .top, spacing: AppSpacing.sm) {
            if showBullet {
                Text("•")
                    .font(.system(size: 13))
                    .foregroundStyle(AppColor.textSecondary)
            }
            Text(text)
                .font(.system(size: 13))
                .foregroundStyle(AppColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)  // allow wrapping, don't truncate
            Spacer(minLength: 0)
        }
    }
}

#Preview {
    VStack(alignment: .leading, spacing: AppSpacing.xs) {
        AppHintText(text: "Report numbers are on the top of the page (e.g. top right or top center)")
        AppHintText(text: "Without a bullet", showBullet: false)
    }
    .padding()
    .background(AppColor.contentBackground)
}
