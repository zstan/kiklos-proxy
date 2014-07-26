package kiklos.proxy.core;

import java.util.Date;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import kiklos.planner.AbstractStrategy;
import kiklos.planner.DurationSettings;
import kiklos.planner.SimpleStrategy;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

public class HttpRequestHandler extends ChannelInboundHandlerAdapter {
	private final AsyncHttpClient asyncClient;
	private final PlacementsMapping plMap;
	private final DurationSettings durationSettings;
	private static final String EMPTY_VAST = "<VAST version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"oxml.xsd\" />";
	private final CookieFabric cookieFabric;
	private static final String FILE_ENCODING = UTF_8.name();
	private static final String XML_CONTENT_TYPE = "application/xml; charset=" + FILE_ENCODING;
	private static final String DURATION = "t";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestHandler.class);
    private final MemoryLogStorage memLogStorage;
	
	HttpRequestHandler(AsyncHttpClient c, final PlacementsMapping placements, final MemoryLogStorage logStorage, 
			final CookieFabric cf, final DurationSettings ds) {
		asyncClient = c;
		plMap = placements;
		memLogStorage = logStorage;
		cookieFabric = cf;
		durationSettings = ds;
	}
	
	private int getRequiredAdDuration(final String req) {
		Map<String, List<String>> params = (new QueryStringDecoder(req)).parameters();
		List<String> dur = params.get(DURATION); 
		if (dur != null) {
			try {
				return Integer.parseInt(dur.get(0));
			} catch (NumberFormatException e) {
				return -1;
			}			
		} else
			return -1;
	}
	
	private Pair<List<String>, String> reqTransformer(final String req) {
		String trans = ""; 
		String main = "";
		List<String> vastList = Collections.emptyList();
		QueryStringDecoder decoder = new QueryStringDecoder(req);
		
		Map<String, List<String>> params = decoder.parameters(); 
		if (!params.isEmpty())
			if (!params.get("id").isEmpty() && !params.get("id").get(0).isEmpty()) {
				final String id = params.get("id").get(0);
				vastList =  plMap.getMappingVASTList(id);
				if (!vastList.isEmpty()) {
					params.remove("id");
					
					// stub !!!
					
					main = vastList.get(0);
					// no need params now !!!
					/*
					if (new QueryStringDecoder(main).getParameters().isEmpty()) // TODO: fix it
						trans = "?";
					else
						trans = "&";
					
					for (Map.Entry<String, List<String>> e: params.entrySet()) {
						trans += e.getKey();
						for (String val: e.getValue())
							trans += "=" + val;
						trans += "&"; 
					}
					*/
				}
		}
		LOG.debug("main: {}, params: {}", main, trans);
		return new Pair<List<String>, String>(vastList, trans);
	}
	
	private String composeLogString(final HttpRequest req, final String newUri, final String remoteHost) {
		final String date = DATE_FORMAT.format(new Date());
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
		ByteBuf bb = Unpooled.wrappedBuffer(buff.getBytes());
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, bb);
		if (stCookie == null) {					
			cookieList.add(getOurCookie());			
		}
				
		for (Cookie c: cookieList) {
			c.setDomain(".beintv.ru");
			response.headers().add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(c));
			LOG.debug("writeResp set cookie: {}", ServerCookieEncoder.encode(c));
		}
		
		response.headers().add(HttpHeaders.Names.CONTENT_LENGTH, buff.getBytes().length);
		response.headers().add(HttpHeaders.Names.CONTENT_TYPE, XML_CONTENT_TYPE);
		response.headers().add(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_CACHE);
		
		boolean keepAlive = HttpHeaders.isKeepAlive(msg);
        if (!keepAlive) {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            ctx.write(response);
        }						
	}
	
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }	
				
    @Override    
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
			if (HttpHeaders.is100ContinueExpected(request)) {
				ctx.write(new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.CONTINUE));
			}		
			final String reqUri = request.getUri();		
			if ("/favicon.ico".equals(reqUri)) {
				ctx.channel().close();
				return;			
			}		
			
			/*if (this.getRequiredAdDuration(reqUri) == -1) {
				return empty vast
			}*/
			
			final Pair<List<String>, String> newUriPair = reqTransformer(reqUri);
			List<String> VASTList = newUriPair.getFirst();
			if (VASTList.isEmpty()) {
				int reqDuration = this.getRequiredAdDuration(reqUri);
				LOG.debug("no correspond placement found, try to get from TimeTable req duration: {}", reqDuration);
				VASTList = SimpleStrategy.formAdList(durationSettings, reqDuration);
			}
			final String newPath = VASTList.isEmpty() ? "" : VASTList.get(0);
			final String newParams = newUriPair.getSecond();
	
			final InetSocketAddress sa = (InetSocketAddress)ctx.channel().remoteAddress(); 
		    final String remoteHost = sa.getAddress().getHostAddress();
		    LOG.info(String.format("host: %s port: %d", remoteHost, sa.getPort()));						
			
			if (!newPath.isEmpty()) {
				memLogStorage.put(composeLogString(request, newPath + newParams, remoteHost));
			} else {
				ctx.channel().close();
				return;
			}
			
			Pair<Cookie, List<Cookie>> ourAndSessionCookPair = CookieFabric.getUserSessionCookies(request);
			final Cookie stCookie = ourAndSessionCookPair.getFirst();
			final List<Cookie> sessionCookieList = ourAndSessionCookPair.getSecond();
			
			if (VASTList.size() > 1) {
				List<ListenableFuture<Response>> pool = new ArrayList<>();
				List<String> vastPool = new ArrayList<>();
				
				for (String vs : VASTList) {
					URI decoder = new URI(vs);								
					String path = String.format("%s://%s", decoder.getScheme(), decoder.getHost());
					String query = String.format("%s?%s", decoder.getPath(), decoder.getQuery() == null ? "" : decoder.getQuery());
					LOG.debug("path: {}, query {}", path, query);
					pool.add(createResponse(sessionCookieList, path, query));
				}
				LOG.debug("response pool size: {}", pool.size());
				while (!pool.isEmpty()) {				
					ListenableFuture<Response> p = pool.get(0);
					if (p.isDone() || p.isCancelled()) {
						LOG.debug("isDone {}, isCancelled {}", p.isDone(), p.isCancelled());
						final String respVast = p.get().getResponseBody();
						vastPool.add(respVast.isEmpty() ? EMPTY_VAST : respVast);
						pool.remove(p);
					}
					else {
						TimeUnit.MILLISECONDS.sleep(1);
					}
				}
				final String compoundVast = Vast3Fabric.Vast2ListToVast3(vastPool);
				writeResp(ctx, (HttpRequest)msg, compoundVast, sessionCookieList, stCookie);
				return;
			} else { /* Отдельный if только потому что тут сетим куки от ответа, а в предыдущей нет, переписать когда будет понятно с куками*/
				ListenableFuture<Response> respFut = createResponse(sessionCookieList, newPath, newParams);
				while (true) {
					if (respFut.isDone() || respFut.isCancelled()) {
						Response response = respFut.get();
						final String body = response.getResponseBody();
						List<Cookie> cookieList = CookieFabric.getResponseCookies(response);
						writeResp(ctx, (HttpRequest)msg, body.isEmpty() ? EMPTY_VAST : body, cookieList, stCookie);
						return;
					}
				}
			}	
        }
	}
	
	private ListenableFuture<Response> createResponse(final List<Cookie> sessionCookieList, 
			final String newPath, final String newParams) throws IOException {
		final BoundRequestBuilder rb = asyncClient.prepareGet(String.format("%s%s", newPath, newParams));
		for (Cookie c : sessionCookieList) {
			rb.addHeader(COOKIE, ClientCookieEncoder.encode(c));
		}
		ListenableFuture<Response> f = rb.execute(new AsyncCompletionHandler<Response>() {			
			
			@Override
			public Response onCompleted(Response response) throws Exception {
				LOG.info("req transformed : {}, new params: {}", newPath, newParams);
				LOG.info("status code : {}, response size: {}", response.getStatusCode(), response.getResponseBody().length());
				return response;
			}
		});
		return f;
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOG.warn(cause.getMessage());
		ctx.close();
	}
	
	private Cookie getOurCookie() {	
		Cookie c = new DefaultCookie(CookieFabric.OUR_COOKIE_NAME, cookieFabric.generateUserId(System.currentTimeMillis()));
		c.setMaxAge(60*60*24*30*3);
		c.setPath("/");
		c.setDomain(".beintv.ru");
		c.setHttpOnly(true);
		return c;
	}	
}
