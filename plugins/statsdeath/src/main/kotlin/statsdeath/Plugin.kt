package statsdeath

import configs.conf
import kotlinx.serialization.Serializable
import morestats.Stats
import morestats.StatsAPI
import org.bukkit.event.entity.PlayerDeathEvent
import pluginloader.api.*

@StatsAPI
internal lateinit var stats: Stats

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    listener<PlayerDeathEvent>{stats.add(entity.uuid, config.stat, 1.0)}
}

@Serializable
internal class Config{val stat = "deaths"}