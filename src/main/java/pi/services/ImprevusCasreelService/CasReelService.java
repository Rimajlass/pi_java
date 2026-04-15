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
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CasReelService {

    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUT_ACCEPTE = "ACCEPTE";
    public static final String STATUT_REFUSE = "REFUSE";

    private final Connection cnx;

    public CasReelService() {
        cnx = MyDatabase.getInstance().getCnx();
        creerTableSiAbsente();
        ajouterColonnesWorkflowSiNecessaire();
    }

    public void ajouter(CasRelles casReel) {
        String req = """
                INSERT INTO cas_relles
                (imprevus_id, titre, description, type, categorie, montant, solution, date_effet, justificatif_file_name, resultat, raison_refus, confirmed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

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
            ps.setString(10, emptyToDefault(casReel.getResultat(), STATUT_EN_ATTENTE));
            ps.setString(11, emptyToNull(casReel.getRaisonRefus()));
            ps.setTimestamp(12, casReel.getConfirmedAt() == null ? null : Timestamp.valueOf(casReel.getConfirmedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout cas reel : " + e.getMessage(), e);
        }
    }

    public void modifier(CasRelles casReel) {
        String req = """
                UPDATE cas_relles
                SET imprevus_id = ?, titre = ?, description = ?, type = ?, categorie = ?, montant = ?, solution = ?, date_effet = ?,
                    justificatif_file_name = ?, resultat = ?, raison_refus = ?, confirmed_at = ?
                WHERE id = ?
                """;

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
            ps.setString(10, emptyToDefault(casReel.getResultat(), STATUT_EN_ATTENTE));
            ps.setString(11, emptyToNull(casReel.getRaisonRefus()));
            ps.setTimestamp(12, casReel.getConfirmedAt() == null ? null : Timestamp.valueOf(casReel.getConfirmedAt()));
            ps.setInt(13, casReel.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur modification cas reel : " + e.getMessage(), e);
        }
    }

    public List<CasRelles> afficher() {
        return afficherAvecFiltre(null);
    }

    public List<CasRelles> afficherParStatut(String statut) {
        return afficherAvecFiltre(statut);
    }

    public void changerStatut(int id, String statut, String raisonRefus) {
        String req = "UPDATE cas_relles SET resultat = ?, raison_refus = ?, confirmed_at = ? WHERE id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, statut);
            ps.setString(2, emptyToNull(raisonRefus));
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur confirmation cas reel : " + e.getMessage(), e);
        }
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

    private List<CasRelles> afficherAvecFiltre(String statut) {
        List<CasRelles> liste = new ArrayList<>();
        String req = """
                SELECT cr.id, cr.imprevus_id, cr.titre, cr.description, cr.type, cr.categorie, cr.montant, cr.solution,
                       cr.date_effet, cr.justificatif_file_name, cr.resultat, cr.raison_refus, cr.confirmed_at,
                       i.titre AS imprevu_titre, i.type AS imprevu_type, i.budget AS imprevu_budget
                FROM cas_relles cr
                LEFT JOIN imprevus i ON cr.imprevus_id = i.id
                %s
                ORDER BY cr.id DESC
                """.formatted(statut == null ? "" : "WHERE cr.resultat = ?");

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            if (statut != null) {
                ps.setString(1, statut);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                liste.add(mapCasReel(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur affichage cas reels : " + e.getMessage(), e);
        }

        return liste;
    }

    private CasRelles mapCasReel(ResultSet rs) throws SQLException {
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

        Date dateEffet = rs.getDate("date_effet");
        Timestamp confirmedAt = rs.getTimestamp("confirmed_at");

        CasRelles cas = new CasRelles(
                rs.getInt("id"),
                imprevu,
                rs.getString("titre"),
                rs.getString("description"),
                rs.getString("type"),
                rs.getString("categorie"),
                rs.getDouble("montant"),
                rs.getString("solution"),
                dateEffet == null ? null : dateEffet.toLocalDate(),
                rs.getString("justificatif_file_name")
        );
        cas.setResultat(emptyToDefault(rs.getString("resultat"), STATUT_EN_ATTENTE));
        cas.setRaisonRefus(rs.getString("raison_refus"));
        cas.setConfirmedAt(confirmedAt == null ? null : confirmedAt.toLocalDateTime());
        return cas;
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

    private void ajouterColonnesWorkflowSiNecessaire() {
        ajouterColonneSiAbsente("ALTER TABLE cas_relles ADD COLUMN resultat VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE'");
        ajouterColonneSiAbsente("ALTER TABLE cas_relles ADD COLUMN raison_refus VARCHAR(255) NULL");
        ajouterColonneSiAbsente("ALTER TABLE cas_relles ADD COLUMN confirmed_at DATETIME NULL");
    }

    private void ajouterColonneSiAbsente(String sql) {
        try {
            Statement st = cnx.createStatement();
            st.executeUpdate(sql);
        } catch (SQLException e) {
            if (e.getMessage() == null || !e.getMessage().toLowerCase().contains("duplicate column")) {
                throw new RuntimeException("Erreur mise a jour table cas_relles : " + e.getMessage(), e);
            }
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String emptyToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
