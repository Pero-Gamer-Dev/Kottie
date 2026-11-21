package io.kottie.api.messaging

import java.util.UUID

/**
 * Simple audience messaging. Hosts map to Adventure / legacy chat.
 */
interface Messenger {
    fun send(playerId: UUID, message: String)
    fun broadcast(message: String)
    fun sendActionBar(playerId: UUID, message: String) {
        send(playerId, message)
    }
}

/**
 * Collects outbound messages in tests when no host sink is attached.
 */
class RecordingMessenger : Messenger {
    data class Msg(val playerId: UUID?, val message: String, val kind: Kind)
    enum class Kind { CHAT, BROADCAST, ACTION_BAR }

    private val _messages = java.util.concurrent.CopyOnWriteArrayList<Msg>()
    val messages: List<Msg> get() = _messages.toList()

    override fun send(playerId: UUID, message: String) {
        _messages.add(Msg(playerId, message, Kind.CHAT))
    }

    override fun broadcast(message: String) {
        _messages.add(Msg(null, message, Kind.BROADCAST))
    }

    override fun sendActionBar(playerId: UUID, message: String) {
        _messages.add(Msg(playerId, message, Kind.ACTION_BAR))
    }

    fun clear() = _messages.clear()
}
