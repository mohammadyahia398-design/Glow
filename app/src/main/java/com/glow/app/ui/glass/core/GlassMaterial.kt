package com.glow.app.ui.glass.core

/**
 * المواد الزجاجية الخمس لـ Glow. كل مادة تجمع نمطاً ([GlassStyle]) وعمقاً ([GlassDepth])
 * بدل أن يختارهما كل مكوّن على حدة، فيبقى المظهر متسقاً في كل الواجهة.
 *
 * | المادة | الاستخدام |
 * |---|---|
 * | [UltraThin] | تراكبات خفيفة، بلا ظل |
 * | [Standard] | بطاقات وأسطح عامة |
 * | [Dense] | أسطح قراءة: قوائم وحقول |
 * | [Elevated] | حوارات ولوحات فوق المحتوى |
 * | [Interactive] | أزرار وعناصر تُلمس |
 */
enum class GlassMaterial(val style: GlassStyle, val depth: GlassDepth) {
    UltraThin(GlassStyle.UltraThin, GlassDepth.Flush),
    Standard(GlassStyle.Regular, GlassDepth.Raised),
    Dense(GlassStyle.Dense, GlassDepth.Raised),
    Elevated(GlassStyle.Elevated, GlassDepth.Overlay),
    Interactive(GlassStyle.Interactive, GlassDepth.Floating),
}
