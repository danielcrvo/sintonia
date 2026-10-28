package com.danielcrvo.sintonia.core.network

import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.coroutines.cancellation.CancellationException

class ApiException(
    val kind: Kind,
    val host: String? = null,
    val httpCode: Int? = null,
    cause: Throwable? = null,
) : Exception("$kind${host?.let { " ($it)" }.orEmpty()}", cause) {
    enum class Kind {
        Network,
        Timeout,
        Ssl,

        Auth,
        Http,

        Invalid,

        Empty,

        File,
    }
}

fun Throwable.toApiException(host: String? = null): ApiException = when (this) {
    is ApiException -> this
    is CancellationException -> throw this
    is ResponseTooLargeException -> ApiException(ApiException.Kind.Invalid, host, cause = this)
    is SocketTimeoutException, is InterruptedIOException ->
        ApiException(ApiException.Kind.Timeout, host, cause = this)
    is UnknownHostException -> ApiException(ApiException.Kind.Network, host, cause = this)
    is SSLException -> ApiException(ApiException.Kind.Ssl, host, cause = this)
    is IOException -> ApiException(ApiException.Kind.Network, host, cause = this)
    else -> ApiException(ApiException.Kind.Invalid, host, cause = this)
}

class ResponseTooLargeException : IOException("Response exceeds the size limit")

fun hostOf(address: String?): String? {
    if (address.isNullOrBlank()) return null
    val withoutScheme = address.trim().substringAfter("://")
    val withoutUser = withoutScheme.substringAfterLast('@')
    return withoutUser
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringBefore(':')
        .takeIf { it.isNotEmpty() }
}
