package spi

import configs.Conf
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.iq80.leveldb.Options
import pluginloader.api.*
import tower.api.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Conf
internal var config = Config()

private lateinit var spi: (String, KSerializer<Any>) -> SPI<Any>

interface SPI<T>{
    fun remove(uuid: UUID)

    fun save(uuid: UUID, data: T)

    fun load(uuid: UUID, callback: (data: T?) -> Unit)

    fun iterate(callback: (uuid: UUID, data: T) -> Unit, end: () -> Unit)

    companion object{
        @Suppress("UNCHECKED_CAST")
        fun <T> get(plugin: LoaderPlugin, table: String, kSerializer: KSerializer<T>): SPI<T> = spi(table, kSerializer as KSerializer<Any>) as SPI<T>
    }
}

internal lateinit var plu: LoaderPlugin

private val json = Json{this.ignoreUnknownKeys = true}

@InternalSerializationApi
@Load
internal fun load(plugin: LoaderPlugin){
    plu = plugin
    if(config.standalone){
        val database = Database(File(config.databaseDir), Options())
        plugin.unloadHandler { database.close() }
        spi = {table, serializer -> SpiImpl(database, table, serializer)}
        return
    }
    if(config.server){
        val database = Database(File(config.databaseDir), Options())
        plugin.unloadHandler{database.close()}
        spi = {table, serializer -> SpiImpl(database, table, serializer)}
        val server = TowerServer(plugin)!!
        val locks = HashMap<UUID, String>()
        val unlocks = HashMap<UUID, MutableList<() -> Unit>>()
        server.onClose(plugin){name ->
            val iterator = locks.iterator()
            iterator.forEach{if(it.value == name){
                database.unlock(it.key)
                unlocks.remove(it.key)
                iterator.remove()
            }}
        }
        var client: TowerConnect? = null
        if(config.serverSharing) {
            client = TowerConnect(plugin)
            client!!.old().register("remove", plugin, Drop.serializer())
            client.old().register("save", plugin, Save.serializer())
            client.old().register("load_req", "load_res", plugin, LoadReq.serializer(), LoadRes.serializer())
            val iterateMap = HashMap<Int, Pair<(UUID, ByteArray) -> Unit, () -> Unit>>()
            client.old().register("iter_start", plugin, IterateStart.serializer())
            client.old().register("iter_data", plugin, IterateData.serializer()){
                iterateMap[it.id].nonNull{pair -> pair.first(it.uuid, it.data)}
            }
            client.old().register("iter_end", plugin, IterateEnd.serializer()){
                iterateMap.remove(it.id).nonNull{pair -> pair.second()}
            }
            client.old().register("lock", plugin, Lock.serializer())
            client.old().register("unlock", plugin, Unlock.serializer())
            client.old().register("spi.ServerReloaded", plugin, ServerReloaded.serializer()){
                locks.forEach{client.send(Lock(it.key))}
            }
        }
        server.onInit(plu){name, connect ->
            connect.old().register("remove", plugin, Drop.serializer()){drop ->
                if(config.serverSharing && drop.table in config.serverSharingTypes){
                    client!!.send(drop)
                    return@register
                }
                database.drop(drop.uuid, drop.table)
            }
            connect.old().register("save", plugin, Save.serializer()){save ->
                if(config.serverSharing && save.table in config.serverSharingTypes){
                    client!!.send(save)
                    return@register
                }
                database.push(save.uuid, save.table, save.data)
            }
            connect.old().registerCallback("load_req", "load_res", plugin,
                    LoadReq.serializer(), LoadRes.serializer()){ input, output ->
                if(config.serverSharing && input.table in config.serverSharingTypes){
                    client!!.request(input){output(it)}
                    return@registerCallback
                }
                val lock = locks[input.uuid]
                if(lock != null && lock != name){
                    unlocks.computeIfAbsent(input.uuid){ArrayList()}.add{
                        database.get(input.uuid, input.table){output(LoadRes(input.uuid, it))}
                    }
                    return@registerCallback
                }
                database.get(input.uuid, input.table){output(LoadRes(input.uuid, it))}
            }
            connect.old().register("iter_start", plugin, IterateStart.serializer()){
                database.iterate(it.table, {uuid, data -> connect.send(IterateData(it.id, uuid, data))}, {connect.send(IterateEnd(it.id))})
            }
            connect.old().register("iter_data", plugin, IterateData.serializer())
            connect.old().register("iter_end", plugin, IterateEnd.serializer())
            connect.old().register("lock", plugin, Lock.serializer()){
                if(config.serverSharing)client!!.send(it)
                if(locks[it.uuid] == name)return@register
                if(locks[it.uuid] != null){
                    unlocks.computeIfAbsent(it.uuid){ArrayList()}.add{
                        database.lock(it.uuid)
                        locks[it.uuid] = name
                    }
                    return@register
                }
                database.lock(it.uuid)
                locks[it.uuid] = name
            }
            connect.old().register("unlock", plugin, Unlock.serializer()){
                if(config.serverSharing)client!!.send(it)
                if(locks[it.uuid] != name){
                    unlocks.computeIfAbsent(it.uuid){ArrayList()}.add{
                        database.unlock(it.uuid)
                        locks.remove(it.uuid)
                    }
                    return@register
                }
                database.unlock(it.uuid)
                locks.remove(it.uuid)
                unlocks.remove(it.uuid).nonNull{list -> list.forEach{call -> call()}}
            }
            connect.old().register("spi.ControlDrop", plugin, ControlDrop.serializer()){database.delete(it.uuid)}
            connect.old().register("spi.ServerReloaded", plugin, ServerReloaded.serializer())
            connect.send(ServerReloaded())
        }
        return
    }
    if(config.client){
        val client = TowerConnect(plugin)!!
        client.old().register("remove", plugin, Drop.serializer())
        client.old().register("save", plugin, Save.serializer())
        client.old().register("load_req", "load_res", plugin, LoadReq.serializer(), LoadRes.serializer())
        val iterateMap = HashMap<Int, Pair<(UUID, ByteArray) -> Unit, () -> Unit>>()
        var iterateId = 0
        client.old().register("iter_start", plugin, IterateStart.serializer())
        client.old().register("iter_data", plugin, IterateData.serializer()){
            iterateMap[it.id].nonNull{pair -> pair.first(it.uuid, it.data)}
        }
        client.old().register("iter_end", plugin, IterateEnd.serializer()){
            iterateMap.remove(it.id).nonNull{pair -> pair.second()}
        }
        client.old().register("lock", plugin, Lock.serializer())
        client.old().register("unlock", plugin, Unlock.serializer())
        client.old().register("spi.ControlDrop", plugin, ControlDrop.serializer())
        client.old().register("spi.ServerReloaded", plugin, ServerReloaded.serializer()){
            onlinePlayers.forEach{client.send(Lock(it.uuid))}
        }
        caching {
            if (config.lockSupport && plugin is Plugin) {
                plugin.registerListener(PlayerJoinEvent::class, {client.send(Lock(it.uuid))})
                plugin.registerListener(PlayerQuitEvent::class, {client.send(Unlock(it.uuid))})
                plugin.registerCommand("spi", { sender, args ->
                    if (sender.isNotOp) return@registerCommand
                    val uuid = UUID.fromString(args[0])
                    client.send(ControlDrop(uuid))
                    sender.sendMessage("ok")
                }, checkOp = true)
            }
        }//not bukkit
        spi = {table, serializer ->
            object: SPI<Any>{
                override fun remove(uuid: UUID) {
                    client.send(Drop(table, uuid))
                }

                override fun save(uuid: UUID, data: Any) {
                    client.send(Save(table, uuid, Json.encodeToString(serializer, data).toByteArray()))
                }

                override fun load(uuid: UUID, callback: (data: Any?) -> Unit) {
                    runTaskLater(config.timingsBefore) {
                        client.request(LoadReq(table, uuid)) {
                            val data = it.data
                            if (data == null) callback(null)
                            else {
                                var value: Any? = null
                                caching{value = json.decodeFromString(serializer, String(data))}?.printStackTrace()
                                callback(value)
                            }
                        }
                    }
                }

                override fun iterate(callback: (uuid: UUID, data: Any) -> Unit, end: () -> Unit) {
                    val id = iterateId++
                    iterateMap[id] = Pair({uuid, data -> callback(uuid, Json.decodeFromString(serializer, String(data)))}, end)
                    client.send(IterateStart(id, table))
                }
            }
        }
        onlinePlayers.forEach{client.send(Lock(it.uuid))}
        return
    }
}

@Unload
internal fun unload(){
    if(config.client) {
        val client = TowerConnect(plu)!!
        onlinePlayers.forEach{client.send(Unlock(it.uuid))}
    }
}

@Serializable
private class Drop(val table: String, val uuid: @Serializable(UUIDSerializer::class) UUID): Packet

@Serializable
private class Save(val table: String, val uuid: @Serializable(UUIDSerializer::class) UUID, val data: ByteArray): Packet

@Serializable
private class LoadReq(val table: String, val uuid: @Serializable(UUIDSerializer::class) UUID): Request<LoadRes>

@Serializable
private class LoadRes(val uuid: @Serializable(UUIDSerializer::class) UUID, val data: ByteArray?): Response

@Serializable
private class IterateStart(val id: Int, val table: String): Packet

@Serializable
private class IterateData(val id: Int, @Serializable(UUIDSerializer::class) val uuid: UUID, val data: ByteArray): Packet

@Serializable
private class IterateEnd(val id: Int): Packet

@Serializable
private class Lock(val uuid: @Serializable(UUIDSerializer::class) UUID): Packet

@Serializable
private class Unlock(val uuid: @Serializable(UUIDSerializer::class) UUID): Packet

@Serializable
private class ServerReloaded: Packet

@Serializable
private class ControlDrop(val uuid: @Serializable(UUIDSerializer::class) UUID): Packet

@Serializable
internal class Config(
        val client: Boolean = false,
        val server: Boolean = false,
        val standalone: Boolean = true,
        val databaseDir: String = "spi",
        val timingsBefore: Int = 10,
        val timingsBeforeQuit: Int = 1,
        val lockSupport: Boolean = true,
        val serverSharing: Boolean = false,
        val serverSharingTypes: Set<String> = setOf()
)