package triggermsg

import cmdhelp.execute
import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import pluginloader.api.Listener
import pluginloader.api.nonNull
import pluginloader.api.runTask

@Conf
internal var config = Config()

private val withSpaces by lazy {
    val map = HashMap<String, List<String>>()
    config.mapping.forEach{if(!it.key.contains(" ")) map[it.key.toLowerCase()] = it.value}
    map
}
private val withoutSpaces by lazy {
    val map = HashMap<String, List<String>>()
    config.mapping.forEach{if(it.key.contains(" ")) map[it.key.toLowerCase()] = it.value}
    map
}

@Listener
internal fun onChat(event: AsyncPlayerChatEvent) {
    if (event.isCancelled) return
    val player = event.player
    val message = event.message.replace("!", "").toLowerCase()
    withSpaces.forEach{
        if(message.contains(it.key)){
            runTask{it.value.execute(player)}
            return
        }
    }
    val e = message.split(" ")
    e.forEach{withoutSpaces[it].nonNull{cmd -> runTask{cmd.execute(event.player)}}}
}

@Serializable
internal class Config(
    val mapping: Map<String, List<String>> = mapOf("nekopara" to listOf("randombylist nekopara %player%"))
)