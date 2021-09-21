package tower.impl

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.util.concurrent.ThreadFactory

internal class Server(private val port: Int, private val invoke: (Channel, SystemPacket) -> Unit): Runnable{
    private var needToClose: ChannelFuture? = null

    override fun run() {
        val bossGroup: EventLoopGroup = NioEventLoopGroup(1, ThreadFactory{Thread(it, "Boss group")})
        val workerGroup: EventLoopGroup = NioEventLoopGroup()

        try {
            val b = ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channelFactory(ChannelFactory<ServerChannel>{NioServerSocketChannel()})
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline()
                            .addLast(
                                    NettyPacketDecodeFrame(), NettyJsonPacketDecoder(),
                                    NettyPacketEncodeFrame(), NettyJsonPacketEncoder(),
                                    object: ChannelInboundHandlerAdapter() {
                                        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                                            if(msg == ClosePacket){
                                                ctx.close()
                                                return
                                            }
                                            invoke(ctx.channel(), msg as SystemPacket)
                                        }

                                        override fun channelInactive(ctx: ChannelHandlerContext) {
                                            invoke(ctx.channel(), ClosePacket)
                                        }

                                        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                                            cause.printStackTrace()
                                            ctx.close()
                                        }


                            })
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

            val f = b.bind(port).sync()
            needToClose = f
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
}