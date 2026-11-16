# Publishing (1.5.0)

## Maven Local (default)

```bash
./gradlew publishToMavenLocal
```

Artifacts land under `~/.m2/repository/io/kottie/` with classifiers `sources` where configured.

Consumer example:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {
    compileOnly("io.kottie:kottie-api:1.5.0")
}
```

## External repository

```bash
./gradlew publish \
  -PpublishRepoUrl=https://maven.example.com/releases \
  -PpublishRepoUser=USER \
  -PpublishRepoPassword=TOKEN
```

Do not commit credentials. Prefer environment-backed Gradle properties in CI.

## Modules typically published

| Module | Coordinate |
|--------|------------|
| kottie-api | `io.kottie:kottie-api` |
| kottie-core | `io.kottie:kottie-core` |
| kottie-coroutines | `io.kottie:kottie-coroutines` |
| kottie-server-spi | `io.kottie:kottie-server-spi` |
| kottie-ecs-runtime | `io.kottie:kottie-ecs-runtime` |
| kottie-config | `io.kottie:kottie-config` |
| kottie-serialization | `io.kottie:kottie-serialization` |
| kottie-storage | `io.kottie:kottie-storage` |
| kottie-packets | `io.kottie:kottie-packets` |
| kottie-network | `io.kottie:kottie-network` |
| kottie-adapter-paper | `io.kottie:kottie-adapter-paper` |
| kottie-cluster | `io.kottie:kottie-cluster` |

`kottie-testkit` and `examples/*` are optional to publish.

## Release gate

```bash
./scripts/release-check.sh
```
