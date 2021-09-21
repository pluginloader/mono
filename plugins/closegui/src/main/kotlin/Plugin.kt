package closegui

import org.bukkit.Bukkit
import pluginloader.api.Args
import pluginloader.api.Command
import pluginloader.api.Sender

@Command("closegui", op = true)
internal fun closegui(sender: Sender, args: Args){
    Bukkit.getPlayer(args[0])?.closeInventory()
}