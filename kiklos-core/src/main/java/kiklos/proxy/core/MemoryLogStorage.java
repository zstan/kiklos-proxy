package kiklos.proxy.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryLogStorage {
	
	private final List<String> memStorageList;	
	private static final int LOG_DATA_CHUNK_SIZE = 10000;
    private final BlockingQueue logData = new LinkedBlockingQueue();
    private final ExecutorService minPriorityPool;
	private static final String LOG_NAME = ".access_log";
	private static final Logger LOG = LoggerFactory.getLogger(MemoryLogStorage.class);
	
	MemoryLogStorage(final RedissonClient memStorage, final ExecutorService execPool) {
		memStorageList = memStorage.getList(LOG_NAME);
        minPriorityPool = execPool;
        if (minPriorityPool != null)
            minPriorityPool.execute(new MemUpdater());
	}
	
	public void put(final String str) {
        try {
            logData.put(str);
        } catch (InterruptedException e) {
            LOG.error("error: ", e);
        }
    }

	private void exportData() throws IOException {
		int memStorageSize = memStorageList.size();
		LOG.info("data size: {}", memStorageSize);
		Path path = FileSystems.getDefault().getPath("./logs", "access.log");
		try (BufferedWriter bw = Files.newBufferedWriter(path, Charset.forName("UTF-8"), StandardOpenOption.APPEND)) { 
			for (int i = 0; i < (memStorageSize / LOG_DATA_CHUNK_SIZE) + 1; ++i) {
				int to = Math.min(memStorageSize, (i + 1) * LOG_DATA_CHUNK_SIZE);
				List<String> tmp = memStorageList.subList(i * LOG_DATA_CHUNK_SIZE, to);
				for (String s : tmp) {
					s = URLDecoder.decode(s, "UTF-8");
					bw.write(s + "\n");
				}
				memStorageList.removeAll(tmp);
			}
		}
	}

    private class MemUpdater implements Runnable {
        @Override
        public void run() {
            List<String> tmpList = new ArrayList<>();

            while (true) {
                try {
                    tmpList.clear();

                    Object data = logData.take();

                    while (data != null){
                        tmpList.add((String)data);
                        data = logData.poll(1, TimeUnit.SECONDS);
                    }
                    if (!tmpList.isEmpty())
                        memStorageList.addAll(tmpList);

                    if (LOG.isDebugEnabled())
                        LOG.debug("MemoryLogStorage put data, size: " + tmpList.size());

                    TimeUnit.SECONDS.sleep(2);
                } catch (Exception e) {
                    LOG.error("error while put into mem: ", e);
                }
            }
        }
    }

    public static void main(String[] args) {
		final RedissonClient storage = Redisson.create();
		MemoryLogStorage ms = new MemoryLogStorage(storage, null);
		try {
			ms.exportData();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
}
