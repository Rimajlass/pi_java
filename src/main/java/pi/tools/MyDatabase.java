package pi.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static final String URL =
            "jdbc:mysql://localhost:3306/decides_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static MyDatabase myDb;

    private Connection cnx;

    private MyDatabase() {
        connect();
    }

    public static MyDatabase getInstance() {
        if (myDb == null) {
            myDb = new MyDatabase();
        }
        return myDb;
    }

    public Connection getCnx() {
        try {
            if (cnx == null || !cnx.isValid(2)) {
                connect();
            }
        } catch (SQLException e) {
            System.out.println("Connection check failed: " + e.getMessage());
        }
        return cnx;
    }

    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion MySQL etablie.");
        } catch (ClassNotFoundException e) {
            cnx = null;
            System.out.println("Driver MySQL introuvable: " + e.getMessage());
        } catch (SQLException e) {
            cnx = null;
            System.out.println("Connexion impossible: " + e.getMessage());
        }
    }
}
