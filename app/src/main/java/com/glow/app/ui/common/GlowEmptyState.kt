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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.ui.theme.LocalGlowSkin

// المشهد مصغّر إلى نحو ثلثي حجمه السابق (كان 220×190).
private val SceneW = 150.dp
private val SceneH = 130.dp
private val GlassY = 68.dp
private val GlassW = 133.dp
private val GlassH = 57.dp

private val Magenta = Color(0xFFF042FF)
private val Violet = Color(0xFF7226FF)
private val Lilac = Color(0xFFB07CFF)
private val Periwinkle = Color(0xFFC9B2FF)
private val Blush = Color(0xFFFFE5F1)
private val Ink = Color(0xFF010030)
private val Pale = Color(0xFFE4D4FF)

/**
 * حالة فراغ بلغة «الصلب أمام الزجاج»: بطاقة فيها رمز، خلفها كتلة ملوّنة،
 * وأمامهما لوح زجاجي يطمس ما خلفه تدريجياً (أقوى في الأسفل). يطفو المشهد ببطء.
 *
 * الداكن: بطاقة لافندر باهتة ولوح نيلي. الفاتح: بطاقة بيضاء بظل ناعم وأيقونة بنفسجية ولوح أخف.
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
    val dark = skin.isDark
    val transition = rememberInfiniteTransition(label = "emptyFloat")
    val float by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "emptyFloatValue",
    )
    val solid = if (dark) Pale else Color(0xFF0E0060)
    val onSolid = if (dark) Ink else Color.White

    Column(
        modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 64.dp),
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
                                listOf(Magenta.copy(alpha = if (dark) 0.26f else 0.16f), Color.Transparent),
                                center = center,
                                radius = size.width * 0.85f,
                            ),
                            radius = size.width * 0.85f,
                        )
                    }
                    .graphicsLayer {
                        rotationZ = -6f
                        translationY = float * 4.dp.toPx()
                    },
            ) {
                EmptyScene(icon, dark)
                GlassPane(icon, dark)
                if (badge != null) {
                    Box(
                        Modifier
                            .offset(x = 54.dp, y = 108.dp)
                            .size(32.dp)
                            .shadow(8.dp, CircleShape, ambientColor = Violet, spotColor = Violet)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        GlowIcon(badge, if (dark) Ink else Violet, 18.dp)
                    }
                }
            }
        }
        Spacer(Modifier.height(if (badge != null) 26.dp else 20.dp))
        Text(
            title,
            color = skin.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            message,
            color = skin.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        if (actionLabel != null && onAction != null) {
            Box(
                Modifier
                    .padding(top = 18.dp)
                    .height(48.dp)
                    .widthIn(min = 150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(solid)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel, color = onSolid, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** البطاقة الصلبة وخلفها الكتلة الملوّنة التي تبدو كامتداد ثلاثي الأبعاد. */
@Composable
private fun EmptyScene(icon: IconKind, dark: Boolean) {
    val back = if (dark) {
        Brush.linearGradient(listOf(Lilac, Magenta, Violet))
    } else {
        Brush.linearGradient(listOf(Blush, Magenta.copy(alpha = 0.55f), Violet.copy(alpha = 0.65f)))
    }
    val cardShadow = if (dark) Magenta else Violet.copy(alpha = 0.45f)
    Box(Modifier.size(SceneW, SceneH)) {
        Box(
            Modifier
                .offset(x = 27.dp, y = 12.dp)
                .size(102.dp)
                .background(back, RoundedCornerShape(28.dp)),
        )
        Box(
            Modifier
                .offset(x = 14.dp, y = 0.dp)
                .size(102.dp)
                .shadow(if (dark) 10.dp else 14.dp, RoundedCornerShape(28.dp), ambientColor = cardShadow, spotColor = cardShadow)
                .background(if (dark) Pale else Color.White, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.TopCenter,
        ) {
            GlowIcon(icon, if (dark) Ink else Violet, 52.dp, Modifier.padding(top = 15.dp))
        }
    }
}

/**
 * اللوح الزجاجي: نسخة ثانية من المشهد تُمَوَّه وتظهر بقناع متدرج (لا شيء في الأعلى، كاملة في الأسفل)،
 * فوقها صبغة متدرجة ولمعة علوية وحافة رفيعة. في الفاتح أخف بكثير حتى لا يبدو ثقيلاً.
 */
@Composable
private fun GlassPane(icon: IconKind, dark: Boolean) {
    val shape = RoundedCornerShape(20.dp)
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val bottomTint = when {
        dark -> Violet.copy(alpha = if (canBlur) 0.80f else 0.92f)
        else -> Violet.copy(alpha = if (canBlur) 0.40f else 0.60f)
    }
    val midTint = if (dark) Periwinkle.copy(alpha = if (canBlur) 0.10f else 0.35f)
    else Color.White.copy(alpha = if (canBlur) 0.22f else 0.45f)
    val rimEnd = if (dark) Color.White.copy(alpha = 0.08f) else Violet.copy(alpha = 0.14f)
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
                .then(if (canBlur) Modifier.blur(11.dp) else Modifier),
        ) {
            EmptyScene(icon, dark)
        }
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.35f to midTint,
                                1f to bottomTint,
                            ),
                        ),
                    )
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = if (dark) 0.26f else 0.45f), Color.Transparent),
                            endY = size.height * 0.3f,
                        ),
                    )
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent),
                        ),
                        size = Size(size.width, 1.2.dp.toPx()),
                    )
                }
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.65f), rimEnd)),
                    shape,
                ),
        )
    }
}
