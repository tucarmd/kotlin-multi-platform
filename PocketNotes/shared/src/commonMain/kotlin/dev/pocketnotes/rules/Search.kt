package dev.pocketnotes.rules

import dev.pocketnotes.model.Note

/**
 * [App 2 · B1] Search and sort rules: pure functions, input → output, no state and no I/O.
 *
 * Why here and not in the UI: both apps must find the same notes for the same query and list them in the same order.
 * Doing it once in commonMain gives one behaviour and one set of tests (RulesTest).
 *
 * Called by: NotesCore. Calls: nothing outside this file.
 */

/** Vietnamese letters with a diacritic, grouped by the plain letter they fold to. Lower case only: [fold] lower-cases first. */
private val VIETNAMESE_FOLDS: Map<Char, Char> = buildMap {
    fun group(plain: Char, accented: String) = accented.forEach { put(it, plain) }
    group('a', "àáạảãâầấậẩẫăằắặẳẵ")
    group('e', "èéẹẻẽêềếệểễ")
    group('i', "ìíịỉĩ")
    group('o', "òóọỏõôồốộổỗơờớợởỡ")
    group('u', "ùúụủũưừứựửữ")
    group('y', "ỳýỵỷỹ")
    // TRAP: đ is a separate letter, not "d + a mark". Unicode decomposition (NFD) leaves it alone,
    // so stripping combining marks never turns "Đà" into "da". It has to be mapped by hand.
    put('đ', 'd')
    put('Đ', 'd')
}

/**
 * Lower-cases [text] and removes Vietnamese diacritics, so "Ghi Chú Đầu" and "ghi chu dau" compare equal.
 *
 * Examples: "Ghi chú" → "ghi chu" · "ĐẶT HÀNG" → "dat hang" · "Phở" → "pho" · "café" → "cafe".
 */
fun fold(text: String): String {
    val out = StringBuilder(text.length)
    for (c in text.lowercase()) {
        // TRAP: text pasted from macOS / some keyboards is decomposed ("u" + U+0301 instead of "ú").
        // Dropping the combining marks (U+0300..U+036F) handles that form; the map handles the precomposed one.
        if (c in '̀'..'ͯ') continue
        out.append(VIETNAMESE_FOLDS[c] ?: c)
    }
    return out.toString()
}

/**
 * True when [query] appears in the note's title or body, ignoring case and Vietnamese diacritics.
 * A blank query matches every note. Runs of spaces in the query count as one space.
 *
 * Examples: query "ghi chu" matches title "Ghi chú" · "DAT HANG" matches body "đặt hàng" · "" matches anything.
 * TEST: RulesTest.`search ignores case and Vietnamese diacritics`
 */
fun matches(note: Note, query: String): Boolean {
    val needle = fold(query.trim()).replace(Regex("\\s+"), " ")
    if (needle.isEmpty()) return true
    return needle in fold(note.title) || needle in fold(note.body)
}

/**
 * Pinned notes first, then the most recently edited first. Ties (same time) fall back to the higher id,
 * so the order never flickers between two emissions.
 *
 * Example: [A(unpinned, 10:00), B(pinned, 08:00), C(unpinned, 11:00)] → [B, C, A].
 * TEST: RulesTest.`pinned notes come first then newest first`
 */
fun sortNotes(notes: List<Note>): List<Note> =
    notes.sortedWith(
        compareByDescending<Note> { it.pinned }
            .thenByDescending { it.updatedAt }
            .thenByDescending { it.id },
    )
