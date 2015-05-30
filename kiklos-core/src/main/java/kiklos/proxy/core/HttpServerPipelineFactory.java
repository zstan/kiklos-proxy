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
	
	private final AsyncHttpClient httpClient = new AsyncHttpClient(cfg);
	
	private final ExecutorService pool = Executors.newCachedThreadPool(new MinPriorityThreadFactory());
	private final PlacementsMapping placementsMap = new PlacementsMapping(storage, pool);
	private final DurationSettings durationsConfig = new DurationSettings(storage, pool);
	private final MemoryLogStorage memLogStorage = new MemoryLogStorage(storage);
	private final AdProcessing adProcessing = new AdProcessing();
	private final DirWatchDog timeTableWatchDog = new DirWatchDog(storage, pool, adProcessing);	
	private final CookieFabric cookieFabric;	
	
    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        //p.addLast(new HttpRequestHandler(httpClient, placementsMap, memLogStorage, cookieFabric, durationsConfig, timeTableWatchDog));
        p.addLast(new HttpRequestHandler(this));
    }
    
	public AsyncHttpClient getHttpClient() {
		return httpClient;
	}

	public PlacementsMapping getPlacementsMap() {
		return placementsMap;
	}

	public DurationSettings getDurationsConfig() {
		return durationsConfig;
	}

	public MemoryLogStorage getMemLogStorage() {
		return memLogStorage;
	}

	public DirWatchDog getTimeTableWatchDog() {
		return timeTableWatchDog;
	}

	public AdProcessing getAdProcessing() {
		return adProcessing;
	}

	public CookieFabric getCookieFabric() {
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


