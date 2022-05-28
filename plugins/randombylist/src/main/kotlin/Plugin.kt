package randombylist

import cmdexec.Commands
import configs.Conf
import configs.conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import pluginloader.api.*
import kotlin.random.Random

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    val plu = this
    command(checkOp = true, name = "randombylist"){sender, args ->
        args.use(sender, 1, "randombylist [type] {player}") ?: return@command
        val type = args.cantFind(sender, config.randomCommands[args[0]], "random command", args[0]) ?: return@command
        val player = if(args.size == 1) null else args.player(sender, 1) ?: return@command
        var i = Random.nextInt(type.keys.sum() + 1)
        sender.sendMessage("$prefix Execute §6'${args[1]}'")
        type.forEach{
            i -= it.key
            if(i <= 0){
                it.value.exec(plu){replace("%player%", player?.name ?: "%%%")}
                return@command
            }
        }
    }
}

@Serializable
internal class Config{val randomCommands = mapOf("nekopara" to mapOf(
    1000000 to Commands("text %player% 1"),
    1000001 to Commands("text %player% 2")
))}