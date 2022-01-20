package tower.api

import configs.Conf
import io.netty.channel.Channel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import pluginloader.api.*
import tower.impl.*
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.random.Random
import kotlin.reflect.KClass

@Deprecated("Use TowerClientAPI or TowerServerAPI")
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Tower

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class TowerClientAPI

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class TowerServerAPI

interface TowerConnect{
    fun broadcast(packet: Packet)

    fun send(packet: Packet)

    fun <T: Response> request(request: Request<T>, callback: (T) -> Unit)

    fun <T: Packet> register(id: String, plugin: LoaderPlugin, kClass: KClass<T>, kSerializer: KSerializer<T>)

    fun <T: Packet> register(id: String, plugin: LoaderPlugin, kClass: KClass<T>, kSerializer: KSerializer<T>, callback: (T) -> Unit)

    fun <I: Request<O>, O: Response> register(iId: String, oId: String,
        plugin: LoaderPlugin,
        iClass: KClass<I>, iSerializer: KSerializer<I>,
        oClass: KClass<O>, oSerializer: KSerializer<O>)

    fun <I: Request<O>, O: Response> register(
        iId: String, oId: String,
        plugin: LoaderPlugin,
        iClass: KClass<I>,
        iSerializer: KSerializer<I>,
        oClass: KClass<O>,
        oSerializer: KSerializer<O>,
        callback: (I) -> O
    )

    fun <I: Request<O>, O: Response> registerCallback(
            iId: String, oId: String,
            plugin: LoaderPlugin,
            iClass: KClass<I>,
            iSerializer: KSerializer<I>,
            oClass: KClass<O>,
            oSerializer: KSerializer<O>,
            callback: (I, (O) -> Unit) -> Unit
    )

    fun old(): OldTower {
        return OldTower(this)
    }

    companion object: (LoaderPlugin) -> TowerConnect?{
        override fun invoke(plu: LoaderPlugin): TowerConnect? = defaultConnection
    }
}

inline fun <reified T: Packet> TowerConnect.reg(plugin: LoaderPlugin, kSerializer: KSerializer<T>){
    register(T::class.java.canonicalName, plugin, T::class, kSerializer)
}

inline fun <reified T: Packet> TowerConnect.reg(plugin: LoaderPlugin, kSerializer: KSerializer<T>, noinline callback: (T) -> Unit){
    register(T::class.java.canonicalName, plugin, T::class, kSerializer, callback)
}

inline fun <reified I: Request<O>, reified O: Response> TowerConnect.reg(plugin: LoaderPlugin, iSerializer: KSerializer<I>, oSerializer: KSerializer<O>){
    register(I::class.java.canonicalName, O::class.java.canonicalName, plugin, I::class, iSerializer, O::class, oSerializer)
}

inline fun <reified I: Request<O>, reified O: Response> TowerConnect.reg(plugin: LoaderPlugin, iSerializer: KSerializer<I>, oSerializer: KSerializer<O>, noinline callback: (I, (O) -> Unit) -> Unit){
    registerCallback(I::class.java.canonicalName, O::class.java.canonicalName, plugin, I::class, iSerializer, O::class, oSerializer, callback)
}

@JvmInline
value class OldTower(val connect: TowerConnect){
    inline fun <reified T: Packet> register(plugin: LoaderPlugin, kSerializer: KSerializer<T>, pluginName: String = plugin.name)
            = register("$pluginName.${T::class.java.name}", plugin, kSerializer)

    inline fun <reified T: Packet> register(plugin: LoaderPlugin, kSerializer: KSerializer<T>, noinline callback: (T) -> Unit)
            = register("${plugin.name}.${T::class.java.name}", plugin, kSerializer, callback)

    inline fun <reified I: Request<O>, reified O: Response> register
                (plugin: LoaderPlugin, iSerializer: KSerializer<I>, oSerializer: KSerializer<O>)
            = register("${plugin.name}.${I::class.java.name}", "${plugin.name}.${O::class.java.name}", plugin, iSerializer, oSerializer)

    inline fun <reified I: Request<O>, reified O: Response> register
                (plugin: LoaderPlugin, iSerializer: KSerializer<I>, oSerializer: KSerializer<O>, noinline callback: (I) -> O)
            = register("${plugin.name}.${I::class.java.name}", "${plugin.name}.${O::class.java.name}", plugin, iSerializer, oSerializer, callback)

    inline fun <reified I: Request<O>, reified O: Response> register
                (plugin: LoaderPlugin, iSerializer: KSerializer<I>, oSerializer: KSerializer<O>, noinline callback: (I, (O) -> Unit) -> Unit)
            = registerCallback("${plugin.name}.${I::class.java.name}", "${plugin.name}.${O::class.java.name}", plugin, iSerializer, oSerializer, callback)

////

    inline fun <reified T: Packet> register(id: String, plugin: LoaderPlugin, kSerializer: KSerializer<T>)
            = connect.register(id, plugin, T::class, kSerializer)

    inline fun <reified T: Packet> register
                (id: String, plugin: LoaderPlugin, kSerializer: KSerializer<T>, noinline callback: (T) -> Unit)
            = connect.register(id, plugin, T::class, kSerializer, callback)

    inline fun <reified I: Request<O>, reified O: Response> register
                (iId: String, oId: String, plugin: LoaderPlugin, iSerializer: KSerializer<I>, oSerializer: KSerializer<O>)
            = connect.register(iId, oId, plugin, I::class, iSerializer, O::class, oSerializer)

    inline fun <reified I: Request<O>, reified O: Response> register
                (iId: String, oId: String, plugin: LoaderPlugin, iSerializer: KSerializer<I>, oSerializer: KSerializer<O>, noinline callback: (I) -> O)
            = connect.register(iId, oId, plugin, I::class, iSerializer, O::class, oSerializer, callback)

    inline fun <reified I: Request<O>, reified O: Response> registerCallback
                (iId: String, oId: String, plugin: LoaderPlugin, iSerializer: KSerializer<I>, oSerializer: KSerializer<O>, noinline callback: (I, (O) -> Unit) -> Unit)
            = connect.registerCallback(iId, oId, plugin, I::class, iSerializer, O::class, oSerializer, callback)

////

    inline fun <reified T: Packet> register(plugin: LoaderPlugin, sharedID: String, kSerializer: KSerializer<T>){
        connect.register("$sharedID.${T::class.java.name}", plugin, T::class, kSerializer)
    }

    inline fun <reified T: Packet> register(plugin: LoaderPlugin, sharedID: String, kSerializer: KSerializer<T>, noinline callback: (T) -> Unit){
        connect.register("$sharedID.${T::class.java.name}", plugin, T::class, kSerializer, callback)
    }

    inline fun <reified I: Request<O>, reified O: Response> register
                (sharedID: String, plugin: LoaderPlugin, iSerializer: KSerializer<I>, oSerializer: KSerializer<O>)
            = connect.register("$sharedID.${I::class.java.name}", "$sharedID.${O::class.java.name}", plugin, I::class, iSerializer, O::class, oSerializer)

    inline fun <reified I: Request<O>, reified O: Response> register
                (sharedID: String, plugin: LoaderPlugin, iSerializer: KSerializer<I>, oSerializer: KSerializer<O>, noinline callback: (I) -> O)
            = connect.register("$sharedID.${I::class.java.name}", "$sharedID.${O::class.java.name}", plugin, I::class, iSerializer, O::class, oSerializer, callback)

    inline fun <reified I: Request<O>, reified O: Response> registerCallback
                (sharedID: String, plugin: LoaderPlugin, iSerializer: KSerializer<I>, oSerializer: KSerializer<O>, noinline callback: (I, (O) -> Unit) -> Unit)
            = connect.registerCallback("$sharedID.${I::class.java.name}", "$sharedID.${O::class.java.name}", plugin, I::class, iSerializer, O::class, oSerializer, callback)
}

interface TowerServer{
    val map: Map<String, TowerConnect>

    fun onInit(plugin: LoaderPlugin, callback: (String, TowerConnect) -> Unit)

    fun onClose(plugin: LoaderPlugin, callback: (String) -> Unit)

    companion object: (LoaderPlugin) -> TowerServer?{
        override fun invoke(plu: LoaderPlugin): TowerServer? = defaultServer
    }
}

private var client: Client? = null
private var defaultConnection: TowerConnectImpl? = null
private var server: Server? = null
private var defaultServer: TowerServerImpl? = null
private var loaded = true

@Conf
internal var config: Config = Config(ClientConfig(false, "client-${Random.nextInt(1000)}", "localhost", 5544), ServerConfig(false, 5544))

@Unload
internal fun unload(){
    loaded = false
    client.nonNull(Client::close)
    server.nonNull(Server::close)
}

@Load
internal fun load(plugin: LoaderPlugin) {
    if (config.client.enabled) {
        client = Client(config.client.host, config.client.port,
            {plugin.task{defaultConnection!!(it)}}, {
                it.channel().writeAndFlush(ProtocolLevelReqPacket(ProtocolLevel.FIRST.ordinal))
                it.channel().writeAndFlush(InitPacket("client${Random.nextLong()}"))
            })
        client!!.connect()
        defaultConnection = TowerConnectImpl({client!!.writeAndFlush(it)}, onClose = {
            plugin.task {
                if (!loaded) return@task
                Thread {
                    println("Disconnected!")
                    Thread.sleep(3000)
                    plugin.task {
                        println("Try reconnect..")
                        client!!.connect()
                    }
                }.start()
            }
        })
        plugin.fieldReplacer(TowerClientAPI::class){_ -> defaultConnection}
    }
    if(config.server.enabled){
        val map = HashMap<Channel, String>()
        val client = HashMap<String, TowerConnectImpl>()
        server = Server(config.server.port){channel, packet -> plugin.task{
            val id: String
            val connect: TowerConnectImpl
            when(packet){
                is InitPacket -> {
                    id = packet.name
                    connect = TowerConnectImpl({channel.writeAndFlush(it)}, onBroadcast = true, broadcast = defaultServer!!::broadcast)
                    map[channel] = id
                    println("New connection $id")
                    client[id] = connect
                    defaultServer!!.onInit(id, connect)
                }
                is ClosePacket -> {
                    id = map.remove(channel) ?: return@task
                    connect = client.remove(id) ?: return@task
                    defaultServer!!.onDisable(id)
                }
                else -> {
                    id = map[channel] ?: return@task
                    connect = client[id] ?: return@task
                }
            }
            connect.invoke(packet)
        }}
        defaultServer = TowerServerImpl()
        Thread(server).start()
        plugin.fieldReplacer(TowerServerAPI::class, defaultServer)
    }
    plugin.fieldHandler(Tower::class){field, _, _ ->
        unsetFinal(field)
        field.set(null, if(field.type == TowerServer::class.java) defaultServer else defaultConnection)
    }
}

private val modifiersField by lazy{Field::class.java.getDeclaredField("modifiers")}

private fun unsetFinal(field: Field){
    if(field.modifiers and Modifier.FINAL != 0){
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
    }
    field.isAccessible = true
}

@Serializable
internal data class Config(val client: ClientConfig, val server: ServerConfig)

@Serializable
internal data class ClientConfig(val enabled: Boolean, val name: String, val host: String, val port: Int)

@Serializable
internal data class ServerConfig(val enabled: Boolean, val port: Int)