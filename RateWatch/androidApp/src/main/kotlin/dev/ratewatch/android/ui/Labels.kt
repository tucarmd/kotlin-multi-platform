package dev.ratewatch.android.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.ratewatch.android.R
import dev.ratewatch.model.Age
import dev.ratewatch.model.AgeUnit
import dev.ratewatch.model.AmountError
import dev.ratewatch.model.RefreshError

/**
 * [App 3 · B5] Core enums → string resources. The core never holds UI text, so each UI owns this mapping.
 * Its iOS twin is Labels.swift, with the same keys. `when` without `else`: a new Kotlin case stops this compiling.
 */
@StringRes
fun RefreshError.label(): Int = when (this) {
    RefreshError.Offline -> R.string.error_offline
    RefreshError.Timeout -> R.string.error_timeout
    RefreshError.ServerError -> R.string.error_server
    RefreshError.BadResponse -> R.string.error_bad_response
    RefreshError.UnknownBase -> R.string.error_unknown_base
    RefreshError.Unknown -> R.string.error_unknown
}

@StringRes
fun AmountError.label(): Int = when (this) {
    AmountError.NotNumeric -> R.string.amount_not_numeric
    AmountError.TooLarge -> R.string.amount_too_large
}

/** Age(2, Hours) → "Updated 2 h ago"; null (nothing saved) → "Not downloaded yet". */
@Composable
fun updatedText(age: Age?): String = when (age?.unit) {
    null -> stringResource(R.string.updated_never)
    AgeUnit.JustNow -> stringResource(R.string.updated_just_now)
    AgeUnit.Minutes -> stringResource(R.string.updated_minutes, age.amount.toString())
    AgeUnit.Hours -> stringResource(R.string.updated_hours, age.amount.toString())
    AgeUnit.Days -> stringResource(R.string.updated_days, age.amount.toString())
}
