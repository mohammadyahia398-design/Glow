package com.glow.app.vocal

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import com.glow.app.GlowStrings
import com.glow.app.R

/** Network boundary for the vocal-isolation service. No provider secret is stored on-device. */
class VocalIsolationClient(private val context: Context) {
    data class Job(val id: String, val status: String)
    data class Progress(val status: String, val ready: Boolean)

    suspend fun checkHealth(baseUrl: String): Unit = withContext(Dispatchers.IO) {
        val c = (URL(normalized(baseUrl) + "/health").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val body = readJson(c)
            if (c.responseCode !in 200..299 || !body.optBoolean("ok")) {
                error(body.optString("error").ifBlank { GlowStrings.get(R.string.vi_err_server_bad) })
            }
        } finally {
            c.disconnect()
        }
    }

    suspend fun separate(baseUrl: String, source: Uri, displayName: String): Job = withContext(Dispatchers.IO) {
        val endpoint = normalized(baseUrl) + "/v1/separate"
        val boundary = "Glow-${UUID.randomUUID()}"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 120_000
            doOutput = true
            setChunkedStreamingMode(64 * 1024)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val input = context.contentResolver.openInputStream(source)
                ?: error(GlowStrings.get(R.string.vi_err_open))
            connection.outputStream.use { raw ->
                BufferedOutputStream(raw).use { out ->
                    out.write("--$boundary\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"${safeName(displayName)}\"\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray())
                    input.use { it.copyTo(out, 64 * 1024) }
                    out.write("\r\n--$boundary--\r\n".toByteArray())
                }
            }
            val body = readJson(connection)
            val id = body.optString("predictionId")
            if (connection.responseCode !in 200..299 || id.isBlank()) {
                val detail = body.optString("error", GlowStrings.get(R.string.vi_err_start, connection.responseCode))
                error(
                    when (connection.responseCode) {
                        429 -> GlowStrings.get(R.string.vi_err_429)
                        413 -> GlowStrings.get(R.string.vi_err_413)
                        503 -> GlowStrings.get(R.string.vi_err_503)
                        else -> detail
                    },
                )
            }
            Job(id, body.optString("status", "starting"))
        } finally {
            connection.disconnect()
        }
    }

    suspend fun awaitResult(baseUrl: String, id: String, onStatus: (String) -> Unit): File = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + 30 * 60 * 1000L
        while (System.currentTimeMillis() < deadline) {
            val c = (URL(normalized(baseUrl) + "/v1/predictions/" + id).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/json")
            }
            val result = try {
                val body = readJson(c)
                if (c.responseCode !in 200..299) {
                    error(body.optString("error", GlowStrings.get(R.string.vi_err_check)))
                }
                Progress(body.optString("status", "processing"), body.optBoolean("audioReady"))
            } finally {
                c.disconnect()
            }
            onStatus(result.status)
            when (result.status.lowercase()) {
                "succeeded" -> if (result.ready) return@withContext download(baseUrl, id)
                    else error(GlowStrings.get(R.string.vi_err_nofile))
                "failed", "canceled", "cancelled" -> error(GlowStrings.get(R.string.vi_err_failed, result.status))
            }
            delay(3_000)
        }
        error(GlowStrings.get(R.string.vi_err_timeout))
    }

    suspend fun cancel(baseUrl: String, id: String) = withContext(Dispatchers.IO) {
        val c = (URL(normalized(baseUrl) + "/v1/predictions/" + id + "/cancel").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 15_000
        }
        try {
            val body = readJson(c)
            if (c.responseCode !in 200..299) {
                error(body.optString("error", GlowStrings.get(R.string.vi_err_cancel)))
            }
        } finally {
            c.disconnect()
        }
    }

    private fun download(baseUrl: String, id: String): File {
        val c = (URL(normalized(baseUrl) + "/v1/predictions/" + id + "/audio").openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 180_000
        }
        try {
            if (c.responseCode !in 200..299) {
                val body = readJson(c)
                error(body.optString("error", GlowStrings.get(R.string.vi_err_download, c.responseCode)))
            }
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "Glow/Vocals").apply { mkdirs() }
            val target = File(dir, "Glow-Vocals-${System.currentTimeMillis()}.wav")
            BufferedInputStream(c.inputStream).use { input ->
                BufferedOutputStream(target.outputStream()).use { output ->
                    input.copyTo(output, 64 * 1024)
                }
            }
            if (!target.exists() || target.length() < 44) {
                target.delete()
                error(GlowStrings.get(R.string.vi_err_invalid_audio))
            }
            return target
        } finally {
            c.disconnect()
        }
    }

    private fun readJson(c: HttpURLConnection): JSONObject {
        val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        return try {
            JSONObject(text)
        } catch (_: Exception) {
            JSONObject().put("error", GlowStrings.get(R.string.vi_err_unclear))
        }
    }

    private fun normalized(value: String) = value.trim().trimEnd('/').also {
        require(it.startsWith("https://")) { GlowStrings.get(R.string.vi_err_backend) }
    }

    private fun safeName(value: String) =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "audio" }
}
