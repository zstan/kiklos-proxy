package kiklos.tv.timetable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import kiklos.proxy.core.PairEx;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirWatchDog {	
	private final static File TIME_TABLE_FOLDER = new File("./timetable");
	private final static File OLD_DATA_FOLDER = new File("./timetable/old");
	private static final String TIMETABLE_MAP_NAME = ".timetable";
	private static final SimpleDateFormat TIME_TABLE_DATE = new SimpleDateFormat("yyMMdd");
    private static final Logger LOG = LoggerFactory.getLogger(DirWatchDog.class);
    private Map<PairEx<String, String>, Map<PairEx<Long, Long>, PairEx<Short, List<Short>>>> mapExternal;
    private volatile Map<PairEx<String, String>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> mapInternal;
	
	public DirWatchDog(final Redisson memStorage) {
		LOG.debug("DirWatchDog initialization start");
		if (!TIME_TABLE_FOLDER.exists())
			TIME_TABLE_FOLDER.mkdirs();
		mapExternal = memStorage.getMap(TIMETABLE_MAP_NAME);
		mapInternal = map2TreeMapCopy(mapExternal);
		Thread t1 = new Thread(new MapUpdater());
		Thread t2 = new Thread(new MapCleaner());
		t1.setPriority(Thread.MIN_PRIORITY);
		t1.start();
		t2.setPriority(Thread.MIN_PRIORITY);
		t2.start();
		LOG.debug("DirWatchDog initialization complete");
	}
	
	private static Map<PairEx<String, String>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> map2TreeMapCopy(final Map<PairEx<String, String>, Map<PairEx<Long, Long>, PairEx<Short, List<Short>>>> mIn) {
		Map<PairEx<String, String>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> mOut = new HashMap<>(mIn.size());
		for (Map.Entry<PairEx<String, String>, Map<PairEx<Long, Long>, PairEx<Short, List<Short>>>> e : mIn.entrySet()) {
			mOut.put(e.getKey(), new TreeMap<>(e.getValue()));
		}
		return mOut;
	}
	
	private boolean watchDogIt() {
		Map<PairEx<String, String>, Map<PairEx<Long, Long>, PairEx<Short, List<Short>>>> tmp = 
				new HashMap<>(TIME_TABLE_FOLDER.listFiles().length);
		final Date now = new Date();
		for (final File fileEntry : TIME_TABLE_FOLDER.listFiles()) {
			String name, path;
			if (fileEntry.isFile() && fileEntry.getName().matches("\\w+_\\d{6}\\.(txt|xml)")) { // sts_210814.txt, 408_140826.xml
				name = fileEntry.getName();
				path = fileEntry.getAbsolutePath();
			} else {
				continue;
			}
			String channel = name.substring(0, name.indexOf("_"));
			String date = name.substring(name.indexOf("_") + 1, name.indexOf("."));
			LOG.debug("DirWatchDog found timetable channel: {}, date: {}", channel, date);
			Date d;
			try {
				d = TIME_TABLE_DATE.parse(date);
				Calendar c = Calendar.getInstance();
				c.setTime(d);
				if (now.before(d) && Calendar.getInstance().get(Calendar.YEAR) == c.get(Calendar.YEAR)) {
					LOG.debug("DirWatchDog, try to parse new timetable :{}", path);
					Map<PairEx<Long, Long>, PairEx<Short, List<Short>>> m = TvTimetableParser.parseTimeTable(path);
					if (m != null) {
						tmp.put(new PairEx<>(new SimpleEntry<>(channel, date)), m);
					} else {
						LOG.debug("DirWatchDog, timetable file is incorrect");
					}
				}
				LOG.debug("DirWatchDog, move old timetable :{}", path);
				FileUtils.moveFileToDirectory(fileEntry, OLD_DATA_FOLDER, true);
			} catch (ParseException | IOException e) {
				e.printStackTrace();
			}			 
		}
		if (!tmp.isEmpty()) {
			LOG.debug("DirWatchDog, putAll");
			mapExternal.putAll(tmp);
		}
		return tmp.isEmpty() ? false : true;
	}
	
    private class MapUpdater implements Runnable {
        @Override
        public void run() {
        	while (true) {
	            LOG.debug("check timetable dir");
            	if (watchDogIt()) {
            		mapInternal = map2TreeMapCopy(mapExternal);
            	}
	            try {
					//TimeUnit.MINUTES.sleep(1);
	            	TimeUnit.SECONDS.sleep(30);
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
	            LOG.debug("try to clear timetable map");
	            Calendar c = Calendar.getInstance();
           		c.roll(Calendar.DATE, false);
           		final Date yesterday = c.getTime();
	            for (Map.Entry<PairEx<String, String>, Map<PairEx<Long, Long>, PairEx<Short, List<Short>>>> e : mapExternal.entrySet()) {
	            	final String date = e.getKey().getValue();
	            	try {
						Date d = TIME_TABLE_DATE.parse(date);						
						if (yesterday.after(d)) {
							LOG.info("DirWatchDog, replace key: {}", e.getKey());
							mapExternal.remove(e.getKey());
						}
					} catch (ParseException e1) {
						e1.printStackTrace();
					}	            
	            }
	            
	            try {
					TimeUnit.HOURS.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        }
    }
        
}
