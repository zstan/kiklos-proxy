package ru.amberdata.dal.pg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import ru.amberdata.dal.DataAccess;
import ru.amberdata.dal.SqlQueueProcessor;

public class PgDataAccessImpl implements DataAccess, AutoCloseable {

    private static PgDataAccessImpl INSTANCE;
    private final static String DATABASE_NAME = "test.counter";
    private final static String DATABASE_URI = "127.0.0.1:26257";
    private final static String DATABASE_USER = "kiklos";
    private static Connection conn;
    private static ConcurrentLinkedQueue<String> batchQueue = new ConcurrentLinkedQueue<>();
    private final static String INSERT_STMT = "INSERT INTO " + DATABASE_NAME + "(%s) VALUES (%s);";
    private static SqlQueueProcessor processor = new SqlQueueProcessor();

    private PgDataAccessImpl() {
    }

    public static PgDataAccessImpl build() {
        if (INSTANCE != null)
            return INSTANCE;

        try {
            Class.forName("org.postgresql.Driver");
            final String connStr = String.format("jdbc:postgresql://%s/%s?sslmode=disable", DATABASE_URI, DATABASE_NAME);
            conn = DriverManager.getConnection(connStr, DATABASE_USER, null);

            if (conn != null) {
                System.out.println("You made it, take control your database now!");
            } else {
                System.err.println("Failed to make connection!");
                return null;
            }

            INSTANCE = new PgDataAccessImpl();
            processor.process(conn, batchQueue);

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        return INSTANCE;
    }

    public void addEntry(Map<String, String> entry) {

        String columns = "";
        String values = "";

        for (Map.Entry<String, String> e : entry.entrySet()) {
            columns += !columns.isEmpty() ? "," + e.getKey() : e.getKey();
            values += !values.isEmpty() ? ",\'" + e.getValue() + '\'' : '\'' + e.getValue() + '\'';
        }
        System.out.println(String.format(INSERT_STMT, columns, values));
        batchQueue.add(String.format(INSERT_STMT, columns, values));
    }

    @Override
    public void close() throws Exception {
        if (conn != null)
            conn.close();
    }

}