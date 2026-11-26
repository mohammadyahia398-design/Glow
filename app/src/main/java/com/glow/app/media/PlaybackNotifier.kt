package com.glow.app.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import com.glow.app.MainActivity
import com.glow.app.R

/** يبني إشعار التشغيل (MediaStyle) وقناته. */
class PlaybackNotifier(private val context: Context) {

    // عناصر لا تتغير مع الحالة: تُبنى مرة واحدة بدل كل تحديث للإشعار.
    private val previousAction by lazy {
        action(R.drawable.ic_notif_previous, "السابق", GlowMediaSessionService.ACTION_PREVIOUS, 1)
    }
    private val nextAction by lazy {
        action(R.drawable.ic_notif_next, "التالي", GlowMediaSessionService.ACTION_NEXT, 3)
    }
    private val openAppIntent by lazy { createOpenAppIntent() }

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "التشغيل",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "عناصر التحكم بالموسيقى أثناء التشغيل"
                setShowBadge(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun build(state: PlayerState, token: MediaSession.Token, art: Bitmap? = null): Notification {
        val track = state.current
        val toggleIcon = if (state.isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
        val toggleLabel = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل"

        // نفس منطق العرض في المكتبة: العنوان المقروء، والفنان أو تاريخ الإضافة عند غيابه.
        val title = track?.displayTitle ?: "Glow"
        val text = track?.let { t ->
            t.artist.takeIf { it != UNKNOWN_ARTIST } ?: t.addedLabel ?: t.artist
        }.orEmpty()

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_glow)
            .setColor(context.getColor(R.color.glow_accent))
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setContentTitle(title)
            .setContentText(text)
            .setLargeIcon(art)
            .setContentIntent(openAppIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(state.isPlaying)
            .addAction(previousAction)
            .addAction(action(toggleIcon, toggleLabel, GlowMediaSessionService.ACTION_TOGGLE, 2))
            .addAction(nextAction)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(token)
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .build()
    }

    private fun action(iconRes: Int, label: String, action: String, requestCode: Int): Notification.Action {
        val intent = Intent(context, GlowMediaSessionService::class.java).setAction(action)
        val pending = PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Action.Builder(Icon.createWithResource(context, iconRes), label, pending).build()
    }

    private fun createOpenAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val CHANNEL_ID = "glow_playback"

        // مطابقة لقيمة MediaStoreRepository الخاصة (private) ولما تستخدمه LibraryScreen.
        private const val UNKNOWN_ARTIST = "فنان غير معروف"
    }
}
