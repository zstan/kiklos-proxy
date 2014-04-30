package kiklos.proxy.core;

import java.util.ArrayList;
import java.util.List;

import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryLogStorage {
	
	private final List<String> memStorageList;	
	private static final int LOG_DATA_SIZE = 10;	
	private List<String> logData = new ArrayList<String>(LOG_DATA_SIZE);
	private static final String LOG_NAME = ".access_log";
	private static final Logger LOG = LoggerFactory.getLogger(MemoryLogStorage.class);
	
	MemoryLogStorage(final Redisson memStorage) {
		memStorageList = memStorage.getList(LOG_NAME);
	}
	
	public synchronized void put(final String str) {
		logData.add(str);
		if (logData.size() == LOG_DATA_SIZE) {
			memStorageList.addAll(logData);
			LOG.debug("MemoryLogStorage put data");
			logData.clear();
		}		
	}

}
