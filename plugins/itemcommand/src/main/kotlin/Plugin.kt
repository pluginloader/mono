package itemcommand

import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import pluginloader.api.Listener
import pluginloader.api.bukkit.NBT

@Conf
internal var conf = Config()

@Listener
internal fun use(event: PlayerInteractEvent){
    if(event.hand == EquipmentSlot.OFF_HAND)return
    val nbt = NBT.string(event.player.inventory.itemInMainHand ?: return, "cmd") ?: return
    val loc = event.player.location
    conf.mapping[nbt]?.forEach{Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it
            .replace("%player%", event.player.name)
            .replace("%cords%", "${loc.world.name},${loc.blockX},${loc.blockY},${loc.blockZ}")
    )}
}

@Serializable
internal class Config(val mapping: Map<String, List<String>> = mapOf("cmd" to listOf("cmd %player%")))