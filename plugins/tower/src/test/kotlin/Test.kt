package tower.test

import org.junit.jupiter.api.Test
import tower.impl.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

class NetworkTest{
    private val disable = true
    //Disabled by reason of bad firewall, using for tests
    @Test
    fun test(){
        if(disable)return
        val counter = AtomicInteger(0)
        val tower = Server(4444){channel, packet -> channel.writeAndFlush(packet)}
        tower.startSync()
        val clientCount = 20
        repeat(clientCount) {
            Thread {
                var client: Client? = null
                val future = CompletableFuture<Void>()
                client = Client("localhost", 4444, {client!!.close()}, {
                    it.channel().writeAndFlush(InitPacket("lol"))
                    future.complete(null)
                })
                client.startSync()
                future.get()
                client.close()
            }.start()
        }
        val count = clientCount * 4
        var time = 0
        while (counter.get() != clientCount * 2) {
            if(time++ == count){
                println("Timeout :/")
                exitProcess(-1)
            }
            Thread.sleep(10)
        }
        tower.close()
    }
}