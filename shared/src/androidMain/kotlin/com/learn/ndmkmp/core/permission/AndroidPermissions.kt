package com.learn.ndmkmp.core.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Day 1 — reads runtime permission state, knows NOTHING about Compose. Ported from the old app's `PermissionService`;
 * [AndroidPermissionController] (Permissions tab) and `AndroidMediaPicker` (Media tab) both use it, so nobody has
 * to rewrite the "permanently denied" guesswork.
 *
 * ## Why this needs SharedPreferences
 *
 * Android has no API that answers "have I ever requested this permission". There are only 2 pieces of information:
 *
 * ```
 * checkSelfPermission(p)                    -> GRANTED | DENIED   (granted yet?)
 * shouldShowRequestPermissionRationale(p)   -> true | false        (should we explain?)
 * ```
 *
 * The real table of those 2 pieces:
 *
 * ```
 *               | checkSelf | rationale |
 * fresh install |  DENIED   |  false    |  <- never asked
 * denied x1     |  DENIED   |  TRUE     |  <- can still ask again
 * denied x2     |  DENIED   |  false    |  <- the system goes silent, only Settings left
 * ```
 *
 * Row 1 and row 3 are EXACTLY the same. Without recording "I've asked already" a freshly installed app gets
 * read as permanently denied → shows "Open Settings" on the very first launch, before asking anything.
 * This is Android permission trap #1, and the reason this file exists instead of calling
 * `checkSelfPermission` scattered across screens.
 */
object AndroidPermissions {

    private const val TAG = "PermissionService"

    /** A prefs file just for this — kept separate, not mixed with the app's other data. */
    private const val PREFS_NAME = "permission_asked"

    /** Granted yet — the only question Android answers directly. Needs only a Context. */
    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Full state of one permission. Needs an [Activity], not a Context:
     * `shouldShowRequestPermissionRationale` is an Activity API (it's tied to the visible window).
     */
    fun statusOf(activity: Activity, permission: String): PermissionStatus = when {
        isGranted(activity, permission) -> PermissionStatus.GRANTED
        // Never asked → leave NOT_REQUESTED, do NOT guess permanently denied (see the table in the doc above)
        !wasAsked(activity, permission) -> PermissionStatus.NOT_REQUESTED
        activity.shouldShowRequestPermissionRationale(permission) -> PermissionStatus.DENIED
        else -> PermissionStatus.PERMANENTLY_DENIED
    }

    /**
     * State of the whole group. The WORST state becomes the group state:
     * BLE scan needs both BLUETOOTH_SCAN and BLUETOOTH_CONNECT — grant only one and scanning still throws
     * SecurityException, so treating it as not granted matches reality.
     * An empty group (ble_connect on API ≤ 30) = nothing to ask = GRANTED.
     */
    fun statusOfGroup(activity: Activity, group: PermissionGroup): PermissionGroupState {
        val perPermission = group.runtimePermissions.associateWith { statusOf(activity, it) }
        val worst = perPermission.values.maxByOrNull { severity(it) } ?: PermissionStatus.GRANTED
        return PermissionGroupState(status = worst, perPermission = perPermission)
    }

    /** GRANTED is the best (0), PERMANENTLY_DENIED the worst (3). */
    private fun severity(status: PermissionStatus): Int = when (status) {
        PermissionStatus.GRANTED -> 0
        PermissionStatus.NOT_REQUESTED -> 1
        PermissionStatus.DENIED -> 2
        PermissionStatus.PERMANENTLY_DENIED -> 3
    }

    /**
     * Call RIGHT BEFORE firing `requestPermissions` — marks "this permission has been asked".
     *
     * Why write before asking instead of in the callback: if the user rotates / the app is killed while
     * the dialog is showing the callback may never arrive, but the system HAS already counted that request.
     * Writing first matches what the system counts.
     *
     * `commit()` not `apply()`: `apply()` writes asynchronously, and the very next line opens a system
     * dialog — the app may be killed there before the flag reaches disk.
     */
    fun markAsked(context: Context, permissions: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            permissions.forEach { putBoolean(it, true) }
        }.commit()
        Log.d(TAG, "markAsked $permissions")
    }

    private fun wasAsked(context: Context, permission: String): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(permission, false)

    /**
     * Clear the "asked" flags. Use after `adb shell pm reset-permissions com.learn.ndmkmp` —
     * that command resets the system side but does NOT clear the app's prefs; without clearing the app still thinks it asked.
     */
    fun resetAskedFlags(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        Log.d(TAG, "resetAskedFlags")
    }

    /**
     * Week 3 · Day 14 — whether POST_NOTIFICATIONS must be requested before starting the Foreground Service.
     * Only a runtime permission on API 33+; below 33 notifications always show → `false`.
     */
    fun needsNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !isGranted(context, Manifest.permission.POST_NOTIFICATIONS)

    /**
     * Open this app's own App Info page — the only way out of PERMANENTLY_DENIED.
     *
     * Implicit = only says "I want to open the app details page", the system picks the right Settings app.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            // Called from a non-Activity Context (e.g. a service) without this flag → crash
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.d(TAG, "openAppSettings")
    }
}

/**
 * Permission matrix by API level — `minSdk` 26 so **both branches run for real**:
 *
 * ```
 *   BLE scan     | API 26–30: ACCESS_FINE_LOCATION (runtime) + BLUETOOTH/ADMIN (install-time)
 *                | API 31+  : BLUETOOTH_SCAN (runtime)
 *   BLE connect  | API 26–30: nothing to request (install-time BLUETOOTH is enough)
 *                | API 31+  : BLUETOOTH_CONNECT (runtime)
 *   Camera       | all      : CAMERA (runtime)
 * ```
 *
 * `fun` not `val`: the list depends on `Build.VERSION.SDK_INT` → passing sdkInt in makes it testable/previewable.
 *
 * ⚠️ API ≤ 30 devices also need the **Location service turned on** (not just the permission) to find devices.
 */
fun permissionGroups(sdkInt: Int = Build.VERSION.SDK_INT): List<PermissionGroup> = listOf(
    PermissionGroup(
        id = PermissionGroupId.BLE_SCAN,
        runtimePermissions = if (sdkInt >= Build.VERSION_CODES.S) {
            // The Manifest also declares usesPermissionFlags="neverForLocation" so location is no longer required
            listOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            // API ≤ 30: without this permission the scan returns 0 devices with NO error at all
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        },
        installTimeNote = if (sdkInt >= Build.VERSION_CODES.S) "" else "BLUETOOTH, BLUETOOTH_ADMIN",
    ),
    PermissionGroup(
        id = PermissionGroupId.BLE_CONNECT,
        runtimePermissions = if (sdkInt >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyList()
        },
        installTimeNote = if (sdkInt >= Build.VERSION_CODES.S) "" else "BLUETOOTH",
    ),
    PermissionGroup(
        id = PermissionGroupId.CAMERA,
        // CAMERA is a runtime permission since API 23, minSdk 26 → no branching
        runtimePermissions = listOf(Manifest.permission.CAMERA),
    ),
)
