package kiklos.proxy.core;

import java.io.InputStream;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

public class TimeTableResponder
{
	public static void main(String[] args) throws Exception
	{		
	    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	    StatusPrinter.print(lc);
	    InputStream resource = TimeTableResponder.class.getResourceAsStream("logback.xml");
	    System.out.println(resource);
	    int procCount = Runtime.getRuntime().availableProcessors();
	    System.out.println("system procs count: " + procCount);
	    
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup(procCount * 4);
        
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
        	.channel(NioServerSocketChannel.class)
        	.handler(new LoggingHandler(LogLevel.WARN))
        	.childHandler(new HttpServerPipelineFactory());

/*        String[] portsArr = ports.trim().split(",");
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
        }*/
		
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

	private static class HttpServerPipelineFactory extends ChannelInitializer<SocketChannel> {
		private static Configuration config = new Configuration();

		@Override
		public void initChannel(SocketChannel ch) {
			ch.config().setTcpNoDelay(true);
			ch.config().setConnectTimeoutMillis(config.getConnectTimeout());
			ChannelPipeline p = ch.pipeline();
			p.addLast(new HttpServerCodec());
            p.addLast("compressor", new HttpContentCompressor(8));
			p.addLast(new HttpRequestHandler(config));
		}
	}
}
