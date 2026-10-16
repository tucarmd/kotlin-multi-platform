package dev.devicepulse.sensor

import dev.devicepulse.model.ConnectionType
import dev.devicepulse.model.NetworkStatus
import dev.devicepulse.model.Reading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_interface_type_wired
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_t
import platform.Network.nw_path_uses_interface_type
import platform.darwin.dispatch_get_main_queue

class IosNetworkSensor : NetworkSensor {

    override fun observe(): Flow<Reading<NetworkStatus>> = callbackFlow {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path -> trySend(path.toReading()) }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
        awaitClose { nw_path_monitor_cancel(monitor) }
    }.distinctUntilChanged()

    private fun nw_path_t.toReading(): Reading<NetworkStatus> {
        val online = nw_path_get_status(this) == nw_path_status_satisfied
        val type = when {
            !online -> ConnectionType.None
            nw_path_uses_interface_type(this, nw_interface_type_wifi) -> ConnectionType.Wifi
            nw_path_uses_interface_type(this, nw_interface_type_cellular) -> ConnectionType.Cellular
            nw_path_uses_interface_type(this, nw_interface_type_wired) -> ConnectionType.Ethernet
            else -> ConnectionType.Unknown
        }
        return Reading.Available(NetworkStatus(type, online))
    }
}
