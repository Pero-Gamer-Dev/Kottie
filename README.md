# Kottie

Kottie is a modern, high-performance, asynchronous-first server API engineered for Minecraft Java Edition architectures. Built from the ground up with Kotlin and Java compatibility in mind, Kottie shifts away from the monolithic legacy designs of Bukkit to deliver a highly customizable, component-driven, network-ready development platform.

## 🚀 Key Features

* **Asynchronous Reactive Pipeline:** Powered by Kotlin Coroutines and asynchronous flows. Say goodbye to main-thread TPS drops caused by blocking database queries or heavy plugin loops.
* **Component Entity System (ECS):** Ditch fragile inheritance models. Game entities are highly modular data carriers—attach custom metadata, custom pathfinding, or custom network states instantly.
* **Network-First Architecture:** Native, structured serialization targets out-of-the-box. Built to securely distribute player inventories, custom mechanics, and game cross-states across multi-instance server networks via Redis or database pools.
* **Declarative Config Streams:** Automatically map files directly to clean data types. Supports live hot-reloading streams natively without interrupting the runtime server environment.
* **Packet Interception Layer:** Complete, high-performance packet manipulation API exposed safely out of the box, allowing developers to craft per-player mechanics flawlessly.

## 📦 Quick Start (Kotlin DSL Example)

```kotlin
import io.kottie.api.Kottie
import io.kottie.api.event.player.AsyncPlayerJoinEvent
import io.kottie.api.ecs.components.ManaComponent
import kotlinx.coroutines.flow.collect

class MyCustomPlugin {
    suspend fun onEnable() {
        val eventBus = Kottie.getEventBus()

        // Asynchronously listen to incoming player connections
        eventBus.listen(AsyncPlayerJoinEvent::class.java).collect { event ->
            val player = event.player
            
            // Attaching custom high-quality mechanics via components
            player.addComponent(ManaComponent(maxMana = 100, currentMana = 100))
            
            println("Successfully initialized custom network profile for \${player.name}")
        }
    }
}
```

## 🛠️ Installation & Dependency Management

### Gradle (Kotlin DSL)
```kotlin
repositories {
    maven("https://kottie.io")
}

dependencies {
    compileOnly("io.kottie:kottie-api:1.0.0")
}
```

### Maven
```xml
<repository>
    <id>kottie-repo</id>
    <url>https://kottie.io</url>
</repository>

<dependency>
    <groupId>io.kottie</groupId>
    <artifactId>kottie-api</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

## 📄 License
Kottie API is distributed under the MIT License, providing complete legal security and freedom for enterprise networks and commercial plugins alike.

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
