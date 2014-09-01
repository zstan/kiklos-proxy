package kiklos.tv.timetable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import kiklos.proxy.core.PairEx;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;

public class TvTimetableParser {
	
	private static final Logger LOG = LoggerFactory.getLogger(TvTimetableParser.class);
	private static final XPath xPath = XPathFactory.newInstance().newXPath();
	private static XPathExpression xpathExpr = null;
	
	static final SimpleDateFormat DATE_TV_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	static final SimpleDateFormat DATE_FILE_FORMAT = new SimpleDateFormat("yyMMdd");
	static final SimpleDateFormat TIME_TV_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private static final byte TV_ITEMS_COUNT = 6;
	private static Calendar calendar = Calendar.getInstance();  

	static long dateHMToSeconds(final Date d) {
		calendar.setTime(d);
		return calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND);
	}	
	
	/*
     * @param filename  the filename to query pattern: \d+_\d{6}\.xml, null returns ""
     * @return the name of the file without the path, or an empty string if none exists 
	 */
	static Calendar getDateFromFileName(final String filename) {
		if (filename.isEmpty() || filename.indexOf('_') == -1)
			return null;
		else {
			String dateStr = FilenameUtils.getBaseName(filename);
			dateStr = dateStr.substring(dateStr.indexOf('_') + 1);
			LOG.debug("dateStr: {}", dateStr);
			Date date;
			try {
				date = DATE_FILE_FORMAT.parse(dateStr);
			} catch (ParseException e) {
				e.printStackTrace();
				return null;
			}
			final Calendar c = Calendar.getInstance();
			c.setTime(date);
			return c;
		}
	}
	
	public static Map<PairEx<Long, Long>, PairEx<Short, List<Short>>> parseTimeTable(final String path) throws IOException {
		InputStream in;
		LOG.debug("parseTimeTable: {}", path);
		if (path.endsWith("txt")) { // vimb
			in = new BufferedInputStream(new FileInputStream(path));
			return parseVimbTimeTable(in);
		} else if (path.endsWith("xml")) {
			in = new BufferedInputStream(new FileInputStream(path));
			return parseXmlTimeTable(in, getDateFromFileName(path));
		} else
			return null;
	}
	
	private static Calendar updateCalendar(Calendar dst, final Calendar c) {
		dst.set(Calendar.HOUR, c.get(Calendar.HOUR_OF_DAY));
		dst.set(Calendar.MINUTE, c.get(Calendar.MINUTE));
		dst.set(Calendar.SECOND, c.get(Calendar.SECOND));
		return dst;
	}
	
	static Map<PairEx<Long, Long>, PairEx<Short, List<Short>>> parseXmlTimeTable(final InputStream in, 
			final Calendar calendar) throws IOException {
		
		if (calendar == null) {
			return null;
		}
		
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
		
		Map<PairEx<Long, Long>, PairEx<Short, List<Short>>> tOut = new HashMap<>();
		Calendar onAirCalendar = Calendar.getInstance(); 
		
		if(nodeList.getLength() > 0) {
			PairEx<Long, Long> window;
			for(int i = 0 ; i < nodeList.getLength(); ++i) {
				Element el = (Element)nodeList.item(i);
				short duration;
				long onAir;
				try {
					duration = (short)dateHMToSeconds(TIME_TV_FORMAT.parse(el.getAttribute("Duration")));
					onAirCalendar.setTime(TIME_TV_FORMAT.parse(el.getAttribute("OnAir")));
					onAir = updateCalendar(calendar, onAirCalendar).getTimeInMillis();
					window = new PairEx<>(onAir, onAir + duration * 1000);
					NodeList spots = el.getElementsByTagName("Spot");
					List<Short> durationsList = new ArrayList<>(spots.getLength());
					for (int j = 0; j < spots.getLength(); ++j) {
						Element spot = (Element)spots.item(j);
						final String dur = spot. getAttribute("Duration");
						durationsList.add((short)dateHMToSeconds(TIME_TV_FORMAT.parse(dur)));
					}
					tOut.put(window, new PairEx<>(duration, durationsList));
				} catch (ParseException e) {
					e.printStackTrace();
				}				
			}
		}		
		
		return tOut;
	}
	
	static Map<PairEx<Long, Long>, PairEx<Short, List<Short>>> parseVimbTimeTable(final InputStream in) throws IOException {
		// <start, end>, <duration, <spot1_duration, spot2_duration, spot3_duration ....>>
		Map<PairEx<Long, Long>, PairEx<Short, List<Short>>> tOut = new HashMap<>();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));				
		String line = reader.readLine();
		
		PairEx<Long, Long> window = null; 
		PairEx<Short, List<Short>> block = null;
		
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
					
					window = new PairEx<>(wStart.getTime(), wEnd.getTime());
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
					block = new PairEx<>(summary, l);
					tOut.put(window, block);
				} else {
					block.getValue().add(adBlockTime);
				}				
			}
			line = reader.readLine();
		}
		return tOut;
	}
	
	public static PairEx<Short, List<Short>> getWindow(PairEx<Long, Long> key, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> m) {
		try {
			//SortedMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> head = m.headMap(key);
			NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> head = m;
			PairEx <Long, Long> tmp = null;
			for (Map.Entry<PairEx<Long, Long>, PairEx<Short, List<Short>>> e : head.entrySet()) {
				LOG.debug("1");
				long l = key.getKey();
				LOG.debug("1");
				l = e. getKey().getClass().to getKey();
				LOG.debug("1");
				l = e.getKey().getValue();
				LOG.debug("1");
				if (key.getKey().compareTo(e.getKey().getKey()) > 0 && key.getKey().compareTo(e.getKey().getValue()) < 0) {
					tmp = e.getKey();
					break;
				}
			}
			return tmp == null ? null : head.get(tmp);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
