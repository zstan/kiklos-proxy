package kiklos.proxy.core;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelperUtils {
	
	private static final Logger LOG = LoggerFactory.getLogger(HelperUtils.class);
	public static final SimpleDateFormat DATE_FILE_FORMAT = new SimpleDateFormat("yyMMdd");
	public static final SimpleDateFormat TIME_TV_FORMAT = new SimpleDateFormat("HH:mm:ss");

	static int getRequiredAdDuration(final String req) {
		Map<String, List<String>> params = (new QueryStringDecoder(req)).parameters();
		List<String> dur = params.get(HttpRequestHandler.DURATION); 
		if (dur != null) {
			try {
				int d = Integer.parseInt(dur.get(0)); 
				return d > HttpRequestHandler.MAX_DURATION_BLOCK ? HttpRequestHandler.MAX_DURATION_BLOCK : d;
			} catch (NumberFormatException e) {
				return -1;
			}			
		} else
			return -1;
	}
	
	static String queryParams2String(final Map<String, List<String>> params) {
		QueryStringEncoder enc = new QueryStringEncoder(""); 
		for (Map.Entry<String, List<String>> e: params.entrySet()) {
			for (String val: e.getValue()) {
				enc.addParam(e.getKey(), val);
			}													
		}

		String query = "";
		try {
			query = enc.toUri().getQuery();
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}					
		
		LOG.debug("proxy params to vast req: {}", query);
		return query;
	}
	
	static String getChannelFromParams(final Map<String, List<String>> params) {
		List<String> channel = params.remove(HttpRequestHandler.CHANNEL);
		if (channel == null || channel.isEmpty()) {
			LOG.info("no channel param found, using default");
			return HttpRequestHandler.DEFAULT_CHANNEL;
		} else {
			return channel.remove(0);
		}
	}
	
	
	public static Calendar updateCalendar(final Calendar dst, final Calendar c) {
		Calendar out = Calendar.getInstance();
		out.setTime(dst.getTime());
		out.set(Calendar.HOUR, c.get(Calendar.HOUR_OF_DAY));
		out.set(Calendar.MINUTE, c.get(Calendar.MINUTE));
		out.set(Calendar.SECOND, c.get(Calendar.SECOND));	
		return out;
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
	
	public static boolean calendarDayComparer(final Calendar now, final Calendar c) {
		return now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && 
				now.get(Calendar.MONTH) == c.get(Calendar.MONTH) && 
				now.get(Calendar.DAY_OF_MONTH) <= c.get(Calendar.DAY_OF_MONTH);		
	}
	
	public static boolean calendarDayComparer(final Calendar c) {
		final Calendar now = Calendar.getInstance();
		return calendarDayComparer(now, c);		
	}	
	
	public static void try2sleep(TimeUnit unit, long duration) {
        try {
        	unit.sleep(duration);
		} catch (InterruptedException e1) {
				e1.printStackTrace();
		}
	}
}
