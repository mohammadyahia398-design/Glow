package com.glow.app.ui.glass.lighting

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.sqrt

/**
 * اختبارات رياضية مستقلة لمصدر الضوء المستخدم في الانكسار والانعكاس.
 * لا تحتاج إلى جهاز Android؛ جميع العمليات هندسية نقية.
 */
class LightSourceTest {

    private fun assertNear(actual: Float, expected: Float, tolerance: Float = 0.0001f) {
        assertThat(actual).isWithin(tolerance).of(expected)
    }

    @Test
    fun default_light_points_from_upper_left() {
        val vector = LightSource.Default.towardLight
        assertNear(vector.x, -sqrt(0.5f))
        assertNear(vector.y, -sqrt(0.5f))
    }

    @Test
    fun right_angle_points_to_positive_x() {
        val vector = LightSource(0f).towardLight
        assertNear(vector.x, 1f)
        assertNear(vector.y, 0f)
    }

    @Test
    fun down_angle_points_to_positive_y() {
        val vector = LightSource(90f).towardLight
        assertNear(vector.x, 0f)
        assertNear(vector.y, 1f)
    }

    @Test
    fun left_angle_points_to_negative_x() {
        val vector = LightSource(180f).towardLight
        assertNear(vector.x, -1f)
        assertNear(vector.y, 0f)
    }

    @Test
    fun up_angle_points_to_negative_y() {
        val vector = LightSource(270f).towardLight
        assertNear(vector.x, 0f)
        assertNear(vector.y, -1f)
    }

    @Test
    fun full_rotation_preserves_direction() {
        val base = LightSource(45f).towardLight
        val rotated = LightSource(405f).towardLight
        assertNear(rotated.x, base.x)
        assertNear(rotated.y, base.y)
    }

    @Test
    fun facing_is_one_when_normal_faces_light() {
        assertNear(LightSource(0f).facing(Offset(1f, 0f)), 1f)
    }

    @Test
    fun facing_is_negative_one_when_normal_opposes_light() {
        assertNear(LightSource(0f).facing(Offset(-1f, 0f)), -1f)
    }

    @Test
    fun facing_normalizes_non_unit_normal() {
        assertNear(LightSource(0f).facing(Offset(12f, 0f)), 1f)
    }

    @Test
    fun zero_normal_has_no_facing() {
        assertNear(LightSource(0f).facing(Offset.Zero), 0f)
    }

    @Test
    fun rim_strength_ignores_back_facing_edges() {
        assertNear(LightSource(0f, 1.5f).rimStrength(Offset(-1f, 0f)), 0f)
    }

    @Test
    fun rim_strength_scales_with_intensity() {
        assertNear(LightSource(0f, 1.5f).rimStrength(Offset(1f, 0f)), 1.5f)
    }

    @Test
    fun from_vector_normalizes_direction() {
        val source = LightSource.fromVector(Offset(8f, 0f))
        assertNear(source.towardLight.x, 1f)
        assertNear(source.towardLight.y, 0f)
    }

    @Test
    fun vector_from_diagonal_is_normalized_to_unit_length() {
        val vector = LightSource.fromVector(Offset(3f, 4f)).towardLight
        assertNear(vector.x * vector.x + vector.y * vector.y, 1f)
    }

    @Test
    fun from_zero_vector_uses_default_direction() {
        val source = LightSource.fromVector(Offset.Zero, intensity = 0.7f)
        assertNear(source.towardLight.x, LightSource.Default.towardLight.x)
        assertNear(source.towardLight.y, LightSource.Default.towardLight.y)
        assertNear(source.intensity, 0.7f)
    }

    @Test
    fun from_vector_normalizes_negative_angle() {
        val source = LightSource.fromVector(Offset(1f, -1f))
        assertThat(source.angleDegrees).isAtLeast(0f)
        assertThat(source.angleDegrees).isLessThan(360f)
        assertNear(source.towardLight.x, sqrt(0.5f))
        assertNear(source.towardLight.y, -sqrt(0.5f))
    }

    @Test
    fun lerp_keeps_shortest_path_across_zero_degrees() {
        val halfway = LightSource(350f).lerp(LightSource(10f), 0.5f)
        assertNear(halfway.angleDegrees, 0f)
    }

    @Test
    fun lerp_keeps_shortest_path_across_360_degrees() {
        val halfway = LightSource(10f).lerp(LightSource(350f), 0.5f)
        assertNear(halfway.angleDegrees, 0f)
    }

    @Test
    fun lerp_interpolates_intensity() {
        val halfway = LightSource(0f, 0.2f).lerp(LightSource(90f, 1.8f), 0.5f)
        assertNear(halfway.intensity, 1f)
    }

    @Test
    fun lerp_fraction_below_zero_returns_start() {
        val result = LightSource(20f, 0.4f).lerp(LightSource(80f, 1.4f), -1f)
        assertNear(result.angleDegrees, 20f)
        assertNear(result.intensity, 0.4f)
    }

    @Test
    fun lerp_fraction_above_one_returns_end() {
        val result = LightSource(20f, 0.4f).lerp(LightSource(80f, 1.4f), 2f)
        assertNear(result.angleDegrees, 80f)
        assertNear(result.intensity, 1.4f)
    }

    @Test
    fun point_toward_uses_touch_vector() {
        val state = LightState(LightSource(225f, 1f))
        state.pointToward(Offset(20f, 0f), Offset.Zero)
        assertNear(state.source.angleDegrees, 0f)
        assertNear(state.source.intensity, 1.25f)
    }

    @Test
    fun point_toward_clamps_boost_to_supported_intensity() {
        val state = LightState(LightSource(0f, 1.9f))
        state.pointToward(Offset(0f, 20f), Offset.Zero, boost = 1f)
        assertNear(state.source.intensity, 2f)
    }

    @Test
    fun point_toward_ignores_touch_at_center() {
        val state = LightState(LightSource(45f, 0.8f))
        state.pointToward(Offset(0.5f, 0.5f), Offset.Zero)
        assertNear(state.source.angleDegrees, 45f)
        assertNear(state.source.intensity, 0.8f)
    }

    @Test
    fun release_restores_original_source() {
        val rest = LightSource(135f, 0.65f)
        val state = LightState(rest)
        state.pointToward(Offset(10f, 0f), Offset.Zero)
        state.release()
        assertNear(state.source.angleDegrees, 135f)
        assertNear(state.source.intensity, 0.65f)
    }

    @Test
    fun snap_to_replaces_angle_and_intensity() {
        val state = LightState(LightSource.Default)
        state.snapTo(LightSource(315f, 1.7f))
        assertNear(state.source.angleDegrees, 315f)
        assertNear(state.source.intensity, 1.7f)
    }
    // Regression coverage for malformed inputs that previously could leak NaN into drawing.

    @Test
    fun non_finite_normal_is_ignored_without_nan_output() {
        assertNear(LightSource.Default.facing(Offset(Float.NaN, 1f)), 0f)
        assertNear(LightSource.Default.facing(Offset(Float.POSITIVE_INFINITY, 0f)), 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun non_finite_angle_is_rejected() {
        LightSource(Float.NaN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun non_finite_intensity_is_rejected() {
        LightSource(0f, Float.POSITIVE_INFINITY)
    }

    @Test(expected = IllegalArgumentException::class)
    fun non_finite_interpolation_fraction_is_rejected() {
        LightSource(0f).lerp(LightSource(90f), Float.NaN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun non_finite_direction_is_rejected() {
        LightSource.fromVector(Offset(Float.NaN, 1f))
    }

    @Test
    fun invalid_touch_coordinates_do_not_corrupt_current_light() {
        val state = LightState(LightSource(120f, 0.8f))
        state.pointToward(Offset(Float.NaN, 20f), Offset.Zero)
        assertNear(state.source.angleDegrees, 120f)
        assertNear(state.source.intensity, 0.8f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun non_finite_touch_boost_is_rejected() {
        LightState().pointToward(Offset(10f, 0f), Offset.Zero, Float.NaN)
    }

    }
