package com.learn.ndmkmp.presentation.api

import com.learn.ndmkmp.core.network.ApiFormat
import com.learn.ndmkmp.core.network.ApiResult
import com.learn.ndmkmp.core.network.ApiService
import com.learn.ndmkmp.presentation.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A long body (e.g. /posts returns 10 posts) drawn into one Text stutters — truncate, enough to look at. */
private const val BODY_PREVIEW = 3000

/** The Response card, already formatted. */
data class ApiResultUi(
    val statusLine: String,
    /** true = green, false = red. */
    val ok: Boolean,
    val contentType: String?,
    /** Indented JSON (truncated), null = no body. */
    val body: String?,
)

data class ApiUiState(
    val presets: List<ApiPreset> = ApiStrings.PRESETS,
    val customUrl: String = "https://jsonplaceholder.typicode.com/users/1",
    val lastRequest: String = "",
    /** While true every button is disabled → no overlapping requests. */
    val sending: Boolean = false,
    val result: ApiResultUi? = null,
) {
    val canSendCustom: Boolean get() = customUrl.isNotBlank() && !sending
}

/** Day 10 — API tab: 6 test presets + a custom GET. [ApiService] does its own I/O off the main thread. */
class ApiViewModel(private val api: ApiService) : SharedViewModel() {

    private val _state = MutableStateFlow(ApiUiState())
    val state: StateFlow<ApiUiState> = _state.asStateFlow()

    fun onCustomUrlChange(url: String) = _state.update { it.copy(customUrl = url) }

    fun sendCustom() = send(ApiPreset("custom", "GET", _state.value.customUrl.trim()))

    fun send(preset: ApiPreset) {
        if (_state.value.sending) return
        _state.update {
            it.copy(sending = true, result = null, lastRequest = ApiStrings.request(preset.method, ApiFormat.buildUrl(preset.url, preset.params)))
        }
        scope.launch {
            val r = if (preset.method == "POST") api.post(preset.url, preset.body.orEmpty()) else api.get(preset.url, preset.params)
            _state.update { it.copy(sending = false, result = format(r)) }
        }
    }

    private fun format(r: ApiResult): ApiResultUi {
        val (line, ok) = ApiStrings.statusLine(r)
        val body = when (r) {
            is ApiResult.Success -> r.body
            is ApiResult.HttpError -> r.body
            else -> ""
        }
        val pretty = body.takeIf { it.isNotBlank() }?.let(ApiFormat::prettyJson)
        return ApiResultUi(
            statusLine = line,
            ok = ok,
            contentType = (r as? ApiResult.Success)?.contentType?.let { "content-type: $it" },
            body = pretty?.let { if (it.length > BODY_PREVIEW) it.take(BODY_PREVIEW) + "\n… (${it.length} chars)" else it },
        )
    }
}
