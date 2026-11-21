# Platform services (2.4)

Kottie 2.4 expands the facade toward a **Paper-like service surface** while remaining a library, not a server fork.

## New on `Kottie`

| API | Purpose |
|-----|---------|
| `commands` | Register / dispatch commands, optional permission |
| `permissions` | `has` / `grant` / `revoke` |
| `messenger` | send / broadcast / action bar |
| `services` | Priority service registry |
| `regions` | Folia-ready region scheduling (fallback = server thread) |

## Example

```kotlin
kottie.commands.register(
  CommandMeta("spawn", permission = "kottie.spawn"),
  executor = { ctx ->
    kottie.messenger.send(ctx.senderId!!, "Teleporting...")
    CommandResult.SUCCESS
  }
)

kottie.regions.execute("world", 100, -200) {
  // mutate region-owned state on Folia; global thread on Paper fallback
}
```

## Host bridging

| Kottie | Paper / Folia host |
|--------|-------------------|
| CommandManager.dispatch | Brigadier / Bukkit CommandMap |
| PermissionService | Permissible / LuckPerms |
| Messenger | Audience / player.sendMessage |
| RegionScheduler | Folia RegionScheduler |

## Honest scope

2.4 is a **step toward** a complete server API for plugins. It is still **not** a replacement for Paper’s runtime, world engine, or protocol stack.
