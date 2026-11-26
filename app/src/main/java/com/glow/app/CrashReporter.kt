package com.glow.app

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

/**
 * يسجّل سبب أي انهيار في ملف صغير، ويعرضه التطبيق عند الفتح التالي ليُنسخ ويُرسل للمطوّر.
 * لا يرسل شيئاً خارج الجهاز.
 */
object CrashReporter {
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val full = Log.getStackTraceString(error).lines()
                val trimmed = if (full.size > 130) {
                    full.take(70) + listOf(GlowStrings.get(R.string.crash_trimmed)) + full.takeLast(55)
                } else {
                    full
                }
                val text = buildString {
                    append("Glow crash\n")
                    append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) ")
                    append("${Build.MANUFACTURER} ${Build.MODEL}\n")
                    append("thread: ${thread.name}\n\n")
                    append(trimmed.joinToString("\n"))
                }
                File(app.filesDir, FILE).writeText(text)
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** يرجع نص آخر انهيار (إن وُجد) ثم يمسحه. */
    fun take(context: Context): String? {
        val file = File(context.applicationContext.filesDir, FILE)
        if (!file.exists()) return null
        val text = runCatching { file.readText() }.getOrNull()
        runCatching { file.delete() }
        return text
    }
}
