package disableanycraft

import org.bukkit.event.inventory.*
import pluginloader.api.*

@Load
internal fun Plugin.load(){
    listener<CraftItemEvent>{cancel()}
    listener<InventoryClickEvent>{if(clickedInventory?.type == InventoryType.CRAFTING)cancel()}
    listener<InventoryDragEvent>{if(inventory?.type == InventoryType.CRAFTING)cancel()}
}