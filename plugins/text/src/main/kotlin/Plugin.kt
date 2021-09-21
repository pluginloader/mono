package text

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pluginloader.api.*
import provide.Provider
import provide.registerProvider
import java.util.*

fun interface Broadcast: (String) -> Unit{
    companion object{
        operator fun invoke(str: String) = broadcast.actual(str)
    }
}

fun interface BroadcastLocal: (String) -> Unit{
    companion object{
        operator fun invoke(str: String) = broadcastLocal.actual(str)
    }
}

fun interface BroadcastChat: (String, Player) -> Unit{
    companion object{
        operator fun invoke(str: String, player: Player) = broadcastChat.actual(str, player)
    }
}

@Load
internal fun load(plugin: Plugin) {
    broadcast = plugin.registerProvider(Broadcast::class, Broadcast{str -> onlinePlayers.forEach{it.sendMessage(str)}})
    broadcastLocal = plugin.registerProvider(BroadcastLocal::class, BroadcastLocal{str -> onlinePlayers.forEach{it.sendMessage(str)}})
    broadcastChat = plugin.registerProvider(BroadcastChat::class, BroadcastChat{str, _ -> onlinePlayers.forEach{it.sendMessage(str)}})
}

private lateinit var broadcast: Provider<Broadcast>
private lateinit var broadcastLocal: Provider<BroadcastLocal>
private lateinit var broadcastChat: Provider<BroadcastChat>

private const val prefix = "§8[§aPlu§8]§f"

@Command("text", op = true)
internal fun cmd(sender: Sender, args: Args){
    if(args.isEmpty()){
        sender.sendMessage("$prefix Usage: §6/text [player] [msg]")
        return
    }
    val player = Bukkit.getPlayer(args[0])
    if(player == null){
        sender.sendMessage("$prefix§c Player §6'${args[0]}'§c offline")
        return
    }
    val str = args.drop(1).joinToString(" ").replace("&", "§")
    player.sendMessage(str)
}

@Command("broad", op = true)
internal fun broadcast(sender: Sender, args: Args){
    Broadcast(args.joinToString(separator = " ").replace("&", "§").replace("$", "¨"))
}

@Command("broadlocal", op = true)
internal fun broadlocal(sender: Sender, args: Args){
    BroadcastLocal(args.joinToString(separator = " ").replace("&", "§").replace("$", "¨"))
}

@Command("chatmsg", op = true)
internal fun chatmsg(sender: Sender, args: Args){
    if(args.isEmpty()){
        sender.sendMessage("$prefix Usage: §6/chatmsg [player] [msg]")
        return
    }
    val player = Bukkit.getPlayer(args[0])
    if(player == null){
        sender.sendMessage("$prefix§c Player §6'${args[0]}'§c offline")
        return
    }
    val str = args.drop(1).joinToString(" ").replace("&", "§")
    BroadcastChat(str, player)
}