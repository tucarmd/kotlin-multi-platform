package dev.devicepulse

import app.cash.turbine.test
import dev.devicepulse.data.HistoryRepository
import dev.devicepulse.data.Snapshot
import dev.devicepulse.model.BatteryInfo
import dev.devicepulse.model.ChargingState
import dev.devicepulse.model.ConnectionType
import dev.devicepulse.model.LocationFix
import dev.devicepulse.model.NetworkStatus
import dev.devicepulse.model.Reading
import dev.devicepulse.sensor.BatterySensor
import dev.devicepulse.sensor.LatencyProbe
import dev.devicepulse.sensor.LocationSensor
import dev.devicepulse.sensor.NetworkSensor
import dev.devicepulse.ui.DashboardState
import dev.devicepulse.ui.DashboardViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val battery = MutableStateFlow<Reading<BatteryInfo>>(Reading.Loading)
    private val network = MutableStateFlow<Reading<NetworkStatus>>(Reading.Loading)
    private var locationSubscriptions = 0
    private var locationResult: Reading<LocationFix> = Reading.PermissionDenied
    private val history = FakeHistory()

    private fun viewModel(status: HttpStatusCode = HttpStatusCode.NoContent) = DashboardViewModel(
        battery = object : BatterySensor { override fun observe() = battery },
        location = object : LocationSensor {
            override fun observe(): Flow<Reading<LocationFix>> {
                locationSubscriptions++
                return MutableStateFlow(locationResult)
            }
        },
        network = object : NetworkSensor { override fun observe() = network },
        probe = LatencyProbe(HttpClient(MockEngine { respond("", status) })),
        history = history,
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun stateCombinesAllSensors() = runTest(dispatcher) {
        val vm = viewModel()
        vm.state.test {
            assertEquals(DashboardState(), awaitItem())
            battery.value = Reading.Available(BatteryInfo(15, ChargingState.Charging))
            network.value = Reading.Available(NetworkStatus(ConnectionType.Wifi, isOnline = true))
            advanceUntilIdle()
            val latest = expectMostRecentItem()
            assertEquals(BatteryInfo(15, ChargingState.Charging), (latest.battery as Reading.Available).value)
            assertEquals(Reading.PermissionDenied, latest.location)
            assertEquals(ConnectionType.Wifi, (latest.network as Reading.Available).value.type)
        }
    }

    @Test
    fun refreshLocationResubscribesSoAGrantIsPickedUp() = runTest(dispatcher) {
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            advanceUntilIdle()
            assertEquals(Reading.PermissionDenied, expectMostRecentItem().location)

            val fix = LocationFix(10.77, 106.70, 8.0, null, Instant.fromEpochMilliseconds(0))
            locationResult = Reading.Available(fix)
            vm.refreshLocation()
            advanceUntilIdle()
            assertEquals(Reading.Available(fix), expectMostRecentItem().location)
            assertEquals(2, locationSubscriptions)
        }
    }

    @Test
    fun measureLatencyGoesThroughLoadingToResult() = runTest(dispatcher) {
        val vm = viewModel(HttpStatusCode.BadGateway)
        vm.state.test {
            awaitItem()
            vm.measureLatency()
            // MockEngine answers on a real dispatcher, so wait for emissions instead of advancing virtual time.
            var latency = awaitItem().latencyMs
            while (latency == null || latency == Reading.Loading) latency = awaitItem().latencyMs
            assertEquals(Reading.Error("HTTP 502"), latency)
        }
    }

    @Test
    fun saveSnapshotStoresCurrentState() = runTest(dispatcher) {
        val vm = viewModel()
        vm.state.test {
            battery.value = Reading.Available(BatteryInfo(80, ChargingState.Full))
            advanceUntilIdle()
            vm.saveSnapshot()
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        val saved = history.saved.single()
        assertIs<Reading.Available<BatteryInfo>>(saved.battery)
        assertEquals(80, saved.battery.value.levelPercent)
    }

    private class FakeHistory : HistoryRepository {
        val saved = mutableListOf<DashboardState>()
        override fun observe(): Flow<List<Snapshot>> = MutableStateFlow(emptyList())
        override suspend fun save(state: DashboardState) {
            saved += state
        }
        override suspend fun clear() = saved.clear()
    }
}
