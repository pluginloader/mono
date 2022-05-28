package setnbt

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import pluginloader.api.*
import pluginloader.api.bukkit.NBT
import pluginloader.api.bukkit.NBT.Companion.readNBT
import pluginloader.api.bukkit.NBT.Companion.writeNBT
import java.lang.reflect.Method

@Command("setnbt", op = true)
internal fun cmd(player: Player, args: Args){
    args.use(player, 1, "setnbt nbt:1§f, or: §6/setnbt nbt:\"str\"")
    val item = player.inventory.itemInMainHand
    val str = args.joinToString(" ")
    val sep = str.indexOf(':')
    val key = str.substring(0, sep)
    val value = str.substring(sep + 1, str.length)
    if(value[0] == '"' && value[value.length - 1] == '"'){
        val setValue = value.substring(1, value.length - 1)
        NBT.setString(item, key, setValue)
        player.sendMessage("$prefix Set §6$key§f to string §6$setValue")
        return
    }
    try{
        NBT.setInt(item, key, value.toInt())
        player.sendMessage("$prefix Set §6$key§f to int §6$value")
    }catch (ex: Throwable){
        try {
            NBT.setDouble(item, key, value.toDouble())
            player.sendMessage("$prefix Set §6$key§f to double §6$value")
        }catch (ex: Throwable){
            ex.printStackTrace()
            player.sendMessage("$prefix§c Error :/")
        }
    }
}