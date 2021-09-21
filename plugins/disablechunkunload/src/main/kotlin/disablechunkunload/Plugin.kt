package disablechunkunload

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.world.ChunkUnloadEvent
import pluginloader.api.Listener
import pluginloader.api.cancel

@Listener
internal fun unload(event: ChunkUnloadEvent) {
    event.cancel()
}