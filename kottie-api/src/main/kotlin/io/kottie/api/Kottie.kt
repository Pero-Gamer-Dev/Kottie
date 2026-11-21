package io.kottie.api

import io.kottie.api.command.CommandManager
import io.kottie.api.inventory.InventoryService
import io.kottie.api.scoreboard.ScoreboardService
import io.kottie.api.bossbar.BossBarService
import io.kottie.api.title.TitleService
import io.kottie.api.plugin.PluginLoader
import io.kottie.api.player.PlayerService
import io.kottie.api.world.WorldService
import io.kottie.api.config.ConfigService
import io.kottie.api.ecs.ComponentRegistry
import io.kottie.api.ecs.EntityStore
import io.kottie.api.event.EventBus
import io.kottie.api.lifecycle.PluginLifecycle
import io.kottie.api.messaging.Messenger
import io.kottie.api.permission.PermissionService
import io.kottie.api.scheduler.RegionScheduler
import io.kottie.api.scheduler.ServerScheduler
import io.kottie.api.service.ServiceRegistry

/**
 * Main entry point facade for the Kottie platform.
 * Obtained from the runtime after successful bootstrap.
 */
interface Kottie {
    val eventBus: EventBus
    val scheduler: ServerScheduler
    val lifecycle: PluginLifecycle
    val entityStore: EntityStore
    val componentRegistry: ComponentRegistry
    val config: ConfigService

    /** Command registration and dispatch. */
    val commands: CommandManager

    /** Permission checks (in-memory or host-bridged). */
    val permissions: PermissionService

    /** Player messaging / broadcast. */
    val messenger: Messenger

    /** Service registry (platform services). */
    val services: ServiceRegistry

    /**
     * Region-aware scheduling (Folia-ready). Default implementations may
     * fall back to the global server thread.
     */
    val regions: RegionScheduler

    /** Online players directory. */
    val players: PlayerService

    /** World directory. */
    val worlds: WorldService

    /** Inventory snapshots / apply. */
    val inventories: InventoryService

    val scoreboards: ScoreboardService
    val bossBars: BossBarService
    val titles: TitleService
    val plugins: PluginLoader

    companion object {
        @Volatile
        private var instance: Kottie? = null

        fun get(): Kottie =
            instance ?: error("Kottie runtime has not been initialized")

        fun setInstance(kottie: Kottie) {
            instance = kottie
        }

        fun clearInstance() {
            instance = null
        }
    }
}
