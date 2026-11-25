package com.glow.app.ui.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.liquid.backdrop.Backdrop
import com.glow.app.liquid.ui.LiquidCircleButton
import com.glow.app.liquid.ui.LiquidSlider
import com.glow.app.media.PlayerState
import com.glow.app.media.RepeatMode
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.GlowIconTap
import com.glow.app.ui.common.IconKind
import com.glow.app.ui.common.formatTime
import com.glow.app.ui.theme.LocalGlowSkin

/** شريط التقدم مع الزمن، وصف أزرار: خلط، سابق، تشغيل، تالٍ، تكرار. كلها بلون [contentColor]. */
@Composable
fun NowPlayingControls(
    state: PlayerState,
    contentColor: Color,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    liquidBackdrop: Backdrop? = null,
) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val value = drag ?: state.progress
    val dim = contentColor.copy(alpha = 0.75f)
    val accent = LocalGlowSkin.current.accent

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val finishSeek = {
            drag?.let { onSeek((it * state.durationMs).toLong()) }
            drag = null
        }
        if (liquidBackdrop != null) {
            // زجاج سائل (أندرويد 13+): المقبض ينتفخ وينكسر خلفه الغلاف. يبقى من اليسار لليمين كالقديم.
            val latestValue by rememberUpdatedState(if (value.isNaN()) 0f else value.coerceIn(0f, 1f))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LiquidSlider(
                    value = { latestValue },
                    onValueChange = { drag = it },
                    valueRange = 0f..1f,
                    visibilityThreshold = 0.001f,
                    backdrop = liquidBackdrop,
                    accentColor = accent,
                    trackColor = contentColor.copy(alpha = 0.30f),
                    onValueChangeFinished = { finishSeek() },
                    modifier = Modifier
                        .height(36.dp)
                        .semantics {
                            contentDescription = "التقدم"
                            progressBarRangeInfo = ProgressBarRangeInfo(latestValue, 0f..1f)
                            setProgress { target ->
                                onSeek((target.coerceIn(0f, 1f) * state.durationMs).toLong())
                                true
                            }
                        },
                )
            }
        } else {
            ArtSeekBar(
                value = value,
                onValueChange = { drag = it },
                onValueChangeFinished = { finishSeek() },
                color = contentColor,
                description = "التقدم",
                accent = accent,
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime((value * state.durationMs).toLong()), color = dim, fontSize = 13.sp)
            Text(formatTime(state.durationMs), color = dim, fontSize = 13.sp)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlowIconTap("خلط", onShuffle) {
                GlowIcon(IconKind.Shuffle, contentColor.copy(alpha = if (state.shuffle) 1f else 0.5f))
            }
            val playIcon = if (state.isPlaying) IconKind.Pause else IconKind.Play
            val playLabel = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل"
            if (liquidBackdrop != null) {
                val surface = Color.White.copy(alpha = 0.12f)
                LiquidCircleButton(onPrevious, liquidBackdrop, 56.dp, "السابق", surfaceColor = surface) {
                    GlowIcon(IconKind.Prev, contentColor, 32.dp)
                }
                LiquidCircleButton(onTogglePlay, liquidBackdrop, 76.dp, playLabel, surfaceColor = surface) {
                    GlowIcon(playIcon, contentColor, 44.dp)
                }
                LiquidCircleButton(onNext, liquidBackdrop, 56.dp, "التالي", surfaceColor = surface) {
                    GlowIcon(IconKind.Next, contentColor, 32.dp)
                }
            } else {
                GlowIconTap("السابق", onPrevious, size = 56.dp) {
                    GlowIcon(IconKind.Prev, contentColor, 32.dp)
                }
                GlowIconTap(
                    description = playLabel,
                    onClick = onTogglePlay,
                    size = 76.dp,
                ) {
                    GlowIcon(playIcon, contentColor, 52.dp)
                }
                GlowIconTap("التالي", onNext, size = 56.dp) {
                    GlowIcon(IconKind.Next, contentColor, 32.dp)
                }
            }
            GlowIconTap("تكرار", onRepeat) {
                GlowIcon(
                    if (state.repeat == RepeatMode.One) IconKind.RepeatOne else IconKind.Repeat,
                    contentColor.copy(alpha = if (state.repeat == RepeatMode.Off) 0.5f else 1f),
                )
            }
        }
    }
}
