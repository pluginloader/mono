package tower.impl

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.util.AttributeKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pluginloader.api.caching
import pluginloader.api.nonNull
import java.nio.ByteBuffer

@Serializable
private data class JsonPacket(val id: String, val json: String)

internal val protocolLevelEncoding = AttributeKey.valueOf<ProtocolLevel>("plu_tower_protocol_level_encoding")
internal val protocolLevelDecoding = AttributeKey.valueOf<ProtocolLevel>("plu_tower_protocol_level_decoding")

internal enum class ProtocolLevel{
    NONE,
    FIRST;

    companion object{
        val values = values()
    }
}

internal class NettyFullJsonEncoder: MessageToByteEncoder<SystemPacket>(){
    override fun encode(ctx: ChannelHandlerContext, msg: SystemPacket, out: ByteBuf) {
        caching {
            val level = ctx.channel().attr(protocolLevelEncoding).get()
            if(level == ProtocolLevel.NONE) {
                val jsonObj = JsonObject(
                    mapOf(
                        "id" to JsonPrimitive(SystemPacket.getID(msg)),
                        "json" to JsonPrimitive(SystemPacket.serializeJson(msg))
                    )
                )
                val json = jsonObj.toString().toByteArray()
                //Json encoding in this situation unstable, I don't why, and replaced with upper simple encode

                //val packet = JsonPacket(SystemPacket.getID(msg), SystemPacket.serializeJson(msg))
                //val json = Json.encodeToString(JsonPacket.serializer(), packet).toByteArray()
                //"{\"id\":\"init\",\"json\":\"{\\\"name\\\":\\\"lol\\\"}\"}".toByteArray()
                //if(String(json) != "{\"id\":\"init\",\"json\":\"{\\\"name\\\":\\\"lol\\\"}\"}" && String(json) != "{\"id\":\"close\",\"json\":\"\"}"){
                //    println("Just json, meh '${String(json)}'\nSource must be '{\"id\":\"init\",\"json\":\"{\\\"name\\\":\\\"lol\\\"}\"}'")
                //    exitProcess(-1)
                //}
                val buffer = ByteBuffer.allocate(json.size + 4)
                buffer.putInt(json.size)
                buffer.put(json)
                out.writeBytes(buffer.array())
            }else{
                //println("Send packet ${msg} binary dump: ${SystemPacket.binarySerialize(msg).joinToString(", ")}")
                out.writeBytes(SystemPacket.binarySerialize(msg))
            }
            if(msg is ProtocolLevelResPacket) {
                ctx.channel().attr(protocolLevelEncoding).set(ProtocolLevel.values[msg.level])
            }
        }.nonNull {
            it.printStackTrace()
            caching{println(JsonPacket(SystemPacket.getID(msg), SystemPacket.serializeJson(msg)))}
            throw it
        }
    }
}

internal class NettyFullJsonDecoder: ByteToMessageDecoder(){
    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        var str: String? = null
        var packetSize = 0
        caching {
            if (!buf.isReadable) return
            if (buf.readableBytes() < 4) return
            buf.markReaderIndex()
            val level = ctx.channel().attr(protocolLevelDecoding).get()
            packetSize = buf.readInt()
            if(packetSize > buf.readableBytes()){
                buf.resetReaderIndex()
                return
            }

            val packet = if(level == ProtocolLevel.NONE){
                val rawJson = ByteArray(packetSize)
                buf.readBytes(rawJson)
                str = String(rawJson)
                val json = Json.decodeFromString(JsonPacket.serializer(), str!!)
                SystemPacket.deserializeJson(json.id, json.json)
            }else{
                val id = buf.readByte()
                val bytes = ByteArray(packetSize - 1)
                buf.readBytes(bytes)
                SystemPacket.binaryDeserialize(id, bytes)
            }

            packet?.let{
                out.add(it)
            }
        }.nonNull {
            println("Error on decoding packet '${str}', size: $packetSize")
            it.stackTraceToString()
        }
    }
}