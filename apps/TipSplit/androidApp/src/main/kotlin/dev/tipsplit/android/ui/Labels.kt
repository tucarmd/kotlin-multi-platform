package dev.tipsplit.android.ui

import androidx.annotation.StringRes
import dev.tipsplit.android.R
import dev.tipsplit.model.InputError

/**
 * [App 1 · B5] Core enum → string resource. The core never holds UI text, so each UI owns this mapping.
 * Its iOS twin is Labels.swift, with the same keys.
 *
 * [Bài 7 · Resources và đa ngôn ngữ] Tip Split uses each platform's NATIVE resources instead of Compose
 * Multiplatform's composeResources/: Android reads res/values (en) or res/values-vi (vi) by the phone's language,
 * iOS reads en.lproj / vi.lproj. The keys (error_not_a_number…) are the same on both, so the two files line up.
 * The returned Int is an R.string id; the text is looked up later by stringResource() in SplitScreen.
 */
@StringRes
fun InputError.label(): Int = when (this) {
    InputError.NotNumeric -> R.string.error_not_a_number
    InputError.NotPositive -> R.string.error_not_positive
    InputError.TooLarge -> R.string.error_too_large
    InputError.NoPeople -> R.string.error_no_people
    InputError.TooManyPeople -> R.string.error_too_many_people
}
