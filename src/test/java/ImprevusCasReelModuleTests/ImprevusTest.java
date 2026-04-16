package ImprevusCasReelModuleTests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pi.entities.Imprevus;
import pi.services.ImprevusCasreelService.ImprevusService;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImprevusTest {

    private static ImprevusService imprevusService;
    private static Connection connection;

    private final List<Integer> idsToDelete = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        imprevusService = new ImprevusService();
        connection = MyDatabase.getInstance().getCnx();
        assertNotNull(connection);
    }

    @AfterEach
    void cleanDatabase() throws SQLException {
        for (Integer id : idsToDelete) {
            deleteImprevuById(id);
        }
        idsToDelete.clear();
    }

    @Test
    void testAjouterImprevu() {
        String uniqueTitre = "TestImpAdd_" + UUID.randomUUID();
        Imprevus imprevu = new Imprevus(uniqueTitre, "Depense", 150.0);

        imprevusService.ajouter(imprevu);

        Imprevus savedImprevu = findImprevuByTitre(uniqueTitre);
        assertNotNull(savedImprevu);
        assertEquals(uniqueTitre, savedImprevu.getTitre());
        assertEquals("Depense", savedImprevu.getType());
        assertEquals(150.0, savedImprevu.getBudget());

        idsToDelete.add(savedImprevu.getId());
    }

    @Test
    void testAfficherImprevus() {
        String uniqueTitre = "TestImpList_" + UUID.randomUUID();
        Imprevus imprevu = new Imprevus(uniqueTitre, "Gain", 220.0);
        imprevusService.ajouter(imprevu);

        Imprevus savedImprevu = findImprevuByTitre(uniqueTitre);
        assertNotNull(savedImprevu);
        idsToDelete.add(savedImprevu.getId());

        List<Imprevus> imprevus = imprevusService.afficher();
        assertFalse(imprevus.isEmpty());
        assertTrue(imprevus.stream().anyMatch(item -> item.getId() == savedImprevu.getId()));
    }

    @Test
    void testModifierImprevu() {
        String uniqueTitre = "TestImpUpdate_" + UUID.randomUUID();
        Imprevus imprevu = new Imprevus(uniqueTitre, "Depense", 300.0);
        imprevusService.ajouter(imprevu);

        Imprevus savedImprevu = findImprevuByTitre(uniqueTitre);
        assertNotNull(savedImprevu);
        idsToDelete.add(savedImprevu.getId());

        savedImprevu.setTitre(uniqueTitre + "_Updated");
        savedImprevu.setType("Gain");
        savedImprevu.setBudget(455.5);
        imprevusService.modifier(savedImprevu);

        Imprevus updatedImprevu = imprevusService.getById(savedImprevu.getId());
        assertNotNull(updatedImprevu);
        assertEquals(uniqueTitre + "_Updated", updatedImprevu.getTitre());
        assertEquals("Gain", updatedImprevu.getType());
        assertEquals(455.5, updatedImprevu.getBudget());
    }

    @Test
    void testSupprimerImprevu() {
        String uniqueTitre = "TestImpDelete_" + UUID.randomUUID();
        Imprevus imprevu = new Imprevus(uniqueTitre, "Depense", 99.0);
        imprevusService.ajouter(imprevu);

        Imprevus savedImprevu = findImprevuByTitre(uniqueTitre);
        assertNotNull(savedImprevu);

        imprevusService.supprimer(savedImprevu.getId());

        Imprevus deletedImprevu = imprevusService.getById(savedImprevu.getId());
        assertTrue(deletedImprevu == null);
    }

    private Imprevus findImprevuByTitre(String titre) {
        List<Imprevus> imprevus = imprevusService.afficher();
        return imprevus.stream()
                .filter(item -> titre.equals(item.getTitre()))
                .findFirst()
                .orElse(null);
    }

    private void deleteImprevuById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM imprevus WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
