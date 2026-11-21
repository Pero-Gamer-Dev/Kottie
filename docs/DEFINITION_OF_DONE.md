# Definition of Done — Kottie 2.0.0

## Met

| Criterion | Evidence |
|-----------|----------|
| Reproducible source + Gradle build | Multi-module, wrapper, version catalog |
| JDK 17/21 documented + CI matrix | `ci.yml`, `compatibility.md` |
| Kotlin + Java examples | `examples/*` |
| API vs internals separation | `io.kottie.api.*` vs implementation modules |
| Thread-affinity rules | `ServerScheduler` + tests |
| Lifecycle cancellation | Scopes + disable path |
| Config validation + atomic snapshots | Config module + tests |
| Codecs versioned + size-limited | Serialization + tests |
| Durable storage option | `FileSystemRepository` |
| SQL SPI | `SqlRepositoryDriver` |
| ECS queries + systems | `EntityQuery`, `SystemRunner` |
| Paper host adapter | `kottie-adapter-paper` |
| Velocity host adapter | `kottie-adapter-velocity` |
| Cluster presence + messaging | `kottie-cluster` |
| Layout + release scripts | `verify-layout.sh`, `release-check.sh` |
| MIT license file | `LICENSE` |
| Honest non-claims documented | `docs/RELEASE_2.0.md` |

## Explicitly not claimed in 2.0.0

| Item | Notes |
|------|--------|
| Full Paper Netty packet interception | SPI + in-memory only |
| Official Redis/JDBC modules | SPI + reference drivers only |
| Maven Central publication | Operator must publish |
| TPS / latency marketing numbers | Methodology only |
| Replacement for Bukkit entity API | ECS is metadata-only |

## 2.0.1

| Item | Status |
|------|--------|
| CI build green path (wrapper + JDK setup) | Fixed |
| ComponentKey compile on Kotlin 2.0 | Fixed |
| examples aggregate project | Fixed |
