# Kottie architecture (1.9)

## Module map

```
io.kottie.api              Public contracts
kottie-core                Runtime
kottie-coroutines          Scopes
kottie-server-spi          ServerAdapter
kottie-ecs-runtime         EntityStore + queries + SystemRunner
kottie-config              Typed config
kottie-serialization       Codecs
kottie-storage             Memory + file + SQL SPI
kottie-packets             PacketBus
kottie-network             NetworkBridge / Redis transport
kottie-cluster             Presence + messenger
kottie-adapter-paper       Paper host adapter
kottie-adapter-velocity    Velocity/proxy host adapter
kottie-testkit             Tests
examples/*                 Samples
```

## Proxy + backend

```
Velocity (adapter-velocity) ── NetworkBridge ── Paper (adapter-paper)
         ClusterPresence / ClusterMessenger on both
```
