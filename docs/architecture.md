# Kottie Architecture Notes (0.1.0-alpha)

This tree implements the **recommended direction** from the Complete Architecture Blueprint:

- Layered separation: `api` → `core` → optional adapters / SPI
- Explicit lifecycle states and owned scopes
- Safe scheduler abstraction
- Event bus with subscription ownership and handler isolation
- Basic typed ECS (Kottie-managed only)
- Atomic config snapshot holder

## What is deliberately *not* present yet

- Real Paper/Bukkit adapter (SPI is ready)
- Packet interception
- Redis / JDBC bridges
- Full config hot-reload + validation + migration
- Versioned serialization codecs
- Production-grade decision-event delivery guarantees

These are scheduled for later phases exactly as described in the blueprint.
