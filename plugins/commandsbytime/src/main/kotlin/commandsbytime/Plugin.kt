package commandsbytime

import cmdexec.Commands
import configs.Conf
import kotlinx.serialization.Serializable
import pluginloader.api.*
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

@Conf
internal var config = Config()

private val commands = ArrayList<TimeCommand>()

@Load
internal fun load(plugin: Plugin){
    val zoneid = TimeZone.getTimeZone(config.timezone).toZoneId()
    var currentDay = -1
    config.timers.forEach{
        it.per.forEach{str ->
            val splt = str.split(":")
            var hours = splt[0]
            if(hours.startsWith("0"))hours = hours.substring(1)
            var minutes = splt[1]
            if(minutes.startsWith("0"))minutes = minutes.substring(1)
            commands.add(TimeCommand(hours.toInt(), minutes.toInt(), it.commands))
        }
        it.perMinuteInHour.forEach{minute ->
            repeat(24){i ->
                commands.add(TimeCommand(i, minute, it.commands))
            }
        }
    }
    plugin.runTaskTimer(10, {
        val time = Instant.now().atZone(zoneid)
        if(time.dayOfYear != currentDay){
            commands.forEach{it.called.set(false)}
            currentDay = time.dayOfYear
        }
        commands.forEach{
            if(it.called.get())return@forEach
            if(it.hour != time.hour || it.minute != time.minute)return@forEach
            it.commands.exec(plugin)
            it.called.set(true)
        }
    })
}

private class TimeCommand(val hour: Int, val minute: Int, val commands: Commands, var called: AtomicBoolean = AtomicBoolean(false))

@Serializable
internal class Config(
    val timezone: String = TimeZone.getDefault().id,
    val timers: List<Timer> = listOf(Timer())
)

@Serializable
internal class Timer(
    val commands: Commands = Commands(listOf("!cmd")),
    val perMinuteInHour: IntArray = intArrayOf(3),
    val per: List<String> = listOf("3:00")
)