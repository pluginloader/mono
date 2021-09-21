package tower.impl

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json

internal sealed class SystemPacket{
    companion object{
        fun getID(packet: SystemPacket) = when(packet){
            is InitPacket -> "init"
            is ClosePacket -> "close"
            is DataPacket -> "data"
            is RequestPacket -> "request"
            is ResponsePacket -> "response"
            is BroadcastPacket -> "broadcast"
        }

        fun serializeCbor(packet: SystemPacket): ByteArray = when(packet){
            is InitPacket -> Cbor.encodeToByteArray(InitPacket.serializer(), packet)
            is ClosePacket -> byteArrayOf()
            is DataPacket -> Cbor.encodeToByteArray(DataPacket.serializer(), packet)
            is RequestPacket -> Cbor.encodeToByteArray(RequestPacket.serializer(), packet)
            is ResponsePacket -> Cbor.encodeToByteArray(ResponsePacket.serializer(), packet)
            is BroadcastPacket -> Cbor.encodeToByteArray(BroadcastPacket.serializer(), packet)
        }

        fun deserializeCbor(id: String, data: ByteArray): SystemPacket? {
            return when (id) {
                "init" -> Cbor.decodeFromByteArray(InitPacket.serializer(), data)
                "close" -> ClosePacket
                "data" -> Cbor.decodeFromByteArray(DataPacket.serializer(), data)
                "request" -> Cbor.decodeFromByteArray(RequestPacket.serializer(), data)
                "response" -> Cbor.decodeFromByteArray(ResponsePacket.serializer(), data)
                "broadcast" -> Cbor.decodeFromByteArray(BroadcastPacket.serializer(), data)
                else -> null
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun serializeJson(packet: SystemPacket): String = when(packet){
            is InitPacket -> Json.encodeToString(InitPacket.serializer(), packet)
            is ClosePacket -> ""
            is DataPacket -> Json.encodeToString(DataPacket.serializer(), packet)
            is RequestPacket -> Json.encodeToString(RequestPacket.serializer(), packet)
            is ResponsePacket -> Json.encodeToString(ResponsePacket.serializer(), packet)
            is BroadcastPacket -> Json.encodeToString(BroadcastPacket.serializer(), packet)
        }

        fun deserializeJson(id: String, json: String): SystemPacket? {
            try {
                return when (id) {
                    "init" -> Json.decodeFromString(InitPacket.serializer(), json)
                    "close" -> ClosePacket
                    "data" -> Json.decodeFromString(DataPacket.serializer(), json)
                    "request" -> Json.decodeFromString(RequestPacket.serializer(), json)
                    "response" -> Json.decodeFromString(ResponsePacket.serializer(), json)
                    "broadcast" -> Json.decodeFromString(BroadcastPacket.serializer(), json)
                    else -> null
                }
            }catch (ex: Throwable){
                println("source id: $id, json: $json")
                throw ex
            }
        }
    }
}

@Serializable
internal data class InitPacket(val name: String): SystemPacket()

@Serializable
internal object ClosePacket : SystemPacket()

@Serializable
internal data class DataPacket(val id: String, val data: String): SystemPacket()

@Serializable
internal data class RequestPacket(val id: String, val data: String, val returnId: Int): SystemPacket()

@Serializable
internal data class ResponsePacket(val id: String, val data: String, val returnId: Int): SystemPacket()

@Serializable
internal data class BroadcastPacket(val id: String, val data: String): SystemPacket()