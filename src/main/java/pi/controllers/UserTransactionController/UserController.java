package pi.controllers.UserTransactionController;

import pi.entities.User;
import pi.services.UserTransactionService.TransactionService;
import pi.services.UserTransactionService.UserService;

import java.util.List;

public class UserController {

    private final UserService userService;
    private final TransactionService transactionService;

    public UserController() {
        this.userService = new UserService();
        this.transactionService = new TransactionService();
    }

    public List<User> index(String search, String role, String sortBy, String order) {
        return userService.findForAdminIndex(search, role, sortBy, order);
    }

    public User create(User user, String plainPassword) {
        return userService.create(user, plainPassword);
    }

    public User register(String nom, String email, String password, String confirmPassword, String role, double solde,
                         String faceIdCredentialId, String facePlusToken) {
        return userService.register(nom, email, password, confirmPassword, role, solde, faceIdCredentialId, facePlusToken);
    }

    public User show(int userId) {
        return userService.findById(userId);
    }

    public TransactionService.UserDashboardStats showDetails(int userId) {
        return transactionService.buildUserDashboard(userId);
    }

    public TransactionService.UserTransactionSummary transactionsHistory(int userId) {
        return transactionService.buildHistorySummary(userId);
    }

    public User edit(User user, String plainPassword) {
        return userService.update(user, plainPassword);
    }

    public void delete(int userId) {
        userService.delete(userId);
    }

    public User login(String email, String plainPassword) {
        return userService.authenticate(email, plainPassword);
    }

    public boolean validateEmailAvailability(String email) {
        return userService.isEmailAvailable(email);
    }
}
