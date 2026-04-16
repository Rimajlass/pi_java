package pi.services;

import pi.entities.SavingAccount;
import pi.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SavingAccountService {

    private final Connection cnx;

    public SavingAccountService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    private void validateSavingAccount(SavingAccount s) throws Exception {
        if (s.getUserId() <= 0) {
            throw new Exception("user_id invalide");
        }
        if (s.getSold() < 0) {
            throw new Exception("Le solde ne peut pas être négatif");
        }
        if (s.getDateCreation() == null) {
            throw new Exception("La date de création est obligatoire");
        }
        if (s.getTauxInteret() < 0 || s.getTauxInteret() > 100) {
            throw new Exception("Le taux d'intérêt doit être entre 0 et 100");
        }
    }

    public void ajouter(SavingAccount s) {
        String req = "INSERT INTO saving_account(user_id, sold, date_creation, taux_interet) VALUES (?, ?, ?, ?)";
        try {
            validateSavingAccount(s);
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, s.getUserId());
            ps.setDouble(2, s.getSold());
            ps.setDate(3, s.getDateCreation());
            ps.setDouble(4, s.getTauxInteret());
            ps.executeUpdate();
            System.out.println("Saving account ajoutée avec succès");
        } catch (Exception e) {
            System.out.println("Erreur ajout saving account : " + e.getMessage());
        }
    }

    public List<SavingAccount> afficher() {
        List<SavingAccount> list = new ArrayList<>();
        String req = "SELECT * FROM saving_account";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);

            while (rs.next()) {
                SavingAccount s = new SavingAccount();
                s.setId(rs.getInt("id"));
                s.setUserId(rs.getInt("user_id"));
                s.setSold(rs.getDouble("sold"));
                s.setDateCreation(rs.getDate("date_creation"));
                s.setTauxInteret(rs.getDouble("taux_interet"));
                list.add(s);
            }
        } catch (SQLException e) {
            System.out.println("Erreur affichage saving account : " + e.getMessage());
        }

        return list;
    }

    public void modifier(SavingAccount s) {
        String req = "UPDATE saving_account SET user_id=?, sold=?, date_creation=?, taux_interet=? WHERE id=?";
        try {
            if (s.getId() <= 0) {
                throw new Exception("id invalide pour modification");
            }
            validateSavingAccount(s);

            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, s.getUserId());
            ps.setDouble(2, s.getSold());
            ps.setDate(3, s.getDateCreation());
            ps.setDouble(4, s.getTauxInteret());
            ps.setInt(5, s.getId());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Saving account modifiée avec succès");
            } else {
                System.out.println("Aucune saving account trouvée avec cet id");
            }
        } catch (Exception e) {
            System.out.println("Erreur modification saving account : " + e.getMessage());
        }
    }

    public void supprimer(int id) {
        String req = "DELETE FROM saving_account WHERE id=?";
        try {
            if (id <= 0) {
                throw new Exception("id invalide");
            }

            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, id);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Saving account supprimée avec succès");
            } else {
                System.out.println("Aucune saving account trouvée avec cet id");
            }
        } catch (Exception e) {
            System.out.println("Erreur suppression saving account : " + e.getMessage());
        }
    }
}