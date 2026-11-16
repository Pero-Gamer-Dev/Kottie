# Migration guide — toward 2.0

## 1.x → 2.0.0

- Bump dependency version to `2.0.0`.
- Prefer `io.kottie.api.*` only in feature plugins.
- Adapters stay optional:
  - Paper hosts → `kottie-adapter-paper`
  - Velocity hosts → `kottie-adapter-velocity`
- Storage: `InMemoryRepository` still valid for tests; production should use `FileSystemRepository` or `SqlKeyValueRepository`.
- No intentional hard breaks in public API contracts introduced for 2.0 freeze; review release notes if you shadowed internal packages.

## 1.8 → 1.9

- New optional `kottie-adapter-velocity`.

## 1.6 → 1.7

- New ECS query APIs and built-in components; additive.

## Install

```bash
./gradlew publishToMavenLocal
```

```kotlin
dependencies {
    compileOnly("io.kottie:kottie-api:2.0.0")
}
```
