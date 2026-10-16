package dev.pocketnotes.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.pocketnotes.android.R
import dev.pocketnotes.model.RelativeTime

/**
 * [App 2 · B5] Core value → text. The core never holds UI text, so each UI owns this mapping.
 * Its iOS twin is Labels.swift, with the same string keys.
 *
 * `when` without `else`: adding a RelativeTime case in Kotlin stops this file compiling until it gets a label.
 */
@Composable
fun RelativeTime.label(): String = when (this) {
    RelativeTime.JustNow -> stringResource(R.string.just_now)
    is RelativeTime.MinutesAgo -> stringResource(R.string.minutes_ago, minutes)
    is RelativeTime.HoursAgo -> stringResource(R.string.hours_ago, hours)
    RelativeTime.Yesterday -> stringResource(R.string.yesterday)
    is RelativeTime.DaysAgo -> stringResource(R.string.days_ago, days)
    // Day / month order lives in the string itself: en "%2$d/%3$d/%1$d", vi "%3$d/%2$d/%1$d".
    is RelativeTime.OnDate -> stringResource(R.string.on_date, year, month, day)
}
