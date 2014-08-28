package kiklos.tv.timetable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import static org.junit.Assert.*;
import kiklos.proxy.core.Pair;

import org.junit.Test;

public class TvTimetableParserTest {
	
	//@Test
	public void testTimeTable() throws URISyntaxException, IOException, ParseException {
		InputStream in = getClass().getResourceAsStream("sts_1.txt");
		Map<Pair<Long, Long>, Pair<Short, List<Short>>> m = TvTimetableParser.parseVimbTimeTable(in);
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
		
		final String sd = "2014.04.11 08:30:00";
		long now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();
		
		InputStream in = getClass().getResourceAsStream("sts_1.txt");
		NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>> m = TvTimetableParser.parseVimbTimeTable(in);
		
		Pair<Long, Long> p = new Pair<Long, Long>(now, 0L);
		Pair<Short, List<Short>> pp = TvTimetableParser.getWindow(p, m);
		assertTrue(pp.getFirst() == 65);
		
		in = getClass().getResourceAsStream("408_140827.xml");
		m = TvTimetableParser.parseXmlTimeTable(in, TvTimetableParser.getDateFromFileName("408_140827.xml"));
		//DATE_FILE_FORMAT
		Date d = TvTimetableParser.TIME_TV_FORMAT.parse("00:01:00");
		System.out.println(d.getTime());
		System.out.println(TvTimetableParser.dateHMToSeconds(d));
	}	
}
