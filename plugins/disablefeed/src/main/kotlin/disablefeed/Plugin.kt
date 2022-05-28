package disablefeed

import org.bukkit.event.entity.FoodLevelChangeEvent
import pluginloader.api.*

@Load
internal fun Plugin.onLoad(){
    onlinePlayers.forEach{it.foodLevel = 20}
    listener<FoodLevelChangeEvent>{foodLevel = 20}
}