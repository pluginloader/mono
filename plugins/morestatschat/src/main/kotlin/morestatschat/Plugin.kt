package morestatschat

import chat.chatReplace
import configs.Conf
import kotlinx.serialization.Serializable
import morestats.Stats
import morestats.StatsAPI
import pluginloader.api.Load
import pluginloader.api.Plugin
import pluginloader.api.uuid

@StatsAPI
internal lateinit var stats: Stats

@Conf
internal var config = Config()

@Load
internal fun load(plugin: Plugin){
    plugin.chatReplace{player, message ->
        var msg = message
        config.mapping.forEach{msg = msg.replace(it.key, stats.get(player.uuid, it.value).toInt().toString())}
        msg
    }
}

@Serializable
internal class Config(val mapping: Map<String, String> = mapOf("%lvl%" to "level"))