package ru.amberdata.dal.pg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Main {

    public static void main(String[] argv) throws SQLException {

        System.out.println("-------- PostgreSQL "
                + "JDBC Connection Testing ------------");

        try {

            Class.forName("org.postgresql.Driver");

        } catch (ClassNotFoundException e) {

            System.out.println("Where is your PostgreSQL JDBC Driver? "
                    + "Include in your library path!");
            e.printStackTrace();
            return;

        }

        System.out.println("PostgreSQL JDBC Driver Registered!");

        Connection connection = null;

        try {

            connection = DriverManager.getConnection(
                    "jdbc:postgresql://127.0.0.1:26257/test?sslmode=disable", "kiklos",
                    null);

        } catch (SQLException e) {

            System.out.println("Connection Failed! Check output console");
            e.printStackTrace();
            return;

        }

        if (connection != null) {
            System.out.println("You made it, take control your database now!");
        } else {
            System.out.println("Failed to make connection!");
        }

        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS accounts (id INT PRIMARY KEY, balance INT)");

        connection.createStatement().execute("INSERT INTO accounts (id, balance) VALUES (1, 1000), (2, 250)");

        // Print out the balances.
        System.out.println("Initial balances:");
        ResultSet res = connection.createStatement().executeQuery("SELECT id, balance FROM accounts");
        while (res.next()) {
            System.out.printf("\taccount %s: %s\n", res.getInt("id"), res.getInt("balance"));
        }

        connection.close();
    }

}