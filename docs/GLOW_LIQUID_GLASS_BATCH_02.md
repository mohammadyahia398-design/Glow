# Liquid Glass — Batch 2 (toggle + slider + button)

Base: main @ 3a932f6.
- New: com.glow.app.liquid.ui (LiquidButton, LiquidToggle, LiquidSlider) + ui/utils (DampedDragAnimation, InteractiveHighlight, DragGestureInspector, Lerp). Ported from the pack, Capsule() -> RoundedCornerShape(50), no kyant-shapes dependency.
- Modified: SettingsScreen.kt (skip-short-songs switch), EqualizerSheet.kt (band sliders).
- New components render only when GlassTier.supportsRefraction (Android 13+, enough RAM); otherwise the old GlassSwitch / material Slider stay.
- LiquidButton is added but not wired anywhere yet (no real-screen GlassButton exists; only the preview gallery).
- Not compile-verified locally. Rely on the Build triggered after Apply patch.
- Rollback: delete liquid/ui and restore the two files from git.
