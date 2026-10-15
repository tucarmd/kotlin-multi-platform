package dev.triplog.android.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dev.triplog.android.R
import dev.triplog.core.TripRecorder
import dev.triplog.model.LiveTrip
import dev.triplog.model.RecordState
import dev.triplog.rules.formatDistance
import dev.triplog.rules.formatDuration
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * [App 4 · B5] TrackingService: the foreground service that keeps the process alive while a trip records.
 *
 * ```
 * RecordScreen "Start" ─► TrackingService.start(context) ─► ContextCompat.startForegroundService
 *   └─► onStartCommand: startForeground(notification, LOCATION)   ← within 5 s or Android 12+ crashes
 *                       recorder.start()                          ← the shared TripRecorder does the work
 *                       lifecycleScope: recorder.state → notification text; Idle again → stopSelf()
 * Notification "Stop" / RecordScreen "Stop" ─► recorder.stop() ─► Saving ─► Idle ─► service stops itself
 * ```
 *
 * Why a service at all: without one, Android may kill the app a few minutes after the screen turns off and stops
 * delivering background location at all on Android 8+ (a few fixes per hour). A foreground service with type
 * `location` is the official way to keep receiving fixes, at the price of a notification the user always sees.
 *
 * Why the recorder is NOT created here: it is the Koin single shared with RecordViewModel, so rotating or closing
 * the screen never loses the trip, and the service only drives it.
 *
 * Called by: RecordScreen (start), its own notification (stop). Calls: TripRecorder.
 */
class TrackingService : LifecycleService() {

    private val recorder: TripRecorder by inject()
    private var watching = false

    /**
     * Start = go foreground and start the recorder. Stop = ask the recorder to stop; the state watcher
     * then removes the notification and stops the service once the trip is saved.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            recorder.stop()
            if (!watching) stopSelf() // stop pressed on a stale notification after a process restart
            return START_NOT_STICKY
        }

        createChannel()
        // PLATFORM: the type argument is required on Android 14+ and must match android:foregroundServiceType in
        // the manifest, or startForeground throws. ServiceCompat drops it on Android 9 and older (no types there).
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(null, paused = false),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        recorder.start()
        if (!watching) {
            watching = true
            lifecycleScope.launch { followRecorder() }
        }
        // Not STICKY: after the system kills the process the recorder starts again as Idle, so there is nothing
        // to resume. Recording across a process kill is out of scope (see the plan's "later" column).
        return START_NOT_STICKY
    }

    /** Mirrors the recorder state into the notification, and stops the service when the trip is saved. */
    private suspend fun followRecorder() {
        // TRAP: recorder.start() is processed asynchronously, so the first value seen here is often still Idle.
        // Only an Idle that comes AFTER Recording/Paused means "the trip is over".
        var seenActive = false
        recorder.state.collect { state ->
            when (state) {
                is RecordState.Recording -> { seenActive = true; notify(state.trip, paused = false) }
                is RecordState.Paused -> { seenActive = true; notify(state.trip, paused = true) }
                RecordState.Saving -> Unit
                RecordState.Idle -> if (seenActive) {
                    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        // The user stopped the app from Task Manager, or the system is shutting the service down: save what we have.
        if (recorder.state.value !is RecordState.Idle) recorder.stop()
        super.onDestroy()
    }

    private fun notify(trip: LiveTrip, paused: Boolean) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(trip, paused))
    }

    private fun createChannel() {
        // LOW importance: no sound, no pop-up every second while the text updates.
        // The Compat class skips the call below Android 8, where channels do not exist (minSdk is 24).
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(getString(R.string.notification_channel))
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel) // safe to call again
    }

    private fun buildNotification(trip: LiveTrip?, paused: Boolean): Notification {
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val stop = PendingIntent.getService(
            this, 1, Intent(this, TrackingService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val title = getString(if (paused) R.string.paused else R.string.notification_recording)
        val text = trip?.let {
            getString(R.string.notification_text, formatDuration(it.elapsedMillis), formatDistance(it.distanceMeters))
        } ?: getString(R.string.waiting_gps)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.stop), stop)
            // Android 12+ may delay a foreground notification by up to 10 s; show it right away.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val ACTION_START = "dev.triplog.action.START"
        private const val ACTION_STOP = "dev.triplog.action.STOP"
        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 1

        /**
         * Starts recording through the service. Call only when location permission is granted:
         * PLATFORM: Android 14+ throws SecurityException from startForeground(type = location) without it.
         */
        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
