package itemcommand

import cmdexec.Commands
import configs.conf
import kotlinx.serialization.Serializable
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import pluginloader.api.*
import pluginloader.api.bukkit.NBT

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    val plu = this
    listener<PlayerInteractEvent>{
        if(hand == EquipmentSlot.OFF_HAND)return@listener
        val nbt = NBT.string(player.inventory.itemInMainHand ?: return@listener, "cmd") ?: return@listener
        val loc = player.location
        config.mapping[nbt]?.exec(plu, player){
            replace("%cords%", "${loc.world.name},${loc.blockX},${loc.blockY},${loc.blockZ}")
        }
    }
}

@Serializable
internal class Config{val mapping = mapOf("cmd" to Commands("cmd %player%"))}