package com.learn.ndmkmp.core.network

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException

// Airplane mode → UnknownHostException (no DNS) · server down → ConnectException. OkHttp throws them unwrapped.
actual fun platformNetworkErrorKind(e: Throwable): NetworkErrorKind = when (e) {
    is UnknownHostException, is ConnectException, is NoRouteToHostException -> NetworkErrorKind.NO_NETWORK
    is java.net.SocketTimeoutException -> NetworkErrorKind.TIMEOUT
    else -> NetworkErrorKind.OTHER
}
