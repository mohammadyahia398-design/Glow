package com.glow.app.ui.vocal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import com.glow.app.ui.common.GlowFilledButton
import com.glow.app.ui.common.GlowOutlinedButton
import com.glow.app.ui.common.GlowProgressBar
import com.glow.app.ui.common.GlowRingLoader
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.glow.app.ui.theme.LocalGlowSkin
import com.glow.app.vocal.VocalIsolationClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

private const val DEFAULT_VOCAL_WORKER =
    "https://glow-vocal-worker.mohammadyahia398.workers.dev"

@Composable
fun VocalIsolationScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val skin = LocalGlowSkin.current
    val prefs = remember { context.getSharedPreferences("glow_vocal_settings", Context.MODE_PRIVATE) }
    var endpoint by remember {
        mutableStateOf(prefs.getString("endpoint", null).orEmpty().ifBlank { DEFAULT_VOCAL_WORKER })
    }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("اختر أغنية، ثم افحص اتصال الخادم وابدأ العزل.") }
    var phase by remember { mutableStateOf("idle") }
    var healthState by remember { mutableStateOf("idle") }
    var healthMessage by remember { mutableStateOf("لم يتم فحص الاتصال بعد") }
    var resultFile by remember { mutableStateOf<File?>(null) }
    var predictionId by remember { mutableStateOf("") }
    var work by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val client = remember { VocalIsolationClient(context.applicationContext) }
    val busy = phase == "uploading" || phase == "processing"

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Exception) {
                // Some document providers do not offer persistable grants.
            }
            selectedUri = uri
            selectedName = queryName(context, uri)
            resultFile = null
            predictionId = ""
            phase = "selected"
            status = "الأغنية جاهزة. افحص الاتصال ثم ابدأ العزل."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(skin.bgTop, skin.bgBottom, skin.bgTop),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 26.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                "GLOW  /  STUDIO",
                color = skin.accentText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.4.sp,
            )
            Text(
                "Vocal Isolation",
                color = skin.text,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "استخرج صوت المغني من الأغنية واترك الآلات والإيقاعات خلفه.",
                color = skin.textSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }

        StudioHero(
            skin = skin,
            phase = phase,
            selected = selectedName.isNotBlank(),
        )

        StudioCard(skin = skin) {
            SectionHeading("مصدر الصوت", "01", skin)
            Text(
                if (selectedName.isBlank()) "لم تختر ملفاً صوتياً بعد" else selectedName,
                color = if (selectedName.isBlank()) skin.textSecondary else skin.text,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = if (selectedName.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            GlowOutlinedButton(
                onClick = { picker.launch(arrayOf("audio/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                contentColor = skin.accentText,
            ) {
                Text(if (selectedUri == null) "اختيار أغنية من الجهاز" else "تغيير الأغنية")
            }
            if (selectedUri != null) {
                Text(
                    "الملف الأصلي لن يتم تعديله.",
                    color = skin.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        StudioCard(skin = skin) {
            SectionHeading("اتصال خادم المعالجة", "02", skin)
            Text(
                "يُستخدم هذا العنوان للاتصال بخادم Glow. لا تضع مفتاح Replicate هنا.",
                color = skin.textSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
            OutlinedTextField(
                value = endpoint,
                onValueChange = {
                    endpoint = it
                    prefs.edit().putString("endpoint", it).apply()
                    healthState = "idle"
                    healthMessage = "تغيّر العنوان؛ افحص الاتصال مجدداً."
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = skin.text,
                    fontSize = 13.sp,
                ),
                placeholder = { Text("https://your-worker.workers.dev") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = skin.text,
                    unfocusedTextColor = skin.text,
                    focusedBorderColor = skin.accent,
                    unfocusedBorderColor = skin.divider,
                    focusedLabelColor = skin.accentText,
                    cursorColor = skin.accent,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlowOutlinedButton(
                    onClick = {
                        scope.launch {
                            healthState = "checking"
                            healthMessage = "جارٍ فحص الخادم…"
                            try {
                                client.checkHealth(endpoint)
                                healthState = "ok"
                                healthMessage = "الخادم متصل ويستجيب."
                            } catch (error: Exception) {
                                healthState = "error"
                                healthMessage = error.message ?: "تعذّر الاتصال بالخادم."
                            }
                        }
                    },
                    enabled = !busy && healthState != "checking" && endpoint.startsWith("https://"),
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentColor = skin.accentText,
                ) {
                    if (healthState == "checking") {
                        GlowRingLoader(diameter = 20.dp, strokeWidth = 2.dp, color = skin.accent)
                        Spacer(Modifier.size(8.dp))
                    }
                    Text("اختبار الاتصال")
                }
                Text(
                    healthMessage,
                    modifier = Modifier.weight(1.25f),
                    color = when (healthState) {
                        "ok" -> Color(0xFF55C99A)
                        "error" -> Color(0xFFFF8A78)
                        else -> skin.textSecondary
                    },
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        StudioCard(skin = skin) {
            SectionHeading("عملية العزل", "03", skin)
            Text(
                "يُرسل الملف إلى Demucs عبر خادم Glow. قد تستغرق المعالجة عدة دقائق حسب طول الأغنية والضغط على الخدمة.",
                color = skin.textSecondary,
                fontSize = 13.sp,
                lineHeight = 21.sp,
            )
            GlowFilledButton(
                onClick = {
                    val uri = selectedUri ?: return@GlowFilledButton
                    if (!endpoint.startsWith("https://")) {
                        phase = "error"
                        status = "أدخل عنوان خادم صحيحاً يبدأ بـ https://"
                        return@GlowFilledButton
                    }
                    work = scope.launch {
                        phase = "uploading"
                        predictionId = ""
                        resultFile = null
                        status = "جارٍ رفع الأغنية إلى خادم Glow…"
                        try {
                            val job = client.separate(endpoint, uri, selectedName)
                            predictionId = job.id
                            phase = "processing"
                            status = "بدأ الفصل. يجري الآن استخراج صوت المغني…"
                            val file = client.awaitResult(endpoint, job.id) { state ->
                                status = when (state.lowercase()) {
                                    "starting" -> "الخدمة تجهّز مهمة الفصل…"
                                    "processing" -> "Demucs يعالج الأغنية…"
                                    "succeeded" -> "اكتمل الفصل، جارٍ استلام الملف…"
                                    else -> "حالة المعالجة: $state"
                                }
                            }
                            resultFile = file
                            phase = "done"
                            status = "اكتمل العزل وحُفظ الملف داخل مساحة Glow."
                        } catch (cancel: CancellationException) {
                            phase = "canceled"
                            status = "تم إيقاف العملية من التطبيق."
                            throw cancel
                        } catch (error: Exception) {
                            phase = "error"
                            val message = error.message.orEmpty()
                            status = when {
                                message.contains("429") ||
                                    message.contains("throttled", true) ||
                                    message.contains("rate limit", true) ->
                                    "الخدمة وصلت إلى حد الطلبات حالياً. انتظر حتى تتجدد الحصة قبل إعادة المحاولة."
                                message.contains("50 MB", true) ->
                                    "حجم الملف أكبر من الحد المسموح به (50 ميجابايت). اختر ملفاً أصغر."
                                else -> message.ifBlank { "حدث خطأ غير متوقع أثناء المعالجة." }
                            }
                        }
                    }
                },
                enabled = selectedUri != null && !busy && endpoint.startsWith("https://"),
                modifier = Modifier.fillMaxWidth().height(58.dp),
            ) {
                Text(
                    when (phase) {
                        "uploading" -> "جارٍ رفع الأغنية…"
                        "processing" -> "جارٍ عزل الصوت…"
                        "done" -> "إعادة معالجة الأغنية"
                        else -> "ابدأ عزل صوت المغني"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            if (busy) {
                GlowProgressBar(color = skin.accent)
                GlowOutlinedButton(
                    onClick = {
                        val id = predictionId
                        if (id.isBlank()) {
                            // During upload there is no prediction ID to cancel remotely yet.
                            work?.cancel()
                        } else {
                            scope.launch {
                                try {
                                    client.cancel(endpoint, id)
                                } catch (_: Exception) {
                                    // The local polling job is still stopped below.
                                } finally {
                                    work?.cancel()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    contentColor = Color(0xFFFF8A78),
                ) {
                    Text(if (predictionId.isBlank()) "إيقاف الرفع" else "إلغاء المعالجة")
                }
            }
        }

        StudioCard(skin = skin) {
            SectionHeading("حالة المهمة والنتيجة", "04", skin)
            Text(
                status,
                color = when (phase) {
                    "done" -> Color(0xFF55C99A)
                    "error" -> Color(0xFFFF8A78)
                    else -> skin.text
                },
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
            resultFile?.let { file ->
                HorizontalDivider(color = skin.divider)
                Text(
                    file.name,
                    color = skin.text,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${file.length() / 1024} KB  •  WAV",
                    color = skin.textSecondary,
                    fontSize = 12.sp,
                )
                GlowFilledButton(
                    onClick = { openResult(context, file) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Text("تشغيل صوت المغني")
                }
                GlowOutlinedButton(
                    onClick = { shareResult(context, file) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    contentColor = skin.accentText,
                ) {
                    Text("مشاركة الملف الناتج")
                }
            }
        }

        Text(
            "ملاحظة الجودة: قد تبقى آثار من الآلات أو المؤثرات حسب جودة التسجيل والمكساج الأصلي. المعالجة تستخدم رصيد Replicate وقد تخضع لحدود الاستخدام.",
            color = skin.textSecondary,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun StudioHero(
    skin: com.glow.app.ui.theme.GlowSkin,
    phase: String,
    selected: Boolean,
) {
    val active = phase == "uploading" || phase == "processing"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = skin.panel,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(
                    Brush.linearGradient(
                        listOf(skin.accent.copy(alpha = .95f), skin.accent.copy(alpha = .35f)),
                    ),
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text("♫", color = skin.onAccent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    when {
                        active -> "استوديو العزل يعمل"
                        phase == "done" -> "اكتملت المهمة"
                        phase == "error" -> "تحتاج المهمة إلى مراجعة"
                        selected -> "الأغنية جاهزة"
                        else -> "مساحة فصل الصوت"
                    },
                    color = skin.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text(
                    when {
                        active -> "اترك Glow مفتوحاً حتى تكتمل العملية."
                        phase == "done" -> "ملف صوت المغني أصبح جاهزاً."
                        phase == "error" -> "راجع رسالة الحالة أدناه."
                        selected -> "الخطوة التالية: فحص اتصال الخادم."
                        else -> "اختر ملفاً صوتياً للبدء."
                    },
                    color = skin.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun StudioCard(
    skin: com.glow.app.ui.theme.GlowSkin,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        color = skin.panel,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionHeading(
    title: String,
    index: String,
    skin: com.glow.app.ui.theme.GlowSkin,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = skin.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
            index,
            color = skin.accentText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
        )
    }
}

private fun queryName(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "ملف صوتي"
    } catch (_: Exception) {
        "ملف صوتي"
    }
}

private fun openResult(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "audio/wav")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // The calling screen will continue to show the saved result.
    }
}

private fun shareResult(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND)
        .setType("audio/wav")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(Intent.createChooser(intent, "مشاركة صوت المغني"))
    } catch (_: Exception) {
        // Keep the result available in the app if no share target exists.
    }
}
