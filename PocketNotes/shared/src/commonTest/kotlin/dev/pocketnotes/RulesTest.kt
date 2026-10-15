package dev.pocketnotes

import dev.pocketnotes.model.Note
import dev.pocketnotes.model.NoteDraft
import dev.pocketnotes.model.RelativeTime
import dev.pocketnotes.model.SaveAction
import dev.pocketnotes.rules.fold
import dev.pocketnotes.rules.matches
import dev.pocketnotes.rules.planSave
import dev.pocketnotes.rules.relativeTime
import dev.pocketnotes.rules.sortNotes
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** [App 2 · B7] The pure rules: search, sort, relative time, save plan. Each test name says the behaviour it protects. */
class RulesTest {

    private val noon = Instant.parse("2026-10-15T12:00:00Z")
    private val utc = TimeZone.UTC

    private fun note(id: Long, title: String = "", body: String = "", pinned: Boolean = false, at: Instant = noon) =
        Note(id = id, title = title, body = body, pinned = pinned, updatedAt = at)

    @Test
    fun `search ignores case and Vietnamese diacritics`() {
        val n = note(1, title = "Ghi chú đầu tiên", body = "Mua phở và CÀ PHÊ")
        assertTrue(matches(n, "ghi chu"))
        assertTrue(matches(n, "GHI CHÚ"))
        assertTrue(matches(n, "dau tien"))
        assertTrue(matches(n, "ca phe"))
        assertFalse(matches(n, "bánh mì"))
    }

    @Test
    fun `the letter đ folds to d in both cases`() {
        assertEquals("dat hang di da nang", fold("ĐẶT HÀNG đi Đà Nẵng"))
        assertTrue(matches(note(1, title = "Đi chợ"), "di cho"))
    }

    @Test
    fun `decomposed accents from a pasted text are ignored too`() {
        // "chú" written as "chu" + U+0301 (combining acute accent), the form macOS sometimes produces
        assertTrue(matches(note(1, title = "Ghi chú"), "ghi chu"))
    }

    @Test
    fun `a blank query matches every note and extra spaces are collapsed`() {
        assertTrue(matches(note(1, title = "Anything"), "   "))
        assertTrue(matches(note(1, title = "Ghi chú"), "  ghi    chu "))
    }

    @Test
    fun `pinned notes come first then newest first`() {
        val older = note(1, title = "A", at = noon - 2.hours)
        val pinnedOld = note(2, title = "B", pinned = true, at = noon - 4.hours)
        val newest = note(3, title = "C", at = noon)
        assertEquals(listOf(2L, 3L, 1L), sortNotes(listOf(older, pinnedOld, newest)).map { it.id })
    }

    @Test
    fun `notes edited at the same time keep a stable order by id`() {
        assertEquals(listOf(5L, 4L), sortNotes(listOf(note(4), note(5))).map { it.id })
    }

    @Test
    fun `relative time uses the now it is given`() {
        assertEquals(RelativeTime.JustNow, relativeTime(noon - 30.seconds, noon, utc))
        assertEquals(RelativeTime.MinutesAgo(5), relativeTime(noon - 5.minutes, noon, utc))
        assertEquals(RelativeTime.HoursAgo(3), relativeTime(noon - 3.hours, noon, utc))
        assertEquals(RelativeTime.Yesterday, relativeTime(noon - 1.days, noon, utc))
        assertEquals(RelativeTime.DaysAgo(3), relativeTime(noon - 3.days, noon, utc))
        assertEquals(RelativeTime.OnDate(2026, 9, 1), relativeTime(Instant.parse("2026-09-01T08:00:00Z"), noon, utc))
    }

    @Test
    fun `yesterday means the previous calendar day and not 24 hours`() {
        val justAfterMidnight = Instant.parse("2026-10-15T01:00:00Z")
        assertEquals(RelativeTime.Yesterday, relativeTime(Instant.parse("2026-10-14T23:00:00Z"), justAfterMidnight, utc))
    }

    @Test
    fun `yesterday depends on the time zone`() {
        // 2026-10-14 20:00 UTC is already 2026-10-15 03:00 in Ho Chi Minh City (UTC+7): same day there
        val from = Instant.parse("2026-10-14T20:00:00Z")
        assertEquals(RelativeTime.Yesterday, relativeTime(from, noon, utc))
        assertEquals(RelativeTime.HoursAgo(16), relativeTime(from, noon, TimeZone.of("Asia/Ho_Chi_Minh")))
    }

    @Test
    fun `a time in the future shows as just now`() {
        assertEquals(RelativeTime.JustNow, relativeTime(noon + 5.minutes, noon, utc))
    }

    @Test
    fun `a new note left empty is not created`() {
        assertEquals(SaveAction.Skip, planSave(null, NoteDraft(0, "  ", "\n", pinned = true), noon))
    }

    @Test
    fun `a new note with text is inserted with the current time`() {
        val action = planSave(null, NoteDraft(0, "Milk", "", pinned = false), noon)
        assertEquals(SaveAction.Insert(note(0, title = "Milk")), action)
    }

    @Test
    fun `opening a note without changes writes nothing`() {
        val saved = note(7, title = "T", body = "B", at = noon - 1.days)
        assertEquals(SaveAction.Skip, planSave(saved, NoteDraft(7, "T", "B", pinned = false), noon))
    }

    @Test
    fun `changing only the pin keeps the edit time`() {
        val saved = note(7, title = "T", at = noon - 1.days)
        assertEquals(SaveAction.Update(saved.copy(pinned = true)), planSave(saved, NoteDraft(7, "T", "", true), noon))
    }

    @Test
    fun `editing the text moves the edit time to now`() {
        val saved = note(7, title = "T", at = noon - 1.days)
        val action = planSave(saved, NoteDraft(7, "T2", "", pinned = false), noon)
        assertEquals(SaveAction.Update(saved.copy(title = "T2", updatedAt = noon)), action)
    }

    @Test
    fun `clearing an existing note deletes it`() {
        assertEquals(SaveAction.Delete(7), planSave(note(7, title = "T"), NoteDraft(7, "", " ", false), noon))
    }
}
