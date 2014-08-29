package kiklos.tv.timetable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TimeZone;
import java.util.TreeMap;

import static org.junit.Assert.*;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class TvTimetableParserTest {
	
	//@Test
	public void testTimeTable() throws URISyntaxException, IOException, ParseException {
		InputStream in = getClass().getResourceAsStream("sts_1.txt");
		Map<Pair<Long, Long>, Pair<Short, List<Short>>> m = TvTimetableParser.parseVimbTimeTable(in);
		for (Map.Entry<Pair<Long, Long>, Pair<Short, List<Short>>> e : m.entrySet()) {
			System.out.println("window: " + e.getKey().getKey() + " to: " + e.getKey().getValue() + " duration summary: " + e.getValue().getKey());
			for (short dur: e.getValue().getValue()) {
				System.out.println("    duration: " + dur);
			}
			System.out.println();
		}
	}
	
	@Test
	public void testNow() throws IOException, ParseException {
		
		final String sd = "2014.04.11 08:30:00";
		long now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();
		
		InputStream in = getClass().getResourceAsStream("sts_1.txt");
		NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>> m = new TreeMap<>(TvTimetableParser.parseVimbTimeTable(in));
		
		Pair<Long, Long> p = Pair.of(now, 0L);
		Pair<Short, List<Short>> pp = TvTimetableParser.getWindow(p, m);
		assertTrue(pp.getKey() == 65);
		
		// --------------------
		
		final String sd2 = "2014.08.27 07:09:30";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd2).getTime();
		
		in = getClass().getResourceAsStream("408_140827.xml");
		m = new TreeMap<>(TvTimetableParser.parseXmlTimeTable(in, TvTimetableParser.getDateFromFileName("408_140827.xml")));
		
		p = Pair.of(now, 0L);
		pp = TvTimetableParser.getWindow(p, m);
		System.out.println(pp);
	}
}
