package dev.triplog.android.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [App 4 · B5] Shares a GPX text through the Android share sheet (Drive, Gmail, Strava…).
 *
 * Why FileProvider: other apps cannot read our private files, and a file:// uri crashes on Android 7+.
 * FileProvider hands out a content:// uri with a one-off read grant instead. iOS does the same with ShareLink.
 *
 * Called by: TripDetailScreen. Calls: GpxWriter output from TripLibrary.gpx().
 */
suspend fun shareGpx(context: Context, fileName: String, gpx: String, chooserTitle: String) {
    val file = withContext(Dispatchers.IO) {
        File(context.cacheDir, "gpx").apply { mkdirs() }.resolve(fileName).apply { writeText(gpx) }
    }
    // Authority must match android:authorities in the manifest; the folder must be listed in res/xml/file_paths.xml.
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("application/gpx+xml")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // TRAP: the read grant travels with ClipData. Without it some targets opened through the chooser get
    // "permission denied" on the uri.
    send.clipData = ClipData.newRawUri(fileName, uri)
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
