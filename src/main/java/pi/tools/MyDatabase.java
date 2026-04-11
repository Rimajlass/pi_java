package pi.tools;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    String url ="jdbc:mysql://localhost:3306/decides_db";
    String user="root";
    String mdp ="";
    private Connection cnx;
    private SQLException connectionError;
    static MyDatabase myDb;
    private MyDatabase(){
        try {
            cnx = DriverManager.getConnection(url, user, mdp);
            System.out.println("cnx etablie !!");
        } catch (SQLException e) {
            this.connectionError = e;
        }
    }
    public static MyDatabase getInstance(){
        if(myDb ==null){
            myDb=new MyDatabase();
        }
        return myDb;
    }

    public Connection getCnx() {
        if (cnx == null) {
            String message = "Unable to connect to MySQL at " + url +
                    ". Check that the MySQL server is running, the database 'decides_db' exists, and the credentials are correct.";
            throw new IllegalStateException(message, connectionError);
        }
        return cnx;
    }
}
