package pi.services;

import pi.entities.FinancialGoal;
import pi.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FinancialGoalService {

    private final Connection cnx;

    public FinancialGoalService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    private void validateFinancialGoal(FinancialGoal f) throws Exception {
        if (f.getSavingAccountId() <= 0) {
            throw new Exception("saving_account_id invalide");
        }

        if (f.getNom() == null || f.getNom().trim().isEmpty()) {
            throw new Exception("Le nom est obligatoire");
        }

        if (f.getNom().trim().length() < 3) {
            throw new Exception("Le nom doit contenir au moins 3 caractères");
        }

        if (f.getMontantCible() <= 0) {
            throw new Exception("Le montant cible doit être supérieur à 0");
        }

        if (f.getMontantActuel() < 0) {
            throw new Exception("Le montant actuel ne peut pas être négatif");
        }

        if (f.getMontantActuel() > f.getMontantCible()) {
            throw new Exception("Le montant actuel ne peut pas dépasser le montant cible");
        }

        if (f.getDateLimite() == null) {
            throw new Exception("La date limite est obligatoire");
        }

        Date today = new Date(System.currentTimeMillis());
        if (f.getDateLimite().before(today)) {
            throw new Exception("La date limite doit être aujourd'hui ou dans le futur");
        }

        if (f.getPriorite() < 1 || f.getPriorite() > 3) {
            throw new Exception("La priorité doit être 1, 2 ou 3");
        }
    }

    public void ajouter(FinancialGoal f) {
        String req = "INSERT INTO financial_goal(saving_account_id, nom, montant_cible, montant_actuel, date_limite, priorite) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            validateFinancialGoal(f);

            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, f.getSavingAccountId());
            ps.setString(2, f.getNom());
            ps.setDouble(3, f.getMontantCible());
            ps.setDouble(4, f.getMontantActuel());
            ps.setDate(5, f.getDateLimite());
            ps.setInt(6, f.getPriorite());

            ps.executeUpdate();
            System.out.println("Financial goal ajoutée avec succès");
        } catch (Exception e) {
            System.out.println("Erreur ajout financial goal : " + e.getMessage());
        }
    }

    public List<FinancialGoal> afficher() {
        List<FinancialGoal> list = new ArrayList<>();
        String req = "SELECT * FROM financial_goal";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);

            while (rs.next()) {
                FinancialGoal f = new FinancialGoal();
                f.setId(rs.getInt("id"));
                f.setSavingAccountId(rs.getInt("saving_account_id"));
                f.setNom(rs.getString("nom"));
                f.setMontantCible(rs.getDouble("montant_cible"));
                f.setMontantActuel(rs.getDouble("montant_actuel"));
                f.setDateLimite(rs.getDate("date_limite"));
                f.setPriorite(rs.getInt("priorite"));

                list.add(f);
            }
        } catch (SQLException e) {
            System.out.println("Erreur affichage financial goal : " + e.getMessage());
        }

        return list;
    }

    public void modifier(FinancialGoal f) {
        String req = "UPDATE financial_goal SET saving_account_id=?, nom=?, montant_cible=?, montant_actuel=?, date_limite=?, priorite=? WHERE id=?";

        try {
            if (f.getId() <= 0) {
                throw new Exception("id invalide pour modification");
            }

            validateFinancialGoal(f);

            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, f.getSavingAccountId());
            ps.setString(2, f.getNom());
            ps.setDouble(3, f.getMontantCible());
            ps.setDouble(4, f.getMontantActuel());
            ps.setDate(5, f.getDateLimite());
            ps.setInt(6, f.getPriorite());
            ps.setInt(7, f.getId());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Financial goal modifiée avec succès");
            } else {
                System.out.println("Aucune financial goal trouvée avec cet id");
            }
        } catch (Exception e) {
            System.out.println("Erreur modification financial goal : " + e.getMessage());
        }
    }

    public void supprimer(int id) {
        String req = "DELETE FROM financial_goal WHERE id=?";

        try {
            if (id <= 0) {
                throw new Exception("id invalide");
            }

            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, id);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Financial goal supprimée avec succès");
            } else {
                System.out.println("Aucune financial goal trouvée avec cet id");
            }
        } catch (Exception e) {
            System.out.println("Erreur suppression financial goal : " + e.getMessage());
        }
    }
}