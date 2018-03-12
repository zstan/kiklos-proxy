package kiklos.proxy.core;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import kiklos.planner.DurationSettings;
import kiklos.planner.SimpleStrategy;
import kiklos.tv.timetable.AdProcessing;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;

import static io.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static io.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import kiklos.tv.timetable.Vast3Fabric;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class HttpRequestHandler extends ChannelInboundHandlerAdapter {
	private final AsyncHttpClient httpClient;
	private final PlacementsMapping placementsMapping;
	private final DurationSettings durationSettings;
	private final CookieFabric cookieFabric;
	static final String DEFAULT_CHANNEL = "408";
	private static final DateTimeFormatter datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestHandler.class);
    private final MemoryLogStorage memLogStorage;
    private final AdProcessing adProcessing;
    private final Configuration cfg;
	
	HttpRequestHandler(Configuration config) {
        this.cfg = config;
		httpClient = config.getHttpClient();
		placementsMapping = config.getPlacementsMap();
		memLogStorage = config.getMemLogStorage();
		cookieFabric = config.getCookieFabric();
		durationSettings = config.getDurationsConfig();
		adProcessing = config.getAdProcessing();
	}

	/**
	 * @param queryParams query params
	 * @return list for Ad uri`s
     */
	private List<String> reqTransformer(final String plID, final String account, final Map<String, List<String>> queryParams) {
        // пробуем найти уже замапленные vast url`s
        // редактируется в редис базе
        // redis-cli HSET "\".placements\"" "\"212\"" "[\"java.util.ArrayList\",[\"http://asdasdfasdf.com/\", \"http://asg.vidigital.ru/1/50006/c/v/2\", \"http://ib.adnxs.com/ptv?id=2504637\"]]"
        // берем vast xml или из базы редиса или из расписания, если попадаем в рекламный блок бьем его на части и каждой части смотрим в редис базу
        // за соотв. записями : redis-cli HSET "\".durations\"" "5" "\"http://ads.adfox.ru/216891/getCode?p1=bpvvo&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\""
        // так же для каждого канала в отдельности конфигурится допуски +- от рекламного блока.
        List<String> proxyAdURIs =  placementsMapping.getProxyURIList(plID);

        // Если не нашли в редис, лезем в расписание
        if (proxyAdURIs.isEmpty()) {
            int reqDuration = HelperUtils.getRequiredAdDuration(queryParams);
            LOG.debug("no correspond placement found, try to get from TimeTable req duration: {}", reqDuration);
            PairEx<Short, List<Short>> tt4ch = adProcessing.getAdListFromTimeTable(HelperUtils.getChannelFromParams(queryParams));
            if (tt4ch != null) {
                reqDuration = tt4ch.getKey();
                LOG.debug("reqDuration: {}", reqDuration);
                proxyAdURIs = SimpleStrategy.formAdList(durationSettings, reqDuration);
            } else {
                proxyAdURIs = Collections.emptyList(); // empty for unknown channel !!!
            }
        }

        if (LOG.isDebugEnabled())
            LOG.debug("proxyAdURIs size: {}", proxyAdURIs.size());

        if (!proxyAdURIs.isEmpty()) {

            // к нашей системе идем с запросом
            // http://178.170.237.19/22627[99]/tvd?id=111&puid30=12377&puid6=15&pr=123&eid1=12377:1:123&dl=http://1tv.ru/ott/:www.ru
            // выкидываем id все остальные параметры конкатенируем и передаем в AD
            //http://v.adfox.ru/{account}/getCode?pp=efi&ps=byof&p2=eyit&pfc=a&pfb=a&plp=a&pli=a&pop=a&pct=c&puid5=1&puid6={priority}
            // &puid25=1&puid30={placement}&pr={random}&eid1={placement}:{session}:{random}&dl=http://1tv.ru/ott/:{referer}
            // в редис сетим соотв.
            // redis-cli HSET "\".placements\"" "\"111\"" "[\"java.util.ArrayList\",[\"http://v.adfox.ru/{account}/getCode?pp=efi&ps=byof&p2=eyit&pfc=a&pfb=a&plp=a&pli=a&pop=a&pct=c&puid5=1&puid25=1\"]]"

            queryParams.remove(Configuration.ID);
            queryParams.remove(Configuration.DURATION);
            queryParams.remove(Configuration.CHANNEL);

            String query = HelperUtils.queryParams2String(queryParams);

            if (!Strings.isNullOrEmpty(query)) {
                List<String> transformedAdURIs = new ArrayList<>(proxyAdURIs.size());

                for (String uri: proxyAdURIs) {
                    if (!Strings.isNullOrEmpty(account))
                        uri = uri.replace("{account}", account);

                    transformedAdURIs.add(uri + (queryParams.isEmpty() ? "?" + query : "&" + query));

                    if (LOG.isDebugEnabled())
                        LOG.debug("final ad string: {}{}", uri, query);
                }
                return transformedAdURIs;
            } else
                return proxyAdURIs;
        }
		return Collections.emptyList();
	}
	
	private String composeLogString(final HttpRequest req, final String newUri, final String remoteHost) {
		final String date = LocalDateTime.now().format(datePattern);
		final String Uri = req.uri();
		String cookieString = "";
		final String cString = req.headers().get(COOKIE);
		try {
			if (cString != null)
				cookieString = URLEncoder.encode(cString, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e1) {
			LOG.error("can`t encode cookie: {}", cString);
            cookieString = "<e>";
		}
		return String.format("%s\t%s\t%s\t%s\t%s", date, Uri, newUri, cookieString, remoteHost);
	}

	private void writeResp(final ChannelHandlerContext ctx, final HttpRequest msg,
			final String buff, List<Cookie> cookieList, final Cookie stCookie) {
		writeResp(ctx, msg, buff, cookieList, stCookie, Configuration.XML_CONTENT_TYPE);
	}

	private void writeResp(final ChannelHandlerContext ctx, final HttpRequest msg,
			final String buff, List<Cookie> cookieList, final Cookie stCookie, final String contentType) {
		ByteBuf bb = Unpooled.wrappedBuffer(buff.getBytes());
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, bb);
		if (stCookie == null) {					
			cookieList.add(cookieFabric.createCookie());
		}
				
		LOG.debug(cookieList.toString());
		for (Cookie c: cookieList) {
			c.setDomain(".1tv.ru");
			response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(c));
            if (LOG.isDebugEnabled())
			    LOG.debug("writeResp set cookie: {}", ServerCookieEncoder.STRICT.encode(c));
		}

		response.headers()
                .add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
		        .add(HttpHeaderNames.CONTENT_TYPE, contentType)
		        .add(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_CACHE)
		        .add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, Configuration.ALLOW_ACC_CONTROL)
                .add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

		boolean keepAlive = HttpUtil.isKeepAlive(msg);

        if (!keepAlive) {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.write(response);
        }
	}
	
    @Override    
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest)msg;

            Map<String, List<String>> queryParams;

            String plID = "";

            String req = request.uri().toLowerCase();

            {
                if (HttpUtil.is100ContinueExpected(request)) {
                    ctx.write(new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.CONTINUE));
                }

                if ("/favicon.ico".equals(request.uri())) {
                    ctx.channel().close();
                    return;
                }

                QueryStringDecoder decoder = new QueryStringDecoder(req);

                queryParams = decoder.parameters();

                Map<String, String> debugParams = getDebugParams(queryParams);

                if (!debugParams.isEmpty()) {
                    writeDebug(debugParams, ctx, msg);
                    ctx.channel().close();
                    return;
                }

                if (!queryParams.isEmpty()) {
                    List<String> idList = queryParams.get(Configuration.ID);

                    if (idList != null && !idList.isEmpty()) {
                        plID = idList.get(0);

                        if (Strings.isNullOrEmpty(plID)) {
                            ctx.channel().close();
                            return;
                        }
                    }
                }
            }

            String account = HelperUtils.getAccount(req);

			final List<String> adUrls = reqTransformer(plID, account, queryParams);

            if (LOG.isDebugEnabled())
			    LOG.debug("VASTUrlList size: {}", adUrls.size());
			
			final InetSocketAddress sa = (InetSocketAddress)ctx.channel().remoteAddress(); 
		    final String remoteHost = sa.getAddress().getHostAddress();
		    LOG.info("remote host: {}:{}", remoteHost, sa.getPort());
			
			if (!adUrls.isEmpty()) {
				memLogStorage.put(composeLogString(request, "", remoteHost));
			} else {
				ctx.channel().close();
				return;
			}

            List<String> cookieStrings = request.headers().getAll(COOKIE);
            Map.Entry<Cookie, List<Cookie>> ourAndSessionCookPair = CookieFabric.getUserSessionCookies(cookieStrings);
			final Cookie stCookie = ourAndSessionCookPair.getKey();
			final List<Cookie> sessionCookieList = ourAndSessionCookPair.getValue();
			
            List<CompletableFuture<Response>> requestsPool = new ArrayList<>(adUrls.size());
            List<String> adContents = new ArrayList<>(adUrls.size());

            for (String vs : adUrls) {
                if (LOG.isDebugEnabled())
                    LOG.debug("try to create request: {}", vs);
                requestsPool.add(createResponse(sessionCookieList, vs).toCompletableFuture());
            }

            if (LOG.isDebugEnabled())
                LOG.debug("response pool size: {}", requestsPool.size());

            boolean cookieAccepted = false;

            CompletableFuture<Response>[] cfs = requestsPool.toArray(new CompletableFuture[requestsPool.size()]);

            CompletableFuture<List<Response>> completed = CompletableFuture.allOf(cfs).exceptionally(e -> errorHandle(e))
                    .thenApply(x -> Arrays.stream(cfs).map(CompletableFuture::join).collect(Collectors.toList()));

            for (Response response : completed.get(cfg.getConnectTimeout(), TimeUnit.MILLISECONDS)) {
                if (LOG.isDebugEnabled())
                    LOG.debug("response statusCode: {}", response.getStatusCode());

                final String respBody = response.getResponseBody();

                adContents.add(respBody.isEmpty() ? Configuration.EMPTY_VAST : respBody);
                if (!cookieAccepted) { // нет нужды сетить все куки, если крутилка одна и та же.., сетим первые и все.
                    sessionCookieList.addAll(CookieFabric.getResponseCookies(response.getHeaders(SET_COOKIE)));
                    cookieAccepted = true;
                }
                LOG.debug("response pool remove");
            }

            writeResp(ctx, (HttpRequest)msg, adUrls.size() > 1 ?
                Vast3Fabric.Vast2ListToVast3(adContents) : adContents.isEmpty() ? Configuration.EMPTY_VAST : adContents.get(0),
                        sessionCookieList, stCookie);
        }
	}

    private void writeDebug(Map<String, String> debugParams, ChannelHandlerContext ctx, Object msg) {
        String ch = debugParams.get(Configuration.CHANNEL);
        PairEx<Short, List<Short>> adList = adProcessing.getAdListFromTimeTable(ch);
        writeResp(ctx, (HttpRequest)msg, adList == null ? Configuration.EMPTY_VAST_NO_AD : adList.toString(), new ArrayList<>(), null, "text/plain");
    }

    private static Void errorHandle(Throwable e){
        if (e != null) {
            LOG.error("error occured: " + e.getCause());
        }
        return null;
    }
	
	private ListenableFuture<Response> createResponse(final List<Cookie> sessionCookies, final String newPath){
		final BoundRequestBuilder rb = httpClient.prepareGet(newPath);
		for (Cookie c : sessionCookies) {
			rb.addHeader(COOKIE, ClientCookieEncoder.STRICT.encode(c).replace("\"", "")); // read rfc ! adfox don`t like \" symbols
		}
		return rb.execute(new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) {
                LOG.info("req completed : {} status code : {}, response size: {}", newPath, response.getStatusCode(), response.getResponseBody().length());
                return response;
            }
		});
	}
	
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }	
					
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOG.warn("netty error: ", cause);
        cause.printStackTrace();
		ctx.close();
	}

    private Map<String, String> getDebugParams(final Map<String, List<String>> params) {
        Map<String, String> mOut = new HashMap<>();

        if (params.keySet().contains("debug")) {
            if (params.get(Configuration.CHANNEL) != null) {
                return Collections.singletonMap(Configuration.CHANNEL, params.get(Configuration.CHANNEL).get(0));
            }
        }
        return mOut;
    }
}
