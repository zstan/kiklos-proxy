package kiklos.proxy.core;


import io.netty.handler.ssl.SslContext;
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
import ru.amberdata.dal.DataAccess;
import ru.amberdata.dal.pg.PgDataAccessImpl;

public class HttpServerPipelineFactory extends ChannelInitializer<SocketChannel> {

	private final SslContext sslContext;

	public HttpServerPipelineFactory(SslContext context) {
		this.sslContext = context;
		cookieFabric = CookieFabric.buildCookieFabric();
        if (dal == null)
            System.exit(100);
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
    private final PgDataAccessImpl dal = PgDataAccessImpl.build();

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
		if (this.sslContext != null) {
			p.addLast(this.sslContext.newHandler(ch.alloc()));
		}
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

    public DataAccess getDal() {
        return dal;
    }
}

