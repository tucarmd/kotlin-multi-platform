package com.learn.ndmkmp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppSpacing
import com.learn.ndmkmp.ui.theme.AppTheme

/**
 * a small grey hint line with a bullet in front.
 *
 * In the design: "• Report numbers are on the top of the page (e.g. top right or top center)".
 *
 * Why draw the "•" with a `Text` instead of an HTML list like on the web: Compose has no `<ul><li>`.
 * Composing `Row { Text("•"); Text(content) }` is more reliable than prefixing the string with "• " —
 * that way, wrapped text snaps back to the margin instead of lining up with the line above.
 */
@Composable
fun AppHintText(
    text: String,
    modifier: Modifier = Modifier,
    bullet: Boolean = true,
) {
    Row(modifier = modifier) {
        if (bullet) {
            Text(
                text = "•",
                fontSize = 13.sp,
                color = AppColor.textSecondary,
                modifier = Modifier.padding(end = AppSpacing.sm),
            )
        }
        Text(text = text, fontSize = 13.sp, color = AppColor.textSecondary, lineHeight = 18.sp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEFF5FF, widthDp = 300)
@Composable
private fun AppHintTextPreview() = AppTheme {
    Column(
        Modifier.padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        AppHintText("Report numbers are on the top of the page (e.g. top right or top center)")
        AppHintText("Without a bullet", bullet = false)
    }
}
