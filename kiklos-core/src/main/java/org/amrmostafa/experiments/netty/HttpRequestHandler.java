package org.amrmostafa.experiments.netty;

import java.util.Date;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
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
	private final String FAKE_USER_AGENT = "Opera/9.80 (X11; Linux x86_64) Presto/2.12.388 Version/12.16";
	private static final String FILE_ENCODING = UTF_8.name();
	private static final String TEXT_CONTENT_TYPE = "text/plain; charset=" + FILE_ENCODING;
	private static final String SESSION_ID_COOKIE = "sess";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH-mm-ss_dd-MM-yyyy");
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestHandler.class);
    private final MemoryLogStorage memLogStorage;
	
	HttpRequestHandler(AsyncHttpClient c, final PlacementsMapping placements, final MemoryLogStorage logStorage) {
		asyncClient = c;
		plMap = placements;
		memLogStorage = logStorage;
	}
	
	private String reqTransformer(final String req) {
		String out = req;
		QueryStringDecoder decoder = new QueryStringDecoder(req);
		if (!decoder.getParameters().isEmpty()) {
			final String id = decoder.getParameters().get("id").get(0);
			String newId = plMap.getMappingPlacement(id);
			decoder.getParameters().remove("id");
			decoder.getParameters().put("id", Arrays.asList(newId));
			
			// stub !!!
			
			out = decoder.getPath() + "?";
			for (Map.Entry<String, List<String>> e: decoder.getParameters().entrySet()) {
				out += e.getKey();
				for (String val: e.getValue())
					out += "=" + val;
				out += "&"; 
			}
		}
		return out;
	}
	
	private String composeLogString(final MessageEvent e) {
		final String date = DATE_FORMAT.format(new Date());
		HttpRequest req = (HttpRequest)e.getMessage();
		final String Uri = req.getUri();
		String cookieString = "<err cookie>";
		final String cString = req.headers().get(COOKIE);
		try {			
			cookieString = URLEncoder.encode(cString, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			LOG.error("can`t encode cookie: {}", cString);
		}
		return String.format("%s\t%s\t%s", date, Uri, cookieString);
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		if (HttpHeaders.is100ContinueExpected(request)) {
			send100Continue(e);
		}
		
		final String Uri = ((HttpRequest)e.getMessage()).getUri();
		
		memLogStorage.put(composeLogString(e));
		
		final String newUri = reqTransformer(Uri);
		if (newUri.isEmpty()) {
			e.getChannel().close();
			return;
		}
		
		System.out.println("\n\n---------------------------------------------");		
		
		Cookie cook = findSessionIdCookie((HttpRequest)e.getMessage());		
		System.out.println("cook: " + cook.getName() + ":" + cook.getValue());
		
		asyncClient.prepareGet(String.format("%s%s", AD_DOMAIN, newUri)).addHeader("user-agent", FAKE_USER_AGENT).execute(new AsyncCompletionHandler<Response>(){

			StringBuilder buff = new StringBuilder(4096);
			
			private void writeResp(MessageEvent e) {
				HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				
				response.setContent(ChannelBuffers.copiedBuffer(buff.toString(), CharsetUtil.UTF_8));
				response.headers().add(HttpHeaders.Names.CONTENT_TYPE, TEXT_CONTENT_TYPE);
				
				ChannelFuture future = e.getChannel().write(response);
				future.addListener(ChannelFutureListener.CLOSE);
			}			
			
			@Override
			public Response onCompleted(Response response) throws Exception {
				if (!response.getResponseBody().isEmpty())
					buff.append(response.getResponseBody());
				else
					buff.append(EMPTY_VAST);
				writeResp(e);
				//List<String> list = redisson.getList("anyList");			
				//List<String> list = redisson. getList("mylist");
				//VAST v = VASTv2Parser.parse(response.getResponseBody());
				System.out.println("req incoming : " + Uri);
				System.out.println("req transformed : " + newUri);
				System.out.println("status code : " + response.getStatusCode());
				System.out.println("request size:" + response.getResponseBody().length());
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
	
	private Cookie findSessionIdCookie(HttpRequest request) {
		String cookieString = request.headers().get(COOKIE);
		if (cookieString != null) {
			CookieDecoder cookieDecoder = new CookieDecoder();
			Set<Cookie> cookies = cookieDecoder.decode(cookieString);
			if (!cookies.isEmpty()) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals(SESSION_ID_COOKIE)) {
						return cookie;
					}
				}
			}
		}
		return null;
	}	
}
