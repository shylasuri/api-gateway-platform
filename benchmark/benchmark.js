// Real-traffic benchmark for the API gateway's rate limiters.
// Zero dependencies — uses Node's built-in fetch (Node 18+).
//
// Usage:
//   node benchmark.js --key <apiKey> --url <gatewayUrl> --strategy <name> \
//                      --requests 500 --concurrency 20
//
// This tool does not simulate or fabricate results: it fires real HTTP
// requests at a running gateway instance and reports what actually happened.

function parseArgs(argv) {
  const args = { requests: 200, concurrency: 10, strategy: 'UNSPECIFIED' };
  for (let i = 0; i < argv.length; i += 2) {
    const key = argv[i]?.replace(/^--/, '');
    const value = argv[i + 1];
    if (!key) continue;
    args[key] = value;
  }
  return args;
}

async function fireRequest(url, apiKey) {
  const startedAt = performance.now();
  try {
    const res = await fetch(url, { headers: { 'X-API-Key': apiKey } });
    const latencyMs = performance.now() - startedAt;
    return { status: res.status, latencyMs, rejected: res.status === 429 };
  } catch (err) {
    return { status: 0, latencyMs: performance.now() - startedAt, rejected: false, error: String(err) };
  }
}

async function runBatch(url, apiKey, count, concurrency) {
  const results = [];
  let inFlight = 0;
  let launched = 0;

  return new Promise((resolve) => {
    function launchNext() {
      if (launched >= count) {
        if (inFlight === 0) resolve(results);
        return;
      }
      launched += 1;
      inFlight += 1;
      fireRequest(url, apiKey).then((result) => {
        results.push(result);
        inFlight -= 1;
        launchNext();
      });
    }
    for (let i = 0; i < Math.min(concurrency, count); i++) launchNext();
  });
}

async function main() {
  const args = parseArgs(process.argv.slice(2));

  if (!args.key || !args.url) {
    console.error('Usage: node benchmark.js --key <apiKey> --url <gatewayUrl> --strategy <name> [--requests N] [--concurrency N]');
    process.exit(1);
  }

  const requestCount = parseInt(args.requests, 10);
  const concurrency = parseInt(args.concurrency, 10);

  console.log(`Benchmarking strategy=${args.strategy} url=${args.url} requests=${requestCount} concurrency=${concurrency}`);
  const startedAt = performance.now();
  const results = await runBatch(args.url, args.key, requestCount, concurrency);
  const totalTimeSeconds = (performance.now() - startedAt) / 1000;

  const processed = results.length;
  const rejected = results.filter((r) => r.rejected).length;
  const allowed = results.filter((r) => !r.rejected && r.status > 0);
  const avgLatency = allowed.length > 0
    ? allowed.reduce((sum, r) => sum + r.latencyMs, 0) / allowed.length
    : 0;
  const throughput = allowed.length / totalTimeSeconds;

  console.log('\n--- Results ---');
  console.log(`Strategy:            ${args.strategy}`);
  console.log(`Requests processed:  ${processed}`);
  console.log(`Requests allowed:    ${allowed.length}`);
  console.log(`Requests rejected:   ${rejected} (429)`);
  console.log(`Throughput:          ${throughput.toFixed(2)} req/s (allowed only)`);
  console.log(`Avg latency:         ${avgLatency.toFixed(2)} ms (allowed only)`);
  console.log(`Total wall time:     ${totalTimeSeconds.toFixed(2)} s`);
}

main();
