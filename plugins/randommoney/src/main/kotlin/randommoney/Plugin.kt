package randommoney

import configs.Conf
import kotlinx.serialization.Serializable
import money.Money
import org.bukkit.Bukkit
import pluginloader.api.Args
import pluginloader.api.Color
import pluginloader.api.Command
import pluginloader.api.Sender
import kotlin.random.Random

@Conf
internal var config = Config()

private const val prefix = "§8[§aPlu§8]§f"

@Command("randommoney", op = true)
internal fun randommoney(sender: Sender, args: Args){
    if(args.size < 3){
        sender.sendMessage("$prefix Usage: §6/randommoney [player] [min] [max]")
        return
    }
    val player = Bukkit.getPlayer(args[0])
    if(player == null){
        sender.sendMessage("$prefix§c Player §6'${args[0]}'§c not found")
        return
    }
    val one = args[1].toDoubleOrNull()
    if(one == null){
        sender.sendMessage("$prefix §6'${args[1]}'§c not a number")
        return
    }
    val two = args[2].toDoubleOrNull()
    if(two == null){
        sender.sendMessage("$prefix §6'${args[2]}'§c not a number")
        return
    }
    val random = Random.nextDouble(args[1].toDouble(), args[2].toDouble())
    val money = (Money.get(player) / 100 * random).toLong()
    config.moneycmd.forEach{Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it.replace("%player%", player.name).replace("%money%", money.toString()))}
    sender.sendMessage("$prefix Player §6'${player.name}'§f, money §6'$money'")
}

@Serializable
internal class Config(val moneycmd: List<String> = listOf("softeco %player% %money%"))