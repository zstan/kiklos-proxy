package kiklos.tv.timetable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import kiklos.proxy.core.Pair;

public class TvTimetableParser {
	
	private static final Logger LOG = LoggerFactory.getLogger(TvTimetableParser.class);
	
	private static final SimpleDateFormat DATE_TV_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	private static final SimpleDateFormat TIME_TV_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private static final byte TV_ITEMS_COUNT = 6;

	private static long dateHMToSeconds(final Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.HOUR_OF_DAY) * 3600 + c.get(Calendar.MINUTE) * 60 + c.get(Calendar.SECOND);
	}	
	
	public static Map<Pair<Long, Long>, Pair<Short, List<Short>>> parseTimeTable(final InputStream in) throws IOException {
		Map<Pair<Long, Long>, Pair<Short, List<Short>>> tOut = new TreeMap<>();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));				
		String line = reader.readLine();
		
		Pair<Long, Long> window = null; 
		Pair<Short, List<Short>> block = null;
		
		while (line != null) {
			line = line.trim();
			if (!line.isEmpty()) {
				String[] items = line.split(";");
				if (items.length < TV_ITEMS_COUNT) {
					LOG.error("malformed items.length: {}", line);
					continue;
				}
				Date wStart, wEnd;
				short summary = 0, adBlockTime;
				try {
					wStart = DATE_TV_FORMAT.parse(items[0]);
					wEnd = DATE_TV_FORMAT.parse(items[1]);
					
					window = new Pair<>(wStart.getTime(), wEnd.getTime());
					block = tOut.get(window);
					
					if (block == null) {
						summary = (short)dateHMToSeconds(TIME_TV_FORMAT.parse(items[4]));
					}
					adBlockTime = (short)dateHMToSeconds(TIME_TV_FORMAT.parse(items[3]));					
				} catch (ParseException e) {
					LOG.error("malformed items parse: {}", line);
					continue;
				}								
								
				if (block == null) {
					List<Short> l = new ArrayList<>();
					l.add(adBlockTime);
					block = new Pair<>(summary, l);
					tOut.put(window, block);
				} else {
					block.getSecond().add(adBlockTime);
				}				
			}
			line = reader.readLine();
		}
		return tOut;
	}
}
