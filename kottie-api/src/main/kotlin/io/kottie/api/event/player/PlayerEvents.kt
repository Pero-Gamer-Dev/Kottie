package io.kottie.api.event.player

import io.kottie.api.ecs.Entity
import io.kottie.api.event.KottieEvent
import java.util.UUID

/**
 * Observation event: a player has joined. Payload is a snapshot-friendly handle.
 * Safe to process asynchronously provided no live server mutations occur
 * without going through the scheduler.
 */
data class PlayerJoinEvent(
    val playerId: UUID,
    val name: String,
    val entity: Entity
) : KottieEvent

/**
 * Observation event: a player has quit.
 */
data class PlayerQuitEvent(
    val playerId: UUID,
    val name: String
) : KottieEvent
