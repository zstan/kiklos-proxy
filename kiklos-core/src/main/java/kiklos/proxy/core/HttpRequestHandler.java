package kiklos.proxy.core;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
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

import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import kiklos.planner.DurationSettings;
import kiklos.planner.SimpleStrategy;
import kiklos.tv.timetable.AdProcessing;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
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
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.apache.commons.lang3.tuple.Pair;
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
	private static final String EMPTY_VAST = "<VAST version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"oxml.xsd\" />";
	private static final String EMPTY_VAST_NO_AD = "<VAST version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"oxml.xsd\" <no_ad_for_this_time/>/>";
	private final CookieFabric cookieFabric;
	private static final String XML_CONTENT_TYPE = "application/xml; charset=" + StandardCharsets.UTF_8.name();
	static final String DURATION = "t";
	static final String CHANNEL = "ch";
    static final String ID = "id";
	static final String DEFAULT_CHANNEL = "408";
	private static final int COOKIE_MAX_AGE = 60*60*24*30*3;
	static short MAX_DURATION_BLOCK = 900;
	private static final DateTimeFormatter datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestHandler.class);
    private final MemoryLogStorage memLogStorage;
    private final AdProcessing adProcessing;
	
	HttpRequestHandler(Configuration config) {
		httpClient = config.getHttpClient();
		placementsMapping = config.getPlacementsMap();
		memLogStorage = config.getMemLogStorage();
		cookieFabric = config.getCookieFabric();
		durationSettings = config.getDurationsConfig();
		adProcessing = config.getAdProcessing();
	}

	/**
	 * @param req request string
	 * @return list for Ad uri`s
     */
	private List<String> reqTransformer(String req) {
        req = req.toLowerCase();
		QueryStringDecoder decoder = new QueryStringDecoder(req);
		Map<String, List<String>> queryParams = decoder.parameters();
		
		if (!queryParams.isEmpty()) {
            String plID = "";
            List<String> idList = queryParams.get(ID);
            if (!idList.isEmpty() && !idList.get(0).isEmpty()) {
                plID = idList.get(0);
            }

			if (!plID.isEmpty()) {
				// пробуем найти уже замапленные vast url`s
				// редактируется в редис базе
				// redis-cli HSET "\".placements\"" "\"212\"" "[\"java.util.ArrayList\",[\"http://asdasdfasdf.com/\", \"http://asg.vidigital.ru/1/50006/c/v/2\", \"http://ib.adnxs.com/ptv?id=2504637\"]]"
				// берем vast xml или из базы редиса или из расписания, если попадаем в рекламный блок бьем его на части и каждой части смотрим в редис базу
				// за соотв. записями : redis-cli HSET "\".durations\"" "5" "\"http://ads.adfox.ru/216891/getCode?p1=bpvvo&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\""
				// так же для каждого канала в отдельности конфигурится допуски +- от рекламного блока.
				List<String> vastList =  placementsMapping.getMappingVASTList(plID);

				// Если не нашли в редис, лезем в расписание
				if (vastList.isEmpty()) {
					int reqDuration = HelperUtils.getRequiredAdDuration(queryParams);
					LOG.debug("no correspond placement found, try to get from TimeTable req duration: {}", reqDuration);
					PairEx<Short, List<Short>> tt4ch = adProcessing.getAdListFromTimeTable(HelperUtils.getChannelFromParams(queryParams));
					if (tt4ch != null) {
						reqDuration = tt4ch.getKey();
						LOG.debug("reqDuration: {}", reqDuration);
						vastList = SimpleStrategy.formAdList(durationSettings, reqDuration);
					} else {
						vastList = Collections.emptyList(); // empty for unknown channel !!!
					}
				}

				if (LOG.isDebugEnabled())
					LOG.debug("vastList size: {}", vastList.size());

				if (!vastList.isEmpty()) {

                    // к нашей системе идем с запросом
                    // http://178.170.237.19/22627[99]/tvd?id=111&puid30=12377&puid6=15&pr=123&eid1=12377:1:123&dl=http://1tv.ru/ott/:www.ru
                    // выкидываем id все остальные параметры конкатенируем и передаем в AD
                    //http://v.adfox.ru/{account}/getCode?pp=efi&ps=byof&p2=eyit&pfc=a&pfb=a&plp=a&pli=a&pop=a&pct=c&puid5=1&puid6={priority}
                    // &puid25=1&puid30={placement}&pr={random}&eid1={placement}:{session}:{random}&dl=http://1tv.ru/ott/:{referer}
                    // в редис сетим соотв.
                    // redis-cli HSET "\".placements\"" "\"111\"" "[\"java.util.ArrayList\",[\"http://v.adfox.ru/{account}/getCode?pp=efi&ps=byof&p2=eyit&pfc=a&pfb=a&plp=a&pli=a&pop=a&pct=c&puid5=1&puid25=1\"]]"

                    String account = HelperUtils.getAccount(req);

					queryParams.remove(ID);
					queryParams.remove(DURATION);
					queryParams.remove(CHANNEL);
					
					String query = HelperUtils.queryParams2String(queryParams);

					if (!Strings.isNullOrEmpty(query)) {
                        List<String> vastUriList = new ArrayList<>(vastList.size());

						for (String vastURL: vastList) {
                            if (!account.isEmpty())
                                vastURL = vastURL.replace("{account}", account);

							if (queryParams.isEmpty())
                                vastURL += "?";
							else
                                vastURL += "&";
							vastUriList.add(vastURL + query);

                            if (LOG.isDebugEnabled())
                                LOG.debug("final ad string: {}{}", vastURL, query);
						}
                        return vastUriList;
					} else
						return vastList;
				}
			}	
		}
		return Collections.emptyList();
	}
	
	private Map<String, String> getDebugParams(final String req) {
		final QueryStringDecoder decoder = new QueryStringDecoder(req);
		Map<String, String> mOut = new HashMap<>();
		final Map<String, List<String>> params = decoder.parameters();
		if (params.keySet().contains("debug")) {
			if (params.get(CHANNEL) != null) {
				mOut.put(CHANNEL, params.get(CHANNEL).get(0));
			}
		}
		return mOut;
	}
	
	private String composeLogString(final HttpRequest req, final String newUri, final String remoteHost) {
		final String date = LocalDateTime.now().format(datePattern);
		final String Uri = req.getUri();
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
		writeResp(ctx, msg, buff, cookieList, stCookie, XML_CONTENT_TYPE);
	}

	private void writeResp(final ChannelHandlerContext ctx, final HttpRequest msg,
			final String buff, List<Cookie> cookieList, final Cookie stCookie, final String contentType) {
		ByteBuf bb = Unpooled.wrappedBuffer(buff.getBytes());
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, bb);
		if (stCookie == null) {					
			cookieList.add(currentCookie());
		}
				
		LOG.debug(cookieList.toString());
		for (Cookie c: cookieList) {
			c.setDomain(".1tv.ru");
			response.headers().add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.STRICT.encode(c));
            if (LOG.isDebugEnabled())
			    LOG.debug("writeResp set cookie: {}", ServerCookieEncoder.STRICT.encode(c));
		}
		
		response.headers()
                .add(HttpHeaders.Names.CONTENT_LENGTH, buff.getBytes().length)
		        .add(HttpHeaders.Names.CONTENT_TYPE, contentType)
		        .add(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_CACHE)
		        .add(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .add(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		
		boolean keepAlive = HttpHeaders.isKeepAlive(msg);

        if (!keepAlive) {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            ctx.write(response);
        }						
	}
	
    @Override    
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest)msg;

			if (HttpHeaders.is100ContinueExpected(request)) {
				ctx.write(new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.CONTINUE));
			}

			final String reqUri = request.uri();

			if ("/favicon.ico".equals(reqUri)) {
				ctx.channel().close();
				return;			
			}
			
			Map<String, String> debugParams = getDebugParams(reqUri); 
			
			if (!debugParams.isEmpty()) { 
				String ch = debugParams.get(CHANNEL);
				PairEx<Short, List<Short>> adList = adProcessing.getAdListFromTimeTable(ch);
				writeResp(ctx, (HttpRequest)msg, adList == null ? EMPTY_VAST_NO_AD : adList.toString(), new ArrayList<Cookie>(), null, "text/plain");
				return;
			}			
			
			/*if (this.getRequiredAdDuration(reqUri) == -1) {
				return empty vast
			}*/
			
			final List<String> vastUrls = reqTransformer(reqUri);
            if (LOG.isDebugEnabled())
			    LOG.debug("VASTUrlList size: {}", vastUrls.size());
			
			final InetSocketAddress sa = (InetSocketAddress)ctx.channel().remoteAddress(); 
		    final String remoteHost = sa.getAddress().getHostAddress();
		    LOG.info("remote host: {}:{}", remoteHost, sa.getPort());
			
			if (!vastUrls.isEmpty()) {
				memLogStorage.put(composeLogString(request, "", remoteHost));
			} else {
				ctx.channel().close();
				return;
			}
			
			Pair<Cookie, List<Cookie>> ourAndSessionCookPair = CookieFabric.getUserSessionCookies(request);
			final Cookie stCookie = ourAndSessionCookPair.getKey();
			final List<Cookie> sessionCookieList = ourAndSessionCookPair.getValue();
			
            List<CompletableFuture<Response>> requestsPool = new ArrayList<>();
            List<String> adContents = new ArrayList<>();

            for (String vs : vastUrls) {
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

            for (Response response : completed.get(5000, TimeUnit.MILLISECONDS)) {
                if (LOG.isDebugEnabled())
                    LOG.debug("response statusCode: {}", response.getStatusCode());

                final String respBody = response.getResponseBody();

                adContents.add(respBody.isEmpty() ? EMPTY_VAST : respBody);
                if (!cookieAccepted) { // нет нужды сетить все куки, если крутилка одна и та же.., сетим первые и все.
                    sessionCookieList.addAll(CookieFabric.getResponseCookies(response));
                    cookieAccepted = true;
                }
                LOG.debug("response pool remove");
            }

            writeResp(ctx, (HttpRequest)msg, vastUrls.size() > 1 ?
                                Vast3Fabric.Vast2ListToVast3(adContents) :
                                adContents.isEmpty() ? EMPTY_VAST : adContents.get(0),
                        sessionCookieList, stCookie);
        }
	}

    private static Void errorHandle(Throwable e){
        if (e != null) {
            LOG.error("error occured: " + e.getCause());
        }
        return null;
    }
	
	private ListenableFuture<Response> createResponse(final List<Cookie> sessionCookies,
			final String newPath) throws IOException {
		final BoundRequestBuilder rb = httpClient.prepareGet(newPath);
		for (Cookie c : sessionCookies) {
			rb.addHeader(COOKIE, ClientCookieEncoder.STRICT.encode(c).replace("\"", "")); // read rfc ! adfox don`t like \" symbols
		}
		return rb.execute(new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
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
		LOG.warn(cause.getMessage());
		ctx.close();
	}
	
	private Cookie currentCookie() {
		Cookie c = new DefaultCookie(CookieFabric.OUR_COOKIE_NAME, cookieFabric.generateUserId());
		c.setMaxAge(COOKIE_MAX_AGE);
		c.setPath("/");
		c.setDomain(".adinsertion.pro");
		c.setHttpOnly(true);
		return c;
	}
}
