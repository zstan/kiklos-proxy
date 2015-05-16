package kiklos.tv.timetable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TimeZone;
import java.util.TreeMap;

import kiklos.proxy.core.HelperUtils;
import kiklos.proxy.core.PairEx;
import static org.junit.Assert.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
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
	
	//@Test
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
		String content = IOUtils.toString(in, StandardCharsets.UTF_8); 
		m = new TreeMap<>(TvTimetableParser.parseXmlTimeTable(content, HelperUtils.DATE_FILE_FORMAT.parse("140827")));		
		
		sd = "2014.08.27 07:09:30";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();
				
		PairEx<Long, Long> p = new PairEx<>(now, 0L);
		PairEx<Short, List<Short>> p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "408");
		
		System.out.println("1 " + p3);
		assertTrue(p3.getKey() == 60);
		
		sd = "2014.08.27 07:08:37";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();

		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "408");
		
		System.out.println("2 " + p3);
		assertTrue(p3.getKey() == 75);
		
		sd = "2014.08.27 02:30:00";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();

		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "408");
		
		System.out.println("3 " + p3);
		assertTrue(p3.getKey() == 75);
		
		sd = "2014.08.27 02:28:30";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();

		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "408");
		
		assertTrue(p3.getKey() == 75);		
		//System.out.println(p3);

		sd = "2014.08.27 07:30:00";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();

		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "408");
		
		assertTrue(p3.getKey() == 75);		
		//System.out.println(p3);		
		
		sd = "2014.08.27 09:16:00";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();

		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "408");
		
		assertTrue(p3.getKey() == 150);		
		System.out.println(p3);		
		
		sd = "2014.08.27 09:15:58";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();

		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "408");
		
		assertTrue(p3.getKey() == 120);	
		
		/////////////////*********************//////////////////
		
		in = getClass().getResourceAsStream("407_140827.xml");
		content = IOUtils.toString(in, StandardCharsets.UTF_8);
		m = new TreeMap<>(TvTimetableParser.parseXmlTimeTable(content, HelperUtils.DATE_FILE_FORMAT.parse("140827")));		
		
		sd = "2014.08.27 18:50:35";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();
				
		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "407");
		
		System.out.println(p3);		
		assertTrue(p3.getKey() == 75);	
		
		sd = "2014.08.27 18:49:35";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();
				
		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "407");
		
		System.out.println(p3);		
		assertTrue(p3.getKey() == 75);	
		
		sd = "2014.08.27 18:51:35";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();
				
		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "407");
		
		System.out.println(p3);		
		assertTrue(p3.getKey() == 75);		
	}
	
	@Test
	public void testCsv() throws IOException, ParseException, URISyntaxException {
//		URL path = getClass().getResource("PERREG.csv");
//		File csvData = new File(path.getPath());
//		CSVParser parser = CSVParser.parse(csvData, StandardCharsets.UTF_8, CSVFormat.EXCEL);
//		boolean startAdBlock = false, endAdBlock = false;
//		for (CSVRecord csvRecord : parser) {
//			String adEvent = csvRecord.get(6);
//			if (adEvent.endsWith("_STF"))
//				startAdBlock = true;
//			if (adEvent.endsWith("_ENF")) {
//				endAdBlock = true;
//				startAdBlock = false;
//			}
//			if (startAdBlock)
//				System.out.println(csvRecord.get(2));
//		 }		
		InputStream in = getClass().getResourceAsStream("404_150515.csv");
		String content = IOUtils.toString(in, StandardCharsets.UTF_8);
		Map<PairEx<Long, Long>, PairEx<Short, List<Short>>> m = new TreeMap<>(TvTimetableParser.parseCsvTimeTable(content, HelperUtils.DATE_FILE_FORMAT.parse("140827")));		
		
		String sd = "2014.08.27 06:10:20";
		long now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();
		
		PairEx<Long, Long> p = new PairEx<>(now, 0L);
		PairEx<Short, List<Short>> p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "404");
		
		System.out.println(p3 + "testCsv");		
		assertTrue(p3.getKey() == 66);	
		
		sd = "2014.08.28 00:25:18";
		now = TvTimetableParser.DATE_TV_FORMAT.parse(sd).getTime();
		
		p = new PairEx<>(now, 0L);
		p3 = TvTimetableParser.getWindow(p, new TreeMap<>(m), "404");
		
		System.out.println(p3 + "testCsv");		
		assertTrue(p3.getKey() == 85);	
		
		DirWatchDog dd = new DirWatchDog();
		//File
		String path = getClass().getResource("1481_150516.csv").toString();
		Map<PairEx<String, String>, PairEx<String, String>> mOut = dd.readDataFile(new File(new URI(path)));
		for (Map.Entry<PairEx<String, String>, PairEx<String, String>> me : mOut.entrySet()) {
			System.out.println(me.getKey());
			//System.out.println(me.getValue());
		}
		
		TimeZone tz = TimeZone.getDefault();
		System.out.println(tz);
		Calendar cl = Calendar.getInstance(tz);
		System.out.println(cl.get(Calendar.HOUR_OF_DAY));
	}
}
