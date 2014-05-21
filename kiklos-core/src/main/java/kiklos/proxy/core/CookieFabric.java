package kiklos.proxy.core;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

public class CookieFabric {
	
	private final static int byteMask = 0x4f;	
	private final static SecureRandom random = new SecureRandom();	
	private final static String substitunionTable = "5FRA7KObkcHinBvxu.wUZX6YpdfTWDMVlhQ1gsGj_Le029SC3yPmratNJz84oqEIm32rm13rm12p3or12perk3m452m345;2m45k6m57lm567;4m567m467;4km67km";
	private MessageDigest md;
	
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
		Boolean b = false;
		foo(b);
		System.out.println(b);
	}
	
	private static void foo(Boolean b) {
		b = true;
	}
	
}
