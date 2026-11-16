# Kottie 2.0.0

Modern, asynchronous-first **API framework** for Minecraft Java Edition plugins (Kotlin / Java).

**Status:** `2.0.0` — public API freeze for the designed scope.

> **Not** a Minecraft server fork. **Not** a drop-in Paper/Velocity replacement.  
> Adapters are **host-wired skeletons**; you supply Paper/Velocity executors and event forwarding.

## Features

| Area | 2.0 capability |
|------|----------------|
| Lifecycle | Owned scopes, ordered shutdown |
| Scheduler | Server/proxy thread affinity |
| Events | Observation vs decision, priorities |
| ECS | Components, indexed queries, systems |
| Config | Typed bind/observe, validation, atomic reload |
| Serialization | Versioned codecs, size limits |
| Storage | Memory, **filesystem**, SQL SPI |
| Network | Bridge SPI, Redis-shaped transport |
| Cluster | Presence heartbeats, topic messenger |
| Paper | Scheduler + join/quit bridge |
| Velocity | Scheduler + login/disconnect bridge |

## Build & verify

```bash
bash scripts/verify-layout.sh
./scripts/release-check.sh
# or:
./gradlew build
./gradlew :kottie-testkit:test
./gradlew publishToMavenLocal
```

## Dependency

```kotlin
repositories { mavenLocal(); mavenCentral() }
dependencies {
    compileOnly("io.kottie:kottie-api:2.0.0")
}
```

## Documentation

| Doc | Topic |
|-----|--------|
| `docs/RELEASE_2.0.md` | Freeze meaning + production checklist |
| `docs/compatibility.md` | Support matrix |
| `docs/architecture.md` | Module map |
| `docs/paper-adapter.md` | Paper wiring |
| `docs/velocity-adapter.md` | Velocity wiring |
| `docs/storage.md` | Repositories |
| `docs/ecs.md` | Queries & components |
| `docs/cluster.md` | Multi-instance |
| `docs/publishing.md` | Maven Local / external repo |

## License

MIT — see `LICENSE`.

## After 2.0

Possible 2.x additions (non-breaking): real Paper packet binding, Lettuce module, JMH numbers, Maven Central publishing.  
Breaking API changes wait for 3.0.
