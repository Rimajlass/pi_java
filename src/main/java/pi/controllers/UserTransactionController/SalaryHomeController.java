package pi.controllers.UserTransactionController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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
        welcomeLabel.setText("Take control of your salary.");
        userNameLabel.setText("Welcome " + user.getNom() + " | " + user.getEmail());
        profileNameLabel.setText(user.getNom());
    }

}
