import SwiftUI
import Shared

/// [App 2 · B6] The list: search bar, notes (pinned first), swipe to delete or pin, Undo bar, "new note" button.
/// Draws `model.state` and fires actions; filters and sorts nothing itself. Android twin: NoteListScreen.kt.
struct NoteListView: View {
    let model: NotesModel
    let onNew: () -> Void

    var body: some View {
        content
            .navigationTitle(L("app_name"))
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: onNew) { Image(systemName: "square.and.pencil") }
                        .accessibilityLabel(L("new_note"))
                }
            }
            // Recompute "5 min ago" each time the list comes back (from the editor or on first show).
            .onAppear { model.refreshClock() }
    }

    /// One branch per NotesState case. `switch` on `onEnum(of:)` stops compiling if Kotlin adds a case.
    @ViewBuilder
    private var content: some View {
        switch onEnum(of: model.state) {
        case .loading:
            ProgressView()
        case .failed:
            Text(L("load_failed")).foregroundStyle(.red).padding()
        case .ready(let ready):
            readyList(ready)
        }
    }

    private func readyList(_ ready: NotesState.Ready) -> some View {
        List {
            ForEach(ready.items, id: \.note.id) { item in
                NavigationLink(value: Route.edit(item.note.id)) {
                    NoteRowView(item: item)
                }
                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                    Button(role: .destructive) { model.delete(item.note.id) } label: {
                        Label(L("delete"), systemImage: "trash")
                    }
                }
                .swipeActions(edge: .leading) {
                    Button { model.togglePin(item.note.id) } label: {
                        Label(item.note.pinned ? L("unpin") : L("pin"), systemImage: item.note.pinned ? "pin.slash" : "pin")
                    }
                    .tint(.orange)
                }
            }
        }
        .listStyle(.plain)
        .overlay { emptyState(ready) }
        // Binding(get:set:) because the text lives in Kotlin: reading comes from the state, writing goes to the core.
        .searchable(text: Binding(get: { ready.query }, set: model.search), prompt: L("search_hint"))
        .safeAreaInset(edge: .bottom) {
            if ready.recentlyDeleted != nil { undoBar }
        }
        .animation(.default, value: ready.recentlyDeleted?.id)
    }

    @ViewBuilder
    private func emptyState(_ ready: NotesState.Ready) -> some View {
        if !ready.hasNotes {
            ContentUnavailableView(L("empty_title"), systemImage: "note.text", description: Text(L("empty_body")))
        } else if ready.items.isEmpty {
            ContentUnavailableView(L("no_results", ready.query.trimmingCharacters(in: .whitespaces)), systemImage: "magnifyingglass")
        }
    }

    /// iOS has no Snackbar: a small bar above the home indicator plays the same role.
    /// The core hides it after 5 s by clearing `recentlyDeleted`, exactly like the Android snackbar.
    private var undoBar: some View {
        HStack {
            Text(L("deleted"))
            Spacer()
            Button(L("undo")) { model.undoDelete() }.bold()
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
        .transition(.move(edge: .bottom).combined(with: .opacity))
    }
}

/// [App 2 · B6] One line of the list: title (or "Untitled"), first two lines of the body, "edited … ago", pin mark.
struct NoteRowView: View {
    let item: NoteItem

    var body: some View {
        let note = item.note
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Text(note.title.trimmingCharacters(in: .whitespaces).isEmpty ? L("untitled") : note.title)
                    .font(.headline)
                    .lineLimit(1)
                if !note.body.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(note.body).font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
                }
                Text(item.edited.label).font(.caption).foregroundStyle(.tertiary)
            }
            Spacer()
            if note.pinned {
                Image(systemName: "pin.fill").foregroundStyle(.orange).accessibilityLabel(L("pinned"))
            }
        }
        .padding(.vertical, 2)
    }
}
