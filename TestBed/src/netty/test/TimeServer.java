package netty.test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ReferenceCountUtil;

public class TimeServer
{
	class DiscardServerHandler extends ChannelInboundHandlerAdapter
	{

		public void channelRead(ChannelHandlerContext ctx, Object msg)
		{
			ByteBuf in = (ByteBuf) msg;
			try
			{
				while (in.isReadable())
				{ // (1)
					System.out.print((char) in.readByte());
					System.out.flush();
				}
			} finally
			{
				ReferenceCountUtil.release(msg); // (2)
			}
		}

		@Override
		public void channelActive(final ChannelHandlerContext ctx)
		{ // (1)
			final ByteBuf time = ctx.alloc().buffer(4); // (2)
			time.writeInt((int) (System.currentTimeMillis() / 1000L + 2208988800L));

			final ChannelFuture f = ctx.writeAndFlush(time); // (3)
			f.addListener(new ChannelFutureListener()
			{
				@Override
				public void operationComplete(ChannelFuture future)
				{
					assert f == future;
					ctx.close();
				}
			}); // (4)
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
		{
			cause.printStackTrace();
			ctx.close();
		}
	}

	private int port;

	public TimeServer(int port)
	{
		this.port = port;
	}

	public void run() throws Exception
	{
		EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try
		{
			ServerBootstrap b = new ServerBootstrap(); // (2)
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class) // (3)
					.childHandler(new ChannelInitializer<SocketChannel>()
					{ // (4)
						@Override
						public void initChannel(SocketChannel ch) throws Exception
						{
							ch.pipeline().addLast(new DiscardServerHandler());
						}
					}).option(ChannelOption.SO_BACKLOG, 128)          // (5)
					.childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

			// Bind and start to accept incoming connections.
			ChannelFuture f = b.bind(port).sync(); // (7)

			// Wait until the server socket is closed.
			// In this example, this does not happen, but you can do that to
			// gracefully
			// shut down your server.
			f.channel().closeFuture().sync();
		} finally
		{
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception
	{
		int port;
		if (args.length > 0)
		{
			port = Integer.parseInt(args[0]);
		} else
		{
			port = 8080;
		}
		new TimeServer(port).run();
	}
}
