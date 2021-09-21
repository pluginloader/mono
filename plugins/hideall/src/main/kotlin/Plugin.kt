package hideall

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import pluginloader.api.*

@Listener
internal fun join(event: PlayerJoinEvent){
    hide(event.player)
}

private fun hide(player: Player){
    onlinePlayers.forEach{
        it.hidePlayer(plugin, player)
        player.hidePlayer(plugin, it)
    }
}

@Load
internal fun load(){
    onlinePlayers.forEach{hide(it)}
}

@Unload
internal fun unload(){
    onlinePlayers.forEach{player ->
        onlinePlayers.forEach{
            it.showPlayer(plugin, player)
            player.showPlayer(plugin, it)
        }
    }
}