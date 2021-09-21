package disablefeed

import org.bukkit.event.entity.FoodLevelChangeEvent
import pluginloader.api.Listener
import pluginloader.api.Load
import pluginloader.api.onlinePlayers

@Load
internal fun onLoad(){
    onlinePlayers.forEach{it.foodLevel = 20}
}

@Listener
internal fun onFeed(event: FoodLevelChangeEvent){
    event.foodLevel = 20
}