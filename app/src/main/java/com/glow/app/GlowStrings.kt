package com.glow.app

import android.content.Context

/**
 * وصول موحّد إلى نصوص الواجهة من أي مكان (واجهة، خدمة، ويدجت) بلغة الجهاز الحالية.
 * تُهيَّأ مرة واحدة من [GlowApplication].
 */
object GlowStrings {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(id: Int, vararg args: Any?): String {
        val ctx = appContext ?: return ""
        return if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)
    }

    /** «5 أغاني» / «5 songs» بصيغة الجمع الصحيحة للغة. */
    fun songs(count: Int): String {
        val ctx = appContext ?: return count.toString()
        return ctx.resources.getQuantityString(R.plurals.songs_count, count, count)
    }
}
