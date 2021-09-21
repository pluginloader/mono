package disableanycraft

import org.bukkit.event.inventory.*
import pluginloader.api.Listener
import pluginloader.api.cancel

@Listener
internal fun craft(event: CraftItemEvent){
    event.cancel()
}

@Listener
internal fun click(event: InventoryClickEvent){
    if(event.clickedInventory == null)return
    if(event.clickedInventory.type == InventoryType.CRAFTING)event.cancel()
}

@Listener
internal fun drag(event: InventoryDragEvent){
    if(event.inventory == null)return
    if(event.inventory.type == InventoryType.CRAFTING)event.cancel()
}