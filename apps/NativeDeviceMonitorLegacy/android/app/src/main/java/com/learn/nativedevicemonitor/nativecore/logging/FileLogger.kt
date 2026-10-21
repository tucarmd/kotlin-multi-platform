package com.learn.nativedevicemonitor.nativecore.logging

import android.content.Context
import com.learn.nativedevicemonitor.nativecore.device.DeviceStatus
import java.io.File
import java.io.IOException

/** Log read result: valid records + number of broken lines skipped (so the UI reports it instead of silently swallowing). */
data class LogReadResult(
    val records: List<DeviceStatus>,
    val corruptLines: Int,
    /** IO error (disk full, file locked…). `null` = read fine. */
    val error: String? = null,
)

/**
 * Week 3 · Day 12–13 — write / read / clear a JSONL file (one JSON per line, see https://jsonlines.org).
 *
 * ```
 *  append(status)  ──► file.appendText(json + "\n")        (append at the end, never re-reads the whole file)
 *  readLogs()      ──► no file yet → [] · each line → DeviceStatus.fromJson (broken → corruptLines++)
 *  clearLogs()     ──► file.writeText("")
 * ```
 *
 * Why JSONL and not one JSON array: appending only needs to write one more line. A JSON array means every write
 * reads everything → fixes the `]` → writes it back; app killed midway = the WHOLE file is broken. A broken JSONL breaks only 1 line.
 *
 * ## Thread-safe
 * Three places write the same file: the monitor loop (Dispatchers.Default), the WorkManager worker (its own thread),
 * the "Append now" button in the UI. `@Synchronized` on a single instance → two lines never interleave.
 * Hence exactly **1 instance for the whole app**: get it via [FileLogger.shared].
 *
 * Blocking functions (IO) — from a coroutine wrap them in `withContext(Dispatchers.IO)`; don't call on main.
 *
 * @param file takes a `File` not a `Context` → JVM unit tests pass a temp file, no device needed.
 */
class FileLogger(private val file: File) {

    /** `true` = written; `false` = IO error (logged) — the monitor carries on next tick. */
    @Synchronized
    fun append(status: DeviceStatus): Boolean = try {
        file.parentFile?.mkdirs()
        file.appendText(status.toJson() + "\n")
        true
    } catch (e: IOException) {
        lastError = e.message ?: e.javaClass.simpleName
        false
    }

    @Synchronized
    fun readLogs(): LogReadResult {
        if (!file.exists()) return LogReadResult(emptyList(), 0)   // never written ≠ error
        val lines = try {
            file.readLines()
        } catch (e: IOException) {
            return LogReadResult(emptyList(), 0, error = e.message ?: e.javaClass.simpleName)
        }
        var corrupt = 0
        val records = lines.filter { it.isNotBlank() }.mapNotNull { line ->
            // org.json throws JSONException (RuntimeException) — catch Exception so one garbage line doesn't break the screen.
            try {
                DeviceStatus.fromJson(line)
            } catch (_: Exception) {
                corrupt++
                null
            }
        }
        return LogReadResult(records, corrupt)
    }

    /** `true` = cleared. A missing file also counts as cleared. */
    @Synchronized
    fun clearLogs(): Boolean = try {
        if (file.exists()) file.writeText("")
        true
    } catch (e: IOException) {
        lastError = e.message ?: e.javaClass.simpleName
        false
    }

    /** Path to print in the UI → the learner copies it into an `adb shell run-as … cat` command. */
    val path: String get() = file.absolutePath

    @Volatile
    var lastError: String? = null
        private set

    companion object {
        const val FILE_NAME = "monitor.jsonl"

        @Volatile
        private var instance: FileLogger? = null

        /**
         * `filesDir` = the app's private storage (`/data/data/<pkg>/files/`): no permission needed, removed on uninstall,
         * other apps can't read it. View it: `adb shell run-as com.learn.nativedevicemonitor cat files/monitor.jsonl`.
         */
        fun shared(context: Context): FileLogger =
            instance ?: synchronized(this) {
                instance ?: FileLogger(File(context.applicationContext.filesDir, FILE_NAME)).also { instance = it }
            }
    }
}
