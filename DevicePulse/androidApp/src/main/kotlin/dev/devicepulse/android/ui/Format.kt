package dev.devicepulse.android.ui

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant

/** Locale.ROOT keeps the decimal point a dot, so coordinates read the same in every language. */
fun Double.fixed(decimals: Int): String = String.format(Locale.ROOT, "%.${decimals}f", this)

private val clockFormat = DateTimeFormatter.ofPattern("HH:mm:ss · dd/MM/yyyy")

fun Instant.toClockText(): String =
    java.time.Instant.ofEpochMilli(toEpochMilliseconds()).atZone(ZoneId.systemDefault()).format(clockFormat)
