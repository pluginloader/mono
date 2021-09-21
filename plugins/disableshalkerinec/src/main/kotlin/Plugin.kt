package disableshalkerinec

import cmdhelp.execute
import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import pluginloader.api.Listener

@Conf
internal var config = Config()

@Listener
internal fun onDrag(event: InventoryDragEvent) {
    if (event.view.type != InventoryType.ENDER_CHEST) return
    var stack = event.cursor
    if (stack != null && stack.type != null && isShulker(stack.type)) {
        event.isCancelled = true
        config.commandsOnTry.execute(event.whoClicked as Player)
        return
    }
    stack = event.oldCursor
    if (stack != null && stack.type != null && isShulker(stack.type)) {
        config.commandsOnTry.execute(event.whoClicked as Player)
        event.isCancelled = true
    }
}

@Listener
internal fun onClick(event: InventoryClickEvent) {
    if (event.clickedInventory == null) return
    if (event.inventory.type == InventoryType.ENDER_CHEST) {
        if(event.click == ClickType.LEFT && event.clickedInventory.type == InventoryType.PLAYER)return
        var stack = event.cursor
        if (event.click == ClickType.NUMBER_KEY) stack = event.whoClicked.inventory.getItem(event.hotbarButton)
        else if (event.isShiftClick) stack = event.currentItem
        if (stack == null || stack.type == null) return
        if (isShulker(stack.type)) {
            config.commandsOnTry.execute(event.whoClicked as Player)
            event.isCancelled = true
        }
    }
}

@Serializable
internal class Config(val commandsOnTry: List<String> = listOf("text %player% shulker blocked"))

private fun isShulker(material: Material): Boolean {
    return when (material) {
        Material.BLACK_SHULKER_BOX,
        Material.BLUE_SHULKER_BOX,
        Material.BROWN_SHULKER_BOX,
        Material.CYAN_SHULKER_BOX,
        Material.GRAY_SHULKER_BOX,
        Material.GREEN_SHULKER_BOX,
        Material.LIGHT_BLUE_SHULKER_BOX,
        Material.LIME_SHULKER_BOX,
        Material.MAGENTA_SHULKER_BOX,
        Material.ORANGE_SHULKER_BOX,
        Material.PINK_SHULKER_BOX,
        Material.PURPLE_SHULKER_BOX,
        Material.RED_SHULKER_BOX,
        Material.SILVER_SHULKER_BOX,
        Material.WHITE_SHULKER_BOX,
        Material.YELLOW_SHULKER_BOX -> true
        else -> false
    }
}