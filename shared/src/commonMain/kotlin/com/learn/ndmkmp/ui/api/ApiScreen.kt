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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.core.network.ApiFormat
import com.learn.ndmkmp.core.network.ApiService
import com.learn.ndmkmp.core.network.ApiResult
import com.learn.ndmkmp.ui.common.AppCard
import com.learn.ndmkmp.ui.common.AppHintText
import com.learn.ndmkmp.ui.common.AppPrimaryButton
import com.learn.ndmkmp.ui.common.AppSecondaryButton
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppSpacing
import kotlinx.coroutines.launch

/** A long body (e.g. /posts returns 10 posts) drawn into one Text stutters — truncate, enough to look at. */
private const val BODY_PREVIEW = 3000

/**
 * Day 10 — tab API: "simple test interface" theo Word.
 *
 * ```
 *  ApiScreen
 *   ├─ AppCard "Test requests"  6 buttons = 6 rows of the test table (200 · params · POST 201 · headers · 500 · timeout)
 *   ├─ AppCard "Custom GET"     URL field + [Send GET]
 *   └─ AppCard "Response"       request · STATUS (green/red) · content-type · indented JSON body
 * ```
 *
 * `scope.launch { client.get(...) }`: the coroutine runs on Main, the [ApiService] does its own I/O off the main thread →
 * the UI doesn't freeze, the result is assigned straight to state. While sending every button is disabled → no overlapping requests.
 */
@Composable
fun ApiScreen(client: ApiService) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var customUrl by rememberSaveable { mutableStateOf("https://jsonplaceholder.typicode.com/users/1") }
    var lastRequest by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ApiResult?>(null) }
    var sending by remember { mutableStateOf(false) }

    fun send(preset: ApiPreset) {
        sending = true
        lastRequest = ApiStrings.request(preset.method, ApiFormat.buildUrl(preset.url, preset.params))
        result = null
        // The Response card is at the bottom → scroll to the end to see "Sending…" then the result (a long body keeps growing downward).
        scope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
        scope.launch {
            result = if (preset.method == "POST") {
                client.post(preset.url, preset.body.orEmpty())
            } else {
                client.get(preset.url, preset.params)
            }
            sending = false
        }
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
            ApiStrings.PRESETS.forEach { preset ->
                AppSecondaryButton(preset.label, onClick = { send(preset) }, enabled = !sending)
            }
            AppHintText(ApiStrings.AIRPLANE_HINT)
        }

        AppCard(title = ApiStrings.CUSTOM_TITLE) {
            OutlinedTextField(
                value = customUrl,
                onValueChange = { customUrl = it },
                label = { Text(ApiStrings.URL_LABEL) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Send),
                modifier = Modifier.fillMaxWidth(),
            )
            AppPrimaryButton(
                ApiStrings.SEND_GET,
                onClick = { send(ApiPreset("custom", "GET", customUrl.trim())) },
                loading = sending,
                enabled = customUrl.isNotBlank(),
            )
        }

        AppCard(title = ApiStrings.RESULT_TITLE) {
            if (lastRequest.isNotEmpty()) {
                Text(lastRequest, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = AppColor.textPrimary)
            }
            val r = result
            when {
                sending -> AppHintText(ApiStrings.SENDING)
                r == null -> AppHintText(ApiStrings.NOTHING_YET)
                else -> ResultBody(r)
            }
        }
    }
}

@Composable
private fun ResultBody(r: ApiResult) {
    val (line, ok) = ApiStrings.statusLine(r)
    Text(line, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (ok) AppColor.success else AppColor.danger)
    if (r is ApiResult.Success && r.contentType != null) {
        Text("content-type: ${r.contentType}", fontSize = 12.sp, color = AppColor.textSecondary)
    }
    val body = when (r) {
        is ApiResult.Success -> r.body
        is ApiResult.HttpError -> r.body
        else -> ""
    }
    if (body.isNotBlank()) {
        val pretty = ApiFormat.prettyJson(body)
        Text(
            text = if (pretty.length > BODY_PREVIEW) pretty.take(BODY_PREVIEW) + "\n… (${pretty.length} chars)" else pretty,
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
