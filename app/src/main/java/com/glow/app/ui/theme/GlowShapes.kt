package com.glow.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** A restrained shape scale shared by cards, controls, sheets and artwork. */
object GlowShapes {
    val Pill = RoundedCornerShape(percent = 50)
    val Control = RoundedCornerShape(16.dp)
    val Card = RoundedCornerShape(22.dp)
    val Artwork = RoundedCornerShape(24.dp)
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val TopSheet = Sheet

    val Material = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )
}
