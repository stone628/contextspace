# Performance Tuning — Ktor (Netty engine)

## Recommended baseline (`application.yaml`)

```yaml
ktor:
  deployment:
    port: 8080
    host: "0.0.0.0"

    # --- Thread pools (start here) ---
    callGroupSize: 16       # cores × 4 — handles application handlers
    workerGroupSize: 8      # cores × 2 — Netty I/O threads
    connectionGroupSize: 4  # cores × 1 — accepts new connections

    # --- Connection management ---
    connectionIdleTimeout: 30000   # 30s, tune per traffic pattern
    keepAlive: true
    tcpNoDelay: true

    # --- Limits ---
    requestBodySizeLimit: 10485760  # 10 MB
    maxHeaderSize: 16384            # 16 KB

    # --- Graceful shutdown ---
    shutdownTimeout: 10

    # --- Compression ---
    contentCompression:
      enabled: true
      minSize: 1024
      priority: 0.8
```

## Thread pool sizing

| Pool | Formula | Purpose |
|---|---|---|
| `connectionGroupSize` | `cores × 1` | Accept connections (rarely a bottleneck) |
| `workerGroupSize` | `cores × 2` | Network I/O read/write |
| `callGroupSize` | `cores × 4` | Run application handlers |

- **IO-bound** (DB calls, Redis, external APIs) → increase `callGroupSize`
- **CPU-bound** (BCrypt, crypto, image processing) → keep pools closer to `cores`
- Monitor with VisualVM, JMX (`ktor.request.count`, `ktor.call.duration`), or a profiler

## Connection management

- **`connectionIdleTimeout`**: Lower value (15-30s) frees resources faster under variable load. Higher value (60s+) keeps connections warm for burst traffic.
- **`keepAlive: true`**: Always enable — eliminates TCP handshake overhead per request.
- **`tcpNoDelay: true`**: Always enable — prevents Nagle buffering (adds ~40ms latency).

## Content compression

Enable with `minSize: 1024` to compress large JSON responses. Monitor CPU — if it spikes, raise `minSize` or lower `priority`.

## Monitoring checklist

1. Are threads saturating? → Bump `callGroupSize` or review async boundaries (`withContext(IO)`)
2. Are connections queuing? → Increase `workerGroupSize` or DB pool size
3. Is latency spiking? → Check `connectionIdleTimeout` vs DB pool `maxLifetime`
4. Is bandwidth high? → Enable compressed responses or paginate payloads
