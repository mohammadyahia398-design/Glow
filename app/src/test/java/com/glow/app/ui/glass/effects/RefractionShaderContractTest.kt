package com.glow.app.ui.glass.effects

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * يحمي عقد الربط بين أسماء Uniforms في Kotlin وتعريفاتها داخل AGSL.
 * لا يستبدل اختبار RuntimeShader على جهاز API 33+، لكنه يكشف الانحرافات النصية مبكراً.
 */
class RefractionShaderContractTest {

    @Test
    fun every_required_uniform_is_declared_in_shader_source() {
        val uniforms = listOf(
            RefractionShader.UNIFORM_CONTENT,
            RefractionShader.UNIFORM_SIZE,
            RefractionShader.UNIFORM_CORNER_RADIUS,
            RefractionShader.UNIFORM_EDGE_WIDTH,
            RefractionShader.UNIFORM_STRENGTH,
            RefractionShader.UNIFORM_DISPERSION,
            RefractionShader.UNIFORM_LIGHT_DIRECTION,
            RefractionShader.UNIFORM_LIGHT_INTENSITY,
        )

        uniforms.forEach { uniform ->
            assertThat(RefractionShader.SOURCE).contains(uniform)
        }
    }

    @Test
    fun shader_uses_content_as_runtime_shader_input() {
        assertThat(RefractionShader.SOURCE).contains("uniform shader content;")
        assertThat(RefractionShader.SOURCE).contains("half4 main(float2 fragCoord)")
        assertThat(RefractionShader.SOURCE).contains("content.eval(")
    }

    @Test
    fun directional_fresnel_lighting_is_present() {
        assertThat(RefractionShader.SOURCE).contains("uLightDirection")
        assertThat(RefractionShader.SOURCE).contains("uLightIntensity")
        assertThat(RefractionShader.SOURCE).contains("pow(grazing, 5.0)")
        assertThat(RefractionShader.SOURCE).contains("directional")
    }

    @Test
    fun shader_has_safe_fallback_for_zero_light_vector() {
        assertThat(RefractionShader.SOURCE).contains("lightLength > 0.0001")
        assertThat(RefractionShader.SOURCE).contains("float2(-0.70710678, -0.70710678)")
    }
}
