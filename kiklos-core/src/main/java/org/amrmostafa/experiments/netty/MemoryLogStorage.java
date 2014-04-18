package org.amrmostafa.experiments.netty;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.redisson.Redisson;

public class MemoryLogStorage {
	
	private final List<String> memStorageList;	
	private List<String> logData = new ArrayList<String>();
	private final int LOG_DATA_SIZE = 100;
	private static final String LOG_NAME = ".access_log";
	
	MemoryLogStorage(final Redisson memStorage) {
		memStorageList = memStorage.getList(LOG_NAME);
	}
	
	public synchronized void put(final String str) {
		logData.add(str);
		if (logData.size() == LOG_DATA_SIZE)
			memStorageList.addAll(logData);
		logData.clear();
	}

}
