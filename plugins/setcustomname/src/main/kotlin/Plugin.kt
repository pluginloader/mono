package setcustomname

import org.bukkit.Bukkit
import playerinfo.PlayerReadable
import pluginloader.api.*

private const val prefix = "§8[§aPlu§8]§f"

@Command("setcustomname", op = true)
internal fun command(sender: Sender, args: Args){
    if(args.isEmpty()){
        sender.sendMessage("$prefix Usage: §6/setcustomname [player] [<readable>\nlol]")
        return
    }
    val player = Bukkit.getPlayer(args[0]) ?: return
    runAsync {
        val readable = PlayerReadable(player.uuid)
        runTask {
            player.customName = args.drop(1).joinToString(" ").replace("\\n", "\n").replace("<readable>", readable)
        }
    }
}