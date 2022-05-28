package pstore

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import pluginloader.api.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

interface PStore<T>{
    operator fun get(uuid: UUID): T?

    operator fun set(uuid: UUID, obj: T?)

    fun remove(uuid: UUID): T?

    operator fun get(player: Player): T?

    operator fun set(player: Player, obj: T?)

    fun remove(player: Player): T?
}

inline fun <T: Any> PStore<T>.getOr(uuid: UUID, compute: () -> T): T{
    var get = get(uuid)
    if(get == null){
        get = compute()
        set(uuid, get)
    }
    return get
}

inline fun <T: Any> PStore<T>.getOr(player: Player, compute: () -> T): T{
    var get = get(player)
    if(get == null){
        get = compute()
        set(player, get)
    }
    return get
}

@Suppress("UNCHECKED_CAST")
fun <T> LoaderPlugin.pStore(load: PStore<T>.(Player) -> Unit = {}, unload: (Player, T?) -> Unit = {_, _ ->}): PStore<T>{
    val pStore = PStoreImpl(load as PStore<Any>.(Player) -> Unit){player, any -> unload(player, any as T?)}
    loaded(stores, pStore)
    onlinePlayers.forEach{pStore.load(it)}
    unloadHandler{onlinePlayers.forEach{pStore.unload(it)}}
    return pStore as PStore<T>
}

private val stores = ArrayList<PStoreImpl>()

private class PStoreImpl(
    private val load: PStore<Any>.(Player) -> Unit,
    private val unload: (Player, Any?) -> Unit
): PStore<Any>{
    private val map = HashMap<UUID, Any>()

    override fun get(uuid: UUID): Any? {
        return map[uuid]
    }

    override fun set(uuid: UUID, obj: Any?) {
        if(obj == null) remove(uuid)
        else map[uuid] = obj
    }

    override fun remove(uuid: UUID): Any? {
        return map.remove(uuid)
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

    internal fun load(player: Player){
        load.invoke(this, player)
    }

    internal fun unload(player: Player){
        unload.invoke(player, map.remove(player.uuid))
    }
}

@Load
internal fun Plugin.load(){
    listener<PlayerJoinEvent>{stores.forEach{it.load(player)}}
    listener<PlayerQuitEvent>{stores.forEach{it.unload(player)}}
}