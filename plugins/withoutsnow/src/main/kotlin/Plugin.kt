package withoutsnow

import org.bukkit.Material
import org.bukkit.event.block.BlockFormEvent
import pluginloader.api.Listener
import pluginloader.api.cancel

@Listener
internal fun event(event: BlockFormEvent){
    if(event.getNewState().type == Material.SNOW)event.cancel()
}