# Deploying API Gateway Platform (free tier)

A step-by-step runbook to deploy the whole stack for free:

| Piece | Host | Cost |
|---|---|---|
| Frontend (React) | **Vercel** | Free, always-on |
| Backend (Spring Boot) | **Render** (Docker web service) | Free; sleeps after ~15 min idle |
| sample-api (Node) | **Render** (Docker web service) | Free |
| PostgreSQL | **Neon** | Free, persistent |
| Redis | **Upstash** | Free, persistent |
| Email (password reset) | **Resend** | Free (3k/mo) |

> The backend runs with `SPRING_PROFILES_ACTIVE=prod` (see `application-prod.yml`), which **requires** every secret and **refuses to start** if one is missing — no dev defaults leak into production.

Do the steps **in order**; later steps need URLs produced by earlier ones.

---

## 0. Prerequisites
- This repo pushed to GitHub (Render and Vercel deploy from it).
- Free accounts: [Neon](https://neon.tech), [Upstash](https://upstash.com), [Resend](https://resend.com), [Render](https://render.com), [Vercel](https://vercel.com).

## 1. PostgreSQL — Neon
1. Create a project → a database is created automatically.
2. Open **Connection Details** and copy the values (host, database, user, password).
3. Build the JDBC URL the backend needs (note `jdbc:` prefix and `sslmode=require`):
   ```
   DATABASE_URL=jdbc:postgresql://<host>/<database>?sslmode=require
   DATABASE_USERNAME=<user>
   DATABASE_PASSWORD=<password>
   ```

## 2. Redis — Upstash
1. Create a **Redis** database (any free region).
2. From its page, copy **Endpoint** (host), **Port** (usually `6379`), and **Password**.
3. You'll use:
   ```
   REDIS_HOST=<endpoint-host>
   REDIS_PORT=6379
   REDIS_USERNAME=default
   REDIS_PASSWORD=<password>
   REDIS_SSL=true
   ```

## 3. Email — Resend
1. Sign up, then **API Keys → Create API Key**. Copy it → `RESEND_API_KEY`.
2. Sender address (`MAIL_FROM`):
   - **Quick start:** `onboarding@resend.dev` works with no setup, **but only delivers to your own Resend account email.** Fine for a demo you test yourself.
   - **Real delivery to anyone:** add and verify your domain in Resend, then use `noreply@yourdomain.com`.

## 4. Backend + sample-api — Render (Blueprint)
1. **New → Blueprint**, connect this GitHub repo. Render reads [`render.yaml`](render.yaml) and proposes **gateway-backend** and **gateway-sample-api**.
2. Apply it. `JWT_SECRET` is auto-generated; fill the `sync: false` vars from steps 1–3, plus:
   ```
   FRONTEND_URL=https://placeholder.vercel.app     # temporary; fixed in step 6
   SAMPLE_API_URL=https://gateway-sample-api.onrender.com/products
   SEED_ADMIN_EMAIL=admin@yourdomain.com
   SEED_ADMIN_PASSWORD=<strong password>
   SEED_CONSUMER_EMAIL=demo@yourdomain.com
   SEED_CONSUMER_PASSWORD=<strong password>
   RESEND_API_KEY=<from step 3>
   MAIL_FROM=GatewayPlatform <onboarding@resend.dev>
   ```
3. Deploy. First build takes a few minutes (Maven). When live, note the backend URL, e.g. `https://gateway-backend.onrender.com`. Confirm `‹backend-url›/actuator/health` returns `{"status":"UP"}`.
   - If `SAMPLE_API_URL`'s host differs from the name Render assigned, update it to the real **gateway-sample-api** URL and redeploy.

## 5. Frontend — Vercel
1. **Add New → Project**, import this repo.
2. Set **Root Directory** to `frontend` (Vercel auto-detects Vite via [`vercel.json`](frontend/vercel.json)).
3. Add an environment variable:
   ```
   VITE_API_BASE_URL=https://gateway-backend.onrender.com   # your step-4 URL, no trailing slash
   ```
4. Deploy. Note your frontend URL, e.g. `https://your-app.vercel.app`.

## 6. Wire the two together
1. Back in Render → **gateway-backend** → Environment, set:
   ```
   FRONTEND_URL=https://your-app.vercel.app     # exact Vercel origin, no trailing slash
   ```
2. Save → this redeploys the backend. CORS is now locked to your frontend.

## 7. Verify
- Open the Vercel URL → **Register** a new account → you land on the dashboard.
- Create an API key, then call the gateway:
  ```
  curl https://gateway-backend.onrender.com/gateway/products -H "X-API-Key: <your key>"
  ```
- **Password reset:** on the login page → *Forgot password?* → enter your email → check your inbox (remember the `onboarding@resend.dev` limitation) → open the link → set a new password.
- Sign in as admin with `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD`.

---

## Free-tier caveats
- **Render free backend sleeps** after ~15 min idle; the first request then takes ~30–60s to wake. (The frontend and databases stay up.)
- **Neon / Upstash** free tiers are persistent but have generous usage caps — plenty for a demo/portfolio.
- **Resend** free sends 3k/mo; without a verified domain it only emails your own address.

## Security reminders
- Never commit real values — only `.env.production.example` (a template) is tracked.
- `SPRING_PROFILES_ACTIVE=prod` guarantees `PASSWORD_RESET_EXPOSE_TOKEN=false` and rejects a weak/missing `JWT_SECRET`.
- Rotate `JWT_SECRET` and the seed passwords if they're ever exposed.
- Schema is managed by Hibernate `ddl-auto=update`; for ongoing schema changes in production, migrate to Flyway/Liquibase.
