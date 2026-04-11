package pi.services.ImprevusCasreelService;

import pi.entities.CasRelles;
import pi.entities.Imprevus;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CasReelService {

    private final Connection cnx;

    public CasReelService() {
        cnx = MyDatabase.getInstance().getCnx();
        creerTableSiAbsente();
    }

    public void ajouter(CasRelles casReel) {
        String req = "INSERT INTO cas_relles (imprevus_id, titre, description, type, categorie, montant, solution, date_effet, justificatif_file_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            setImprevu(ps, 1, casReel.getImprevus());
            ps.setString(2, casReel.getTitre());
            ps.setString(3, casReel.getDescription());
            ps.setString(4, casReel.getType());
            ps.setString(5, casReel.getCategorie());
            ps.setDouble(6, casReel.getMontant());
            ps.setString(7, casReel.getSolution());
            ps.setDate(8, casReel.getDateEffet() == null ? null : Date.valueOf(casReel.getDateEffet()));
            ps.setString(9, emptyToNull(casReel.getJustificatifFileName()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout cas reel : " + e.getMessage(), e);
        }
    }

    public void modifier(CasRelles casReel) {
        String req = "UPDATE cas_relles SET imprevus_id = ?, titre = ?, description = ?, type = ?, categorie = ?, montant = ?, solution = ?, date_effet = ?, justificatif_file_name = ? WHERE id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            setImprevu(ps, 1, casReel.getImprevus());
            ps.setString(2, casReel.getTitre());
            ps.setString(3, casReel.getDescription());
            ps.setString(4, casReel.getType());
            ps.setString(5, casReel.getCategorie());
            ps.setDouble(6, casReel.getMontant());
            ps.setString(7, casReel.getSolution());
            ps.setDate(8, casReel.getDateEffet() == null ? null : Date.valueOf(casReel.getDateEffet()));
            ps.setString(9, emptyToNull(casReel.getJustificatifFileName()));
            ps.setInt(10, casReel.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur modification cas reel : " + e.getMessage(), e);
        }
    }

    public List<CasRelles> afficher() {
        List<CasRelles> liste = new ArrayList<>();
        String req = "SELECT cr.id, cr.imprevus_id, cr.titre, cr.description, cr.type, cr.categorie, cr.montant, cr.solution, cr.date_effet, cr.justificatif_file_name, i.titre AS imprevu_titre, i.type AS imprevu_type, i.budget AS imprevu_budget FROM cas_relles cr LEFT JOIN imprevus i ON cr.imprevus_id = i.id ORDER BY cr.id DESC";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                Imprevus imprevu = null;
                int imprevuId = rs.getInt("imprevus_id");
                if (!rs.wasNull()) {
                    imprevu = new Imprevus(
                            imprevuId,
                            rs.getString("imprevu_titre"),
                            rs.getString("imprevu_type"),
                            rs.getDouble("imprevu_budget")
                    );
                }

                Date date = rs.getDate("date_effet");
                liste.add(new CasRelles(
                        rs.getInt("id"),
                        imprevu,
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getString("type"),
                        rs.getString("categorie"),
                        rs.getDouble("montant"),
                        rs.getString("solution"),
                        date == null ? null : date.toLocalDate(),
                        rs.getString("justificatif_file_name")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur affichage cas reels : " + e.getMessage(), e);
        }

        return liste;
    }

    public void supprimer(int id) {
        String req = "DELETE FROM cas_relles WHERE id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression cas reel : " + e.getMessage(), e);
        }
    }

    private void setImprevu(PreparedStatement ps, int index, Imprevus imprevu) throws SQLException {
        if (imprevu == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, imprevu.getId());
        }
    }

    private void creerTableSiAbsente() {
        String req = """
                CREATE TABLE IF NOT EXISTS cas_relles (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    imprevus_id INT NULL,
                    titre VARCHAR(255) NOT NULL,
                    description TEXT NULL,
                    type VARCHAR(50) NOT NULL,
                    categorie VARCHAR(100) NULL,
                    montant DOUBLE NOT NULL,
                    solution VARCHAR(100) NULL,
                    date_effet DATE NOT NULL,
                    justificatif_file_name VARCHAR(255) NULL,
                    CONSTRAINT fk_cas_relles_imprevus
                        FOREIGN KEY (imprevus_id) REFERENCES imprevus(id)
                        ON DELETE SET NULL
                )
                """;

        try {
            Statement st = cnx.createStatement();
            st.executeUpdate(req);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur creation table cas_relles : " + e.getMessage(), e);
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
