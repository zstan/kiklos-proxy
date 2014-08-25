package kiklos.tv.timetable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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
	private final File timeTableFolder = new File("./timetable");
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
	
	private List <Pair<String, String>> listFilesForFolder() {		
		List <Pair<String, String>> files = new ArrayList<>();
		if (!timeTableFolder.exists())
			timeTableFolder.mkdirs();
	    for (final File fileEntry : timeTableFolder.listFiles()) {
	        if (fileEntry.isFile() && fileEntry.getName().matches("\\w+_\\d{6}\\.txt")) { // sts_210814.txt
	        	files.add(new Pair<>(fileEntry.getName() , fileEntry.getAbsolutePath()));
	        }
	    }
	    return files;
	}	
	
	private Map<String, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> getRemoteCollection() throws FileNotFoundException, IOException {
		Map<String, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> tmp;
		//tmp.putAll(mapExternal);
		//return Collections.unmodifiableMap(tmp);
		List <Pair<String, String>> files = listFilesForFolder();
		for (Pair<String, String> fp : listFilesForFolder()) {
			final String name = fp.getFirst();
			String channel = name.substring(0, name.indexOf("_"));
			LOG.debug("DirWatchDog channel: {}", channel);
			//tmp = TvTimetableParser.parseTimeTable(new BufferedInputStream(new FileInputStream(f)));			
		}
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
