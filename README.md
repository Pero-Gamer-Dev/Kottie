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
