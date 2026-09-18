---
name: cloudflare-backend-basics
description: Use when building, deploying, or managing Cloudflare Workers API backends, Cloudflare Pages, Cloudflare D1 (SQL), Cloudflare R2 (Storage), or Cloudflare Turnstile bot verification for RIDERsYNK.
---

# Cloudflare Backend Architecture & Skill Guide

This skill provides guidelines and patterns for building and maintaining the RIDERsYNK backend using Cloudflare Edge services.

---

## Cloudflare Stack Overview

- **Cloudflare Workers**: Serverless API execution at the edge (0ms cold start).
- **Cloudflare D1**: Serverless SQLite database for user profiles, trip metadata, and convoy sessions.
- **Cloudflare R2**: Object storage with **Zero Egress Fees** for trip audio, avatar images, and exported GPX files.
- **Cloudflare KV**: Ultra-low latency key-value cache for active rider GPS pings.
- **Cloudflare Turnstile**: Free, privacy-friendly bot verification replacing reCAPTCHA.

---

## Directory Structure

```text
d:\My Applications\RIDERsYNK\
├── wrangler.toml               # Primary Cloudflare environment configuration
├── cloudflare-backend/
│   ├── package.json            # Node.js dependencies for Worker
│   └── src/
│       └── index.ts            # Workers Edge API Router & Handlers
```

---

## Command Reference

### Local Development
```bash
npx wrangler dev
```

### Deploying Worker Edge API
```bash
npx wrangler deploy
```

### Executing D1 SQL Queries
```bash
npx wrangler d1 execute ridesync-db --command "SELECT * FROM users;"
```

### Creating an R2 Bucket
```bash
npx wrangler r2 bucket create ridesync-media-bucket
```
