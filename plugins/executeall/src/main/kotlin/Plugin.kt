package executeall

import org.bukkit.Bukkit
import pluginloader.api.Args
import pluginloader.api.Command
import pluginloader.api.Sender
import pluginloader.api.onlinePlayers

private const val prefix = "§8[§aPlu§8]§f"

@Command("executeall", op = true)
internal fun cmd(sender: Sender, args: Args){
    if(args.isEmpty()){
        sender.sendMessage("$prefix Usage: §6/executeall [command <player>]")
        return
    }
    val cmd = args.joinToString(" ")
    onlinePlayers.forEach{Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("<player>", it.name))}
}