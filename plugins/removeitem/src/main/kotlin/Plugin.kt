package removeitem

import org.bukkit.Bukkit
import org.bukkit.Color
import pluginloader.api.Args
import pluginloader.api.Command
import pluginloader.api.Sender
import pluginloader.api.nonNull

private const val prefix = "§8[§aPlu§8]§f"

@Command("removeitem", op = true)
internal fun cmd(sender: Sender, args: Args){
    if(args.size < 2){
        sender.sendMessage("$prefix Usage: §6/removeitem [player] [amount]")
        return
    }
    Bukkit.getPlayer(args[0]).nonNull{it.inventory.itemInMainHand.amount -= args[1].toInt()}
}