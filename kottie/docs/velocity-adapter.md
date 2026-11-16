# Velocity adapter (1.9.0)

## Role

Run Kottie on a **proxy** (Velocity) for login/disconnect tracking and cluster messaging — without depending on the Velocity API at compile time inside `kottie-adapter-velocity`.

## Wiring

```kotlin
val adapter = VelocityServerAdapter.forVelocityHost(
    proxyExecutor = Executor { r -> /* proxy.server.eventLoop or scheduler */ r.run() },
    isProxyThread = { /* proxy thread check */ true }
)
adapter.initialize()
val runtime = KottieRuntime.start(adapter)
val bridge = adapter.attachEventBridge(
    runtime.eventBus,
    runtime.entityStore,
    runtime.scope,
    onLoginNotify = { id, name -> messenger.publish("proxy-login", ...) }
)

// Velocity @Subscribe PostLoginEvent → bridge.onPlayerLogin(uuid, username)
// Velocity @Subscribe DisconnectEvent → bridge.onPlayerDisconnect(uuid, username)
```

## With cluster

```
Velocity proxy (Kottie + VelocityServerAdapter)
        ↕ NetworkBridge / Redis
Backend Paper (Kottie + PaperServerAdapter)
```

Use `ClusterPresenceService` on both sides so each instance sees peers.

## Declared versions

`3.3`, `3.4` — extend after you verify.

## Not in 1.9

- Bundled Velocity dependency  
- Automatic plugin messaging channel codecs  
- BungeeCord-specific adapter (same pattern can be copied)  
