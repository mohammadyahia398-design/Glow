package com.glow.app.ui.vocal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
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
import com.glow.app.ui.common.GlowFilledButton
import com.glow.app.ui.common.GlowOutlinedButton
import com.glow.app.ui.common.GlowProgressBar
import com.glow.app.ui.common.GlowRingLoader
import androidx.compose.material3.HorizontalDivider
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
import com.glow.app.GlowStrings
import com.glow.app.R
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import com.glow.app.liquid.backdrop.backdrops.layerBackdrop
import com.glow.app.liquid.backdrop.backdrops.rememberLayerBackdrop
import com.glow.app.liquid.ui.LiquidGlassCard
import com.glow.app.ui.common.LocalLiquidBackdrop
import com.glow.app.ui.common.glowLiquid
import com.glow.app.ui.glass.core.rememberGlassTier

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
    var status by remember { mutableStateOf(GlowStrings.get(R.string.vi_status_pick)) }
    var phase by remember { mutableStateOf("idle") }
    var healthState by remember { mutableStateOf("idle") }
    var healthMessage by remember { mutableStateOf(GlowStrings.get(R.string.vi_health_unchecked)) }
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
            status = GlowStrings.get(R.string.vi_ready)
        }
    }

    val glassTier = rememberGlassTier()
    val vocalBackdrop = rememberLayerBackdrop()
    val liquidBackdrop = if (glassTier.supportsRefraction) vocalBackdrop else null
    CompositionLocalProvider(LocalLiquidBackdrop provides liquidBackdrop) {
    Box(modifier.fillMaxSize()) {
    // خلفية هادئة بوهجين؛ هي ما تنكسر خلف البطاقات والأزرار الزجاجية.
    Box(
        Modifier
            .matchParentSize()
            .layerBackdrop(vocalBackdrop)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(skin.bgTop, skin.bgBottom, skin.bgTop)))
                drawRect(
                    Brush.radialGradient(
                        listOf(skin.accent.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.12f),
                        radius = size.width * 0.9f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFF7226FF).copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 0.6f),
                        radius = size.width * 0.9f,
                    ),
                )
            },
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                GlowStrings.get(R.string.vi_desc),
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
            SectionHeading(GlowStrings.get(R.string.vi_sec_source), "01", skin)
            Text(
                if (selectedName.isBlank()) GlowStrings.get(R.string.vi_no_file) else selectedName,
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
                Text(if (selectedUri == null) GlowStrings.get(R.string.vi_pick_song) else GlowStrings.get(R.string.vi_change_song))
            }
            if (selectedUri != null) {
                Text(
                    GlowStrings.get(R.string.vi_original_safe),
                    color = skin.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        StudioCard(skin = skin) {
            SectionHeading(GlowStrings.get(R.string.vi_sec_conn), "02", skin)
            Text(
                GlowStrings.get(R.string.vi_conn_hint),
                color = skin.textSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
            var endpointFocused by remember { mutableStateOf(false) }
            val fieldShape = RoundedCornerShape(50)
            BasicTextField(
                value = endpoint,
                onValueChange = {
                    endpoint = it
                    prefs.edit().putString("endpoint", it).apply()
                    healthState = "idle"
                    healthMessage = GlowStrings.get(R.string.vi_addr_changed)
                },
                enabled = !busy,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = skin.text, fontSize = 13.sp),
                cursorBrush = SolidColor(skin.accent),
                modifier = Modifier.fillMaxWidth().onFocusChanged { endpointFocused = it.isFocused },
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .glowLiquid(
                                fieldShape,
                                Brush.verticalGradient(listOf(skin.chip.copy(alpha = 0.95f), skin.chip.copy(alpha = 0.70f))),
                                skin.isDark,
                                rimColor = if (endpointFocused) skin.accent else Color.White,
                                fillAlpha = 0.5f,
                                interactive = false,
                            )
                            .then(if (endpointFocused) Modifier.border(1.5.dp, skin.accent, fieldShape) else Modifier)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (endpoint.isEmpty()) {
                            Text("https://your-worker.workers.dev", color = skin.textSecondary, fontSize = 13.sp)
                        }
                        inner()
                    }
                },
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
                            healthMessage = GlowStrings.get(R.string.vi_checking)
                            try {
                                client.checkHealth(endpoint)
                                healthState = "ok"
                                healthMessage = GlowStrings.get(R.string.vi_connected)
                            } catch (error: Exception) {
                                healthState = "error"
                                healthMessage = error.message ?: GlowStrings.get(R.string.vi_conn_fail)
                            }
                        }
                    },
                    enabled = !busy && healthState != "checking" && endpoint.startsWith("https://"),
                    modifier = Modifier.height(48.dp),
                    contentColor = skin.accentText,
                ) {
                    if (healthState == "checking") {
                        GlowRingLoader(diameter = 20.dp, strokeWidth = 2.dp, color = skin.accent)
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(GlowStrings.get(R.string.vi_test_conn), maxLines = 1, softWrap = false)
                }
                Text(
                    healthMessage,
                    modifier = Modifier.weight(1f),
                    color = when (healthState) {
                        "ok" -> Color(0xFF87F5F5)
                        "error" -> Color(0xFFFF6B8E)
                        else -> skin.textSecondary
                    },
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        StudioCard(skin = skin) {
            SectionHeading(GlowStrings.get(R.string.vi_sec_process), "03", skin)
            Text(
                GlowStrings.get(R.string.vi_process_hint),
                color = skin.textSecondary,
                fontSize = 13.sp,
                lineHeight = 21.sp,
            )
            GlowFilledButton(
                onClick = {
                    val uri = selectedUri ?: return@GlowFilledButton
                    if (!endpoint.startsWith("https://")) {
                        phase = "error"
                        status = GlowStrings.get(R.string.vi_bad_url)
                        return@GlowFilledButton
                    }
                    work = scope.launch {
                        phase = "uploading"
                        predictionId = ""
                        resultFile = null
                        status = GlowStrings.get(R.string.vi_uploading_glow)
                        try {
                            val job = client.separate(endpoint, uri, selectedName)
                            predictionId = job.id
                            phase = "processing"
                            status = GlowStrings.get(R.string.vi_started)
                            val file = client.awaitResult(endpoint, job.id) { state ->
                                status = when (state.lowercase()) {
                                    "starting" -> GlowStrings.get(R.string.vi_st_starting)
                                    "processing" -> GlowStrings.get(R.string.vi_st_processing)
                                    "succeeded" -> GlowStrings.get(R.string.vi_st_succeeded)
                                    else -> GlowStrings.get(R.string.vi_st_other, state)
                                }
                            }
                            resultFile = file
                            phase = "done"
                            status = GlowStrings.get(R.string.vi_done)
                        } catch (cancel: CancellationException) {
                            phase = "canceled"
                            status = GlowStrings.get(R.string.vi_cancelled)
                            throw cancel
                        } catch (error: Exception) {
                            phase = "error"
                            val message = error.message.orEmpty()
                            status = when {
                                message.contains("429") ||
                                    message.contains("throttled", true) ||
                                    message.contains("rate limit", true) ->
                                    GlowStrings.get(R.string.vi_rate_limit)
                                message.contains("50 MB", true) ->
                                    GlowStrings.get(R.string.vi_too_big)
                                else -> message.ifBlank { GlowStrings.get(R.string.vi_unexpected) }
                            }
                        }
                    }
                },
                enabled = selectedUri != null && !busy && endpoint.startsWith("https://"),
                modifier = Modifier.fillMaxWidth().height(58.dp),
            ) {
                Text(
                    when (phase) {
                        "uploading" -> GlowStrings.get(R.string.vi_btn_uploading)
                        "processing" -> GlowStrings.get(R.string.vi_btn_processing)
                        "done" -> GlowStrings.get(R.string.vi_btn_reprocess)
                        else -> GlowStrings.get(R.string.vi_btn_start)
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
                    contentColor = Color(0xFFFF6B8E),
                ) {
                    Text(if (predictionId.isBlank()) GlowStrings.get(R.string.vi_stop_upload) else GlowStrings.get(R.string.vi_cancel_proc))
                }
            }
        }

        StudioCard(skin = skin) {
            SectionHeading(GlowStrings.get(R.string.vi_sec_result), "04", skin)
            Text(
                status,
                color = when (phase) {
                    "done" -> Color(0xFF87F5F5)
                    "error" -> Color(0xFFFF6B8E)
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
                    Text(GlowStrings.get(R.string.vi_play_vocal))
                }
                GlowOutlinedButton(
                    onClick = { shareResult(context, file) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    contentColor = skin.accentText,
                ) {
                    Text(GlowStrings.get(R.string.vi_share_result))
                }
            }
        }

        Text(
            GlowStrings.get(R.string.vi_quality_note),
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
                        active -> GlowStrings.get(R.string.vi_hero_active)
                        phase == "done" -> GlowStrings.get(R.string.vi_hero_done)
                        phase == "error" -> GlowStrings.get(R.string.vi_hero_error)
                        selected -> GlowStrings.get(R.string.vi_hero_selected)
                        else -> GlowStrings.get(R.string.vi_hero_idle)
                    },
                    color = skin.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text(
                    when {
                        active -> GlowStrings.get(R.string.vi_sub_active)
                        phase == "done" -> GlowStrings.get(R.string.vi_sub_done)
                        phase == "error" -> GlowStrings.get(R.string.vi_sub_error)
                        selected -> GlowStrings.get(R.string.vi_sub_selected)
                        else -> GlowStrings.get(R.string.vi_sub_idle)
                    },
                    color = skin.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
    }
    }
}

@Composable
private fun StudioCard(
    skin: com.glow.app.ui.theme.GlowSkin,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val backdrop = LocalLiquidBackdrop.current
    val shape = RoundedCornerShape(25.dp)
    if (backdrop != null) {
        Box(Modifier.fillMaxWidth()) {
            LiquidGlassCard(
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 25.dp,
                surfaceColor = skin.panel.copy(alpha = if (skin.isDark) 0.68f else 0.74f),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                    content = content,
                )
            }
            Box(
                Modifier
                    .matchParentSize()
                    .border(1.dp, skin.glassBorder.copy(alpha = 0.22f), shape),
            )
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = skin.panel,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
                content = content,
            )
        }
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
        } ?: GlowStrings.get(R.string.audio_file)
    } catch (_: Exception) {
        GlowStrings.get(R.string.audio_file)
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
        context.startActivity(Intent.createChooser(intent, GlowStrings.get(R.string.vi_share_chooser)))
    } catch (_: Exception) {
        // Keep the result available in the app if no share target exists.
    }
}
