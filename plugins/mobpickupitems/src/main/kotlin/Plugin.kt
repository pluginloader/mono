package mobpickupitems

import org.bukkit.entity.EntityType
import org.bukkit.event.entity.EntityPickupItemEvent
import pluginloader.api.Listener
import pluginloader.api.cancel

@Listener
internal fun event(event: EntityPickupItemEvent){
    if(event.entityType == EntityType.PLAYER)return
    event.cancel()
}