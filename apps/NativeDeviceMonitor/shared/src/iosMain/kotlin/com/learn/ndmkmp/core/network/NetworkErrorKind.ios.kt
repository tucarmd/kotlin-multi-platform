package com.learn.ndmkmp.core.network

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDataNotAllowed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut

// Darwin engine wraps the NSURLSession error: same codes the old Swift ApiClient switched on.
actual fun platformNetworkErrorKind(e: Throwable): NetworkErrorKind {
    val error = (e as? DarwinHttpRequestException)?.origin ?: return NetworkErrorKind.OTHER
    if (error.domain != NSURLErrorDomain) return NetworkErrorKind.OTHER
    return when (error.code) {
        NSURLErrorTimedOut -> NetworkErrorKind.TIMEOUT
        NSURLErrorNotConnectedToInternet, NSURLErrorCannotFindHost, NSURLErrorCannotConnectToHost,
        NSURLErrorNetworkConnectionLost, NSURLErrorDataNotAllowed -> NetworkErrorKind.NO_NETWORK
        else -> NetworkErrorKind.OTHER
    }
}
