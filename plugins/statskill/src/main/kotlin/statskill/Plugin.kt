package statskill

import cmdexec.Commands
import configs.Conf
import configs.conf
import kotlinx.serialization.Serializable
import morestats.Stats
import morestats.StatsAPI
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.EntityDeathEvent
import pluginloader.api.*

@StatsAPI
internal lateinit var stats: Stats

@Load
internal fun Plugin.load(){
    val plu = this
    val config = conf(::Config)
    listener<EntityDeathEvent>{
        val killer = entity.killer ?: return@listener
        if(killer.uniqueId == entity.uniqueId)return@listener
        if(entityType == EntityType.PLAYER) config.playerKillCommands.exec(plu, killer)
        stats.add(killer.uuid, if(entityType == EntityType.PLAYER) config.playerKillStat else config.mobKillStat, 1.0)
    }
}

@Serializable
internal class Config{
    val playerKillCommands = Commands("!onkill %player%")
    val playerKillStat = "player_kills"
    val mobKillStat = "mob_kills"
}