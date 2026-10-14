package dev.devicepulse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.devicepulse.data.HistoryRepository
import dev.devicepulse.data.Snapshot
import dev.devicepulse.model.BatteryInfo
import dev.devicepulse.model.LocationFix
import dev.devicepulse.model.NetworkStatus
import dev.devicepulse.model.Reading
import dev.devicepulse.sensor.BatterySensor
import dev.devicepulse.sensor.LatencyProbe
import dev.devicepulse.sensor.LocationSensor
import dev.devicepulse.sensor.NetworkSensor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val battery: Reading<BatteryInfo> = Reading.Loading,
    val location: Reading<LocationFix> = Reading.Loading,
    val network: Reading<NetworkStatus> = Reading.Loading,
    /** null = never measured; Loading = measuring now. */
    val latencyMs: Reading<Long>? = null,
)

class DashboardViewModel(
    battery: BatterySensor,
    location: LocationSensor,
    network: NetworkSensor,
    private val probe: LatencyProbe,
    private val history: HistoryRepository,
) : ViewModel() {

    private val latency = MutableStateFlow<Reading<Long>?>(null)

    // Bumped after a permission grant so the location flow re-subscribes and re-checks permission.
    private val locationRestarts = MutableStateFlow(0)

    val state: StateFlow<DashboardState> = combine(
        battery.observe(),
        locationRestarts.flatMapLatest { location.observe() },
        network.observe(),
        latency,
    ) { b, l, n, ms -> DashboardState(b, l, n, ms) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    val snapshots: StateFlow<List<Snapshot>> = history.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun measureLatency() {
        if (latency.value == Reading.Loading) return
        latency.value = Reading.Loading
        viewModelScope.launch { latency.value = probe.measure() }
    }

    fun refreshLocation() {
        locationRestarts.value++
    }

    fun saveSnapshot() {
        viewModelScope.launch { history.save(state.value) }
    }

    fun clearHistory() {
        viewModelScope.launch { history.clear() }
    }
}
