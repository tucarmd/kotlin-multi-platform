package dev.ratewatch

import dev.ratewatch.model.Age
import dev.ratewatch.model.AgeUnit
import dev.ratewatch.model.AmountError
import dev.ratewatch.model.ConvertInput
import dev.ratewatch.model.ConvertResult
import dev.ratewatch.model.Freshness
import dev.ratewatch.model.RateTable
import dev.ratewatch.rules.COMMON_CURRENCIES
import dev.ratewatch.rules.MAX_AMOUNT
import dev.ratewatch.rules.ageOf
import dev.ratewatch.rules.convert
import dev.ratewatch.rules.convertInput
import dev.ratewatch.rules.currencyCodes
import dev.ratewatch.rules.formatAmount
import dev.ratewatch.rules.formatDecimal
import dev.ratewatch.rules.formatRate
import dev.ratewatch.rules.freshnessOf
import dev.ratewatch.rules.isStale
import dev.ratewatch.rules.parseAmount
import dev.ratewatch.rules.rateBetween
import dev.ratewatch.rules.rateRows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [App 3 · B7] Pure rules: conversion through the base, freshness at the one-hour edge, amount parsing and formatting.
 * Plain asserts, no coroutines: these run in milliseconds on the JVM and on the iOS Simulator.
 */
class RulesTest {

    private val hour = 60 * 60 * 1000L
    private val usd = RateTable(
        base = "USD",
        rates = mapOf("USD" to 1.0, "EUR" to 0.9, "JPY" to 150.0, "VND" to 26_312.5, "AUD" to 1.43),
        fetchedAt = 0,
    )

    // convert / rateBetween

    @Test
    fun `converts EUR to JPY through the USD base`() {
        // 90 EUR = 100 USD = 15 000 JPY
        assertEquals(15_000.0, convert(9_000, "EUR", "JPY", usd)!!, 1e-9)
        assertEquals(150.0 / 0.9, rateBetween("EUR", "JPY", usd)!!, 1e-9)
    }

    @Test
    fun `converting to the same currency keeps the amount`() {
        assertEquals(12.5, convert(1_250, "EUR", "EUR", usd)!!, 1e-9)
    }

    @Test
    fun `an unknown currency gives no result instead of zero`() {
        assertNull(convert(100, "USD", "XYZ", usd))
        assertNull(rateBetween("XYZ", "USD", usd))
    }

    @Test
    fun `convert input goes from waiting to invalid to ready`() {
        assertEquals(ConvertResult.Waiting, convertInput(ConvertInput(amountText = " "), usd))
        assertEquals(ConvertResult.Invalid(AmountError.NotNumeric), convertInput(ConvertInput(amountText = "12a"), usd))
        assertEquals(ConvertResult.NoRates, convertInput(ConvertInput(amountText = "1"), null))
        val ready = assertIs<ConvertResult.Ready>(convertInput(ConvertInput("100", "USD", "VND"), usd))
        assertEquals("2.631.250", ready.amountText)
        assertEquals("26.312,50", ready.rateText)
    }

    @Test
    fun `a huge amount is too large rather than not a number`() {
        assertEquals(
            ConvertResult.Invalid(AmountError.TooLarge),
            convertInput(ConvertInput(amountText = "99999999999999999999"), usd),
        )
        assertTrue(parseAmount("1000000000001")!! > MAX_AMOUNT)
    }

    // isStale / freshnessOf / ageOf

    @Test
    fun `a table exactly one hour old is still fresh`() {
        assertFalse(isStale(fetchedAt = 0, now = hour))
    }

    @Test
    fun `one millisecond past one hour is stale`() {
        assertTrue(isStale(fetchedAt = 0, now = hour + 1))
    }

    @Test
    fun `a fetch time in the future counts as stale`() {
        assertTrue(isStale(fetchedAt = hour, now = 0))
    }

    @Test
    fun `freshness is missing without a table`() {
        assertEquals(Freshness.Missing, freshnessOf(null, now = 0))
        assertEquals(Freshness.Fresh, freshnessOf(usd, now = 10 * 60_000))
        assertEquals(Freshness.Stale, freshnessOf(usd, now = 2 * hour))
    }

    @Test
    fun `age uses the biggest whole unit`() {
        assertEquals(Age(0, AgeUnit.JustNow), ageOf(0, 30_000))
        assertEquals(Age(5, AgeUnit.Minutes), ageOf(0, 5 * 60_000))
        assertEquals(Age(2, AgeUnit.Hours), ageOf(0, 3 * hour - 60_000))
        assertEquals(Age(2, AgeUnit.Days), ageOf(0, 49 * hour))
    }

    // parseAmount / format

    @Test
    fun `amounts parse with either separator style`() {
        assertEquals(10_000, parseAmount("100"))
        assertEquals(1_250, parseAmount("12.5"))
        assertEquals(1_250, parseAmount("12,50"))
        assertEquals(12_000_000, parseAmount("120.000"))
        assertEquals(123_456, parseAmount("1,234.56"))
        assertEquals(50, parseAmount(",5"))
    }

    @Test
    fun `non numbers are rejected`() {
        assertNull(parseAmount(""))
        assertNull(parseAmount("."))
        assertNull(parseAmount("12a"))
        assertNull(parseAmount("-5"))
    }

    @Test
    fun `formatting groups thousands with a dot and decimals with a comma`() {
        assertEquals("2.631.250", formatDecimal(2_631_250.4, 0))
        assertEquals("26.312,50", formatDecimal(26_312.5, 2))
        assertEquals("—", formatDecimal(Double.NaN, 2))
        assertEquals("2.631.250", formatAmount(2_631_250.0, "VND"))
        assertEquals("89,44", formatAmount(89.444, "EUR"))
    }

    @Test
    fun `rates keep about six significant digits`() {
        assertEquals("26.312,50", formatRate(26_312.5))
        assertEquals("1,43302", formatRate(1.433021))
        assertEquals("0,894425", formatRate(0.894425))
        assertEquals("0,0000380228", formatRate(0.0000380228))
    }

    // rateRows / currencyCodes

    @Test
    fun `favorites come first and the base is left out`() {
        val rows = rateRows(usd, favorites = setOf("VND"))
        assertEquals(listOf("VND", "AUD", "EUR", "JPY"), rows.map { it.code })
        assertTrue(rows.first().favorite)
    }

    @Test
    fun `pickers fall back to common currencies before the first download`() {
        assertEquals(COMMON_CURRENCIES, currencyCodes(null))
        assertEquals(listOf("AUD", "EUR", "JPY", "USD", "VND"), currencyCodes(usd))
    }
}
