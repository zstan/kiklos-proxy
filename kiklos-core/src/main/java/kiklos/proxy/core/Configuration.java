package kiklos.proxy.core;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import kiklos.planner.DurationSettings;
import kiklos.tv.timetable.AdProcessing;
import kiklos.tv.timetable.DirWatchDog;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class Configuration {
    public static final String EMPTY_VAST = "<VAST version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"oxml.xsd\" />";
    public static final String EMPTY_VAST_NO_AD = "<VAST version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"oxml.xsd\" <no_ad_for_this_time/>/>";
    public static final String XML_CONTENT_TYPE = "application/xml; charset=" + StandardCharsets.UTF_8.name();
    public static final String ALLOW_ACC_CONTROL = "http://static.1tv.ru";
    public static final String DURATION = "t";
    public static final String CHANNEL = "ch";
    public static final String ID = "id";

    private final AsyncHttpClient httpClient = asyncHttpClient(ASYNC_CFG);
    private final RedissonClient storage = Redisson.create();
    private final ExecutorService minPriorityPool = Executors.newCachedThreadPool(r ->
        {Thread t = new Thread(r, "min-priority-pool"); t.setPriority(Thread.MIN_PRIORITY); return t;});
    private final PlacementsMapping placementsMap = new PlacementsMapping(storage, minPriorityPool);
    private final DurationSettings durationsConfig = new DurationSettings(storage, minPriorityPool);
    private final MemoryLogStorage memLogStorage = new MemoryLogStorage(storage, minPriorityPool);
    private final AdProcessing adProcessing = new AdProcessing();
    private final DirWatchDog timeTableWatchDog = new DirWatchDog(storage, minPriorityPool, adProcessing);
    private final CookieFabric cookieFabric = new CookieFabric();
    private static final int timeout = 5000;

    private final static AsyncHttpClientConfig ASYNC_CFG = new DefaultAsyncHttpClientConfig.Builder()
            .setCompressionEnforced(true)
            .setConnectTimeout(timeout)
            .setRequestTimeout(timeout)
            .setUserAgent("Opera/9.80 (X11; Linux x86_64) Presto/2.12.388 Version/12.16")
            .setFollowRedirect(true)
            .setTcpNoDelay(true)
            .build();

    //private NettyAsyncHttpProviderConfig providerConfig = new NettyAsyncHttpProviderConfig();

    public Configuration() {
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
