package com.glow.app.ui.vocal

import android.net.Uri
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.vocal.VocalIsolationClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VocalIsolationScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("glow_vocal_settings", 0) }
    var endpoint by remember { mutableStateOf(prefs.getString("endpoint", "") ?: "") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("اختر أغنية لبدء فصل صوت المغني") }
    var phase by remember { mutableStateOf("idle") }
    var resultFile by remember { mutableStateOf<File?>(null) }
    var predictionId by remember { mutableStateOf("") }
    var work by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val client = remember { VocalIsolationClient(context.applicationContext) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            selectedUri = uri
            selectedName = queryName(context, uri)
            resultFile = null
            status = "جاهز للمعالجة"
            phase = "selected"
        }
    }

    Column(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF101D27), Color(0xFF071018)))).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(22.dp))
        Text("GLOW / STUDIO", color = Color(0xFF8DDCCB), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Vocal Isolation", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("افصل الأداء الصوتي عن الآلات والإيقاعات", color = Color(0xFFB6C7CE), fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        Surface(color = Color(0xFF172B37).copy(alpha = .88f), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("مصدر الصوت", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(if (selectedName.isBlank()) "لم يتم اختيار ملف بعد" else selectedName, color = Color(0xFFB8C9D0), maxLines = 2)
                OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) { Text("اختيار أغنية من الجهاز") }
                HorizontalDivider(color = Color.White.copy(alpha = .10f))
                Text("عنوان خادم المعالجة", color = Color.White, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = endpoint, onValueChange = { endpoint = it; prefs.edit().putString("endpoint", it).apply() }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("https://your-worker.workers.dev") }, supportingText = { Text("يُضاف عنوان Cloudflare Worker بعد نشره. لا تضع مفتاح Replicate هنا.") })
                Button(onClick = {
                    val uri = selectedUri ?: return@Button
                    if (endpoint.isBlank()) { status = "أدخل عنوان الخادم أولاً"; return@Button }
                    work = scope.launch {
                        phase = "uploading"; status = "جارٍ رفع الملف بأمان…"
                        try {
                            val job = client.separate(endpoint, uri, selectedName)
                            predictionId = job.id
                            phase = "processing"; status = "جارٍ فصل صوت المغني باستخدام الذكاء الاصطناعي…"
                            val file = client.awaitResult(endpoint, job.id) { state -> status = when (state.lowercase()) { "starting", "processing" -> "النموذج يعالج الأغنية…"; "succeeded" -> "اكتملت المعالجة"; else -> "حالة المعالجة: $state" } }
                            resultFile = file; phase = "done"; status = "تم حفظ الملف الناتج داخل مساحة Glow الخاصة بالتطبيق."
                        } catch (cancel: CancellationException) { phase = "canceled"; status = "تم إيقاف المعالجة"; throw cancel }
                        catch (error: Exception) { phase = "error"; status = error.message ?: "حدث خطأ غير متوقع" }
                    }
                }, enabled = selectedUri != null && endpoint.startsWith("https://") && phase !in listOf("uploading", "processing"), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("ابدأ عزل صوت المغني", fontWeight = FontWeight.Bold) }
                if (phase == "uploading" || phase == "processing") {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { val id = predictionId; work?.cancel(); if (id.isNotBlank()) scope.launch { try { client.cancel(endpoint, id) } catch (_: Exception) {} } }, modifier = Modifier.fillMaxWidth()) { Text("إلغاء المعالجة") }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Surface(color = Color(0xFF10232D), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("حالة المهمة", color = Color(0xFF8DDCCB), fontWeight = FontWeight.Bold)
                Text(status, color = Color.White)
                resultFile?.let { file ->
                    Text("${file.name} • ${file.length() / 1024} KB", color = Color(0xFFB6C7CE), fontSize = 12.sp)
                    Button(onClick = {
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "audio/wav").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        try { context.startActivity(intent) } catch (_: Exception) { status = "لا يوجد تطبيق لتشغيل WAV." }
                    }, modifier = Modifier.fillMaxWidth()) { Text("تشغيل الملف الناتج") }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("ملاحظة الجودة: الفصل بالذكاء الاصطناعي قد يترك آثاراً موسيقية خفيفة حسب جودة التسجيل والمكساج الأصلي.", color = Color(0xFF8EA3AD), fontSize = 12.sp, lineHeight = 18.sp)
    }
}

private fun queryName(context: android.content.Context, uri: Uri): String {
    return try { context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "ملف صوتي" } catch (_: Exception) { "ملف صوتي" }
}
