package ExpenseRevenueTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import pi.entities.Revenue;
import pi.entities.User;
import pi.services.RevenueExpenseService.RevenueService;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RevenueServiceTest {

    private static RevenueService revenueService;
    private static Connection connection;
    private final List<Integer> revenueIdsToDelete = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        revenueService = new RevenueService();
        connection = MyDatabase.getInstance().getCnx();
        assertNotNull(connection);
    }

    @AfterEach
    void cleanDatabase() throws SQLException {
        for (Integer revenueId : revenueIdsToDelete) {
            deleteRevenueIfExists(revenueId);
        }
        revenueIdsToDelete.clear();
    }

    @Test
    @Order(1)
    void testAddRevenue() throws SQLException {
        Revenue revenue = createRevenue("TEST_ADD_REVENUE");

        revenueService.add(revenue);
        revenueIdsToDelete.add(revenue.getId());

        assertTrue(revenue.getId() > 0);

        Revenue savedRevenue = revenueService.getById(revenue.getId());
        assertNotNull(savedRevenue);
        assertEquals(revenue.getAmount(), savedRevenue.getAmount());
        assertEquals(revenue.getType(), savedRevenue.getType());
        assertEquals(revenue.getDescription(), savedRevenue.getDescription());
    }

    @Test
    @Order(2)
    void testGetAllRevenue() throws SQLException {
        Revenue revenue = createRevenue("TEST_LIST_REVENUE");
        revenueService.add(revenue);
        revenueIdsToDelete.add(revenue.getId());

        List<Revenue> revenues = revenueService.getAll();

        assertNotNull(revenues);
        assertFalse(revenues.isEmpty());
        assertTrue(revenues.stream().anyMatch(item -> item.getId() == revenue.getId()));
    }

    @Test
    @Order(3)
    void testUpdateRevenue() throws SQLException {
        Revenue revenue = createRevenue("TEST_UPDATE_REVENUE");
        revenueService.add(revenue);
        revenueIdsToDelete.add(revenue.getId());

        revenue.setAmount(999.50);
        revenue.setType("BONUS");
        revenue.setDescription("UPDATED_REVENUE_DESCRIPTION");
        revenue.setReceivedAt(LocalDate.now().minusDays(1));
        revenue.setCreatedAt(LocalDateTime.now().withNano(0));

        revenueService.update(revenue);

        Revenue updatedRevenue = revenueService.getById(revenue.getId());
        assertNotNull(updatedRevenue);
        assertEquals(999.50, updatedRevenue.getAmount());
        assertEquals("BONUS", updatedRevenue.getType());
        assertEquals("UPDATED_REVENUE_DESCRIPTION", updatedRevenue.getDescription());
        assertEquals(revenue.getReceivedAt(), updatedRevenue.getReceivedAt());
    }

    @Test
    @Order(4)
    void testDeleteRevenue() throws SQLException {
        Revenue revenue = createRevenue("TEST_DELETE_REVENUE");
        revenueService.add(revenue);

        assertTrue(revenue.getId() > 0);

        revenueService.delete(revenue.getId());

        Revenue deletedRevenue = revenueService.getById(revenue.getId());
        assertEquals(null, deletedRevenue);
    }

    private Revenue createRevenue(String description) throws SQLException {
        User user = new User();
        user.setId(getExistingUserId());

        Revenue revenue = new Revenue();
        revenue.setUser(user);
        revenue.setAmount(250.75);
        revenue.setType("FIXE");
        revenue.setReceivedAt(LocalDate.now());
        revenue.setDescription(description);
        revenue.setCreatedAt(LocalDateTime.now().withNano(0));
        return revenue;
    }

    private int getExistingUserId() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM user ORDER BY id LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next(), "No user found in database for Revenue tests.");
            return rs.getInt("id");
        }
    }

    private void deleteRevenueIfExists(int revenueId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM revenue WHERE id = ?")) {
            ps.setInt(1, revenueId);
            ps.executeUpdate();
        }
    }
}
