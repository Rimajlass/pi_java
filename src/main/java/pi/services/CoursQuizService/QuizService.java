package pi.services.CoursQuizService;

import pi.entities.Cours;
import pi.entities.Quiz;
import pi.entities.User;
import pi.interfaces.IQuizService;
import pi.services.MailingService.LearningNotificationMailer;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class QuizService implements IQuizService {

    private final Connection cnx;
    private final LearningNotificationMailer notificationMailer = LearningNotificationMailer.defaultInstance();

    public QuizService() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void ajouter(Quiz quiz) {
        try {
            boolean ok = ajouterWithResult(quiz);
            if (!ok) {
                throw new IllegalStateException("Insertion quiz échouée.");
            }
            System.out.println("Quiz ajouté avec succès !");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du quiz: " + e.getMessage(), e);
        }
    }

    /**
     * Variante de {@link #ajouter(Quiz)} avec retour succès/échec et propagation des erreurs SQL.
     */
    public boolean ajouterWithResult(Quiz quiz) throws SQLException {
        String sql = "INSERT INTO quiz (cours_id, user_id, question, choix_reponses, reponse_correcte, points_valeur) VALUES (?, ?, ?, ?, ?, ?)";
        if (quiz == null || quiz.getCours() == null || quiz.getUser() == null) {
            throw new SQLException("Quiz invalide (cours/user manquant).");
        }

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, quiz.getCours().getId());
            ps.setInt(2, quiz.getUser().getId());
            ps.setString(3, quiz.getQuestion());
            ps.setString(4, quiz.getChoixReponses());
            ps.setString(5, quiz.getReponseCorrecte());
            ps.setInt(6, quiz.getPointsValeur());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                notificationMailer.notifyQuizAddedAsync(quiz, null);
            }
            return ok;
        }
    }

    @Override
    public void modifier(Quiz quiz) {
        String sql = "UPDATE quiz SET cours_id=?, user_id=?, question=?, choix_reponses=?, reponse_correcte=?, points_valeur=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, quiz.getCours().getId());
            ps.setInt(2, quiz.getUser().getId());
            ps.setString(3, quiz.getQuestion());
            ps.setString(4, quiz.getChoixReponses());
            ps.setString(5, quiz.getReponseCorrecte());
            ps.setInt(6, quiz.getPointsValeur());
            ps.setInt(7, quiz.getId());

            ps.executeUpdate();
            System.out.println("Quiz modifié avec succès !");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification du quiz: " + e.getMessage(), e);
        }
    }

    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM quiz WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Quiz supprimé avec succès !");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du quiz: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Quiz> afficher() {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Cours cours = new Cours();
                cours.setId(rs.getInt("cours_id"));

                User user = new User();
                user.setId(rs.getInt("user_id"));

                Quiz q = new Quiz(
                        rs.getInt("id"),
                        cours,
                        user,
                        rs.getString("question"),
                        rs.getString("choix_reponses"),
                        rs.getString("reponse_correcte"),
                        rs.getInt("points_valeur")
                );

                list.add(q);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des quiz: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public Quiz recupererParId(int id) {
        String sql = "SELECT * FROM quiz WHERE id=?";
        Quiz quiz = null;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cours cours = new Cours();
                    cours.setId(rs.getInt("cours_id"));

                    User user = new User();
                    user.setId(rs.getInt("user_id"));

                    quiz = new Quiz(
                            rs.getInt("id"),
                            cours,
                            user,
                            rs.getString("question"),
                            rs.getString("choix_reponses"),
                            rs.getString("reponse_correcte"),
                            rs.getInt("points_valeur")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement du quiz: " + e.getMessage(), e);
        }

        return quiz;
    }

    @Override
    public List<Quiz> rechercher(String critere) {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz WHERE question LIKE ? ORDER BY question ASC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, "%" + (critere == null ? "" : critere) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Cours cours = new Cours();
                    cours.setId(rs.getInt("cours_id"));

                    User user = new User();
                    user.setId(rs.getInt("user_id"));

                    Quiz q = new Quiz(
                            rs.getInt("id"),
                            cours,
                            user,
                            rs.getString("question"),
                            rs.getString("choix_reponses"),
                            rs.getString("reponse_correcte"),
                            rs.getInt("points_valeur")
                    );

                    list.add(q);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recherche quiz: " + e.getMessage(), e);
        }

        return list;
    }
}
