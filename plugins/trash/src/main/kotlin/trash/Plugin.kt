package trash

import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pluginloader.api.Command

@Conf
internal var config = Config()

@Command("trash")
internal fun cmd(player: Player){
    player.openInventory(Bukkit.createInventory(null, config.inventorySize, config.inventoryName))
}

@Serializable
internal class Config(val inventorySize: Int = 27, val inventoryName: String = "Trashcan")