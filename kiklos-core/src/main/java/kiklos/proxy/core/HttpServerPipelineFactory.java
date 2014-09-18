package kiklos.proxy.core;


import kiklos.planner.DurationSettings;
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
		.setCompressionEnabled(true)
		.setConnectionTimeoutInMs(1000)
		.setRequestTimeoutInMs(1000)
		.setUserAgent("Opera/9.80 (X11; Linux x86_64) Presto/2.12.388 Version/12.16")
		.setFollowRedirects(true)
		.build();
	
	private final AsyncHttpClient httpClient = new AsyncHttpClient(cfg);
	private final ExecutorService pool = Executors.newCachedThreadPool(new SimpleThreadFactory());
	private final PlacementsMapping placementsMap = new PlacementsMapping(storage, pool);
	private final DurationSettings durationsConfig = new DurationSettings(storage, pool);
	private final MemoryLogStorage memLogStorage = new MemoryLogStorage(storage);
	private final DirWatchDog timeTableWatchDog = new DirWatchDog(storage, pool);
	private final CookieFabric cookieFabric;	
	
    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpRequestHandler(httpClient, placementsMap, memLogStorage, cookieFabric, durationsConfig, timeTableWatchDog));
    }
}

class SimpleThreadFactory implements ThreadFactory {
	   public Thread newThread(Runnable r) {
		 Thread t = new Thread(r);
		 t.setPriority(Thread.MIN_PRIORITY);
	     return t;
	   }
 }


