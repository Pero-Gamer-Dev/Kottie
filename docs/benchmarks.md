# Benchmark methodology (1.0)

Kottie 1.0 does **not** publish performance claims. When you run benchmarks:

1. Record hardware, OS, JDK version, Kotlin version.
2. Warm up (e.g. 10s) before measuring.
3. Report p50 / p95 / p99 for:
   - Event emit + decision handler completion
   - Observation event throughput
   - Config publish latency
   - Codec encode/decode throughput and payload size
4. Never attribute TPS changes solely to coroutines or ECS without a controlled A/B on a real server.

A minimal micro-harness can call:

- `EventBus.emit` in a loop with decision handlers  
- `PayloadCodec.encode/decode`  
- `EntityStore` component set/get  

Store results next to the commit hash. Do not merge marketing language into the README without data.
