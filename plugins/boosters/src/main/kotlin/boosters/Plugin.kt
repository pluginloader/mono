package boosters

import booster.boostInfo
import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pluginloader.api.*

@Conf
internal var config = Config()
private lateinit var plugin: LoaderPlugin

@Load
internal fun load(plu: LoaderPlugin){
    plugin = plu
}

@Command("boosters")
internal fun cmd(player: Player, args: Args){
    var pl = player
    if(args.isNotEmpty() && player.isOp){
        pl = Bukkit.getPlayer(args[0])
    }
    plugin.boostInfo(pl.uuid).nonNull { list ->
        val infinity = HashMap<String, Double>()
        val timed = ArrayList<Triple<String, Double, Int>>()
        var ok = false
        list.forEach {
            if(it.end())return@forEach
            config.mapping[it.type] ?: return@forEach
            ok = true
            if(it.infinity){
                infinity[it.type] = infinity.getOrDefault(it.type, 0.0) + it.boost
            }else{
                timed.add(Triple(it.type, it.boost, it.timeToEndInMinutes()))
            }
        }
        if(!ok){
            pl.sendMessage(config.none)
            return
        }
        infinity.forEach{
            pl.sendMessage(config.message
                    .replace("%type%", (config.mapping[it.key] ?: error("")) + " ${it.value.toInt()}.${(it.value * 10).toInt()}")
                    .replace("%time%", config.infinity))
        }
        for((type, boost, minutes) in timed){
            pl.sendMessage(config.message.replace("%type%", (config.mapping[type] ?: error("")) + " ${boost.toInt()}.${(boost * 10).toInt()}")
                    .replace("%time%", config.time.replace("%time%", minutes.toString())))
        }
    }
}

@Serializable
internal class Config(
    val infinity: String = "вечность",
    val time: String = "%time% минут",
    val none: String = "§8[§ci§8]§f У тебя нет активных бустеров",
    val message: String = "§8[§ai§8]§f Бустер %type%, длится %time%",
    val mapping: Map<String, String> = mapOf("level" to "уровней")
)