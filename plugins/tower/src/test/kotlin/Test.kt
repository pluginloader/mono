package tower.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tower.impl.Client
import tower.impl.InitPacket
import tower.impl.Server

@Serializable
private data class JsonPacket(val id: String, val json: String)

fun main() {
    println(JsonPacket::class.java.canonicalName)
    if(true)return
    val packet = JsonPacket("data", "{\"id\":\"save\",\"data\":\"{\\\"table\\\":\\\"morestats\\\",\\\"uuid\\\":{\\\"least\\\":-9113846697860385283,\\\"most\\\":-614855475102805527},\\\"data\\\":[123,34,115,116,97,116,115,34,58,123,34,108,101,118,101,108,34,58,49,46,48,125,125]}\"}")
    println(Json.encodeToString(JsonPacket.serializer(), packet))
    if(true)return
    val tower = Server(4444){channel, packet ->
        println("Server recived $packet")
        channel.writeAndFlush(packet)
    }
    Thread(tower).start()
    Thread.sleep(2000)
    repeat(30) {
        Thread {
            var client: Client? = null
            client = Client("localhost", 4444, { println("Client recieved $it") }) {
                println("write lol")
                it.channel().writeAndFlush(InitPacket("lol"))
                Thread.sleep(1000)
                client?.close()
            }
            client.connect()
        }.start()
    }
    Thread.sleep(2000)
    tower.close()
}