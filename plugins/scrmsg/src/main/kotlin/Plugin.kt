package scrmsg

import com.destroystokyo.paper.Title
import org.bukkit.Bukkit
import pluginloader.api.*

@Command("scrmsg", op = true)
internal fun cmd(sender: Sender, args: Args){
    if(args.isEmpty()){
        sender.sendMessage("$prefix Usage: §6/scrmsg <Player|*> action msg...")
        sender.sendMessage("$prefix Or: §6/scrmsg <Player|*> normal title | subtitle")
        return
    }
    val player = if(args[0] == "*") Bukkit.getOnlinePlayers() else listOf(args.player(sender, 0) ?: return)
    if(args[1] == "action"){
        player.forEach{it.sendActionBar(args.copyOfRange(2, args.size).joinToString(" "))}
    }else if(args[1] == "normal"){
        var findSeparator = 2
        while (findSeparator != args.size){
            if(args[findSeparator] == "|")break
            findSeparator++
        }
        val title = args.copyOfRange(2, findSeparator).joinToString(" ")
        val subtitle = if(args.size <= findSeparator + 1) "" else args.copyOfRange(findSeparator + 1, args.size).joinToString(" ")
        player.forEach{it.sendTitle(Title(title, subtitle, 10, 70, 20))}
    }
}