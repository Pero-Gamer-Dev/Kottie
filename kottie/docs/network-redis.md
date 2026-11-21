# Redis-shaped network bridge (1.2.0)

## Design

- `NetworkBridge` SPI stays transport-neutral.
- `RedisTransport` is the plug-in point for Lettuce/Jedis/etc.
- `RedisNetworkBridge` adds reconnect/backoff, idempotency, stale policy, and binary framing.
- `InMemoryRedisTransport` covers tests without a Redis process.

## Production sketch

Implement `RedisTransport` with your client:

1. `connect()` / `disconnect()` map to client connection lifecycle.
2. `publish(channel, bytes)` → `PUBLISH`.
3. `subscribe(channel, handler)` → pub/sub listener; call `handler` for each message body.
4. On connection lost, call `RedisNetworkBridge.notifyConnectionLost()`.

## Framing

`NetworkMessageCodec` encodes channel, idempotency key, schema metadata, timestamp, and payload. Keep payload as domain codec output (`PayloadCodec`), not live server objects.

## Policies

| Policy | Behaviour |
|--------|-----------|
| ReconnectPolicy | Exponential backoff, capped delay, max attempts |
| OutagePolicy.DROP_STALE | Ignore older `sentAtEpochMs` per channel |
| IdempotencyTracker | Drop duplicate `idempotencyKey`s |

## Not in 1.2

- Bundled Lettuce/Jedis dependency  
- Consumer groups / Redis Streams  
- Cross-DC conflict CRDTs  
