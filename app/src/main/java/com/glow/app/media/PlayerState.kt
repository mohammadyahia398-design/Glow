package com.glow.app.media

/**
 * لقطة غير قابلة للتعديل من حالة المشغّل تقرؤها الواجهة والخدمة.
 * queue مرتبة تماماً كما ستُشغّل، وqueuePosition هو موضع المقطع الحالي فيها.
 */
data class PlayerState(
    val queue: List<Track> = emptyList(),
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
    val queuePosition: Int = -1,
    val audioSessionId: Int = 0,
) {
    /** نسبة التقدم من 0 إلى 1. */
    val progress: Float
        get() = if (durationMs > 0) {
            (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }

    /** عدد المقاطع التي ستأتي بعد المقطع الحالي. */
    val upcomingCount: Int
        get() = if (queuePosition < 0) 0 else (queue.size - queuePosition - 1).coerceAtLeast(0)
}
