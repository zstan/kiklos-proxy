package kiklos.tv.timetable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import kiklos.proxy.core.PairEx;
import static org.junit.Assert.*;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class TvTimetableParserTest {
	
	//@Test
	public void testTimeTable() throws URISyntaxException, IOException, ParseException {
		InputStream in = getClass().getResourceAsStream("sts_1.txt");
		Map<PairEx<Long, Long>, PairEx<Short, List<Short>>> m = TvTimetableParser.parseVimbTimeTable(in);
		for (Map.Entry<PairEx<Long, Long>, PairEx<Short, List<Short>>> e : m.entrySet()) {
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
		Map<PairEx<Long, Long>, PairEx<Short, List<Short>>> m = TvTimetableParser.parseVimbTimeTable(in);
		Map<PairEx<String, String>, Map<PairEx<Long, Long>, PairEx<Short, List<Short>>>> mm = new HashMap<>();
		PairEx<String, String> pp = new PairEx<>("1", "2");
		mm.put(pp, m);
		
		Map<PairEx<String, String>, TreeMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> m2 = DirWatchDog.map2TreeMapCopy(mm);
		m = m2.get(pp);
		
		PairEx<Long, Long> p = new PairEx<>(now, 0L); 
		PairEx<Short, List<Short>> p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m));
		System.out.println(p3.getKey());
		//assertTrue(p3.getKey() == 65);
		
		// --------------------
		
		final String sd2 = "2014.08.27 07:09:30";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd2).getTime();
		
		in = getClass().getResourceAsStream("408_140827.xml");
		m = new TreeMap<>(TvTimetableParser.parseXmlTimeTable(in, TvTimetableParser.getDateFromFileName("408_140827.xml")));
		
		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m));
		System.out.println(p3);
		
        Calendar c = Calendar.getInstance();
   		c.roll(Calendar.DATE, false);
   		
   		System.out.println(c.getTime());
   		
   		System.out.println(TvTimetableParser.DATE_FILE_FORMAT.format(c.getTime()));
   		
	}
}
