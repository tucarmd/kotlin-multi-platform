package dev.obdlink.ble

import com.juul.kable.Filter
import com.juul.kable.Peripheral
import com.juul.kable.PlatformAdvertisement
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import com.juul.kable.indicate
import com.juul.kable.notify
import com.juul.kable.write
import com.juul.kable.writeWithoutResponse
import dev.obdlink.obd.model.Dongle
import dev.obdlink.obd.model.LinkStatus
import dev.obdlink.obd.transport.BluetoothUnavailableException
import dev.obdlink.obd.transport.ElmTransport
import dev.obdlink.obd.transport.LinkLostException
import dev.obdlink.obd.transport.PromptCollector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

/**
 * [App 5 · B3] KableElmTransport — [ElmTransport] over Bluetooth LE, written once for Android and iOS with Kable.
 *
 * Flow of one command: write "010C\r" to the write characteristic → the dongle answers through notifications on the
 * notify characteristic, possibly in several chunks → [PromptCollector] glues them until `>` → return the text.
 *
 * Why Kable and not BluetoothGatt + CoreBluetooth twice: Kable wraps both behind one coroutine API
 * (`connect()` suspends until services are discovered, `observe()` is a Flow, `write()` suspends until sent),
 * so this file is common code. The price: Kable decides the GATT queueing, so we never write while a write is
 * pending (CommandQueue already guarantees one command at a time).
 *
 * Used by: ObdCore through the ElmTransport interface (bound in Koin's bleModule). Uses: Kable, ElmProfiles.
 */
class KableElmTransport(private val scope: CoroutineScope) : ElmTransport {

    private val _link = MutableStateFlow(LinkStatus.Disconnected)
    override val link: StateFlow<LinkStatus> = _link.asStateFlow()

    // Advertisements from the last scan, by id: Kable builds a Peripheral from an Advertisement.
    private val advertisements = mutableMapOf<String, PlatformAdvertisement>()
    private var peripheral: Peripheral? = null
    private var peripheralId: String? = null
    private var channel: ElmProfiles.Channel? = null
    private var writeType = WriteType.WithResponse
    private var inbox = Channel<String>(Channel.UNLIMITED)
    private var watchJob: Job? = null

    /**
     * Scans for ELM327-looking dongles: known service UUIDs OR known name prefixes (see [ElmProfiles]).
     * Any scan failure (radio off, permission missing) becomes [BluetoothUnavailableException].
     *
     * PLATFORM: Android ≤ 30 needs ACCESS_FINE_LOCATION to see any result, API 31+ needs BLUETOOTH_SCAN.
     * Without it Android does not fail loudly: the scan just returns nothing. The app asks before calling scan().
     * iOS shows its Bluetooth prompt by itself the first time CoreBluetooth starts.
     */
    override fun scan(): Flow<Dongle> = Scanner {
        filters {
            ElmProfiles.serviceUuids.forEach { uuid -> match { services = listOf(Uuid.parse(uuid)) } }
            ElmProfiles.namePrefixes.forEach { prefix -> match { name = Filter.Name.Prefix(prefix) } }
        }
    }.advertisements
        .map { ad ->
            // PLATFORM: Identifier is a MAC String on Android but a Uuid on Apple, hence toString().
            @Suppress("RedundantConversion")
            val id = ad.identifier.toString()
            advertisements[id] = ad
            Dongle(id = id, name = ad.name ?: ad.peripheralName.orEmpty(), rssi = ad.rssi)
        }
        .catch { e ->
            if (e is CancellationException) throw e
            throw BluetoothUnavailableException(e)
        }

    /**
     * Connects, finds the ELM327 serial characteristics and subscribes to notifications.
     * Returns only once the subscription is live, so the answer to the first command cannot be missed.
     */
    override suspend fun connect(dongle: Dongle) {
        val p = peripheralFor(dongle)
        // connect() suspends until GATT services are discovered. The returned scope dies with this connection.
        val connection = p.connect()

        val services = p.services.value.orEmpty()
        val gatt = services.map { service ->
            ElmProfiles.Service(
                uuid = service.serviceUuid.toString(),
                characteristics = service.characteristics.map {
                    ElmProfiles.Characteristic(
                        uuid = it.characteristicUuid.toString(),
                        canWrite = it.properties.write || it.properties.writeWithoutResponse,
                        canNotify = it.properties.notify || it.properties.indicate,
                    )
                },
            )
        }
        val picked = ElmProfiles.pick(gatt) ?: run {
            p.disconnect()
            error("No ELM327 serial service on ${dongle.name}")
        }
        val writeProps = services.flatMap { it.characteristics }
            .first { it.characteristicUuid.toString() == picked.write }.properties
        // Prefer acknowledged writes; some clones only offer write-without-response.
        writeType = if (writeProps.write) WriteType.WithResponse else WriteType.WithoutResponse

        val answers = Channel<String>(Channel.UNLIMITED)
        val subscribed = CompletableDeferred<Unit>()
        connection.launch {
            p.observe(characteristicOf(Uuid.parse(picked.service), Uuid.parse(picked.notify))) {
                subscribed.complete(Unit)
            }.collect { bytes -> answers.trySend(bytes.decodeToString()) }
        }
        // TRAP: observe() only enables notifications when collected, asynchronously. Writing before the CCCD write
        // finished loses the answer to the first command (ATZ), and init times out for no visible reason.
        withTimeout(5.seconds) { subscribed.await() }

        channel = picked
        inbox = answers
        _link.value = LinkStatus.Connected
        watchJob?.cancel()
        watchJob = scope.launch {
            p.state.collect { state ->
                // Disconnected while we still think we are Connected = nobody asked for it = Lost.
                if (state is State.Disconnected && _link.value == LinkStatus.Connected) {
                    _link.value = LinkStatus.Lost
                    answers.close() // wakes up a send() that is waiting for an answer
                }
            }
        }
    }

    override suspend fun disconnect() {
        // Mark Disconnected FIRST so the state watcher does not report this as Lost.
        _link.value = LinkStatus.Disconnected
        watchJob?.cancel()
        inbox.close()
        runCatching { peripheral?.disconnect() }
    }

    /**
     * Writes [command] + "\r" and collects notifications until the `>` prompt.
     *
     * TRAP: an answer can come in several notifications (20-byte BLE packets); only `>` means done (PromptCollector).
     * TRAP: a command that timed out may still answer later. We drop everything already in the inbox before
     * writing, otherwise that late answer would be returned as the answer to THIS command.
     */
    override suspend fun send(command: String): String {
        val p = peripheral
        val ch = channel
        if (p == null || ch == null || _link.value != LinkStatus.Connected) throw LinkLostException()
        val answers = inbox
        while (answers.tryReceive().isSuccess) { /* drop stale chunk */ }

        try {
            p.write(
                characteristicOf(Uuid.parse(ch.service), Uuid.parse(ch.write)),
                (command + "\r").encodeToByteArray(),
                writeType,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw LinkLostException("Write failed: ${e.message}")
        }

        val collector = PromptCollector()
        while (true) {
            val chunk = answers.receiveCatching().getOrNull() ?: throw LinkLostException()
            collector.feed(chunk)?.let { return it }
        }
    }

    /** Reuses the Peripheral for the same dongle (Kable can reconnect it), otherwise builds one from the scan. */
    private fun peripheralFor(dongle: Dongle): Peripheral {
        peripheral?.let { if (peripheralId == dongle.id) return it }
        peripheral?.close()
        val ad = advertisements[dongle.id] ?: error("Dongle ${dongle.id} was not seen in this scan")
        return Peripheral(ad).also {
            peripheral = it
            peripheralId = dongle.id
        }
    }
}
