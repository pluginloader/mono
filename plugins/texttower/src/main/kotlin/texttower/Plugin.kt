package texttower

import configs.Conf
import configs.conf
import kotlinx.serialization.Serializable
import pluginloader.api.Load
import pluginloader.api.LoaderPlugin
import pluginloader.api.Plugin
import pluginloader.api.onlinePlayers
import provide.provide
import provide.registerProvider
import text.Broadcast
import text.BroadcastChat
import text.BroadcastLocal
import tower.api.Packet
import tower.api.TowerConnect

@Load
internal fun Plugin.load(){
    val config = conf(::Config)
    if(!config.chatRegister && !config.globalRegister)return
    val connect = TowerConnect(this) ?: return
    connect.register("chat_packet", this, ChatPacket::class, ChatPacket.serializer()){onlinePlayers.forEach{p -> p.sendMessage(it.message)}}
    if(config.globalRegister)registerProvider(Broadcast{connect.broadcast(ChatPacket(it))})
    if(config.chatRegister)registerProvider(BroadcastChat{it, _ -> connect.broadcast(ChatPacket(it))})
}

@Serializable
private class ChatPacket(val message: String): Packet

@Serializable
internal class Config{
    val chatRegister = true
    val globalRegister = true
}