package com.glow.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.media.DeviceEqualizer
import com.glow.app.ui.theme.LocalGlowSkin
import kotlin.math.roundToInt

/** لوحة معادل صوتي حيّة مرتبطة بجلسة المشغّل الحالية. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    audioSessionId: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val skin = LocalGlowSkin.current
    val engine = remember(audioSessionId) { DeviceEqualizer(audioSessionId) }
    val bandCount = engine.bandCount
    val range = engine.levelRange
    val levels = remember(engine, bandCount) {
        mutableStateListOf<Int>().apply {
            repeat(bandCount) { index -> add(engine.bandLevel(index) ?: 0) }
        }
    }
    val presets = remember(engine) { engine.presetNames }
    var enabled by remember(engine) { mutableStateOf(engine.enabled) }
    var selectedPreset by remember(engine) {
        mutableIntStateOf(engine.currentPresetIndex?.takeIf { it >= 0 }?.plus(1) ?: 0)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = skin.panel,
        contentColor = skin.text,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 28.dp)) {
            Text(
                "المعادل الصوتي",
                color = skin.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "تحكم مباشر في ترددات الصوت للمقطع الحالي",
                color = skin.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
            )

            if (!engine.available || bandCount == 0) {
                Text(
                    if (audioSessionId <= 0) {
                        "ابدأ تشغيل مقطع موسيقي أولاً لتفعيل المعادل."
                    } else {
                        "لا يوفر هذا الجهاز معادلاً صوتياً متاحاً لجلسة التشغيل الحالية."
                    },
                    color = skin.textSecondary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("تفعيل المعادل", color = skin.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("يطبّق التأثير على جلسة الصوت الحالية", color = skin.textSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = engine.setEnabled(it)
                        },
                    )
                }

                Text("الإعدادات المسبقة", color = skin.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp),
                ) {
                    itemsIndexed(presets) { index, name ->
                        FilterChip(
                            selected = selectedPreset == index,
                            onClick = {
                                selectedPreset = index
                                if (index > 0 && engine.applyPreset(index - 1)) {
                                    refreshLevels(engine, levels)
                                    enabled = engine.setEnabled(true)
                                }
                            },
                            label = { Text(name.ifBlank { "Preset ${index}" }) },
                        )
                    }
                }

                Text("النطاقات الترددية", color = skin.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(levels) { index, level ->
                        val frequency = engine.centerFrequencyHz(index)
                        val label = frequency?.let(::frequencyLabel) ?: "النطاق ${index + 1}"
                        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(label, color = skin.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    formatDb(level),
                                    color = skin.accentText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Slider(
                                value = level.toFloat(),
                                onValueChange = { value ->
                                    val next = value.roundToInt().coerceIn(range.first, range.last)
                                    if (engine.setBandLevel(index, next)) {
                                        levels[index] = next
                                        selectedPreset = 0
                                        if (!enabled) enabled = engine.setEnabled(true)
                                    }
                                },
                                valueRange = range.first.toFloat()..range.last.toFloat(),
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    androidx.compose.material3.TextButton(onClick = {
                        if (engine.resetFlat()) {
                            refreshLevels(engine, levels)
                            selectedPreset = 0
                        }
                    }) {
                        Text("إعادة ضبط", color = skin.accentText)
                    }
                }
            }
        }
    }
}

private fun refreshLevels(engine: DeviceEqualizer, target: MutableList<Int>) {
    target.indices.forEach { index -> target[index] = engine.bandLevel(index) ?: target[index] }
}

private fun frequencyLabel(hz: Int): String = when {
    hz >= 1_000 -> "${(hz / 100f).roundToInt() / 10f} kHz"
    else -> "$hz Hz"
}

private fun formatDb(millibels: Int): String {
    val db = millibels / 100f
    return if (db > 0f) "+${"%.1f".format(db)} dB" else "${"%.1f".format(db)} dB"
}
