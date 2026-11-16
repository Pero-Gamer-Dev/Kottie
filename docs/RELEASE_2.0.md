# Kottie 2.0.0 — Release freeze

## What 2.0 means

`2.0.0` freezes the **designed public API surface** under `io.kottie.api.*` for the 2.0.x line.

- Additive changes may appear in 2.0.x (new methods with defaults / new modules).
- Breaking changes to `io.kottie.api.*` require 3.0.
- Implementation modules (`kottie-core`, adapters, etc.) may still receive fixes.

## Production readiness checklist

| Gate | Status in 2.0.0 |
|------|-----------------|
| Reproducible Gradle multi-module build | Yes |
| Layout verification script | Yes (`scripts/verify-layout.sh`) |
| CI workflow JDK 17 + 21 | Yes (`.github/workflows/ci.yml`) |
| Maven Local publishing for libraries | Yes |
| Kotlin + Java sample compile | Yes (`examples/*`) |
| Thread-affinity contracts documented | Yes |
| Config validation + atomic reload | Yes |
| Versioned codecs + size limits | Yes |
| Durable file storage + SQL SPI | Yes |
| ECS queries + gameplay components | Yes |
| Paper adapter (host-wired) | Yes (skeleton + scheduler + join/quit) |
| Velocity adapter (host-wired) | Yes (skeleton + login/disconnect) |
| Cluster presence + messenger | Yes |
| Full Paper/NMS packet pipeline | **No** |
| Bundled Redis/JDBC client | **No** (SPI + in-memory/file) |
| Live Maven Central / kottie.io | **No** (build from source / your repo) |
| Published JMH numbers | **No** (methodology only) |
| Official multi-version CI vs Paper JARs | **No** |

## Recommended production use

1. Depend on `io.kottie:kottie-api:2.0.0` (`compileOnly` in plugins).
2. Bootstrap with `PaperServerAdapter` or `VelocityServerAdapter` on the host.
3. Use `FileSystemRepository` or your own `SqlRepositoryDriver` for durable data.
4. Use `RedisNetworkBridge` only with a host-provided `RedisTransport`.
5. Treat packet module as experimental/test until a real Paper Netty binding exists.

## Non-goals retained from the blueprint

- Kottie is **not** a Minecraft server fork.
- ECS does **not** replace the native entity model.
- Network does **not** promise strong cross-DC consistency.
