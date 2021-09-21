package texttower

import configs.Conf
import kotlinx.serialization.Serializable
import pluginloader.api.Load
import pluginloader.api.LoaderPlugin
import pluginloader.api.onlinePlayers
import provide.provide
import provide.registerProvider
import text.Broadcast
import text.BroadcastChat
import text.BroadcastLocal
import tower.api.Packet
import tower.api.TowerConnect

@Conf
internal var config = Config()

@Load
internal fun load(plugin: LoaderPlugin){
    if(!config.chatRegister && !config.globalRegister)return
    val connect = TowerConnect(plugin) ?: return
    connect.register("chat_packet", plugin, ChatPacket::class, ChatPacket.serializer()){onlinePlayers.forEach{p -> p.sendMessage(it.message)}}
    if(config.globalRegister)plugin.registerProvider(Broadcast{connect.broadcast(ChatPacket(it))})
    if(config.chatRegister)plugin.registerProvider(BroadcastChat{it, _ -> connect.broadcast(ChatPacket(it))})
}

@Serializable
private class ChatPacket(val message: String): Packet

@Serializable
internal class Config(val chatRegister: Boolean = true, val globalRegister: Boolean = true)