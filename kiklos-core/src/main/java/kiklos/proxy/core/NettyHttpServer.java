package kiklos.proxy.core;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
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
		
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool(),
						4));
		
		bootstrap.setPipelineFactory(new HttpServerPipelineFactory());
		
		bootstrap.bind(new InetSocketAddress(80));
		
	}
}
