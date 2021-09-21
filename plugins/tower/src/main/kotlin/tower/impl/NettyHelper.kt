package tower.impl

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import pluginloader.api.caching
import pluginloader.api.nonNull

@Serializable
private data class JsonPacket(val id: String, val json: String)

@Serializable
private class CborPacket(val id: String, val data: ByteArray)

internal class NettyCborPacketEncoder: MessageToByteEncoder<SystemPacket>() {
    override fun encode(ctx: ChannelHandlerContext, msg: SystemPacket, out: ByteBuf) {
        val packet = CborPacket(SystemPacket.getID(msg), SystemPacket.serializeCbor(msg))
        val data = Cbor.encodeToByteArray(CborPacket.serializer(), packet)
        out.writeBytes(data)
    }
}

internal class NettyCborPacketDecoder: ByteToMessageDecoder(){
    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        val size = buf.readableBytes()
        if(size == 0)return
        val raw = ByteArray(size)
        buf.readBytes(raw)
        val data = Cbor.decodeFromByteArray(CborPacket.serializer(), raw)
        SystemPacket.deserializeCbor(data.id, data.data)?.let{out.add(it)}
    }
}

internal class NettyJsonPacketEncoder: MessageToByteEncoder<SystemPacket>() {
    override fun encode(ctx: ChannelHandlerContext, msg: SystemPacket, out: ByteBuf) {
        caching {
            val packet = JsonPacket(SystemPacket.getID(msg), SystemPacket.serializeJson(msg))
            val json = Json.encodeToString(JsonPacket.serializer(), packet).toByteArray()
            out.writeBytes(json)
        }.nonNull {
            it.printStackTrace()
            caching{println(JsonPacket(SystemPacket.getID(msg), SystemPacket.serializeJson(msg)))}
            throw it
        }
    }
}

internal class NettyJsonPacketDecoder: ByteToMessageDecoder(){
    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        caching {
            val size = buf.readableBytes()
            if (size == 0) return
            val rawJson = ByteArray(size)
            buf.readBytes(rawJson)
            val json = Json.decodeFromString(JsonPacket.serializer(), String(rawJson))
            SystemPacket.deserializeJson(json.id, json.json)?.let { out.add(it) }
        }.nonNull {
            println("Error on decoding packet")
            throw it
        }
    }
}

internal class NettyPacketEncodeFrame: MessageToByteEncoder<ByteBuf>(){
    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
        caching {
            val size = msg.readableBytes()
            out.writeInt(size)
            out.writeBytes(msg, msg.readerIndex(), size)
        }.nonNull {
            println("Error on encode frame")
            it.printStackTrace()
            throw it
        }
    }
}

internal class NettyPacketDecodeFrame: ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        caching {
            if (!buf.isReadable) return
            if (buf.readableBytes() < 4) return
            buf.markReaderIndex()
            val size = buf.readInt()
            if (buf.readableBytes() < size) {
                buf.resetReaderIndex()
                return
            }
            out.add(buf.readBytes(size))
        }.nonNull {
            println("Error on decode frame")
            it.printStackTrace()
            throw it
        }
    }
}