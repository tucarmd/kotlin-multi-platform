import SwiftUI
import Shared

/// [App 2 · B6] Where the app can go. `.edit(0)` means "new note". Android twin: Routes in App.kt.
enum Route: Hashable {
    case edit(Int64)
}

/// [App 2 · B6] NavigationStack: list → edit. Both screens get the same NotesModel, like Android's single ViewModel.
struct RootView: View {
    let model: NotesModel
    @State private var path: [Route] = []

    var body: some View {
        NavigationStack(path: $path) {
            NoteListView(model: model, onNew: { path.append(.edit(0)) })
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .edit(let id):
                        NoteEditView(model: model, noteId: id)
                    }
                }
        }
        .tint(Color(red: 0.95, green: 0.63, blue: 0.0))
    }
}
