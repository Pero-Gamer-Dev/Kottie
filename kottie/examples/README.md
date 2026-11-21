# Kottie examples (1.8)

## kotlin-plugin

`MyCustomPlugin` demonstrates:

1. Registering / using ECS components (`Mana`, `Health`, `Stats`, `Cooldown`)
2. Subscribing to `PlayerJoinEvent` and attaching components on the server thread
3. A coroutine tick loop that queries entities with `CooldownComponent` and ticks cooldowns
4. Optional `ClusterMessenger` publish on join (cross-server)
5. Optional `ConfigService.observe` for `ExamplePluginConfig` hot-reload

Wire from a host bootstrap:

```kotlin
val plugin = MyCustomPlugin(kottie, clusterMessenger = messenger)
plugin.onEnable()
```

## java-plugin

`JavaExamplePlugin` shows Java access to component keys and the `Kottie` facade.
Lifecycle suspend functions remain Kotlin-first.

## Note

Examples are **compileOnly** samples — not published Maven artifacts.
