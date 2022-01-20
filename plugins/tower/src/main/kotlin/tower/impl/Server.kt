package tower.impl

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadFactory
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger

internal class Server(private val port: Int, private val invoke: (Channel, SystemPacket) -> Unit): Runnable{
    private var needToClose: ChannelFuture? = null

    override fun run() {
        run(null)
    }

    private fun run(future: CompletableFuture<Void>?) {
        val bossGroup: EventLoopGroup = NioEventLoopGroup(1, ThreadFactory{Thread(it, "Boss group")})
        val workerGroup: EventLoopGroup = NioEventLoopGroup()

        try {
            val b = ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channelFactory(ChannelFactory<ServerChannel>{NioServerSocketChannel()})
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.attr(protocolLevelEncoding).set(ProtocolLevel.NONE)
                        ch.attr(protocolLevelDecoding).set(ProtocolLevel.NONE)
                        ch.pipeline()
                            .addLast(
                                    NettyFullJsonDecoder(), NettyFullJsonEncoder(),
                                    object: ChannelInboundHandlerAdapter() {
                                        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                                            if(msg == ClosePacket){
                                                ctx.close()
                                                return
                                            }
                                            msg as SystemPacket
                                            if(msg is ProtocolLevelReqPacket){
                                                var lvl = msg.level
                                                if(lvl >= ProtocolLevel.values.size || lvl < 0)lvl = ProtocolLevel.values.size - 1
                                                ch.writeAndFlush(ProtocolLevelResPacket(lvl))
                                                return
                                            }else if(msg is ProtocolLevelResPacket){
                                                ch.attr(protocolLevelDecoding).set(ProtocolLevel.values[msg.level])
                                                return
                                            }
                                            invoke(ctx.channel(), msg)
                                        }

                                        override fun channelInactive(ctx: ChannelHandlerContext) {
                                            invoke(ctx.channel(), ClosePacket)
                                        }

                                        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                                            //cause.printStackTrace()
                                            ctx.close()
                                        }
                            })
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

            val f = b.bind(port).sync()
            needToClose = f
            future?.complete(null)
            println("Server startup")
            f.channel().closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

    fun close(){
        val lock = needToClose ?: return
        lock.channel()?.close()
    }

    fun startSync(){
        val future = CompletableFuture<Void>()
        Thread{run(future)}.start()
        future.get()
    }
}