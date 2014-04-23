package kiklos.proxy.core;


import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.redisson.Redisson;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class HttpServerPipelineFactory implements ChannelPipelineFactory {
	
	private final Redisson storage = Redisson.create();
	AsyncHttpClientConfig cfg = new AsyncHttpClientConfig.Builder()
		.setCompressionEnabled(true)
		.setConnectionTimeoutInMs(1000)
		.setRequestTimeoutInMs(1000)
		.setUserAgent("Opera/9.80 (X11; Linux x86_64) Presto/2.12.388 Version/12.16")
		.build();
	
	private final AsyncHttpClient cl = new AsyncHttpClient(cfg);
	private final PlacementsMapping plMap = new PlacementsMapping(storage);
	private final MemoryLogStorage memLogStorage = new MemoryLogStorage(storage);
	
	public ChannelPipeline getPipeline() throws Exception {
		// Create a default channel pipeline implementation 
		ChannelPipeline pipeline = Channels.pipeline();
		
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("handler", new HttpRequestHandler(cl, plMap, memLogStorage));
		
		return pipeline;
	}
}