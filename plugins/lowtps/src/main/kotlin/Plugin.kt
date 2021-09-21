package lowtps

import cmdhelp.execute
import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import pluginloader.api.*

@Conf
internal var config = Config()

private lateinit var task: BukkitTask

@Load
internal fun load(){
    task = runTaskTimer(config.time){
        if(Bukkit.getTPS()[1] >= config.tps)return@runTaskTimer
        config.commands.execute()
	    if(config.callOnce)task.cancel()
    }
}

@Unload
internal fun unload(){
    task.cancel()
}

@Serializable
internal class Config(
	val time: Int = 14000, 
	val tps: Int = 5,
	val callOnce: Boolean = true,
	val commands: List<String> = listOf("!cmd on low tps"),
)