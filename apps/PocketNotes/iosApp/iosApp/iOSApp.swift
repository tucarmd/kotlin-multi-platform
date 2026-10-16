import SwiftUI
import Shared

/// [App 2 · B4] App entry: starts Koin once (which opens the SQLite database), then builds the one model both
/// screens share. Android does the same in PocketNotesApp.kt + App.kt.
@main
struct iOSApp: App {
    @State private var model: NotesModel

    init() {
        doInitKoin()                                              // Koin.kt: initKoin(), same modules as Android
        _model = State(initialValue: NotesModel(core: notesCore())) // KoinIos.kt: notesCore() with its own MainScope
    }

    var body: some Scene {
        WindowGroup {
            RootView(model: model)
        }
    }
}
