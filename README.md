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
