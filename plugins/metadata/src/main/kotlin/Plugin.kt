package metadata

import configs.Conf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.iq80.leveldb.Options
import org.iq80.leveldb.ReadOptions
import org.iq80.leveldb.impl.Iq80DBFactory
import pluginloader.api.Load
import pluginloader.api.LoaderPlugin
import pluginloader.api.caching
import tower.api.TowerConnect
import tower.api.TowerServer
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executors

fun <T: Any> LoaderPlugin.metadata(serializer: KSerializer<T>, pull: (T) -> Unit, drop: (T) -> Unit, name: String = this.name)
        : MetadataStorage<T> = get(serializer, pull, drop, this, name)

interface MetadataStorage<T: Any>{
    fun push(data: T)

    fun drop(data: T)
}

internal interface MetadataGet{
    operator fun <T: Any> invoke(kSerializer: KSerializer<T>, pull: (T) -> Unit, drop: (T) -> Unit, plugin: LoaderPlugin, name: String): MetadataStorage<T>
}

@Conf
internal var config = Config()

private lateinit var get: MetadataGet

@Load
internal fun load(plugin: LoaderPlugin){
    when{
        config.client -> {
            val pushListeners = HashMap<String, (String) -> Unit>()
            val dropListeners = HashMap<String, (String) -> Unit>()
            val client = TowerConnect(plugin)!!
            client.old().register(plugin, Push.serializer()){
                val listener = pushListeners[it.name] ?: return@register
                listener(it.data)
            }
            client.old().register(plugin, Drop.serializer()){
                val listener = dropListeners[it.name] ?: return@register
                listener(it.data)
            }
            client.old().register(plugin, All.serializer())
            get = object: MetadataGet{
                override fun <T: Any> invoke(kSerializer: KSerializer<T>, pull: (T) -> Unit, drop: (T) -> Unit, plugin: LoaderPlugin, name: String): MetadataStorage<T> {
                    pushListeners[name] = {pull(Json.decodeFromString(kSerializer, it))}
                    dropListeners[name] = {drop(Json.decodeFromString(kSerializer, it))}
                    plugin.unloadHandler {
                        pushListeners.remove(name)
                        dropListeners.remove(name)
                    }
                    client.send(All(name))
                    return object: MetadataStorage<T> {
                        override fun push(data: T) = client.send(Push(name, Json.encodeToString(kSerializer, data)))
                        override fun drop(data: T) = client.send(Drop(name, Json.encodeToString(kSerializer, data)))
                    }
                }
            }
        }
        config.server -> {
            val db = Database(File(config.tableName), Options().createIfMissing(true))
            plugin.unloadHandler{db.close()}
            val tower = TowerServer(plugin)!!
            tower.onInit(plugin){_, connect ->
                connect.old().register(plugin, Push.serializer()){
                    tower.map.values.forEach{client -> client.send(it)}
                    db.add(it.name, it.data)
                }
                connect.old().register(plugin, Drop.serializer()){
                    tower.map.values.forEach{client -> client.send(it)}
                    db.drop(it.name, it.data)
                }
                connect.old().register(plugin, All.serializer()){
                    db.all(it.name){list -> list.forEach{json -> connect.send(Push(it.name, json))}}
                }
            }
            get = object: MetadataGet{
                override fun <T: Any> invoke(kSerializer: KSerializer<T>, pull: (T) -> Unit, drop: (T) -> Unit, plugin: LoaderPlugin, name: String): MetadataStorage<T> {
                    db.all(name){plugin.task{it.forEach{json -> pull(Json.decodeFromString(kSerializer, json))}}}
                    return MetadataStorageImpl(kSerializer, db, name, pull, drop)
                }
            }
        }
        config.standalone -> {
            val db = Database(File(config.tableName), Options().createIfMissing(true))
            plugin.unloadHandler{db.close()}
            get = object: MetadataGet{
                override fun <T: Any> invoke(kSerializer: KSerializer<T>, pull: (T) -> Unit, drop: (T) -> Unit, plugin: LoaderPlugin, name: String): MetadataStorage<T> {
                    db.all(name){plugin.task{it.forEach{json -> pull(Json.decodeFromString(kSerializer, json))}}}
                    return MetadataStorageImpl(kSerializer, db, name, pull, drop)
                }
            }
        }
    }
}

internal class Database(dir: File, options: Options) {
    private val executor = Executors.newSingleThreadExecutor()
    private val database = Iq80DBFactory.factory.open(dir, options)
    private val readOptions = ReadOptions()

    private inline fun submit(crossinline call: () -> Unit){
        executor.submit{caching(call)?.printStackTrace()}
    }

    fun close(){
        executor.submit{database.close()}
        executor.shutdown()
    }

    fun all(name: String, callback: (List<String>) -> Unit){
        submit {
            val read = database[name.toByteArray()] ?: return@submit
            val buf = ByteBuffer.wrap(read)
            val list = ArrayList<String>()
            repeat(buf.int){
                caching {
                    val bytes = ByteArray(buf.int)
                    buf.get(bytes)
                    list.add(String(bytes))
                }
            }
            callback(list)
        }
    }

    fun drop(name: String, json: String){
        submit {
            val read = database[name.toByteArray()] ?: return@submit
            val buf = ByteBuffer.wrap(read)
            val list = ArrayList<String>()
            repeat(buf.int){
                caching {
                    val bytes = ByteArray(buf.int)
                    buf.get(bytes)
                    list.add(String(bytes))
                }
            }
            if(list.remove(json)){
                val toBytes = json.toByteArray()
                val newBuf = ByteBuffer.allocate(read.size - toBytes.size - 4)
                newBuf.putInt(list.size)
                list.forEach{
                    val bytes = it.toByteArray()
                    newBuf.putInt(bytes.size).put(bytes)
                }
                database.put(name.toByteArray(), newBuf.array())
            }
        }
    }

    fun add(name: String, json: String){
        submit {
            val read = database[name.toByteArray()]
            val list = ArrayList<String>()
            if(read != null){
                val buf = ByteBuffer.wrap(read)
                repeat(buf.int){
                    caching {
                        val bytes = ByteArray(buf.int)
                        buf.get(bytes)
                        list.add(String(bytes))
                    }
                }
            }
            if(!list.contains(json)){
                list.add(json)
                val toBytes = json.toByteArray()
                val newBuf = ByteBuffer.allocate((read?.size ?: 4) + toBytes.size + 4)
                newBuf.putInt(list.size)
                list.forEach{
                    val bytes = it.toByteArray()
                    newBuf.putInt(bytes.size).put(bytes)
                }
                database.put(name.toByteArray(), newBuf.array())
            }
        }
    }

}

private class MetadataStorageImpl<T: Any>(val serializer: KSerializer<T>, val db: Database, val name: String,
                                          val pull: (T) -> Unit, val dropOther: (T) -> Unit): MetadataStorage<T>{
    override fun push(data: T) {
        pull(data)
        val json = Json.encodeToString(serializer, data)
        db.add(name, json)
    }

    override fun drop(data: T) {
        dropOther(data)
        val json = Json.encodeToString(serializer, data)
        db.drop(name, json)
    }
}

@Serializable
internal data class Config(
    val client: Boolean = false,
    val server: Boolean = false,
    val standalone: Boolean = true,
    val tableName: String = "plugin_metadata"
)