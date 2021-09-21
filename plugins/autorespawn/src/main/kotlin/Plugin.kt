package autorespawn

import org.bukkit.event.entity.PlayerDeathEvent
import pluginloader.api.Listener
import pluginloader.api.runTaskLater

@Listener
internal fun onDeath(event: PlayerDeathEvent){
    runTaskLater(1){event.entity.spigot().respawn()}
}