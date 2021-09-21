package disablepearl

import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.ProjectileLaunchEvent
import pluginloader.api.Listener
import pluginloader.api.cancel

@Listener(EventPriority.MONITOR)
internal fun onPlayerShoot(event: ProjectileLaunchEvent) {
    if (event.entityType != EntityType.ENDER_PEARL) return
    val projectile = event.entity
    if (projectile == null || projectile.shooter !is Player) return
    event.cancel()
}