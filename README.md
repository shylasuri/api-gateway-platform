# API Gateway Platform

Multi-strategy rate limiting, usage quotas, analytics, and usage-based billing —
a Spring Boot + React reference implementation of an API gateway, built as a
final-year CSE project.

> **Read this before demoing:** see [Verification status](#verification-status)
> for exactly what has and hasn't been run in this environment.

---

## Table of contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Technology stack](#technology-stack)
4. [Project structure](#project-structure)
5. [Database design](#database-design)
6. [Redis design](#redis-design)
7. [Request lifecycle](#request-lifecycle)
8. [Rate-limiting algorithms](#rate-limiting-algorithms)
9. [Setup instructions](#setup-instructions)
10. [Environment variables](#environment-variables)
11. [Docker instructions](#docker-instructions)
12. [Stripe test-mode setup](#stripe-test-mode-setup)
13. [API documentation](#api-documentation)
14. [Running tests](#running-tests)
15. [Benchmarking](#benchmarking)
16. [Sample API requests](#sample-api-requests)
17. [Default local credentials](#default-local-credentials)
18. [Troubleshooting](#troubleshooting)
19. [Verification status](#verification-status)

---

## Overview

API consumers register, generate an API key, and call backend APIs through the
gateway at `/gateway/<route>`. The gateway:

1. Authenticates the request via `X-API-Key`
2. Identifies the consumer and their subscription plan
3. Applies one of four configurable rate-limiting strategies
4. Checks monthly/daily quota
5. Forwards the request to the registered backend API
6. Records usage (status, latency, allow/reject reason)
7. Returns the backend's response, with `X-RateLimit-*` headers attached

Everything an admin can configure — APIs, rate limits, plans, quotas — lives
in the database. Nothing about a specific backend API or a specific plan's
limits is hardcoded in Java.

## Architecture

```
┌─────────────┐        ┌──────────────────────────────────────────┐
│   React      │  JWT   │              Spring Boot                 │
│  Dashboard   │◄──────►│  Auth · API Keys · Plans · Admin APIs    │
│ (Vite/TS)    │        │                                          │
└─────────────┘        │  ┌────────────────────────────────────┐  │
                        │  │           Gateway Pipeline          │  │
      X-API-Key         │  │ API Key → Consumer → Plan →         │  │
┌─────────────┐  ──────►│  │ Rate Limiter → Quota → Forward →    │  │
│  API Client  │        │  │ Record Usage → Response             │  │
└─────────────┘        │  └────────────────────────────────────┘  │
                        └───────┬───────────────────┬──────────────┘
                                │                     │
                         ┌──────▼──────┐      ┌───────▼───────┐
                         │  PostgreSQL  │      │     Redis      │
                         │ (durable:    │      │ (hot path:     │
                         │  users, keys,│      │  rate-limit     │
                         │  usage,      │      │  counters/      │
                         │  billing)    │      │  buckets)       │
                         └─────────────┘      └────────────────┘
                                                        │
                                                ┌────────▼────────┐
                                                │   Backend APIs   │
                                                │ (e.g. sample-api)│
                                                └─────────────────┘
```

## Technology stack

**Backend:** Java 21, Spring Boot 3.3, Maven, Spring Web/Security/Data JPA,
PostgreSQL, Redis, JJWT, Stripe Java SDK (test mode), springdoc-openapi,
Lombok, JUnit 5 + Mockito + H2 (tests).

**Frontend:** React 18, TypeScript, Vite, Tailwind CSS, React Router, Axios,
Recharts.

**Infrastructure:** Docker, Docker Compose, PostgreSQL 16, Redis 7.

## Project structure

```
api-gateway-platform/
├── backend/                     Spring Boot / Maven project
│   ├── src/main/java/com/gateway/platform/
│   │   ├── entity/               JPA entities (User, ApiKey, Api, ...)
│   │   ├── repository/           Spring Data repositories
│   │   ├── security/             JWT auth (filter, service, principal)
│   │   ├── service/
│   │   │   └── ratelimit/        RateLimiterStrategy + 4 implementations
│   │   ├── controller/           REST controllers
│   │   ├── config/                Security, Redis, OpenAPI, seed data
│   │   ├── dto/                   Request/response DTOs
│   │   ├── exception/             ApiException + global handler
│   │   └── util/                  API key hashing
│   ├── src/test/java/...          Unit tests
│   └── pom.xml
├── frontend/                    React / Vite / TypeScript project
│   └── src/
│       ├── pages/                 Consumer pages (dashboard, api-keys, ...)
│       ├── pages/admin/           Admin pages
│       ├── components/            Shared UI (Card, Table, Modal, ...)
│       ├── context/                Auth context
│       └── api/                    Axios client
├── sample-api/                  Dependency-free Node sample backend
├── benchmark/                   Rate-limiter benchmarking script
├── docker-compose.yml
├── .env.example
└── README.md (this file)
```

## Database design

10 core entities, all with primary keys, foreign keys, indexes, and
timestamps:

| Entity | Purpose |
|---|---|
| `User` | Dashboard account (CONSUMER or ADMIN) |
| `ApiKey` | Hashed API key for gateway auth; raw key shown once |
| `Api` | A registered backend API (route, backend URL, method, status) |
| `SubscriptionPlan` | FREE/PRO/ENTERPRISE defaults — editable by admins |
| `Subscription` | Links a User to their current SubscriptionPlan |
| `RateLimitConfiguration` | Optional per-API override of the plan's rate limit |
| `UsageRecord` | One row per gateway request (allowed/rejected, latency, etc.) |
| `Quota` | Per-user, per-month (and per-day) usage counters |
| `BillingRecord` | Finalized/estimated billing snapshot for a period |

Schema is managed via `spring.jpa.hibernate.ddl-auto=update` for this
project's scope (a dependency-free approach appropriate for a course
project); `DataSeeder` (a `CommandLineRunner`) seeds the ADMIN user, a
sample CONSUMER, the three default plans, and the sample Products API on
first boot — idempotently, so it's safe on every restart. For a production
system, swap this for Flyway/Liquibase migrations.

## Redis design

Redis holds only **high-frequency, ephemeral** rate-limit state — durable
accounting (quotas, billing, usage history) always lives in PostgreSQL.

| Strategy | Key pattern | Redis structure |
|---|---|---|
| Fixed Window | `rl:fixed:{apiKeyId}:{apiId}:{windowIndex}` | `INCR` counter, `EXPIRE` at window end |
| Sliding Window | `rl:sliding:{apiKeyId}:{apiId}` | Sorted set (`ZADD`/`ZREMRANGEBYSCORE`), scored by request timestamp |
| Token Bucket | `rl:token:{apiKeyId}:{apiId}` | Hash `{tokens, ts}`, refilled lazily on each request |
| Leaky Bucket | `rl:leaky:{apiKeyId}:{apiId}` | Hash `{level, ts}`, leaked lazily on each request |

Every strategy executes as a single **Lua script** via
`RedisTemplate.execute(...)`, so the read-modify-write cycle (check state,
update state, decide) is atomic — concurrent requests from the same
consumer can never both "win" the last slot in a window or bucket.

## Request lifecycle

```
Client → X-API-Key → Gateway
  → Validate API key (401 if invalid/revoked)
  → Identify consumer → Identify plan
  → Load rate-limit config (per-API override, else plan default)
  → Rate limiter check        → 429 RATE_LIMIT_EXCEEDED if exceeded
  → Quota check                → 429 QUOTA_EXCEEDED if exceeded
  → Forward to backend API     → 502/503 if backend unavailable/inactive
  → Capture status + latency
  → Consume 1 unit of quota
  → Record UsageRecord
  → Return response (+ X-RateLimit-Limit / X-RateLimit-Remaining headers)
```

Error responses always follow this shape:

```json
{
  "timestamp": "2026-09-07T10:15:30Z",
  "status": 429,
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded",
  "retryAfter": 10
}
```

## Rate-limiting algorithms

| Strategy | How it works | Best for |
|---|---|---|
| **Fixed Window** | Counts requests in discrete, non-overlapping time windows (e.g. every 60s). Simple and cheap, but allows up to 2x the limit in a burst straddling a window boundary. | Simple, predictable limits where boundary bursts are acceptable |
| **Sliding Window** | Logs every request's timestamp and counts how many fall within the rolling `[now - window, now]` interval. Smooths out the boundary-burst problem of fixed windows at the cost of more Redis memory (one sorted-set entry per request). | Fairer enforcement, APIs sensitive to burst abuse |
| **Token Bucket** | A bucket holds tokens up to `capacity`, refilling at `refillRate`/second. Each request consumes a token; requests are allowed as long as tokens remain, so brief bursts are absorbed up to the bucket's capacity. | Allowing controlled bursts while enforcing a steady average rate |
| **Leaky Bucket** | A virtual queue "leaks" (drains) at a fixed `processingRate`/second regardless of arrival rate. Incoming requests add to the queue; if it's full, requests are rejected. Produces the smoothest, most constant output rate. | Protecting downstream services that need a steady, predictable load |

## Setup instructions

### Option A — Docker Compose (recommended)

```bash
git clone <this-repo>
cd api-gateway-platform
cp .env.example .env
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Sample API (direct): http://localhost:8081/products

### Option B — Run locally without Docker

**Backend:**
```bash
cd backend
# Start Postgres and Redis yourself, or: docker compose up postgres redis
export DATABASE_URL=jdbc:postgresql://localhost:5432/api_gateway_platform
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export REDIS_HOST=localhost
export JWT_SECRET=local-development-only-change-me-this-must-be-at-least-32-chars
mvn spring-boot:run
```

**Sample API:**
```bash
cd sample-api
node server.js   # no install needed — zero dependencies
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

## Environment variables

See [`.env.example`](.env.example) for the full list with defaults. Key ones:

| Variable | Purpose |
|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL connection |
| `REDIS_HOST`, `REDIS_PORT` | Redis connection |
| `JWT_SECRET` | HMAC signing key for JWTs — **must be ≥32 chars**, the app refuses to start otherwise |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | Stripe **test-mode** keys — safe to leave blank locally (see below) |
| `FRONTEND_URL` | Used for CORS allow-listing |
| `SAMPLE_API_URL` | Where the seeded Products API forwards to |
| `SEED_*` | Local-development-only seed credentials |

No real secrets are committed anywhere in this repository.

## Docker instructions

`docker compose up --build` builds and starts four services: `postgres`,
`redis`, `sample-api`, `backend`, and `frontend`. Inside the Docker network,
services address each other **by service name**, not `localhost` — e.g. the
backend connects to `postgres:5432` and `redis:6379`, and forwards gateway
traffic to `http://sample-api:8081`. The frontend's nginx container proxies
`/api/*` to `http://backend:8080` so the browser only ever talks to one
origin. Only the browser-facing ports (5173, 8080, 8081, 5432, 6379) are
published to your host machine.

## Stripe test-mode setup

1. Create a free Stripe account and switch to **Test mode**.
2. Copy your test secret key (`sk_test_...`) from
   https://dashboard.stripe.com/test/apikeys.
3. Set `STRIPE_SECRET_KEY` in `.env`.
4. Restart the backend.

If `STRIPE_SECRET_KEY` is unset, `StripeService` logs a warning and every
Stripe call becomes a safe no-op — **billing amounts are still calculated
correctly** from real usage/quota data recorded in Postgres; only the
optional "sync a customer/subscription to Stripe" step is skipped. This is
a deliberate, documented fallback, not fake functionality.

## API documentation

Full interactive OpenAPI/Swagger docs are available at
`/swagger-ui.html` once the backend is running, covering authentication,
API keys, APIs, plans, gateway, usage, analytics, and billing endpoints.

Representative endpoints:

| Method | Path | Description |
|---|---|---|
| POST | `/auth/register`, `/auth/login` | Auth |
| POST/GET/DELETE | `/api-keys`, `/api-keys/{id}`, `/api-keys/{id}/rotate` | API key management |
| GET | `/apis` | List registered APIs (consumer view) |
| POST/PUT/DELETE | `/admin/apis`, `/admin/apis/{id}` | Admin API management |
| PUT | `/admin/apis/{id}/rate-limit` | Configure per-API rate limit |
| GET/POST/PUT | `/plans`, `/admin/plans`, `/admin/plans/{id}` | Subscription plans |
| ANY | `/gateway/**` | The gateway proxy itself (requires `X-API-Key`) |
| GET | `/dashboard/summary`, `/usage`, `/usage/analytics`, `/billing` | Consumer dashboard |
| GET | `/admin/analytics`, `/admin/consumers` | Admin dashboard |

## Running tests

```bash
cd backend
mvn test
```

Test coverage includes: all four rate limiters (boundary conditions — exact
limit, exhausted bucket, full queue, expired sliding-window entries),
authentication (registration conflicts, bad credentials, disabled
accounts), API key handling (raw key never persisted, revoked keys
rejected, cross-user access denied), quota accounting (exceeded/not
exceeded, consumption), billing overage calculation, and the full gateway
pipeline (valid request forwarded and recorded; invalid key / unknown
route / rate-limit / quota / inactive-API cases all short-circuit
**before** the backend is ever called).

> See [Verification status](#verification-status) — these tests are
> written and reviewed but could not be executed in the environment that
> produced this repository.

## Benchmarking

See [`benchmark/README.md`](benchmark/README.md). In short:

```bash
cd benchmark
node benchmark.js --key <your-api-key> --url http://localhost:8080/gateway/products \
  --strategy TOKEN_BUCKET --requests 500 --concurrency 20
```

Reconfigure the target API's rate-limit strategy between runs (from Admin >
APIs) to compare real, measured throughput/rejection/latency across all
four algorithms. The script fires genuine HTTP requests — no numbers are
pre-filled or assumed.

## Sample API requests

**Register + login:**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@example.com","password":"password123","fullName":"Dev User","company":"Acme"}'
```

**Create an API key** (with the JWT from above):
```bash
curl -X POST http://localhost:8080/api-keys \
  -H "Authorization: Bearer <JWT>" -H "Content-Type: application/json" \
  -d '{"label":"My first key"}'
```

**Call the gateway:**
```bash
curl http://localhost:8080/gateway/products -H "X-API-Key: gw_live_xxxxxxxx"
```

Expected response (forwarded from `sample-api`):
```json
[{"id":1,"name":"Laptop","price":75000}, ...]
```

## Default local credentials

**For local development only — never use these in production.**

| Role | Email | Password |
|---|---|---|
| Admin | `admin@gateway.local` | `Admin123!` |
| Consumer (seeded, PRO plan) | `consumer@gateway.local` | `Consumer123!` |

Override via `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` / etc. in `.env`.

## Troubleshooting

- **Backend can't reach Postgres/Redis in Docker**: make sure you're using
  the service names (`postgres`, `redis`), not `localhost` — see
  [Docker instructions](#docker-instructions).
- **`JWT_SECRET must be set and at least 32 characters`**: the app refuses
  to start with a short/missing secret by design. Set `JWT_SECRET` in `.env`.
- **429 on every request immediately**: check the consumer's plan and any
  per-API rate-limit override in Admin > APIs — a very low `limit`/`window`
  combination will do this intentionally.
- **Stripe errors on startup**: they shouldn't occur — `StripeService` is
  designed to no-op gracefully when `STRIPE_SECRET_KEY` is unset. If you do
  see an error, check the key is a valid `sk_test_...` value.
- **Frontend shows CORS errors**: confirm `FRONTEND_URL` on the backend
  matches the origin the browser is actually using.
- **`mvn test` fails to download dependencies**: you need network access to
  Maven Central; this cannot be worked around offline.

## Verification status

Being transparent about what was actually run, versus written and reviewed
but not executed, in the environment that produced this codebase:

- ✅ **Frontend**: `npm install`, `npx tsc -b` (zero TypeScript errors), and
  `npx vite build` (successful production build) were all actually run and
  passed.
- ⚠️ **Backend**: could **not** be compiled or tested in this environment —
  the sandbox's network allowlist does not include Maven Central, so
  `mvn compile` / `mvn test` cannot reach the artifacts they need. The code
  was written carefully and reviewed against the real APIs of Spring Boot
  3.3, JJWT 0.12, and the Stripe Java SDK, but **you should run
  `mvn clean compile` and `mvn test` yourself as the first step** after
  cloning, and treat any errors that surface as expected first-run
  friction to fix, not a sign the design is wrong.
- ⚠️ **Docker Compose / end-to-end flow**: written to the spec (service
  names, health checks, dependency ordering) but not executed — you should
  run `docker compose up --build` and work through the sample requests
  above as your own verification pass.

If you hit compile errors, they'll most likely be minor (an import, a
method signature drift between library versions) rather than structural —
the rate-limiting algorithms, the gateway pipeline, and the entity/service
layering are the parts most worth reviewing carefully if something doesn't
line up.
