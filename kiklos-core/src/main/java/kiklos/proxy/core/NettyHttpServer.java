package kiklos.proxy.core;

import java.io.InputStream;

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
	    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	    StatusPrinter.print(lc);
	    InputStream resource = NettyHttpServer.class.getResourceAsStream("logback.xml");
	    System.out.println(resource);
	    int procCount = Runtime.getRuntime().availableProcessors();
	    System.out.println("proc count: " + procCount);
	    
        EventLoopGroup bossGroup = new NioEventLoopGroup(procCount * 2);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
        	.channel(NioServerSocketChannel.class)
        	.handler(new LoggingHandler(LogLevel.INFO))
        	.childHandler(new HttpServerPipelineFactory());
		
		try {
			Channel ch = bootstrap.bind(80).sync().channel();
			ch.closeFuture().sync();
		 } finally {
		        bossGroup.shutdownGracefully();
		        workerGroup.shutdownGracefully();

		        bossGroup.terminationFuture().sync();
		        workerGroup.terminationFuture().sync();
		 }          		
	}
}
