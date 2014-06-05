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

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.util.CharsetUtil;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static org.jboss.netty.util.CharsetUtil.UTF_8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

public class HttpRequestHandler extends SimpleChannelUpstreamHandler {
	private final AsyncHttpClient asyncClient;
	private final PlacementsMapping plMap;
	private static final String EMPTY_VAST = "<VAST version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"oxml.xsd\" />";
	private final CookieFabric cookieFabric;
	private static final String FILE_ENCODING = UTF_8.name();
	private static final String TEXT_CONTENT_TYPE = "application/xml; charset=" + FILE_ENCODING;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestHandler.class);
    private final MemoryLogStorage memLogStorage;     
	
	HttpRequestHandler(AsyncHttpClient c, final PlacementsMapping placements, final MemoryLogStorage logStorage, 
			final CookieFabric cf) {
		asyncClient = c;
		plMap = placements;
		memLogStorage = logStorage;
		cookieFabric = cf;
	}
	
	private Pair<List<String>, String> reqTransformer(final String req) {
		String trans = ""; 
		String main = "";
		List<String> vastList = Collections.emptyList();
		QueryStringDecoder decoder = new QueryStringDecoder(req);
		
		Map<String, List<String>> params = decoder.getParameters(); 
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
	
	private void writeResp(MessageEvent e, final String buff, final List<CookieEncoder> cookieList, final Cookie stCookie) {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		CookieEncoder ce = new CookieEncoder(true);
		if (stCookie == null) {					
			ce.addCookie(getOurCookie());
		}
		cookieList.add(ce);
		
		response.setContent(ChannelBuffers.copiedBuffer(buff, CharsetUtil.UTF_8));
		response.headers().add(HttpHeaders.Names.CONTENT_TYPE, TEXT_CONTENT_TYPE);
		for (CookieEncoder ce1: cookieList)
			response.headers().add(HttpHeaders.Names.SET_COOKIE, ce1.encode());
		response.headers().add(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_CACHE);
		
		ChannelFuture future = e.getChannel().write(response);
		HttpHeaders.setContentLength((HttpMessage)e.getMessage(), response.getContent().readableBytes());
		future.addListener(ChannelFutureListener.CLOSE);
	}			
				
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent mEvent) throws Exception {
		
		final HttpRequest request = (HttpRequest)mEvent.getMessage();				
		if (HttpHeaders.is100ContinueExpected(request)) {
			send100Continue(mEvent);
		}		
		final String reqUri = request.getUri();		
		if ("/favicon.ico".equals(reqUri)) {
			mEvent.getChannel().close();
			return;			
		}

		//Response resp = new ResponseBuilder
		
		final Pair<List<String>, String> newUriPair = reqTransformer(reqUri);
		final List<String> VASTList = newUriPair.getFirst();
		final String newPath = VASTList.isEmpty() ? "" : VASTList.get(0);
		final String newParams = newUriPair.getSecond();

		final InetSocketAddress sa = (InetSocketAddress)ctx.getChannel().getRemoteAddress(); 
	    final String remoteHost = sa.getAddress().getHostAddress();
	    LOG.info(String.format("host: %s port: %d", remoteHost, sa.getPort()));						
		
		if (!newPath.isEmpty()) {
			memLogStorage.put(composeLogString(request, newPath + newParams, remoteHost));
		} else {
			mEvent.getChannel().close();
			return;
		}
		
		Pair<Cookie, CookieEncoder> ourAndSessionCookPair = CookieFabric.getSessionCookies(request);
		final Cookie stCookie = ourAndSessionCookPair.getFirst();
		final CookieEncoder sessionCookieEncoder = ourAndSessionCookPair.getSecond();
		
		if (VASTList.size() > 1) {
			List<CookieEncoder> cookieList = new ArrayList<>();
			if (sessionCookieEncoder != null)
				cookieList.add(sessionCookieEncoder);
			List<ListenableFuture<Response>> pool = new ArrayList<>();
			List<String> vastPool = new ArrayList<>();
			
			for (String vs : VASTList) {
				URI decoder = new URI(vs);								
				String path = String.format("%s://%s", decoder.getScheme(), decoder.getHost());
				String query = String.format("%s?%s", decoder.getPath(), decoder.getQuery() == null ? "" : decoder.getQuery());
				LOG.debug("path: {}, query {}", path, query);
				pool.add(createResponse(sessionCookieEncoder, path, query));
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
			writeResp(mEvent, compoundVast, cookieList, stCookie);
			return;
		} else {
			ListenableFuture<Response> respFut = createResponse(sessionCookieEncoder, newPath, newParams);
			while (true) {
				if (respFut.isDone() || respFut.isCancelled()) {
					Response response = respFut.get();
					final String body = response.getResponseBody();
					List<CookieEncoder> cookieList = CookieFabric.getResponseCookies(response);
					writeResp(mEvent, body.isEmpty() ? EMPTY_VAST : body, cookieList, stCookie);
					return;
				}
			}
		}		
	}
	
	private ListenableFuture<Response> createResponse(final CookieEncoder sessionCookieEncoder, 
			final String newPath, final String newParams) throws IOException {
		final BoundRequestBuilder rb = asyncClient.prepareGet(String.format("%s%s", newPath, newParams));
		rb.addHeader(COOKIE, sessionCookieEncoder.encode());
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
	
	private void send100Continue(MessageEvent e) {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
		e.getChannel().write(response);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		e.getCause().printStackTrace();
		e.getChannel().close();
	}
	
	private Cookie getOurCookie() {		
		Cookie c = new DefaultCookie(CookieFabric.OUR_COOKIE_NAME, cookieFabric.generateUserId(System.currentTimeMillis()));
		c.setMaxAge(60*60*24*30*3);
		c.setPath("/");
		c.setDomain(".beintv.ru");
		return c;
	}	
}
