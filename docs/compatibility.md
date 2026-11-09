# Kottie Compatibility Matrix (1.0.0)

| Component | Requirement | Notes |
|-----------|-------------|-------|
| JDK | 17+ | 17 and 21 supported targets |
| Kotlin | 2.0.x | See `gradle/libs.versions.toml` |
| Minecraft / Paper | Adapter skeleton only | `kottie-adapter-paper` fails fast without a real Paper binding |
| Packet adapter | In-memory / test | Production Paper packets not shipped |
| Network bridge | In-memory | Redis/NATS not shipped |
| Serialization | Codec SPI + JSON helper | |
| Storage | In-memory reference | |

## Installation (1.0.0)

Artifacts are **built from source** in this release:

```bash
git clone <your-fork> kottie && cd kottie
./gradlew publishToMavenLocal
```

Then in a plugin:

```kotlin
repositories { mavenLocal(); mavenCentral() }
dependencies {
    compileOnly("io.kottie:kottie-api:1.0.0")
}
```

The README coordinates `https://kottie.io` / `io.kottie:kottie-api:1.0.0` are **reserved** for a future published repository and are not verified live in 1.0.0.

## Module independence

Core (`api` + `core` + scheduler/events/lifecycle) boots without ECS, config, packets, network, or Paper.
