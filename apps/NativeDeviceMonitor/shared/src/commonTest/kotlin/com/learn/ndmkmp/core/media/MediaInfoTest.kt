package com.learn.ndmkmp.core.media

import kotlin.test.assertEquals
import kotlin.test.Test

/** Day 10 — pure Media rules (no camera needed). Same cases as `ios/NativeDeviceMonitorTests/MediaInfoTests.swift`. */
class MediaInfoTest {

    @Test
    fun formatBytes_usesBinaryUnits() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("1023 B", formatBytes(1023))
        assertEquals("1.5 KB", formatBytes(1536))
        assertEquals("2.0 MB", formatBytes(2L * 1024 * 1024))
    }

    @Test
    fun sampleSize_keepsLongSideAtLeastMaxSide() {
        assertEquals(1, sampleSizeFor(800, 600, 1024))     // already smaller → no downscale
        assertEquals(1, sampleSizeFor(2047, 1000, 1024))   // /2 = 1023 < 1024 → keep 1
        assertEquals(2, sampleSizeFor(2048, 1536, 1024))
        assertEquals(2, sampleSizeFor(4032, 3024, 1024))   // 12 MP photo → 2016 px (doesn't go below 1024)
    }
}
