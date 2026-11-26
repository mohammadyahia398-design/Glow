package com.glow.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.ui.theme.LocalGlowSkin
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.glow.app.GlowStrings
import com.glow.app.R

/**
 * شريط تمرير سريع للقوائم الطويلة: مقبض يظهر أثناء التمرير ويختفي بعد لحظة؛ اسحبه للقفز في القائمة،
 * وتظهر فقاعة بالحرف (أو الرقم) عند الموضع. يُرسم فوق القائمة، فلا يحتاج مساحة منها.
 * [label] يرجع النص المعروض في الفقاعة لفهرس عنصر.
 */
@Composable
fun GlowFastScroll(
    state: LazyListState,
    modifier: Modifier = Modifier,
    minItems: Int = 40,
    label: (Int) -> String,
) {
    val skin = LocalGlowSkin.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var visible by remember { mutableStateOf(false) }
    val scrolling = state.isScrollInProgress
    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) {
            visible = true
        } else {
            delay(1200)
            visible = false
        }
    }
    val shownAlpha by animateFloatAsState(if (visible || dragging) 1f else 0f, label = "fastScrollAlpha")
    val total by remember { derivedStateOf { state.layoutInfo.totalItemsCount } }
    val listFraction by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val denom = (info.totalItemsCount - info.visibleItemsInfo.size).coerceAtLeast(1)
            (state.firstVisibleItemIndex.toFloat() / denom).coerceIn(0f, 1f)
        }
    }
    if (total < minItems) return

    val thumbHeight = 56.dp
    BoxWithConstraints(modifier) {
        // بلا لمس عندما يكون المقبض مخفياً، حتى لا يعترض ضغط الصفوف عند الحافة.
        if (shownAlpha > 0.05f) {
            val trackPx = with(density) { maxHeight.toPx() }
            val thumbPx = with(density) { thumbHeight.toPx() }
            val fraction = if (dragging) dragFraction else listFraction
            val offsetY = (fraction * (trackPx - thumbPx).coerceAtLeast(0f)).roundToInt()

            if (dragging) {
                val index = ((total - 1) * fraction).roundToInt().coerceIn(0, total - 1)
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(-40.dp.roundToPx(), offsetY) }
                        .height(thumbHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(skin.accent)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            label(index),
                            color = skin.onAccent,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, offsetY) }
                    .size(width = 28.dp, height = thumbHeight)
                    .graphicsLayer { this.alpha = shownAlpha }
                    .semantics { contentDescription = GlowStrings.get(R.string.fast_scroll) }
                    .pointerInput(trackPx) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragging = true
                                dragFraction = listFraction
                            },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                            onVerticalDrag = { change, dy ->
                                change.consume()
                                val range = (trackPx - thumbPx).coerceAtLeast(1f)
                                dragFraction = (dragFraction + dy / range).coerceIn(0f, 1f)
                                val target = ((total - 1) * dragFraction).roundToInt().coerceIn(0, total - 1)
                                scope.launch { state.scrollToItem(target) }
                            },
                        )
                    },
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 3.dp)
                        .size(width = 5.dp, height = 44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(skin.accent.copy(alpha = 0.9f)),
                )
            }
        }
    }
}
