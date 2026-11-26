package com.glow.app

import android.content.Intent

/** أوامر تأتي من اختصارات أيقونة التطبيق (الضغط المطوّل). */
object GlowActions {
    const val SHUFFLE_ALL = "com.glow.app.action.SHUFFLE_ALL"
    const val OPEN_ONLINE = "com.glow.app.action.OPEN_ONLINE"

    fun from(intent: Intent?): String? =
        intent?.action?.takeIf { it == SHUFFLE_ALL || it == OPEN_ONLINE }
}
