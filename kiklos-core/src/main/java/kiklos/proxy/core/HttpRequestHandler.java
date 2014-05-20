package kiklos.proxy.core;

import java.util.Date;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

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
import target.eyes.vag.codec.xml.javolution.mast.impl.Condition;
import target.eyes.vag.codec.xml.javolution.mast.impl.MAST;
import target.eyes.vag.codec.xml.javolution.mast.impl.Source;
import target.eyes.vag.codec.xml.javolution.mast.impl.Sources;
import target.eyes.vag.codec.xml.javolution.mast.impl.StartConditions;
import target.eyes.vag.codec.xml.javolution.mast.impl.Trigger;
import target.eyes.vag.codec.xml.javolution.mast.impl.Triggers;
import target.eyes.vag.codec.xml.javolution.vast.v2.impl.VAST;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import com.ning.http.client.Response.ResponseBuilder;

public class HttpRequestHandler extends SimpleChannelUpstreamHandler {
	private HttpRequest request;
	private final AsyncHttpClient asyncClient;
	private final PlacementsMapping plMap;
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
	
	private Pair<List<String>, String> reqTransformer(final String req) {
		String trans = ""; 
		String main = "";
		List<String> vastList = Collections.emptyList();
		QueryStringDecoder decoder = new QueryStringDecoder(req);
		
		Map<String, List<String>> params = decoder.getParameters(); 
		if (!params.isEmpty())
			if (!params.get("id").isEmpty()) {
				final String id = decoder.getParameters().get("id").get(0);
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
	
	private String createMastFromVastList(final List<String> vastList) {
		MAST m1 = new MAST();
		m1.setSchemaLocation("http://openvideoplayer.sf.net/mast");
		m1.setXmlns("http://openvideoplayer.sf.net/mast");
		m1.setXsi("http://www.w3.org/2001/XMLSchema-instance");
		Trigger tr1 = new Trigger();
		tr1.setDescription("test_preroll");
		tr1.setId("preroll");
		Triggers trgs = new Triggers();
		trgs.getTriggers().add(tr1);
		m1.setTriggers(trgs);
		Sources ss = new Sources();
		tr1.setSources(ss);
		StartConditions sCond = tr1.getStartConditions();
		Condition cond = new Condition();
		cond.setName("OnItemStart");
		cond.setType("event");
		sCond.getStartConditions().add(cond);
		
		
		for (final String vast: vastList) {
			Source s1 = new Source();
			s1.setFormat("vast");
			s1.setUri(vast);		
			ss.getSources().add(s1);		
		}
		
		StringWriter sw = new StringWriter();
		
		XMLObjectWriter ow = new XMLObjectWriter();
		try {
			ow.setOutput(sw);
			ow.write(m1, "MAST", MAST.class);
			ow.flush();			
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		return sw.toString();
	}
	
	private void writeResp0(MessageEvent e, final String body, final List<CookieEncoder> cookieList) {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		
		response.setContent(ChannelBuffers.copiedBuffer(body, CharsetUtil.UTF_8));
		response.headers().add(HttpHeaders.Names.CONTENT_TYPE, TEXT_CONTENT_TYPE);
		for (CookieEncoder ce: cookieList)
			response.headers().add(HttpHeaders.Names.SET_COOKIE, ce.encode());
		response.headers().add(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_CACHE);
		
		ChannelFuture future = e.getChannel().write(response);
		HttpHeaders.setContentLength((HttpMessage)e.getMessage(), response.getContent().readableBytes());
		future.addListener(ChannelFutureListener.CLOSE);
	}				
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent mEvent) throws Exception {
		
		request = (HttpRequest)mEvent.getMessage();
				
		if (HttpHeaders.is100ContinueExpected(request)) {
			send100Continue(mEvent);
		}
		
		final String Uri = request.getUri();
		
		if ("/favicon.ico".equals(Uri)) {
			mEvent.getChannel().close();
			return;			
		}

		//Response resp = new ResponseBuilder
		
		final Pair<List<String>, String> newUriPair = reqTransformer(Uri);
		final List<String> VASTList = newUriPair.getFirst();
		final String newPath = VASTList.isEmpty() ? "" : VASTList.get(0);
		final String newParams = newUriPair.getSecond();

	    String remoteHost = ((InetSocketAddress)ctx.getChannel().getRemoteAddress()).getAddress().getHostAddress();
	    int remotePort = ((InetSocketAddress)ctx.getChannel().getRemoteAddress()).getPort();
	    LOG.info(String.format("host: %s port: %d", remoteHost, remotePort));						
		
		if (!newUriPair.getFirst().isEmpty()) {
			memLogStorage.put(composeLogString(request, newPath + newParams, remoteHost));
		}
		
		if (newPath.isEmpty()) {
			mEvent.getChannel().close();
			return;
		}
				
		if (VASTList.size() > 1) {
			//List<CookieEncoder> cookieList = storeADSessionCookie(response);
			String mast = createMastFromVastList(VASTList);
			List<CookieEncoder> cookieList = Collections.emptyList();
			writeResp0(mEvent, mast, cookieList);
			return;
		}
		
		LOG.info("\n---------------------------------------------");	
		
		BoundRequestBuilder rb = asyncClient.prepareGet(String.format("%s%s", newPath, newParams)); 
		
		for (CookieEncoder ce : getSessionCookies(request)) {
			rb.addHeader(COOKIE, ce.encode());
			LOG.debug("cookie: {}", ce.encode());
		}
				
			rb.execute(new AsyncCompletionHandler<Response>(){

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
				
				writeResp(mEvent, cookieList);
				//VAST v = VASTv2Parser.parse(response.getResponseBody());
				LOG.info("req incoming : {}, req transformed : {}", Uri, newPath, newParams);
				LOG.info("status code : {}, request size: {}", response.getStatusCode(), response.getResponseBody().length());
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
	
	private List<CookieEncoder> getSessionCookies(HttpRequest request) {
		List<String> cookieStrings = request.headers().getAll(COOKIE);
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
