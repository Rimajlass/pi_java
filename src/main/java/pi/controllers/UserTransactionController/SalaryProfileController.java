package pi.controllers.UserTransactionController;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pi.entities.Transaction;
import pi.entities.User;
import pi.mains.Main;
import pi.services.UserTransactionService.TransactionService;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SalaryProfileController {

    @FXML
    private Label profileInitialLabel;
    @FXML
    private Label profileNameLabel;
    @FXML
    private Label profileEmailLabel;
    @FXML
    private Label headerBalanceLabel;
    @FXML
    private Label liveBalanceLabel;
    @FXML
    private Label metricExpenseLabel;
    @FXML
    private Label metricSavingsLabel;
    @FXML
    private Label txCountLabel;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField passwordField;
    @FXML
    private TableView<Transaction> txTable;

    private final TransactionService transactionService = new TransactionService();
    private final DecimalFormat money = new DecimalFormat("#,##0.00");
    private User currentUser;

    @FXML
    public void initialize() {
        buildTable();
    }

    public void setUser(User user) {
        if (user == null) {
            return;
        }
        this.currentUser = user;
        String name = user.getNom() == null || user.getNom().isBlank() ? "User" : user.getNom();
        String email = user.getEmail() == null ? "-" : user.getEmail();
        String initial = name.substring(0, 1).toUpperCase();

        profileInitialLabel.setText(initial);
        profileNameLabel.setText(name);
        profileEmailLabel.setText(email);
        headerBalanceLabel.setText(money.format(user.getSoldeTotal()) + " TND");
        liveBalanceLabel.setText(money.format(user.getSoldeTotal()));

        fullNameField.setText(name);
        emailField.setText(email);
        passwordField.clear();

        loadTransactions(user.getId());
    }

    private void loadTransactions(int userId) {
        List<Transaction> rows = transactionService.findByUserId(userId);
        txTable.setItems(FXCollections.observableArrayList(rows));

        double totalExpense = 0;
        double totalSavings = 0;
        for (Transaction tx : rows) {
            if ("EXPENSE".equalsIgnoreCase(tx.getType())) {
                totalExpense += tx.getMontant();
            } else if ("SAVING".equalsIgnoreCase(tx.getType())) {
                totalSavings += tx.getMontant();
            }
        }

        metricExpenseLabel.setText(money.format(totalExpense) + " TND");
        metricSavingsLabel.setText(money.format(totalSavings) + " TND");
        txCountLabel.setText(String.valueOf(rows.size()));
    }

    private void buildTable() {
        TableColumn<Transaction, String> typeCol = new TableColumn<>("TYPE");
        typeCol.setPrefWidth(130);
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));

        TableColumn<Transaction, String> amountCol = new TableColumn<>("AMOUNT");
        amountCol.setPrefWidth(140);
        amountCol.setCellValueFactory(c -> new SimpleStringProperty(money.format(c.getValue().getMontant()) + " TND"));

        TableColumn<Transaction, String> dateCol = new TableColumn<>("DATE");
        dateCol.setPrefWidth(120);
        dateCol.setCellValueFactory(c -> {
            LocalDate date = c.getValue().getDate();
            return new SimpleStringProperty(date == null ? "-" : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });

        TableColumn<Transaction, String> descCol = new TableColumn<>("DESCRIPTION");
        descCol.setPrefWidth(420);
        descCol.setCellValueFactory(c -> {
            String desc = c.getValue().getDescription();
            return new SimpleStringProperty(desc == null || desc.isBlank() ? "-" : desc);
        });

        txTable.getColumns().setAll(typeCol, amountCol, dateCol, descCol);
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/salary-home-view.fxml"));
            Parent root = loader.load();
            SalaryHomeController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            Stage stage = (Stage) profileNameLabel.getScene().getWindow();
            if (currentUser != null) {
                stage.setUserData(currentUser);
            }
            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/salary-home.css").toExternalForm());
            stage.setTitle("Salary Home");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de revenir au dashboard salary.", e);
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/login-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) profileNameLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/login.css").toExternalForm());
            stage.setUserData(null);
            stage.setTitle("User Secure Login");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page Login.", e);
        }
    }
}

