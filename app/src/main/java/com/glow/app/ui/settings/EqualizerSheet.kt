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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.media.DeviceEqualizer
import com.glow.app.liquid.backdrop.backdrops.rememberCanvasBackdrop
import com.glow.app.liquid.ui.LiquidSlider
import com.glow.app.liquid.ui.LiquidToggle
import com.glow.app.ui.common.GlowBottomSheet
import com.glow.app.ui.common.GlowChip
import com.glow.app.ui.common.LocalDialogBackdrop
import com.glow.app.ui.glass.components.GlassSwitch
import com.glow.app.ui.glass.core.rememberGlassTier
import com.glow.app.ui.theme.LocalGlowSkin
import kotlin.math.roundToInt

/** لوحة معادل صوتي حيّة مرتبطة بجلسة المشغّل الحالية. */
@Composable
fun EqualizerSheet(
    audioSessionId: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val skin = LocalGlowSkin.current
    val glassTier = rememberGlassTier()
    val eqBackdrop = rememberCanvasBackdrop { drawRect(skin.panel) }
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

    GlowBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        // خلفية الزجاج من اللوحة نفسها (أندرويد 13+)، وإلا خلفية مرسومة بلون السطح.
        val sliderBackdrop = LocalDialogBackdrop.current ?: eqBackdrop
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
                    if (glassTier.supportsRefraction) {
                        LiquidToggle(
                            selected = { enabled },
                            onSelect = { enabled = engine.setEnabled(it) },
                            backdrop = sliderBackdrop,
                            accentColor = skin.accent,
                            trackColor = skin.textSecondary.copy(alpha = 0.35f),
                        )
                    } else {
                        GlassSwitch(
                            checked = enabled,
                            onCheckedChange = { enabled = engine.setEnabled(it) },
                            checkedTrackColor = skin.accent,
                            uncheckedTrackColor = skin.textSecondary.copy(alpha = 0.35f),
                            rimColor = Color.White,
                        )
                    }
                }

                Text("الإعدادات المسبقة", color = skin.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp),
                ) {
                    itemsIndexed(presets) { index, name ->
                        GlowChip(
                            label = name.ifBlank { "Preset ${index}" },
                            selected = selectedPreset == index,
                            onClick = {
                                selectedPreset = index
                                if (index > 0 && engine.applyPreset(index - 1)) {
                                    refreshLevels(engine, levels)
                                    enabled = engine.setEnabled(true)
                                }
                            },
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
                            if (glassTier.supportsRefraction) Row(
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
                            val onLevelChange: (Float) -> Unit = { value ->
                                val next = value.roundToInt().coerceIn(range.first, range.last)
                                if (engine.setBandLevel(index, next)) {
                                    levels[index] = next
                                    selectedPreset = 0
                                    if (!enabled) enabled = engine.setEnabled(true)
                                }
                            }
                            if (glassTier.supportsRefraction) {
                                LiquidSlider(
                                    value = { levels[index].toFloat() },
                                    onValueChange = onLevelChange,
                                    valueRange = range.first.toFloat()..range.last.toFloat(),
                                    visibilityThreshold = (range.last - range.first) * 0.002f,
                                    backdrop = sliderBackdrop,
                                    accentColor = skin.accent,
                                    trackColor = skin.textSecondary.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                )
                            } else {
                                GlowGlassSlider(
                                    label = label,
                                    valueText = formatDb(level),
                                    value = (level - range.first).toFloat() / (range.last - range.first).coerceAtLeast(1),
                                    onValueChange = { f -> onLevelChange(range.first + f * (range.last - range.first)) },
                                )
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    com.glow.app.ui.common.GlowTextButton(onClick = {
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
