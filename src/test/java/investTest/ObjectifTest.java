package investTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import pi.entities.Objectif;
import pi.services.InvestissementService.ObjectifService;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(OrderAnnotation.class)
public class ObjectifTest {

    private static ObjectifService objectifService;
    private static Connection cnx;
    private final List<Integer> insertedObjectifIds = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        objectifService = new ObjectifService();
        cnx = MyDatabase.getInstance().getCnx();

        assertNotNull(objectifService);
        assertNotNull(cnx);
    }

    @AfterEach
    void cleanDatabase() throws Exception {
        for (Integer id : insertedObjectifIds) {
            objectifService.delete(id);
        }
        insertedObjectifIds.clear();
    }

    @Test
    @Order(1)
    void testAddObjectif() throws Exception {
        String uniqueName = "Objectif Test " + System.nanoTime();
        Objectif obj = new Objectif(uniqueName, 2.0, 1000.0, 2000.0, false, LocalDate.now());

        objectifService.add(obj);

        assertTrue(obj.getId() > 0, "add() should set a generated ID on the objectif");

        Objectif found = findObjectifByName(uniqueName);
        assertNotNull(found);
        assertEquals(uniqueName, found.getName());
        assertEquals(2000.0, found.getTargetAmount());
        assertFalse(found.isCompleted());

        insertedObjectifIds.add(obj.getId());
    }

    @Test
    @Order(2)
    void testGetAllObjectifs() throws Exception {
        String uniqueName = "Objectif List " + System.nanoTime();
        Objectif obj = new Objectif(uniqueName, 1.5, 500.0, 750.0, false, LocalDate.now());
        objectifService.add(obj);
        insertedObjectifIds.add(obj.getId());

        List<Objectif> list = objectifService.getAll();

        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(o -> uniqueName.equals(o.getName())));
    }

    @Test
    @Order(3)
    void testUpdateObjectif() throws Exception {
        String originalName = "Objectif Update " + System.nanoTime();
        Objectif obj = new Objectif(originalName, 2.0, 1000.0, 2000.0, false, LocalDate.now());
        objectifService.add(obj);
        insertedObjectifIds.add(obj.getId());

        String updatedName = originalName + " Modifie";
        obj.setName(updatedName);
        obj.setTargetMultiplier(3.0);
        obj.setInitialAmount(1500.0);
        obj.setTargetAmount(4500.0);

        objectifService.update(obj);

        Objectif updated = findObjectifByName(updatedName);
        assertNotNull(updated);
        assertEquals(updatedName, updated.getName());
        assertEquals(3.0, updated.getTargetMultiplier());
        assertEquals(4500.0, updated.getTargetAmount());
    }

    @Test
    @Order(4)
    void testDeleteObjectif() throws Exception {
        String uniqueName = "Objectif Delete " + System.nanoTime();
        Objectif obj = new Objectif(uniqueName, 2.0, 1000.0, 2000.0, false, LocalDate.now());
        objectifService.add(obj);
        int id = obj.getId();

        objectifService.delete(id);

        Objectif deleted = findObjectifById(id);
        assertNull(deleted, "Objectif should no longer exist after deletion");
    }

    @Test
    @Order(5)
    void testCheckAndMarkCompletedDoesNotMarkWhenBelowTarget() throws Exception {
        // Objectif with a very high target — no investments linked so currentValue = 0
        // checkAndMarkCompleted should leave is_completed = false
        String uniqueName = "Objectif Check " + System.nanoTime();
        Objectif obj = new Objectif(uniqueName, 2.0, 1000.0, 9999999.0, false, LocalDate.now());
        objectifService.add(obj);
        insertedObjectifIds.add(obj.getId());

        objectifService.checkAndMarkCompleted(obj.getId());

        Objectif result = findObjectifById(obj.getId());
        assertNotNull(result);
        assertFalse(result.isCompleted(), "Objectif should NOT be marked completed when current value is below target");
    }

    private Objectif findObjectifByName(String name) throws SQLException {
        String sql = "SELECT * FROM objectif WHERE name = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Objectif(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("target_multiplier"),
                            rs.getDouble("initial_amount"),
                            rs.getDouble("target_amount"),
                            rs.getBoolean("is_completed"),
                            rs.getDate("created_at").toLocalDate()
                    );
                }
            }
        }
        return null;
    }

    private Objectif findObjectifById(int id) throws SQLException {
        String sql = "SELECT * FROM objectif WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Objectif(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("target_multiplier"),
                            rs.getDouble("initial_amount"),
                            rs.getDouble("target_amount"),
                            rs.getBoolean("is_completed"),
                            rs.getDate("created_at").toLocalDate()
                    );
                }
            }
        }
        return null;
    }
}
