package randombylist

import cmdhelp.execute
import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import pluginloader.api.Args
import pluginloader.api.Color
import pluginloader.api.Command
import pluginloader.api.Sender
import kotlin.random.Random

@Conf
internal var config = Config()

private val prefix = "§8[§aPlu§8]§f"

@Command("randombylist", op = true)
internal fun cmd(sender: Sender, args: Args){
    if(args.isEmpty()){
        sender.sendMessage("$prefix Usage: §6/randombylist [type] {player}")
        return
    }
    val type = config.randomCommands[args[0]] ?: return
    val player = if(args.size == 1) null else Bukkit.getPlayer(args[1])
    var i = Random.nextInt(type.keys.sum() + 1)
    sender.sendMessage("$prefix Execute §6'${args[1]}'")
    type.forEach{
        i -= it.key
        if(i <= 0){
            it.value.execute{cmd -> if(player != null)cmd.replace("%player%", player.name) else cmd}
            return
        }
    }
}

@Serializable
internal class Config(val randomCommands: Map<String, Map<Int, List<String>>> =
    mapOf("nekopara" to mapOf(1000000 to listOf("text %player% 1"), 1000001 to listOf("text %player% 2"))))