export interface Env {
  DB: any;
  STORAGE: any;
  CONVOY_CACHE: any;
  TURNSTILE_SECRET_KEY?: string;
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);

    // Enable CORS for Android client requests
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Turnstile-Token',
      'Content-Type': 'application/json',
    };

    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    try {
      // 1. Health check endpoint
      if (url.pathname === '/api/health') {
        return new Response(
          JSON.stringify({ status: 'ok', provider: 'Cloudflare Workers Edge', timestamp: Date.now() }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 2. Turnstile Bot Verification Endpoint
      if (url.pathname === '/api/verify-turnstile' && request.method === 'POST') {
        const body = await request.json() as { token?: string };
        const turnstileToken = body.token;

        if (!turnstileToken) {
          return new Response(
            JSON.stringify({ success: false, error: 'Missing turnstile token' }),
            { status: 400, headers: corsHeaders }
          );
        }

        const verifyResponse = await fetch('https://challenges.cloudflare.com/turnstile/v0/siteverify', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({
            secret: env.TURNSTILE_SECRET_KEY || '1x0000000000000000000000000000000AA', // Test secret key fallback
            response: turnstileToken,
          }),
        });

        const outcome = await verifyResponse.json() as { success: boolean };
        return new Response(JSON.stringify(outcome), { status: 200, headers: corsHeaders });
      }

      // 3. Realtime Convoy GPS Telemetry Ingestion (KV / D1)
      if (url.pathname === '/api/convoy/ping' && request.method === 'POST') {
        const ping = await request.json() as any;
        const riderId = ping.riderId || 'unknown_rider';

        // Cache latest location ping in Cloudflare KV (sub-millisecond latency)
        if (env.CONVOY_CACHE) {
          await env.CONVOY_CACHE.put(`rider:${riderId}`, JSON.stringify(ping), { expirationTtl: 300 });
        }

        return new Response(
          JSON.stringify({ success: true, riderId, syncedAt: Date.now() }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 4. Default 404 handler
      return new Response(
        JSON.stringify({ error: 'Endpoint not found on Cloudflare Edge' }),
        { status: 404, headers: corsHeaders }
      );
    } catch (err: any) {
      return new Response(
        JSON.stringify({ error: 'Internal Cloudflare Worker Error', details: err.message }),
        { status: 500, headers: corsHeaders }
      );
    }
  },
};
