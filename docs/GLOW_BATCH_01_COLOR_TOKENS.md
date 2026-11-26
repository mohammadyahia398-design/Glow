# Glow — Batch 1: Calm colour tokens

Scope: presentation only. Files: ui/theme/GlowPalette.kt, GlowSkin.kt, GlowEffects.kt. No dependencies, Gradle, audio or navigation changes.

Rule: neutral black/white dominate; only 6 official colours are used in the UI:
F042FF accent · 7226FF violet (gradient partner) · 87F5F5 rare accent · E5E8F5 text · FFE5F1 light container · 010030 light-mode ink.
Other official colours/gradients (Pink Radiance, Aurora Mist, Deep Violet Abyss) are kept in the brief but not used, to avoid visual fatigue.

Changes:
- Dark text: lavender E4D4FF -> neutral E5E8F5; secondary 9A90B8 -> A3A7B8.
- Glass hairlines/highlights now neutral (based on E5E8F5).
- Background halos softened (magenta 0.10 -> 0.06, violet 0.14 -> 0.08).
- Light mode: ink 010030, neutral chips/outlines, FFE5F1 container, near-white bottom FFF7FB.

Contrast (WCAG): dark text 17.2, dark secondary 8.2, dark accent 6.9, light text 19.8, light secondary 7.1, light accent text 6.0.
Known: white on F042FF = 3.0 (OK for icons/large text only) — handled in the accessibility batch.
Not touched: user-selectable colourways (Dusk, Amber, ...), GlowEmptyState Pale colour (empty-states batch).
Rollback: revert this commit.
