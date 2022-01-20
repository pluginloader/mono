package moretext

import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import pluginloader.api.Load
import pluginloader.api.Plugin

@Conf
internal var config = Config()

@Load
internal fun load(plugin: Plugin){
    config.texts.forEach{entry -> plugin.registerCommand(entry.key, {sender, args ->
        val player = Bukkit.getPlayer(args[0]) ?: return@registerCommand
        player.sendMessage(entry.value + args.drop(1).joinToString(" "))
    }, true)}
}

@Serializable
internal class Config(val texts: Map<String, String> = mapOf("et" to "§8[§ci§8]§f ", "wt" to "§8[§ei§8]§f ", "st" to "§8[§ai§8]§f "))