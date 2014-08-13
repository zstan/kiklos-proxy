package kiklos.tv.timetable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import kiklos.proxy.core.Pair;

import org.junit.Test;

public class TvTimetableParserTest {
	
	@Test
	public void testVast3WithSeq() throws URISyntaxException, IOException, ParseException {
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
}
