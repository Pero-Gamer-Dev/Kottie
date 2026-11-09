# Kottie 1.0.0

Modern, asynchronous-first **API framework** for Minecraft Java Edition plugins (Kotlin / Java).

**Status:** `1.0.0` — public API frozen for the designed scope.  
**Not** a Minecraft server fork. **Not** a drop-in Paper replacement.  
A real Paper runtime binding is a skeleton that fails fast until you supply a server-backed scheduler.

## What 1.0 includes

| Area | Capability |
|------|------------|
| Lifecycle | State machine, owned scopes, shutdown timeout |
| Scheduler | Server-thread affinity, I/O/compute dispatchers |
| Events | Observation vs Decision, priorities, metrics |
| ECS | Component registry, entity store, snapshots (metadata only) |
| Config | Typed bind/reload, validation, atomic snapshots |
| Serialization | Versioned codecs, size limits, safe decode |
| Storage | Repository + versioned repository SPI (in-memory ref) |
| Packets | Interception SPI + in-memory test bus |
| Network | Optional bridge SPI (idempotency, stale policy, in-memory) |
| Paper | Adapter **skeleton** with clear unsupported failure |

## What 1.0 does **not** claim

- TPS improvements without your own benchmarks  
- Multi-instance strong consistency  
- Live `https://kottie.io` Maven hosting (build from source / `publishToMavenLocal`)  
- Full NMS/packet coverage across Paper versions  

See `docs/DEFINITION_OF_DONE.md` and `docs/compatibility.md`.

## Build

```bash
./gradlew build
./gradlew :kottie-testkit:test
./gradlew publishToMavenLocal   # optional local install
```

## Plugin dependency (after publishToMavenLocal)

```kotlin
repositories { mavenLocal(); mavenCentral() }
dependencies {
    compileOnly("io.kottie:kottie-api:1.0.0")
}
```

## Quick mental model

```
Plugin
  → io.kottie.api (stable)
      → kottie-core runtime
          → optional: config, ecs, packets, network
          → ServerAdapter (in-memory tests, or Paper skeleton)
```

## Modules

`kottie-api`, `kottie-core`, `kottie-coroutines`, `kottie-server-spi`, `kottie-ecs-runtime`, `kottie-config`, `kottie-serialization`, `kottie-storage`, `kottie-packets`, `kottie-network`, `kottie-adapter-paper`, `kottie-testkit`, `examples/*`

## License

MIT (as stated in the original design intent).
