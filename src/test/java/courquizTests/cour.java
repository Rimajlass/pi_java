package courquizTests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import pi.entities.Cours;
import pi.entities.User;
import pi.services.CoursQuizService.CoursService;
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
public class cour {

    private static CoursService coursService;
    private static Connection cnx;
    private final List<Integer> insertedCoursIds = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        coursService = new CoursService();
        cnx = MyDatabase.getInstance().getCnx();

        assertNotNull(coursService);
        assertNotNull(cnx);
    }

    @AfterEach
    void cleanDatabase() throws SQLException {
        for (Integer id : insertedCoursIds) {
            deleteCoursById(id);
        }
        insertedCoursIds.clear();
    }

    @Test
    @Order(1)
    void testAjouterCours() throws SQLException {
        String uniqueTitre = "Cours Test " + System.nanoTime();
        Cours cours = createCours(uniqueTitre);

        coursService.ajouter(cours);

        Cours insertedCours = findCoursByTitre(uniqueTitre);
        assertNotNull(insertedCours);
        assertEquals(uniqueTitre, insertedCours.getTitre());
        assertEquals("video", insertedCours.getTypeMedia());

        insertedCoursIds.add(insertedCours.getId());
    }

    @Test
    @Order(2)
    void testAfficherCours() throws SQLException {
        String uniqueTitre = "Cours List " + System.nanoTime();
        Cours cours = createCours(uniqueTitre);
        coursService.ajouter(cours);

        Cours insertedCours = findCoursByTitre(uniqueTitre);
        assertNotNull(insertedCours);
        insertedCoursIds.add(insertedCours.getId());

        List<Cours> coursList = coursService.afficher();

        assertNotNull(coursList);
        assertFalse(coursList.isEmpty());
        assertTrue(coursList.stream().anyMatch(c -> uniqueTitre.equals(c.getTitre())));
    }

    @Test
    @Order(3)
    void testModifierCours() throws SQLException {
        String originalTitre = "Cours Update " + System.nanoTime();
        Cours cours = createCours(originalTitre);
        coursService.ajouter(cours);

        Cours insertedCours = findCoursByTitre(originalTitre);
        assertNotNull(insertedCours);
        insertedCoursIds.add(insertedCours.getId());

        String updatedTitre = originalTitre + " Modifie";
        insertedCours.setTitre(updatedTitre);
        insertedCours.setContenuTexte("Contenu texte modifie pour verifier correctement le test unitaire.");
        insertedCours.setTypeMedia("pdf");
        insertedCours.setUrlMedia("https://example.com/updated.pdf");

        coursService.modifier(insertedCours);

        Cours updatedCours = coursService.recupererParId(insertedCours.getId());
        assertNotNull(updatedCours);
        assertEquals(updatedTitre, updatedCours.getTitre());
        assertEquals("pdf", updatedCours.getTypeMedia());
        assertEquals("https://example.com/updated.pdf", updatedCours.getUrlMedia());
    }

    @Test
    @Order(4)
    void testSupprimerCours() throws SQLException {
        String uniqueTitre = "Cours Delete " + System.nanoTime();
        Cours cours = createCours(uniqueTitre);
        coursService.ajouter(cours);

        Cours insertedCours = findCoursByTitre(uniqueTitre);
        assertNotNull(insertedCours);

        coursService.supprimer(insertedCours.getId());

        Cours deletedCours = coursService.recupererParId(insertedCours.getId());
        assertTrue(deletedCours == null);
    }

    private Cours createCours(String titre) throws SQLException {
        User user = new User();
        user.setId(findExistingUserId());

        return new Cours(
                user,
                titre,
                "Contenu texte de test suffisamment long pour valider l'ajout du cours.",
                "video",
                "https://example.com/media"
        );
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

        throw new SQLException("Aucun user_id valide trouve pour executer les tests du module cours.");
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

    private void deleteCoursById(int id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM cours WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
