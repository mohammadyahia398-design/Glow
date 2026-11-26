package com.glow.app.media

/**
 * تعذّر تشغيل مقطع: [track] هو المقطع الفاشل، و[reason] سبب مفهوم للمستخدم،
 * و[stopped] صحيح إن توقف التشغيل كله (لا مقطع تالٍ يصلح) بدل التخطي.
 */
data class PlaybackFailure(
    val track: Track?,
    val reason: String,
    val stopped: Boolean,
)
