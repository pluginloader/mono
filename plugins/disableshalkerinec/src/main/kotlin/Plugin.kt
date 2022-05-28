package disableshalkerinec

import cmdexec.Commands
import configs.Conf
import configs.conf
import kotlinx.serialization.Serializable
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import pluginloader.api.*

@Conf
internal var config = Config()

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    val plu = this
    listener<InventoryDragEvent>{
        if (view.type != InventoryType.ENDER_CHEST) return@listener
        if (isShulker(cursor?.type)) {
            cancel()
            config.commandsOnTry.exec(plu, whoClicked as Player)
            return@listener
        }
        if (isShulker(oldCursor?.type)) {
            config.commandsOnTry.exec(plu, whoClicked as Player)
            cancel()
        }
    }
    listener<InventoryClickEvent>{
        if(clickedInventory == null || inventory.type != InventoryType.ENDER_CHEST)return@listener
        if(click == ClickType.LEFT && clickedInventory.type == InventoryType.PLAYER)return@listener
        var stack = cursor
        if (click == ClickType.NUMBER_KEY) stack = whoClicked.inventory.getItem(hotbarButton)
        else if (isShiftClick) stack = currentItem
        if (isShulker(stack?.type)) {
            config.commandsOnTry.exec(plu, whoClicked as Player)
            cancel()
        }
    }
}

@Serializable
internal class Config{val commandsOnTry = Commands("text %player% shulker blocked")}

private fun isShulker(material: Material?): Boolean {
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