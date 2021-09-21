package trueop

import configs.Conf
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerJoinEvent
import playerinfo.PlayerUUIDByNick
import pluginloader.api.*
import spi.SPI

@Conf
internal var config = Config()

private lateinit var spi: SPI<Boolean>

@Load
internal fun load(plugin: LoaderPlugin){
    spi = SPI.get(plugin, config.table, Boolean.serializer())
}

internal const val prefix = "§8[§aPlu§8]§f"

@Command("trueop", op = true)
internal fun command(sender: Sender, args: Args){
    if(args.isEmpty()){
        sender.sendMessage("$prefix Usage: §6/trueop [player]")
        return
    }
    val uuid = PlayerUUIDByNick(args[0])
    if(uuid == null){
        sender.sendMessage("$prefix §cPlayer §6'${args[0]}'§c not found")
        return
    }
    spi.load(uuid){
        if(it != null && it){
            spi.remove(uuid)
            sender.sendMessage("$prefix Removed")
        }else{
            spi.save(uuid, true)
            Bukkit.getPlayer(uuid).nonNull{pl -> pl.isOp = true}
            sender.sendMessage("$prefix Added")
        }
    }
}

@Listener
internal fun onJoin(event: PlayerJoinEvent){
    val uuid = event.uuid
    spi.load(uuid){
        if(it != null && it) Bukkit.getPlayer(uuid).nonNull{pl -> pl.isOp = true}
    }
}

@Serializable
internal class Config(val table: String = "trueop")