package kiklos.planner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DurationSettings {
	private static final String DURATIONS_MAP_NAME = ".durations";
    private static final Logger LOG = LoggerFactory.getLogger(DurationSettings.class);
    private volatile Map<Integer, String> durationsMap;
    private Map<Integer, String> durExternal;
    private volatile List<Integer> keyList = new ArrayList<>();
    private final ExecutorService pool;
    
	public DurationSettings(final RedissonClient memStorage, final ExecutorService execPool) {
		durExternal = memStorage.getMap(DURATIONS_MAP_NAME);
		durationsMap = getRemoteCollection();
		pool = execPool;
		pool.execute(new DurationsUpdater());
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
	            LOG.debug("Read new durations config start");	            
	            durationsMap = getRemoteCollection();
	            LOG.debug("Read new durations config finish");
	            try {
					TimeUnit.MINUTES.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        }
    }	
}
