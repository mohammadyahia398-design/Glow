# Glow Vocal Isolation — Android Integration Batch

## Included
- Dedicated Vocal Isolation destination in the existing Liquid Glass bottom navigation.
- Android document picker with persistable read permission where supported.
- HTTPS-only configurable Worker endpoint (no Replicate token or provider secret in the APK).
- Multipart audio upload, prediction polling, cancellation request, and WAV download.
- Output stored under the app-specific `Android/data/<package>/files/Glow/Vocals` directory.
- WAV preview delegated to the user's installed audio player through a scoped FileProvider URI.
- Explicit empty, busy, completed, and error states.
- INTERNET permission and narrowly scoped FileProvider paths.

## Required deployment configuration
1. Deploy `backend/vocal-worker` as a Cloudflare Worker.
2. Configure the `RATE_LIMITER` Durable Object binding and migration from `wrangler.toml`.
3. Add `REPLICATE_API_TOKEN` as a Worker secret. Never put it in Android resources, BuildConfig, source code, or this patch.
4. Paste the HTTPS Worker URL into the Vocal Isolation screen.

The UI intentionally does not claim that separation is available until a valid Worker URL and server-side Replicate secret are configured.

## Important limitations in this batch
- Replicate token provisioning and Worker deployment are intentionally deferred as requested.
- The saved WAV is app-specific storage and can be removed when Glow is uninstalled. Export-to-public-Music and durable job history are not implemented here.
- Playback preview opens the system audio handler; it does not replace or mutate Glow's active queue.
- The current Worker rate-limiter binding is required. The endpoint must be deployed with the matching Durable Object configuration.
- Separation quality depends on the source mix and model; artifacts may remain.

## Safety properties
- Original source URI is read only; no write operation is made to the source file.
- No API key is embedded in the Android application.
- Worker URL must use HTTPS.
- Output is written to a new uniquely named WAV file and checked for a minimally plausible file size.
