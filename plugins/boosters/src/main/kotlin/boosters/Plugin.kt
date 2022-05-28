package boosters

import booster.boostInfo
import configs.Conf
import configs.conf
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player
import pluginloader.api.*

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    command(name = "boosters"){player, args ->
        player as Player
        var pl = player
        if(pl.isOp && args.isNotEmpty()) pl = args.player(pl, 0) ?: return@command
        val info = boostInfo(pl.uuid) ?: return@command
        val infinity = HashMap<String, Double>()
        val timed = ArrayList<Triple<String, Double, Int>>()
        var ok = false
        info.forEach {
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
            return@command
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
internal class Config {
    val infinity = "eternity"
    val time = "%time% minute(s)"
    val none = "§8[§ci§8]§f You have no active boosters"
    val message = "§8[§ai§8]§f Booster %type%, lasts %time%"
    val mapping = mapOf("level" to "levels")
}