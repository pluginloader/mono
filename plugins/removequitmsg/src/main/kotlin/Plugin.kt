package removequitmsg

import org.bukkit.event.player.PlayerQuitEvent
import pluginloader.api.Listener

@Listener
internal fun quit(event: PlayerQuitEvent) {
    event.quitMessage = ""
}