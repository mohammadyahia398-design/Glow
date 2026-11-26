package com.glow.app.media

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** سطر كلمات مؤقّت: [timeMs] لحظة ظهوره من بداية الأغنية. */
data class LyricLine(val timeMs: Long, val text: String)

sealed interface LyricsResult {
    /** كلمات مزامنة مع الوقت. */
    data class Synced(val lines: List<LyricLine>) : LyricsResult

    /** كلمات نصية بلا توقيت. */
    data class Plain(val lines: List<String>) : LyricsResult

    /** لا توجد كلمات لهذه الأغنية. */
    data object None : LyricsResult

    /** تعذّر الجلب (شبكة أو خادم)؛ لا يُخزَّن فيمكن إعادة المحاولة. */
    data object Failed : LyricsResult
}

private val LRC_TIME = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
private const val UNKNOWN_ARTIST_NAME = "فنان غير معروف"
private const val NEGATIVE_CACHE_MS = 7L * 24 * 60 * 60 * 1000

/** يحوّل نص LRC إلى أسطر مرتبة بالزمن. الأسطر الفارغة تصبح «♪». */
internal fun parseLrc(text: String): List<LyricLine> {
    val out = ArrayList<LyricLine>()
    for (raw in text.lineSequence()) {
        val line = raw.trim()
        if (!line.startsWith("[")) continue
        val times = ArrayList<Long>()
        var end = 0
        var m = LRC_TIME.find(line, 0)
        while (m != null && m.range.first == end) {
            val min = m.groupValues[1].toLong()
            val sec = m.groupValues[2].toLong()
            val frac = m.groupValues[3]
            val ms = when (frac.length) {
                0 -> 0L
                1 -> frac.toLong() * 100
                2 -> frac.toLong() * 10
                else -> frac.take(3).toLong()
            }
            times += (min * 60 + sec) * 1000 + ms
            end = m.range.last + 1
            m = LRC_TIME.find(line, end)
        }
        if (times.isEmpty()) continue
        val body = line.substring(end).trim().ifEmpty { "♪" }
        times.forEach { out += LyricLine(it, body) }
    }
    return out.sortedBy { it.timeMs }
}

/** يجلب كلمات الأغنية من lrclib.net (مجاني بلا مفتاح) ويخزّنها في ذاكرة التطبيق المؤقتة. */
object LyricsRepository {

    suspend fun load(context: Context, track: Track): LyricsResult = withContext(Dispatchers.IO) {
        val title = track.displayTitle
        if (title.startsWith("رسالة واتساب") || title.isBlank()) return@withContext LyricsResult.None
        val artist = track.artist.trim().takeIf { it.isNotEmpty() && it != UNKNOWN_ARTIST_NAME }

        val dir = File(context.cacheDir, "lyrics").apply { mkdirs() }
        val file = File(dir, sha1("$artist|$title|${track.durationMs / 1000}"))
        readCache(file)?.let { return@withContext it }

        val result = try {
            fetch(title, artist, track.durationMs / 1000)
        } catch (_: Exception) {
            return@withContext LyricsResult.Failed
        }
        runCatching { writeCache(file, result) }
        result
    }

    private fun fetch(title: String, artist: String?, seconds: Long): LyricsResult {
        val query = if (artist != null) {
            "track_name=${enc(title)}&artist_name=${enc(artist)}"
        } else {
            "q=${enc(title)}"
        }
        val conn = URL("https://lrclib.net/api/search?$query").openConnection() as HttpURLConnection
        val body = try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", "Glow/1.0 (https://github.com/mohammadyahia398-design/Glow)")
            if (conn.responseCode != 200) throw java.io.IOException("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }

        val arr = JSONArray(body)
        class Hit(val synced: String, val plain: String, val gap: Long)
        val hits = ArrayList<Hit>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optBoolean("instrumental", false)) continue
            val synced = o.optString("syncedLyrics", "").takeIf { it != "null" }.orEmpty()
            val plain = o.optString("plainLyrics", "").takeIf { it != "null" }.orEmpty()
            if (synced.isBlank() && plain.isBlank()) continue
            val gap = if (seconds > 0) abs(o.optDouble("duration", 0.0).toLong() - seconds) else 0L
            hits += Hit(synced, plain, gap)
        }

        // الأفضل: مؤقّتة وبمدة قريبة (حتى 4 ثوانٍ)، ثم أي مؤقّتة، ثم نصية الأقرب مدة.
        val synced = hits.filter { it.synced.isNotBlank() }
        val best = synced.filter { it.gap <= 4 }.minByOrNull { it.gap }
            ?: synced.minByOrNull { it.gap }
        if (best != null) {
            val lines = parseLrc(best.synced)
            if (lines.isNotEmpty()) return LyricsResult.Synced(lines)
        }
        val plain = hits.filter { it.plain.isNotBlank() }.minByOrNull { it.gap }
        if (plain != null) {
            return LyricsResult.Plain(plain.plain.lines().map { it.trimEnd() })
        }
        return LyricsResult.None
    }

    private fun readCache(file: File): LyricsResult? {
        if (!file.exists()) return null
        val text = runCatching { file.readText() }.getOrNull() ?: return null
        val body = text.drop(2)
        return when (text.firstOrNull()) {
            'S' -> parseLrc(body).takeIf { it.isNotEmpty() }?.let { LyricsResult.Synced(it) }
            'P' -> LyricsResult.Plain(body.lines())
            'N' -> if (System.currentTimeMillis() - file.lastModified() < NEGATIVE_CACHE_MS) LyricsResult.None else null
            else -> null
        }
    }

    private fun writeCache(file: File, result: LyricsResult) {
        when (result) {
            is LyricsResult.Synced ->
                file.writeText("S\n" + result.lines.joinToString("\n") { "[${stamp(it.timeMs)}]${it.text}" })
            is LyricsResult.Plain -> file.writeText("P\n" + result.lines.joinToString("\n"))
            LyricsResult.None -> file.writeText("N\n")
            LyricsResult.Failed -> Unit
        }
    }

    private fun stamp(ms: Long): String {
        val min = ms / 60000
        val sec = (ms / 1000) % 60
        val cs = (ms % 1000) / 10
        return "%02d:%02d.%02d".format(min, sec, cs)
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
