package org.amrmostafa.experiments.netty;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.redisson.Redisson;

public class PlacementsMapping {
	private final Redisson storage = Redisson.create();
	private final static String PLACEMENTS_MAP_NAME = ".placements";
	private Map<String, List<String>> placements = new ConcurrentHashMap<>();
	
	public PlacementsMapping() {
		Map<String, List<String>> pl = storage.getMap(PLACEMENTS_MAP_NAME);
		pl.put("111", Arrays.asList("2504637", "some comment1"));
		pl.put("222", Arrays.asList("2504638", "some comment2"));
		
		placements.putAll(pl);
	}
	
	public List<String> getMappingPlacementList(final String key) {
		return placements.get(key);
	}
	
	public String getMappingPlacement(final String key) {
		List<String> val = placements.get(key); 
		if (val != null)
			return val.get(0);
		else
			return "";
	}	
}
