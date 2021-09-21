package gcmd

import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import pluginloader.api.*
import tower.api.Packet
import tower.api.TowerClientAPI
import tower.api.TowerConnect

@TowerClientAPI
internal lateinit var tower: TowerConnect

@Load
internal fun load(plugin: LoaderPlugin){
    tower.old().register(plugin, Cmd.serializer()){Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it.cmd)}
}

@Command("gcmd", op = true)
internal fun gcmd(sender: Sender, args: Args){
    val cmd = args.joinToString(" ")
    tower.broadcast(Cmd(cmd))
    sender.sendMessage("§8[§aPlu§8]§f Executed §6'$cmd'")
}

@Serializable
internal class Cmd(val cmd: String): Packet