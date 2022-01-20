package setnbt

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import pluginloader.api.*
import pluginloader.api.bukkit.NBT
import pluginloader.api.bukkit.NBT.Companion.readNBT
import pluginloader.api.bukkit.NBT.Companion.writeNBT
import java.lang.reflect.Method

private const val prefix = "§8[§aPlu§8]§f"

@Command("setnbt", op = true)
internal fun cmd(player: Player, args: Args){
    if(args.isEmpty()){
        player.sendMessage("$prefix Usage: §6/setnbt nbt:1§f, or: §6/setnbt nbt:\"str\"")
        return
    }
    val item = player.inventory.itemInMainHand
    val str = args.joinToString(" ")
    if(str.startsWith("{")){
        caching {
            val containerClazz = Class.forName("pluginloader.internal.nbtapi.NBTContainer")
            val newStr = "{id:\"minecraft:stone\",Count:1b,tag:{" + str.substring(1, str.length - 1) + "}}"
            val container = containerClazz.getConstructor(String::class.java).newInstance(newStr)
            var method: Method? = null
            Class.forName("pluginloader.internal.nbtapi.NBTItem").declaredMethods.forEach{
                if(it.name == "convertNBTtoItem")method = it
            }
            val i = method!!.invoke(null, container) as ItemStack
            val nbt = i.readNBT()
            nbt.writeTo(item)
            player.sendMessage("$prefix Inserted input nbt to item")
        }.nonNull {
            it.printStackTrace()
            player.sendMessage("$prefix Some errors")
        }
        return
    }
    val sep = str.indexOf(':')
    val key = str.substring(0, sep)
    val value = str.substring(sep + 1, str.length)
    if(value[0] == '"' && value[value.length - 1] == '"'){
        val setValue = value.substring(1, value.length - 1)
        NBT.setString(item, key, setValue)
        player.sendMessage("$prefix Set §6'$key'§f to string §6'$setValue'")
        return
    }
    try{
        NBT.setInt(item, key, value.toInt())
        player.sendMessage("$prefix Set §6'$key'§f to int §6'$value'")
    }catch (ex: Throwable){
        try {
            NBT.setDouble(item, key, value.toDouble())
            player.sendMessage("$prefix Set §6'$key'§f to double §6'$value'")
        }catch (ex: Throwable){
            ex.printStackTrace()
            player.sendMessage("$prefix§c Error :/")
        }
    }
}