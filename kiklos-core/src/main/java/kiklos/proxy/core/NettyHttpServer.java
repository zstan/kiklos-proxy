package kiklos.proxy.core;

import java.io.File;
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

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

public class NettyHttpServer
{
	public static void main(String[] args) throws Exception
	{		
	    int procCount = Runtime.getRuntime().availableProcessors();
		String host = "127.0.0.1";
		String ports = "8080,8081";

		if (args.length != 0) {
			System.out.println("bind to host: " + args[0] + " port(s): " + args[1]);
			host = args[0];
			ports = args[1];
		}
	    
        EventLoopGroup bossGroup = new NioEventLoopGroup(procCount * 2);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
        	.channel(NioServerSocketChannel.class)
        	.handler(new LoggingHandler(LogLevel.INFO));
		
		try {

			String[] portsArr = ports.trim().split(",");
			Collection<Channel> channels = new ArrayList<>(portsArr.length);

			for (String port : portsArr) {

				bootstrap.childHandler(new HttpServerPipelineFactory(null));

				if (port.indexOf("443") != -1) {
					SslContext sslCtx = SslContextBuilder.forServer(new File("/etc/apache2/ssl-crt/STAR_tvdesk_ru.crt"), new File("/etc/apache2/ssl-crt/private_tvdesk_ru.key")).build();
					//SelfSignedCertificate ssc = new SelfSignedCertificate();
					//SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
					bootstrap.childHandler(new HttpServerPipelineFactory(sslCtx));
				}
				Channel ch = bootstrap.bind(host, Integer.valueOf(port)).sync().channel();
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
