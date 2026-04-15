package UserTransactionTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pi.entities.User;
import pi.services.UserTransactionService.UserService;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private static UserService userService;
    private static Connection cnx;
    private final List<Integer> insertedUserIds = new ArrayList<>();

    @BeforeAll
    static void init() {
        userService = new UserService();
        cnx = MyDatabase.getInstance().getCnx();
        assertNotNull(cnx, "Connexion DB indisponible");
    }

    @AfterEach
    void cleanDatabase() throws Exception {
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
    void addTest_createUser_shouldExistInDatabase() {
        User u = new User();
        u.setNom("Test User");
        u.setEmail("ut_add_" + System.currentTimeMillis() + "@mail.com");
        u.setRoles("[\"ROLE_USER\"]");
        u.setSoldeTotal(100.0);

        User created = userService.create(u, "StrongPass123");
        insertedUserIds.add(created.getId());

        assertTrue(created.getId() > 0);
        User fromDb = userService.findByEmail(created.getEmail());
        assertNotNull(fromDb);
        assertEquals(created.getEmail(), fromDb.getEmail());
    }

    @Test
    void getListTest_findForAdminIndex_shouldReturnCreatedUser() {
        User u = new User();
        u.setNom("List User");
        u.setEmail("ut_list_" + System.currentTimeMillis() + "@mail.com");
        u.setRoles("[\"ROLE_USER\"]");
        u.setSoldeTotal(50.0);
        User created = userService.create(u, "StrongPass123");
        insertedUserIds.add(created.getId());

        List<User> users = userService.findForAdminIndex("ut_list_", "", "id", "DESC");
        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertTrue(users.stream().anyMatch(x -> created.getEmail().equalsIgnoreCase(x.getEmail())));
    }

    @Test
    void updateTest_updateUser_shouldApplyChanges() {
        User u = new User();
        u.setNom("Old Name");
        u.setEmail("ut_upd_" + System.currentTimeMillis() + "@mail.com");
        u.setRoles("[\"ROLE_USER\"]");
        u.setSoldeTotal(70.0);
        User created = userService.create(u, "StrongPass123");
        insertedUserIds.add(created.getId());

        created.setNom("New Name");
        created.setSoldeTotal(999.5);
        created.setRoles("[\"ROLE_SALARY\"]");
        userService.update(created, "NewPass456");

        User updated = userService.findById(created.getId());
        assertNotNull(updated);
        assertEquals("New Name", updated.getNom());
        assertEquals(999.5, updated.getSoldeTotal(), 0.0001);
        assertTrue(updated.hasRole("ROLE_SALARY"));
    }

    @Test
    void deleteTest_deleteUser_shouldNotExistAfterDelete() {
        User u = new User();
        u.setNom("Delete User");
        u.setEmail("ut_del_" + System.currentTimeMillis() + "@mail.com");
        u.setRoles("[\"ROLE_USER\"]");
        u.setSoldeTotal(10.0);
        User created = userService.create(u, "StrongPass123");

        userService.delete(created.getId());
        User fromDb = userService.findById(created.getId());
        assertTrue(fromDb == null);
    }
}

