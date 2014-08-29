package kiklos.tv.timetable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirWatchDog {	
	private final File timeTableFolder = new File("./timetable");
	private static final String TIMETABLE_MAP_NAME = ".timetable";
	private static final SimpleDateFormat TIME_TABLE_DATE = new SimpleDateFormat("yyMMdd");
    private static final Logger LOG = LoggerFactory.getLogger(DirWatchDog.class);
    private Map<Pair<String, String>, Map<Pair<Long, Long>, Pair<Short, List<Short>>>> mapExternal;
    private volatile Map<Pair<String, String>, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> mapInternal;
	
	public DirWatchDog(final Redisson memStorage) {
		LOG.debug("DirWatchDog initialization start");
		if (!timeTableFolder.exists())
			timeTableFolder.mkdirs();
		mapExternal = memStorage.getMap(TIMETABLE_MAP_NAME);
		//mapInternal = map2TreeMapCopy(mapExternal);
		Thread t1 = new Thread(new MapUpdater());
		Thread t2 = new Thread(new MapCleaner());
		t1.setPriority(Thread.MIN_PRIORITY);
		t1.start();
		t2.setPriority(Thread.MIN_PRIORITY);
		t2.start();
		LOG.debug("DirWatchDog initialization complete");
	}
	
	private static Map<Pair<String, String>, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> map2TreeMapCopy(final Map<Pair<String, String>, Map<Pair<Long, Long>, Pair<Short, List<Short>>>> mIn) {
		Map<Pair<String, String>, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>>> mOut = new HashMap<>(mIn.size());
		for (Map.Entry<Pair<String, String>, Map<Pair<Long, Long>, Pair<Short, List<Short>>>> e : mIn.entrySet()) {
			mOut.put(e.getKey(), new TreeMap<>(e.getValue()));
		}
		return mOut;
	}
	
	private boolean watchDogIt() {
		Map<Pair<String, String>, Map<Pair<Long, Long>, Pair<Short, List<Short>>>> tmp = 
				new HashMap<>(timeTableFolder.listFiles().length);
		final Date now = new Date();
		for (final File fileEntry : timeTableFolder.listFiles()) {
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
				if (now.before(d) || now.equals(d) || Calendar.getInstance().get(Calendar.YEAR) == c.get(Calendar.YEAR)) {
					tmp.put(Pair.of(channel, date), TvTimetableParser.parseTimeTable(path));
					LOG.debug("put new timetable :{}", path);
					//fileEntry.delete(); !!!!!!!!!!!!!!!!!!!!!					
				} else {
					LOG.debug("DirWatchDog, delete old timetable :{}", path);
					fileEntry.delete();
				}
			} catch (ParseException | IOException e) {
				e.printStackTrace();
			}			 
		}
		mapExternal.putAll(tmp);
		return tmp.isEmpty() ? false : true;
	}
	
    private class MapUpdater implements Runnable {
        @Override
        public void run() {
        	while (true) {
	            LOG.debug("check timetable dir");
            	if (watchDogIt()) {
            		//mapInternal = map2TreeMapCopy(mapExternal);
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
           		c.roll(Calendar.DATE, true);
           		final Date yesterday = c.getTime();
	            for (Map.Entry<Pair<String, String>, Map<Pair<Long, Long>, Pair<Short, List<Short>>>> e : mapExternal.entrySet()) {
	            	final String date = e.getKey().getValue();
	            	try {
						Date d = TIME_TABLE_DATE.parse(date);						
						if (yesterday.after(d)) {
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
