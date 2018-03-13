package kiklos.proxy.core;

import static io.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CookieFabric {
	
	private final static int byteMask = 0x4f;	
	private final static ThreadLocal<SplittableRandom> random = ThreadLocal.withInitial(() -> new SplittableRandom());
	private final static String substitutionTable = "5FRA7KObkcHinBvxu.wUZX6YpdfTWDMVlhQ1gsGj_Le029SC3yPmratNJz84oqEIm32rm13rm12p3or12perk3m452m345;2m45k6m57lm567;4m567m467;4km67km";
    private static final int COOKIE_MAX_AGE = 60*60*24*30*3;
	private final static ThreadLocal<MessageDigest> md = new ThreadLocal<MessageDigest>() {
        @Override protected MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
        }
    };
	private static final Logger LOG = LoggerFactory.getLogger(CookieFabric.class);
    static final String UID_COOKIE_NAME = "tuid";
	
	public CookieFabric() {}
	
	String generateUserId() {
        long sessionCreationTime = System.currentTimeMillis();

        String cString = Long.toString(sessionCreationTime) + Integer.toString(random.get().nextInt());

		byte[] bytesOfMessage;

        try {
			bytesOfMessage = cString.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
		byte[] thedigest = md.get().digest(bytesOfMessage);
		int seed = random.get().nextInt();
		
		char[] uuid = new char[20];

		uuid[0] = substitutionTable.charAt( (thedigest[0] & byteMask));
		uuid[1] = substitutionTable.charAt( (thedigest[1] & byteMask));
		uuid[2] = substitutionTable.charAt( (thedigest[2] & byteMask));

		uuid[3] = substitutionTable.charAt( (thedigest[3] & byteMask));

		uuid[4] = substitutionTable.charAt( (thedigest[4] & byteMask));
		uuid[5] = substitutionTable.charAt( (thedigest[5] & byteMask));
		uuid[6] = substitutionTable.charAt( (thedigest[6] & byteMask));
		uuid[7] = substitutionTable.charAt( (thedigest[7] & byteMask));

		uuid[8] = substitutionTable.charAt( (thedigest[8] & byteMask));

		uuid[9] = substitutionTable.charAt( (thedigest[9] & byteMask));
		uuid[10] = substitutionTable.charAt( (thedigest[10] & byteMask));
		uuid[11] = substitutionTable.charAt( (thedigest[11] & byteMask));

		uuid[12] = substitutionTable.charAt( (thedigest[12] & byteMask));
		uuid[13] = substitutionTable.charAt( (thedigest[13] & byteMask));
		uuid[14] = substitutionTable.charAt( (thedigest[14] & byteMask));
		
		uuid[15] = substitutionTable.charAt( ((thedigest[14] + seed) & byteMask));
		uuid[16] = substitutionTable.charAt( ((thedigest[13] + seed) & byteMask));
		uuid[17] = substitutionTable.charAt( ((thedigest[12] + seed) & byteMask));
		uuid[18] = substitutionTable.charAt( ((thedigest[11] + seed) & byteMask));
		uuid[19] = substitutionTable.charAt( ((thedigest[10] + seed) & byteMask));

		return new String(uuid);
	}

	static Map.Entry<Cookie, List<Cookie>> getUserSessionCookies(final List<String> cookieStrings) {
        if (LOG.isDebugEnabled())
		    LOG.debug("getUserSessionCookies cookieStrings size: {}", cookieStrings.size());

        List<Cookie> httpCookieList = new ArrayList<>(cookieStrings.size());

        if (cookieStrings.isEmpty())
            return Pair.of(null, httpCookieList);

        for (String cookieString : cookieStrings) {
			Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieString);
			httpCookieList.addAll(cookies);
		}
        if (LOG.isDebugEnabled())
		    LOG.debug("getUserSessionCookies size: {}", httpCookieList.size());

        Cookie ourCookie = null;
		for (Cookie cookie : httpCookieList) {
			if (cookie.name().equals(UID_COOKIE_NAME)) {
				ourCookie = cookie;
			}
		}
		return Pair.of(ourCookie, httpCookieList);
	}
	
	static List<Cookie> getResponseCookies(final List<String> cookies) {
		List<Cookie> httpCookieList = new ArrayList<>();
		
		if (cookies != null) {
			if (LOG.isDebugEnabled())
				LOG.debug("getResponseCookies: {} len: {}", SET_COOKIE, cookies.size());
			for (String cookieString : cookies) {
				if (cookieString != null) {
					if (LOG.isDebugEnabled())
						LOG.debug("{} string: {}", SET_COOKIE, cookieString);
					Cookie cookies0 = ClientCookieDecoder.STRICT.decode(cookieString);
					if (cookies0 != null) {
						httpCookieList.add(cookies0);
					}
				}
			}
		}
		return httpCookieList;
	}

    public Cookie createCookie() {
        Cookie c = new DefaultCookie(CookieFabric.UID_COOKIE_NAME, generateUserId());
        c.setMaxAge(COOKIE_MAX_AGE);
        c.setPath("/");
        c.setDomain(".1tv.ru");
        c.setHttpOnly(true);
        return c;
    }
}
