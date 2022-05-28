package morestatscmd

import configs.conf
import kotlinx.serialization.Serializable
import morestats.Stats
import morestats.StatsAPI
import org.bukkit.entity.Player
import pluginloader.api.Load
import pluginloader.api.Plugin
import pluginloader.api.command
import pluginloader.api.uuid

@StatsAPI
internal lateinit var stats: Stats

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    command(name = config.commandName){sender, _ ->
        if(sender !is Player)return@command
        config.message.forEach{
            var msg = it
            config.mappings.forEach{ pair -> msg = msg.replace(pair.key, stats.get(sender.uuid, pair.value).toInt().toString())}
            sender.sendMessage(msg)
        }
    }
}

@Serializable
internal class Config {
    val commandName = "mystats"
    val mappings = mapOf("%level%" to "level")
    val message = listOf("§8[§ai§8]§f Level: §6%level%")
}