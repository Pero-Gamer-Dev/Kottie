# Migration notes (toward 1.0)

## From 0.7 → 0.8

- New optional module: `kottie-network`
- New API package: `io.kottie.api.network`
- No breaking changes to existing 0.7 facades (`EventBus`, `EntityStore`, `ConfigService`, `PacketBus`)

## Preparing for 1.0

1. Depend only on `io.kottie.api.*` from plugins.
2. Treat network and packet modules as optional.
3. Register component keys via `ComponentRegistry`.
4. Use `ConfigService` validation — never mutate live config objects.
5. Use `PayloadCodec.safeDecode` for any external bytes.
6. Cancel plugin scopes / subscriptions on disable.
