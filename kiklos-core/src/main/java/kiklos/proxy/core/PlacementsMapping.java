package kiklos.proxy.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlacementsMapping {	
	private static final String PLACEMENTS_MAP_NAME = ".placements";
    private static final Logger LOG = LoggerFactory.getLogger(PlacementsMapping.class);
	private volatile Map<String, List<String>> placements;
	private Map<String, List<String>> plExternal;
	private final ExecutorService pool; 
	
	public PlacementsMapping(final Redisson memStorage, final ExecutorService execPool) {
		
		//pl.put("111", Arrays.asList("2504637", "some comment1"));
		plExternal = memStorage.getMap(PLACEMENTS_MAP_NAME);
		placements = getRemoteCollection();
		pool = execPool;
		pool.execute(new PlacementsUpdater());
	}
	
	private Map<String, List<String>> getRemoteCollection() {
		Map<String, List<String>> tmp = new HashMap<>(plExternal.size());
		tmp.putAll(plExternal);
		return Collections.unmodifiableMap(tmp);
	}
	
	public List<String> getMappingPlacementList(final String key) {
		return placements.get(key);
	}
	
	public List<String> getMappingVASTList(final String key) {
		List<String> val = placements.get(key); 
		if (val != null)
			return val;
		else
			return Collections.emptyList();
	}	

    private class PlacementsUpdater implements Runnable {
        @Override
        public void run() {
        	while (true) {
	            LOG.info("Read new placements config");
	            placements = getRemoteCollection();
	            try {
					TimeUnit.MINUTES.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        }
    }

}
