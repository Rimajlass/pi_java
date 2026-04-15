package UserTransactionTest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pi.services.UserTransactionService.PasswordService;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {

    private static PasswordService passwordService;

    @BeforeAll
    static void init() {
        File phpExe = new File("C:\\xampp\\php\\php.exe");
        assertTrue(phpExe.exists(), "php.exe introuvable: C:\\xampp\\php\\php.exe");
        passwordService = new PasswordService();
    }

    @Test
    void addTest_hashPassword_shouldReturnHashedValue() {
        String hashed = passwordService.hashPassword("PidevPass123");
        assertNotNull(hashed);
        assertTrue(hashed.length() > 20);
    }

    @Test
    void getListTest_verifyPassword_shouldReturnTrueForCorrectPassword() {
        String plain = "StrongPass789";
        String hashed = passwordService.hashPassword(plain);
        boolean ok = passwordService.verifyPassword(plain, hashed);
        assertTrue(ok);
    }

    @Test
    void updateTest_rehashAfterPasswordChange_shouldUseNewPassword() {
        String oldPassword = "OldPass123";
        String newPassword = "NewPass456";
        String oldHash = passwordService.hashPassword(oldPassword);
        String newHash = passwordService.hashPassword(newPassword);

        assertTrue(passwordService.verifyPassword(newPassword, newHash));
        assertFalse(passwordService.verifyPassword(oldPassword, newHash));
        assertFalse(oldHash.equals(newHash));
    }

    @Test
    void deleteHideTest_verifyPassword_shouldFailForWrongPassword() {
        String hashed = passwordService.hashPassword("CorrectOne123");
        boolean ok = passwordService.verifyPassword("WrongOne123", hashed);
        assertFalse(ok);
    }
}

