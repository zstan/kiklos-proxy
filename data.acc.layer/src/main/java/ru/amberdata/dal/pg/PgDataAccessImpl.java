package ru.amberdata.dal.pg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import ru.amberdata.dal.DataAccess;

public class PgDataAccessImpl implements DataAccess, AutoCloseable {

    private static PgDataAccessImpl INSTANCE;
    private final static String DATABASE_NAME = "counter";
    private final static String DATABASE_URI = "127.0.0.1:26257";
    private final static String DATABASE_USER = "kiklos";
    private static int batchSize = 100;
    private static Connection conn;
    private static ConcurrentLinkedQueue<Map<String, String>> batchQueue = new ConcurrentLinkedQueue<>();
    private static AtomicInteger queueCount = new AtomicInteger();
    private final static String INSERT_STMT = "INSERT INTO " + DATABASE_NAME + "(%s) VALUES (%s);";

    private PgDataAccessImpl() {
    }

    public static PgDataAccessImpl build() throws ClassNotFoundException, SQLException {
        if (INSTANCE != null)
            return INSTANCE;

        batchSize = Integer.valueOf(System.getProperty("DB_BATCH_SIZE", "100"));

        Class.forName("org.postgresql.Driver");
        final String connStr = String.format("jdbc:postgresql://%s/%s?sslmode=disable", DATABASE_URI, DATABASE_NAME);
        conn = DriverManager.getConnection(connStr, DATABASE_USER, null);

        if (conn != null) {
            System.out.println("You made it, take control your database now!");
        } else {
            throw new SQLException("Failed to make connection!");
        }

        INSTANCE = new PgDataAccessImpl();
        return INSTANCE;
    }

    public void addEntry(Map<String, String> entry) throws SQLException {

        batchQueue.add(entry);

        int count = queueCount.incrementAndGet();

        if (count >= batchSize) {
            synchronized (this) {
                queueCount.set(0);
                Statement stmt = conn.createStatement();
                conn.setAutoCommit(false);
                Map<String, String> m = batchQueue.poll();

                while (m != null) {

                    String columns = "";
                    String values = "";

                    for (Map.Entry<String, String> e : m.entrySet()) {
                        columns += !columns.isEmpty() ? ',' + e.getKey() : e.getKey();
                        values += !values.isEmpty() ? ',' + e.getValue() : e.getValue();
                    }
                    stmt.addBatch(String.format(INSERT_STMT, columns, values));
                    m = batchQueue.poll();
                    connection.createStatement().execute("INSERT INTO accounts (id, balance) VALUES (1, 1000), (2, 250)");
                }
            }
        }

        //connection.createStatement().execute("CREATE TABLE IF NOT EXISTS accounts (id INT PRIMARY KEY, balance INT)");



        // Print out the balances.
        System.out.println("Initial balances:");
        ResultSet res = connection.createStatement().executeQuery("SELECT id, balance FROM accounts");
        while (res.next()) {
            System.out.printf("\taccount %s: %s\n", res.getInt("id"), res.getInt("balance"));
        }

        connection.close();
    }

    @Override
    public void close() throws Exception {
        if (conn != null)
            conn.close();
    }

    public static class CountEntry {

        public CountEntry() {

        }

    }

}