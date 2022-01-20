package chancecmd

import cmdexec.Commands
import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import pluginloader.api.*
import kotlin.random.Random

@Conf
internal var config = Config()
private lateinit var plu: LoaderPlugin

@Load
internal fun load(plugin: LoaderPlugin){
    plu = plugin
}

@Command("chancecmd", op = true)
internal fun chancecmd(sender: Sender, args: Args){
    if(args.isEmpty()){
        sender.sendMessage("§8[§aPlu§8]§f Usage: /chancecmd [player] [type] [chance 0.0-100.0]")
        return
    }
    val player = Bukkit.getPlayer(args[0]) ?: return
    val cmd = config.mapping[args[1]] ?: return
    val chance = args[2].toDouble()
    if(chance <= Random.nextDouble(0.0, 100.0))return
    cmd.exec(plu, player)
}

@Serializable
internal class Config(val mapping: Map<String, Commands> = mapOf("type" to Commands(listOf("text %player% random!"))))