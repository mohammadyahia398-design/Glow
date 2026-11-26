package com.glow.app.liquid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.glow.app.ui.glass.components.LiquidBarItem
import com.glow.app.ui.theme.LocalGlowSkin

/**
 * شريط Glow السفلي بزجاج Liquid Glass (أندرويد 13+).
 * يستعمل عناصر [LiquidBarItem] نفسها التي يستعملها الشريط القديم.
 * الحساب الداخلي بترتيب LTR، ويُعكس الترتيب يدوياً في RTL ليظهر الأول على اليمين.
 */
@Composable
fun GlowLiquidBottomBar(
    items: List<LiquidBarItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val skin = LocalGlowSkin.current
    val systemRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val ordered = remember(items, systemRtl) { if (systemRtl) items.asReversed() else items }
    val selectedIndex = ordered.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)

    val idle = GlowBarStyle.idle(skin)
    val accent = skin.accentText
    val container = GlowBarStyle.container(skin)
    val thumb = GlowBarStyle.thumb(skin)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        LiquidBottomTabs(
            selectedTabIndex = selectedIndex,
            onTabSelected = { index -> ordered.getOrNull(index)?.let { onSelect(it.id) } },
            backdrop = backdrop,
            tabsCount = ordered.size,
            containerColor = container,
            thumbColor = thumb,
            modifier = modifier,
            selectedContent = { Tabs(ordered, accent, onSelect) },
            content = { Tabs(ordered, idle, onSelect) },
        )
    }
}

@Composable
private fun RowScope.Tabs(
    items: List<LiquidBarItem>,
    tint: Color,
    onSelect: (String) -> Unit,
) {
    items.forEach { item ->
        LiquidBottomTab(onClick = { onSelect(item.id) }) {
            Box(Modifier.size(26.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                item.icon(tint)
            }
            Text(
                text = item.label,
                color = tint,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
