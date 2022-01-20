package triggermsg

import cmdexec.Commands
import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.event.player.AsyncPlayerChatEvent
import pluginloader.api.*

@Conf
internal var config = Config()
private lateinit var plu: LoaderPlugin

@Load
internal fun load(plugin: LoaderPlugin){
    plu = plugin
}

private val withSpaces by lazy {
    val map = HashMap<String, Commands>()
    config.mapping.forEach{if(!it.key.contains(" ")) map[it.key.lowercase()] = it.value}
    map
}
private val withoutSpaces by lazy {
    val map = HashMap<String, Commands>()
    config.mapping.forEach{if(it.key.contains(" ")) map[it.key.lowercase()] = it.value}
    map
}

@Listener
internal fun onChat(event: AsyncPlayerChatEvent) {
    if (event.isCancelled) return
    val player = event.player
    val message = event.message.replace("!", "").lowercase()
    withSpaces.forEach{
        if(message.contains(it.key)){
            runTask{it.value.exec(plu, player)}
            return
        }
    }
    val e = message.split(" ")
    e.forEach{withoutSpaces[it].nonNull{cmd -> runTask{cmd.exec(plu, event.player)}}}
}

@Serializable
internal class Config(
    val mapping: Map<String, Commands> = mapOf("nekopara" to Commands(listOf("randombylist nekopara %player%")))
)