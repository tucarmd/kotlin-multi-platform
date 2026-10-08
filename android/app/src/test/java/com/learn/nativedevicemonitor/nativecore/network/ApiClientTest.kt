package com.learn.nativedevicemonitor.nativecore.network

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.concurrent.thread

/**
 * Day 10 — ApiClient runs FOR REAL over HTTP against a mini server on 127.0.0.1 (`ServerSocket`, ~30 lines below) →
 * no internet, no mock library. Same cases as `ios/NativeDeviceMonitorTests/ApiClientTests.swift`
 * (iOS intercepts requests with `URLProtocol` instead of a server).
 *
 * Why not `com.sun.net.httpserver`: Android unit tests compile against `android.jar` — that package isn't there.
 */
class ApiClientTest {

    private lateinit var server: ServerSocket
    private lateinit var base: String

    /** One fully read request: method · path · query · headers (lowercase keys) · body. */
    private class Req(val method: String, val path: String, val query: String?, val headers: Map<String, String>, val body: String)

    private fun route(req: Req): Pair<Int, String> = when (req.path) {
        // echo back method + query + the X-Client header so the test can check what the client sent
        "/ok" -> 200 to """{"method":"${req.method}","query":"${req.query}","client":"${req.headers["x-client"]}"}"""
        "/posts" -> 201 to """{"id":101,"echo":${req.body},"contentType":"${req.headers["content-type"]}"}"""
        "/fail" -> 500 to """{"error":"boom"}"""
        "/slow" -> { Thread.sleep(1_500); 200 to "{}" }
        else -> 404 to "{}"
    }

    private fun handle(socket: Socket) = socket.use { s ->
        val input = s.getInputStream().bufferedReader()
        val (method, target) = input.readLine().split(" ")
        val headers = generateSequence { input.readLine()?.takeIf { it.isNotEmpty() } }
            .associate { it.substringBefore(":").lowercase() to it.substringAfter(":").trim() }
        val length = headers["content-length"]?.toInt() ?: 0
        val body = CharArray(length).also { input.read(it, 0, length) }.concatToString()
        val (status, text) = route(Req(method, target.substringBefore("?"), target.substringAfter("?", "").ifEmpty { null }, headers, body))
        val bytes = text.toByteArray()
        s.getOutputStream().apply {
            write("HTTP/1.1 $status X\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
            write(bytes)
            flush()
        }
    }

    @Before
    fun startServer() {
        server = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))   // port 0 = the system picks a free port
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                thread(isDaemon = true) { runCatching { handle(socket) } }
            }
        }
        base = "http://127.0.0.1:${server.localPort}"
    }

    @After
    fun stopServer() = server.close()

    @Test
    fun get200_success_sendsParamsAndDefaultHeaders() = runBlocking {
        val r = ApiClient().get("$base/ok", mapOf("userId" to "1"))
        assertTrue(r is ApiResult.Success)
        r as ApiResult.Success
        assertEquals(200, r.status)
        assertTrue(r.body.contains("\"method\":\"GET\""))
        assertTrue(r.body.contains("\"query\":\"userId=1\""))
        assertTrue(r.body.contains("NativeDeviceMonitor-Android"))
        assertEquals("application/json", r.contentType)
    }

    @Test
    fun post201_sendsJsonBodyWithContentType() = runBlocking {
        val r = ApiClient().post("$base/posts", """{"title":"hi"}""")
        r as ApiResult.Success
        assertEquals(201, r.status)
        assertTrue(r.body.contains("\"id\":101"))
        assertTrue(r.body.contains("\"title\":\"hi\""))
        assertTrue(r.body.contains("application/json"))
    }

    @Test
    fun status500_isHttpError_withErrorBody() = runBlocking {
        val r = ApiClient().get("$base/fail")
        r as ApiResult.HttpError
        assertEquals(500, r.status)
        assertEquals("""{"error":"boom"}""", r.body)   // body read from errorStream, not lost
    }

    @Test
    fun slowServer_isTimeout() = runBlocking {
        val r = ApiClient(readTimeoutMs = 300).get("$base/slow")
        assertTrue("got $r", r is ApiResult.Timeout)
        assertEquals(300, (r as ApiResult.Timeout).timeoutMs)
    }

    @Test
    fun closedPort_isNoNetwork() = runBlocking {
        val port = server.localPort
        server.close()
        val r = ApiClient(connectTimeoutMs = 1_000).get("http://127.0.0.1:$port/ok")
        assertTrue("got $r", r is ApiResult.NoNetwork)
    }

    @Test
    fun malformedUrl_isFailure_notCrash() = runBlocking {
        assertTrue(ApiClient().get("not a url") is ApiResult.Failure)
    }

    @Test
    fun classify_mapsExceptions() {
        assertTrue(ApiClient.classify(SocketTimeoutException(), 8000, 1) is ApiResult.Timeout)
        assertTrue(ApiClient.classify(UnknownHostException("x"), 8000, 1) is ApiResult.NoNetwork)
        assertTrue(ApiClient.classify(java.io.IOException("x"), 8000, 1) is ApiResult.Failure)
    }

    @Test
    fun buildUrl_sortsAndEncodes() {
        assertEquals("https://a.b/p", ApiClient.buildUrl("https://a.b/p", emptyMap()))
        assertEquals(
            "https://a.b/p?q=a%20b%26c&userId=1",
            ApiClient.buildUrl("https://a.b/p", mapOf("userId" to "1", "q" to "a b&c")),
        )
        assertEquals("https://a.b/p?x=1&y=2", ApiClient.buildUrl("https://a.b/p?x=1", mapOf("y" to "2")))
    }

    @Test
    fun prettyJson_indentsJson_keepsNonJson() {
        // org.json's JVM and Android versions wrap lines slightly differently → only check "indented by 2 spaces"
        val pretty = ApiClient.prettyJson("""{"a":1,"url":"https://x.y/z"}""")
        assertTrue(pretty, pretty.contains("\n  \"a\": 1"))
        assertTrue(pretty, pretty.contains("https://x.y/z"))   // no "\/" left
        assertEquals("<html>oops</html>", ApiClient.prettyJson("<html>oops</html>"))
        assertEquals("{broken", ApiClient.prettyJson("{broken"))
    }
}
