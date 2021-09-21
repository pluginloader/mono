package sound

import org.bukkit.Bukkit
import pluginloader.api.Args
import pluginloader.api.Command
import pluginloader.api.Sender

@Command("sound", op = true)
internal fun sound(sender: Sender, args: Args){
    if(args.size < 4)return
    val player = Bukkit.getPlayer(args[0])
    player.playSound(player.location, args[1], args[2].toFloat(), args[3].toFloat())
}