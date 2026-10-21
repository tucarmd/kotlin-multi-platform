package com.learn.nativedevicemonitor.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.nativedevicemonitor.ui.theme.AppColor
import com.learn.nativedevicemonitor.ui.theme.AppRadius
import com.learn.nativedevicemonitor.ui.theme.AppSpacing
import com.learn.nativedevicemonitor.ui.theme.AppTheme

/**
 * primary button: full width, pill-shaped, with disabled and loading states.
 *
 * In the design these are the "Search" and "Save vehicle" buttons — both shown in the
 * **grey disabled state**, i.e. the design treats disabled as a NORMAL state (form not filled yet),
 * not an exception. So it's a first-class parameter here, not something bolted on later.
 *
 * `loading = true` disables the button too: tapping again while sending sends twice —
 * a classic bug; blocking it inside the component means no screen can forget.
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    /**
     * Day 27: destructive action button (Delete / Remove) — red background instead of blue.
     * Why a parameter and not a 4th component: only ONE background color differs, everything else
     * (height, rounding, loading, disabled) is identical — another file means copying 40 lines to change 1 value.
     * Ionic does the same: the same `<ion-button>`, `color="primary"` becomes `color="danger"`.
     */
    danger: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(AppRadius.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (danger) AppColor.danger else AppColor.primary,
            contentColor = Color.White,
            disabledContainerColor = AppColor.disabled,
            disabledContentColor = AppColor.textSecondary,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Secondary button: same shape, but outlined — for actions that aren't the screen's primary action
 * (Cancel, Back). Same file as the primary button because the two must always change shape together.
 */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT),
        enabled = enabled,
        shape = RoundedCornerShape(AppRadius.pill),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColor.primary),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

private val BUTTON_HEIGHT = 52.dp

@Preview(showBackground = true, backgroundColor = 0xFFEFF5FF, widthDp = 340)
@Composable
private fun AppButtonsPreview() = AppTheme {
    Column(
        Modifier.padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        AppPrimaryButton("Save vehicle", {})
        AppPrimaryButton("Save vehicle", {}, enabled = false)
        AppPrimaryButton("Saving…", {}, loading = true)
        AppSecondaryButton("Cancel", {})
    }
}
