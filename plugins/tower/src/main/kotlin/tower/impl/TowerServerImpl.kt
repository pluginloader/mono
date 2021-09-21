package tower.impl

import pluginloader.api.LoaderPlugin
import tower.api.TowerConnect
import tower.api.TowerServer

internal class TowerServerImpl: TowerServer{
    private val mapping = HashMap<String, TowerConnectImpl>()
    private val onLoad = ArrayList<(String, TowerConnect) -> Unit>()
    private val onClose = ArrayList<(String) -> Unit>()

    override val map: Map<String, TowerConnect> get() = mapping

    override fun onInit(plugin: LoaderPlugin, callback: (String, TowerConnect) -> Unit) {
        mapping.forEach(callback)
        onLoad.add(callback)
        plugin.unloadHandler{onLoad.remove(callback)}
    }

    override fun onClose(plugin: LoaderPlugin, callback: (String) -> Unit) {
        onClose.add(callback)
        plugin.unloadHandler{onClose.remove(callback)}
    }

    fun broadcast(packet: BroadcastPacket){
        mapping.values.forEach{it.sendPacket(packet)}
    }

    fun onInit(id: String, tower: TowerConnectImpl){
        onLoad.forEach{it(id, tower)}
        mapping[id] = tower
    }

    fun onDisable(id: String){
        mapping.remove(id) ?: return
        onClose.forEach{it(id)}
    }
}