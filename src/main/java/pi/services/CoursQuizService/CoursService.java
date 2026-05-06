package pi.services.CoursQuizService;

import pi.entities.Cours;
import pi.entities.User;
import pi.interfaces.ICoursService;
import pi.services.MailingService.LearningNotificationMailer;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CoursService implements ICoursService {

    private final Connection cnx;
    private final LearningNotificationMailer notificationMailer = LearningNotificationMailer.defaultInstance();

    public CoursService() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void ajouter(Cours cours) {
        try {
            boolean ok = ajouterWithResult(cours);
            if (!ok) {
                throw new IllegalStateException("Insertion cours échouée.");
            }
            System.out.println("Cours ajouté avec succès !");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du cours: " + e.getMessage(), e);
        }
    }

    public boolean ajouterWithResult(Cours cours) throws SQLException {
        if (cours == null || cours.getUser() == null) {
            throw new SQLException("Cours invalide (user manquant).");
        }
        cours.validate();

        String sql = "INSERT INTO cours (user_id, titre, contenu_texte, type_media, url_media) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, cours.getUser().getId());
            ps.setString(2, cours.getTitre());
            ps.setString(3, cours.getContenuTexte());
            ps.setString(4, cours.getTypeMedia());
            ps.setString(5, cours.getUrlMedia());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                notificationMailer.notifyCourseAddedAsync(cours);
            }
            return ok;
        }
    }

    @Override
    public void modifier(Cours cours) {
        cours.validate();
        String sql = "UPDATE cours SET user_id=?, titre=?, contenu_texte=?, type_media=?, url_media=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, cours.getUser().getId());
            ps.setString(2, cours.getTitre());
            ps.setString(3, cours.getContenuTexte());
            ps.setString(4, cours.getTypeMedia());
            ps.setString(5, cours.getUrlMedia());
            ps.setInt(6, cours.getId());

            ps.executeUpdate();
            System.out.println("Cours modifié avec succès !");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification du cours: " + e.getMessage(), e);
        }
    }

    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM cours WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Cours supprimé avec succès !");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du cours: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Cours> afficher() {
        List<Cours> list = new ArrayList<>();
        String sql = "SELECT * FROM cours";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));

                Cours c = new Cours(
                        rs.getInt("id"),
                        user,
                        rs.getString("titre"),
                        rs.getString("contenu_texte"),
                        rs.getString("type_media"),
                        rs.getString("url_media")
                );

                list.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des cours: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public Cours recupererParId(int id) {
        String sql = "SELECT * FROM cours WHERE id=?";
        Cours cours = null;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("user_id"));

                    cours = new Cours(
                            rs.getInt("id"),
                            user,
                            rs.getString("titre"),
                            rs.getString("contenu_texte"),
                            rs.getString("type_media"),
                            rs.getString("url_media")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement du cours: " + e.getMessage(), e);
        }

        return cours;
    }

    @Override
    public List<Cours> rechercher(String critere) {
        List<Cours> list = new ArrayList<>();
        String sql = "SELECT * FROM cours WHERE titre LIKE ? OR contenu_texte LIKE ? ORDER BY titre ASC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            String searchPattern = "%" + (critere == null ? "" : critere) + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("user_id"));

                    Cours c = new Cours(
                            rs.getInt("id"),
                            user,
                            rs.getString("titre"),
                            rs.getString("contenu_texte"),
                            rs.getString("type_media"),
                            rs.getString("url_media")
                    );

                    list.add(c);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recherche cours: " + e.getMessage(), e);
        }

        return list;
    }
}
