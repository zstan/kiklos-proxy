package kiklos.tv.timetable;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.TimeUnit;

import kiklos.proxy.core.Pair;

import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirWatchDog {	
	private static final String TIMETABLE_DIR = "./timetable";
	private static final String TIMETABLE_MAP_NAME = ".timetable";
	private static final String FILE_MASK = ".txt"; 
    private static final Logger LOG = LoggerFactory.getLogger(DirWatchDog.class);
    private Map<String, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> mapExternal;
    private volatile Map<String, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> mapInternal;
	
	public DirWatchDog(final Redisson memStorage) {
		mapExternal = memStorage.getMap(TIMETABLE_MAP_NAME);
		mapInternal = getRemoteCollection();
		Thread t = new Thread(new MapUpdater());
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	private void listFilesForFolder(final File folder) {
		List <String> files;
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isFile() && fileEntry.getName().endsWith(FILE_MASK)) {
	            System.out.println();
	        }
	    }
	}	
	
	private Map<String, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> getRemoteCollection() {
		Map<String, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> tmp = new HashMap<>();
		tmp.putAll(mapExternal);
		return Collections.unmodifiableMap(tmp);
	}
	
    private class MapUpdater implements Runnable {
        @Override
        public void run() {
        	while (true) {
	            LOG.debug("check timetable dir");
	            mapInternal = getRemoteCollection();
	            try {
					TimeUnit.MINUTES.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        }
    }

}
