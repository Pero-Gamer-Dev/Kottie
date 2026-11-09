# Kottie 1.0 Architecture

## Layering

```
Plugin / feature code
        ↓
io.kottie.api          (stable contracts)
        ↓
kottie-core            (lifecycle, event bus, diagnostics)
        ↓
optional modules       (ecs, config, serialization, storage, packets, network)
        ↓
ServerAdapter          (in-memory | paper skeleton | future full Paper)
        ↓
Minecraft server (when adapter is fully bound)
```

## 1.0 freeze

Public packages under `io.kottie.api.*` are the compatibility surface for 1.0.x.  
Implementation modules may still receive fixes; prefer depending on `kottie-api` from plugins.

## Roadmap after 1.0

- **1.1** — Paper adapter with real scheduler + join/quit bridge for one declared version  
- **1.2** — Optional Redis network bridge  
- **1.3** — Published Maven repository + CI version matrix  
