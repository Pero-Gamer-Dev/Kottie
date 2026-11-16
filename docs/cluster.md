# Multi-instance cluster helpers (1.4.0)

## Goal

Harden multi-instance deployments without requiring Redis client libraries at compile time.

## Building blocks

| API | Role |
|-----|------|
| `InstanceId` | Stable process identity |
| `ServerPlatform` | PAPER / FOLIA / PURPUR / SPIGOT / UNKNOWN |
| `ClusterPresenceService` | Heartbeats + membership view |
| `ClusterMessenger` | Topic publish/subscribe across instances |
| `NetworkBridge` | Transport (in-memory or Redis-shaped) |

## Typical topology

```
Server A                     Server B
ClusterPresence  ←── NetworkBridge ──→  ClusterPresence
ClusterMessenger ←── (same bridge) ──→  ClusterMessenger
```

## Usage sketch

```kotlin
val bridge = RedisNetworkBridge(transport, scope) // or InMemoryNetworkBridge
bridge.connect()
val presence = DefaultClusterPresenceService(bridge, scope, platform = ServerPlatform.PAPER)
presence.start()
val messenger = DefaultClusterMessenger(bridge, scope)
messenger.subscribe("inventory-sync") { env -> /* apply snapshot */ }
messenger.publish("inventory-sync", codec.encode(snapshot))
```

## What 1.4 does not claim

- Automatic inventory merge / conflict CRDTs  
- Folia region-thread scheduling implementation (platform enum only)  
- Guaranteed exactly-once delivery (idempotency keys still apply at the bridge layer)  
