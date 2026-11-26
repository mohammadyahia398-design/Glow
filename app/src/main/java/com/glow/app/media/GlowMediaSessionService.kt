package com.glow.app.media

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.glow.app.MainActivity
import com.glow.app.ui.common.loadTrackArtwork
import com.glow.app.widget.GlowWidget
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * خدمة التشغيل: تملك [PlayerController] وجلسة الوسائط والإشعار.
 * تُسجَّل في المانيفست بـ foregroundServiceType="mediaPlayback".
 */
class GlowMediaSessionService : Service() {

    inner class LocalBinder : Binder() {
        val controller: PlayerController get() = this@GlowMediaSessionService.controller
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var controller: PlayerController
    private lateinit var session: MediaSession
    private lateinit var notifier: PlaybackNotifier
    private lateinit var notificationManager: NotificationManager

    /** يوقف التشغيل مؤقتاً عند فصل سماعات الأذن أو انقطاع البلوتوث. */
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY &&
                controller.state.value.isPlaying &&
                getSharedPreferences(
                    com.glow.app.settings.SettingsStore.PREFS_NAME,
                    Context.MODE_PRIVATE,
                ).getBoolean(com.glow.app.settings.SettingsStore.PREF_PAUSE_ON_UNPLUG, true)
            ) {
                controller.pause()
            }
        }
    }

    private var isForeground = false
    private var lastKey: Pair<Long?, Boolean>? = null

    /** مهلة قصيرة قبل مغادرة المقدمة، كي لا نفقدها بين مقطع وآخر أثناء التحضير. */
    private var detachJob: Job? = null

    // آخر قيم أُرسلت إلى الجلسة، لتفادي تحديثها كل نصف ثانية بلا حاجة.
    private var metaTrackId: Long? = null
    private var metaDurationMs = -1L
    private var metaHasArt = false

    // غلاف المقطع الحالي (محلي أو إنترنتي) للإشعار وشاشة القفل.
    private var artTrackId: Long? = null
    private var artBitmap: Bitmap? = null
    private var artJob: Job? = null
    private var pbCode = -1
    private var pbTrackId: Long? = null
    private var pbPositionMs = 0L
    private var pbRealtimeMs = 0L

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        notifier = PlaybackNotifier(this)
        notifier.ensureChannel()

        controller = PlayerController(this, scope)
        ContextCompat.registerReceiver(
            this,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        session = MediaSession(this, SESSION_TAG).apply {
            setSessionActivity(openAppIntent())
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = this@GlowMediaSessionService.controller.resume()
                override fun onPause() = this@GlowMediaSessionService.controller.pause()
                override fun onSkipToNext() = this@GlowMediaSessionService.controller.next()
                override fun onSkipToPrevious() = this@GlowMediaSessionService.controller.previous()
                override fun onSeekTo(pos: Long) = this@GlowMediaSessionService.controller.seekTo(pos)
            })
            isActive = true
        }

        scope.launch {
            controller.state.collect { render(it) }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> controller.togglePlay()
            ACTION_NEXT -> controller.next()
            ACTION_PREVIOUS -> controller.previous()
        }
        // startForegroundService يفرض استدعاء startForeground خلال خمس ثوانٍ.
        val state = controller.state.value
        enterForeground(notifier.build(state, session.sessionToken, artFor(state.current)))
        lastKey = state.current?.id to state.isPlaying

        when {
            state.current == null -> {
                // لا شيء للتشغيل: لا نبقى في المقدمة بإشعار فارغ.
                leaveForeground(detach = false)
                notificationManager.cancel(NOTIFICATION_ID)
                stopSelf()
            }
            state.isPlaying -> cancelDetach()
            else -> scheduleDetach()
        }
        return START_NOT_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (controller.state.value.current == null) stopSelf()
        return false
    }

    /** عند إزالة التطبيق من المهام: لا نُبقي خدمة متوقفة تحتفظ بالمشغّل والإشعار. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!controller.state.value.isPlaying) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        unregisterReceiver(noisyReceiver)
        scope.cancel()
        session.isActive = false
        session.release()
        controller.release()
        notificationManager.cancel(NOTIFICATION_ID)
        GlowWidget.update(applicationContext, null, null)
        super.onDestroy()
    }

    // ── العرض ──────────────────────────────────────────────────

    private fun artFor(track: Track?): Bitmap? = if (track != null && track.id == artTrackId) artBitmap else null

    /** يحمّل غلاف المقطع الجديد في الخلفية ثم يحدّث الجلسة والإشعار. */
    private fun ensureArt(track: Track?) {
        if (track?.id == artTrackId) return
        artTrackId = track?.id
        artBitmap = null
        artJob?.cancel()
        if (track == null) return
        artJob = scope.launch {
            val bmp = withContext(Dispatchers.IO) { loadTrackArtwork(applicationContext, track) }
            if (bmp == null || artTrackId != track.id) return@launch
            artBitmap = bmp
            val latest = controller.state.value
            if (latest.current?.id != track.id) return@launch
            updateSession(latest)
            notificationManager.notify(NOTIFICATION_ID, notifier.build(latest, session.sessionToken, bmp))
            GlowWidget.update(applicationContext, latest, bmp)
        }
    }

    private fun render(state: PlayerState) {
        ensureArt(state.current)
        updateSession(state)

        val key = state.current?.id to state.isPlaying
        if (key == lastKey) return
        lastKey = key
        GlowWidget.update(applicationContext, state, artFor(state.current))

        val notification = notifier.build(state, session.sessionToken, artFor(state.current))
        when {
            state.isPlaying -> {
                cancelDetach()
                enterForeground(notification)
            }
            state.current != null -> {
                notificationManager.notify(NOTIFICATION_ID, notification)
                if (isForeground) scheduleDetach()
            }
            else -> {
                cancelDetach()
                leaveForeground(detach = false)
                notificationManager.cancel(NOTIFICATION_ID)
            }
        }
    }

    private fun updateSession(state: PlayerState) {
        val track = state.current

        val art = artFor(track)
        if (track?.id != metaTrackId || state.durationMs != metaDurationMs || (art != null) != metaHasArt) {
            metaTrackId = track?.id
            metaDurationMs = state.durationMs
            metaHasArt = art != null
            session.setMetadata(
                MediaMetadata.Builder()
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
                    .putString(MediaMetadata.METADATA_KEY_TITLE, track?.title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, track?.artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, track?.album)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs)
                    .build(),
            )
        }

        val code = when {
            track == null -> PlaybackState.STATE_NONE
            state.isPlaying -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_PAUSED
        }

        // النظام يستقرئ الموضع أثناء التشغيل، فلا نحدّث الجلسة إلا عند تغيّر الحالة
        // أو عند قفزة في الموضع (تقديم/تأخير). السرعة غير العادية تُصحَّح تلقائياً بالانحراف.
        val now = SystemClock.elapsedRealtime()
        val expected =
            if (pbCode == PlaybackState.STATE_PLAYING) pbPositionMs + (now - pbRealtimeMs) else pbPositionMs
        val drifted = abs(state.positionMs - expected) > POSITION_TOLERANCE_MS

        if (code != pbCode || track?.id != pbTrackId || drifted) {
            pbCode = code
            pbTrackId = track?.id
            pbPositionMs = state.positionMs
            pbRealtimeMs = now
            session.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(SESSION_ACTIONS)
                    .setState(code, state.positionMs, 1f)
                    .build(),
            )
        }
    }

    private fun enterForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForeground = true
        } catch (_: IllegalStateException) {
            // Android 12+ قد يمنع الدخول إلى المقدمة من الخلفية؛ نكتفي بتحديث الإشعار بدل الانهيار.
            isForeground = false
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun leaveForeground(detach: Boolean) {
        if (!isForeground) return
        stopForeground(if (detach) STOP_FOREGROUND_DETACH else STOP_FOREGROUND_REMOVE)
        isForeground = false
    }

    private fun scheduleDetach() {
        detachJob?.cancel()
        detachJob = scope.launch {
            delay(FOREGROUND_GRACE_MS)
            val latest = controller.state.value
            if (!latest.isPlaying) {
                if (latest.current != null) {
                    leaveForeground(detach = true)
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        notifier.build(latest, session.sessionToken, artFor(latest.current)),
                    )
                } else {
                    leaveForeground(detach = false)
                    notificationManager.cancel(NOTIFICATION_ID)
                }
            }
            detachJob = null
        }
    }

    private fun cancelDetach() {
        detachJob?.cancel()
        detachJob = null
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val ACTION_TOGGLE = "com.glow.app.action.TOGGLE"
        const val ACTION_NEXT = "com.glow.app.action.NEXT"
        const val ACTION_PREVIOUS = "com.glow.app.action.PREVIOUS"

        private const val NOTIFICATION_ID = 1001
        private const val SESSION_TAG = "GlowSession"
        private const val FOREGROUND_GRACE_MS = 3_000L
        private const val POSITION_TOLERANCE_MS = 1_500L

        private const val SESSION_ACTIONS =
            PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO
    }
}
