package disablejoin

import configs.conf
import kotlinx.serialization.Serializable
import morestats.Stats
import morestats.StatsAPI
import org.bukkit.Bukkit
import pluginloader.api.*

@StatsAPI
internal lateinit var stats: Stats

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    stats.onLoad {uuid ->
        val player = Bukkit.getPlayer(uuid) ?: return@onLoad
        val need = config.mapping[player.world.name] ?: return@onLoad
        val current = stats.get(uuid, config.stat)
        if(need > current)player.kickPlayer(config.message)
    }
}

@Serializable
internal class Config {
    val stat = "level"
    val mapping = mapOf("wooorld" to 1)
    val message = "§8[§ci§8]§f You have an insufficient level"
}