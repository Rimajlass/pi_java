package pi.services;

import pi.entities.Imprevus;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ImprevusService {

    private final Connection cnx;

    public ImprevusService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    public void ajouter(Imprevus i) {
        String req = "INSERT INTO imprevus (titre, type, budget) VALUES (?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, i.getTitre());
            ps.setString(2, i.getType());
            ps.setDouble(3, i.getBudget());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur ajout : " + e.getMessage());
        }
    }

    public List<Imprevus> afficher() {
        List<Imprevus> liste = new ArrayList<>();
        String req = "SELECT * FROM imprevus";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);

            while (rs.next()) {
                liste.add(new Imprevus(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("type"),
                        rs.getDouble("budget")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Erreur affichage : " + e.getMessage());
        }

        return liste;
    }

    public Imprevus getById(int id) {
        String req = "SELECT * FROM imprevus WHERE id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Imprevus(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("type"),
                        rs.getDouble("budget")
                );
            }
        } catch (SQLException e) {
            System.out.println("Erreur recherche : " + e.getMessage());
        }

        return null;
    }

    public void modifier(Imprevus i) {
        String req = "UPDATE imprevus SET titre = ?, type = ?, budget = ? WHERE id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, i.getTitre());
            ps.setString(2, i.getType());
            ps.setDouble(3, i.getBudget());
            ps.setInt(4, i.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur modification : " + e.getMessage());
        }
    }

    public void supprimer(int id) {
        String req = "DELETE FROM imprevus WHERE id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur suppression : " + e.getMessage());
        }
    }
}
