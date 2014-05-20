package kiklos.proxy.core;

import java.security.SecureRandom;

public class CookieFabric {
	
	private final static int byteMask = 0x3f;	
	private final static SecureRandom random = new SecureRandom();
	private final static String substitunionTable = "5FRA7KObkcHinBvxu.wUZX6YpdfTWDMVlhQ1gsGj_Le029SC3yPmratNJz84oqEIm32rm13rm12p3or12perk3m452m345;2m45k6m57lm567;4m567m467;4km67km";	
	
	public String generateUserId(long sessionCreationTime) {

		char[] uuid = new char[20];

		int seed = 0;
		long timeInMils = 0;
		long counter = 0;

		seed = random.nextInt();
		counter = random.nextInt(100);
		timeInMils = sessionCreationTime;
		
		uuid[0] = substitunionTable.charAt( ((seed >>> 24) & byteMask));
		uuid[1] = substitunionTable.charAt( ((seed >>> 16) & byteMask));
		uuid[2] = substitunionTable.charAt( ((seed >>> 8) & byteMask));

		uuid[3] = substitunionTable.charAt( ((seed) & byteMask));

		uuid[4] = substitunionTable.charAt( (int)((timeInMils >>> 32) & byteMask));
		uuid[5] = substitunionTable.charAt( (int)((timeInMils >>> 24) & byteMask));
		uuid[6] = substitunionTable.charAt( (int)((timeInMils >>> 16) & byteMask));
		uuid[7] = substitunionTable.charAt( (int)((timeInMils >>> 2) & byteMask));

		uuid[8] = substitunionTable.charAt( (int)((timeInMils) & byteMask));

		uuid[9] = substitunionTable.charAt( (int)((counter >>> 2) & byteMask));
		uuid[10] = substitunionTable.charAt( (int)((counter >>> 4) & byteMask));
		uuid[11] = substitunionTable.charAt( (int)((counter >>> 8) & byteMask));

		uuid[12] = substitunionTable.charAt( (int)(((counter + timeInMils) >>> 4) & byteMask));
		uuid[13] = substitunionTable.charAt( (int)(((counter + timeInMils) >>> 2) & byteMask));
		uuid[14] = substitunionTable.charAt( (int)((counter + timeInMils) & byteMask));
		
		uuid[15] = substitunionTable.charAt( ((seed >>> 1) & byteMask));
		uuid[16] = substitunionTable.charAt( ((seed >>> 2) & byteMask));
		uuid[17] = substitunionTable.charAt( ((seed >>> 3) & byteMask));
		uuid[18] = substitunionTable.charAt( ((seed >>> 4) & byteMask));
		uuid[19] = substitunionTable.charAt( ((seed >>> 5) & byteMask));
		

		return new String(uuid);
	}
	
	public static void main(String[] arg) {
		CookieFabric cf = new CookieFabric();
		System.out.println(cf.generateUserId(System.currentTimeMillis()));
		System.out.println(cf.generateUserId(System.currentTimeMillis()));
		System.out.println(cf.generateUserId(System.currentTimeMillis()));
		System.out.println(cf.generateUserId(System.currentTimeMillis()));
		System.out.println(cf.generateUserId(System.currentTimeMillis()));
		System.out.println(cf.generateUserId(System.currentTimeMillis()));
	}
	
}
