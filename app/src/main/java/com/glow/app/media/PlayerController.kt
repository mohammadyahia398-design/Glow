package com.glow.app.media

import android.content.Context
import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.PlaybackException
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * محرك تشغيل Glow المبني على AndroidX Media3 ExoPlayer.
 *
 * يحتفظ هذا الصنف بواجهة التحكم التي تستخدمها الواجهة والخدمة، بينما يتولى
 * ExoPlayer فك الترميز، التخزين المؤقت، التقديم، سرعة التشغيل، وجلسة الصوت.
 * إدارة Audio Focus يدوية هنا حتى نحافظ على سلوك ducking والاستئناف الموجود.
 * يجب استدعاء دوال التحكم من الخيط الرئيسي.
 */
@OptIn(UnstableApi::class)
class PlayerController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val queue = PlayQueue()
    private var tracks: List<Track> = emptyList()

    private var player: ExoPlayer? = null
    private var prepared = false
    private var currentAudioSessionId = 0

    /** نية المستخدم: هل يجب أن يبدأ التشغيل فور وصول المشغّل إلى STATE_READY؟ */
    private var playWhenReady = false
    private var consecutiveErrors = 0
    private var resumeOnFocusGain = false
    private var ticker: Job? = null
    private var focusRequest: AudioFocusRequest? = null

    private var speed = 1f
    private var volume = 1f
    private var sleepJob: Job? = null
    private val _playbackErrors = MutableSharedFlow<PlaybackFailure>(extraBufferCapacity = 1)
    /** يصدر عند تعذّر تشغيل مقطع (فيُتخطى أو يتوقف التشغيل)، مع السبب. */
    val playbackErrors: SharedFlow<PlaybackFailure> = _playbackErrors
    private val _sleepEndsAt = MutableStateFlow<Long?>(null)

    /** وقت انتهاء مؤقّت النوم بالمللي ثانية، أو null إن لم يكن مفعّلاً. */
    val sleepEndsAt: StateFlow<Long?> = _sleepEndsAt.asStateFlow()

    /** معرّف جلسة الصوت الحالية، ويُستخدم لربط مؤثرات Android الصوتية. */
    val audioSessionId: Int get() = currentAudioSessionId

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val platformAudioAttributes = PlatformAudioAttributes.Builder()
        .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
        .setContentType(PlatformAudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val media3AudioAttributes = Media3AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
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
        queue.load(tracks.size, startIndex.coerceIn(0, tracks.lastIndex), queue.shuffle)
        consecutiveErrors = 0
        playCurrent()
    }

    /** يضيف مقاطع إلى نهاية الطابور دون إيقاف المقطع الحالي. */
    fun addToQueue(additions: List<Track>) = enqueue(additions, playNext = false)

    /** يضع المقاطع بعد المقطع الحالي مباشرة، مع الحفاظ على ترتيبها. */
    fun playNext(additions: List<Track>) = enqueue(additions, playNext = true)

    private fun enqueue(additions: List<Track>, playNext: Boolean) {
        if (additions.isEmpty()) return
        val existingIds = tracks.asSequence().map { it.id }.toHashSet()
        val unique = additions.filter { existingIds.add(it.id) }
        if (unique.isEmpty()) return

        val wasEmpty = queue.currentIndex == null
        val firstNewIndex = tracks.size
        tracks = tracks + unique
        queue.append((firstNewIndex until tracks.size).toList(), playNext)
        if (wasEmpty) {
            consecutiveErrors = 0
            playCurrent()
        } else {
            refreshPlaylist(preservePosition = true, autoPlay = playWhenReady)
        }
    }

    /** يشغّل عنصراً من موضعه الحالي في شاشة قائمة التشغيل. */
    fun playQueueItem(queuePosition: Int) {
        if (queue.select(queuePosition) != null) {
            consecutiveErrors = 0
            selectCurrentAndPlay()
        }
    }

    /** يعيد ترتيب عنصرين مع إبقاء المقطع الحالي كما هو. */
    fun moveQueueItem(fromPosition: Int, toPosition: Int) {
        if (queue.move(fromPosition, toPosition)) {
            refreshPlaylist(preservePosition = true, autoPlay = playWhenReady)
        }
    }

    /** يحذف عنصراً من الطابور فقط؛ لا يحذف الملف من الجهاز. */
    fun removeQueueItem(queuePosition: Int) {
        val oldCurrentPosition = queue.currentPosition
        val wasCurrent = queuePosition == oldCurrentPosition
        val shouldResume = playWhenReady
        val removedOriginalIndex = queue.removeAt(queuePosition) ?: return
        tracks = tracks.toMutableList().apply {
            if (removedOriginalIndex in indices) removeAt(removedOriginalIndex)
        }
        if (tracks.isEmpty() || queue.currentIndex == null) {
            stopPlayback()
        } else if (wasCurrent) {
            consecutiveErrors = 0
            refreshPlaylist(preservePosition = false, autoPlay = shouldResume)
        } else {
            refreshPlaylist(preservePosition = true, autoPlay = shouldResume)
        }
    }

    /** يبقي المقطع الحالي ويحذف كل المقاطع القادمة دفعة واحدة. */
    fun clearUpcoming() {
        val current = queue.currentPosition
        if (current < 0) return
        for (index in queue.size - 1 downTo current + 1) {
            val removedOriginalIndex = queue.removeAt(index) ?: continue
            tracks = tracks.toMutableList().apply {
                if (removedOriginalIndex in indices) removeAt(removedOriginalIndex)
            }
        }
        refreshPlaylist(preservePosition = true, autoPlay = playWhenReady)
    }

    fun togglePlay() {
        if (playWhenReady) pause() else resume()
    }

    fun resume() {
        val activePlayer = player
        if (activePlayer == null) {
            if (tracks.isNotEmpty()) playCurrent()
            return
        }
        if (!prepared) {
            if (requestFocus()) {
                playWhenReady = true
                activePlayer.playWhenReady = true
            }
            return
        }
        if (!activePlayer.isPlaying && requestFocus()) {
            playWhenReady = true
            activePlayer.play()
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
        val oldOrder = queue.orderedIndices()
        if (queue.next(auto = false) != null) {
            if (oldOrder != queue.orderedIndices()) {
                refreshPlaylist(preservePosition = false, autoPlay = true)
            } else {
                selectCurrentAndPlay()
            }
        }
    }

    fun previous() {
        val activePlayer = player
        if (activePlayer != null && prepared && activePlayer.currentPosition > RESTART_THRESHOLD_MS) {
            seekTo(0L)
        } else if (queue.previous() != null) {
            selectCurrentAndPlay()
        }
    }

    fun seekTo(positionMs: Long) {
        val activePlayer = player ?: return
        if (!prepared) return
        val duration = activePlayer.duration
        val target = if (duration > 0L && duration != C.TIME_UNSET) {
            positionMs.coerceIn(0L, duration)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        activePlayer.seekTo(target)
        publish()
    }

    fun setSpeed(value: Float) {
        speed = if (value.isFinite() && value > 0f) value.coerceIn(MIN_SPEED, MAX_SPEED) else 1f
        applySpeed()
    }

    /** يطبق السرعة حتى أثناء الإيقاف؛ لا يستدعي play ولا يغيّر نية التشغيل. */
    private fun applySpeed() {
        val activePlayer = player ?: return
        runCatching {
            activePlayer.setPlaybackParameters(PlaybackParameters(speed, 1f))
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepJob = null
        if (minutes <= 0) {
            _sleepEndsAt.value = null
            return
        }
        val safeMinutes = minutes.coerceAtMost(MAX_SLEEP_MINUTES)
        val delayMs = safeMinutes * 60_000L
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
        refreshPlaylist(preservePosition = true, autoPlay = playWhenReady)
    }

    fun cycleRepeat() {
        queue.repeat = queue.repeat.cycle()
        player?.repeatMode = media3RepeatMode()
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

    // ── إدارة المشغّل ───────────────────────────────────────────

    private fun pauseInternal() {
        playWhenReady = false
        player?.pause()
        stopTicker()
        publish()
    }

    private fun setVolumeLevel(level: Float) {
        volume = level.coerceIn(0f, 1f)
        player?.volume = volume
    }

    /**
     * يشغّل المقطع الحالي، ويتخطى تلقائياً المقاطع التي يفشل إنشاء مصدرها.
     * حلقة بدل الاستدعاء الذاتي لتجنّب تعمّق المكدّس في قوائم كبيرة تالفة.
     */
    private fun playCurrent() {
        if (queue.currentIndex == null) return
        if (!requestFocus()) {
            playWhenReady = false
            if (player != null) refreshPlaylist(preservePosition = false, autoPlay = false)
            else publish()
            return
        }
        playWhenReady = true
        val activePlayer = player
        if (activePlayer != null) {
            refreshPlaylist(preservePosition = false, autoPlay = true)
            return
        }

        while (true) {
            val index = queue.currentIndex ?: return
            tracks.getOrNull(index) ?: return
            if (startPlayer()) return
            if (!advanceAfterFailure()) return
        }
    }

    /** يبدّل العنصر المحدد داخل قائمة ExoPlayer دون إنشاء مشغّل جديد. */
    private fun selectCurrentAndPlay() {
        val activePlayer = player
        if (activePlayer == null) {
            playCurrent()
            return
        }
        val focusGranted = requestFocus()
        playWhenReady = focusGranted
        activePlayer.seekTo(queue.currentPosition, 0L)
        activePlayer.playWhenReady = focusGranted
        if (focusGranted) activePlayer.play() else activePlayer.pause()
        if (focusGranted) startTicker() else stopTicker()
        publish()
    }

    /** يعيد مزامنة قائمة ExoPlayer بعد إعادة الترتيب أو الحذف أو الإضافة. */
    private fun refreshPlaylist(preservePosition: Boolean, autoPlay: Boolean) {
        val activePlayer = player
        if (activePlayer == null) {
            if (autoPlay && queue.currentIndex != null) playCurrent() else publish()
            return
        }
        if (queue.size == 0 || queue.currentPosition !in 0 until queue.size) {
            stopPlayback()
            return
        }

        val savedPosition = if (preservePosition) activePlayer.currentPosition.coerceAtLeast(0L) else 0L
        val mediaItems = buildMediaItems()
        if (mediaItems.isEmpty()) {
            stopPlayback()
            return
        }

        playWhenReady = autoPlay
        prepared = false
        activePlayer.setMediaItems(mediaItems, queue.currentPosition, savedPosition)
        activePlayer.repeatMode = media3RepeatMode()
        activePlayer.prepare()
        activePlayer.playWhenReady = autoPlay
        if (autoPlay) activePlayer.play() else activePlayer.pause()
        publish()
    }

    private fun buildMediaItems(): List<MediaItem> =
        queue.orderedIndices().mapNotNull { originalIndex ->
            tracks.getOrNull(originalIndex)?.let { MediaItem.fromUri(it.uri) }
        }

    /** يترك ExoPlayer ينفذ التكرار بلا فجوة صوتية بين المقاطع. */
    private fun media3RepeatMode(): Int = when (queue.repeat) {
        RepeatMode.Off -> Player.REPEAT_MODE_OFF
        RepeatMode.All -> Player.REPEAT_MODE_ALL
        RepeatMode.One -> Player.REPEAT_MODE_ONE
    }

    /** @return true إن أنشأ ExoPlayer وبدأ التحضير، وإلا false. */
    private fun startPlayer(): Boolean {
        var created: ExoPlayer? = null
        return try {
            val exo = ExoPlayer.Builder(context)
                .setAudioAttributes(media3AudioAttributes, false)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build()
            created = exo
            player = exo
            currentAudioSessionId = 0
            exo.setHandleAudioBecomingNoisy(false)
            exo.volume = volume
            val generatedSessionId = runCatching { audioManager.generateAudioSessionId() }.getOrDefault(0)
            if (generatedSessionId > 0) {
                exo.setAudioSessionId(generatedSessionId)
                currentAudioSessionId = generatedSessionId
            }
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (player !== exo) return
                    when (playbackState) {
                        Player.STATE_READY -> {
                            prepared = true
                            consecutiveErrors = 0
                            if (playWhenReady) {
                                exo.play()
                                applySpeed()
                                startTicker()
                            }
                            publish()
                        }
                        Player.STATE_ENDED -> {
                            prepared = true
                            publish()
                            onCompleted()
                        }
                        Player.STATE_IDLE -> {
                            prepared = false
                            publish()
                        }
                        else -> publish()
                    }
                }

                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    if (player !== exo) return
                    val nextSessionId = audioSessionId.coerceAtLeast(0)
                    if (currentAudioSessionId > 0 && currentAudioSessionId != nextSessionId) {
                        DeviceEqualizer.releaseSession(currentAudioSessionId)
                    }
                    currentAudioSessionId = nextSessionId
                    publish()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (player !== exo) return
                    val index = exo.currentMediaItemIndex
                    if (index in 0 until queue.size) queue.select(index)
                    publish()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (player !== exo) return
                    if (isPlaying) startTicker()
                    else if (!playWhenReady) stopTicker()
                    publish()
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (player === exo) handleError(error)
                }
            })

            prepared = false
            val mediaItems = buildMediaItems()
            if (mediaItems.isEmpty()) {
                exo.release()
                player = null
                currentAudioSessionId = 0
                return false
            }
            exo.setMediaItems(mediaItems, queue.currentPosition, 0L)
            exo.repeatMode = media3RepeatMode()
            exo.playWhenReady = playWhenReady
            exo.setPlaybackParameters(PlaybackParameters(speed, 1f))
            exo.prepare()
            publish()
            true
        } catch (_: Exception) {
            if (player === created) player = null
            prepared = false
            currentAudioSessionId = 0
            runCatching { created?.release() }
            false
        }
    }

    private fun onCompleted() {
        if (queue.next(auto = true) != null) {
            refreshPlaylist(preservePosition = false, autoPlay = true)
        } else {
            stopPlayback()
        }
    }

    /** خطأ أثناء التحضير أو التشغيل: نتخطى المقطع التالف. */
    private fun handleError(error: PlaybackException?) {
        val failed = queue.currentIndex?.let { tracks.getOrNull(it) }
        releasePlayer()
        val advanced = advanceAfterFailure()
        _playbackErrors.tryEmit(PlaybackFailure(failed, reasonFor(error), stopped = !advanced))
        if (advanced) playCurrent()
    }

    private fun reasonFor(error: PlaybackException?): String = when (error?.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "تحقق من اتصالك بالإنترنت"
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "الملف لم يعد موجوداً"
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "الخادم لم يستجب لهذا المقطع"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FAILED -> "الملف تالف أو غير مدعوم"
        else -> "حدث خطأ غير متوقع"
    }

    /** يعيد تشغيل مقطع فشل سابقاً (من زر «إعادة المحاولة»). */
    fun retryTrack(track: Track) {
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index < 0) return
        val position = queue.orderedIndices().indexOf(index)
        if (position >= 0) playQueueItem(position)
    }

    /** @return false إن تعطلت القائمة كلها أو انتهت. */
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
        val old = player
        player = null
        prepared = false
        DeviceEqualizer.releaseSession(currentAudioSessionId)
        currentAudioSessionId = 0
        runCatching { old?.release() }
    }

    // ── Audio Focus ─────────────────────────────────────────────

    private fun requestFocus(): Boolean {
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(platformAudioAttributes)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
            .also { focusRequest = it }
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }

    // ── حالة الواجهة ────────────────────────────────────────────

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
        val orderedTracks = queue.orderedIndices().mapNotNull { tracks.getOrNull(it) }
        val track = queue.currentIndex?.let { tracks.getOrNull(it) }
        val activePlayer = player
        val ready = prepared && activePlayer != null
        val rawDuration = if (ready) activePlayer?.duration ?: 0L else track?.durationMs ?: 0L
        val duration = rawDuration.takeIf { it >= 0L && it != C.TIME_UNSET } ?: 0L
        _state.value = PlayerState(
            queue = orderedTracks,
            current = track,
            isPlaying = ready && activePlayer?.isPlaying == true,
            positionMs = if (ready) activePlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L else 0L,
            durationMs = duration,
            shuffle = queue.shuffle,
            repeat = queue.repeat,
            queuePosition = queue.currentPosition,
            audioSessionId = currentAudioSessionId,
        )
    }

    private companion object {
        const val TICK_MS = 500L
        const val RESTART_THRESHOLD_MS = 3_000L
        const val DUCK_VOLUME = 0.3f
        const val MIN_SPEED = 0.25f
        const val MAX_SPEED = 3f
        const val MAX_SLEEP_MINUTES = 24 * 60
    }
}
