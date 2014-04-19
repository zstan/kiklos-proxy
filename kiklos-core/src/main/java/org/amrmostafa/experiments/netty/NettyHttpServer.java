package org.amrmostafa.experiments.netty;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class NettyHttpServer
{
	public static void main(String[] args)
	{
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool(),
						4));
		
		bootstrap.setPipelineFactory(new HttpServerPipelineFactory());
		
		bootstrap.bind(new InetSocketAddress(80));
		
	}
}
