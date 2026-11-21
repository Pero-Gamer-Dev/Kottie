# Changelog

## 2.0.1

### Fixed
- Kotlin compile: `ComponentKey` as data class (vararg + `@JvmField`)
- CI: executable wrapper, Temurin + Gradle cache, stacktraces on failure
- Gradle: `pluginManagement`, examples aggregate project, Java 17 release flags

## 2.0.0

- Public API freeze for designed scope
- Paper + Velocity host adapters, cluster, storage, ECS queries, full module set
- MIT license, release checklist docs

## 2.1.0

### Added
- NamespacedRedisTransport
- ReconnectableRedisTransport
- LettuceWiring host sketch (docs)
- Microbench helper + smoke test

## 2.2.0

### Fixed
- Inconsistent JVM target on JDK 21 runners (Java 21 vs Kotlin 17) via toolchain 17

### Added
- FileSystemVersionedRepository (optimistic concurrency on disk)

## 2.3.0

### Added
- PluginChannelPacketBridge (plugin messaging → PacketBus)
- PluginMessagePayload
- docs/plugin-channels.md

## 2.4.0

### Added
- CommandManager / DefaultCommandManager
- PermissionService / DefaultPermissionService
- Messenger / RecordingMessenger
- ServiceRegistry / DefaultServiceRegistry
- RegionScheduler / FallbackRegionScheduler
- Kottie facade exposes commands, permissions, messenger, services, regions

## 2.5.0

### Added
- PlayerService / PlayerView / GameMode
- WorldService / WorldView / LocationSnapshot
- InventoryService / ItemStackSnapshot / InventorySnapshot
- In-memory implementations wired to KottieRuntime

## 2.6.0

### Added
- ScoreboardService / BossBarService / TitleService
- PluginLoader / DefaultPluginLoader (dependency-ordered lifecycle)
- In-memory / recording implementations on KottieRuntime
