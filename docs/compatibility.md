# Compatibility matrix — Kottie 2.0.0

| Component | Supported | Notes |
|-----------|-----------|--------|
| **JDK** | 17, 21 | CI matrix |
| **Kotlin** | 2.0.x | See `gradle/libs.versions.toml` |
| **Gradle** | Wrapper 8.x | |
| **Paper (host)** | Declared 1.20.4, 1.21, 1.21.1 | Adapter is host-wired; verify on your exact build |
| **Velocity (host)** | Declared 3.3, 3.4 | Adapter is host-wired |
| **Packets** | In-memory / SPI | No production Netty binding shipped |
| **Network** | In-memory + RedisTransport SPI | Bring your own Lettuce/Jedis |
| **Storage** | Memory, filesystem, SQL SPI | Bring your own JDBC for SQL |
| **OS** | Linux/macOS/Windows for build | Server host OS = whatever runs Paper/Velocity |

## Maven coordinates (after publishToMavenLocal)

| Artifact | Coordinate |
|----------|------------|
| API | `io.kottie:kottie-api:2.0.0` |
| Core | `io.kottie:kottie-core:2.0.0` |
| Paper adapter | `io.kottie:kottie-adapter-paper:2.0.0` |
| Velocity adapter | `io.kottie:kottie-adapter-velocity:2.0.0` |
| Cluster | `io.kottie:kottie-cluster:2.0.0` |
| … | See `docs/publishing.md` |

## Module independence

Core boots without: ECS usage, config files, packets, network, cluster, Paper, Velocity.
