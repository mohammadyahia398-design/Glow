package com.glow.app.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * يشغّل الصوت بـ MediaPlayer ويدير الطابور وتركيز الصوت.
 * يجب استدعاء كل دوال هذا الصنف من الخيط الرئيسي.
 */
class PlayerController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val queue = PlayQueue()
    private var tracks: List<Track> = emptyList()

    private var player: MediaPlayer? = null
    private var prepared = false

    /** نية المستخدم: هل يجب أن يبدأ التشغيل فور جاهزية المقطع؟ (تشمل مرحلة التحضير). */
    private var playWhenReady = false
    private var consecutiveErrors = 0
    private var resumeOnFocusGain = false
    private var ticker: Job? = null
    private var focusRequest: AudioFocusRequest? = null

    private var speed = 1f
    private var volume = 1f
    private var sleepJob: Job? = null
    private val _sleepEndsAt = MutableStateFlow<Long?>(null)

    /** وقت انتهاء مؤقّت النوم (بالمللي ثانية)، أو null إن لم يكن مفعّلاً. */
    val sleepEndsAt: StateFlow<Long?> = _sleepEndsAt.asStateFlow()

    /** معرّف جلسة الصوت الحالية (0 إن لم يكن هناك مشغّل)، يحتاجه المعادل. */
    val audioSessionId: Int get() = player?.audioSessionId ?: 0

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // يُعاد مستوى الصوت كي لا يبقى مخفَّضاً (ducking) عند الاستئناف اليدوي لاحقاً.
                setVolumeLevel(1f)
                resumeOnFocusGain = false
                pauseInternal()
                abandonFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (playWhenReady) {
                    resumeOnFocusGain = true
                    pauseInternal()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> setVolumeLevel(DUCK_VOLUME)
            AudioManager.AUDIOFOCUS_GAIN -> {
                setVolumeLevel(1f)
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    resume()
                }
            }
        }
    }

    // ── التحكم العام ────────────────────────────────────────────

    fun playTracks(list: List<Track>, startIndex: Int) {
        if (list.isEmpty()) return
        tracks = list
        queue.load(list.size, startIndex, queue.shuffle)
        consecutiveErrors = 0
        playCurrent()
    }

    fun togglePlay() {
        if (playWhenReady) pause() else resume()
    }

    fun resume() {
        val mp = player
        if (mp == null) {
            if (tracks.isNotEmpty()) playCurrent()
            return
        }
        if (!prepared) {
            // المقطع ما زال قيد التحضير: نسجّل النية فقط ويبدأ عند الجاهزية.
            if (requestFocus()) playWhenReady = true
            return
        }
        if (!mp.isPlaying && requestFocus()) {
            playWhenReady = true
            mp.start()
            applySpeed()
            startTicker()
            publish()
        }
    }

    /** إيقاف مؤقت بطلب المستخدم: يلغي أي استئناف تلقائي مرتبط بتركيز الصوت. */
    fun pause() {
        resumeOnFocusGain = false
        pauseInternal()
    }

    fun next() {
        if (queue.next(auto = false) != null) playCurrent()
    }

    fun previous() {
        val mp = player
        if (mp != null && prepared && mp.currentPosition > RESTART_THRESHOLD_MS) {
            seekTo(0L)
        } else if (queue.previous() != null) {
            playCurrent()
        }
    }

    fun seekTo(positionMs: Long) {
        val mp = player ?: return
        if (!prepared) return
        val duration = mp.duration
        val target = if (duration > 0) positionMs.coerceIn(0L, duration.toLong()) else positionMs.coerceAtLeast(0L)
        mp.seekTo(target.toInt())
        publish()
    }

    fun setSpeed(value: Float) {
        speed = value
        applySpeed()
    }

    /** يطبّق السرعة فقط أثناء التشغيل، لأن ضبطها وهو متوقف قد يبدأ التشغيل. */
    private fun applySpeed() {
        val mp = player ?: return
        if (!prepared || !mp.isPlaying) return
        try {
            mp.playbackParams = PlaybackParams().setSpeed(speed)
        } catch (_: Exception) {
            // سرعة غير مدعومة على هذا الجهاز: نبقي السرعة العادية.
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepJob = null
        if (minutes <= 0) {
            _sleepEndsAt.value = null
            return
        }
        val delayMs = minutes * 60_000L
        _sleepEndsAt.value = System.currentTimeMillis() + delayMs
        sleepJob = scope.launch {
            delay(delayMs)
            pause()
            abandonFocus()
            _sleepEndsAt.value = null
            sleepJob = null
        }
    }

    fun setShuffle(enabled: Boolean) {
        queue.setShuffle(enabled)
        publish()
    }

    fun cycleRepeat() {
        queue.repeat = queue.repeat.cycle()
        publish()
    }

    fun release() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepEndsAt.value = null
        playWhenReady = false
        resumeOnFocusGain = false
        stopTicker()
        releasePlayer()
        abandonFocus()
    }

    // ── الداخل ─────────────────────────────────────────────────

    private fun pauseInternal() {
        playWhenReady = false
        val mp = player
        if (mp != null && prepared && mp.isPlaying) mp.pause()
        stopTicker()
        publish()
    }

    private fun setVolumeLevel(level: Float) {
        volume = level
        player?.setVolume(level, level)
    }

    /**
     * يشغّل المقطع الحالي، ويتخطى تلقائياً المقاطع التي يفشل تحضيرها فوراً.
     * حلقة بدل الاستدعاء الذاتي لتجنّب تعمّق المكدّس في قوائم كبيرة تالفة.
     */
    private fun playCurrent() {
        while (true) {
            val index = queue.currentIndex ?: return
            val track = tracks.getOrNull(index) ?: return

            releasePlayer()
            if (!requestFocus()) {
                playWhenReady = false
                publish()
                return
            }
            playWhenReady = true
            if (startPlayer(track)) return
            if (!advanceAfterFailure()) return
        }
    }

    /** @return true إن بدأ التحضير بنجاح، false إن فشل إنشاء المشغّل فوراً. */
    private fun startPlayer(track: Track): Boolean {
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(audioAttributes)
            mp.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            mp.setVolume(volume, volume)
            mp.setDataSource(context, track.uri)
            mp.setOnPreparedListener {
                prepared = true
                consecutiveErrors = 0
                if (!playWhenReady) {
                    // أوقفه المستخدم أو فقد التركيز أثناء التحضير.
                    publish()
                    return@setOnPreparedListener
                }
                try {
                    it.start()
                } catch (_: Exception) {
                    handleError()
                    return@setOnPreparedListener
                }
                applySpeed()
                startTicker()
                publish()
            }
            mp.setOnCompletionListener { onCompleted() }
            mp.setOnErrorListener { _, _, _ ->
                handleError()
                true
            }
            mp.prepareAsync()
        } catch (_: Exception) {
            mp.release()
            return false
        }
        player = mp
        prepared = false
        publish()
        return true
    }

    private fun onCompleted() {
        if (queue.next(auto = true) != null) playCurrent() else stopPlayback()
    }

    /** خطأ أثناء التحضير أو التشغيل: نتخطى المقطع التالف. */
    private fun handleError() {
        releasePlayer()
        if (advanceAfterFailure()) playCurrent()
    }

    /**
     * يسجّل فشل مقطع وينتقل للتالي.
     * @return false إن تعطلت القائمة كلها أو انتهت (وقد أُوقف التشغيل).
     */
    private fun advanceAfterFailure(): Boolean {
        consecutiveErrors++
        if (consecutiveErrors >= tracks.size || queue.next(auto = false) == null) {
            stopPlayback()
            return false
        }
        return true
    }

    private fun stopPlayback() {
        playWhenReady = false
        stopTicker()
        releasePlayer()
        abandonFocus()
        publish()
    }

    private fun releasePlayer() {
        player?.let {
            it.setOnPreparedListener(null)
            it.setOnCompletionListener(null)
            it.setOnErrorListener(null)
            it.release()
        }
        player = null
        prepared = false
    }

    private fun requestFocus(): Boolean {
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
            .also { focusRequest = it }
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                publish()
                delay(TICK_MS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun publish() {
        val track = queue.currentIndex?.let { tracks.getOrNull(it) }
        val mp = player
        val ready = prepared && mp != null
        _state.value = PlayerState(
            queue = tracks,
            current = track,
            isPlaying = ready && mp?.isPlaying == true,
            positionMs = if (ready) mp?.currentPosition?.toLong() ?: 0L else 0L,
            durationMs = if (ready) mp?.duration?.toLong() ?: 0L else track?.durationMs ?: 0L,
            shuffle = queue.shuffle,
            repeat = queue.repeat,
        )
    }

    private companion object {
        const val TICK_MS = 500L
        const val RESTART_THRESHOLD_MS = 3_000
        const val DUCK_VOLUME = 0.3f
    }
}
