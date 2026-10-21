package com.learn.ndmkmp.core.background

import android.content.Context
import com.learn.ndmkmp.core.device.AndroidDeviceStatusSource
import com.learn.ndmkmp.core.logging.FileLogger
import kotlinx.io.files.Path
import java.io.File

/**
 * P7 — the ONE [LoopDeviceMonitor] of the process.
 *
 * ```
 *  MainActivity (Monitor tab) ─┐
 *  MonitorService (FGS)       ─┼─► AndroidMonitorRuntime.monitor(ctx) ─► same loop · same status · same monitor.jsonl
 *  MonitorWorker (WorkManager)─┘
 * ```
 *
 * Why a process-wide `object` (the old app's `object BackgroundMonitor`): the Activity is destroyed on rotation /
 * backgrounding, the service and the worker are started by the system without any Activity. If each built its own
 * monitor, Stop on the screen could not stop the service's loop, and two FileLoggers (= two Mutexes) could interleave
 * lines in the same file. Built lazily from `applicationContext` — never holds an Activity.
 */
object AndroidMonitorRuntime {

    @Volatile
    private var instance: LoopDeviceMonitor? = null

    fun monitor(context: Context): LoopDeviceMonitor =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(appContext: Context) = LoopDeviceMonitor(
        source = AndroidDeviceStatusSource(appContext),
        // filesDir = private to the app, kept until uninstall · `adb shell run-as com.learn.ndmkmp cat files/monitor.jsonl`
        logger = FileLogger(Path(File(appContext.filesDir, FileLogger.FILE_NAME).absolutePath)),
        background = AndroidPlatformBackground(appContext),
    )
}
