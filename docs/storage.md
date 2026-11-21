# Storage drivers (1.6.0)

## Available

| Driver | Module | Durable? | Notes |
|--------|--------|----------|-------|
| `InMemoryRepository` | kottie-storage | No | Tests / single process |
| `InMemoryVersionedRepository` | kottie-storage | No | Optimistic concurrency |
| `FileSystemRepository` | kottie-storage | **Yes** | One hashed file per key, atomic write |
| `SqlKeyValueRepository` | kottie-storage | Via driver | Needs `SqlRepositoryDriver` |
| `InMemorySqlDriver` | kottie-storage | No | Test stand-in for SQL SPI |

## FileSystemRepository

```kotlin
val repo = FileSystemRepository(
    rootDir = Path.of("data/profiles"),
    codec = jsonPayloadCodec<MyProfile>("game:profile", 1)
)
repo.save("player-uuid", profile)
val loaded = repo.find("player-uuid")
```

- Keys are SHA-256 hashed → no path traversal  
- Writes use temp file + atomic move  
- Values are domain snapshots via `PayloadCodec` (never live server objects)

## SQL SPI

Implement `SqlRepositoryDriver` with HikariCP + JDBC in your server plugin:

- `connect` / `close`
- `execute` / `queryOne` / `queryMany`

Then:

```kotlin
val repo = SqlKeyValueRepository(driver, codec)
repo.ensureSchema()
```

## Rules

- Do not store Bukkit/Paper entity references  
- Enforce `maxPayloadBytes` on encode and decode  
- Prefer versioned repositories for multi-writer conflict detection  

## FileSystemVersionedRepository (2.2)

Optimistic concurrency on disk: 8-byte version prefix + codec payload.

```kotlin
val repo = FileSystemVersionedRepository(Path.of("data/v"), codec)
repo.saveIfVersion("player-1", profile, expectedVersion = 0L)
val v = repo.findVersioned("player-1")
```
