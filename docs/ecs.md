# ECS (1.7.0)

## Model

- Metadata layer only — does not replace Bukkit/Paper entities
- One component per `ComponentKey`
- Prefer immutable data classes

## Built-in components

| Component | Key | Purpose |
|-----------|-----|---------|
| `ManaComponent` | `kottie:mana` | Example resource |
| `HealthComponent` | `kottie:health` | HP with damage/heal helpers |
| `ShieldComponent` | `kottie:shield` | Absorb then pass remaining damage |
| `CooldownComponent` | `kottie:cooldown` | Named remaining-ms map + tick |
| `StatsComponent` | `kottie:stats` | base + flat + percent |

## Queries

```kotlin
val combatants = store.query(
  EntityQuery.allOf(HealthComponent.KEY, StatsComponent.KEY)
)

val unshielded = store.query(
  EntityQuery(
    required = setOf(HealthComponent.KEY),
    excluded = setOf(ShieldComponent.KEY)
  )
)
```

`DefaultEntityStore` maintains a component→entity index so queries intersect sets instead of always scanning all entities.

## Systems

```kotlin
val runner = SystemRunner(store)
runner.register(EntityQuery.allOf(CooldownComponent.KEY)) { entities, deltaMs ->
  entities.forEach { e ->
    e.get(CooldownComponent.KEY)?.let { e.set(CooldownComponent.KEY, it.tick(deltaMs)) }
  }
}
// from server scheduler:
runner.tick(50)
```
