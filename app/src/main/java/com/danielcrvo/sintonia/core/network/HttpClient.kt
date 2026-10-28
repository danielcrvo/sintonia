package com.danielcrvo.sintonia.core.network

import com.danielcrvo.sintonia.core.common.LimitedInputStream
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface Downloader {
    suspend fun download(url: String, destination: File, readTimeoutSeconds: Long = HttpClient.LongReadSeconds): File
}

@Singleton
class HttpClient
    @Inject
    constructor(private val client: OkHttpClient) : Downloader {
        override suspend fun download(url: String, destination: File, readTimeoutSeconds: Long): File =
            withContext(Dispatchers.IO) {
                val host = hostOf(url)
                try {
                    val call = client
                        .newBuilder()
                        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                        .callTimeout(DownloadDeadlineMillis, TimeUnit.MILLISECONDS)
                        .build()
                        .newCall(
                            Request
                                .Builder()
                                .url(url)
                                .header("Accept-Encoding", "gzip")
                                .build(),
                        )
                    try {
                        call.await().use { response ->
                            response.requireSuccess(host)
                            destination.parentFile?.mkdirs()
                            LimitedInputStream(response.body.byteStream().unzipIfNeeded(), MaxDownloadBytes).use { input ->
                                destination.outputStream().buffered().use { out -> copyCancellable(input, out) }
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        call.cancel() // the body may already be streaming: really abort the transfer
                        throw cancelled
                    }
                    destination
                } catch (error: IOException) {
                    destination.delete()
                    throw error.toApiException(host)
                } catch (error: ApiException) {
                    destination.delete()
                    throw error
                }
            }

        suspend fun getText(url: String, readTimeoutSeconds: Long = ShortReadSeconds): String =
            withContext(Dispatchers.IO) {
                val host = hostOf(url)
                try {
                    val call = client
                        .newBuilder()
                        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                        .build()
                        .newCall(Request.Builder().url(url).build())
                    call.await().use { response ->
                        response.requireSuccess(host)
                        LimitedInputStream(response.body.byteStream(), MaxTextBytes).bufferedReader().use { it.readText() }
                    }
                } catch (error: IOException) {
                    throw error.toApiException(host)
                }
            }

        private fun Response.requireSuccess(host: String?) {
            if (isSuccessful) return
            val kind = if (code == 401 || code == 403) ApiException.Kind.Auth else ApiException.Kind.Http
            throw ApiException(kind, host, httpCode = code)
        }

        companion object {
            const val ShortReadSeconds = 20L
            const val LongReadSeconds = 120L

            /** Cap after decompression (huge lists reach a few hundred MB). */
            const val MaxDownloadBytes = 512L * 1024 * 1024

            const val MaxTextBytes = 8L * 1024 * 1024
            const val DownloadDeadlineMillis = 15L * 60 * 1000
        }
    }

private suspend fun copyCancellable(input: InputStream, output: java.io.OutputStream) {
    val buffer = ByteArray(CopyBufferBytes)
    val context = currentCoroutineContext()
    while (true) {
        val read = input.read(buffer)
        if (read < 0) return
        output.write(buffer, 0, read)
        context.ensureActive()
    }
}

private const val CopyBufferBytes = 64 * 1024

private fun InputStream.unzipIfNeeded(): InputStream {
    val pushback = PushbackInputStream(this, 2)
    val first = pushback.read()
    val second = pushback.read()
    if (second != -1) pushback.unread(second)
    if (first != -1) pushback.unread(first)
    return if (first == GzipFirstByte && second == GzipSecondByte) GZIPInputStream(pushback) else pushback
}

private const val GzipFirstByte = 0x1f
private const val GzipSecondByte = 0x8b

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation: CancellableContinuation<Response> ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) continuation.resume(response) else response.close()
        }
    })
}
