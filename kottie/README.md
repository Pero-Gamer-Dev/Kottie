# Kottie 2.6.0

Modern, asynchronous-first **API framework** for Minecraft Java Edition plugins (Kotlin / Java).

**Status:** `2.6.0` — scoreboard / boss bar / titles + plugin loader skeleton.

## What’s new in 2.6.0

- **`ScoreboardService`** — objectives, scores, per-player slots
- **`BossBarService`** — progress bars, viewers, colors/styles
- **`TitleService`** — title/subtitle/times (`RecordingTitleService` for tests)
- **`PluginLoader`** — register, dependency order, enable/disable lifecycle
- Docs: `docs/ui-and-plugins.md`

Still not a Paper fork: no isolated plugin classloader, no live scoreboard packets by themselves.

## Build

```bash
chmod +x gradlew
./gradlew assemble :kottie-testkit:test --no-daemon --stacktrace
```

## Dependency

```kotlin
compileOnly("io.kottie:kottie-api:2.6.0")
```

## License

MIT — see `LICENSE`.
