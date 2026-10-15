package dev.triplog.android.ui

import androidx.annotation.StringRes
import dev.triplog.android.R
import dev.triplog.model.LocationProblem
import java.text.DateFormat
import java.util.Date

/**
 * [App 4 · B5] Core enum → string resource. The core never holds UI text, so each UI owns this mapping.
 * Its iOS twin is Labels.swift, with the same keys.
 */
@StringRes
fun LocationProblem.label(): Int = when (this) {
    LocationProblem.PermissionDenied -> R.string.problem_permission
    LocationProblem.LocationOff -> R.string.problem_location_off
}

/** Epoch millis → "15 Oct 2026, 08:30" in the phone's language. */
fun formatDateTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
