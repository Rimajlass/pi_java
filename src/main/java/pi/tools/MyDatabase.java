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
    private RuntimeException initializationError;

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
            if (cnx == null || cnx.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de verifier l'etat de la connexion MySQL.", e);
        }

        if (cnx == null) {
            if (initializationError != null) {
                throw initializationError;
            }
            throw new IllegalStateException("La connexion MySQL n'a pas ete initialisee.");
        }

        return cnx;
    }

    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            initializationError = null;
            System.out.println("Connexion MySQL etablie.");
        } catch (ClassNotFoundException e) {
            cnx = null;
            initializationError = new IllegalStateException(
                    "Driver MySQL introuvable. Verifiez la dependance mysql-connector-java.",
                    e
            );
        } catch (SQLException e) {
            cnx = null;
            initializationError = new IllegalStateException(
                    "Connexion a decides_db impossible. Verifiez MySQL, la base, l'utilisateur root et le mot de passe.",
                    e
            );
        }
    }
}
//////testtt