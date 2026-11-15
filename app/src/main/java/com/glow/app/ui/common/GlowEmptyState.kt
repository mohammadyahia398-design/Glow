package com.glow.app.ui.common

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.ui.theme.LocalGlowSkin

private val SceneW = 220.dp
private val SceneH = 190.dp
private val GlassY = 100.dp
private val GlassW = 196.dp
private val GlassH = 84.dp

private val Cyan1 = Color(0xFF67E8F9)
private val Cyan2 = Color(0xFF06B6D4)
private val Cyan3 = Color(0xFF0891B2)
private val Indigo = Color(0xFF4F46E5)
private val Periwinkle = Color(0xFF93A8FF)
private val Ink = Color(0xFF07141C)

/**
 * حالة فراغ بلغة «الصلب أمام الزجاج»: مجسّم أبيض صلب فيه رمز، خلفه كتلة سماوية ساطعة،
 * وأمامهما لوح زجاجي يطمس ما خلفه تدريجياً (أقوى في الأسفل) بصبغة نيلية.
 * يطفو المشهد ببطء، ويعرض عنواناً وجملة وزراً أبيض سميكاً اختيارياً.
 *
 * على أندرويد أقدم من 12 لا يتوفر تمويه حي، فيُعوَّض بصبغة أكثف على اللوح.
 */
@Composable
fun GlowEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: IconKind = IconKind.Note,
    badge: IconKind? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val skin = LocalGlowSkin.current
    val transition = rememberInfiniteTransition(label = "emptyFloat")
    val float by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "emptyFloatValue",
    )
    val solid = if (skin.isDark) Color(0xFFF8FAFF) else Color(0xFF0B1620)
    val onSolid = if (skin.isDark) Ink else Color.White

    Column(
        modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // المشهد يُرسم دائماً من اليسار لليمين حتى تتطابق النسختان الحادة والمموّهة في أي لغة.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                Modifier
                    .size(SceneW, SceneH)
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Cyan2.copy(alpha = 0.30f), Color.Transparent),
                                center = center,
                                radius = size.width * 0.9f,
                            ),
                            radius = size.width * 0.9f,
                        )
                    }
                    .graphicsLayer {
                        rotationZ = -6f
                        translationY = float * 5.dp.toPx()
                    },
            ) {
                EmptyScene(icon)
                GlassPane(icon)
                if (badge != null) {
                    Box(
                        Modifier
                            .offset(x = 80.dp, y = 158.dp)
                            .size(46.dp)
                            .shadow(10.dp, CircleShape, ambientColor = Indigo, spotColor = Indigo)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        GlowIcon(badge, Ink, 24.dp)
                    }
                }
            }
        }
        Spacer(Modifier.height(if (badge != null) 36.dp else 28.dp))
        Text(
            title,
            color = skin.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            message,
            color = skin.textSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (actionLabel != null && onAction != null) {
            Box(
                Modifier
                    .padding(top = 24.dp)
                    .height(52.dp)
                    .widthIn(min = 160.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(solid)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel, color = onSolid, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** المجسّم الأبيض الصلب وخلفه الكتلة السماوية التي تبدو كامتداد ثلاثي الأبعاد. */
@Composable
private fun EmptyScene(icon: IconKind) {
    Box(Modifier.size(SceneW, SceneH)) {
        Box(
            Modifier
                .offset(x = 40.dp, y = 18.dp)
                .size(150.dp)
                .background(
                    Brush.linearGradient(listOf(Cyan1, Cyan2, Cyan3)),
                    RoundedCornerShape(40.dp),
                ),
        )
        Box(
            Modifier
                .offset(x = 20.dp, y = 0.dp)
                .size(150.dp)
                .shadow(14.dp, RoundedCornerShape(40.dp), ambientColor = Cyan2, spotColor = Cyan2)
                .background(Color(0xFFF8FAFF), RoundedCornerShape(40.dp)),
            contentAlignment = Alignment.TopCenter,
        ) {
            GlowIcon(icon, Ink, 78.dp, Modifier.padding(top = 22.dp))
        }
    }
}

/**
 * اللوح الزجاجي: نسخة ثانية من المشهد تُمَوَّه وتظهر بقناع متدرج (لا شيء في الأعلى، كاملة في الأسفل)،
 * فوقها صبغة نيلية متدرجة ولمعة علوية وحافة رفيعة.
 */
@Composable
private fun GlassPane(icon: IconKind) {
    val shape = RoundedCornerShape(28.dp)
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(
        Modifier
            .offset(x = 0.dp, y = GlassY)
            .size(GlassW, GlassH)
            .clip(shape),
    ) {
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset(x = 0.dp, y = -GlassY)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val start = GlassY.toPx() / size.height
                    drawRect(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                start to Color.Transparent,
                                1f to Color.Black,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
                .then(if (canBlur) Modifier.blur(16.dp) else Modifier),
        ) {
            EmptyScene(icon)
        }
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.35f to Periwinkle.copy(alpha = if (canBlur) 0.10f else 0.35f),
                                1f to Indigo.copy(alpha = if (canBlur) 0.88f else 0.95f),
                            ),
                        ),
                    )
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                            endY = size.height * 0.3f,
                        ),
                    )
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent),
                        ),
                        size = Size(size.width, 1.5.dp.toPx()),
                    )
                }
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.08f))),
                    shape,
                ),
        )
    }
}
