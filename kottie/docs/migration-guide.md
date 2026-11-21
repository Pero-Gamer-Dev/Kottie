# Migration guide

## 2.0.0 → 2.0.1

- **Patch only** — bump version to `2.0.1`.
- `ComponentKey` is no longer an inline value class; it is a `data class`.
  - Equality remains by `id`.
  - Java: `ManaComponent.KEY` / `@JvmField` still valid.
  - If you relied on value-class identity quirks, retest equality/map keys (behavior should be clearer).
- CI workflow and Gradle settings fixes are repo-side only.

## 1.x → 2.0

- Prefer `io.kottie.api.*` in plugins.
- Optional adapters: `kottie-adapter-paper`, `kottie-adapter-velocity`.
- Install: `./gradlew publishToMavenLocal` then `compileOnly("io.kottie:kottie-api:2.0.1")`.
