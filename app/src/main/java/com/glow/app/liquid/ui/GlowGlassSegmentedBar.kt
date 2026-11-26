package com.glow.app.liquid.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.liquid.backdrop.Backdrop
import com.glow.app.ui.theme.LocalGlowSkin

/**
 * شريط خيارات زجاجي سائل بنص فقط، يعتمد المكوّن نفسه الذي يعتمده الشريط السفلي
 * [LiquidBottomTabs]: عدسة تنزلق بالنقر وبالسحب وتنكسر عند اللمس.
 * يعمل في RTL بعكس الترتيب يدوياً ليظهر الخيار الأول على اليمين.
 */
@Composable
fun GlowGlassSegmentedBar(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return
    val skin = LocalGlowSkin.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val order = remember(labels.size, rtl) {
        labels.indices.toList().let { if (rtl) it.asReversed() else it }
    }
    val visualSelected = order.indexOf(selectedIndex).coerceAtLeast(0)
    val idle = GlowBarStyle.idle(skin)
    val accent = skin.accentText

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        LiquidBottomTabs(
            selectedTabIndex = visualSelected,
            onTabSelected = { v -> order.getOrNull(v)?.let(onSelect) },
            backdrop = backdrop,
            tabsCount = labels.size,
            containerColor = GlowBarStyle.container(skin),
            thumbColor = GlowBarStyle.thumb(skin),
            modifier = modifier.fillMaxWidth().height(54.dp),
            selectedContent = { Segments(order, labels, accent, onSelect) },
            content = { Segments(order, labels, idle, onSelect) },
        )
    }
}

@Composable
private fun RowScope.Segments(
    order: List<Int>,
    labels: List<String>,
    tint: Color,
    onSelect: (Int) -> Unit,
) {
    order.forEach { i ->
        LiquidBottomTab(onClick = { onSelect(i) }) {
            Text(
                text = labels[i],
                color = tint,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
