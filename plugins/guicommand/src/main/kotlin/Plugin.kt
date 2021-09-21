package guicommand

import cmdhelp.execute
import configs.Conf
import gui.ConfigInventory
import guicommand.Config
import guicommand.config
import kotlinx.serialization.Serializable
import money.Money
import morestats.Stats
import morestats.StatsAPI
import org.bukkit.Material
import org.bukkit.entity.Player
import pluginloader.api.*

@StatsAPI
internal lateinit var stats: Stats

@Conf
internal var config = Config()

@Load
internal fun load(plugin: Plugin){
    config.commands.forEach{
        plugin.registerCommand(it.key, {player, args ->
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
                                entry.value.error.execute(player)
                                return@fill
                            }
                        }else{
                            if(stats.get(player.uuid, entry.key) < entry.value.count){
                                entry.value.error.execute(player)
                                return@fill
                            }
                        }
                    }

                    item.needStats?.get("money").nonNull{stat -> Money.withdraw(player, stat.count)}
                    item.commands.execute(player)
                }
            }
            active.open(player)
        })
    }
}

@Serializable
internal class Config(
    val commands: Map<String, GUI> = mapOf("guicommand" to GUI())
)

@Serializable
internal class GUI(
    val inventory: ConfigInventory = ConfigInventory(context = listOf("----a----")),
    val items: List<CommandItem> = listOf(CommandItem()),
    val arg: String? = null,
)

@Serializable
internal class CommandItem(
    val char: Char = 'a',
    val item: Item = Item().type(Material.GOLD_SWORD).name("§6Нажми"),
    val commands: List<String> = listOf("!onclick"),
    val needStats: Map<String, Stat>? = mapOf("level" to Stat())
)

@Serializable
internal class Stat(
    val count: Double = 1.0,
    val error: List<String> = listOf("et %player% Не хватает уровня")
)