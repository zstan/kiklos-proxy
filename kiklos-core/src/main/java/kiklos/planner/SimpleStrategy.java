package kiklos.planner;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleStrategy implements AbstractStrategy {
	private static final Logger LOG = LoggerFactory.getLogger(SimpleStrategy.class);
	
	@Override
	public List<String> getTimeTable(final DurationSettings settings, final int summaryDuration) {
		Integer[] durationsList = (Integer[]) settings.getDurationsSet().toArray();
		Arrays.sort(durationsList);
		LOG.debug("duration list size: {}, summary duration: {}", Array.getLength(durationsList), summaryDuration);
		if (summaryDuration == -1) {
			List<String> l = new ArrayList<>();
			String defaultPlacement = settings.getPlacement(durationsList[0]);
			l.add(defaultPlacement);
			l.add(defaultPlacement);
			return l;
		} else {
			return null;
		}
	}

}
