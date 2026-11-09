# Definition of Done — Kottie 1.0.0

## Met in this release

| Criterion | Evidence |
|-----------|----------|
| Reproducible source tree + Gradle build | Multi-module project, wrapper, version catalog |
| JDK / Kotlin documented | `docs/compatibility.md`, `libs.versions.toml` |
| Kotlin + Java example plugins | `examples/kotlin-plugin`, `examples/java-plugin` |
| Public API vs internals separated | `io.kottie.api.*` vs `io.kottie.core` / impl modules |
| Thread-affinity rules documented + tested | `ServerScheduler`, smoke tests |
| Coroutine jobs cancelled on shutdown | `CancellableScope`, runtime tests |
| Config reload validated + atomic | `DefaultConfigService`, config tests |
| Serialized payloads versioned + size-limited | `PayloadCodec`, `safeDecode` tests |
| Unsupported packet/server combos fail clearly | `UnsupportedPacketVersionException`, `UnsupportedServerEnvironmentException` |
| Optional modules do not block core boot | packets / network / paper adapter optional |
| Compatibility + migration docs | `docs/compatibility.md`, `docs/migration-guide.md` |

## Explicitly out of scope for 1.0.0

| Item | Reason |
|------|--------|
| Full Paper/NMS packet implementation | Requires version-locked Paper dependency and CI against a server JAR |
| Redis/JDBC production bridges | SPI only; reference impl is in-memory |
| Live Maven Central / kottie.io publishing | Build from source; coordinates are reserved for future publishing |
| Performance claims / TPS guarantees | No production benchmarks published |
| Replacement of Bukkit entity model | ECS is metadata-only by design |

## Remaining for a future 1.1+ “production server” release

1. Paper adapter with real scheduler + event bridge  
2. CI matrix on declared Minecraft versions  
3. Published artifacts with checksums  
4. Benchmark harness results under documented load  
5. Optional Redis module with reconnect integration tests against a real broker  
