package dev.devicepulse.sensor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dev.devicepulse.model.ConnectionType
import dev.devicepulse.model.NetworkStatus
import dev.devicepulse.model.Reading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidNetworkSensor(private val context: Context) : NetworkSensor {

    override fun observe(): Flow<Reading<NetworkStatus>> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val offline = Reading.Available(NetworkStatus(ConnectionType.None, isOnline = false))

        val current = manager.activeNetwork?.let { manager.getNetworkCapabilities(it) }
        trySend(current?.toReading() ?: offline)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.toReading())
            }

            override fun onLost(network: Network) {
                trySend(offline)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun NetworkCapabilities.toReading(): Reading<NetworkStatus> {
        val type = when {
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.Wifi
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.Cellular
            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.Ethernet
            else -> ConnectionType.Unknown
        }
        val online = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return Reading.Available(NetworkStatus(type, online))
    }
}
