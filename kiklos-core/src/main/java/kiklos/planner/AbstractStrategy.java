package kiklos.planner;

import java.util.List;

public interface AbstractStrategy {
	
	public List<String> getTimeTable(final DurationSettings settings, final int summaryDuration);
}
