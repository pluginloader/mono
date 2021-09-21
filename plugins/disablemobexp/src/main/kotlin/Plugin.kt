package disablemobexp

import org.bukkit.event.entity.EntityDeathEvent
import pluginloader.api.Listener

@Listener
internal fun event(event: EntityDeathEvent){
    event.droppedExp = 0
}