package cmdhelp

import org.bukkit.Bukkit
import org.bukkit.entity.Player

fun List<String>.execute(replace: (String) -> String = {it}) {
    forEach{Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replace(it))}
}

fun List<String>.execute(player: Player, replace: (String) -> String = {it}){
    execute{replace(it.replace("%player%", player.name))}
}