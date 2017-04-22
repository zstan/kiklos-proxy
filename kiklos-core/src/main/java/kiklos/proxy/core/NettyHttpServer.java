package kiklos.proxy.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

public class NettyHttpServer
{
	public static void main(String[] args) throws Exception
	{		
	    int procCount = Runtime.getRuntime().availableProcessors();
	    System.out.println("proc count: " + procCount);
		String host = "127.0.0.1";

		if (args.length != 0) {
			System.out.println("bind to host: " + args[0]);
			host = args[0];
		}
	    
        EventLoopGroup bossGroup = new NioEventLoopGroup(procCount * 2);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
        	.channel(NioServerSocketChannel.class)
        	.handler(new LoggingHandler(LogLevel.INFO))
        	.childHandler(new HttpServerPipelineFactory());
		
		try {

			List<Integer> ports = Arrays.asList(8080, 8081);
			Collection<Channel> channels = new ArrayList<>(ports.size());

			for (int port : ports) {
				Channel ch = bootstrap.bind(host, port).sync().channel();
				channels.add(ch);
			}

			for (Channel ch : channels) {
				ch.closeFuture().sync();
			}

		 } finally {
		        bossGroup.shutdownGracefully();
		        workerGroup.shutdownGracefully();
		 }
	}
}
