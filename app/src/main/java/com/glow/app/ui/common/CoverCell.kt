package com.glow.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.media.Track
import com.glow.app.ui.theme.LocalGlowSkin
import com.glow.app.ui.theme.GlowShapes

/**
 * خلية شبكة: غلاف مربع بزوايا مدوّرة، وتحته عنوان وسطر ثانوي (اختياريان).
 * إن وُجد [overlay] يُكتب اسمه فوق الغلاف بخط أبيض (يُستخدم للفنانين).
 */
@Composable
fun CoverCell(
    art: Track,
    title: String?,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: String? = null,
) {
    val skin = LocalGlowSkin.current
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            AlbumArt(art, Modifier.fillMaxSize(), radius = 28.dp, px = 400)
            // طبقة زجاجية: صبغة متدرجة من الأسفل، حافة علوية رفيعة لامعة، وإطار ناعم
            val glassShape = GlowShapes.Artwork
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(glassShape)
                    .drawBehind {
                        drawRect(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.55f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.38f),
                                ),
                            ),
                        )
                        drawRect(
                            Brush.verticalGradient(
                                colors = listOf(skin.accent.copy(alpha = 0f), skin.accent.copy(alpha = 0.26f)),
                                startY = size.height * 0.5f,
                                endY = size.height,
                            ),
                        )
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                                endY = size.height * 0.25f,
                            ),
                        )
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.8f), Color.Transparent),
                            ),
                            topLeft = Offset(size.width * 0.12f, 0f),
                            size = Size(size.width * 0.76f, 1.5.dp.toPx()),
                        )
                    }
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.04f))),
                        glassShape,
                    ),
            )
            if (overlay != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(GlowShapes.Artwork)
                        .background(Color.Black.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = overlay,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
        if (title != null) {
            Text(
                text = title,
                color = skin.text,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = skin.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
    }
}
