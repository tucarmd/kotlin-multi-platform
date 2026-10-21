package com.learn.ndmkmp.ui.api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.presentation.api.ApiResultUi
import com.learn.ndmkmp.presentation.api.ApiStrings
import com.learn.ndmkmp.presentation.api.ApiViewModel
import com.learn.ndmkmp.ui.common.AppCard
import com.learn.ndmkmp.ui.common.AppHintText
import com.learn.ndmkmp.ui.common.AppPrimaryButton
import com.learn.ndmkmp.ui.common.AppSecondaryButton
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppSpacing

/**
 * Day 10 — tab API. Draws [ApiViewModel.state], every tap calls the ViewModel.
 *
 * ```
 *  ApiScreen
 *   ├─ AppCard "Test requests"  6 buttons = 6 rows of the test table (200 · params · POST 201 · headers · 500 · timeout)
 *   ├─ AppCard "Custom GET"     URL field + [Send GET]
 *   └─ AppCard "Response"       request · STATUS (green/red) · content-type · indented JSON body
 * ```
 */
@Composable
fun ApiScreen(vm: ApiViewModel) {
    val state by vm.state.collectAsState()
    val scrollState = rememberScrollState()

    // The Response card is at the bottom → scroll there to see "Sending…" then the result. Scrolling is a View concern.
    LaunchedEffect(state.sending) {
        if (state.sending) scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.contentBackground)
            .verticalScroll(scrollState)
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        AppHintText(ApiStrings.HINT, bullet = false)

        AppCard(title = ApiStrings.PRESETS_TITLE) {
            state.presets.forEach { preset ->
                AppSecondaryButton(preset.label, onClick = { vm.send(preset) }, enabled = !state.sending)
            }
            AppHintText(ApiStrings.AIRPLANE_HINT)
        }

        AppCard(title = ApiStrings.CUSTOM_TITLE) {
            OutlinedTextField(
                value = state.customUrl,
                onValueChange = vm::onCustomUrlChange,
                label = { Text(ApiStrings.URL_LABEL) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Send),
                modifier = Modifier.fillMaxWidth(),
            )
            AppPrimaryButton(ApiStrings.SEND_GET, onClick = vm::sendCustom, loading = state.sending, enabled = state.canSendCustom)
        }

        AppCard(title = ApiStrings.RESULT_TITLE) {
            if (state.lastRequest.isNotEmpty()) {
                Text(state.lastRequest, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = AppColor.textPrimary)
            }
            val r = state.result
            when {
                state.sending -> AppHintText(ApiStrings.SENDING)
                r == null -> AppHintText(ApiStrings.NOTHING_YET)
                else -> ResultBody(r)
            }
        }
    }
}

@Composable
private fun ResultBody(r: ApiResultUi) {
    Text(r.statusLine, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (r.ok) AppColor.success else AppColor.danger)
    r.contentType?.let { Text(it, fontSize = 12.sp, color = AppColor.textSecondary) }
    r.body?.let {
        Text(
            text = it,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = AppColor.terminalText,
            lineHeight = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColor.terminalBackground, RoundedCornerShape(8.dp))
                .padding(AppSpacing.sm),
        )
    }
}
