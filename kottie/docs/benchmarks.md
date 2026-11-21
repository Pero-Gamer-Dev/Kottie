# Benchmarks (2.1)

## Microbench helper

`io.kottie.bench.Microbench` runs simple `System.nanoTime` loops (not JMH).

```kotlin
val r = Microbench.measure("ecs-set-get", 50_000) {
    entity.set(HealthComponent.KEY, HealthComponent(20.0, 20.0))
    entity.get(HealthComponent.KEY)
}
println(Microbench.format(r))
```

## When publishing numbers

Always include: JDK version, OS, CPU, commit hash, iteration count, warmup.

## Not claimed

2.1 does not publish official ops/s tables. Use Microbench locally or wire JMH in your fork.
