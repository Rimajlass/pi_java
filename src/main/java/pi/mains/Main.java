package pi.mains;

import pi.tools.MyDatabase;
import pi.entities.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        Connection cnx = MyDatabase.getInstance().getCnx();
        List<User> users = new ArrayList<>();

        try {
            String query = "SELECT * FROM user";
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(query);

            while (rs.next()) {
                User u = new User();

                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setEmail(rs.getString("email"));
                u.setPassword(rs.getString("password"));
                u.setRoles(rs.getString("roles"));
                u.setSoldeTotal(rs.getDouble("solde_total"));

                Date d = rs.getDate("date_inscription");
                if (d != null) {
                    u.setDateInscription(d.toLocalDate());
                }

                users.add(u);
            }

            for (User u : users) {
                System.out.println(u);
            }

        } catch (SQLException e) {
            System.out.println("non " + e.getMessage());
        }
    }
}
/////test