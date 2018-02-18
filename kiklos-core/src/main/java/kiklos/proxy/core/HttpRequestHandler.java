package kiklos.proxy.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import kiklos.planner.DurationSettings;
import kiklos.planner.SimpleStrategy;
import kiklos.tv.timetable.AdProcessing;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

public class HttpRequestHandler extends ChannelInboundHandlerAdapter {
	private final AsyncHttpClient httpClient;
	private final PlacementsMapping placementsMapping;
	private final DurationSettings durationSettings;
	private static final String EMPTY_VAST = "<VAST version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"oxml.xsd\" />";
	private static final String EMPTY_VAST_NO_AD = "<VAST version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"oxml.xsd\" <no_ad_for_this_time/>/>";
	private final CookieFabric cookieFabric;
	private static final String XML_CONTENT_TYPE = "application/xml; charset=" + UTF_8.name();
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
	private List<String> reqTransformer(final String req) {
		QueryStringDecoder decoder = new QueryStringDecoder(req);		
		Map<String, List<String>> params = decoder.parameters(); 
		
		if (!params.isEmpty()) {
			final List<String> idList = params.get(ID);
			if (!idList.isEmpty() && !idList.get(0).isEmpty()) {
				final String id = idList.get(0);

				// пробуем найти уже замапленные vast url`s
				// редактируется в редис базе
				// redis-cli HSET "\".placements\"" "\"212\"" "[\"java.util.ArrayList\",[\"http://asdasdfasdf.com/\", \"http://asg.vidigital.ru/1/50006/c/v/2\", \"http://ib.adnxs.com/ptv?id=2504637\"]]"
				// берем vast xml или из базы редиса или из расписания, если попадаем в рекламный блок бьем его на части и каждой части смотрим в редис базу
				// за соотв. записями : redis-cli HSET "\".durations\"" "5" "\"http://ads.adfox.ru/216891/getCode?p1=bpvvo&p2=euhw&pfc=a&pfb=a&plp=a&pli=a&pop=a\""
				// так же для каждого канала в отдельности конфигурится допуски +- от рекламного блока.
				List<String> vastList =  placementsMapping.getMappingVASTList(id);

				// Если не нашли в редис, лезем в расписание
				if (vastList.isEmpty()) {
					int reqDuration = HelperUtils.getRequiredAdDuration(params);
					LOG.debug("no correspond placement found, try to get from TimeTable req duration: {}", reqDuration);
					PairEx<Short, List<Short>> tt4ch = adProcessing.getAdListFromTimeTable(HelperUtils.getChannelFromParams(params));
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
					List<String> vastUriList = new ArrayList<>(vastList.size());
					params.remove(ID);
					params.remove(DURATION);
					params.remove(CHANNEL);
					
					String query = HelperUtils.queryParams2String(params);
					
					if (!Strings.isNullOrEmpty(query)) {
						for (String v: vastList) {
							if (new QueryStringDecoder(v).parameters().isEmpty())
								v += "?";
							else	
								v += "&";
							vastUriList.add(v + query);
						}				
					} else {
						vastUriList = vastList;	
					}	
					return vastUriList;
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
		String cookieString = "<e>";
		final String cString = req.headers().get(COOKIE);
		try {
			if (cString != null)
				cookieString = URLEncoder.encode(cString, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			LOG.error("can`t encode cookie: {}", cString);
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
			c.setDomain(".adinsertion.pro");
			response.headers().add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(c));
			LOG.debug("writeResp set cookie: {}", ServerCookieEncoder.encode(c));
		}
		
		response.headers().add(HttpHeaders.Names.CONTENT_LENGTH, buff.getBytes().length);
		response.headers().add(HttpHeaders.Names.CONTENT_TYPE, contentType);
		response.headers().add(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_CACHE);
		response.headers().add("Access-Control-Allow-Origin", "*");
		
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

			final String reqUri = request.getUri();

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
			
			if (vastUrls.size() > 1) {
				List<ListenableFuture<Response>> requestsPool = new ArrayList<>();
				List<String> vastPool = new ArrayList<>();
				
				for (String vs : vastUrls) {
					if (LOG.isDebugEnabled())
						LOG.debug("try to create request: {}", vs);
					requestsPool.add(createResponse(sessionCookieList, vs));
				}

				if (LOG.isDebugEnabled())
					LOG.debug("response pool size: {}", requestsPool.size());
				
				boolean cookieAccepted = false;

				while (!requestsPool.isEmpty()) {
					ListenableFuture<Response> p = requestsPool.get(0);
					if (p.isDone() || p.isCancelled()) {
                        if (LOG.isDebugEnabled())
						    LOG.debug("isDone {}, isCancelled {}", p.isDone(), p.isCancelled());
						if (p.isDone()) {
							final Response response = p.get();
							final String respVast = response.getResponseBody();
							vastPool.add(respVast.isEmpty() ? EMPTY_VAST : respVast);
							if (!cookieAccepted) { // нет нужды сетить все куки, если крутилка одна и та же.., сетим первые и все.
								sessionCookieList.addAll(CookieFabric.getResponseCookies(response));
								cookieAccepted = true;
							}
						}
						requestsPool.remove(p);
						LOG.debug("response pool remove");
					}
					else
						HelperUtils.try2sleep(TimeUnit.MILLISECONDS, 10);
				}
				
				final String compoundVast = Vast3Fabric.Vast2ListToVast3(vastPool);
				writeResp(ctx, (HttpRequest)msg, compoundVast, sessionCookieList, stCookie);				
			} else { /* Отдельный if только потому что тут сетим куки от ответа, а в предыдущей нет, переписать когда будет понятно с куками*/
				final ListenableFuture<Response> respFut = createResponse(sessionCookieList, vastUrls.get(0));
                final Response response = respFut.get();
                final String body = response.getResponseBody();
                final List<Cookie> cookieList = CookieFabric.getResponseCookies(response);
                writeResp(ctx, (HttpRequest)msg, body.isEmpty() ? EMPTY_VAST : body, cookieList, stCookie);
			}
        }
	}
	
	private ListenableFuture<Response> createResponse(final List<Cookie> sessionCookieList, 
			final String newPath) throws IOException {
		final BoundRequestBuilder rb = httpClient.prepareGet(newPath);
		for (Cookie c : sessionCookieList) {
			rb.addHeader(COOKIE, ClientCookieEncoder.encode(c).replace("\"", "")); // read rfc ! adfox don`t like \" symbols
		}
		ListenableFuture<Response> f = rb.execute(new AsyncCompletionHandler<Response>() {
			@Override
			public Response onCompleted(Response response) throws Exception {
				LOG.info("req completed : {} status code : {}, response size: {}", newPath, response.getStatusCode(), response.getResponseBody().length());
				return response;
			}
		});
		return f;
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
