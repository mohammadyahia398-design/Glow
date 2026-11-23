package com.glow.app.ui.glass.effects

/**
 * شيدر AGSL يحاكي انكسار الضوء عند حواف الزجاج (يتطلب API 33).
 *
 * الفكرة: بالقرب من الحافة تُقرأ البكسلات من موضع أعمق داخل الشكل، بإزاحة
 * تقوى عند الحافة وتتلاشى نحو الداخل، مع تشتّت لوني خفيف (إزاحة مختلفة قليلاً
 * لكل قناة لون). يبدو أثر ذلك كانحناء الصورة عند حافة عدسة.
 *
 * يفترض الشيدر شكلاً مستطيلاً بزوايا دائرية موحّدة (ومنها الكبسولة والدائرة).
 * الأشكال العامة لا تُطبَّق عليها هذه المؤثرات (انظر RefractionEffect).
 *
 * الدخل `content` هو الصورة المراد كسرها (الخلفية بعد التمويه).
 * الإزاحة تُقرأ دائماً من الداخل، فلا تتطلب بكسلات من خارج حدود الطبقة.
 */
object RefractionShader {

    const val UNIFORM_CONTENT = "content"
    const val UNIFORM_SIZE = "uSize"
    const val UNIFORM_CORNER_RADIUS = "uCornerRadius"
    const val UNIFORM_EDGE_WIDTH = "uEdgeWidth"
    const val UNIFORM_STRENGTH = "uStrength"
    const val UNIFORM_DISPERSION = "uDispersion"
    const val UNIFORM_LIGHT_DIRECTION = "uLightDirection"
    const val UNIFORM_LIGHT_INTENSITY = "uLightIntensity"

    const val SOURCE: String = """
uniform shader content;
uniform float2 uSize;
uniform float uCornerRadius;
uniform float uEdgeWidth;
uniform float uStrength;
uniform float uDispersion;
uniform float2 uLightDirection;
uniform float uLightIntensity;

float roundedRectSdf(float2 p) {
    float2 halfSize = uSize * 0.5;
    float2 q = abs(p) - halfSize + float2(uCornerRadius);
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - uCornerRadius;
}

float2 clampCoord(float2 c) {
    return clamp(c, float2(0.5), uSize - float2(0.5));
}

half4 main(float2 fragCoord) {
    float2 p = fragCoord - uSize * 0.5;
    float depth = -roundedRectSdf(p);

    if (depth >= uEdgeWidth || uStrength <= 0.0) {
        return content.eval(fragCoord);
    }

    float dx = roundedRectSdf(p + float2(1.0, 0.0)) - roundedRectSdf(p - float2(1.0, 0.0));
    float dy = roundedRectSdf(p + float2(0.0, 1.0)) - roundedRectSdf(p - float2(0.0, 1.0));
    float2 grad = float2(dx, dy);
    float len = length(grad);
    float2 outward = len > 0.0001 ? grad / len : float2(0.0);

    float t = clamp(depth / uEdgeWidth, 0.0, 1.0);
    float u = 1.0 - t;
    float bend = pow(u, 1.6);
    float shift = uStrength * bend;

    half4 cr = content.eval(clampCoord(fragCoord - outward * shift * (1.0 + uDispersion)));
    half4 cg = content.eval(clampCoord(fragCoord - outward * shift));
    half4 cb = content.eval(clampCoord(fragCoord - outward * shift * (1.0 - uDispersion)));
    half4 refr = half4(cr.r, cg.g, cb.b, cg.a);

    // حلقة مقلوبة عند أطراف الحافة: يظهر محتوى الداخل مضغوطاً ومنعكساً كالكرة الزجاجية.
    float rim = uEdgeWidth * 0.34;
    float k = 1.0 - clamp(depth / rim, 0.0, 1.0);
    k = k * k * (3.0 - 2.0 * k);
    float2 mp = fragCoord - outward * (2.0 * max(rim - depth, 0.0) + shift * 0.35);
    half4 mir = content.eval(clampCoord(mp));
    half4 col = mix(refr, mir, half(k * 0.85));

    // اتجاه الضوء يأتي من LightSource في Compose بدلاً من قيمة ثابتة داخل الشيدر.
    float lightLength = length(uLightDirection);
    float2 lightDir = lightLength > 0.0001
        ? uLightDirection / lightLength
        : float2(-0.70710678, -0.70710678);
    float lightFacing = dot(outward, lightDir);
    float frontLight = max(lightFacing, 0.0);
    float backLight = max(-lightFacing, 0.0);

    // تقريب Schlick: تزداد الانعكاسية قرب حافة العدسة، وتبقى هادئة في الوسط.
    float viewCosine = sqrt(max(1.0 - u * u, 0.0));
    float grazing = 1.0 - viewCosine;
    float fresnel = 0.04 + 0.96 * pow(grazing, 5.0);
    float rimBand = pow(u, 3.2);
    float directional = frontLight * 0.58 + backLight * 0.22;
    float specular = rimBand * directional * fresnel * uLightIntensity;
    float softRim = 0.035 * u * u * uLightIntensity;
    col.rgb += half3(half(specular + softRim)) * col.a;

    // تدرج ضوئي داخلي خفيف يوضح تحدب العدسة دون غسل ألوان الغلاف.
    float innerGlow = pow(max(frontLight, 0.0), 2.0) * pow(u, 1.8) * 0.035;
    col.rgb += half3(half(innerGlow)) * col.a;
    return col;
}
"""
}
