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

	final static AsyncHttpClientConfig ASYNC_CFG = new AsyncHttpClientConfig.Builder()
		.setCompressionEnforced(true)
		.setConnectTimeout(1000)
		.setRequestTimeout(1000)
		.setUserAgent("Opera/9.80 (X11; Linux x86_64) Presto/2.12.388 Version/12.16")
		.setFollowRedirect(true)
		.build();
	
	private final AsyncHttpClient httpClient = new AsyncHttpClient(ASYNC_CFG);
	private final Redisson storage = Redisson.create();
	private final ExecutorService pool = Executors.newCachedThreadPool(new MinPriorityThreadFactory());
	private final PlacementsMapping placementsMap = new PlacementsMapping(storage, pool);
	private final DurationSettings durationsConfig = new DurationSettings(storage, pool);
	private final MemoryLogStorage memLogStorage = new MemoryLogStorage(storage);
	private final AdProcessing adProcessing = new AdProcessing();
	private final DirWatchDog timeTableWatchDog = new DirWatchDog(storage, pool, adProcessing);	
	private final CookieFabric cookieFabric;	
	
    @Override
    public void initChannel(SocketChannel ch) {
		ch.config().setTcpNoDelay(true);
		ch.config().setConnectTimeoutMillis(3000);
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpRequestHandler(this));
    }
    
	AsyncHttpClient getHttpClient() {
		return httpClient;
	}

	PlacementsMapping getPlacementsMap() {
		return placementsMap;
	}

	DurationSettings getDurationsConfig() {
		return durationsConfig;
	}

	MemoryLogStorage getMemLogStorage() {
		return memLogStorage;
	}

	DirWatchDog getTimeTableWatchDog() {
		return timeTableWatchDog;
	}

	AdProcessing getAdProcessing() {
		return adProcessing;
	}

	CookieFabric getCookieFabric() {
		return cookieFabric;
	}
}

class MinPriorityThreadFactory implements ThreadFactory {
	public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setPriority(Thread.MIN_PRIORITY);
	    	return t;
	}
 }