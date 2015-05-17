package kiklos.tv.timetable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import kiklos.proxy.core.HelperUtils;
import kiklos.proxy.core.PairEx;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class DirWatchDog {	
	private final static File TIME_TABLE_FOLDER = new File("./timetable");
	private final static File OLD_DATA_FOLDER = new File("./timetable/old");
	private static final String TIMETABLE_MAP_NAME = ".timetable";
	private static final long PAUSE_BEFORE_DELETE = 60 * 1000 * 30;
	private static final SimpleDateFormat TIME_TABLE_DATE = new SimpleDateFormat("yyMMdd");
    private static final Logger LOG = LoggerFactory.getLogger(DirWatchDog.class);
    
    //          (channel, format)         (date, content)
    private Map<PairEx<String, String>, PairEx<String, String>> mapExternal; // channel, day, content
    private volatile Map<PairEx<String, String>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> mapInternal;
    private ExecutorService pool;
	
    public DirWatchDog() {}
    
	public DirWatchDog(final Redisson memStorage, final ExecutorService execPool) {
		LOG.debug("DirWatchDog initialization start");
		if (!TIME_TABLE_FOLDER.exists())
			TIME_TABLE_FOLDER.mkdirs();
		mapExternal = memStorage.getMap(TIMETABLE_MAP_NAME);
		mapInternal = map2TreeMapCopy(mapExternal);
		pool = execPool;
		pool.execute(new MapUpdater());
		pool.execute(new MapCleaner());		
		LOG.debug("DirWatchDog initialization complete");
	}
	
	static Map<PairEx<String, String>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> map2TreeMapCopy(final Map<PairEx<String, String>, PairEx<String, String>> mIn) {
		Map<PairEx<String, String>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> mOut = new HashMap<>(mIn.size());
		for (Map.Entry<PairEx<String, String>, PairEx<String, String>> e : mIn.entrySet()) {
			final String ch = e.getKey().getKey();
			final String format = e.getKey().getValue();
			final String date = e.getValue().getKey(); 
			final String content = e.getValue().getValue();
			NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> tm = null;
			try {
				tm = TvTimetableParser.parseTimeTable(date, format, content);
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
		// currentDate and date before !!! fix it !
		final String currentDate = HelperUtils.DATE_FILE_FORMAT.format(now);
		NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> m = mapInternal.get(new PairEx<>(ch, currentDate));
		if (m == null) {
			LOG.info("timetable for {} channel for {} date not found", ch, currentDate);
			return null;
		}
		long current_time = ch.equals("1481") ? now.getTime() + 25200000 : now.getTime();
		PairEx<Long, Long> p = new PairEx<>(current_time, current_time); // STUB !!!!!
		LOG.debug("looking for window: " + current_time);
		PairEx<Short, List<Short>> pp = TvTimetableParser.getWindow(p, m, ch);
		return pp;
	}
	
	private boolean watchDogIt() {		
		for (final File fileEntry : TIME_TABLE_FOLDER.listFiles()) {
			if (fileEntry.isFile()) { 
				if (fileEntry.getName().matches("\\w+_\\d{6}\\.(txt|xml|csv)")) { // sts_210814.txt, 408_140826.xml, 404_140826.csv
					Map<PairEx<String, String>, PairEx<String, String>> mOut = readDataFile(fileEntry);
					if (!mOut.isEmpty()) {
						LOG.debug("DirWatchDog mapExternal, putAll");
						mapExternal.putAll(mOut);
					} else {
						LOG.warn(fileEntry.getName() + " not under regexp rules");
						continue;					
					}
				}
			}
		}
		return false;
	}
	
	Map<PairEx<String, String>, PairEx<String, String>> readDataFile(final File fileEntry) {
		final String name = fileEntry.getName();
		final String path = fileEntry.getAbsolutePath();
		final String channel = name.substring(0, name.indexOf("_"));
		final String date = name.substring(name.indexOf("_") + 1, name.indexOf("."));
		final String format = name.substring(name.indexOf(".") + 1, name.length());
		final Calendar now = Calendar.getInstance();
		int reserve = TIME_TABLE_FOLDER.listFiles() == null ? 0 : TIME_TABLE_FOLDER.listFiles().length;
		Map<PairEx<String, String>, PairEx<String, String>> tmp = 
				new HashMap<>(reserve);		
		LOG.debug("DirWatchDog found timetable channel: {}, date: {}", channel, date);
		Date d;
		try {
			d = TIME_TABLE_DATE.parse(date);
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			if (HelperUtils.CalendarDayComparer (now, c)) {
				
				InputStream in = new AutoCloseInputStream(new BufferedInputStream(new FileInputStream(path)));
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
				
				String line = reader.readLine();
				StringBuilder buff = new StringBuilder();
				
				while (line != null) {
					buff.append(line + '\n');
					line = reader.readLine();
				}
				
				reader.close();
				
				LOG.debug("DirWatchDog, add new data to external storage ch: {}, date: {}", channel, date);
				tmp.put(new PairEx<String, String>(channel, format), new PairEx<String, String>(date, buff.toString()));
				
				LOG.debug("DirWatchDog, move old timetable :{}", path);
				FileUtils.moveFileToDirectory(fileEntry, OLD_DATA_FOLDER, true);					
			}
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}			 
		return tmp;		
	}
	
    private class MapUpdater implements Runnable {
        @Override
        public void run() {
        	while (true) {
	            LOG.debug("check timetable dir");
            	if (watchDogIt()) {
            		mapInternal = map2TreeMapCopy(mapExternal);
            	}
            	HelperUtils.try2sleep(TimeUnit.MINUTES, 10);
        	}
        }
    }

    private class MapCleaner implements Runnable {
        @Override
        public void run() {
        	while (true) {	            
	            Calendar now = Calendar.getInstance();
	            for (Map.Entry<PairEx<String, String>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> e : mapInternal.entrySet()) {
	            	final NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> curMap = e.getValue();
	            	Map.Entry<PairEx<Long, Long>, PairEx<Short, List<Short>>> lastEntry = curMap.lastEntry();
					if (now.getTimeInMillis() > lastEntry.getKey().getValue() + PAUSE_BEFORE_DELETE) {
						LOG.info("DirWatchDog MapCleaner, delete old: {}", e.getKey().toString());
						mapExternal.remove(e.getKey());
						mapInternal.remove(e.getKey());
					}
	            
					HelperUtils.try2sleep(TimeUnit.MINUTES, 30);
	            }
        	}
        }
    }    
}
