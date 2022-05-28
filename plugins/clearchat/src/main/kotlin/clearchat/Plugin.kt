package clearchat

import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerChatEvent
import pluginloader.api.*
import pstore.PStore
import pstore.pStore
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.random.Random

interface ClearChat{
    fun replace(player: Player, message: String, filter: Boolean): String?
}

fun Plugin.clearChat(): ClearChat{
    return ClearChatImpl(pStore(load = {this[it] = ArrayList(2)}))
}

private class ClearChatImpl(val store: PStore<MutableList<String>>): ClearChat{
    override fun replace(player: Player, message: String, filter: Boolean): String? {
        return replace(player, message, store[player.uuid], filter)
    }
}

@Load
internal fun Plugin.load(){
    val store = pStore<MutableList<String>>(load = {this[it] = ArrayList(2)})
    listener<AsyncPlayerChatEvent>(priority = EventPriority.LOW){
        if(isCancelled)return@listener
        val latest = store[player]
        val msg = replace(player, message, latest, true)
        if(msg == null){
            message = "!"
            cancel()
            return@listener
        }
        message = msg
    }
}

private fun replace(player: Player, sourceMsg: String, latest: MutableList<String>?, filter: Boolean): String?{
    var message = sourceMsg
    if(latest != null) {
        if (latest.size < 2){
            latest.add(message)
        }else{
            var contains = true
            latest.forEach{if(it != message)contains = false}
            latest.removeAt(0)
            latest.add(message)
            if(contains){
                player.sendMessage(config.flood)
                return null
            }
        }
        if(message.length > 10 && latest.size == 2) {
            var contains = true
            latest.forEach{if (it != message) contains = false}
            if (contains) {
                player.sendMessage(config.flood)
                return null
            }
        }
    }
    if(filter) {
        message = message.split(" ").stream().map {
            if (config.list.contains(it.lowercase().replace("!", "").replace(",", "").replace(".", ""))) {
                val random = config.replaces[Random.nextInt(config.replaces.size)]
                if (it.startsWith("!")) "!$random"
                else random
            } else it
        }.collect(Collectors.joining(" "))
    }
    val buf = StringBuffer(message.length)
    var last: Char = 0.toChar()
    var lastCount = 1
    message.toCharArray().forEach{
        if(it == last){
            lastCount++
            if(lastCount > 3) return@forEach
        }else lastCount = 1
        last = it
        buf.append(it)
    }
    val msg = buf.toString()
    val lower = msg.lowercase()
    var caps = 0
    val msgChars = msg.toCharArray()
    lower.toCharArray().forEachIndexed{index, c -> if(msgChars[index] != c)caps++}
    return if(msg.length > 4 && caps > msg.length / 2) lower else msg
}

@Conf
internal var config = Config()

@Serializable
internal class Config {
    val flood = "§8[§ci§8]§f Don't litter the chat, think of the players"
    val replaces = listOf("^-^", "^_^", "T_T", "o_O", "o_o")
    val list = setOf("replaceme")
}