package softeco

import booster.Booster
import booster.booster
import configs.Conf
import kotlinx.serialization.Serializable
import money.Money
import org.bukkit.Bukkit
import pluginloader.api.*
import readablelong.readable
import softeco.Config
import softeco.config

@Conf
internal var config = Config()

private const val prefix = "§8[§aPlu§8]§f"

internal lateinit var booster: Booster

@Load
internal fun load(plugin: Plugin){
    booster = plugin.booster("money")
}

@Command("softeco", op = true)
internal fun softeco(sender: Sender, args: Args){
    if(args.isEmpty()){
        sender.sendMessage("$prefix Usage: §6/softeco [player] [money]")
        return
    }
    val player = Bukkit.getPlayer(args[0])
    if(player == null){
        sender.sendMessage("$prefix§c Player §6'${args[0]}'§c not found")
        return
    }
    var money = args[1].toDoubleOrNull()
    if(money == null){
        sender.sendMessage("$prefix§c §6'${args[1]}'§c not a number")
        return
    }
    if(args.size > 2 && args[2] == "true")money = booster.calculate(player.uuid, money)
    Money.deposit(player, money)
    player.sendActionBar(config.message.replace("%money%", money.toLong().readable()))
}

@Serializable
internal class Config(val message: String = "§7Выдано %money% монет")