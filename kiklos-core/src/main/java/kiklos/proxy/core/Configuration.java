package kiklos.proxy.core;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;
import kiklos.planner.DurationSettings;
import kiklos.tv.timetable.AdProcessing;
import kiklos.tv.timetable.DirWatchDog;
import org.redisson.Redisson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Configuration {
    private final AsyncHttpClient httpClient = new AsyncHttpClient(ASYNC_CFG);
    private final Redisson storage = Redisson.create();
    private final ExecutorService minPriorityPool = Executors.newCachedThreadPool(r ->
        {Thread t = new Thread(r, "min-priority-pool"); t.setPriority(Thread.MIN_PRIORITY); return t;});
    private final PlacementsMapping placementsMap = new PlacementsMapping(storage, minPriorityPool);
    private final DurationSettings durationsConfig = new DurationSettings(storage, minPriorityPool);
    private final MemoryLogStorage memLogStorage = new MemoryLogStorage(storage);
    private final AdProcessing adProcessing = new AdProcessing();
    private final DirWatchDog timeTableWatchDog = new DirWatchDog(storage, minPriorityPool, adProcessing);
    private final CookieFabric cookieFabric;

    private final static AsyncHttpClientConfig ASYNC_CFG = new AsyncHttpClientConfig.Builder()
            .setCompressionEnforced(true)
            .setConnectTimeout(1000)
            .setRequestTimeout(1000)
            .setUserAgent("Opera/9.80 (X11; Linux x86_64) Presto/2.12.388 Version/12.16")
            .setFollowRedirect(true)
            .build();

    private NettyAsyncHttpProviderConfig providerConfig = new NettyAsyncHttpProviderConfig();

    public Configuration() {
        cookieFabric = CookieFabric.buildCookieFabric();
/*        System.err.println(httpClient.getConfig().getClass().getName());
        System.err.println(httpClient.getConfig().getAsyncHttpProviderConfig().getClass().getName());
        NettyAsyncHttpProviderConfig cfg = (NettyAsyncHttpProviderConfig) httpClient.getConfig().getAsyncHttpProviderConfig();
        cfg.addProperty("tcpNoDelay", true);*/
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
