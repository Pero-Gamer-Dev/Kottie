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


# Kottie

Modern, asynchronous-first Minecraft Java Edition server API (Kotlin / Java).

**Status:** `0.1.0-alpha` – architectural skeleton implementing the Phase 0–3 contracts from the design blueprint.

This repository is a clean-room implementation of the public contracts described in the architecture review. It is **not** a full production server fork or a drop-in Paper replacement yet.

## Modules

| Module | Purpose |
|--------|---------|
| `kottie-api` | Stable public interfaces (events, lifecycle, scheduler, ECS, …) |
| `kottie-core` | Runtime bootstrap, default event bus, in-memory scheduler |
| `kottie-coroutines` | Scope ownership helpers |
| `kottie-server-spi` | Adapter contract for real server implementations |
| `kottie-ecs-runtime` | Simple concurrent entity / component store |
| `kottie-config` | Typed config holder (atomic snapshots) |
| `kottie-testkit` | Smoke tests |
| `examples/kotlin-plugin` | Corrected lifecycle example |

## Quick Start (development)

```bash
./gradlew build
./gradlew :kottie-testkit:test
```

## Design Principles (enforced)

- Explicit thread affinity – world mutations only via `ServerScheduler.onServerThread`
- Owned coroutine scopes – cancelled on disable
- Typed subscriptions with disposal – no silent listener leaks
- ECS is an opt-in metadata layer, not a replacement for the server entity model
- Configuration published as immutable snapshots after validation (skeleton)

## Roadmap alignment

See `docs/architecture.md` and the original blueprint for the full phase plan.
