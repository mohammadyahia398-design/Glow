package com.glow.app.ui.glass.components

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.glow.app.ui.glass.core.LocalGlassConfig
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * أبعاد المفتاح الزجاجي، مأخوذة بنسبها من مرجع iOS 27 Beta 2:
 * المسار كبسولة رفيعة، والعدسة أعلى منه وتبرز عن طرفيه، ومركزها شفاف مسطّح
 * وانكسارها في حافة ضيقة فقط.
 */
object GlassSwitchSpec {
    val TrackWidth = 44.dp
    val TrackHeight = 18.dp
    val ThumbWidth = 40.dp
    val ThumbHeight = 28.dp

    /** مقدار بروز العدسة عن طرفي المسار. العرض الكلي = المسار + ضعف البروز. */
    val Overhang = 10.dp

    /** فراغ رأسي حول العدسة يتّسع للظل ولنموّها عند اللمس. */
    val VerticalPad = 6.dp

    /** عرض المحتوى الحقيقي الذي تلتقطه العدسة خارج حدّها لتنكسر به حافتها. */
    val Bleed = 12.dp

    /**
     * العدسة وهي ساكنة شبه مسطّحة: حافة ضيقة، وسماكة صغيرة، فانكسار خفيف جداً.
     * عند الضغط أو السحب «ترتفع»: تتسع الحافة وتزداد السماكة فيقوى الانكسار.
     */
    val BezelRest = 5.dp
    val BezelLift = 9.dp
    val ThicknessRest = 3.dp
    val ThicknessLift = 14.dp

    /** نموّ العدسة عند الارتفاع (لكل جهة). */
    val LiftGrowX = 3.dp
    val LiftGrowY = 2.dp

    /** الظل: قريب وناعم ساكناً، وأبعد وأوسع عند الارتفاع. */
    val ShadowDyRest = 1.dp
    val ShadowDyLift = 4.5.dp
    val ShadowDxLift = 1.5.dp
    val ShadowStepRest = 0.6.dp
    val ShadowStepLift = 1.7.dp

    const val RefractiveIndex = 1.5f
    const val DispersionRest = 0.10f
    const val DispersionLift = 0.40f
}

private fun mixF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * مفتاح تبديل زجاجي: مسار ملوّن وعدسة زجاجية تنزلق عليه وتنكسر عند حافتها الصورة التي تحتها.
 *
 * - [onCheckedChange] = null: المفتاح للعرض فقط (الصف الأب يستقبل اللمس)، كما كان `Switch` في الإعدادات.
 * - يدعم النقر والسحب، ويتبع اتجاه اللغة (في RTL تكون «التشغيل» جهة اليسار).
 * - مع «تقليل الحركة» تنتقل العدسة فوراً.
 * - قبل Android 13 لا يتوفر الانكسار، فتُرسم العدسة شفافة بحافة لامعة.
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedTrackColor: Color = Color(0xFF34C759),
    uncheckedTrackColor: Color = Color(0xFF8E8E93).copy(alpha = 0.45f),
    rimColor: Color = Color.White,
) {
    val s = GlassSwitchSpec
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reduceMotion = LocalGlassConfig.current.reduceMotion

    val trackWpx = with(density) { s.TrackWidth.toPx() }
    val trackHpx = with(density) { s.TrackHeight.toPx() }
    val thumbWpx = with(density) { s.ThumbWidth.toPx() }
    val thumbHpx = with(density) { s.ThumbHeight.toPx() }
    val overhangPx = with(density) { s.Overhang.toPx() }
    val padPx = with(density) { s.VerticalPad.toPx() }
    val bleedPx = with(density) { s.Bleed.toPx() }
    val bezelRestPx = with(density) { s.BezelRest.toPx() }
    val bezelLiftPx = with(density) { s.BezelLift.toPx() }
    val thickRestPx = with(density) { s.ThicknessRest.toPx() }
    val thickLiftPx = with(density) { s.ThicknessLift.toPx() }
    val growXPx = with(density) { s.LiftGrowX.toPx() }
    val growYPx = with(density) { s.LiftGrowY.toPx() }
    val rimPx = with(density) { 1.dp.toPx() }
    val innerRimInsetPx = with(density) { 2.dp.toPx() }
    val shadowDyRest = with(density) { s.ShadowDyRest.toPx() }
    val shadowDyLift = with(density) { s.ShadowDyLift.toPx() }
    val shadowDxLift = with(density) { s.ShadowDxLift.toPx() }
    val shadowStepRest = with(density) { s.ShadowStepRest.toPx() }
    val shadowStepLift = with(density) { s.ShadowStepLift.toPx() }

    val totalW = s.TrackWidth + s.Overhang * 2
    val totalH = s.ThumbHeight + s.VerticalPad * 2
    val totalHpx = thumbHpx + padPx * 2
    val travelPx = trackWpx + overhangPx * 2 - thumbWpx
    val trackTopPx = (totalHpx - trackHpx) / 2f

    val settle: AnimationSpec<Float> =
        if (reduceMotion) snap() else spring(dampingRatio = 0.62f, stiffness = 420f)
    val pressSpec: AnimationSpec<Float> =
        if (reduceMotion) snap() else spring(dampingRatio = 0.60f, stiffness = 300f)

    val interactive = enabled && onCheckedChange != null
    val currentChecked by rememberUpdatedState(checked)
    val currentCallback by rememberUpdatedState(onCheckedChange)
    val scope = rememberCoroutineScope()

    val progress = remember { Animatable(if (checked) 1f else 0f) }
    var dragging by remember { mutableStateOf(false) }
    var tapping by remember { mutableStateOf(false) }

    LaunchedEffect(checked) {
        if (!dragging) progress.animateTo(if (checked) 1f else 0f, settle)
    }

    val pressTarget = if (dragging || tapping) 1f else 0f
    val press by animateFloatAsState(pressTarget, pressSpec, label = "glassSwitchLift")

    val effectHolder = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) SwitchLensHolder() else null
    }

    fun thumbLeft(): Float {
        val f = if (rtl) 1f - progress.value else progress.value
        return f * travelPx
    }

    fun trackColor(): Color =
        lerp(uncheckedTrackColor, checkedTrackColor, progress.value.coerceIn(0f, 1f))

    val gestures = if (interactive) {
        Modifier
            .pointerInput(travelPx) {
                detectTapGestures(
                    onPress = {
                        tapping = true
                        tryAwaitRelease()
                        tapping = false
                    },
                    onTap = { currentCallback?.invoke(!currentChecked) },
                )
            }
            .pointerInput(travelPx, rtl) {
                fun finish() {
                    val target = progress.value > 0.5f
                    dragging = false
                    scope.launch { progress.animateTo(if (target) 1f else 0f, settle) }
                    if (target != currentChecked) currentCallback?.invoke(target)
                }
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { finish() },
                    onDragCancel = { finish() },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        val d = dx / travelPx * (if (rtl) -1f else 1f)
                        scope.launch { progress.snapTo((progress.value + d).coerceIn(0f, 1f)) }
                    },
                )
            }
    } else {
        Modifier
    }

    val a11y = if (interactive) {
        Modifier.semantics {
            role = Role.Switch
            toggleableState = ToggleableState(checked)
            onClick {
                currentCallback?.invoke(!currentChecked)
                true
            }
        }
    } else {
        Modifier
    }

    val layerW = s.ThumbWidth + s.Bleed * 2
    val layerH = s.ThumbHeight + s.Bleed * 2

    Box(
        modifier = modifier
            .size(totalW, totalH)
            .alpha(if (enabled) 1f else 0.4f)
            .then(gestures)
            .then(a11y),
    ) {
        // المسار وظل العدسة.
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    drawRoundRect(
                        color = trackColor(),
                        topLeft = Offset(overhangPx, trackTopPx),
                        size = Size(trackWpx, trackHpx),
                        cornerRadius = CornerRadius(trackHpx / 2f),
                    )
                    val lift = press
                    val lc = lift.coerceIn(0f, 1f)
                    val gx = growXPx * lift
                    val gy = growYPx * lift
                    val left = thumbLeft() - gx
                    val top = padPx - gy
                    val w = thumbWpx + 2f * gx
                    val h = thumbHpx + 2f * gy
                    val thumbPath = Path().apply {
                        addRoundRect(RoundRect(left, top, left + w, top + h, CornerRadius(h / 2f)))
                    }
                    val dy = mixF(shadowDyRest, shadowDyLift, lift)
                    val dx = shadowDxLift * lc
                    val step = mixF(shadowStepRest, shadowStepLift, lift)
                    val a = mixF(0.025f, 0.05f, lc)
                    // الظل خارج العدسة فقط، فيبقى داخلها شفافاً.
                    clipPath(thumbPath, ClipOp.Difference) {
                        for (i in 1..6) {
                            val e = i * step
                            drawRoundRect(
                                color = Color.Black.copy(alpha = a),
                                topLeft = Offset(left + dx - e, top + dy - e),
                                size = Size(w + 2f * e, h + 2f * e),
                                cornerRadius = CornerRadius(h / 2f + e),
                            )
                        }
                    }
                },
        )

        // العدسة: تعيد رسم المسار داخل طبقتها ثم يُطبَّق عليها الانكسار.
        Box(
            Modifier
                .absoluteOffset {
                    IntOffset((thumbLeft() - bleedPx).roundToInt(), (padPx - bleedPx).roundToInt())
                }
                .size(layerW, layerH)
                .graphicsLayer {
                    val p = press
                    val pc = p.coerceIn(0f, 1f)
                    val hw = thumbWpx / 2f + growXPx * p
                    val hh = thumbHpx / 2f + growYPx * p
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    this.shape = SwitchLensClip(cx - hw, cy - hh, cx + hw, cy + hh, hh)
                    this.clip = true
                    this.renderEffect =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && effectHolder != null) {
                            effectHolder.effect(
                                layerW = size.width,
                                layerH = size.height,
                                halfW = hw,
                                halfH = hh,
                                corner = hh,
                                bezel = mixF(bezelRestPx, bezelLiftPx, p).coerceAtLeast(1f),
                                thick = mixF(thickRestPx, thickLiftPx, p).coerceAtLeast(0f),
                                index = s.RefractiveIndex,
                                dispersion = mixF(s.DispersionRest, s.DispersionLift, pc),
                            )
                        } else {
                            null
                        }
                }
                .drawBehind {
                    val layerLeft = thumbLeft() - bleedPx
                    val layerTop = padPx - bleedPx
                    drawRoundRect(
                        color = trackColor(),
                        topLeft = Offset(overhangPx - layerLeft, trackTopPx - layerTop),
                        size = Size(trackWpx, trackHpx),
                        cornerRadius = CornerRadius(trackHpx / 2f),
                    )
                },
        )

        // جسم العدسة وحافتها اللامعة: فوق المحتوى المنكسر فتبقى حادّة.
        Box(
            Modifier
                .absoluteOffset {
                    IntOffset((thumbLeft() - bleedPx).roundToInt(), (padPx - bleedPx).roundToInt())
                }
                .size(layerW, layerH)
                .drawBehind {
                    val p = press
                    val lc = p.coerceIn(0f, 1f)
                    val hw = thumbWpx / 2f + growXPx * p
                    val hh = thumbHpx / 2f + growYPx * p
                    val tl = Offset(size.width / 2f - hw, size.height / 2f - hh)
                    val sz = Size(hw * 2f, hh * 2f)
                    val corner = CornerRadius(hh)

                    // جسم الزجاج: خفيف جداً ساكناً، وأوضح عند الارتفاع.
                    drawRoundRect(
                        color = rimColor.copy(alpha = mixF(0.05f, 0.11f, lc)),
                        topLeft = tl,
                        size = sz,
                        cornerRadius = corner,
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                rimColor.copy(alpha = mixF(0.10f, 0.24f, lc)),
                                Color.Transparent,
                            ),
                            startY = tl.y,
                            endY = tl.y + hh,
                        ),
                        topLeft = tl,
                        size = sz,
                        cornerRadius = corner,
                    )

                    // الحافة الخارجية: رفيعة ولامعة من الأعلى، وتقوى عند الارتفاع.
                    val k = mixF(0.55f, 1f, lc)
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                rimColor.copy(alpha = 0.95f * k),
                                rimColor.copy(alpha = 0.30f * k),
                                rimColor.copy(alpha = 0.60f * k),
                            ),
                            start = tl,
                            end = Offset(tl.x + sz.width, tl.y + sz.height),
                        ),
                        topLeft = Offset(tl.x + rimPx / 2f, tl.y + rimPx / 2f),
                        size = Size(sz.width - rimPx, sz.height - rimPx),
                        cornerRadius = CornerRadius(hh - rimPx / 2f),
                        style = Stroke(width = rimPx),
                    )

                    // عند الارتفاع فقط: الحافة الداخلية (جدار الزجاج) واللمعة الهلالية.
                    if (lc > 0.01f) {
                        val inset = innerRimInsetPx
                        val inTl = Offset(tl.x + inset, tl.y + inset)
                        val inSz = Size(sz.width - 2f * inset, sz.height - 2f * inset)
                        val inCorner = CornerRadius((hh - inset).coerceAtLeast(0f))
                        val fade = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                rimColor.copy(alpha = 0.70f * lc),
                                Color.Transparent,
                            ),
                            startX = tl.x,
                            endX = tl.x + sz.width,
                        )
                        // هلال علوي (أوضح) .
                        clipRect(
                            left = tl.x,
                            top = tl.y,
                            right = tl.x + sz.width,
                            bottom = tl.y + hh * 0.9f,
                        ) {
                            drawRoundRect(
                                brush = fade,
                                topLeft = inTl,
                                size = inSz,
                                cornerRadius = inCorner,
                                style = Stroke(width = rimPx * 1.6f),
                            )
                        }
                        // خط سفلي رفيع أخف.
                        clipRect(
                            left = tl.x,
                            top = tl.y + hh * 1.15f,
                            right = tl.x + sz.width,
                            bottom = tl.y + sz.height,
                        ) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        rimColor.copy(alpha = 0.38f * lc),
                                        Color.Transparent,
                                    ),
                                    startX = tl.x,
                                    endX = tl.x + sz.width,
                                ),
                                topLeft = Offset(tl.x + inset * 0.6f, tl.y + inset * 0.6f),
                                size = Size(sz.width - 1.2f * inset, sz.height - 1.2f * inset),
                                cornerRadius = CornerRadius((hh - inset * 0.6f).coerceAtLeast(0f)),
                                style = Stroke(width = rimPx),
                            )
                        }
                    }
                },
        )
    }
}

/** مستطيل مدوّر بإحداثيات صريحة: حدّ العدسة المرئي داخل طبقتها الأكبر. */
private class SwitchLensClip(
    private val left: Float,
    private val top: Float,
    private val right: Float,
    private val bottom: Float,
    private val corner: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = corner.coerceAtLeast(0f)
        return Outline.Rounded(RoundRect(left, top, right, bottom, CornerRadius(r, r)))
    }
}

/** يبني تأثير الانكسار (API 33+) ويحتفظ بالشيدر ويحدّث قيمه، فلا يُنشأ شيدر جديد كل إطار. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class SwitchLensHolder {
    private val shader = RuntimeShader(SwitchLensShader.SOURCE)

    fun effect(
        layerW: Float,
        layerH: Float,
        halfW: Float,
        halfH: Float,
        corner: Float,
        bezel: Float,
        thick: Float,
        index: Float,
        dispersion: Float,
    ): RenderEffect {
        shader.setFloatUniform("uSize", layerW, layerH)
        shader.setFloatUniform("uHalf", halfW, halfH)
        shader.setFloatUniform("uCorner", corner)
        shader.setFloatUniform("uBezel", bezel)
        shader.setFloatUniform("uThick", thick)
        shader.setFloatUniform("uIndex", index)
        shader.setFloatUniform("uDisp", dispersion)
        return AndroidRenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }
}

/**
 * شيدر AGSL لعدسة مفتاح iOS 27: المركز مسطّح بلا تكبير، والانكسار في حافة ضيقة فقط
 * بقانون سنيل على منحنى سطح محدّب، وهالة نيلية خفيفة عند الحافة، ولا حلقة منعكسة.
 */
private object SwitchLensShader {
    const val SOURCE: String = """
uniform shader content;
uniform float2 uSize;
uniform float2 uHalf;
uniform float uCorner;
uniform float uBezel;
uniform float uThick;
uniform float uIndex;
uniform float uDisp;

float lensSdf(float2 p) {
    float2 q = abs(p) - uHalf + float2(uCorner);
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - uCorner;
}

float2 clampCoord(float2 c) {
    return clamp(c, float2(0.5), uSize - float2(0.5));
}

half4 main(float2 fragCoord) {
    float2 p = fragCoord - uSize * 0.5;
    float depth = -lensSdf(p);

    if (depth >= uBezel || depth < 0.0 || uThick <= 0.0) {
        return content.eval(fragCoord);
    }

    float dx = lensSdf(p + float2(1.0, 0.0)) - lensSdf(p - float2(1.0, 0.0));
    float dy = lensSdf(p + float2(0.0, 1.0)) - lensSdf(p - float2(0.0, 1.0));
    float2 grad = float2(dx, dy);
    float len = length(grad);
    float2 outward = len > 0.0001 ? grad / len : float2(0.0);

    float t = clamp(depth / uBezel, 0.0, 1.0);
    float u = 1.0 - t;
    float u3 = u * u * u;
    float inner = max(1.0 - u3 * u, 0.0001);
    float h = pow(inner, 0.25);
    float slope = uThick * (u3 * pow(inner, -0.75)) / uBezel;

    float thetaI = atan(slope);
    float thetaT = asin(clamp(sin(thetaI) / uIndex, -1.0, 1.0));
    float shiftLen = uThick * h * tan(thetaI - thetaT);
    float2 shift = outward * shiftLen;

    half4 cg = content.eval(clampCoord(fragCoord + shift));
    half4 cf = content.eval(clampCoord(fragCoord + shift * (1.0 + uDisp)));
    // هالة نيلية خفيفة عند الحافة، تظهر حيث يختلف المحتوى المنكسر.
    float fringe = clamp(distance(float3(cf.rgb), float3(cg.rgb)) * 2.0, 0.0, 1.0) * (1.0 - t);
    half4 col = cg;
    col.rgb = mix(col.rgb, half3(0.31, 0.27, 0.90) * col.a, half(fringe * 0.6));
    return col;
}
"""
}
