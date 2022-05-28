package sspi

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.bukkit.entity.Player
import pluginloader.api.LoaderPlugin
import pluginloader.api.uuid
import pstore.PStore
import pstore.pStore
import spi.SPI
import java.util.*

interface SSpi<T> {
    operator fun get(uuid: UUID): T?

    operator fun set(uuid: UUID, obj: T?)

    fun remove(uuid: UUID): T?

    fun forceSave(uuid: UUID)

    operator fun get(player: Player): T?

    operator fun set(player: Player, obj: T?)

    fun remove(player: Player): T?

    fun forceSave(player: Player)
}

inline fun <reified T> LoaderPlugin.sSpi(
    noinline load: (Player, T?) -> Unit = { _, _ ->},
    noinline unload: (Player, T?) -> Unit = { _, _ ->},
    saveOnWrite: Boolean = false
): SSpi<T>{
    return sSpi(serializer(), load, unload, saveOnWrite)
}

@Suppress("UNCHECKED_CAST")
fun <T> LoaderPlugin.sSpi(
    serializer: KSerializer<T>,
    load: (Player, T?) -> Unit = { _, _ ->},
    unload: (Player, T?) -> Unit = { _, _ ->},
    saveOnWrite: Boolean = false
): SSpi<T>{
    val spi = SPI.get(this, name, serializer)
    return SSpiImpl(spi as SPI<Any>, pStore(load = { player ->
        spi.load(player.uuid){
            if(player.isOnline.not())return@load
            this[player] = it
            load(player, it as T?)
        }
    }, unload = {player, data ->
        unload(player, data as T?)
        if(saveOnWrite.not()) {
            if(data == null)spi.remove(player.uuid)
            else spi.save(player.uuid, data)
        }
    }), saveOnWrite) as SSpi<T>
}

private class SSpiImpl(val spi: SPI<Any>, val pStore: PStore<Any>, val saveOnWrite: Boolean): SSpi<Any>{
    override fun get(uuid: UUID): Any? {
        return pStore[uuid]
    }

    override fun set(uuid: UUID, obj: Any?) {
        if(obj == null){
            remove(uuid)
            return
        }
        pStore[uuid] = obj
        if(saveOnWrite) spi.save(uuid, obj)
    }

    override fun remove(uuid: UUID): Any? {
        if(saveOnWrite) spi.remove(uuid)
        return pStore.remove(uuid)
    }

    override fun forceSave(uuid: UUID) {
        val obj = pStore[uuid]
        if(obj == null) spi.remove(uuid)
        else spi.save(uuid, obj)
    }

    override fun get(player: Player): Any? {
        return get(player.uuid)
    }

    override fun set(player: Player, obj: Any?) {
        set(player.uuid, obj)
    }

    override fun remove(player: Player): Any? {
        return remove(player.uuid)
    }

    override fun forceSave(player: Player) {
        forceSave(player.uuid)
    }
}