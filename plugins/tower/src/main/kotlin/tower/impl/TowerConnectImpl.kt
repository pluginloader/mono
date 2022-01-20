package tower.impl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import pluginloader.api.LoaderPlugin
import pluginloader.api.caching
import pluginloader.api.nonNull
import tower.api.Packet
import tower.api.Request
import tower.api.Response
import tower.api.TowerConnect
import kotlin.random.Random
import kotlin.reflect.KClass

internal class TowerConnectImpl(val sendPacket: (SystemPacket) -> Unit,
                                private val onInit: (String) -> Unit = {},
                                private val onClose: () -> Unit = {},
                                private val onBroadcast: Boolean = false,
                                private val broadcast: (BroadcastPacket) -> Unit = {})
    : TowerConnect, (SystemPacket) -> Unit{
    private val names = HashMap<KClass<*>, String>()
    private val namesLookup = HashMap<String, KClass<*>>()
    private val serializers = HashMap<KClass<*>, KSerializer<Any>>()
    private val dataListen = HashMap<KClass<*>, (Any) -> Unit>()
    private val requestListen = HashMap<KClass<*>, (Any, (Any) -> Unit) -> Unit>()
    private val requestWait = HashMap<Int, (Any) -> Unit>()

    override fun broadcast(packet: Packet) {
        sendPacket(BroadcastPacket(names[packet::class]!!, Json.encodeToString(serializers[packet::class]!!, packet as Any)))
    }

    override fun send(packet: Packet) {
        sendPacket(DataPacket(names[packet::class]!!, Json.encodeToString(serializers[packet::class]!!, packet as Any)))
    }

    override fun <T: Response> request(request: Request<T>, callback: (T) -> Unit) {
        var random: Int
        do { random = Random.nextInt()
        }while (requestWait[random] != null)
        @Suppress("UNCHECKED_CAST")
        requestWait[random] = {callback(it as T)}
        sendPacket(RequestPacket(names[request::class]!!, Json.encodeToString(serializers[request::class]!!, request),random))
    }

    override fun <T: Packet> register(
        id: String,
        plugin: LoaderPlugin,
        kClass: KClass<T>,
        kSerializer: KSerializer<T>
    ) {
        names[kClass] = id
        namesLookup[id] = kClass
        @Suppress("UNCHECKED_CAST")
        serializers[kClass] = kSerializer as KSerializer<Any>
        plugin.unloadHandler {
            names.remove(kClass)
            namesLookup.remove(id)
            serializers.remove(kClass)
        }
    }

    override fun <T : Packet> register(
        id: String,
        plugin: LoaderPlugin,
        kClass: KClass<T>,
        kSerializer: KSerializer<T>,
        callback: (T) -> Unit
    ) {
        names[kClass] = id
        namesLookup[id] = kClass
        @Suppress("UNCHECKED_CAST")
        serializers[kClass] = kSerializer as KSerializer<Any>
        @Suppress("UNCHECKED_CAST")
        dataListen[kClass] = {callback(it as T)}
        plugin.unloadHandler {
            names.remove(kClass)
            namesLookup.remove(id)
            serializers.remove(kClass)
            dataListen.remove(kClass)
        }
    }

    override fun <I : Request<O>, O : Response> register(
        iId: String,
        oId: String,
        plugin: LoaderPlugin,
        iClass: KClass<I>,
        iSerializer: KSerializer<I>,
        oClass: KClass<O>,
        oSerializer: KSerializer<O>
    ) {
        names[iClass] = iId
        namesLookup[iId] = iClass
        @Suppress("UNCHECKED_CAST")
        serializers[iClass] = iSerializer as KSerializer<Any>
        names[oClass] = oId
        namesLookup[oId] = oClass
        @Suppress("UNCHECKED_CAST")
        serializers[oClass] = oSerializer as KSerializer<Any>
        plugin.unloadHandler {
            names.remove(iClass)
            namesLookup.remove(iId)
            serializers.remove(iClass)
            names.remove(oClass)
            namesLookup.remove(oId)
            serializers.remove(oClass)
        }
    }

    override fun <I : Request<O>, O : Response> register(
        iId: String,
        oId: String,
        plugin: LoaderPlugin,
        iClass: KClass<I>,
        iSerializer: KSerializer<I>,
        oClass: KClass<O>,
        oSerializer: KSerializer<O>,
        callback: (I) -> O
    ) {
        registerCallback(iId, oId, plugin, iClass, iSerializer, oClass, oSerializer){packet, resend -> resend(callback(packet))}
    }

    override fun <I : Request<O>, O : Response> registerCallback(
            iId: String,
            oId: String,
            plugin: LoaderPlugin,
            iClass: KClass<I>,
            iSerializer: KSerializer<I>,
            oClass: KClass<O>,
            oSerializer: KSerializer<O>,
            callback: (I, (O) -> Unit) -> Unit) {
        names[iClass] = iId
        namesLookup[iId] = iClass
        @Suppress("UNCHECKED_CAST")
        serializers[iClass] = iSerializer as KSerializer<Any>
        names[oClass] = oId
        namesLookup[oId] = oClass
        @Suppress("UNCHECKED_CAST")
        serializers[oClass] = oSerializer as KSerializer<Any>
        @Suppress("UNCHECKED_CAST")
        requestListen[iClass] = {packet, resend -> callback(packet as I, resend)}
        plugin.unloadHandler {
            names.remove(iClass)
            namesLookup.remove(iId)
            serializers.remove(iClass)
            names.remove(oClass)
            namesLookup.remove(oId)
            serializers.remove(oClass)
            requestListen.remove(iClass)
        }
    }

    override fun invoke(packet: SystemPacket) {
        when(packet){
            is InitPacket -> onInit(packet.name)
            is DataPacket -> {
                val kClass = namesLookup[packet.id] ?: return
                var decode: Any? = null
                caching{
                    decode = Json.decodeFromString(serializers[kClass]!!, packet.data)
                }.nonNull {
                    println("Error on packet decode, packet id: ${packet.id}")
                    println("Source packet: $packet")
                    it.printStackTrace()
                    return
                }
                dataListen[kClass]!!(decode!!)
            }
            is RequestPacket -> {
                val kClass = namesLookup[packet.id] ?: return
                requestListen[kClass]!!(Json.decodeFromString(serializers[kClass]!!, packet.data)){
                    sendPacket(ResponsePacket(names[it::class]!!, Json.encodeToString(serializers[it::class]!!, it), packet.returnId))
                }
            }
            is ResponsePacket -> {
                val listener = requestWait.remove(packet.returnId) ?: return
                val kClass = namesLookup[packet.id] ?: return
                listener(Json.decodeFromString(serializers[kClass]!!, packet.data))
            }
            is ClosePacket -> onClose()
            is BroadcastPacket -> {
                if(onBroadcast){
                    broadcast(packet)
                    return
                }
                val kClass = namesLookup[packet.id] ?: return
                val decoded = Json.decodeFromString(serializers[kClass]!!, packet.data)
                dataListen[kClass]!!(decoded)
            }
            is ProtocolLevelReqPacket -> error("")
            is ProtocolLevelResPacket -> error("")
        }
    }
}