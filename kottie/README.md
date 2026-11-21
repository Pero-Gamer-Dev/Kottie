# Kottie 2.0.1

Modern, asynchronous-first **API framework** for Minecraft Java Edition plugins (Kotlin / Java).

**Status:** `2.0.1` — patch over 2.0.0 (CI + compile fixes).

## 2.0.1 fixes

| Issue | Fix |
|-------|-----|
| CI Build (JDK 17/21) failing | Hardened `.github/workflows/ci.yml` (`chmod +x gradlew`, Temurin cache, `--stacktrace`) |
| `ComponentKey` value-class errors | `ComponentKey` is a **data class** (supports `vararg` queries + `@JvmField` keys) |
| Missing `pluginManagement` | Added to `settings.gradle.kts` |
| `:examples` aggregate project | `examples/build.gradle.kts` shell; root skips applying Kotlin to `:examples` |
| Java toolchain probe on partial JREs | Explicit Java 17 `release` / sourceCompatibility |

## Build & CI

```bash
chmod +x gradlew
bash scripts/verify-layout.sh
./gradlew assemble --no-daemon --stacktrace
./gradlew :kottie-testkit:test --no-daemon --stacktrace
./gradlew publishToMavenLocal
# or
./scripts/release-check.sh
```

## Dependency

```kotlin
repositories { mavenLocal(); mavenCentral() }
dependencies {
    compileOnly("io.kottie:kottie-api:2.0.1")
}
```

## Docs

See `docs/RELEASE_2.0.md`, `docs/compatibility.md`, `docs/architecture.md`.

## License

MIT — see `LICENSE`.
