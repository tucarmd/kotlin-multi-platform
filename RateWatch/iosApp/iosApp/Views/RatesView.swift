import SwiftUI
import Shared

/// [App 3 · B6] Rates tab. Draws `model.state` and fires actions; computes nothing itself.
/// Its Android twin is RatesScreen.kt, with the same sections in the same order.
struct RatesView: View {
    let model: RatesModel

    var body: some View {
        let state = model.state
        NavigationStack {
            List {
                if state.showOfflineBanner, let error = state.error {
                    Section {
                        Label(L("offline_banner", error.label), systemImage: "wifi.slash")
                            .foregroundStyle(.red)
                    }
                }
                if state.table != nil {
                    rateSections(state)
                } else if state.showEmptyError {
                    ContentUnavailableView {
                        Label(L("empty_error_title"), systemImage: "exclamationmark.triangle")
                    } description: {
                        Text(state.error?.label ?? "")
                    } actions: {
                        Button(L("retry")) { model.refresh() }.buttonStyle(.borderedProminent)
                    }
                } else {
                    HStack(spacing: 12) {
                        ProgressView()
                        Text(L("loading")).foregroundStyle(.secondary)
                    }
                }
            }
            .animation(.default, value: state.showOfflineBanner)
            // PLATFORM: pull-to-refresh is one modifier on iOS; the spinner lasts as long as the async closure.
            .refreshable { await model.refreshAndWait() }
            .navigationTitle(L("app_name"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) { basePicker(state) }
            }
        }
    }

    @ViewBuilder
    private func rateSections(_ state: RatesState) -> some View {
        Section {
            VStack(alignment: .leading, spacing: 4) {
                Text(L("base_label", state.base)).font(.headline)
                Text(updatedText(state.age))
                    .font(.footnote)
                    // Red when older than 1 h, same rule as Android (freshness comes from the core).
                    .foregroundStyle(state.freshness == .stale ? .red : .secondary)
            }
        }
        let favorites = state.rows.filter { $0.favorite }
        let others = state.rows.filter { !$0.favorite }
        if !favorites.isEmpty {
            Section(L("favorites")) {
                ForEach(favorites, id: \.code) { rateLine($0) }
            }
        }
        Section(L("all_currencies")) {
            ForEach(others, id: \.code) { rateLine($0) }
        }
    }

    private func rateLine(_ row: RateRow) -> some View {
        HStack {
            Text(row.code).fontWeight(.semibold)
            Spacer()
            Text(row.rateText).monospacedDigit()
            Button {
                model.toggleFavorite(row.code)
            } label: {
                Image(systemName: row.favorite ? "star.fill" : "star")
                    .foregroundStyle(row.favorite ? Color.accentColor : .secondary)
            }
            // TRAP: without .borderless, a Button inside a List row makes the WHOLE row the tap target.
            .buttonStyle(.borderless)
            .accessibilityLabel(L(row.favorite ? "unstar" : "star"))
        }
    }

    private func basePicker(_ state: RatesState) -> some View {
        Menu {
            Picker(L("pick_base"), selection: Binding(get: { state.base }, set: model.selectBase)) {
                ForEach(state.currencies, id: \.self) { Text($0).tag($0) }
            }
        } label: {
            HStack(spacing: 2) {
                Text(state.base).fontWeight(.semibold)
                Image(systemName: "chevron.down").font(.caption)
            }
        }
    }
}
