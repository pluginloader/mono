package moretext

import configs.conf
import kotlinx.serialization.Serializable
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import pluginloader.api.Load
import pluginloader.api.Plugin
import text.Text
import text.TextAPI

@TextAPI
internal lateinit var text: Text

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    config.texts.forEach{entry -> registerCommand(entry.key, {_, args ->
        val player = Bukkit.getPlayer(args[0]) ?: return@registerCommand
        text.buildMessage(args.drop(1).joinToString(" ")).whenComplete{list, _ ->
            list.forEach{
                val text = TextComponent(entry.value)
                text.extra = listOf(it)
                player.sendMessage(text)
            }
        }
    }, true)}
}

@Serializable
internal class Config{val texts = mapOf("et" to "§8[§ci§8]§f ", "wt" to "§8[§ei§8]§f ", "st" to "§8[§ai§8]§f ")}