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

import kiklos.proxy.core.HelperUtils;
import kiklos.proxy.core.PairEx;
import static org.junit.Assert.*;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.xml.sax.InputSource;

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
		
		String sd = "2014.04.11 08:30:00";
		long now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();
		
		InputStream in = getClass().getResourceAsStream("sts_1.txt");
		Map<PairEx<Long, Long>, PairEx<Short, List<Short>>> m = TvTimetableParser.parseVimbTimeTable(in);
		Map<PairEx<String, String>, Map<PairEx<Long, Long>, PairEx<Short, List<Short>>>> mm = new HashMap<>();
		PairEx<String, String> pp = new PairEx<>("1", "2");
		mm.put(pp, m);
		
/*		Map<PairEx<String, String>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> m2 = DirWatchDog.map2TreeMapCopy(mm);
		m = m2.get(pp);
		
		PairEx<Long, Long> p = new PairEx<>(now, 0L); 
		PairEx<Short, List<Short>> p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m));
		System.out.println(p3.getKey());*/
		//assertTrue(p3.getKey() == 65);
		
		// --------------------
		
		in = getClass().getResourceAsStream("408_140827.xml");
		InputSource source = new InputSource(in);
		m = new TreeMap<>(TvTimetableParser.parseXmlTimeTable(source, HelperUtils.DATE_FILE_FORMAT.parse("140827")));		
		
		sd = "2014.08.27 07:09:30";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();
				
		PairEx<Long, Long> p = new PairEx<>(now, 0L);
		PairEx<Short, List<Short>> p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "408");
		
		assertTrue(p3.getKey() == 60);
		
		System.out.println(p3);
		
		sd = "2014.08.27 07:08:37";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();

		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "408");
		
		assertTrue(p3.getKey() == 60);
		
		sd = "2014.08.27 02:30:00";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();

		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "408");
		
		assertTrue(p3.getKey() == 75);
		
		sd = "2014.08.27 02:28:30";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();

		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "408");
		
		assertTrue(p3 == null);		
		//System.out.println(p3);
		
	}
}
