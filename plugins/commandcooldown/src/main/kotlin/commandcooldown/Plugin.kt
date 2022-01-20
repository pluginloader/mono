package commandcooldown

import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import pluginloader.api.*
import java.util.*
import kotlin.collections.HashMap

@Conf
internal val config = Config()

private val command = HashMap<String, Cooldown>()
private val cd = HashMap<UUID, HashMap<Cooldown, Long>>()

@Load
internal fun load(){
    config.commands.forEach{
        val cooldown = Cooldown(it.time * 1000L)
        it.commands.forEach{cmd -> command[cmd] = cooldown}
    }
}

@Listener(EventPriority.HIGHEST)
internal fun cmd(event: PlayerCommandPreprocessEvent){
    if(event.isCancelled)return
    try {
        if (!event.message.startsWith("/")) return
        val bug = event.message.indexOf(':')
        val start = if (bug == -1) 1 else bug + 1
        val end = event.message.indexOf(' ')
        val cmd = event.message.substring(start, if (end == -1) event.message.length else end).lowercase()
        val found = command[cmd] ?: return
        val map = cd[event.player.uuid] ?: HashMap()
        val cooldownEnd = map.getOrDefault(found, 0)
        val current = System.currentTimeMillis()
        if (current > cooldownEnd) {
            map[found] = current + found.time
            cd[event.player.uuid] = map
        } else {
            val time = cooldownEnd - current
            event.player.sendMessage(config.message.replace("%time%", formatTime(time)))
            event.cancel()
        }
    }catch (ex: Throwable){
        ex.printStackTrace()
        // :/
    }
}

private fun formatTime(time: Long): String {
    var time = time
    val sb = StringBuilder()
    var formattedOnce = false
    if (time > 86400000) {
        val days = time / 86400000
        sb.append(days).append(plurals(" день ", " дня ", " дней ", days.toInt()))
        time %= 86400000
        formattedOnce = true
    }
    if (time > 3600000) {
        val hours = time / 3600000
        sb.append(hours).append(plurals(" час ", " часа ", " часов ", hours.toInt()))
        time %= 3600000
        if (formattedOnce) return sb.toString()
        formattedOnce = true
    }
    if (time > 60000) {
        val minutes = time / 60000
        sb.append(minutes).append(plurals(" минуту ", " минуты ", " минут ", minutes.toInt()))
        time %= 60000
        if (formattedOnce) return sb.toString()
        formattedOnce = true
    }
    if (time > 1000) {
        val seconds = time / 1000
        sb.append(seconds).append(plurals(" секунду ", " секунды ", " секунд ", seconds.toInt()))
        time %= 1000
        if (formattedOnce) return sb.toString()
    }
    return sb.append(time).append(" мс.").toString()
}

private fun plurals(one: String, couple: String, many: String, n: Int): String {
    return if (n % 10 == 1 && n % 100 != 11) one else if (n % 10 in 2..4 && (n % 100 < 10 || n % 100 >= 20)) couple else many
}

private data class Cooldown(val time: Long)

@Serializable
internal class Config(
    val message: String = "${Color.ORANGE}Вы можете ввести эту команду через %time%",
    val commands: List<Command> = listOf(Command())
)

@Serializable
internal class Command(
    val commands: List<String> = listOf("command"),
    val time: Int = 10,
)