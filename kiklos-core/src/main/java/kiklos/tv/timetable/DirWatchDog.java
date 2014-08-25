package kiklos.tv.timetable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
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
	private static final SimpleDateFormat TIME_TABLE_DATE = new SimpleDateFormat("ddMMyy");
    private static final Logger LOG = LoggerFactory.getLogger(DirWatchDog.class);
    private Map<Pair<String, String>, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> mapExternal;
    private volatile Map<Pair<String, String>, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> mapInternal;
	
	public DirWatchDog(final Redisson memStorage) {
		if (!timeTableFolder.exists())
			timeTableFolder.mkdirs();
		mapExternal = memStorage.getMap(TIMETABLE_MAP_NAME);
		mapInternal = watchDogIt();
		Thread t1 = new Thread(new MapUpdater());
		Thread t2 = new Thread(new MapCleaner());
		t1.setPriority(Thread.MIN_PRIORITY);
		t1.start();
		t2.setPriority(Thread.MIN_PRIORITY);
		t2.start();		
	}
	
	private Map<Pair<String, String>, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> watchDogIt() {
		Map<Pair<String, String>, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> tmp = new HashMap<>(timeTableFolder.listFiles().length);
		final Date now = new Date();
		for (final File fileEntry : timeTableFolder.listFiles()) {
			String name, path;
			if (fileEntry.isFile() && fileEntry.getName().matches("\\w+_\\d{6}\\.txt")) { // sts_210814.txt
				name = fileEntry.getName();
				path = fileEntry.getAbsolutePath();
			} else {
				continue;
			}
			String channel = name.substring(0, name.indexOf("_"));
			String date = name.substring(name.indexOf("_") + 1, name.indexOf("."));
			LOG.debug("DirWatchDog channel: {}, date: {}", channel, date);
			Date d;
			try {
				d = TIME_TABLE_DATE.parse(date);
				if (now.before(d) || now.equals(d)) {
					tmp.put(new Pair<String, String>(channel, date), TvTimetableParser.parseTimeTable(new BufferedInputStream(new FileInputStream(path))));
				}				
			} catch (ParseException | IOException e) {
				e.printStackTrace();
			}			 
		}
		mapExternal.putAll(tmp);
		tmp.clear();
		tmp.putAll(mapExternal);
		return Collections.unmodifiableMap(tmp);
	}
	
    private class MapUpdater implements Runnable {
        @Override
        public void run() {
        	while (true) {
	            LOG.debug("check timetable dir");
            	mapInternal = watchDogIt();
	            try {
					TimeUnit.MINUTES.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        }
    }

    private class MapCleaner implements Runnable {
        @Override
        public void run() {
        	while (true) {
	            LOG.debug("clear timetable map");
	            final Date now = new Date();
	            for (Map.Entry<Pair<String, String>, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> e : mapExternal.entrySet()) {
	            	final String date = e.getKey().getSecond();
	            	try {
						Date d = TIME_TABLE_DATE.parse(date);
						if (now.after(d)) {
							mapExternal.remove(e.getKey());
						}
					} catch (ParseException e1) {
						e1.printStackTrace();
					}	            
	            }
	            
	            try {
					TimeUnit.HOURS.sleep(12);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        }
    }
        
}
