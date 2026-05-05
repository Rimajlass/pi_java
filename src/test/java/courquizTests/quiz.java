package courquizTests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import pi.entities.Cours;
import pi.entities.Quiz;
import pi.entities.User;
import pi.services.CoursQuizService.CoursService;
import pi.services.CoursQuizService.QuizService;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(OrderAnnotation.class)
public class quiz {

    private static QuizService quizService;
    private static CoursService coursService;
    private static Connection cnx;
    private final List<Integer> insertedQuizIds = new ArrayList<>();
    private final List<Integer> insertedCoursIds = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        quizService = new QuizService();
        coursService = new CoursService();
        cnx = MyDatabase.getInstance().getCnx();

        assertNotNull(quizService);
        assertNotNull(coursService);
        assertNotNull(cnx);
    }

    @AfterEach
    void cleanDatabase() throws SQLException {
        for (Integer id : insertedQuizIds) {
            deleteQuizById(id);
        }
        insertedQuizIds.clear();

        for (Integer id : insertedCoursIds) {
            deleteCoursById(id);
        }
        insertedCoursIds.clear();
    }

    @Test
    @Order(1)
    void testAjouterQuiz() throws SQLException {
        Cours cours = createCourseForQuiz();
        String uniqueQuestion = "Question test " + System.nanoTime();
        Quiz quiz = createQuiz(cours, uniqueQuestion);

        quizService.ajouter(quiz);

        Quiz insertedQuiz = findQuizByQuestion(uniqueQuestion);
        assertNotNull(insertedQuiz);
        assertEquals(uniqueQuestion, insertedQuiz.getQuestion());
        assertEquals(10, insertedQuiz.getPointsValeur());

        insertedQuizIds.add(insertedQuiz.getId());
    }

    @Test
    @Order(2)
    void testAfficherQuiz() throws SQLException {
        Cours cours = createCourseForQuiz();
        String uniqueQuestion = "Question list " + System.nanoTime();
        Quiz quiz = createQuiz(cours, uniqueQuestion);

        quizService.ajouter(quiz);

        Quiz insertedQuiz = findQuizByQuestion(uniqueQuestion);
        assertNotNull(insertedQuiz);
        insertedQuizIds.add(insertedQuiz.getId());

        List<Quiz> quizList = quizService.afficher();

        assertNotNull(quizList);
        assertFalse(quizList.isEmpty());
        assertTrue(quizList.stream().anyMatch(q -> uniqueQuestion.equals(q.getQuestion())));
    }

    @Test
    @Order(3)
    void testModifierQuiz() throws SQLException {
        Cours cours = createCourseForQuiz();
        String originalQuestion = "Question update " + System.nanoTime();
        Quiz quiz = createQuiz(cours, originalQuestion);

        quizService.ajouter(quiz);

        Quiz insertedQuiz = findQuizByQuestion(originalQuestion);
        assertNotNull(insertedQuiz);
        insertedQuizIds.add(insertedQuiz.getId());

        String updatedQuestion = originalQuestion + " modifiee";
        insertedQuiz.setQuestion(updatedQuestion);
        insertedQuiz.setChoixReponses("[\"A\",\"B\",\"C\",\"D\"]");
        insertedQuiz.setReponseCorrecte("B");
        insertedQuiz.setPointsValeur(20);

        quizService.modifier(insertedQuiz);

        Quiz updatedQuiz = quizService.recupererParId(insertedQuiz.getId());
        assertNotNull(updatedQuiz);
        assertEquals(updatedQuestion, updatedQuiz.getQuestion());
        assertEquals("B", updatedQuiz.getReponseCorrecte());
        assertEquals(20, updatedQuiz.getPointsValeur());
    }

    @Test
    @Order(4)
    void testSupprimerQuiz() throws SQLException {
        Cours cours = createCourseForQuiz();
        String uniqueQuestion = "Question delete " + System.nanoTime();
        Quiz quiz = createQuiz(cours, uniqueQuestion);

        quizService.ajouter(quiz);

        Quiz insertedQuiz = findQuizByQuestion(uniqueQuestion);
        assertNotNull(insertedQuiz);

        quizService.supprimer(insertedQuiz.getId());

        Quiz deletedQuiz = quizService.recupererParId(insertedQuiz.getId());
        assertTrue(deletedQuiz == null);
    }

    private Quiz createQuiz(Cours cours, String question) throws SQLException {
        User user = new User();
        user.setId(findExistingUserId());

        return new Quiz(
                cours,
                user,
                question,
                "[\"A\",\"B\",\"C\"]",
                "A",
                10
        );
    }

    private Cours createCourseForQuiz() throws SQLException {
        User user = new User();
        user.setId(findExistingUserId());

        String uniqueTitre = "Cours support quiz " + System.nanoTime();
        Cours cours = new Cours(
                user,
                uniqueTitre,
                "Contenu texte de support pour le quiz avec plus de trente caracteres.",
                "video",
                "https://example.com/support"
        );

        coursService.ajouter(cours);

        Cours insertedCours = findCoursByTitre(uniqueTitre);
        assertNotNull(insertedCours);
        insertedCoursIds.add(insertedCours.getId());
        return insertedCours;
    }

    private int findExistingUserId() throws SQLException {
        String[] queries = {
                "SELECT id FROM user ORDER BY id LIMIT 1",
                "SELECT id FROM users ORDER BY id LIMIT 1",
                "SELECT user_id FROM cours ORDER BY id LIMIT 1",
                "SELECT user_id FROM quiz ORDER BY id LIMIT 1"
        };

        for (String query : queries) {
            try (PreparedStatement ps = cnx.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (SQLException ignored) {
            }
        }

        throw new SQLException("Aucun user_id valide trouve pour executer les tests du module quiz.");
    }

    private Cours findCoursByTitre(String titre) throws SQLException {
        String sql = "SELECT * FROM cours WHERE titre = ? ORDER BY id DESC LIMIT 1";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, titre);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("user_id"));

                    return new Cours(
                            rs.getInt("id"),
                            user,
                            rs.getString("titre"),
                            rs.getString("contenu_texte"),
                            rs.getString("type_media"),
                            rs.getString("url_media")
                    );
                }
            }
        }

        return null;
    }

    private Quiz findQuizByQuestion(String question) throws SQLException {
        String sql = "SELECT * FROM quiz WHERE question = ? ORDER BY id DESC LIMIT 1";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, question);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cours cours = new Cours();
                    cours.setId(rs.getInt("cours_id"));

                    User user = new User();
                    user.setId(rs.getInt("user_id"));

                    return new Quiz(
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
        }

        return null;
    }

    private void deleteQuizById(int id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM quiz WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void deleteCoursById(int id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM cours WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
