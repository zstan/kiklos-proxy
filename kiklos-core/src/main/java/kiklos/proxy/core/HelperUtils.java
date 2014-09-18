package kiklos.proxy.core;

import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelperUtils {
	
	private static final Logger LOG = LoggerFactory.getLogger(HelperUtils.class);

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
	
	static String getChannelFromParams(final Map<String, List<String>> params) {
		List<String> channel = params.remove(HttpRequestHandler.CHANNEL);
		if (channel == null || channel.isEmpty()) {
			LOG.info("no channel param found, using default");
			return HttpRequestHandler.DEFAULT_CHANNEL;
		} else {
			return channel.remove(0);
		}
	}	
}
