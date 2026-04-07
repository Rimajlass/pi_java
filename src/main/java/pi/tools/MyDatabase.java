package pi.tools;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    String url ="jdbc:mysql://localhost:3306/decides_db";
    String user="root";
    String mdp ="";
    private Connection cnx;
    static MyDatabase myDb;
    private MyDatabase(){
        try {
            cnx = DriverManager.getConnection(url, user, mdp);            System.out.println("cnx etablie !!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public static MyDatabase getInstance(){
        if(myDb ==null){
            myDb=new MyDatabase();
        }
        return myDb;
    }

    public Connection getCnx() {
        return cnx;
    }
}
//////testtt