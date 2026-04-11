package pi.mains;

import pi.entities.Transaction;
import pi.entities.User;
import pi.services.TransactionService;
import pi.services.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            UserService userService = new UserService();
            TransactionService transactionService = new TransactionService();

            User user = new User();
            user.setNom("Test User");
            user.setEmail("test.user@example.com");
            user.setPassword("password123");
            user.setRoles("USER");
            user.setDateInscription(LocalDate.now());
            user.setSoldeTotal(500.0);

            userService.add(user);
            System.out.println("Added user: " + user);

            User fetchedUser = userService.getById(user.getId());
            System.out.println("Fetched user: " + fetchedUser);

            user.setNom("Test User Updated");
            userService.update(user);
            System.out.println("Updated user: " + userService.getById(user.getId()));

            List<User> users = userService.getAll();
            System.out.println("User count: " + users.size());

            Transaction transaction = new Transaction(
                    user,
                    null,
                    "DEBIT",
                    120.5,
                    LocalDate.now(),
                    "Transaction CRUD test",
                    "Main"
            );

            transactionService.add(transaction);
            System.out.println("Added transaction: " + transaction);

            Transaction fetchedTransaction = transactionService.getById(transaction.getId());
            System.out.println("Fetched transaction: " + fetchedTransaction);

            transaction.setMontant(150.0);
            transaction.setDescription("Updated transaction");
            transactionService.update(transaction);
            System.out.println("Updated transaction: " + transactionService.getById(transaction.getId()));

            List<Transaction> transactions = transactionService.getAll();
            System.out.println("Transaction count: " + transactions.size());

            transactionService.delete(transaction.getId());
            System.out.println("Deleted transaction id: " + transaction.getId());

            userService.delete(user.getId());
            System.out.println("Deleted user id: " + user.getId());
        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Validation error: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("Startup error: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("Cause: " + e.getCause().getMessage());
            }
        }
    }
}
