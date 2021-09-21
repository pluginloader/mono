package statsdeath

import configs.Conf
import kotlinx.serialization.Serializable
import morestats.Stats
import morestats.StatsAPI
import org.bukkit.event.entity.PlayerDeathEvent
import pluginloader.api.Listener
import pluginloader.api.Load
import pluginloader.api.uuid

@Conf
internal var config = Config()

@StatsAPI
internal lateinit var stats: Stats

@Listener
internal fun onDeath(event: PlayerDeathEvent){
    stats.add(event.entity.uuid, config.stat, 1.0)
}

@Serializable
internal class Config(val stat: String = "deaths")