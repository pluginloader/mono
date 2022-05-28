package commandcooldown

import configs.conf
import kotlinx.serialization.Serializable
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import pluginloader.api.*
import java.util.*
import kotlin.collections.HashMap

private val command = HashMap<String, Cooldown>()
private val cd = HashMap<UUID, HashMap<Cooldown, Long>>()

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    config.commands.forEach{
        val cooldown = Cooldown(it.time * 1000L)
        it.commands.forEach{cmd -> command[cmd] = cooldown}
    }
    listener<PlayerCommandPreprocessEvent>(priority = EventPriority.HIGHEST){
        if(isCancelled)return@listener
        if (!message.startsWith("/")) return@listener
        val bug = message.indexOf(':')
        val start = if (bug == -1) 1 else bug + 1
        val end = message.indexOf(' ')
        val cmd = message.substring(start, if (end == -1) message.length else end).lowercase()
        val found = command[cmd] ?: return@listener
        val map = cd[uuid] ?: HashMap()
        val cooldownEnd = map.getOrDefault(found, 0)
        val current = System.currentTimeMillis()
        if (current > cooldownEnd) {
            map[found] = current + found.time
            cd[uuid] = map
        } else {
            val time = cooldownEnd - current
            player.sendMessage(config.message.replace("%time%", formatTime(time)))
            cancel()
        }
    }
}

private fun formatTime(inputTime: Long): String {
    var time = inputTime
    val sb = StringBuilder()
    var formattedOnce = false
    if (time > 86400000) {
        val days = time / 86400000
        sb.append(days).append(plurals(" day ", " days ", days.toInt()))
        time %= 86400000
        formattedOnce = true
    }
    if (time > 3600000) {
        val hours = time / 3600000
        sb.append(hours).append(plurals(" hour ", " hours ", hours.toInt()))
        time %= 3600000
        if (formattedOnce) return sb.toString()
        formattedOnce = true
    }
    if (time > 60000) {
        val minutes = time / 60000
        sb.append(minutes).append(plurals(" minute ", " minutes ", minutes.toInt()))
        time %= 60000
        if (formattedOnce) return sb.toString()
        formattedOnce = true
    }
    if (time > 1000) {
        val seconds = time / 1000
        sb.append(seconds).append(plurals(" second ", " seconds ", seconds.toInt()))
        time %= 1000
        if (formattedOnce) return sb.toString()
    }
    return sb.append(time).append(" мс.").toString()
}

private fun plurals(one: String, many: String, n: Int): String {
    return if(n == 1) one else many
}

private data class Cooldown(val time: Long)

@Serializable
internal class Config {
    val message = "§6You can enter this command after %time%"
    val commands = listOf(Command())
}

@Serializable
internal class Command {
    val commands = listOf("command")
    val time = 10
}