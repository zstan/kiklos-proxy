package kiklos.tv.timetable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import kiklos.proxy.core.Pair;

import org.junit.Test;

public class TvTimetableParserTest {
	
	//@Test
	public void testTimeTable() throws URISyntaxException, IOException, ParseException {
		InputStream in = getClass().getResourceAsStream("sts_1.txt");
		Map<Pair<Long, Long>, Pair<Short, List<Short>>> m = TvTimetableParser.parseTimeTable(in);
		for (Map.Entry<Pair<Long, Long>, Pair<Short, List<Short>>> e : m.entrySet()) {
			System.out.println("window: " + e.getKey().getFirst() + " to: " + e.getKey().getSecond() + " duration summary: " + e.getValue().getFirst());
			for (short dur: e.getValue().getSecond()) {
				System.out.println("    duration: " + dur);
			}
			System.out.println();
		}
	}
	
	@Test
	public void testNow() throws IOException, ParseException {
		Date d = new Date();
		long now = d.getTime();
		
		SimpleDateFormat DATE_TV_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		//DATE_TV_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+4"));
		
		Date dd = DATE_TV_FORMAT.parse("2014.08.14 21:07:53");
		//System.out.println(dd.getTime());
		System.out.println("now: " + now);
		
		InputStream in = getClass().getResourceAsStream("sts_1.txt");
		TreeMap<Pair<Long, Long>, Pair<Short, List<Short>>> m = TvTimetableParser.parseTimeTable(in);
		
		Pair<Long, Long> p = m.floorKey(new Pair(now, 0));
		System.out.println(p);
		
		Map.Entry<Pair<Long, Long>, Pair<Short, List<Short>>> e = m.floorEntry(new Pair(now, 0));
		System.out.println(e.getValue().getSecond());		
	}	
}
