package ExpenseRevenueTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import pi.entities.Expense;
import pi.entities.Revenue;
import pi.entities.User;
import pi.services.RevenueExpenseService.ExpenseService;
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
public class ExpenseServiceTest {

    private static ExpenseService expenseService;
    private static RevenueService revenueService;
    private static Connection connection;
    private final List<Integer> expenseIdsToDelete = new ArrayList<>();
    private final List<Integer> revenueIdsToDelete = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        expenseService = new ExpenseService();
        revenueService = new RevenueService();
        connection = MyDatabase.getInstance().getCnx();
        assertNotNull(connection);
    }

    @AfterEach
    void cleanDatabase() throws SQLException {
        for (Integer expenseId : expenseIdsToDelete) {
            deleteExpenseIfExists(expenseId);
        }
        expenseIdsToDelete.clear();

        for (Integer revenueId : revenueIdsToDelete) {
            deleteRevenueIfExists(revenueId);
        }
        revenueIdsToDelete.clear();
    }

    @Test
    @Order(1)
    void testAddExpense() throws SQLException {
        Revenue revenue = createRevenueForExpense("TEST_EXPENSE_ADD_REVENUE");
        Expense expense = createExpense(revenue, "TEST_ADD_EXPENSE");

        expenseService.add(expense);
        expenseIdsToDelete.add(expense.getId());

        assertTrue(expense.getId() > 0);

        Expense savedExpense = expenseService.getById(expense.getId());
        assertNotNull(savedExpense);
        assertEquals(expense.getAmount(), savedExpense.getAmount());
        assertEquals(expense.getCategory(), savedExpense.getCategory());
        assertEquals(expense.getDescription(), savedExpense.getDescription());
        assertEquals(revenue.getId(), savedExpense.getRevenue().getId());
    }

    @Test
    @Order(2)
    void testGetAllExpense() throws SQLException {
        Revenue revenue = createRevenueForExpense("TEST_EXPENSE_LIST_REVENUE");
        Expense expense = createExpense(revenue, "TEST_LIST_EXPENSE");
        expenseService.add(expense);
        expenseIdsToDelete.add(expense.getId());

        List<Expense> expenses = expenseService.getAll();

        assertNotNull(expenses);
        assertFalse(expenses.isEmpty());
        assertTrue(expenses.stream().anyMatch(item -> item.getId() == expense.getId()));
    }

    @Test
    @Order(3)
    void testUpdateExpense() throws SQLException {
        Revenue revenue = createRevenueForExpense("TEST_EXPENSE_UPDATE_REVENUE");
        Expense expense = createExpense(revenue, "TEST_UPDATE_EXPENSE");
        expenseService.add(expense);
        expenseIdsToDelete.add(expense.getId());

        expense.setAmount(321.45);
        expense.setCategory("Transport");
        expense.setDescription("UPDATED_EXPENSE_DESCRIPTION");
        expense.setExpenseDate(LocalDate.now().minusDays(2));

        expenseService.update(expense);

        Expense updatedExpense = expenseService.getById(expense.getId());
        assertNotNull(updatedExpense);
        assertEquals(321.45, updatedExpense.getAmount());
        assertEquals("Transport", updatedExpense.getCategory());
        assertEquals("UPDATED_EXPENSE_DESCRIPTION", updatedExpense.getDescription());
        assertEquals(expense.getExpenseDate(), updatedExpense.getExpenseDate());
    }

    @Test
    @Order(4)
    void testDeleteExpense() throws SQLException {
        Revenue revenue = createRevenueForExpense("TEST_EXPENSE_DELETE_REVENUE");
        Expense expense = createExpense(revenue, "TEST_DELETE_EXPENSE");
        expenseService.add(expense);

        assertTrue(expense.getId() > 0);

        expenseService.delete(expense.getId());

        Expense deletedExpense = expenseService.getById(expense.getId());
        assertEquals(null, deletedExpense);
    }

    private Expense createExpense(Revenue revenue, String description) throws SQLException {
        User user = new User();
        user.setId(getExistingUserId());

        return new Expense(
                revenue,
                user,
                120.25,
                "Alimentation",
                LocalDate.now(),
                description
        );
    }

    private Revenue createRevenueForExpense(String description) throws SQLException {
        User user = new User();
        user.setId(getExistingUserId());

        Revenue revenue = new Revenue();
        revenue.setUser(user);
        revenue.setAmount(800.00);
        revenue.setType("FIXE");
        revenue.setReceivedAt(LocalDate.now());
        revenue.setDescription(description);
        revenue.setCreatedAt(LocalDateTime.now().withNano(0));

        revenueService.add(revenue);
        revenueIdsToDelete.add(revenue.getId());
        return revenue;
    }

    private int getExistingUserId() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM user ORDER BY id LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next(), "No user found in database for Expense tests.");
            return rs.getInt("id");
        }
    }

    private void deleteExpenseIfExists(int expenseId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM expense WHERE id = ?")) {
            ps.setInt(1, expenseId);
            ps.executeUpdate();
        }
    }

    private void deleteRevenueIfExists(int revenueId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM revenue WHERE id = ?")) {
            ps.setInt(1, revenueId);
            ps.executeUpdate();
        }
    }
}
