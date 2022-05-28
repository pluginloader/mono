package commandsbytime

import cmdexec.Commands
import configs.conf
import kotlinx.serialization.Serializable
import pluginloader.api.*
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    val zoneid = TimeZone.getTimeZone(config.timezone).toZoneId()
    var currentDay = -1
    val commands = ArrayList<TimeCommand>()
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
    runTaskTimer(10){
        val time = Instant.now().atZone(zoneid)
        if(time.dayOfYear != currentDay){
            commands.forEach{it.called.set(false)}
            currentDay = time.dayOfYear
        }
        commands.forEach{
            if(it.called.get())return@forEach
            if(it.hour != time.hour || it.minute != time.minute)return@forEach
            it.commands.exec(this)
            it.called.set(true)
        }
    }
}

private class TimeCommand(val hour: Int, val minute: Int, val commands: Commands, var called: AtomicBoolean = AtomicBoolean(false))

@Serializable
internal class Config {
    val timezone = TimeZone.getDefault().id
    val timers = listOf(Timer())
}

@Serializable
internal class Timer {
    val commands = Commands("!cmd")
    val perMinuteInHour = intArrayOf(3)
    val per = listOf("3:00")
}