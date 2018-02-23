package kiklos.proxy.core;

import static io.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static io.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.HttpRequest;

import org.apache.commons.lang3.tuple.Pair;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CookieFabric {
	
	private final static int byteMask = 0x4f;	
	private final static Random random = new SecureRandom();	
	private final static String substitunionTable = "5FRA7KObkcHinBvxu.wUZX6YpdfTWDMVlhQ1gsGj_Le029SC3yPmratNJz84oqEIm32rm13rm12p3or12perk3m452m345;2m45k6m57lm567;4m567m467;4km67km";
	private MessageDigest md;
	private static final Logger LOG = LoggerFactory.getLogger(CookieFabric.class);
    static final String OUR_COOKIE_NAME = "tuid";
	
	private CookieFabric() {}
	
	static CookieFabric buildCookieFabric() {
		CookieFabric cf = new CookieFabric();		
		try {
			cf.md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		return cf;
	}
	
	String generateUserId() {
		long sessionCreationTime = System.currentTimeMillis();

		String cString = Long.toString(sessionCreationTime) + Integer.toString(random.nextInt());

		byte[] bytesOfMessage;

		try {
			bytesOfMessage = cString.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
		byte[] thedigest = md.digest(bytesOfMessage);
		int seed = random.nextInt();
		
		char[] uuid = new char[20];

		uuid[0] = substitunionTable.charAt( (thedigest[0] & byteMask));
		uuid[1] = substitunionTable.charAt( (thedigest[1] & byteMask));
		uuid[2] = substitunionTable.charAt( (thedigest[2] & byteMask));

		uuid[3] = substitunionTable.charAt( (thedigest[3] & byteMask));

		uuid[4] = substitunionTable.charAt( (thedigest[4] & byteMask));
		uuid[5] = substitunionTable.charAt( (thedigest[5] & byteMask));
		uuid[6] = substitunionTable.charAt( (thedigest[6] & byteMask));
		uuid[7] = substitunionTable.charAt( (thedigest[7] & byteMask));

		uuid[8] = substitunionTable.charAt( (thedigest[8] & byteMask));

		uuid[9] = substitunionTable.charAt( (thedigest[9] & byteMask));
		uuid[10] = substitunionTable.charAt( (thedigest[10] & byteMask));
		uuid[11] = substitunionTable.charAt( (thedigest[11] & byteMask));

		uuid[12] = substitunionTable.charAt( (thedigest[12] & byteMask));
		uuid[13] = substitunionTable.charAt( (thedigest[13] & byteMask));
		uuid[14] = substitunionTable.charAt( (thedigest[14] & byteMask));
		
		uuid[15] = substitunionTable.charAt( ((thedigest[14] + seed) & byteMask));
		uuid[16] = substitunionTable.charAt( ((thedigest[13] + seed) & byteMask));
		uuid[17] = substitunionTable.charAt( ((thedigest[12] + seed) & byteMask));
		uuid[18] = substitunionTable.charAt( ((thedigest[11] + seed) & byteMask));
		uuid[19] = substitunionTable.charAt( ((thedigest[10] + seed) & byteMask));

		return new String(uuid);
	}

	static Pair<io.netty.handler.codec.http.cookie.Cookie, List<io.netty.handler.codec.http.cookie.Cookie>> getUserSessionCookies(final HttpRequest request) {
		List<String> cookieStrings = request.headers().getAll(COOKIE);

        if (LOG.isDebugEnabled())
		    LOG.debug("getUserSessionCookies cookieStrings size: {}", cookieStrings.size());
		
		List<io.netty.handler.codec.http.cookie.Cookie> httpCookieList = new ArrayList<>();
		
		if (cookieStrings.isEmpty())
			return Pair.of(null, httpCookieList);		
		
		for (String cookieString : cookieStrings) {
			Set<Cookie> cookies = CookieDecoder.decode(cookieString);
			httpCookieList.addAll(cookies);
		}
        if (LOG.isDebugEnabled())
		    LOG.debug("getUserSessionCookies size: {}", httpCookieList.size());

        io.netty.handler.codec.http.cookie.Cookie ourCookie = null;
		for (io.netty.handler.codec.http.cookie.Cookie cookie : httpCookieList) {
			if (cookie.name().equals(OUR_COOKIE_NAME)) {
				ourCookie = cookie;
			}
		}
		return Pair.of(ourCookie, httpCookieList);
	}
	
	static List<io.netty.handler.codec.http.cookie.Cookie> getResponseCookies(final Response request) {
		List<String> cookieStrings = request.getHeaders(SET_COOKIE);
		List<io.netty.handler.codec.http.cookie.Cookie> httpCookieList = new ArrayList<>();
		
		if (cookieStrings != null) {
			if (LOG.isDebugEnabled())
				LOG.debug("getResponseCookies: {} len: {}", SET_COOKIE, cookieStrings.size());
			for (String cookieString : cookieStrings) {
				if (cookieString != null) {
					if (LOG.isDebugEnabled())
						LOG.debug("{} string: {}", SET_COOKIE, cookieString);
					Set<Cookie> cookies = CookieDecoder.decode(cookieString);
					if (!cookies.isEmpty()) {
						httpCookieList.addAll(cookies);
					}
				}
			}
		}
		return httpCookieList;
	}
}
