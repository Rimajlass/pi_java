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

import java.io.IOException;

public class SalaryHomeController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label profileNameLabel;

    private User currentUser;

    @FXML
    private void handleOpenAboutAction(ActionEvent event) {
        openAbout((Node) event.getSource());
    }

    @FXML
    private void handleOpenServiceAction(ActionEvent event) {
        openService((Node) event.getSource());
    }

    @FXML
    private void handleOpenContactAction(ActionEvent event) {
        openContact((Node) event.getSource());
    }

    private void openAbout(Node source) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/about-view.fxml"));
            Parent root = loader.load();
            AboutController controller = loader.getController();

            Stage stage = (Stage) source.getScene().getWindow();
            Object userData = stage.getUserData();
            if (userData instanceof User user) {
                controller.setUser(user);
            }

            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/about.css").toExternalForm());
            stage.setTitle("About Us");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page About.", e);
        }
    }

    private void openService(Node source) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/service-view.fxml"));
            Parent root = loader.load();
            ServiceController controller = loader.getController();

            Stage stage = (Stage) source.getScene().getWindow();
            Object userData = stage.getUserData();
            if (userData instanceof User user) {
                controller.setUser(user);
            }

            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/service.css").toExternalForm());
            stage.setTitle("Services");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page Services.", e);
        }
    }

    private void openContact(Node source) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/contact-view.fxml"));
            Parent root = loader.load();
            ContactController controller = loader.getController();

            Stage stage = (Stage) source.getScene().getWindow();
            Object userData = stage.getUserData();
            if (userData instanceof User user) {
                controller.setUser(user);
            }

            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/contact.css").toExternalForm());
            stage.setTitle("Contact");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page Contact.", e);
        }
    }

    public void setUser(User user) {
        if (user == null) {
            return;
        }
        this.currentUser = user;
        welcomeLabel.setText("Take control of your salary.");
        userNameLabel.setText("Welcome " + user.getNom() + " | " + user.getEmail());
        profileNameLabel.setText(user.getNom());
    }

    @FXML
    private void handleOpenProfileClick(MouseEvent event) {
        try {
            openProfile((Node) event.getSource());
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir le profil salary.", e);
        }
    }

    @FXML
    private void handleLogoutAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/login-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
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

    private void openProfile(Node source) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/salary-profile-view.fxml"));
        Parent root = loader.load();
        SalaryProfileController controller = loader.getController();

        Stage stage = (Stage) source.getScene().getWindow();
        Object userData = stage.getUserData();
        User user = currentUser;
        if (userData instanceof User u) {
            user = u;
        }
        if (user != null) {
            controller.setUser(user);
        }

        Scene scene = new Scene(root, 1460, 780);
        scene.getStylesheets().add(Main.class.getResource("/pi/styles/salary-profile.css").toExternalForm());
        stage.setTitle("My Salary Profile");
        stage.setScene(scene);
        stage.show();
    }

}
