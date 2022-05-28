package iteminfo

import org.bukkit.Material
import org.bukkit.entity.Player
import pluginloader.api.Command
import pluginloader.api.prefix

@Command("iteminfo", op = true)
internal fun cmd(player: Player){
    val item = player.inventory.itemInMainHand
    if(item == null || item.type == Material.AIR){
        player.sendMessage("$prefix Empty")
        return
    }
    player.sendMessage("$prefix Type: §6${item.type}§f, meta: §6${item.durability}")
}