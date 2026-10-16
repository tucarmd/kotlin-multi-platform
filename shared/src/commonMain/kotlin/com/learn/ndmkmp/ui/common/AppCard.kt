package com.learn.ndmkmp.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppRadius
import com.learn.ndmkmp.ui.theme.AppSpacing
import com.learn.ndmkmp.ui.theme.AppTheme

/**
 * a white rounded block with a shadow, sitting on the page's light-blue background.
 *
 * This is the most repeated shape in the CarMD design (the "Enter Report Number" block,
 * the "2016 Honda CR-V" block, the Nickname/Mileage block). In Ionic it's `ion-card` + the `.app-card` class;
 * here it wraps Material 3's `Card` to lock corner radius + shadow + padding to the tokens.
 *
 * `title` = null draws no title — because in the design some cards have a title, some don't.
 *
 * `content: @Composable ColumnScope.() -> Unit` is exactly `<ng-content>`: the body passed in by the
 * screen. `ColumnScope` lets the inside use `Modifier.weight()` as if it were inside a Column.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColor.surface),
        // `--box-shadow-1: 0 4px 12px rgba(0,0,0,.12)` in CarMD. Compose doesn't take separate offset/blur
        // like CSS — only one `elevation` number; 4dp gives the closest-looking shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.textPrimary,
                )
            }
            content()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEFF5FF, widthDp = 340)
@Composable
private fun AppCardPreview() = AppTheme {
    Column(
        Modifier.padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        AppCard(title = "With title") { Text("Body content") }
        AppCard { Text("No title") }
    }
}
