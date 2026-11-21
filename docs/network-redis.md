# Redis-shaped network (2.1)

## Stack

```
RedisNetworkBridge
  → ReconnectableRedisTransport   (optional)
    → NamespacedRedisTransport    (optional)
      → RedisTransport            (InMemory / Lettuce host impl)
```

## 2.1 additions

### NamespacedRedisTransport

Prefixes channels: `NamespacedRedisTransport(inner, "prod")` publishes to `prod:chat` when you use logical channel `chat`.

### ReconnectableRedisTransport

- Exponential backoff reconnect
- Re-binds subscriptions after reconnect
- Call `notifyDisconnected()` from your client connection listener

### Lettuce

Kottie does **not** bundle Lettuce. See `LettuceWiring.kt` for a host-side sketch. Use:

```kotlin
val transport = ReconnectableRedisTransport(
    NamespacedRedisTransport(yourLettuceTransport, "prod"),
    scope
)
```

## Policies (unchanged)

Idempotency + `OutagePolicy` still apply at `RedisNetworkBridge` / `NetworkMessage` layer.
