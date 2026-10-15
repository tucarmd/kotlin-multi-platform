package dev.obdlink.ble

import dev.obdlink.ble.ElmProfiles.Characteristic
import dev.obdlink.ble.ElmProfiles.Service
import dev.obdlink.ble.ElmProfiles.full
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** [App 5 · B7] Choosing the ELM327 serial characteristics from a GATT table, no phone needed. */
class ElmProfilesTest {

    private val genericAccess = Service(full(0x1800), listOf(Characteristic(full(0x2A00), canWrite = false, canNotify = false)))

    @Test
    fun `FFF0 dongle writes to FFF2 and listens on FFF1`() {
        val fff0 = Service(
            full(0xFFF0),
            listOf(
                Characteristic(full(0xFFF1), canWrite = false, canNotify = true),
                Characteristic(full(0xFFF2), canWrite = true, canNotify = false),
            ),
        )
        assertEquals(ElmProfiles.Channel(full(0xFFF0), write = full(0xFFF2), notify = full(0xFFF1)), ElmProfiles.pick(listOf(genericAccess, fff0)))
    }

    @Test
    fun `FFE0 dongle uses FFE1 for both directions`() {
        val ffe0 = Service(full(0xFFE0), listOf(Characteristic(full(0xFFE1), canWrite = true, canNotify = true)))
        assertEquals(ElmProfiles.Channel(full(0xFFE0), write = full(0xFFE1), notify = full(0xFFE1)), ElmProfiles.pick(listOf(ffe0)))
    }

    @Test
    fun `unknown vendor service is used when it has write and notify`() {
        val custom = "e7810a71-73ae-499d-8c15-faa9aef0c3f2"
        val service = Service(custom, listOf(Characteristic("bef8d6c9-9c21-4c9e-b632-bd58c1009f9f", canWrite = true, canNotify = true)))
        assertEquals(custom, ElmProfiles.pick(listOf(genericAccess, service))?.service)
    }

    @Test
    fun `device without a serial stream is rejected`() {
        assertNull(ElmProfiles.pick(listOf(genericAccess)))
    }

    @Test
    fun `short uuid expands on the Bluetooth base uuid`() {
        assertEquals("0000fff0-0000-1000-8000-00805f9b34fb", full(0xFFF0))
    }
}
