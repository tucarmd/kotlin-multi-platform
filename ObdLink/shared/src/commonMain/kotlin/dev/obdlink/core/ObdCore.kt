package dev.obdlink.core

import dev.obdlink.model.CodesFailure
import dev.obdlink.model.CodesState
import dev.obdlink.model.ConnectionState
import dev.obdlink.model.ConsoleKind
import dev.obdlink.model.ConsoleLine
import dev.obdlink.model.LiveData
import dev.obdlink.model.ObdError
import dev.obdlink.model.ObdState
import dev.obdlink.model.Reading
import dev.obdlink.obd.model.Dongle
import dev.obdlink.obd.model.LinkStatus
import dev.obdlink.obd.model.ObdCommand
import dev.obdlink.obd.model.ObdPid
import dev.obdlink.obd.model.ObdResult
import dev.obdlink.obd.rules.ConsoleInput
import dev.obdlink.obd.rules.decodeDtcResponse
import dev.obdlink.obd.rules.decodePidAnswer
import dev.obdlink.obd.rules.displayAnswer
import dev.obdlink.obd.rules.isClearConfirmed
import dev.obdlink.obd.rules.parseConsoleInput
import dev.obdlink.obd.transport.BluetoothUnavailableException
import dev.obdlink.obd.transport.CommandTimeoutException
import dev.obdlink.obd.transport.ElmTransport
import dev.obdlink.obd.transport.LinkLostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * [App 5 · B2] ObdCore — the connection state machine plus every user action of the four tabs.
 *
 * Idle → Scanning → Connecting → Initializing → Ready ⇄ Polling; a lost link goes to Reconnecting (3 attempts,
 * each one runs Initializing again) and then back to Ready/Polling, or to Error. `reset()` brings Error back to Idle.
 *
 * Why every action is `scope.launch { … }`: the scope runs on ONE thread at a time (Koin gives it
 * `Dispatchers.Default.limitedParallelism(1)`), so the job fields below are never touched by two threads, and
 * actions run in the order the user fired them. The UI only reads [state].
 *
 * Why no exception reaches the UI: every transport / queue failure is caught here and becomes a state branch
 * (ConnectionState.Error, CodesState.Failed, a console NoAnswer line).
 *
 * Used by: ObdViewModel (Android), ObdModel (iOS) via Koin. Uses: ElmTransport, CommandQueue, :core:obd rules.
 */
class ObdCore(
    private val transport: ElmTransport,
    private val scope: CoroutineScope,
    private val timing: ObdTiming = ObdTiming(),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val queue = CommandQueue(transport, timing.commandTimeout, timing.searchTimeout)

    private val _state = MutableStateFlow(ObdState())

    /** Everything the tabs draw. Android: collectAsStateWithLifecycle(). Swift: `for await` (SKIE). */
    val state: StateFlow<ObdState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var sessionJob: Job? = null // connect, init, reconnect
    private var liveJob: Job? = null
    private var watchJob: Job? = null
    private var codesJob: Job? = null
    private var codesBeforeConfirm: CodesState = CodesState.NotRead

    private val connection: ConnectionState get() = _state.value.connection

    // ---------------------------------------------------------------- Connect tab

    /** Idle → Scanning for [ObdTiming.scanDuration], collecting dongles; then back to Idle with the list kept. */
    fun scan() = act {
        if (connection != ConnectionState.Idle && connection != ConnectionState.Scanning) return@act
        scanJob?.cancel()
        _state.update { it.copy(connection = ConnectionState.Scanning, dongles = emptyList()) }
        scanJob = scope.launch {
            try {
                withTimeoutOrNull(timing.scanDuration) {
                    transport.scan().collect { found -> _state.update { it.copy(dongles = it.dongles.upsert(found)) } }
                }
                if (connection == ConnectionState.Scanning) setConnection(ConnectionState.Idle)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BluetoothUnavailableException) {
                setConnection(ConnectionState.Error(ObdError.BluetoothOff))
            } catch (e: Exception) {
                setConnection(ConnectionState.Error(ObdError.ScanFailed))
            }
        }
    }

    /** Stops a running scan early. The dongles found so far stay listed. */
    fun stopScan() = act {
        scanJob?.cancel()
        scanJob = null
        if (connection == ConnectionState.Scanning) setConnection(ConnectionState.Idle)
    }

    /**
     * Connecting → Initializing → Ready. Any failure ends in Error(ConnectFailed / SetupFailed / BluetoothOff).
     * Only allowed from Idle or Scanning (a running scan is stopped first: scanning slows connecting down).
     */
    fun connect(dongle: Dongle) = act {
        if (connection != ConnectionState.Idle && connection != ConnectionState.Scanning) return@act
        scanJob?.cancel()
        scanJob = null
        sessionJob = scope.launch {
            setConnection(ConnectionState.Connecting(dongle))
            openLink(dongle)?.let { error ->
                setConnection(ConnectionState.Error(error))
                return@launch
            }
            if (!initialize(dongle)) {
                safeDisconnect()
                setConnection(ConnectionState.Error(ObdError.SetupFailed))
                return@launch
            }
            onReady(dongle, resumeLive = false)
        }
    }

    /** Closes the link on purpose from any state and goes back to Idle. Live values and codes are cleared. */
    fun disconnect() = act {
        stopEverything()
        safeDisconnect()
    }

    /** Error → Idle, so the user can scan again. */
    fun reset() = act {
        if (connection !is ConnectionState.Error) return@act
        stopEverything()
        safeDisconnect()
    }

    // ---------------------------------------------------------------- Live tab

    /** Ready → Polling: asks RPM, speed, coolant in turn until [stopLive] or the link drops. */
    fun startLive() = act { startLiveNow() }

    /** Polling → Ready. The last values stay on screen; the refresh rate drops to 0. */
    fun stopLive() = act {
        liveJob?.cancel()
        liveJob = null
        val current = connection
        if (current is ConnectionState.Polling) setConnection(ConnectionState.Ready(current.dongle))
        _state.update { it.copy(live = it.live.copy(refreshHz = 0.0)) }
    }

    // ---------------------------------------------------------------- Codes tab

    /** Mode 03. Works while Ready or Polling (the queue interleaves it with the live PIDs). */
    fun readCodes() = act {
        if (!_state.value.isConnected || _state.value.codes.isBusy()) return@act
        setCodes(CodesState.Reading)
        codesJob = scope.launch { setCodes(readCodesNow()) }
    }

    /** First half of "clear codes": only shows the confirmation. Nothing is sent yet. */
    fun requestClearCodes() = act {
        val codes = _state.value.codes
        if (!_state.value.isConnected || codes.isBusy() || codes is CodesState.ConfirmClear) return@act
        codesBeforeConfirm = codes
        setCodes(CodesState.ConfirmClear((codes as? CodesState.Loaded)?.codes ?: emptyList()))
    }

    /** The user said no: back to what was shown before. */
    fun cancelClearCodes() = act {
        if (_state.value.codes is CodesState.ConfirmClear) setCodes(codesBeforeConfirm)
    }

    /**
     * The user confirmed: send mode 04, then read the codes again to show the result.
     * TRAP: mode 04 also resets readiness monitors; this is the ONLY place that sends it. TEST: ObdCoreTest.
     */
    fun confirmClearCodes() = act {
        if (_state.value.codes !is CodesState.ConfirmClear || !_state.value.isConnected) return@act
        setCodes(CodesState.Clearing)
        codesJob = scope.launch {
            val exchange = exchangeLogged(ObdCommand.ClearDtc.text)
            val result = exchange?.result
            setCodes(
                when {
                    exchange == null -> CodesState.Failed(CodesFailure.NoAnswer)
                    result is ObdResult.Data && isClearConfirmed(result.messages) -> readCodesNow()
                    else -> CodesState.Failed(CodesFailure.Rejected)
                },
            )
        }
    }

    // ---------------------------------------------------------------- Console tab

    /** Sends one typed line (AT or hex) and logs both directions. Mode 04 is refused here (see ConsoleInput). */
    fun sendConsole(typed: String) = act {
        when (val input = parseConsoleInput(typed)) {
            ConsoleInput.Empty -> Unit
            ConsoleInput.Invalid -> log(ConsoleKind.InvalidInput, typed.trim())
            ConsoleInput.ClearBlocked -> log(ConsoleKind.ClearBlocked, typed.trim())
            is ConsoleInput.Command ->
                if (!_state.value.isConnected) {
                    log(ConsoleKind.NotConnected, input.text)
                } else {
                    scope.launch { exchangeLogged(input.text) }
                }
        }
    }

    /** Empties the console log. */
    fun clearConsole() = act { _state.update { it.copy(console = emptyList()) } }

    // ---------------------------------------------------------------- internals

    private fun act(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    /** Opens the BLE link with a time limit. Returns null on success, otherwise the reason. */
    private suspend fun openLink(dongle: Dongle): ObdError? = try {
        withTimeout(timing.connectTimeout) { transport.connect(dongle) }
        null
    } catch (e: TimeoutCancellationException) {
        safeDisconnect()
        ObdError.ConnectFailed
    } catch (e: CancellationException) {
        throw e
    } catch (e: BluetoothUnavailableException) {
        ObdError.BluetoothOff
    } catch (e: Exception) {
        safeDisconnect()
        ObdError.ConnectFailed
    }

    /** Initializing: ATZ, ATE0, ATL0, ATSP0. False if any of them fails or gets no answer. */
    private suspend fun initialize(dongle: Dongle): Boolean {
        setConnection(ConnectionState.Initializing(dongle))
        for (command in ObdCommand.InitSequence) {
            val exchange = exchangeLogged(command.text) ?: return false
            when (val result = exchange.result) {
                is ObdResult.Error -> return false
                is ObdResult.Text -> if (command == ObdCommand.Reset) _state.update { it.copy(adapterVersion = result.text) }
                else -> Unit
            }
        }
        return true
    }

    private fun onReady(dongle: Dongle, resumeLive: Boolean) {
        setConnection(ConnectionState.Ready(dongle))
        watchJob?.cancel()
        watchJob = scope.launch {
            // TRAP: wait for the NEXT Lost. After a reconnect the link is Connected again, so an old Lost is gone.
            transport.link.first { it == LinkStatus.Lost }
            linkLost()
        }
        if (resumeLive) startLiveNow()
    }

    private fun startLiveNow() {
        val current = connection as? ConnectionState.Ready ?: return
        setConnection(ConnectionState.Polling(current.dongle))
        liveJob = scope.launch { pollLoop() }
    }

    /**
     * One round = the three PIDs. Timeouts keep the old value; NO DATA clears it (engine off).
     * [ObdTiming.maxSilentRounds] rounds without a single answer = the link is dead even if BLE did not say so.
     */
    private suspend fun pollLoop() {
        var silentRounds = 0
        var roundStart = timeSource.markNow()
        while (true) {
            var answered = 0
            for (pid in LIVE_PIDS) {
                val exchange = try {
                    queue.send(ObdCommand.Pid(pid))
                } catch (e: CommandTimeoutException) {
                    null
                } catch (e: LinkLostException) {
                    linkLost()
                    return
                }
                if (exchange == null) continue
                answered++
                val reading = (exchange.result as? ObdResult.Data)
                    ?.let { decodePidAnswer(pid, it.messages) }
                    ?.let { Reading(it.value) }
                _state.update { it.copy(live = it.live.with(pid, reading)) }
            }
            silentRounds = if (answered == 0) silentRounds + 1 else 0
            if (silentRounds >= timing.maxSilentRounds) {
                linkLost()
                return
            }
            val elapsed = roundStart.elapsedNow()
            roundStart = timeSource.markNow()
            val hz = if (elapsed > Duration.ZERO) 1.seconds / elapsed else 0.0
            _state.update { it.copy(live = it.live.copy(refreshHz = hz)) }
            delay(timing.pollGap) // also gives the Codes / Console commands a gap to get the queue
        }
    }

    /**
     * Ready/Polling → Reconnecting. Not suspending on purpose: it may be called from inside the live loop,
     * which it cancels, so the reconnect must already be launched in its own job before that.
     */
    private fun linkLost() {
        val current = connection
        if (current !is ConnectionState.Ready && current !is ConnectionState.Polling) return
        val resume = current is ConnectionState.Polling
        val dongle = current.dongleOrNull ?: return
        // Set synchronously: the watcher and the live loop can both notice the loss; the second call must see
        // Reconnecting and do nothing, or two reconnect loops would fight over the link.
        _state.update {
            it.copy(connection = ConnectionState.Reconnecting(dongle, 1, timing.maxReconnects), live = LiveData())
        }
        watchJob?.cancel()
        sessionJob = scope.launch { reconnect(dongle, resume) }
        val live = liveJob
        liveJob = null
        live?.cancel() // may be the caller itself: cancel last
    }

    // TEST: ObdCoreTest.`link lost while polling reconnects and resumes polling`
    private suspend fun reconnect(dongle: Dongle, resumeLive: Boolean) {
        for (attempt in 1..timing.maxReconnects) {
            setConnection(ConnectionState.Reconnecting(dongle, attempt, timing.maxReconnects))
            delay(timing.reconnectDelay)
            safeDisconnect()
            if (openLink(dongle) == null) {
                if (initialize(dongle)) {
                    onReady(dongle, resumeLive)
                    return
                }
                safeDisconnect()
            }
        }
        setConnection(ConnectionState.Error(ObdError.ReconnectFailed))
    }

    private suspend fun readCodesNow(): CodesState {
        val exchange = exchangeLogged(ObdCommand.ReadDtc.text) ?: return CodesState.Failed(CodesFailure.NoAnswer)
        return when (val result = exchange.result) {
            // Some ECUs answer "NO DATA" instead of "43 00" when nothing is stored.
            ObdResult.NoData -> CodesState.Loaded(emptyList())
            is ObdResult.Data ->
                if (result.messages.any { it.firstOrNull() == 0x43 }) {
                    CodesState.Loaded(decodeDtcResponse(result.messages))
                } else {
                    CodesState.Failed(CodesFailure.Rejected) // e.g. 7F 03 xx negative answer
                }
            else -> CodesState.Failed(CodesFailure.Rejected)
        }
    }

    /** Sends through the queue and writes both directions to the console. Null = no answer / link lost. */
    private suspend fun exchangeLogged(text: String) = try {
        log(ConsoleKind.Sent, text)
        queue.sendText(text).also { log(ConsoleKind.Received, displayAnswer(it.raw, text)) }
    } catch (e: CommandTimeoutException) {
        log(ConsoleKind.NoAnswer, text)
        null
    } catch (e: LinkLostException) {
        log(ConsoleKind.NoAnswer, text)
        null
    }

    private fun stopEverything() {
        listOf(scanJob, sessionJob, liveJob, watchJob, codesJob).forEach { it?.cancel() }
        scanJob = null
        sessionJob = null
        liveJob = null
        watchJob = null
        codesJob = null
        _state.update { it.copy(connection = ConnectionState.Idle, live = LiveData(), codes = CodesState.NotRead) }
    }

    private suspend fun safeDisconnect() {
        try {
            transport.disconnect()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Already gone: nothing to close.
        }
    }

    private fun setConnection(connection: ConnectionState) = _state.update { it.copy(connection = connection) }

    private fun setCodes(codes: CodesState) = _state.update { it.copy(codes = codes) }

    private fun log(kind: ConsoleKind, text: String) = _state.update {
        it.copy(console = (it.console + ConsoleLine(kind, text)).takeLast(ObdState.CONSOLE_LIMIT))
    }

    private fun CodesState.isBusy() = this == CodesState.Reading || this == CodesState.Clearing

    private fun List<Dongle>.upsert(found: Dongle): List<Dongle> =
        if (any { it.id == found.id }) map { if (it.id == found.id) found else it } else this + found

    private fun LiveData.with(pid: ObdPid, reading: Reading?) = when (pid) {
        ObdPid.EngineRpm -> copy(rpm = reading)
        ObdPid.VehicleSpeed -> copy(speedKmh = reading)
        ObdPid.CoolantTemp -> copy(coolantC = reading)
    }

    private companion object {
        val LIVE_PIDS = listOf(ObdPid.EngineRpm, ObdPid.VehicleSpeed, ObdPid.CoolantTemp)
    }
}

/**
 * [App 5 · B2] Every time limit of the state machine in one place, so tests can shrink them and readers find them.
 */
data class ObdTiming(
    val scanDuration: Duration = 10.seconds,
    /** Android's own direct-connect timeout is ~30 s; we give up earlier. */
    val connectTimeout: Duration = 15.seconds,
    val commandTimeout: Duration = 2.seconds,
    val searchTimeout: Duration = 10.seconds,
    val reconnectDelay: Duration = 2.seconds,
    val maxReconnects: Int = 3,
    val pollGap: Duration = 50.milliseconds,
    val maxSilentRounds: Int = 3,
)
