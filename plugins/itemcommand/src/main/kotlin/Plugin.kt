package itemcommand

import cmdexec.Commands
import configs.Conf
import kotlinx.serialization.Serializable
import org.apache.logging.log4j.core.util.Loader
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import pluginloader.api.Listener
import pluginloader.api.Load
import pluginloader.api.LoaderPlugin
import pluginloader.api.bukkit.NBT

@Conf
internal var conf = Config()
private lateinit var plu: LoaderPlugin

@Load
internal fun load(plugin: LoaderPlugin){
    plu = plugin
}

@Listener
internal fun use(event: PlayerInteractEvent){
    if(event.hand == EquipmentSlot.OFF_HAND)return
    val nbt = NBT.string(event.player.inventory.itemInMainHand ?: return, "cmd") ?: return
    val loc = event.player.location
    conf.mapping[nbt]?.exec(plu, event.player){
        replace("%cords%", "${loc.world.name},${loc.blockX},${loc.blockY},${loc.blockZ}")
    }
}

@Serializable
internal class Config(val mapping: Map<String, Commands> = mapOf("cmd" to Commands(listOf("cmd %player%"))))