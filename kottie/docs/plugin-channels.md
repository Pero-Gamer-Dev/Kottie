# Plugin channel packet bridge (2.3)

## Goal

Hook **Bukkit/Paper plugin messaging** into Kottie `PacketBus` without NMS and without a Paper compile dependency inside Kottie.

## Flow

```
Player → plugin message → host listener
       → PluginChannelPacketBridge.onPluginMessageReceived
       → PacketBus (INBOUND handlers)

PacketBus OUTBOUND handlers
       → outboundSink → player.sendPluginMessage(...)
```

## Host sketch

```kotlin
val bridge = PluginChannelPacketBridge()
bridge.registerChannel("kottie:sync")
bridge.outboundSink = PluginChannelPacketBridge.OutboundSink { channel, playerId, data ->
    val player = server.getPlayer(playerId) ?: return@OutboundSink
    player.sendPluginMessage(plugin, channel, data)
}

// IncomingPluginMessageEvent / Messenger listener:
bridge.onPluginMessageReceived(channel, player.uniqueId, message)

// Subscribe in feature code:
bridge.bus.subscribe(bridge.typeFor("kottie:sync")!!, PacketDirection.INBOUND) { ctx ->
    val payload = ctx.event.payload as PluginMessagePayload
    // decode payload.data
}
```

## Rules

- Do not block the netty/main thread in handlers
- Use `cancel()` to drop; `replace()` to mutate outbound bytes
- Still **not** a full NMS packet interceptor
