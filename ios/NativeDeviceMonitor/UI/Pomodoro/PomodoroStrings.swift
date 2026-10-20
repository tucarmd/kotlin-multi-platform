import Foundation

/// Pomodoro screen strings (MVVM lesson 01). iOS only — no Android twin yet.
enum PomodoroStrings {
    static let title = "Pomodoro"
    static let hint = "MVVM lesson 01 — View shows, ViewModel decides what to show, Model holds the rules. "
        + "Lock the phone while it runs and come back: the time is still right."
    static let openButton = "Open Pomodoro timer"

    static let focus = "Focus"
    static let rest = "Break"

    static let start = "Start"
    static let pause = "Pause"
    static let resume = "Resume"
    static let reset = "Reset"
    static let skip = "Skip"

    static func today(_ n: Int) -> String { "Focus sessions today: \(n)" }
}
