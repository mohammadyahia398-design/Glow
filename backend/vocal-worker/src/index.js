/**
 * Glow Vocal Isolation API — Cloudflare Worker
 *
 * Endpoints:
 *   GET  /health
 *   POST /v1/separate       multipart/form-data: audio=<file>
 *   GET  /v1/predictions/:id
 *   GET  /v1/predictions/:id/audio
 *   POST /v1/predictions/:id/cancel
 *
 * Required Worker secret:
 *   REPLICATE_API_TOKEN
 *
 * Required Durable Object binding:
 *   RATE_LIMITER -> GlowRateLimiter
 *
 * The Replicate token is never returned to the Android client.
 */

const REPLICATE_API = "https://api.replicate.com/v1";
const MODEL_OWNER = "cjwbw";
const MODEL_NAME = "demucs";
const MAX_UPLOAD_BYTES = 50 * 1024 * 1024;
const ALLOWED_AUDIO_TYPES = new Set([
  "audio/mpeg",
  "audio/mp3",
  "audio/wav",
  "audio/x-wav",
  "audio/flac",
  "audio/aac",
  "audio/mp4",
  "audio/ogg",
  "audio/aiff",
  "application/octet-stream"
]);

function json(data, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "GET, POST, OPTIONS",
      "access-control-allow-headers": "content-type",
      ...extraHeaders
    }
  });
}

function cors() {
  return new Response(null, {
    status: 204,
    headers: {
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "GET, POST, OPTIONS",
      "access-control-allow-headers": "content-type",
      "access-control-max-age": "86400"
    }
  });
}

function errorMessage(value, fallback) {
  if (typeof value === "string" && value.trim()) return value.slice(0, 500);
  return fallback;
}

async function replicateFetch(path, env, init = {}) {
  return fetch(`${REPLICATE_API}${path}`, {
    ...init,
    headers: {
      authorization: `Bearer ${env.REPLICATE_API_TOKEN}`,
      ...(init.headers || {})
    }
  });
}

async function getLatestModelVersion(env) {
  const response = await replicateFetch(
    `/models/${MODEL_OWNER}/${MODEL_NAME}/versions`,
    env
  );
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(errorMessage(body.detail, "Could not load Demucs model version."));
  }
  const version = body.results?.[0]?.id;
  if (!version) throw new Error("Demucs model has no published version.");
  return version;
}

async function uploadInput(file, env) {
  const form = new FormData();
  form.append("content", file, file.name || "glow-audio-input");
  const response = await replicateFetch("/files", env, {
    method: "POST",
    body: form
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(errorMessage(body.detail, "Replicate rejected the audio upload."));
  }
  const url = body.urls?.get || body.urls?.download || body.url;
  if (!url) throw new Error("Replicate did not return an input file URL.");
  return { id: body.id, url };
}

async function createPrediction(fileUrl, env) {
  const version = await getLatestModelVersion(env);
  const response = await replicateFetch("/predictions", env, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      version,
      input: {
        audio: fileUrl,
        stem: "vocals",
        model_name: "htdemucs",
        shifts: 2,
        overlap: 0.25,
        output_format: "wav"
      }
    })
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(errorMessage(body.detail, "Could not start vocal separation."));
  }
  if (!body.id) throw new Error("Replicate returned no prediction ID.");
  return body;
}

async function getPrediction(id, env) {
  const response = await replicateFetch(`/predictions/${encodeURIComponent(id)}`, env);
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    return { error: errorMessage(body.detail, "Prediction could not be found."), status: response.status };
  }
  return body;
}

function findVocalUrl(output) {
  if (!output) return null;
  if (typeof output === "string" && /^https:\/\//i.test(output)) return output;
  if (Array.isArray(output)) {
    for (const item of output) {
      const found = findVocalUrl(item);
      if (found) return found;
    }
    return null;
  }
  if (typeof output === "object") {
    const preferredKeys = ["vocals", "vocal", "vocals_url", "vocal_url"];
    for (const key of preferredKeys) {
      if (key in output) {
        const found = findVocalUrl(output[key]);
        if (found) return found;
      }
    }
    for (const value of Object.values(output)) {
      const found = findVocalUrl(value);
      if (found) return found;
    }
  }
  return null;
}

async function handleSeparate(request, env) {
  if (!env.REPLICATE_API_TOKEN) {
    return json({ error: "Server is not configured: missing Replicate token." }, 503);
  }

  const clientIp = request.headers.get("CF-Connecting-IP") || "unknown";
  const limiterId = env.RATE_LIMITER.idFromName(clientIp);
  const limiter = env.RATE_LIMITER.get(limiterId);
  const limitResponse = await limiter.fetch("https://rate-limit/check", { method: "POST" });
  if (!limitResponse.ok) {
    return json({ error: "Rate limit reached. Try again later." }, 429, {
      "retry-after": "3600"
    });
  }

  const contentLength = Number(request.headers.get("content-length") || 0);
  if (contentLength > MAX_UPLOAD_BYTES) {
    return json({ error: "Audio file exceeds the 50 MB upload limit." }, 413);
  }

  let form;
  try {
    form = await request.formData();
  } catch (_) {
    return json({ error: "Expected multipart/form-data with an audio field." }, 400);
  }

  const file = form.get("audio");
  if (!(file instanceof File)) {
    return json({ error: "Missing audio file. Use multipart field name 'audio'." }, 400);
  }
  if (file.size <= 0 || file.size > MAX_UPLOAD_BYTES) {
    return json({ error: "Audio file must be between 1 byte and 50 MB." }, 413);
  }
  if (file.type && !ALLOWED_AUDIO_TYPES.has(file.type.toLowerCase())) {
    return json({ error: "Unsupported audio MIME type." }, 415);
  }

  try {
    const uploaded = await uploadInput(file, env);
    const prediction = await createPrediction(uploaded.url, env);
    return json({
      predictionId: prediction.id,
      status: prediction.status || "starting",
      message: "Vocal separation started."
    }, 202);
  } catch (error) {
    return json({ error: errorMessage(error?.message, "Vocal separation could not start.") }, 502);
  }
}

async function handlePrediction(id, env) {
  const prediction = await getPrediction(id, env);
  if (prediction.error) return json({ error: prediction.error }, prediction.status || 502);
  const vocalUrl = prediction.status === "succeeded"
    ? findVocalUrl(prediction.output)
    : null;
  return json({
    id: prediction.id,
    status: prediction.status,
    error: prediction.error || null,
    progress: prediction.status === "succeeded" ? 100 : null,
    audioReady: Boolean(vocalUrl)
  });
}

async function handleAudio(id, env) {
  const prediction = await getPrediction(id, env);
  if (prediction.error) return json({ error: prediction.error }, prediction.status || 502);
  if (prediction.status !== "succeeded") {
    return json({ error: "Vocal track is not ready yet.", status: prediction.status }, 409);
  }
  const vocalUrl = findVocalUrl(prediction.output);
  if (!vocalUrl) {
    return json({ error: "The model completed but no vocals output was found." }, 502);
  }

  // Replicate output URLs are short-lived; proxy the bytes immediately to the client.
  const audioResponse = await fetch(vocalUrl, { redirect: "follow" });
  if (!audioResponse.ok) {
    return json({ error: "Could not download the generated vocal track." }, 502);
  }
  const headers = new Headers({
    "content-type": audioResponse.headers.get("content-type") || "audio/wav",
    "content-disposition": 'attachment; filename="glow-vocals.wav"',
    "cache-control": "no-store",
    "access-control-allow-origin": "*"
  });
  const length = audioResponse.headers.get("content-length");
  if (length) headers.set("content-length", length);
  return new Response(audioResponse.body, { status: 200, headers });
}

async function handleCancel(id, env) {
  const response = await replicateFetch(`/predictions/${encodeURIComponent(id)}/cancel`, env, {
    method: "POST"
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) return json({ error: errorMessage(body.detail, "Could not cancel job.") }, response.status);
  return json({ id: body.id, status: body.status || "canceled" });
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return cors();
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/health") {
      return json({ ok: true, service: "Glow Vocal Isolation", engine: "Demucs HTDemucs" });
    }
    if (request.method === "POST" && url.pathname === "/v1/separate") {
      return handleSeparate(request, env);
    }

    const match = url.pathname.match(/^\/v1\/predictions\/([a-zA-Z0-9_-]+)(\/audio|\/cancel)?$/);
    if (match) {
      const id = match[1];
      const action = match[2] || "";
      if (request.method === "GET" && action === "") return handlePrediction(id, env);
      if (request.method === "GET" && action === "/audio") return handleAudio(id, env);
      if (request.method === "POST" && action === "/cancel") return handleCancel(id, env);
    }

    return json({ error: "Not found." }, 404);
  }
};

/**
 * Per-IP fixed-window rate limiter.
 * Three uploads per hour per IP. The object stores only counters and timestamps,
 * never audio or Replicate credentials.
 */
export class GlowRateLimiter {
  constructor(state) {
    this.state = state;
  }

  async fetch(request) {
    const now = Date.now();
    const key = "hour-window";
    const current = await this.state.storage.get(key);
    const windowMs = 60 * 60 * 1000;
    let record = current;
    if (!record || now - record.startedAt >= windowMs) {
      record = { startedAt: now, count: 0 };
    }
    if (record.count >= 3) {
      return new Response("rate limited", { status: 429 });
    }
    record.count += 1;
    await this.state.storage.put(key, record, { expirationTtl: 3600 });
    return new Response("ok", { status: 200 });
  }
}
