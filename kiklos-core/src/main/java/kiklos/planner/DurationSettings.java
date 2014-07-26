package kiklos.planner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DurationSettings {
	private static final String DURATIONS_MAP_NAME = ".durations";
    private static final Logger LOG = LoggerFactory.getLogger(DurationSettings.class);
    private volatile Map<Integer, String> durationsMap;
    private Map<Integer, String> durExternal;
    private volatile List<Integer> keyList = new ArrayList<>();
    
	public DurationSettings(final Redisson memStorage) {
		
		durExternal = memStorage.getMap(DURATIONS_MAP_NAME);
		durationsMap = getRemoteCollection();
		Thread t = new Thread(new DurationsUpdater());
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}  
	
	private Map<Integer, String> getRemoteCollection() {
		Map<Integer, String> tmp = new HashMap<>(durExternal.size());
		tmp.putAll(durExternal);
		List<Integer> tmpL = new ArrayList<>(); 
		tmpL.addAll(tmp.keySet());
		keyList = tmpL;
		Collections.sort(keyList);
		return Collections.unmodifiableMap(tmp);
	}
	
	public List<Integer> getDurationsList() {
		if (LOG.isDebugEnabled()) {
			String sOut = "";
			for (int i: keyList)
				sOut += i + " ";
			LOG.debug("getDurationsList: {}", sOut);
		}
		return keyList;
	}
	
	public String getPlacement(final Integer duration) {
		return durationsMap.get(duration);
	}	
	
    private class DurationsUpdater implements Runnable {
        @Override
        public void run() {
        	while (true) {
	            LOG.debug("Read new durations config");
	            durationsMap = getRemoteCollection();
	            try {
					TimeUnit.MINUTES.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        }
    }	
}
