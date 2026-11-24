# Liquid Glass — Batch 1 (engine only)

Base: main @ cd559c5. Adds the Backdrop engine (Kyant, Apache 2.0) as `com.glow.app.liquid.backdrop`.
- 30 new Kotlin files, Android-only. No existing file is modified or deleted. No Gradle change.
- Nothing is wired into any screen yet (no UI change expected).
- Lens/refraction needs Android 13+, blur needs Android 12+; callers must provide a fallback below that.
- Lens supports CornerBasedShape only (kyant-shapes dependency deferred to batch 2).
- Not compile-verified locally: rely on GitHub Actions Build for the resulting SHA.
- Rollback: delete app/src/main/java/com/glow/app/liquid/ and THIRD_PARTY/liquid-glass/.

## Fix 01-a (after first Build failure)
- LayerRecorder.kt: removed `context(node)` (needs Kotlin 2.2+); `node` is now a normal first parameter, 3 call sites updated.
- InverseLayerScope.kt: removed `blendMode`/`colorFilter` overrides (not in GraphicsLayerScope on Compose 1.7).
