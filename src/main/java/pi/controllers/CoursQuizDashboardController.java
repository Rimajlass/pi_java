package pi.controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class CoursQuizDashboardController {

    @FXML
    private StackPane contentStack;

    @FXML
    private Node frontView;

    @FXML
    private Node backOfficeView;

    @FXML
    private CoursQuizFrontController frontViewController;

    @FXML
    private CoursQuizBackOfficeController backOfficeViewController;

    @FXML
    public void initialize() {
        if (frontViewController != null) {
            frontViewController.setDashboardController(this);
        }
        if (backOfficeViewController != null) {
            backOfficeViewController.setDashboardController(this);
        }

        showFront();
    }

    public void showFront() {
        setVisible(frontView, true);
        setVisible(backOfficeView, false);
        if (frontViewController != null) {
            frontViewController.onShow();
        }
    }

    public void showBackOffice() {
        setVisible(frontView, false);
        setVisible(backOfficeView, true);
        if (backOfficeViewController != null) {
            backOfficeViewController.onShow();
        }
    }

    private void setVisible(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }
}

