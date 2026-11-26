package com.glow.app.liquid.ui

import androidx.compose.ui.graphics.Color
import com.glow.app.ui.theme.GlowSkin

/**
 * ألوان الزجاج المشتركة بين الشريط السفلي وأي شريط زجاجي قابل للسحب (مثل شريط المظهر).
 * مصدر واحد كي لا يختلف الشكلان.
 */
internal object GlowBarStyle {
    fun container(skin: GlowSkin): Color =
        if (skin.isDark) Color(0xFF000002).copy(alpha = 0.40f)
        else Color(0xFFFEFDFF).copy(alpha = 0.40f)

    fun thumb(skin: GlowSkin): Color =
        if (skin.isDark) Color.White.copy(alpha = 0.10f)
        else Color.Black.copy(alpha = 0.10f)

    fun idle(skin: GlowSkin): Color =
        skin.text.copy(alpha = if (skin.isDark) 0.92f else 0.82f)
}
