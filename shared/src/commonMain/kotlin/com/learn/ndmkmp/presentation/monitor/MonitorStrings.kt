package com.learn.ndmkmp.presentation.monitor

/** Monitor screen strings (Week 3). Rule 5.3: UI text in one place, English. Mirrors `UI/Monitor/MonitorStrings.swift`. */
object MonitorStrings {
    const val HINT =
        "Week 3 — device providers, JSONL file logger and background monitor. " +
            "Start a mechanism, send the app to the background / kill it, come back and read the log."

    // Device status
    const val STATUS_TITLE = "Device status (now)"
    const val REFRESH = "Refresh status"
    const val APPEND_NOW = "Append to log"
    const val NOT_READ_YET = "Tap Refresh status to read battery, network and Bluetooth."
    const val NULL_NOTE = "null = could not be measured (e.g. Bluetooth permission not granted)."

    // Monitor loop
    const val MONITOR_TITLE = "Background monitor"
    const val START_LOOP = "Start in-app loop (10 s)"
    const val START_FGS = "Start foreground service (10 s)"
    const val STOP = "Stop"
    const val RUNNING = "RUNNING"
    const val STOPPED = "STOPPED"
    const val LOOP_HINT = "In-app loop: stops when Android freezes/kills the app in the background."
    const val FGS_HINT = "Foreground service: keeps running with a notification. Force stop ends it — by design."
    fun modeLabel(mode: String?) = when (mode) {
        "IN_APP_LOOP" -> "in-app loop"
        "FOREGROUND_SERVICE" -> "foreground service"
        else -> "—"
    }
    fun runningLine(mode: String, intervalSec: Long, count: Int) =
        "$mode · every $intervalSec s · $count records this run"

    // WorkManager
    const val WORK_TITLE = "WorkManager (every 15 min)"
    const val SCHEDULE = "Schedule periodic"
    const val CANCEL = "Cancel periodic"
    const val WORK_HINT =
        "Minimum 15 min, the system picks the exact time (Doze can delay it). Survives app kill and reboot; " +
            "force stop cancels it until the app is opened again."
    fun workState(state: String?) = "State: ${state ?: "not scheduled"}"

    // Logs
    const val LOGS_TITLE = "Logs (monitor.jsonl)"
    const val RELOAD = "Reload logs"
    const val CLEAR = "Clear logs"
    const val EMPTY = "No records yet."
    fun summary(count: Int, corrupt: Int) =
        "$count records" + if (corrupt > 0) " · $corrupt corrupt line(s) skipped" else ""
    fun showingLast(n: Int) = "Newest first · showing last $n"
    const val CLEARED = "Log cleared."
    const val CLEAR_FAILED = "Could not clear the log file."
    const val APPEND_FAILED = "Could not write to the log file."
}
