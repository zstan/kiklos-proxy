package kiklos.proxy.core;

import java.util.Date;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
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
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;
import static org.jboss.netty.util.CharsetUtil.UTF_8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import target.eyes.vag.codec.xml.javolution.VASTv2Parser;
import target.eyes.vag.codec.xml.javolution.vast.v2.impl.VAST;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class HttpRequestHandler extends SimpleChannelUpstreamHandler {
	private HttpRequest request;
	private final AsyncHttpClient asyncClient;
	private final PlacementsMapping plMap;
	private final String AD_DOMAIN = "http://ib.adnxs.com";
	private final String EMPTY_VAST = "<VAST /> ";
	private static final String FILE_ENCODING = UTF_8.name();
	private static final String TEXT_CONTENT_TYPE = "application/xml; charset=" + FILE_ENCODING;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestHandler.class);
    private final MemoryLogStorage memLogStorage;
	
	HttpRequestHandler(AsyncHttpClient c, final PlacementsMapping placements, final MemoryLogStorage logStorage) {
		asyncClient = c;
		plMap = placements;
		memLogStorage = logStorage;
	}
	
	private Pair<String, Boolean> reqTransformer(final String req) {
		String trans = req;
		Boolean transformed = false;
		QueryStringDecoder decoder = new QueryStringDecoder(req);
		
		Map<String, List<String>> params = decoder.getParameters(); 
		if (!params.isEmpty())
			if (!params.get("id").isEmpty()) {
				transformed = true;
				final String id = decoder.getParameters().get("id").get(0);
				String newId = plMap.getMappingPlacement(id);
				params.remove("id");
				params.put("id", Arrays.asList(newId));				
				
				// stub !!!
				
				trans = decoder.getPath() + "?";
				for (Map.Entry<String, List<String>> e: params.entrySet()) {
					trans += e.getKey();
					for (String val: e.getValue())
						trans += "=" + val;
					trans += "&"; 
				}
		}
		return new Pair<String, Boolean>(trans, transformed);
	}
	
	private String composeLogString(final HttpRequest req, final String newUri) {
		final String date = DATE_FORMAT.format(new Date());
		final String Uri = req.getUri();
		String cookieString = "<err>";
		final String cString = req.headers().get(COOKIE);
		try {
			if (cString != null)
				cookieString = URLEncoder.encode(cString, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			LOG.error("can`t encode cookie: {}", cString);
		}
		return String.format("%s\t%s\t%s\t%s", date, Uri, newUri, cookieString);
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		
		request = (HttpRequest)e.getMessage();
				
		if (HttpHeaders.is100ContinueExpected(request)) {
			send100Continue(e);
		}
		
		final String Uri = request.getUri();
		
		if ("/favicon.ico".equals(Uri)) {
			e.getChannel().close();
			return;			
		}
		
		final Pair<String, Boolean> newUriPair = reqTransformer(Uri);
		final String newUri = newUriPair.getFirst();
		
		if (newUriPair.getSecond()) {
			memLogStorage.put(composeLogString(request, newUri));
		}
		
		if (newUri.isEmpty()) {
			e.getChannel().close();
			return;
		}
		
		LOG.info("\n\n---------------------------------------------");	
		
	    String remoteHost = ((InetSocketAddress)ctx.getChannel().getRemoteAddress()).getAddress().getHostAddress();
	    int remotePort = ((InetSocketAddress)ctx.getChannel().getRemoteAddress()).getPort();
	    LOG.info(String.format("host: %s port: %d", remoteHost, remotePort));				
		
		asyncClient.prepareGet(String.format("%s%s", AD_DOMAIN, newUri)).execute(new AsyncCompletionHandler<Response>(){

			StringBuilder buff = new StringBuilder(4096);
			
			private void writeResp(MessageEvent e, final List<CookieEncoder> cookieList) {
				HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				
				response.setContent(ChannelBuffers.copiedBuffer(buff.toString(), CharsetUtil.UTF_8));
				response.headers().add(HttpHeaders.Names.CONTENT_TYPE, TEXT_CONTENT_TYPE);
				for (CookieEncoder ce: cookieList)
					response.headers().add(HttpHeaders.Names.SET_COOKIE, ce.encode());
				response.headers().add(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_CACHE);
				
				ChannelFuture future = e.getChannel().write(response);
				HttpHeaders.setContentLength((HttpMessage)e.getMessage(), response.getContent().readableBytes());
				future.addListener(ChannelFutureListener.CLOSE);
			}			
			
			@Override
			public Response onCompleted(Response response) throws Exception {
				if (!response.getResponseBody().isEmpty())
					buff.append(response.getResponseBody());
				else
					buff.append(EMPTY_VAST);
				
				List<CookieEncoder> cookieList = storeADSessionCookie(response);								
				
				writeResp(e, cookieList);
				//List<String> list = redisson.getList("anyList");			
				//List<String> list = redisson. getList("mylist");
				//VAST v = VASTv2Parser.parse(response.getResponseBody());
				LOG.info("req incoming : {}", Uri);
				LOG.info("req transformed : {}", newUri);
				LOG.info("status code : {}", response.getStatusCode());
				LOG.info("request size: {}", response.getResponseBody().length());
				return response;
			}

		});
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
	
	private List<CookieEncoder> storeADSessionCookie(Response request) {		
		List<String> cookieStrings = request.getHeaders(SET_COOKIE);
		if (cookieStrings == null)
			return Collections.emptyList();
		
		List<CookieEncoder> httpCookieEncoderList = new ArrayList<>();
		for (String cookieString : cookieStrings) {
			if (cookieString != null) {
				CookieDecoder cookieDecoder = new CookieDecoder();
				Set<Cookie> cookies = cookieDecoder.decode(cookieString);
				if (!cookies.isEmpty()) {
					for (Cookie cookie : cookies) {
						CookieEncoder ce = new CookieEncoder(false);
						ce.addCookie(cookie);
						httpCookieEncoderList.add(ce);
					}
				}
			}
		}
		return httpCookieEncoderList;
	}
}
