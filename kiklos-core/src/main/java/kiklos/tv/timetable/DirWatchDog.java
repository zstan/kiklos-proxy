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
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import kiklos.proxy.core.HelperUtils;
import kiklos.proxy.core.PairEx;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;

public class DirWatchDog {	
	private final static File TIME_TABLE_FOLDER = new File("./timetable");
	private final static File OLD_DATA_FOLDER = new File(TIME_TABLE_FOLDER + "/old");
	private static final String TIMETABLE_MAP_NAME = ".timetable";
	private static final SimpleDateFormat TIME_TABLE_DATE = new SimpleDateFormat("yyMMdd");
    private static final Logger LOG = LoggerFactory.getLogger(DirWatchDog.class);
    private static final String TIME_TABLE_FORMAT = "\\w+_\\d{6}\\.(txt|xml|csv|xslx)";// sts_210814.txt, 408_140826.xml, 404_140826.csv
    private AdProcessing adProcessing;
    
    //          (channel, date)         (format, content)
    private Map<PairEx<String, Date>, PairEx<String, String>> mapExternal; // channel, day, content    
	
    public DirWatchDog() {}
    
	public DirWatchDog(final Redisson memStorage, final ExecutorService execPool, final AdProcessing adProcessing) {
		if (!OLD_DATA_FOLDER.exists())
			OLD_DATA_FOLDER.mkdirs();
		this.adProcessing = adProcessing;
		mapExternal = memStorage.getMap(TIMETABLE_MAP_NAME);
		adProcessing.mapUpdater(mapExternal);
		execPool.execute(new MapUpdater());
		execPool.execute(new MapCleaner());		
	}
	
	private boolean watchDogIt() {
		boolean bResult = false;
		for (final File fileEntry : TIME_TABLE_FOLDER.listFiles()) {
			if (fileEntry.isFile()) { 
				if (fileEntry.getName().matches(TIME_TABLE_FORMAT)) {
					PairEx<PairEx<String, Date>, PairEx<String, String>> mOut = readDataFile(fileEntry);
					if (mOut != null) {						
						mapExternal.put(mOut.getKey(), mOut.getValue());
						LOG.info("add new timetable: {}, mapExternal size: {}", fileEntry, mapExternal.size());
						bResult = true;
					} else {
						LOG.warn("can`t parse file {} or file is empty", fileEntry.getName());
						continue;					
					}
				} else {
					LOG.warn("{} not satisfy file format {} rules", fileEntry.getName(), TIME_TABLE_FORMAT);
				}
			}
		}
		return bResult;
	}
	
	PairEx<PairEx<String, Date>, PairEx<String, String>> readDataFile(final File fileEntry) {
		final String name = fileEntry.getName();
		final String path = fileEntry.getAbsolutePath();
		final String channel = name.substring(0, name.indexOf("_"));
		final String date = name.substring(name.indexOf("_") + 1, name.indexOf("."));
		final String format = name.substring(name.indexOf(".") + 1, name.length());		
		
		PairEx<PairEx<String, Date>, PairEx<String, String>> result = null;		
		LOG.debug("found timetable channel: {}, date: {}", channel, date);
		Date d;
		try {
			d = TIME_TABLE_DATE.parse(date);
			Calendar fileDate = Calendar.getInstance();
			fileDate.setTime(d);
			if (HelperUtils.calendarDayComparer(fileDate)) {
				StringBuilder buff = new StringBuilder();
				
				if (format.matches("(txt|xml|csv)")) {
					InputStream in = new AutoCloseInputStream(new BufferedInputStream(new FileInputStream(path)));
					BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
					
					String line = reader.readLine();				
					
					while (line != null) {
						buff.append(line + '\n');
						line = reader.readLine();
					}				
					reader.close();
				} else {
					OPCPackage p = OPCPackage.open(path, PackageAccess.READ);
					XLSX2CSV xlsx2csv = new XLSX2CSV(p, buff, 10);
					xlsx2csv.process();
				}
				
				LOG.debug("add new data to external storage ch: {}, date: {}", channel, date);
				result = new PairEx<>(new PairEx<String, Date>(channel, fileDate.getTime()), new PairEx<String, String>(format, buff.toString()));
				
				LOG.debug("DirWatchDog, move old timetable : {}", path);
				FileUtils.moveFileToDirectory(fileEntry, OLD_DATA_FOLDER, true);					
			} else {
				LOG.debug("file is old enough: {}", path);	
			}
		} catch (ParseException | IOException | OpenXML4JException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}			 
		return result;		
	}
	
    private class MapUpdater implements Runnable {
        @Override
        public void run() {
        	while (true) {
	            LOG.debug("check timetable dir");;
            	if (watchDogIt()) {
            		adProcessing.mapUpdater(mapExternal);
            	}
            	HelperUtils.try2sleep(TimeUnit.MINUTES, 10);
        	}
        }
    }

    private class MapCleaner implements Runnable {
        @Override
        public void run() {
        	HelperUtils.try2sleep(TimeUnit.SECONDS, 10);
        	while (true) {	            
	            Calendar now = Calendar.getInstance();
	            now.roll(Calendar.DAY_OF_YEAR, false); // day before
	            
	            for (Map.Entry<PairEx<String, Date>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> e : adProcessing.getTimeTableMap().entrySet()) {
	            	final PairEx<String, Date> cannelDate = e.getKey();	            	
	            	Date timeTableDate = cannelDate.getValue();	    			
					if (now.after(timeTableDate)) {
						LOG.info("DirWatchDog MapCleaner, delete old: {}", e.getKey().toString());
						mapExternal.remove(e.getKey());						
						adProcessing.removeOldTimeTable(e.getKey());
						LOG.info("mapExternal.size: {}", mapExternal.size());
					}
	            
					HelperUtils.try2sleep(TimeUnit.MINUTES, 30);
	            }
        	}
        }
    }    
}
