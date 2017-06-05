package ru.amberdata.dal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zhenya on 2017-06-02.
 */
public class SqlQueueProcessor {

    private static int queueSize = Integer.parseInt(System.getProperty("SQL_PROCESS_THREADS", "2"));
    private static int batchSize = Integer.valueOf(System.getProperty("DB_BATCH_SIZE", "1"));

    private static ExecutorService execServ = Executors.newFixedThreadPool(queueSize);

    public SqlQueueProcessor() {
        System.out.println("start with: " + queueSize + " processing threads");
        System.out.println("start with: " + batchSize + " sql batch size");
    }

    public void process(Connection conn, final Queue<String> queue) throws SQLException {

        for (int i = 0; i < queueSize; ++i) {

                execServ.submit(() -> {

                    Statement stmt = conn.createStatement();
                    conn.setAutoCommit(false);

                    int size = 0;

                    while (true) {
                        String sql = queue.poll();
                        try {
                            System.out.println("sql:" + sql);
                            stmt.addBatch(sql);
                            ++size;
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        if (size >= batchSize) {
                            stmt.executeBatch();
                            conn.commit();
                            size = 0;
                        }
                    }
                });
        }
    }
}
