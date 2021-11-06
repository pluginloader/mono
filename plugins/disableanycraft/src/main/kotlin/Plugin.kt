package disableanycraft

import org.bukkit.entity.Player
import org.bukkit.event.inventory.*
import pluginloader.api.Listener
import pluginloader.api.cancel
import pluginloader.api.nonNull
import pluginloader.api.runTaskLater

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
    if(event.inventory.type == InventoryType.CRAFTING){
        event.cancel()
    }
}

@Listener
internal fun close(event: InventoryCloseEvent){
    val player = event.player
    event.view.cursor.nonNull {
        player.inventory.addItem(it)
        event.view.cursor = null
    }
}

@Listener
internal fun open(event: InventoryOpenEvent){
    val player = event.player
    event.view.cursor.nonNull {
        player.inventory.addItem(it)
        event.view.cursor = null
    }
}