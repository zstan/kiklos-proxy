package kiklos.tv.timetable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.TimeUnit;

import kiklos.proxy.core.PairEx;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import com.google.common.base.Charsets;

public class DirWatchDog {	
	private final static File TIME_TABLE_FOLDER = new File("./timetable");
	private final static File OLD_DATA_FOLDER = new File("./timetable/old");
	private static final String TIMETABLE_MAP_NAME = ".timetable";
	private static final SimpleDateFormat TIME_TABLE_DATE = new SimpleDateFormat("yyMMdd");
    private static final Logger LOG = LoggerFactory.getLogger(DirWatchDog.class);    
    private Map<String, PairEx<String, String>> mapExternal; // channel, day, content
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
	
	static Map<PairEx<String, String>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> map2TreeMapCopy(final Map<String, PairEx<String, String>> mIn) {
		Map<PairEx<String, String>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> mOut = new HashMap<>(mIn.size());
		for (Map.Entry<String, PairEx<String, String>> e : mIn.entrySet()) {
			final String ch = e.getKey();
			final String date = e.getValue().getKey(); 
			final String content = e.getValue().getValue();
			InputSource inputSource = new InputSource(new StringReader(content));
			NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> tm = null;
			try {
				tm = TvTimetableParser.parseTimeTable(date, inputSource);
			} catch (IOException e1) {
				LOG.info("DirWatchDog, timetable file is incorrect, {}", date);
				e1.printStackTrace();
			}
			
			mOut.put(new PairEx<String, String>(ch, date), tm);
		}
		return mOut;
	}
	
	public PairEx<Short, List<Short>> getAdListFromTimeTable(final String ch) {
		Calendar c = Calendar.getInstance();
		Date now = c.getTime();		
		final String currentDate = TvTimetableParser.DATE_FILE_FORMAT.format(now);
		NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> m = mapInternal.get(new PairEx<>(ch, currentDate));
		if (m == null) {
			LOG.info("timetable for {} channel for {} date not found", ch, currentDate);
			return null;
		}
		PairEx<Long, Long> p = new PairEx<>(now.getTime(), now.getTime());
		PairEx<Short, List<Short>> pp = TvTimetableParser.getWindow(p, m, ch);
		return pp;
	}
	
	private boolean watchDogIt() {
		Map<String, PairEx<String, String>> tmp = 
				new HashMap<>(TIME_TABLE_FOLDER.listFiles().length);
		final Calendar now = Calendar.getInstance();
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
				if (now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && 
						now.get(Calendar.MONTH) == c.get(Calendar.MONTH) && 
						now.get(Calendar.DAY_OF_MONTH) == c.get(Calendar.DAY_OF_MONTH)) {
					
					InputStream in = new AutoCloseInputStream(new BufferedInputStream(new FileInputStream(path)));
					BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
					
					String line = reader.readLine();
					StringBuilder buff = new StringBuilder();
					
					while (line != null) {
						buff.append(line);
						line = reader.readLine();
					}
					
					reader.close();
					
					LOG.debug("DirWatchDog, add new data to external storage ch: {}, date: {}", channel, date);
					tmp.put(channel, new PairEx<String, String>(date, buff.toString()));
					
					LOG.debug("DirWatchDog, move old timetable :{}", path);
					FileUtils.moveFileToDirectory(fileEntry, OLD_DATA_FOLDER, true);					
				}
			} catch (ParseException | IOException e) {
				e.printStackTrace();
			}			 
		}
		if (!tmp.isEmpty()) {
			LOG.debug("DirWatchDog mapExternal, putAll");
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
	            Calendar c = Calendar.getInstance();
           		c.roll(Calendar.DAY_OF_YEAR, false);           		
           		final Date yesterday = c.getTime();
           		LOG.debug("try to clear timetable map, yesterday: {}", yesterday.toString());
	            for (Map.Entry<PairEx<String, String>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> e : mapInternal.entrySet()) {
	            	final String date = e.getKey().getValue();
	            	try {
						Date d = TIME_TABLE_DATE.parse(date);						
						if (yesterday.after(d)) {
							LOG.info("DirWatchDog, replace key: {}", e.getKey());
							mapExternal.remove(e.getKey());
							// update mapinternal too !!!!
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
