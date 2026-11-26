package com.glow.app.online

import android.net.Uri
import com.glow.app.media.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** بحث Audius المجاني (بلا مفتاح). الروابط تُشغَّل مباشرة عبر ExoPlayer. */
object AudiusClient {
    private const val BASE = "https://api.audius.co/v1"
    private const val APP = "Glow"

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, "UTF-8")
        val conn = URL("$BASE/tracks/search?query=$q&limit=30&app_name=$APP")
            .openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode !in 200..299) throw IOException("HTTP " + conn.responseCode)
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parse(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(body: String): List<Track> {
        val data = JSONObject(body).optJSONArray("data") ?: return emptyList()
        val out = ArrayList<Track>()
        for (i in 0 until data.length()) {
            val o = data.optJSONObject(i) ?: continue
            val id = o.optString("id")
            if (id.isEmpty()) continue
            if (!o.optBoolean("is_streamable", true) || o.optBoolean("is_stream_gated", false)) continue
            out.add(
                Track(
                    id = -(1_000_000_000L + (id.hashCode().toLong() and 0x7FFFFFFFL)),
                    title = o.optString("title").ifBlank { "بدون عنوان" },
                    artist = o.optJSONObject("user")?.optString("name").orEmpty().ifBlank { "فنان غير معروف" },
                    album = "Audius",
                    albumId = -1L,
                    durationMs = o.optLong("duration") * 1000L,
                    uri = Uri.parse("$BASE/tracks/$id/stream?app_name=$APP"),
                    genre = o.optString("genre"),
                    artworkUrl = o.optJSONObject("artwork")
                        ?.let { a -> a.optString("480x480").ifBlank { a.optString("150x150") } }
                        ?.ifBlank { null },
                ),
            )
        }
        return out
    }
}
