package kiklos.tv.timetable;

import java.io.BufferedReader;
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
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import kiklos.proxy.core.HelperUtils;
import kiklos.proxy.core.PairEx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;

enum TvChannelRange {
	STS (24, 26),
	DEFAULT (30, 30);
	
	final int lower, upper;
	
	TvChannelRange(final int l, final int u) {
		lower = l * 1000 * 60;
		upper = u * 1000 * 60;
	}
	
	static TvChannelRange getRange4Channel(final short ch) {
		switch (ch) {
			case 408: return STS;
			default: return DEFAULT;
		}
	}
	
}

public class TvTimetableParser {
	
	private static final Logger LOG = LoggerFactory.getLogger(TvTimetableParser.class);
	private static final XPath xPath = XPathFactory.newInstance().newXPath();
	private static XPathExpression xpathExpr = null;
	
	static final SimpleDateFormat DATE_TV_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	static final SimpleDateFormat TIME_TV_FORMAT = new SimpleDateFormat("HH:mm:ss");
	
	private static final byte TV_ITEMS_COUNT = 6;
	private static Calendar calendar = Calendar.getInstance();  

	static long dateHMToSeconds(final Date d) {
		calendar.setTime(d);
		return calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND);
	}	
	
	public static NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> parseTimeTable(final String dateStr, final InputSource source) throws IOException {
		LOG.debug("parseTimeTable: {}", dateStr);
/*		if (path.endsWith("txt")) { // vimb
			in = new BufferedInputStream(new FileInputStream(path));
			return parseVimbTimeTable(in);
		} else if (path.endsWith("xml")) {*/
			//in = new BufferedInputStream(new FileInputStream(path));
			Date date;
			try {
				date = HelperUtils.DATE_FILE_FORMAT.parse(dateStr);
			} catch (ParseException e) {
				e.printStackTrace();
				return null;
			}		
			return parseXmlTimeTable(source, date);
		//} else
		//	return null;
	}
	
	static TreeMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> parseXmlTimeTable(final InputSource in, 
			final Date date) throws IOException {		
		
		if (date == null) {
			return null;
		}
		
		Calendar cl = Calendar.getInstance();
		cl.setTime(date);
		
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
		
		TreeMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> tOut = new TreeMap<>();
		Calendar onAirCalendar = Calendar.getInstance();
		
		try {
			if(nodeList.getLength() > 0) {
				PairEx<Long, Long> window;
				Date midDay = TIME_TV_FORMAT.parse("12:00:00");
				Date midNight = TIME_TV_FORMAT.parse("00:00:00");
				boolean afterMidDay = false;
				for(int i = 0 ; i < nodeList.getLength(); ++i) {
					Element el = (Element)nodeList.item(i);
					short duration;
					long onAir;
					duration = (short)dateHMToSeconds(TIME_TV_FORMAT.parse(el.getAttribute("Duration")));
					onAirCalendar.setTime(TIME_TV_FORMAT.parse(el.getAttribute("OnAir")));
					if (afterMidDay || onAirCalendar.after(midDay)) { 
						afterMidDay = true;
						if (onAirCalendar.after(midNight)) {
							onAirCalendar.roll(Calendar.DAY_OF_YEAR, true);
						}
					}
					onAir = HelperUtils.updateCalendar(cl, onAirCalendar).getTimeInMillis();
					window = new PairEx<>(onAir, onAir + duration * 1000);
					NodeList spots = el.getElementsByTagName("Spot");
					List<Short> durationsList = new ArrayList<>(spots.getLength());
					for (int j = 0; j < spots.getLength(); ++j) {
						Element spot = (Element)spots.item(j);
						final String dur = spot. getAttribute("Duration");
						durationsList.add((short)dateHMToSeconds(TIME_TV_FORMAT.parse(dur)));
					}
					tOut.put(window, new PairEx<>(duration, durationsList));
				} 				
			}
		} catch (ParseException e) {
			e.printStackTrace();
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
	
	// return duration and ad list
	public static PairEx<Short, List<Short>> getWindow(PairEx<Long, Long> key, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> m, final String ch) {
		PairEx <Long, Long> window = null;
		long deltha = -1, tmpKey;
		TvChannelRange range = TvChannelRange.getRange4Channel(Short.parseShort(ch));
		
		for (Map.Entry<PairEx<Long, Long>, PairEx<Short, List<Short>>> e : m.entrySet()) {				
			if (key.getKey() >= e.getKey().getKey() - range.lower && key.getKey() <= e.getKey().getValue() + range.upper) {
				
				if (deltha != -1 && key.getKey() < e.getKey().getKey()) {
					if (e.getKey().getKey() - key.getKey() - deltha <= 0) {
						window = e.getKey();
						break;
					}
				}				
				
				if (key.getKey() > e.getKey().getValue()) {
					deltha = key.getKey() - e.getKey().getValue(); 	
				}
				window = e.getKey();
			}
		}
		return window == null ? null : m.get(window);
	}
}
