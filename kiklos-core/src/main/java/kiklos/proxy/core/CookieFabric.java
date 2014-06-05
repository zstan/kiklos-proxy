package kiklos.proxy.core;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.Response;

public class CookieFabric {
	
	private final static int byteMask = 0x4f;	
	private final static Random random = new SecureRandom();	
	private final static String substitunionTable = "5FRA7KObkcHinBvxu.wUZX6YpdfTWDMVlhQ1gsGj_Le029SC3yPmratNJz84oqEIm32rm13rm12p3or12perk3m452m345;2m45k6m57lm567;4m567m467;4km67km";
	private MessageDigest md;
	private static final Logger LOG = LoggerFactory.getLogger(CookieFabric.class);
    public static final String OUR_COOKIE_NAME = "tuid";
	
	private CookieFabric() {}
	
	public static CookieFabric buildCookieFabric() {
		CookieFabric cf = new CookieFabric();		
		try {
			cf.md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		return cf;
	}
	
	public String generateUserId(long sessionCreationTime) {
		
		String cString = Long.toString(sessionCreationTime);
		cString += Integer.toString(random.nextInt());
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
	
	public static Pair<Cookie, CookieEncoder> getSessionCookies(final HttpRequest request) {		
		List<String> cookieStrings = request.headers().getAll(COOKIE);
		if (cookieStrings == null)
			return new Pair<Cookie, CookieEncoder>(null, null);
		
		List<Cookie> httpCookieList = new ArrayList<>();
		CookieDecoder cookieDecoder = new CookieDecoder();
		
		for (String cookieString : cookieStrings) {
			Set<Cookie> cookies = cookieDecoder.decode(cookieString);
			httpCookieList.addAll(cookies);
		}
		LOG.debug("getSessionCookies size: {}", httpCookieList.size());
		
		CookieEncoder ce = new CookieEncoder(false);
		Cookie ourCookie = null;
		for (Cookie cookie : httpCookieList) {
			if (cookie.getName().equals(OUR_COOKIE_NAME)) {
				ourCookie = cookie;
			}
			ce.addCookie(cookie);
		}
		return new Pair<>(ourCookie, ce);
	}
	
	public static List<CookieEncoder> getResponseCookies(final Response request) {		
		List<String> cookieStrings = request.getHeaders(SET_COOKIE);
		List<CookieEncoder> httpCookieEncoderList = new ArrayList<>();
		
		if (cookieStrings != null) {
			LOG.debug("getResponseCookies: {} len: {}", SET_COOKIE, cookieStrings.size());			
			for (String cookieString : cookieStrings) {
				if (cookieString != null) {
					LOG.debug("{} string: {}", SET_COOKIE, cookieString);
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
		}
		return httpCookieEncoderList;
	}		
	
	public static void main(String[] arg) {
		CookieFabric cf = new CookieFabric();
		Set<String> ss = new HashSet<>();
		for (int i = 0; i< 1000000; ++i) {
			String s = cf.generateUserId(System.currentTimeMillis());
			if (ss.contains(s))
				System.out.println("!!!");
			else
				ss.add(s);
		}
/*		System.out.println(cf.generateUserId(System.currentTimeMillis()));
		System.out.println(cf.generateUserId(System.currentTimeMillis()));
		System.out.println(cf.generateUserId(System.currentTimeMillis()));
		System.out.println(cf.generateUserId(System.currentTimeMillis()));
		System.out.println(cf.generateUserId(System.currentTimeMillis()));*/
	}
	
}
