import Foundation
import Observation
import Shared

/// [App 2 · B6] NotesModel: bridges the Kotlin NotesCore to SwiftUI. The iOS twin of NotesViewModel on Android.
///
/// Why @Observable: SwiftUI redraws only the views that read a property that changed.
/// The Kotlin StateFlow is read with `for await` because SKIE turns it into an AsyncSequence.
/// No logic here: every action forwards to NotesCore, which is tested in shared/commonTest.
///
/// Used by: NoteListView, NoteEditView. Uses: NotesCore from `Shared` (built by `notesCore()`).
@MainActor @Observable
final class NotesModel {
    private(set) var state: NotesState
    // PLATFORM: Android's core lives in viewModelScope and stops by itself. Here the core owns a MainScope,
    // so whoever drops the model must call close(); deinit does it.
    @ObservationIgnored private let core: NotesCore

    init(core: NotesCore) {
        self.core = core
        self.state = core.state.value
        Task { [weak self] in
            for await state in core.state { self?.state = state }
        }
    }

    deinit { core.close() }

    func search(_ text: String) { core.search(text: text) }
    func refreshClock() { core.refreshClock() }
    /// PLATFORM: Kotlin Long is Int64 in Swift.
    func find(_ id: Int64) -> Note? { core.find(id: id) }
    func save(id: Int64, title: String, body: String, pinned: Bool) {
        core.save(id: id, title: title, body: body, pinned: pinned)
    }
    func togglePin(_ id: Int64) { core.togglePin(id: id) }
    func delete(_ id: Int64) { core.delete(id: id) }
    func undoDelete() { core.undoDelete() }
    func dismissUndo() { core.dismissUndo() }
}
