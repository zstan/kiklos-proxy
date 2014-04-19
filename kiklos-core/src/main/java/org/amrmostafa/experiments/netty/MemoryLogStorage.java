package org.amrmostafa.experiments.netty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryLogStorage {
	
	private final List<String> memStorageList;	
	private List<String> logData = new ArrayList<String>();
	private final int LOG_DATA_SIZE = 5;
	private static final String LOG_NAME = ".access_log";
	private static final Logger LOG = LoggerFactory.getLogger(MemoryLogStorage.class);
	
	MemoryLogStorage(final Redisson memStorage) {
		memStorageList = memStorage.getList(LOG_NAME);
	}
	
	public synchronized void put(final String str) {
		logData.add(str);
		if (logData.size() == LOG_DATA_SIZE) {
			memStorageList.addAll(logData);
			LOG.info("MemoryLogStorage put data");
			System.out.println("MemoryLogStorage put data");
			logData.clear();
		}		
	}

}
