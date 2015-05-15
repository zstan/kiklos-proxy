package kiklos.planner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.IntIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleStrategy implements AbstractStrategy {
	private static final Logger LOG = LoggerFactory.getLogger(SimpleStrategy.class);
	private static List<Integer> durationsList;
	
	public static List<String> formAdList(final DurationSettings settings, final int summaryDuration) {		
		if (summaryDuration < durationsList.get(0) && summaryDuration != -1)
			return Collections.emptyList();
		LOG.debug("duration list size: {}, summary duration: {}", durationsList.size(), summaryDuration);
		List<String> lOut = new ArrayList<>();
		if (summaryDuration == -1) {			
			String defaultPlacement = settings.getPlacement(durationsList.get(0));
			lOut.add(defaultPlacement);
			lOut.add(defaultPlacement);			
		} else {
			ArrayIntList ttList = getTimeTable(summaryDuration); 
			for(IntIterator iter = ttList.iterator(); iter.hasNext(); ) 
				lOut.add(settings.getPlacement(iter.next()));
		}
		return lOut;
	}
	
	// вставляем длительности по очереди 5, 10, 15 пока вставляется, когда не всатвляется - вставляем минимальные.
	private static ArrayIntList getTimeTable(final int summaryDuration) {
		ArrayIntList lOut = new ArrayIntList();
		int remain = summaryDuration;
		int pos = 0;
		while (true) {
			int item = durationsList.get(pos++);
			if (pos == durationsList.size())
				pos = 0;
			if (remain - item >= 0) {
				remain -= item;
				lOut.add(item);
			} else {
				if (remain == 0)
					break;
				else {
					int minDuration = durationsList.get(0);
					while (remain >= minDuration) {
						lOut.add(minDuration);
						remain -= minDuration;
					}
					break;
				}
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("SimpleStrategy durations: {}", lOut);
		}
		return lOut;
	}
	
/*	public static void main(String[] args) {
		SimpleStrategy.durationsList = Arrays.asList(5, 10, 15, 20);
		for (int i: getTimeTable(115)) {
			System.out.print(i + " ");
		}
	}
*/
}
