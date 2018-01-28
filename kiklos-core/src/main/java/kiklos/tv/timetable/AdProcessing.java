package kiklos.tv.timetable;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kiklos.proxy.core.HelperUtils;
import kiklos.proxy.core.PairEx;

public class AdProcessing {
	
	private static final Logger LOG = LoggerFactory.getLogger(AdProcessing.class);
	private volatile Map<PairEx<String, Date>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> mapInternal;

	public PairEx<Short, List<Short>> getAdListFromTimeTable(final String ch) {
		Date now = Calendar.getInstance().getTime(); // gmt
		long currentTime = now.getTime();
		currentTime = ch.equals("1481") ? currentTime + 25200000 : currentTime; // STUB !!!!!
		final String currentDateStr = HelperUtils.DATE_FILE_FORMAT.format(currentTime);
		final String currentTimeStr = HelperUtils.TIME_TV_FORMAT.format(currentTime);
		// currentDate and date before !!! fix it !
		for (Map.Entry<PairEx<String, Date>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> me: mapInternal.entrySet()) {
			LOG.debug("searching " + currentTimeStr + " in map " + me.getKey().toString() + " map size: " + mapInternal.size());
			PairEx<Long, Long> p = new PairEx<>(currentTime, currentTime);
			PairEx<Short, List<Short>> durationAdList = TvTimetableParser.getWindow(p, me.getValue(), ch);
			LOG.debug("looking for window: " + currentTime + " durationAdList " + durationAdList);
			if (durationAdList != null)	
				return durationAdList;
		}
		LOG.info("durations for {} channel for {} time not found", ch, currentTimeStr); // todo: все в московское время перевести !!!
		return null;
	}
	
	void mapUpdater(final Map<PairEx<String, Date>, PairEx<String, String>> mapExternal) {
		mapInternal = map2TreeMapCopy(mapExternal);
		LOG.info("mapInternal.size: {}", mapInternal.size());
	}
	
	void removeOldTimeTable(final PairEx<String, Date> key) {
		mapInternal.remove(key);
		LOG.info("mapInternal.size: {}", mapInternal.size());
	}
	
	Map<PairEx<String, Date>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> getTimeTableMap() {
		return mapInternal;
	}
	
	static Map<PairEx<String, Date>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> map2TreeMapCopy(final Map<PairEx<String, Date>, PairEx<String, String>> mIn) {
		Map<PairEx<String, Date>, NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>>> mOut = new HashMap<>(mIn.size());
		for (Map.Entry<PairEx<String, Date>, PairEx<String, String>> e : mIn.entrySet()) {			
			final String ch = e.getKey().getKey();
			final Date date = e.getKey().getValue();
			final String format = e.getValue().getKey(); 
			final String content = e.getValue().getValue();
			LOG.debug("parse 2 append {} date", date);
			NavigableMap<PairEx<Long, Long>, PairEx<Short, List<Short>>> tm = null;
			try {
				tm = TvTimetableParser.parseTimeTable(date, format, content);
			} catch (final IOException e1) {
				LOG.info("DirWatchDog, timetable file is incorrect, {}", date);
				e1.printStackTrace();
			}
			
			mOut.put(new PairEx<String, Date>(ch, date), tm);
		}
		return mOut;
	}	
}
