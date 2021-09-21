package morestatscmd

import configs.Conf
import kotlinx.serialization.Serializable
import morestats.Stats
import morestats.StatsAPI
import org.bukkit.entity.Player
import pluginloader.api.Load
import pluginloader.api.Plugin
import pluginloader.api.uuid

@StatsAPI
internal lateinit var stats: Stats

@Conf
internal var config = Config()

@Load
internal fun load(plugin: Plugin){
    plugin.registerCommand(config.commandName, { sender, _ ->
        if(sender !is Player)return@registerCommand
        config.message.forEach{
            var msg = it
            config.mappings.forEach{ pair -> msg = msg.replace(pair.key, stats.get(sender.uuid, pair.value).toInt().toString())}
            sender.sendMessage(msg)
        }
    })
}

@Serializable
internal class Config(
    val commandName: String = "mystats",
    val mappings: Map<String, String> = mapOf("%level%" to "level"),
    val message: List<String> = listOf("§8[§ai§8]§f Уровень: §6%level%")
)