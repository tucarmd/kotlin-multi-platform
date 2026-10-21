package com.learn.ndmkmp.core.logging

import com.learn.ndmkmp.core.background.LogReadResult
import com.learn.ndmkmp.core.device.DeviceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readLine
import kotlinx.io.writeString

/**
 * Week 3 · Day 12–13 — write / read / clear a JSONL file (one JSON per line, see https://jsonlines.org).
 *
 * ```
 *  append(status)  ──► sink(append = true) + json + "\n"     (never re-reads the whole file)
 *  readLogs()      ──► no file yet → [] · each line → DeviceStatus.fromJson (broken → corruptLines++)
 *  clearLogs()     ──► sink(append = false) = truncate
 * ```
 *
 * Why JSONL and not one JSON array: appending only writes one more line; app killed midway breaks 1 line, not the file.
 *
 * *KMP:* the old apps had `java.io.File` + `@Synchronized` (Android) and `FileManager` + a serial queue (iOS).
 * Here: kotlinx-io `SystemFileSystem` (same API on both) + a coroutine [Mutex] — the loop, the Android worker /
 * iOS background task and the "Append now" button never interleave two lines. One instance per file → one Mutex.
 *
 * @param path takes a `Path` → tests pass a temp file. The app passes `filesDir` (Android) / Documents (iOS).
 */
class FileLogger(val path: Path) {

    private val lock = Mutex()

    /** Last I/O error text — the Monitor card shows it next to the loop status. */
    var lastError: String? = null
        private set

    /** `true` = written; `false` = I/O error (kept in [lastError]) — the loop carries on next tick. */
    suspend fun append(status: DeviceStatus): Boolean = io {
        try {
            path.parent?.let { SystemFileSystem.createDirectories(it) }
            SystemFileSystem.sink(path, append = true).buffered().use { it.writeString(status.toJson() + "\n") }
            true
        } catch (e: IOException) {
            lastError = e.message ?: "IOException"
            false
        }
    }

    suspend fun readLogs(): LogReadResult = io {
        if (!SystemFileSystem.exists(path)) return@io LogReadResult(emptyList(), 0)   // never written ≠ error
        val lines = try {
            SystemFileSystem.source(path).buffered().use { src ->
                generateSequence { src.readLine() }.toList()
            }
        } catch (e: IOException) {
            return@io LogReadResult(emptyList(), 0, error = e.message ?: "IOException")
        }
        var corrupt = 0
        val records = lines.filter { it.isNotBlank() }.mapNotNull { line ->
            try {
                DeviceStatus.fromJson(line)
            } catch (_: IllegalArgumentException) {   // SerializationException: one garbage line doesn't break the screen
                corrupt++
                null
            }
        }
        LogReadResult(records, corrupt)
    }

    /** `true` = cleared. A missing file also counts as cleared. */
    suspend fun clearLogs(): Boolean = io {
        try {
            if (SystemFileSystem.exists(path)) SystemFileSystem.sink(path, append = false).close()
            true
        } catch (e: IOException) {
            lastError = e.message ?: "IOException"
            false
        }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { lock.withLock { block() } }

    companion object {
        const val FILE_NAME = "monitor.jsonl"
    }
}
