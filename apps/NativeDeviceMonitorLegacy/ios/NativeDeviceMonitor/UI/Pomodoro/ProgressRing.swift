import SwiftUI

/// A circle that fills clockwise from 12 o'clock. `progress` 0…1. Pure drawing, knows nothing about Pomodoro.
struct ProgressRing<Label: View>: View {
    let progress: Double
    var color: Color = AppColor.primary
    var lineWidth: CGFloat = 14
    @ViewBuilder var label: () -> Label

    var body: some View {
        ZStack {
            Circle().stroke(AppColor.disabled, lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: min(max(progress, 0), 1))
                .stroke(color, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))   // trim starts at 3 o'clock → turn back to 12
                .animation(.linear(duration: 0.25), value: progress)
            label()
        }
    }
}

#Preview {
    ProgressRing(progress: 0.35) { Text("16:15").font(.largeTitle.monospacedDigit()) }
        .frame(width: 220, height: 220)
        .padding()
}
