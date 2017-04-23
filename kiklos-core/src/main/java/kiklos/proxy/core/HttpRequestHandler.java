package kiklos.proxy.core;

import java.util.*;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;

import io.netty.handler.codec.http.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.REFERER;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.USER_AGENT;
import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestHandler extends ChannelInboundHandlerAdapter {

	//private final AsyncHttpClient httpClient;
	private final CookieFabric cookieFabric;
	private static final String FILE_ENCODING = UTF_8.name();
	private static final String XML_CONTENT_TYPE = "application/xml; charset=" + FILE_ENCODING;
	private static final String IGIF_CONTENT_TYPE = "image/gif";
	private static final int COOKIE_MAX_AGE = 60*60*24*30*3;
	public static short MAX_DURATION_BLOCK = 900;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestHandler.class);
    private final MemoryLogStorage memLogStorage;

	static byte[] trackingGif = { 0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x1, 0x0, 0x1, 0x0, (byte) 0x80, 0x0, 0x0, (byte)  0xff, (byte)  0xff,  (byte) 0xff, 0x0, 0x0, 0x0, 0x2c, 0x0, 0x0, 0x0, 0x0, 0x1, 0x0, 0x1, 0x0, 0x0, 0x2, 0x2, 0x44, 0x1, 0x0, 0x3b };
	static byte[] trackingPng = {(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,0x00,0x00,0x00,0x0D,0x49,0x48,0x44,0x52,0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,0x08,0x06,0x00,0x00,0x00,0x1F,0x15,(byte)0xC4,(byte)0x89,0x00,0x00,0x00,0x0B,0x49,0x44,0x41,0x54,0x78,(byte)0xDA,0x63,0x60,0x00,0x02,0x00,0x00,0x05,0x00,0x01,(byte)0xE9,(byte)0xFA,(byte)0xDC,(byte)0xD8,0x00,0x00,0x00,0x00,0x49,0x45,0x4E,0x44,(byte)0xAE,0x42,0x60,(byte)0x82};

	public static byte[] get1x1PixelImage() {
		return trackingGif;   // trackingGif is about 38 bytes where trackingPng is 68
	}

	HttpRequestHandler(HttpServerPipelineFactory instance) {
		//httpClient = instance.getHttpClient();
		memLogStorage = instance.getMemLogStorage();
		cookieFabric = instance.getCookieFabric();
	}
	
	private String composeLogString(final HttpRequest req, final String remoteHost) {
		final String date = DATE_FORMAT.format(new Date());
		final String uri = req.uri();
		String cookieStr = req.headers().get(COOKIE);
		String refererStr = req.headers().get(REFERER);
		String userAgentStr = req.headers().get(USER_AGENT);

		Set<Cookie> cookie = CookieDecoder.decode(cookieStr == null ? "" : cookieStr);

		return String.format("%s\t%s\t%s\t%s\t%s\t%s", date, uri, refererStr, userAgentStr, cookie.toString(), remoteHost);
	}

	private void writeResp(final ChannelHandlerContext ctx, final HttpRequest msg,
			final byte[] buff, List<Cookie> cookieList, final String contentType) {

		System.err.println(HttpRequestHandler.class.getName());

		ByteBuf bb = Unpooled.wrappedBuffer(buff);
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, bb);

		Pair<Cookie, List<Cookie>> ourAndSessionCookPair = CookieFabric.getUserSessionCookies(msg);
		final Cookie stCookie = ourAndSessionCookPair.getKey();
		final List<Cookie> sessionCookieList = ourAndSessionCookPair.getValue();

		LOG.debug("sessionCookieList: " + sessionCookieList.size());

		if (sessionCookieList.isEmpty())
			cookieList.add(createCookie());
				
		LOG.debug(cookieList.toString());
		for (Cookie c: cookieList) {
			response.headers().add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(c));
			LOG.debug("writeResp set cookie: {}", ServerCookieEncoder.encode(c));
		}
		response.headers().add(HttpHeaders.Names.CONTENT_LENGTH, buff.length);
		response.headers().add(HttpHeaders.Names.CONTENT_TYPE, contentType);
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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof HttpRequest) {
        	
            HttpRequest request = (HttpRequest) msg;
			if (HttpHeaders.is100ContinueExpected(request)) {
				ctx.write(new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.CONTINUE));
			}

			final String reqUri = request.getUri();

			if ("/favicon.ico".equals(reqUri.toLowerCase())) {
				ctx.channel().close();
				return;			
			}

			final InetSocketAddress sa = (InetSocketAddress)ctx.channel().remoteAddress();
		    final String remoteHost = sa.getAddress().getHostAddress();
		    //LOG.info(String.format("host: %s port: %d", remoteHost, sa.getPort()));
			LOG.info(composeLogString(request, remoteHost));

			writeResp(ctx, (HttpRequest)msg, get1x1PixelImage(), new ArrayList<>(), IGIF_CONTENT_TYPE);
        }
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
	
	private Cookie createCookie() {
		Cookie c = new DefaultCookie(CookieFabric.OUR_COOKIE_NAME, cookieFabric.generateUserId(System.currentTimeMillis()));
		c.setMaxAge(COOKIE_MAX_AGE);
		c.setPath("/");
		c.setHttpOnly(true);
		return c;
	}
	
}
