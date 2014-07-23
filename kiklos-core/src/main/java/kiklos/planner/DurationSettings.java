package kiklos.planner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DurationSettings {
	private static final String DURATIONS_MAP_NAME = ".durations";
    private static final Logger LOG = LoggerFactory.getLogger(DurationSettings.class);
    private volatile Map<Integer, String> durationsMap;
    private Map<Integer, String> durExternal;
    
	public DurationSettings(final Redisson memStorage) {
		
		durExternal = memStorage.getMap(DURATIONS_MAP_NAME);
		durExternal.put(1, "value");
		durationsMap = getRemoteCollection();
		Thread t = new Thread(new DurationsUpdater());
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}  
	
	private Map<Integer, String> getRemoteCollection() {
		Map<Integer, String> tmp = new HashMap<>(durExternal.size());
		tmp.putAll(durExternal);
		return Collections.unmodifiableMap(tmp);
	}
	
	public Set<Integer> getDurationsSet() {
		return durationsMap.keySet();
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
					TimeUnit.MINUTES.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        }
    }	
}
