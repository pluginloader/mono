package disablejoin

import configs.Conf
import kotlinx.serialization.Serializable
import morestats.Stats
import morestats.StatsAPI
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerJoinEvent
import pluginloader.api.*

@StatsAPI
internal lateinit var stats: Stats

@Conf
internal var config = Config()

@Load
internal fun load(plugin: Plugin){
    stats.onLoad {uuid ->
        val player = Bukkit.getPlayer(uuid) ?: return@onLoad
        val need = config.mapping[player.world.name] ?: return@onLoad
        val current = stats.get(uuid, config.stat)
        if(need > current)player.kickPlayer(config.message)
    }
}

@Serializable
internal class Config(
    val stat: String = "level", 
    val mapping: Map<String, Int> = mapOf("wooorld" to 1), 
    val message: String = "§8[§ci§8]§f У вас недостаточный уровень"
)