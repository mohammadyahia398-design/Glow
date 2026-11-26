package com.glow.app.media

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
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
private const val MAX_IMPORT_BYTES = 1_000_000

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
        // كلمات اختارها المستخدم بنفسه لها الأولوية على الجلب من الإنترنت.
        readCache(manualFile(context, track))?.let { return@withContext it }
        // كلمات مدمجة داخل ملف MP3 نفسه (وسم ID3، إطار USLT).
        readEmbedded(context, track)?.let { return@withContext it }
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

    /** يقرأ كلمات مدمجة في وسم ID3v2.3/2.4 (إطار USLT) لأغنية MP3 محلية؛ null إن لم توجد أو لم يكن الملف MP3. */
    private fun readEmbedded(context: Context, track: Track): LyricsResult? {
        if (track.albumId == -1L || track.uri.scheme != "content") return null
        return try {
            context.contentResolver.openInputStream(track.uri)?.use { raw ->
                val input = java.io.DataInputStream(raw.buffered())
                val head = ByteArray(10)
                input.readFully(head)
                if (head[0] != 'I'.code.toByte() || head[1] != 'D'.code.toByte() || head[2] != '3'.code.toByte()) {
                    return null
                }
                val major = head[3].toInt()
                if (major != 3 && major != 4) return null
                var remaining = syncsafe(head, 6)
                if ((head[5].toInt() and 0x40) != 0) {
                    val sz = ByteArray(4)
                    input.readFully(sz)
                    val extSize = if (major == 4) syncsafe(sz, 0) - 4 else beInt(sz, 0)
                    if (extSize < 0) return null
                    skipFully(input, extSize.toLong())
                    remaining -= 4 + extSize
                }
                val fh = ByteArray(10)
                while (remaining >= 10) {
                    input.readFully(fh)
                    remaining -= 10
                    if (fh[0].toInt() == 0) break
                    val id = String(fh, 0, 4, Charsets.ISO_8859_1)
                    val size = if (major == 4) syncsafe(fh, 4) else beInt(fh, 4)
                    if (size < 0 || size > remaining) break
                    if (id == "USLT" && size in 1..MAX_IMPORT_BYTES) {
                        val body = ByteArray(size)
                        input.readFully(body)
                        return parseUslt(body)
                    }
                    skipFully(input, size.toLong())
                    remaining -= size
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseUslt(body: ByteArray): LyricsResult? {
        if (body.size < 5) return null
        val enc = body[0].toInt()
        val wide = enc == 1 || enc == 2
        var i = 4
        while (i < body.size) {
            if (wide) {
                if (i + 1 < body.size && body[i].toInt() == 0 && body[i + 1].toInt() == 0) {
                    i += 2
                    break
                }
                i += 2
            } else {
                if (body[i].toInt() == 0) {
                    i += 1
                    break
                }
                i += 1
            }
        }
        val bytes = body.copyOfRange(minOf(i, body.size), body.size)
        val text = when (enc) {
            1 -> String(bytes, Charsets.UTF_16)
            2 -> String(bytes, Charsets.UTF_16BE)
            3 -> String(bytes, Charsets.UTF_8)
            else -> decode(bytes)
        }.removePrefix("\uFEFF").replace("\u0000", "")
        val synced = parseLrc(text)
        return when {
            synced.isNotEmpty() -> LyricsResult.Synced(synced)
            text.isNotBlank() -> LyricsResult.Plain(text.lines().map { it.trimEnd() })
            else -> null
        }
    }

    private fun syncsafe(b: ByteArray, o: Int): Int =
        ((b[o].toInt() and 0x7F) shl 21) or ((b[o + 1].toInt() and 0x7F) shl 14) or
            ((b[o + 2].toInt() and 0x7F) shl 7) or (b[o + 3].toInt() and 0x7F)

    private fun beInt(b: ByteArray, o: Int): Int =
        ((b[o].toInt() and 0xFF) shl 24) or ((b[o + 1].toInt() and 0xFF) shl 16) or
            ((b[o + 2].toInt() and 0xFF) shl 8) or (b[o + 3].toInt() and 0xFF)

    private fun skipFully(input: java.io.InputStream, n: Long) {
        var left = n
        while (left > 0) {
            val s = input.skip(left)
            if (s <= 0) {
                if (input.read() < 0) throw java.io.EOFException()
                left--
            } else {
                left -= s
            }
        }
    }

    private fun manualFile(context: Context, track: Track): File {
        val artist = track.artist.trim().takeIf { it.isNotEmpty() && it != UNKNOWN_ARTIST_NAME }
        val key = sha1("$artist|${track.displayTitle}|${track.durationMs / 1000}")
        return File(File(context.filesDir, "lyrics_manual"), key)
    }

    /** هل لهذه الأغنية كلمات اختارها المستخدم بنفسه؟ */
    fun hasManual(context: Context, track: Track): Boolean = manualFile(context, track).exists()

    /** يحذف الكلمات التي اختارها المستخدم فتعود الكلمات التلقائية. */
    fun clearManual(context: Context, track: Track) {
        runCatching { manualFile(context, track).delete() }
    }

    /**
     * يقرأ ملفاً اختاره المستخدم (.lrc مؤقّت أو نص عادي) ويحفظه لهذه الأغنية في ذاكرة التطبيق الدائمة.
     * يرجع false إن تعذّرت القراءة أو كان الملف فارغاً أو كبيراً جداً.
     */
    suspend fun importManual(context: Context, track: Track, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > MAX_IMPORT_BYTES) return@withContext false
                }
                out.toByteArray()
            } ?: return@withContext false
            val text = decode(bytes).removePrefix("\uFEFF")
            val synced = parseLrc(text)
            val result = when {
                synced.isNotEmpty() -> LyricsResult.Synced(synced)
                text.isNotBlank() -> LyricsResult.Plain(text.lines().map { it.trimEnd() })
                else -> return@withContext false
            }
            val file = manualFile(context, track)
            file.parentFile?.mkdirs()
            writeCache(file, result)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** UTF-8 أولاً، وإن لم يكن الملف UTF-8 صالحاً فنقرأه بترميز windows-1256 (ملفات عربية قديمة). */
    private fun decode(bytes: ByteArray): String {
        val strict = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            strict.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            String(bytes, charset("windows-1256"))
        }
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
