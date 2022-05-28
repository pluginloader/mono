package guicommand

import cmdexec.Commands
import configs.Conf
import configs.conf
import gui.ConfigInventory
import kotlinx.serialization.Serializable
import money.Money
import morestats.Stats
import morestats.StatsAPI
import org.bukkit.Material
import org.bukkit.entity.Player
import pluginloader.api.*

@StatsAPI
internal lateinit var stats: Stats

@Load
internal fun Plugin.load(){
    conf(::Config).commands.forEach{registerCommand(it.key, {player, args ->
        if (player !is Player) return@registerCommand
        if(it.value.arg != null) {
            if (args.isEmpty() || args[0] != it.value.arg) return@registerCommand
        }
        val active = it.value.inventory.active()
        it.value.items.forEach{item ->
            active.fill(item.char, item.item.item()){
                item.needStats?.forEach{entry ->
                    if(entry.key == "money"){
                        if(!Money.has(player, entry.value.count)){
                            entry.value.error.exec(this, player)
                            return@fill
                        }
                    }else{
                        if(stats.get(player.uuid, entry.key) < entry.value.count){
                            entry.value.error.exec(this, player)
                            return@fill
                        }
                    }
                }

                item.needStats?.get("money").nonNull{stat -> Money.withdraw(player, stat.count)}
                item.commands.exec(this, player)
            }
        }
        active.open(player)
    })}
}

@Serializable
internal class Config {
    val commands = mapOf("guicommand" to GUI())
}

@Serializable
internal class GUI {
    val inventory = ConfigInventory(context = listOf("----a----"))
    val items = listOf(CommandItem())
    val arg: String? = null
}

@Serializable
internal class CommandItem {
    val char = 'a'
    val item = Item().type(Material.GOLD_SWORD).name("§6Clock")
    val commands = Commands("!onclick")
    val needStats: Map<String, Stat>? = mapOf("level" to Stat())
}

@Serializable
internal class Stat {
    val count = 1.0
    val error = Commands("et %player% Not enough level")
}