package dev.obdlink

import dev.obdlink.core.CommandQueue
import dev.obdlink.obd.model.ObdCommand
import dev.obdlink.obd.model.ObdPid
import dev.obdlink.obd.model.ObdResult
import dev.obdlink.obd.rules.decodePidAnswer
import dev.obdlink.obd.transport.CommandTimeoutException
import dev.obdlink.obd.transport.LinkLostException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** [App 5 · B7] One command at a time, timeout, one retry, long first timeout after ATSP0. Virtual time. */
class CommandQueueTest {

    private suspend fun TestScope.connected(fake: FakeElmTransport = FakeElmTransport()): FakeElmTransport {
        fake.connect(fake.dongle)
        return fake
    }

    @Test
    fun `answer split into 3 chunks comes back whole`() = runTest {
        val fake = connected()
        fake.chunkSize = 5 // "41 0C 1A F8\r\r>" = 14 chars → "41 0C", " 1A F", "8\r\r>"
        val exchange = CommandQueue(fake).send(ObdCommand.Pid(ObdPid.EngineRpm))
        val data = assertIs<ObdResult.Data>(exchange.result)
        assertEquals(1726, decodePidAnswer(ObdPid.EngineRpm, data.messages)?.value)
    }

    @Test
    fun `NO DATA is returned as a result and not thrown`() = runTest {
        val fake = connected()
        fake.answers["010D"] = "NO DATA\r\r>"
        assertEquals(ObdResult.NoData, CommandQueue(fake).send(ObdCommand.Pid(ObdPid.VehicleSpeed)).result)
    }

    @Test
    fun `two concurrent sends keep their order and never overlap`() = runTest {
        val fake = connected()
        fake.delays["010C"] = 300.milliseconds // the first one is slow
        val queue = CommandQueue(fake)
        val rpm = async { queue.send(ObdCommand.Pid(ObdPid.EngineRpm)) }
        val speed = async { queue.send(ObdCommand.Pid(ObdPid.VehicleSpeed)) }

        assertEquals("010C", rpm.await().command)
        assertEquals("41 0C 1A F8\r\r", rpm.await().raw)
        assertEquals("41 0D 32\r\r", speed.await().raw)
        assertEquals(listOf("010C", "010D"), fake.sent)
        assertEquals(1, fake.maxInFlight)
    }

    @Test
    fun `timeout then retry then error`() = runTest {
        val fake = connected()
        fake.silent += "010C"
        val queue = CommandQueue(fake, timeout = 2.seconds, retries = 1)
        val start = testScheduler.currentTime

        assertFailsWith<CommandTimeoutException> { queue.send(ObdCommand.Pid(ObdPid.EngineRpm)) }
        assertEquals(listOf("010C", "010C"), fake.sent) // first try + one retry
        assertEquals(4_000, testScheduler.currentTime - start)
    }

    @Test
    fun `retry succeeds when the second try answers`() = runTest {
        val fake = connected()
        fake.silentTimes["010D"] = 1
        val exchange = CommandQueue(fake).send(ObdCommand.Pid(ObdPid.VehicleSpeed))
        assertIs<ObdResult.Data>(exchange.result)
        assertEquals(2, fake.sent.size)
    }

    @Test
    fun `no command slips in between a timeout and its retry`() = runTest {
        val fake = connected()
        fake.silentTimes["010C"] = 1
        val queue = CommandQueue(fake)
        val rpm = async { queue.send(ObdCommand.Pid(ObdPid.EngineRpm)) }
        launch { queue.send(ObdCommand.Pid(ObdPid.CoolantTemp)) }
        rpm.await()
        advanceTimeBy(100.milliseconds)
        assertEquals(listOf("010C", "010C", "0105"), fake.sent)
    }

    @Test
    fun `first request after ATSP0 gets the long search timeout`() = runTest {
        val fake = connected()
        fake.delays["010C"] = 5.seconds // "SEARCHING..." takes 5 s
        val queue = CommandQueue(fake, timeout = 2.seconds, searchTimeout = 10.seconds)

        queue.send(ObdCommand.AutoProtocol)
        assertIs<ObdResult.Data>(queue.send(ObdCommand.Pid(ObdPid.EngineRpm)).result)
        // Protocol is known now: the same 5 s wait is a timeout again.
        assertFailsWith<CommandTimeoutException> { queue.send(ObdCommand.Pid(ObdPid.EngineRpm)) }
    }

    @Test
    fun `dropped link while waiting fails fast instead of retrying`() = runTest {
        val fake = connected()
        fake.silent += "03"
        val queue = CommandQueue(fake)
        val read = async { runCatching { queue.send(ObdCommand.ReadDtc) } }
        advanceTimeBy(500.milliseconds)
        fake.dropLink()
        assertIs<LinkLostException>(read.await().exceptionOrNull())
        assertEquals(1, fake.sent.size)
    }
}
