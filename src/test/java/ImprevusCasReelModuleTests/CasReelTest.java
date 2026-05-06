package ImprevusCasReelModuleTests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pi.entities.CasRelles;
import pi.entities.User;
import pi.services.ImprevusCasreelService.CasReelService;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CasReelTest {

    private static CasReelService casReelService;
    private static Connection connection;
    private static int testUserId = 1;

    private final List<Integer> idsToDelete = new ArrayList<>();

    @BeforeAll
    static void setUp() throws SQLException {
        casReelService = new CasReelService();
        connection = MyDatabase.getInstance().getCnx();
        assertNotNull(connection);
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM `user` ORDER BY id ASC LIMIT 1")) {
            if (rs.next()) {
                testUserId = rs.getInt(1);
            }
        }
    }

    @AfterEach
    void cleanDatabase() throws SQLException {
        for (Integer id : idsToDelete) {
            deleteCasReelById(id);
        }
        idsToDelete.clear();
    }

    @Test
    void testAjouterCasReel() {
        String uniqueTitre = "TestCasAdd_" + UUID.randomUUID();
        CasRelles casReel = new CasRelles(null, uniqueTitre, "Description test", "Gain",
                "Manuel", 50.0, "Solution A", LocalDate.now(), "justif.pdf");
        User u = new User();
        u.setId(testUserId);
        casReel.setUser(u);

        CasReelService.CaseInsertOutcome outcome = casReelService.ajouter(casReel);
        assertTrue(outcome.caseId() > 0);

        CasRelles savedCasReel = findCasReelByTitre(uniqueTitre);
        assertNotNull(savedCasReel);
        assertEquals(uniqueTitre, savedCasReel.getTitre());
        assertEquals("Gain", savedCasReel.getType());
        assertEquals(50.0, savedCasReel.getMontant());
        assertEquals(CasReelService.STATUT_EN_ATTENTE, savedCasReel.getResultat());

        idsToDelete.add(savedCasReel.getId());
    }

    @Test
    void testAfficherCasReels() {
        String uniqueTitre = "TestCasList_" + UUID.randomUUID();
        CasRelles casReel = new CasRelles(null, uniqueTitre, "Description list", "Gain",
                "Manuel", 250.0, "Solution B", LocalDate.now(), "preuve.png");
        User u = new User();
        u.setId(testUserId);
        casReel.setUser(u);
        casReelService.ajouter(casReel);

        CasRelles savedCasReel = findCasReelByTitre(uniqueTitre);
        assertNotNull(savedCasReel);
        idsToDelete.add(savedCasReel.getId());

        List<CasRelles> casReels = casReelService.afficher();
        assertFalse(casReels.isEmpty());
        assertTrue(casReels.stream().anyMatch(item -> item.getId() == savedCasReel.getId()));
    }

    @Test
    void testModifierCasReel() {
        String uniqueTitre = "TestCasUpdate_" + UUID.randomUUID();
        CasRelles casReel = new CasRelles(null, uniqueTitre, "Avant modification", "Gain",
                "Manuel", 320.0, "Solution C", LocalDate.now(), "file.txt");
        User u = new User();
        u.setId(testUserId);
        casReel.setUser(u);
        casReelService.ajouter(casReel);

        CasRelles savedCasReel = findCasReelByTitre(uniqueTitre);
        assertNotNull(savedCasReel);
        idsToDelete.add(savedCasReel.getId());

        savedCasReel.setTitre(uniqueTitre + "_Updated");
        savedCasReel.setDescription("Apres modification");
        savedCasReel.setMontant(500.0);
        savedCasReel.setType("Gain");
        savedCasReel.setSolution("Solution modifiee");
        casReelService.modifier(savedCasReel);

        CasRelles updatedCasReel = findCasReelById(savedCasReel.getId());
        assertNotNull(updatedCasReel);
        assertEquals(uniqueTitre + "_Updated", updatedCasReel.getTitre());
        assertEquals("Apres modification", updatedCasReel.getDescription());
        assertEquals(500.0, updatedCasReel.getMontant());
        assertEquals("Gain", updatedCasReel.getType());
    }

    @Test
    void testSupprimerCasReel() {
        String uniqueTitre = "TestCasDelete_" + UUID.randomUUID();
        CasRelles casReel = new CasRelles(null, uniqueTitre, "Description delete", "Gain",
                "Manuel", 10.0, "Solution D", LocalDate.now(), "delete.pdf");
        User u = new User();
        u.setId(testUserId);
        casReel.setUser(u);
        casReelService.ajouter(casReel);

        CasRelles savedCasReel = findCasReelByTitre(uniqueTitre);
        assertNotNull(savedCasReel);

        casReelService.supprimer(savedCasReel.getId());

        CasRelles deletedCasReel = findCasReelById(savedCasReel.getId());
        assertTrue(deletedCasReel == null);
    }

    private CasRelles findCasReelByTitre(String titre) {
        return casReelService.afficher().stream()
                .filter(item -> titre.equals(item.getTitre()))
                .findFirst()
                .orElse(null);
    }

    private CasRelles findCasReelById(int id) {
        return casReelService.afficher().stream()
                .filter(item -> item.getId() == id)
                .findFirst()
                .orElse(null);
    }

    private void deleteCasReelById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM cas_relles WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
