# Rate-Limiter Benchmark

Compares the four rate-limiting strategies (Fixed Window, Sliding Window,
Token Bucket, Leaky Bucket) against a **running** instance of the platform.
It does not fabricate numbers — every result comes from real HTTP calls
against `/gateway/products` using a real API key.

## Prerequisites

1. The full stack is running: `docker compose up --build`
2. You have a valid `X-API-Key` for a consumer (create one from the
   dashboard's API Keys page, or via `POST /api-keys` with a JWT).
3. For each strategy you want to benchmark, set that API's rate-limit
   configuration accordingly from the Admin > APIs > Rate limit dialog
   (or `PUT /admin/apis/{id}/rate-limit`).

## Usage

```bash
cd benchmark
npm install
node benchmark.js --key gw_live_xxxxxxxx --url http://localhost:8080/gateway/products \
  --strategy FIXED_WINDOW --requests 500 --concurrency 20
```

Run it once per strategy (reconfiguring the API's rate limit between runs)
and compare the printed summary:

- **Requests processed** — total attempted
- **Requests rejected** — count that received HTTP 429
- **Throughput** — successful requests / second
- **Average latency** — mean round-trip time in ms, allowed requests only

## Example comparison table (fill in with YOUR real run output)

| Strategy        | Processed | Rejected | Throughput (req/s) | Avg latency (ms) |
|------------------|-----------|----------|---------------------|-------------------|
| Fixed Window     |           |          |                     |                   |
| Sliding Window   |           |          |                     |                   |
| Token Bucket     |           |          |                     |                   |
| Leaky Bucket     |           |          |                     |                   |

Do not fill this table in without actually running the benchmark — the
whole point is to compare real, measured behavior, not assumed numbers.
