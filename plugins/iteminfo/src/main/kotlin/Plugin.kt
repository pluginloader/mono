package iteminfo

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryOpenEvent
import pluginloader.api.Color
import pluginloader.api.Command

private const val prefix = "§8[§aPlu§8]§f"

@Command("iteminfo", op = true)
internal fun cmd(player: Player){
    val item = player.inventory.itemInMainHand
    if(item == null || item.type == Material.AIR){
        player.sendMessage("$prefix Empty")
        return
    }
    player.sendMessage("$prefix Type: §6'${item.type}'§f, meta: §6'${item.durability}'")
}