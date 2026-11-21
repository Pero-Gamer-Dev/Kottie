# Player, world & inventory (2.5)

## Design

All types are **snapshots / views**, not live Bukkit objects.

| API | Role |
|-----|------|
| `PlayerService` | Online directory, teleport request, update/remove |
| `PlayerView` | id, name, location, health, game mode |
| `WorldService` | World list, lookup, default world |
| `LocationSnapshot` | world + x/y/z/yaw/pitch |
| `InventoryService` | snapshot / apply player inventory |
| `ItemStackSnapshot` | type id + amount + string meta |

## Host bridging

```kotlin
// On Bukkit PlayerJoinEvent
kottie.players.update(MutablePlayerView(
  uniqueId = player.uniqueId,
  name = player.name,
  location = LocationSnapshot(
    player.world.name,
    player.location.x, player.location.y, player.location.z,
    player.location.yaw, player.location.pitch
  )
))

// Teleport: kottie.players.teleport → host runs player.teleport on region thread
```

## Inventory

Hosts convert `ItemStackSnapshot` ↔ Bukkit `ItemStack` (including NBT if needed).
Kottie stores portable snapshots only.
