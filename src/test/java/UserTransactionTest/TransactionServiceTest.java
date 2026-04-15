package UserTransactionTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pi.entities.Transaction;
import pi.entities.User;
import pi.services.UserTransactionService.TransactionService;
import pi.services.UserTransactionService.UserService;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionServiceTest {

    private static TransactionService transactionService;
    private static UserService userService;
    private static Connection cnx;

    private final List<Integer> insertedTxIds = new ArrayList<>();
    private final List<Integer> insertedUserIds = new ArrayList<>();

    @BeforeAll
    static void init() {
        transactionService = new TransactionService();
        userService = new UserService();
        cnx = MyDatabase.getInstance().getCnx();
        assertNotNull(cnx, "Connexion DB indisponible");
    }

    @AfterEach
    void cleanDatabase() throws Exception {
        for (Integer txId : insertedTxIds) {
            try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM transaction WHERE id = ?")) {
                ps.setInt(1, txId);
                ps.executeUpdate();
            }
        }
        insertedTxIds.clear();

        for (Integer userId : insertedUserIds) {
            try (PreparedStatement psTx = cnx.prepareStatement("DELETE FROM transaction WHERE user_id = ?")) {
                psTx.setInt(1, userId);
                psTx.executeUpdate();
            }
            try (PreparedStatement psUser = cnx.prepareStatement("DELETE FROM `user` WHERE id = ?")) {
                psUser.setInt(1, userId);
                psUser.executeUpdate();
            }
        }
        insertedUserIds.clear();
    }

    @Test
    void addTest_insertTransaction_shouldExistInDatabase() throws Exception {
        User user = createTempUser("tx_add_");
        transactionService.insertTransactionForUser(
                user.getId(),
                "EXPENSE",
                25.5,
                LocalDate.now(),
                "Unit test add",
                "tests"
        );

        int txId = findLastTransactionIdForUser(user.getId());
        insertedTxIds.add(txId);

        List<Transaction> rows = transactionService.findByUserId(user.getId());
        assertNotNull(rows);
        assertTrue(rows.stream().anyMatch(t -> t.getId() == txId));
    }

    @Test
    void getListTest_findAll_shouldContainExpectedData() throws Exception {
        User user = createTempUser("tx_list_");
        transactionService.insertTransactionForUser(
                user.getId(),
                "SAVING",
                80.0,
                LocalDate.now(),
                "Unit test list",
                "tests"
        );

        int txId = findLastTransactionIdForUser(user.getId());
        insertedTxIds.add(txId);

        List<Transaction> rows = transactionService.findAllForAdmin(
                null, null, null, user.getNom(), "date", "DESC"
        );
        assertNotNull(rows);
        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().anyMatch(t -> t.getId() == txId));
    }

    @Test
    void updateTest_modifyTransaction_shouldApplyChanges() throws Exception {
        User user = createTempUser("tx_upd_");
        transactionService.insertTransactionForUser(
                user.getId(),
                "INVESTMENT",
                120.0,
                LocalDate.now(),
                "Before update",
                "tests"
        );

        int txId = findLastTransactionIdForUser(user.getId());
        insertedTxIds.add(txId);

        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE transaction SET montant = ?, description = ?, date = ? WHERE id = ?")) {
            ps.setDouble(1, 333.0);
            ps.setString(2, "After update");
            ps.setDate(3, Date.valueOf(LocalDate.now().minusDays(1)));
            ps.setInt(4, txId);
            ps.executeUpdate();
        }

        List<Transaction> rows = transactionService.findByUserId(user.getId());
        Transaction updated = rows.stream().filter(t -> t.getId() == txId).findFirst().orElse(null);
        assertNotNull(updated);
        assertEquals(333.0, updated.getMontant(), 0.0001);
        assertEquals("After update", updated.getDescription());
    }

    @Test
    void deleteTest_deleteTransaction_shouldNotExistAfterDelete() throws Exception {
        User user = createTempUser("tx_del_");
        transactionService.insertTransactionForUser(
                user.getId(),
                "EXPENSE",
                40.0,
                LocalDate.now(),
                "To delete",
                "tests"
        );

        int txId = findLastTransactionIdForUser(user.getId());

        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM transaction WHERE id = ?")) {
            ps.setInt(1, txId);
            ps.executeUpdate();
        }

        List<Transaction> rows = transactionService.findByUserId(user.getId());
        assertTrue(rows.stream().noneMatch(t -> t.getId() == txId));
    }

    private User createTempUser(String prefix) {
        User u = new User();
        u.setNom(prefix + "user");
        u.setEmail(prefix + System.currentTimeMillis() + "@mail.com");
        u.setRoles("[\"ROLE_USER\"]");
        u.setSoldeTotal(500.0);
        User created = userService.create(u, "StrongPass123");
        insertedUserIds.add(created.getId());
        return created;
    }

    private int findLastTransactionIdForUser(int userId) throws Exception {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT id FROM transaction WHERE user_id = ? ORDER BY id DESC LIMIT 1")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        throw new IllegalStateException("Aucune transaction trouvee pour user_id=" + userId);
    }
}

