package tower.impl

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import pluginloader.api.caching
import pluginloader.api.nonNull
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

internal class Client(private val host: String, private val port: Int, private val invoke: (SystemPacket) -> Unit, private val onInit: (ChannelFuture) -> Unit = {}): Runnable {
    @Volatile
    private var needToClose = AtomicReference<ChannelFuture?>()
    private val started = AtomicBoolean(false)
    private val packets = ConcurrentLinkedQueue<SystemPacket>()
    private val lock = ReentrantLock()

    override fun run(){
        val workerGroup: EventLoopGroup = NioEventLoopGroup()

        try {
            val b = Bootstrap()
            b.group(workerGroup)
            b.channel(NioSocketChannel::class.java)
            b.option(ChannelOption.SO_KEEPALIVE, true)
            b.handler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                            NettyPacketDecodeFrame(), NettyJsonPacketDecoder(),
                            NettyPacketEncodeFrame(), NettyJsonPacketEncoder(),
                            object : ChannelInboundHandlerAdapter() {
                                override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                                    if(msg == ClosePacket){
                                        ctx.close()
                                        return
                                    }
                                    invoke(msg as SystemPacket)
                                }

                                override fun channelActive(ctx: ChannelHandlerContext) {
                                    println("Client channel init")
                                }

                                override fun channelInactive(ctx: ChannelHandlerContext?) {
                                    invoke(ClosePacket)
                                }

                                override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                                    cause.printStackTrace()
                                    ctx.close()
                                }
                            })
                }
            })
            val time = System.currentTimeMillis()
            var tr = 1
            var f: ChannelFuture? = null
            while(true) {
                try {
                    f = b.connect(host, port)
                    f.get(5, TimeUnit.SECONDS)
                    needToClose.set(f)
                    break
                } catch (ex: Throwable) {
                    println("Error on connect, try: ${tr++}, wait 1.5 second")
                    Thread.sleep(1500)
                }
                if(f != null && needToClose.get() == null){
                    caching {
                        f.cancel(true)
                    }.nonNull {
                        println("Error on cancel connect")
                        it.printStackTrace()
                    }
                }
            }
            val timeEnd = System.currentTimeMillis()
            caching{onInit(f!!)}?.printStackTrace()
            lock.lock()
            started.set(true)
            val list = ArrayList<SystemPacket>()
            packets.forEach{list.add(it)}
            packets.clear()
            lock.unlock()
            list.forEach{f!!.channel().writeAndFlush(it)}
            println("Client connected in ${timeEnd - time}ms")

            f!!.channel().closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
        }
    }

    fun connect(){
        close()
        Thread(this).start()
    }

    fun close(){
        val lock = needToClose.get() ?: return
        val ch = lock.channel()
        if(ch != null){
            if(ch.isOpen || ch.isActive)ch.close()
        }
        needToClose.set(null)
    }

    fun writeAndFlush(packet: SystemPacket){
        if(started.get().not()){
            if(lock.tryLock()) {
                packets.add(packet)
                lock.unlock()
                return
            }
        }
        val lock = needToClose.get() ?: return
        lock.channel().writeAndFlush(packet)
    }
}