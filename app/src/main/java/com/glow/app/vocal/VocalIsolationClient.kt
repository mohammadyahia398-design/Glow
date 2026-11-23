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

/** Network boundary for the vocal-isolation service. No provider secret is stored on-device. */
class VocalIsolationClient(private val context: Context) {
    data class Job(val id: String, val status: String)
    data class Progress(val status: String, val ready: Boolean)

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
                ?: error("تعذّر فتح الملف الصوتي المحدد.")
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
                error(body.optString("error", "تعذّر بدء معالجة الصوت (${connection.responseCode})."))
            }
            Job(id, body.optString("status", "starting"))
        } finally { connection.disconnect() }
    }

    suspend fun awaitResult(baseUrl: String, id: String, onStatus: (String) -> Unit): File = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + 30 * 60 * 1000L
        while (System.currentTimeMillis() < deadline) {
            val c = (URL(normalized(baseUrl) + "/v1/predictions/" + id).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000; readTimeout = 30_000; setRequestProperty("Accept", "application/json")
            }
            val result = try {
                val body = readJson(c)
                if (c.responseCode !in 200..299) error(body.optString("error", "تعذّر التحقق من حالة المعالجة."))
                Progress(body.optString("status", "processing"), body.optBoolean("audioReady"))
            } finally { c.disconnect() }
            onStatus(result.status)
            when (result.status.lowercase()) {
                "succeeded" -> if (result.ready) return@withContext download(baseUrl, id)
                    else error("اكتملت المعالجة لكن لم يُعثر على ملف صوت المغني.")
                "failed", "canceled", "cancelled" -> error("فشلت المعالجة: حالة المهمة ${result.status}.")
            }
            delay(3_000)
        }
        error("انتهت مهلة المعالجة. يمكنك المحاولة مجدداً.")
    }

    suspend fun cancel(baseUrl: String, id: String) = withContext(Dispatchers.IO) {
        val c = (URL(normalized(baseUrl) + "/v1/predictions/" + id + "/cancel").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 15_000
        }
        try { c.inputStream?.close() } catch (_: Exception) { try { c.errorStream?.close() } catch (_: Exception) {} }
        finally { c.disconnect() }
    }

    private fun download(baseUrl: String, id: String): File {
        val c = (URL(normalized(baseUrl) + "/v1/predictions/" + id + "/audio").openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000; readTimeout = 180_000
        }
        try {
            if (c.responseCode !in 200..299) error("تعذّر تنزيل ملف الصوت الناتج (${c.responseCode}).")
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "Glow/Vocals").apply { mkdirs() }
            val target = File(dir, "Glow-Vocals-${System.currentTimeMillis()}.wav")
            BufferedInputStream(c.inputStream).use { input -> BufferedOutputStream(target.outputStream()).use { output -> input.copyTo(output, 64 * 1024) } }
            if (!target.exists() || target.length() < 44) { target.delete(); error("الملف الصوتي الناتج غير صالح أو فارغ.") }
            return target
        } finally { c.disconnect() }
    }

    private fun readJson(c: HttpURLConnection): JSONObject {
        val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        return try { JSONObject(text) } catch (_: Exception) { JSONObject().put("error", "استجابة غير مفهومة من الخادم.") }
    }
    private fun normalized(value: String) = value.trim().trimEnd('/').also {
        require(it.startsWith("https://")) { "أدخل عنوان Backend صالحاً يبدأ بـ https://" }
    }
    private fun safeName(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "audio" }
}
