package disablecorus

import org.bukkit.Material
import org.bukkit.event.player.PlayerItemConsumeEvent
import pluginloader.api.Listener
import pluginloader.api.cancel

@Listener
internal fun eat(event: PlayerItemConsumeEvent){
    if(event.item != null && event.item.type == Material.CHORUS_FRUIT){
        event.cancel()
    }
}