package chat

import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerChatEvent
import playerinfo.PlayerColor
import playerinfo.PlayerReadable
import pluginloader.api.*
import text.Broadcast
import text.BroadcastChat
import text.BroadcastLocal

@Conf
internal var config = Config()

private val postProcessing = ArrayList<(Player, String) -> String>()

fun LoaderPlugin.chatReplace(replace: (Player, String) -> String){
    postProcessing.add(replace)
    unloadHandler{postProcessing.remove(replace)}
}

@Listener(priority = EventPriority.HIGHEST)
internal fun event(event: AsyncPlayerChatEvent){
    if(event.isCancelled)return
    event.cancel()
    val uuid = event.uuid
    val local = event.message.startsWith("!")
    if(local && event.message.length == 1)return
    val msg = if(local) event.message.substring(1) else event.message
    var message = config.msg
            .replace("%player%", PlayerReadable(uuid))
            .replace("%color%", PlayerColor(uuid))
    postProcessing.forEach{message = it(event.player, message)}
    if(!local && config.localchatEnabled){
        val distance = config.localchatDistance * config.localchatDistance
        val loc = event.player.location
        message = message.replace("%world%", config.localchatWorld)
        message = message.replace("%msg%", msg)
        Bukkit.getOnlinePlayers().forEach{
            if(loc.world == it.location.world && loc.distanceSquared(it.location) < distance)
                it.sendMessage(message)
        }
    }else{
        message = message.replace("%world%", config.worlds[event.player.world.name] ?: config.defaultWorld)
        message = message.replace("%msg%", msg)
        BroadcastChat(message, event.player)
    }
}

@Serializable
internal class Config {
    val msg = "§8[%world%§8] %player% %color%»§f %msg%"
    val defaultWorld = "§6Another"
    val worlds = mapOf("world" to "§aWorld")
    val localchatEnabled = true
    val localchatDistance = 50
    val localchatWorld = "§bL"
    val enabled = true
}