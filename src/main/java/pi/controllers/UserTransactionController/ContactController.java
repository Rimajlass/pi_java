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
import pi.entities.User;
import pi.mains.Main;
import pi.tools.AppSceneStyles;
import pi.tools.ThemeManager;

import java.io.IOException;

public class ContactController {

    @FXML
    private Label profileNameLabel;

    @FXML
    private Label contactStatusLabel;

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
    private void handleOpenServiceAction(ActionEvent event) {
        try {
            openService((Node) event.getSource());
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page Services.", e);
        }
    }

    @FXML
    private void handleSendMessage(ActionEvent event) {
        contactStatusLabel.setText("Message sent successfully.");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/login-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1460, 780);
            AppSceneStyles.apply(scene, "/pi/styles/login.css");
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
        AppSceneStyles.apply(scene, "/pi/styles/salary-home.css");
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
        AppSceneStyles.apply(scene, "/pi/styles/about.css");
        stage.setTitle("About Us");
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
        AppSceneStyles.apply(scene, "/pi/styles/service.css");
        stage.setTitle("Services");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleToggleTheme(ActionEvent event) {
        ThemeManager.toggleTheme(((Node) event.getSource()).getScene());
    }
}
