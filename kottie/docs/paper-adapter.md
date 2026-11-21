# Paper adapter (1.1.0)

## Goal

Bridge a Paper/Bukkit server into Kottie without putting NMS types on the public API.

## Supported version set (declared)

`1.20.4`, `1.21`, `1.21.1` — extend `supportedVersions` as you verify.

## Host plugin wiring

1. Create a `PaperServerScheduler` with Bukkit’s main-thread executor.
2. Build `PaperServerAdapter.forPaperHost(...)`.
3. Start `KottieRuntime` with that adapter.
4. `attachEventBridge` and forward Bukkit join/quit into the bridge.

Pseudo-wiring (host module has Paper on `compileOnly`):

- Main thread: `Bukkit.getScheduler().runTask(plugin, runnable)`
- Primary check: `Bukkit.isPrimaryThread()`
- Join: `bridge.onPlayerJoin(player.uniqueId, player.name)`
- Quit: `bridge.onPlayerQuit(player.uniqueId, player.name)`

## What 1.1 does not do

- Full packet interception against Paper’s Netty pipeline  
- Automatic registration of Bukkit listeners (host still registers them)  
- Multi-version NMS abstraction  

Those remain 1.2+ / optional modules.
