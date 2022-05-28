package triggermsg

import cmdexec.Commands
import configs.Conf
import configs.conf
import kotlinx.serialization.Serializable
import org.bukkit.event.player.AsyncPlayerChatEvent
import pluginloader.api.*

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    val withSpaces = let {
        val map = HashMap<String, Commands>()
        config.mapping.forEach{if(!it.key.contains(" ")) map[it.key.lowercase()] = it.value}
        map
    }
    val withoutSpaces = let{
        val map = HashMap<String, Commands>()
        config.mapping.forEach{if(it.key.contains(" ")) map[it.key.lowercase()] = it.value}
        map
    }
    val plu = this
    listener<AsyncPlayerChatEvent>{
        if (isCancelled) return@listener
        val message = message.replace("!", "").lowercase()
        withSpaces.forEach{
            if(message.contains(it.key)){
                runTask{it.value.exec(plu, player)}
                return@listener
            }
        }
        val e = message.split(" ")
        e.forEach{withoutSpaces[it].nonNull{cmd -> runTask{cmd.exec(plu, player)}}}
    }
}

@Serializable
internal class Config {
    val mapping = mapOf("nekopara" to Commands("randombylist nekopara %player%"))
}