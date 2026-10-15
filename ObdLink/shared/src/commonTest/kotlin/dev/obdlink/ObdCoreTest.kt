package dev.obdlink

import dev.obdlink.core.ObdCore
import dev.obdlink.core.ObdTiming
import dev.obdlink.model.CodesState
import dev.obdlink.model.ConnectionState
import dev.obdlink.model.ConsoleKind
import dev.obdlink.model.ConsoleLine
import dev.obdlink.model.ObdError
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * [App 5 · B7] ObdCore state machine against FakeElmTransport, on virtual time.
 * The core runs in backgroundScope (its loops never end by themselves); each step advances the clock.
 */
class ObdCoreTest {

    private val timing = ObdTiming(
        scanDuration = 1.seconds,
        connectTimeout = 2.seconds,
        reconnectDelay = 100.milliseconds,
    )

    private fun TestScope.core(fake: FakeElmTransport) =
        ObdCore(fake, backgroundScope, timing, testScheduler.timeSource)

    /** Records every connection state, to check the path and not only the end. */
    private fun TestScope.recordStates(core: ObdCore): List<ConnectionState> {
        val seen = mutableListOf<ConnectionState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            core.state.collect { if (seen.lastOrNull() != it.connection) seen += it.connection }
        }
        return seen
    }

    private fun TestScope.connectReady(fake: FakeElmTransport, core: ObdCore) {
        core.connect(fake.dongle)
        advanceTimeBy(1.seconds)
        assertIs<ConnectionState.Ready>(core.state.value.connection)
    }

    @Test
    fun `scan lists the dongle once and stops after the scan duration`() = runTest {
        val fake = FakeElmTransport()
        val core = core(fake)
        core.scan()
        advanceTimeBy(500.milliseconds)
        assertEquals(ConnectionState.Scanning, core.state.value.connection)
        assertEquals(listOf(fake.dongle.copy(rssi = -50)), core.state.value.dongles)

        advanceTimeBy(1.seconds)
        assertEquals(ConnectionState.Idle, core.state.value.connection)
        assertEquals(1, core.state.value.dongles.size) // kept for the user to pick
    }

    @Test
    fun `bluetooth off during scan ends in BluetoothOff`() = runTest {
        val fake = FakeElmTransport().apply { bluetoothOff = true }
        val core = core(fake)
        core.scan()
        runCurrent()
        assertEquals(ConnectionState.Error(ObdError.BluetoothOff), core.state.value.connection)
    }

    @Test
    fun `connect runs the init sequence and reaches Ready`() = runTest {
        val fake = FakeElmTransport()
        val core = core(fake)
        val states = recordStates(core)
        connectReady(fake, core)

        assertEquals(listOf("ATZ", "ATE0", "ATL0", "ATSP0"), fake.sent)
        assertEquals("ELM327 v1.5", core.state.value.adapterVersion)
        assertEquals(
            listOf(
                ConnectionState.Idle,
                ConnectionState.Connecting(fake.dongle),
                ConnectionState.Initializing(fake.dongle),
                ConnectionState.Ready(fake.dongle),
            ),
            states,
        )
    }

    @Test
    fun `init that gets a question mark ends in SetupFailed`() = runTest {
        val fake = FakeElmTransport().apply { answers["ATE0"] = "?\r\r>" }
        val core = core(fake)
        core.connect(fake.dongle)
        advanceTimeBy(1.seconds)
        assertEquals(ConnectionState.Error(ObdError.SetupFailed), core.state.value.connection)
    }

    @Test
    fun `connect failure ends in ConnectFailed and reset goes back to Idle`() = runTest {
        val fake = FakeElmTransport().apply { failConnects = 1 }
        val core = core(fake)
        core.connect(fake.dongle)
        advanceTimeBy(1.seconds)
        assertEquals(ConnectionState.Error(ObdError.ConnectFailed), core.state.value.connection)

        core.reset()
        runCurrent()
        assertEquals(ConnectionState.Idle, core.state.value.connection)
    }

    @Test
    fun `live polling shows rpm speed and coolant at least twice a second`() = runTest {
        val fake = FakeElmTransport()
        val core = core(fake)
        connectReady(fake, core)

        core.startLive()
        advanceTimeBy(1.seconds)
        val live = core.state.value.live
        assertIs<ConnectionState.Polling>(core.state.value.connection)
        assertEquals(1726, live.rpm?.value)
        assertEquals(50, live.speedKmh?.value)
        assertEquals(83, live.coolantC?.value)
        assertTrue(live.refreshHz >= 2.0, "refreshHz was ${live.refreshHz}")

        core.stopLive()
        runCurrent()
        assertIs<ConnectionState.Ready>(core.state.value.connection)
    }

    @Test
    fun `NO DATA clears a gauge instead of keeping a stale value`() = runTest {
        val fake = FakeElmTransport()
        val core = core(fake)
        connectReady(fake, core)
        core.startLive()
        advanceTimeBy(500.milliseconds)
        assertEquals(50, core.state.value.live.speedKmh?.value)

        fake.answers["010D"] = "NO DATA\r\r>"
        advanceTimeBy(500.milliseconds)
        assertEquals(null, core.state.value.live.speedKmh)
        assertEquals(1726, core.state.value.live.rpm?.value)
    }

    @Test
    fun `link lost while polling reconnects and resumes polling`() = runTest {
        val fake = FakeElmTransport()
        val core = core(fake)
        connectReady(fake, core)
        core.startLive()
        advanceTimeBy(500.milliseconds)
        val states = recordStates(core)

        fake.dropLink()
        runCurrent()
        assertIs<ConnectionState.Reconnecting>(core.state.value.connection)
        assertEquals(null, core.state.value.live.rpm) // stale values are cleared

        advanceTimeBy(2.seconds)
        assertIs<ConnectionState.Polling>(core.state.value.connection)
        assertEquals(1726, core.state.value.live.rpm?.value)
        assertEquals(2, fake.connectCalls)
        assertTrue(states.any { it is ConnectionState.Initializing }, "re-init after reconnect: $states")
    }

    @Test
    fun `three failed reconnects end in ReconnectFailed`() = runTest {
        val fake = FakeElmTransport()
        val core = core(fake)
        val states = recordStates(core)
        connectReady(fake, core)
        core.startLive()
        advanceTimeBy(300.milliseconds)

        fake.failConnects = 3
        fake.dropLink()
        advanceTimeBy(5.seconds)

        assertEquals(ConnectionState.Error(ObdError.ReconnectFailed), core.state.value.connection)
        assertEquals(4, fake.connectCalls) // first connect + 3 attempts
        val attempts = states.filterIsInstance<ConnectionState.Reconnecting>().map { it.attempt }
        assertEquals(listOf(1, 2, 3), attempts)
    }

    @Test
    fun `silent dongle while polling counts as a lost link`() = runTest {
        val fake = FakeElmTransport()
        val core = core(fake)
        connectReady(fake, core)
        core.startLive()
        advanceTimeBy(300.milliseconds)

        val states = recordStates(core)

        fake.silent += listOf("010C", "010D", "0105") // BLE still "connected", dongle frozen
        advanceTimeBy(37.seconds) // 3 rounds × 3 PIDs × (2 s + retry 2 s) = 36 s
        assertTrue(states.any { it is ConnectionState.Reconnecting }, "states: $states")
    }

    @Test
    fun `43 01 03 01 shows P0301 on the Codes tab`() = runTest {
        val fake = FakeElmTransport()
        val core = core(fake)
        connectReady(fake, core)

        core.readCodes()
        advanceTimeBy(500.milliseconds)
        val codes = assertIs<CodesState.Loaded>(core.state.value.codes)
        assertEquals(listOf("P0301"), codes.codes.map { it.code })
    }

    @Test
    fun `clear codes sends nothing until confirmed`() = runTest {
        val fake = FakeElmTransport()
        val core = core(fake)
        connectReady(fake, core)
        core.readCodes()
        advanceTimeBy(500.milliseconds)

        core.requestClearCodes()
        advanceTimeBy(500.milliseconds)
        assertIs<CodesState.ConfirmClear>(core.state.value.codes)
        assertFalse("04" in fake.sent)

        core.cancelClearCodes()
        runCurrent()
        assertIs<CodesState.Loaded>(core.state.value.codes)
        assertFalse("04" in fake.sent)

        fake.answers["03"] = "43 00\r\r>" // after clearing the car reports nothing
        core.requestClearCodes()
        core.confirmClearCodes()
        advanceTimeBy(500.milliseconds)
        assertTrue("04" in fake.sent)
        assertEquals(CodesState.Loaded(emptyList()), core.state.value.codes)
    }

    @Test
    fun `codes and live polling share the queue without mixing answers`() = runTest {
        val fake = FakeElmTransport()
        val core = core(fake)
        connectReady(fake, core)
        core.startLive()
        advanceTimeBy(300.milliseconds)
        core.readCodes()
        advanceTimeBy(500.milliseconds)

        assertEquals(listOf("P0301"), assertIs<CodesState.Loaded>(core.state.value.codes).codes.map { it.code })
        assertEquals(1726, core.state.value.live.rpm?.value)
        assertEquals(1, fake.maxInFlight)
    }

    @Test
    fun `console logs both directions and refuses mode 04`() = runTest {
        val fake = FakeElmTransport()
        val core = core(fake)
        connectReady(fake, core)
        core.clearConsole()

        core.sendConsole("at rv")
        core.sendConsole("04")
        core.sendConsole("hello")
        advanceTimeBy(500.milliseconds)

        assertEquals(
            listOf(
                ConsoleLine(ConsoleKind.ClearBlocked, "04"),
                ConsoleLine(ConsoleKind.InvalidInput, "hello"),
                ConsoleLine(ConsoleKind.Sent, "ATRV"),
                ConsoleLine(ConsoleKind.Received, "12.6V"),
            ),
            core.state.value.console,
        )
        assertFalse("04" in fake.sent)
    }

    @Test
    fun `console before connecting says not connected`() = runTest {
        val core = core(FakeElmTransport())
        core.sendConsole("010C")
        runCurrent()
        assertEquals(listOf(ConsoleLine(ConsoleKind.NotConnected, "010C")), core.state.value.console)
    }

    @Test
    fun `disconnect goes back to Idle and clears live values`() = runTest {
        val fake = FakeElmTransport()
        val core = core(fake)
        connectReady(fake, core)
        core.startLive()
        advanceTimeBy(300.milliseconds)

        core.disconnect()
        advanceTimeBy(100.milliseconds)
        assertEquals(ConnectionState.Idle, core.state.value.connection)
        assertEquals(null, core.state.value.live.rpm)
    }
}
