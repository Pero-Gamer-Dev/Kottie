# UI services & plugin loader (2.6)

## Scoreboard / boss bar / titles

| API | Host maps to |
|-----|----------------|
| `ScoreboardService` | Bukkit Scoreboard / Adventure sidebar |
| `BossBarService` | Bukkit BossBar / Adventure BossBar |
| `TitleService` | `Player.showTitle` / Adventure |

In-memory implementations record state for tests; hosts should forward mutations to the live player connection on the server thread.

## Plugin loader skeleton

`PluginLoader` / `DefaultPluginLoader`:

- Register metadata + optional `PluginLifecycle` instance
- Topological enable order (`depend` / `softDepend`)
- Cycle detection
- Enable/disable all

**Not included:** isolated `PluginClassLoader`, jar scanning, paper-plugin.yml bootstrap, dependency download.

```kotlin
kottie.plugins.register(
  PluginDescription(id = "shop", name = "Shop", version = "1.0", depend = listOf("economy")),
  shopLifecycle
)
kottie.plugins.enableAll()
```
