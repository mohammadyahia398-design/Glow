# Glow — Vocal Isolation Backend (Batch 01)

This patch adds the secure server-side foundation for AI vocal extraction.

## What this batch includes

- Cloudflare Worker API.
- Replicate file upload and Demucs HTDemucs prediction creation.
- Status polling, result download proxy, and cancellation endpoints.
- Replicate token is read only from the Worker secret `REPLICATE_API_TOKEN`.
- 50 MB input limit.
- MIME type checks.
- Durable Object rate limit: 3 new jobs per IP per hour.
- No public object-storage bucket and no permanent audio storage in this Worker.

## Important

This batch is the backend foundation only. It does not yet add the Android screen or connect the player. That is intentionally kept as the next isolated app batch so the current player remains untouched until the backend endpoint is deployed and verified.

The model's actual output structure may vary by published model version. The Worker searches recursively for a `vocals` output key and reports a clear error if none is present. Validate one test prediction before relying on the feature.

## Deploy from a phone

1. Create/sign in to a Cloudflare account.
2. Open Workers & Pages and create a Worker named `glow-vocal-worker`.
3. Upload the contents of `backend/vocal-worker/src/index.js` as the Worker code.
4. In Worker Settings → Bindings, add a Durable Object binding:
   - Variable name: `RATE_LIMITER`
   - Class name: `GlowRateLimiter`
5. For the Durable Object migration, use Wrangler deployment from Cloudflare's supported deployment flow. The provided `wrangler.toml` declares the required SQLite Durable Object migration. If the dashboard editor cannot configure this migration, deploy using Wrangler in a browser-based Codespace.
6. In Worker Settings → Variables and Secrets, add a secret:
   - Name: `REPLICATE_API_TOKEN`
   - Value: your Replicate API token
7. Deploy, then open `https://YOUR-WORKER.workers.dev/health`.
8. Expected response: `{"ok":true,"service":"Glow Vocal Isolation","engine":"Demucs HTDemucs"}`.

Never put the Replicate token in the Android app, GitHub repository, ZIP patch, or BuildConfig.

## API contract for the Android batch

### Start

`POST /v1/separate`

Content type: `multipart/form-data`

Field:
- `audio`: selected audio file

Success (HTTP 202):
```json
{
  "predictionId": "replicate-prediction-id",
  "status": "starting",
  "message": "Vocal separation started."
}
```

### Poll

`GET /v1/predictions/{predictionId}`

Possible statuses include `starting`, `processing`, `succeeded`, `failed`, and `canceled`.

### Download

`GET /v1/predictions/{predictionId}/audio`

Returns the vocals audio bytes as a WAV download after success.

### Cancel

`POST /v1/predictions/{predictionId}/cancel`

## Operational notes

- Replicate charges for model usage; check current pricing in your Replicate account before processing long tracks.
- Input and output files managed by Replicate are temporary according to Replicate's file/prediction retention policy. Download and save the result locally.
- Audio is sent to Replicate for processing. Do not process files unless you have the right to use and upload them.
- Vocal isolation is source-dependent. Dense mixes, backing vocals, reverb, distortion, and mastering can leave artifacts; "perfectly clean" separation cannot be guaranteed.
- The Worker rate limiter is a basic per-IP safeguard, not user authentication. For a public production launch, add authenticated user sessions and account-level quotas before exposing the endpoint broadly.
