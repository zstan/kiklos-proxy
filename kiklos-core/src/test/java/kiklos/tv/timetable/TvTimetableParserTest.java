package kiklos.tv.timetable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import kiklos.proxy.core.Pair;

import org.junit.Test;

import com.google.common.base.Charsets;

public class TvTimetableParserTest {
	@Test
	public void testVast3WithSeq() throws URISyntaxException, IOException {
		Map<Pair<Long, Long>, Pair<Short, List<Short>>> tt = new TreeMap<>();
		InputStream in = getClass().getResourceAsStream("sts_1.txt");
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
		String line = reader.readLine();
		
		boolean pr = false;
		Pair<Long, Long> window = null; 
		Pair<Short, List<Short>> block = null;
		
		while (line != null) {
			line = line.trim();
			if (line.isEmpty()) {
				if (pr) {
					window = null; 
					block = null;
					pr = false;
				} else {
					pr = true;
				}
			} else {
				System.out.println(line.split(";")[0]);
			}
			line = reader.readLine();
		}
	}
}
