package com.danielcrvo.sintonia.core.network

import com.danielcrvo.sintonia.core.common.LimitedInputStream
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class HttpClientTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val server = MockWebServer().apply { start() }
    private val client = HttpClient(OkHttpClient())

    @AfterTest
    fun tearDown() = server.shutdown()

    private fun target() = File(folder.root, "baixado.txt")

    @Test
    fun `downloads to a file`() = runTest {
        server.enqueue(MockResponse().setBody("#EXTM3U\nlinha"))
        val file = client.download(server.url("/lista.m3u").toString(), target())
        assertEquals("#EXTM3U\nlinha", file.readText())
    }

    @Test
    fun `decompresses gzip even without an encoding header`() = runTest {
        val gzipped = ByteArrayOutputStream().also { GZIPOutputStream(it).use { gz -> gz.write("<tv></tv>".toByteArray()) } }
        server.enqueue(MockResponse().setBody(Buffer().write(gzipped.toByteArray())))
        val file = client.download(server.url("/guia.xml.gz").toString(), target())
        assertEquals("<tv></tv>", file.readText())
    }

    @Test
    fun `401 becomes an auth error and 404 an http error with the code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        val auth = assertFailsWith<ApiException> { client.download(server.url("/a").toString(), target()) }
        assertEquals(ApiException.Kind.Auth, auth.kind)

        server.enqueue(MockResponse().setResponseCode(404))
        val http = assertFailsWith<ApiException> { client.download(server.url("/b").toString(), target()) }
        assertEquals(ApiException.Kind.Http, http.kind)
        assertEquals(404, http.httpCode)
        assertFalse(target().exists())
    }

    @Test
    fun `unreachable server becomes a network error`() = runTest {
        val url = server.url("/x").toString()
        server.shutdown()
        val error = assertFailsWith<ApiException> { client.getText(url) }
        assertEquals(ApiException.Kind.Network, error.kind)
    }

    @Test
    fun `read past the limit throws a response-too-large error`() {
        val stream = LimitedInputStream(ByteArray(100).inputStream(), maxBytes = 50)
        assertFailsWith<ResponseTooLargeException> { stream.readBytes() }
    }

    @Test
    fun `oversized response becomes an invalid error`() {
        assertEquals(ApiException.Kind.Invalid, ResponseTooLargeException().toApiException().kind)
    }
}
