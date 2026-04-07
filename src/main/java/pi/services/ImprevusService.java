package pi.services;

import pi.entities.Imprevus;
import pi.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ImprevusService {

    private Connection cnx;

    public ImprevusService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    public void ajouter(Imprevus i) {
        String req = "INSERT INTO imprevus (titre, type, budget, message_educatif) VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, i.getTitre());
            ps.setString(2, i.getType());
            ps.setDouble(3, i.getBudget());
            ps.setString(4, i.getMessage_educatif());

            ps.executeUpdate();
            System.out.println("Imprévu ajouté avec succès !");
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
                Imprevus i = new Imprevus(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("type"),
                        rs.getDouble("budget"),
                        rs.getString("message_educatif")
                );
                liste.add(i);
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
                        rs.getDouble("budget"),
                        rs.getString("message_educatif")
                );
            }
        } catch (SQLException e) {
            System.out.println("Erreur recherche : " + e.getMessage());
        }

        return null;
    }

    public void modifier(Imprevus i) {
        String req = "UPDATE imprevus SET titre = ?, type = ?, budget = ?, message_educatif = ? WHERE id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, i.getTitre());
            ps.setString(2, i.getType());
            ps.setDouble(3, i.getBudget());
            ps.setString(4, i.getMessage_educatif());
            ps.setInt(5, i.getId());

            System.out.println("ID envoyé = " + i.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Imprévu modifié avec succès !");
            } else {
                System.out.println("Aucun imprévu trouvé avec cet id.");
            }
        } catch (SQLException e) {
            System.out.println("Erreur modification : " + e.getMessage());
        }
    }

    public void supprimer(int id) {
        String req = "DELETE FROM imprevus WHERE id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Imprévu supprimé avec succès !");
            } else {
                System.out.println("Aucun imprévu trouvé avec cet id.");
            }
        } catch (SQLException e) {
            System.out.println("Erreur suppression : " + e.getMessage());
        }
    }
}