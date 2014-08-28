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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;

import kiklos.proxy.core.Pair;

public class TvTimetableParser {
	
	private static final Logger LOG = LoggerFactory.getLogger(TvTimetableParser.class);
	private static final XPath xPath = XPathFactory.newInstance().newXPath();
	private static XPathExpression xpathExpr = null;
	
	static final SimpleDateFormat DATE_TV_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	static final SimpleDateFormat DATE_FILE_FORMAT = new SimpleDateFormat("yyMMdd");
	static final SimpleDateFormat TIME_TV_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private static final byte TV_ITEMS_COUNT = 6;

	static long dateHMToSeconds(final Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.HOUR_OF_DAY) * 3600 + c.get(Calendar.MINUTE) * 60 + c.get(Calendar.SECOND);
	}	
	
	/*
     * @param filename  the filename to query pattern: \d+_\d{6}\.xml, null returns ""
     * @return the name of the file without the path, or an empty string if none exists 
	 */
	static Date getDateFromFileName(final String filename) {
		if (filename.isEmpty() || filename.indexOf('_') == -1)
			return null;
		else {
			final String dateStr = FilenameUtils.getBaseName(filename).substring(filename.indexOf('_') + 1);
			Date d;
			try {
				d = DATE_FILE_FORMAT.parse(dateStr);
			} catch (ParseException e) {
				e.printStackTrace();
				return null;
			}
			return d;
		}
	}
	
	public static NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>> parseTimeTable(final String path) throws IOException {
		InputStream in;
		if (path.endsWith("txt")) { // vimb
			in = new BufferedInputStream(new FileInputStream(path));
			return parseVimbTimeTable(in);
		} else {
			in = new BufferedInputStream(new FileInputStream(path));
			return parseXmlTimeTable(in, getDateFromFileName(path));
		}
	}
	
	static NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>> parseXmlTimeTable(final InputStream in, final Date date) throws IOException {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document dom = null;
		
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse(in);
		} catch(ParserConfigurationException | SAXException pce) {
			pce.printStackTrace();
			return null;
		}
		
		Element elem = dom.getDocumentElement();
		elem.normalize();
		
		NodeList nodeList = null;
		
		try {
			if (xpathExpr == null) {
				xpathExpr = xPath.compile("/Schedule/Program/Break");
			}
			nodeList = (NodeList) xpathExpr.evaluate(dom, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return null;
		}
		
		TreeMap<Pair<Long, Long>, Pair<Short, List<Short>>> tOut = new TreeMap<>();
		
		if(nodeList.getLength() > 0) {
			Pair<Long, Long> window;
			for(int i = 0 ; i < nodeList.getLength(); ++i) {
				Element el = (Element)nodeList.item(i);
				//System.out.println(el.getAttribute("OnAir") + " " + el.getAttribute("Duration"));
				short duration;
				long onAir;
				try {
					duration = (short)dateHMToSeconds(TIME_TV_FORMAT.parse(el.getAttribute("Duration")));
					onAir = TIME_TV_FORMAT.parse(el.getAttribute("OnAir")).getTime();
					window = new Pair<>(onAir + date.getTime(), 0L);
					NodeList spots = el.getChildNodes();
					List<Short> durationsList = new ArrayList<>(spots.getLength());
					for (int j = 0; j < spots.getLength(); ++j) {
						Element spot = (Element)nodeList.item(i);
						final String dur = spot.getAttribute("Duration");
						durationsList.add((short)dateHMToSeconds(TIME_TV_FORMAT.parse(dur)));
					}
					tOut.put(window, new Pair<>(duration, durationsList));
				} catch (ParseException e) {
					e.printStackTrace();
				}				
				//Employee e = getEmployee(el);
				//myEmpls.add(e);
			}
		}		
		
		return null;
	}
	
	static NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>> parseVimbTimeTable(final InputStream in) throws IOException {
		// <start, end>, <duration, <spot1_duration, spot2_duration, spot3_duration ....>>
		TreeMap<Pair<Long, Long>, Pair<Short, List<Short>>> tOut = new TreeMap<>();
		
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
	
	public static Pair<Short, List<Short>> getWindow(Pair<Long, Long> key, NavigableMap<Pair<Long, Long>, Pair<Short, List<Short>>> m) {		
		SortedMap<Pair<Long, Long>, Pair<Short, List<Short>>> head = m.headMap(key);
		Pair <Long, Long> tmp = null;
		for (Map.Entry<Pair<Long, Long>, Pair<Short, List<Short>>> e : head.entrySet()) {
			if (key.getFirst() > e.getKey().getFirst() && key.getFirst() < e.getKey().getSecond()) {
				tmp = e.getKey();
				break;
			}
		}
		return tmp == null ? null : head.get(tmp);
	}
}
