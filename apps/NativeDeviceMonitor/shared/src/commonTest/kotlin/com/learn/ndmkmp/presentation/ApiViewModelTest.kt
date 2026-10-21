package com.learn.ndmkmp.presentation

import com.learn.ndmkmp.core.network.ApiResult
import com.learn.ndmkmp.core.network.ApiService
import com.learn.ndmkmp.presentation.api.ApiPreset
import com.learn.ndmkmp.presentation.api.ApiViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiViewModelTest : MainDispatcherTest() {

    /** Answers only when the test says so → we can look at the "sending" state in between. */
    private class GatedApi : ApiService {
        val answer = CompletableDeferred<ApiResult>()
        var calls = 0
        override suspend fun get(url: String, params: Map<String, String>): ApiResult { calls++; return answer.await() }
        override suspend fun post(url: String, body: String): ApiResult { calls++; return answer.await() }
    }

    @Test
    fun send_locksButtons_thenFormatsTheResult() = runTest(dispatcher) {
        val api = GatedApi()
        val vm = ApiViewModel(api)
        vm.send(ApiPreset("x", "GET", "https://x.test/a", mapOf("b" to "1")))
        advanceUntilIdle()
        assertTrue(vm.state.value.sending)
        assertEquals("GET https://x.test/a?b=1", vm.state.value.lastRequest)
        assertEquals(false, vm.state.value.canSendCustom)

        vm.send(ApiPreset("y", "GET", "https://x.test/b"))   // ignored while in flight
        advanceUntilIdle()
        assertEquals(1, api.calls)

        api.answer.complete(ApiResult.Success(200, """{"a":1}""", "application/json", 42))
        advanceUntilIdle()
        val r = vm.state.value.result!!
        assertEquals("SUCCESS · HTTP 200 · 42 ms", r.statusLine)
        assertTrue(r.ok)
        assertEquals("content-type: application/json", r.contentType)
        assertEquals("{\n    \"a\": 1\n}", r.body)
        vm.clear()
    }

    @Test
    fun timeout_isRedWithNoBody() = runTest(dispatcher) {
        val api = GatedApi().apply { answer.complete(ApiResult.Timeout(8000, 8001)) }
        val vm = ApiViewModel(api)
        vm.sendCustom()
        advanceUntilIdle()
        val r = vm.state.value.result!!
        assertEquals(false, r.ok)
        assertNull(r.body)
        vm.clear()
    }
}
