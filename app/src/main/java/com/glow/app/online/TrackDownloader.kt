package com.glow.app.online

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.glow.app.media.Track

/** تنزيل أغنية مسموح بتحميلها إلى Music/Glow عبر مدير التنزيل في النظام. */
object TrackDownloader {
    fun start(context: Context, track: Track): Boolean {
        val url = track.downloadUrl ?: return false
        return runCatching {
            val name = ("${track.artist} - ${track.title}")
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .take(100)
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(track.title)
                .setMimeType("audio/mpeg")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "Glow/$name.mp3")
            val manager = context.applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            true
        }.getOrDefault(false)
    }
}
