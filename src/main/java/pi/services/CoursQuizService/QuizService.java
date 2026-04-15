package pi.services.CoursQuizService;

import pi.entities.Cours;
import pi.entities.Quiz;
import pi.entities.User;
import pi.interfaces.IQuizService;
import pi.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizService implements IQuizService {

    Connection cnx;

    public QuizService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void ajouter(Quiz quiz) {
        String sql = "INSERT INTO quiz (cours_id, user_id, question, choix_reponses, reponse_correcte, points_valeur) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, quiz.getCours().getId());
            ps.setInt(2, quiz.getUser().getId());
            ps.setString(3, quiz.getQuestion());
            ps.setString(4, quiz.getChoixReponses());
            ps.setString(5, quiz.getReponseCorrecte());
            ps.setInt(6, quiz.getPointsValeur());

            ps.executeUpdate();
            System.out.println("Quiz ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void modifier(Quiz quiz) {
        String sql = "UPDATE quiz SET cours_id=?, user_id=?, question=?, choix_reponses=?, reponse_correcte=?, points_valeur=? WHERE id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
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
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM quiz WHERE id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);

            ps.executeUpdate();
            System.out.println("Quiz supprimé avec succès !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Quiz> afficher() {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

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
            System.out.println(e.getMessage());
        }

        return list;
    }

    @Override
    public Quiz recupererParId(int id) {
        String sql = "SELECT * FROM quiz WHERE id=?";
        Quiz quiz = null;

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

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
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return quiz;
    }

    @Override
    public List<Quiz> rechercher(String critere) {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz WHERE question LIKE ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, "%" + critere + "%");
            ResultSet rs = ps.executeQuery();

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
            System.out.println("Erreur recherche Quiz : " + e.getMessage());
        }

        return list;
    }
}
