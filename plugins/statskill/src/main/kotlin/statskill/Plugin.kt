package statskill

import cmdhelp.execute
import configs.Conf
import kotlinx.serialization.Serializable
import morestats.Stats
import morestats.StatsAPI
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.EntityDeathEvent
import pluginloader.api.Listener
import pluginloader.api.killer
import pluginloader.api.uuid

@Conf
internal var config = Config()

@StatsAPI
internal lateinit var stats: Stats

@Listener
internal fun death(event: EntityDeathEvent){
    val killer = event.entity.killer ?: return
    if(killer.uniqueId == event.entity.uniqueId)return
    if(event.entityType == EntityType.PLAYER)config.playerKillCommands.execute(killer)
    stats.add(killer.uuid, if(event.entity.type == EntityType.PLAYER) config.playerKillStat else config.mobKillStat, 1.0)
}

@Serializable
internal class Config(
    val playerKillCommands: List<String> = listOf("!onkill %player%"),
    val playerKillStat: String = "player_kills",
    val mobKillStat: String = "mob_kills"
)