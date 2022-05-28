package morestatschat

import chat.chatReplace
import configs.conf
import kotlinx.serialization.Serializable
import morestats.Stats
import morestats.StatsAPI
import pluginloader.api.Load
import pluginloader.api.Plugin
import pluginloader.api.uuid

@StatsAPI
internal lateinit var stats: Stats

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    chatReplace{player, message ->
        var msg = message
        config.mapping.forEach{msg = msg.replace(it.key, stats.get(player.uuid, it.value).toInt().toString())}
        msg
    }
}

@Serializable
internal class Config{val mapping = mapOf("%lvl%" to "level")}