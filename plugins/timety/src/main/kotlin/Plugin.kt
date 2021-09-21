package timety

import org.bukkit.Bukkit
import pluginloader.api.Args
import pluginloader.api.Command
import pluginloader.api.Sender
import pluginloader.api.runTaskLater

private const val prefix = "§8[§aPlu§8]§f"

@Command("timety", op = true)
internal fun timety(sender: Sender, args: Args){
    if(args.isEmpty()){
        sender.sendMessage("$prefix Usage: §6/timety [time in ticks] [command]")
        return
    }
    val ticks = args[0].toIntOrNull()
    if(ticks == null){
        sender.sendMessage("$prefix §6'${args[0]}'§c is not number")
        return
    }
    runTaskLater(ticks){
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), args.drop(1).joinToString(" "))
    }
}