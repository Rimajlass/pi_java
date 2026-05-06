package pi.controllers.UserTransactionController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import pi.controllers.ExpenseRevenueController.FRONT.SalaryExpenseController;
import pi.controllers.ImprevusCasreelController.ImprevusFrontController;
import pi.entities.User;
import pi.mains.Main;
import pi.savings.ui.SavingsGoalsApp;

import java.io.IOException;

public class ServiceController {

    @FXML
    private Label profileNameLabel;

    public void setUser(User user) {
        if (user == null) {
            return;
        }
        profileNameLabel.setText(user.getNom());
    }

    @FXML
    private void handleBackToHomeAction(ActionEvent event) {
        try {
            openSalaryHome((Node) event.getSource());
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page Home.", e);
        }
    }

    @FXML
    private void handleBackToHome(MouseEvent event) {
        try {
            openSalaryHome((Node) event.getSource());
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page Home.", e);
        }
    }

    @FXML
    private void handleOpenAboutAction(ActionEvent event) {
        try {
            openAbout((Node) event.getSource());
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page About.", e);
        }
    }

    @FXML
    private void handleOpenContactAction(ActionEvent event) {
        try {
            openContact((Node) event.getSource());
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page Contact.", e);
        }
    }

    @FXML
    private void handleOpenServiceAction(ActionEvent event) {
        try {
            openService((Node) event.getSource());
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page Services.", e);
        }
    }

    @FXML
    private void handleOpenSavingsGoals(MouseEvent event) {
        try {
            openSavingsGoals((Node) event.getSource());
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'ouvrir l'interface Savings & Goals.", e);
        }
    }

    @FXML
    private void handleOpenUnexpectedRealCases(MouseEvent event) {
        try {
            openUnexpectedRealCases((Node) event.getSource());
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir l'interface Unexpected Events & Real Cases.", e);
        }
    }

    @FXML
    private void handleOpenRevenueExpenseAction(ActionEvent event) {
        try {
            openRevenueExpense((Node) event.getSource());
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page Revenus & Expenses.", e);
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/login-view.fxml"));
            Parent root = loader.load();

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

    private void openSalaryHome(Node source) throws IOException {
        Stage stage = (Stage) source.getScene().getWindow();
        Object userData = stage.getUserData();

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/salary-home-view.fxml"));
        Parent root = loader.load();
        SalaryHomeController controller = loader.getController();
        if (userData instanceof User user) {
            controller.setUser(user);
        }

        Scene scene = new Scene(root, 1460, 780);
        scene.getStylesheets().add(Main.class.getResource("/pi/styles/salary-home.css").toExternalForm());
        stage.setTitle("Salary Home");
        stage.setScene(scene);
        stage.show();
    }

    private void openAbout(Node source) throws IOException {
        Stage stage = (Stage) source.getScene().getWindow();
        Object userData = stage.getUserData();

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/about-view.fxml"));
        Parent root = loader.load();
        AboutController controller = loader.getController();
        if (userData instanceof User user) {
            controller.setUser(user);
        }

        Scene scene = new Scene(root, 1460, 780);
        scene.getStylesheets().add(Main.class.getResource("/pi/styles/about.css").toExternalForm());
        stage.setTitle("About Us");
        stage.setScene(scene);
        stage.show();
    }

    private void openContact(Node source) throws IOException {
        Stage stage = (Stage) source.getScene().getWindow();
        Object userData = stage.getUserData();

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/contact-view.fxml"));
        Parent root = loader.load();
        ContactController controller = loader.getController();
        if (userData instanceof User user) {
            controller.setUser(user);
        }

        Scene scene = new Scene(root, 1460, 780);
        scene.getStylesheets().add(Main.class.getResource("/pi/styles/contact.css").toExternalForm());
        stage.setTitle("Contact");
        stage.setScene(scene);
        stage.show();
    }

    private void openService(Node source) throws IOException {
        Stage stage = (Stage) source.getScene().getWindow();
        Object userData = stage.getUserData();

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/service-view.fxml"));
        Parent root = loader.load();
        ServiceController controller = loader.getController();
        if (userData instanceof User user) {
            controller.setUser(user);
        }

        Scene scene = new Scene(root, 1460, 780);
        scene.getStylesheets().add(Main.class.getResource("/pi/styles/service.css").toExternalForm());
        stage.setTitle("Services");
        stage.setScene(scene);
        stage.show();
    }

    private void openSavingsGoals(Node source) throws Exception {
        Stage stage = (Stage) source.getScene().getWindow();
        Object userData = stage.getUserData();
        stage.setUserData(userData);
        new SavingsGoalsApp().start(stage);
    }

    private void openRevenueExpense(Node source) throws IOException {
        Stage stage = (Stage) source.getScene().getWindow();
        Object userData = stage.getUserData();

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/Expense/Revenue/FRONT/salary-expense-view.fxml"));
        Parent root = loader.load();
        SalaryExpenseController controller = loader.getController();
        if (userData instanceof User user) {
            controller.setUser(user);
        }

        Scene scene = new Scene(root, 1460, 780);
        stage.setUserData(userData);
        stage.setTitle("Income & Expense Management");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleOpenInvestissement(MouseEvent event) {
        try {
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Object userData = currentStage.getUserData();
            FXMLLoader loader = new FXMLLoader(
                    ServiceController.class.getResource("/Invest/Crypto.fxml"));
            Scene scene = new Scene(loader.load(), 900, 700);
            Object controller = loader.getController();
            if (userData instanceof User user && controller instanceof pi.controllers.InvestissementController.CryptoController cryptoController) {
                cryptoController.setUser(user);
            }
            Stage stage = new Stage();
            stage.setTitle("Investissement");
            stage.setScene(scene);
            stage.setUserData(userData);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'ouvrir l'interface Investissement.", e);
        }
    }

    private void openUnexpectedRealCases(Node source) throws IOException {
        Stage stage = (Stage) source.getScene().getWindow();
        Object userData = stage.getUserData();

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/imprevus-view.fxml"));
        Parent root = loader.load();
        if (userData instanceof User user) {
            Object controller = loader.getController();
            if (controller instanceof ImprevusFrontController imprevusFrontController) {
                imprevusFrontController.setUser(user);
            }
        }

        Scene scene = new Scene(root, 1460, 900);
        stage.setUserData(userData);
        stage.setTitle("Unexpected Events & Real Cases");
        stage.setScene(scene);
        stage.show();
    }
}
