package tower.impl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer

internal sealed class SystemPacket{
    companion object{
        fun getID(packet: SystemPacket) = when(packet){
            is InitPacket -> "init"
            is ClosePacket -> "close"
            is DataPacket -> "data"
            is RequestPacket -> "request"
            is ResponsePacket -> "response"
            is BroadcastPacket -> "broadcast"
            is ProtocolLevelReqPacket -> "protocol_level_req"
            is ProtocolLevelResPacket -> "protocol_level_res"
        }

        @Suppress("UNCHECKED_CAST")
        fun serializeJson(packet: SystemPacket): String = when(packet){
            is InitPacket -> Json.encodeToString(InitPacket.serializer(), packet)
            is ClosePacket -> ""
            is DataPacket -> Json.encodeToString(DataPacket.serializer(), packet)
            is RequestPacket -> Json.encodeToString(RequestPacket.serializer(), packet)
            is ResponsePacket -> Json.encodeToString(ResponsePacket.serializer(), packet)
            is BroadcastPacket -> Json.encodeToString(BroadcastPacket.serializer(), packet)
            is ProtocolLevelReqPacket -> Json.encodeToString(ProtocolLevelReqPacket.serializer(), packet)
            is ProtocolLevelResPacket -> Json.encodeToString(ProtocolLevelResPacket.serializer(), packet)
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
                    "protocol_level_req" -> Json.decodeFromString(ProtocolLevelReqPacket.serializer(), json)
                    "protocol_level_res" -> Json.decodeFromString(ProtocolLevelResPacket.serializer(), json)
                    else -> null
                }
            }catch (ex: Throwable){
                println("source id: $id, json: $json")
                throw ex
            }
        }

        fun binarySerialize(packet: SystemPacket): ByteArray = when(packet){
            is InitPacket -> {
                val arr = packet.name.toByteArray()
                val buf = ByteBuffer.allocate(4 + 1 + 2 + arr.size)
                buf.putInt(buf.array().size - 4).put(0.toByte())
                buf.putShort(arr.size.toShort()).put(arr)
                buf.array()
            }//0
            is ClosePacket -> ClosePacket.constBytes//1
            is DataPacket -> {
                val id = packet.id.toByteArray()
                val data = packet.data.toByteArray()
                val buf = ByteBuffer.allocate(4 + 1 +
                        2 + id.size +
                        4 + data.size
                )
                buf.putInt(buf.array().size - 4).put(2.toByte())
                buf.putShort(id.size.toShort()).put(id)
                buf.putInt(data.size).put(data)
                buf.array()
            }//2
            is RequestPacket -> {
                val id = packet.id.toByteArray()
                val data = packet.data.toByteArray()
                val buf = ByteBuffer.allocate(4 + 1 +
                        2 + id.size +
                        4 + data.size +
                        4
                )
                buf.putInt(buf.array().size - 4).put(3.toByte())
                buf.putShort(id.size.toShort()).put(id)
                buf.putInt(data.size).put(data)
                buf.putInt(packet.returnId)
                buf.array()
            }//3
            is ResponsePacket -> {
                val id = packet.id.toByteArray()
                val data = packet.data.toByteArray()
                val buf = ByteBuffer.allocate(4 + 1 +
                        2 + id.size +
                        4 + data.size +
                        4
                )
                buf.putInt(buf.array().size - 4).put(4.toByte())
                buf.putShort(id.size.toShort()).put(id)
                buf.putInt(data.size).put(data)
                buf.putInt(packet.returnId)
                buf.array()
            }//4
            is BroadcastPacket -> {
                val id = packet.id.toByteArray()
                val data = packet.data.toByteArray()
                val buf = ByteBuffer.allocate(4 + 1 +
                        2 + id.size +
                        4 + data.size
                )
                buf.putInt(buf.array().size - 4).put(5.toByte())
                buf.putShort(id.size.toShort()).put(id)
                buf.putInt(data.size).put(data)
                buf.array()
            }//5
            is ProtocolLevelReqPacket -> error("Can't be binary serialized")
            is ProtocolLevelResPacket -> error("Can't be binary serialized")
        }

        fun binaryDeserialize(id: Byte, array: ByteArray): SystemPacket? {
            val buf = ByteBuffer.wrap(array)
            return when(id.toInt()) {
                0 -> {
                    val size = buf.short.toInt()
                    InitPacket(String(array, 2, size))
                }
                1 -> ClosePacket
                2 -> {
                    val idSize = buf.short.toInt()
                    val packetId = String(array, 2, idSize)
                    buf.position(2 + idSize)
                    val dataSize = buf.int
                    val data = String(array, buf.position(), dataSize)
                    DataPacket(packetId, data)
                }
                3 -> {
                    val idSize = buf.short.toInt()
                    val packetId = String(array, 2, idSize)
                    buf.position(2 + idSize)
                    val dataSize = buf.int
                    val data = String(array, buf.position(), dataSize)
                    buf.position(array.size - 4)
                    RequestPacket(packetId, data, buf.int)
                }
                4 -> {
                    val idSize = buf.short.toInt()
                    val packetId = String(array, 2, idSize)
                    buf.position(2 + idSize)
                    val dataSize = buf.int
                    val data = String(array, buf.position(), dataSize)
                    buf.position(array.size - 4)
                    ResponsePacket(packetId, data, buf.int)
                }
                5 -> {
                    val idSize = buf.short.toInt()
                    val packetId = String(array, 2, idSize)
                    buf.position(2 + idSize)
                    val dataSize = buf.int
                    val data = String(array, buf.position(), dataSize)
                    BroadcastPacket(packetId, data)
                }
                else -> null
            }
        }
    }
}

@Serializable
internal data class InitPacket(val name: String): SystemPacket()

@Serializable
internal object ClosePacket: SystemPacket(){
    val constBytes = byteArrayOf(0, 0, 0, 0, 1)
}

@Serializable
internal data class DataPacket(val id: String, val data: String): SystemPacket()

@Serializable
internal data class RequestPacket(val id: String, val data: String, val returnId: Int): SystemPacket()

@Serializable
internal data class ResponsePacket(val id: String, val data: String, val returnId: Int): SystemPacket()

@Serializable
internal data class BroadcastPacket(val id: String, val data: String): SystemPacket()

@Serializable
internal data class ProtocolLevelReqPacket(val level: Int): SystemPacket()

@Serializable
internal data class ProtocolLevelResPacket(val level: Int): SystemPacket()