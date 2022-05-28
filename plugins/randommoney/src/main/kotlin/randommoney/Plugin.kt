package randommoney

import cmdexec.Commands
import configs.conf
import kotlinx.serialization.Serializable
import money.Money
import org.bukkit.entity.Player
import pluginloader.api.*
import kotlin.random.Random

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    val plu = this
    command(checkOp = true, name = "randommoney"){sender, args ->
        args.use(sender, 3, "randommoney [player] [min] [max]") ?: return@command
        val player = args.player(sender, 0) ?: return@command
        val min = args.double(sender, 1) ?: return@command
        val max = args.double(sender, 2) ?: return@command
        val random = Random.nextDouble(min, max)
        val money = (Money.get(player) / 100 * random).toLong()
        config.moneycmd.exec(plu, player){replace("%money%", money.toString())}
        if(sender is Player)sender.sendMessage("$prefix Player §6${player.name}§f, money §6$money")
    }
}

@Serializable
internal class Config{val moneycmd = Commands("softeco %player% %money%") }