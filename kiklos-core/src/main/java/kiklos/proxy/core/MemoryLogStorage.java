package kiklos.proxy.core;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.redisson.Redisson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryLogStorage {
	
	private final List<String> memStorageList;	
	private static final int LOG_DATA_SIZE = 10;
	private static final int LOG_DATA_CHUNK_SIZE = 10000;
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

	private void exportData() throws IOException {
		int memStorageSize = memStorageList.size();
		LOG.info("data size: {}", memStorageSize);
		Path path = FileSystems.getDefault().getPath("./logs", "access.log");
		BufferedWriter bw = Files.newBufferedWriter(path, Charset.forName("UTF-8"), StandardOpenOption.APPEND); 
		for (int i = 0; i < (memStorageSize / LOG_DATA_CHUNK_SIZE) + 1; ++i) {
			int to = Math.min(memStorageSize, (i + 1) * LOG_DATA_CHUNK_SIZE);
			List<String> tmp = memStorageList.subList(i * LOG_DATA_CHUNK_SIZE, to);
			for (String s : tmp) {
				s = URLDecoder.decode(s, "UTF-8");
				bw.write(s + "\n");
			}
			memStorageList.removeAll(tmp);
		}
		bw.close();
	}
	
	public static void main(String[] args) {
		final Redisson storage = Redisson.create();
		MemoryLogStorage ms = new MemoryLogStorage(storage);
		try {
			ms.exportData();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
}
