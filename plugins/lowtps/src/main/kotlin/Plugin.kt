package lowtps

import cmdexec.Commands
import configs.conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import pluginloader.api.*

private lateinit var task: BukkitTask

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    val plu = this
    task = pluginloader.api.runTaskTimer(config.time){
        if(Bukkit.getTPS()[1] >= config.tps)return@runTaskTimer
        config.commands.exec(plu)
        if(config.callOnce)task.cancel()
    }
}

@Unload
internal fun unload(){
    task.cancel()
}

@Serializable
internal class Config {
    val time = 14000
    val tps = 5
    val callOnce = true
    val commands = Commands("!cmd on low tps")
}