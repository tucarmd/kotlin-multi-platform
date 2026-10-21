import SwiftUI

/// MVVM lesson 01 — the **View**: draws what `PomodoroViewModel` says and forwards taps. No time math, no formatting,
/// no business `if` in here.
///
/// ```
///  PomodoroView
///   └─ ScrollView > VStack
///       ├─ AppHintText (lesson hint)
///       ├─ AppCard: phase title · ProgressRing(timeText) · "Focus sessions today: N"
///       ├─ AppPrimaryButton  Start / Pause / Resume
///       └─ HStack  AppSecondaryButton Reset · AppSecondaryButton Skip
///  .task(id: isRunning)   ← tick loop, only while running; SwiftUI cancels it when the screen goes away
///  .onChange(scenePhase)  ← back to .active → appBecameActive() catches up after lock / background
/// ```
///
/// The ViewModel is created by the parent (`MonitorView`) and passed in — so the timer keeps going when you pop back.
struct PomodoroView: View {

    @State private var viewModel: PomodoroViewModel
    @Environment(\.scenePhase) private var scenePhase

    init(viewModel: PomodoroViewModel) {
        _viewModel = State(initialValue: viewModel)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: AppSpacing.md) {
                AppHintText(text: PomodoroStrings.hint, showBullet: false)
                timerCard
                AppPrimaryButton(title: viewModel.primaryTitle, action: viewModel.primaryTapped)
                HStack(spacing: AppSpacing.sm) {
                    AppSecondaryButton(title: PomodoroStrings.reset, action: viewModel.reset, isEnabled: viewModel.canReset)
                    AppSecondaryButton(title: PomodoroStrings.skip, action: viewModel.skip)
                }
            }
            .padding(AppSpacing.md)
        }
        .background(AppColor.contentBackground)
        .navigationTitle(PomodoroStrings.title)
        .navigationBarTitleDisplayMode(.inline)
        .task(id: viewModel.isRunning) {
            guard viewModel.isRunning else { return }
            while !Task.isCancelled {
                viewModel.tick()
                try? await Task.sleep(for: .milliseconds(250))
            }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { viewModel.appBecameActive() }
        }
    }

    private var timerCard: some View {
        AppCard {
            VStack(spacing: AppSpacing.md) {
                Text(viewModel.phaseTitle)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(viewModel.isFocus ? AppColor.primary : AppColor.success)
                ProgressRing(progress: viewModel.progress, color: viewModel.isFocus ? AppColor.primary : AppColor.success) {
                    Text(viewModel.timeText)
                        .font(.system(size: 48, weight: .bold).monospacedDigit())
                        .foregroundStyle(AppColor.textPrimary)
                }
                .frame(width: 220, height: 220)
                Text(viewModel.todayText)
                    .font(.system(size: 13))
                    .foregroundStyle(AppColor.textSecondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, AppSpacing.sm)
        }
    }
}

#Preview {
    NavigationStack {
        PomodoroView(viewModel: PomodoroViewModel(store: InMemoryPomodoroSessionStore(dates: [.now, .now])))
    }
}
