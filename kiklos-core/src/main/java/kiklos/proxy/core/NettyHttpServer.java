package kiklos.proxy.core;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.concurrent.Executors;

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
	public static void main(String[] args)
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
		
/*		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool(),
						procCount * 2));		
*/		
		//bootstrap.han setPipelineFactory(new HttpServerPipelineFactory());
		
		//bootstrap.bind(new InetSocketAddress(80));
        
        Channel ch;
		try {
			ch = bootstrap.bind(80).sync().channel();
			ch.closeFuture().sync();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}                		
	}
}
