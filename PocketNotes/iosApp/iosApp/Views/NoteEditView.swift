import SwiftUI
import Shared

/// [App 2 · B6] The editor: title, body, pin and delete buttons. Saves when it disappears (Back button or swipe back).
/// Android twin: NoteEditScreen.kt. `noteId` 0 means a new note.
///
/// Why the draft is @State here and not in the core: it is only "what is in the fields right now". The core gets one
/// save() when the user leaves, and the shared rule planSave decides insert / update / delete / nothing.
struct NoteEditView: View {
    let model: NotesModel
    let noteId: Int64

    @State private var title = ""
    @State private var text = ""
    @State private var pinned = false
    @State private var loaded = false
    @State private var deleted = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            TextField(L("title_hint"), text: $title)
                .font(.title2.bold())
            TextEditor(text: $text)
                .overlay(alignment: .topLeading) {
                    if text.isEmpty {
                        Text(L("body_hint")).foregroundStyle(.tertiary)
                            .padding(.top, 8).padding(.leading, 5)
                            .allowsHitTesting(false)
                    }
                }
        }
        .padding(.horizontal)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                Button { pinned.toggle() } label: {
                    Image(systemName: pinned ? "pin.fill" : "pin")
                }
                .accessibilityLabel(pinned ? L("unpin") : L("pin"))
                if noteId != 0 {
                    Button(role: .destructive) {
                        deleted = true
                        model.delete(noteId) // the list then shows "Note deleted · Undo"
                        dismiss()
                    } label: {
                        Image(systemName: "trash")
                    }
                    .accessibilityLabel(L("delete"))
                }
            }
        }
        .onAppear {
            // TRAP: onAppear can run more than once for the same screen. Fill the fields only the first time,
            // or coming back to this view would throw away what the user typed.
            guard !loaded else { return }
            loaded = true
            if let note = model.find(noteId) {
                title = note.title
                text = note.body
                pinned = note.pinned
            }
        }
        .onDisappear {
            // PLATFORM: iOS has no Back callback for the swipe-back gesture, so saving happens on disappear.
            // Android saves in its BackHandler instead (onDispose there also fires on rotation).
            guard !deleted else { return }
            model.save(id: noteId, title: title, body: text, pinned: pinned)
        }
    }
}
