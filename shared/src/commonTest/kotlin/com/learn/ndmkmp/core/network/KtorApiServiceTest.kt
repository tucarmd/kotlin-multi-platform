package com.learn.ndmkmp.core.network

import com.learn.ndmkmp.core.network.KtorApiService.Companion.configure
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** P6 — the old ApiClientTest (local socket server) ported to Ktor MockEngine: same cases, runs on JVM and iOS. */
class KtorApiServiceTest {

    private var lastRequest: HttpRequestData? = null

    private fun service(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        KtorApiService(HttpClient(MockEngine { req -> lastRequest = req; handler(req) }) { configure() })

    private val json = headersOf(HttpHeaders.ContentType, "application/json; charset=utf-8")

    @Test
    fun get200_success_sendsParamsAndDefaultHeaders() = runTest {
        val r = service { respond("""{"id":1}""", HttpStatusCode.OK, json) }
            .get("https://x.test/posts", mapOf("userId" to "1", "a b" to "c"))
        val ok = assertIs<ApiResult.Success>(r)
        assertEquals(200, ok.status)
        assertEquals("""{"id":1}""", ok.body)
        assertTrue(ok.contentType!!.startsWith("application/json"))
        val req = lastRequest!!
        assertEquals("https://x.test/posts?a%20b=c&userId=1", req.url.toString())
        assertEquals("application/json", req.headers[HttpHeaders.Accept])
        assertTrue(req.headers["X-Client"]!!.startsWith("NativeDeviceMonitor-KMP"))
    }

    @Test
    fun post201_sendsJsonBodyWithContentType() = runTest {
        val r = service { respond("""{"id":101}""", HttpStatusCode.Created, json) }
            .post("https://x.test/posts", """{"title":"hi"}""")
        assertEquals(201, assertIs<ApiResult.Success>(r).status)
        val body = assertIs<TextContent>(lastRequest!!.body)
        assertEquals("""{"title":"hi"}""", body.text)
        assertEquals("application/json", body.contentType.withoutParameters().toString())
    }

    @Test
    fun status500_isHttpError_withErrorBody() = runTest {
        val r = service { respond("boom", HttpStatusCode.InternalServerError) }.get("https://x.test/status/500")
        val err = assertIs<ApiResult.HttpError>(r)
        assertEquals(500, err.status)
        assertEquals("boom", err.body)
    }

    @Test
    fun socketTimeout_isTimeout() = runTest {
        val r = service { throw SocketTimeoutException("read timed out") }.get("https://x.test/delay/10")
        assertEquals(8_000, assertIs<ApiResult.Timeout>(r).timeoutMs)
    }

    @Test
    fun unknownError_isFailure_notCrash() = runTest {
        val r = service { throw IllegalStateException("weird") }.get("https://x.test/")
        assertTrue(assertIs<ApiResult.Failure>(r).message.contains("weird"))
    }

    @Test
    fun buildUrl_sortsAndEncodes() {
        assertEquals("https://x.test/p", ApiFormat.buildUrl("https://x.test/p", emptyMap()))
        assertEquals("https://x.test/p?a=1&b=%C3%A9%20x", ApiFormat.buildUrl("https://x.test/p", mapOf("b" to "é x", "a" to "1")))
        assertEquals("https://x.test/p?q=1&z=2", ApiFormat.buildUrl("https://x.test/p?q=1", mapOf("z" to "2")))
    }

    @Test
    fun prettyJson_indentsJson_keepsNonJson() {
        assertTrue(ApiFormat.prettyJson("""{"a":1}""").contains("\n"))
        assertEquals("<html>oops</html>", ApiFormat.prettyJson("<html>oops</html>"))
        assertEquals("{broken", ApiFormat.prettyJson("{broken"))
    }
}
