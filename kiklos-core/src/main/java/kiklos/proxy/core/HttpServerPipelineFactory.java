package kiklos.proxy.core;


import kiklos.planner.DurationSettings;
import kiklos.tv.timetable.AdProcessing;
import kiklos.tv.timetable.DirWatchDog;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.redisson.Redisson;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class HttpServerPipelineFactory extends ChannelInitializer<SocketChannel> {

	public HttpServerPipelineFactory() {
		cookieFabric = CookieFabric.buildCookieFabric();
	}

	private final Redisson storage = Redisson.create();
	AsyncHttpClientConfig cfg = new AsyncHttpClientConfig.Builder()
		.setCompressionEnforced(true)
		.setConnectTimeout(1000)
		.setRequestTimeout(1000)
		.setUserAgent("Opera/9.80 (X11; Linux x86_64) Presto/2.12.388 Version/12.16")
		.setFollowRedirect(true)
		.build();
	
	//private final AsyncHttpClient httpClient = new AsyncHttpClient(cfg);
	private final MemoryLogStorage memLogStorage = new MemoryLogStorage(storage);
	private final CookieFabric cookieFabric;
	
    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpRequestHandler(this));
    }
    
/*	public AsyncHttpClient getHttpClient() {
		return httpClient;
	}*/

	public MemoryLogStorage getMemLogStorage() {
		return memLogStorage;
	}

	public CookieFabric getCookieFabric() {
		return cookieFabric;
	}
    
}

