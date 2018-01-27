package kiklos.tv.timetable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
	DEFAULT (5, 5);
	
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
	
	private static final byte TV_ITEMS_COUNT = 6;
	private static Calendar calendar = Calendar.getInstance();  

	static long dateHMToSeconds(final Date d) {
		calendar.setTime(d);
		return calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND);
	}	
	
	public static NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> parseTimeTable(final Date date, final String format, final String content) throws IOException 
	{
		if (format.equals("txt")) { // vimb
			//in = new BufferedInputStream(new FileInputStream(path));
			//return parseVimbTimeTable(in);
		} else if (format.equals("xml")) { // vi			
			return parseXmlTimeTable(content, date);
		} else if (format.equals("csv") || format.equals("xlsx")) { // excel
			try {
				return parseCsvTimeTable(content, date);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		LOG.error("empty map");
		return null;
	}
	
	static TreeMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> parseCsvTimeTable(final String content, 
			final Date date) throws ParseException, IOException, ArrayIndexOutOfBoundsException {
		if (date == null) {
			return null;
		}
		
		Calendar cl = Calendar.getInstance();
		cl.setTime(date);
		
		CSVParser parser = CSVParser.parse(content, CSVFormat.EXCEL);
		
		TreeMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> tOut = new TreeMap<>();
		Calendar onAirCalendar = Calendar.getInstance();
		
		
		Calendar midDay = Calendar.getInstance();
		midDay.setTime(date);
		midDay.set(Calendar.HOUR, 12);
		midDay.set(Calendar.MINUTE, 0);
		midDay.set(Calendar.SECOND, 0);
			
		boolean afterMidDay = false;
		boolean startAdBlock = false, endAdBlock = false;
		short duration = 0;
		long onAir = 0;
		PairEx<Long, Long> window;
		List<Short> durationsList = new ArrayList<>();
		
		for (CSVRecord csvRecord : parser) {
			if (csvRecord.size() < 6)
				continue;
			String adEvent = csvRecord.get(6);
			if (adEvent.endsWith("_STR")) {
				startAdBlock = true;
				durationsList = new ArrayList<>();
				duration = (short)dateHMToSeconds(HelperUtils.TIME_TV_FORMAT.parse(csvRecord.get(3)));				
				durationsList.add((short)dateHMToSeconds(HelperUtils.TIME_TV_FORMAT.parse(csvRecord.get(3))));				
				
				onAirCalendar.setTime(HelperUtils.TIME_TV_FORMAT.parse(csvRecord.get(1)));
				onAirCalendar = HelperUtils.updateCalendar(cl, onAirCalendar);
				if (afterMidDay || onAirCalendar.after(midDay)) { 
					afterMidDay = true;
					if (onAirCalendar.before(midDay)) {
						onAirCalendar.roll(Calendar.DAY_OF_YEAR, true);
						cl.roll(Calendar.DAY_OF_YEAR, true);
					}
				}
				onAir = HelperUtils.updateCalendar(cl, onAirCalendar).getTimeInMillis();
				
				continue;
			}
			if (adEvent.endsWith("_ENR")) {
				endAdBlock = true;
				startAdBlock = false;
				window = new PairEx<>(onAir, onAir + duration * 1000);
				tOut.put(window, new PairEx<>(duration, durationsList));
				
				continue;
			}
			if (startAdBlock) {				
				duration += (short)dateHMToSeconds(HelperUtils.TIME_TV_FORMAT.parse(csvRecord.get(3)));
				durationsList.add((short)dateHMToSeconds(HelperUtils.TIME_TV_FORMAT.parse(csvRecord.get(3))));
			}			
		 }	
		return tOut;
	}
	
	static TreeMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> parseXmlTimeTable(final String content, 
			final Date date) throws IOException {		
		
		if (date == null) {
			return null;
		}
		
		InputSource inputSource = new InputSource(new StringReader(content));
		
		Calendar cl = Calendar.getInstance();
		cl.setTime(date);
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document dom = null;
		
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse(inputSource);
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
				Date midDay = HelperUtils.TIME_TV_FORMAT.parse("12:00:00");
				Date midNight = HelperUtils.TIME_TV_FORMAT.parse("00:00:00");
				boolean afterMidDay = false;
				for(int i = 0 ; i < nodeList.getLength(); ++i) {
					Element el = (Element)nodeList.item(i);
					short duration;
					long onAir;
					duration = (short)dateHMToSeconds(HelperUtils.TIME_TV_FORMAT.parse(el.getAttribute("Duration")));
					onAirCalendar.setTime(HelperUtils.TIME_TV_FORMAT.parse(el.getAttribute("OnAir")));
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
						durationsList.add((short)dateHMToSeconds(HelperUtils.TIME_TV_FORMAT.parse(dur)));
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
						summary = (short)dateHMToSeconds(HelperUtils.TIME_TV_FORMAT.parse(items[4]));
					}
					adBlockTime = (short)dateHMToSeconds(HelperUtils.TIME_TV_FORMAT.parse(items[3]));					
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
	
    // return duration and ad list                                                // onAir, onAir + duration * 1000, duration, durationsList      // channel
	public static PairEx<Short, List<Short>> getWindow(PairEx<Long, Long> moment, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> m, final String ch) {
		PairEx <Long, Long> window = null, prevValue = null;
		long deltha = -1;
		TvChannelRange range = TvChannelRange.getRange4Channel(Short.parseShort(ch));
		
		final long current = moment.getKey();
		LOG.debug("searching1: " + current);
		LOG.debug("map size: " + m.size());
		
		for (Map.Entry<PairEx<Long, Long>, PairEx<Short, List<Short>>> e : m.entrySet()) {
			final long lower = e.getKey().getKey();
			final long upper = e.getKey().getValue();
			//LOG.debug("lower: " + lower);
			
			if (current >= lower - range.lower && current <= upper + range.upper) {
				
				if (current >= lower && current <= upper) {
					window = e.getKey();
					break;
				}
				
				if (deltha != -1 && current < lower) {
					if (lower - current - deltha <= 0) {
						window = e.getKey();						
					} else {
						window = prevValue;
					}
					break;
				}				
				
				if (current > upper) {
					deltha = current - upper;
					prevValue = e.getKey(); 
				}
				window = e.getKey();
				LOG.warn("unreachable code, getWindow");
			}
		}
		return window == null ? null : m.get(window);
	}
}
